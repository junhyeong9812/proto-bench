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
 * Phase 6: Ultra/Extreme 복잡도 RPC 추가
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
    // Phase 6: Ultra/Extreme 복잡도 RPC 추가
    // ============================================

    /**
     * 복잡한 데이터 RPC: 복잡도에 따른 데이터 반환
     *
     * 클라이언트가 요청한 복잡도(simple, medium, complex, ultra, extreme)에 맞는
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
            // Phase 6 추가
            "ultra" -> buildUltraResponse(request.requestId, timestamp)
            "extreme" -> buildExtremeResponse(request.requestId, timestamp)
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
            .setResponseSize(response.serializedSize)
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
            .setResponseSize(response.serializedSize)
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
            .setResponseSize(response.serializedSize)
            .setComplex(complexData)
            .build()
    }

    // ============================================
    // Phase 6: Ultra 응답 빌드 (~200회 빌더 호출)
    // ============================================

    /**
     * Ultra 응답 빌드
     *
     * 3단계 중첩 구조를 포함하는 Ultra 데이터를 빌드한다.
     * - addresses → contacts (2단계)
     * - orders → items → attributes (3단계)
     * - categories → subcategories → items (3단계)
     * - history → changes (2단계)
     *
     * @param requestId 요청 ID
     * @param timestamp 타임스탬프
     * @return ComplexDataResponse
     */
    private fun buildUltraResponse(requestId: String, timestamp: Long): ComplexDataResponse {
        val data = dataService.generateUltraData(requestId)

        val ultraData = UltraData.newBuilder()
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

        val response = ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setUltra(ultraData)
            .build()

        return ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setResponseSize(response.serializedSize)
            .setUltra(ultraData)
            .build()
    }

    // ============================================
    // Phase 6: Extreme 응답 빌드 (~800회 빌더 호출)
    // ============================================

    /**
     * Extreme 응답 빌드
     *
     * 4단계 중첩 구조를 포함하는 Extreme 데이터를 빌드한다.
     * - addresses → contacts (2단계)
     * - orders → items → attributes → values (4단계)
     * - organizations → departments → teams → members (4단계)
     * - events → participants → roles (3단계)
     *
     * @param requestId 요청 ID
     * @param timestamp 타임스탬프
     * @return ComplexDataResponse
     */
    private fun buildExtremeResponse(requestId: String, timestamp: Long): ComplexDataResponse {
        val data = dataService.generateExtremeData(requestId)

        val extremeData = ExtremeData.newBuilder()
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

        val response = ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setExtreme(extremeData)
            .build()

        return ComplexDataResponse.newBuilder()
            .setRequestId(requestId)
            .setTimestamp(timestamp)
            .setResponseSize(response.serializedSize)
            .setExtreme(extremeData)
            .build()
    }

    // ============================================
    // Phase 5: 기존 Proto 빌드 헬퍼
    // ============================================

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

    // ============================================
    // Phase 6: Ultra용 Proto 빌드 헬퍼
    // ============================================

    /**
     * Contact Proto 빌드
     *
     * @param contact ContactDto
     * @return Contact Proto
     */
    private fun buildContact(contact: ContactDto): Contact {
        return Contact.newBuilder()
            .setName(contact.name)
            .setPhone(contact.phone)
            .setEmail(contact.email)
            .setRole(contact.role)
            .build()
    }

    /**
     * AddressWithContacts Proto 빌드 (2단계 중첩)
     *
     * @param addr AddressWithContactsDto
     * @return AddressWithContacts Proto
     */
    private fun buildAddressWithContacts(addr: AddressWithContactsDto): AddressWithContacts {
        return AddressWithContacts.newBuilder()
            .setCity(addr.city)
            .setStreet(addr.street)
            .setZipcode(addr.zipcode)
            .setCountry(addr.country)
            .addAllContacts(addr.contacts.map { buildContact(it) })
            .build()
    }

    /**
     * Attribute Proto 빌드
     *
     * @param attr AttributeDto
     * @return Attribute Proto
     */
    private fun buildAttribute(attr: AttributeDto): Attribute {
        return Attribute.newBuilder()
            .setKey(attr.key)
            .setValue(attr.value)
            .setType(attr.type)
            .build()
    }

    /**
     * ItemWithAttributes Proto 빌드 (3단계 중첩)
     *
     * @param item ItemWithAttributesDto
     * @return ItemWithAttributes Proto
     */
    private fun buildItemWithAttributes(item: ItemWithAttributesDto): ItemWithAttributes {
        return ItemWithAttributes.newBuilder()
            .setProductId(item.productId)
            .setName(item.name)
            .setQuantity(item.quantity)
            .setPrice(item.price)
            .addAllAttributes(item.attributes.map { buildAttribute(it) })
            .build()
    }

    /**
     * OrderWithAttributes Proto 빌드 (3단계 중첩)
     *
     * @param order OrderWithAttributesDto
     * @return OrderWithAttributes Proto
     */
    private fun buildOrderWithAttributes(order: OrderWithAttributesDto): OrderWithAttributes {
        return OrderWithAttributes.newBuilder()
            .setOrderId(order.orderId)
            .setAmount(order.amount)
            .setTimestamp(order.timestamp)
            .setStatus(order.status)
            .addAllItems(order.items.map { buildItemWithAttributes(it) })
            .build()
    }

    /**
     * Subcategory Proto 빌드
     *
     * @param subcat SubcategoryDto
     * @return Subcategory Proto
     */
    private fun buildSubcategory(subcat: SubcategoryDto): Subcategory {
        return Subcategory.newBuilder()
            .setId(subcat.id)
            .setName(subcat.name)
            .addAllItems(subcat.items)
            .build()
    }

    /**
     * Category Proto 빌드 (3단계 중첩)
     *
     * @param cat CategoryDto
     * @return Category Proto
     */
    private fun buildCategory(cat: CategoryDto): Category {
        return Category.newBuilder()
            .setId(cat.id)
            .setName(cat.name)
            .setDescription(cat.description)
            .addAllSubcategories(cat.subcategories.map { buildSubcategory(it) })
            .build()
    }

    /**
     * Change Proto 빌드
     *
     * @param change ChangeDto
     * @return Change Proto
     */
    private fun buildChange(change: ChangeDto): Change {
        return Change.newBuilder()
            .setField(change.field)
            .setOldValue(change.oldValue)
            .setNewValue(change.newValue)
            .build()
    }

    /**
     * HistoryEntry Proto 빌드 (2단계 중첩)
     *
     * @param entry HistoryEntryDto
     * @return HistoryEntry Proto
     */
    private fun buildHistoryEntry(entry: HistoryEntryDto): HistoryEntry {
        return HistoryEntry.newBuilder()
            .setId(entry.id)
            .setTimestamp(entry.timestamp)
            .setAction(entry.action)
            .addAllChanges(entry.changes.map { buildChange(it) })
            .build()
    }

    // ============================================
    // Phase 6: Extreme용 Proto 빌드 헬퍼
    // ============================================

    /**
     * AttributeValue Proto 빌드 (4단계 중첩)
     *
     * @param av AttributeValueDto
     * @return AttributeValue Proto
     */
    private fun buildAttributeValue(av: AttributeValueDto): AttributeValue {
        return AttributeValue.newBuilder()
            .setValue(av.value)
            .setLabel(av.label)
            .setIsDefault(av.isDefault)
            .build()
    }

    /**
     * AttributeWithValues Proto 빌드 (4단계 중첩)
     *
     * @param attr AttributeWithValuesDto
     * @return AttributeWithValues Proto
     */
    private fun buildAttributeWithValues(attr: AttributeWithValuesDto): AttributeWithValues {
        return AttributeWithValues.newBuilder()
            .setKey(attr.key)
            .setType(attr.type)
            .addAllValues(attr.values.map { buildAttributeValue(it) })
            .build()
    }

    /**
     * ExtremeItem Proto 빌드 (4단계 중첩)
     *
     * @param item ExtremeItemDto
     * @return ExtremeItem Proto
     */
    private fun buildExtremeItem(item: ExtremeItemDto): ExtremeItem {
        return ExtremeItem.newBuilder()
            .setProductId(item.productId)
            .setName(item.name)
            .setQuantity(item.quantity)
            .setPrice(item.price)
            .addAllAttributes(item.attributes.map { buildAttributeWithValues(it) })
            .build()
    }

    /**
     * ExtremeOrder Proto 빌드 (4단계 중첩)
     *
     * @param order ExtremeOrderDto
     * @return ExtremeOrder Proto
     */
    private fun buildExtremeOrder(order: ExtremeOrderDto): ExtremeOrder {
        return ExtremeOrder.newBuilder()
            .setOrderId(order.orderId)
            .setAmount(order.amount)
            .setTimestamp(order.timestamp)
            .setStatus(order.status)
            .setPriority(order.priority)
            .addAllItems(order.items.map { buildExtremeItem(it) })
            .build()
    }

    /**
     * Member Proto 빌드 (4단계 중첩)
     *
     * @param member MemberDto
     * @return Member Proto
     */
    private fun buildMember(member: MemberDto): Member {
        return Member.newBuilder()
            .setId(member.id)
            .setName(member.name)
            .setRole(member.role)
            .setEmail(member.email)
            .build()
    }

    /**
     * Team Proto 빌드 (4단계 중첩)
     *
     * @param team TeamDto
     * @return Team Proto
     */
    private fun buildTeam(team: TeamDto): Team {
        return Team.newBuilder()
            .setId(team.id)
            .setName(team.name)
            .addAllMembers(team.members.map { buildMember(it) })
            .build()
    }

    /**
     * Department Proto 빌드 (3단계 중첩)
     *
     * @param dept DepartmentDto
     * @return Department Proto
     */
    private fun buildDepartment(dept: DepartmentDto): Department {
        return Department.newBuilder()
            .setId(dept.id)
            .setName(dept.name)
            .setBudget(dept.budget)
            .addAllTeams(dept.teams.map { buildTeam(it) })
            .build()
    }

    /**
     * Organization Proto 빌드 (4단계 중첩)
     *
     * @param org OrganizationDto
     * @return Organization Proto
     */
    private fun buildOrganization(org: OrganizationDto): Organization {
        return Organization.newBuilder()
            .setId(org.id)
            .setName(org.name)
            .setType(org.type)
            .addAllDepartments(org.departments.map { buildDepartment(it) })
            .build()
    }

    /**
     * Role Proto 빌드 (4단계 중첩)
     *
     * @param role RoleDto
     * @return Role Proto
     */
    private fun buildRole(role: RoleDto): Role {
        return Role.newBuilder()
            .setId(role.id)
            .setName(role.name)
            .addAllPermissions(role.permissions)
            .build()
    }

    /**
     * Participant Proto 빌드 (3단계 중첩)
     *
     * @param participant ParticipantDto
     * @return Participant Proto
     */
    private fun buildParticipant(participant: ParticipantDto): Participant {
        return Participant.newBuilder()
            .setId(participant.id)
            .setName(participant.name)
            .setEmail(participant.email)
            .addAllRoles(participant.roles.map { buildRole(it) })
            .build()
    }

    /**
     * Event Proto 빌드 (3단계 중첩)
     *
     * @param event EventDto
     * @return Event Proto
     */
    private fun buildEvent(event: EventDto): Event {
        return Event.newBuilder()
            .setId(event.id)
            .setName(event.name)
            .setTimestamp(event.timestamp)
            .setLocation(event.location)
            .addAllParticipants(event.participants.map { buildParticipant(it) })
            .build()
    }
}