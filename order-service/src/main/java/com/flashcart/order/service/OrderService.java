package com.flashcart.order.service;

import com.flashcart.order.domain.Order;
import com.flashcart.order.domain.OrderRepository;
import com.flashcart.order.messaging.OrderPlacedEvent;
import com.flashcart.order.web.dto.PlaceOrderRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orderRepository, ApplicationEventPublisher events) {
        this.orderRepository = orderRepository;
        this.events = events;
    }

    @Transactional
    public Order place(Long customerId, PlaceOrderRequest request) {
        Order order = orderRepository.save(
                new Order(customerId, request.productId(), request.quantity()));
        events.publishEvent(new OrderPlacedEvent(
                order.getId(), customerId, order.getProductId(), order.getQuantity()));
        return order;
    }

    @Transactional(readOnly = true)
    public Page<Order> listForCustomer(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Order getForCustomer(Long orderId, Long customerId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getCustomerId().equals(customerId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
    @Transactional
    public void confirm(Long orderId, long totalPaise) {
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            if (!order.isPending()) {
                log.info("Order {} already {}, ignoring duplicate StockReserved", orderId, order.getStatus());
                return;
            }
            order.confirm(totalPaise);
            log.info("Order {} CONFIRMED, total {} paise", orderId, totalPaise);
        }, () -> log.warn("StockReserved for unknown order {}", orderId));
    }

    @Transactional
    public void cancel(Long orderId, String reason) {
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            if (!order.isPending()) {
                log.info("Order {} already {}, ignoring duplicate StockRejected", orderId, order.getStatus());
                return;
            }
            order.cancel(reason);
            log.info("Order {} CANCELLED: {}", orderId, reason);
        }, () -> log.warn("StockRejected for unknown order {}", orderId));
    }
}