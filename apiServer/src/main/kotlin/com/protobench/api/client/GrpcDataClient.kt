package com.protobench.api.client

import com.protobench.proto.*
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * dataServer gRPC 클라이언트
 *
 * gRPC 채널을 통해 dataServer의 gRPC 서비스를 호출한다.
 *
 * Phase 5: 복잡한 데이터 구조 요청 메서드 추가
 */
@Component
class GrpcDataClient(
    @Value("\${data-server.grpc.host}") private val host: String,
    @Value("\${data-server.grpc.port}") private val port: Int
) {
    private lateinit var channel: ManagedChannel
    private lateinit var stub: DataServiceGrpcKt.DataServiceCoroutineStub

    /**
     * gRPC 채널 초기화
     *
     * Spring 컨텍스트 초기화 후 자동 호출된다.
     */
    @PostConstruct
    fun init() {
        channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .maxInboundMessageSize(10 * 1024 * 1024)
            .build()

        stub = DataServiceGrpcKt.DataServiceCoroutineStub(channel)
        println("✅ gRPC Client connected to $host:$port")
    }

    /**
     * gRPC 채널 종료
     *
     * Spring 컨텍스트 종료 시 자동 호출된다.
     */
    @PreDestroy
    fun shutdown() {
        if (::channel.isInitialized) {
            channel.shutdown()
            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    channel.shutdownNow()
                }
            } catch (e: InterruptedException) {
                channel.shutdownNow()
            }
        }
    }

    /**
     * Unary RPC: 단일 요청으로 전체 데이터 가져오기
     *
     * @param requestId 요청 ID
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return DataResponse (전체 페이로드 포함)
     */
    suspend fun getData(requestId: String, size: String = "1mb"): DataResponse {
        val request = DataRequest.newBuilder()
            .setRequestId(requestId)
            .setSize(size)
            .build()

        return stub.getData(request)
    }

    /**
     * Server Streaming RPC: 스트리밍으로 청크 데이터 가져오기
     *
     * @param requestId 요청 ID
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return Flow<DataChunk> (청크 스트림)
     */
    fun getDataStream(requestId: String, size: String = "1mb"): Flow<DataChunk> {
        val request = DataRequest.newBuilder()
            .setRequestId(requestId)
            .setSize(size)
            .build()

        return stub.getDataStream(request)
    }

    // ============================================
    // Phase 5: 복잡한 데이터 구조 요청
    // ============================================

    /**
     * 복잡한 데이터 요청 (gRPC Unary)
     *
     * @param requestId 요청 ID
     * @param complexity 복잡도 (simple, medium, complex)
     * @return ComplexDataResponse
     */
    suspend fun getComplexData(requestId: String, complexity: String = "simple"): ComplexDataResponse {
        val request = ComplexDataRequest.newBuilder()
            .setRequestId(requestId)
            .setComplexity(complexity)
            .build()

        return stub.getComplexData(request)
    }
}