package com.cricket.service;

import com.cricket.dto.InningsRequest;
import com.cricket.dto.InningsResponse;
import com.cricket.entity.Innings;
import com.cricket.entity.InningsStatus;
import com.cricket.entity.Match;
import com.cricket.entity.Player;
import com.cricket.entity.Team;
import com.cricket.exception.DuplicateResourceException;
import com.cricket.exception.ResourceNotFoundException;
import com.cricket.repository.InningsRepository;
import com.cricket.repository.MatchRepository;
import com.cricket.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InningsService {

    private final InningsRepository inningsRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    public InningsService(InningsRepository inningsRepository, MatchRepository matchRepository,
                          TeamRepository teamRepository) {
        this.inningsRepository = inningsRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional
    public InningsResponse create(Long matchId, InningsRequest request) {
        Match match = getMatch(matchId);
        Team battingTeam = getTeam(request.getBattingTeamId());
        Team bowlingTeam = getTeam(request.getBowlingTeamId());
        validateTeams(match, battingTeam, bowlingTeam);
        if (inningsRepository.existsByMatchIdAndInningsNumber(matchId, request.getInningsNumber())) {
            throw new DuplicateResourceException("Innings number already exists for this match");
        }
        Innings innings = new Innings();
        innings.setMatch(match);
        innings.setBattingTeam(battingTeam);
        innings.setBowlingTeam(bowlingTeam);
        innings.setInningsNumber(request.getInningsNumber());
        innings.setTargetRuns(request.getTargetRuns());
        innings.setStatus(InningsStatus.LIVE);
        return toResponse(inningsRepository.save(innings));
    }

    @Transactional(readOnly = true)
    public List<InningsResponse> findByMatchId(Long matchId) {
        getMatch(matchId);
        return inningsRepository.findByMatchIdOrderByInningsNumber(matchId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Innings getEntity(Long id) {
        return inningsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Innings not found: " + id));
    }

    @Transactional(readOnly = true)
    public InningsResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public InningsResponse toResponse(Innings innings) {
        return new InningsResponse(
                innings.getId(), innings.getMatch().getId(), innings.getBattingTeam().getId(),
                innings.getBattingTeam().getName(), innings.getBowlingTeam().getId(),
                innings.getBowlingTeam().getName(), innings.getInningsNumber(), innings.getTargetRuns(),
                innings.getTotalRuns(), innings.getWickets(), innings.getLegalBalls(),
                formatOvers(innings.getLegalBalls()), innings.getStatus(), id(innings.getStriker()),
                id(innings.getNonStriker()), id(innings.getCurrentBowler()));
    }

    public String formatOvers(Integer legalBalls) {
        return legalBalls / 6 + "." + legalBalls % 6;
    }

    private void validateTeams(Match match, Team battingTeam, Team bowlingTeam) {
        if (battingTeam.getId().equals(bowlingTeam.getId())) {
            throw new IllegalArgumentException("Batting and bowling teams must be different");
        }
        boolean matchHasTeams = (match.getTeamA().getId().equals(battingTeam.getId())
                && match.getTeamB().getId().equals(bowlingTeam.getId()))
                || (match.getTeamB().getId().equals(battingTeam.getId())
                && match.getTeamA().getId().equals(bowlingTeam.getId()));
        if (!matchHasTeams) {
            throw new IllegalArgumentException("Innings teams must belong to the match");
        }
    }

    private Match getMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }

    private Team getTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }

    private Long id(Player player) {
        return player == null ? null : player.getId();
    }
}