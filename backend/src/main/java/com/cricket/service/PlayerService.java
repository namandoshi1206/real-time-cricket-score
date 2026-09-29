package com.cricket.service;

import com.cricket.dto.PlayerRequest;
import com.cricket.dto.PlayerResponse;
import com.cricket.entity.Player;
import com.cricket.entity.Team;
import com.cricket.entity.TeamPlayer;
import com.cricket.exception.DuplicateResourceException;
import com.cricket.exception.ResourceNotFoundException;
import com.cricket.repository.PlayerRepository;
import com.cricket.repository.TeamPlayerRepository;
import com.cricket.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final TeamPlayerRepository teamPlayerRepository;

    public PlayerService(PlayerRepository playerRepository, TeamRepository teamRepository,
                         TeamPlayerRepository teamPlayerRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.teamPlayerRepository = teamPlayerRepository;
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> findAll() {
        return playerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlayerResponse findById(Long id) {
        return toResponse(getPlayer(id));
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> findByTeamId(Long teamId) {
        getTeam(teamId);
        return teamPlayerRepository.findByTeamId(teamId).stream()
                .map(TeamPlayer::getPlayer)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PlayerResponse create(PlayerRequest request) {
        Team team = getTeam(request.getTeamId());
        ensureUnique(request, null);
        Player player = new Player();
        apply(request, player);
        player = playerRepository.save(player);
        saveMembership(player, team);
        return toResponse(player, team);
    }

    @Transactional
    public PlayerResponse update(Long id, PlayerRequest request) {
        Player player = getPlayer(id);
        Team team = getTeam(request.getTeamId());
        ensureUnique(request, id);
        apply(request, player);
        player = playerRepository.save(player);
        teamPlayerRepository.deleteByPlayerId(id);
        saveMembership(player, team);
        return toResponse(player, team);
    }

    @Transactional
    public void delete(Long id) {
        Player player = getPlayer(id);
        teamPlayerRepository.deleteByPlayerId(id);
        playerRepository.delete(player);
    }

    private Player getPlayer(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    private Team getTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }

    private void ensureUnique(PlayerRequest request, Long id) {
        boolean duplicate = id == null
                ? playerRepository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCase(request.getFirstName(), request.getLastName())
                : playerRepository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndIdNot(
                request.getFirstName(), request.getLastName(), id);
        if (duplicate) {
            throw new DuplicateResourceException("Player with this name already exists");
        }
    }

    private void apply(PlayerRequest request, Player player) {
        player.setFirstName(request.getFirstName().trim());
        player.setLastName(request.getLastName().trim());
        player.setRole(request.getRole());
        player.setBattingStyle(request.getBattingStyle());
        player.setBowlingStyle(request.getBowlingStyle());
    }

    private void saveMembership(Player player, Team team) {
        TeamPlayer membership = new TeamPlayer();
        membership.setPlayer(player);
        membership.setTeam(team);
        teamPlayerRepository.save(membership);
    }

    private PlayerResponse toResponse(Player player) {
        List<TeamPlayer> memberships = teamPlayerRepository.findByPlayerId(player.getId());
        if (memberships.isEmpty()) {
            return toResponse(player, null);
        }
        return toResponse(player, memberships.getFirst().getTeam());
    }

    private PlayerResponse toResponse(Player player, Team team) {
        return new PlayerResponse(
                player.getId(), player.getFirstName(), player.getLastName(), player.getRole(),
                player.getBattingStyle(), player.getBowlingStyle(),
                team == null ? null : team.getId(), team == null ? null : team.getName());
    }
}