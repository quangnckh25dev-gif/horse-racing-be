package com.horseracing.service;

import com.horseracing.dto.BetOptionResponse;
import com.horseracing.dto.BetRequest;
import com.horseracing.dto.BetResponse;
import com.horseracing.dto.BetSelectionResponse;
import com.horseracing.dto.BetTicketResponse;
import com.horseracing.dto.BettingHistoryResponse;
import com.horseracing.dto.SettleBetResponse;
import com.horseracing.entity.Bet;
import com.horseracing.entity.BetSelection;
import com.horseracing.entity.BetTicket;
import com.horseracing.entity.Horse;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceEntry;
import com.horseracing.entity.RaceResult;
import com.horseracing.entity.User;
import com.horseracing.repository.BetRepository;
import com.horseracing.repository.BetSelectionRepository;
import com.horseracing.repository.BetTicketRepository;
import com.horseracing.repository.HorseRepository;
import com.horseracing.repository.JockeyRepository;
import com.horseracing.repository.RaceEntryRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RaceResultRepository;
import com.horseracing.repository.SystemConfigRepository;
import com.horseracing.repository.UserRepository;
import com.horseracing.repository.ViolationRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BettingService {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_WON = "Won";
    private static final String STATUS_LOST = "Lost";
    private static final BigDecimal DEFAULT_ODDS = BigDecimal.valueOf(2);
    private static final BigDecimal DEFAULT_EXACT_POSITION_FACTOR = BigDecimal.valueOf(0.25);
    private static final BigDecimal DEFAULT_VIOLATION_ODDS = BigDecimal.valueOf(1.80);
    private static final BigDecimal DEFAULT_ODDS_MAX = BigDecimal.valueOf(8);
    private static final Set<String> VALID_BET_TYPES = Set.of("WIN", "EXACT_POSITION", "VIOLATION");

    private final BetRepository betRepository;
    private final BetTicketRepository betTicketRepository;
    private final BetSelectionRepository betSelectionRepository;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceResultRepository raceResultRepository;
    private final ViolationRepository violationRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final HorseRepository horseRepository;
    private final JockeyRepository jockeyRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final WalletService walletService;

    public BettingService(BetRepository betRepository,
                          BetTicketRepository betTicketRepository,
                          BetSelectionRepository betSelectionRepository,
                          RaceRepository raceRepository,
                          RaceEntryRepository raceEntryRepository,
                          RaceResultRepository raceResultRepository,
                          ViolationRepository violationRepository,
                          SystemConfigRepository systemConfigRepository,
                          HorseRepository horseRepository,
                          JockeyRepository jockeyRepository,
                          UserRepository userRepository,
                          CurrentUserService currentUserService,
                          WalletService walletService) {
        this.betRepository = betRepository;
        this.betTicketRepository = betTicketRepository;
        this.betSelectionRepository = betSelectionRepository;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.raceResultRepository = raceResultRepository;
        this.violationRepository = violationRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.horseRepository = horseRepository;
        this.jockeyRepository = jockeyRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.walletService = walletService;
    }

    public List<BetOptionResponse> getBetOptions(Integer raceId) {
        Race race = ensureRaceExists(raceId);
        List<RaceEntry> entries = raceEntryRepository.findPublicEntriesByRaceId(raceId);
        int maxPosition = resolveMaxBetPosition(entries);
        List<BetOptionResponse> options = new ArrayList<>();

        for (RaceEntry entry : entries) {
            options.addAll(toBetOptionResponses(entry, maxPosition));
        }

        return options;
    }

    public List<BetResponse> getMineByRace(Integer raceId, HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        ensureRaceExists(raceId);
        return betRepository.findByUserIdAndRaceIdOrderByCreatedAtDesc(user.getUserId(), raceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BettingHistoryResponse getMyHistory(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        List<BetResponse> singleBets = betRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
        List<BetTicketResponse> parlayTickets = betTicketRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(this::toTicketResponse)
                .toList();
        return new BettingHistoryResponse(singleBets, parlayTickets);
    }

    @Transactional
    public BetResponse placeBet(Integer raceId, BetRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Race race = ensureRaceOpenForBetting(raceId);
        RaceEntry entry = validateBetRequest(race, request);
        String betType = normalizeBetType(request.getBetType());
        Integer targetPosition = resolveTargetPosition(race, request.getTargetPosition(), betType);

        BigDecimal odds = calculateOddsForBet(entry, betType, targetPosition);
        if (odds.compareTo(BigDecimal.ONE) <= 0) {
            throw new IllegalArgumentException("odds must be greater than 1.");
        }

        Bet bet = new Bet();
        bet.setUserId(user.getUserId());
        bet.setRaceId(raceId);
        bet.setEntryId(request.getEntryId());
        bet.setBetType(betType);
        bet.setTargetPosition(targetPosition);
        bet.setAmount(request.getAmount());
        bet.setOdds(odds);
        bet.setPotentialPayout(request.getAmount().multiply(odds).setScale(2, RoundingMode.HALF_UP));
        bet.setStatus(STATUS_PENDING);

        Bet saved = betRepository.save(bet);
        walletService.debitForBet(user.getUserId(), saved.getAmount(), saved.getBetId());
        return toResponse(saved);
    }

    @Transactional
    public BetTicketResponse placeParlayTicket(Integer raceId, BetRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Race race = ensureRaceOpenForBetting(raceId);
        BigDecimal amount = validateAmount(request == null ? null : request.getAmount());
        if (request.getSelections() == null || request.getSelections().size() < 2) {
            throw new IllegalArgumentException("A parlay ticket requires at least two selections.");
        }

        BigDecimal totalOdds = BigDecimal.ONE;
        List<BetSelection> selections = new ArrayList<>();
        for (BetRequest.SelectionRequest selectionRequest : request.getSelections()) {
            RaceEntry entry = validateSelectionRequest(race, selectionRequest);
            String betType = normalizeBetType(selectionRequest.getBetType());
            Integer targetPosition = resolveTargetPosition(race, selectionRequest.getTargetPosition(), betType);
            BigDecimal odds = calculateOddsForBet(entry, betType, targetPosition);
            totalOdds = totalOdds.multiply(odds);

            BetSelection selection = new BetSelection();
            selection.setRaceId(raceId);
            selection.setEntryId(entry.getEntryId());
            selection.setBetType(betType);
            selection.setTargetPosition(targetPosition);
            selection.setOdds(odds);
            selection.setResolved(false);
            selection.setWon(null);
            selections.add(selection);
        }

        totalOdds = totalOdds.setScale(2, RoundingMode.HALF_UP);
        BetTicket ticket = new BetTicket();
        ticket.setUserId(user.getUserId());
        ticket.setRaceId(raceId);
        ticket.setAmount(amount);
        ticket.setOdds(totalOdds);
        ticket.setPotentialPayout(amount.multiply(totalOdds).setScale(2, RoundingMode.HALF_UP));
        ticket.setStatus(STATUS_PENDING);

        BetTicket savedTicket = betTicketRepository.save(ticket);
        for (BetSelection selection : selections) {
            selection.setTicketId(savedTicket.getTicketId());
        }
        betSelectionRepository.saveAll(selections);
        walletService.debitForBetTicket(user.getUserId(), savedTicket.getAmount(), savedTicket.getTicketId());
        return toTicketResponse(savedTicket);
    }

    @Transactional
    public SettleBetResponse settleRaceBets(Integer raceId) {
        ensureRaceExists(raceId);
        List<Bet> pendingBets = betRepository.findByRaceIdAndStatus(raceId, STATUS_PENDING);
        List<BetTicket> pendingTickets = betTicketRepository.findByRaceIdAndStatus(raceId, STATUS_PENDING);
        List<RaceResult> results = raceResultRepository.findByRaceId(raceId);

        if ((pendingBets.isEmpty() && pendingTickets.isEmpty()) || results.isEmpty()) {
            return new SettleBetResponse(raceId, pendingBets.size() + pendingTickets.size(), 0, 0);
        }

        Map<Integer, RaceResult> resultByEntryId = new HashMap<>();
        for (RaceResult result : results) {
            resultByEntryId.put(result.getEntryId(), result);
        }

        int won = 0;
        int lost = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Bet bet : pendingBets) {
            RaceResult result = resultByEntryId.get(bet.getEntryId());
            if (isWinningSelection(raceId, bet.getEntryId(), bet.getBetType(), bet.getTargetPosition(), result)) {
                bet.setStatus(STATUS_WON);
                walletService.creditBetWin(bet.getUserId(), bet.getPotentialPayout(), bet.getBetId());
                won++;
            } else {
                bet.setStatus(STATUS_LOST);
                lost++;
            }
            bet.setSettledAt(now);
        }

        betRepository.saveAll(pendingBets);

        for (BetTicket ticket : pendingTickets) {
            List<BetSelection> selections = betSelectionRepository.findByTicketIdOrderBySelectionIdAsc(ticket.getTicketId());
            boolean allWon = true;
            for (BetSelection selection : selections) {
                boolean selectionWon = isWinningSelection(
                        raceId,
                        selection.getEntryId(),
                        selection.getBetType(),
                        selection.getTargetPosition(),
                        resultByEntryId.get(selection.getEntryId())
                );
                selection.setResolved(true);
                selection.setWon(selectionWon);
                if (!selectionWon) {
                    allWon = false;
                }
            }
            if (allWon) {
                ticket.setStatus(STATUS_WON);
                walletService.creditBetTicketWin(ticket.getUserId(), ticket.getPotentialPayout(), ticket.getTicketId());
                won++;
            } else {
                ticket.setStatus(STATUS_LOST);
                lost++;
            }
            ticket.setSettledAt(now);
            betSelectionRepository.saveAll(selections);
        }

        betTicketRepository.saveAll(pendingTickets);
        return new SettleBetResponse(raceId, pendingBets.size() + pendingTickets.size(), won, lost);
    }

    private boolean isWinningSelection(Integer raceId, Integer entryId, String betType,
                                       Integer targetPosition, RaceResult result) {
        if ("VIOLATION".equals(betType)) {
            return violationRepository.countEntryViolations(raceId, entryId) > 0;
        }
        if (result == null || Boolean.TRUE.equals(result.getDnf()) || Boolean.TRUE.equals(result.getDq())
                || result.getFinishPosition() == null) {
            return false;
        }

        return switch (betType) {
            case "WIN" -> Integer.valueOf(1).equals(result.getFinishPosition());
            case "PLACE" -> result.getFinishPosition() <= 2;
            case "SHOW" -> result.getFinishPosition() <= 3;
            case "EXACT" -> targetPosition != null && targetPosition.equals(result.getFinishPosition());
            case "EXACT_POSITION" -> targetPosition != null && targetPosition.equals(result.getFinishPosition());
            default -> false;
        };
    }

    private Race ensureRaceOpenForBetting(Integer raceId) {
        Race race = ensureRaceExists(raceId);
        if (race.getRaceDate() != null && !race.getRaceDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("The race has already started. Betting is no longer allowed.");
        }
        if (!"RegistrationOpen".equalsIgnoreCase(race.getStatus())) {
            throw new IllegalArgumentException("This race is not open for betting.");
        }
        return race;
    }

    private Race ensureRaceExists(Integer raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("raceId is required.");
        }
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Race was not found."));
    }

    private RaceEntry validateBetRequest(Race race, BetRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Bet data is invalid.");
        }
        if (request.getEntryId() == null) {
            throw new IllegalArgumentException("entryId is required.");
        }
        validateAmount(request.getAmount());

        RaceEntry entry = raceEntryRepository.findById(request.getEntryId())
                .orElseThrow(() -> new IllegalArgumentException("Race entry was not found."));
        if (!race.getRaceId().equals(entry.getRaceId())) {
            throw new IllegalArgumentException("Race entry does not belong to this race.");
        }
        if (!"Ready".equalsIgnoreCase(entry.getRegistrationStatus())
                || entry.getJockeyId() == null
                || !Boolean.TRUE.equals(entry.getJockeyConfirmed())) {
            throw new IllegalArgumentException("Only ready entries with confirmed jockey can race.");
        }
        return entry;
    }

    private RaceEntry validateSelectionRequest(Race race, BetRequest.SelectionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Selection data is invalid.");
        }
        BetRequest wrapper = new BetRequest();
        wrapper.setEntryId(request.getEntryId());
        wrapper.setBetType(request.getBetType());
        wrapper.setTargetPosition(request.getTargetPosition());
        wrapper.setAmount(BigDecimal.ONE);
        return validateBetRequest(race, wrapper);
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0.");
        }
        return amount;
    }

    private String normalizeBetType(String betType) {
        String normalized = betType == null || betType.isBlank()
                ? "WIN"
                : betType.trim().toUpperCase(Locale.ROOT);
        if ("EXACT".equals(normalized)) {
            normalized = "EXACT_POSITION";
        }
        if (!VALID_BET_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("betType only accepts WIN, EXACT_POSITION, or VIOLATION.");
        }
        return normalized;
    }

    private Integer resolveTargetPosition(Race race, Integer requestedTargetPosition, String betType) {
        if (!"EXACT_POSITION".equals(betType)) {
            return null;
        }
        Integer targetPosition = requestedTargetPosition;
        if (targetPosition == null || targetPosition <= 0) {
            throw new IllegalArgumentException("targetPosition is required when betType is EXACT_POSITION.");
        }
        int maxPosition = resolveMaxBetPosition(raceEntryRepository.findPublicEntriesByRaceId(race.getRaceId()));
        if (targetPosition > maxPosition) {
            throw new IllegalArgumentException("targetPosition cannot exceed the number of bettable positions.");
        }
        return targetPosition;
    }

    private BigDecimal calculateOddsForBet(RaceEntry entry, String betType, Integer targetPosition) {
        Integer horseRank = horseRepository.findHorseRank(entry.getHorseId()).orElse(null);
        BigDecimal baseOdds = resolveBaseOdds(entry, horseRank);
        BigDecimal odds = switch (betType) {
            case "EXACT_POSITION" -> calculateExactOdds(baseOdds, targetPosition);
            case "VIOLATION" -> readConfigDecimal("ODDS_VIOLATION", DEFAULT_VIOLATION_ODDS);
            default -> baseOdds;
        };
        return normalizeOdds(odds);
    }

    private List<BetOptionResponse> toBetOptionResponses(RaceEntry entry, int maxPosition) {
        List<BetOptionResponse> options = new ArrayList<>();
        Horse horse = horseRepository.findById(entry.getHorseId()).orElse(null);
        String jockeyName = entry.getJockeyId() == null ? null : jockeyRepository.findById(entry.getJockeyId())
                .flatMap(jockey -> userRepository.findById(jockey.getUserId()))
                .map(User::getFullName)
                .orElse(null);
        Integer horseRank = horse == null ? null : horseRepository.findHorseRank(horse.getHorseId()).orElse(null);
        BigDecimal baseOdds = resolveBaseOdds(entry, horseRank);

        options.add(toBetOptionResponse(entry, horse, jockeyName, horseRank, "WIN", null, baseOdds, baseOdds));
        options.add(toBetOptionResponse(entry, horse, jockeyName, horseRank, "VIOLATION", null,
                baseOdds, readConfigDecimal("ODDS_VIOLATION", DEFAULT_VIOLATION_ODDS)));

        for (int position = 1; position <= maxPosition; position++) {
            options.add(toBetOptionResponse(entry, horse, jockeyName, horseRank, "EXACT_POSITION", position,
                    baseOdds, calculateExactOdds(baseOdds, position)));
        }

        return options;
    }

    private BetOptionResponse toBetOptionResponse(RaceEntry entry, Horse horse, String jockeyName,
                                                  Integer horseRank, String betType, Integer targetPosition,
                                                  BigDecimal baseOdds, BigDecimal odds) {
        return new BetOptionResponse(
                entry.getEntryId(),
                entry.getHorseId(),
                horse == null ? null : horse.getHorseName(),
                entry.getJockeyId(),
                jockeyName,
                entry.getLaneNumber(),
                horseRank,
                betType,
                targetPosition,
                normalizeOdds(baseOdds),
                normalizeOdds(odds)
        );
    }

    private int resolveMaxBetPosition(List<RaceEntry> entries) {
        return Math.max(entries.size(), 1);
    }

    private BigDecimal resolveBaseOdds(RaceEntry entry, Integer horseRank) {
        if (entry.getOdds() != null && entry.getOdds().compareTo(BigDecimal.ONE) > 0) {
            return entry.getOdds();
        }
        if (horseRank == null || horseRank <= 0) {
            return readConfigDecimal("ODDS_BASE_UNRANKED", BigDecimal.valueOf(2.50));
        }
        if (horseRank <= 15) {
            return readConfigDecimal("ODDS_BASE_RANK_" + horseRank, DEFAULT_ODDS);
        }
        return readConfigDecimal("ODDS_BASE_RANK_OVER_15", BigDecimal.valueOf(5.00));
    }

    private BigDecimal calculateExactOdds(BigDecimal baseOdds, Integer targetPosition) {
        if (targetPosition == null) {
            return baseOdds;
        }
        BigDecimal factor = readConfigDecimal("EXACT_POSITION_FACTOR", DEFAULT_EXACT_POSITION_FACTOR);
        BigDecimal extra = factor.multiply(BigDecimal.valueOf(Math.max(targetPosition - 1, 0)));
        return baseOdds.add(extra);
    }

    private BigDecimal readConfigDecimal(String key, BigDecimal defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
                .map(config -> parseDecimal(config.getConfigValue(), defaultValue))
                .orElse(defaultValue);
    }

    private BigDecimal parseDecimal(String value, BigDecimal defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private BigDecimal normalizeOdds(BigDecimal odds) {
        BigDecimal maxOdds = readConfigDecimal("ODDS_MAX", DEFAULT_ODDS_MAX);
        // Keep a positive odds floor so payout calculations remain valid.
        BigDecimal minOdds = readConfigDecimal("ODDS_MIN", BigDecimal.valueOf(1.1));
        BigDecimal value = odds == null ? DEFAULT_ODDS : odds;
        if (value.compareTo(maxOdds) > 0) {
            value = maxOdds;
        }
        if (value.compareTo(minOdds) < 0) {
            value = minOdds;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BetResponse toResponse(Bet bet) {
        Race race = raceRepository.findById(bet.getRaceId()).orElse(null);
        RaceEntry entry = raceEntryRepository.findById(bet.getEntryId()).orElse(null);
        Horse horse = entry == null ? null : horseRepository.findById(entry.getHorseId()).orElse(null);
        Integer jockeyId = entry == null ? null : entry.getJockeyId();
        String jockeyName = jockeyId == null ? null : jockeyRepository.findById(jockeyId)
                .flatMap(jockey -> userRepository.findById(jockey.getUserId()))
                .map(User::getFullName)
                .orElse(null);
        return new BetResponse(
                bet.getBetId(),
                bet.getUserId(),
                bet.getRaceId(),
                race == null ? null : race.getRaceName(),
                bet.getEntryId(),
                horse == null ? null : horse.getHorseId(),
                horse == null ? null : horse.getHorseName(),
                jockeyId,
                jockeyName,
                bet.getBetType(),
                bet.getTargetPosition(),
                bet.getAmount(),
                bet.getOdds(),
                bet.getPotentialPayout(),
                bet.getStatus(),
                bet.getCreatedAt(),
                bet.getSettledAt()
        );
    }

    private BetTicketResponse toTicketResponse(BetTicket ticket) {
        Race race = raceRepository.findById(ticket.getRaceId()).orElse(null);
        List<BetSelectionResponse> selections = betSelectionRepository
                .findByTicketIdOrderBySelectionIdAsc(ticket.getTicketId())
                .stream()
                .map(this::toSelectionResponse)
                .toList();
        return new BetTicketResponse(
                ticket.getTicketId(),
                ticket.getUserId(),
                ticket.getRaceId(),
                race == null ? null : race.getRaceName(),
                ticket.getAmount(),
                ticket.getOdds(),
                ticket.getPotentialPayout(),
                ticket.getStatus(),
                selections,
                ticket.getCreatedAt(),
                ticket.getSettledAt()
        );
    }

    private BetSelectionResponse toSelectionResponse(BetSelection selection) {
        RaceEntry entry = raceEntryRepository.findById(selection.getEntryId()).orElse(null);
        Horse horse = entry == null ? null : horseRepository.findById(entry.getHorseId()).orElse(null);
        return new BetSelectionResponse(
                selection.getSelectionId(),
                selection.getEntryId(),
                horse == null ? null : horse.getHorseId(),
                horse == null ? null : horse.getHorseName(),
                selection.getBetType(),
                selection.getTargetPosition(),
                selection.getOdds(),
                selection.getResolved(),
                selection.getWon()
        );
    }
}
