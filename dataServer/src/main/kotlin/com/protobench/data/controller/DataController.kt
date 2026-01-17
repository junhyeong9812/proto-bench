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
 * Phase 6: Ultra/Extreme 복잡도 엔드포인트 추가
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
            "supportedComplexities" to listOf("simple", "medium", "complex", "ultra", "extreme")
        )
    }

    // ============================================
    // Phase 5: 복잡한 데이터 구조 엔드포인트
    // Phase 6: Ultra/Extreme 복잡도 추가
    // ============================================

    /**
     * 복잡한 데이터를 JSON 형식으로 반환
     *
     * @param requestId 요청 ID
     * @param complexity 복잡도 (simple, medium, complex, ultra, extreme)
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
            // ============================================
            // Phase 6: Ultra/Extreme 복잡도 처리
            // ============================================
            "ultra" -> {
                val data = dataService.generateUltraData(requestId)
                val response = ComplexDataJsonResponse(
                    requestId = requestId,
                    timestamp = timestamp,
                    serializedSize = estimateJsonSize(data),
                    complexity = "ultra",
                    data = data
                )
                ResponseEntity.ok(response)
            }
            "extreme" -> {
                val data = dataService.generateExtremeData(requestId)
                val response = ComplexDataJsonResponse(
                    requestId = requestId,
                    timestamp = timestamp,
                    serializedSize = estimateJsonSize(data),
                    complexity = "extreme",
                    data = data
                )
                ResponseEntity.ok(response)
            }
            else -> {
                ResponseEntity.badRequest().body(
                    mapOf("error" to "Invalid complexity. Use: simple, medium, complex, ultra, extreme")
                )
            }
        }
    }

    /**
     * 복잡한 데이터를 Protobuf Binary 형식으로 반환
     *
     * @param requestId 요청 ID
     * @param complexity 복잡도 (simple, medium, complex, ultra, extreme)
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
            // ============================================
            // Phase 6: Ultra/Extreme Protobuf 빌드
            // ============================================
            "ultra" -> {
                val data = dataService.generateUltraData(requestId)
                buildUltraProtoResponse(requestId, timestamp, data)
            }
            "extreme" -> {
                val data = dataService.generateExtremeData(requestId)
                buildExtremeProtoResponse(requestId, timestamp, data)
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

    // ============================================
    // Phase 6: Ultra Protobuf 응답 빌드
    // ============================================

    /**
     * Ultra Protobuf 응답 빌드 (~200회 빌더 호출)
     *
     * 3단계 중첩 구조를 Protobuf로 변환한다.
     *
     * @param requestId 요청 ID
     * @param timestamp 타임스탬프
     * @param data UltraDataDto
     * @return Protobuf 바이너리 바이트 배열
     */
    private fun buildUltraProtoResponse(requestId: String, timestamp: Long, data: UltraDataDto): ByteArray {
        // Contact 빌드 함수
        fun buildContact(contact: ContactDto) = com.protobench.proto.Contact.newBuilder()
            .setName(contact.name)
            .setPhone(contact.phone)
            .setEmail(contact.email)
            .setRole(contact.role)
            .build()

        // AddressWithContacts 빌드 함수 (2단계 중첩)
        fun buildAddressWithContacts(addr: AddressWithContactsDto) = com.protobench.proto.AddressWithContacts.newBuilder()
            .setCity(addr.city)
            .setStreet(addr.street)
            .setZipcode(addr.zipcode)
            .setCountry(addr.country)
            .addAllContacts(addr.contacts.map { buildContact(it) })
            .build()

        // Attribute 빌드 함수
        fun buildAttribute(attr: AttributeDto) = com.protobench.proto.Attribute.newBuilder()
            .setKey(attr.key)
            .setValue(attr.value)
            .setType(attr.type)
            .build()

        // ItemWithAttributes 빌드 함수 (3단계 중첩)
        fun buildItemWithAttributes(item: ItemWithAttributesDto) = com.protobench.proto.ItemWithAttributes.newBuilder()
            .setProductId(item.productId)
            .setName(item.name)
            .setQuantity(item.quantity)
            .setPrice(item.price)
            .addAllAttributes(item.attributes.map { buildAttribute(it) })
            .build()

        // OrderWithAttributes 빌드 함수 (3단계 중첩)
        fun buildOrderWithAttributes(order: OrderWithAttributesDto) = com.protobench.proto.OrderWithAttributes.newBuilder()
            .setOrderId(order.orderId)
            .setAmount(order.amount)
            .setTimestamp(order.timestamp)
            .setStatus(order.status)
            .addAllItems(order.items.map { buildItemWithAttributes(it) })
            .build()

        // Subcategory 빌드 함수
        fun buildSubcategory(subcat: SubcategoryDto) = com.protobench.proto.Subcategory.newBuilder()
            .setId(subcat.id)
            .setName(subcat.name)
            .addAllItems(subcat.items)
            .build()

        // Category 빌드 함수 (3단계 중첩)
        fun buildCategory(cat: CategoryDto) = com.protobench.proto.Category.newBuilder()
            .setId(cat.id)
            .setName(cat.name)
            .setDescription(cat.description)
            .addAllSubcategories(cat.subcategories.map { buildSubcategory(it) })
            .build()

        // Change 빌드 함수
        fun buildChange(change: ChangeDto) = com.protobench.proto.Change.newBuilder()
            .setField(change.field)
            .setOldValue(change.oldValue)
            .setNewValue(change.newValue)
            .build()

        // HistoryEntry 빌드 함수 (2단계 중첩)
        fun buildHistoryEntry(entry: HistoryEntryDto) = com.protobench.proto.HistoryEntry.newBuilder()
            .setId(entry.id)
            .setTimestamp(entry.timestamp)
            .setAction(entry.action)
            .addAllChanges(entry.changes.map { buildChange(it) })
            .build()

        val ultraData = com.protobench.proto.UltraData.newBuilder()
            // 기본 필드 15개
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
            .setPriority(data.priority)
            .setLevel(data.level)
            .setRating(data.rating)
            .setVerified(data.verified)
            .setPremium(data.premium)
            // 배열/맵
            .addAllTags(data.tags)
            .addAllPermissions(data.permissions)
            .putAllMetadata(data.metadata)
            .putAllScores(data.scores)
            // 2단계 중첩
            .addAllAddresses(data.addresses.map { buildAddressWithContacts(it) })
            // 3단계 중첩
            .addAllOrders(data.orders.map { buildOrderWithAttributes(it) })
            .addAllCategories(data.categories.map { buildCategory(it) })
            .addAllHistory(data.history.map { buildHistoryEntry(it) })
            .setDescription(data.description)
            .setNotes(data.notes)
            .build()

        val response = com.protobench.proto.ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setUltra(ultraData)
            .build()

        return com.protobench.proto.ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setResponseSize(response.serializedSize)
            .setUltra(ultraData)
            .build()
            .toByteArray()
    }

    // ============================================
    // Phase 6: Extreme Protobuf 응답 빌드
    // ============================================

    /**
     * Extreme Protobuf 응답 빌드 (~800회 빌더 호출)
     *
     * 4단계 중첩 구조를 Protobuf로 변환한다.
     *
     * @param requestId 요청 ID
     * @param timestamp 타임스탬프
     * @param data ExtremeDataDto
     * @return Protobuf 바이너리 바이트 배열
     */
    private fun buildExtremeProtoResponse(requestId: String, timestamp: Long, data: ExtremeDataDto): ByteArray {
        // Contact 빌드 함수
        fun buildContact(contact: ContactDto) = com.protobench.proto.Contact.newBuilder()
            .setName(contact.name)
            .setPhone(contact.phone)
            .setEmail(contact.email)
            .setRole(contact.role)
            .build()

        // AddressWithContacts 빌드 함수 (2단계 중첩)
        fun buildAddressWithContacts(addr: AddressWithContactsDto) = com.protobench.proto.AddressWithContacts.newBuilder()
            .setCity(addr.city)
            .setStreet(addr.street)
            .setZipcode(addr.zipcode)
            .setCountry(addr.country)
            .addAllContacts(addr.contacts.map { buildContact(it) })
            .build()

        // AttributeValue 빌드 함수 (4단계 중첩)
        fun buildAttributeValue(av: AttributeValueDto) = com.protobench.proto.AttributeValue.newBuilder()
            .setValue(av.value)
            .setLabel(av.label)
            .setIsDefault(av.isDefault)
            .build()

        // AttributeWithValues 빌드 함수 (4단계 중첩)
        fun buildAttributeWithValues(attr: AttributeWithValuesDto) = com.protobench.proto.AttributeWithValues.newBuilder()
            .setKey(attr.key)
            .setType(attr.type)
            .addAllValues(attr.values.map { buildAttributeValue(it) })
            .build()

        // ExtremeItem 빌드 함수 (4단계 중첩)
        fun buildExtremeItem(item: ExtremeItemDto) = com.protobench.proto.ExtremeItem.newBuilder()
            .setProductId(item.productId)
            .setName(item.name)
            .setQuantity(item.quantity)
            .setPrice(item.price)
            .addAllAttributes(item.attributes.map { buildAttributeWithValues(it) })
            .build()

        // ExtremeOrder 빌드 함수 (4단계 중첩)
        fun buildExtremeOrder(order: ExtremeOrderDto) = com.protobench.proto.ExtremeOrder.newBuilder()
            .setOrderId(order.orderId)
            .setAmount(order.amount)
            .setTimestamp(order.timestamp)
            .setStatus(order.status)
            .setPriority(order.priority)
            .addAllItems(order.items.map { buildExtremeItem(it) })
            .build()

        // Member 빌드 함수 (4단계 중첩)
        fun buildMember(member: MemberDto) = com.protobench.proto.Member.newBuilder()
            .setId(member.id)
            .setName(member.name)
            .setRole(member.role)
            .setEmail(member.email)
            .build()

        // Team 빌드 함수 (4단계 중첩)
        fun buildTeam(team: TeamDto) = com.protobench.proto.Team.newBuilder()
            .setId(team.id)
            .setName(team.name)
            .addAllMembers(team.members.map { buildMember(it) })
            .build()

        // Department 빌드 함수 (3단계 중첩)
        fun buildDepartment(dept: DepartmentDto) = com.protobench.proto.Department.newBuilder()
            .setId(dept.id)
            .setName(dept.name)
            .setBudget(dept.budget)
            .addAllTeams(dept.teams.map { buildTeam(it) })
            .build()

        // Organization 빌드 함수 (4단계 중첩)
        fun buildOrganization(org: OrganizationDto) = com.protobench.proto.Organization.newBuilder()
            .setId(org.id)
            .setName(org.name)
            .setType(org.type)
            .addAllDepartments(org.departments.map { buildDepartment(it) })
            .build()

        // Role 빌드 함수 (4단계 중첩)
        fun buildRole(role: RoleDto) = com.protobench.proto.Role.newBuilder()
            .setId(role.id)
            .setName(role.name)
            .addAllPermissions(role.permissions)
            .build()

        // Participant 빌드 함수 (3단계 중첩)
        fun buildParticipant(participant: ParticipantDto) = com.protobench.proto.Participant.newBuilder()
            .setId(participant.id)
            .setName(participant.name)
            .setEmail(participant.email)
            .addAllRoles(participant.roles.map { buildRole(it) })
            .build()

        // Event 빌드 함수 (3단계 중첩)
        fun buildEvent(event: EventDto) = com.protobench.proto.Event.newBuilder()
            .setId(event.id)
            .setName(event.name)
            .setTimestamp(event.timestamp)
            .setLocation(event.location)
            .addAllParticipants(event.participants.map { buildParticipant(it) })
            .build()

        val extremeData = com.protobench.proto.ExtremeData.newBuilder()
            // 기본 필드 20개
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
            .setPriority(data.priority)
            .setLevel(data.level)
            .setRating(data.rating)
            .setVerified(data.verified)
            .setPremium(data.premium)
            .setTier(data.tier)
            .setRegion(data.region)
            .setLanguage(data.language)
            .setCurrency(data.currency)
            .setTimezone(data.timezone)
            // 대량 배열/맵
            .addAllTags(data.tags)
            .addAllPermissions(data.permissions)
            .putAllMetadata(data.metadata)
            .putAllScores(data.scores)
            // 2단계 중첩
            .addAllAddresses(data.addresses.map { buildAddressWithContacts(it) })
            // 4단계 중첩
            .addAllOrders(data.orders.map { buildExtremeOrder(it) })
            .addAllOrganizations(data.organizations.map { buildOrganization(it) })
            // 3단계 중첩
            .addAllEvents(data.events.map { buildEvent(it) })
            .setDescription(data.description)
            .setNotes(data.notes)
            .setAdditionalInfo(data.additionalInfo)
            .build()

        val response = com.protobench.proto.ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setExtreme(extremeData)
            .build()

        return com.protobench.proto.ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setResponseSize(response.serializedSize)
            .setExtreme(extremeData)
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
            // Phase 6 추가
            is UltraDataDto -> 15000    // ~15KB
            is ExtremeDataDto -> 50000   // ~50KB
            else -> 0
        }
    }
}