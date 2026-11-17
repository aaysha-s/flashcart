package com.flashcart.inventory.messaging;

import com.flashcart.inventory.product.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PaymentFailedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedListener.class);

    private final StockReservationService reservationService;
    private final JsonMapper jsonMapper;

    public PaymentFailedListener(StockReservationService reservationService, JsonMapper jsonMapper) {
        this.reservationService = reservationService;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    public void onPaymentFailed(String payload) {
        PaymentFailedEvent event = jsonMapper.readValue(payload, PaymentFailedEvent.class);
        boolean released = reservationService.releaseForOrder(event.orderId(), event.productId());
        if (released) {
            log.info("Released stock for order {} after failed payment", event.orderId());
        } else {
            log.info("Nothing to release for order {} (already released or never reserved)", event.orderId());
        }
    }
}
