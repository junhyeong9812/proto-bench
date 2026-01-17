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
 * Phase 6: Ultra/Extreme 복잡도 데이터 구조 추가
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

        // ============================================
        // Phase 6: Ultra/Extreme 복잡도 설정
        // ============================================

        // Ultra 설정 (~150개 필드, 3단계 중첩)
        const val ULTRA_TAGS_COUNT = 20
        const val ULTRA_PERMISSIONS_COUNT = 20
        const val ULTRA_METADATA_COUNT = 20
        const val ULTRA_SCORES_COUNT = 10
        const val ULTRA_ADDRESSES_COUNT = 10
        const val ULTRA_CONTACTS_PER_ADDRESS = 5
        const val ULTRA_ORDERS_COUNT = 10
        const val ULTRA_ITEMS_PER_ORDER = 5
        const val ULTRA_ATTRIBUTES_PER_ITEM = 3
        const val ULTRA_CATEGORIES_COUNT = 10
        const val ULTRA_SUBCATEGORIES_PER_CATEGORY = 5
        const val ULTRA_ITEMS_PER_SUBCATEGORY = 3
        const val ULTRA_HISTORY_COUNT = 20
        const val ULTRA_CHANGES_PER_HISTORY = 3

        // Extreme 설정 (~500개 필드, 4단계 중첩)
        const val EXTREME_TAGS_COUNT = 50
        const val EXTREME_PERMISSIONS_COUNT = 50
        const val EXTREME_METADATA_COUNT = 50
        const val EXTREME_SCORES_COUNT = 20
        const val EXTREME_ADDRESSES_COUNT = 20
        const val EXTREME_CONTACTS_PER_ADDRESS = 5
        const val EXTREME_ORDERS_COUNT = 20
        const val EXTREME_ITEMS_PER_ORDER = 10
        const val EXTREME_ATTRIBUTES_PER_ITEM = 5
        const val EXTREME_VALUES_PER_ATTRIBUTE = 3
        const val EXTREME_ORGS_COUNT = 10
        const val EXTREME_DEPTS_PER_ORG = 5
        const val EXTREME_TEAMS_PER_DEPT = 5
        const val EXTREME_MEMBERS_PER_TEAM = 3
        const val EXTREME_EVENTS_COUNT = 30
        const val EXTREME_PARTICIPANTS_PER_EVENT = 5
        const val EXTREME_ROLES_PER_PARTICIPANT = 3
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
        println("✅ Phase 6: Ultra/Extreme data generators ready")
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

    // ============================================
    // Phase 6: Ultra/Extreme 데이터 구조 생성
    // ============================================

    /**
     * Ultra 데이터 생성 (~150개 필드, 3단계 중첩)
     *
     * 구조:
     * - 기본 필드 15개
     * - tags[20], permissions[20]
     * - addresses[10] → contacts[5] (2단계 중첩)
     * - orders[10] → items[5] → attributes[3] (3단계 중첩)
     * - categories[10] → subcategories[5] → items[3] (3단계 중첩)
     * - history[20] → changes[3] (2단계 중첩)
     * - metadata{20}, scores{10}
     *
     * 총 빌더 호출: ~200회
     * 예상 JSON 크기: ~15KB
     * 예상 Protobuf 크기: ~5KB
     *
     * @param requestId 요청 ID
     * @return UltraDataDto
     */
    fun generateUltraData(requestId: String): UltraDataDto {
        return UltraDataDto(
            // 기본 필드 15개
            id = "ultra-$requestId",
            name = "User-${randomString(8)}",
            age = ThreadLocalRandom.current().nextInt(18, 80),
            score = ThreadLocalRandom.current().nextDouble(0.0, 100.0),
            isActive = ThreadLocalRandom.current().nextBoolean(),
            email = "${randomString(10)}@example.com",
            phone = "+82-10-${randomDigits(4)}-${randomDigits(4)}",
            createdAt = System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(86400000),
            updatedAt = System.currentTimeMillis(),
            status = listOf("ACTIVE", "PENDING", "INACTIVE").random(),
            priority = ThreadLocalRandom.current().nextInt(1, 10),
            level = ThreadLocalRandom.current().nextInt(1, 100),
            rating = ThreadLocalRandom.current().nextDouble(0.0, 5.0),
            verified = ThreadLocalRandom.current().nextBoolean(),
            premium = ThreadLocalRandom.current().nextBoolean(),

            // 배열/맵
            tags = (1..ULTRA_TAGS_COUNT).map { "tag-${randomString(5)}" },
            permissions = (1..ULTRA_PERMISSIONS_COUNT).map { "permission-${randomString(6)}" },
            metadata = (1..ULTRA_METADATA_COUNT).associate { "key-$it" to "value-${randomString(10)}" },
            scores = (1..ULTRA_SCORES_COUNT).associate { "score-$it" to ThreadLocalRandom.current().nextInt(0, 100) },

            // 2단계 중첩
            addresses = (1..ULTRA_ADDRESSES_COUNT).map { generateAddressWithContacts() },

            // 3단계 중첩
            orders = (1..ULTRA_ORDERS_COUNT).map { generateOrderWithAttributes() },
            categories = (1..ULTRA_CATEGORIES_COUNT).map { generateCategory() },
            history = (1..ULTRA_HISTORY_COUNT).map { generateHistoryEntry() },

            description = "Ultra Description: ${randomString(150)}",
            notes = "Ultra Notes: ${randomString(100)}"
        )
    }

    /**
     * Extreme 데이터 생성 (~500개 필드, 4단계 중첩)
     *
     * 구조:
     * - 기본 필드 20개
     * - tags[50], permissions[50]
     * - addresses[20] → contacts[5] (2단계 중첩)
     * - orders[20] → items[10] → attributes[5] → values[3] (4단계 중첩)
     * - organizations[10] → departments[5] → teams[5] → members[3] (4단계 중첩)
     * - events[30] → participants[5] → roles[3] (3단계 중첩)
     * - metadata{50}, scores{20}
     *
     * 총 빌더 호출: ~800회
     * 예상 JSON 크기: ~50KB
     * 예상 Protobuf 크기: ~15KB
     *
     * @param requestId 요청 ID
     * @return ExtremeDataDto
     */
    fun generateExtremeData(requestId: String): ExtremeDataDto {
        return ExtremeDataDto(
            // 기본 필드 20개
            id = "extreme-$requestId",
            name = "User-${randomString(8)}",
            age = ThreadLocalRandom.current().nextInt(18, 80),
            score = ThreadLocalRandom.current().nextDouble(0.0, 100.0),
            isActive = ThreadLocalRandom.current().nextBoolean(),
            email = "${randomString(10)}@example.com",
            phone = "+82-10-${randomDigits(4)}-${randomDigits(4)}",
            createdAt = System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(86400000),
            updatedAt = System.currentTimeMillis(),
            status = listOf("ACTIVE", "PENDING", "INACTIVE").random(),
            priority = ThreadLocalRandom.current().nextInt(1, 10),
            level = ThreadLocalRandom.current().nextInt(1, 100),
            rating = ThreadLocalRandom.current().nextDouble(0.0, 5.0),
            verified = ThreadLocalRandom.current().nextBoolean(),
            premium = ThreadLocalRandom.current().nextBoolean(),
            tier = listOf("BRONZE", "SILVER", "GOLD", "PLATINUM").random(),
            region = listOf("APAC", "EMEA", "AMER").random(),
            language = listOf("ko", "en", "ja", "zh").random(),
            currency = listOf("KRW", "USD", "JPY", "EUR").random(),
            timezone = listOf("Asia/Seoul", "America/New_York", "Europe/London").random(),

            // 대량 배열/맵
            tags = (1..EXTREME_TAGS_COUNT).map { "tag-${randomString(5)}" },
            permissions = (1..EXTREME_PERMISSIONS_COUNT).map { "permission-${randomString(6)}" },
            metadata = (1..EXTREME_METADATA_COUNT).associate { "key-$it" to "value-${randomString(10)}" },
            scores = (1..EXTREME_SCORES_COUNT).associate { "score-$it" to ThreadLocalRandom.current().nextInt(0, 100) },

            // 2단계 중첩
            addresses = (1..EXTREME_ADDRESSES_COUNT).map { generateAddressWithContacts() },

            // 4단계 중첩 (orders → items → attributes → values)
            orders = (1..EXTREME_ORDERS_COUNT).map { generateExtremeOrder() },

            // 4단계 중첩 (organizations → departments → teams → members)
            organizations = (1..EXTREME_ORGS_COUNT).map { generateOrganization() },

            // 3단계 중첩 (events → participants → roles)
            events = (1..EXTREME_EVENTS_COUNT).map { generateEvent() },

            description = "Extreme Description: ${randomString(200)}",
            notes = "Extreme Notes: ${randomString(150)}",
            additionalInfo = "Additional Info: ${randomString(100)}"
        )
    }

    // ============================================
    // Phase 6: Ultra용 헬퍼 메서드
    // ============================================

    /**
     * 연락처 객체 생성
     *
     * @return ContactDto
     */
    private fun generateContact(): ContactDto {
        val roles = listOf("PRIMARY", "SECONDARY", "EMERGENCY", "BILLING")
        return ContactDto(
            name = "Contact-${randomString(6)}",
            phone = "+82-10-${randomDigits(4)}-${randomDigits(4)}",
            email = "${randomString(8)}@contact.com",
            role = roles.random()
        )
    }

    /**
     * 연락처가 포함된 주소 객체 생성 (2단계 중첩)
     *
     * @return AddressWithContactsDto
     */
    private fun generateAddressWithContacts(): AddressWithContactsDto {
        val cities = listOf("Seoul", "Busan", "Incheon", "Daegu", "Daejeon", "Gwangju", "Ulsan")
        val countries = listOf("Korea", "Japan", "USA", "China", "Germany")
        return AddressWithContactsDto(
            city = cities.random(),
            street = "${randomDigits(3)} ${randomString(10)} Street",
            zipcode = randomDigits(5),
            country = countries.random(),
            contacts = (1..ULTRA_CONTACTS_PER_ADDRESS).map { generateContact() }
        )
    }

    /**
     * 속성 객체 생성
     *
     * @return AttributeDto
     */
    private fun generateAttribute(): AttributeDto {
        val types = listOf("STRING", "NUMBER", "BOOLEAN", "DATE")
        return AttributeDto(
            key = "attr-${randomString(4)}",
            value = "val-${randomString(8)}",
            type = types.random()
        )
    }

    /**
     * 속성이 포함된 아이템 객체 생성 (3단계 중첩)
     *
     * @return ItemWithAttributesDto
     */
    private fun generateItemWithAttributes(): ItemWithAttributesDto {
        val quantity = ThreadLocalRandom.current().nextInt(1, 10)
        val price = ThreadLocalRandom.current().nextDouble(10.0, 500.0)
        return ItemWithAttributesDto(
            productId = "prod-${randomString(8)}",
            name = "Product-${randomString(6)}",
            quantity = quantity,
            price = price,
            attributes = (1..ULTRA_ATTRIBUTES_PER_ITEM).map { generateAttribute() }
        )
    }

    /**
     * 속성이 포함된 주문 객체 생성 (3단계 중첩)
     *
     * @return OrderWithAttributesDto
     */
    private fun generateOrderWithAttributes(): OrderWithAttributesDto {
        val items = (1..ULTRA_ITEMS_PER_ORDER).map { generateItemWithAttributes() }
        val statuses = listOf("PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED")
        return OrderWithAttributesDto(
            orderId = "order-${randomString(12)}",
            amount = items.sumOf { it.price * it.quantity },
            timestamp = System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(2592000000),
            status = statuses.random(),
            items = items
        )
    }

    /**
     * 서브카테고리 객체 생성
     *
     * @return SubcategoryDto
     */
    private fun generateSubcategory(): SubcategoryDto {
        return SubcategoryDto(
            id = "subcat-${randomString(6)}",
            name = "Subcategory-${randomString(8)}",
            items = (1..ULTRA_ITEMS_PER_SUBCATEGORY).map { "item-${randomString(6)}" }
        )
    }

    /**
     * 카테고리 객체 생성 (3단계 중첩)
     *
     * @return CategoryDto
     */
    private fun generateCategory(): CategoryDto {
        return CategoryDto(
            id = "cat-${randomString(6)}",
            name = "Category-${randomString(8)}",
            description = "Category description: ${randomString(30)}",
            subcategories = (1..ULTRA_SUBCATEGORIES_PER_CATEGORY).map { generateSubcategory() }
        )
    }

    /**
     * 변경 이력 객체 생성
     *
     * @return ChangeDto
     */
    private fun generateChange(): ChangeDto {
        val fields = listOf("name", "email", "status", "score", "level", "rating")
        return ChangeDto(
            field = fields.random(),
            oldValue = "old-${randomString(8)}",
            newValue = "new-${randomString(8)}"
        )
    }

    /**
     * 히스토리 엔트리 객체 생성 (2단계 중첩)
     *
     * @return HistoryEntryDto
     */
    private fun generateHistoryEntry(): HistoryEntryDto {
        val actions = listOf("CREATE", "UPDATE", "DELETE", "RESTORE")
        return HistoryEntryDto(
            id = "hist-${randomString(8)}",
            timestamp = System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(86400000L * 30),
            action = actions.random(),
            changes = (1..ULTRA_CHANGES_PER_HISTORY).map { generateChange() }
        )
    }

    // ============================================
    // Phase 6: Extreme용 헬퍼 메서드
    // ============================================

    /**
     * 속성 값 객체 생성 (4단계 중첩용)
     *
     * @return AttributeValueDto
     */
    private fun generateAttributeValue(): AttributeValueDto {
        return AttributeValueDto(
            value = "val-${randomString(6)}",
            label = "Label-${randomString(4)}",
            isDefault = ThreadLocalRandom.current().nextBoolean()
        )
    }

    /**
     * 값이 포함된 속성 객체 생성 (4단계 중첩)
     *
     * @return AttributeWithValuesDto
     */
    private fun generateAttributeWithValues(): AttributeWithValuesDto {
        val types = listOf("STRING", "NUMBER", "BOOLEAN", "ENUM", "DATE")
        return AttributeWithValuesDto(
            key = "attr-${randomString(4)}",
            type = types.random(),
            values = (1..EXTREME_VALUES_PER_ATTRIBUTE).map { generateAttributeValue() }
        )
    }

    /**
     * Extreme 아이템 객체 생성 (4단계 중첩)
     *
     * @return ExtremeItemDto
     */
    private fun generateExtremeItem(): ExtremeItemDto {
        val quantity = ThreadLocalRandom.current().nextInt(1, 10)
        val price = ThreadLocalRandom.current().nextDouble(10.0, 500.0)
        return ExtremeItemDto(
            productId = "prod-${randomString(8)}",
            name = "Product-${randomString(6)}",
            quantity = quantity,
            price = price,
            attributes = (1..EXTREME_ATTRIBUTES_PER_ITEM).map { generateAttributeWithValues() }
        )
    }

    /**
     * Extreme 주문 객체 생성 (4단계 중첩)
     *
     * @return ExtremeOrderDto
     */
    private fun generateExtremeOrder(): ExtremeOrderDto {
        val items = (1..EXTREME_ITEMS_PER_ORDER).map { generateExtremeItem() }
        val statuses = listOf("PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED")
        return ExtremeOrderDto(
            orderId = "order-${randomString(12)}",
            amount = items.sumOf { it.price * it.quantity },
            timestamp = System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(2592000000),
            status = statuses.random(),
            priority = ThreadLocalRandom.current().nextInt(1, 5),
            items = items
        )
    }

    /**
     * 멤버 객체 생성 (4단계 중첩)
     *
     * @return MemberDto
     */
    private fun generateMember(): MemberDto {
        val roles = listOf("DEVELOPER", "DESIGNER", "MANAGER", "ANALYST", "LEAD")
        return MemberDto(
            id = "member-${randomString(6)}",
            name = "Member-${randomString(8)}",
            role = roles.random(),
            email = "${randomString(8)}@company.com"
        )
    }

    /**
     * 팀 객체 생성 (4단계 중첩)
     *
     * @return TeamDto
     */
    private fun generateTeam(): TeamDto {
        return TeamDto(
            id = "team-${randomString(6)}",
            name = "Team-${randomString(8)}",
            members = (1..EXTREME_MEMBERS_PER_TEAM).map { generateMember() }
        )
    }

    /**
     * 부서 객체 생성 (3단계 중첩)
     *
     * @return DepartmentDto
     */
    private fun generateDepartment(): DepartmentDto {
        return DepartmentDto(
            id = "dept-${randomString(6)}",
            name = "Department-${randomString(8)}",
            budget = ThreadLocalRandom.current().nextDouble(100000.0, 10000000.0),
            teams = (1..EXTREME_TEAMS_PER_DEPT).map { generateTeam() }
        )
    }

    /**
     * 조직 객체 생성 (4단계 중첩)
     *
     * @return OrganizationDto
     */
    private fun generateOrganization(): OrganizationDto {
        val types = listOf("CORPORATION", "STARTUP", "NON_PROFIT", "GOVERNMENT")
        return OrganizationDto(
            id = "org-${randomString(6)}",
            name = "Organization-${randomString(8)}",
            type = types.random(),
            departments = (1..EXTREME_DEPTS_PER_ORG).map { generateDepartment() }
        )
    }

    /**
     * 역할 객체 생성 (4단계 중첩)
     *
     * @return RoleDto
     */
    private fun generateRole(): RoleDto {
        val roleNames = listOf("SPEAKER", "ATTENDEE", "ORGANIZER", "MODERATOR", "VIP")
        return RoleDto(
            id = "role-${randomString(6)}",
            name = roleNames.random(),
            permissions = (1..3).map { "perm-${randomString(4)}" }
        )
    }

    /**
     * 참가자 객체 생성 (3단계 중첩)
     *
     * @return ParticipantDto
     */
    private fun generateParticipant(): ParticipantDto {
        return ParticipantDto(
            id = "participant-${randomString(6)}",
            name = "Participant-${randomString(8)}",
            email = "${randomString(8)}@event.com",
            roles = (1..EXTREME_ROLES_PER_PARTICIPANT).map { generateRole() }
        )
    }

    /**
     * 이벤트 객체 생성 (3단계 중첩)
     *
     * @return EventDto
     */
    private fun generateEvent(): EventDto {
        val locations = listOf("Seoul", "Busan", "Tokyo", "Singapore", "New York")
        return EventDto(
            id = "event-${randomString(6)}",
            name = "Event-${randomString(8)}",
            timestamp = System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(86400000L * 365),
            location = locations.random(),
            participants = (1..EXTREME_PARTICIPANTS_PER_EVENT).map { generateParticipant() }
        )
    }

    // ============================================
    // Phase 5: 기존 헬퍼 메서드
    // ============================================

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

