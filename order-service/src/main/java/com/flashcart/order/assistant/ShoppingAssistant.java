package com.flashcart.order.assistant;

import com.flashcart.order.service.OrderService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ShoppingAssistant {

    private static final String SYSTEM_PROMPT = """
            You are FlashCart's shopping assistant. Prices are in Indian rupees.
            Always use searchProducts to find products; never invent products, prices or stock.
            Only call placeOrder when the customer clearly asks to buy or order something.
            If they are just browsing, recommend products and ask whether they want to order.
            Keep replies short and friendly.
            """;

    private final ChatClient chatClient;
    private final RestClient inventory;
    private final OrderService orderService;

    public ShoppingAssistant(ChatClient.Builder chatClientBuilder,
                             RestClient.Builder restClientBuilder,
                             OrderService orderService,
                             @Value("${flashcart.inventory-url}") String inventoryUrl) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.inventory = restClientBuilder.baseUrl(inventoryUrl).build();
        this.orderService = orderService;
    }

    public String chat(Long customerId, String bearerToken, String message) {
        return chatClient.prompt()
                .user(message)
                .tools(new ShoppingTools(inventory, orderService, customerId, bearerToken))
                .call()
                .content();
    }
}
