package com.protobench.api.controller

import com.protobench.api.benchmark.BenchmarkCollector
import com.protobench.api.benchmark.BenchmarkResult
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 벤치마크 제어 컨트롤러
 *
 * k6 테스트 스크립트에서 호출하여 벤치마크를 시작/종료한다.
 */
@RestController
@RequestMapping("/benchmark")
class BenchmarkController(
    private val benchmarkCollector: BenchmarkCollector
) {

    /**
     * 벤치마크 시작
     *
     * @param protocol 테스트 프로토콜 이름 (HTTP/JSON, gRPC/Unary 등)
     * @param testName 테스트 식별 이름
     * @return 시작 상태 정보
     */
    @PostMapping("/start")
    fun startBenchmark(
        @RequestParam protocol: String,
        @RequestParam(defaultValue = "default") testName: String
    ): ResponseEntity<Map<String, Any>> {
        val result = benchmarkCollector.start(protocol, testName)
        return ResponseEntity.ok(result)
    }

    /**
     * 벤치마크 종료 및 결과 반환
     *
     * @return BenchmarkResult (벤치마크 미실행 시 400 Bad Request)
     */
    @PostMapping("/end")
    fun endBenchmark(): ResponseEntity<BenchmarkResult> {
        val result = benchmarkCollector.end()
        return if (result != null) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 현재 벤치마크 상태 조회
     *
     * @return 상태 정보 (실행 중 여부, 요청 수 등)
     */
    @GetMapping("/status")
    fun getStatus(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(benchmarkCollector.status())
    }
}