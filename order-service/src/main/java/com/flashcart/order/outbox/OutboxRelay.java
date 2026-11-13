package com.flashcart.order.outbox;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Tracer tracer;
    private final Propagator propagator;

    public OutboxRelay(OutboxRepository outboxRepository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       Tracer tracer,
                       Propagator propagator) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : outboxRepository.lockNextBatch()) {
            Span span = startSpan(event);
            try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
                event.markPublished();
                log.info("Relayed outbox event {} to {}", event.getId(), event.getTopic());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                span.error(e);
                return;
            } catch (ExecutionException | TimeoutException e) {
                span.error(e);
                log.warn("Kafka unavailable; outbox event {} will be retried", event.getId());
                return;
            } finally {
                span.end();
            }
        }
    }

    private Span startSpan(OutboxEvent event) {
        String name = "outbox publish " + event.getTopic();
        if (event.getTraceparent() == null) {
            return tracer.nextSpan().name(name).start();
        }
        Map<String, String> carrier = Map.of("traceparent", event.getTraceparent());
        return propagator.extract(carrier, Map::get).name(name).start();
    }
}