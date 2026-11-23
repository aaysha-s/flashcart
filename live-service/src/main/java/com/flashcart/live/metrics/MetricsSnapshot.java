package com.flashcart.live.metrics;

import java.util.List;
import java.util.Map;

public record MetricsSnapshot(
        long generatedAtEpochMs,
        int windowSeconds,
        Map<String, List<Long>> perSecond,
        Map<String, Long> totals,
        long revenuePaise,
        Map<Long, Long> soldOutAtEpochMs
) {}