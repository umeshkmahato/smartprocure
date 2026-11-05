# SmartProcure — JMH Benchmarks (Phase 11)

Standalone Maven module that measures **Virtual Threads vs Platform Threads**
throughput and latency under simulated I/O-bound load — the dominant workload
pattern in the SmartProcure backend (DB queries, Kafka produce, HTTP calls).

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |

## Build

```bash
cd benchmarks
mvn package -q
```

Produces `target/benchmarks.jar` (shaded fat JAR with JMH Main-Class).

## Run

```bash
# Full benchmark — all parameter combinations, JSON report
java -jar target/benchmarks.jar -rf json -rff results/results.json

# Quick smoke run (1 warmup, 1 measurement iteration)
java -jar target/benchmarks.jar -wi 1 -i 1 -f 1

# Run only a specific concurrency level
java -jar target/benchmarks.jar -p concurrency=500

# List available benchmarks
java -jar target/benchmarks.jar -l
```

## Parameters

| Parameter | Values | Description |
|---|---|---|
| `concurrency` | 50, 200, 500 | Tasks submitted per operation |
| `ioDelayMs` | 20, 50 | Simulated I/O wait per task (ms) |

## Expected results

| Scenario | Platform Threads | Virtual Threads | Uplift |
|---|---|---|---|
| concurrency=50, io=20ms | baseline | ≈ parity | — |
| concurrency=200, io=50ms | pool saturation starts | scales freely | ~1.5–2× |
| concurrency=500, io=50ms | thread starvation | scales freely | ~3–5× |

At `concurrency=500` the fixed platform thread pool (200 threads) is exhausted;
tasks queue, throughput degrades, and tail latency spikes. Virtual threads park
their carrier thread instead of blocking it, so throughput scales linearly.

## Reading the JSON report

```bash
# Pretty-print the results
cat results/results.json | python3 -m json.tool | less
```

Key fields per benchmark entry:

```json
{
  "benchmark": "...VirtualVsPlatformThreadBenchmark.virtualThreads",
  "params": { "concurrency": "500", "ioDelayMs": "50" },
  "primaryMetric": {
    "score": 94.3,
    "scoreUnit": "ops/s"
  },
  "secondaryMetrics": {
    "·p99": { "score": 285.4, "scoreUnit": "ms/op" }
  }
}
```
