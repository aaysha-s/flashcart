package com.flashcart.live.metrics;

import java.util.Locale;

public enum MetricType {
    PLACED,
    CONFIRMED,
    PAYMENT_FAILED,
    REJECTED;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}