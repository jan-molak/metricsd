package com.smartcodeltd.metrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;

// As MetricRegistry can't be easily sub-classed or decorated we get this "lovely" copy'n'paste inheritance here.
public class MetricRegistry extends com.codahale.metrics.MetricRegistry {
    private final Timestamp timestamp;

    public static String name(String name, String... names) {
        final StringBuilder builder = new StringBuilder();
        append(builder, name);
        if (names != null) {
            for (String s : names) {
                append(builder, s);
            }
        }
        return builder.toString();
    }

    public static String name(Class<?> klass, String... names) {
        return name(klass.getName(), names);
    }

    private static void append(StringBuilder builder, String part) {
        if (part != null && !part.isEmpty()) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(part);
        }
    }

    private final ConcurrentMap<String, TimestampedMetric> metrics;
    private final List<MetricRegistryListener> listeners;

    public MetricRegistry() {
        this(new Timestamp());
    }

    public MetricRegistry(Timestamp timestamp) {
        this.metrics   = new ConcurrentHashMap<String, TimestampedMetric>();
        this.listeners = new CopyOnWriteArrayList<MetricRegistryListener>();
        this.timestamp = timestamp;
    }

    @SuppressWarnings("unchecked")
    public <T extends TimestampedMetric> T register(String name, T metric) throws IllegalArgumentException {
        if (metric instanceof MetricSet) {
            registerAll(name, (MetricSet) metric);
        } else {
            final Metric existing = metrics.putIfAbsent(name, metric);
            if (existing == null) {
                onMetricAdded(name, metric);
            } else {
                throw new IllegalArgumentException("A metric named " + name + " already exists");
            }
        }
        return metric;
    }

    @Override public void registerAll(MetricSet metrics) throws IllegalArgumentException {
        registerAll(null, metrics);
    }

    @Override public Counter counter(String name) {
        return getOrAdd(name, MetricBuilder.COUNTERS);
    }

    public com.smartcodeltd.metrics.timestamped.Gauge gauge(String name) {
        return getOrAdd(name, MetricBuilder.GAUGES);
    }

    @Override public Histogram histogram(String name) {
        return getOrAdd(name, MetricBuilder.HISTOGRAMS);
    }

    @Override public Meter meter(String name) {
        return getOrAdd(name, MetricBuilder.METERS);
    }

    @Override public Timer timer(String name) {
        return getOrAdd(name, MetricBuilder.TIMERS);
    }

    @Override public boolean remove(String name) {
        final Metric metric = metrics.remove(name);
        if (metric != null) {
            onMetricRemoved(name, metric);
            return true;
        }
        return false;
    }

    @Override public void removeMatching(MetricFilter filter) {
        for (Map.Entry<String, TimestampedMetric> entry : metrics.entrySet()) {
            if (filter.matches(entry.getKey(), entry.getValue())) {
                remove(entry.getKey());
            }
        }
    }

    @Override public void addListener(MetricRegistryListener listener) {
        listeners.add(listener);

        for (Map.Entry<String, TimestampedMetric> entry : metrics.entrySet()) {
            notifyListenerOfAddedMetric(listener, entry.getValue(), entry.getKey());
        }
    }

    @Override public void removeListener(MetricRegistryListener listener) {
        listeners.remove(listener);
    }

    @Override public SortedSet<String> getNames() {
        return Collections.unmodifiableSortedSet(new TreeSet<String>(metrics.keySet()));
    }

    @Override public SortedMap<String, Gauge> getGauges() {
        return getGauges(MetricFilter.ALL);
    }

    @Override public SortedMap<String, Gauge> getGauges(MetricFilter filter) {
        return getMetrics(Gauge.class, filter);
    }

    @Override public SortedMap<String, Counter> getCounters() {
        return getCounters(MetricFilter.ALL);
    }

    @Override public SortedMap<String, Counter> getCounters(MetricFilter filter) {
        return getMetrics(Counter.class, filter);
    }

    @Override public SortedMap<String, Histogram> getHistograms() {
        return getHistograms(MetricFilter.ALL);
    }

    @Override public SortedMap<String, Histogram> getHistograms(MetricFilter filter) {
        return getMetrics(Histogram.class, filter);
    }

    @Override public SortedMap<String, Meter> getMeters() {
        return getMeters(MetricFilter.ALL);
    }

    @Override public SortedMap<String, Meter> getMeters(MetricFilter filter) {
        return getMetrics(Meter.class, filter);
    }

    @Override public SortedMap<String, Timer> getTimers() {
        return getTimers(MetricFilter.ALL);
    }

    @Override public SortedMap<String, Timer> getTimers(MetricFilter filter) {
        return getMetrics(Timer.class, filter);
    }

    @SuppressWarnings("unchecked")
    private <T extends TimestampedMetric> T getOrAdd(String name, MetricBuilder<T> builder) {
        final Metric metric = metrics.get(name);
        if (builder.isInstance(metric)) {
            return (T) metric;
        } else if (metric == null) {
            try {
                return register(name, builder.newMetric(timestamp));
            } catch (IllegalArgumentException e) {
                final Metric added = metrics.get(name);
                if (builder.isInstance(added)) {
                    return (T) added;
                }
            }
        }
        throw new IllegalArgumentException(name + " is already used for a different type of metric");
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> SortedMap<String, T> getMetrics(Class<T> klass, MetricFilter filter) {
        final TreeMap<String, T> timers = new TreeMap<String, T>();
        for (Map.Entry<String, TimestampedMetric> entry : metrics.entrySet()) {
            if (klass.isInstance(entry.getValue()) && filter.matches(entry.getKey(),
                    entry.getValue())) {
                timers.put(entry.getKey(), (T) entry.getValue());
            }
        }
        return Collections.unmodifiableSortedMap(timers);
    }

    private void onMetricAdded(String name, Metric metric) {
        for (MetricRegistryListener listener : listeners) {
            notifyListenerOfAddedMetric(listener, metric, name);
        }
    }

    private void notifyListenerOfAddedMetric(MetricRegistryListener listener, Metric metric, String name) {
        if (metric instanceof Gauge) {
            listener.onGaugeAdded(name, (Gauge<?>) metric);
        } else if (metric instanceof Counter) {
            listener.onCounterAdded(name, (Counter) metric);
        } else if (metric instanceof Histogram) {
            listener.onHistogramAdded(name, (Histogram) metric);
        } else if (metric instanceof Meter) {
            listener.onMeterAdded(name, (Meter) metric);
        } else if (metric instanceof Timer) {
            listener.onTimerAdded(name, (Timer) metric);
        } else {
            throw new IllegalArgumentException("Unknown metric type: " + metric.getClass());
        }
    }

    private void onMetricRemoved(String name, Metric metric) {
        for (MetricRegistryListener listener : listeners) {
            notifyListenerOfRemovedMetric(name, metric, listener);
        }
    }

    private void notifyListenerOfRemovedMetric(String name, Metric metric, MetricRegistryListener listener) {
        if (metric instanceof Gauge) {
            listener.onGaugeRemoved(name);
        } else if (metric instanceof Counter) {
            listener.onCounterRemoved(name);
        } else if (metric instanceof Histogram) {
            listener.onHistogramRemoved(name);
        } else if (metric instanceof Meter) {
            listener.onMeterRemoved(name);
        } else if (metric instanceof Timer) {
            listener.onTimerRemoved(name);
        } else {
            throw new IllegalArgumentException("Unknown metric type: " + metric.getClass());
        }
    }

    private void registerAll(String prefix, MetricSet metrics) throws IllegalArgumentException {
        for (Map.Entry<String, Metric> entry : metrics.getMetrics().entrySet()) {
            if (entry.getValue() instanceof MetricSet) {
                registerAll(name(prefix, entry.getKey()), (MetricSet) entry.getValue());
            } else {
                register(name(prefix, entry.getKey()), entry.getValue());
            }
        }
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.<String, Metric>unmodifiableMap(metrics);
    }

    private interface MetricBuilder<T extends TimestampedMetric> {
        MetricBuilder<com.smartcodeltd.metrics.timestamped.Counter> COUNTERS = new MetricBuilder<com.smartcodeltd.metrics.timestamped.Counter>() {
            @Override
            public com.smartcodeltd.metrics.timestamped.Counter newMetric(Timestamp timestamp) {
                return new com.smartcodeltd.metrics.timestamped.Counter(timestamp);
            }

            @Override
            public boolean isInstance(Metric metric) {
                return com.smartcodeltd.metrics.timestamped.Counter.class.isInstance(metric);
            }
        };

        MetricBuilder<com.smartcodeltd.metrics.timestamped.Gauge> GAUGES = new MetricBuilder<com.smartcodeltd.metrics.timestamped.Gauge>() {
            @Override
            public com.smartcodeltd.metrics.timestamped.Gauge newMetric(Timestamp timestamp) {
                return new com.smartcodeltd.metrics.timestamped.Gauge(timestamp);
            }

            @Override
            public boolean isInstance(Metric metric) {
                return com.smartcodeltd.metrics.timestamped.Gauge.class.isInstance(metric);
            }
        };

        MetricBuilder<com.smartcodeltd.metrics.timestamped.Histogram> HISTOGRAMS = new MetricBuilder<com.smartcodeltd.metrics.timestamped.Histogram>() {
            @Override
            public com.smartcodeltd.metrics.timestamped.Histogram newMetric(Timestamp timestamp) {
                return new com.smartcodeltd.metrics.timestamped.Histogram(timestamp, new ExponentiallyDecayingReservoir());
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Histogram.class.isInstance(metric);
            }
        };

        MetricBuilder<com.smartcodeltd.metrics.timestamped.Meter> METERS = new MetricBuilder<com.smartcodeltd.metrics.timestamped.Meter>() {
            @Override
            public com.smartcodeltd.metrics.timestamped.Meter newMetric(Timestamp timestamp) {
                return new com.smartcodeltd.metrics.timestamped.Meter(timestamp);
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Meter.class.isInstance(metric);
            }
        };

        MetricBuilder<com.smartcodeltd.metrics.timestamped.Timer> TIMERS = new MetricBuilder<com.smartcodeltd.metrics.timestamped.Timer>() {
            @Override
            public com.smartcodeltd.metrics.timestamped.Timer newMetric(Timestamp timestamp) {
                return new com.smartcodeltd.metrics.timestamped.Timer(timestamp);
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Timer.class.isInstance(metric);
            }
        };

        T newMetric(Timestamp ts);

        boolean isInstance(Metric metric);
    }
}