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

@Service
class GrpcDataService(
    private val dataService: DataService
) : DataServiceGrpcKt.DataServiceCoroutineImplBase() {

    /**
     * 단일 요청-응답: 1MB 데이터 한번에 전송
     */
    override suspend fun getData(request: DataRequest): DataResponse {
        val payload = dataService.getPayload()

        return DataResponse.newBuilder()
            .setRequestId(request.requestId)
            .setPayload(ByteString.copyFrom(payload))
            .setTimestamp(System.currentTimeMillis())
            .setPayloadSize(payload.size)
            .build()
    }

    /**
     * 서버 스트리밍: 1MB 데이터를 청크로 나눠서 전송
     */
    override fun getDataStream(request: DataRequest): Flow<DataChunk> = flow {
        val chunks = dataService.getChunks()
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