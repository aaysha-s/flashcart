package com.flashcart.inventory.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic orderPlacedTopic() {
        return TopicBuilder.name(Topics.ORDER_PLACED).partitions(3).replicas(1).build();
    }
}