// ============================================
// Phase 6: Ultra DTO 클래스
// ============================================

/**
 * 연락처 DTO (2단계 중첩용)
 */
data class ContactDto(
    val name: String,
    val phone: String,
    val email: String,
    val role: String
)

/**
 * 연락처가 포함된 주소 DTO (2단계 중첩)
 */
data class AddressWithContactsDto(
    val city: String,
    val street: String,
    val zipcode: String,
    val country: String,
    val contacts: List<ContactDto>
)

/**
 * 속성 DTO (3단계 중첩용)
 */
data class AttributeDto(
    val key: String,
    val value: String,
    val type: String
)

/**
 * 속성이 포함된 아이템 DTO (3단계 중첩)
 */
data class ItemWithAttributesDto(
    val productId: String,
    val name: String,
    val quantity: Int,
    val price: Double,
    val attributes: List<AttributeDto>
)

/**
 * 속성이 포함된 주문 DTO (3단계 중첩)
 */
data class OrderWithAttributesDto(
    val orderId: String,
    val amount: Double,
    val timestamp: Long,
    val status: String,
    val items: List<ItemWithAttributesDto>
)

/**
 * 서브카테고리 DTO (3단계 중첩용)
 */
data class SubcategoryDto(
    val id: String,
    val name: String,
    val items: List<String>
)

