package com.cricket.dto;

import com.cricket.entity.InningsStatus;

import java.util.List;

public record InningsResponse(
        Long id,
        Long matchId,
        Long battingTeamId,
        String battingTeamName,
        Long bowlingTeamId,
        String bowlingTeamName,
        Integer inningsNumber,
        Integer targetRuns,
        Integer totalRuns,
        Integer wickets,
        Integer legalBalls,
        String overs,
        InningsStatus status,
        Long strikerId,
        Long nonStrikerId,
        Long currentBowlerId,
        Double runRate,
        List<BatterStatistics> batting,
        List<BowlerStatistics> bowling,
        ExtrasSummary extras
) {
    public InningsResponse(Long id, Long matchId, Long battingTeamId, String battingTeamName,
                           Long bowlingTeamId, String bowlingTeamName, Integer inningsNumber,
                           Integer targetRuns, Integer totalRuns, Integer wickets, Integer legalBalls,
                           String overs, InningsStatus status, Long strikerId, Long nonStrikerId,
                           Long currentBowlerId) {
        this(id, matchId, battingTeamId, battingTeamName, bowlingTeamId, bowlingTeamName,
                inningsNumber, targetRuns, totalRuns, wickets, legalBalls, overs, status,
                strikerId, nonStrikerId, currentBowlerId, null, List.of(), List.of(),
                new ExtrasSummary(0, 0, 0, 0, 0, 0));
    }
}