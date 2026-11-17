package com.flashcart.live;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
public class RedisListenerConfig {

    @Bean
    RedisMessageListenerContainer liveUpdatesListener(RedisConnectionFactory connectionFactory,
                                                      StockWebSocketHandler stockHandler) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                (message, pattern) -> stockHandler.broadcast(
                        new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(Channels.LIVE));
        return container;
    }
}
