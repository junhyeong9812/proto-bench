package com.protobench.api.benchmark

import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

data class RequestMetric(
    val latencyNanos: Long,
    val responseBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
)

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
 *
 * 벤치마크 시작/종료 및 요청별 메트릭을 수집한다.
 * JMX MXBean을 활용하여 힙 메모리, GC 정보도 수집한다.
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

    private val memoryMXBean = ManagementFactory.getMemoryMXBean()
    private val gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans()

    private var startHeapUsed: Long = 0
    private var startGcCount: Long = 0
    private var startGcTime: Long = 0
    private var peakHeapUsed: Long = 0

    /**
     * 벤치마크 시작
     *
     * @param protocol 테스트 프로토콜 이름 (HTTP/JSON, gRPC/Unary 등)
     * @param testName 테스트 식별 이름
     * @return 시작 상태 정보
     */
    fun start(protocol: String, testName: String = "default"): Map<String, Any> {
        if (isRunning.getAndSet(true)) {
            return mapOf("error" to "Benchmark already running")
        }

        metrics.clear()
        successCount.set(0)
        failCount.set(0)

        this.protocol = protocol
        this.testName = testName
        this.startTime = System.currentTimeMillis()

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
     *
     * @param latencyNanos 요청 지연 시간 (나노초)
     * @param responseBytes 응답 크기 (바이트)
     * @param success 성공 여부
     */
    fun record(latencyNanos: Long, responseBytes: Long, success: Boolean) {
        if (!isRunning.get()) return

        if (success) {
            successCount.incrementAndGet()
            metrics.add(RequestMetric(latencyNanos, responseBytes))
        } else {
            failCount.incrementAndGet()
        }

        val currentHeap = memoryMXBean.heapMemoryUsage.used
        if (currentHeap > peakHeapUsed) {
            peakHeapUsed = currentHeap
        }
    }

    /**
     * 벤치마크 종료 및 결과 계산
     *
     * @return BenchmarkResult (null이면 벤치마크가 실행 중이 아님)
     */
    fun end(): BenchmarkResult? {
        if (!isRunning.getAndSet(false)) {
            return null
        }

        endTime = System.currentTimeMillis()
        val durationMs = endTime - startTime

        val endHeapUsed = memoryMXBean.heapMemoryUsage.used
        val endGcCount = gcMXBeans.sumOf { it.collectionCount }
        val endGcTime = gcMXBeans.sumOf { it.collectionTime }

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
     * 현재 벤치마크 상태 조회
     *
     * @return 상태 정보 (실행 중 여부, 요청 수 등)
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