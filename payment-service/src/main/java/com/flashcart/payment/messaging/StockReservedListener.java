package com.flashcart.payment.messaging;

import com.flashcart.payment.domain.Payment;
import com.flashcart.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StockReservedListener {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public StockReservedListener(PaymentService paymentService,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 JsonMapper jsonMapper) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = Topics.STOCK_RESERVED)
    public void onStockReserved(String payload) {
        StockReservedEvent event = jsonMapper.readValue(payload, StockReservedEvent.class);
        Payment payment = paymentService.process(event.orderId(), event.totalPaise());

        if (payment.isCompleted()) {
            send(Topics.PAYMENT_COMPLETED, event.orderId(),
                    new PaymentCompletedEvent(event.orderId(), payment.getAmountPaise()));
        } else {
            send(Topics.PAYMENT_FAILED, event.orderId(),
                    new PaymentFailedEvent(event.orderId(), event.productId(),
                            event.quantity(), payment.getFailureReason()));
        }
    }

    private void send(String topic, Long orderId, Object event) {
        kafkaTemplate.send(topic, String.valueOf(orderId), jsonMapper.writeValueAsString(event)).join();
    }
}