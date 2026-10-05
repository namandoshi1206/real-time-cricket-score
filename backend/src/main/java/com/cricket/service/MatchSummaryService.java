package com.cricket.service;

import com.cricket.dto.InningsResponse;
import com.cricket.dto.MatchResponse;
import com.cricket.dto.MatchSummaryResponse;
import com.cricket.entity.MatchStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatchSummaryService {

    private final MatchService matchService;
    private final InningsService inningsService;
    private final DeliveryService deliveryService;

    public MatchSummaryService(MatchService matchService, InningsService inningsService,
                              DeliveryService deliveryService) {
        this.matchService = matchService;
        this.inningsService = inningsService;
        this.deliveryService = deliveryService;
    }

    @Transactional(readOnly = true)
    public MatchSummaryResponse getSummary(Long matchId) {
        MatchResponse match = matchService.findById(matchId);
        List<InningsResponse> innings = inningsService.findByMatchId(matchId).stream()
                .map(score -> deliveryService.scorecard(score.id()))
                .toList();
        Map<Long, Integer> totals = new LinkedHashMap<>();
        innings.forEach(score -> totals.merge(score.battingTeamId(), score.totalRuns(), Integer::sum));

        Long winnerTeamId = null;
        String result = match.status() == MatchStatus.COMPLETED ? "Result unavailable" : "Match in progress";
        if (match.status() == MatchStatus.COMPLETED && !innings.isEmpty()) {
            InningsResponse successfulChase = innings.stream()
                    .filter(score -> score.targetRuns() != null && score.totalRuns() >= score.targetRuns())
                    .findFirst().orElse(null);
            if (successfulChase != null) {
                winnerTeamId = successfulChase.battingTeamId();
                result = successfulChase.battingTeamName() + " won by "
                        + (10 - successfulChase.wickets()) + " wickets";
            } else if (totals.size() >= 2) {
                List<Map.Entry<Long, Integer>> ranked = totals.entrySet().stream()
                        .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()).toList();
                if (!ranked.get(0).getValue().equals(ranked.get(1).getValue())) {
                    Long winningId = ranked.get(0).getKey();
                    winnerTeamId = winningId;
                    String winnerName = innings.stream()
                        .filter(score -> score.battingTeamId().equals(winningId))
                            .map(InningsResponse::battingTeamName).findFirst().orElse("Winning team");
                    result = winnerName + " won by "
                            + (ranked.get(0).getValue() - ranked.get(1).getValue()) + " runs";
                } else {
                    result = "Match tied";
                }
            }
        }
        return new MatchSummaryResponse(match, innings, winnerTeamId, result);
    }
}