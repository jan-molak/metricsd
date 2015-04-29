package com.smartcodeltd.statsd;

import com.codahale.metrics.Metric;

public interface TimestampedMetric extends Metric {
    boolean updatedWithin(long period);
}