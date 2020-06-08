package com.dvl.grafana;

import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Service
public class ServiceConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ServiceConsumer.class);

    private final PropertiesConfiguration propertiesConfiguration;

    private final MonitoredThreadPoolExecutor threadPoolExecutor;

    private final AtomicReference<RateLimiter> rateLimiter = new AtomicReference<>();
    private int currentRate;

    @Autowired
    public ServiceConsumer(PropertiesConfiguration propertiesConfiguration, ServiceProvider serviceProvider, ApplicationMetrics applicationMetrics) {
        this.propertiesConfiguration = propertiesConfiguration;
        this.threadPoolExecutor = new MonitoredThreadPoolExecutor(4, 8, 500L, MILLISECONDS,  new ArrayBlockingQueue<>(500), new ThreadFactoryBuilder().build(), applicationMetrics.getMetricRegistry(), "poolname");

        updateRateLimiterAndMaxDelay();
        newScheduledThreadPool(1).scheduleWithFixedDelay(this::updateRateLimiterAndMaxDelay, 0, 2, SECONDS);

        Stream.iterate(0, i -> i+1).forEach(s -> {
            rateLimiter.get().acquire();
            threadPoolExecutor.execute(serviceProvider::createSomeString);
        });
    }

    private void updateRateLimiterAndMaxDelay() {
        int rateLimit = propertiesConfiguration.getInt("ratelimit", 1);
        if (rateLimit < 1) {
            rateLimit = 1;
        }
        if (rateLimit != currentRate) {
            logger.info("Updating the current rate from {} to {}", currentRate, rateLimit);
            rateLimiter.getAndSet(RateLimiter.create(rateLimit));
            currentRate = rateLimit;
        }

        int maxDelay = propertiesConfiguration.getInt("max-delay-millis", 100);
        if (maxDelay < 1) {
            maxDelay = 1;
        }
        if (maxDelay != threadPoolExecutor.getMaximumDelay()) {
            logger.info("Updating the maximum delay from {} to {}", threadPoolExecutor.getMaximumDelay(), maxDelay);
            threadPoolExecutor.setMaximumDelay(maxDelay);
        }
    }
}