/**
 * 카테고리 DTO (3단계 중첩)
 */
data class CategoryDto(
    val id: String,
    val name: String,
    val description: String,
    val subcategories: List<SubcategoryDto>
)

/**
 * 변경 이력 DTO (2단계 중첩용)
 */
data class ChangeDto(
    val field: String,
    val oldValue: String,
    val newValue: String
)

/**
 * 히스토리 엔트리 DTO (2단계 중첩)
 */
data class HistoryEntryDto(
    val id: String,
    val timestamp: Long,
    val action: String,
    val changes: List<ChangeDto>
)

/**
 * Ultra 데이터 DTO (~150개 필드, 3단계 중첩)
 */
data class UltraDataDto(
    // 기본 필드 15개
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
    val priority: Int,
    val level: Int,
    val rating: Double,
    val verified: Boolean,
    val premium: Boolean,

    // 배열/맵
    val tags: List<String>,
    val permissions: List<String>,
    val metadata: Map<String, String>,
    val scores: Map<String, Int>,

    // 2단계 중첩
    val addresses: List<AddressWithContactsDto>,

    // 3단계 중첩
    val orders: List<OrderWithAttributesDto>,
    val categories: List<CategoryDto>,
    val history: List<HistoryEntryDto>,

    val description: String,
    val notes: String
)

