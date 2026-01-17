# Phase 3: 고동시성 멀티플렉싱 테스트 결과

## 개요

| 항목 | 값 |
|------|-----|
| 페이로드 크기 | 10KB |
| 동시 사용자 (VU) | 50 → 100 → 200 → 500 |
| 테스트 시간 | 각 30초 |

## 가설

> "100명 이상 동시 접속 시 HTTP/1.1은 커넥션 풀 한계로 느려지고,  
> gRPC는 단일 연결에서 모든 요청을 처리하여 유리할 것이다"

---

## 결과: 50 VU (기준선)

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 순위 |
|----------|-------------------|-------------|-------------|------|
| **HTTP/Binary** | **12,150.71** | 2.31ms | 4.03ms | 🥇 |
| gRPC/Unary | 10,774.85 | 3.00ms | 5.38ms | 🥈 |
| HTTP/JSON | 10,081.00 | 3.02ms | 5.12ms | 🥉 |
| gRPC/Stream | 9,579.97 | 3.30ms | 5.48ms | 4위 |

### 분석 (50 VU)

**HTTP/Binary가 여전히 가장 빠름**
- HTTP/Binary: 12,151 req/s
- gRPC/Unary: 10,775 req/s
- **차이: HTTP가 13% 더 빠름**

50 VU에서는 아직 커넥션 풀 한계에 도달하지 않아 HTTP가 유리함.

---

## 결과: 100 VU (중간 부하)

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 순위 |
|----------|-------------------|-------------|-------------|------|
| **HTTP/Binary** | **13,839.70** | 4.16ms | 7.56ms | 🥇 |
| gRPC/Unary | 13,423.32 | 5.47ms | 9.10ms | 🥈 |
| HTTP/JSON | 11,357.85 | 5.55ms | 9.93ms | 🥉 |
| gRPC/Stream | 11,232.49 | 6.08ms | 9.72ms | 4위 |

### 분석 (100 VU)

**HTTP/Binary와 gRPC/Unary의 격차가 줄어듦**
- HTTP/Binary: 13,840 req/s
- gRPC/Unary: 13,423 req/s
- **차이: HTTP가 3% 더 빠름** (50VU에서 13% → 100VU에서 3%로 감소)

---

## 결과: 200 VU (고부하)

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 순위 |
|----------|-------------------|-------------|-------------|------|
| **gRPC/Unary** | **15,387.59** | 9.95ms | 15.22ms | 🥇 |
| HTTP/Binary | 14,146.86 | 8.36ms | 15.01ms | 🥈 |
| HTTP/JSON | 12,573.28 | 10.36ms | 18.48ms | 🥉 |
| gRPC/Stream | 12,338.88 | 11.32ms | 17.52ms | 4위 |

### 분석 (200 VU)

**🔄 역전 발생! gRPC/Unary가 1위로 올라섬**
- gRPC/Unary: 15,388 req/s
- HTTP/Binary: 14,147 req/s
- **차이: gRPC가 9% 더 빠름**

HTTP/2 멀티플렉싱 효과가 나타나기 시작.

---

## 결과: 500 VU (스트레스)

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 순위 |
|----------|-------------------|-------------|-------------|------|
| **gRPC/Unary** | **16,052.12** | 11.22ms | 15.75ms | 🥇 |
| HTTP/Binary | 14,325.16 | 12.41ms | 21.40ms | 🥈 |
| HTTP/JSON | 12,557.63 | 14.55ms | 25.33ms | 🥉 |
| gRPC/Stream | 11,556.28 | 15.56ms | 22.15ms | 4위 |

### 분석 (500 VU)

**gRPC/Unary가 확실한 우위**
- gRPC/Unary: 16,052 req/s
- HTTP/Binary: 14,325 req/s
- **차이: gRPC가 12% 더 빠름**

---

## VU별 성능 변화 추이

### Throughput (req/s)

| VU | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream |
|----|-----------|-------------|------------|-------------|
| 50 | 10,081 | **12,151** | 10,775 | 9,580 |
| 100 | 11,358 | **13,840** | 13,423 | 11,232 |
| 200 | 12,573 | 14,147 | **15,388** | 12,339 |
| 500 | 12,558 | 14,325 | **16,052** | 11,556 |

### VU 증가에 따른 Throughput 증가율

| 프로토콜 | 50→100 VU | 100→200 VU | 200→500 VU | 50→500 총 증가 |
|----------|-----------|------------|------------|---------------|
| HTTP/JSON | +13% | +11% | 0% | **+25%** |
| HTTP/Binary | +14% | +2% | +1% | **+18%** |
| gRPC/Unary | +25% | +15% | +4% | **+49%** |
| gRPC/Stream | +17% | +10% | -6% | **+21%** |

### Latency P95 (ms)

| VU | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream |
|----|-----------|-------------|------------|-------------|
| 50 | 5.12 | **4.03** | 5.38 | 5.48 |
| 100 | 9.93 | **7.56** | 9.10 | 9.72 |
| 200 | 18.48 | **15.01** | **15.22** | 17.52 |
| 500 | 25.33 | 21.40 | **15.75** | 22.15 |

---

## 핵심 인사이트

### 1. 가설 검증: ✅ 부분 성공

> "100명 이상 동시 접속 시 gRPC가 유리할 것이다"

- 100 VU: HTTP가 여전히 약간 우위 (3%)
- **200 VU: gRPC 역전 (9% 우위)**
- **500 VU: gRPC 확실한 우위 (12% 우위)**

**역전 포인트: 100~200 VU 사이**

### 2. 스케일링 특성 비교

| 프로토콜 | 스케일링 특성 |
|----------|--------------|
| **gRPC/Unary** | VU 증가에 따라 throughput 계속 증가 (+49%) |
| HTTP/Binary | 200 VU부터 포화 시작 (+18%) |
| HTTP/JSON | 200 VU부터 포화 (+25%) |
| gRPC/Stream | 500 VU에서 오히려 감소 (+21%) |

### 3. 멀티플렉싱 효과

| VU | HTTP/Binary 연결 수 (추정) | gRPC 연결 수 |
|----|--------------------------|-------------|
| 50 | ~50개 | ~10개 |
| 100 | ~100개 | ~10개 |
| 200 | ~200개 | ~10개 |
| 500 | ~500개 (커넥션 풀 압박) | ~10개 |

gRPC는 단일 연결에서 수백 개의 스트림을 처리하여 연결 오버헤드 최소화.

### 4. Latency 안정성

| VU | HTTP/Binary p95 | gRPC/Unary p95 | 비고 |
|----|-----------------|----------------|------|
| 50 | 4.03ms | 5.38ms | HTTP 우위 |
| 100 | 7.56ms | 9.10ms | HTTP 우위 |
| 200 | 15.01ms | 15.22ms | **동등** |
| 500 | 21.40ms | **15.75ms** | **gRPC 우위** |

500 VU에서 gRPC의 Latency p95가 36% 더 낮음!

### 5. 서버 메모리 사용량

| VU | HTTP Peak Heap | gRPC Peak Heap |
|----|----------------|----------------|
| 50 | ~800-960 MB | ~800-960 MB |
| 100 | ~980-1180 MB | ~1180-1200 MB |
| 200 | ~1230-1450 MB | ~1450-1470 MB |
| 500 | ~1480-1510 MB | ~1510-1520 MB |

고부하에서도 메모리 사용량은 비슷함.

---

## Phase 1-2-3 종합 비교

| 조건 | 승자 | 차이 |
|------|------|------|
| 1MB 페이로드 (Phase 1) | HTTP/Binary | 2.1배 빠름 |
| 1KB 페이로드 (Phase 2) | gRPC/Unary | 1.6배 빠름 |
| 10KB 페이로드 (Phase 2) | gRPC/Unary | 1.4배 빠름 |
| 50 VU (Phase 3) | HTTP/Binary | 13% 빠름 |
| 100 VU (Phase 3) | HTTP/Binary | 3% 빠름 |
| **200 VU (Phase 3)** | **gRPC/Unary** | **9% 빠름** |
| **500 VU (Phase 3)** | **gRPC/Unary** | **12% 빠름** |

---

## 결론

### 가설 검증 결과

| 가설 | 결과 |
|------|------|
| "100명 이상에서 gRPC 유리" | **△ 부분 성공** - 200명부터 역전 |
| "HTTP는 커넥션 풀 한계로 느려짐" | **✅ 성공** - 200 VU부터 포화 |
| "gRPC 멀티플렉싱 효과" | **✅ 성공** - 스케일링 우수 |

### 언제 무엇을 써야 하나?

| 상황 | 추천 프로토콜 |
|------|--------------|
| 대용량 파일 전송 (1MB+) | HTTP/Binary |
| 소용량 API 통신 (~10KB) | gRPC/Unary |
| **저부하 (< 100 VU)** | **HTTP/Binary** |
| **고부하 (200+ VU)** | **gRPC/Unary** |
| 실시간 스트리밍 | gRPC/Stream |

### 핵심 발견

1. **역전 포인트**: 100~200 VU 사이에서 gRPC가 HTTP를 추월
2. **스케일링**: gRPC/Unary가 VU 증가에 가장 잘 대응 (+49%)
3. **Latency 안정성**: 500 VU에서 gRPC p95가 36% 더 낮음
4. **HTTP 포화**: 200 VU부터 throughput 증가율 급감

---

