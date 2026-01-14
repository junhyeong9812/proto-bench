import http from 'k6/http';
import { check, sleep } from 'k6';

// 테스트 설정
export const options = {
    scenarios: {
        http_json_test: {
            executor: 'constant-vus',
            vus: 10,              // 동시 사용자 수
            duration: '30s',      // 테스트 시간
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],  // 95%가 500ms 이하
    },
};

const API_SERVER = 'http://192.168.55.114:8080';

// 테스트 시작 전 벤치마크 시작
export function setup() {
    const startRes = http.post(`${API_SERVER}/benchmark/start?protocol=HTTP/JSON&testName=k6-http-json`);
    console.log('Benchmark started:', startRes.body);
    return { startTime: Date.now() };
}

// 메인 테스트 로직
export default function () {
    const res = http.get(`${API_SERVER}/api/data/http/json`);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has payload': (r) => {
            const body = JSON.parse(r.body);
            return body.payloadSize > 0;
        },
    });
}

// 테스트 종료 후 결과 수집
export function teardown(data) {
    sleep(1);  // 마지막 요청 완료 대기

    const endRes = http.post(`${API_SERVER}/benchmark/end`);
    console.log('=== Benchmark Result ===');
    console.log(endRes.body);
}