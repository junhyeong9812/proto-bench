package com.protobench.data.service

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.util.concurrent.ThreadLocalRandom

/**
 * 페이로드 데이터 관리 서비스
 *
 * 서버 시작 시 다양한 크기의 랜덤 바이트 배열을 메모리에 생성하여 보관.
 * HTTP/gRPC 요청 시 해당 페이로드를 반환한다.
 *
 * Phase 5: 복잡한 데이터 구조 생성 기능 추가
 */
@Service
class DataService {

    companion object {
        val PAYLOAD_SIZES = mapOf(
            "1kb" to 1 * 1024,
            "10kb" to 10 * 1024,
            "50kb" to 50 * 1024,
            "100kb" to 100 * 1024,
            "200kb" to 200 * 1024,
            "500kb" to 500 * 1024,
            "1mb" to 1 * 1024 * 1024
        )
        const val CHUNK_SIZE = 64 * 1024

        // Phase 5: 복잡도별 설정
        const val SIMPLE_TAGS_COUNT = 0
        const val MEDIUM_TAGS_COUNT = 10
        const val MEDIUM_METADATA_COUNT = 5
        const val COMPLEX_TAGS_COUNT = 10
        const val COMPLEX_ORDERS_COUNT = 5
        const val COMPLEX_ITEMS_PER_ORDER = 3
        const val COMPLEX_METADATA_COUNT = 10
        const val COMPLEX_SCORES_COUNT = 5
        const val COMPLEX_PERMISSIONS_COUNT = 10
        const val COMPLEX_ADDRESSES_COUNT = 3
    }

    private val payloads = mutableMapOf<String, ByteArray>()
    val serverId = "data-server-${System.currentTimeMillis() % 10000}"

    @PostConstruct
    fun init() {
        PAYLOAD_SIZES.forEach { (name, size) ->
            val payload = ByteArray(size)
            ThreadLocalRandom.current().nextBytes(payload)
            payloads[name] = payload
            println("✅ Initialized payload: $name (${size} bytes)")
        }
        println("✅ Phase 5: Complex data generators ready")
    }

    /**
     * 지정된 크기의 페이로드 반환
     *
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return 랜덤 바이트 배열
     */
    fun getPayload(size: String = "1mb"): ByteArray {
        return payloads[size.lowercase()] ?: payloads["1mb"]!!
    }

    /**
     * 페이로드를 청크 단위로 분할하여 반환 (스트리밍용)
     *
     * @param size 페이로드 크기 키
     * @return 청크 리스트 (각 청크는 64KB)
     */
    fun getPayloadAsChunks(size: String = "1mb"): List<ByteArray> {
        val payload = getPayload(size)
        return payload.toList().chunked(CHUNK_SIZE).map { it.toByteArray() }
    }

    /**
     * 페이로드 크기(바이트) 반환
     *
     * @param size 페이로드 크기 키
     * @return 바이트 수
     */
    fun getPayloadSize(size: String = "1mb"): Int {
        return PAYLOAD_SIZES[size.lowercase()] ?: PAYLOAD_SIZES["1mb"]!!
    }

    // ============================================
    // Phase 5: 복잡한 데이터 구조 생성
    // ============================================

    /**
     * Simple 데이터 생성 (필드 5개, 중첩 없음)
     *
     * @param requestId 요청 ID
     * @return SimpleDataDto
     */
    fun generateSimpleData(requestId: String): SimpleDataDto {
        return SimpleDataDto(
            id = "simple-$requestId",
            name = "User-${randomString(8)}",
            age = ThreadLocalRandom.current().nextInt(18, 80),
            score = ThreadLocalRandom.current().nextDouble(0.0, 100.0),
            isActive = ThreadLocalRandom.current().nextBoolean()
        )
    }

    /**
     * Medium 데이터 생성 (필드 15개, 중첩 1단계)
     *
     * @param requestId 요청 ID
     * @return MediumDataDto
     */
    fun generateMediumData(requestId: String): MediumDataDto {
        return MediumDataDto(
            id = "medium-$requestId",
            name = "User-${randomString(8)}",
            age = ThreadLocalRandom.current().nextInt(18, 80),
            score = ThreadLocalRandom.current().nextDouble(0.0, 100.0),
            isActive = ThreadLocalRandom.current().nextBoolean(),
            email = "${randomString(10)}@example.com",
            phone = "+82-10-${randomDigits(4)}-${randomDigits(4)}",
            createdAt = System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(86400000),
            updatedAt = System.currentTimeMillis(),
            status = listOf("ACTIVE", "PENDING", "INACTIVE").random(),
            tags = (1..MEDIUM_TAGS_COUNT).map { "tag-${randomString(5)}" },
            address = generateAddress(),
            metadata = (1..MEDIUM_METADATA_COUNT).associate { "key-$it" to "value-${randomString(10)}" }
        )
    }

