package com.horseracing.service;

import com.horseracing.dto.DashboardResponse;
import com.horseracing.dto.RaceSummaryResponse;
import com.horseracing.dto.SharedDashboardResponse;
import com.horseracing.entity.Race;
import com.horseracing.entity.User;
import com.horseracing.repository.HorseRepository;
import com.horseracing.repository.JockeyRepository;
import com.horseracing.repository.RaceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @PersistenceContext
    private EntityManager entityManager;

    private final RaceRepository raceRepository;
    private final HorseRepository horseRepository;
    private final JockeyRepository jockeyRepository;
    private final LeaderboardService leaderboardService;
    private final CurrentUserService currentUserService;

    public DashboardService(RaceRepository raceRepository,
                            HorseRepository horseRepository,
                            JockeyRepository jockeyRepository,
                            LeaderboardService leaderboardService,
                            CurrentUserService currentUserService) {
        this.raceRepository = raceRepository;
        this.horseRepository = horseRepository;
        this.jockeyRepository = jockeyRepository;
        this.leaderboardService = leaderboardService;
        this.currentUserService = currentUserService;
    }

    public Object getDashboardForCurrentUser(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        return switch (roleName(user)) {
            case "Admin" -> buildAdminDashboard();
            case "Organizer" -> buildOrganizerDashboard(user);
            case "Referee" -> buildRefereeDashboard(user);
            case "HorseOwner" -> buildOwnerDashboard(user);
            case "Jockey" -> buildJockeyDashboard(user);
            case "Spectator" -> buildSpectatorDashboard(user);
            default -> throw new IllegalArgumentException("Dashboard is not available for this role.");
        };
    }

    public Object getAdminDashboard(HttpServletRequest request) {
        User user = requireRole(request, "Admin");
        return buildAdminDashboard();
    }

    public Object getOrganizerDashboard(HttpServletRequest request) {
        User user = requireRole(request, "Organizer");
        return buildOrganizerDashboard(user);
    }

    public Object getRefereeDashboard(HttpServletRequest request) {
        User user = requireRole(request, "Referee");
        return buildRefereeDashboard(user);
    }

    public Object getOwnerDashboard(HttpServletRequest request) {
        User user = requireRole(request, "HorseOwner");
        return buildOwnerDashboard(user);
    }

    public Object getJockeyDashboard(HttpServletRequest request) {
        User user = requireRole(request, "Jockey");
        return buildJockeyDashboard(user);
    }

    public Object getSpectatorDashboard(HttpServletRequest request) {
        User user = requireRole(request, "Spectator");
        return buildSpectatorDashboard(user);
    }

    public DashboardResponse getDashboard() {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                        SELECT TotalActiveUsers, PendingApprovals, OngoingTournaments,
                               UpcomingRaces, FinishedRaces, TotalHorses, TotalJockeys,
                               TotalBets, WonBets
                        FROM vw_SystemDashboard
                        """)
                .getSingleResult();

        return new DashboardResponse(
                toInteger(row[0]),
                toInteger(row[1]),
                toInteger(row[2]),
                toInteger(row[3]),
                toInteger(row[4]),
                toInteger(row[5]),
                toInteger(row[6]),
                toInteger(row[7]),
                toInteger(row[8])
        );
    }

    public SharedDashboardResponse getSharedDashboard() {
        List<RaceSummaryResponse> featuredRaces = raceRepository.findFeaturedDashboardRaces()
                .stream()
                .map(this::toRaceSummary)
                .toList();

        return new SharedDashboardResponse(
                toInteger(raceRepository.count()),
                toInteger(raceRepository.countByStatus("Draft")),
                toInteger(raceRepository.countByStatus("Ongoing")),
                toInteger(raceRepository.countByStatus("Finished")),
                toInteger(raceRepository.countByStatus("RegistrationOpen")),
                toInteger(horseRepository.count()),
                toInteger(jockeyRepository.count()),
                leaderboardService.getGlobalJockeyLeaderboard().stream().limit(5).toList(),
                leaderboardService.getGlobalHorseLeaderboard().stream().limit(5).toList(),
                featuredRaces
        );
    }

    private Map<String, Object> buildAdminDashboard() {
        Map<String, Object> data = baseDashboard("Admin");
        data.put("metrics", List.of(
                metric("Active Users", count("""
                        SELECT COUNT(1)
                        FROM Users u
                        JOIN Roles r ON r.RoleID = u.RoleID
                        WHERE u.IsActive = 1 AND r.RoleName <> 'Admin'
                        """), "Accounts available"),
                metric("Pending Accounts", count("""
                        SELECT COUNT(1)
                        FROM Users u
                        JOIN Roles r ON r.RoleID = u.RoleID
                        WHERE u.IsApproved = 0 AND r.RoleName NOT IN ('Admin', 'Spectator')
                        """), "Awaiting approval"),
                metric("Pending Deposits", count("SELECT COUNT(1) FROM DepositRequests WHERE Status = 'Pending'"), "Need review"),
                metric("Open Complaints", count("""
                        SELECT
                            (SELECT COUNT(1) FROM DepositComplaints WHERE Status = 'Pending')
                          + (SELECT COUNT(1) FROM RaceComplaints WHERE Status = 'Forwarded')
                        """), "Deposits and races")
        ));
        data.put("pendingAccounts", rows("""
                SELECT TOP 6 u.UserID, u.Username, u.FullName, u.Email, r.RoleName, u.CreatedAt
                FROM Users u
                JOIN Roles r ON r.RoleID = u.RoleID
                WHERE u.IsApproved = 0 AND r.RoleName NOT IN ('Admin', 'Spectator')
                ORDER BY u.CreatedAt DESC
                """, "userId", "username", "fullName", "email", "role", "createdAt"));
        data.put("recentDeposits", rows("""
                SELECT TOP 6 dr.DepositRequestID, u.Username, u.FullName, dr.Amount, dr.PaymentMethod, dr.TransferCode, dr.Status, dr.CreatedAt
                FROM DepositRequests dr
                JOIN Users u ON u.UserID = dr.UserID
                ORDER BY dr.CreatedAt DESC
                """, "depositRequestId", "username", "fullName", "amount", "paymentMethod", "transferCode", "status", "createdAt"));
        data.put("systemStatus", List.of(
                statusItem("API Service", "Healthy"),
                statusItem("Database", "Healthy"),
                statusItem("Auth Service", "Healthy"),
                statusItem("Payment Review", "Active")
        ));
        return data;
    }

    private Map<String, Object> buildOrganizerDashboard(User user) {
        Map<String, Object> data = baseDashboard("Organizer");
        data.put("metrics", List.of(
                metric("Draft Races", count("""
                        SELECT COUNT(1)
                        FROM Races r
                        JOIN Tournaments t ON t.TournamentID = r.TournamentID
                        WHERE t.CreatedBy = :userId AND r.Status = 'Draft'
                        """, Map.of("userId", user.getUserId())), "In progress"),
                metric("Registration Open", count("""
                        SELECT COUNT(1)
                        FROM Races r
                        JOIN Tournaments t ON t.TournamentID = r.TournamentID
                        WHERE t.CreatedBy = :userId AND r.Status = 'RegistrationOpen'
                        """, Map.of("userId", user.getUserId())), "Open for entries"),
                metric("Entry Approvals", count("""
                        SELECT COUNT(1)
                        FROM RaceEntries re
                        JOIN Races r ON r.RaceID = re.RaceID
                        JOIN Tournaments t ON t.TournamentID = r.TournamentID
                        WHERE t.CreatedBy = :userId AND re.RegistrationStatus = 'Pending'
                        """, Map.of("userId", user.getUserId())), "Pending"),
                metric("Results Pending", count("""
                        SELECT COUNT(DISTINCT rr.RaceID)
                        FROM RaceResults rr
                        JOIN Races r ON r.RaceID = rr.RaceID
                        JOIN Tournaments t ON t.TournamentID = r.TournamentID
                        WHERE t.CreatedBy = :userId AND rr.ApprovalStatus = 'Pending'
                        """, Map.of("userId", user.getUserId())), "Awaiting review")
        ));
        data.put("upcomingRaces", rows("""
                SELECT TOP 6 r.RaceID, r.RaceName, r.RaceDate, r.TrackLength, r.Status, r.MaxParticipants,
                       (SELECT COUNT(1) FROM RaceEntries re WHERE re.RaceID = r.RaceID AND re.RegistrationStatus NOT IN ('Rejected', 'Withdrawn', 'PreRaceRejected')) AS Entries
                FROM Races r
                JOIN Tournaments t ON t.TournamentID = r.TournamentID
                WHERE t.CreatedBy = :userId
                  AND r.Status IN ('Draft', 'RegistrationOpen')
                ORDER BY r.RaceDate ASC
                """, Map.of("userId", user.getUserId()),
                "raceId", "raceName", "raceDate", "trackLength", "status", "maxParticipants", "entries"));
        data.put("entryApprovals", rows("""
                SELECT TOP 6 re.EntryID, r.RaceName, h.HorseName, u.FullName AS OwnerName, re.RegisteredAt, re.RegistrationStatus
                FROM RaceEntries re
                JOIN Races r ON r.RaceID = re.RaceID
                JOIN Tournaments t ON t.TournamentID = r.TournamentID
                JOIN Horses h ON h.HorseID = re.HorseID
                JOIN HorseOwners ho ON ho.OwnerID = h.OwnerID
                JOIN Users u ON u.UserID = ho.UserID
                WHERE t.CreatedBy = :userId
                  AND re.RegistrationStatus = 'Pending'
                ORDER BY re.RegisteredAt DESC
                """, Map.of("userId", user.getUserId()),
                "entryId", "raceName", "horseName", "ownerName", "registeredAt", "status"));
        return data;
    }

    private Map<String, Object> buildRefereeDashboard(User user) {
        Map<String, Object> data = baseDashboard("Referee");
        data.put("metrics", List.of(
                metric("Assigned Races", count("""
                        SELECT COUNT(1)
                        FROM RaceReferees rr
                        JOIN Referees ref ON ref.RefereeID = rr.RefereeID
                        JOIN Races r ON r.RaceID = rr.RaceID
                        WHERE ref.UserID = :userId AND r.Status <> 'Draft'
                        """, Map.of("userId", user.getUserId())), "Visible to you"),
                metric("Registration Open", countAssignedRefereeRaces(user.getUserId(), "RegistrationOpen"), "Pre-race check"),
                metric("Ongoing", countAssignedRefereeRaces(user.getUserId(), "Ongoing"), "Race control"),
                metric("Finished", countAssignedRefereeRaces(user.getUserId(), "Finished"), "Minutes and results")
        ));
        data.put("assignedRaces", rows("""
                SELECT TOP 6 r.RaceID, r.RaceName, r.RaceDate, r.TrackLength, r.Status, rr.[Role]
                FROM Races r
                JOIN RaceReferees rr ON rr.RaceID = r.RaceID
                JOIN Referees ref ON ref.RefereeID = rr.RefereeID
                WHERE ref.UserID = :userId AND r.Status <> 'Draft'
                ORDER BY r.RaceDate ASC
                """, Map.of("userId", user.getUserId()),
                "raceId", "raceName", "raceDate", "trackLength", "status", "refereeRole"));
        data.put("preRaceChecks", rows("""
                SELECT TOP 6 prc.PreRaceCheckID, r.RaceName, h.HorseName, prc.Status, prc.Reason, prc.CheckedAt
                FROM PreRaceChecks prc
                JOIN Races r ON r.RaceID = prc.RaceID
                JOIN Horses h ON h.HorseID = prc.HorseID
                WHERE prc.RefereeID IN (SELECT RefereeID FROM Referees WHERE UserID = :userId)
                ORDER BY CASE WHEN prc.Status = 'Pending' THEN 0 ELSE 1 END, prc.CheckedAt DESC
                """, Map.of("userId", user.getUserId()),
                "checkId", "raceName", "horseName", "status", "reason", "checkedAt"));
        data.put("violationOptions", rows("""
                SELECT 'FALSE_START' AS Type, 'False Start' AS Label, 5 AS Penalty, 0 AS IsDq
                UNION ALL SELECT 'FOUL_RIDING', 'Foul Riding', 3, 0
                UNION ALL SELECT 'SHORT_COURSE', 'Short Course', 5, 0
                UNION ALL SELECT 'INTERFERENCE', 'Interference', 5, 0
                UNION ALL SELECT 'EQUIPMENT_FAULT', 'Equipment Fault', 0, 1
                """, "type", "label", "penalty", "isDq"));
        return data;
    }

    private Map<String, Object> buildOwnerDashboard(User user) {
        Integer ownerId = scalarInteger("SELECT OwnerID FROM HorseOwners WHERE UserID = :userId", Map.of("userId", user.getUserId()));
        if (ownerId == null) {
            throw new IllegalArgumentException("Current user does not have a HorseOwner profile.");
        }

        Map<String, Object> data = baseDashboard("HorseOwner");
        data.put("metrics", List.of(
                metric("My Horses", count("SELECT COUNT(1) FROM Horses WHERE OwnerID = :ownerId", Map.of("ownerId", ownerId)), "Total"),
                metric("Active", count("SELECT COUNT(1) FROM Horses WHERE OwnerID = :ownerId AND IsActive = 1 AND HealthStatus = 'Active'", Map.of("ownerId", ownerId)), "Ready"),
                metric("Injured", count("SELECT COUNT(1) FROM Horses WHERE OwnerID = :ownerId AND HealthStatus = 'Injured'", Map.of("ownerId", ownerId)), "Recovering"),
                metric("Race Registrations", count("SELECT COUNT(1) FROM RaceEntries re JOIN Horses h ON h.HorseID = re.HorseID WHERE h.OwnerID = :ownerId", Map.of("ownerId", ownerId)), "All time")
        ));
        data.put("wallet", walletSummary(user.getUserId()));
        data.put("myHorses", rows("""
                SELECT TOP 6 HorseID, HorseName, Breed, BirthYear, WeightKg, HealthStatus, IsActive
                FROM Horses
                WHERE OwnerID = :ownerId
                ORDER BY CreatedAt DESC
                """, Map.of("ownerId", ownerId),
                "horseId", "horseName", "breed", "birthYear", "weightKg", "healthStatus", "active"));
        data.put("raceRegistrations", rows("""
                SELECT TOP 6 re.EntryID, r.RaceName, r.RaceDate, h.HorseName, re.RegistrationStatus, re.RejectReason
                FROM RaceEntries re
                JOIN Races r ON r.RaceID = re.RaceID
                JOIN Horses h ON h.HorseID = re.HorseID
                WHERE h.OwnerID = :ownerId
                ORDER BY re.RegisteredAt DESC
                """, Map.of("ownerId", ownerId),
                "entryId", "raceName", "raceDate", "horseName", "status", "rejectReason"));
        data.put("jockeyInvitations", rows("""
                SELECT TOP 6 ji.InvitationID, h.HorseName, r.RaceName, u.FullName AS JockeyName, ji.DealAmount, ji.Status
                FROM JockeyInvitations ji
                JOIN RaceEntries re ON re.EntryID = ji.EntryID
                JOIN Horses h ON h.HorseID = re.HorseID
                JOIN Races r ON r.RaceID = re.RaceID
                JOIN Jockeys j ON j.JockeyID = ji.JockeyID
                JOIN Users u ON u.UserID = j.UserID
                WHERE ji.InvitedByOwner = :ownerId
                ORDER BY ji.InvitedAt DESC
                """, Map.of("ownerId", ownerId),
                "invitationId", "horseName", "raceName", "jockeyName", "dealAmount", "status"));
        return data;
    }

    private Map<String, Object> buildJockeyDashboard(User user) {
        Integer jockeyId = scalarInteger("SELECT JockeyID FROM Jockeys WHERE UserID = :userId", Map.of("userId", user.getUserId()));
        if (jockeyId == null) {
            throw new IllegalArgumentException("Current user does not have a Jockey profile.");
        }

        Map<String, Object> data = baseDashboard("Jockey");
        data.put("metrics", List.of(
                metric("Pending Invitations", count("SELECT COUNT(1) FROM JockeyInvitations WHERE JockeyID = :jockeyId AND Status = 'Pending'", Map.of("jockeyId", jockeyId)), "With offers"),
                metric("Upcoming Rides", count("""
                        SELECT COUNT(1)
                        FROM RaceEntries re
                        JOIN Races r ON r.RaceID = re.RaceID
                        WHERE re.JockeyID = :jockeyId AND re.RegistrationStatus = 'Ready' AND r.Status IN ('RegistrationOpen', 'Ongoing')
                        """, Map.of("jockeyId", jockeyId)), "Assigned"),
                metric("Earnings", money("""
                        SELECT ISNULL(SUM(DealAmount), 0)
                        FROM JockeyInvitations
                        WHERE JockeyID = :jockeyId AND Status = 'Accepted'
                        """, Map.of("jockeyId", jockeyId)), "Accepted deals"),
                metric("Win Rate", jockeyWinRate(jockeyId), "Career")
        ));
        data.put("wallet", walletSummary(user.getUserId()));
        data.put("pendingInvitations", rows("""
                SELECT TOP 6 ji.InvitationID, r.RaceName, h.HorseName, owner.FullName AS OwnerName, ji.DealAmount, ji.Message, ji.Status, ji.InvitedAt
                FROM JockeyInvitations ji
                JOIN RaceEntries re ON re.EntryID = ji.EntryID
                JOIN Races r ON r.RaceID = re.RaceID
                JOIN Horses h ON h.HorseID = re.HorseID
                JOIN HorseOwners ho ON ho.OwnerID = ji.InvitedByOwner
                JOIN Users owner ON owner.UserID = ho.UserID
                WHERE ji.JockeyID = :jockeyId
                ORDER BY CASE WHEN ji.Status = 'Pending' THEN 0 ELSE 1 END, ji.InvitedAt DESC
                """, Map.of("jockeyId", jockeyId),
                "invitationId", "raceName", "horseName", "ownerName", "dealAmount", "message", "status", "invitedAt"));
        data.put("upcomingRides", rows("""
                SELECT TOP 6 re.EntryID, r.RaceName, r.RaceDate, h.HorseName, re.RegistrationStatus, r.Status AS RaceStatus
                FROM RaceEntries re
                JOIN Races r ON r.RaceID = re.RaceID
                JOIN Horses h ON h.HorseID = re.HorseID
                WHERE re.JockeyID = :jockeyId
                ORDER BY r.RaceDate ASC
                """, Map.of("jockeyId", jockeyId),
                "entryId", "raceName", "raceDate", "horseName", "entryStatus", "raceStatus"));
        return data;
    }

    private Map<String, Object> buildSpectatorDashboard(User user) {
        Map<String, Object> data = baseDashboard("Spectator");
        data.put("metrics", List.of(
                metric("Wallet Balance", money("SELECT ISNULL(Balance, 0) FROM Wallets WHERE UserID = :userId", Map.of("userId", user.getUserId())), "Available"),
                metric("Open Races", count("SELECT COUNT(1) FROM Races WHERE Status = 'RegistrationOpen'"), "Betting available"),
                metric("Pending Bets", count("""
                        SELECT (SELECT COUNT(1) FROM Bets WHERE UserID = :userId AND Status = 'Pending')
                             + (SELECT COUNT(1) FROM BetTickets WHERE UserID = :userId AND Status = 'Pending')
                        """, Map.of("userId", user.getUserId())), "Awaiting results"),
                metric("Won Bets", count("""
                        SELECT (SELECT COUNT(1) FROM Bets WHERE UserID = :userId AND Status = 'Won')
                             + (SELECT COUNT(1) FROM BetTickets WHERE UserID = :userId AND Status = 'Won')
                        """, Map.of("userId", user.getUserId())), "All time")
        ));
        data.put("wallet", walletSummary(user.getUserId()));
        data.put("featuredRaces", raceRepository.findFeaturedDashboardRaces().stream().map(this::toRaceSummary).toList());
        data.put("recentBets", rows("""
                SELECT TOP 6 BetID AS Id, 'Single' AS Kind, BetType, RaceID, EntryID, Amount, Odds, PotentialPayout, Status, CreatedAt
                FROM Bets
                WHERE UserID = :userId
                UNION ALL
                SELECT TOP 6 TicketID AS Id, 'Parlay' AS Kind, 'PARLAY' AS BetType, RaceID, NULL AS EntryID, Amount, Odds, PotentialPayout, Status, CreatedAt
                FROM BetTickets
                WHERE UserID = :userId
                ORDER BY CreatedAt DESC
                """, Map.of("userId", user.getUserId()),
                "id", "kind", "betType", "raceId", "entryId", "amount", "odds", "potentialPayout", "status", "createdAt"));
        data.put("leaderboardPreview", Map.of(
                "jockeys", leaderboardService.getGlobalJockeyLeaderboard().stream().limit(5).toList(),
                "horses", leaderboardService.getGlobalHorseLeaderboard().stream().limit(5).toList()
        ));
        return data;
    }

    private Integer countAssignedRefereeRaces(Integer userId, String status) {
        return count("""
                SELECT COUNT(1)
                FROM RaceReferees rr
                JOIN Referees ref ON ref.RefereeID = rr.RefereeID
                JOIN Races r ON r.RaceID = rr.RaceID
                WHERE ref.UserID = :userId AND r.Status = :status
                """, Map.of("userId", userId, "status", status));
    }

    private Integer jockeyWinRate(Integer jockeyId) {
        Object[] row = singleRow("""
                SELECT ISNULL(TotalRaces, 0), ISNULL(TotalWins, 0)
                FROM Jockeys
                WHERE JockeyID = :jockeyId
                """, Map.of("jockeyId", jockeyId));
        if (row == null || toInteger(row[0]) == 0) {
            return 0;
        }
        return (int) Math.round(toInteger(row[1]) * 100.0 / toInteger(row[0]));
    }

    private Map<String, Object> walletSummary(Integer userId) {
        return Map.of(
                "balance", money("SELECT ISNULL(Balance, 0) FROM Wallets WHERE UserID = :userId", Map.of("userId", userId)),
                "recentTransactions", rows("""
                        SELECT TOP 5 wt.TransactionID, wt.TransactionType, wt.Amount, wt.Description, wt.CreatedAt
                        FROM WalletTransactions wt
                        JOIN Wallets w ON w.WalletID = wt.WalletID
                        WHERE w.UserID = :userId
                        ORDER BY wt.CreatedAt DESC
                        """, Map.of("userId", userId),
                        "transactionId", "transactionType", "amount", "description", "createdAt")
        );
    }

    private Map<String, Object> baseDashboard(String role) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", role);
        data.put("generatedAt", LocalDateTime.now());
        return data;
    }

    private Map<String, Object> metric(String label, Object value, String helper) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("label", label);
        metric.put("value", value);
        metric.put("helper", helper);
        return metric;
    }

    private Map<String, Object> statusItem(String label, String status) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("status", status);
        return item;
    }

    private User requireRole(HttpServletRequest request, String expectedRole) {
        User user = currentUserService.getCurrentUser(request);
        if (!expectedRole.equals(roleName(user))) {
            throw new IllegalArgumentException("You do not have permission to view this dashboard.");
        }
        return user;
    }

    private String roleName(User user) {
        if (user == null || user.getRole() == null || user.getRole().getRoleName() == null) {
            return "";
        }
        return user.getRole().getRoleName();
    }

    private Integer count(String sql) {
        return count(sql, Map.of());
    }

    private Integer count(String sql, Map<String, Object> params) {
        return toInteger(singleScalar(sql, params));
    }

    private BigDecimal money(String sql, Map<String, Object> params) {
        Object value = singleScalar(sql, params);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal amount) {
            return amount;
        }
        return BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private Integer scalarInteger(String sql, Map<String, Object> params) {
        Object value = singleScalar(sql, params);
        return value == null ? null : toInteger(value);
    }

    private Object singleScalar(String sql, Map<String, Object> params) {
        List<?> results = createQuery(sql, params).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private Object[] singleRow(String sql, Map<String, Object> params) {
        List<?> results = createQuery(sql, params).getResultList();
        return results.isEmpty() ? null : (Object[]) results.get(0);
    }

    private List<Map<String, Object>> rows(String sql, String... keys) {
        return rows(sql, Map.of(), keys);
    }

    private List<Map<String, Object>> rows(String sql, Map<String, Object> params, String... keys) {
        return createQuery(sql, params).getResultList()
                .stream()
                .map(row -> rowToMap(row, keys))
                .toList();
    }

    private Map<String, Object> rowToMap(Object row, String... keys) {
        Object[] values = row instanceof Object[] array ? array : new Object[] { row };
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], i < values.length ? normalizeValue(values[i]) : null);
        }
        return map;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return value;
    }

    private Query createQuery(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }

    private Integer toInteger(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private RaceSummaryResponse toRaceSummary(Race race) {
        return new RaceSummaryResponse(
                race.getRaceId(),
                race.getTournamentId(),
                race.getRoundId(),
                race.getRaceName(),
                race.getRaceDate(),
                race.getTrackLength(),
                race.getTrackType(),
                race.getMaxParticipants(),
                race.getPrizeFirst(),
                race.getPrizeSecond(),
                race.getPrizeThird(),
                race.getStatus(),
                race.getRegistrationOpen(),
                race.getRegistrationClose()
        );
    }
}
