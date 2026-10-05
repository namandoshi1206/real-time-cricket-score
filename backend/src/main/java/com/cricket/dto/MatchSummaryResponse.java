package com.cricket.dto;

import java.util.List;

public record MatchSummaryResponse(
        MatchResponse match,
        List<InningsResponse> innings,
        Long winnerTeamId,
        String result
) {
}