package com.cricket.dto;

import com.cricket.entity.InningsStatus;

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
        Long currentBowlerId
) {
}