    /**
     * Complex 데이터 생성 (필드 50+개, 중첩 2단계)
     *
     * @param requestId 요청 ID
     * @return ComplexDataDto
     */
    fun generateComplexData(requestId: String): ComplexDataDto {
        return ComplexDataDto(
            id = "complex-$requestId",
            name = "User-${randomString(8)}",
            age = ThreadLocalRandom.current().nextInt(18, 80),
            score = ThreadLocalRandom.current().nextDouble(0.0, 100.0),
            isActive = ThreadLocalRandom.current().nextBoolean(),
            email = "${randomString(10)}@example.com",
            phone = "+82-10-${randomDigits(4)}-${randomDigits(4)}",
            createdAt = System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(86400000),
            updatedAt = System.currentTimeMillis(),
            status = listOf("ACTIVE", "PENDING", "INACTIVE").random(),
            tags = (1..COMPLEX_TAGS_COUNT).map { "tag-${randomString(5)}" },
            address = generateAddress(),
            billingAddress = generateAddress(),
            orders = (1..COMPLEX_ORDERS_COUNT).map { generateOrder() },
            metadata = (1..COMPLEX_METADATA_COUNT).associate { "key-$it" to "value-${randomString(10)}" },
            scores = (1..COMPLEX_SCORES_COUNT).associate { "score-$it" to ThreadLocalRandom.current().nextInt(0, 100) },
            permissions = (1..COMPLEX_PERMISSIONS_COUNT).map { "permission-${randomString(6)}" },
            addresses = (1..COMPLEX_ADDRESSES_COUNT).map { generateAddress() },
            description = "Description: ${randomString(100)}",
            notes = "Notes: ${randomString(50)}"
        )
    }

    /**
     * 주소 객체 생성
     *
     * @return AddressDto
     */
    private fun generateAddress(): AddressDto {
        val cities = listOf("Seoul", "Busan", "Incheon", "Daegu", "Daejeon", "Gwangju", "Ulsan")
        val countries = listOf("Korea", "Japan", "USA", "China", "Germany")
        return AddressDto(
            city = cities.random(),
            street = "${randomDigits(3)} ${randomString(10)} Street",
            zipcode = randomDigits(5),
            country = countries.random()
        )
    }

    /**
     * 주문 객체 생성 (아이템 포함)
     *
     * @return OrderDto
     */
    private fun generateOrder(): OrderDto {
        val items = (1..COMPLEX_ITEMS_PER_ORDER).map { generateItem() }
        return OrderDto(
            orderId = "order-${randomString(12)}",
            amount = items.sumOf { it.price * it.quantity },
            timestamp = System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(2592000000), // 30일 내
            items = items
        )
    }

    /**
     * 아이템 객체 생성
     *
     * @return ItemDto
     */
    private fun generateItem(): ItemDto {
        val quantity = ThreadLocalRandom.current().nextInt(1, 10)
        val price = ThreadLocalRandom.current().nextDouble(10.0, 500.0)
        return ItemDto(
            productId = "prod-${randomString(8)}",
            name = "Product-${randomString(6)}",
            quantity = quantity,
            price = price
        )
    }

    /**
     * 랜덤 문자열 생성
     *
     * @param length 문자열 길이
     * @return 랜덤 알파벳 문자열
     */
    private fun randomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        return (1..length).map { chars.random() }.joinToString("")
    }

    /**
     * 랜덤 숫자 문자열 생성
     *
     * @param length 숫자 길이
     * @return 랜덤 숫자 문자열
     */
    private fun randomDigits(length: Int): String {
        return (1..length).map { ThreadLocalRandom.current().nextInt(0, 10) }.joinToString("")
    }
}

// ============================================
// Phase 5: DTO 클래스
// ============================================

/**
 * 아이템 DTO (이중 중첩용)
 */
data class ItemDto(
    val productId: String,
    val name: String,
    val quantity: Int,
    val price: Double
)

/**
 * 주문 DTO (중첩 배열용)
 */
data class OrderDto(
    val orderId: String,
    val amount: Double,
    val timestamp: Long,
    val items: List<ItemDto>
)

/**
 * 주소 DTO (중첩 객체용)
 */
data class AddressDto(
    val city: String,
    val street: String,
    val zipcode: String,
    val country: String
)

/**
 * Simple 데이터 DTO (필드 5개, 중첩 없음)
 */
data class SimpleDataDto(
    val id: String,
    val name: String,
    val age: Int,
    val score: Double,
    val isActive: Boolean
)

/**
 * Medium 데이터 DTO (필드 13개, 중첩 1단계)
 */
data class MediumDataDto(
    val id: String,
    val name: String,
    val age: Int,
    val score: Double,
    val isActive: Boolean,
    val email: String,
    val phone: String,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String,
    val tags: List<String>,
    val address: AddressDto,
    val metadata: Map<String, String>
)

/**
 * Complex 데이터 DTO (필드 20개, 중첩 2단계)
 */
data class ComplexDataDto(
    val id: String,
    val name: String,
    val age: Int,
    val score: Double,
    val isActive: Boolean,
    val email: String,
    val phone: String,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String,
    val tags: List<String>,
    val address: AddressDto,
    val billingAddress: AddressDto,
    val orders: List<OrderDto>,
    val metadata: Map<String, String>,
    val scores: Map<String, Int>,
    val permissions: List<String>,
    val addresses: List<AddressDto>,
    val description: String,
    val notes: String
)