package com.cricket.service;

import com.cricket.dto.DeliveryRequest;
import com.cricket.dto.DeliveryResponse;
import com.cricket.dto.BatterStatistics;
import com.cricket.dto.BowlerStatistics;
import com.cricket.dto.ExtrasSummary;
import com.cricket.dto.InningsResponse;
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
import java.util.LinkedHashMap;
import java.util.Map;

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
        validateBatterState(innings, batsman, nonStriker);
        validateSequence(innings, request);
        validateRuns(request);
        boolean legal = request.getExtraType() != ExtraType.WIDE
                && request.getExtraType() != ExtraType.NO_BALL;
        boolean wicket = request.getWicketType() != null && request.getWicketType() != WicketType.NO_WICKET;
        Player dismissedBatsman = null;
        if (wicket) {
            Long dismissedId = request.getDismissedBatsmanId() == null
                    ? batsman.getId() : request.getDismissedBatsmanId();
            dismissedBatsman = dismissedId.equals(batsman.getId()) ? batsman
                    : dismissedId.equals(nonStriker.getId()) ? nonStriker : null;
            if (dismissedBatsman == null) {
                throw new IllegalArgumentException("Dismissed batsman must be the striker or non-striker");
            }
            if (isDismissed(inningsId, dismissedBatsman)) {
                throw new IllegalArgumentException("Dismissed batsman cannot receive another delivery");
            }
            validateWicketForExtra(request);
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
        delivery.setDismissedBatsman(dismissedBatsman);

        Delivery saved = deliveryRepository.save(delivery);
        innings.setTotalRuns(innings.getTotalRuns() + saved.getTotalRuns());
        if (legal) {
            innings.setLegalBalls(innings.getLegalBalls() + 1);
        }
        if (wicket) {
            innings.setWickets(innings.getWickets() + 1);
        }
        Player nextStriker = batsman;
        Player nextNonStriker = nonStriker;
        if (completedRuns(request) % 2 != 0) {
            Player previousStriker = nextStriker;
            nextStriker = nextNonStriker;
            nextNonStriker = previousStriker;
        }
        if (legal && innings.getLegalBalls() % 6 == 0) {
            Player previousStriker = nextStriker;
            nextStriker = nextNonStriker;
            nextNonStriker = previousStriker;
        }
        innings.setStriker(nextStriker);
        innings.setNonStriker(nextNonStriker);
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

    @Transactional(readOnly = true)
    public InningsResponse scorecard(Long inningsId) {
        Innings innings = inningsService.getEntity(inningsId);
        InningsResponse base = inningsService.toResponse(innings);
        List<Delivery> deliveries = deliveryRepository.findByInningsIdOrderById(inningsId);
        Map<Long, BatterAccumulator> batters = new LinkedHashMap<>();
        Map<Long, BowlerAccumulator> bowlers = new LinkedHashMap<>();
        int wides = 0;
        int noBalls = 0;
        int byes = 0;
        int legByes = 0;
        int penalties = 0;

        for (Delivery delivery : deliveries) {
            Player striker = delivery.getBatsman();
            Player nonStriker = delivery.getNonStriker();
            Player bowler = delivery.getBowler();
            BatterAccumulator batter = batters.computeIfAbsent(striker.getId(), ignored -> new BatterAccumulator(striker));
            batters.computeIfAbsent(nonStriker.getId(), ignored -> new BatterAccumulator(nonStriker));
            batter.runs += delivery.getRuns();
            if (delivery.getExtraType() != ExtraType.WIDE) {
                batter.ballsFaced++;
            }
            if (delivery.getRuns() == 4) {
                batter.fours++;
            } else if (delivery.getRuns() == 6) {
                batter.sixes++;
            }
            if (delivery.getDismissedBatsman() != null) {
                BatterAccumulator dismissed = batters.computeIfAbsent(delivery.getDismissedBatsman().getId(),
                        ignored -> new BatterAccumulator(delivery.getDismissedBatsman()));
                dismissed.dismissed = true;
            }

            BowlerAccumulator figures = bowlers.computeIfAbsent(bowler.getId(), ignored -> new BowlerAccumulator(bowler));
            if (delivery.isLegalDelivery()) {
                figures.legalBalls++;
            }
            if (delivery.getWicketType() != null && delivery.getWicketType() != WicketType.NO_WICKET
                    && delivery.getWicketType() != WicketType.RUN_OUT) {
                figures.wickets++;
            }
            int concededRuns = switch (delivery.getExtraType()) {
                case BYE, LEG_BYE, PENALTY -> delivery.getRuns();
                case NONE, WIDE, NO_BALL -> delivery.getTotalRuns();
            };
            figures.runsConceded += concededRuns;

            switch (delivery.getExtraType()) {
                case WIDE -> wides += delivery.getExtraRuns();
                case NO_BALL -> noBalls += delivery.getExtraRuns();
                case BYE -> byes += delivery.getExtraRuns();
                case LEG_BYE -> legByes += delivery.getExtraRuns();
                case PENALTY -> penalties += delivery.getExtraRuns();
                case NONE -> { }
            }
        }

        int extraTotal = wides + noBalls + byes + legByes + penalties;
        double runRate = innings.getLegalBalls() == 0 ? 0.0
                : roundTwoDecimals(innings.getTotalRuns() * 6.0 / innings.getLegalBalls());
        List<BatterStatistics> batting = batters.values().stream().map(BatterAccumulator::toStatistics).toList();
        List<BowlerStatistics> bowling = bowlers.values().stream()
                .map(figures -> figures.toStatistics(inningsService.formatOvers(figures.legalBalls)))
                .toList();
        return new InningsResponse(base.id(), base.matchId(), base.battingTeamId(), base.battingTeamName(),
                base.bowlingTeamId(), base.bowlingTeamName(), base.inningsNumber(), base.targetRuns(),
                base.totalRuns(), base.wickets(), base.legalBalls(), base.overs(), base.status(),
                base.strikerId(), base.nonStrikerId(), base.currentBowlerId(), runRate, batting, bowling,
                new ExtrasSummary(wides, noBalls, byes, legByes, penalties, extraTotal));
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

    private void validateBatterState(Innings innings, Player batsman, Player nonStriker) {
        if (isDismissed(innings.getId(), batsman) || isDismissed(innings.getId(), nonStriker)) {
            throw new IllegalArgumentException("Dismissed batsman cannot receive another delivery");
        }
        Player currentStriker = innings.getStriker();
        Player currentNonStriker = innings.getNonStriker();
        if (currentStriker == null && currentNonStriker == null) {
            return;
        }
        if (currentStriker == null || currentNonStriker == null) {
            throw new IllegalArgumentException("Both current batsmen must be set");
        }

        boolean sameStriker = currentStriker.getId().equals(batsman.getId());
        boolean sameNonStriker = currentNonStriker.getId().equals(nonStriker.getId());
        boolean replaceStriker = sameNonStriker && isDismissed(innings.getId(), currentStriker)
                && !isDismissed(innings.getId(), batsman);
        boolean replaceNonStriker = sameStriker && isDismissed(innings.getId(), currentNonStriker)
                && !isDismissed(innings.getId(), nonStriker);
        if (!(sameStriker && sameNonStriker) && !replaceStriker && !replaceNonStriker) {
            throw new IllegalArgumentException("Batsmen do not match the current innings strike state");
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
            case BYE, LEG_BYE -> require(runs == 0, "Bye extras cannot include batsman runs");
            case PENALTY -> require(runs == 0 && extraRuns >= 1, "PENALTY requires at least one extra run");
        }
    }

    private void validateWicketForExtra(DeliveryRequest request) {
        ExtraType extraType = request.getExtraType();
        WicketType wicketType = request.getWicketType();
        if (extraType == ExtraType.WIDE && wicketType != WicketType.RUN_OUT && wicketType != WicketType.STUMPED) {
            throw new IllegalArgumentException("Only run-out or stumped wickets can be recorded on a wide");
        }
        if (extraType == ExtraType.NO_BALL && wicketType != WicketType.RUN_OUT) {
            throw new IllegalArgumentException("Only run-out wickets can be recorded on a no-ball");
        }
    }

    private int completedRuns(DeliveryRequest request) {
        int runningExtras = switch (request.getExtraType()) {
            case WIDE, NO_BALL -> Math.max(0, request.getExtraRuns() - 1);
            case BYE, LEG_BYE -> request.getExtraRuns();
            case NONE, PENALTY -> 0;
        };
        return request.getRuns() + runningExtras;
    }

    private boolean isDismissed(Long inningsId, Player player) {
        return deliveryRepository.existsByInningsIdAndDismissedBatsmanId(inningsId, player.getId());
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
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

    private static class BatterAccumulator {
        private final Player player;
        private int runs;
        private int ballsFaced;
        private int fours;
        private int sixes;
        private boolean dismissed;

        private BatterAccumulator(Player player) {
            this.player = player;
        }

        private BatterStatistics toStatistics() {
            double strikeRate = ballsFaced == 0 ? 0.0 : Math.round(runs * 10000.0 / ballsFaced) / 100.0;
            return new BatterStatistics(player.getId(), player.getFirstName() + " " + player.getLastName(),
                    runs, ballsFaced, fours, sixes, dismissed, strikeRate);
        }
    }

    private static class BowlerAccumulator {
        private final Player player;
        private int legalBalls;
        private int runsConceded;
        private int wickets;

        private BowlerAccumulator(Player player) {
            this.player = player;
        }

        private BowlerStatistics toStatistics(String overs) {
            double economy = legalBalls == 0 ? 0.0
                    : Math.round(runsConceded * 6.0 * 100.0 / legalBalls) / 100.0;
            return new BowlerStatistics(player.getId(), player.getFirstName() + " " + player.getLastName(),
                    legalBalls, overs, runsConceded, wickets, economy);
        }
    }
}