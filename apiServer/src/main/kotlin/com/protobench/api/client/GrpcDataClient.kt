package com.protobench.api.client

import com.protobench.proto.DataRequest
import com.protobench.proto.DataResponse
import com.protobench.proto.DataChunk
import com.protobench.proto.DataServiceGrpcKt
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
 */
@Component
class GrpcDataClient(
    @Value("\${data-server.grpc.host}") private val host: String,
    @Value("\${data-server.grpc.port}") private val port: Int
) {
    private lateinit var channel: ManagedChannel
    private lateinit var stub: DataServiceGrpcKt.DataServiceCoroutineStub

    @PostConstruct
    fun init() {
        channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .maxInboundMessageSize(10 * 1024 * 1024)  // 10MB
            .build()

        stub = DataServiceGrpcKt.DataServiceCoroutineStub(channel)
        println("✅ gRPC Client connected to $host:$port")
    }

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
     * 단일 요청으로 전체 데이터 가져오기
     */
    suspend fun getData(requestId: String): DataResponse {
        val request = DataRequest.newBuilder()
            .setRequestId(requestId)
            .build()

        return stub.getData(request)
    }

    /**
     * 스트리밍으로 청크 데이터 가져오기
     */
    fun getDataStream(requestId: String): Flow<DataChunk> {
        val request = DataRequest.newBuilder()
            .setRequestId(requestId)
            .build()

        return stub.getDataStream(request)
    }
}