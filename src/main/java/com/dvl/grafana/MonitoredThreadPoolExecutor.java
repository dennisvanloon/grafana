package com.dvl.grafana;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class MonitoredThreadPoolExecutor extends ThreadPoolExecutor {
    private final MetricRegistry metricRegistry;

    private final String metricsPrefix;

    private final ThreadLocal<Timer.Context> taskExecutionTimer = new ThreadLocal<>();

    Random random = new Random();

    private int maximumDelay=100;

    public MonitoredThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, MetricRegistry metricRegistry, String poolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.metricRegistry = metricRegistry;
        this.metricsPrefix = MetricRegistry.name(getClass(), poolName);

        setRejectedExecutionHandler((r, executor) -> metricRegistry.counter(MetricRegistry.name(metricsPrefix, "rejected-tasks")).inc());
        registerGauges();
    }

    private void registerGauges() {
        metricRegistry.register(MetricRegistry.name(metricsPrefix, "corePoolSize"), (Gauge<Integer>) this::getCorePoolSize);
        metricRegistry.register(MetricRegistry.name(metricsPrefix, "activeThreads"), (Gauge<Integer>) this::getActiveCount);
        metricRegistry.register(MetricRegistry.name(metricsPrefix, "maxPoolSize"), (Gauge<Integer>) this::getMaximumPoolSize);
        metricRegistry.register(MetricRegistry.name(metricsPrefix, "queueSize"), (Gauge<Integer>) () -> getQueue().size());
    }

    @Override
    protected void beforeExecute(Thread thread, Runnable task) {
        super.beforeExecute(thread, task);
        Timer timer = metricRegistry.timer(MetricRegistry.name(metricsPrefix, "task-execution"));
        taskExecutionTimer.set(timer.time());
        sleepUninterruptibly(random.nextInt(maximumDelay), MILLISECONDS);
    }
    @Override
    protected void afterExecute(Runnable task, Throwable throwable) {
        Timer.Context context = taskExecutionTimer.get();
        context.stop();
        super.afterExecute(task, throwable);

        if (throwable != null) {
            metricRegistry.counter(MetricRegistry.name(metricsPrefix, "failed-tasks")).inc();
        } else {
            metricRegistry.counter(MetricRegistry.name(metricsPrefix, "successful-tasks")).inc();
        }
    }

    public int getMaximumDelay() {
        return maximumDelay;
    }

    public void setMaximumDelay(int maximumDelay) {
        this.maximumDelay = maximumDelay;
    }
}
