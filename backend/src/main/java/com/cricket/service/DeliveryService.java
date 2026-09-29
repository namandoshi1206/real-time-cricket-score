package com.cricket.service;

import com.cricket.dto.DeliveryRequest;
import com.cricket.dto.DeliveryResponse;
import com.cricket.entity.Delivery;
import com.cricket.entity.ExtraType;
import com.cricket.entity.Innings;
import com.cricket.entity.InningsStatus;
import com.cricket.entity.MatchType;
import com.cricket.entity.Player;
import com.cricket.entity.WicketType;
import com.cricket.exception.ResourceNotFoundException;
import com.cricket.repository.DeliveryRepository;
import com.cricket.repository.InningsRepository;
import com.cricket.repository.PlayerRepository;
import com.cricket.repository.TeamPlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final InningsRepository inningsRepository;
    private final PlayerRepository playerRepository;
    private final TeamPlayerRepository teamPlayerRepository;
    private final InningsService inningsService;

    public DeliveryService(DeliveryRepository deliveryRepository, InningsRepository inningsRepository,
                           PlayerRepository playerRepository, TeamPlayerRepository teamPlayerRepository,
                           InningsService inningsService) {
        this.deliveryRepository = deliveryRepository;
        this.inningsRepository = inningsRepository;
        this.playerRepository = playerRepository;
        this.teamPlayerRepository = teamPlayerRepository;
        this.inningsService = inningsService;
    }

    @Transactional
    public DeliveryResponse record(Long inningsId, DeliveryRequest request) {
        Innings innings = inningsService.getEntity(inningsId);
        if (innings.getStatus() != InningsStatus.LIVE) {
            throw new IllegalArgumentException("Innings is not active");
        }
        Player batsman = getPlayer(request.getBatsmanId());
        Player nonStriker = getPlayer(request.getNonStrikerId());
        Player bowler = getPlayer(request.getBowlerId());
        validatePlayers(innings, batsman, nonStriker, bowler);
        validateSequence(innings, request);
        validateRuns(request);
        boolean legal = request.getExtraType() != ExtraType.WIDE
                && request.getExtraType() != ExtraType.NO_BALL;
        boolean wicket = request.getWicketType() != null && request.getWicketType() != WicketType.NO_WICKET;
        if (deliveryRepository.existsByInningsIdAndDismissedBatsmanId(inningsId, batsman.getId())) {
            throw new IllegalArgumentException("Dismissed batsman cannot receive another delivery");
        }

        Delivery delivery = new Delivery();
        delivery.setInnings(innings);
        delivery.setOverNumber(request.getOverNumber());
        delivery.setBallNumber(request.getBallNumber());
        delivery.setBatsman(batsman);
        delivery.setNonStriker(nonStriker);
        delivery.setBowler(bowler);
        delivery.setRuns(request.getRuns());
        delivery.setExtraType(request.getExtraType());
        delivery.setExtraRuns(request.getExtraRuns());
        delivery.setTotalRuns(request.getRuns() + request.getExtraRuns());
        delivery.setLegalDelivery(legal);
        delivery.setWicketType(wicket ? request.getWicketType() : null);
        delivery.setDismissedBatsman(wicket ? batsman : null);

        Delivery saved = deliveryRepository.save(delivery);
        innings.setTotalRuns(innings.getTotalRuns() + saved.getTotalRuns());
        if (legal) {
            innings.setLegalBalls(innings.getLegalBalls() + 1);
        }
        if (wicket) {
            innings.setWickets(innings.getWickets() + 1);
        }
        innings.setStriker(batsman);
        innings.setNonStriker(nonStriker);
        innings.setCurrentBowler(bowler);
        updateCompletion(innings);
        inningsRepository.save(innings);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> findByInningsId(Long inningsId) {
        inningsService.getEntity(inningsId);
        return deliveryRepository.findByInningsIdOrderById(inningsId).stream()
                .map(this::toResponse).toList();
    }

    private void validatePlayers(Innings innings, Player batsman, Player nonStriker, Player bowler) {
        if (batsman.getId().equals(nonStriker.getId())) {
            throw new IllegalArgumentException("Batsman and non-striker must be different");
        }
        if (!teamPlayerRepository.existsByTeamIdAndPlayerId(innings.getBattingTeam().getId(), batsman.getId())
                || !teamPlayerRepository.existsByTeamIdAndPlayerId(innings.getBattingTeam().getId(), nonStriker.getId())) {
            throw new IllegalArgumentException("Batsmen must belong to the batting team");
        }
        if (!teamPlayerRepository.existsByTeamIdAndPlayerId(innings.getBowlingTeam().getId(), bowler.getId())) {
            throw new IllegalArgumentException("Bowler must belong to the bowling team");
        }
    }

    private void validateSequence(Innings innings, DeliveryRequest request) {
        int expectedOver = innings.getLegalBalls() / 6 + 1;
        int expectedBall = innings.getLegalBalls() % 6 + 1;
        if (request.getOverNumber() != expectedOver || request.getBallNumber() != expectedBall) {
            throw new IllegalArgumentException("Invalid delivery sequence; expected " + expectedOver + "." + expectedBall);
        }
    }

    private void validateRuns(DeliveryRequest request) {
        ExtraType extraType = request.getExtraType();
        int runs = request.getRuns();
        int extraRuns = request.getExtraRuns();
        if (runs < 0 || runs > 6 || extraRuns < 0) {
            throw new IllegalArgumentException("Runs must be between 0 and 6 and extras cannot be negative");
        }
        if (extraType == null) {
            throw new IllegalArgumentException("Extra type is required");
        }
        switch (extraType) {
            case NONE -> require(extraRuns == 0, "NONE cannot have extra runs");
            case WIDE -> require(runs == 0 && extraRuns >= 1, "WIDE requires at least one extra run and no batsman runs");
            case NO_BALL -> require(extraRuns >= 1, "NO_BALL requires at least one extra run");
            case BYE, LEG_BYE -> require(runs == 0 && extraRuns >= 1, "Bye extras require no batsman runs");
            case PENALTY -> require(runs == 0 && extraRuns >= 1, "PENALTY requires at least one extra run");
        }
    }

    private void updateCompletion(Innings innings) {
        MatchType type = innings.getMatch().getMatchType();
        Integer maxBalls = type == MatchType.T20 ? 20 * 6 : type == MatchType.ODI ? 50 * 6 : null;
        boolean targetReached = innings.getTargetRuns() != null && innings.getTotalRuns() >= innings.getTargetRuns();
        boolean oversCompleted = maxBalls != null && innings.getLegalBalls() >= maxBalls;
        boolean wicketsLost = innings.getWickets() >= 10;
        if (targetReached || oversCompleted || wicketsLost) {
            innings.setStatus(InningsStatus.COMPLETED);
        }
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private Player getPlayer(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    private DeliveryResponse toResponse(Delivery delivery) {
        return new DeliveryResponse(delivery.getId(), delivery.getInnings().getId(), delivery.getOverNumber(),
                delivery.getBallNumber(), delivery.getBatsman().getId(), delivery.getNonStriker().getId(),
                delivery.getBowler().getId(), delivery.getRuns(), delivery.getExtraType(), delivery.getExtraRuns(),
                delivery.getTotalRuns(), delivery.isLegalDelivery(), delivery.getWicketType(),
                delivery.getDismissedBatsman() == null ? null : delivery.getDismissedBatsman().getId());
    }
}