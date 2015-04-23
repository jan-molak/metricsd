package com.smartcodeltd.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

/**
 * Ignores metrics older than its attention span
 */
public class ForgetfulFilter implements MetricFilter {
    private final long memorySpan;

    public ForgetfulFilter(long memorySpanInSeconds) {
        this.memorySpan = memorySpanInSeconds * 1000;
    }

    @Override
    public boolean matches(String name, Metric metric) {
        return ((TimestampedMetric) metric).updatedWithin(memorySpan);
    }
}