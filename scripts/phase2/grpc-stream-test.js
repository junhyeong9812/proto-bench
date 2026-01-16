/**
 * =============================================================================
 * Phase 2: gRPC/Stream 테스트 스크립트
 * =============================================================================
 *
 * 목적: 작은 페이로드(1KB, 10KB)에서 gRPC Streaming 성능 측정
 *
 * gRPC/Stream 특성:
 * - 1KB, 10KB는 64KB 청크보다 작음
 * - 따라서 스트리밍이 아닌 단일 청크로 전송됨
 * - Unary와 거의 동일한 성능 예상 (약간의 스트리밍 오버헤드)
 *
 * 실행 방법:
 * k6 run -e SIZE=1kb scripts/phase2/grpc-stream-test.js
 * k6 run -e SIZE=10kb scripts/phase2/grpc-stream-test.js
 *
 * =============================================================================
 */

import http from 'k6/http';
import { check } from 'k6';

// 환경 변수
const SIZE = __ENV.SIZE || '1kb';
const VUS = parseInt(__ENV.VUS) || 10;
const DURATION = __ENV.DURATION || '30s';
const API_SERVER = __ENV.API_SERVER || 'http://192.168.55.114:8080';

// 테스트 옵션
export const options = {
    scenarios: {
        grpc_stream_test: {
            executor: 'constant-vus',
            vus: VUS,
            duration: DURATION,
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        http_req_failed: ['rate<0.01'],
    },
};

// 테스트 시작
export function setup() {
    console.log('='.repeat(60));
    console.log(`Phase 2: gRPC/Stream Test - Size: ${SIZE}`);
    console.log(`VUs: ${VUS}, Duration: ${DURATION}`);
    console.log('='.repeat(60));

    const healthRes = http.get(`${API_SERVER}/api/health`);
    if (healthRes.status !== 200) {
        throw new Error(`Server not ready: ${healthRes.status}`);
    }
    console.log('✅ Server is ready');

    const startRes = http.post(
        `${API_SERVER}/benchmark/start?protocol=gRPC/Stream&testName=phase2-${SIZE}`
    );
    console.log(`Benchmark started: ${startRes.body}`);

    return { startTime: Date.now(), size: SIZE };
}

// 메인 테스트 로직
export default function (data) {
    const res = http.get(`${API_SERVER}/api/data/grpc/stream?size=${data.size}`);

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
        'protocol is gRPC/Stream': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.protocol === 'gRPC/Stream';
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