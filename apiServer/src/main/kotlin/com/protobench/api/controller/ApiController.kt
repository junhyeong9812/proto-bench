package com.protobench.api.controller

import com.protobench.api.service.ApiDataService
import com.protobench.api.service.ApiResponse
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 데이터 API 컨트롤러 (k6에서 호출)
 */
@RestController
@RequestMapping("/api")
class ApiController(
    private val apiDataService: ApiDataService
) {

    /**
     * HTTP JSON 방식으로 데이터 요청
     * GET /api/data/http/json
     */
    @GetMapping("/data/http/json")
    fun getDataHttpJson(): ResponseEntity<ApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getDataHttpJson())
    }

    /**
     * HTTP Binary 방식으로 데이터 요청
     * GET /api/data/http/binary
     */
    @GetMapping("/data/http/binary")
    fun getDataHttpBinary(): ResponseEntity<ApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getDataHttpBinary())
    }

    /**
     * gRPC Unary 방식으로 데이터 요청
     * GET /api/data/grpc
     */
    @GetMapping("/data/grpc")
    fun getDataGrpc(): ResponseEntity<ApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getDataGrpc())
    }

    /**
     * gRPC Streaming 방식으로 데이터 요청
     * GET /api/data/grpc/stream
     */
    @GetMapping("/data/grpc/stream")
    fun getDataGrpcStream(): ResponseEntity<ApiResponse> = runBlocking {
        ResponseEntity.ok(apiDataService.getDataGrpcStream())
    }

    /**
     * 헬스체크
     * GET /api/health
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "apiServer"
        ))
    }
}