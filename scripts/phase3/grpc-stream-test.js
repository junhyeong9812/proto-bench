import http from 'k6/http';
import { check, sleep } from 'k6';

const VUS = parseInt(__ENV.VUS) || 50;
const SIZE = __ENV.SIZE || '10kb';
const API_SERVER = __ENV.API_SERVER || 'http://192.168.55.114:8080';

export const options = {
    scenarios: {
        grpc_stream_test: {
            executor: 'constant-vus',
            vus: VUS,
            duration: '30s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.01'],
    },
};

export function setup() {
    const startRes = http.post(`${API_SERVER}/benchmark/start?protocol=gRPC/Stream&testName=phase3-${VUS}vu-${SIZE}`);
    console.log('============================================================');
    console.log(`Phase 3: gRPC/Stream Test - VUs: ${VUS}, Size: ${SIZE}`);
    console.log('============================================================');
    console.log('✅ Server is ready');
    console.log(`Benchmark started: ${startRes.body}`);
    return { startTime: Date.now(), vus: VUS, size: SIZE };
}

export default function (data) {
    const res = http.get(`${API_SERVER}/api/data/grpc/stream?size=${data.size}`);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has payload': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.payloadSize > 0;
            } catch { return false; }
        },
    });
}

export function teardown(data) {
    sleep(1);
    const endRes = http.post(`${API_SERVER}/benchmark/end`);
    console.log('\n============================================================');
    console.log('BENCHMARK RESULT');
    console.log('============================================================');
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
    console.log('============================================================');
}