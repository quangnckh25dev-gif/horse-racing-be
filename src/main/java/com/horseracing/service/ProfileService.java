package com.horseracing.service;

import com.horseracing.dto.ProfileRequest;
import com.horseracing.dto.ProfileResponse;
import com.horseracing.entity.HorseOwner;
import com.horseracing.entity.Jockey;
import com.horseracing.entity.Referee;
import com.horseracing.entity.User;
import com.horseracing.repository.HorseOwnerRepository;
import com.horseracing.repository.JockeyRepository;
import com.horseracing.repository.RefereeRepository;
import com.horseracing.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final HorseOwnerRepository horseOwnerRepository;
    private final JockeyRepository jockeyRepository;
    private final RefereeRepository refereeRepository;
    private final UserRepository userRepository;

    public ProfileService(HorseOwnerRepository horseOwnerRepository, JockeyRepository jockeyRepository,
                          RefereeRepository refereeRepository, UserRepository userRepository) {
        this.horseOwnerRepository = horseOwnerRepository;
        this.jockeyRepository = jockeyRepository;
        this.refereeRepository = refereeRepository;
        this.userRepository = userRepository;
    }

    public ProfileResponse getOwnerProfile(Integer userId) {
        HorseOwner owner = horseOwnerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Owner profile was not found."));
        return toOwnerResponse(owner, getUser(userId));
    }

    public ProfileResponse updateOwnerProfile(Integer userId, ProfileRequest request) {
        HorseOwner owner = horseOwnerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Owner profile was not found."));

        if (request.getNationalId() != null) owner.setNationalId(request.getNationalId());
        if (request.getAddress() != null) owner.setAddress(request.getAddress());
        if (request.getOrganization() != null) owner.setOrganization(request.getOrganization());
        if (request.getLicenseNumber() != null) owner.setLicenseNumber(request.getLicenseNumber());

        return toOwnerResponse(horseOwnerRepository.save(owner), getUser(userId));
    }

    public ProfileResponse getJockeyProfile(Integer userId) {
        Jockey jockey = jockeyRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Jockey profile was not found."));
        return toJockeyResponse(jockey, getUser(userId));
    }

    public ProfileResponse updateJockeyProfile(Integer userId, ProfileRequest request) {
        Jockey jockey = jockeyRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Jockey profile was not found."));

        if (request.getNationalId() != null) jockey.setNationalId(request.getNationalId());
        if (request.getLicenseNumber() != null) jockey.setLicenseNumber(request.getLicenseNumber());
        if (request.getWeightKg() != null) jockey.setWeightKg(request.getWeightKg());
        if (request.getHeightCm() != null) jockey.setHeightCm(request.getHeightCm());
        if (request.getExperienceYear() != null) jockey.setExperienceYear(request.getExperienceYear());

        return toJockeyResponse(jockeyRepository.save(jockey), getUser(userId));
    }

    public ProfileResponse getRefereeProfile(Integer userId) {
        Referee referee = refereeRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Referee profile was not found."));
        return toRefereeResponse(referee, getUser(userId));
    }

    public ProfileResponse updateRefereeProfile(Integer userId, ProfileRequest request) {
        Referee referee = refereeRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Referee profile was not found."));

        if (request.getBadgeNumber() != null) referee.setBadgeNumber(request.getBadgeNumber());
        if (request.getSpeciality() != null) referee.setSpeciality(request.getSpeciality());

        return toRefereeResponse(refereeRepository.save(referee), getUser(userId));
    }

    private User getUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found."));
    }

    private ProfileResponse toOwnerResponse(HorseOwner owner, User user) {
        return new ProfileResponse("HorseOwner", owner.getOwnerId(), owner.getUserId(), user.getUsername(),
                user.getFullName(), owner.getNationalId(), owner.getAddress(), owner.getOrganization(),
                owner.getLicenseNumber(), null, null, null, null, null, null, null, owner.getCreatedAt());
    }

    private ProfileResponse toJockeyResponse(Jockey jockey, User user) {
        return new ProfileResponse("Jockey", jockey.getJockeyId(), jockey.getUserId(), user.getUsername(),
                user.getFullName(), jockey.getNationalId(), null, null, jockey.getLicenseNumber(),
                jockey.getWeightKg(), jockey.getHeightCm(), jockey.getExperienceYear(), jockey.getTotalRaces(),
                jockey.getTotalWins(), null, null, jockey.getCreatedAt());
    }

    private ProfileResponse toRefereeResponse(Referee referee, User user) {
        return new ProfileResponse("Referee", referee.getRefereeId(), referee.getUserId(), user.getUsername(),
                user.getFullName(), null, null, null, null, null, null, null, null, null,
                referee.getBadgeNumber(), referee.getSpeciality(), referee.getCreatedAt());
    }
}
