package com.orderms.inventory.repository;

import com.orderms.inventory.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    Optional<InventoryReservation> findByReservationId(String reservationId);
    List<InventoryReservation> findByOrderId(String orderId);
}
