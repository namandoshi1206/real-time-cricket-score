package com.cricket.dto;

import com.cricket.entity.MatchStatus;
import com.cricket.entity.MatchType;

import java.time.LocalDateTime;

public record MatchResponse(
        Long id,
        String title,
        Long teamAId,
        String teamAName,
        Long teamBId,
        String teamBName,
        String venue,
        MatchType matchType,
        LocalDateTime scheduledDate,
        MatchStatus status
) {
}