// ============================================
// Phase 6: Extreme DTO 클래스
// ============================================

/**
 * 속성 값 DTO (4단계 중첩용)
 */
data class AttributeValueDto(
    val value: String,
    val label: String,
    val isDefault: Boolean
)

/**
 * 값이 포함된 속성 DTO (4단계 중첩)
 */
data class AttributeWithValuesDto(
    val key: String,
    val type: String,
    val values: List<AttributeValueDto>
)

/**
 * Extreme 아이템 DTO (4단계 중첩)
 */
data class ExtremeItemDto(
    val productId: String,
    val name: String,
    val quantity: Int,
    val price: Double,
    val attributes: List<AttributeWithValuesDto>
)

/**
 * Extreme 주문 DTO (4단계 중첩)
 */
data class ExtremeOrderDto(
    val orderId: String,
    val amount: Double,
    val timestamp: Long,
    val status: String,
    val priority: Int,
    val items: List<ExtremeItemDto>
)

/**
 * 멤버 DTO (4단계 중첩용)
 */
data class MemberDto(
    val id: String,
    val name: String,
    val role: String,
    val email: String
)

/**
 * 팀 DTO (4단계 중첩)
 */
data class TeamDto(
    val id: String,
    val name: String,
    val members: List<MemberDto>
)

