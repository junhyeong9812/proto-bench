/**
 * =============================================================================
 * Phase 4: gRPC/Unary 역전 포인트 탐색 테스트
 * =============================================================================
 *
 * 목적: 10KB ~ 1MB 사이에서 gRPC와 HTTP 성능 역전 포인트 탐색
 *
 * 테스트 크기: 10KB → 50KB → 100KB → 200KB → 500KB
 *
 * 실행 방법:
 * k6 run -e SIZE=50kb scripts/phase4/grpc-test.js
 * k6 run -e SIZE=100kb scripts/phase4/grpc-test.js
 * k6 run -e SIZE=200kb scripts/phase4/grpc-test.js
 * k6 run -e SIZE=500kb scripts/phase4/grpc-test.js
 *
 * =============================================================================
 */

import http from 'k6/http';
import { check } from 'k6';

// 환경 변수
const SIZE = __ENV.SIZE || '100kb';
const VUS = parseInt(__ENV.VUS) || 10;
const DURATION = __ENV.DURATION || '30s';
const API_SERVER = __ENV.API_SERVER || 'http://192.168.55.114:8080';

// 테스트 옵션
export const options = {
    scenarios: {
        grpc_test: {
            executor: 'constant-vus',
            vus: VUS,
            duration: DURATION,
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.01'],
    },
};

// 테스트 시작
export function setup() {
    console.log('='.repeat(60));
    console.log(`Phase 4: gRPC/Unary Crossover Test - Size: ${SIZE}`);
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
        `${API_SERVER}/benchmark/start?protocol=gRPC/Unary&testName=phase4-${SIZE}`
    );
    console.log(`Benchmark started: ${startRes.body}`);

    return { startTime: Date.now(), size: SIZE };
}

// 메인 테스트 로직
export default function (data) {
    const res = http.get(`${API_SERVER}/api/data/grpc?size=${data.size}`);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has payload': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.payloadSize > 0;
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
    } catch (e) {
        console.log('Raw result:', endRes.body);
    }

    console.log('='.repeat(60));
}