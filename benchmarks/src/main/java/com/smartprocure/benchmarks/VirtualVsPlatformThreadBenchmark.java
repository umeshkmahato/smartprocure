package com.smartprocure.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * JMH benchmark: Virtual Threads vs Platform Threads.
 *
 * <p>Simulates the SmartProcure backend's most common workload pattern:
 * many concurrent I/O-bound tasks (DB queries, HTTP calls, Kafka produce)
 * each blocking for a realistic duration.
 *
 * <h2>What is measured</h2>
 * <ul>
 *   <li><b>throughput</b> – operations per second at high concurrency</li>
 *   <li><b>average latency</b> – mean time per operation (ms)</li>
 *   <li><b>p99 sample time</b> – tail latency under load</li>
 * </ul>
 *
 * <h2>How to run</h2>
 * <pre>
 *   cd benchmarks
 *   mvn package -q
 *   java -jar target/benchmarks.jar -rf json -rff results/results.json
 * </pre>
 *
 * <h2>Expected outcome</h2>
 * Virtual threads scale linearly with I/O concurrency; platform threads
 * stall once the fixed pool is exhausted (thread starvation), showing
 * dramatically lower throughput at CONCURRENCY ≥ pool size.
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "-Xms256m", "-Xmx512m"
})
public class VirtualVsPlatformThreadBenchmark {

    // ── Benchmark parameters ───────────────────────────────────────────────────

    /**
     * Number of concurrent tasks submitted per operation.
     * At CONCURRENCY=500 the platform thread pool (200 threads) is saturated;
     * virtual threads continue to scale.
     */
    @Param({"50", "200", "500"})
    public int concurrency;

    /**
     * Simulated I/O wait per task (milliseconds).
     * Models a typical DB round-trip or Kafka produce acknowledgement.
     */
    @Param({"20", "50"})
    public int ioDelayMs;

    // ── Executors under test ──────────────────────────────────────────────────

    private ExecutorService platformThreadPool;
    private ExecutorService virtualThreadPool;

    @Setup(Level.Trial)
    public void setUp() {
        // Platform thread pool — fixed at 200 (mirrors Tomcat default)
        platformThreadPool = Executors.newFixedThreadPool(200,
                Thread.ofPlatform().name("platform-", 0).factory());

        // Virtual thread executor — unbounded; each task gets its own VT
        virtualThreadPool = Executors.newVirtualThreadPerTaskExecutor();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws InterruptedException {
        shutdownGracefully(platformThreadPool);
        shutdownGracefully(virtualThreadPool);
    }

    // ── Benchmarks ────────────────────────────────────────────────────────────

    /**
     * Platform threads: submit {@code concurrency} blocking tasks to a
     * fixed-size pool. When concurrency > pool size, tasks queue and
     * throughput degrades.
     */
    @Benchmark
    public void platformThreads(Blackhole bh) throws Exception {
        bh.consume(runConcurrentIoTasks(platformThreadPool));
    }

    /**
     * Virtual threads: submit {@code concurrency} blocking tasks — each
     * gets its own virtual thread. No queuing; carrier threads park instead
     * of blocking, allowing OS threads to serve other VTs.
     */
    @Benchmark
    public void virtualThreads(Blackhole bh) throws Exception {
        bh.consume(runConcurrentIoTasks(virtualThreadPool));
    }

    // ── Shared workload ───────────────────────────────────────────────────────

    /**
     * Submits {@code concurrency} tasks to {@code executor}, each sleeping
     * {@code ioDelayMs} ms (simulating blocking I/O), then waits for all
     * to complete. Returns the total number of completed tasks.
     */
    private int runConcurrentIoTasks(ExecutorService executor) throws Exception {
        List<Future<Integer>> futures = new ArrayList<>(concurrency);

        for (int i = 0; i < concurrency; i++) {
            final int taskId = i;
            futures.add(executor.submit(() -> {
                simulateIo(ioDelayMs);
                return taskId;
            }));
        }

        int completed = 0;
        for (Future<Integer> f : futures) {
            f.get(); // propagate any exception
            completed++;
        }
        return completed;
    }

    /**
     * Simulates a blocking I/O operation (DB query, HTTP call, Kafka ack).
     * Uses {@link Thread#sleep} so virtual threads park their carrier thread
     * rather than blocking it.
     */
    private static void simulateIo(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void shutdownGracefully(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

    // ── Programmatic runner (optional — for IDE execution) ────────────────────

    /**
     * Run directly from an IDE or via {@code java -cp ... ...BenchmarkMain}.
     * Produces a JSON report at {@code benchmarks/results/results.json}.
     */
    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(VirtualVsPlatformThreadBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result("results/results.json")
                .forks(1)
                .build();
        new Runner(opts).run();
    }
}
