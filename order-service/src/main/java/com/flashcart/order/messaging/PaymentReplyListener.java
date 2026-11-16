package com.flashcart.order.messaging;

import com.flashcart.order.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PaymentReplyListener {

    private final OrderService orderService;
    private final JsonMapper jsonMapper;

    public PaymentReplyListener(OrderService orderService, JsonMapper jsonMapper) {
        this.orderService = orderService;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED)
    public void onPaymentCompleted(String payload) {
        PaymentCompletedEvent event = jsonMapper.readValue(payload, PaymentCompletedEvent.class);
        orderService.confirm(event.orderId(), event.amountPaise());
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    public void onPaymentFailed(String payload) {
        PaymentFailedEvent event = jsonMapper.readValue(payload, PaymentFailedEvent.class);
        orderService.cancel(event.orderId(), "Payment failed: " + event.reason());
    }
}