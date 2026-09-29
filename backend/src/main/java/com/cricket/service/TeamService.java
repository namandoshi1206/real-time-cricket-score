package com.cricket.service;

import com.cricket.dto.TeamRequest;
import com.cricket.dto.TeamResponse;
import com.cricket.entity.Team;
import com.cricket.exception.DuplicateResourceException;
import com.cricket.exception.ResourceNotFoundException;
import com.cricket.repository.TeamPlayerRepository;
import com.cricket.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamPlayerRepository teamPlayerRepository;

    public TeamService(TeamRepository teamRepository, TeamPlayerRepository teamPlayerRepository) {
        this.teamRepository = teamRepository;
        this.teamPlayerRepository = teamPlayerRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> findAll() {
        return teamRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse findById(Long id) {
        return toResponse(getTeam(id));
    }

    @Transactional
    public TeamResponse create(TeamRequest request) {
        ensureUnique(request, null);
        Team team = new Team();
        apply(request, team);
        return toResponse(teamRepository.save(team));
    }

    @Transactional
    public TeamResponse update(Long id, TeamRequest request) {
        Team team = getTeam(id);
        ensureUnique(request, id);
        apply(request, team);
        return toResponse(teamRepository.save(team));
    }

    @Transactional
    public void delete(Long id) {
        Team team = getTeam(id);
        if (teamPlayerRepository.existsByTeamId(id)) {
            throw new DuplicateResourceException("Team cannot be deleted while it has players");
        }
        teamRepository.delete(team);
    }

    private Team getTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }

    private void ensureUnique(TeamRequest request, Long id) {
        boolean duplicateName = id == null
                ? teamRepository.existsByNameIgnoreCase(request.getName())
                : teamRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id);
        boolean duplicateShortName = id == null
                ? teamRepository.existsByShortNameIgnoreCase(request.getShortName())
                : teamRepository.existsByShortNameIgnoreCaseAndIdNot(request.getShortName(), id);
        if (duplicateName || duplicateShortName) {
            throw new DuplicateResourceException("Team name or short name already exists");
        }
    }

    private void apply(TeamRequest request, Team team) {
        team.setName(request.getName().trim());
        team.setShortName(request.getShortName().trim());
        team.setCountry(request.getCountry() == null ? null : request.getCountry().trim());
    }

    private TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getShortName(), team.getCountry());
    }
}