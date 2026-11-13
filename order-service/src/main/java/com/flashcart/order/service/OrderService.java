package com.flashcart.order.service;

import com.flashcart.order.domain.Order;
import com.flashcart.order.domain.OrderRepository;
import com.flashcart.order.messaging.OrderPlacedEvent;
import com.flashcart.order.messaging.Topics;
import com.flashcart.order.outbox.OutboxEvent;
import com.flashcart.order.outbox.OutboxRepository;
import com.flashcart.order.web.dto.PlaceOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;
    private final Tracer tracer;
    private final Propagator propagator;

    public OrderService(OrderRepository orderRepository,
                        OutboxRepository outboxRepository,
                        JsonMapper jsonMapper,
                        Tracer tracer,
                        Propagator propagator) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
        this.tracer = tracer;
        this.propagator = propagator;
    }



    @Transactional
    public Order place(Long customerId, PlaceOrderRequest request) {
        Order order = orderRepository.save(
                new Order(customerId, request.productId(), request.quantity()));
        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getId(), customerId, order.getProductId(), order.getQuantity());
        outboxRepository.save(new OutboxEvent(
                Topics.ORDER_PLACED,
                String.valueOf(order.getProductId()),
                jsonMapper.writeValueAsString(event),
                currentTraceparent()));
        return order;
    }

    private String currentTraceparent() {
        TraceContext context = tracer.currentTraceContext().context();
        if (context == null) {
            return null;
        }
        Map<String, String> carrier = new HashMap<>();
        propagator.inject(context, carrier, Map::put);
        return carrier.get("traceparent");
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