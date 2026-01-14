import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        http_binary_test: {
            executor: 'constant-vus',
            vus: 10,
            duration: '30s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
    },
};

const API_SERVER = 'http://localhost:8080';

export function setup() {
    const startRes = http.post(`${API_SERVER}/benchmark/start?protocol=HTTP/Binary&testName=k6-http-binary`);
    console.log('Benchmark started:', startRes.body);
    return { startTime: Date.now() };
}

export default function () {
    const res = http.get(`${API_SERVER}/api/data/http/binary`);

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
    console.log('=== Benchmark Result ===');
    console.log(endRes.body);
}