package com.protobench.api.controller

import com.protobench.api.benchmark.BenchmarkCollector
import com.protobench.api.benchmark.BenchmarkResult
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 벤치마크 제어 컨트롤러
 */
@RestController
@RequestMapping("/benchmark")
class BenchmarkController(
    private val benchmarkCollector: BenchmarkCollector
) {

    /**
     * 벤치마크 시작
     * POST /benchmark/start?protocol=HTTP&testName=test1
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
     * POST /benchmark/end
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
     * GET /benchmark/status
     */
    @GetMapping("/status")
    fun getStatus(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(benchmarkCollector.status())
    }
}