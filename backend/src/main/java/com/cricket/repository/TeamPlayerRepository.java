package com.cricket.repository;

import com.cricket.entity.TeamPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamPlayerRepository extends JpaRepository<TeamPlayer, Long> {
	List<TeamPlayer> findByTeamId(Long teamId);

	List<TeamPlayer> findByPlayerId(Long playerId);

	boolean existsByTeamId(Long teamId);

	boolean existsByTeamIdAndPlayerId(Long teamId, Long playerId);

	void deleteByPlayerId(Long playerId);
}
