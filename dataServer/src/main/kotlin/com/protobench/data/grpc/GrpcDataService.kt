package com.protobench.data.grpc

import com.protobench.data.service.DataService
import com.protobench.proto.DataRequest
import com.protobench.proto.DataResponse
import com.protobench.proto.DataChunk
import com.protobench.proto.DataServiceGrpcKt
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service

/**
 * gRPC 데이터 서비스
 *
 * DataServiceGrpcKt.DataServiceCoroutineImplBase를 상속받아
 * Unary RPC와 Server Streaming RPC를 구현한다.
 */
@Service
class GrpcDataService(
    private val dataService: DataService
) : DataServiceGrpcKt.DataServiceCoroutineImplBase() {

    /**
     * Unary RPC: 단일 요청 → 단일 응답
     *
     * 클라이언트가 요청한 크기의 페이로드를 한 번에 전송한다.
     *
     * @param request 요청 (requestId, size 포함)
     * @return DataResponse (전체 페이로드 포함)
     */
    override suspend fun getData(request: DataRequest): DataResponse {
        val size = if (request.size.isNullOrEmpty()) "1mb" else request.size
        val payload = dataService.getPayload(size)

        return DataResponse.newBuilder()
            .setRequestId(request.requestId)
            .setPayload(ByteString.copyFrom(payload))
            .setTimestamp(System.currentTimeMillis())
            .setPayloadSize(payload.size)
            .build()
    }

    /**
     * Server Streaming RPC: 단일 요청 → 다중 응답(스트림)
     *
     * 클라이언트가 요청한 크기의 페이로드를 64KB 청크로 나누어 스트리밍한다.
     *
     * @param request 요청 (requestId, size 포함)
     * @return Flow<DataChunk> (청크 스트림)
     */
    override fun getDataStream(request: DataRequest): Flow<DataChunk> = flow {
        val size = if (request.size.isNullOrEmpty()) "1mb" else request.size
        val chunks = dataService.getPayloadAsChunks(size)
        val totalChunks = chunks.size

        chunks.forEachIndexed { index, chunk ->
            val dataChunk = DataChunk.newBuilder()
                .setRequestId(request.requestId)
                .setChunk(ByteString.copyFrom(chunk))
                .setChunkIndex(index)
                .setTotalChunks(totalChunks)
                .setIsLast(index == totalChunks - 1)
                .build()

            emit(dataChunk)
        }
    }
}