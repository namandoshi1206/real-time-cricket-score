package com.cricket.dto.live;

public record LiveBowler(
        String id,
        String name,
        String overs,
        Integer maidens,
        Integer runs,
        Integer wickets,
        Double economy
) {
}