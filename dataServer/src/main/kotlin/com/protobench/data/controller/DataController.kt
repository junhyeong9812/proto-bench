package com.protobench.data.controller

import com.protobench.data.service.DataService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.Base64

/**
 * HTTP 데이터 응답 DTO
 */
data class DataResponse(
    val requestId: String,
    val payload: String,  // Base64 encoded
    val timestamp: Long,
    val payloadSize: Int
)

/**
 * HTTP용 데이터 컨트롤러
 */
@RestController
@RequestMapping("/data")
class DataController(
    private val dataService: DataService
) {

    /**
     * JSON으로 1MB 데이터 반환 (Base64 인코딩)
     */
    @GetMapping("/json")
    fun getDataAsJson(
        @RequestParam(defaultValue = "unknown") requestId: String
    ): ResponseEntity<DataResponse> {
        val payload = dataService.getPayload()
        val response = DataResponse(
            requestId = requestId,
            payload = Base64.getEncoder().encodeToString(payload),
            timestamp = System.currentTimeMillis(),
            payloadSize = payload.size
        )
        return ResponseEntity.ok(response)
    }

    /**
     * 바이너리로 1MB 데이터 반환 (raw bytes)
     */
    @GetMapping("/binary", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getDataAsBinary(
        @RequestParam(defaultValue = "unknown") requestId: String
    ): ResponseEntity<ByteArray> {
        val payload = dataService.getPayload()
        return ResponseEntity.ok()
            .header("X-Request-Id", requestId)
            .header("X-Payload-Size", payload.size.toString())
            .header("X-Timestamp", System.currentTimeMillis().toString())
            .body(payload)
    }

    /**
     * 헬스체크 / 메타정보
     */
    @GetMapping("/info")
    fun getInfo(): Map<String, Any> {
        return mapOf(
            "service" to "dataServer",
            "payloadSize" to dataService.payloadSize,
            "chunkSize" to dataService.chunkSize,
            "totalChunks" to dataService.getTotalChunks()
        )
    }
}