package com.flashcart.order.web;

import com.flashcart.order.domain.Order;
import com.flashcart.order.service.OrderService;
import com.flashcart.order.web.dto.OrderResponse;
import com.flashcart.order.web.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody PlaceOrderRequest request) {
        Order order = orderService.place(customerId(jwt), request);
        return ResponseEntity.accepted()
                .location(URI.create("/api/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }

    @GetMapping
    public PagedModel<OrderResponse> myOrders(@AuthenticationPrincipal Jwt jwt,
                                              @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(orderService.listForCustomer(customerId(jwt), pageable)
                .map(OrderResponse::from));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return OrderResponse.from(orderService.getForCustomer(id, customerId(jwt)));
    }

    private Long customerId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
