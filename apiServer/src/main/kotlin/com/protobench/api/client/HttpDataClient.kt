package com.protobench.api.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

data class HttpDataResponse(
    val requestId: String,
    val payload: String,
    val timestamp: Long,
    val payloadSize: Int
)

// ============================================
// Phase 5: 복잡한 데이터 DTO
// ============================================

/**
 * 복잡한 데이터 JSON 응답 (공통)
 */
data class ComplexDataJsonResponse(
    val requestId: String,
    val timestamp: Long,
    val serializedSize: Int,
    val complexity: String,
    val data: Map<String, Any>
)

/**
 * dataServer HTTP 클라이언트
 *
 * WebClient를 사용하여 dataServer의 HTTP 엔드포인트를 호출한다.
 *
 * Phase 5: 복잡한 데이터 구조 요청 메서드 추가
 */
@Component
class HttpDataClient(
    @Value("\${data-server.http.url}") private val baseUrl: String
) {
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
        .build()

    /**
     * JSON 형식으로 데이터 요청
     *
     * @param requestId 요청 ID
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return HttpDataResponse (Base64 인코딩된 payload 포함)
     */
    suspend fun getDataJson(requestId: String, size: String = "1mb"): HttpDataResponse {
        return webClient.get()
            .uri("/data/json?requestId=$requestId&size=$size")
            .retrieve()
            .awaitBody()
    }

    /**
     * Binary 형식으로 데이터 요청
     *
     * @param requestId 요청 ID
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return 바이너리 바이트 배열
     */
    suspend fun getDataBinary(requestId: String, size: String = "1mb"): ByteArray {
        return webClient.get()
            .uri("/data/binary?requestId=$requestId&size=$size")
            .retrieve()
            .awaitBody()
    }

    // ============================================
    // Phase 5: 복잡한 데이터 구조 요청
    // ============================================

    /**
     * 복잡한 데이터를 JSON 형식으로 요청
     *
     * @param requestId 요청 ID
     * @param complexity 복잡도 (simple, medium, complex)
     * @return ComplexDataJsonResponse
     */
    suspend fun getComplexDataJson(requestId: String, complexity: String = "simple"): ComplexDataJsonResponse {
        return webClient.get()
            .uri("/data/complex/json?requestId=$requestId&complexity=$complexity")
            .retrieve()
            .awaitBody()
    }

    /**
     * 복잡한 데이터를 Protobuf Binary 형식으로 요청
     *
     * @param requestId 요청 ID
     * @param complexity 복잡도 (simple, medium, complex)
     * @return 바이너리 바이트 배열 (Protobuf 인코딩)
     */
    suspend fun getComplexDataBinary(requestId: String, complexity: String = "simple"): ByteArray {
        return webClient.get()
            .uri("/data/complex/binary?requestId=$requestId&complexity=$complexity")
            .retrieve()
            .awaitBody()
    }
}