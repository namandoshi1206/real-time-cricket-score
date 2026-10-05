package com.cricket.dto.live;

public record LiveScore(
        Integer runs,
        Integer wickets,
        String overs,
        Integer inningsNumber,
        Double runRate,
        Integer targetRuns
) {
}