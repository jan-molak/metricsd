package com.smartcodeltd.statsd;

import java.util.concurrent.atomic.AtomicLong;

public class Timestamp {
    public long now() {
        return System.currentTimeMillis();
    }

    public boolean isWithin(long period, AtomicLong timestamp) {
        return timestamp.get() > (now() - period);
    }
}