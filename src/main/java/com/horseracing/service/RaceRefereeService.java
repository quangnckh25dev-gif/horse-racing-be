package com.horseracing.service;

import com.horseracing.dto.RaceRefereeRequest;
import com.horseracing.dto.RaceRefereeResponse;
import com.horseracing.dto.RefereeResponse;
import com.horseracing.entity.Notification;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceReferee;
import com.horseracing.entity.Referee;
import com.horseracing.entity.User;
import com.horseracing.repository.NotificationRepository;
import com.horseracing.repository.RaceRefereeRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RefereeRepository;
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

    public RaceRefereeService(RaceRepository raceRepository,
                              RefereeRepository refereeRepository,
                              RaceRefereeRepository raceRefereeRepository,
                              NotificationRepository notificationRepository,
                              UserRepository userRepository) {
        this.raceRepository = raceRepository;
        this.refereeRepository = refereeRepository;
        this.raceRefereeRepository = raceRefereeRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<RefereeResponse> getReferees() {
        Map<Integer, User> usersById = userRepository.findByRole_RoleNameAndIsActiveTrue("Referee")
                .stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        return refereeRepository.findAll()
                .stream()
                .filter(referee -> usersById.containsKey(referee.getUserId()))
                .map(referee -> toRefereeResponse(referee, usersById.get(referee.getUserId())))
                .toList();
    }

    public RaceRefereeResponse assignReferee(Integer raceId, RaceRefereeRequest request) {
        Race race = getRaceOrThrow(raceId);
        Referee referee = getRefereeOrThrow(request);

        if (raceRefereeRepository.existsByRaceIdAndRefereeId(raceId, referee.getRefereeId())) {
            throw new IllegalArgumentException("Referee da duoc phan cong vao race nay");
        }

        RaceReferee raceReferee = new RaceReferee();
        raceReferee.setRaceId(race.getRaceId());
        raceReferee.setRefereeId(referee.getRefereeId());
        raceReferee.setRole(resolveRole(request.getRole()));

        RaceReferee saved = raceRefereeRepository.save(raceReferee);
        createAssignmentNotification(referee, race);

        return toRaceRefereeResponse(saved);
    }

    @Transactional
    public void removeReferee(Integer raceId, Integer refereeId) {
        getRaceOrThrow(raceId);
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
        return role == null || role.isBlank() ? "Assistant" : role;
    }

    private void createAssignmentNotification(Referee referee, Race race) {
        Notification notification = new Notification();
        notification.setUserId(referee.getUserId());
        notification.setTitle("Ban duoc phan cong trong tai");
        notification.setBody("Ban duoc phan cong vao cuoc dua: " + race.getRaceName());
        notification.setNotifType("RaceReminder");
        notification.setRelatedEntityId(race.getRaceId());
        notification.setRelatedEntity("Race");
        notificationRepository.save(notification);
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
