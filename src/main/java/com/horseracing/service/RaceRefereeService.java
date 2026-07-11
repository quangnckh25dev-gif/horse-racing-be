package com.horseracing.service;

import com.horseracing.dto.RaceRefereeRequest;
import com.horseracing.dto.RaceRefereeResponse;
import com.horseracing.dto.RefereeResponse;
import com.horseracing.entity.Notification;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceReferee;
import com.horseracing.entity.Referee;
import com.horseracing.entity.Tournament;
import com.horseracing.entity.User;
import com.horseracing.repository.NotificationRepository;
import com.horseracing.repository.RaceRefereeRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RefereeRepository;
import com.horseracing.repository.TournamentRepository;
import com.horseracing.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RaceRefereeService {

    private final RaceRepository raceRepository;
    private final RefereeRepository refereeRepository;
    private final RaceRefereeRepository raceRefereeRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TournamentRepository tournamentRepository;

    public RaceRefereeService(RaceRepository raceRepository,
                              RefereeRepository refereeRepository,
                              RaceRefereeRepository raceRefereeRepository,
                              NotificationRepository notificationRepository,
                              UserRepository userRepository,
                              TournamentRepository tournamentRepository) {
        this.raceRepository = raceRepository;
        this.refereeRepository = refereeRepository;
        this.raceRefereeRepository = raceRefereeRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.tournamentRepository = tournamentRepository;
    }

    public List<RefereeResponse> getReferees(User organizer) {
        requireOrganizer(organizer);
        Map<Integer, User> usersById = userRepository.findByRole_RoleNameAndIsActiveTrue("Referee")
                .stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsApproved()))
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        return refereeRepository.findAll()
                .stream()
                .filter(referee -> usersById.containsKey(referee.getUserId()))
                .map(referee -> toRefereeResponse(referee, usersById.get(referee.getUserId())))
                .toList();
    }

    public List<RaceRefereeResponse> getRaceReferees(Integer raceId, User organizer) {
        Race race = getRaceOrThrow(raceId);
        ensureOwnedRace(race, organizer);
        return raceRefereeRepository.findByRaceId(raceId)
                .stream()
                .map(this::toRaceRefereeResponse)
                .toList();
    }

    @Transactional
    //của buiquangann
    public RaceRefereeResponse assignReferee(Integer raceId, RaceRefereeRequest request, User organizer) {
        Race race = getRaceOrThrow(raceId);
        ensureOwnedRace(race, organizer);
        Referee referee = getRefereeOrThrow(request);
        User refereeUser = userRepository.findById(referee.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user cua referee"));
        if (!isApprovedActiveRole(refereeUser, "Referee")) {
            throw new IllegalArgumentException("Referee chua active hoac chua duoc duyet");
        }
        String role = resolveRole(request.getRole());

        if (raceRefereeRepository.existsByRaceIdAndRefereeId(raceId, referee.getRefereeId())) {
            throw new IllegalArgumentException("Referee da duoc phan cong vao race nay");
        }
        if ("Chief".equalsIgnoreCase(role) && raceRefereeRepository.existsByRaceIdAndRoleIgnoreCase(raceId, "Chief")) {
            throw new IllegalArgumentException("Race da co Chief referee");
        }

        RaceReferee raceReferee = new RaceReferee();
        raceReferee.setRaceId(race.getRaceId());
        raceReferee.setRefereeId(referee.getRefereeId());
        raceReferee.setRole(role);

        RaceReferee saved = raceRefereeRepository.save(raceReferee);
        createAssignmentNotification(referee, race);

        return toRaceRefereeResponse(saved);
    }

    @Transactional
    public void removeReferee(Integer raceId, Integer refereeId, User organizer) {
        Race race = getRaceOrThrow(raceId);
        ensureOwnedRace(race, organizer);
        getRefereeByIdOrThrow(refereeId);

        if (!raceRefereeRepository.existsByRaceIdAndRefereeId(raceId, refereeId)) {
            throw new IllegalArgumentException("Referee chua duoc phan cong vao race nay");
        }

        raceRefereeRepository.deleteByRaceIdAndRefereeId(raceId, refereeId);
    }

    private Race getRaceOrThrow(Integer raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("Race id khong hop le");
        }

        return raceRepository.findById(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay race"));
    }

    private Referee getRefereeOrThrow(RaceRefereeRequest request) {
        if (request == null || request.getRefereeId() == null) {
            throw new IllegalArgumentException("RefereeID khong duoc de trong");
        }
        return getRefereeByIdOrThrow(request.getRefereeId());
    }

    private Referee getRefereeByIdOrThrow(Integer refereeId) {
        return refereeRepository.findById(refereeId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay referee"));
    }

    private String resolveRole(String role) {
        if (role == null || role.isBlank()) {
            return "Assistant";
        }
        String normalized = role.trim();
        if ("Chief".equalsIgnoreCase(normalized)) {
            return "Chief";
        }
        if ("Assistant".equalsIgnoreCase(normalized)) {
            return "Assistant";
        }
        throw new IllegalArgumentException("Role referee chi chap nhan Chief hoac Assistant");
    }

    private void createAssignmentNotification(Referee referee, Race race) {
        Notification notification = new Notification();
        notification.setUserId(referee.getUserId());
        notification.setTitle("Ban duoc phan cong trong tai");
        notification.setBody("Ban duoc phan cong vao cuoc dua: " + race.getRaceName());
        notification.setNotifType("RefereeAssigned");
        notification.setRelatedEntityId(race.getRaceId());
        notification.setRelatedEntity("Race");
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    private void ensureOwnedRace(Race race, User organizer) {
        requireOrganizer(organizer);
        Tournament tournament = tournamentRepository.findByTournamentIdAndCreatedBy(
                        race.getTournamentId(), organizer.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Race khong thuoc Organizer hien tai"));
        if (!Boolean.TRUE.equals(organizer.getIsActive()) || !Boolean.TRUE.equals(organizer.getIsApproved())) {
            throw new IllegalArgumentException("Tai khoan Organizer chua duoc kich hoat hoac phe duyet");
        }
        if (tournament.getTournamentId() == null) {
            throw new IllegalArgumentException("Tournament khong hop le");
        }
    }

    private void requireOrganizer(User user) {
        if (!isApprovedActiveRole(user, "Organizer")) {
            throw new IllegalArgumentException("Chi Organizer moi co quyen phan cong referee");
        }
    }

    private boolean isApprovedActiveRole(User user, String roleName) {
        return user != null
                && user.getRole() != null
                && roleName.equalsIgnoreCase(user.getRole().getRoleName())
                && Boolean.TRUE.equals(user.getIsActive())
                && Boolean.TRUE.equals(user.getIsApproved());
    }

    private RefereeResponse toRefereeResponse(Referee referee, User user) {
        return new RefereeResponse(
                referee.getRefereeId(),
                referee.getUserId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                referee.getBadgeNumber(),
                referee.getSpeciality()
        );
    }

    private RaceRefereeResponse toRaceRefereeResponse(RaceReferee raceReferee) {
        return new RaceRefereeResponse(
                raceReferee.getRaceRefereeId(),
                raceReferee.getRaceId(),
                raceReferee.getRefereeId(),
                raceReferee.getRole(),
                raceReferee.getAssignedAt()
        );
    }
}
