package com.cricket.live;

import com.cricket.dto.live.LiveMatch;

import java.util.List;

public interface CricketDataProvider {
    List<LiveMatch> fetchMatches(LiveFeedCategory category);
}