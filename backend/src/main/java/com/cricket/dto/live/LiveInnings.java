package com.cricket.dto.live;

public record LiveInnings(
        String teamId,
        String teamName,
        Integer inningsNumber,
        Integer runs,
        Integer wickets,
        String overs,
        Double runRate,
        Integer targetRuns
) {
}