package com.flashcart.live.metrics;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Per-second counters in Redis, shared by every live-service pod. */
@Component
public class MetricsStore {

    static final int WINDOW_SECONDS = 60;
    private static final Duration BUCKET_TTL = Duration.ofHours(1);
    private static final String PREFIX = "flashcart:metrics:";

    private final StringRedisTemplate redis;

    public MetricsStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void record(MetricType type, long epochMs, long amountPaise) {
        String bucket = PREFIX + type.key() + ":" + (epochMs / 1000);
        redis.opsForValue().increment(bucket);
        redis.expire(bucket, BUCKET_TTL);
        redis.opsForValue().increment(PREFIX + type.key() + ":total");
        if (amountPaise > 0) {
            redis.opsForValue().increment(PREFIX + "revenue:total", amountPaise);
        }
    }

    /** Keeps the FIRST time a product sold out. */
    public void soldOut(Long productId, long epochMs) {
        redis.opsForHash().putIfAbsent(PREFIX + "soldout", String.valueOf(productId), String.valueOf(epochMs));
    }

    public MetricsSnapshot snapshot(long nowMs) {
        long nowSecond = nowMs / 1000;
        Map<String, List<Long>> perSecond = new LinkedHashMap<>();
        Map<String, Long> totals = new LinkedHashMap<>();
        for (MetricType type : MetricType.values()) {
            List<String> keys = new ArrayList<>(WINDOW_SECONDS);
            for (long second = nowSecond - WINDOW_SECONDS + 1; second <= nowSecond; second++) {
                keys.add(PREFIX + type.key() + ":" + second);
            }
            List<String> values = redis.opsForValue().multiGet(keys);
            perSecond.put(type.key(), values == null
                    ? List.of()
                    : values.stream().map(MetricsStore::toLong).toList());
            totals.put(type.key(), toLong(redis.opsForValue().get(PREFIX + type.key() + ":total")));
        }
        Map<Long, Long> soldOut = new TreeMap<>();
        redis.opsForHash().entries(PREFIX + "soldout").forEach((product, at) ->
                soldOut.put(Long.valueOf(product.toString()), Long.valueOf(at.toString())));
        return new MetricsSnapshot(nowMs, WINDOW_SECONDS, perSecond, totals,
                toLong(redis.opsForValue().get(PREFIX + "revenue:total")), soldOut);
    }

    private static long toLong(String value) {
        return value == null ? 0 : Long.parseLong(value);
    }
}