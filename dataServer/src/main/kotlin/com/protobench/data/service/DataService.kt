package com.protobench.data.service

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.util.concurrent.ThreadLocalRandom

/**
 * 페이로드 데이터 관리 서비스
 *
 * 서버 시작 시 다양한 크기의 랜덤 바이트 배열을 메모리에 생성하여 보관.
 * HTTP/gRPC 요청 시 해당 페이로드를 반환한다.
 */
@Service
class DataService {

    companion object {
        val PAYLOAD_SIZES = mapOf(
            "1kb" to 1 * 1024,
            "10kb" to 10 * 1024,
            "100kb" to 100 * 1024,
            "1mb" to 1 * 1024 * 1024
        )
        const val CHUNK_SIZE = 64 * 1024
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
}