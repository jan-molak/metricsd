package com.smartcodeltd.metrics.timestamped;

import com.smartcodeltd.metrics.Timestamp;
import com.smartcodeltd.metrics.TimestampedMetric;

import java.util.concurrent.atomic.AtomicLong;

public class Meter
        extends com.codahale.metrics.Meter
        implements TimestampedMetric
{
    public Meter(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override public void mark() {
        super.mark();
        markUpdate();
    }

    @Override public void mark(long n) {
        super.mark(n);
        markUpdate();
    }

    @Override
    public boolean updatedWithin(long period) {
        return true;    // todo: determine when to say yes and when to say no
    }

    // -- private

    private final AtomicLong lastUpdate = new AtomicLong();
    private final Timestamp timestamp;

    private void markUpdate() {
        lastUpdate.set(timestamp.now());
    }
}