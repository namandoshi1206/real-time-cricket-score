package com.cricket.dto.live;

public record LiveBatter(
        String id,
        String name,
        Integer runs,
        Integer balls,
        Integer fours,
        Integer sixes,
        Double strikeRate,
        Boolean onStrike
) {
}