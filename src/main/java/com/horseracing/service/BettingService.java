package com.horseracing.service;

import com.horseracing.dto.BetOptionResponse;
import com.horseracing.dto.BetRequest;
import com.horseracing.dto.BetResponse;
import com.horseracing.dto.SettleBetResponse;
import com.horseracing.entity.Bet;
import com.horseracing.entity.Horse;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceEntry;
import com.horseracing.entity.RaceResult;
import com.horseracing.entity.User;
import com.horseracing.repository.BetRepository;
import com.horseracing.repository.HorseRepository;
import com.horseracing.repository.JockeyRepository;
import com.horseracing.repository.RaceEntryRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RaceResultRepository;
import com.horseracing.repository.SystemConfigRepository;
import com.horseracing.repository.UserRepository;
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
    private static final BigDecimal DEFAULT_ODDS_FACTOR_PLACE = BigDecimal.valueOf(0.75);
    private static final BigDecimal DEFAULT_ODDS_FACTOR_SHOW = BigDecimal.valueOf(0.50);
    private static final BigDecimal DEFAULT_ODDS_MAX = BigDecimal.valueOf(8);
    private static final Set<String> VALID_BET_TYPES = Set.of("WIN", "PLACE", "SHOW", "EXACT");

    private final BetRepository betRepository;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceResultRepository raceResultRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final HorseRepository horseRepository;
    private final JockeyRepository jockeyRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final WalletService walletService;

    public BettingService(BetRepository betRepository,
                          RaceRepository raceRepository,
                          RaceEntryRepository raceEntryRepository,
                          RaceResultRepository raceResultRepository,
                          SystemConfigRepository systemConfigRepository,
                          HorseRepository horseRepository,
                          JockeyRepository jockeyRepository,
                          UserRepository userRepository,
                          CurrentUserService currentUserService,
                          WalletService walletService) {
        this.betRepository = betRepository;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.raceResultRepository = raceResultRepository;
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

    public List<BetResponse> getMyHistory(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        return betRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BetResponse placeBet(Integer raceId, BetRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Race race = ensureRaceOpenForBetting(raceId);
        RaceEntry entry = validateBetRequest(race, request);
        String betType = normalizeBetType(request.getBetType());
        Integer targetPosition = resolveTargetPosition(race, request, betType);

        BigDecimal odds = calculateOddsForBet(entry, betType, targetPosition);
        if (odds.compareTo(BigDecimal.ONE) <= 0) {
            throw new IllegalArgumentException("odds phai lon hon 1");
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
    public SettleBetResponse settleRaceBets(Integer raceId) {
        ensureRaceExists(raceId);
        List<Bet> pendingBets = betRepository.findByRaceIdAndStatus(raceId, STATUS_PENDING);
        List<RaceResult> results = raceResultRepository.findByRaceId(raceId);

        if (pendingBets.isEmpty() || results.isEmpty()) {
            return new SettleBetResponse(raceId, pendingBets.size(), 0, 0);
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
            if (isWinningBet(bet, result)) {
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
        return new SettleBetResponse(raceId, pendingBets.size(), won, lost);
    }

    private boolean isWinningBet(Bet bet, RaceResult result) {
        if (result == null || Boolean.TRUE.equals(result.getDnf()) || Boolean.TRUE.equals(result.getDq())
                || result.getFinishPosition() == null) {
            return false;
        }

        return switch (bet.getBetType()) {
            case "WIN" -> Integer.valueOf(1).equals(result.getFinishPosition());
            case "PLACE" -> result.getFinishPosition() <= 2;
            case "SHOW" -> result.getFinishPosition() <= 3;
            case "EXACT" -> bet.getTargetPosition() != null
                    && bet.getTargetPosition().equals(result.getFinishPosition());
            default -> false;
        };
    }

    private Race ensureRaceOpenForBetting(Integer raceId) {
        Race race = ensureRaceExists(raceId);
        if (race.getRaceDate() != null && !race.getRaceDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Race da bat dau, khong the dat cuoc");
        }
        if (!"Scheduled".equalsIgnoreCase(race.getStatus())
                && !"RegistrationOpen".equalsIgnoreCase(race.getStatus())) {
            throw new IllegalArgumentException("Race khong con mo dat cuoc");
        }
        return race;
    }

    private Race ensureRaceExists(Integer raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("raceId khong duoc de trong");
        }
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay race"));
    }

    private RaceEntry validateBetRequest(Race race, BetRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu dat cuoc khong hop le");
        }
        if (request.getEntryId() == null) {
            throw new IllegalArgumentException("entryId khong duoc de trong");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount phai lon hon 0");
        }

        RaceEntry entry = raceEntryRepository.findById(request.getEntryId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay race entry"));
        if (!race.getRaceId().equals(entry.getRaceId())) {
            throw new IllegalArgumentException("Race entry khong thuoc race nay");
        }
        if (!"Approved".equalsIgnoreCase(entry.getRegistrationStatus())
                && !"Ready".equalsIgnoreCase(entry.getRegistrationStatus())) {
            throw new IllegalArgumentException("Chi duoc dat cuoc entry da duoc duyet");
        }
        return entry;
    }

    private String normalizeBetType(String betType) {
        String normalized = betType == null || betType.isBlank()
                ? "WIN"
                : betType.trim().toUpperCase(Locale.ROOT);
        if (!VALID_BET_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("betType chi chap nhan WIN, PLACE, SHOW, EXACT");
        }
        return normalized;
    }

    private Integer resolveTargetPosition(Race race, BetRequest request, String betType) {
        if (!"EXACT".equals(betType)) {
            return null;
        }
        Integer targetPosition = request.getTargetPosition();
        if (targetPosition == null || targetPosition <= 0) {
            throw new IllegalArgumentException("targetPosition bat buoc khi betType la EXACT");
        }
        int maxPosition = resolveMaxBetPosition(raceEntryRepository.findPublicEntriesByRaceId(race.getRaceId()));
        if (targetPosition > maxPosition) {
            throw new IllegalArgumentException("targetPosition khong duoc lon hon so vi tri co the cuoc");
        }
        return targetPosition;
    }

    private BigDecimal calculateOddsForBet(RaceEntry entry, String betType, Integer targetPosition) {
        Integer horseRank = horseRepository.findHorseRank(entry.getHorseId()).orElse(null);
        BigDecimal baseOdds = resolveBaseOdds(entry, horseRank);
        BigDecimal odds = switch (betType) {
            case "PLACE" -> baseOdds.multiply(readConfigDecimal("ODDS_FACTOR_PLACE", DEFAULT_ODDS_FACTOR_PLACE));
            case "SHOW" -> baseOdds.multiply(readConfigDecimal("ODDS_FACTOR_SHOW", DEFAULT_ODDS_FACTOR_SHOW));
            case "EXACT" -> calculateExactOdds(baseOdds, targetPosition);
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
        options.add(toBetOptionResponse(entry, horse, jockeyName, horseRank, "PLACE", null,
                baseOdds, baseOdds.multiply(readConfigDecimal("ODDS_FACTOR_PLACE", DEFAULT_ODDS_FACTOR_PLACE))));
        options.add(toBetOptionResponse(entry, horse, jockeyName, horseRank, "SHOW", null,
                baseOdds, baseOdds.multiply(readConfigDecimal("ODDS_FACTOR_SHOW", DEFAULT_ODDS_FACTOR_SHOW))));

        for (int position = 1; position <= maxPosition; position++) {
            options.add(toBetOptionResponse(entry, horse, jockeyName, horseRank, "EXACT", position,
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
        // San ODDS_MIN 1.1: PLACE/SHOW nhan he so < 1 nen odds co the <= 1
        // => placeBet tu chan ("odds phai lon hon 1") va nguoi thang van lo tien. KHONG XOA!
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
        return new BetResponse(
                bet.getBetId(),
                bet.getUserId(),
                bet.getRaceId(),
                race == null ? null : race.getRaceName(),
                bet.getEntryId(),
                horse == null ? null : horse.getHorseName(),
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
}
