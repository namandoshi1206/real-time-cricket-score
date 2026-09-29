package com.cricket.service;

import com.cricket.dto.MatchRequest;
import com.cricket.dto.MatchResponse;
import com.cricket.entity.Match;
import com.cricket.entity.MatchStatus;
import com.cricket.entity.Team;
import com.cricket.exception.ResourceNotFoundException;
import com.cricket.repository.MatchRepository;
import com.cricket.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    public MatchService(MatchRepository matchRepository, TeamRepository teamRepository) {
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> findAll() {
        return matchRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MatchResponse findById(Long id) {
        return toResponse(getMatch(id));
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> findByStatus(MatchStatus status) {
        return matchRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> findByType(com.cricket.entity.MatchType matchType) {
        return matchRepository.findByMatchType(matchType).stream().map(this::toResponse).toList();
    }

    @Transactional
    public MatchResponse create(MatchRequest request) {
        Team teamA = getTeam(request.getTeamAId());
        Team teamB = getTeam(request.getTeamBId());
        validateTeams(teamA, teamB);
        Match match = new Match();
        apply(request, match, teamA, teamB);
        return toResponse(matchRepository.save(match));
    }

    @Transactional
    public MatchResponse update(Long id, MatchRequest request) {
        Match match = getMatch(id);
        Team teamA = getTeam(request.getTeamAId());
        Team teamB = getTeam(request.getTeamBId());
        validateTeams(teamA, teamB);
        validateStatusTransition(match.getStatus(), request.getStatus());
        apply(request, match, teamA, teamB);
        return toResponse(matchRepository.save(match));
    }

    @Transactional
    public void delete(Long id) {
        matchRepository.delete(getMatch(id));
    }

    private Match getMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }

    private Team getTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }

    private void validateTeams(Team teamA, Team teamB) {
        if (teamA.getId().equals(teamB.getId())) {
            throw new IllegalArgumentException("A match must use two different teams");
        }
    }

    private void validateStatusTransition(MatchStatus current, MatchStatus requested) {
        if (current == requested) {
            return;
        }
        boolean allowed = switch (current) {
            case UPCOMING -> requested == MatchStatus.LIVE || requested == MatchStatus.ABANDONED;
            case LIVE -> requested == MatchStatus.INNINGS_BREAK
                    || requested == MatchStatus.COMPLETED || requested == MatchStatus.ABANDONED;
            case INNINGS_BREAK -> requested == MatchStatus.LIVE
                    || requested == MatchStatus.COMPLETED || requested == MatchStatus.ABANDONED;
            case COMPLETED, ABANDONED -> false;
        };
        if (!allowed) {
            throw new IllegalArgumentException("Invalid match status transition from " + current + " to " + requested);
        }
    }

    private void apply(MatchRequest request, Match match, Team teamA, Team teamB) {
        match.setTitle(request.getTitle().trim());
        match.setTeamA(teamA);
        match.setTeamB(teamB);
        match.setVenue(request.getVenue().trim());
        match.setMatchType(request.getMatchType());
        match.setScheduledDate(request.getScheduledDate());
        match.setStatus(request.getStatus());
    }

    private MatchResponse toResponse(Match match) {
        return new MatchResponse(
                match.getId(), match.getTitle(), match.getTeamA().getId(), match.getTeamA().getName(),
                match.getTeamB().getId(), match.getTeamB().getName(), match.getVenue(), match.getMatchType(),
                match.getScheduledDate(), match.getStatus());
    }
}