package com.protobench.api.service

import com.protobench.api.benchmark.BenchmarkCollector
import com.protobench.api.client.GrpcDataClient
import com.protobench.api.client.HttpDataClient
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.util.UUID

data class ApiResponse(
    val requestId: String,
    val protocol: String,
    val payloadSize: Int,
    val latencyMs: Double
)

/**
 * Phase 5: 복잡한 데이터 API 응답
 */
data class ComplexApiResponse(
    val requestId: String,
    val protocol: String,
    val complexity: String,
    val serializedSize: Int,
    val latencyMs: Double
)

/**
 * API 데이터 서비스
 *
 * HTTP/gRPC 클라이언트를 통해 dataServer에 요청하고,
 * 벤치마크 메트릭을 수집한다.
 *
 * Phase 5: 복잡한 데이터 구조 서비스 메서드 추가
 */
@Service
class ApiDataService(
    private val httpClient: HttpDataClient,
    private val grpcClient: GrpcDataClient,
    private val benchmarkCollector: BenchmarkCollector
) {

    /**
     * HTTP JSON 방식으로 데이터 요청
     *
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return ApiResponse
     */
    suspend fun getDataHttpJson(size: String = "1mb"): ApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val response = httpClient.getDataJson(requestId, size)
            val latencyNanos = System.nanoTime() - startNanos

            benchmarkCollector.record(latencyNanos, response.payloadSize.toLong(), true)

            ApiResponse(
                requestId = requestId,
                protocol = "HTTP/JSON",
                payloadSize = response.payloadSize,
                latencyMs = latencyNanos / 1_000_000.0
            )
        } catch (e: Exception) {
            val latencyNanos = System.nanoTime() - startNanos
            benchmarkCollector.record(latencyNanos, 0, false)
            throw e
        }
    }

    /**
     * HTTP Binary 방식으로 데이터 요청
     *
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return ApiResponse
     */
    suspend fun getDataHttpBinary(size: String = "1mb"): ApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val response = httpClient.getDataBinary(requestId, size)
            val latencyNanos = System.nanoTime() - startNanos

            benchmarkCollector.record(latencyNanos, response.size.toLong(), true)

            ApiResponse(
                requestId = requestId,
                protocol = "HTTP/Binary",
                payloadSize = response.size,
                latencyMs = latencyNanos / 1_000_000.0
            )
        } catch (e: Exception) {
            val latencyNanos = System.nanoTime() - startNanos
            benchmarkCollector.record(latencyNanos, 0, false)
            throw e
        }
    }

    /**
     * gRPC Unary 방식으로 데이터 요청
     *
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return ApiResponse
     */
    suspend fun getDataGrpc(size: String = "1mb"): ApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val response = grpcClient.getData(requestId, size)
            val latencyNanos = System.nanoTime() - startNanos

            benchmarkCollector.record(latencyNanos, response.payloadSize.toLong(), true)

            ApiResponse(
                requestId = requestId,
                protocol = "gRPC/Unary",
                payloadSize = response.payloadSize,
                latencyMs = latencyNanos / 1_000_000.0
            )
        } catch (e: Exception) {
            val latencyNanos = System.nanoTime() - startNanos
            benchmarkCollector.record(latencyNanos, 0, false)
            throw e
        }
    }

    /**
     * gRPC Streaming 방식으로 데이터 요청
     *
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return ApiResponse
     */
    suspend fun getDataGrpcStream(size: String = "1mb"): ApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val chunks = grpcClient.getDataStream(requestId, size).toList()
            val latencyNanos = System.nanoTime() - startNanos
            val totalSize = chunks.sumOf { it.chunk.size() }

            benchmarkCollector.record(latencyNanos, totalSize.toLong(), true)

            ApiResponse(
                requestId = requestId,
                protocol = "gRPC/Stream",
                payloadSize = totalSize,
                latencyMs = latencyNanos / 1_000_000.0
            )
        } catch (e: Exception) {
            val latencyNanos = System.nanoTime() - startNanos
            benchmarkCollector.record(latencyNanos, 0, false)
            throw e
        }
    }

    // ============================================
    // Phase 5: 복잡한 데이터 구조 서비스
    // ============================================

    /**
     * HTTP JSON 방식으로 복잡한 데이터 요청
     *
     * @param complexity 복잡도 (simple, medium, complex)
     * @return ComplexApiResponse
     */
    suspend fun getComplexDataHttpJson(complexity: String = "simple"): ComplexApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val response = httpClient.getComplexDataJson(requestId, complexity)
            val latencyNanos = System.nanoTime() - startNanos

            benchmarkCollector.record(latencyNanos, response.serializedSize.toLong(), true)

            ComplexApiResponse(
                requestId = requestId,
                protocol = "HTTP/JSON",
                complexity = complexity,
                serializedSize = response.serializedSize,
                latencyMs = latencyNanos / 1_000_000.0
            )
        } catch (e: Exception) {
            val latencyNanos = System.nanoTime() - startNanos
            benchmarkCollector.record(latencyNanos, 0, false)
            throw e
        }
    }

    /**
     * HTTP Binary (Protobuf) 방식으로 복잡한 데이터 요청
     *
     * @param complexity 복잡도 (simple, medium, complex)
     * @return ComplexApiResponse
     */
    suspend fun getComplexDataHttpBinary(complexity: String = "simple"): ComplexApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val response = httpClient.getComplexDataBinary(requestId, complexity)
            val latencyNanos = System.nanoTime() - startNanos

            benchmarkCollector.record(latencyNanos, response.size.toLong(), true)

            ComplexApiResponse(
                requestId = requestId,
                protocol = "HTTP/Binary(Protobuf)",
                complexity = complexity,
                serializedSize = response.size,
                latencyMs = latencyNanos / 1_000_000.0
            )
        } catch (e: Exception) {
            val latencyNanos = System.nanoTime() - startNanos
            benchmarkCollector.record(latencyNanos, 0, false)
            throw e
        }
    }

    /**
     * gRPC Unary 방식으로 복잡한 데이터 요청
     *
     * @param complexity 복잡도 (simple, medium, complex)
     * @return ComplexApiResponse
     */
    suspend fun getComplexDataGrpc(complexity: String = "simple"): ComplexApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val response = grpcClient.getComplexData(requestId, complexity)
            val latencyNanos = System.nanoTime() - startNanos

            benchmarkCollector.record(latencyNanos, response.serializedSize.toLong(), true)

            ComplexApiResponse(
                requestId = requestId,
                protocol = "gRPC/Unary",
                complexity = complexity,
                serializedSize = response.serializedSize,
                latencyMs = latencyNanos / 1_000_000.0
            )
        } catch (e: Exception) {
            val latencyNanos = System.nanoTime() - startNanos
            benchmarkCollector.record(latencyNanos, 0, false)
            throw e
        }
    }

    /**
     * gRPC Stream 방식으로 복잡한 데이터 요청
     *
     * 복잡한 데이터는 청크로 나누지 않고 Unary와 동일하게 처리한다.
     *
     * @param complexity 복잡도 (simple, medium, complex)
     * @return ComplexApiResponse
     */
    suspend fun getComplexDataGrpcStream(complexity: String = "simple"): ComplexApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val response = grpcClient.getComplexData(requestId, complexity)
            val latencyNanos = System.nanoTime() - startNanos

            benchmarkCollector.record(latencyNanos, response.serializedSize.toLong(), true)

            ComplexApiResponse(
                requestId = requestId,
                protocol = "gRPC/Stream",
                complexity = complexity,
                serializedSize = response.serializedSize,
                latencyMs = latencyNanos / 1_000_000.0
            )
        } catch (e: Exception) {
            val latencyNanos = System.nanoTime() - startNanos
            benchmarkCollector.record(latencyNanos, 0, false)
            throw e
        }
    }
}