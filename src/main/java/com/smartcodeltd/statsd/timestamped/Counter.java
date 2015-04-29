package com.smartcodeltd.statsd.timestamped;

import com.smartcodeltd.statsd.Timestamp;
import com.smartcodeltd.statsd.TimestampedMetric;

import java.util.concurrent.atomic.AtomicLong;

public class Counter
        extends com.codahale.metrics.Counter
        implements TimestampedMetric
{
    public Counter(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override public void inc()       {
        super.inc();
        markUpdate();
    }

    @Override public void inc(long n) {
        super.inc(n);
        markUpdate();
    }

    @Override public void dec() {
        super.dec(1);
        markUpdate();
    }

    @Override public void dec(long n) {
        super.dec(n);
        markUpdate();
    }

    @Override public boolean updatedWithin(long period) {
        return timestamp.isWithin(period, lastUpdate);
    }

    // -- private

    private final AtomicLong lastUpdate = new AtomicLong();
    private final Timestamp timestamp;

    private void markUpdate() {
        lastUpdate.set(timestamp.now());
    }
}