package com.protobench.api.controller

import com.protobench.api.service.ApiDataService
import com.protobench.api.service.ApiResponse
import com.protobench.api.service.ComplexApiResponse
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 데이터 API 컨트롤러
 *
 * k6 부하 테스트에서 호출되는 엔드포인트를 제공한다.
 * 각 엔드포인트는 dataServer에 HTTP 또는 gRPC로 요청한다.
 *
 * Phase 5: 복잡한 데이터 구조 엔드포인트 추가
 */
@RestController
@RequestMapping("/api")
class ApiController(
    private val apiDataService: ApiDataService
) {

    /**
     * HTTP JSON 방식으로 데이터 요청
     *
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return ApiResponse
     */
    @GetMapping("/data/http/json")
    fun getDataHttpJson(
        @RequestParam(defaultValue = "1mb") size: String
    ): ResponseEntity<ApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getDataHttpJson(size))
    }

    /**
     * HTTP Binary 방식으로 데이터 요청
     *
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return ApiResponse
     */
    @GetMapping("/data/http/binary")
    fun getDataHttpBinary(
        @RequestParam(defaultValue = "1mb") size: String
    ): ResponseEntity<ApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getDataHttpBinary(size))
    }

    /**
     * gRPC Unary 방식으로 데이터 요청
     *
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return ApiResponse
     */
    @GetMapping("/data/grpc")
    fun getDataGrpc(
        @RequestParam(defaultValue = "1mb") size: String
    ): ResponseEntity<ApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getDataGrpc(size))
    }

    /**
     * gRPC Streaming 방식으로 데이터 요청
     *
     * @param size 페이로드 크기 (1kb, 10kb, 100kb, 1mb)
     * @return ApiResponse
     */
    @GetMapping("/data/grpc/stream")
    fun getDataGrpcStream(
        @RequestParam(defaultValue = "1mb") size: String
    ): ResponseEntity<ApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getDataGrpcStream(size))
    }

    /**
     * 헬스체크
     *
     * @return 서버 상태
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "apiServer"
        ))
    }

    /**
     * 지원하는 페이로드 크기 목록 반환
     *
     * @return 크기 목록 (1kb, 10kb, 50kb, 100kb, 200kb, 500kb, 1mb)
     */
    @GetMapping("/sizes")
    fun getSizes(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(listOf("1kb", "10kb", "50kb", "100kb", "200kb", "500kb", "1mb"))
    }

    // ============================================
    // Phase 5: 복잡한 데이터 구조 엔드포인트
    // ============================================

    /**
     * HTTP JSON 방식으로 복잡한 데이터 요청
     *
     * @param complexity 복잡도 (simple, medium, complex)
     * @return ComplexApiResponse
     */
    @GetMapping("/complex/http/json")
    fun getComplexDataHttpJson(
        @RequestParam(defaultValue = "simple") complexity: String
    ): ResponseEntity<ComplexApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getComplexDataHttpJson(complexity))
    }

    /**
     * HTTP Binary (Protobuf) 방식으로 복잡한 데이터 요청
     *
     * @param complexity 복잡도 (simple, medium, complex)
     * @return ComplexApiResponse
     */
    @GetMapping("/complex/http/binary")
    fun getComplexDataHttpBinary(
        @RequestParam(defaultValue = "simple") complexity: String
    ): ResponseEntity<ComplexApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getComplexDataHttpBinary(complexity))
    }

    /**
     * gRPC Unary 방식으로 복잡한 데이터 요청
     *
     * @param complexity 복잡도 (simple, medium, complex)
     * @return ComplexApiResponse
     */
    @GetMapping("/complex/grpc")
    fun getComplexDataGrpc(
        @RequestParam(defaultValue = "simple") complexity: String
    ): ResponseEntity<ComplexApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getComplexDataGrpc(complexity))
    }

    /**
     * gRPC Stream 방식으로 복잡한 데이터 요청
     *
     * @param complexity 복잡도 (simple, medium, complex)
     * @return ComplexApiResponse
     */
    @GetMapping("/complex/grpc/stream")
    fun getComplexDataGrpcStream(
        @RequestParam(defaultValue = "simple") complexity: String
    ): ResponseEntity<ComplexApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getComplexDataGrpcStream(complexity))
    }

    /**
     * 지원하는 복잡도 목록 반환
     *
     * @return 복잡도 목록 및 설명
     */
    @GetMapping("/complexities")
    fun getComplexities(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "complexities" to listOf("simple", "medium", "complex"),
            "description" to mapOf(
                "simple" to "5 fields, no nesting",
                "medium" to "13 fields, 1 level nesting, arrays",
                "complex" to "20 fields, 2 level nesting, arrays, maps"
            )
        ))
    }
}