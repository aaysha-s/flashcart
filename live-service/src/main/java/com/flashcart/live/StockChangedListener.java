package com.flashcart.live;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StockChangedListener {

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    public StockChangedListener(StringRedisTemplate redis, JsonMapper jsonMapper) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "stock.changed")
    public void onStockChanged(String payload) {
        StockChangedEvent event = jsonMapper.readValue(payload, StockChangedEvent.class);
        String message = jsonMapper.writeValueAsString(StockUpdate.from(event));
        redis.convertAndSend(Channels.LIVE, message);
    }
}