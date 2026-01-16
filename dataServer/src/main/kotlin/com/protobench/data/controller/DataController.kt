package com.protobench.data.controller

import com.protobench.data.service.DataService
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
 * HTTP 데이터 컨트롤러
 *
 * dataServer의 HTTP 엔드포인트를 제공한다.
 * JSON(Base64) 또는 Binary 형식으로 페이로드를 반환한다.
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
            "supportedSizes" to DataService.PAYLOAD_SIZES.keys
        )
    }
}