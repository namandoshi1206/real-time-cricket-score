package com.cricket;

import com.cricket.dto.DeliveryRequest;
import com.cricket.dto.DeliveryResponse;
import com.cricket.entity.Delivery;
import com.cricket.entity.ExtraType;
import com.cricket.entity.Innings;
import com.cricket.entity.InningsStatus;
import com.cricket.entity.Match;
import com.cricket.entity.MatchType;
import com.cricket.entity.Player;
import com.cricket.entity.Team;
import com.cricket.entity.WicketType;
import com.cricket.exception.ResourceNotFoundException;
import com.cricket.repository.DeliveryRepository;
import com.cricket.repository.InningsRepository;
import com.cricket.repository.PlayerRepository;
import com.cricket.repository.TeamPlayerRepository;
import com.cricket.service.DeliveryService;
import com.cricket.service.InningsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringEngineTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private InningsRepository inningsRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamPlayerRepository teamPlayerRepository;

    @Mock
    private InningsService inningsService;

    private DeliveryService deliveryService;
    private Innings innings;
    private Player batsman;
    private Player nonStriker;
    private Player bowler;

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryService(deliveryRepository, inningsRepository, playerRepository,
                teamPlayerRepository, inningsService);
        Team battingTeam = team(1L, "Batting Team");
        Team bowlingTeam = team(2L, "Bowling Team");
        Match match = new Match();
        match.setId(10L);
        match.setMatchType(MatchType.T20);
        match.setTeamA(battingTeam);
        match.setTeamB(bowlingTeam);
        innings = new Innings();
        innings.setId(100L);
        innings.setMatch(match);
        innings.setBattingTeam(battingTeam);
        innings.setBowlingTeam(bowlingTeam);
        innings.setStatus(InningsStatus.LIVE);
        innings.setTotalRuns(0);
        innings.setWickets(0);
        innings.setLegalBalls(0);
        batsman = player(11L);
        nonStriker = player(12L);
        bowler = player(13L);
        lenient().when(inningsService.getEntity(100L)).thenReturn(innings);
        lenient().when(playerRepository.findById(11L)).thenReturn(Optional.of(batsman));
        lenient().when(playerRepository.findById(12L)).thenReturn(Optional.of(nonStriker));
        lenient().when(playerRepository.findById(13L)).thenReturn(Optional.of(bowler));
        lenient().when(teamPlayerRepository.existsByTeamIdAndPlayerId(anyLong(), anyLong())).thenReturn(true);
        lenient().when(deliveryRepository.existsByInningsIdAndDismissedBatsmanId(anyLong(), anyLong())).thenReturn(false);
        lenient().when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> {
            Delivery delivery = invocation.getArgument(0);
            delivery.setId(1L);
            return delivery;
        });
    }

    @Test
    void recordsDotBall() {
        DeliveryResponse response = record(0, ExtraType.NONE, 0, null);

        assertEquals(0, response.totalRuns());
        assertEquals(0, innings.getTotalRuns());
        assertEquals(1, innings.getLegalBalls());
    }

    @Test
    void recordsOneTwoThreeFourAndSixRuns() {
        for (int runs : new int[]{1, 2, 3, 4, 6}) {
            innings.setLegalBalls(0);
            innings.setTotalRuns(0);
            DeliveryResponse response = record(runs, ExtraType.NONE, 0, null);
            assertEquals(runs, response.totalRuns());
            assertEquals(runs, innings.getTotalRuns());
        }
    }

    @Test
    void recordsWideWithoutLegalBall() {
        DeliveryResponse response = record(0, ExtraType.WIDE, 1, null);

        assertEquals(1, response.totalRuns());
        assertFalse(response.legalDelivery());
        assertEquals(0, innings.getLegalBalls());
    }

    @Test
    void recordsNoBallWithoutLegalBall() {
        DeliveryResponse response = record(4, ExtraType.NO_BALL, 1, null);

        assertEquals(5, response.totalRuns());
        assertFalse(response.legalDelivery());
        assertEquals(0, innings.getLegalBalls());
    }

    @Test
    void recordsByeAndLegByeAsLegalDeliveries() {
        DeliveryResponse bye = record(0, ExtraType.BYE, 2, null);
        innings.setLegalBalls(0);
        innings.setTotalRuns(0);
        DeliveryResponse legBye = record(0, ExtraType.LEG_BYE, 3, null);

        assertTrue(bye.legalDelivery());
        assertTrue(legBye.legalDelivery());
        assertEquals(3, legBye.totalRuns());
        assertEquals(1, innings.getLegalBalls());
    }

    @Test
    void recordsWicketAndCountsDismissedBatsman() {
        DeliveryResponse response = record(0, ExtraType.NONE, 0, WicketType.BOWLED);

        assertEquals(WicketType.BOWLED, response.wicketType());
        assertEquals(11L, response.dismissedBatsmanId());
        assertEquals(1, innings.getWickets());
    }

    @Test
    void countsLegalBallsAndFormatsOvers() {
        for (int ball = 1; ball <= 6; ball++) {
            DeliveryRequest request = request(0, ExtraType.NONE, 0, null);
            request.setBallNumber(ball);
            deliveryService.record(100L, request);
        }

        assertEquals(6, innings.getLegalBalls());
        assertEquals("1.0", new InningsService(null, null, null).formatOvers(6));
    }

    @Test
    void completesInningsWhenTargetIsReached() {
        innings.setTargetRuns(5);
        record(4, ExtraType.NONE, 0, null);
        DeliveryRequest request = request(1, ExtraType.NONE, 0, null);
        request.setBallNumber(2);
        deliveryService.record(100L, request);

        assertEquals(InningsStatus.COMPLETED, innings.getStatus());
    }

    @Test
    void rejectsUnknownPlayer() {
        when(playerRepository.findById(11L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> record(0, ExtraType.NONE, 0, null));
    }

    @Test
    void rejectsPlayerFromWrongTeam() {
        when(teamPlayerRepository.existsByTeamIdAndPlayerId(1L, 11L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> record(0, ExtraType.NONE, 0, null));
    }

    @Test
    void rejectsInvalidDeliverySequence() {
        DeliveryRequest request = request(0, ExtraType.NONE, 0, null);
        request.setBallNumber(2);

        assertThrows(IllegalArgumentException.class, () -> deliveryService.record(100L, request));
    }

    @Test
    void rejectsDismissedBatsman() {
        when(deliveryRepository.existsByInningsIdAndDismissedBatsmanId(100L, 11L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> record(0, ExtraType.NONE, 0, null));
    }

    @Test
    void rollsBackStateWhenDeliverySaveFails() {
        doThrow(new RuntimeException("database failure")).when(deliveryRepository).save(any(Delivery.class));

        assertThrows(RuntimeException.class, () -> record(1, ExtraType.NONE, 0, null));
        assertEquals(0, innings.getTotalRuns());
        assertEquals(0, innings.getLegalBalls());
        verify(inningsRepository, never()).save(any(Innings.class));
    }

    private DeliveryResponse record(int runs, ExtraType extraType, int extraRuns, WicketType wicketType) {
        return deliveryService.record(100L, request(runs, extraType, extraRuns, wicketType));
    }

    private DeliveryRequest request(int runs, ExtraType extraType, int extraRuns, WicketType wicketType) {
        DeliveryRequest request = new DeliveryRequest();
        request.setOverNumber(1);
        request.setBallNumber(1);
        request.setBatsmanId(11L);
        request.setNonStrikerId(12L);
        request.setBowlerId(13L);
        request.setRuns(runs);
        request.setExtraType(extraType);
        request.setExtraRuns(extraRuns);
        request.setWicketType(wicketType);
        return request;
    }

    private Player player(Long id) {
        Player player = new Player();
        player.setId(id);
        return player;
    }

    private Team team(Long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        return team;
    }
}