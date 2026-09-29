package com.cricket.repository;

import com.cricket.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
	boolean existsByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);

	boolean existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndIdNot(String firstName, String lastName, Long id);
}
