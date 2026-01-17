# Phase 2: 소용량 페이로드 (1KB, 10KB) 성능 비교 결과

## 개요

| 항목 | 값 |
|------|-----|
| 페이로드 크기 | 1KB, 10KB |
| 동시 사용자 (VU) | 10 |
| 테스트 시간 | 30초 |

## 가설

> "작은 페이로드에서는 HTTP 헤더 오버헤드(~400B)가 상대적으로 커지므로  
> gRPC의 바이너리 효율이 발휘되어 gRPC가 HTTP보다 빠를 것이다"

---

## 결과: 1KB 페이로드

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 순위 |
|----------|-------------------|-------------|-------------|------|
| **gRPC/Unary** | **5,876.83** | **0.67ms** | 1.19ms | 🥇 |
| gRPC/Stream | 5,822.50 | 0.65ms | 1.17ms | 🥈 |
| HTTP/Binary | 3,695.04 | 1.24ms | 1.88ms | 🥉 |
| HTTP/JSON | 3,428.50 | 1.42ms | 2.14ms | 4위 |

### 분석

**gRPC가 HTTP보다 약 1.6배 빠르다!**

- gRPC/Unary: 5,877 req/s
- HTTP/Binary: 3,695 req/s
- **차이: +59%**

---

## 결과: 10KB 페이로드

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 순위 |
|----------|-------------------|-------------|-------------|------|
| **gRPC/Unary** | **5,748.21** | **0.66ms** | 1.16ms | 🥇 |
| gRPC/Stream | 4,412.42 | 1.03ms | 1.85ms | 🥈 |
| HTTP/Binary | 4,026.66 | 1.21ms | 1.88ms | 🥉 |
| HTTP/JSON | 3,168.81 | 1.67ms | 2.43ms | 4위 |

### 분석

**gRPC가 여전히 HTTP보다 빠르다**

- gRPC/Unary: 5,748 req/s
- HTTP/Binary: 4,027 req/s
- **차이: +43%**

---

## Phase 1 vs Phase 2 비교

| 페이로드 | 1위 | 2위 | gRPC vs HTTP |
|---------|-----|-----|--------------|
| **1MB** (Phase 1) | HTTP/Binary (2,506) | gRPC/Unary (1,187) | HTTP 2.1배 빠름 |
| **1KB** (Phase 2) | gRPC/Unary (5,877) | gRPC/Stream (5,823) | **gRPC 1.6배 빠름** |
| **10KB** (Phase 2) | gRPC/Unary (5,748) | gRPC/Stream (4,412) | **gRPC 1.4배 빠름** |

---

## 핵심 인사이트

### 1. 가설 검증: ✅ 성공

> 작은 페이로드에서 gRPC가 HTTP보다 빠르다

- 1KB: gRPC가 **59% 더 빠름**
- 10KB: gRPC가 **43% 더 빠름**

### 2. 역전 포인트 발견

| 페이로드 크기 | 승자 |
|--------------|------|
| 1KB | gRPC |
| 10KB | gRPC |
| 100KB | ? (미측정) |
| 1MB | HTTP |

**역전 포인트는 10KB ~ 1MB 사이에 존재**

### 3. HTTP 헤더 오버헤드 영향

| 페이로드 | HTTP 헤더 비율 | gRPC 프레이밍 비율 |
|---------|---------------|-------------------|
| 1KB | ~40% | ~5% |
| 10KB | ~4% | ~0.5% |
| 1MB | ~0.04% | ~0.005% |

작은 페이로드에서 HTTP 헤더 오버헤드가 상대적으로 크기 때문에 gRPC가 유리해진다.

### 4. gRPC/Stream vs gRPC/Unary

| 페이로드 | Unary | Stream | 차이 |
|---------|-------|--------|------|
| 1KB | 5,877 | 5,823 | 거의 동일 |
| 10KB | 5,748 | 4,412 | Unary 30% 빠름 |

- 1KB: 청크 분할이 없어서 Stream과 Unary 성능 동일
- 10KB: 여전히 단일 청크지만 Stream 오버헤드로 인해 Unary가 우위

### 5. 서버 메모리 사용량

| 테스트 | Peak Heap |
|--------|-----------|
| HTTP/JSON 1KB | 143.64 MB |
| HTTP/Binary 1KB | 171.25 MB |
| gRPC/Unary 1KB | 276.80 MB |
| gRPC/Stream 1KB | 341.23 MB |
| gRPC/Unary 10KB | 494.66 MB |
| gRPC/Stream 10KB | 503.41 MB |

**gRPC가 HTTP보다 메모리를 더 많이 사용함** (Protobuf 직렬화/역직렬화 버퍼)

---

## 결론

### Phase 1 (1MB)
> HTTP/Binary가 gRPC보다 **2배 빠르다**

### Phase 2 (1KB, 10KB)
> gRPC/Unary가 HTTP/Binary보다 **1.5배 빠르다**

### 언제 무엇을 써야 하나?

| 상황 | 추천 |
|------|------|
| 대용량 파일 전송 (1MB+) | HTTP/Binary |
| 소용량 API 통신 (~10KB) | gRPC/Unary |
| 실시간 스트리밍 | gRPC/Stream |
| JSON 필수 환경 | HTTP/JSON |

---

## 다음 단계

