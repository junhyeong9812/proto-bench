package com.protobench.data.grpc

import com.protobench.data.service.*
import com.protobench.proto.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service

/**
 * gRPC 데이터 서비스
 *
 * DataServiceGrpcKt.DataServiceCoroutineImplBase를 상속받아
 * Unary RPC와 Server Streaming RPC를 구현한다.
 *
 * Phase 5: 복잡한 데이터 구조 RPC 추가
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

    // ============================================
    // Phase 5: 복잡한 데이터 구조 RPC
    // ============================================

    /**
     * 복잡한 데이터 RPC: 복잡도에 따른 데이터 반환
     *
     * 클라이언트가 요청한 복잡도(simple, medium, complex)에 맞는
     * 데이터 구조를 생성하여 반환한다.
     *
     * @param request 요청 (requestId, complexity 포함)
     * @return ComplexDataResponse (복잡도에 따른 데이터 포함)
     */
    override suspend fun getComplexData(request: ComplexDataRequest): ComplexDataResponse {
        val complexity = if (request.complexity.isNullOrEmpty()) "simple" else request.complexity
        val timestamp = System.currentTimeMillis()

        return when (complexity.lowercase()) {
            "simple" -> buildSimpleResponse(request.requestId, timestamp)
            "medium" -> buildMediumResponse(request.requestId, timestamp)
            "complex" -> buildComplexResponse(request.requestId, timestamp)
            else -> buildSimpleResponse(request.requestId, timestamp)
        }
    }

    /**
     * Simple 응답 빌드
     *
     * @param requestId 요청 ID
     * @param timestamp 타임스탬프
     * @return ComplexDataResponse
     */
    private fun buildSimpleResponse(requestId: String, timestamp: Long): ComplexDataResponse {
        val data = dataService.generateSimpleData(requestId)

        val simpleData = SimpleData.newBuilder()
            .setId(data.id)
            .setName(data.name)
            .setAge(data.age)
            .setScore(data.score)
            .setIsActive(data.isActive)
            .build()

        val response = ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setSimple(simpleData)
            .build()

        return ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setSerializedSize(response.serializedSize)
            .setSimple(simpleData)
            .build()
    }

    /**
     * Medium 응답 빌드
     *
     * @param requestId 요청 ID
     * @param timestamp 타임스탬프
     * @return ComplexDataResponse
     */
    private fun buildMediumResponse(requestId: String, timestamp: Long): ComplexDataResponse {
        val data = dataService.generateMediumData(requestId)

        val address = buildAddress(data.address)

        val mediumData = MediumData.newBuilder()
            .setId(data.id)
            .setName(data.name)
            .setAge(data.age)
            .setScore(data.score)
            .setIsActive(data.isActive)
            .setEmail(data.email)
            .setPhone(data.phone)
            .setCreatedAt(data.createdAt)
            .setUpdatedAt(data.updatedAt)
            .setStatus(data.status)
            .addAllTags(data.tags)
            .setAddress(address)
            .putAllMetadata(data.metadata)
            .build()

        val response = ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setMedium(mediumData)
            .build()

        return ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setSerializedSize(response.serializedSize)
            .setMedium(mediumData)
            .build()
    }

    /**
     * Complex 응답 빌드
     *
     * @param requestId 요청 ID
     * @param timestamp 타임스탬프
     * @return ComplexDataResponse
     */
    private fun buildComplexResponse(requestId: String, timestamp: Long): ComplexDataResponse {
        val data = dataService.generateComplexData(requestId)

        val complexData = ComplexData.newBuilder()
            .setId(data.id)
            .setName(data.name)
            .setAge(data.age)
            .setScore(data.score)
            .setIsActive(data.isActive)
            .setEmail(data.email)
            .setPhone(data.phone)
            .setCreatedAt(data.createdAt)
            .setUpdatedAt(data.updatedAt)
            .setStatus(data.status)
            .addAllTags(data.tags)
            .setAddress(buildAddress(data.address))
            .setBillingAddress(buildAddress(data.billingAddress))
            .addAllOrders(data.orders.map { buildOrder(it) })
            .putAllMetadata(data.metadata)
            .putAllScores(data.scores)
            .addAllPermissions(data.permissions)
            .addAllAddresses(data.addresses.map { buildAddress(it) })
            .setDescription(data.description)
            .setNotes(data.notes)
            .build()

        val response = ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setComplex(complexData)
            .build()

        return ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setSerializedSize(response.serializedSize)
            .setComplex(complexData)
            .build()
    }

    /**
     * Address Proto 빌드
     *
     * @param addr AddressDto
     * @return Address Proto
     */
    private fun buildAddress(addr: AddressDto): Address {
        return Address.newBuilder()
            .setCity(addr.city)
            .setStreet(addr.street)
            .setZipcode(addr.zipcode)
            .setCountry(addr.country)
            .build()
    }

    /**
     * Order Proto 빌드
     *
     * @param order OrderDto
     * @return Order Proto
     */
    private fun buildOrder(order: OrderDto): Order {
        return Order.newBuilder()
            .setOrderId(order.orderId)
            .setAmount(order.amount)
            .setTimestamp(order.timestamp)
            .addAllItems(order.items.map { buildItem(it) })
            .build()
    }

    /**
     * Item Proto 빌드
     *
     * @param item ItemDto
     * @return Item Proto
     */
    private fun buildItem(item: ItemDto): Item {
        return Item.newBuilder()
            .setProductId(item.productId)
            .setName(item.name)
            .setQuantity(item.quantity)
            .setPrice(item.price)
            .build()
    }
}