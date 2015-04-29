package com.smartcodeltd.statsd.timestamped;

import com.codahale.metrics.Reservoir;
import com.smartcodeltd.statsd.Timestamp;
import com.smartcodeltd.statsd.TimestampedMetric;

import java.util.concurrent.atomic.AtomicLong;

public class Histogram
        extends com.codahale.metrics.Histogram
        implements TimestampedMetric
{
    public Histogram(Timestamp timestamp, Reservoir reservoir) {
        super(reservoir);

        this.timestamp = timestamp;
    }

    @Override public void update(int value) {
        super.update(value);
        markUpdate();
    }


    @Override public void update(long value) {
        super.update(value);
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