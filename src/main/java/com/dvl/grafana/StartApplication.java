package com.dvl.grafana;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

public class StartApplication {

    private static final Logger log = LoggerFactory.getLogger(StartApplication.class);

    private static final MetricRegistry registry = new MetricRegistry();

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private static final Meter meter = registry.meter(StartApplication.class.getName() + ".meter");

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(StartApplication::stop));

        final Graphite graphite = new Graphite(new InetSocketAddress("localhost", 2003));
        final GraphiteReporter reporter = GraphiteReporter.forRegistry(registry)
                .prefixedWith("com.dvl.grafana")
                .convertRatesTo(SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);

        reporter.start(10, SECONDS);
        executor.scheduleAtFixedRate(updateMetrics(), 10, 10, SECONDS);
    }

    private static Runnable updateMetrics() {
        return () -> meter.mark();
    }

    private static void stop() {
        log.info("Stopping the application");
        executor.shutdownNow();
        log.info("Application stopped");
    }

}