package com.cricket.controller;

import com.cricket.dto.live.LiveFeedResponse;
import com.cricket.dto.live.LiveMatchResponse;
import com.cricket.dto.live.LiveScoreResponse;
import com.cricket.dto.live.LiveScorecardResponse;
import com.cricket.live.LiveCricketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/live")
public class LiveCricketController {

    private final LiveCricketService liveCricketService;

    public LiveCricketController(LiveCricketService liveCricketService) {
        this.liveCricketService = liveCricketService;
    }

    @GetMapping("/matches")
    public LiveFeedResponse matches() {
        return liveCricketService.getMatches();
    }

    @GetMapping("/matches/{matchId}")
    public LiveMatchResponse match(@PathVariable String matchId) {
        return liveCricketService.getMatch(matchId);
    }

    @GetMapping("/matches/{matchId}/score")
    public LiveScoreResponse score(@PathVariable String matchId) {
        return liveCricketService.getScore(matchId);
    }

    @GetMapping("/matches/{matchId}/scorecard")
    public LiveScorecardResponse scorecard(@PathVariable String matchId) {
        return liveCricketService.getScorecard(matchId);
    }

    @GetMapping("/upcoming")
    public LiveFeedResponse upcoming() {
        return liveCricketService.getUpcoming();
    }

    @GetMapping("/recent")
    public LiveFeedResponse recent() {
        return liveCricketService.getRecent();
    }
}