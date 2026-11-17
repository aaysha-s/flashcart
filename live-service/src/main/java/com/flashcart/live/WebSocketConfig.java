package com.flashcart.live;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final StockWebSocketHandler stockHandler;

    public WebSocketConfig(StockWebSocketHandler stockHandler) {
        this.stockHandler = stockHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(stockHandler, "/ws/stock")
                .setAllowedOriginPatterns("*"); // tightened to the storefront's origin in Phase 13
    }
}