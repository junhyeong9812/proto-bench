import http from 'k6/http';
import { check, sleep } from 'k6';

const SIZE = __ENV.SIZE || '1kb';
const API_SERVER = __ENV.API_SERVER || 'http://192.168.55.114:8080';

export const options = {
    scenarios: {
        grpc_test: {
            executor: 'constant-vus',
            vus: 10,
            duration: '30s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
    },
};

export function setup() {
    const startRes = http.post(`${API_SERVER}/benchmark/start?protocol=gRPC/Unary&testName=phase2-${SIZE}`);
    console.log(`[Phase 2] gRPC/Unary ${SIZE} 테스트 시작`);
    return { startTime: Date.now(), size: SIZE };
}

export default function (data) {
    const res = http.get(`${API_SERVER}/api/data/grpc?size=${data.size}`);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has payload': (r) => {
            const body = JSON.parse(r.body);
            return body.payloadSize > 0;
        },
    });
}

export function teardown(data) {
    sleep(1);
    const endRes = http.post(`${API_SERVER}/benchmark/end`);
    console.log(`=== gRPC/Unary ${data.size} Result ===`);
    console.log(endRes.body);
}