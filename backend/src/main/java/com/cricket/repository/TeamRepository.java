package com.cricket.repository;

import com.cricket.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
	boolean existsByNameIgnoreCase(String name);

	boolean existsByShortNameIgnoreCase(String shortName);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	boolean existsByShortNameIgnoreCaseAndIdNot(String shortName, Long id);
}
