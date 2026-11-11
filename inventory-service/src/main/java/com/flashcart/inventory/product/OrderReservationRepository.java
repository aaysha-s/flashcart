package com.flashcart.inventory.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderReservationRepository extends JpaRepository<OrderReservation, Long> {

    Optional<OrderReservation> findByOrderId(Long orderId);
}