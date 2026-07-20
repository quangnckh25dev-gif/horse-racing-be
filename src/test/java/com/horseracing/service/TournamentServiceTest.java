package com.horseracing.service;

import com.horseracing.dto.TournamentRequest;
import com.horseracing.entity.Role;
import com.horseracing.entity.Tournament;
import com.horseracing.entity.User;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RoundRepository;
import com.horseracing.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private RoundRepository roundRepository;
    @Mock
    private RaceRepository raceRepository;

    private TournamentService service;

    @BeforeEach
    void setUp() {
        service = new TournamentService(tournamentRepository, roundRepository, raceRepository);
    }

    @Test
    void createTournamentAlwaysUsesJwtOrganizerAndDraftStatus() {
        User organizer = user(7, "Organizer");
        saveReturnsArgument();

        var response = service.createTournament(validRequest(), organizer);

        assertEquals("Draft", response.status());
        assertEquals(7, response.createdBy());
        assertEquals(new BigDecimal("50000000"), response.budgetTotal());
    }

    @Test
    void updateTournamentRejectsTournamentOwnedByAnotherOrganizer() {
        User organizer = user(7, "Organizer");
        when(tournamentRepository.findByTournamentIdAndCreatedBy(2, 7)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.updateTournament(2, validRequest(), organizer));

        assertTrue(exception.getMessage().contains("current organizer"));
    }

    @Test
    void submitTournamentMovesDraftToPendingApproval() {
        User organizer = user(7, "Organizer");
        saveReturnsArgument();
        Tournament tournament = draftTournament(7);
        when(tournamentRepository.findByTournamentIdAndCreatedBy(1, 7))
                .thenReturn(Optional.of(tournament));

        var response = service.submitTournament(1, organizer);

        assertEquals("PendingApproval", response.status());
    }

    @Test
    void adminCanApprovePendingTournament() {
        User admin = user(1, "Admin");
        saveReturnsArgument();
        Tournament tournament = draftTournament(7);
        tournament.setStatus("PendingApproval");
        when(tournamentRepository.findById(1)).thenReturn(Optional.of(tournament));

        var response = service.reviewTournament(1, "Open", null, admin);

        assertEquals("Open", response.status());
        assertEquals(1, response.approvedByAdmin());
    }

    @Test
    void adminMustProvideReasonWhenRejectingTournament() {
        User admin = user(1, "Admin");
        Tournament tournament = draftTournament(7);
        tournament.setStatus("PendingApproval");
        when(tournamentRepository.findById(1)).thenReturn(Optional.of(tournament));

        assertThrows(IllegalArgumentException.class,
                () -> service.reviewTournament(1, "Draft", " ", admin));
    }

    private TournamentRequest validRequest() {
        TournamentRequest request = new TournamentRequest();
        request.setTournamentName("Giai dua mua he");
        request.setStartDate(LocalDate.of(2026, 8, 1));
        request.setEndDate(LocalDate.of(2026, 8, 10));
        request.setBudgetTotal(new BigDecimal("50000000"));
        request.setMaxHorses(20);
        request.setMaxParticipants(20);
        return request;
    }

    private void saveReturnsArgument() {
        when(tournamentRepository.save(any(Tournament.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Tournament draftTournament(Integer createdBy) {
        Tournament tournament = new Tournament();
        tournament.setTournamentName("Giai dua mua he");
        tournament.setStartDate(LocalDate.of(2026, 8, 1));
        tournament.setEndDate(LocalDate.of(2026, 8, 10));
        tournament.setCreatedBy(createdBy);
        tournament.setStatus("Draft");
        return tournament;
    }

    private User user(Integer id, String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        User user = new User();
        user.setUserId(id);
        user.setRole(role);
        user.setIsActive(true);
        user.setIsApproved(true);
        return user;
    }
}
