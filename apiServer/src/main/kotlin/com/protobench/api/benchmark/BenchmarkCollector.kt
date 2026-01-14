package com.protobench.api.benchmark

import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 개별 요청 메트릭
 */
data class RequestMetric(
    val latencyNanos: Long,
    val responseBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 벤치마크 결과
 */
data class BenchmarkResult(
    val protocol: String,
    val testName: String,
    val durationMs: Long,
    val totalRequests: Long,
    val successRequests: Long,
    val failedRequests: Long,
    val throughputRps: Double,
    val latency: LatencyStats,
    val serverMetrics: ServerMetrics,
    val dataTransfer: DataTransferStats
)

data class LatencyStats(
    val avgMs: Double,
    val minMs: Double,
    val maxMs: Double,
    val p50Ms: Double,
    val p95Ms: Double,
    val p99Ms: Double
)

data class ServerMetrics(
    val startHeapMb: Double,
    val endHeapMb: Double,
    val peakHeapMb: Double,
    val gcCount: Long,
    val gcTimeMs: Long
)

data class DataTransferStats(
    val totalBytes: Long,
    val avgResponseBytes: Double
)

/**
 * 벤치마크 메트릭 수집기
 */
@Component
class BenchmarkCollector {

    private val isRunning = AtomicBoolean(false)
    private val metrics = ConcurrentLinkedQueue<RequestMetric>()
    private val successCount = AtomicLong(0)
    private val failCount = AtomicLong(0)

    private var startTime: Long = 0
    private var endTime: Long = 0
    private var protocol: String = ""
    private var testName: String = ""

    // JMX MXBeans
    private val memoryMXBean = ManagementFactory.getMemoryMXBean()
    private val gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans()

    private var startHeapUsed: Long = 0
    private var startGcCount: Long = 0
    private var startGcTime: Long = 0
    private var peakHeapUsed: Long = 0

    /**
     * 벤치마크 시작
     */
    fun start(protocol: String, testName: String = "default"): Map<String, Any> {
        if (isRunning.getAndSet(true)) {
            return mapOf("error" to "Benchmark already running")
        }

        // 초기화
        metrics.clear()
        successCount.set(0)
        failCount.set(0)

        this.protocol = protocol
        this.testName = testName
        this.startTime = System.currentTimeMillis()

        // 시작 시점 메트릭
        startHeapUsed = memoryMXBean.heapMemoryUsage.used
        peakHeapUsed = startHeapUsed
        startGcCount = gcMXBeans.sumOf { it.collectionCount }
        startGcTime = gcMXBeans.sumOf { it.collectionTime }

        return mapOf(
            "status" to "started",
            "protocol" to protocol,
            "testName" to testName,
            "startTime" to startTime
        )
    }

    /**
     * 요청 메트릭 기록
     */
    fun record(latencyNanos: Long, responseBytes: Long, success: Boolean) {
        if (!isRunning.get()) return

        if (success) {
            successCount.incrementAndGet()
            metrics.add(RequestMetric(latencyNanos, responseBytes))
        } else {
            failCount.incrementAndGet()
        }

        // Peak 힙 메모리 추적
        val currentHeap = memoryMXBean.heapMemoryUsage.used
        if (currentHeap > peakHeapUsed) {
            peakHeapUsed = currentHeap
        }
    }

    /**
     * 벤치마크 종료 및 결과 계산
     */
    fun end(): BenchmarkResult? {
        if (!isRunning.getAndSet(false)) {
            return null
        }

        endTime = System.currentTimeMillis()
        val durationMs = endTime - startTime

        // 종료 시점 메트릭
        val endHeapUsed = memoryMXBean.heapMemoryUsage.used
        val endGcCount = gcMXBeans.sumOf { it.collectionCount }
        val endGcTime = gcMXBeans.sumOf { it.collectionTime }

        // 레이턴시 계산
        val latencies = metrics.map { it.latencyNanos / 1_000_000.0 }.sorted()
        val totalBytes = metrics.sumOf { it.responseBytes }

        val latencyStats = if (latencies.isNotEmpty()) {
            LatencyStats(
                avgMs = latencies.average(),
                minMs = latencies.first(),
                maxMs = latencies.last(),
                p50Ms = percentile(latencies, 50.0),
                p95Ms = percentile(latencies, 95.0),
                p99Ms = percentile(latencies, 99.0)
            )
        } else {
            LatencyStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        return BenchmarkResult(
            protocol = protocol,
            testName = testName,
            durationMs = durationMs,
            totalRequests = successCount.get() + failCount.get(),
            successRequests = successCount.get(),
            failedRequests = failCount.get(),
            throughputRps = if (durationMs > 0) successCount.get() * 1000.0 / durationMs else 0.0,
            latency = latencyStats,
            serverMetrics = ServerMetrics(
                startHeapMb = startHeapUsed / (1024.0 * 1024.0),
                endHeapMb = endHeapUsed / (1024.0 * 1024.0),
                peakHeapMb = peakHeapUsed / (1024.0 * 1024.0),
                gcCount = endGcCount - startGcCount,
                gcTimeMs = endGcTime - startGcTime
            ),
            dataTransfer = DataTransferStats(
                totalBytes = totalBytes,
                avgResponseBytes = if (successCount.get() > 0) totalBytes.toDouble() / successCount.get() else 0.0
            )
        )
    }

    /**
     * 현재 상태 확인
     */
    fun status(): Map<String, Any> {
        return mapOf(
            "isRunning" to isRunning.get(),
            "protocol" to protocol,
            "testName" to testName,
            "currentRequests" to (successCount.get() + failCount.get()),
            "successRequests" to successCount.get(),
            "failedRequests" to failCount.get(),
            "elapsedMs" to if (isRunning.get()) System.currentTimeMillis() - startTime else 0
        )
    }

    private fun percentile(sortedList: List<Double>, percentile: Double): Double {
        if (sortedList.isEmpty()) return 0.0
        val index = (percentile / 100.0 * sortedList.size).toInt()
            .coerceIn(0, sortedList.size - 1)
        return sortedList[index]
    }
}