package com.protobench.data.service

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.ThreadLocalRandom

@Service
class DataService(
    @Value("\${data.payload-size-mb:1}") private val payloadSizeMb: Int,
    @Value("\${data.chunk-size-kb:64}") private val chunkSizeKb: Int
) {
    private lateinit var payload: ByteArray

    val payloadSize: Int
        get() = payload.size

    val chunkSize: Int
        get() = chunkSizeKb * 1024

    @PostConstruct
    fun init() {
        // 서버 시작 시 1MB 데이터를 메모리에 생성
        val size = payloadSizeMb * 1024 * 1024
        payload = ByteArray(size)
        ThreadLocalRandom.current().nextBytes(payload)
        println("✅ DataService initialized with ${payloadSizeMb}MB payload (${payload.size} bytes)")
    }

    /**
     * 전체 페이로드 반환
     */
    fun getPayload(): ByteArray = payload

    /**
     * 청크 단위로 페이로드 반환 (스트리밍용)
     */
    fun getChunks(): List<ByteArray> {
        return payload.toList()
            .chunked(chunkSize)
            .map { it.toByteArray() }
    }

    /**
     * 특정 인덱스의 청크 반환
     */
    fun getChunk(index: Int): ByteArray? {
        val chunks = getChunks()
        return if (index in chunks.indices) chunks[index] else null
    }

    /**
     * 총 청크 수
     */
    fun getTotalChunks(): Int {
        return (payload.size + chunkSize - 1) / chunkSize
    }
}