- **Phase 3**: 고동시성 (100+ VU) - 멀티플렉싱 효과 검증
- **Phase 4**: 복잡한 데이터 구조 - JSON vs Protobuf 파싱 비용 비교
```azure
root@jun:/home/jun/distributed-log-pipeline/proto-bench/scripts# ./run-phase2.sh
================================
Phase 2: 소용량 페이로드 테스트
================================

[0/8] 서버 상태 확인...
✅ 서버 정상

[Warmup] JIT 워밍업 (각 엔드포인트 50회)...
✅ 워밍업 완료

========== 1KB 페이로드 테스트 ==========

[1/8] HTTP/JSON 1KB 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase2/http-json-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_json_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-16T16:54:45Z" level=info msg="============================================================" source=console
time="2026-01-16T16:54:45Z" level=info msg="Phase 2: HTTP/JSON Test - Size: 1kb" source=console
time="2026-01-16T16:54:45Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-16T16:54:45Z" level=info msg="============================================================" source=console
time="2026-01-16T16:54:45Z" level=info msg="✅ Server is ready" source=console
time="2026-01-16T16:54:45Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/JSON\",\"testName\":\"phase2-1kb\",\"startTime\":1768582485545}" source=console

running (0m01.0s), 10/10 VUs, 3543 complete and 0 interrupted iterations
http_json_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 6986 complete and 0 interrupted iterations
http_json_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 11048 complete and 0 interrupted iterations
http_json_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 15148 complete and 0 interrupted iterations
http_json_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 18344 complete and 0 interrupted iterations
http_json_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 21605 complete and 0 interrupted iterations
http_json_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 24796 complete and 0 interrupted iterations
http_json_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 28176 complete and 0 interrupted iterations
http_json_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 31524 complete and 0 interrupted iterations
http_json_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 35022 complete and 0 interrupted iterations
http_json_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 38240 complete and 0 interrupted iterations
http_json_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 41490 complete and 0 interrupted iterations
http_json_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 44761 complete and 0 interrupted iterations
http_json_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 48024 complete and 0 interrupted iterations
http_json_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 51303 complete and 0 interrupted iterations
http_json_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 54709 complete and 0 interrupted iterations
http_json_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 58092 complete and 0 interrupted iterations
http_json_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 61458 complete and 0 interrupted iterations
http_json_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 64689 complete and 0 interrupted iterations
http_json_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 68042 complete and 0 interrupted iterations
http_json_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 71387 complete and 0 interrupted iterations
http_json_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 74828 complete and 0 interrupted iterations
http_json_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 78038 complete and 0 interrupted iterations
http_json_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 81322 complete and 0 interrupted iterations
http_json_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 84504 complete and 0 interrupted iterations
http_json_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 87835 complete and 0 interrupted iterations
http_json_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 91564 complete and 0 interrupted iterations
http_json_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 95676 complete and 0 interrupted iterations
http_json_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 99469 complete and 0 interrupted iterations
http_json_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 102811 complete and 0 interrupted iterations
http_json_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-16T16:55:15Z" level=info msg="\n============================================================" source=console
time="2026-01-16T16:55:15Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-16T16:55:15Z" level=info msg="============================================================" source=console
time="2026-01-16T16:55:15Z" level=info msg="Protocol: HTTP/JSON" source=console
time="2026-01-16T16:55:15Z" level=info msg="Test Name: phase2-1kb" source=console
time="2026-01-16T16:55:15Z" level=info msg="Duration: 30009ms" source=console
time="2026-01-16T16:55:15Z" level=info msg="Total Requests: 102886" source=console
time="2026-01-16T16:55:15Z" level=info msg="Success Requests: 102886" source=console
time="2026-01-16T16:55:15Z" level=info msg="Failed Requests: 0" source=console
time="2026-01-16T16:55:15Z" level=info msg="Throughput: 3428.50 req/s" source=console
time="2026-01-16T16:55:15Z" level=info msg="\nLatency:" source=console
time="2026-01-16T16:55:15Z" level=info msg="  Average: 1.42ms" source=console
time="2026-01-16T16:55:15Z" level=info msg="  P50: 1.40ms" source=console
time="2026-01-16T16:55:15Z" level=info msg="  P95: 2.14ms" source=console
time="2026-01-16T16:55:15Z" level=info msg="  P99: 2.68ms" source=console
time="2026-01-16T16:55:15Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-16T16:55:15Z" level=info msg="  Peak Heap: 143.64MB" source=console
time="2026-01-16T16:55:15Z" level=info msg="  GC Count: 101" source=console
time="2026-01-16T16:55:15Z" level=info msg="  GC Time: 328ms" source=console
time="2026-01-16T16:55:15Z" level=info msg="============================================================" source=console
time="2026-01-16T16:55:15Z" level=info msg="Total test time: 30.11s" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<1000' p(95)=3.65ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 308658  10245.473223/s
    checks_succeeded...: 100.00% 308658 out of 308658
    checks_failed......: 0.00%   0 out of 308658

    ✓ status is 200
    ✓ has payload
    ✓ protocol is HTTP/JSON

    HTTP
    http_req_duration..............: avg=2.6ms  min=1.02ms med=2.59ms max=102.52ms p(90)=3.4ms  p(95)=3.65ms
      { expected_response:true }...: avg=2.6ms  min=1.02ms med=2.59ms max=102.52ms p(90)=3.4ms  p(95)=3.65ms
    http_req_failed................: 0.00%  0 out of 102889
    http_reqs......................: 102889 3415.257322/s

    EXECUTION
    iteration_duration.............: avg=2.89ms min=1.09ms med=2.87ms max=25.54ms  p(90)=3.73ms p(95)=3.99ms
    iterations.....................: 102886 3415.157741/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 25 MB  820 kB/s
    data_sent......................: 11 MB  348 kB/s




running (0m30.1s), 00/10 VUs, 102886 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 10 VUs  30s

[2/8] HTTP/Binary 1KB 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase2/http-binary-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_binary_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-16T16:55:18Z" level=info msg="============================================================" source=console
time="2026-01-16T16:55:18Z" level=info msg="Phase 2: HTTP/Binary Test - Size: 1kb" source=console
time="2026-01-16T16:55:18Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-16T16:55:18Z" level=info msg="============================================================" source=console
time="2026-01-16T16:55:18Z" level=info msg="✅ Server is ready" source=console
time="2026-01-16T16:55:18Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/Binary\",\"testName\":\"phase2-1kb\",\"startTime\":1768582518896}" source=console

running (0m01.0s), 10/10 VUs, 3348 complete and 0 interrupted iterations
http_binary_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 6755 complete and 0 interrupted iterations
http_binary_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 10194 complete and 0 interrupted iterations
http_binary_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 13699 complete and 0 interrupted iterations
http_binary_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 17694 complete and 0 interrupted iterations
http_binary_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 21226 complete and 0 interrupted iterations
http_binary_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 24638 complete and 0 interrupted iterations
http_binary_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 28251 complete and 0 interrupted iterations
http_binary_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 31851 complete and 0 interrupted iterations
http_binary_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 35390 complete and 0 interrupted iterations
http_binary_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 38810 complete and 0 interrupted iterations
http_binary_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 42513 complete and 0 interrupted iterations
http_binary_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 45983 complete and 0 interrupted iterations
http_binary_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 49681 complete and 0 interrupted iterations
http_binary_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 53409 complete and 0 interrupted iterations
http_binary_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 56909 complete and 0 interrupted iterations
http_binary_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 60388 complete and 0 interrupted iterations
http_binary_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 64079 complete and 0 interrupted iterations
http_binary_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 67697 complete and 0 interrupted iterations
http_binary_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 71140 complete and 0 interrupted iterations
http_binary_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 74750 complete and 0 interrupted iterations
http_binary_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 78137 complete and 0 interrupted iterations
http_binary_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 81583 complete and 0 interrupted iterations
http_binary_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 85080 complete and 0 interrupted iterations
http_binary_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 89072 complete and 0 interrupted iterations
http_binary_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 93021 complete and 0 interrupted iterations
http_binary_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 97390 complete and 0 interrupted iterations
http_binary_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 102012 complete and 0 interrupted iterations
http_binary_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 106352 complete and 0 interrupted iterations
http_binary_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 110766 complete and 0 interrupted iterations
http_binary_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-16T16:55:48Z" level=info msg="\n============================================================" source=console
time="2026-01-16T16:55:48Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-16T16:55:48Z" level=info msg="============================================================" source=console
time="2026-01-16T16:55:48Z" level=info msg="Protocol: HTTP/Binary" source=console
time="2026-01-16T16:55:48Z" level=info msg="Test Name: phase2-1kb" source=console
time="2026-01-16T16:55:48Z" level=info msg="Duration: 30004ms" source=console
time="2026-01-16T16:55:48Z" level=info msg="Total Requests: 110866" source=console
time="2026-01-16T16:55:48Z" level=info msg="Throughput: 3695.04 req/s" source=console
time="2026-01-16T16:55:48Z" level=info msg="\nLatency:" source=console
time="2026-01-16T16:55:48Z" level=info msg="  Average: 1.24ms" source=console
time="2026-01-16T16:55:48Z" level=info msg="  P50: 1.23ms" source=console
time="2026-01-16T16:55:48Z" level=info msg="  P95: 1.88ms" source=console
time="2026-01-16T16:55:48Z" level=info msg="  P99: 2.18ms" source=console
time="2026-01-16T16:55:48Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-16T16:55:48Z" level=info msg="  Peak Heap: 171.25MB" source=console
time="2026-01-16T16:55:48Z" level=info msg="  GC Count: 62" source=console
time="2026-01-16T16:55:48Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<1000' p(95)=3.36ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 332598  11066.961473/s
    checks_succeeded...: 100.00% 332598 out of 332598
    checks_failed......: 0.00%   0 out of 332598

    ✓ status is 200
    ✓ has payload
    ✓ protocol is HTTP/Binary

    HTTP
    http_req_duration..............: avg=2.41ms min=883.23µs med=2.39ms max=29.94ms p(90)=3.15ms p(95)=3.36ms
      { expected_response:true }...: avg=2.41ms min=883.23µs med=2.39ms max=29.94ms p(90)=3.15ms p(95)=3.36ms
    http_req_failed................: 0.00%  0 out of 110869
    http_reqs......................: 110869 3689.08698/s

    EXECUTION
    iteration_duration.............: avg=2.68ms min=967.86µs med=2.65ms max=10.86ms p(90)=3.46ms p(95)=3.7ms 
    iterations.....................: 110866 3688.987158/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 27 MB  893 kB/s
    data_sent......................: 12 MB  384 kB/s




running (0m30.1s), 00/10 VUs, 110866 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 10 VUs  30s

[3/8] gRPC/Unary 1KB 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase2/grpc-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-16T16:55:52Z" level=info msg="[Phase 2] gRPC/Unary 1kb 테스트 시작" source=console

running (0m01.0s), 10/10 VUs, 4638 complete and 0 interrupted iterations
grpc_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 10795 complete and 0 interrupted iterations
grpc_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 16875 complete and 0 interrupted iterations
grpc_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 22929 complete and 0 interrupted iterations
grpc_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 29029 complete and 0 interrupted iterations
grpc_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 35297 complete and 0 interrupted iterations
grpc_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 41593 complete and 0 interrupted iterations
grpc_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 47706 complete and 0 interrupted iterations
grpc_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 53685 complete and 0 interrupted iterations
grpc_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 60047 complete and 0 interrupted iterations
grpc_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 65822 complete and 0 interrupted iterations
grpc_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 72091 complete and 0 interrupted iterations
grpc_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 78286 complete and 0 interrupted iterations
grpc_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 84501 complete and 0 interrupted iterations
grpc_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 90333 complete and 0 interrupted iterations
grpc_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 96692 complete and 0 interrupted iterations
grpc_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 103082 complete and 0 interrupted iterations
grpc_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 109247 complete and 0 interrupted iterations
grpc_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 115562 complete and 0 interrupted iterations
grpc_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 121869 complete and 0 interrupted iterations
grpc_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 127814 complete and 0 interrupted iterations
grpc_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 133633 complete and 0 interrupted iterations
grpc_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 139948 complete and 0 interrupted iterations
grpc_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 146218 complete and 0 interrupted iterations
grpc_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 152408 complete and 0 interrupted iterations
grpc_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 158544 complete and 0 interrupted iterations
grpc_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 164302 complete and 0 interrupted iterations
grpc_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 170262 complete and 0 interrupted iterations
grpc_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 176327 complete and 0 interrupted iterations
grpc_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 182147 complete and 0 interrupted iterations
grpc_test   [ 100% ] 10 VUs  30.0s/30s

running (0m31.0s), 00/10 VUs, 182211 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 10 VUs  30s
time="2026-01-16T16:56:23Z" level=info msg="=== gRPC/Unary 1kb Result ===" source=console
time="2026-01-16T16:56:23Z" level=info msg="{\"protocol\":\"gRPC/Unary\",\"testName\":\"phase2-1kb\",\"durationMs\":31005,\"totalRequests\":182211,\"successRequests\":182211,\"failedRequests\":0,\"throughputRps\":5876.826318335752,\"latency\":{\"avgMs\":0.670042800220619,\"minMs\":0.164871,\"maxMs\":37.726962,\"p50Ms\":0.606113,\"p95Ms\":1.185704,\"p99Ms\":1.738596},\"serverMetrics\":{\"startHeapMb\":75.69514465332031,\"endHeapMb\":165.37550354003906,\"peakHeapMb\":276.8022232055664,\"gcCount\":53,\"gcTimeMs\":281},\"dataTransfer\":{\"totalBytes\":186584064,\"avgResponseBytes\":1024.0}}" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<500' p(95)=2.21ms


  █ TOTAL RESULTS 

    checks_total.......: 364422  11721.747998/s
    checks_succeeded...: 100.00% 364422 out of 364422
    checks_failed......: 0.00%   0 out of 364422

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=1.45ms min=660.81µs med=1.35ms max=78.1ms  p(90)=1.9ms  p(95)=2.21ms
      { expected_response:true }...: avg=1.45ms min=660.81µs med=1.35ms max=78.1ms  p(90)=1.9ms  p(95)=2.21ms
    http_req_failed................: 0.00%  0 out of 182213
    http_reqs......................: 182213 5860.938329/s

    EXECUTION
    iteration_duration.............: avg=1.63ms min=716.12µs med=1.53ms max=39.79ms p(90)=2.11ms p(95)=2.41ms
    iterations.....................: 182211 5860.873999/s
    vus............................: 0      min=0           max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 44 MB  1.4 MB/s
    data_sent......................: 18 MB  569 kB/s




running (0m31.1s), 00/10 VUs, 182211 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 10 VUs  30s

[4/8] gRPC/Stream 1KB 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase2/grpc-stream-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_stream_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-16T16:56:26Z" level=info msg="============================================================" source=console
time="2026-01-16T16:56:26Z" level=info msg="Phase 2: gRPC/Stream Test - Size: 1kb" source=console
time="2026-01-16T16:56:26Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-16T16:56:26Z" level=info msg="============================================================" source=console
time="2026-01-16T16:56:26Z" level=info msg="✅ Server is ready" source=console
time="2026-01-16T16:56:26Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Stream\",\"testName\":\"phase2-1kb\",\"startTime\":1768582586540}" source=console

running (0m01.0s), 10/10 VUs, 5368 complete and 0 interrupted iterations
grpc_stream_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 11229 complete and 0 interrupted iterations
grpc_stream_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 16919 complete and 0 interrupted iterations
grpc_stream_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 22611 complete and 0 interrupted iterations
grpc_stream_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 28342 complete and 0 interrupted iterations
grpc_stream_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 33786 complete and 0 interrupted iterations
grpc_stream_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 39459 complete and 0 interrupted iterations
grpc_stream_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 45266 complete and 0 interrupted iterations
grpc_stream_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 51107 complete and 0 interrupted iterations
grpc_stream_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 56988 complete and 0 interrupted iterations
grpc_stream_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 63011 complete and 0 interrupted iterations
grpc_stream_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 68749 complete and 0 interrupted iterations
grpc_stream_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 74514 complete and 0 interrupted iterations
grpc_stream_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 80280 complete and 0 interrupted iterations
grpc_stream_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 86329 complete and 0 interrupted iterations
grpc_stream_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 92213 complete and 0 interrupted iterations
grpc_stream_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 98221 complete and 0 interrupted iterations
grpc_stream_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 104166 complete and 0 interrupted iterations
grpc_stream_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 110123 complete and 0 interrupted iterations
grpc_stream_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 115793 complete and 0 interrupted iterations
grpc_stream_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 122006 complete and 0 interrupted iterations
grpc_stream_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 127788 complete and 0 interrupted iterations
grpc_stream_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 133723 complete and 0 interrupted iterations
grpc_stream_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 139277 complete and 0 interrupted iterations
grpc_stream_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 145197 complete and 0 interrupted iterations
grpc_stream_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 151035 complete and 0 interrupted iterations
grpc_stream_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 157016 complete and 0 interrupted iterations
grpc_stream_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 162975 complete and 0 interrupted iterations
grpc_stream_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 168701 complete and 0 interrupted iterations
grpc_stream_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 174645 complete and 0 interrupted iterations
grpc_stream_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-16T16:56:56Z" level=info msg="\n============================================================" source=console
time="2026-01-16T16:56:56Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-16T16:56:56Z" level=info msg="============================================================" source=console
time="2026-01-16T16:56:56Z" level=info msg="Protocol: gRPC/Stream" source=console
time="2026-01-16T16:56:56Z" level=info msg="Test Name: phase2-1kb" source=console
time="2026-01-16T16:56:56Z" level=info msg="Duration: 30006ms" source=console
time="2026-01-16T16:56:56Z" level=info msg="Total Requests: 174710" source=console
time="2026-01-16T16:56:56Z" level=info msg="Throughput: 5822.50 req/s" source=console
time="2026-01-16T16:56:56Z" level=info msg="\nLatency:" source=console
time="2026-01-16T16:56:56Z" level=info msg="  Average: 0.65ms" source=console
time="2026-01-16T16:56:56Z" level=info msg="  P50: 0.59ms" source=console
time="2026-01-16T16:56:56Z" level=info msg="  P95: 1.17ms" source=console
time="2026-01-16T16:56:56Z" level=info msg="  P99: 1.66ms" source=console
time="2026-01-16T16:56:56Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-16T16:56:56Z" level=info msg="  Peak Heap: 341.23MB" source=console
time="2026-01-16T16:56:56Z" level=info msg="  GC Count: 28" source=console
time="2026-01-16T16:56:56Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<1000' p(95)=2.24ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 524130  17433.809701/s
    checks_succeeded...: 100.00% 524130 out of 524130
    checks_failed......: 0.00%   0 out of 524130

    ✓ status is 200
    ✓ has payload
    ✓ protocol is gRPC/Stream

    HTTP
    http_req_duration..............: avg=1.46ms min=701.76µs med=1.37ms max=48.61ms p(90)=1.94ms p(95)=2.24ms
      { expected_response:true }...: avg=1.46ms min=701.76µs med=1.37ms max=48.61ms p(90)=1.94ms p(95)=2.24ms
    http_req_failed................: 0.00%  0 out of 174713
    http_reqs......................: 174713 5811.369688/s

    EXECUTION
    iteration_duration.............: avg=1.69ms min=753.31µs med=1.59ms max=16.07ms p(90)=2.22ms p(95)=2.53ms
    iterations.....................: 174710 5811.2699/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 42 MB  1.4 MB/s
    data_sent......................: 18 MB  604 kB/s




running (0m30.1s), 00/10 VUs, 174710 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 10 VUs  30s

========== 10KB 페이로드 테스트 ==========

[5/8] HTTP/JSON 10KB 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase2/http-json-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_json_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-16T16:56:59Z" level=info msg="============================================================" source=console
time="2026-01-16T16:56:59Z" level=info msg="Phase 2: HTTP/JSON Test - Size: 10kb" source=console
time="2026-01-16T16:56:59Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-16T16:56:59Z" level=info msg="============================================================" source=console
time="2026-01-16T16:56:59Z" level=info msg="✅ Server is ready" source=console
time="2026-01-16T16:56:59Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/JSON\",\"testName\":\"phase2-10kb\",\"startTime\":1768582619818}" source=console

running (0m01.0s), 10/10 VUs, 2977 complete and 0 interrupted iterations
http_json_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 5915 complete and 0 interrupted iterations
http_json_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 8943 complete and 0 interrupted iterations
http_json_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 12098 complete and 0 interrupted iterations
http_json_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 15156 complete and 0 interrupted iterations
http_json_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 18277 complete and 0 interrupted iterations
http_json_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 21153 complete and 0 interrupted iterations
http_json_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 24071 complete and 0 interrupted iterations
http_json_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 27104 complete and 0 interrupted iterations
http_json_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 30042 complete and 0 interrupted iterations
http_json_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 32898 complete and 0 interrupted iterations
http_json_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 35894 complete and 0 interrupted iterations
http_json_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 39009 complete and 0 interrupted iterations
http_json_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 41910 complete and 0 interrupted iterations
http_json_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 44821 complete and 0 interrupted iterations
http_json_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 48104 complete and 0 interrupted iterations
http_json_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 51050 complete and 0 interrupted iterations
http_json_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 54018 complete and 0 interrupted iterations
http_json_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 57575 complete and 0 interrupted iterations
http_json_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 60696 complete and 0 interrupted iterations
http_json_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 63757 complete and 0 interrupted iterations
http_json_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 66854 complete and 0 interrupted iterations
http_json_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 69771 complete and 0 interrupted iterations
http_json_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 73068 complete and 0 interrupted iterations
http_json_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 77110 complete and 0 interrupted iterations
http_json_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 81089 complete and 0 interrupted iterations
http_json_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 84577 complete and 0 interrupted iterations
http_json_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 87839 complete and 0 interrupted iterations
http_json_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 91525 complete and 0 interrupted iterations
http_json_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 95040 complete and 0 interrupted iterations
http_json_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-16T16:57:29Z" level=info msg="\n============================================================" source=console
time="2026-01-16T16:57:29Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-16T16:57:29Z" level=info msg="============================================================" source=console
time="2026-01-16T16:57:29Z" level=info msg="Protocol: HTTP/JSON" source=console
time="2026-01-16T16:57:29Z" level=info msg="Test Name: phase2-10kb" source=console
time="2026-01-16T16:57:29Z" level=info msg="Duration: 30005ms" source=console
time="2026-01-16T16:57:29Z" level=info msg="Total Requests: 95080" source=console
time="2026-01-16T16:57:29Z" level=info msg="Success Requests: 95080" source=console
time="2026-01-16T16:57:29Z" level=info msg="Failed Requests: 0" source=console
time="2026-01-16T16:57:29Z" level=info msg="Throughput: 3168.81 req/s" source=console
time="2026-01-16T16:57:29Z" level=info msg="\nLatency:" source=console
time="2026-01-16T16:57:29Z" level=info msg="  Average: 1.67ms" source=console
time="2026-01-16T16:57:29Z" level=info msg="  P50: 1.67ms" source=console
time="2026-01-16T16:57:29Z" level=info msg="  P95: 2.43ms" source=console
time="2026-01-16T16:57:29Z" level=info msg="  P99: 2.80ms" source=console
time="2026-01-16T16:57:29Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-16T16:57:29Z" level=info msg="  Peak Heap: 349.56MB" source=console
time="2026-01-16T16:57:29Z" level=info msg="  GC Count: 23" source=console
time="2026-01-16T16:57:29Z" level=info msg="  GC Time: 169ms" source=console
time="2026-01-16T16:57:29Z" level=info msg="============================================================" source=console
time="2026-01-16T16:57:29Z" level=info msg="Total test time: 30.03s" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<1000' p(95)=3.92ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 285240  9494.401492/s
    checks_succeeded...: 100.00% 285240 out of 285240
    checks_failed......: 0.00%   0 out of 285240

    ✓ status is 200
    ✓ has payload
    ✓ protocol is HTTP/JSON

    HTTP
    http_req_duration..............: avg=2.86ms min=1.14ms med=2.88ms max=28.84ms p(90)=3.69ms p(95)=3.92ms
      { expected_response:true }...: avg=2.86ms min=1.14ms med=2.88ms max=28.84ms p(90)=3.69ms p(95)=3.92ms
    http_req_failed................: 0.00%  0 out of 95083
    http_reqs......................: 95083  3164.900354/s

    EXECUTION
    iteration_duration.............: avg=3.13ms min=1.22ms med=3.14ms max=21.98ms p(90)=4.01ms p(95)=4.25ms
    iterations.....................: 95080  3164.800497/s
    vus............................: 10     min=10         max=10
    vus_max........................: 10     min=10         max=10

    NETWORK
    data_received..................: 23 MB  763 kB/s
    data_sent......................: 9.8 MB 326 kB/s




running (0m30.0s), 00/10 VUs, 95080 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 10 VUs  30s

[6/8] HTTP/Binary 10KB 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase2/http-binary-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_binary_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-16T16:57:33Z" level=info msg="============================================================" source=console
time="2026-01-16T16:57:33Z" level=info msg="Phase 2: HTTP/Binary Test - Size: 10kb" source=console
time="2026-01-16T16:57:33Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-16T16:57:33Z" level=info msg="============================================================" source=console
time="2026-01-16T16:57:33Z" level=info msg="✅ Server is ready" source=console
time="2026-01-16T16:57:33Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/Binary\",\"testName\":\"phase2-10kb\",\"startTime\":1768582653174}" source=console

running (0m01.0s), 10/10 VUs, 4412 complete and 0 interrupted iterations
http_binary_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 9359 complete and 0 interrupted iterations
http_binary_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 14187 complete and 0 interrupted iterations
http_binary_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 19234 complete and 0 interrupted iterations
http_binary_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 24355 complete and 0 interrupted iterations
http_binary_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 28399 complete and 0 interrupted iterations
http_binary_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 33116 complete and 0 interrupted iterations
http_binary_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 37735 complete and 0 interrupted iterations
http_binary_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 41860 complete and 0 interrupted iterations
http_binary_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 46202 complete and 0 interrupted iterations
http_binary_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 50804 complete and 0 interrupted iterations
http_binary_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 54992 complete and 0 interrupted iterations
http_binary_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 59678 complete and 0 interrupted iterations
http_binary_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 64237 complete and 0 interrupted iterations
http_binary_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 67629 complete and 0 interrupted iterations
http_binary_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 71123 complete and 0 interrupted iterations
http_binary_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 74954 complete and 0 interrupted iterations
http_binary_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 78635 complete and 0 interrupted iterations
http_binary_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 82266 complete and 0 interrupted iterations
http_binary_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 85721 complete and 0 interrupted iterations
http_binary_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 89162 complete and 0 interrupted iterations
http_binary_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 92743 complete and 0 interrupted iterations
http_binary_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 96098 complete and 0 interrupted iterations
http_binary_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 99676 complete and 0 interrupted iterations
http_binary_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 103316 complete and 0 interrupted iterations
http_binary_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 106890 complete and 0 interrupted iterations
http_binary_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 110304 complete and 0 interrupted iterations
http_binary_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 113766 complete and 0 interrupted iterations
http_binary_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 117215 complete and 0 interrupted iterations
http_binary_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 120791 complete and 0 interrupted iterations
http_binary_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-16T16:58:03Z" level=info msg="\n============================================================" source=console
time="2026-01-16T16:58:03Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-16T16:58:03Z" level=info msg="============================================================" source=console
time="2026-01-16T16:58:03Z" level=info msg="Protocol: HTTP/Binary" source=console
time="2026-01-16T16:58:03Z" level=info msg="Test Name: phase2-10kb" source=console
time="2026-01-16T16:58:03Z" level=info msg="Duration: 30007ms" source=console
time="2026-01-16T16:58:03Z" level=info msg="Total Requests: 120828" source=console
time="2026-01-16T16:58:03Z" level=info msg="Throughput: 4026.66 req/s" source=console
time="2026-01-16T16:58:03Z" level=info msg="\nLatency:" source=console
time="2026-01-16T16:58:03Z" level=info msg="  Average: 1.21ms" source=console
time="2026-01-16T16:58:03Z" level=info msg="  P50: 1.15ms" source=console
time="2026-01-16T16:58:03Z" level=info msg="  P95: 1.88ms" source=console
time="2026-01-16T16:58:03Z" level=info msg="  P99: 2.16ms" source=console
time="2026-01-16T16:58:03Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-16T16:58:03Z" level=info msg="  Peak Heap: 356.02MB" source=console
time="2026-01-16T16:58:03Z" level=info msg="  GC Count: 26" source=console
time="2026-01-16T16:58:03Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<1000' p(95)=3.28ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 362484  12062.520049/s
    checks_succeeded...: 100.00% 362484 out of 362484
    checks_failed......: 0.00%   0 out of 362484

    ✓ status is 200
    ✓ has payload
    ✓ protocol is HTTP/Binary

    HTTP
    http_req_duration..............: avg=2.26ms min=971.13µs med=2.19ms max=36.83ms p(90)=3.06ms p(95)=3.28ms
      { expected_response:true }...: avg=2.26ms min=971.13µs med=2.19ms max=36.83ms p(90)=3.06ms p(95)=3.28ms
    http_req_failed................: 0.00%  0 out of 120831
    http_reqs......................: 120831 4020.939849/s

    EXECUTION
    iteration_duration.............: avg=2.46ms min=1.05ms   med=2.39ms max=15.18ms p(90)=3.32ms p(95)=3.56ms
    iterations.....................: 120828 4020.840016/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 29 MB  977 kB/s
    data_sent......................: 13 MB  422 kB/s




running (0m30.1s), 00/10 VUs, 120828 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 10 VUs  30s

[7/8] gRPC/Unary 10KB 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase2/grpc-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-16T16:58:06Z" level=info msg="[Phase 2] gRPC/Unary 10kb 테스트 시작" source=console

running (0m01.0s), 10/10 VUs, 5247 complete and 0 interrupted iterations
grpc_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 11283 complete and 0 interrupted iterations
grpc_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 16989 complete and 0 interrupted iterations
grpc_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 22893 complete and 0 interrupted iterations
grpc_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 28891 complete and 0 interrupted iterations
grpc_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 34846 complete and 0 interrupted iterations
grpc_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 40792 complete and 0 interrupted iterations
grpc_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 47107 complete and 0 interrupted iterations
grpc_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 53128 complete and 0 interrupted iterations
grpc_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 59166 complete and 0 interrupted iterations
grpc_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 65265 complete and 0 interrupted iterations
grpc_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 71384 complete and 0 interrupted iterations
grpc_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 77468 complete and 0 interrupted iterations
grpc_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 83369 complete and 0 interrupted iterations
grpc_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 89602 complete and 0 interrupted iterations
grpc_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 95194 complete and 0 interrupted iterations
grpc_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 100943 complete and 0 interrupted iterations
grpc_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 106858 complete and 0 interrupted iterations
grpc_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 112867 complete and 0 interrupted iterations
grpc_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 118590 complete and 0 interrupted iterations
grpc_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 124417 complete and 0 interrupted iterations
grpc_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 130546 complete and 0 interrupted iterations
grpc_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 136669 complete and 0 interrupted iterations
grpc_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 142720 complete and 0 interrupted iterations
grpc_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 148810 complete and 0 interrupted iterations
grpc_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 154655 complete and 0 interrupted iterations
grpc_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 160570 complete and 0 interrupted iterations
grpc_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 166241 complete and 0 interrupted iterations
grpc_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 172219 complete and 0 interrupted iterations
grpc_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 178168 complete and 0 interrupted iterations
grpc_test   [ 100% ] 10 VUs  30.0s/30s

running (0m31.0s), 00/10 VUs, 178229 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 10 VUs  30s
time="2026-01-16T16:58:37Z" level=info msg="=== gRPC/Unary 10kb Result ===" source=console
time="2026-01-16T16:58:37Z" level=info msg="{\"protocol\":\"gRPC/Unary\",\"testName\":\"phase2-10kb\",\"durationMs\":31006,\"totalRequests\":178229,\"successRequests\":178229,\"failedRequests\":0,\"throughputRps\":5748.210023866349,\"latency\":{\"avgMs\":0.6572791348097146,\"minMs\":0.159599,\"maxMs\":15.494043,\"p50Ms\":0.597256,\"p95Ms\":1.157411,\"p99Ms\":1.686448},\"serverMetrics\":{\"startHeapMb\":318.48697662353516,\"endHeapMb\":335.2779083251953,\"peakHeapMb\":494.65628814697266,\"gcCount\":28,\"gcTimeMs\":242},\"dataTransfer\":{\"totalBytes\":1825064960,\"avgResponseBytes\":10240.0}}" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<500' p(95)=2.19ms


  █ TOTAL RESULTS 

    checks_total.......: 356458  11472.9886/s
    checks_succeeded...: 100.00% 356458 out of 356458
    checks_failed......: 0.00%   0 out of 356458

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=1.46ms min=661.58µs med=1.38ms max=56.95ms p(90)=1.91ms p(95)=2.19ms
      { expected_response:true }...: avg=1.46ms min=661.58µs med=1.38ms max=56.95ms p(90)=1.91ms p(95)=2.19ms
    http_req_failed................: 0.00%  0 out of 178231
    http_reqs......................: 178231 5736.558672/s

    EXECUTION
    iteration_duration.............: avg=1.66ms min=714.82µs med=1.57ms max=17.34ms p(90)=2.15ms p(95)=2.44ms
    iterations.....................: 178229 5736.4943/s
    vus............................: 0      min=0           max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 43 MB  1.4 MB/s
    data_sent......................: 18 MB  562 kB/s




running (0m31.1s), 00/10 VUs, 178229 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 10 VUs  30s

[8/8] gRPC/Stream 10KB 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase2/grpc-stream-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_stream_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-16T16:58:40Z" level=info msg="============================================================" source=console
time="2026-01-16T16:58:40Z" level=info msg="Phase 2: gRPC/Stream Test - Size: 10kb" source=console
time="2026-01-16T16:58:40Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-16T16:58:40Z" level=info msg="============================================================" source=console
time="2026-01-16T16:58:40Z" level=info msg="✅ Server is ready" source=console
time="2026-01-16T16:58:40Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Stream\",\"testName\":\"phase2-10kb\",\"startTime\":1768582720925}" source=console

running (0m01.0s), 10/10 VUs, 4229 complete and 0 interrupted iterations
grpc_stream_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 8605 complete and 0 interrupted iterations
grpc_stream_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 12884 complete and 0 interrupted iterations
grpc_stream_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 17281 complete and 0 interrupted iterations
grpc_stream_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 21847 complete and 0 interrupted iterations
grpc_stream_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 26298 complete and 0 interrupted iterations
grpc_stream_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 30524 complete and 0 interrupted iterations
grpc_stream_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 34895 complete and 0 interrupted iterations
grpc_stream_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 39392 complete and 0 interrupted iterations
grpc_stream_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 43963 complete and 0 interrupted iterations
grpc_stream_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 48491 complete and 0 interrupted iterations
grpc_stream_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 52583 complete and 0 interrupted iterations
grpc_stream_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 56974 complete and 0 interrupted iterations
grpc_stream_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 61438 complete and 0 interrupted iterations
grpc_stream_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 65637 complete and 0 interrupted iterations
grpc_stream_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 69927 complete and 0 interrupted iterations
grpc_stream_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 74386 complete and 0 interrupted iterations
grpc_stream_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 78624 complete and 0 interrupted iterations
grpc_stream_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 83123 complete and 0 interrupted iterations
grpc_stream_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 87542 complete and 0 interrupted iterations
grpc_stream_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 92155 complete and 0 interrupted iterations
grpc_stream_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 96839 complete and 0 interrupted iterations
grpc_stream_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 101381 complete and 0 interrupted iterations
grpc_stream_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 105777 complete and 0 interrupted iterations
grpc_stream_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 110223 complete and 0 interrupted iterations
grpc_stream_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 114462 complete and 0 interrupted iterations
grpc_stream_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 118798 complete and 0 interrupted iterations
grpc_stream_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 123438 complete and 0 interrupted iterations
grpc_stream_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 127815 complete and 0 interrupted iterations
grpc_stream_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 132338 complete and 0 interrupted iterations
grpc_stream_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-16T16:59:10Z" level=info msg="\n============================================================" source=console
time="2026-01-16T16:59:10Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-16T16:59:10Z" level=info msg="============================================================" source=console
time="2026-01-16T16:59:10Z" level=info msg="Protocol: gRPC/Stream" source=console
time="2026-01-16T16:59:10Z" level=info msg="Test Name: phase2-10kb" source=console
time="2026-01-16T16:59:10Z" level=info msg="Duration: 30006ms" source=console
time="2026-01-16T16:59:10Z" level=info msg="Total Requests: 132399" source=console
time="2026-01-16T16:59:10Z" level=info msg="Throughput: 4412.42 req/s" source=console
time="2026-01-16T16:59:10Z" level=info msg="\nLatency:" source=console
time="2026-01-16T16:59:10Z" level=info msg="  Average: 1.03ms" source=console
time="2026-01-16T16:59:10Z" level=info msg="  P50: 0.93ms" source=console
time="2026-01-16T16:59:10Z" level=info msg="  P95: 1.85ms" source=console
time="2026-01-16T16:59:10Z" level=info msg="  P99: 2.38ms" source=console
time="2026-01-16T16:59:10Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-16T16:59:10Z" level=info msg="  Peak Heap: 503.41MB" source=console
time="2026-01-16T16:59:10Z" level=info msg="  GC Count: 16" source=console
time="2026-01-16T16:59:10Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<1000' p(95)=3.07ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 397197  13216.404777/s
    checks_succeeded...: 100.00% 397197 out of 397197
    checks_failed......: 0.00%   0 out of 397197

    ✓ status is 200
    ✓ has payload
    ✓ protocol is gRPC/Stream

    HTTP
    http_req_duration..............: avg=1.95ms min=936.52µs med=1.79ms max=36.63ms p(90)=2.73ms p(95)=3.07ms
      { expected_response:true }...: avg=1.95ms min=936.52µs med=1.79ms max=36.63ms p(90)=2.73ms p(95)=3.07ms
    http_req_failed................: 0.00%  0 out of 132402
    http_reqs......................: 132402 4405.568081/s

    EXECUTION
    iteration_duration.............: avg=2.24ms min=983.8µs  med=2.08ms max=23.61ms p(90)=3.02ms p(95)=3.39ms
    iterations.....................: 132399 4405.468259/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 32 MB  1.1 MB/s
    data_sent......................: 14 MB  463 kB/s




running (0m30.1s), 00/10 VUs, 132399 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 10 VUs  30s

================================
✅ Phase 2 테스트 완료!
결과 파일: /home/jun/distributed-log-pipeline/proto-bench/scripts/../results/phase2/*_20260116_165435.log
================================
root@jun:/home/jun/distributed-log-pipeline/proto-bench/scripts# 

```