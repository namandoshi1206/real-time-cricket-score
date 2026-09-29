package com.cricket.repository;

import com.cricket.entity.Match;
import com.cricket.entity.MatchStatus;
import com.cricket.entity.MatchType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
	List<Match> findByStatus(MatchStatus status);

	List<Match> findByMatchType(MatchType matchType);
}
