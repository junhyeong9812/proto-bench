package com.protobench.api.service

import com.protobench.api.benchmark.BenchmarkCollector
import com.protobench.api.client.GrpcDataClient
import com.protobench.api.client.HttpDataClient
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * API 응답 DTO
 */
data class ApiResponse(
    val requestId: String,
    val protocol: String,
    val payloadSize: Int,
    val latencyMs: Double
)

/**
 * 데이터 요청 서비스 (HTTP/gRPC 선택)
 */
@Service
class ApiDataService(
    private val httpClient: HttpDataClient,
    private val grpcClient: GrpcDataClient,
    private val benchmarkCollector: BenchmarkCollector
) {

    /**
     * HTTP JSON으로 데이터 요청
     */
    suspend fun getDataHttpJson(): ApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val response = httpClient.getDataJson(requestId)
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
     * HTTP Binary로 데이터 요청
     */
    suspend fun getDataHttpBinary(): ApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val response = httpClient.getDataBinary(requestId)
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
     * gRPC Unary로 데이터 요청
     */
    suspend fun getDataGrpc(): ApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val response = grpcClient.getData(requestId)
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
     * gRPC Streaming으로 데이터 요청
     */
    suspend fun getDataGrpcStream(): ApiResponse {
        val requestId = UUID.randomUUID().toString()
        val startNanos = System.nanoTime()

        return try {
            val chunks = grpcClient.getDataStream(requestId).toList()
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
}