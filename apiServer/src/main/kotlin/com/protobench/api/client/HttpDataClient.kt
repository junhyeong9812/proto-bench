package com.protobench.api.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

/**
 * HTTP 응답 DTO
 */
data class HttpDataResponse(
    val requestId: String,
    val payload: String,
    val timestamp: Long,
    val payloadSize: Int
)

/**
 * dataServer HTTP 클라이언트
 */
@Component
class HttpDataClient(
    @Value("\${data-server.http.url}") private val baseUrl: String
) {
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }  // 10MB
        .build()

    /**
     * JSON 형태로 데이터 요청
     */
    suspend fun getDataJson(requestId: String): HttpDataResponse {
        return webClient.get()
            .uri("/data/json?requestId=$requestId")
            .retrieve()
            .awaitBody()
    }

    /**
     * Binary 형태로 데이터 요청
     */
    suspend fun getDataBinary(requestId: String): ByteArray {
        return webClient.get()
            .uri("/data/binary?requestId=$requestId")
            .retrieve()
            .awaitBody()
    }
}