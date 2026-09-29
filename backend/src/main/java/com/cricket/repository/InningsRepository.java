package com.cricket.repository;

import com.cricket.entity.Innings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InningsRepository extends JpaRepository<Innings, Long> {
	List<Innings> findByMatchIdOrderByInningsNumber(Long matchId);

	boolean existsByMatchIdAndInningsNumber(Long matchId, Integer inningsNumber);
}
