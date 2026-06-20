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
import com.horseracing.repository.RaceEntryRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RaceResultRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BettingService {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_WON = "Won";
    private static final String STATUS_LOST = "Lost";
    private static final BigDecimal DEFAULT_ODDS = BigDecimal.valueOf(2);

    private final BetRepository betRepository;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceResultRepository raceResultRepository;
    private final HorseRepository horseRepository;
    private final CurrentUserService currentUserService;
    private final WalletService walletService;

    public BettingService(BetRepository betRepository,
                          RaceRepository raceRepository,
                          RaceEntryRepository raceEntryRepository,
                          RaceResultRepository raceResultRepository,
                          HorseRepository horseRepository,
                          CurrentUserService currentUserService,
                          WalletService walletService) {
        this.betRepository = betRepository;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.raceResultRepository = raceResultRepository;
        this.horseRepository = horseRepository;
        this.currentUserService = currentUserService;
        this.walletService = walletService;
    }

    public List<BetOptionResponse> getBetOptions(Integer raceId) {
        ensureRaceExists(raceId);
        return raceEntryRepository.findByRaceId(raceId)
                .stream()
                .filter(entry -> "Approved".equalsIgnoreCase(entry.getRegistrationStatus()))
                .map(this::toBetOptionResponse)
                .toList();
    }

    public BetResponse getMineByRace(Integer raceId, HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        ensureRaceExists(raceId);
        Bet bet = betRepository.findByUserIdAndRaceId(user.getUserId(), raceId)
                .orElseThrow(() -> new IllegalArgumentException("Chua co ve cuoc cho race nay"));
        return toResponse(bet);
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
        validateBetRequest(race.getRaceId(), request);

        if (betRepository.findByUserIdAndRaceId(user.getUserId(), raceId).isPresent()) {
            throw new IllegalArgumentException("Ban da dat cuoc race nay");
        }

        BigDecimal odds = request.getOdds() == null ? DEFAULT_ODDS : request.getOdds();
        if (odds.compareTo(BigDecimal.ONE) <= 0) {
            throw new IllegalArgumentException("odds phai lon hon 1");
        }

        Bet bet = new Bet();
        bet.setUserId(user.getUserId());
        bet.setRaceId(raceId);
        bet.setEntryId(request.getEntryId());
        bet.setBetType("WIN");
        bet.setAmount(request.getAmount());
        bet.setOdds(odds);
        bet.setPotentialPayout(request.getAmount().multiply(odds));
        bet.setStatus(STATUS_PENDING);

        Bet saved = betRepository.save(bet);
        walletService.debitForBet(user.getUserId(), saved.getAmount(), saved.getBetId());
        return toResponse(saved);
    }

    @Transactional
    public SettleBetResponse settleRaceBets(Integer raceId) {
        ensureRaceExists(raceId);
        List<Bet> pendingBets = betRepository.findByRaceIdAndStatus(raceId, STATUS_PENDING);
        Optional<RaceResult> winner = raceResultRepository.findByRaceId(raceId)
                .stream()
                .filter(result -> Integer.valueOf(1).equals(result.getFinishPosition()))
                .findFirst();

        if (winner.isEmpty()) {
            return new SettleBetResponse(raceId, pendingBets.size(), 0, 0);
        }

        int won = 0;
        int lost = 0;
        LocalDateTime now = LocalDateTime.now();
        Integer winnerEntryId = winner.get().getEntryId();

        for (Bet bet : pendingBets) {
            if (winnerEntryId.equals(bet.getEntryId())) {
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

    private Race ensureRaceOpenForBetting(Integer raceId) {
        Race race = ensureRaceExists(raceId);
        if (race.getRaceDate() != null && !race.getRaceDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Race da bat dau, khong the dat cuoc");
        }
        if (race.getStatus() != null
                && ("Running".equalsIgnoreCase(race.getStatus()) || "Finished".equalsIgnoreCase(race.getStatus()))) {
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

    private void validateBetRequest(Integer raceId, BetRequest request) {
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
        if (!raceId.equals(entry.getRaceId())) {
            throw new IllegalArgumentException("Race entry khong thuoc race nay");
        }
        if (!"Approved".equalsIgnoreCase(entry.getRegistrationStatus())) {
            throw new IllegalArgumentException("Chi duoc dat cuoc entry da duoc duyet");
        }
    }

    private BetOptionResponse toBetOptionResponse(RaceEntry entry) {
        Horse horse = horseRepository.findById(entry.getHorseId()).orElse(null);
        return new BetOptionResponse(
                entry.getEntryId(),
                entry.getHorseId(),
                horse == null ? null : horse.getHorseName(),
                entry.getJockeyId(),
                DEFAULT_ODDS
        );
    }

    private BetResponse toResponse(Bet bet) {
        return new BetResponse(
                bet.getBetId(),
                bet.getUserId(),
                bet.getRaceId(),
                bet.getEntryId(),
                bet.getBetType(),
                bet.getAmount(),
                bet.getOdds(),
                bet.getPotentialPayout(),
                bet.getStatus(),
                bet.getCreatedAt(),
                bet.getSettledAt()
        );
    }
}
