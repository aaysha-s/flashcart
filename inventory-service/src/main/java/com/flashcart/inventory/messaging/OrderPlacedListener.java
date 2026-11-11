package com.flashcart.inventory.messaging;

import com.flashcart.inventory.product.InsufficientStockException;
import com.flashcart.inventory.product.Product;
import com.flashcart.inventory.product.ProductNotFoundException;
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
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public OrderPlacedListener(StockReservationService reservationService,
                               KafkaTemplate<String, String> kafkaTemplate,
                               JsonMapper jsonMapper) {
        this.reservationService = reservationService;
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = Topics.ORDER_PLACED)
    public void onOrderPlaced(String payload) {
        OrderPlacedEvent event = jsonMapper.readValue(payload, OrderPlacedEvent.class);
        log.info("Received OrderPlaced for order {}: product {} x {}",
                event.orderId(), event.productId(), event.quantity());

        try {
            Product product = reservationService.reserve(event.productId(), event.quantity());
            long totalPaise = product.getPricePaise() * event.quantity();
            send(Topics.STOCK_RESERVED, event.orderId(),
                    new StockReservedEvent(event.orderId(), event.productId(), event.quantity(), totalPaise));
            log.info("Reserved stock for order {}, total {} paise", event.orderId(), totalPaise);
        } catch (InsufficientStockException | ProductNotFoundException | StockConflictException e) {
            send(Topics.STOCK_REJECTED, event.orderId(),
                    new StockRejectedEvent(event.orderId(), event.productId(), e.getMessage()));
            log.info("Rejected order {}: {}", event.orderId(), e.getMessage());
        }
    }

    private void send(String topic, Long orderId, Object event) {
        kafkaTemplate.send(topic, String.valueOf(orderId), jsonMapper.writeValueAsString(event)).join();
    }
}