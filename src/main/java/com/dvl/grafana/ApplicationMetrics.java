package com.dvl.grafana;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Service
public class ApplicationMetrics {

    private final MetricRegistry metricRegistry = new MetricRegistry();

    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public ApplicationMetrics() {
        final Graphite graphite = new Graphite(new InetSocketAddress("localhost", 2003));
        final GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
                .convertRatesTo(SECONDS)
                .convertDurationsTo(MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);

        reporter.start(5, SECONDS);
    }
}
