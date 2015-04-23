package com.smartcodeltd.metrics.timestamped;

import com.smartcodeltd.metrics.Timestamp;
import com.smartcodeltd.metrics.TimestampedMetric;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Timer
        extends com.codahale.metrics.Timer
        implements TimestampedMetric
{
    public Timer(Timestamp timestamp) {
        super();
        this.timestamp = timestamp;
    }

    @Override public void update(long duration, TimeUnit unit) {
        super.update(duration, unit);
        markUpdate();
    }

    // -- private

    private final AtomicLong lastUpdate = new AtomicLong();
    private final Timestamp timestamp;

    private void markUpdate() {
        lastUpdate.set(timestamp.now());
    }

    @Override
    public boolean updatedWithin(long period) {
        return true;    // todo: determine when to say yes and when to say no
    }
}