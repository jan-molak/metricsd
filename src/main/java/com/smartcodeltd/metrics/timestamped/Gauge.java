package com.smartcodeltd.metrics.timestamped;

import com.smartcodeltd.metrics.Timestamp;
import com.smartcodeltd.metrics.TimestampedMetric;

import java.util.concurrent.atomic.AtomicLong;

public class Gauge implements
        com.codahale.metrics.Gauge,
        TimestampedMetric
{
    public Gauge(Timestamp timestamp) {
        this.timestamp = timestamp;
        this.value     = new AtomicLong();
    }

    public void set(long newValue) {
        value.set(newValue);
        markUpdate();
    }

    @Override public boolean updatedWithin(long period) {
        return timestamp.isWithin(period, lastUpdate);
    }

    // -- private

    private final AtomicLong lastUpdate = new AtomicLong();
    private final AtomicLong value;

    private final Timestamp  timestamp;

    private void markUpdate() {
        lastUpdate.set(timestamp.now());
    }

    @Override
    public Long getValue() {
        return value.get();
    }
}