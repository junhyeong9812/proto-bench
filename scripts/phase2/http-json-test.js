/**
 * =============================================================================
 * Phase 2: HTTP/JSON 테스트 스크립트
 * =============================================================================
 *
 * 목적: 작은 페이로드(1KB, 10KB)에서 HTTP/JSON 성능 측정
 *
 * HTTP/JSON 특성:
 * - Base64 인코딩으로 33% 크기 증가
 * - JSON 파싱 오버헤드
 * - 텍스트 헤더 오버헤드 (~400 bytes)
 *
 * 실행 방법:
 * k6 run -e SIZE=1kb scripts/phase2/http-json-test.js
 * k6 run -e SIZE=10kb scripts/phase2/http-json-test.js
 *
 * =============================================================================
 */

// HTTP 요청 모듈 가져오기
import http from 'k6/http';

// 유틸리티 함수들
// check: 응답 검증
// sleep: 대기 (여기서는 사용하지 않음 - 최대 처리량 측정)
import { check } from 'k6';

// =============================================================================
// 테스트 설정
// =============================================================================

// 환경 변수에서 설정값 읽기
// __ENV: k6의 환경 변수 객체
// SIZE: 테스트할 페이로드 크기 (1kb, 10kb 등)
// VUS: Virtual Users 수
// DURATION: 테스트 시간
const SIZE = __ENV.SIZE || '1kb';
const VUS = parseInt(__ENV.VUS) || 10;
const DURATION = __ENV.DURATION || '30s';

// API 서버 주소
const API_SERVER = __ENV.API_SERVER || 'http://192.168.55.114:8080';

/**
 * k6 테스트 옵션
 *
 * scenarios: 테스트 시나리오 정의
 * thresholds: 성공 기준 정의
 */
export const options = {
    // 시나리오 정의
    scenarios: {
        http_json_test: {
            // executor: 실행 방식
            // 'constant-vus': 일정한 VU 수 유지
            executor: 'constant-vus',

            // VU 수
            vus: VUS,

            // 테스트 시간
            duration: DURATION,
        },
    },

    // 성공 기준
    thresholds: {
        // 95%의 요청이 1초 이내 응답
        http_req_duration: ['p(95)<1000'],

        // 실패율 1% 미만
        http_req_failed: ['rate<0.01'],
    },
};

// =============================================================================
// 테스트 라이프사이클
// =============================================================================

/**
 * setup: 테스트 시작 전 1회 실행
 *
 * 역할:
 * 1. 서버 상태 확인
 * 2. 벤치마크 시작 신호 전송
 * 3. 테스트 정보 로깅
 */
export function setup() {
    console.log('='.repeat(60));
    console.log(`Phase 2: HTTP/JSON Test - Size: ${SIZE}`);
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
        `${API_SERVER}/benchmark/start?protocol=HTTP/JSON&testName=phase2-${SIZE}`
    );
    console.log(`Benchmark started: ${startRes.body}`);

    // setup 반환값은 default 함수와 teardown에 전달됨
    return {
        startTime: Date.now(),
        size: SIZE,
    };
}

/**
 * default: 메인 테스트 로직 (각 VU가 반복 실행)
 *
 * @param data - setup()의 반환값
 */
export default function (data) {
    // HTTP GET 요청
    // 쿼리 파라미터로 페이로드 크기 지정
    const res = http.get(`${API_SERVER}/api/data/http/json?size=${data.size}`);

    // 응답 검증
    check(res, {
        // HTTP 상태 코드 확인
        'status is 200': (r) => r.status === 200,

        // 응답 바디 확인
        'has payload': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.payloadSize > 0;
            } catch {
                return false;
            }
        },

        // 프로토콜 확인
        'protocol is HTTP/JSON': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.protocol === 'HTTP/JSON';
            } catch {
                return false;
            }
        },
    });

    // 참고: sleep()을 사용하지 않음
    // 이유: 최대 처리량을 측정하기 위해 쉬지 않고 요청
}

/**
 * teardown: 테스트 종료 후 1회 실행
 *
 * 역할:
 * 1. 벤치마크 종료 신호 전송
 * 2. 결과 출력
 *
 * @param data - setup()의 반환값
 */
export function teardown(data) {
    // 벤치마크 종료 및 결과 수신
    const endRes = http.post(`${API_SERVER}/benchmark/end`);

    console.log('\n' + '='.repeat(60));
    console.log('BENCHMARK RESULT');
    console.log('='.repeat(60));

    // 결과 파싱 및 출력
    try {
        const result = JSON.parse(endRes.body);
        console.log(`Protocol: ${result.protocol}`);
        console.log(`Test Name: ${result.testName}`);
        console.log(`Duration: ${result.durationMs}ms`);
        console.log(`Total Requests: ${result.totalRequests}`);
        console.log(`Success Requests: ${result.successRequests}`);
        console.log(`Failed Requests: ${result.failedRequests}`);
        console.log(`Throughput: ${result.throughputRps.toFixed(2)} req/s`);
        console.log('\nLatency:');
        console.log(`  Average: ${result.latency.avgMs.toFixed(2)}ms`);
        console.log(`  P50: ${result.latency.p50Ms.toFixed(2)}ms`);
        console.log(`  P95: ${result.latency.p95Ms.toFixed(2)}ms`);
        console.log(`  P99: ${result.latency.p99Ms.toFixed(2)}ms`);
        console.log('\nServer Metrics:');
        console.log(`  Peak Heap: ${result.serverMetrics.peakHeapMb.toFixed(2)}MB`);
        console.log(`  GC Count: ${result.serverMetrics.gcCount}`);
        console.log(`  GC Time: ${result.serverMetrics.gcTimeMs}ms`);
    } catch (e) {
        console.log('Raw result:', endRes.body);
    }

    console.log('='.repeat(60));

    // 테스트 총 시간
    const totalTime = (Date.now() - data.startTime) / 1000;
    console.log(`Total test time: ${totalTime.toFixed(2)}s`);
}