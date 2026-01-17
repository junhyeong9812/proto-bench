# k6 완벽 가이드

> k6를 처음 접하는 개발자를 위한 상세 학습 문서

## 목차

1. [k6란 무엇인가?](#1-k6란-무엇인가)
2. [설치 및 시작](#2-설치-및-시작)
3. [기본 문법](#3-기본-문법)
4. [주요 개념](#4-주요-개념)
5. [HTTP 요청](#5-http-요청)
6. [검증 (Checks)](#6-검증-checks)
7. [임계값 (Thresholds)](#7-임계값-thresholds)
8. [시나리오](#8-시나리오)
9. [결과 분석](#9-결과-분석)
10. [실전 팁](#10-실전-팁)

---

## 1. k6란 무엇인가?

### 정의

k6는 **Grafana Labs에서 개발한 오픈소스 부하 테스트 도구**입니다.

```
k6의 특징:
- JavaScript로 테스트 스크립트 작성
- Go로 구현되어 높은 성능
- CLI 기반으로 CI/CD 통합 용이
- 다양한 프로토콜 지원 (HTTP, WebSocket, gRPC)
```

### 왜 k6를 사용하나?

| 도구 | 장점 | 단점 |
|------|------|------|
| **k6** | 개발자 친화적, 가벼움, CI/CD 통합 | GUI 없음 |
| JMeter | 강력한 GUI, 플러그인 풍부 | 무겁고 복잡 |
| Gatling | Scala 기반, 상세한 리포트 | 학습 곡선 높음 |
| Locust | Python 기반, 분산 테스트 용이 | Python 필요 |

### k6의 아키텍처

```
┌────────────────────────────────────────────┐
│              k6 엔진 (Go)                   │
│  ┌──────────────────────────────────────┐  │
│  │     JavaScript 런타임 (goja)         │  │
│  │     ┌────────────────────────┐       │  │
│  │     │  테스트 스크립트 (.js)  │       │  │
│  │     └────────────────────────┘       │  │
│  └──────────────────────────────────────┘  │
│                    │                        │
│  ┌────────────┬────┴────┬────────────┐     │
│  │ HTTP 모듈  │ WS 모듈  │ gRPC 모듈  │     │
│  └────────────┴─────────┴────────────┘     │
└────────────────────────────────────────────┘
                    │
                    ▼
            ┌───────────────┐
            │  대상 서버     │
            └───────────────┘
```

---

## 2. 설치 및 시작

### 2.1 설치 방법

```bash
# macOS
brew install k6

# Ubuntu/Debian
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# Windows
choco install k6

# Docker
docker run --rm -i grafana/k6 run - <script.js
```

### 2.2 첫 번째 테스트

```javascript
// hello.js
import http from 'k6/http';

export default function () {
    http.get('https://test.k6.io');
}
```

```bash
# 실행
k6 run hello.js
```

### 2.3 실행 옵션

```bash
# VU(Virtual Users) 10명으로 30초간 테스트
k6 run --vus 10 --duration 30s script.js

# 설정 파일 사용
k6 run script.js

# JSON 결과 출력
k6 run --out json=results.json script.js

# 환경 변수 전달
k6 run -e API_URL=http://localhost:8080 script.js
```

---

## 3. 기본 문법

### 3.1 스크립트 구조

```javascript
// ============================================================================
// 1. Import (모듈 가져오기)
// ============================================================================
import http from 'k6/http';           // HTTP 요청
import { check, sleep } from 'k6';    // 유틸리티 함수

// ============================================================================
// 2. Options (테스트 설정)
// ============================================================================
export const options = {
    vus: 10,              // Virtual Users (동시 사용자 수)
    duration: '30s',      // 테스트 시간
};

// ============================================================================
// 3. Setup (테스트 전 1회 실행)
// ============================================================================
export function setup() {
    console.log('테스트 시작!');
    // 테스트 데이터 준비, 인증 토큰 획득 등
    return { token: 'abc123' };  // 반환값은 default 함수와 teardown에 전달
}

// ============================================================================
// 4. Default (메인 테스트 로직 - VU마다 반복 실행)
// ============================================================================
export default function (data) {
    // data = setup()의 반환값
    const res = http.get('https://test.k6.io');
    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(1);  // 1초 대기
}

// ============================================================================
// 5. Teardown (테스트 후 1회 실행)
// ============================================================================
export function teardown(data) {
    console.log('테스트 완료!');
    // 정리 작업, 결과 저장 등
}
```

### 3.2 실행 순서

```
┌──────────────────────────────────────────────────────────┐
│                      테스트 실행                          │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  1. init (스크립트 로드 - 모든 VU에서 1회)                │
│     ↓                                                    │
│  2. setup() (1회 실행)                                   │
│     ↓                                                    │
│  ┌────────────────────────────────────────────┐         │
│  │  3. default() (VU마다 반복)                 │         │
│  │     VU 1: ──────────────────────────→      │         │
│  │     VU 2: ──────────────────────────→      │         │
│  │     VU 3: ──────────────────────────→      │         │
│  │     ...                                    │         │
│  └────────────────────────────────────────────┘         │
│     ↓                                                    │
│  4. teardown() (1회 실행)                                │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 4. 주요 개념

### 4.1 VU (Virtual User)

**Virtual User**는 동시에 테스트를 실행하는 가상 사용자입니다.

```javascript
export const options = {
    vus: 100,  // 100명의 가상 사용자가 동시에 요청
};
```

각 VU는:
- 독립적인 실행 환경을 가짐
- 자신만의 쿠키, 연결 등을 유지
- default() 함수를 반복 실행

### 4.2 Iteration

**Iteration**은 default() 함수가 한 번 실행되는 것입니다.

```javascript
export default function () {
    http.get('https://test.k6.io');
    sleep(1);
}

// VU 10명, 30초 동안:
// - 각 VU가 약 30번씩 반복 (sleep 1초)
// - 총 약 300 iterations
```

### 4.3 주요 메트릭

k6가 자동으로 수집하는 메트릭들:

| 메트릭 | 설명 |
|--------|------|
| `http_reqs` | 총 HTTP 요청 수 |
| `http_req_duration` | 요청 소요 시간 (평균, p90, p95) |
| `http_req_failed` | 실패한 요청 비율 |
| `http_req_waiting` | 서버 응답 대기 시간 (TTFB) |
| `http_req_connecting` | TCP 연결 시간 |
| `iteration_duration` | 반복 소요 시간 |
| `vus` | 활성 VU 수 |
| `data_received` | 수신 데이터 양 |
| `data_sent` | 송신 데이터 양 |

---

## 5. HTTP 요청

### 5.1 기본 요청

```javascript
import http from 'k6/http';

export default function () {
    // GET 요청
    const getRes = http.get('https://api.example.com/users');
    
    // POST 요청 (JSON)
    const postRes = http.post(
        'https://api.example.com/users',
        JSON.stringify({ name: 'John', email: 'john@example.com' }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    
    // PUT 요청
    const putRes = http.put(
        'https://api.example.com/users/1',
        JSON.stringify({ name: 'Jane' }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    
    // DELETE 요청
    const deleteRes = http.del('https://api.example.com/users/1');
}
```

### 5.2 요청 옵션

```javascript
const params = {
    // 헤더
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer token123',
    },
    
    // 태그 (메트릭 그룹화)
    tags: { name: 'UserAPI' },
    
    // 타임아웃
    timeout: '30s',
    
    // 리다이렉트 따라가기
    redirects: 5,
    
    // 압축
    compression: 'gzip',
};

const res = http.get('https://api.example.com', params);
```

### 5.3 응답 처리

```javascript
const res = http.get('https://api.example.com/users');

// 상태 코드
console.log(res.status);        // 200

// 응답 바디
console.log(res.body);          // 문자열

// JSON 파싱
const data = res.json();
console.log(data.name);         // JSON 객체로 접근

// 헤더
console.log(res.headers['Content-Type']);

// 타이밍 정보
console.log(res.timings.duration);      // 전체 시간
console.log(res.timings.waiting);       // TTFB
console.log(res.timings.connecting);    // 연결 시간
```

### 5.4 배치 요청

여러 요청을 병렬로 실행:

```javascript
import http from 'k6/http';

export default function () {
    const responses = http.batch([
        ['GET', 'https://api.example.com/users'],
        ['GET', 'https://api.example.com/posts'],
        ['GET', 'https://api.example.com/comments'],
    ]);
    
    // 또는 객체 형태
    const res = http.batch({
        users: ['GET', 'https://api.example.com/users'],
        posts: ['GET', 'https://api.example.com/posts'],
    });
    
    console.log(res.users.status);
    console.log(res.posts.status);
}
```

---

## 6. 검증 (Checks)

### 6.1 기본 사용법

```javascript
import { check } from 'k6';
import http from 'k6/http';

export default function () {
    const res = http.get('https://api.example.com/users');
    
    // 단일 검증
    check(res, {
        'status is 200': (r) => r.status === 200,
    });
    
    // 다중 검증
    check(res, {
        'status is 200': (r) => r.status === 200,
        'body is not empty': (r) => r.body.length > 0,
        'response time < 500ms': (r) => r.timings.duration < 500,
        'content-type is JSON': (r) => r.headers['Content-Type'].includes('application/json'),
    });
}
```

### 6.2 JSON 검증

```javascript
export default function () {
    const res = http.get('https://api.example.com/users/1');
    const user = res.json();
    
    check(res, {
        'status is 200': (r) => r.status === 200,
        'user has id': () => user.id !== undefined,
        'user name is John': () => user.name === 'John',
        'user has email': () => user.email !== undefined,
    });
}
```

### 6.3 그룹화

```javascript
import { group, check } from 'k6';
import http from 'k6/http';

export default function () {
    group('User API', function () {
        const res = http.get('https://api.example.com/users');
        check(res, { 'users status 200': (r) => r.status === 200 });
    });
    
    group('Post API', function () {
        const res = http.get('https://api.example.com/posts');
        check(res, { 'posts status 200': (r) => r.status === 200 });
    });
}
```

---

## 7. 임계값 (Thresholds)

### 7.1 기본 사용법

임계값은 **테스트 통과/실패 기준**을 정의합니다.

```javascript
export const options = {
    thresholds: {
        // 95%의 요청이 500ms 이하
        http_req_duration: ['p(95)<500'],
        
        // 실패율 1% 이하
        http_req_failed: ['rate<0.01'],
        
        // 모든 체크 통과
        checks: ['rate>0.99'],
    },
};
```

### 7.2 다양한 조건

```javascript
export const options = {
    thresholds: {
        // 여러 조건 조합
        http_req_duration: [
            'p(95)<500',    // 95%가 500ms 이하
            'p(99)<1000',   // 99%가 1000ms 이하
            'avg<300',      // 평균 300ms 이하
            'max<2000',     // 최대 2000ms 이하
        ],
        
        // 특정 URL만 필터링
        'http_req_duration{url:https://api.example.com/users}': ['p(95)<200'],
        
        // 커스텀 메트릭
        'my_custom_metric': ['count>100'],
    },
};
```

### 7.3 중단 조건

```javascript
export const options = {
    thresholds: {
        // abortOnFail: true로 설정하면 조건 실패 시 테스트 중단
        http_req_failed: [
            {
                threshold: 'rate<0.1',
                abortOnFail: true,
                delayAbortEval: '10s',  // 10초 후 평가 시작
            },
        ],
    },
};
```

---

## 8. 시나리오

### 8.1 시나리오란?

시나리오는 **더 복잡한 부하 패턴**을 정의할 수 있게 해줍니다.

```javascript
export const options = {
    scenarios: {
        // 시나리오 1: 일정한 부하
        constant_load: {
            executor: 'constant-vus',
            vus: 10,
            duration: '1m',
        },
        
        // 시나리오 2: 점진적 증가
        ramping_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },
                { duration: '1m', target: 20 },
                { duration: '30s', target: 0 },
            ],
        },
    },
};
```

### 8.2 Executor 종류

| Executor | 설명 | 사용 사례 |
|----------|------|-----------|
| `constant-vus` | 일정한 VU 수 유지 | 기본 부하 테스트 |
| `ramping-vus` | VU 수를 단계적으로 변경 | 스트레스 테스트 |
| `constant-arrival-rate` | 일정한 요청률 유지 | 처리량 테스트 |
| `ramping-arrival-rate` | 요청률 단계적 변경 | 한계 테스트 |
| `per-vu-iterations` | VU당 정해진 횟수 실행 | 기능 테스트 |
| `shared-iterations` | 전체 VU가 총 N회 실행 | 데이터 처리 |

### 8.3 예제: 스트레스 테스트

```javascript
export const options = {
    scenarios: {
        stress_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                // 워밍업: 0 → 100 (2분)
                { duration: '2m', target: 100 },
                
                // 유지: 100명으로 5분
                { duration: '5m', target: 100 },
                
                // 스트레스: 100 → 200 (2분)
                { duration: '2m', target: 200 },
                
                // 유지: 200명으로 5분
                { duration: '5m', target: 200 },
                
                // 쿨다운: 200 → 0 (2분)
                { duration: '2m', target: 0 },
            ],
        },
    },
};
```

### 8.4 예제: 요청률 기반

```javascript
export const options = {
    scenarios: {
        // 초당 100개 요청 유지
        constant_rps: {
            executor: 'constant-arrival-rate',
            rate: 100,              // 초당 요청 수
            timeUnit: '1s',         // rate의 단위
            duration: '1m',
            preAllocatedVUs: 50,    // 미리 할당할 VU
            maxVUs: 100,            // 최대 VU
        },
    },
};
```

---

## 9. 결과 분석

### 9.1 터미널 출력 이해하기

```
         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: script.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration
              * default: 10 looping VUs for 30s

     ✓ status is 200
     ✓ response time < 500ms

     checks.........................: 100.00% ✓ 600      ✗ 0
     data_received..................: 1.2 MB  40 kB/s
     data_sent......................: 48 kB   1.6 kB/s
     http_req_blocked...............: avg=1.2ms   min=1µs     med=2µs     max=120ms
     http_req_connecting............: avg=500µs   min=0s      med=0s      max=50ms
   ✓ http_req_duration..............: avg=150ms   min=100ms   med=140ms   max=400ms
     http_req_failed................: 0.00%   ✓ 0        ✗ 300
     http_reqs......................: 300     10/s
     iteration_duration.............: avg=151ms   min=101ms   med=141ms   max=401ms
     iterations.....................: 300     10/s
     vus............................: 10      min=10     max=10
```

### 9.2 주요 해석 포인트

1. **http_req_duration**: 가장 중요한 메트릭
    - `avg`: 평균 응답 시간
    - `p(95)`: 95%가 이 시간 이내에 응답

2. **http_req_failed**: 실패율
    - 0%가 이상적
    - 1% 이상이면 문제 확인 필요

3. **http_reqs**: 처리량
    - 초당 요청 수 (RPS)

4. **checks**: 검증 통과율
    - 100%가 이상적

### 9.3 결과 내보내기

```bash
# JSON 형식
k6 run --out json=results.json script.js

# CSV 형식
k6 run --out csv=results.csv script.js

# InfluxDB로 전송 (대시보드용)
k6 run --out influxdb=http://localhost:8086/k6 script.js
```

---

## 10. 실전 팁

### 10.1 환경 변수 사용

```javascript
// script.js
const BASE_URL = __ENV.API_URL || 'http://localhost:8080';

export default function () {
    http.get(`${BASE_URL}/api/data`);
}
```

```bash
k6 run -e API_URL=http://production.example.com script.js
```

### 10.2 데이터 파일 사용

```javascript
// data.json: [{"id": 1, "name": "John"}, {"id": 2, "name": "Jane"}]

import { SharedArray } from 'k6/data';

const users = new SharedArray('users', function () {
    return JSON.parse(open('./data.json'));
});

export default function () {
    // 랜덤 사용자 선택
    const user = users[Math.floor(Math.random() * users.length)];
    http.get(`https://api.example.com/users/${user.id}`);
}
```

### 10.3 인증 처리

```javascript
import http from 'k6/http';

// 테스트 시작 시 토큰 획득
export function setup() {
    const loginRes = http.post('https://api.example.com/login', {
        username: 'test',
        password: 'password',
    });
    return { token: loginRes.json('token') };
}

export default function (data) {
    const params = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
        },
    };
    http.get('https://api.example.com/protected', params);
}
```

### 10.4 에러 처리

```javascript
import http from 'k6/http';
import { check, fail } from 'k6';

export default function () {
    const res = http.get('https://api.example.com/users');
    
    // 심각한 에러면 테스트 중단
    if (res.status === 500) {
        fail('서버 에러 발생!');
    }
    
    // 경고 레벨 체크
    const checkResult = check(res, {
        'status is 200': (r) => r.status === 200,
    });
    
    if (!checkResult) {
        console.warn(`요청 실패: ${res.status}`);
    }
}
```

### 10.5 커스텀 메트릭

```javascript
import http from 'k6/http';
import { Counter, Gauge, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭 정의
const myCounter = new Counter('my_counter');
const myGauge = new Gauge('my_gauge');
const myRate = new Rate('my_rate');
const myTrend = new Trend('my_trend');

export default function () {
    const res = http.get('https://api.example.com/users');
    
    // 메트릭 기록
    myCounter.add(1);                          // 카운터 증가
    myGauge.add(res.timings.duration);         // 현재 값 설정
    myRate.add(res.status === 200);            // 성공률
    myTrend.add(res.timings.duration);         // 추세 데이터
}

export const options = {
    thresholds: {
        'my_rate': ['rate>0.95'],               // 95% 이상 성공
        'my_trend': ['p(95)<500'],              // 95%가 500ms 이하
    },
};
```

---

## proto-bench 프로젝트에서의 k6 사용

### 테스트 스크립트 예시

```javascript
// scripts/http-json-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        http_json_test: {
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

// 테스트 시작 전: 벤치마크 시작
export function setup() {
    http.post(`${API_SERVER}/benchmark/start?protocol=HTTP/JSON`);
    return {};
}

// 메인 테스트 로직
export default function () {
    const res = http.get(`${API_SERVER}/api/data/http/json?size=1mb`);
    
    check(res, {
        'status is 200': (r) => r.status === 200,
        'has payload': (r) => {
            const body = JSON.parse(r.body);
            return body.payloadSize > 0;
        },
    });
}

// 테스트 종료 후: 벤치마크 종료 및 결과 출력
export function teardown() {
    const result = http.post(`${API_SERVER}/benchmark/end`);
    console.log('Benchmark Result:', result.body);
}
```

### 실행 방법

```bash
# 단일 테스트
k6 run scripts/http-json-test.js

# 전체 테스트 (스크립트로)
./scripts/run-all.sh

# 환경 변수와 함께
k6 run -e SIZE=10kb scripts/http-json-test.js
```

---

## 추가 학습 자료

- [k6 공식 문서](https://k6.io/docs/)
- [k6 예제 저장소](https://github.com/grafana/k6/tree/master/examples)
- [k6 Cloud](https://k6.io/cloud/) - 클라우드 기반 부하 테스트
- [Grafana k6 블로그](https://grafana.com/blog/tags/k6/)