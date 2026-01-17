package com.protobench.api.benchmark

import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import com.sun.management.OperatingSystemMXBean

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
    val gcTimeMs: Long,
    // CPU 관련 메트릭 추가
    val avgCpuUsagePercent: Double,
    val peakCpuUsagePercent: Double,
    val avgProcessCpuPercent: Double,
    val peakProcessCpuPercent: Double
)

data class DataTransferStats(
    val totalBytes: Long,
    val avgResponseBytes: Double
)

/**
 * 벤치마크 메트릭 수집기
 *
 * 벤치마크 시작/종료 및 요청별 메트릭을 수집한다.
 * JMX MXBean을 활용하여 힙 메모리, GC 정보, CPU 사용량도 수집한다.
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
    private val osMXBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean

    private var startHeapUsed: Long = 0
    private var startGcCount: Long = 0
    private var startGcTime: Long = 0
    private var peakHeapUsed: Long = 0

    // CPU 메트릭 저장용
    private val cpuUsageSamples = ConcurrentLinkedQueue<Double>()
    private val processCpuSamples = ConcurrentLinkedQueue<Double>()
    @Volatile private var peakCpuUsage: Double = 0.0
    @Volatile private var peakProcessCpu: Double = 0.0

    // CPU 샘플링 스레드
    @Volatile private var cpuSamplingThread: Thread? = null
    @Volatile private var stopSampling = false

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

        // CPU 메트릭 초기화
        cpuUsageSamples.clear()
        processCpuSamples.clear()
        peakCpuUsage = 0.0
        peakProcessCpu = 0.0
        stopSampling = false

        this.protocol = protocol
        this.testName = testName
        this.startTime = System.currentTimeMillis()

        startHeapUsed = memoryMXBean.heapMemoryUsage.used
        peakHeapUsed = startHeapUsed
        startGcCount = gcMXBeans.sumOf { it.collectionCount }
        startGcTime = gcMXBeans.sumOf { it.collectionTime }

        // CPU 샘플링 스레드 시작 (100ms 간격)
        cpuSamplingThread = Thread {
            while (!stopSampling && isRunning.get()) {
                try {
                    // 시스템 전체 CPU 사용률 (0.0 ~ 1.0)
                    val systemCpuLoad = osMXBean.cpuLoad
                    // 현재 JVM 프로세스의 CPU 사용률 (0.0 ~ 1.0)
                    val processCpuLoad = osMXBean.processCpuLoad

                    if (systemCpuLoad >= 0) {
                        val cpuPercent = systemCpuLoad * 100
                        cpuUsageSamples.add(cpuPercent)
                        if (cpuPercent > peakCpuUsage) {
                            peakCpuUsage = cpuPercent
                        }
                    }

                    if (processCpuLoad >= 0) {
                        val processPercent = processCpuLoad * 100
                        processCpuSamples.add(processPercent)
                        if (processPercent > peakProcessCpu) {
                            peakProcessCpu = processPercent
                        }
                    }

                    Thread.sleep(100) // 100ms 간격으로 샘플링
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    // 샘플링 실패 시 무시
                }
            }
        }.apply {
            name = "cpu-sampling-thread"
            isDaemon = true
            start()
        }

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

        // CPU 샘플링 스레드 종료
        stopSampling = true
        cpuSamplingThread?.interrupt()
        cpuSamplingThread?.join(1000) // 최대 1초 대기
        cpuSamplingThread = null

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

        // CPU 평균 계산
        val avgCpuUsage = if (cpuUsageSamples.isNotEmpty()) {
            cpuUsageSamples.average()
        } else 0.0

        val avgProcessCpu = if (processCpuSamples.isNotEmpty()) {
            processCpuSamples.average()
        } else 0.0

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
                gcTimeMs = endGcTime - startGcTime,
                avgCpuUsagePercent = avgCpuUsage,
                peakCpuUsagePercent = peakCpuUsage,
                avgProcessCpuPercent = avgProcessCpu,
                peakProcessCpuPercent = peakProcessCpu
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
        val currentCpuUsage = osMXBean.cpuLoad * 100
        val currentProcessCpu = osMXBean.processCpuLoad * 100

        return mapOf(
            "isRunning" to isRunning.get(),
            "protocol" to protocol,
            "testName" to testName,
            "currentRequests" to (successCount.get() + failCount.get()),
            "successRequests" to successCount.get(),
            "failedRequests" to failCount.get(),
            "elapsedMs" to if (isRunning.get()) System.currentTimeMillis() - startTime else 0,
            "currentCpuUsagePercent" to if (currentCpuUsage >= 0) currentCpuUsage else 0.0,
            "currentProcessCpuPercent" to if (currentProcessCpu >= 0) currentProcessCpu else 0.0
        )
    }

    private fun percentile(sortedList: List<Double>, percentile: Double): Double {
        if (sortedList.isEmpty()) return 0.0
        val index = (percentile / 100.0 * sortedList.size).toInt()
            .coerceIn(0, sortedList.size - 1)
        return sortedList[index]
    }
}