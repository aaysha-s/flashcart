package com.flashcart.inventory.messaging;

import com.flashcart.inventory.product.InsufficientStockException;
import com.flashcart.inventory.product.OrderReservation;
import com.flashcart.inventory.product.OrderReservationRepository;
import com.flashcart.inventory.product.ProductNotFoundException;
import com.flashcart.inventory.product.ProductService;
import com.flashcart.inventory.product.StockConflictException;
import com.flashcart.inventory.product.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderPlacedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedListener.class);

    private final StockReservationService reservationService;
    private final ProductService productService;
    private final OrderReservationRepository reservationRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public OrderPlacedListener(StockReservationService reservationService,
                               ProductService productService,
                               OrderReservationRepository reservationRepository,
                               KafkaTemplate<String, String> kafkaTemplate,
                               JsonMapper jsonMapper) {
        this.reservationService = reservationService;
        this.productService = productService;
        this.reservationRepository = reservationRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = Topics.ORDER_PLACED)
    public void onOrderPlaced(String payload) {
        OrderPlacedEvent event = jsonMapper.readValue(payload, OrderPlacedEvent.class);

        OrderReservation reservation = reservationRepository.findByOrderId(event.orderId())
                .map(existing -> {
                    log.info("Duplicate OrderPlaced for order {}; replaying {}",
                            event.orderId(), existing.getOutcome());
                    return existing;
                })
                .orElseGet(() -> process(event));

        reply(reservation);
    }

    private OrderReservation process(OrderPlacedEvent event) {
        try {
            OrderReservation reservation = reservationService.reserveForOrder(
                    event.orderId(), event.productId(), event.quantity());
            log.info("Reserved stock for order {}, total {} paise",
                    event.orderId(), reservation.getTotalPaise());
            return reservation;
        } catch (InsufficientStockException | ProductNotFoundException | StockConflictException e) {
            log.info("Rejected order {}: {}", event.orderId(), e.getMessage());
            return productService.recordRejection(
                    event.orderId(), event.productId(), event.quantity(), e.getMessage());
        }
    }

    private void reply(OrderReservation reservation) {
        if (reservation.isReserved()) {
            send(Topics.STOCK_RESERVED, reservation.getOrderId(), new StockReservedEvent(
                    reservation.getOrderId(), reservation.getProductId(),
                    reservation.getQuantity(), reservation.getTotalPaise()));
        } else {
            send(Topics.STOCK_REJECTED, reservation.getOrderId(), new StockRejectedEvent(
                    reservation.getOrderId(), reservation.getProductId(), reservation.getReason()));
        }
    }

    private void send(String topic, Long orderId, Object event) {
        kafkaTemplate.send(topic, String.valueOf(orderId), jsonMapper.writeValueAsString(event)).join();
    }
}