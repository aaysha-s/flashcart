package com.flashcart.knowledge.ask;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flashcart.ask")
public record AskProperties(int topK, double similarityThreshold) {}