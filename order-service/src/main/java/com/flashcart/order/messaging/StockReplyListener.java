package com.flashcart.order.messaging;

import com.flashcart.order.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StockReplyListener {

    private final OrderService orderService;
    private final JsonMapper jsonMapper;

    public StockReplyListener(OrderService orderService, JsonMapper jsonMapper) {
        this.orderService = orderService;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = Topics.STOCK_RESERVED)
    public void onStockReserved(String payload) {
        StockReservedEvent event = jsonMapper.readValue(payload, StockReservedEvent.class);
        orderService.confirm(event.orderId(), event.totalPaise());
    }

    @KafkaListener(topics = Topics.STOCK_REJECTED)
    public void onStockRejected(String payload) {
        StockRejectedEvent event = jsonMapper.readValue(payload, StockRejectedEvent.class);
        orderService.cancel(event.orderId(), event.reason());
    }
}
