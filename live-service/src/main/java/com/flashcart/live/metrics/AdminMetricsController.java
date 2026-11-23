package com.flashcart.live.metrics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/live/admin")
public class AdminMetricsController {

    private final MetricsStore metrics;

    public AdminMetricsController(MetricsStore metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/metrics")
    public MetricsSnapshot metrics() {
        return metrics.snapshot(System.currentTimeMillis());
    }
}