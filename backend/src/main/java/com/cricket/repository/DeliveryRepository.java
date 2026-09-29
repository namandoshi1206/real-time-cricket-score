package com.cricket.repository;

import com.cricket.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
	List<Delivery> findByInningsIdOrderById(Long inningsId);

	boolean existsByInningsIdAndDismissedBatsmanId(Long inningsId, Long playerId);
}
