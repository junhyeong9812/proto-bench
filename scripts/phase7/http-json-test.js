/**
 * =============================================================================
 * Phase 7: HTTP/JSON 극한 복잡도 데이터 구조 직렬화 성능 테스트 (CPU 측정 포함)
 * =============================================================================
 *
 * 목적: 극한 복잡도(ultra, extreme)에서 JSON 파싱 비용 및 CPU 사용량 측정
 *       gRPC 역전 포인트 탐색
 *
 * 복잡도: ultra (~150 필드, 3단계 중첩), extreme (~500 필드, 4단계 중첩)
 *
 * 실행 방법:
 * k6 run -e COMPLEXITY=ultra scripts/phase7/http-json-test.js
 * k6 run -e COMPLEXITY=extreme scripts/phase7/http-json-test.js
 *
 * =============================================================================
 */

import http from 'k6/http';
import { check } from 'k6';

// 환경 변수
const COMPLEXITY = __ENV.COMPLEXITY || 'ultra';
const VUS = parseInt(__ENV.VUS) || 10;
const DURATION = __ENV.DURATION || '30s';
const API_SERVER = __ENV.API_SERVER || 'http://192.168.55.114:8080';

// 테스트 옵션
export const options = {
    scenarios: {
        http_json_test: {
            executor: 'constant-vus',
            vus: VUS,
            duration: DURATION,
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<3000'],
        http_req_failed: ['rate<0.01'],
    },
};

// 테스트 시작
export function setup() {
    console.log('='.repeat(60));
    console.log(`Phase 7: HTTP/JSON Extreme Data Test - Complexity: ${COMPLEXITY}`);
    console.log(`VUs: ${VUS}, Duration: ${DURATION}`);
    console.log('='.repeat(60));

    // 헬스 체크
    const healthRes = http.get(`${API_SERVER}/api/health`);
    if (healthRes.status !== 200) {
        throw new Error(`Server not ready: ${healthRes.status}`);
    }
    console.log('✅ Server is ready');

    // 벤치마크 시작
    const startRes = http.post(
        `${API_SERVER}/benchmark/start?protocol=HTTP/JSON&testName=phase7-${COMPLEXITY}`
    );
    console.log(`Benchmark started: ${startRes.body}`);

    return { startTime: Date.now(), complexity: COMPLEXITY };
}

// 메인 테스트 로직
export default function (data) {
    const res = http.get(`${API_SERVER}/api/complex/http/json?complexity=${data.complexity}`);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has serializedSize': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.serializedSize > 0;
            } catch {
                return false;
            }
        },
    });
}

// 테스트 종료
export function teardown(data) {
    const endRes = http.post(`${API_SERVER}/benchmark/end`);

    console.log('\n' + '='.repeat(60));
    console.log('BENCHMARK RESULT');
    console.log('='.repeat(60));

    try {
        const result = JSON.parse(endRes.body);
        console.log(`Protocol: ${result.protocol}`);
        console.log(`Test Name: ${result.testName}`);
        console.log(`Duration: ${result.durationMs}ms`);
        console.log(`Total Requests: ${result.totalRequests}`);
        console.log(`Throughput: ${result.throughputRps.toFixed(2)} req/s`);
        console.log('\nLatency:');
        console.log(`  Average: ${result.latency.avgMs.toFixed(2)}ms`);
        console.log(`  P50: ${result.latency.p50Ms.toFixed(2)}ms`);
        console.log(`  P95: ${result.latency.p95Ms.toFixed(2)}ms`);
        console.log(`  P99: ${result.latency.p99Ms.toFixed(2)}ms`);
        console.log('\nServer Metrics:');
        console.log(`  Peak Heap: ${result.serverMetrics.peakHeapMb.toFixed(2)}MB`);
        console.log(`  GC Count: ${result.serverMetrics.gcCount}`);
        console.log('\nCPU Metrics:');
        console.log(`  Avg System CPU: ${result.serverMetrics.avgCpuUsagePercent.toFixed(2)}%`);
        console.log(`  Peak System CPU: ${result.serverMetrics.peakCpuUsagePercent.toFixed(2)}%`);
        console.log(`  Avg Process CPU: ${result.serverMetrics.avgProcessCpuPercent.toFixed(2)}%`);
        console.log(`  Peak Process CPU: ${result.serverMetrics.peakProcessCpuPercent.toFixed(2)}%`);

        // JSON 형식으로도 출력 (파싱용)
        console.log('\n=== JSON RESULT ===');
        console.log(JSON.stringify(result));
    } catch (e) {
        console.log('Raw result:', endRes.body);
    }

    console.log('='.repeat(60));
}