/**
 * 부서 DTO (3단계 중첩)
 */
data class DepartmentDto(
    val id: String,
    val name: String,
    val budget: Double,
    val teams: List<TeamDto>
)

/**
 * 조직 DTO (4단계 중첩)
 */
data class OrganizationDto(
    val id: String,
    val name: String,
    val type: String,
    val departments: List<DepartmentDto>
)

/**
 * 역할 DTO (4단계 중첩용)
 */
data class RoleDto(
    val id: String,
    val name: String,
    val permissions: List<String>
)

/**
 * 참가자 DTO (3단계 중첩)
 */
data class ParticipantDto(
    val id: String,
    val name: String,
    val email: String,
    val roles: List<RoleDto>
)

/**
 * 이벤트 DTO (3단계 중첩)
 */
data class EventDto(
    val id: String,
    val name: String,
    val timestamp: Long,
    val location: String,
    val participants: List<ParticipantDto>
)

/**
 * Extreme 데이터 DTO (~500개 필드, 4단계 중첩)
 */
data class ExtremeDataDto(
    // 기본 필드 20개
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
    val priority: Int,
    val level: Int,
    val rating: Double,
    val verified: Boolean,
    val premium: Boolean,
    val tier: String,
    val region: String,
    val language: String,
    val currency: String,
    val timezone: String,

    // 대량 배열/맵
    val tags: List<String>,
    val permissions: List<String>,
    val metadata: Map<String, String>,
    val scores: Map<String, Int>,

    // 2단계 중첩
    val addresses: List<AddressWithContactsDto>,

    // 4단계 중첩
    val orders: List<ExtremeOrderDto>,
    val organizations: List<OrganizationDto>,

    // 3단계 중첩
    val events: List<EventDto>,

    val description: String,
    val notes: String,
    val additionalInfo: String
)