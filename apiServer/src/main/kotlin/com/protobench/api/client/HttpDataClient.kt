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

/**
 * dataServer HTTP 클라이언트
 *
 * WebClient를 사용하여 dataServer의 HTTP 엔드포인트를 호출한다.
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
}