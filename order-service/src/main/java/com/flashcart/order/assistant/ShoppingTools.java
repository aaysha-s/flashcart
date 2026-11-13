package com.flashcart.order.assistant;

import com.flashcart.order.domain.Order;
import com.flashcart.order.service.OrderService;
import com.flashcart.order.web.dto.PlaceOrderRequest;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

public class ShoppingTools {

    private static final int MAX_QUANTITY = 5;

    private final RestClient inventory;
    private final OrderService orderService;
    private final Long customerId;
    private final String bearerToken;

    ShoppingTools(RestClient inventory, OrderService orderService, Long customerId, String bearerToken) {
        this.inventory = inventory;
        this.orderService = orderService;
        this.customerId = customerId;
        this.bearerToken = bearerToken;
    }

    @Tool(description = "Search the FlashCart catalogue by meaning. Returns matching products with id, name, description, price in Indian rupees and current stock.")
    public List<ProductSummary> searchProducts(
            @ToolParam(description = "What the customer is looking for, in plain words") String query) {
        InventoryProduct[] results = inventory.get()
                .uri(uri -> uri.path("/api/products/search").queryParam("q", query).queryParam("limit", 5).build())
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(InventoryProduct[].class);
        return results == null ? List.of() : Arrays.stream(results).map(ProductSummary::from).toList();
    }

    @Tool(description = "Place an order for the current customer. Only call this when the customer has clearly asked to buy or order a specific product.")
    public OrderConfirmation placeOrder(
            @ToolParam(description = "The product id, taken from searchProducts results") Long productId,
            @ToolParam(description = "How many units, between 1 and 5") int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            return new OrderConfirmation(null, "REJECTED", "Quantity must be between 1 and " + MAX_QUANTITY);
        }
        Order order = orderService.place(customerId, new PlaceOrderRequest(productId, quantity));
        return new OrderConfirmation(order.getId(), order.getStatus().name(),
                "Order received; stock is being reserved and the order will be confirmed shortly");
    }
}
