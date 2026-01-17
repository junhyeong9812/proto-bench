package com.protobench.data.controller

import com.protobench.data.service.*
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.Base64

data class DataResponse(
    val requestId: String,
    val payload: String,
    val timestamp: Long,
    val payloadSize: Int
)

/**
 * Phase 5: 복잡한 데이터 JSON 응답
 */
data class ComplexDataJsonResponse<T>(
    val requestId: String,
    val timestamp: Long,
    val serializedSize: Int,
    val complexity: String,
    val data: T
)

/**
 * HTTP 데이터 컨트롤러
 *
 * dataServer의 HTTP 엔드포인트를 제공한다.
 * JSON(Base64) 또는 Binary 형식으로 페이로드를 반환한다.
 *
 * Phase 5: 복잡한 데이터 구조 엔드포인트 추가
 */
@RestController
@RequestMapping("/data")
class DataController(
    private val dataService: DataService
) {

    /**
     * JSON 형식으로 페이로드 반환 (Base64 인코딩)
     *
     * @param requestId 요청 ID
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return DataResponse (Base64 인코딩된 payload 포함)
     */
    @GetMapping("/json")
    fun getDataAsJson(
        @RequestParam(defaultValue = "unknown") requestId: String,
        @RequestParam(defaultValue = "1mb") size: String
    ): ResponseEntity<DataResponse> {
        val payload = dataService.getPayload(size)
        val response = DataResponse(
            requestId = requestId,
            payload = Base64.getEncoder().encodeToString(payload),
            timestamp = System.currentTimeMillis(),
            payloadSize = payload.size
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Binary 형식으로 페이로드 반환 (raw bytes)
     *
     * @param requestId 요청 ID
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return 바이너리 바이트 배열
     */
    @GetMapping("/binary", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getDataAsBinary(
        @RequestParam(defaultValue = "unknown") requestId: String,
        @RequestParam(defaultValue = "1mb") size: String
    ): ResponseEntity<ByteArray> {
        val payload = dataService.getPayload(size)
        return ResponseEntity.ok()
            .header("X-Request-Id", requestId)
            .header("X-Payload-Size", payload.size.toString())
            .header("X-Timestamp", System.currentTimeMillis().toString())
            .body(payload)
    }

    /**
     * 서버 정보 반환
     *
     * @return 서버 ID, 지원 크기 목록 등
     */
    @GetMapping("/info")
    fun getInfo(): Map<String, Any> {
        return mapOf(
            "service" to "dataServer",
            "serverId" to dataService.serverId,
            "supportedSizes" to DataService.PAYLOAD_SIZES.keys,
            "supportedComplexities" to listOf("simple", "medium", "complex")
        )
    }

    // ============================================
    // Phase 5: 복잡한 데이터 구조 엔드포인트
    // ============================================

    /**
     * 복잡한 데이터를 JSON 형식으로 반환
     *
     * @param requestId 요청 ID
     * @param complexity 복잡도 (simple, medium, complex)
     * @return ComplexDataJsonResponse (복잡도에 따른 데이터 포함)
     */
    @GetMapping("/complex/json")
    fun getComplexDataAsJson(
        @RequestParam(defaultValue = "unknown") requestId: String,
        @RequestParam(defaultValue = "simple") complexity: String
    ): ResponseEntity<*> {
        val timestamp = System.currentTimeMillis()

        return when (complexity.lowercase()) {
            "simple" -> {
                val data = dataService.generateSimpleData(requestId)
                val response = ComplexDataJsonResponse(
                    requestId = requestId,
                    timestamp = timestamp,
                    serializedSize = estimateJsonSize(data),
                    complexity = "simple",
                    data = data
                )
                ResponseEntity.ok(response)
            }
            "medium" -> {
                val data = dataService.generateMediumData(requestId)
                val response = ComplexDataJsonResponse(
                    requestId = requestId,
                    timestamp = timestamp,
                    serializedSize = estimateJsonSize(data),
                    complexity = "medium",
                    data = data
                )
                ResponseEntity.ok(response)
            }
            "complex" -> {
                val data = dataService.generateComplexData(requestId)
                val response = ComplexDataJsonResponse(
                    requestId = requestId,
                    timestamp = timestamp,
                    serializedSize = estimateJsonSize(data),
                    complexity = "complex",
                    data = data
                )
                ResponseEntity.ok(response)
            }
            else -> {
                ResponseEntity.badRequest().body(
                    mapOf("error" to "Invalid complexity. Use: simple, medium, complex")
                )
            }
        }
    }

    /**
     * 복잡한 데이터를 Protobuf Binary 형식으로 반환
     *
     * @param requestId 요청 ID
     * @param complexity 복잡도 (simple, medium, complex)
     * @return Protobuf 바이너리 바이트 배열
     */
    @GetMapping("/complex/binary", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getComplexDataAsBinary(
        @RequestParam(defaultValue = "unknown") requestId: String,
        @RequestParam(defaultValue = "simple") complexity: String
    ): ResponseEntity<ByteArray> {
        val timestamp = System.currentTimeMillis()

        val responseBytes = when (complexity.lowercase()) {
            "simple" -> {
                val data = dataService.generateSimpleData(requestId)
                buildSimpleProtoResponse(requestId, timestamp, data)
            }
            "medium" -> {
                val data = dataService.generateMediumData(requestId)
                buildMediumProtoResponse(requestId, timestamp, data)
            }
            "complex" -> {
                val data = dataService.generateComplexData(requestId)
                buildComplexProtoResponse(requestId, timestamp, data)
            }
            else -> {
                return ResponseEntity.badRequest().build()
            }
        }

        return ResponseEntity.ok()
            .header("X-Request-Id", requestId)
            .header("X-Complexity", complexity)
            .header("X-Payload-Size", responseBytes.size.toString())
            .header("X-Timestamp", timestamp.toString())
            .body(responseBytes)
    }

    /**
     * Simple Protobuf 응답 빌드
     */
    private fun buildSimpleProtoResponse(requestId: String, timestamp: Long, data: SimpleDataDto): ByteArray {
        val simpleData = com.protobench.proto.SimpleData.newBuilder()
            .setId(data.id)
            .setName(data.name)
            .setAge(data.age)
            .setScore(data.score)
            .setIsActive(data.isActive)
            .build()

        val response = com.protobench.proto.ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setSimple(simpleData)
            .build()

        // serializedSize 설정을 위해 다시 빌드
        return com.protobench.proto.ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setResponseSize(response.serializedSize)
            .setSimple(simpleData)
            .build()
            .toByteArray()
    }

    /**
     * Medium Protobuf 응답 빌드
     */
    private fun buildMediumProtoResponse(requestId: String, timestamp: Long, data: MediumDataDto): ByteArray {
        val address = com.protobench.proto.Address.newBuilder()
            .setCity(data.address.city)
            .setStreet(data.address.street)
            .setZipcode(data.address.zipcode)
            .setCountry(data.address.country)
            .build()

        val mediumData = com.protobench.proto.MediumData.newBuilder()
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

        val response = com.protobench.proto.ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setMedium(mediumData)
            .build()

        return com.protobench.proto.ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setResponseSize(response.serializedSize)
            .setMedium(mediumData)
            .build()
            .toByteArray()
    }

    /**
     * Complex Protobuf 응답 빌드
     */
    private fun buildComplexProtoResponse(requestId: String, timestamp: Long, data: ComplexDataDto): ByteArray {
        // Address 빌드 함수
        fun buildAddress(addr: AddressDto) = com.protobench.proto.Address.newBuilder()
            .setCity(addr.city)
            .setStreet(addr.street)
            .setZipcode(addr.zipcode)
            .setCountry(addr.country)
            .build()

        // Item 빌드 함수
        fun buildItem(item: ItemDto) = com.protobench.proto.Item.newBuilder()
            .setProductId(item.productId)
            .setName(item.name)
            .setQuantity(item.quantity)
            .setPrice(item.price)
            .build()

        // Order 빌드 함수
        fun buildOrder(order: OrderDto) = com.protobench.proto.Order.newBuilder()
            .setOrderId(order.orderId)
            .setAmount(order.amount)
            .setTimestamp(order.timestamp)
            .addAllItems(order.items.map { buildItem(it) })
            .build()

        val complexData = com.protobench.proto.ComplexData.newBuilder()
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

        val response = com.protobench.proto.ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setComplex(complexData)
            .build()

        return com.protobench.proto.ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setResponseSize(response.serializedSize)
            .setComplex(complexData)
            .build()
            .toByteArray()
    }

    /**
     * JSON 크기 추정 (대략적)
     */
    private fun estimateJsonSize(data: Any): Int {
        // 실제로는 Jackson ObjectMapper를 사용해 정확히 계산할 수 있음
        // 여기서는 대략적인 추정치 반환
        return when (data) {
            is SimpleDataDto -> 150
            is MediumDataDto -> 800
            is ComplexDataDto -> 5000
            else -> 0
        }
    }
}