### 실 결과  로그
```azure
root@jun:/home/jun/distributed-log-pipeline/proto-bench/scripts# ./run-phase3.sh
================================
Phase 3: 고동시성 멀티플렉싱 테스트
================================

[0] 서버 상태 확인...
❌ apiServer가 실행되지 않았습니다.
root@jun:/home/jun/distributed-log-pipeline/proto-bench/scripts# ^C
root@jun:/home/jun/distributed-log-pipeline/proto-bench/scripts# ./run-phase3.sh
================================
Phase 3: 고동시성 멀티플렉싱 테스트
================================

[0] 서버 상태 확인...
✅ 서버 정상

[Warmup] JIT 워밍업...
✅ 워밍업 완료

==========================================
         50 VU 테스트 시작
==========================================

[1/16] http-json - 50 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/http-json-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 50 max VUs, 1m0s max duration (incl. graceful stop):
              * http_json_test: 50 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:47:00Z" level=info msg="============================================================" source=console
time="2026-01-17T01:47:00Z" level=info msg="Phase 3: HTTP/JSON Test - VUs: 50, Size: 10kb" source=console
time="2026-01-17T01:47:00Z" level=info msg="============================================================" source=console
time="2026-01-17T01:47:00Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:47:00Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/JSON\",\"testName\":\"phase3-50vu-10kb\",\"startTime\":1768614420065}" source=console

running (0m01.0s), 50/50 VUs, 8659 complete and 0 interrupted iterations
http_json_test   [   3% ] 50 VUs  01.0s/30s

running (0m02.0s), 50/50 VUs, 18919 complete and 0 interrupted iterations
http_json_test   [   7% ] 50 VUs  02.0s/30s

running (0m03.0s), 50/50 VUs, 28977 complete and 0 interrupted iterations
http_json_test   [  10% ] 50 VUs  03.0s/30s

running (0m04.0s), 50/50 VUs, 39235 complete and 0 interrupted iterations
http_json_test   [  13% ] 50 VUs  04.0s/30s

running (0m05.0s), 50/50 VUs, 49723 complete and 0 interrupted iterations
http_json_test   [  17% ] 50 VUs  05.0s/30s

running (0m06.0s), 50/50 VUs, 60135 complete and 0 interrupted iterations
http_json_test   [  20% ] 50 VUs  06.0s/30s

running (0m07.0s), 50/50 VUs, 70345 complete and 0 interrupted iterations
http_json_test   [  23% ] 50 VUs  07.0s/30s

running (0m08.0s), 50/50 VUs, 80579 complete and 0 interrupted iterations
http_json_test   [  27% ] 50 VUs  08.0s/30s

running (0m09.0s), 50/50 VUs, 90918 complete and 0 interrupted iterations
http_json_test   [  30% ] 50 VUs  09.0s/30s

running (0m10.0s), 50/50 VUs, 101530 complete and 0 interrupted iterations
http_json_test   [  33% ] 50 VUs  10.0s/30s

running (0m11.0s), 50/50 VUs, 112057 complete and 0 interrupted iterations
http_json_test   [  37% ] 50 VUs  11.0s/30s

running (0m12.0s), 50/50 VUs, 122325 complete and 0 interrupted iterations
http_json_test   [  40% ] 50 VUs  12.0s/30s

running (0m13.0s), 50/50 VUs, 132982 complete and 0 interrupted iterations
http_json_test   [  43% ] 50 VUs  13.0s/30s

running (0m14.0s), 50/50 VUs, 143570 complete and 0 interrupted iterations
http_json_test   [  47% ] 50 VUs  14.0s/30s

running (0m15.0s), 50/50 VUs, 154077 complete and 0 interrupted iterations
http_json_test   [  50% ] 50 VUs  15.0s/30s

running (0m16.0s), 50/50 VUs, 164888 complete and 0 interrupted iterations
http_json_test   [  53% ] 50 VUs  16.0s/30s

running (0m17.0s), 50/50 VUs, 175685 complete and 0 interrupted iterations
http_json_test   [  57% ] 50 VUs  17.0s/30s

running (0m18.0s), 50/50 VUs, 186202 complete and 0 interrupted iterations
http_json_test   [  60% ] 50 VUs  18.0s/30s

running (0m19.0s), 50/50 VUs, 196591 complete and 0 interrupted iterations
http_json_test   [  63% ] 50 VUs  19.0s/30s

running (0m20.0s), 50/50 VUs, 207130 complete and 0 interrupted iterations
http_json_test   [  67% ] 50 VUs  20.0s/30s

running (0m21.0s), 50/50 VUs, 217709 complete and 0 interrupted iterations
http_json_test   [  70% ] 50 VUs  21.0s/30s

running (0m22.0s), 50/50 VUs, 228149 complete and 0 interrupted iterations
http_json_test   [  73% ] 50 VUs  22.0s/30s

running (0m23.0s), 50/50 VUs, 238894 complete and 0 interrupted iterations
http_json_test   [  77% ] 50 VUs  23.0s/30s

running (0m24.0s), 50/50 VUs, 249476 complete and 0 interrupted iterations
http_json_test   [  80% ] 50 VUs  24.0s/30s

running (0m25.0s), 50/50 VUs, 259951 complete and 0 interrupted iterations
http_json_test   [  83% ] 50 VUs  25.0s/30s

running (0m26.0s), 50/50 VUs, 270310 complete and 0 interrupted iterations
http_json_test   [  87% ] 50 VUs  26.0s/30s

running (0m27.0s), 50/50 VUs, 280977 complete and 0 interrupted iterations
http_json_test   [  90% ] 50 VUs  27.0s/30s

running (0m28.0s), 50/50 VUs, 291433 complete and 0 interrupted iterations
http_json_test   [  93% ] 50 VUs  28.0s/30s

running (0m29.0s), 50/50 VUs, 301861 complete and 0 interrupted iterations
http_json_test   [  97% ] 50 VUs  29.0s/30s

running (0m30.0s), 50/50 VUs, 312363 complete and 0 interrupted iterations
http_json_test   [ 100% ] 50 VUs  30.0s/30s

running (0m31.0s), 00/50 VUs, 312642 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 50 VUs  30s
time="2026-01-17T01:47:31Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:47:31Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:47:31Z" level=info msg="============================================================" source=console
time="2026-01-17T01:47:31Z" level=info msg="Protocol: HTTP/JSON" source=console
time="2026-01-17T01:47:31Z" level=info msg="Test Name: phase3-50vu-10kb" source=console
time="2026-01-17T01:47:31Z" level=info msg="Duration: 31013ms" source=console
time="2026-01-17T01:47:31Z" level=info msg="Total Requests: 312642" source=console
time="2026-01-17T01:47:31Z" level=info msg="Throughput: 10081.00 req/s" source=console
time="2026-01-17T01:47:31Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:47:31Z" level=info msg="  Average: 3.02ms" source=console
time="2026-01-17T01:47:31Z" level=info msg="  P50: 2.79ms" source=console
time="2026-01-17T01:47:31Z" level=info msg="  P95: 5.12ms" source=console
time="2026-01-17T01:47:31Z" level=info msg="  P99: 7.19ms" source=console
time="2026-01-17T01:47:31Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:47:31Z" level=info msg="  Peak Heap: 792.07MB" source=console
time="2026-01-17T01:47:31Z" level=info msg="  GC Count: 58" source=console
time="2026-01-17T01:47:31Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=7.15ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 625284  20047.953061/s
    checks_succeeded...: 100.00% 625284 out of 625284
    checks_failed......: 0.00%   0 out of 625284

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=4.67ms min=1.54ms med=4.36ms max=165.23ms p(90)=6.31ms p(95)=7.15ms
      { expected_response:true }...: avg=4.67ms min=1.54ms med=4.36ms max=165.23ms p(90)=6.31ms p(95)=7.15ms
    http_req_failed................: 0.00%  0 out of 312644
    http_reqs......................: 312644 10024.040655/s

    EXECUTION
    iteration_duration.............: avg=4.78ms min=1.63ms med=4.47ms max=58.34ms  p(90)=6.43ms p(95)=7.29ms
    iterations.....................: 312642 10023.97653/s
    vus............................: 0      min=0           max=50
    vus_max........................: 50     min=50          max=50

    NETWORK
    data_received..................: 75 MB  2.4 MB/s
    data_sent......................: 32 MB  1.0 MB/s




running (0m31.2s), 00/50 VUs, 312642 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 50 VUs  30s

[2/16] http-binary - 50 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/http-binary-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 50 max VUs, 1m0s max duration (incl. graceful stop):
              * http_binary_test: 50 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:47:34Z" level=info msg="============================================================" source=console
time="2026-01-17T01:47:34Z" level=info msg="Phase 3: HTTP/Binary Test - VUs: 50, Size: 10kb" source=console
time="2026-01-17T01:47:34Z" level=info msg="============================================================" source=console
time="2026-01-17T01:47:34Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:47:34Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/Binary\",\"testName\":\"phase3-50vu-10kb\",\"startTime\":1768614454470}" source=console

running (0m01.0s), 50/50 VUs, 11910 complete and 0 interrupted iterations
http_binary_test   [   3% ] 50 VUs  01.0s/30s

running (0m02.0s), 50/50 VUs, 24007 complete and 0 interrupted iterations
http_binary_test   [   7% ] 50 VUs  02.0s/30s

running (0m03.0s), 50/50 VUs, 36182 complete and 0 interrupted iterations
http_binary_test   [  10% ] 50 VUs  03.0s/30s

running (0m04.0s), 50/50 VUs, 48407 complete and 0 interrupted iterations
http_binary_test   [  13% ] 50 VUs  04.0s/30s

running (0m05.0s), 50/50 VUs, 60330 complete and 0 interrupted iterations
http_binary_test   [  17% ] 50 VUs  05.0s/30s

running (0m06.0s), 50/50 VUs, 72463 complete and 0 interrupted iterations
http_binary_test   [  20% ] 50 VUs  06.0s/30s

running (0m07.0s), 50/50 VUs, 84705 complete and 0 interrupted iterations
http_binary_test   [  23% ] 50 VUs  07.0s/30s

running (0m08.0s), 50/50 VUs, 96910 complete and 0 interrupted iterations
http_binary_test   [  27% ] 50 VUs  08.0s/30s

running (0m09.0s), 50/50 VUs, 108964 complete and 0 interrupted iterations
http_binary_test   [  30% ] 50 VUs  09.0s/30s

running (0m10.0s), 50/50 VUs, 121168 complete and 0 interrupted iterations
http_binary_test   [  33% ] 50 VUs  10.0s/30s

running (0m11.0s), 50/50 VUs, 133154 complete and 0 interrupted iterations
http_binary_test   [  37% ] 50 VUs  11.0s/30s

running (0m12.0s), 50/50 VUs, 144964 complete and 0 interrupted iterations
http_binary_test   [  40% ] 50 VUs  12.0s/30s

running (0m13.0s), 50/50 VUs, 156755 complete and 0 interrupted iterations
http_binary_test   [  43% ] 50 VUs  13.0s/30s

running (0m14.0s), 50/50 VUs, 168289 complete and 0 interrupted iterations
http_binary_test   [  47% ] 50 VUs  14.0s/30s

running (0m15.0s), 50/50 VUs, 180089 complete and 0 interrupted iterations
http_binary_test   [  50% ] 50 VUs  15.0s/30s

running (0m16.0s), 50/50 VUs, 191554 complete and 0 interrupted iterations
http_binary_test   [  53% ] 50 VUs  16.0s/30s

running (0m17.0s), 50/50 VUs, 202471 complete and 0 interrupted iterations
http_binary_test   [  57% ] 50 VUs  17.0s/30s

running (0m18.0s), 50/50 VUs, 213988 complete and 0 interrupted iterations
http_binary_test   [  60% ] 50 VUs  18.0s/30s

running (0m19.0s), 50/50 VUs, 227022 complete and 0 interrupted iterations
http_binary_test   [  63% ] 50 VUs  19.0s/30s

running (0m20.0s), 50/50 VUs, 240283 complete and 0 interrupted iterations
http_binary_test   [  67% ] 50 VUs  20.0s/30s

running (0m21.0s), 50/50 VUs, 253657 complete and 0 interrupted iterations
http_binary_test   [  70% ] 50 VUs  21.0s/30s

running (0m22.0s), 50/50 VUs, 266798 complete and 0 interrupted iterations
http_binary_test   [  73% ] 50 VUs  22.0s/30s

running (0m23.0s), 50/50 VUs, 280005 complete and 0 interrupted iterations
http_binary_test   [  77% ] 50 VUs  23.0s/30s

running (0m24.0s), 50/50 VUs, 293729 complete and 0 interrupted iterations
http_binary_test   [  80% ] 50 VUs  24.0s/30s

running (0m25.0s), 50/50 VUs, 307373 complete and 0 interrupted iterations
http_binary_test   [  83% ] 50 VUs  25.0s/30s

running (0m26.0s), 50/50 VUs, 321124 complete and 0 interrupted iterations
http_binary_test   [  87% ] 50 VUs  26.0s/30s

running (0m27.0s), 50/50 VUs, 334994 complete and 0 interrupted iterations
http_binary_test   [  90% ] 50 VUs  27.0s/30s

running (0m28.0s), 50/50 VUs, 348849 complete and 0 interrupted iterations
http_binary_test   [  93% ] 50 VUs  28.0s/30s

running (0m29.0s), 50/50 VUs, 362640 complete and 0 interrupted iterations
http_binary_test   [  97% ] 50 VUs  29.0s/30s

running (0m30.0s), 50/50 VUs, 376396 complete and 0 interrupted iterations
http_binary_test   [ 100% ] 50 VUs  30.0s/30s

running (0m31.0s), 00/50 VUs, 376757 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 50 VUs  30s
time="2026-01-17T01:48:05Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:48:05Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:48:05Z" level=info msg="============================================================" source=console
time="2026-01-17T01:48:05Z" level=info msg="Protocol: HTTP/Binary" source=console
time="2026-01-17T01:48:05Z" level=info msg="Test Name: phase3-50vu-10kb" source=console
time="2026-01-17T01:48:05Z" level=info msg="Duration: 31007ms" source=console
time="2026-01-17T01:48:05Z" level=info msg="Total Requests: 376757" source=console
time="2026-01-17T01:48:05Z" level=info msg="Throughput: 12150.71 req/s" source=console
time="2026-01-17T01:48:05Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:48:05Z" level=info msg="  Average: 2.31ms" source=console
time="2026-01-17T01:48:05Z" level=info msg="  P50: 2.11ms" source=console
time="2026-01-17T01:48:05Z" level=info msg="  P95: 4.03ms" source=console
time="2026-01-17T01:48:05Z" level=info msg="  P99: 5.38ms" source=console
time="2026-01-17T01:48:05Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:48:05Z" level=info msg="  Peak Heap: 962.38MB" source=console
time="2026-01-17T01:48:05Z" level=info msg="  GC Count: 30" source=console
time="2026-01-17T01:48:05Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=5.96ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 753514  24187.205615/s
    checks_succeeded...: 100.00% 753514 out of 753514
    checks_failed......: 0.00%   0 out of 753514

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=3.88ms min=1.25ms med=3.61ms max=127.79ms p(90)=5.29ms p(95)=5.96ms
      { expected_response:true }...: avg=3.88ms min=1.25ms med=3.61ms max=127.79ms p(90)=5.29ms p(95)=5.96ms
    http_req_failed................: 0.00%  0 out of 376759
    http_reqs......................: 376759 12093.667006/s

    EXECUTION
    iteration_duration.............: avg=3.97ms min=1.31ms med=3.7ms  max=39.93ms  p(90)=5.38ms p(95)=6.06ms
    iterations.....................: 376757 12093.602808/s
    vus............................: 0      min=0           max=50
    vus_max........................: 50     min=50          max=50

    NETWORK
    data_received..................: 92 MB  2.9 MB/s
    data_sent......................: 40 MB  1.3 MB/s




running (0m31.2s), 00/50 VUs, 376757 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 50 VUs  30s

[3/16] grpc-unary - 50 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/grpc-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 50 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_test: 50 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:48:08Z" level=info msg="============================================================" source=console
time="2026-01-17T01:48:08Z" level=info msg="Phase 3: gRPC/Unary Test - VUs: 50, Size: 10kb" source=console
time="2026-01-17T01:48:08Z" level=info msg="============================================================" source=console
time="2026-01-17T01:48:08Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:48:08Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Unary\",\"testName\":\"phase3-50vu-10kb\",\"startTime\":1768614488859}" source=console

running (0m01.0s), 50/50 VUs, 9909 complete and 0 interrupted iterations
grpc_test   [   3% ] 50 VUs  01.0s/30s

running (0m02.0s), 50/50 VUs, 21082 complete and 0 interrupted iterations
grpc_test   [   7% ] 50 VUs  02.0s/30s

running (0m03.0s), 50/50 VUs, 31859 complete and 0 interrupted iterations
grpc_test   [  10% ] 50 VUs  03.0s/30s

running (0m04.0s), 50/50 VUs, 41928 complete and 0 interrupted iterations
grpc_test   [  13% ] 50 VUs  04.0s/30s

running (0m05.0s), 50/50 VUs, 52484 complete and 0 interrupted iterations
grpc_test   [  17% ] 50 VUs  05.0s/30s

running (0m06.0s), 50/50 VUs, 63896 complete and 0 interrupted iterations
grpc_test   [  20% ] 50 VUs  06.0s/30s

running (0m07.0s), 50/50 VUs, 75034 complete and 0 interrupted iterations
grpc_test   [  23% ] 50 VUs  07.0s/30s

running (0m08.0s), 50/50 VUs, 86294 complete and 0 interrupted iterations
grpc_test   [  27% ] 50 VUs  08.0s/30s

running (0m09.0s), 50/50 VUs, 96627 complete and 0 interrupted iterations
grpc_test   [  30% ] 50 VUs  09.0s/30s

running (0m10.0s), 50/50 VUs, 108357 complete and 0 interrupted iterations
grpc_test   [  33% ] 50 VUs  10.0s/30s

running (0m11.0s), 50/50 VUs, 120324 complete and 0 interrupted iterations
grpc_test   [  37% ] 50 VUs  11.0s/30s

running (0m12.0s), 50/50 VUs, 131674 complete and 0 interrupted iterations
grpc_test   [  40% ] 50 VUs  12.0s/30s

running (0m13.0s), 50/50 VUs, 142701 complete and 0 interrupted iterations
grpc_test   [  43% ] 50 VUs  13.0s/30s

running (0m14.0s), 50/50 VUs, 154743 complete and 0 interrupted iterations
grpc_test   [  47% ] 50 VUs  14.0s/30s

running (0m15.0s), 50/50 VUs, 166444 complete and 0 interrupted iterations
grpc_test   [  50% ] 50 VUs  15.0s/30s

running (0m16.0s), 50/50 VUs, 177283 complete and 0 interrupted iterations
grpc_test   [  53% ] 50 VUs  16.0s/30s

running (0m17.0s), 50/50 VUs, 188778 complete and 0 interrupted iterations
grpc_test   [  57% ] 50 VUs  17.0s/30s

running (0m18.0s), 50/50 VUs, 200513 complete and 0 interrupted iterations
grpc_test   [  60% ] 50 VUs  18.0s/30s

running (0m19.0s), 50/50 VUs, 211104 complete and 0 interrupted iterations
grpc_test   [  63% ] 50 VUs  19.0s/30s

running (0m20.0s), 50/50 VUs, 222126 complete and 0 interrupted iterations
grpc_test   [  67% ] 50 VUs  20.0s/30s

running (0m21.0s), 50/50 VUs, 233636 complete and 0 interrupted iterations
grpc_test   [  70% ] 50 VUs  21.0s/30s

running (0m22.0s), 50/50 VUs, 245870 complete and 0 interrupted iterations
grpc_test   [  73% ] 50 VUs  22.0s/30s

running (0m23.0s), 50/50 VUs, 257610 complete and 0 interrupted iterations
grpc_test   [  77% ] 50 VUs  23.0s/30s

running (0m24.0s), 50/50 VUs, 269492 complete and 0 interrupted iterations
grpc_test   [  80% ] 50 VUs  24.0s/30s

running (0m25.0s), 50/50 VUs, 279244 complete and 0 interrupted iterations
grpc_test   [  83% ] 50 VUs  25.0s/30s

running (0m26.0s), 50/50 VUs, 290089 complete and 0 interrupted iterations
grpc_test   [  87% ] 50 VUs  26.0s/30s

running (0m27.0s), 50/50 VUs, 301502 complete and 0 interrupted iterations
grpc_test   [  90% ] 50 VUs  27.0s/30s

running (0m28.0s), 50/50 VUs, 311424 complete and 0 interrupted iterations
grpc_test   [  93% ] 50 VUs  28.0s/30s

running (0m29.0s), 50/50 VUs, 322676 complete and 0 interrupted iterations
grpc_test   [  97% ] 50 VUs  29.0s/30s

running (0m30.0s), 50/50 VUs, 334202 complete and 0 interrupted iterations
grpc_test   [ 100% ] 50 VUs  30.0s/30s

running (0m31.0s), 00/50 VUs, 334376 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 50 VUs  30s
time="2026-01-17T01:48:40Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:48:40Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:48:40Z" level=info msg="============================================================" source=console
time="2026-01-17T01:48:40Z" level=info msg="Protocol: gRPC/Unary" source=console
time="2026-01-17T01:48:40Z" level=info msg="Test Name: phase3-50vu-10kb" source=console
time="2026-01-17T01:48:40Z" level=info msg="Duration: 31033ms" source=console
time="2026-01-17T01:48:40Z" level=info msg="Total Requests: 334376" source=console
time="2026-01-17T01:48:40Z" level=info msg="Throughput: 10774.85 req/s" source=console
time="2026-01-17T01:48:40Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:48:40Z" level=info msg="  Average: 3.00ms" source=console
time="2026-01-17T01:48:40Z" level=info msg="  P50: 2.79ms" source=console
time="2026-01-17T01:48:40Z" level=info msg="  P95: 5.38ms" source=console
time="2026-01-17T01:48:40Z" level=info msg="  P99: 6.92ms" source=console
time="2026-01-17T01:48:40Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:48:40Z" level=info msg="  Peak Heap: 956.37MB" source=console
time="2026-01-17T01:48:40Z" level=info msg="  GC Count: 44" source=console
time="2026-01-17T01:48:40Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=7.3ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 668752  21449.585565/s
    checks_succeeded...: 100.00% 668752 out of 668752
    checks_failed......: 0.00%   0 out of 668752

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=4.32ms min=798.26µs med=3.95ms max=129.98ms p(90)=6.32ms p(95)=7.3ms
      { expected_response:true }...: avg=4.32ms min=798.26µs med=3.95ms max=129.98ms p(90)=6.32ms p(95)=7.3ms
    http_req_failed................: 0.00%  0 out of 334378
    http_reqs......................: 334378 10724.856931/s

    EXECUTION
    iteration_duration.............: avg=4.47ms min=1.03ms   med=4.1ms  max=51.12ms  p(90)=6.49ms p(95)=7.5ms
    iterations.....................: 334376 10724.792782/s
    vus............................: 0      min=0           max=50
    vus_max........................: 50     min=50          max=50

    NETWORK
    data_received..................: 81 MB  2.6 MB/s
    data_sent......................: 33 MB  1.1 MB/s




running (0m31.2s), 00/50 VUs, 334376 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 50 VUs  30s

[4/16] grpc-stream - 50 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/grpc-stream-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 50 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_stream_test: 50 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:48:43Z" level=info msg="============================================================" source=console
time="2026-01-17T01:48:43Z" level=info msg="Phase 3: gRPC/Stream Test - VUs: 50, Size: 10kb" source=console
time="2026-01-17T01:48:43Z" level=info msg="============================================================" source=console
time="2026-01-17T01:48:43Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:48:43Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Stream\",\"testName\":\"phase3-50vu-10kb\",\"startTime\":1768614523343}" source=console

running (0m01.0s), 50/50 VUs, 10528 complete and 0 interrupted iterations
grpc_stream_test   [   3% ] 50 VUs  01.0s/30s

running (0m02.0s), 50/50 VUs, 21078 complete and 0 interrupted iterations
grpc_stream_test   [   7% ] 50 VUs  02.0s/30s

running (0m03.0s), 50/50 VUs, 32043 complete and 0 interrupted iterations
grpc_stream_test   [  10% ] 50 VUs  03.0s/30s

running (0m04.0s), 50/50 VUs, 42178 complete and 0 interrupted iterations
grpc_stream_test   [  13% ] 50 VUs  04.0s/30s

running (0m05.0s), 50/50 VUs, 52772 complete and 0 interrupted iterations
grpc_stream_test   [  17% ] 50 VUs  05.0s/30s

running (0m06.0s), 50/50 VUs, 63558 complete and 0 interrupted iterations
grpc_stream_test   [  20% ] 50 VUs  06.0s/30s

running (0m07.0s), 50/50 VUs, 73185 complete and 0 interrupted iterations
grpc_stream_test   [  23% ] 50 VUs  07.0s/30s

running (0m08.0s), 50/50 VUs, 82516 complete and 0 interrupted iterations
grpc_stream_test   [  27% ] 50 VUs  08.0s/30s

running (0m09.0s), 50/50 VUs, 90455 complete and 0 interrupted iterations
grpc_stream_test   [  30% ] 50 VUs  09.0s/30s

running (0m10.0s), 50/50 VUs, 100828 complete and 0 interrupted iterations
grpc_stream_test   [  33% ] 50 VUs  10.0s/30s

running (0m11.0s), 50/50 VUs, 111006 complete and 0 interrupted iterations
grpc_stream_test   [  37% ] 50 VUs  11.0s/30s

running (0m12.0s), 50/50 VUs, 119856 complete and 0 interrupted iterations
grpc_stream_test   [  40% ] 50 VUs  12.0s/30s

running (0m13.0s), 50/50 VUs, 128294 complete and 0 interrupted iterations
grpc_stream_test   [  43% ] 50 VUs  13.0s/30s

running (0m14.0s), 50/50 VUs, 136184 complete and 0 interrupted iterations
grpc_stream_test   [  47% ] 50 VUs  14.0s/30s

running (0m15.0s), 50/50 VUs, 145057 complete and 0 interrupted iterations
grpc_stream_test   [  50% ] 50 VUs  15.0s/30s

running (0m16.0s), 50/50 VUs, 154455 complete and 0 interrupted iterations
grpc_stream_test   [  53% ] 50 VUs  16.0s/30s

running (0m17.0s), 50/50 VUs, 163512 complete and 0 interrupted iterations
grpc_stream_test   [  57% ] 50 VUs  17.0s/30s

running (0m18.0s), 50/50 VUs, 172818 complete and 0 interrupted iterations
grpc_stream_test   [  60% ] 50 VUs  18.0s/30s

running (0m19.0s), 50/50 VUs, 182495 complete and 0 interrupted iterations
grpc_stream_test   [  63% ] 50 VUs  19.0s/30s

running (0m20.0s), 50/50 VUs, 192525 complete and 0 interrupted iterations
grpc_stream_test   [  67% ] 50 VUs  20.0s/30s

running (0m21.0s), 50/50 VUs, 201785 complete and 0 interrupted iterations
grpc_stream_test   [  70% ] 50 VUs  21.0s/30s

running (0m22.0s), 50/50 VUs, 211310 complete and 0 interrupted iterations
grpc_stream_test   [  73% ] 50 VUs  22.0s/30s

running (0m23.0s), 50/50 VUs, 221728 complete and 0 interrupted iterations
grpc_stream_test   [  77% ] 50 VUs  23.0s/30s

running (0m24.0s), 50/50 VUs, 231512 complete and 0 interrupted iterations
grpc_stream_test   [  80% ] 50 VUs  24.0s/30s

running (0m25.0s), 50/50 VUs, 241925 complete and 0 interrupted iterations
grpc_stream_test   [  83% ] 50 VUs  25.0s/30s

running (0m26.0s), 50/50 VUs, 253090 complete and 0 interrupted iterations
grpc_stream_test   [  87% ] 50 VUs  26.0s/30s

running (0m27.0s), 50/50 VUs, 263836 complete and 0 interrupted iterations
grpc_stream_test   [  90% ] 50 VUs  27.0s/30s

running (0m28.0s), 50/50 VUs, 274784 complete and 0 interrupted iterations
grpc_stream_test   [  93% ] 50 VUs  28.0s/30s

running (0m29.0s), 50/50 VUs, 285614 complete and 0 interrupted iterations
grpc_stream_test   [  97% ] 50 VUs  29.0s/30s

running (0m30.0s), 50/50 VUs, 296811 complete and 0 interrupted iterations
grpc_stream_test   [ 100% ] 50 VUs  30.0s/30s

running (0m31.0s), 00/50 VUs, 297046 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 50 VUs  30s
time="2026-01-17T01:49:14Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:49:14Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:49:14Z" level=info msg="============================================================" source=console
time="2026-01-17T01:49:14Z" level=info msg="Protocol: gRPC/Stream" source=console
time="2026-01-17T01:49:14Z" level=info msg="Test Name: phase3-50vu-10kb" source=console
time="2026-01-17T01:49:14Z" level=info msg="Duration: 31007ms" source=console
time="2026-01-17T01:49:14Z" level=info msg="Total Requests: 297046" source=console
time="2026-01-17T01:49:14Z" level=info msg="Throughput: 9579.97 req/s" source=console
time="2026-01-17T01:49:14Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:49:14Z" level=info msg="  Average: 3.30ms" source=console
time="2026-01-17T01:49:14Z" level=info msg="  P50: 3.15ms" source=console
time="2026-01-17T01:49:14Z" level=info msg="  P95: 5.48ms" source=console
time="2026-01-17T01:49:14Z" level=info msg="  P99: 6.99ms" source=console
time="2026-01-17T01:49:14Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:49:14Z" level=info msg="  Peak Heap: 802.04MB" source=console
time="2026-01-17T01:49:14Z" level=info msg="  GC Count: 19" source=console
time="2026-01-17T01:49:14Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=7.93ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 594092  19084.248627/s
    checks_succeeded...: 100.00% 594092 out of 594092
    checks_failed......: 0.00%   0 out of 594092

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=4.84ms min=1.02ms med=4.45ms max=110.92ms p(90)=6.74ms p(95)=7.93ms
      { expected_response:true }...: avg=4.84ms min=1.02ms med=4.45ms max=110.92ms p(90)=6.74ms p(95)=7.93ms
    http_req_failed................: 0.00%  0 out of 297048
    http_reqs......................: 297048 9542.18856/s

    EXECUTION
    iteration_duration.............: avg=5.03ms min=1.14ms med=4.62ms max=40.76ms  p(90)=6.97ms p(95)=8.22ms
    iterations.....................: 297046 9542.124314/s
    vus............................: 0      min=0           max=50
    vus_max........................: 50     min=50          max=50

    NETWORK
    data_received..................: 72 MB  2.3 MB/s
    data_sent......................: 31 MB  1.0 MB/s




running (0m31.1s), 00/50 VUs, 297046 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 50 VUs  30s

==========================================
         100 VU 테스트 시작
==========================================

[5/16] http-json - 100 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/http-json-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m0s max duration (incl. graceful stop):
              * http_json_test: 100 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:49:17Z" level=info msg="============================================================" source=console
time="2026-01-17T01:49:17Z" level=info msg="Phase 3: HTTP/JSON Test - VUs: 100, Size: 10kb" source=console
time="2026-01-17T01:49:17Z" level=info msg="============================================================" source=console
time="2026-01-17T01:49:17Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:49:17Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/JSON\",\"testName\":\"phase3-100vu-10kb\",\"startTime\":1768614557737}" source=console

running (0m01.0s), 100/100 VUs, 12064 complete and 0 interrupted iterations
http_json_test   [   3% ] 100 VUs  01.0s/30s

running (0m02.0s), 100/100 VUs, 24590 complete and 0 interrupted iterations
http_json_test   [   7% ] 100 VUs  02.0s/30s

running (0m03.0s), 100/100 VUs, 36963 complete and 0 interrupted iterations
http_json_test   [  10% ] 100 VUs  03.0s/30s

running (0m04.0s), 100/100 VUs, 49280 complete and 0 interrupted iterations
http_json_test   [  13% ] 100 VUs  04.0s/30s

running (0m05.0s), 100/100 VUs, 61638 complete and 0 interrupted iterations
http_json_test   [  17% ] 100 VUs  05.0s/30s

running (0m06.0s), 100/100 VUs, 74109 complete and 0 interrupted iterations
http_json_test   [  20% ] 100 VUs  06.0s/30s

running (0m07.0s), 100/100 VUs, 86794 complete and 0 interrupted iterations
http_json_test   [  23% ] 100 VUs  07.0s/30s

running (0m08.0s), 100/100 VUs, 98970 complete and 0 interrupted iterations
http_json_test   [  27% ] 100 VUs  08.0s/30s

running (0m09.0s), 100/100 VUs, 111494 complete and 0 interrupted iterations
http_json_test   [  30% ] 100 VUs  09.0s/30s

running (0m10.0s), 100/100 VUs, 123124 complete and 0 interrupted iterations
http_json_test   [  33% ] 100 VUs  10.0s/30s

running (0m11.0s), 100/100 VUs, 135878 complete and 0 interrupted iterations
http_json_test   [  37% ] 100 VUs  11.0s/30s

running (0m12.0s), 100/100 VUs, 148002 complete and 0 interrupted iterations
http_json_test   [  40% ] 100 VUs  12.0s/30s

running (0m13.0s), 100/100 VUs, 160316 complete and 0 interrupted iterations
http_json_test   [  43% ] 100 VUs  13.0s/30s

running (0m14.0s), 100/100 VUs, 172917 complete and 0 interrupted iterations
http_json_test   [  47% ] 100 VUs  14.0s/30s

running (0m15.0s), 100/100 VUs, 184882 complete and 0 interrupted iterations
http_json_test   [  50% ] 100 VUs  15.0s/30s

running (0m16.0s), 100/100 VUs, 194714 complete and 0 interrupted iterations
http_json_test   [  53% ] 100 VUs  16.0s/30s

running (0m17.0s), 100/100 VUs, 205458 complete and 0 interrupted iterations
http_json_test   [  57% ] 100 VUs  17.0s/30s

running (0m18.0s), 100/100 VUs, 217080 complete and 0 interrupted iterations
http_json_test   [  60% ] 100 VUs  18.0s/30s

running (0m19.0s), 100/100 VUs, 227980 complete and 0 interrupted iterations
http_json_test   [  63% ] 100 VUs  19.0s/30s

running (0m20.0s), 100/100 VUs, 238369 complete and 0 interrupted iterations
http_json_test   [  67% ] 100 VUs  20.0s/30s

running (0m21.0s), 100/100 VUs, 249811 complete and 0 interrupted iterations
http_json_test   [  70% ] 100 VUs  21.0s/30s

running (0m22.0s), 100/100 VUs, 261109 complete and 0 interrupted iterations
http_json_test   [  73% ] 100 VUs  22.0s/30s

running (0m23.0s), 100/100 VUs, 272676 complete and 0 interrupted iterations
http_json_test   [  77% ] 100 VUs  23.0s/30s

running (0m24.0s), 100/100 VUs, 284401 complete and 0 interrupted iterations
http_json_test   [  80% ] 100 VUs  24.0s/30s

running (0m25.0s), 100/100 VUs, 295628 complete and 0 interrupted iterations
http_json_test   [  83% ] 100 VUs  25.0s/30s

running (0m26.0s), 100/100 VUs, 306953 complete and 0 interrupted iterations
http_json_test   [  87% ] 100 VUs  26.0s/30s

running (0m27.0s), 100/100 VUs, 317788 complete and 0 interrupted iterations
http_json_test   [  90% ] 100 VUs  27.0s/30s

running (0m28.0s), 100/100 VUs, 329014 complete and 0 interrupted iterations
http_json_test   [  93% ] 100 VUs  28.0s/30s

running (0m29.0s), 100/100 VUs, 340243 complete and 0 interrupted iterations
http_json_test   [  97% ] 100 VUs  29.0s/30s

running (0m30.0s), 100/100 VUs, 351843 complete and 0 interrupted iterations
http_json_test   [ 100% ] 100 VUs  30.0s/30s

running (0m31.0s), 000/100 VUs, 352207 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 100 VUs  30s
time="2026-01-17T01:49:48Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:49:48Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:49:48Z" level=info msg="============================================================" source=console
time="2026-01-17T01:49:48Z" level=info msg="Protocol: HTTP/JSON" source=console
time="2026-01-17T01:49:48Z" level=info msg="Test Name: phase3-100vu-10kb" source=console
time="2026-01-17T01:49:48Z" level=info msg="Duration: 31010ms" source=console
time="2026-01-17T01:49:48Z" level=info msg="Total Requests: 352207" source=console
time="2026-01-17T01:49:48Z" level=info msg="Throughput: 11357.85 req/s" source=console
time="2026-01-17T01:49:48Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:49:48Z" level=info msg="  Average: 5.55ms" source=console
time="2026-01-17T01:49:48Z" level=info msg="  P50: 5.10ms" source=console
time="2026-01-17T01:49:48Z" level=info msg="  P95: 9.93ms" source=console
time="2026-01-17T01:49:48Z" level=info msg="  P99: 14.32ms" source=console
time="2026-01-17T01:49:48Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:49:48Z" level=info msg="  Peak Heap: 980.98MB" source=console
time="2026-01-17T01:49:48Z" level=info msg="  GC Count: 31" source=console
time="2026-01-17T01:49:48Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=13.43ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 704414  22618.063763/s
    checks_succeeded...: 100.00% 704414 out of 704414
    checks_failed......: 0.00%   0 out of 704414

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=8.43ms min=1.44ms med=7.87ms max=123.23ms p(90)=11.76ms p(95)=13.43ms
      { expected_response:true }...: avg=8.43ms min=1.44ms med=7.87ms max=123.23ms p(90)=11.76ms p(95)=13.43ms
    http_req_failed................: 0.00%  0 out of 352209
    http_reqs......................: 352209 11309.0961/s

    EXECUTION
    iteration_duration.............: avg=8.51ms min=1.72ms med=7.94ms max=55.15ms  p(90)=11.83ms p(95)=13.51ms
    iterations.....................: 352207 11309.031882/s
    vus............................: 0      min=0           max=100
    vus_max........................: 100    min=100         max=100

    NETWORK
    data_received..................: 85 MB  2.7 MB/s
    data_sent......................: 36 MB  1.2 MB/s




running (0m31.1s), 000/100 VUs, 352207 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 100 VUs  30s

[6/16] http-binary - 100 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/http-binary-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m0s max duration (incl. graceful stop):
              * http_binary_test: 100 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:49:52Z" level=info msg="============================================================" source=console
time="2026-01-17T01:49:52Z" level=info msg="Phase 3: HTTP/Binary Test - VUs: 100, Size: 10kb" source=console
time="2026-01-17T01:49:52Z" level=info msg="============================================================" source=console
time="2026-01-17T01:49:52Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:49:52Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/Binary\",\"testName\":\"phase3-100vu-10kb\",\"startTime\":1768614592199}" source=console

running (0m01.0s), 100/100 VUs, 14032 complete and 0 interrupted iterations
http_binary_test   [   3% ] 100 VUs  01.0s/30s

running (0m02.0s), 100/100 VUs, 27191 complete and 0 interrupted iterations
http_binary_test   [   7% ] 100 VUs  02.0s/30s

running (0m03.0s), 100/100 VUs, 40671 complete and 0 interrupted iterations
http_binary_test   [  10% ] 100 VUs  03.0s/30s

running (0m04.0s), 100/100 VUs, 54002 complete and 0 interrupted iterations
http_binary_test   [  13% ] 100 VUs  04.0s/30s

running (0m05.0s), 100/100 VUs, 68708 complete and 0 interrupted iterations
http_binary_test   [  17% ] 100 VUs  05.0s/30s

running (0m06.0s), 100/100 VUs, 83265 complete and 0 interrupted iterations
http_binary_test   [  20% ] 100 VUs  06.0s/30s

running (0m07.0s), 100/100 VUs, 98173 complete and 0 interrupted iterations
http_binary_test   [  23% ] 100 VUs  07.0s/30s

running (0m08.0s), 100/100 VUs, 112647 complete and 0 interrupted iterations
http_binary_test   [  27% ] 100 VUs  08.0s/30s

running (0m09.0s), 100/100 VUs, 127424 complete and 0 interrupted iterations
http_binary_test   [  30% ] 100 VUs  09.0s/30s

running (0m10.0s), 100/100 VUs, 142442 complete and 0 interrupted iterations
http_binary_test   [  33% ] 100 VUs  10.0s/30s

running (0m11.0s), 100/100 VUs, 156864 complete and 0 interrupted iterations
http_binary_test   [  37% ] 100 VUs  11.0s/30s

running (0m12.0s), 100/100 VUs, 171095 complete and 0 interrupted iterations
http_binary_test   [  40% ] 100 VUs  12.0s/30s

running (0m13.0s), 100/100 VUs, 186046 complete and 0 interrupted iterations
http_binary_test   [  43% ] 100 VUs  13.0s/30s

running (0m14.0s), 100/100 VUs, 200925 complete and 0 interrupted iterations
http_binary_test   [  47% ] 100 VUs  14.0s/30s

running (0m15.0s), 100/100 VUs, 215344 complete and 0 interrupted iterations
http_binary_test   [  50% ] 100 VUs  15.0s/30s

running (0m16.0s), 100/100 VUs, 229795 complete and 0 interrupted iterations
http_binary_test   [  53% ] 100 VUs  16.0s/30s

running (0m17.0s), 100/100 VUs, 244101 complete and 0 interrupted iterations
http_binary_test   [  57% ] 100 VUs  17.0s/30s

running (0m18.0s), 100/100 VUs, 258724 complete and 0 interrupted iterations
http_binary_test   [  60% ] 100 VUs  18.0s/30s

running (0m19.0s), 100/100 VUs, 273019 complete and 0 interrupted iterations
http_binary_test   [  63% ] 100 VUs  19.0s/30s

running (0m20.0s), 100/100 VUs, 287729 complete and 0 interrupted iterations
http_binary_test   [  67% ] 100 VUs  20.0s/30s

running (0m21.0s), 100/100 VUs, 301023 complete and 0 interrupted iterations
http_binary_test   [  70% ] 100 VUs  21.0s/30s

running (0m22.0s), 100/100 VUs, 314427 complete and 0 interrupted iterations
http_binary_test   [  73% ] 100 VUs  22.0s/30s

running (0m23.0s), 100/100 VUs, 328960 complete and 0 interrupted iterations
http_binary_test   [  77% ] 100 VUs  23.0s/30s

running (0m24.0s), 100/100 VUs, 343408 complete and 0 interrupted iterations
http_binary_test   [  80% ] 100 VUs  24.0s/30s

running (0m25.0s), 100/100 VUs, 358488 complete and 0 interrupted iterations
http_binary_test   [  83% ] 100 VUs  25.0s/30s

running (0m26.0s), 100/100 VUs, 373130 complete and 0 interrupted iterations
http_binary_test   [  87% ] 100 VUs  26.0s/30s

running (0m27.0s), 100/100 VUs, 387790 complete and 0 interrupted iterations
http_binary_test   [  90% ] 100 VUs  27.0s/30s

running (0m28.0s), 100/100 VUs, 401782 complete and 0 interrupted iterations
http_binary_test   [  93% ] 100 VUs  28.0s/30s

running (0m29.0s), 100/100 VUs, 415231 complete and 0 interrupted iterations
http_binary_test   [  97% ] 100 VUs  29.0s/30s

running (0m30.0s), 100/100 VUs, 428801 complete and 0 interrupted iterations
http_binary_test   [ 100% ] 100 VUs  30.0s/30s

running (0m31.0s), 000/100 VUs, 429169 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 100 VUs  30s
time="2026-01-17T01:50:23Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:50:23Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:50:23Z" level=info msg="============================================================" source=console
time="2026-01-17T01:50:23Z" level=info msg="Protocol: HTTP/Binary" source=console
time="2026-01-17T01:50:23Z" level=info msg="Test Name: phase3-100vu-10kb" source=console
time="2026-01-17T01:50:23Z" level=info msg="Duration: 31010ms" source=console
time="2026-01-17T01:50:23Z" level=info msg="Total Requests: 429169" source=console
time="2026-01-17T01:50:23Z" level=info msg="Throughput: 13839.70 req/s" source=console
time="2026-01-17T01:50:23Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:50:23Z" level=info msg="  Average: 4.16ms" source=console
time="2026-01-17T01:50:23Z" level=info msg="  P50: 3.81ms" source=console
time="2026-01-17T01:50:23Z" level=info msg="  P95: 7.56ms" source=console
time="2026-01-17T01:50:23Z" level=info msg="  P99: 10.74ms" source=console
time="2026-01-17T01:50:23Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:50:23Z" level=info msg="  Peak Heap: 1182.90MB" source=console
time="2026-01-17T01:50:23Z" level=info msg="  GC Count: 29" source=console
time="2026-01-17T01:50:23Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=10.97ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 858338  27538.366746/s
    checks_succeeded...: 100.00% 858338 out of 858338
    checks_failed......: 0.00%   0 out of 858338

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=6.89ms min=1.37ms med=6.42ms max=149.09ms p(90)=9.59ms p(95)=10.97ms
      { expected_response:true }...: avg=6.89ms min=1.37ms med=6.42ms max=149.09ms p(90)=9.59ms p(95)=10.97ms
    http_req_failed................: 0.00%  0 out of 429171
    http_reqs......................: 429171 13769.24754/s

    EXECUTION
    iteration_duration.............: avg=6.98ms min=1.53ms med=6.5ms  max=59.85ms  p(90)=9.69ms p(95)=11.08ms
    iterations.....................: 429169 13769.183373/s
    vus............................: 0      min=0           max=100
    vus_max........................: 100    min=100         max=100

    NETWORK
    data_received..................: 104 MB 3.3 MB/s
    data_sent......................: 45 MB  1.4 MB/s




running (0m31.2s), 000/100 VUs, 429169 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 100 VUs  30s

[7/16] grpc-unary - 100 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/grpc-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_test: 100 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:50:26Z" level=info msg="============================================================" source=console
time="2026-01-17T01:50:26Z" level=info msg="Phase 3: gRPC/Unary Test - VUs: 100, Size: 10kb" source=console
time="2026-01-17T01:50:26Z" level=info msg="============================================================" source=console
time="2026-01-17T01:50:26Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:50:26Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Unary\",\"testName\":\"phase3-100vu-10kb\",\"startTime\":1768614626645}" source=console

running (0m01.0s), 100/100 VUs, 13988 complete and 0 interrupted iterations
grpc_test   [   3% ] 100 VUs  01.0s/30s

running (0m02.0s), 100/100 VUs, 29689 complete and 0 interrupted iterations
grpc_test   [   7% ] 100 VUs  02.0s/30s

running (0m03.0s), 100/100 VUs, 43758 complete and 0 interrupted iterations
grpc_test   [  10% ] 100 VUs  03.0s/30s

running (0m04.0s), 100/100 VUs, 58285 complete and 0 interrupted iterations
grpc_test   [  13% ] 100 VUs  04.0s/30s

running (0m05.0s), 100/100 VUs, 72391 complete and 0 interrupted iterations
grpc_test   [  17% ] 100 VUs  05.0s/30s

running (0m06.0s), 100/100 VUs, 86113 complete and 0 interrupted iterations
grpc_test   [  20% ] 100 VUs  06.0s/30s

running (0m07.0s), 100/100 VUs, 101270 complete and 0 interrupted iterations
grpc_test   [  23% ] 100 VUs  07.0s/30s

running (0m08.0s), 100/100 VUs, 116439 complete and 0 interrupted iterations
grpc_test   [  27% ] 100 VUs  08.0s/30s

running (0m09.0s), 100/100 VUs, 131537 complete and 0 interrupted iterations
grpc_test   [  30% ] 100 VUs  09.0s/30s

running (0m10.0s), 100/100 VUs, 147976 complete and 0 interrupted iterations
grpc_test   [  33% ] 100 VUs  10.0s/30s

running (0m11.0s), 100/100 VUs, 164000 complete and 0 interrupted iterations
grpc_test   [  37% ] 100 VUs  11.0s/30s

running (0m12.0s), 100/100 VUs, 178362 complete and 0 interrupted iterations
grpc_test   [  40% ] 100 VUs  12.0s/30s

running (0m13.0s), 100/100 VUs, 193283 complete and 0 interrupted iterations
grpc_test   [  43% ] 100 VUs  13.0s/30s

running (0m14.0s), 100/100 VUs, 208453 complete and 0 interrupted iterations
grpc_test   [  47% ] 100 VUs  14.0s/30s

running (0m15.0s), 100/100 VUs, 223755 complete and 0 interrupted iterations
grpc_test   [  50% ] 100 VUs  15.0s/30s

running (0m16.0s), 100/100 VUs, 239081 complete and 0 interrupted iterations
grpc_test   [  53% ] 100 VUs  16.0s/30s

running (0m17.0s), 100/100 VUs, 255657 complete and 0 interrupted iterations
grpc_test   [  57% ] 100 VUs  17.0s/30s

running (0m18.0s), 100/100 VUs, 268474 complete and 0 interrupted iterations
grpc_test   [  60% ] 100 VUs  18.0s/30s

running (0m19.0s), 100/100 VUs, 279940 complete and 0 interrupted iterations
grpc_test   [  63% ] 100 VUs  19.0s/30s

running (0m20.0s), 100/100 VUs, 294337 complete and 0 interrupted iterations
grpc_test   [  67% ] 100 VUs  20.0s/30s

running (0m21.0s), 100/100 VUs, 308194 complete and 0 interrupted iterations
grpc_test   [  70% ] 100 VUs  21.0s/30s

running (0m22.0s), 100/100 VUs, 321690 complete and 0 interrupted iterations
grpc_test   [  73% ] 100 VUs  22.0s/30s

running (0m23.0s), 100/100 VUs, 333760 complete and 0 interrupted iterations
grpc_test   [  77% ] 100 VUs  23.0s/30s

running (0m24.0s), 100/100 VUs, 345215 complete and 0 interrupted iterations
grpc_test   [  80% ] 100 VUs  24.0s/30s

running (0m25.0s), 100/100 VUs, 355234 complete and 0 interrupted iterations
grpc_test   [  83% ] 100 VUs  25.0s/30s

running (0m26.0s), 100/100 VUs, 364621 complete and 0 interrupted iterations
grpc_test   [  87% ] 100 VUs  26.0s/30s

running (0m27.0s), 100/100 VUs, 376194 complete and 0 interrupted iterations
grpc_test   [  90% ] 100 VUs  27.0s/30s

running (0m28.0s), 100/100 VUs, 388543 complete and 0 interrupted iterations
grpc_test   [  93% ] 100 VUs  28.0s/30s

running (0m29.0s), 100/100 VUs, 402346 complete and 0 interrupted iterations
grpc_test   [  97% ] 100 VUs  29.0s/30s

running (0m30.0s), 100/100 VUs, 415798 complete and 0 interrupted iterations
grpc_test   [ 100% ] 100 VUs  30.0s/30s

running (0m31.0s), 000/100 VUs, 416217 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 100 VUs  30s
time="2026-01-17T01:50:57Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:50:57Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:50:57Z" level=info msg="============================================================" source=console
time="2026-01-17T01:50:57Z" level=info msg="Protocol: gRPC/Unary" source=console
time="2026-01-17T01:50:57Z" level=info msg="Test Name: phase3-100vu-10kb" source=console
time="2026-01-17T01:50:57Z" level=info msg="Duration: 31007ms" source=console
time="2026-01-17T01:50:57Z" level=info msg="Total Requests: 416217" source=console
time="2026-01-17T01:50:57Z" level=info msg="Throughput: 13423.32 req/s" source=console
time="2026-01-17T01:50:57Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:50:57Z" level=info msg="  Average: 5.47ms" source=console
time="2026-01-17T01:50:57Z" level=info msg="  P50: 5.14ms" source=console
time="2026-01-17T01:50:57Z" level=info msg="  P95: 9.10ms" source=console
time="2026-01-17T01:50:57Z" level=info msg="  P99: 12.09ms" source=console
time="2026-01-17T01:50:57Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:50:57Z" level=info msg="  Peak Heap: 1196.69MB" source=console
time="2026-01-17T01:50:57Z" level=info msg="  GC Count: 19" source=console
time="2026-01-17T01:50:57Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=11.6ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 832434  26721.341853/s
    checks_succeeded...: 100.00% 832434 out of 832434
    checks_failed......: 0.00%   0 out of 832434

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=7.07ms min=885.84µs med=6.51ms max=131.75ms p(90)=9.99ms  p(95)=11.6ms
      { expected_response:true }...: avg=7.07ms min=885.84µs med=6.51ms max=131.75ms p(90)=9.99ms  p(95)=11.6ms
    http_req_failed................: 0.00%  0 out of 416219
    http_reqs......................: 416219 13360.735127/s

    EXECUTION
    iteration_duration.............: avg=7.19ms min=1.02ms   med=6.61ms max=65.81ms  p(90)=10.14ms p(95)=11.8ms
    iterations.....................: 416217 13360.670926/s
    vus............................: 0      min=0           max=100
    vus_max........................: 100    min=100         max=100

    NETWORK
    data_received..................: 101 MB 3.2 MB/s
    data_sent......................: 41 MB  1.3 MB/s




running (0m31.2s), 000/100 VUs, 416217 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 100 VUs  30s

[8/16] grpc-stream - 100 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/grpc-stream-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_stream_test: 100 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:51:01Z" level=info msg="============================================================" source=console
time="2026-01-17T01:51:01Z" level=info msg="Phase 3: gRPC/Stream Test - VUs: 100, Size: 10kb" source=console
time="2026-01-17T01:51:01Z" level=info msg="============================================================" source=console
time="2026-01-17T01:51:01Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:51:01Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Stream\",\"testName\":\"phase3-100vu-10kb\",\"startTime\":1768614661061}" source=console

running (0m01.0s), 100/100 VUs, 11255 complete and 0 interrupted iterations
grpc_stream_test   [   3% ] 100 VUs  01.0s/30s

running (0m02.0s), 100/100 VUs, 23056 complete and 0 interrupted iterations
grpc_stream_test   [   7% ] 100 VUs  02.0s/30s

running (0m03.0s), 100/100 VUs, 33572 complete and 0 interrupted iterations
grpc_stream_test   [  10% ] 100 VUs  03.0s/30s

running (0m04.0s), 100/100 VUs, 45999 complete and 0 interrupted iterations
grpc_stream_test   [  13% ] 100 VUs  04.0s/30s

running (0m05.0s), 100/100 VUs, 56282 complete and 0 interrupted iterations
grpc_stream_test   [  17% ] 100 VUs  05.0s/30s

running (0m06.0s), 100/100 VUs, 68277 complete and 0 interrupted iterations
grpc_stream_test   [  20% ] 100 VUs  06.0s/30s

running (0m07.0s), 100/100 VUs, 78114 complete and 0 interrupted iterations
grpc_stream_test   [  23% ] 100 VUs  07.0s/30s

running (0m08.0s), 100/100 VUs, 90005 complete and 0 interrupted iterations
grpc_stream_test   [  27% ] 100 VUs  08.0s/30s

running (0m09.0s), 100/100 VUs, 101709 complete and 0 interrupted iterations
grpc_stream_test   [  30% ] 100 VUs  09.0s/30s

running (0m10.0s), 100/100 VUs, 113667 complete and 0 interrupted iterations
grpc_stream_test   [  33% ] 100 VUs  10.0s/30s

running (0m11.0s), 100/100 VUs, 125671 complete and 0 interrupted iterations
grpc_stream_test   [  37% ] 100 VUs  11.0s/30s

running (0m12.0s), 100/100 VUs, 138679 complete and 0 interrupted iterations
grpc_stream_test   [  40% ] 100 VUs  12.0s/30s

running (0m13.0s), 100/100 VUs, 150027 complete and 0 interrupted iterations
grpc_stream_test   [  43% ] 100 VUs  13.0s/30s

running (0m14.0s), 100/100 VUs, 161767 complete and 0 interrupted iterations
grpc_stream_test   [  47% ] 100 VUs  14.0s/30s

running (0m15.0s), 100/100 VUs, 173543 complete and 0 interrupted iterations
grpc_stream_test   [  50% ] 100 VUs  15.0s/30s

running (0m16.0s), 100/100 VUs, 185179 complete and 0 interrupted iterations
grpc_stream_test   [  53% ] 100 VUs  16.0s/30s

running (0m17.0s), 100/100 VUs, 196918 complete and 0 interrupted iterations
grpc_stream_test   [  57% ] 100 VUs  17.0s/30s

running (0m18.0s), 100/100 VUs, 207946 complete and 0 interrupted iterations
grpc_stream_test   [  60% ] 100 VUs  18.0s/30s

running (0m19.0s), 100/100 VUs, 219946 complete and 0 interrupted iterations
grpc_stream_test   [  63% ] 100 VUs  19.0s/30s

running (0m20.0s), 100/100 VUs, 231265 complete and 0 interrupted iterations
grpc_stream_test   [  67% ] 100 VUs  20.0s/30s

running (0m21.0s), 100/100 VUs, 243207 complete and 0 interrupted iterations
grpc_stream_test   [  70% ] 100 VUs  21.0s/30s

running (0m22.0s), 100/100 VUs, 254409 complete and 0 interrupted iterations
grpc_stream_test   [  73% ] 100 VUs  22.0s/30s

running (0m23.0s), 100/100 VUs, 266781 complete and 0 interrupted iterations
grpc_stream_test   [  77% ] 100 VUs  23.0s/30s

running (0m24.0s), 100/100 VUs, 275435 complete and 0 interrupted iterations
grpc_stream_test   [  80% ] 100 VUs  24.0s/30s

running (0m25.0s), 100/100 VUs, 287891 complete and 0 interrupted iterations
grpc_stream_test   [  83% ] 100 VUs  25.0s/30s

running (0m26.0s), 100/100 VUs, 299683 complete and 0 interrupted iterations
grpc_stream_test   [  87% ] 100 VUs  26.0s/30s

running (0m27.0s), 100/100 VUs, 312945 complete and 0 interrupted iterations
grpc_stream_test   [  90% ] 100 VUs  27.0s/30s

running (0m28.0s), 100/100 VUs, 324506 complete and 0 interrupted iterations
grpc_stream_test   [  93% ] 100 VUs  28.0s/30s

running (0m29.0s), 100/100 VUs, 336537 complete and 0 interrupted iterations
grpc_stream_test   [  97% ] 100 VUs  29.0s/30s

running (0m30.0s), 100/100 VUs, 348091 complete and 0 interrupted iterations
grpc_stream_test   [ 100% ] 100 VUs  30.0s/30s

running (0m31.0s), 000/100 VUs, 348398 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 100 VUs  30s
time="2026-01-17T01:51:32Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:51:32Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:51:32Z" level=info msg="============================================================" source=console
time="2026-01-17T01:51:32Z" level=info msg="Protocol: gRPC/Stream" source=console
time="2026-01-17T01:51:32Z" level=info msg="Test Name: phase3-100vu-10kb" source=console
time="2026-01-17T01:51:32Z" level=info msg="Duration: 31017ms" source=console
time="2026-01-17T01:51:32Z" level=info msg="Total Requests: 348398" source=console
time="2026-01-17T01:51:32Z" level=info msg="Throughput: 11232.49 req/s" source=console
time="2026-01-17T01:51:32Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:51:32Z" level=info msg="  Average: 6.08ms" source=console
time="2026-01-17T01:51:32Z" level=info msg="  P50: 5.96ms" source=console
time="2026-01-17T01:51:32Z" level=info msg="  P95: 9.72ms" source=console
time="2026-01-17T01:51:32Z" level=info msg="  P99: 12.43ms" source=console
time="2026-01-17T01:51:32Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:51:32Z" level=info msg="  Peak Heap: 1206.09MB" source=console
time="2026-01-17T01:51:32Z" level=info msg="  GC Count: 16" source=console
time="2026-01-17T01:51:32Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=13.68ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 696796  22375.548339/s
    checks_succeeded...: 100.00% 696796 out of 696796
    checks_failed......: 0.00%   0 out of 696796

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=8.42ms min=1.13ms med=7.71ms max=260.69ms p(90)=11.39ms p(95)=13.68ms
      { expected_response:true }...: avg=8.42ms min=1.13ms med=7.71ms max=260.69ms p(90)=11.39ms p(95)=13.68ms
    http_req_failed................: 0.00%  0 out of 348400
    http_reqs......................: 348400 11187.838394/s

    EXECUTION
    iteration_duration.............: avg=8.59ms min=1.25ms med=7.86ms max=1.04s    p(90)=11.6ms  p(95)=14ms   
    iterations.....................: 348398 11187.77417/s
    vus............................: 0      min=0           max=100
    vus_max........................: 100    min=100         max=100

    NETWORK
    data_received..................: 85 MB  2.7 MB/s
    data_sent......................: 37 MB  1.2 MB/s




running (0m31.1s), 000/100 VUs, 348398 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 100 VUs  30s

==========================================
         200 VU 테스트 시작
==========================================

[9/16] http-json - 200 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/http-json-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 200 max VUs, 1m0s max duration (incl. graceful stop):
              * http_json_test: 200 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:51:35Z" level=info msg="============================================================" source=console
time="2026-01-17T01:51:35Z" level=info msg="Phase 3: HTTP/JSON Test - VUs: 200, Size: 10kb" source=console
time="2026-01-17T01:51:35Z" level=info msg="============================================================" source=console
time="2026-01-17T01:51:35Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:51:35Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/JSON\",\"testName\":\"phase3-200vu-10kb\",\"startTime\":1768614695437}" source=console

running (0m01.0s), 200/200 VUs, 12046 complete and 0 interrupted iterations
http_json_test   [   3% ] 200 VUs  01.0s/30s

running (0m02.0s), 200/200 VUs, 25327 complete and 0 interrupted iterations
http_json_test   [   7% ] 200 VUs  02.0s/30s

running (0m03.0s), 200/200 VUs, 38533 complete and 0 interrupted iterations
http_json_test   [  10% ] 200 VUs  03.0s/30s

running (0m04.0s), 200/200 VUs, 51741 complete and 0 interrupted iterations
http_json_test   [  13% ] 200 VUs  04.0s/30s

running (0m05.0s), 200/200 VUs, 64019 complete and 0 interrupted iterations
http_json_test   [  17% ] 200 VUs  05.0s/30s

running (0m06.0s), 200/200 VUs, 76998 complete and 0 interrupted iterations
http_json_test   [  20% ] 200 VUs  06.0s/30s

running (0m07.0s), 200/200 VUs, 90474 complete and 0 interrupted iterations
http_json_test   [  23% ] 200 VUs  07.0s/30s

running (0m08.0s), 200/200 VUs, 103876 complete and 0 interrupted iterations
http_json_test   [  27% ] 200 VUs  08.0s/30s

running (0m09.0s), 200/200 VUs, 116474 complete and 0 interrupted iterations
http_json_test   [  30% ] 200 VUs  09.0s/30s

running (0m10.0s), 200/200 VUs, 128689 complete and 0 interrupted iterations
http_json_test   [  33% ] 200 VUs  10.0s/30s

running (0m11.0s), 200/200 VUs, 141662 complete and 0 interrupted iterations
http_json_test   [  37% ] 200 VUs  11.0s/30s

running (0m12.0s), 200/200 VUs, 153677 complete and 0 interrupted iterations
http_json_test   [  40% ] 200 VUs  12.0s/30s

running (0m13.0s), 200/200 VUs, 166954 complete and 0 interrupted iterations
http_json_test   [  43% ] 200 VUs  13.0s/30s

running (0m14.0s), 200/200 VUs, 180137 complete and 0 interrupted iterations
http_json_test   [  47% ] 200 VUs  14.0s/30s

running (0m15.0s), 200/200 VUs, 193335 complete and 0 interrupted iterations
http_json_test   [  50% ] 200 VUs  15.0s/30s

running (0m16.0s), 200/200 VUs, 206808 complete and 0 interrupted iterations
http_json_test   [  53% ] 200 VUs  16.0s/30s

running (0m17.0s), 200/200 VUs, 220142 complete and 0 interrupted iterations
http_json_test   [  57% ] 200 VUs  17.0s/30s

running (0m18.0s), 200/200 VUs, 232764 complete and 0 interrupted iterations
http_json_test   [  60% ] 200 VUs  18.0s/30s

running (0m19.0s), 200/200 VUs, 245796 complete and 0 interrupted iterations
http_json_test   [  63% ] 200 VUs  19.0s/30s

running (0m20.0s), 200/200 VUs, 258559 complete and 0 interrupted iterations
http_json_test   [  67% ] 200 VUs  20.0s/30s

running (0m21.0s), 200/200 VUs, 271567 complete and 0 interrupted iterations
http_json_test   [  70% ] 200 VUs  21.0s/30s

running (0m22.0s), 200/200 VUs, 284709 complete and 0 interrupted iterations
http_json_test   [  73% ] 200 VUs  22.0s/30s

running (0m23.0s), 200/200 VUs, 297962 complete and 0 interrupted iterations
http_json_test   [  77% ] 200 VUs  23.0s/30s

running (0m24.0s), 200/200 VUs, 311095 complete and 0 interrupted iterations
http_json_test   [  80% ] 200 VUs  24.0s/30s

running (0m25.0s), 200/200 VUs, 324511 complete and 0 interrupted iterations
http_json_test   [  83% ] 200 VUs  25.0s/30s

running (0m26.0s), 200/200 VUs, 337519 complete and 0 interrupted iterations
http_json_test   [  87% ] 200 VUs  26.0s/30s

running (0m27.0s), 200/200 VUs, 350210 complete and 0 interrupted iterations
http_json_test   [  90% ] 200 VUs  27.0s/30s

running (0m28.0s), 200/200 VUs, 362827 complete and 0 interrupted iterations
http_json_test   [  93% ] 200 VUs  28.0s/30s

running (0m29.0s), 200/200 VUs, 376366 complete and 0 interrupted iterations
http_json_test   [  97% ] 200 VUs  29.0s/30s

running (0m30.0s), 200/200 VUs, 389444 complete and 0 interrupted iterations
http_json_test   [ 100% ] 200 VUs  30.0s/30s

running (0m31.0s), 000/200 VUs, 390023 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 200 VUs  30s
time="2026-01-17T01:52:06Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:52:06Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:52:06Z" level=info msg="============================================================" source=console
time="2026-01-17T01:52:06Z" level=info msg="Protocol: HTTP/JSON" source=console
time="2026-01-17T01:52:06Z" level=info msg="Test Name: phase3-200vu-10kb" source=console
time="2026-01-17T01:52:06Z" level=info msg="Duration: 31020ms" source=console
time="2026-01-17T01:52:06Z" level=info msg="Total Requests: 390023" source=console
time="2026-01-17T01:52:06Z" level=info msg="Throughput: 12573.28 req/s" source=console
time="2026-01-17T01:52:06Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:52:06Z" level=info msg="  Average: 10.36ms" source=console
time="2026-01-17T01:52:06Z" level=info msg="  P50: 9.53ms" source=console
time="2026-01-17T01:52:06Z" level=info msg="  P95: 18.48ms" source=console
time="2026-01-17T01:52:06Z" level=info msg="  P99: 29.35ms" source=console
time="2026-01-17T01:52:06Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:52:06Z" level=info msg="  Peak Heap: 1232.39MB" source=console
time="2026-01-17T01:52:06Z" level=info msg="  GC Count: 25" source=console
time="2026-01-17T01:52:06Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=24.36ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 780046  25034.678196/s
    checks_succeeded...: 100.00% 780046 out of 780046
    checks_failed......: 0.00%   0 out of 780046

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=15.29ms min=1.76ms med=14.26ms max=150.11ms p(90)=21.03ms p(95)=24.36ms
      { expected_response:true }...: avg=15.29ms min=1.76ms med=14.26ms max=150.11ms p(90)=21.03ms p(95)=24.36ms
    http_req_failed................: 0.00%  0 out of 390025
    http_reqs......................: 390025 12517.403286/s

    EXECUTION
    iteration_duration.............: avg=15.37ms min=1.91ms med=14.34ms max=150.18ms p(90)=21.12ms p(95)=24.46ms
    iterations.....................: 390023 12517.339098/s
    vus............................: 0      min=0           max=200
    vus_max........................: 200    min=200         max=200

    NETWORK
    data_received..................: 94 MB  3.0 MB/s
    data_sent......................: 40 MB  1.3 MB/s




running (0m31.2s), 000/200 VUs, 390023 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 200 VUs  30s

[10/16] http-binary - 200 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/http-binary-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 200 max VUs, 1m0s max duration (incl. graceful stop):
              * http_binary_test: 200 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:52:09Z" level=info msg="============================================================" source=console
time="2026-01-17T01:52:09Z" level=info msg="Phase 3: HTTP/Binary Test - VUs: 200, Size: 10kb" source=console
time="2026-01-17T01:52:09Z" level=info msg="============================================================" source=console
time="2026-01-17T01:52:09Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:52:09Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/Binary\",\"testName\":\"phase3-200vu-10kb\",\"startTime\":1768614729849}" source=console

running (0m01.0s), 200/200 VUs, 14251 complete and 0 interrupted iterations
http_binary_test   [   3% ] 200 VUs  01.0s/30s

running (0m02.0s), 200/200 VUs, 29109 complete and 0 interrupted iterations
http_binary_test   [   7% ] 200 VUs  02.0s/30s

running (0m03.0s), 200/200 VUs, 44793 complete and 0 interrupted iterations
http_binary_test   [  10% ] 200 VUs  03.0s/30s

running (0m04.0s), 200/200 VUs, 60055 complete and 0 interrupted iterations
http_binary_test   [  13% ] 200 VUs  04.0s/30s

running (0m05.0s), 200/200 VUs, 75739 complete and 0 interrupted iterations
http_binary_test   [  17% ] 200 VUs  05.0s/30s

running (0m06.0s), 200/200 VUs, 90917 complete and 0 interrupted iterations
http_binary_test   [  20% ] 200 VUs  06.0s/30s

running (0m07.0s), 200/200 VUs, 106128 complete and 0 interrupted iterations
http_binary_test   [  23% ] 200 VUs  07.0s/30s

running (0m08.0s), 200/200 VUs, 121342 complete and 0 interrupted iterations
http_binary_test   [  27% ] 200 VUs  08.0s/30s

running (0m09.0s), 200/200 VUs, 136820 complete and 0 interrupted iterations
http_binary_test   [  30% ] 200 VUs  09.0s/30s

running (0m10.0s), 200/200 VUs, 151391 complete and 0 interrupted iterations
http_binary_test   [  33% ] 200 VUs  10.0s/30s

running (0m11.0s), 200/200 VUs, 166288 complete and 0 interrupted iterations
http_binary_test   [  37% ] 200 VUs  11.0s/30s

running (0m12.0s), 200/200 VUs, 181867 complete and 0 interrupted iterations
http_binary_test   [  40% ] 200 VUs  12.0s/30s

running (0m13.0s), 200/200 VUs, 195764 complete and 0 interrupted iterations
http_binary_test   [  43% ] 200 VUs  13.0s/30s

running (0m14.0s), 200/200 VUs, 210516 complete and 0 interrupted iterations
http_binary_test   [  47% ] 200 VUs  14.0s/30s

running (0m15.0s), 200/200 VUs, 225110 complete and 0 interrupted iterations
http_binary_test   [  50% ] 200 VUs  15.0s/30s

running (0m16.0s), 200/200 VUs, 239849 complete and 0 interrupted iterations
http_binary_test   [  53% ] 200 VUs  16.0s/30s

running (0m17.0s), 200/200 VUs, 253901 complete and 0 interrupted iterations
http_binary_test   [  57% ] 200 VUs  17.0s/30s

running (0m18.0s), 200/200 VUs, 268778 complete and 0 interrupted iterations
http_binary_test   [  60% ] 200 VUs  18.0s/30s

running (0m19.0s), 200/200 VUs, 283435 complete and 0 interrupted iterations
http_binary_test   [  63% ] 200 VUs  19.0s/30s

running (0m20.0s), 200/200 VUs, 298295 complete and 0 interrupted iterations
http_binary_test   [  67% ] 200 VUs  20.0s/30s

running (0m21.0s), 200/200 VUs, 312656 complete and 0 interrupted iterations
http_binary_test   [  70% ] 200 VUs  21.0s/30s

running (0m22.0s), 200/200 VUs, 327491 complete and 0 interrupted iterations
http_binary_test   [  73% ] 200 VUs  22.0s/30s

running (0m23.0s), 200/200 VUs, 342066 complete and 0 interrupted iterations
http_binary_test   [  77% ] 200 VUs  23.0s/30s

running (0m24.0s), 200/200 VUs, 355887 complete and 0 interrupted iterations
http_binary_test   [  80% ] 200 VUs  24.0s/30s

running (0m25.0s), 200/200 VUs, 369221 complete and 0 interrupted iterations
http_binary_test   [  83% ] 200 VUs  25.0s/30s

running (0m26.0s), 200/200 VUs, 383605 complete and 0 interrupted iterations
http_binary_test   [  87% ] 200 VUs  26.0s/30s

running (0m27.0s), 200/200 VUs, 397135 complete and 0 interrupted iterations
http_binary_test   [  90% ] 200 VUs  27.0s/30s

running (0m28.0s), 200/200 VUs, 410946 complete and 0 interrupted iterations
http_binary_test   [  93% ] 200 VUs  28.0s/30s

running (0m29.0s), 200/200 VUs, 424631 complete and 0 interrupted iterations
http_binary_test   [  97% ] 200 VUs  29.0s/30s

running (0m30.0s), 200/200 VUs, 438195 complete and 0 interrupted iterations
http_binary_test   [ 100% ] 200 VUs  30.0s/30s

running (0m31.0s), 000/200 VUs, 438765 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 200 VUs  30s
time="2026-01-17T01:52:41Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:52:41Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:52:41Z" level=info msg="============================================================" source=console
time="2026-01-17T01:52:41Z" level=info msg="Protocol: HTTP/Binary" source=console
time="2026-01-17T01:52:41Z" level=info msg="Test Name: phase3-200vu-10kb" source=console
time="2026-01-17T01:52:41Z" level=info msg="Duration: 31015ms" source=console
time="2026-01-17T01:52:41Z" level=info msg="Total Requests: 438765" source=console
time="2026-01-17T01:52:41Z" level=info msg="Throughput: 14146.86 req/s" source=console
time="2026-01-17T01:52:41Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:52:41Z" level=info msg="  Average: 8.36ms" source=console
time="2026-01-17T01:52:41Z" level=info msg="  P50: 7.67ms" source=console
time="2026-01-17T01:52:41Z" level=info msg="  P95: 15.01ms" source=console
time="2026-01-17T01:52:41Z" level=info msg="  P99: 23.72ms" source=console
time="2026-01-17T01:52:41Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:52:41Z" level=info msg="  Peak Heap: 1444.84MB" source=console
time="2026-01-17T01:52:41Z" level=info msg="  GC Count: 27" source=console
time="2026-01-17T01:52:41Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=21.52ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 877530  28160.156407/s
    checks_succeeded...: 100.00% 877530 out of 877530
    checks_failed......: 0.00%   0 out of 877530

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=13.57ms min=1.49ms med=12.62ms max=137.16ms p(90)=18.49ms p(95)=21.52ms
      { expected_response:true }...: avg=13.57ms min=1.49ms med=12.62ms max=137.16ms p(90)=18.49ms p(95)=21.52ms
    http_req_failed................: 0.00%  0 out of 438767
    http_reqs......................: 438767 14080.142384/s

    EXECUTION
    iteration_duration.............: avg=13.66ms min=1.58ms med=12.69ms max=133.09ms p(90)=18.59ms p(95)=21.68ms
    iterations.....................: 438765 14080.078204/s
    vus............................: 0      min=0           max=200
    vus_max........................: 200    min=200         max=200

    NETWORK
    data_received..................: 107 MB 3.4 MB/s
    data_sent......................: 46 MB  1.5 MB/s




running (0m31.2s), 000/200 VUs, 438765 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 200 VUs  30s

[11/16] grpc-unary - 200 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/grpc-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 200 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_test: 200 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:52:44Z" level=info msg="============================================================" source=console
time="2026-01-17T01:52:44Z" level=info msg="Phase 3: gRPC/Unary Test - VUs: 200, Size: 10kb" source=console
time="2026-01-17T01:52:44Z" level=info msg="============================================================" source=console
time="2026-01-17T01:52:44Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:52:44Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Unary\",\"testName\":\"phase3-200vu-10kb\",\"startTime\":1768614764295}" source=console

running (0m01.0s), 200/200 VUs, 14411 complete and 0 interrupted iterations
grpc_test   [   3% ] 200 VUs  01.0s/30s

running (0m02.0s), 200/200 VUs, 30155 complete and 0 interrupted iterations
grpc_test   [   7% ] 200 VUs  02.0s/30s

running (0m03.0s), 200/200 VUs, 46551 complete and 0 interrupted iterations
grpc_test   [  10% ] 200 VUs  03.0s/30s

running (0m04.0s), 200/200 VUs, 62995 complete and 0 interrupted iterations
grpc_test   [  13% ] 200 VUs  04.0s/30s

running (0m05.0s), 200/200 VUs, 79023 complete and 0 interrupted iterations
grpc_test   [  17% ] 200 VUs  05.0s/30s

running (0m06.0s), 200/200 VUs, 95027 complete and 0 interrupted iterations
grpc_test   [  20% ] 200 VUs  06.0s/30s

running (0m07.0s), 200/200 VUs, 111707 complete and 0 interrupted iterations
grpc_test   [  23% ] 200 VUs  07.0s/30s

running (0m08.0s), 200/200 VUs, 127875 complete and 0 interrupted iterations
grpc_test   [  27% ] 200 VUs  08.0s/30s

running (0m09.0s), 200/200 VUs, 143933 complete and 0 interrupted iterations
grpc_test   [  30% ] 200 VUs  09.0s/30s

running (0m10.0s), 200/200 VUs, 159521 complete and 0 interrupted iterations
grpc_test   [  33% ] 200 VUs  10.0s/30s

running (0m11.0s), 200/200 VUs, 176224 complete and 0 interrupted iterations
grpc_test   [  37% ] 200 VUs  11.0s/30s

running (0m12.0s), 200/200 VUs, 192726 complete and 0 interrupted iterations
grpc_test   [  40% ] 200 VUs  12.0s/30s

running (0m13.0s), 200/200 VUs, 208876 complete and 0 interrupted iterations
grpc_test   [  43% ] 200 VUs  13.0s/30s

running (0m14.0s), 200/200 VUs, 225015 complete and 0 interrupted iterations
grpc_test   [  47% ] 200 VUs  14.0s/30s

running (0m15.0s), 200/200 VUs, 240817 complete and 0 interrupted iterations
grpc_test   [  50% ] 200 VUs  15.0s/30s

running (0m16.0s), 200/200 VUs, 257097 complete and 0 interrupted iterations
grpc_test   [  53% ] 200 VUs  16.0s/30s

running (0m17.0s), 200/200 VUs, 273244 complete and 0 interrupted iterations
grpc_test   [  57% ] 200 VUs  17.0s/30s

running (0m18.0s), 200/200 VUs, 288989 complete and 0 interrupted iterations
grpc_test   [  60% ] 200 VUs  18.0s/30s

running (0m19.0s), 200/200 VUs, 305624 complete and 0 interrupted iterations
grpc_test   [  63% ] 200 VUs  19.0s/30s

running (0m20.0s), 200/200 VUs, 322275 complete and 0 interrupted iterations
grpc_test   [  67% ] 200 VUs  20.0s/30s

running (0m21.0s), 200/200 VUs, 337731 complete and 0 interrupted iterations
grpc_test   [  70% ] 200 VUs  21.0s/30s

running (0m22.0s), 200/200 VUs, 354990 complete and 0 interrupted iterations
grpc_test   [  73% ] 200 VUs  22.0s/30s

running (0m23.0s), 200/200 VUs, 368741 complete and 0 interrupted iterations
grpc_test   [  77% ] 200 VUs  23.0s/30s

running (0m24.0s), 200/200 VUs, 383856 complete and 0 interrupted iterations
grpc_test   [  80% ] 200 VUs  24.0s/30s

running (0m25.0s), 200/200 VUs, 401326 complete and 0 interrupted iterations
grpc_test   [  83% ] 200 VUs  25.0s/30s

running (0m26.0s), 200/200 VUs, 415385 complete and 0 interrupted iterations
grpc_test   [  87% ] 200 VUs  26.0s/30s

running (0m27.0s), 200/200 VUs, 431233 complete and 0 interrupted iterations
grpc_test   [  90% ] 200 VUs  27.0s/30s

running (0m28.0s), 200/200 VUs, 446352 complete and 0 interrupted iterations
grpc_test   [  93% ] 200 VUs  28.0s/30s

running (0m29.0s), 200/200 VUs, 461215 complete and 0 interrupted iterations
grpc_test   [  97% ] 200 VUs  29.0s/30s

running (0m30.0s), 200/200 VUs, 476596 complete and 0 interrupted iterations
grpc_test   [ 100% ] 200 VUs  30.0s/30s

running (0m31.0s), 000/200 VUs, 477246 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 200 VUs  30s
time="2026-01-17T01:53:15Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:53:15Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:53:15Z" level=info msg="============================================================" source=console
time="2026-01-17T01:53:15Z" level=info msg="Protocol: gRPC/Unary" source=console
time="2026-01-17T01:53:15Z" level=info msg="Test Name: phase3-200vu-10kb" source=console
time="2026-01-17T01:53:15Z" level=info msg="Duration: 31015ms" source=console
time="2026-01-17T01:53:15Z" level=info msg="Total Requests: 477246" source=console
time="2026-01-17T01:53:15Z" level=info msg="Throughput: 15387.59 req/s" source=console
time="2026-01-17T01:53:15Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:53:15Z" level=info msg="  Average: 9.95ms" source=console
time="2026-01-17T01:53:15Z" level=info msg="  P50: 9.65ms" source=console
time="2026-01-17T01:53:15Z" level=info msg="  P95: 15.22ms" source=console
time="2026-01-17T01:53:15Z" level=info msg="  P99: 19.82ms" source=console
time="2026-01-17T01:53:15Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:53:15Z" level=info msg="  Peak Heap: 1457.77MB" source=console
time="2026-01-17T01:53:15Z" level=info msg="  GC Count: 18" source=console
time="2026-01-17T01:53:15Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=20.38ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 954492  30609.680038/s
    checks_succeeded...: 100.00% 954492 out of 954492
    checks_failed......: 0.00%   0 out of 954492

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=12.41ms min=901.44µs med=11.42ms max=156.59ms p(90)=16.9ms  p(95)=20.38ms
      { expected_response:true }...: avg=12.41ms min=901.44µs med=11.42ms max=156.59ms p(90)=16.9ms  p(95)=20.38ms
    http_req_failed................: 0.00%  0 out of 477248
    http_reqs......................: 477248 15304.904157/s

    EXECUTION
    iteration_duration.............: avg=12.56ms min=1ms      med=11.53ms max=85.68ms  p(90)=17.08ms p(95)=20.7ms 
    iterations.....................: 477246 15304.840019/s
    vus............................: 0      min=0           max=200
    vus_max........................: 200    min=200         max=200

    NETWORK
    data_received..................: 116 MB 3.7 MB/s
    data_sent......................: 47 MB  1.5 MB/s




running (0m31.2s), 000/200 VUs, 477246 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 200 VUs  30s

[12/16] grpc-stream - 200 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/grpc-stream-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 200 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_stream_test: 200 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:53:18Z" level=info msg="============================================================" source=console
time="2026-01-17T01:53:18Z" level=info msg="Phase 3: gRPC/Stream Test - VUs: 200, Size: 10kb" source=console
time="2026-01-17T01:53:18Z" level=info msg="============================================================" source=console
time="2026-01-17T01:53:18Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:53:18Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Stream\",\"testName\":\"phase3-200vu-10kb\",\"startTime\":1768614798808}" source=console

running (0m01.0s), 200/200 VUs, 11790 complete and 0 interrupted iterations
grpc_stream_test   [   3% ] 200 VUs  01.0s/30s

running (0m02.0s), 200/200 VUs, 24612 complete and 0 interrupted iterations
grpc_stream_test   [   7% ] 200 VUs  02.0s/30s

running (0m03.0s), 200/200 VUs, 37025 complete and 0 interrupted iterations
grpc_stream_test   [  10% ] 200 VUs  03.0s/30s

running (0m04.0s), 200/200 VUs, 49873 complete and 0 interrupted iterations
grpc_stream_test   [  13% ] 200 VUs  04.0s/30s

running (0m05.0s), 200/200 VUs, 61310 complete and 0 interrupted iterations
grpc_stream_test   [  17% ] 200 VUs  05.0s/30s

running (0m06.0s), 200/200 VUs, 74778 complete and 0 interrupted iterations
grpc_stream_test   [  20% ] 200 VUs  06.0s/30s

running (0m07.0s), 200/200 VUs, 86804 complete and 0 interrupted iterations
grpc_stream_test   [  23% ] 200 VUs  07.0s/30s

running (0m08.0s), 200/200 VUs, 99846 complete and 0 interrupted iterations
grpc_stream_test   [  27% ] 200 VUs  08.0s/30s

running (0m09.0s), 200/200 VUs, 111520 complete and 0 interrupted iterations
grpc_stream_test   [  30% ] 200 VUs  09.0s/30s

running (0m10.0s), 200/200 VUs, 124042 complete and 0 interrupted iterations
grpc_stream_test   [  33% ] 200 VUs  10.0s/30s

running (0m11.0s), 200/200 VUs, 137394 complete and 0 interrupted iterations
grpc_stream_test   [  37% ] 200 VUs  11.0s/30s

running (0m12.0s), 200/200 VUs, 150427 complete and 0 interrupted iterations
grpc_stream_test   [  40% ] 200 VUs  12.0s/30s

running (0m13.0s), 200/200 VUs, 162823 complete and 0 interrupted iterations
grpc_stream_test   [  43% ] 200 VUs  13.0s/30s

running (0m14.0s), 200/200 VUs, 174985 complete and 0 interrupted iterations
grpc_stream_test   [  47% ] 200 VUs  14.0s/30s

running (0m15.0s), 200/200 VUs, 188382 complete and 0 interrupted iterations
grpc_stream_test   [  50% ] 200 VUs  15.0s/30s

running (0m16.0s), 200/200 VUs, 200939 complete and 0 interrupted iterations
grpc_stream_test   [  53% ] 200 VUs  16.0s/30s

running (0m17.0s), 200/200 VUs, 213272 complete and 0 interrupted iterations
grpc_stream_test   [  57% ] 200 VUs  17.0s/30s

running (0m18.0s), 200/200 VUs, 226431 complete and 0 interrupted iterations
grpc_stream_test   [  60% ] 200 VUs  18.0s/30s

running (0m19.0s), 200/200 VUs, 239910 complete and 0 interrupted iterations
grpc_stream_test   [  63% ] 200 VUs  19.0s/30s

running (0m20.0s), 200/200 VUs, 252322 complete and 0 interrupted iterations
grpc_stream_test   [  67% ] 200 VUs  20.0s/30s

running (0m21.0s), 200/200 VUs, 264515 complete and 0 interrupted iterations
grpc_stream_test   [  70% ] 200 VUs  21.0s/30s

running (0m22.0s), 200/200 VUs, 276774 complete and 0 interrupted iterations
grpc_stream_test   [  73% ] 200 VUs  22.0s/30s

running (0m23.0s), 200/200 VUs, 289587 complete and 0 interrupted iterations
grpc_stream_test   [  77% ] 200 VUs  23.0s/30s

running (0m24.0s), 200/200 VUs, 303589 complete and 0 interrupted iterations
grpc_stream_test   [  80% ] 200 VUs  24.0s/30s

running (0m25.0s), 200/200 VUs, 316933 complete and 0 interrupted iterations
grpc_stream_test   [  83% ] 200 VUs  25.0s/30s

running (0m26.0s), 200/200 VUs, 330180 complete and 0 interrupted iterations
grpc_stream_test   [  87% ] 200 VUs  26.0s/30s

running (0m27.0s), 200/200 VUs, 343344 complete and 0 interrupted iterations
grpc_stream_test   [  90% ] 200 VUs  27.0s/30s

running (0m28.0s), 200/200 VUs, 357279 complete and 0 interrupted iterations
grpc_stream_test   [  93% ] 200 VUs  28.0s/30s

running (0m29.0s), 200/200 VUs, 368630 complete and 0 interrupted iterations
grpc_stream_test   [  97% ] 200 VUs  29.0s/30s

running (0m30.0s), 200/200 VUs, 382181 complete and 0 interrupted iterations
grpc_stream_test   [ 100% ] 200 VUs  30.0s/30s

running (0m31.0s), 000/200 VUs, 382715 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 200 VUs  30s
time="2026-01-17T01:53:49Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:53:49Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:53:49Z" level=info msg="============================================================" source=console
time="2026-01-17T01:53:49Z" level=info msg="Protocol: gRPC/Stream" source=console
time="2026-01-17T01:53:49Z" level=info msg="Test Name: phase3-200vu-10kb" source=console
time="2026-01-17T01:53:49Z" level=info msg="Duration: 31017ms" source=console
time="2026-01-17T01:53:49Z" level=info msg="Total Requests: 382715" source=console
time="2026-01-17T01:53:49Z" level=info msg="Throughput: 12338.88 req/s" source=console
time="2026-01-17T01:53:49Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:53:49Z" level=info msg="  Average: 11.32ms" source=console
time="2026-01-17T01:53:49Z" level=info msg="  P50: 11.39ms" source=console
time="2026-01-17T01:53:49Z" level=info msg="  P95: 17.52ms" source=console
time="2026-01-17T01:53:49Z" level=info msg="  P99: 22.26ms" source=console
time="2026-01-17T01:53:49Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:53:49Z" level=info msg="  Peak Heap: 1466.54MB" source=console
time="2026-01-17T01:53:49Z" level=info msg="  GC Count: 15" source=console
time="2026-01-17T01:53:49Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=26.31ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 765430  24572.437838/s
    checks_succeeded...: 100.00% 765430 out of 765430
    checks_failed......: 0.00%   0 out of 765430

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=15.42ms min=1.15ms med=14.24ms max=121.87ms p(90)=20.98ms p(95)=26.31ms
      { expected_response:true }...: avg=15.42ms min=1.15ms med=14.24ms max=121.87ms p(90)=20.98ms p(95)=26.31ms
    http_req_failed................: 0.00%  0 out of 382717
    http_reqs......................: 382717 12286.283125/s

    EXECUTION
    iteration_duration.............: avg=15.66ms min=1.38ms med=14.41ms max=107.23ms p(90)=21.33ms p(95)=26.92ms
    iterations.....................: 382715 12286.218919/s
    vus............................: 0      min=0           max=200
    vus_max........................: 200    min=200         max=200

    NETWORK
    data_received..................: 93 MB  3.0 MB/s
    data_sent......................: 40 MB  1.3 MB/s




running (0m31.1s), 000/200 VUs, 382715 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 200 VUs  30s

==========================================
         500 VU 테스트 시작
==========================================

[13/16] http-json - 500 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/http-json-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 500 max VUs, 1m0s max duration (incl. graceful stop):
              * http_json_test: 500 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:53:53Z" level=info msg="============================================================" source=console
time="2026-01-17T01:53:53Z" level=info msg="Phase 3: HTTP/JSON Test - VUs: 500, Size: 10kb" source=console
time="2026-01-17T01:53:53Z" level=info msg="============================================================" source=console
time="2026-01-17T01:53:53Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:53:53Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/JSON\",\"testName\":\"phase3-500vu-10kb\",\"startTime\":1768614833232}" source=console

running (0m01.0s), 500/500 VUs, 12330 complete and 0 interrupted iterations
http_json_test   [   3% ] 500 VUs  01.0s/30s

running (0m02.0s), 500/500 VUs, 25798 complete and 0 interrupted iterations
http_json_test   [   7% ] 500 VUs  02.0s/30s

running (0m03.0s), 500/500 VUs, 38944 complete and 0 interrupted iterations
http_json_test   [  10% ] 500 VUs  03.0s/30s

running (0m04.0s), 500/500 VUs, 52032 complete and 0 interrupted iterations
http_json_test   [  13% ] 500 VUs  04.0s/30s

running (0m05.0s), 500/500 VUs, 65041 complete and 0 interrupted iterations
http_json_test   [  17% ] 500 VUs  05.0s/30s

running (0m06.0s), 500/500 VUs, 78199 complete and 0 interrupted iterations
http_json_test   [  20% ] 500 VUs  06.0s/30s

running (0m07.0s), 500/500 VUs, 92013 complete and 0 interrupted iterations
http_json_test   [  23% ] 500 VUs  07.0s/30s

running (0m08.0s), 500/500 VUs, 105584 complete and 0 interrupted iterations
http_json_test   [  27% ] 500 VUs  08.0s/30s

running (0m09.0s), 500/500 VUs, 119274 complete and 0 interrupted iterations
http_json_test   [  30% ] 500 VUs  09.0s/30s

running (0m10.0s), 500/500 VUs, 132785 complete and 0 interrupted iterations
http_json_test   [  33% ] 500 VUs  10.0s/30s

running (0m11.0s), 500/500 VUs, 146460 complete and 0 interrupted iterations
http_json_test   [  37% ] 500 VUs  11.0s/30s

running (0m12.0s), 500/500 VUs, 159879 complete and 0 interrupted iterations
http_json_test   [  40% ] 500 VUs  12.0s/30s

running (0m13.0s), 500/500 VUs, 172783 complete and 0 interrupted iterations
http_json_test   [  43% ] 500 VUs  13.0s/30s

running (0m14.0s), 500/500 VUs, 186656 complete and 0 interrupted iterations
http_json_test   [  47% ] 500 VUs  14.0s/30s

running (0m15.0s), 500/500 VUs, 200042 complete and 0 interrupted iterations
http_json_test   [  50% ] 500 VUs  15.0s/30s

running (0m16.0s), 500/500 VUs, 213085 complete and 0 interrupted iterations
http_json_test   [  53% ] 500 VUs  16.0s/30s

running (0m17.0s), 500/500 VUs, 226139 complete and 0 interrupted iterations
http_json_test   [  57% ] 500 VUs  17.0s/30s

running (0m18.0s), 500/500 VUs, 239722 complete and 0 interrupted iterations
http_json_test   [  60% ] 500 VUs  18.0s/30s

running (0m19.0s), 500/500 VUs, 252851 complete and 0 interrupted iterations
http_json_test   [  63% ] 500 VUs  19.0s/30s

running (0m20.0s), 500/500 VUs, 265921 complete and 0 interrupted iterations
http_json_test   [  67% ] 500 VUs  20.0s/30s

running (0m21.0s), 500/500 VUs, 279022 complete and 0 interrupted iterations
http_json_test   [  70% ] 500 VUs  21.0s/30s

running (0m22.0s), 500/500 VUs, 292255 complete and 0 interrupted iterations
http_json_test   [  73% ] 500 VUs  22.0s/30s

running (0m23.0s), 500/500 VUs, 305088 complete and 0 interrupted iterations
http_json_test   [  77% ] 500 VUs  23.0s/30s

running (0m24.0s), 500/500 VUs, 317832 complete and 0 interrupted iterations
http_json_test   [  80% ] 500 VUs  24.0s/30s

running (0m25.0s), 500/500 VUs, 330227 complete and 0 interrupted iterations
http_json_test   [  83% ] 500 VUs  25.0s/30s

running (0m26.0s), 500/500 VUs, 341934 complete and 0 interrupted iterations
http_json_test   [  87% ] 500 VUs  26.0s/30s

running (0m27.0s), 500/500 VUs, 353643 complete and 0 interrupted iterations
http_json_test   [  90% ] 500 VUs  27.0s/30s

running (0m28.0s), 500/500 VUs, 365843 complete and 0 interrupted iterations
http_json_test   [  93% ] 500 VUs  28.0s/30s

running (0m29.0s), 500/500 VUs, 377351 complete and 0 interrupted iterations
http_json_test   [  97% ] 500 VUs  29.0s/30s

running (0m30.0s), 500/500 VUs, 389415 complete and 0 interrupted iterations
http_json_test   [ 100% ] 500 VUs  30.0s/30s

running (0m31.0s), 000/500 VUs, 390015 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 500 VUs  30s
time="2026-01-17T01:54:24Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:54:24Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:54:24Z" level=info msg="============================================================" source=console
time="2026-01-17T01:54:24Z" level=info msg="Protocol: HTTP/JSON" source=console
time="2026-01-17T01:54:24Z" level=info msg="Test Name: phase3-500vu-10kb" source=console
time="2026-01-17T01:54:24Z" level=info msg="Duration: 31058ms" source=console
time="2026-01-17T01:54:24Z" level=info msg="Total Requests: 390015" source=console
time="2026-01-17T01:54:24Z" level=info msg="Throughput: 12557.63 req/s" source=console
time="2026-01-17T01:54:24Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:54:24Z" level=info msg="  Average: 14.55ms" source=console
time="2026-01-17T01:54:24Z" level=info msg="  P50: 13.24ms" source=console
time="2026-01-17T01:54:24Z" level=info msg="  P95: 25.33ms" source=console
time="2026-01-17T01:54:24Z" level=info msg="  P99: 40.85ms" source=console
time="2026-01-17T01:54:24Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:54:24Z" level=info msg="  Peak Heap: 1483.05MB" source=console
time="2026-01-17T01:54:24Z" level=info msg="  GC Count: 22" source=console
time="2026-01-17T01:54:24Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=52.09ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 780030  24994.677216/s
    checks_succeeded...: 100.00% 780030 out of 780030
    checks_failed......: 0.00%   0 out of 780030

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=38.4ms  min=4.29ms med=36.57ms max=274.84ms p(90)=46.54ms p(95)=52.09ms
      { expected_response:true }...: avg=38.4ms  min=4.29ms med=36.57ms max=274.84ms p(90)=46.54ms p(95)=52.09ms
    http_req_failed................: 0.00%  0 out of 390017
    http_reqs......................: 390017 12497.402694/s

    EXECUTION
    iteration_duration.............: avg=38.47ms min=4.86ms med=36.63ms max=279.45ms p(90)=46.62ms p(95)=52.15ms
    iterations.....................: 390015 12497.338608/s
    vus............................: 0      min=0           max=500
    vus_max........................: 500    min=500         max=500

    NETWORK
    data_received..................: 94 MB  3.0 MB/s
    data_sent......................: 40 MB  1.3 MB/s




running (0m31.2s), 000/500 VUs, 390015 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 500 VUs  30s

[14/16] http-binary - 500 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/http-binary-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 500 max VUs, 1m0s max duration (incl. graceful stop):
              * http_binary_test: 500 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:54:27Z" level=info msg="============================================================" source=console
time="2026-01-17T01:54:27Z" level=info msg="Phase 3: HTTP/Binary Test - VUs: 500, Size: 10kb" source=console
time="2026-01-17T01:54:27Z" level=info msg="============================================================" source=console
time="2026-01-17T01:54:27Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:54:27Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/Binary\",\"testName\":\"phase3-500vu-10kb\",\"startTime\":1768614867719}" source=console

running (0m01.0s), 500/500 VUs, 14000 complete and 0 interrupted iterations
http_binary_test   [   3% ] 500 VUs  01.0s/30s

running (0m02.0s), 500/500 VUs, 29164 complete and 0 interrupted iterations
http_binary_test   [   7% ] 500 VUs  02.0s/30s

running (0m03.0s), 500/500 VUs, 44463 complete and 0 interrupted iterations
http_binary_test   [  10% ] 500 VUs  03.0s/30s

running (0m04.0s), 500/500 VUs, 59943 complete and 0 interrupted iterations
http_binary_test   [  13% ] 500 VUs  04.0s/30s

running (0m05.0s), 500/500 VUs, 75630 complete and 0 interrupted iterations
http_binary_test   [  17% ] 500 VUs  05.0s/30s

running (0m06.0s), 500/500 VUs, 91496 complete and 0 interrupted iterations
http_binary_test   [  20% ] 500 VUs  06.0s/30s

running (0m07.0s), 500/500 VUs, 107642 complete and 0 interrupted iterations
http_binary_test   [  23% ] 500 VUs  07.0s/30s

running (0m08.0s), 500/500 VUs, 121935 complete and 0 interrupted iterations
http_binary_test   [  27% ] 500 VUs  08.0s/30s

running (0m09.0s), 500/500 VUs, 137102 complete and 0 interrupted iterations
http_binary_test   [  30% ] 500 VUs  09.0s/30s

running (0m10.0s), 500/500 VUs, 153401 complete and 0 interrupted iterations
http_binary_test   [  33% ] 500 VUs  10.0s/30s

running (0m11.0s), 500/500 VUs, 169221 complete and 0 interrupted iterations
http_binary_test   [  37% ] 500 VUs  11.0s/30s

running (0m12.0s), 500/500 VUs, 185233 complete and 0 interrupted iterations
http_binary_test   [  40% ] 500 VUs  12.0s/30s

running (0m13.0s), 500/500 VUs, 201238 complete and 0 interrupted iterations
http_binary_test   [  43% ] 500 VUs  13.0s/30s

running (0m14.0s), 500/500 VUs, 216804 complete and 0 interrupted iterations
http_binary_test   [  47% ] 500 VUs  14.0s/30s

running (0m15.0s), 500/500 VUs, 233013 complete and 0 interrupted iterations
http_binary_test   [  50% ] 500 VUs  15.0s/30s

running (0m16.0s), 500/500 VUs, 248686 complete and 0 interrupted iterations
http_binary_test   [  53% ] 500 VUs  16.0s/30s

running (0m17.0s), 500/500 VUs, 263914 complete and 0 interrupted iterations
http_binary_test   [  57% ] 500 VUs  17.0s/30s

running (0m18.0s), 500/500 VUs, 277882 complete and 0 interrupted iterations
http_binary_test   [  60% ] 500 VUs  18.0s/30s

running (0m19.0s), 500/500 VUs, 292216 complete and 0 interrupted iterations
http_binary_test   [  63% ] 500 VUs  19.0s/30s

running (0m20.0s), 500/500 VUs, 305786 complete and 0 interrupted iterations
http_binary_test   [  67% ] 500 VUs  20.0s/30s

running (0m21.0s), 500/500 VUs, 319764 complete and 0 interrupted iterations
http_binary_test   [  70% ] 500 VUs  21.0s/30s

running (0m22.0s), 500/500 VUs, 333871 complete and 0 interrupted iterations
http_binary_test   [  73% ] 500 VUs  22.0s/30s

running (0m23.0s), 500/500 VUs, 348734 complete and 0 interrupted iterations
http_binary_test   [  77% ] 500 VUs  23.0s/30s

running (0m24.0s), 500/500 VUs, 362359 complete and 0 interrupted iterations
http_binary_test   [  80% ] 500 VUs  24.0s/30s

running (0m25.0s), 500/500 VUs, 376126 complete and 0 interrupted iterations
http_binary_test   [  83% ] 500 VUs  25.0s/30s

running (0m26.0s), 500/500 VUs, 389932 complete and 0 interrupted iterations
http_binary_test   [  87% ] 500 VUs  26.0s/30s

running (0m27.0s), 500/500 VUs, 403143 complete and 0 interrupted iterations
http_binary_test   [  90% ] 500 VUs  27.0s/30s

running (0m28.0s), 500/500 VUs, 416743 complete and 0 interrupted iterations
http_binary_test   [  93% ] 500 VUs  28.0s/30s

running (0m29.0s), 500/500 VUs, 429341 complete and 0 interrupted iterations
http_binary_test   [  97% ] 500 VUs  29.0s/30s

running (0m30.0s), 500/500 VUs, 443574 complete and 0 interrupted iterations
http_binary_test   [ 100% ] 500 VUs  30.0s/30s

running (0m31.0s), 000/500 VUs, 444653 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 500 VUs  30s
time="2026-01-17T01:54:58Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:54:58Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:54:58Z" level=info msg="============================================================" source=console
time="2026-01-17T01:54:58Z" level=info msg="Protocol: HTTP/Binary" source=console
time="2026-01-17T01:54:58Z" level=info msg="Test Name: phase3-500vu-10kb" source=console
time="2026-01-17T01:54:58Z" level=info msg="Duration: 31040ms" source=console
time="2026-01-17T01:54:58Z" level=info msg="Total Requests: 444653" source=console
time="2026-01-17T01:54:58Z" level=info msg="Throughput: 14325.16 req/s" source=console
time="2026-01-17T01:54:58Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:54:58Z" level=info msg="  Average: 12.41ms" source=console
time="2026-01-17T01:54:58Z" level=info msg="  P50: 11.30ms" source=console
time="2026-01-17T01:54:58Z" level=info msg="  P95: 21.40ms" source=console
time="2026-01-17T01:54:58Z" level=info msg="  P99: 35.30ms" source=console
time="2026-01-17T01:54:58Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:54:58Z" level=info msg="  Peak Heap: 1509.78MB" source=console
time="2026-01-17T01:54:58Z" level=info msg="  GC Count: 22" source=console
time="2026-01-17T01:54:58Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=47.48ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 889306  28488.671385/s
    checks_succeeded...: 100.00% 889306 out of 889306
    checks_failed......: 0.00%   0 out of 889306

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=33.65ms min=1.46ms med=31.83ms max=279.36ms p(90)=41.52ms p(95)=47.48ms
      { expected_response:true }...: avg=33.65ms min=1.46ms med=31.83ms max=279.36ms p(90)=41.52ms p(95)=47.48ms
    http_req_failed................: 0.00%  0 out of 444655
    http_reqs......................: 444655 14244.399762/s

    EXECUTION
    iteration_duration.............: avg=33.73ms min=1.52ms med=31.89ms max=279.42ms p(90)=41.63ms p(95)=47.66ms
    iterations.....................: 444653 14244.335693/s
    vus............................: 0      min=0           max=500
    vus_max........................: 500    min=500         max=500

    NETWORK
    data_received..................: 108 MB 3.5 MB/s
    data_sent......................: 47 MB  1.5 MB/s




running (0m31.2s), 000/500 VUs, 444653 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 500 VUs  30s

[15/16] grpc-unary - 500 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/grpc-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 500 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_test: 500 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:55:02Z" level=info msg="============================================================" source=console
time="2026-01-17T01:55:02Z" level=info msg="Phase 3: gRPC/Unary Test - VUs: 500, Size: 10kb" source=console
time="2026-01-17T01:55:02Z" level=info msg="============================================================" source=console
time="2026-01-17T01:55:02Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:55:02Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Unary\",\"testName\":\"phase3-500vu-10kb\",\"startTime\":1768614902253}" source=console

running (0m01.0s), 500/500 VUs, 15212 complete and 0 interrupted iterations
grpc_test   [   3% ] 500 VUs  00.9s/30s

running (0m02.0s), 500/500 VUs, 32117 complete and 0 interrupted iterations
grpc_test   [   6% ] 500 VUs  01.9s/30s

running (0m03.0s), 500/500 VUs, 47571 complete and 0 interrupted iterations
grpc_test   [  10% ] 500 VUs  02.9s/30s

running (0m04.0s), 500/500 VUs, 62633 complete and 0 interrupted iterations
grpc_test   [  13% ] 500 VUs  03.9s/30s

running (0m05.0s), 500/500 VUs, 78725 complete and 0 interrupted iterations
grpc_test   [  16% ] 500 VUs  04.9s/30s

running (0m06.0s), 500/500 VUs, 95522 complete and 0 interrupted iterations
grpc_test   [  20% ] 500 VUs  05.9s/30s

running (0m07.0s), 500/500 VUs, 111108 complete and 0 interrupted iterations
grpc_test   [  23% ] 500 VUs  06.9s/30s

running (0m08.0s), 500/500 VUs, 128095 complete and 0 interrupted iterations
grpc_test   [  26% ] 500 VUs  07.9s/30s

running (0m09.0s), 500/500 VUs, 145872 complete and 0 interrupted iterations
grpc_test   [  30% ] 500 VUs  08.9s/30s

running (0m10.0s), 500/500 VUs, 163396 complete and 0 interrupted iterations
grpc_test   [  33% ] 500 VUs  10.0s/30s

running (0m11.0s), 500/500 VUs, 182318 complete and 0 interrupted iterations
grpc_test   [  36% ] 500 VUs  10.9s/30s

running (0m12.0s), 500/500 VUs, 200285 complete and 0 interrupted iterations
grpc_test   [  40% ] 500 VUs  11.9s/30s

running (0m13.0s), 500/500 VUs, 216839 complete and 0 interrupted iterations
grpc_test   [  43% ] 500 VUs  12.9s/30s

running (0m14.0s), 500/500 VUs, 234011 complete and 0 interrupted iterations
grpc_test   [  46% ] 500 VUs  13.9s/30s

running (0m15.0s), 500/500 VUs, 249274 complete and 0 interrupted iterations
grpc_test   [  50% ] 500 VUs  14.9s/30s

running (0m16.0s), 500/500 VUs, 265936 complete and 0 interrupted iterations
grpc_test   [  53% ] 500 VUs  15.9s/30s

running (0m17.0s), 500/500 VUs, 282984 complete and 0 interrupted iterations
grpc_test   [  56% ] 500 VUs  16.9s/30s

running (0m18.0s), 500/500 VUs, 299043 complete and 0 interrupted iterations
grpc_test   [  60% ] 500 VUs  17.9s/30s

running (0m19.0s), 500/500 VUs, 312916 complete and 0 interrupted iterations
grpc_test   [  63% ] 500 VUs  18.9s/30s

running (0m20.0s), 500/500 VUs, 330753 complete and 0 interrupted iterations
grpc_test   [  66% ] 500 VUs  19.9s/30s

running (0m21.0s), 500/500 VUs, 347410 complete and 0 interrupted iterations
grpc_test   [  70% ] 500 VUs  20.9s/30s

running (0m22.0s), 500/500 VUs, 363528 complete and 0 interrupted iterations
grpc_test   [  73% ] 500 VUs  21.9s/30s

running (0m23.0s), 500/500 VUs, 380910 complete and 0 interrupted iterations
grpc_test   [  76% ] 500 VUs  22.9s/30s

running (0m24.0s), 500/500 VUs, 396991 complete and 0 interrupted iterations
grpc_test   [  80% ] 500 VUs  23.9s/30s

running (0m25.0s), 500/500 VUs, 412787 complete and 0 interrupted iterations
grpc_test   [  83% ] 500 VUs  24.9s/30s

running (0m26.0s), 500/500 VUs, 430775 complete and 0 interrupted iterations
grpc_test   [  86% ] 500 VUs  25.9s/30s

running (0m27.0s), 500/500 VUs, 447809 complete and 0 interrupted iterations
grpc_test   [  90% ] 500 VUs  26.9s/30s

running (0m28.0s), 500/500 VUs, 464067 complete and 0 interrupted iterations
grpc_test   [  93% ] 500 VUs  27.9s/30s

running (0m29.0s), 500/500 VUs, 480812 complete and 0 interrupted iterations
grpc_test   [  96% ] 500 VUs  28.9s/30s

running (0m30.0s), 500/500 VUs, 496672 complete and 0 interrupted iterations
grpc_test   [ 100% ] 500 VUs  29.9s/30s

running (0m31.0s), 000/500 VUs, 498033 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 500 VUs  30s
time="2026-01-17T01:55:33Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:55:33Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:55:33Z" level=info msg="============================================================" source=console
time="2026-01-17T01:55:33Z" level=info msg="Protocol: gRPC/Unary" source=console
time="2026-01-17T01:55:33Z" level=info msg="Test Name: phase3-500vu-10kb" source=console
time="2026-01-17T01:55:33Z" level=info msg="Duration: 31026ms" source=console
time="2026-01-17T01:55:33Z" level=info msg="Total Requests: 498033" source=console
time="2026-01-17T01:55:33Z" level=info msg="Throughput: 16052.12 req/s" source=console
time="2026-01-17T01:55:33Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:55:33Z" level=info msg="  Average: 11.22ms" source=console
time="2026-01-17T01:55:33Z" level=info msg="  P50: 10.62ms" source=console
time="2026-01-17T01:55:33Z" level=info msg="  P95: 15.75ms" source=console
time="2026-01-17T01:55:33Z" level=info msg="  P99: 21.84ms" source=console
time="2026-01-17T01:55:33Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:55:33Z" level=info msg="  Peak Heap: 1508.03MB" source=console
time="2026-01-17T01:55:33Z" level=info msg="  GC Count: 20" source=console
time="2026-01-17T01:55:33Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=43.77ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 996066  31915.283856/s
    checks_succeeded...: 100.00% 996066 out of 996066
    checks_failed......: 0.00%   0 out of 996066

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=29.8ms min=1.19ms med=28.41ms max=167.27ms p(90)=38.37ms p(95)=43.77ms
      { expected_response:true }...: avg=29.8ms min=1.19ms med=28.41ms max=167.27ms p(90)=38.37ms p(95)=43.77ms
    http_req_failed................: 0.00%  0 out of 498035
    http_reqs......................: 498035 15957.706011/s

    EXECUTION
    iteration_duration.............: avg=30.1ms min=1.25ms med=28.53ms max=127.63ms p(90)=38.74ms p(95)=44.67ms
    iterations.....................: 498033 15957.641928/s
    vus............................: 0      min=0           max=500
    vus_max........................: 500    min=500         max=500

    NETWORK
    data_received..................: 121 MB 3.9 MB/s
    data_sent......................: 49 MB  1.6 MB/s




running (0m31.2s), 000/500 VUs, 498033 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 500 VUs  30s

[16/16] grpc-stream - 500 VUs 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase3/grpc-stream-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 500 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_stream_test: 500 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T01:55:36Z" level=info msg="============================================================" source=console
time="2026-01-17T01:55:36Z" level=info msg="Phase 3: gRPC/Stream Test - VUs: 500, Size: 10kb" source=console
time="2026-01-17T01:55:36Z" level=info msg="============================================================" source=console
time="2026-01-17T01:55:36Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T01:55:36Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Stream\",\"testName\":\"phase3-500vu-10kb\",\"startTime\":1768614936810}" source=console

running (0m01.0s), 500/500 VUs, 12970 complete and 0 interrupted iterations
grpc_stream_test   [   3% ] 500 VUs  00.9s/30s

running (0m02.0s), 500/500 VUs, 27610 complete and 0 interrupted iterations
grpc_stream_test   [   6% ] 500 VUs  01.9s/30s

running (0m03.0s), 500/500 VUs, 41625 complete and 0 interrupted iterations
grpc_stream_test   [  10% ] 500 VUs  02.9s/30s

running (0m04.0s), 500/500 VUs, 54854 complete and 0 interrupted iterations
grpc_stream_test   [  13% ] 500 VUs  04.0s/30s

running (0m05.0s), 500/500 VUs, 68955 complete and 0 interrupted iterations
grpc_stream_test   [  16% ] 500 VUs  04.9s/30s

running (0m06.0s), 500/500 VUs, 81765 complete and 0 interrupted iterations
grpc_stream_test   [  20% ] 500 VUs  05.9s/30s

running (0m07.0s), 500/500 VUs, 93692 complete and 0 interrupted iterations
grpc_stream_test   [  23% ] 500 VUs  06.9s/30s

running (0m08.0s), 500/500 VUs, 105749 complete and 0 interrupted iterations
grpc_stream_test   [  26% ] 500 VUs  07.9s/30s

running (0m09.0s), 500/500 VUs, 118510 complete and 0 interrupted iterations
grpc_stream_test   [  30% ] 500 VUs  08.9s/30s

running (0m10.0s), 500/500 VUs, 130484 complete and 0 interrupted iterations
grpc_stream_test   [  33% ] 500 VUs  09.9s/30s

running (0m11.0s), 500/500 VUs, 141705 complete and 0 interrupted iterations
grpc_stream_test   [  36% ] 500 VUs  10.9s/30s

running (0m12.0s), 500/500 VUs, 153017 complete and 0 interrupted iterations
grpc_stream_test   [  40% ] 500 VUs  11.9s/30s

running (0m13.0s), 500/500 VUs, 164932 complete and 0 interrupted iterations
grpc_stream_test   [  43% ] 500 VUs  12.9s/30s

running (0m14.0s), 500/500 VUs, 175628 complete and 0 interrupted iterations
grpc_stream_test   [  46% ] 500 VUs  13.9s/30s

running (0m15.0s), 500/500 VUs, 187626 complete and 0 interrupted iterations
grpc_stream_test   [  50% ] 500 VUs  14.9s/30s

running (0m16.0s), 500/500 VUs, 199672 complete and 0 interrupted iterations
grpc_stream_test   [  53% ] 500 VUs  15.9s/30s

running (0m17.0s), 500/500 VUs, 211247 complete and 0 interrupted iterations
grpc_stream_test   [  56% ] 500 VUs  16.9s/30s

running (0m18.0s), 500/500 VUs, 222055 complete and 0 interrupted iterations
grpc_stream_test   [  60% ] 500 VUs  17.9s/30s

running (0m19.0s), 500/500 VUs, 234384 complete and 0 interrupted iterations
grpc_stream_test   [  63% ] 500 VUs  18.9s/30s

running (0m20.0s), 500/500 VUs, 245064 complete and 0 interrupted iterations
grpc_stream_test   [  66% ] 500 VUs  19.9s/30s

running (0m21.0s), 500/500 VUs, 256930 complete and 0 interrupted iterations
grpc_stream_test   [  70% ] 500 VUs  20.9s/30s

running (0m22.0s), 500/500 VUs, 268204 complete and 0 interrupted iterations
grpc_stream_test   [  73% ] 500 VUs  21.9s/30s

running (0m23.0s), 500/500 VUs, 279207 complete and 0 interrupted iterations
grpc_stream_test   [  76% ] 500 VUs  22.9s/30s

running (0m24.0s), 500/500 VUs, 290529 complete and 0 interrupted iterations
grpc_stream_test   [  80% ] 500 VUs  24.0s/30s

running (0m25.0s), 500/500 VUs, 301112 complete and 0 interrupted iterations
grpc_stream_test   [  83% ] 500 VUs  24.9s/30s

running (0m26.0s), 500/500 VUs, 312202 complete and 0 interrupted iterations
grpc_stream_test   [  86% ] 500 VUs  25.9s/30s

running (0m27.0s), 500/500 VUs, 323343 complete and 0 interrupted iterations
grpc_stream_test   [  90% ] 500 VUs  26.9s/30s

running (0m28.0s), 500/500 VUs, 334472 complete and 0 interrupted iterations
grpc_stream_test   [  93% ] 500 VUs  27.9s/30s

running (0m29.0s), 500/500 VUs, 345921 complete and 0 interrupted iterations
grpc_stream_test   [  96% ] 500 VUs  28.9s/30s

running (0m30.0s), 500/500 VUs, 357631 complete and 0 interrupted iterations
grpc_stream_test   [ 100% ] 500 VUs  29.9s/30s

running (0m31.0s), 000/500 VUs, 358730 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 500 VUs  30s
time="2026-01-17T01:56:08Z" level=info msg="\n============================================================" source=console
time="2026-01-17T01:56:08Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T01:56:08Z" level=info msg="============================================================" source=console
time="2026-01-17T01:56:08Z" level=info msg="Protocol: gRPC/Stream" source=console
time="2026-01-17T01:56:08Z" level=info msg="Test Name: phase3-500vu-10kb" source=console
time="2026-01-17T01:56:08Z" level=info msg="Duration: 31042ms" source=console
time="2026-01-17T01:56:08Z" level=info msg="Total Requests: 358730" source=console
time="2026-01-17T01:56:08Z" level=info msg="Throughput: 11556.28 req/s" source=console
time="2026-01-17T01:56:08Z" level=info msg="\nLatency:" source=console
time="2026-01-17T01:56:08Z" level=info msg="  Average: 15.56ms" source=console
time="2026-01-17T01:56:08Z" level=info msg="  P50: 15.11ms" source=console
time="2026-01-17T01:56:08Z" level=info msg="  P95: 22.15ms" source=console
time="2026-01-17T01:56:08Z" level=info msg="  P99: 28.46ms" source=console
time="2026-01-17T01:56:08Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T01:56:08Z" level=info msg="  Peak Heap: 1522.96MB" source=console
time="2026-01-17T01:56:08Z" level=info msg="  GC Count: 14" source=console
time="2026-01-17T01:56:08Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=61.17ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 717460  22989.795053/s
    checks_succeeded...: 100.00% 717460 out of 717460
    checks_failed......: 0.00%   0 out of 717460

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=41.49ms min=1.77ms med=40.54ms max=146.63ms p(90)=52.06ms p(95)=61.17ms
      { expected_response:true }...: avg=41.49ms min=1.77ms med=40.54ms max=146.63ms p(90)=52.06ms p(95)=61.17ms
    http_req_failed................: 0.00%  0 out of 358732
    http_reqs......................: 358732 11494.961613/s

    EXECUTION
    iteration_duration.............: avg=41.78ms min=4.14ms med=40.72ms max=141.32ms p(90)=52.41ms p(95)=61.95ms
    iterations.....................: 358730 11494.897526/s
    vus............................: 0      min=0           max=500
    vus_max........................: 500    min=500         max=500

    NETWORK
    data_received..................: 88 MB  2.8 MB/s
    data_sent......................: 38 MB  1.2 MB/s




running (0m31.2s), 000/500 VUs, 358730 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 500 VUs  30s

================================
✅ Phase 3 테스트 완료!
결과 파일: /home/jun/distributed-log-pipeline/proto-bench/scripts/../results/phase3/*_20260117_014648.log
================================
root@jun:/home/jun/distributed-log-pipeline/proto-bench/scripts# 
```