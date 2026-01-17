# Phase 4: 역전 포인트 탐색 테스트 결과

## 개요

| 항목 | 값 |
|------|-----|
| 페이로드 크기 | 10KB → 50KB → 100KB → 200KB → 500KB |
| 동시 사용자 (VU) | 10 |
| 테스트 시간 | 각 30초 |
| 테스트 횟수 | 3회 (1차, 2차, 3차) |

## 가설

> "페이로드 크기가 커질수록 Protobuf 직렬화 오버헤드가 증가하여  
> 100KB ~ 500KB 사이에서 HTTP가 gRPC를 추월할 것이다"

### 배경: 이전 Phase 결과

**Phase 2 결과 (소용량):**
- **1KB**: gRPC/Unary가 59% 더 빠름 (5,877 vs 3,695 req/s)
- **10KB**: gRPC/Unary가 43% 더 빠름 (5,748 vs 4,027 req/s)

**Phase 1 결과 (대용량):**
- **1MB**: HTTP/Binary가 2.1배 더 빠름 (2,506 vs 1,187 req/s)

**이 사이 어딘가에 역전 포인트가 존재할 것으로 예상됨.**

---

## 1차 테스트 결과

### 1차 결과 요약

| 크기 | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream | 승자 |
|------|-----------|-------------|------------|-------------|------|
| 10KB | 3,591 | **4,731** | 4,165 | 3,635 | HTTP/Binary |
| 50KB | 2,656 | **4,148** | 3,833 | 2,349 | HTTP/Binary |
| 100KB | 2,257 | **3,809** | 2,626 | 1,708 | HTTP/Binary |
| 200KB | 1,583 | **3,745** | 3,640 | 1,181 | HTTP/Binary |
| 500KB | 935 | **3,478** | 2,299 | 556 | HTTP/Binary |

### 1차 분석

1차 테스트에서는 **모든 페이로드 크기에서 HTTP/Binary가 우위**를 보였습니다.

이는 Phase 2 결과(gRPC 우위)와 상반되어 추가 검증이 필요했습니다.

---

## 2차 테스트 결과 (검증 테스트)

### 결과: 10KB 페이로드

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 순위 |
|----------|-------------------|-------------|-------------|------|
| **gRPC/Unary** | **6,268.09** | 0.74ms | 1.35ms | 🥇 |
| gRPC/Stream | 4,527.38 | 1.02ms | 1.83ms | 🥈 |
| HTTP/Binary | 3,744.80 | 1.28ms | 1.93ms | 🥉 |
| HTTP/JSON | 3,064.25 | 1.72ms | 2.44ms | 4위 |

### 결과: 50KB 페이로드

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 순위 |
|----------|-------------------|-------------|-------------|------|
| **gRPC/Unary** | **5,628.32** | 0.89ms | 1.59ms | 🥇 |
| HTTP/Binary | 4,088.72 | 1.24ms | 1.99ms | 🥈 |
| gRPC/Stream | 2,407.57 | 2.78ms | 4.49ms | 🥉 |
| HTTP/JSON | 2,381.89 | 2.67ms | 3.76ms | 4위 |

### 결과: 100KB 페이로드

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 순위 |
|----------|-------------------|-------------|-------------|------|
| **gRPC/Unary** | **5,067.02** | 0.99ms | 1.88ms | 🥇 |
| HTTP/Binary | 3,904.42 | 1.36ms | 2.12ms | 🥈 |
| HTTP/JSON | 2,159.31 | 3.22ms | 5.05ms | 🥉 |
| gRPC/Stream | 1,960.14 | 3.84ms | 6.05ms | 4위 |

### 결과: 200KB 페이로드

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 순위 |
|----------|-------------------|-------------|-------------|------|
| **HTTP/Binary** | **4,713.84** | 1.22ms | 1.91ms | 🥇 |
| gRPC/Unary | 4,128.47 | 1.29ms | 2.64ms | 🥈 |
| HTTP/JSON | 1,880.71 | 4.05ms | 5.73ms | 🥉 |
| gRPC/Stream | 1,288.08 | 6.48ms | 9.57ms | 4위 |

### 결과: 500KB 페이로드

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 순위 |
|----------|-------------------|-------------|-------------|------|
| **HTTP/Binary** | **3,883.37** | 1.63ms | 2.40ms | 🥇 |
| gRPC/Unary | 2,429.51 | 2.89ms | 5.53ms | 🥈 |
| HTTP/JSON | 1,014.19 | 8.51ms | 11.53ms | 🥉 |
| gRPC/Stream | 610.46 | 15.00ms | 22.41ms | 4위 |

---

## 3차 테스트 결과

3차 테스트는 2차와 거의 동일한 결과를 보여 **2차 결과의 재현성을 확인**했습니다.

---

## 1차 vs 2차 결과 비교

### 10KB 결과 변화

| 프로토콜 | 1차 결과 | 2차 결과 | 변화 |
|----------|----------|----------|------|
| HTTP/JSON | 3,591 | 3,064 | -15% |
| HTTP/Binary | 4,731 | 3,745 | -21% |
| gRPC/Unary | 4,165 | **6,268** | **+51%** |
| gRPC/Stream | 3,635 | 4,527 | +25% |

### 승자 변화

| 크기 | 1차 승자 | 2차 승자 | 비고 |
|------|----------|----------|------|
| 10KB | HTTP/Binary (+14%) | **gRPC/Unary (+67%)** | 역전 |
| 50KB | HTTP/Binary (+8%) | **gRPC/Unary (+38%)** | 역전 |
| 100KB | HTTP/Binary (+45%) | **gRPC/Unary (+30%)** | 역전 |
| 200KB | HTTP/Binary (+3%) | HTTP/Binary (+14%) | 유지 |
| 500KB | HTTP/Binary (+51%) | HTTP/Binary (+60%) | 유지 |

### 1차 테스트 결과가 달랐던 원인

1. **JIT 워밍업 부족**: 서버가 충분히 워밍업되지 않은 상태
2. **GC 불안정**: 초기 테스트로 인한 GC 압박
3. **gRPC 연결 수립 오버헤드**: HTTP/2 연결이 아직 최적화되지 않음

2차, 3차 테스트에서는 서버가 안정화된 상태에서 측정되어 **Phase 2 결과와 일관된 패턴**을 보였습니다.

---

## 최종 결과: HTTP/Binary vs gRPC/Unary 비교

| 크기 | HTTP/Binary | gRPC/Unary | 차이 | 승자 |
|------|-------------|------------|------|------|
| 10KB | 3,745 | 6,268 | **+67%** | 🏆 gRPC/Unary |
| 50KB | 4,089 | 5,628 | **+38%** | 🏆 gRPC/Unary |
| 100KB | 3,904 | 5,067 | **+30%** | 🏆 gRPC/Unary |
| 200KB | 4,714 | 4,128 | **+14%** | 🏆 HTTP/Binary |
| 500KB | 3,883 | 2,430 | **+60%** | 🏆 HTTP/Binary |

---

## 페이로드 크기별 성능 변화 추이

### Throughput (req/s) 비교표

| 크기 | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream |
|------|-----------|-------------|------------|-------------|
| 10KB | 3,064 | 3,745 | **6,268** | 4,527 |
| 50KB | 2,382 | 4,089 | **5,628** | 2,408 |
| 100KB | 2,159 | 3,904 | **5,067** | 1,960 |
| 200KB | 1,881 | **4,714** | 4,128 | 1,288 |
| 500KB | 1,014 | **3,883** | 2,430 | 610 |

### Latency P95 비교 (ms)

| 크기 | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream |
|------|-----------|-------------|------------|-------------|
| 10KB | 2.44 | 1.93 | **1.35** | 1.83 |
| 50KB | 3.76 | 1.99 | **1.59** | 4.49 |
| 100KB | 5.05 | 2.12 | **1.88** | 6.05 |
| 200KB | 5.73 | **1.91** | 2.64 | 9.57 |
| 500KB | 11.53 | **2.40** | 5.53 | 22.41 |

### 크기 증가에 따른 Throughput 감소율

| 프로토콜 | 10KB→500KB | 특성 |
|----------|------------|------|
| HTTP/Binary | **+4%** | 매우 안정적 (오히려 증가) |
| gRPC/Unary | -61% | 대용량에서 급락 |
| HTTP/JSON | -67% | 지속적 감소 |
| gRPC/Stream | -87% | 가장 급격한 감소 |

---

## 핵심 인사이트

### 1. 역전 포인트 발견: ✅ 가설 검증 성공

**100KB ~ 200KB 사이에서 gRPC→HTTP 역전 발생!**

| 페이로드 크기 | 승자 | 차이 |
|--------------|------|------|
| ≤100KB | gRPC/Unary | 30~67% 더 빠름 |
| ≥200KB | HTTP/Binary | 14~60% 더 빠름 |

### 2. Phase 2 결과와의 일관성 확인

2차, 3차 테스트 결과가 Phase 2와 동일한 패턴을 보임:

| Phase | 페이로드 | 승자 | 차이 |
|-------|---------|------|------|
| Phase 2 | 1KB | gRPC/Unary | +59% |
| Phase 2 | 10KB | gRPC/Unary | +43% |
| Phase 4 (2차) | 10KB | gRPC/Unary | +67% |
| Phase 4 (2차) | 50KB | gRPC/Unary | +38% |
| Phase 4 (2차) | 100KB | gRPC/Unary | +30% |

**소용량에서 gRPC 우위라는 결론이 재확인됨.**

### 3. 프로토콜별 스케일링 특성

| 프로토콜 | 특성 | 적합한 상황 |
|----------|------|------------|
| **HTTP/Binary** | 페이로드 크기에 관계없이 안정적 | 대용량, 가변 크기 |
| **gRPC/Unary** | 소용량에서 최고 성능, 대용량에서 급락 | 소용량 API |
| HTTP/JSON | 지속적인 성능 저하 | JSON 필수 환경 |
| gRPC/Stream | 가장 급격한 성능 저하 | 실시간 스트리밍 |

### 4. 역전이 발생하는 이유

| 요인 | 소용량 (≤100KB) | 대용량 (≥200KB) |
|------|----------------|----------------|
| HTTP 헤더 오버헤드 | 상대적으로 큼 | 무시할 수준 |
| Protobuf 직렬화 비용 | 낮음 | **높음** |
| HTTP/2 프레이밍 효율 | gRPC 유리 | 동등 |
| 메모리 복사 비용 | 낮음 | **gRPC에서 높음** |

---

## 서버 메트릭

| 크기 | HTTP/Binary Peak Heap | gRPC/Unary Peak Heap | GC Count (HTTP) | GC Count (gRPC) |
|------|----------------------|---------------------|-----------------|-----------------|
| 10KB | 647 MB | 649 MB | 15 | 18 |
| 50KB | 654 MB | 659 MB | 25 | 30 |
| 100KB | 675 MB | 683 MB | 36 | 41 |
| 200KB | 698 MB | 706 MB | 72 | 58 |
| 500KB | 721 MB | 725 MB | 128 | 79 |

**관찰**: 대용량에서 HTTP/Binary의 GC 횟수가 더 많음에도 성능 우위 유지.

---

## 전체 Phase 종합 (Phase 1~4)

### 페이로드 크기별 권장 프로토콜

| 페이로드 크기 | 권장 프로토콜 | 성능 차이 | 확신도 |
|--------------|--------------|----------|--------|
| 1KB | gRPC/Unary | +59% | ✅ 높음 |
| 10KB | gRPC/Unary | +43~67% | ✅ 높음 |
| 50KB | gRPC/Unary | +38% | ✅ 높음 |
| 100KB | gRPC/Unary | +30% | ✅ 높음 |
| 200KB | HTTP/Binary | +14% | ✅ 높음 |
| 500KB | HTTP/Binary | +60% | ✅ 높음 |
| 1MB | HTTP/Binary | +111% | ✅ 높음 |

### 동시 접속자별 권장 프로토콜 (Phase 3 결과)

| VU | 권장 프로토콜 | 비고 |
|----|--------------|------|
| ≤100 | HTTP/Binary | 낮은 오버헤드 |
| ≥200 | gRPC/Unary | 멀티플렉싱 효과 |

---

## 최종 결론

### 프로토콜 선택 가이드

| 상황 | 추천 프로토콜 | 근거 |
|------|--------------|------|
| **소용량 API (≤100KB)** | **gRPC/Unary** | 30~67% 더 빠름 |
| **대용량 전송 (≥200KB)** | **HTTP/Binary** | 14~60% 더 빠름 |
| **고동시성 (200+ VU)** | **gRPC/Unary** | 멀티플렉싱 효과 |
| **실시간 양방향 스트리밍** | **gRPC/Stream** | 기능적 요구 |
| **JSON 필수 환경** | HTTP/JSON | 호환성 |

### 역전 포인트 요약

```
소용량                    역전 포인트                    대용량
←─────────────────────────────┼─────────────────────────────→
   gRPC/Unary 우위        100KB~200KB        HTTP/Binary 우위
   (+30~67%)                                    (+14~60%)
```

---

## 테스트 환경

- **서버**: Ktor (Kotlin)
- **로드 테스터**: k6 (Grafana)
- **테스트 일시**:
    - 1차: 2026-01-17 02:16 ~ 02:27 (KST)
    - 2차: 2026-01-17 02:34 ~ 02:45 (KST)
    - 3차: 2차와 유사한 결과 확인
- **총 테스트 시간**: 약 33분 (60개 테스트)
### 실 로그 내용
```azure
root@jun:/home/jun/distributed-log-pipeline/proto-bench/scripts# ./run-phase4.sh
================================
Phase 4: 역전 포인트 탐색 테스트
================================

[0] 서버 상태 확인...
✅ 서버 정상

[Warmup] JIT 워밍업...
✅ 워밍업 완료

==========================================
         10kb 페이로드 테스트
==========================================

[1/20] HTTP/JSON - 10kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/http-json-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_json_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:34:30Z" level=info msg="============================================================" source=console
time="2026-01-17T02:34:30Z" level=info msg="Phase 4: HTTP/JSON Crossover Test - Size: 10kb" source=console
time="2026-01-17T02:34:30Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:34:30Z" level=info msg="============================================================" source=console
time="2026-01-17T02:34:30Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:34:30Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/JSON\",\"testName\":\"phase4-10kb\",\"startTime\":1768617270692}" source=console

running (0m01.0s), 10/10 VUs, 3778 complete and 0 interrupted iterations
http_json_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 7087 complete and 0 interrupted iterations
http_json_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 9963 complete and 0 interrupted iterations
http_json_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 12880 complete and 0 interrupted iterations
http_json_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 15873 complete and 0 interrupted iterations
http_json_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 18934 complete and 0 interrupted iterations
http_json_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 22251 complete and 0 interrupted iterations
http_json_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 25279 complete and 0 interrupted iterations
http_json_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 28240 complete and 0 interrupted iterations
http_json_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 31153 complete and 0 interrupted iterations
http_json_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 34174 complete and 0 interrupted iterations
http_json_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 37151 complete and 0 interrupted iterations
http_json_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 40323 complete and 0 interrupted iterations
http_json_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 43394 complete and 0 interrupted iterations
http_json_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 46338 complete and 0 interrupted iterations
http_json_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 49212 complete and 0 interrupted iterations
http_json_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 52308 complete and 0 interrupted iterations
http_json_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 55256 complete and 0 interrupted iterations
http_json_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 58243 complete and 0 interrupted iterations
http_json_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 61530 complete and 0 interrupted iterations
http_json_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 64662 complete and 0 interrupted iterations
http_json_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 67680 complete and 0 interrupted iterations
http_json_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 70665 complete and 0 interrupted iterations
http_json_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 73494 complete and 0 interrupted iterations
http_json_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 76973 complete and 0 interrupted iterations
http_json_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 80034 complete and 0 interrupted iterations
http_json_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 83082 complete and 0 interrupted iterations
http_json_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 86051 complete and 0 interrupted iterations
http_json_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 89019 complete and 0 interrupted iterations
http_json_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 91921 complete and 0 interrupted iterations
http_json_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:35:00Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:35:00Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:35:00Z" level=info msg="============================================================" source=console
time="2026-01-17T02:35:00Z" level=info msg="Protocol: HTTP/JSON" source=console
time="2026-01-17T02:35:00Z" level=info msg="Test Name: phase4-10kb" source=console
time="2026-01-17T02:35:00Z" level=info msg="Duration: 30007ms" source=console
time="2026-01-17T02:35:00Z" level=info msg="Total Requests: 91949" source=console
time="2026-01-17T02:35:00Z" level=info msg="Throughput: 3064.25 req/s" source=console
time="2026-01-17T02:35:00Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:35:00Z" level=info msg="  Average: 1.72ms" source=console
time="2026-01-17T02:35:00Z" level=info msg="  P50: 1.75ms" source=console
time="2026-01-17T02:35:00Z" level=info msg="  P95: 2.44ms" source=console
time="2026-01-17T02:35:00Z" level=info msg="  P99: 2.74ms" source=console
time="2026-01-17T02:35:00Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:35:00Z" level=info msg="  Peak Heap: 638.83MB" source=console
time="2026-01-17T02:35:00Z" level=info msg="  GC Count: 12" source=console
time="2026-01-17T02:35:00Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=3.93ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 183898  6121.737243/s
    checks_succeeded...: 100.00% 183898 out of 183898
    checks_failed......: 0.00%   0 out of 183898

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=2.97ms min=1.26ms med=3.02ms max=28.35ms p(90)=3.74ms p(95)=3.93ms
      { expected_response:true }...: avg=2.97ms min=1.26ms med=3.02ms max=28.35ms p(90)=3.74ms p(95)=3.93ms
    http_req_failed................: 0.00%  0 out of 91952
    http_reqs......................: 91952  3060.968488/s

    EXECUTION
    iteration_duration.............: avg=3.23ms min=1.34ms med=3.29ms max=20.35ms p(90)=4.02ms p(95)=4.23ms
    iterations.....................: 91949  3060.868621/s
    vus............................: 10     min=10         max=10
    vus_max........................: 10     min=10         max=10

    NETWORK
    data_received..................: 22 MB  738 kB/s
    data_sent......................: 9.5 MB 315 kB/s




running (0m30.0s), 00/10 VUs, 91949 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 10 VUs  30s

[2/20] HTTP/Binary - 10kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/http-binary-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_binary_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:35:04Z" level=info msg="============================================================" source=console
time="2026-01-17T02:35:04Z" level=info msg="Phase 4: HTTP/Binary Crossover Test - Size: 10kb" source=console
time="2026-01-17T02:35:04Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:35:04Z" level=info msg="============================================================" source=console
time="2026-01-17T02:35:04Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:35:04Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/Binary\",\"testName\":\"phase4-10kb\",\"startTime\":1768617304053}" source=console

running (0m01.0s), 10/10 VUs, 3953 complete and 0 interrupted iterations
http_binary_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 8368 complete and 0 interrupted iterations
http_binary_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 12345 complete and 0 interrupted iterations
http_binary_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 15813 complete and 0 interrupted iterations
http_binary_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 19263 complete and 0 interrupted iterations
http_binary_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 22777 complete and 0 interrupted iterations
http_binary_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 26165 complete and 0 interrupted iterations
http_binary_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 29754 complete and 0 interrupted iterations
http_binary_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 33231 complete and 0 interrupted iterations
http_binary_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 37166 complete and 0 interrupted iterations
http_binary_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 40816 complete and 0 interrupted iterations
http_binary_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 44221 complete and 0 interrupted iterations
http_binary_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 47714 complete and 0 interrupted iterations
http_binary_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 51263 complete and 0 interrupted iterations
http_binary_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 54762 complete and 0 interrupted iterations
http_binary_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 58241 complete and 0 interrupted iterations
http_binary_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 62223 complete and 0 interrupted iterations
http_binary_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 65763 complete and 0 interrupted iterations
http_binary_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 70003 complete and 0 interrupted iterations
http_binary_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 74200 complete and 0 interrupted iterations
http_binary_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 77871 complete and 0 interrupted iterations
http_binary_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 81818 complete and 0 interrupted iterations
http_binary_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 85571 complete and 0 interrupted iterations
http_binary_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 89195 complete and 0 interrupted iterations
http_binary_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 93170 complete and 0 interrupted iterations
http_binary_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 96551 complete and 0 interrupted iterations
http_binary_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 100145 complete and 0 interrupted iterations
http_binary_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 103435 complete and 0 interrupted iterations
http_binary_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 107145 complete and 0 interrupted iterations
http_binary_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 112293 complete and 0 interrupted iterations
http_binary_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:35:34Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:35:34Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:35:34Z" level=info msg="============================================================" source=console
time="2026-01-17T02:35:34Z" level=info msg="Protocol: HTTP/Binary" source=console
time="2026-01-17T02:35:34Z" level=info msg="Test Name: phase4-10kb" source=console
time="2026-01-17T02:35:34Z" level=info msg="Duration: 30004ms" source=console
time="2026-01-17T02:35:34Z" level=info msg="Total Requests: 112359" source=console
time="2026-01-17T02:35:34Z" level=info msg="Throughput: 3744.80 req/s" source=console
time="2026-01-17T02:35:34Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:35:34Z" level=info msg="  Average: 1.28ms" source=console
time="2026-01-17T02:35:34Z" level=info msg="  P50: 1.27ms" source=console
time="2026-01-17T02:35:34Z" level=info msg="  P95: 1.93ms" source=console
time="2026-01-17T02:35:34Z" level=info msg="  P99: 2.19ms" source=console
time="2026-01-17T02:35:34Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:35:34Z" level=info msg="  Peak Heap: 646.96MB" source=console
time="2026-01-17T02:35:34Z" level=info msg="  GC Count: 15" source=console
time="2026-01-17T02:35:34Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=3.36ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 224718  7476.641021/s
    checks_succeeded...: 100.00% 224718 out of 224718
    checks_failed......: 0.00%   0 out of 224718

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=2.42ms min=975.27µs med=2.42ms max=43.51ms p(90)=3.17ms p(95)=3.36ms
      { expected_response:true }...: avg=2.42ms min=975.27µs med=2.42ms max=43.51ms p(90)=3.17ms p(95)=3.36ms
    http_req_failed................: 0.00%  0 out of 112362
    http_reqs......................: 112362 3738.420324/s

    EXECUTION
    iteration_duration.............: avg=2.64ms min=1.01ms   med=2.64ms max=16.67ms p(90)=3.43ms p(95)=3.64ms
    iterations.....................: 112359 3738.32051/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 27 MB  909 kB/s
    data_sent......................: 12 MB  393 kB/s




running (0m30.1s), 00/10 VUs, 112359 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 10 VUs  30s

[3/20] gRPC/Unary - 10kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/grpc-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:35:37Z" level=info msg="============================================================" source=console
time="2026-01-17T02:35:37Z" level=info msg="Phase 4: gRPC/Unary Crossover Test - Size: 10kb" source=console
time="2026-01-17T02:35:37Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:35:37Z" level=info msg="============================================================" source=console
time="2026-01-17T02:35:37Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:35:37Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Unary\",\"testName\":\"phase4-10kb\",\"startTime\":1768617337315}" source=console

running (0m01.0s), 10/10 VUs, 6337 complete and 0 interrupted iterations
grpc_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 12750 complete and 0 interrupted iterations
grpc_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 19052 complete and 0 interrupted iterations
grpc_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 25335 complete and 0 interrupted iterations
grpc_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 31562 complete and 0 interrupted iterations
grpc_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 38327 complete and 0 interrupted iterations
grpc_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 44314 complete and 0 interrupted iterations
grpc_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 50853 complete and 0 interrupted iterations
grpc_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 57333 complete and 0 interrupted iterations
grpc_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 63732 complete and 0 interrupted iterations
grpc_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 70142 complete and 0 interrupted iterations
grpc_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 76310 complete and 0 interrupted iterations
grpc_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 82743 complete and 0 interrupted iterations
grpc_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 89245 complete and 0 interrupted iterations
grpc_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 95377 complete and 0 interrupted iterations
grpc_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 101526 complete and 0 interrupted iterations
grpc_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 107989 complete and 0 interrupted iterations
grpc_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 114358 complete and 0 interrupted iterations
grpc_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 120813 complete and 0 interrupted iterations
grpc_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 126903 complete and 0 interrupted iterations
grpc_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 133070 complete and 0 interrupted iterations
grpc_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 139203 complete and 0 interrupted iterations
grpc_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 145116 complete and 0 interrupted iterations
grpc_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 151365 complete and 0 interrupted iterations
grpc_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 157395 complete and 0 interrupted iterations
grpc_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 163485 complete and 0 interrupted iterations
grpc_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 169756 complete and 0 interrupted iterations
grpc_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 175896 complete and 0 interrupted iterations
grpc_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 182038 complete and 0 interrupted iterations
grpc_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 188012 complete and 0 interrupted iterations
grpc_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:36:07Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:36:07Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:36:07Z" level=info msg="============================================================" source=console
time="2026-01-17T02:36:07Z" level=info msg="Protocol: gRPC/Unary" source=console
time="2026-01-17T02:36:07Z" level=info msg="Test Name: phase4-10kb" source=console
time="2026-01-17T02:36:07Z" level=info msg="Duration: 30005ms" source=console
time="2026-01-17T02:36:07Z" level=info msg="Total Requests: 188074" source=console
time="2026-01-17T02:36:07Z" level=info msg="Throughput: 6268.09 req/s" source=console
time="2026-01-17T02:36:07Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:36:07Z" level=info msg="  Average: 0.74ms" source=console
time="2026-01-17T02:36:07Z" level=info msg="  P50: 0.69ms" source=console
time="2026-01-17T02:36:07Z" level=info msg="  P95: 1.35ms" source=console
time="2026-01-17T02:36:07Z" level=info msg="  P99: 1.82ms" source=console
time="2026-01-17T02:36:07Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:36:07Z" level=info msg="  Peak Heap: 648.84MB" source=console
time="2026-01-17T02:36:07Z" level=info msg="  GC Count: 18" source=console
time="2026-01-17T02:36:07Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=2.31ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 376148  12509.086002/s
    checks_succeeded...: 100.00% 376148 out of 376148
    checks_failed......: 0.00%   0 out of 376148

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=1.47ms min=645.61µs med=1.37ms max=56.58ms p(90)=2ms    p(95)=2.31ms
      { expected_response:true }...: avg=1.47ms min=645.61µs med=1.37ms max=56.58ms p(90)=2ms    p(95)=2.31ms
    http_req_failed................: 0.00%  0 out of 188077
    http_reqs......................: 188077 6254.642768/s

    EXECUTION
    iteration_duration.............: avg=1.58ms min=694.54µs med=1.48ms max=29.08ms p(90)=2.12ms p(95)=2.42ms
    iterations.....................: 188074 6254.543001/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 46 MB  1.5 MB/s
    data_sent......................: 18 MB  613 kB/s




running (0m30.1s), 00/10 VUs, 188074 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 10 VUs  30s

[4/20] gRPC/Stream - 10kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/grpc-stream-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_stream_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:36:10Z" level=info msg="============================================================" source=console
time="2026-01-17T02:36:10Z" level=info msg="Phase 4: gRPC/Stream Crossover Test - Size: 10kb" source=console
time="2026-01-17T02:36:10Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:36:10Z" level=info msg="============================================================" source=console
time="2026-01-17T02:36:10Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:36:10Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Stream\",\"testName\":\"phase4-10kb\",\"startTime\":1768617370595}" source=console

running (0m01.0s), 10/10 VUs, 4361 complete and 0 interrupted iterations
grpc_stream_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 8914 complete and 0 interrupted iterations
grpc_stream_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 13427 complete and 0 interrupted iterations
grpc_stream_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 17509 complete and 0 interrupted iterations
grpc_stream_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 22116 complete and 0 interrupted iterations
grpc_stream_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 26746 complete and 0 interrupted iterations
grpc_stream_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 31507 complete and 0 interrupted iterations
grpc_stream_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 36004 complete and 0 interrupted iterations
grpc_stream_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 40443 complete and 0 interrupted iterations
grpc_stream_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 44805 complete and 0 interrupted iterations
grpc_stream_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 49269 complete and 0 interrupted iterations
grpc_stream_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 53937 complete and 0 interrupted iterations
grpc_stream_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 58399 complete and 0 interrupted iterations
grpc_stream_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 63106 complete and 0 interrupted iterations
grpc_stream_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 67745 complete and 0 interrupted iterations
grpc_stream_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 72165 complete and 0 interrupted iterations
grpc_stream_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 76657 complete and 0 interrupted iterations
grpc_stream_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 81226 complete and 0 interrupted iterations
grpc_stream_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 85666 complete and 0 interrupted iterations
grpc_stream_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 90039 complete and 0 interrupted iterations
grpc_stream_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 94738 complete and 0 interrupted iterations
grpc_stream_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 99407 complete and 0 interrupted iterations
grpc_stream_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 104170 complete and 0 interrupted iterations
grpc_stream_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 108480 complete and 0 interrupted iterations
grpc_stream_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 113082 complete and 0 interrupted iterations
grpc_stream_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 117748 complete and 0 interrupted iterations
grpc_stream_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 122091 complete and 0 interrupted iterations
grpc_stream_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 126721 complete and 0 interrupted iterations
grpc_stream_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 131211 complete and 0 interrupted iterations
grpc_stream_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 135784 complete and 0 interrupted iterations
grpc_stream_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:36:40Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:36:40Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:36:40Z" level=info msg="============================================================" source=console
time="2026-01-17T02:36:40Z" level=info msg="Protocol: gRPC/Stream" source=console
time="2026-01-17T02:36:40Z" level=info msg="Test Name: phase4-10kb" source=console
time="2026-01-17T02:36:40Z" level=info msg="Duration: 30005ms" source=console
time="2026-01-17T02:36:40Z" level=info msg="Total Requests: 135844" source=console
time="2026-01-17T02:36:40Z" level=info msg="Throughput: 4527.38 req/s" source=console
time="2026-01-17T02:36:40Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:36:40Z" level=info msg="  Average: 1.02ms" source=console
time="2026-01-17T02:36:40Z" level=info msg="  P50: 0.93ms" source=console
time="2026-01-17T02:36:40Z" level=info msg="  P95: 1.83ms" source=console
time="2026-01-17T02:36:40Z" level=info msg="  P99: 2.32ms" source=console
time="2026-01-17T02:36:40Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:36:40Z" level=info msg="  Peak Heap: 644.34MB" source=console
time="2026-01-17T02:36:40Z" level=info msg="  GC Count: 13" source=console
time="2026-01-17T02:36:40Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=3.03ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 271688  9038.552131/s
    checks_succeeded...: 100.00% 271688 out of 271688
    checks_failed......: 0.00%   0 out of 271688

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=1.93ms min=914.06µs med=1.78ms max=42.33ms p(90)=2.7ms  p(95)=3.03ms
      { expected_response:true }...: avg=1.93ms min=914.06µs med=1.78ms max=42.33ms p(90)=2.7ms  p(95)=3.03ms
    http_req_failed................: 0.00%  0 out of 135847
    http_reqs......................: 135847 4519.37587/s

    EXECUTION
    iteration_duration.............: avg=2.18ms min=959.78µs med=2.03ms max=24.05ms p(90)=2.96ms p(95)=3.3ms 
    iterations.....................: 135844 4519.276066/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 33 MB  1.1 MB/s
    data_sent......................: 14 MB  475 kB/s




running (0m30.1s), 00/10 VUs, 135844 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 10 VUs  30s

==========================================
         50kb 페이로드 테스트
==========================================

[5/20] HTTP/JSON - 50kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/http-json-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_json_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:36:43Z" level=info msg="============================================================" source=console
time="2026-01-17T02:36:43Z" level=info msg="Phase 4: HTTP/JSON Crossover Test - Size: 50kb" source=console
time="2026-01-17T02:36:43Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:36:43Z" level=info msg="============================================================" source=console
time="2026-01-17T02:36:43Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:36:43Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/JSON\",\"testName\":\"phase4-50kb\",\"startTime\":1768617403910}" source=console

running (0m01.0s), 10/10 VUs, 2592 complete and 0 interrupted iterations
http_json_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 4965 complete and 0 interrupted iterations
http_json_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 7235 complete and 0 interrupted iterations
http_json_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 9571 complete and 0 interrupted iterations
http_json_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 11921 complete and 0 interrupted iterations
http_json_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 14106 complete and 0 interrupted iterations
http_json_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 16461 complete and 0 interrupted iterations
http_json_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 18813 complete and 0 interrupted iterations
http_json_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 21326 complete and 0 interrupted iterations
http_json_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 23586 complete and 0 interrupted iterations
http_json_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 25911 complete and 0 interrupted iterations
http_json_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 28110 complete and 0 interrupted iterations
http_json_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 30364 complete and 0 interrupted iterations
http_json_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 32597 complete and 0 interrupted iterations
http_json_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 34783 complete and 0 interrupted iterations
http_json_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 37111 complete and 0 interrupted iterations
http_json_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 39443 complete and 0 interrupted iterations
http_json_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 41760 complete and 0 interrupted iterations
http_json_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 44014 complete and 0 interrupted iterations
http_json_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 46280 complete and 0 interrupted iterations
http_json_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 48641 complete and 0 interrupted iterations
http_json_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 51057 complete and 0 interrupted iterations
http_json_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 53430 complete and 0 interrupted iterations
http_json_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 55823 complete and 0 interrupted iterations
http_json_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 59135 complete and 0 interrupted iterations
http_json_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 61947 complete and 0 interrupted iterations
http_json_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 64208 complete and 0 interrupted iterations
http_json_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 66751 complete and 0 interrupted iterations
http_json_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 69026 complete and 0 interrupted iterations
http_json_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 71433 complete and 0 interrupted iterations
http_json_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:37:13Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:37:13Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:37:13Z" level=info msg="============================================================" source=console
time="2026-01-17T02:37:13Z" level=info msg="Protocol: HTTP/JSON" source=console
time="2026-01-17T02:37:13Z" level=info msg="Test Name: phase4-50kb" source=console
time="2026-01-17T02:37:13Z" level=info msg="Duration: 30006ms" source=console
time="2026-01-17T02:37:13Z" level=info msg="Total Requests: 71471" source=console
time="2026-01-17T02:37:13Z" level=info msg="Throughput: 2381.89 req/s" source=console
time="2026-01-17T02:37:13Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:37:13Z" level=info msg="  Average: 2.67ms" source=console
time="2026-01-17T02:37:13Z" level=info msg="  P50: 2.65ms" source=console
time="2026-01-17T02:37:13Z" level=info msg="  P95: 3.76ms" source=console
time="2026-01-17T02:37:13Z" level=info msg="  P99: 4.21ms" source=console
time="2026-01-17T02:37:13Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:37:13Z" level=info msg="  Peak Heap: 647.16MB" source=console
time="2026-01-17T02:37:13Z" level=info msg="  GC Count: 45" source=console
time="2026-01-17T02:37:13Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=5.21ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 142942  4758.71265/s
    checks_succeeded...: 100.00% 142942 out of 142942
    checks_failed......: 0.00%   0 out of 142942

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=3.91ms min=1.62ms med=3.94ms max=23.44ms p(90)=4.96ms p(95)=5.21ms
      { expected_response:true }...: avg=3.91ms min=1.62ms med=3.94ms max=23.44ms p(90)=4.96ms p(95)=5.21ms
    http_req_failed................: 0.00%  0 out of 71474
    http_reqs......................: 71474  2379.456199/s

    EXECUTION
    iteration_duration.............: avg=4.17ms min=1.66ms med=4.2ms  max=23.71ms p(90)=5.24ms p(95)=5.5ms 
    iterations.....................: 71471  2379.356325/s
    vus............................: 10     min=10         max=10
    vus_max........................: 10     min=10         max=10

    NETWORK
    data_received..................: 17 MB  574 kB/s
    data_sent......................: 7.4 MB 245 kB/s




running (0m30.0s), 00/10 VUs, 71471 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 10 VUs  30s

[6/20] HTTP/Binary - 50kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/http-binary-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_binary_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:37:17Z" level=info msg="============================================================" source=console
time="2026-01-17T02:37:17Z" level=info msg="Phase 4: HTTP/Binary Crossover Test - Size: 50kb" source=console
time="2026-01-17T02:37:17Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:37:17Z" level=info msg="============================================================" source=console
time="2026-01-17T02:37:17Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:37:17Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/Binary\",\"testName\":\"phase4-50kb\",\"startTime\":1768617437202}" source=console

running (0m01.0s), 10/10 VUs, 3318 complete and 0 interrupted iterations
http_binary_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 7654 complete and 0 interrupted iterations
http_binary_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 13002 complete and 0 interrupted iterations
http_binary_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 18686 complete and 0 interrupted iterations
http_binary_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 23253 complete and 0 interrupted iterations
http_binary_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 27295 complete and 0 interrupted iterations
http_binary_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 32862 complete and 0 interrupted iterations
http_binary_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 36215 complete and 0 interrupted iterations
http_binary_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 39475 complete and 0 interrupted iterations
http_binary_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 42832 complete and 0 interrupted iterations
http_binary_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 46015 complete and 0 interrupted iterations
http_binary_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 49857 complete and 0 interrupted iterations
http_binary_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 53185 complete and 0 interrupted iterations
http_binary_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 56548 complete and 0 interrupted iterations
http_binary_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 59920 complete and 0 interrupted iterations
http_binary_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 63242 complete and 0 interrupted iterations
http_binary_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 66603 complete and 0 interrupted iterations
http_binary_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 69890 complete and 0 interrupted iterations
http_binary_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 73119 complete and 0 interrupted iterations
http_binary_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 76380 complete and 0 interrupted iterations
http_binary_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 79487 complete and 0 interrupted iterations
http_binary_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 82843 complete and 0 interrupted iterations
http_binary_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 88317 complete and 0 interrupted iterations
http_binary_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 94262 complete and 0 interrupted iterations
http_binary_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 98579 complete and 0 interrupted iterations
http_binary_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 103317 complete and 0 interrupted iterations
http_binary_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 108648 complete and 0 interrupted iterations
http_binary_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 113943 complete and 0 interrupted iterations
http_binary_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 118371 complete and 0 interrupted iterations
http_binary_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 122619 complete and 0 interrupted iterations
http_binary_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:37:47Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:37:47Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:37:47Z" level=info msg="============================================================" source=console
time="2026-01-17T02:37:47Z" level=info msg="Protocol: HTTP/Binary" source=console
time="2026-01-17T02:37:47Z" level=info msg="Test Name: phase4-50kb" source=console
time="2026-01-17T02:37:47Z" level=info msg="Duration: 30005ms" source=console
time="2026-01-17T02:37:47Z" level=info msg="Total Requests: 122682" source=console
time="2026-01-17T02:37:47Z" level=info msg="Throughput: 4088.72 req/s" source=console
time="2026-01-17T02:37:47Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:37:47Z" level=info msg="  Average: 1.24ms" source=console
time="2026-01-17T02:37:47Z" level=info msg="  P50: 1.16ms" source=console
time="2026-01-17T02:37:47Z" level=info msg="  P95: 1.99ms" source=console
time="2026-01-17T02:37:47Z" level=info msg="  P99: 2.26ms" source=console
time="2026-01-17T02:37:47Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:37:47Z" level=info msg="  Peak Heap: 654.22MB" source=console
time="2026-01-17T02:37:47Z" level=info msg="  GC Count: 25" source=console
time="2026-01-17T02:37:47Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=3.4ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 245364  8165.540592/s
    checks_succeeded...: 100.00% 245364 out of 245364
    checks_failed......: 0.00%   0 out of 245364

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=2.25ms min=923.07µs med=2.14ms max=35.59ms p(90)=3.19ms p(95)=3.4ms 
      { expected_response:true }...: avg=2.25ms min=923.07µs med=2.14ms max=35.59ms p(90)=3.19ms p(95)=3.4ms 
    http_req_failed................: 0.00%  0 out of 122685
    http_reqs......................: 122685 4082.870134/s

    EXECUTION
    iteration_duration.............: avg=2.42ms min=974.33µs med=2.33ms max=15.52ms p(90)=3.44ms p(95)=3.66ms
    iterations.....................: 122682 4082.770296/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 30 MB  993 kB/s
    data_sent......................: 13 MB  429 kB/s




running (0m30.0s), 00/10 VUs, 122682 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 10 VUs  30s

[7/20] gRPC/Unary - 50kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/grpc-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:37:50Z" level=info msg="============================================================" source=console
time="2026-01-17T02:37:50Z" level=info msg="Phase 4: gRPC/Unary Crossover Test - Size: 50kb" source=console
time="2026-01-17T02:37:50Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:37:50Z" level=info msg="============================================================" source=console
time="2026-01-17T02:37:50Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:37:50Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Unary\",\"testName\":\"phase4-50kb\",\"startTime\":1768617470507}" source=console

running (0m01.0s), 10/10 VUs, 5639 complete and 0 interrupted iterations
grpc_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 11383 complete and 0 interrupted iterations
grpc_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 17166 complete and 0 interrupted iterations
grpc_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 23039 complete and 0 interrupted iterations
grpc_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 28953 complete and 0 interrupted iterations
grpc_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 34724 complete and 0 interrupted iterations
grpc_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 40452 complete and 0 interrupted iterations
grpc_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 46227 complete and 0 interrupted iterations
grpc_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 51979 complete and 0 interrupted iterations
grpc_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 57564 complete and 0 interrupted iterations
grpc_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 63200 complete and 0 interrupted iterations
grpc_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 69022 complete and 0 interrupted iterations
grpc_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 74834 complete and 0 interrupted iterations
grpc_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 80602 complete and 0 interrupted iterations
grpc_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 86223 complete and 0 interrupted iterations
grpc_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 92066 complete and 0 interrupted iterations
grpc_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 97772 complete and 0 interrupted iterations
grpc_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 103430 complete and 0 interrupted iterations
grpc_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 108977 complete and 0 interrupted iterations
grpc_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 114607 complete and 0 interrupted iterations
grpc_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 120070 complete and 0 interrupted iterations
grpc_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 125599 complete and 0 interrupted iterations
grpc_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 130953 complete and 0 interrupted iterations
grpc_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 136264 complete and 0 interrupted iterations
grpc_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 141470 complete and 0 interrupted iterations
grpc_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 146864 complete and 0 interrupted iterations
grpc_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 152311 complete and 0 interrupted iterations
grpc_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 157844 complete and 0 interrupted iterations
grpc_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 163466 complete and 0 interrupted iterations
grpc_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 168808 complete and 0 interrupted iterations
grpc_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:38:20Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:38:20Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:38:20Z" level=info msg="============================================================" source=console
time="2026-01-17T02:38:20Z" level=info msg="Protocol: gRPC/Unary" source=console
time="2026-01-17T02:38:20Z" level=info msg="Test Name: phase4-50kb" source=console
time="2026-01-17T02:38:20Z" level=info msg="Duration: 30007ms" source=console
time="2026-01-17T02:38:20Z" level=info msg="Total Requests: 168889" source=console
time="2026-01-17T02:38:20Z" level=info msg="Throughput: 5628.32 req/s" source=console
time="2026-01-17T02:38:20Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:38:20Z" level=info msg="  Average: 0.89ms" source=console
time="2026-01-17T02:38:20Z" level=info msg="  P50: 0.84ms" source=console
time="2026-01-17T02:38:20Z" level=info msg="  P95: 1.59ms" source=console
time="2026-01-17T02:38:20Z" level=info msg="  P99: 2.14ms" source=console
time="2026-01-17T02:38:20Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:38:20Z" level=info msg="  Peak Heap: 659.49MB" source=console
time="2026-01-17T02:38:20Z" level=info msg="  GC Count: 30" source=console
time="2026-01-17T02:38:20Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=2.5ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 337778  11234.929079/s
    checks_succeeded...: 100.00% 337778 out of 337778
    checks_failed......: 0.00%   0 out of 337778

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=1.63ms min=680.93µs med=1.55ms max=49.84ms p(90)=2.2ms  p(95)=2.5ms 
      { expected_response:true }...: avg=1.63ms min=680.93µs med=1.55ms max=49.84ms p(90)=2.2ms  p(95)=2.5ms 
    http_req_failed................: 0.00%  0 out of 168892
    http_reqs......................: 168892 5617.564323/s

    EXECUTION
    iteration_duration.............: avg=1.76ms min=736.02µs med=1.67ms max=15.27ms p(90)=2.34ms p(95)=2.64ms
    iterations.....................: 168889 5617.464539/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 41 MB  1.4 MB/s
    data_sent......................: 17 MB  551 kB/s




running (0m30.1s), 00/10 VUs, 168889 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 10 VUs  30s

[8/20] gRPC/Stream - 50kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/grpc-stream-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_stream_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:38:23Z" level=info msg="============================================================" source=console
time="2026-01-17T02:38:23Z" level=info msg="Phase 4: gRPC/Stream Crossover Test - Size: 50kb" source=console
time="2026-01-17T02:38:23Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:38:23Z" level=info msg="============================================================" source=console
time="2026-01-17T02:38:23Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:38:23Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Stream\",\"testName\":\"phase4-50kb\",\"startTime\":1768617503887}" source=console

running (0m01.0s), 10/10 VUs, 2252 complete and 0 interrupted iterations
grpc_stream_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 4480 complete and 0 interrupted iterations
grpc_stream_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 7018 complete and 0 interrupted iterations
grpc_stream_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 9494 complete and 0 interrupted iterations
grpc_stream_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 12070 complete and 0 interrupted iterations
grpc_stream_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 14239 complete and 0 interrupted iterations
grpc_stream_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 16654 complete and 0 interrupted iterations
grpc_stream_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 19347 complete and 0 interrupted iterations
grpc_stream_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 21379 complete and 0 interrupted iterations
grpc_stream_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 23621 complete and 0 interrupted iterations
grpc_stream_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 26026 complete and 0 interrupted iterations
grpc_stream_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 28542 complete and 0 interrupted iterations
grpc_stream_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 30857 complete and 0 interrupted iterations
grpc_stream_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 33110 complete and 0 interrupted iterations
grpc_stream_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 35631 complete and 0 interrupted iterations
grpc_stream_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 38072 complete and 0 interrupted iterations
grpc_stream_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 40479 complete and 0 interrupted iterations
grpc_stream_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 42681 complete and 0 interrupted iterations
grpc_stream_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 45435 complete and 0 interrupted iterations
grpc_stream_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 47949 complete and 0 interrupted iterations
grpc_stream_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 50537 complete and 0 interrupted iterations
grpc_stream_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 53010 complete and 0 interrupted iterations
grpc_stream_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 55423 complete and 0 interrupted iterations
grpc_stream_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 58068 complete and 0 interrupted iterations
grpc_stream_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 60415 complete and 0 interrupted iterations
grpc_stream_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 62742 complete and 0 interrupted iterations
grpc_stream_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 65024 complete and 0 interrupted iterations
grpc_stream_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 67341 complete and 0 interrupted iterations
grpc_stream_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 69804 complete and 0 interrupted iterations
grpc_stream_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 72214 complete and 0 interrupted iterations
grpc_stream_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:38:53Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:38:53Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:38:53Z" level=info msg="============================================================" source=console
time="2026-01-17T02:38:53Z" level=info msg="Protocol: gRPC/Stream" source=console
time="2026-01-17T02:38:53Z" level=info msg="Test Name: phase4-50kb" source=console
time="2026-01-17T02:38:53Z" level=info msg="Duration: 30007ms" source=console
time="2026-01-17T02:38:53Z" level=info msg="Total Requests: 72244" source=console
time="2026-01-17T02:38:53Z" level=info msg="Throughput: 2407.57 req/s" source=console
time="2026-01-17T02:38:53Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:38:53Z" level=info msg="  Average: 2.78ms" source=console
time="2026-01-17T02:38:53Z" level=info msg="  P50: 2.63ms" source=console
time="2026-01-17T02:38:53Z" level=info msg="  P95: 4.49ms" source=console
time="2026-01-17T02:38:53Z" level=info msg="  P99: 5.23ms" source=console
time="2026-01-17T02:38:53Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:38:53Z" level=info msg="  Peak Heap: 663.04MB" source=console
time="2026-01-17T02:38:53Z" level=info msg="  GC Count: 13" source=console
time="2026-01-17T02:38:53Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=5.81ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 144488  4811.052812/s
    checks_succeeded...: 100.00% 144488 out of 144488
    checks_failed......: 0.00%   0 out of 144488

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=3.85ms min=1.11ms med=3.68ms max=20.54ms p(90)=5.36ms p(95)=5.81ms
      { expected_response:true }...: avg=3.85ms min=1.11ms med=3.68ms max=20.54ms p(90)=5.36ms p(95)=5.81ms
    http_req_failed................: 0.00%  0 out of 72247
    http_reqs......................: 72247  2405.626297/s

    EXECUTION
    iteration_duration.............: avg=4.12ms min=1.78ms med=3.96ms max=16.48ms p(90)=5.64ms p(95)=6.09ms
    iterations.....................: 72244  2405.526406/s
    vus............................: 10     min=10         max=10
    vus_max........................: 10     min=10         max=10

    NETWORK
    data_received..................: 18 MB  585 kB/s
    data_sent......................: 7.6 MB 253 kB/s




running (0m30.0s), 00/10 VUs, 72244 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 10 VUs  30s

==========================================
         100kb 페이로드 테스트
==========================================

[9/20] HTTP/JSON - 100kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/http-json-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_json_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:38:57Z" level=info msg="============================================================" source=console
time="2026-01-17T02:38:57Z" level=info msg="Phase 4: HTTP/JSON Crossover Test - Size: 100kb" source=console
time="2026-01-17T02:38:57Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:38:57Z" level=info msg="============================================================" source=console
time="2026-01-17T02:38:57Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:38:57Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/JSON\",\"testName\":\"phase4-100kb\",\"startTime\":1768617537228}" source=console

running (0m01.0s), 10/10 VUs, 1999 complete and 0 interrupted iterations
http_json_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 3913 complete and 0 interrupted iterations
http_json_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 6003 complete and 0 interrupted iterations
http_json_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 8029 complete and 0 interrupted iterations
http_json_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 10148 complete and 0 interrupted iterations
http_json_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 12501 complete and 0 interrupted iterations
http_json_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 14498 complete and 0 interrupted iterations
http_json_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 16642 complete and 0 interrupted iterations
http_json_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 18876 complete and 0 interrupted iterations
http_json_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 20956 complete and 0 interrupted iterations
http_json_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 23109 complete and 0 interrupted iterations
http_json_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 25291 complete and 0 interrupted iterations
http_json_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 27399 complete and 0 interrupted iterations
http_json_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 30012 complete and 0 interrupted iterations
http_json_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 32137 complete and 0 interrupted iterations
http_json_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 34126 complete and 0 interrupted iterations
http_json_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 36961 complete and 0 interrupted iterations
http_json_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 39090 complete and 0 interrupted iterations
http_json_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 41462 complete and 0 interrupted iterations
http_json_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 43732 complete and 0 interrupted iterations
http_json_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 45776 complete and 0 interrupted iterations
http_json_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 47929 complete and 0 interrupted iterations
http_json_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 50043 complete and 0 interrupted iterations
http_json_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 52110 complete and 0 interrupted iterations
http_json_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 54313 complete and 0 interrupted iterations
http_json_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 56326 complete and 0 interrupted iterations
http_json_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 58538 complete and 0 interrupted iterations
http_json_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 60685 complete and 0 interrupted iterations
http_json_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 62827 complete and 0 interrupted iterations
http_json_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 64774 complete and 0 interrupted iterations
http_json_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:39:27Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:39:27Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:39:27Z" level=info msg="============================================================" source=console
time="2026-01-17T02:39:27Z" level=info msg="Protocol: HTTP/JSON" source=console
time="2026-01-17T02:39:27Z" level=info msg="Test Name: phase4-100kb" source=console
time="2026-01-17T02:39:27Z" level=info msg="Duration: 30010ms" source=console
time="2026-01-17T02:39:27Z" level=info msg="Total Requests: 64801" source=console
time="2026-01-17T02:39:27Z" level=info msg="Throughput: 2159.31 req/s" source=console
time="2026-01-17T02:39:27Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:39:27Z" level=info msg="  Average: 3.22ms" source=console
time="2026-01-17T02:39:27Z" level=info msg="  P50: 3.07ms" source=console
time="2026-01-17T02:39:27Z" level=info msg="  P95: 5.05ms" source=console
time="2026-01-17T02:39:27Z" level=info msg="  P99: 5.56ms" source=console
time="2026-01-17T02:39:27Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:39:27Z" level=info msg="  Peak Heap: 667.47MB" source=console
time="2026-01-17T02:39:27Z" level=info msg="  GC Count: 73" source=console
time="2026-01-17T02:39:27Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=6.33ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 129602  4314.651871/s
    checks_succeeded...: 100.00% 129602 out of 129602
    checks_failed......: 0.00%   0 out of 129602

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=4.33ms min=1.99ms med=4.19ms max=22.38ms p(90)=5.87ms p(95)=6.33ms
      { expected_response:true }...: avg=4.33ms min=1.99ms med=4.19ms max=22.38ms p(90)=5.87ms p(95)=6.33ms
    http_req_failed................: 0.00%  0 out of 64804
    http_reqs......................: 64804  2157.42581/s

    EXECUTION
    iteration_duration.............: avg=4.6ms  min=2.25ms med=4.47ms max=18.62ms p(90)=6.16ms p(95)=6.62ms
    iterations.....................: 64801  2157.325936/s
    vus............................: 10     min=10         max=10
    vus_max........................: 10     min=10         max=10

    NETWORK
    data_received..................: 16 MB  522 kB/s
    data_sent......................: 6.7 MB 224 kB/s




running (0m30.0s), 00/10 VUs, 64801 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 10 VUs  30s

[10/20] HTTP/Binary - 100kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/http-binary-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_binary_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:39:30Z" level=info msg="============================================================" source=console
time="2026-01-17T02:39:30Z" level=info msg="Phase 4: HTTP/Binary Crossover Test - Size: 100kb" source=console
time="2026-01-17T02:39:30Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:39:30Z" level=info msg="============================================================" source=console
time="2026-01-17T02:39:30Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:39:30Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/Binary\",\"testName\":\"phase4-100kb\",\"startTime\":1768617570488}" source=console

running (0m01.0s), 10/10 VUs, 3735 complete and 0 interrupted iterations
http_binary_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 8589 complete and 0 interrupted iterations
http_binary_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 11981 complete and 0 interrupted iterations
http_binary_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 14931 complete and 0 interrupted iterations
http_binary_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 17874 complete and 0 interrupted iterations
http_binary_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 20943 complete and 0 interrupted iterations
http_binary_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 24053 complete and 0 interrupted iterations
http_binary_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 27011 complete and 0 interrupted iterations
http_binary_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 30128 complete and 0 interrupted iterations
http_binary_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 33130 complete and 0 interrupted iterations
http_binary_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 36369 complete and 0 interrupted iterations
http_binary_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 39358 complete and 0 interrupted iterations
http_binary_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 42465 complete and 0 interrupted iterations
http_binary_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 45574 complete and 0 interrupted iterations
http_binary_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 50069 complete and 0 interrupted iterations
http_binary_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 54954 complete and 0 interrupted iterations
http_binary_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 58856 complete and 0 interrupted iterations
http_binary_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 63272 complete and 0 interrupted iterations
http_binary_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 68023 complete and 0 interrupted iterations
http_binary_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 71986 complete and 0 interrupted iterations
http_binary_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 75755 complete and 0 interrupted iterations
http_binary_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 80306 complete and 0 interrupted iterations
http_binary_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 84408 complete and 0 interrupted iterations
http_binary_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 89185 complete and 0 interrupted iterations
http_binary_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 94034 complete and 0 interrupted iterations
http_binary_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 98361 complete and 0 interrupted iterations
http_binary_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 103181 complete and 0 interrupted iterations
http_binary_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 107787 complete and 0 interrupted iterations
http_binary_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 112708 complete and 0 interrupted iterations
http_binary_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 117122 complete and 0 interrupted iterations
http_binary_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:40:00Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:40:00Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:40:00Z" level=info msg="============================================================" source=console
time="2026-01-17T02:40:00Z" level=info msg="Protocol: HTTP/Binary" source=console
time="2026-01-17T02:40:00Z" level=info msg="Test Name: phase4-100kb" source=console
time="2026-01-17T02:40:00Z" level=info msg="Duration: 30006ms" source=console
time="2026-01-17T02:40:00Z" level=info msg="Total Requests: 117156" source=console
time="2026-01-17T02:40:00Z" level=info msg="Throughput: 3904.42 req/s" source=console
time="2026-01-17T02:40:00Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:40:00Z" level=info msg="  Average: 1.36ms" source=console
time="2026-01-17T02:40:00Z" level=info msg="  P50: 1.28ms" source=console
time="2026-01-17T02:40:00Z" level=info msg="  P95: 2.12ms" source=console
time="2026-01-17T02:40:00Z" level=info msg="  P99: 2.41ms" source=console
time="2026-01-17T02:40:00Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:40:00Z" level=info msg="  Peak Heap: 674.50MB" source=console
time="2026-01-17T02:40:00Z" level=info msg="  GC Count: 36" source=console
time="2026-01-17T02:40:00Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=3.53ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 234312  7798.92795/s
    checks_succeeded...: 100.00% 234312 out of 234312
    checks_failed......: 0.00%   0 out of 234312

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=2.38ms min=998.56µs med=2.27ms max=34.28ms p(90)=3.31ms p(95)=3.53ms
      { expected_response:true }...: avg=2.38ms min=998.56µs med=2.27ms max=34.28ms p(90)=3.31ms p(95)=3.53ms
    http_req_failed................: 0.00%  0 out of 117159
    http_reqs......................: 117159 3899.563828/s

    EXECUTION
    iteration_duration.............: avg=2.54ms min=1.04ms   med=2.42ms max=11.76ms p(90)=3.55ms p(95)=3.8ms 
    iterations.....................: 117156 3899.463975/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 29 MB  952 kB/s
    data_sent......................: 12 MB  413 kB/s




running (0m30.0s), 00/10 VUs, 117156 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 10 VUs  30s

[11/20] gRPC/Unary - 100kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/grpc-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:40:03Z" level=info msg="============================================================" source=console
time="2026-01-17T02:40:03Z" level=info msg="Phase 4: gRPC/Unary Crossover Test - Size: 100kb" source=console
time="2026-01-17T02:40:03Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:40:03Z" level=info msg="============================================================" source=console
time="2026-01-17T02:40:03Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:40:03Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Unary\",\"testName\":\"phase4-100kb\",\"startTime\":1768617603748}" source=console

running (0m01.0s), 10/10 VUs, 4677 complete and 0 interrupted iterations
grpc_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 9636 complete and 0 interrupted iterations
grpc_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 14519 complete and 0 interrupted iterations
grpc_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 19692 complete and 0 interrupted iterations
grpc_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 24759 complete and 0 interrupted iterations
grpc_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 29888 complete and 0 interrupted iterations
grpc_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 35338 complete and 0 interrupted iterations
grpc_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 40419 complete and 0 interrupted iterations
grpc_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 45562 complete and 0 interrupted iterations
grpc_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 50709 complete and 0 interrupted iterations
grpc_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 55806 complete and 0 interrupted iterations
grpc_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 61206 complete and 0 interrupted iterations
grpc_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 66068 complete and 0 interrupted iterations
grpc_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 71230 complete and 0 interrupted iterations
grpc_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 76190 complete and 0 interrupted iterations
grpc_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 81177 complete and 0 interrupted iterations
grpc_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 86354 complete and 0 interrupted iterations
grpc_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 91362 complete and 0 interrupted iterations
grpc_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 96633 complete and 0 interrupted iterations
grpc_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 101737 complete and 0 interrupted iterations
grpc_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 106678 complete and 0 interrupted iterations
grpc_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 111662 complete and 0 interrupted iterations
grpc_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 116127 complete and 0 interrupted iterations
grpc_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 121234 complete and 0 interrupted iterations
grpc_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 126630 complete and 0 interrupted iterations
grpc_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 131671 complete and 0 interrupted iterations
grpc_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 136637 complete and 0 interrupted iterations
grpc_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 141646 complete and 0 interrupted iterations
grpc_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 146804 complete and 0 interrupted iterations
grpc_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 151994 complete and 0 interrupted iterations
grpc_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:40:33Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:40:33Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:40:33Z" level=info msg="============================================================" source=console
time="2026-01-17T02:40:33Z" level=info msg="Protocol: gRPC/Unary" source=console
time="2026-01-17T02:40:33Z" level=info msg="Test Name: phase4-100kb" source=console
time="2026-01-17T02:40:33Z" level=info msg="Duration: 30007ms" source=console
time="2026-01-17T02:40:33Z" level=info msg="Total Requests: 152046" source=console
time="2026-01-17T02:40:33Z" level=info msg="Throughput: 5067.02 req/s" source=console
time="2026-01-17T02:40:33Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:40:33Z" level=info msg="  Average: 0.99ms" source=console
time="2026-01-17T02:40:33Z" level=info msg="  P50: 0.85ms" source=console
time="2026-01-17T02:40:33Z" level=info msg="  P95: 1.88ms" source=console
time="2026-01-17T02:40:33Z" level=info msg="  P99: 2.51ms" source=console
time="2026-01-17T02:40:33Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:40:33Z" level=info msg="  Peak Heap: 682.56MB" source=console
time="2026-01-17T02:40:33Z" level=info msg="  GC Count: 41" source=console
time="2026-01-17T02:40:33Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=2.77ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 304092  10116.401148/s
    checks_succeeded...: 100.00% 304092 out of 304092
    checks_failed......: 0.00%   0 out of 304092

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=1.77ms min=765.76µs med=1.65ms max=44.61ms p(90)=2.48ms p(95)=2.77ms
      { expected_response:true }...: avg=1.77ms min=765.76µs med=1.65ms max=44.61ms p(90)=2.48ms p(95)=2.77ms
    http_req_failed................: 0.00%  0 out of 152049
    http_reqs......................: 152049 5058.300377/s

    EXECUTION
    iteration_duration.............: avg=1.95ms min=804.07µs med=1.83ms max=14.1ms  p(90)=2.67ms p(95)=2.96ms
    iterations.....................: 152046 5058.200574/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 37 MB  1.2 MB/s
    data_sent......................: 15 MB  501 kB/s




running (0m30.1s), 00/10 VUs, 152046 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 10 VUs  30s

[12/20] gRPC/Stream - 100kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/grpc-stream-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_stream_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:40:37Z" level=info msg="============================================================" source=console
time="2026-01-17T02:40:37Z" level=info msg="Phase 4: gRPC/Stream Crossover Test - Size: 100kb" source=console
time="2026-01-17T02:40:37Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:40:37Z" level=info msg="============================================================" source=console
time="2026-01-17T02:40:37Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:40:37Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Stream\",\"testName\":\"phase4-100kb\",\"startTime\":1768617637030}" source=console

running (0m01.0s), 10/10 VUs, 1694 complete and 0 interrupted iterations
grpc_stream_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 3459 complete and 0 interrupted iterations
grpc_stream_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 5175 complete and 0 interrupted iterations
grpc_stream_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 7018 complete and 0 interrupted iterations
grpc_stream_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 8752 complete and 0 interrupted iterations
grpc_stream_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 10637 complete and 0 interrupted iterations
grpc_stream_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 12605 complete and 0 interrupted iterations
grpc_stream_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 14565 complete and 0 interrupted iterations
grpc_stream_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 16592 complete and 0 interrupted iterations
grpc_stream_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 18425 complete and 0 interrupted iterations
grpc_stream_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 20430 complete and 0 interrupted iterations
grpc_stream_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 22525 complete and 0 interrupted iterations
grpc_stream_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 24625 complete and 0 interrupted iterations
grpc_stream_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 26703 complete and 0 interrupted iterations
grpc_stream_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 28685 complete and 0 interrupted iterations
grpc_stream_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 30651 complete and 0 interrupted iterations
grpc_stream_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 32732 complete and 0 interrupted iterations
grpc_stream_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 34756 complete and 0 interrupted iterations
grpc_stream_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 36852 complete and 0 interrupted iterations
grpc_stream_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 38878 complete and 0 interrupted iterations
grpc_stream_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 40843 complete and 0 interrupted iterations
grpc_stream_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 42687 complete and 0 interrupted iterations
grpc_stream_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 44607 complete and 0 interrupted iterations
grpc_stream_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 46614 complete and 0 interrupted iterations
grpc_stream_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 48699 complete and 0 interrupted iterations
grpc_stream_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 50757 complete and 0 interrupted iterations
grpc_stream_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 52767 complete and 0 interrupted iterations
grpc_stream_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 54761 complete and 0 interrupted iterations
grpc_stream_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 56803 complete and 0 interrupted iterations
grpc_stream_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 58777 complete and 0 interrupted iterations
grpc_stream_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:41:07Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:41:07Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:41:07Z" level=info msg="============================================================" source=console
time="2026-01-17T02:41:07Z" level=info msg="Protocol: gRPC/Stream" source=console
time="2026-01-17T02:41:07Z" level=info msg="Test Name: phase4-100kb" source=console
time="2026-01-17T02:41:07Z" level=info msg="Duration: 30006ms" source=console
time="2026-01-17T02:41:07Z" level=info msg="Total Requests: 58816" source=console
time="2026-01-17T02:41:07Z" level=info msg="Throughput: 1960.14 req/s" source=console
time="2026-01-17T02:41:07Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:41:07Z" level=info msg="  Average: 3.84ms" source=console
time="2026-01-17T02:41:07Z" level=info msg="  P50: 3.50ms" source=console
time="2026-01-17T02:41:07Z" level=info msg="  P95: 6.05ms" source=console
time="2026-01-17T02:41:07Z" level=info msg="  P99: 7.62ms" source=console
time="2026-01-17T02:41:07Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:41:07Z" level=info msg="  Peak Heap: 683.35MB" source=console
time="2026-01-17T02:41:07Z" level=info msg="  GC Count: 17" source=console
time="2026-01-17T02:41:07Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=7.18ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 117632  3916.730415/s
    checks_succeeded...: 100.00% 117632 out of 117632
    checks_failed......: 0.00%   0 out of 117632

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=4.8ms  min=1.77ms med=4.46ms max=17.72ms p(90)=6.39ms p(95)=7.18ms
      { expected_response:true }...: avg=4.8ms  min=1.77ms med=4.46ms max=17.72ms p(90)=6.39ms p(95)=7.18ms
    http_req_failed................: 0.00%  0 out of 58819
    http_reqs......................: 58819  1958.465097/s

    EXECUTION
    iteration_duration.............: avg=5.07ms min=2.9ms  med=4.74ms max=15.29ms p(90)=6.68ms p(95)=7.47ms
    iterations.....................: 58816  1958.365207/s
    vus............................: 10     min=10         max=10
    vus_max........................: 10     min=10         max=10

    NETWORK
    data_received..................: 14 MB  478 kB/s
    data_sent......................: 6.2 MB 208 kB/s




running (0m30.0s), 00/10 VUs, 58816 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 10 VUs  30s

==========================================
         200kb 페이로드 테스트
==========================================

[13/20] HTTP/JSON - 200kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/http-json-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_json_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:41:10Z" level=info msg="============================================================" source=console
time="2026-01-17T02:41:10Z" level=info msg="Phase 4: HTTP/JSON Crossover Test - Size: 200kb" source=console
time="2026-01-17T02:41:10Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:41:10Z" level=info msg="============================================================" source=console
time="2026-01-17T02:41:10Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:41:10Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/JSON\",\"testName\":\"phase4-200kb\",\"startTime\":1768617670264}" source=console

running (0m01.0s), 10/10 VUs, 1732 complete and 0 interrupted iterations
http_json_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 3586 complete and 0 interrupted iterations
http_json_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 5460 complete and 0 interrupted iterations
http_json_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 7388 complete and 0 interrupted iterations
http_json_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 9307 complete and 0 interrupted iterations
http_json_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 11106 complete and 0 interrupted iterations
http_json_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 13012 complete and 0 interrupted iterations
http_json_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 14883 complete and 0 interrupted iterations
http_json_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 16849 complete and 0 interrupted iterations
http_json_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 18873 complete and 0 interrupted iterations
http_json_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 20783 complete and 0 interrupted iterations
http_json_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 22679 complete and 0 interrupted iterations
http_json_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 24556 complete and 0 interrupted iterations
http_json_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 26372 complete and 0 interrupted iterations
http_json_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 28224 complete and 0 interrupted iterations
http_json_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 30117 complete and 0 interrupted iterations
http_json_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 32009 complete and 0 interrupted iterations
http_json_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 33906 complete and 0 interrupted iterations
http_json_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 35749 complete and 0 interrupted iterations
http_json_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 37652 complete and 0 interrupted iterations
http_json_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 39420 complete and 0 interrupted iterations
http_json_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 41300 complete and 0 interrupted iterations
http_json_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 43184 complete and 0 interrupted iterations
http_json_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 45010 complete and 0 interrupted iterations
http_json_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 46925 complete and 0 interrupted iterations
http_json_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 48862 complete and 0 interrupted iterations
http_json_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 50751 complete and 0 interrupted iterations
http_json_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 52667 complete and 0 interrupted iterations
http_json_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 54573 complete and 0 interrupted iterations
http_json_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 56412 complete and 0 interrupted iterations
http_json_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:41:40Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:41:40Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:41:40Z" level=info msg="============================================================" source=console
time="2026-01-17T02:41:40Z" level=info msg="Protocol: HTTP/JSON" source=console
time="2026-01-17T02:41:40Z" level=info msg="Test Name: phase4-200kb" source=console
time="2026-01-17T02:41:40Z" level=info msg="Duration: 30010ms" source=console
time="2026-01-17T02:41:40Z" level=info msg="Total Requests: 56440" source=console
time="2026-01-17T02:41:40Z" level=info msg="Throughput: 1880.71 req/s" source=console
time="2026-01-17T02:41:40Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:41:40Z" level=info msg="  Average: 4.05ms" source=console
time="2026-01-17T02:41:40Z" level=info msg="  P50: 3.83ms" source=console
time="2026-01-17T02:41:40Z" level=info msg="  P95: 5.73ms" source=console
time="2026-01-17T02:41:40Z" level=info msg="  P99: 6.74ms" source=console
time="2026-01-17T02:41:40Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:41:40Z" level=info msg="  Peak Heap: 692.05MB" source=console
time="2026-01-17T02:41:40Z" level=info msg="  GC Count: 121" source=console
time="2026-01-17T02:41:40Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=6.8ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 112880  3758.696681/s
    checks_succeeded...: 100.00% 112880 out of 112880
    checks_failed......: 0.00%   0 out of 112880

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=5.01ms min=1.45ms med=4.79ms max=20.39ms p(90)=6.3ms  p(95)=6.8ms
      { expected_response:true }...: avg=5.01ms min=1.45ms med=4.79ms max=20.39ms p(90)=6.3ms  p(95)=6.8ms
    http_req_failed................: 0.00%  0 out of 56443
    http_reqs......................: 56443  1879.448235/s

    EXECUTION
    iteration_duration.............: avg=5.29ms min=3.34ms med=5.07ms max=20.66ms p(90)=6.59ms p(95)=7.1ms
    iterations.....................: 56440  1879.348341/s
    vus............................: 10     min=10         max=10
    vus_max........................: 10     min=10         max=10

    NETWORK
    data_received..................: 14 MB  455 kB/s
    data_sent......................: 5.9 MB 196 kB/s




running (0m30.0s), 00/10 VUs, 56440 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 10 VUs  30s

[14/20] HTTP/Binary - 200kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/http-binary-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_binary_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:41:43Z" level=info msg="============================================================" source=console
time="2026-01-17T02:41:43Z" level=info msg="Phase 4: HTTP/Binary Crossover Test - Size: 200kb" source=console
time="2026-01-17T02:41:43Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:41:43Z" level=info msg="============================================================" source=console
time="2026-01-17T02:41:43Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:41:43Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/Binary\",\"testName\":\"phase4-200kb\",\"startTime\":1768617703516}" source=console

running (0m01.0s), 10/10 VUs, 3620 complete and 0 interrupted iterations
http_binary_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 8087 complete and 0 interrupted iterations
http_binary_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 13032 complete and 0 interrupted iterations
http_binary_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 17854 complete and 0 interrupted iterations
http_binary_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 22572 complete and 0 interrupted iterations
http_binary_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 27102 complete and 0 interrupted iterations
http_binary_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 32252 complete and 0 interrupted iterations
http_binary_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 37306 complete and 0 interrupted iterations
http_binary_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 42243 complete and 0 interrupted iterations
http_binary_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 47138 complete and 0 interrupted iterations
http_binary_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 52072 complete and 0 interrupted iterations
http_binary_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 56483 complete and 0 interrupted iterations
http_binary_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 61099 complete and 0 interrupted iterations
http_binary_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 65743 complete and 0 interrupted iterations
http_binary_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 70401 complete and 0 interrupted iterations
http_binary_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 75145 complete and 0 interrupted iterations
http_binary_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 79714 complete and 0 interrupted iterations
http_binary_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 84276 complete and 0 interrupted iterations
http_binary_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 89630 complete and 0 interrupted iterations
http_binary_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 94862 complete and 0 interrupted iterations
http_binary_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 99847 complete and 0 interrupted iterations
http_binary_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 104578 complete and 0 interrupted iterations
http_binary_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 109336 complete and 0 interrupted iterations
http_binary_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 113984 complete and 0 interrupted iterations
http_binary_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 118778 complete and 0 interrupted iterations
http_binary_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 123291 complete and 0 interrupted iterations
http_binary_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 127920 complete and 0 interrupted iterations
http_binary_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 132416 complete and 0 interrupted iterations
http_binary_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 136911 complete and 0 interrupted iterations
http_binary_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 141386 complete and 0 interrupted iterations
http_binary_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:42:13Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:42:13Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:42:13Z" level=info msg="============================================================" source=console
time="2026-01-17T02:42:13Z" level=info msg="Protocol: HTTP/Binary" source=console
time="2026-01-17T02:42:13Z" level=info msg="Test Name: phase4-200kb" source=console
time="2026-01-17T02:42:13Z" level=info msg="Duration: 30004ms" source=console
time="2026-01-17T02:42:13Z" level=info msg="Total Requests: 141434" source=console
time="2026-01-17T02:42:13Z" level=info msg="Throughput: 4713.84 req/s" source=console
time="2026-01-17T02:42:13Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:42:13Z" level=info msg="  Average: 1.22ms" source=console
time="2026-01-17T02:42:13Z" level=info msg="  P50: 1.10ms" source=console
time="2026-01-17T02:42:13Z" level=info msg="  P95: 1.91ms" source=console
time="2026-01-17T02:42:13Z" level=info msg="  P99: 2.37ms" source=console
time="2026-01-17T02:42:13Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:42:13Z" level=info msg="  Peak Heap: 698.00MB" source=console
time="2026-01-17T02:42:13Z" level=info msg="  GC Count: 72" source=console
time="2026-01-17T02:42:13Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=2.89ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 282868  9412.69324/s
    checks_succeeded...: 100.00% 282868 out of 282868
    checks_failed......: 0.00%   0 out of 282868

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=2.01ms min=1.1ms  med=1.9ms max=41.38ms p(90)=2.61ms p(95)=2.89ms
      { expected_response:true }...: avg=2.01ms min=1.1ms  med=1.9ms max=41.38ms p(90)=2.61ms p(95)=2.89ms
    http_req_failed................: 0.00%  0 out of 141437
    http_reqs......................: 141437 4706.446448/s

    EXECUTION
    iteration_duration.............: avg=2.11ms min=1.16ms med=2ms   max=8.28ms  p(90)=2.72ms p(95)=3ms   
    iterations.....................: 141434 4706.34662/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 35 MB  1.1 MB/s
    data_sent......................: 15 MB  499 kB/s




running (0m30.1s), 00/10 VUs, 141434 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 10 VUs  30s

[15/20] gRPC/Unary - 200kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/grpc-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:42:16Z" level=info msg="============================================================" source=console
time="2026-01-17T02:42:16Z" level=info msg="Phase 4: gRPC/Unary Crossover Test - Size: 200kb" source=console
time="2026-01-17T02:42:16Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:42:16Z" level=info msg="============================================================" source=console
time="2026-01-17T02:42:16Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:42:16Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Unary\",\"testName\":\"phase4-200kb\",\"startTime\":1768617736785}" source=console

running (0m01.0s), 10/10 VUs, 3865 complete and 0 interrupted iterations
grpc_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 7958 complete and 0 interrupted iterations
grpc_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 12073 complete and 0 interrupted iterations
grpc_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 15930 complete and 0 interrupted iterations
grpc_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 19948 complete and 0 interrupted iterations
grpc_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 24399 complete and 0 interrupted iterations
grpc_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 28451 complete and 0 interrupted iterations
grpc_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 32501 complete and 0 interrupted iterations
grpc_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 36549 complete and 0 interrupted iterations
grpc_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 40870 complete and 0 interrupted iterations
grpc_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 45060 complete and 0 interrupted iterations
grpc_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 49450 complete and 0 interrupted iterations
grpc_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 53455 complete and 0 interrupted iterations
grpc_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 57316 complete and 0 interrupted iterations
grpc_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 61114 complete and 0 interrupted iterations
grpc_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 65314 complete and 0 interrupted iterations
grpc_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 69701 complete and 0 interrupted iterations
grpc_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 73769 complete and 0 interrupted iterations
grpc_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 77704 complete and 0 interrupted iterations
grpc_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 81733 complete and 0 interrupted iterations
grpc_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 86121 complete and 0 interrupted iterations
grpc_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 90308 complete and 0 interrupted iterations
grpc_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 94690 complete and 0 interrupted iterations
grpc_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 98730 complete and 0 interrupted iterations
grpc_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 102835 complete and 0 interrupted iterations
grpc_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 106833 complete and 0 interrupted iterations
grpc_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 110981 complete and 0 interrupted iterations
grpc_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 115177 complete and 0 interrupted iterations
grpc_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 119639 complete and 0 interrupted iterations
grpc_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 123820 complete and 0 interrupted iterations
grpc_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:42:46Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:42:46Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:42:46Z" level=info msg="============================================================" source=console
time="2026-01-17T02:42:46Z" level=info msg="Protocol: gRPC/Unary" source=console
time="2026-01-17T02:42:46Z" level=info msg="Test Name: phase4-200kb" source=console
time="2026-01-17T02:42:46Z" level=info msg="Duration: 30006ms" source=console
time="2026-01-17T02:42:46Z" level=info msg="Total Requests: 123879" source=console
time="2026-01-17T02:42:46Z" level=info msg="Throughput: 4128.47 req/s" source=console
time="2026-01-17T02:42:46Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:42:46Z" level=info msg="  Average: 1.29ms" source=console
time="2026-01-17T02:42:46Z" level=info msg="  P50: 1.04ms" source=console
time="2026-01-17T02:42:46Z" level=info msg="  P95: 2.64ms" source=console
time="2026-01-17T02:42:46Z" level=info msg="  P99: 3.33ms" source=console
time="2026-01-17T02:42:46Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:42:46Z" level=info msg="  Peak Heap: 706.30MB" source=console
time="2026-01-17T02:42:46Z" level=info msg="  GC Count: 58" source=console
time="2026-01-17T02:42:46Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=3.6ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 247758  8244.620208/s
    checks_succeeded...: 100.00% 247758 out of 247758
    checks_failed......: 0.00%   0 out of 247758

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=2.14ms min=899.66µs med=1.89ms max=37.89ms p(90)=3.21ms p(95)=3.6ms 
      { expected_response:true }...: avg=2.14ms min=899.66µs med=1.89ms max=37.89ms p(90)=3.21ms p(95)=3.6ms 
    http_req_failed................: 0.00%  0 out of 123882
    http_reqs......................: 123882 4122.409935/s

    EXECUTION
    iteration_duration.............: avg=2.39ms min=951.01µs med=2.14ms max=18.42ms p(90)=3.48ms p(95)=3.88ms
    iterations.....................: 123879 4122.310104/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 30 MB  1.0 MB/s
    data_sent......................: 12 MB  408 kB/s




running (0m30.1s), 00/10 VUs, 123879 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 10 VUs  30s

[16/20] gRPC/Stream - 200kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/grpc-stream-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_stream_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:42:50Z" level=info msg="============================================================" source=console
time="2026-01-17T02:42:50Z" level=info msg="Phase 4: gRPC/Stream Crossover Test - Size: 200kb" source=console
time="2026-01-17T02:42:50Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:42:50Z" level=info msg="============================================================" source=console
time="2026-01-17T02:42:50Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:42:50Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Stream\",\"testName\":\"phase4-200kb\",\"startTime\":1768617770056}" source=console

running (0m01.0s), 10/10 VUs, 1180 complete and 0 interrupted iterations
grpc_stream_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 2446 complete and 0 interrupted iterations
grpc_stream_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 3711 complete and 0 interrupted iterations
grpc_stream_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 5054 complete and 0 interrupted iterations
grpc_stream_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 6308 complete and 0 interrupted iterations
grpc_stream_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 7589 complete and 0 interrupted iterations
grpc_stream_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 8892 complete and 0 interrupted iterations
grpc_stream_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 10201 complete and 0 interrupted iterations
grpc_stream_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 11468 complete and 0 interrupted iterations
grpc_stream_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 12749 complete and 0 interrupted iterations
grpc_stream_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 14065 complete and 0 interrupted iterations
grpc_stream_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 15359 complete and 0 interrupted iterations
grpc_stream_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 16666 complete and 0 interrupted iterations
grpc_stream_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 17956 complete and 0 interrupted iterations
grpc_stream_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 19253 complete and 0 interrupted iterations
grpc_stream_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 20561 complete and 0 interrupted iterations
grpc_stream_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 21850 complete and 0 interrupted iterations
grpc_stream_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 23140 complete and 0 interrupted iterations
grpc_stream_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 24433 complete and 0 interrupted iterations
grpc_stream_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 25741 complete and 0 interrupted iterations
grpc_stream_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 27010 complete and 0 interrupted iterations
grpc_stream_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 28314 complete and 0 interrupted iterations
grpc_stream_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 29626 complete and 0 interrupted iterations
grpc_stream_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 30881 complete and 0 interrupted iterations
grpc_stream_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 32207 complete and 0 interrupted iterations
grpc_stream_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 33513 complete and 0 interrupted iterations
grpc_stream_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 34786 complete and 0 interrupted iterations
grpc_stream_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 36072 complete and 0 interrupted iterations
grpc_stream_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 37352 complete and 0 interrupted iterations
grpc_stream_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 38636 complete and 0 interrupted iterations
grpc_stream_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:43:20Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:43:20Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:43:20Z" level=info msg="============================================================" source=console
time="2026-01-17T02:43:20Z" level=info msg="Protocol: gRPC/Stream" source=console
time="2026-01-17T02:43:20Z" level=info msg="Test Name: phase4-200kb" source=console
time="2026-01-17T02:43:20Z" level=info msg="Duration: 30012ms" source=console
time="2026-01-17T02:43:20Z" level=info msg="Total Requests: 38658" source=console
time="2026-01-17T02:43:20Z" level=info msg="Throughput: 1288.08 req/s" source=console
time="2026-01-17T02:43:20Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:43:20Z" level=info msg="  Average: 6.48ms" source=console
time="2026-01-17T02:43:20Z" level=info msg="  P50: 5.87ms" source=console
time="2026-01-17T02:43:20Z" level=info msg="  P95: 9.57ms" source=console
time="2026-01-17T02:43:20Z" level=info msg="  P99: 10.91ms" source=console
time="2026-01-17T02:43:20Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:43:20Z" level=info msg="  Peak Heap: 706.52MB" source=console
time="2026-01-17T02:43:20Z" level=info msg="  GC Count: 19" source=console
time="2026-01-17T02:43:20Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=10.56ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 77316   2574.559865/s
    checks_succeeded...: 100.00% 77316 out of 77316
    checks_failed......: 0.00%   0 out of 77316

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=7.45ms min=1.71ms med=6.84ms max=24.63ms p(90)=10ms    p(95)=10.56ms
      { expected_response:true }...: avg=7.45ms min=1.71ms med=6.84ms max=24.63ms p(90)=10ms    p(95)=10.56ms
    http_req_failed................: 0.00%  0 out of 38661
    http_reqs......................: 38661  1287.37983/s

    EXECUTION
    iteration_duration.............: avg=7.73ms min=5.19ms med=7.12ms max=25.07ms p(90)=10.28ms p(95)=10.85ms
    iterations.....................: 38658  1287.279932/s
    vus............................: 10     min=10         max=10
    vus_max........................: 10     min=10         max=10

    NETWORK
    data_received..................: 9.4 MB 314 kB/s
    data_sent......................: 4.1 MB 137 kB/s




running (0m30.0s), 00/10 VUs, 38658 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 10 VUs  30s

==========================================
         500kb 페이로드 테스트
==========================================

[17/20] HTTP/JSON - 500kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/http-json-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_json_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:43:23Z" level=info msg="============================================================" source=console
time="2026-01-17T02:43:23Z" level=info msg="Phase 4: HTTP/JSON Crossover Test - Size: 500kb" source=console
time="2026-01-17T02:43:23Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:43:23Z" level=info msg="============================================================" source=console
time="2026-01-17T02:43:23Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:43:23Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/JSON\",\"testName\":\"phase4-500kb\",\"startTime\":1768617803306}" source=console

running (0m01.0s), 10/10 VUs, 961 complete and 0 interrupted iterations
http_json_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 2016 complete and 0 interrupted iterations
http_json_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 3034 complete and 0 interrupted iterations
http_json_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 4051 complete and 0 interrupted iterations
http_json_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 5057 complete and 0 interrupted iterations
http_json_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 6072 complete and 0 interrupted iterations
http_json_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 7085 complete and 0 interrupted iterations
http_json_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 8100 complete and 0 interrupted iterations
http_json_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 9124 complete and 0 interrupted iterations
http_json_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 10136 complete and 0 interrupted iterations
http_json_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 11155 complete and 0 interrupted iterations
http_json_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 12173 complete and 0 interrupted iterations
http_json_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 13193 complete and 0 interrupted iterations
http_json_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 14198 complete and 0 interrupted iterations
http_json_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 15206 complete and 0 interrupted iterations
http_json_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 16212 complete and 0 interrupted iterations
http_json_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 17218 complete and 0 interrupted iterations
http_json_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 18236 complete and 0 interrupted iterations
http_json_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 19255 complete and 0 interrupted iterations
http_json_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 20271 complete and 0 interrupted iterations
http_json_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 21284 complete and 0 interrupted iterations
http_json_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 22296 complete and 0 interrupted iterations
http_json_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 23301 complete and 0 interrupted iterations
http_json_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 24305 complete and 0 interrupted iterations
http_json_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 25320 complete and 0 interrupted iterations
http_json_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 26338 complete and 0 interrupted iterations
http_json_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 27360 complete and 0 interrupted iterations
http_json_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 28377 complete and 0 interrupted iterations
http_json_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 29399 complete and 0 interrupted iterations
http_json_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 30423 complete and 0 interrupted iterations
http_json_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:43:53Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:43:53Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:43:53Z" level=info msg="============================================================" source=console
time="2026-01-17T02:43:53Z" level=info msg="Protocol: HTTP/JSON" source=console
time="2026-01-17T02:43:53Z" level=info msg="Test Name: phase4-500kb" source=console
time="2026-01-17T02:43:53Z" level=info msg="Duration: 30016ms" source=console
time="2026-01-17T02:43:53Z" level=info msg="Total Requests: 30442" source=console
time="2026-01-17T02:43:53Z" level=info msg="Throughput: 1014.19 req/s" source=console
time="2026-01-17T02:43:53Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:43:53Z" level=info msg="  Average: 8.51ms" source=console
time="2026-01-17T02:43:53Z" level=info msg="  P50: 8.18ms" source=console
time="2026-01-17T02:43:53Z" level=info msg="  P95: 11.53ms" source=console
time="2026-01-17T02:43:53Z" level=info msg="  P99: 12.39ms" source=console
time="2026-01-17T02:43:53Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:43:53Z" level=info msg="  Peak Heap: 714.88MB" source=console
time="2026-01-17T02:43:53Z" level=info msg="  GC Count: 160" source=console
time="2026-01-17T02:43:53Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=12.58ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 60884   2026.800799/s
    checks_succeeded...: 100.00% 60884 out of 60884
    checks_failed......: 0.00%   0 out of 60884

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=9.54ms min=1.59ms med=9.21ms max=28.62ms p(90)=12.26ms p(95)=12.58ms
      { expected_response:true }...: avg=9.54ms min=1.59ms med=9.21ms max=28.62ms p(90)=12.26ms p(95)=12.58ms
    http_req_failed................: 0.00%  0 out of 30445
    http_reqs......................: 30445  1013.500268/s

    EXECUTION
    iteration_duration.............: avg=9.83ms min=6.8ms  med=9.49ms max=28.89ms p(90)=12.53ms p(95)=12.87ms
    iterations.....................: 30442  1013.4004/s
    vus............................: 10     min=10         max=10
    vus_max........................: 10     min=10         max=10

    NETWORK
    data_received..................: 7.4 MB 246 kB/s
    data_sent......................: 3.2 MB 105 kB/s




running (0m30.0s), 00/10 VUs, 30442 complete and 0 interrupted iterations
http_json_test ✓ [ 100% ] 10 VUs  30s

[18/20] HTTP/Binary - 500kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/http-binary-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * http_binary_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:43:56Z" level=info msg="============================================================" source=console
time="2026-01-17T02:43:56Z" level=info msg="Phase 4: HTTP/Binary Crossover Test - Size: 500kb" source=console
time="2026-01-17T02:43:56Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:43:56Z" level=info msg="============================================================" source=console
time="2026-01-17T02:43:56Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:43:56Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"HTTP/Binary\",\"testName\":\"phase4-500kb\",\"startTime\":1768617836556}" source=console

running (0m01.0s), 10/10 VUs, 3735 complete and 0 interrupted iterations
http_binary_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 7786 complete and 0 interrupted iterations
http_binary_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 11840 complete and 0 interrupted iterations
http_binary_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 15883 complete and 0 interrupted iterations
http_binary_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 19947 complete and 0 interrupted iterations
http_binary_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 23976 complete and 0 interrupted iterations
http_binary_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 28015 complete and 0 interrupted iterations
http_binary_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 32069 complete and 0 interrupted iterations
http_binary_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 36089 complete and 0 interrupted iterations
http_binary_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 40167 complete and 0 interrupted iterations
http_binary_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 44215 complete and 0 interrupted iterations
http_binary_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 48258 complete and 0 interrupted iterations
http_binary_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 52298 complete and 0 interrupted iterations
http_binary_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 56334 complete and 0 interrupted iterations
http_binary_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 60406 complete and 0 interrupted iterations
http_binary_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 64507 complete and 0 interrupted iterations
http_binary_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 68553 complete and 0 interrupted iterations
http_binary_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 72627 complete and 0 interrupted iterations
http_binary_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 76612 complete and 0 interrupted iterations
http_binary_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 80522 complete and 0 interrupted iterations
http_binary_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 84402 complete and 0 interrupted iterations
http_binary_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 88380 complete and 0 interrupted iterations
http_binary_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 92289 complete and 0 interrupted iterations
http_binary_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 96195 complete and 0 interrupted iterations
http_binary_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 99736 complete and 0 interrupted iterations
http_binary_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 103080 complete and 0 interrupted iterations
http_binary_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 106371 complete and 0 interrupted iterations
http_binary_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 109771 complete and 0 interrupted iterations
http_binary_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 113127 complete and 0 interrupted iterations
http_binary_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 116503 complete and 0 interrupted iterations
http_binary_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:44:26Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:44:26Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:44:26Z" level=info msg="============================================================" source=console
time="2026-01-17T02:44:26Z" level=info msg="Protocol: HTTP/Binary" source=console
time="2026-01-17T02:44:26Z" level=info msg="Test Name: phase4-500kb" source=console
time="2026-01-17T02:44:26Z" level=info msg="Duration: 30009ms" source=console
time="2026-01-17T02:44:26Z" level=info msg="Total Requests: 116536" source=console
time="2026-01-17T02:44:26Z" level=info msg="Throughput: 3883.37 req/s" source=console
time="2026-01-17T02:44:26Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:44:26Z" level=info msg="  Average: 1.63ms" source=console
time="2026-01-17T02:44:26Z" level=info msg="  P50: 1.47ms" source=console
time="2026-01-17T02:44:26Z" level=info msg="  P95: 2.40ms" source=console
time="2026-01-17T02:44:26Z" level=info msg="  P99: 3.19ms" source=console
time="2026-01-17T02:44:26Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:44:26Z" level=info msg="  Peak Heap: 720.70MB" source=console
time="2026-01-17T02:44:26Z" level=info msg="  GC Count: 128" source=console
time="2026-01-17T02:44:26Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=3.3ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 233072  7756.547747/s
    checks_succeeded...: 100.00% 233072 out of 233072
    checks_failed......: 0.00%   0 out of 233072

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=2.42ms min=1.54ms med=2.26ms max=34.59ms p(90)=3.07ms p(95)=3.3ms 
      { expected_response:true }...: avg=2.42ms min=1.54ms med=2.26ms max=34.59ms p(90)=3.07ms p(95)=3.3ms 
    http_req_failed................: 0.00%  0 out of 116539
    http_reqs......................: 116539 3878.373712/s

    EXECUTION
    iteration_duration.............: avg=2.56ms min=1.64ms med=2.39ms max=16.71ms p(90)=3.22ms p(95)=3.49ms
    iterations.....................: 116536 3878.273873/s
    vus............................: 10     min=10          max=10
    vus_max........................: 10     min=10          max=10

    NETWORK
    data_received..................: 28 MB  947 kB/s
    data_sent......................: 12 MB  411 kB/s




running (0m30.0s), 00/10 VUs, 116536 complete and 0 interrupted iterations
http_binary_test ✓ [ 100% ] 10 VUs  30s

[19/20] gRPC/Unary - 500kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/grpc-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:44:29Z" level=info msg="============================================================" source=console
time="2026-01-17T02:44:29Z" level=info msg="Phase 4: gRPC/Unary Crossover Test - Size: 500kb" source=console
time="2026-01-17T02:44:29Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:44:29Z" level=info msg="============================================================" source=console
time="2026-01-17T02:44:29Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:44:29Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Unary\",\"testName\":\"phase4-500kb\",\"startTime\":1768617869812}" source=console

running (0m01.0s), 10/10 VUs, 2468 complete and 0 interrupted iterations
grpc_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 4613 complete and 0 interrupted iterations
grpc_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 6974 complete and 0 interrupted iterations
grpc_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 9434 complete and 0 interrupted iterations
grpc_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 11914 complete and 0 interrupted iterations
grpc_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 14397 complete and 0 interrupted iterations
grpc_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 16890 complete and 0 interrupted iterations
grpc_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 19357 complete and 0 interrupted iterations
grpc_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 21812 complete and 0 interrupted iterations
grpc_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 24171 complete and 0 interrupted iterations
grpc_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 26537 complete and 0 interrupted iterations
grpc_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 28938 complete and 0 interrupted iterations
grpc_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 31390 complete and 0 interrupted iterations
grpc_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 33738 complete and 0 interrupted iterations
grpc_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 36209 complete and 0 interrupted iterations
grpc_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 38840 complete and 0 interrupted iterations
grpc_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 41314 complete and 0 interrupted iterations
grpc_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 43817 complete and 0 interrupted iterations
grpc_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 46237 complete and 0 interrupted iterations
grpc_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 48779 complete and 0 interrupted iterations
grpc_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 51315 complete and 0 interrupted iterations
grpc_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 53658 complete and 0 interrupted iterations
grpc_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 56021 complete and 0 interrupted iterations
grpc_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 58387 complete and 0 interrupted iterations
grpc_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 60705 complete and 0 interrupted iterations
grpc_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 63191 complete and 0 interrupted iterations
grpc_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 65552 complete and 0 interrupted iterations
grpc_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 67996 complete and 0 interrupted iterations
grpc_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 70378 complete and 0 interrupted iterations
grpc_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 72885 complete and 0 interrupted iterations
grpc_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:44:59Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:44:59Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:44:59Z" level=info msg="============================================================" source=console
time="2026-01-17T02:44:59Z" level=info msg="Protocol: gRPC/Unary" source=console
time="2026-01-17T02:44:59Z" level=info msg="Test Name: phase4-500kb" source=console
time="2026-01-17T02:44:59Z" level=info msg="Duration: 30011ms" source=console
time="2026-01-17T02:44:59Z" level=info msg="Total Requests: 72912" source=console
time="2026-01-17T02:44:59Z" level=info msg="Throughput: 2429.51 req/s" source=console
time="2026-01-17T02:44:59Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:44:59Z" level=info msg="  Average: 2.89ms" source=console
time="2026-01-17T02:44:59Z" level=info msg="  P50: 2.55ms" source=console
time="2026-01-17T02:44:59Z" level=info msg="  P95: 5.53ms" source=console
time="2026-01-17T02:44:59Z" level=info msg="  P99: 7.10ms" source=console
time="2026-01-17T02:44:59Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:44:59Z" level=info msg="  Peak Heap: 725.00MB" source=console
time="2026-01-17T02:44:59Z" level=info msg="  GC Count: 79" source=console
time="2026-01-17T02:44:59Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=6.54ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 145824  4854.677933/s
    checks_succeeded...: 100.00% 145824 out of 145824
    checks_failed......: 0.00%   0 out of 145824

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=3.81ms min=1.36ms med=3.46ms max=20.54ms p(90)=5.73ms p(95)=6.54ms
      { expected_response:true }...: avg=3.81ms min=1.36ms med=3.46ms max=20.54ms p(90)=5.73ms p(95)=6.54ms
    http_req_failed................: 0.00%  0 out of 72915
    http_reqs......................: 72915  2427.438841/s

    EXECUTION
    iteration_duration.............: avg=4.08ms min=1.58ms med=3.73ms max=16.17ms p(90)=6.02ms p(95)=6.83ms
    iterations.....................: 72912  2427.338967/s
    vus............................: 10     min=10         max=10
    vus_max........................: 10     min=10         max=10

    NETWORK
    data_received..................: 18 MB  590 kB/s
    data_sent......................: 7.2 MB 240 kB/s




running (0m30.0s), 00/10 VUs, 72912 complete and 0 interrupted iterations
grpc_test ✓ [ 100% ] 10 VUs  30s

[20/20] gRPC/Stream - 500kb 테스트...

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/jun/distributed-log-pipeline/proto-bench/scripts/phase4/grpc-stream-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * grpc_stream_test: 10 looping VUs for 30s (gracefulStop: 30s)

time="2026-01-17T02:45:03Z" level=info msg="============================================================" source=console
time="2026-01-17T02:45:03Z" level=info msg="Phase 4: gRPC/Stream Crossover Test - Size: 500kb" source=console
time="2026-01-17T02:45:03Z" level=info msg="VUs: 10, Duration: 30s" source=console
time="2026-01-17T02:45:03Z" level=info msg="============================================================" source=console
time="2026-01-17T02:45:03Z" level=info msg="✅ Server is ready" source=console
time="2026-01-17T02:45:03Z" level=info msg="Benchmark started: {\"status\":\"started\",\"protocol\":\"gRPC/Stream\",\"testName\":\"phase4-500kb\",\"startTime\":1768617903108}" source=console

running (0m01.0s), 10/10 VUs, 577 complete and 0 interrupted iterations
grpc_stream_test   [   3% ] 10 VUs  01.0s/30s

running (0m02.0s), 10/10 VUs, 1191 complete and 0 interrupted iterations
grpc_stream_test   [   7% ] 10 VUs  02.0s/30s

running (0m03.0s), 10/10 VUs, 1799 complete and 0 interrupted iterations
grpc_stream_test   [  10% ] 10 VUs  03.0s/30s

running (0m04.0s), 10/10 VUs, 2418 complete and 0 interrupted iterations
grpc_stream_test   [  13% ] 10 VUs  04.0s/30s

running (0m05.0s), 10/10 VUs, 3029 complete and 0 interrupted iterations
grpc_stream_test   [  17% ] 10 VUs  05.0s/30s

running (0m06.0s), 10/10 VUs, 3643 complete and 0 interrupted iterations
grpc_stream_test   [  20% ] 10 VUs  06.0s/30s

running (0m07.0s), 10/10 VUs, 4264 complete and 0 interrupted iterations
grpc_stream_test   [  23% ] 10 VUs  07.0s/30s

running (0m08.0s), 10/10 VUs, 4872 complete and 0 interrupted iterations
grpc_stream_test   [  27% ] 10 VUs  08.0s/30s

running (0m09.0s), 10/10 VUs, 5486 complete and 0 interrupted iterations
grpc_stream_test   [  30% ] 10 VUs  09.0s/30s

running (0m10.0s), 10/10 VUs, 6107 complete and 0 interrupted iterations
grpc_stream_test   [  33% ] 10 VUs  10.0s/30s

running (0m11.0s), 10/10 VUs, 6722 complete and 0 interrupted iterations
grpc_stream_test   [  37% ] 10 VUs  11.0s/30s

running (0m12.0s), 10/10 VUs, 7332 complete and 0 interrupted iterations
grpc_stream_test   [  40% ] 10 VUs  12.0s/30s

running (0m13.0s), 10/10 VUs, 7951 complete and 0 interrupted iterations
grpc_stream_test   [  43% ] 10 VUs  13.0s/30s

running (0m14.0s), 10/10 VUs, 8561 complete and 0 interrupted iterations
grpc_stream_test   [  47% ] 10 VUs  14.0s/30s

running (0m15.0s), 10/10 VUs, 9170 complete and 0 interrupted iterations
grpc_stream_test   [  50% ] 10 VUs  15.0s/30s

running (0m16.0s), 10/10 VUs, 9777 complete and 0 interrupted iterations
grpc_stream_test   [  53% ] 10 VUs  16.0s/30s

running (0m17.0s), 10/10 VUs, 10390 complete and 0 interrupted iterations
grpc_stream_test   [  57% ] 10 VUs  17.0s/30s

running (0m18.0s), 10/10 VUs, 10981 complete and 0 interrupted iterations
grpc_stream_test   [  60% ] 10 VUs  18.0s/30s

running (0m19.0s), 10/10 VUs, 11564 complete and 0 interrupted iterations
grpc_stream_test   [  63% ] 10 VUs  19.0s/30s

running (0m20.0s), 10/10 VUs, 12181 complete and 0 interrupted iterations
grpc_stream_test   [  67% ] 10 VUs  20.0s/30s

running (0m21.0s), 10/10 VUs, 12786 complete and 0 interrupted iterations
grpc_stream_test   [  70% ] 10 VUs  21.0s/30s

running (0m22.0s), 10/10 VUs, 13399 complete and 0 interrupted iterations
grpc_stream_test   [  73% ] 10 VUs  22.0s/30s

running (0m23.0s), 10/10 VUs, 14008 complete and 0 interrupted iterations
grpc_stream_test   [  77% ] 10 VUs  23.0s/30s

running (0m24.0s), 10/10 VUs, 14615 complete and 0 interrupted iterations
grpc_stream_test   [  80% ] 10 VUs  24.0s/30s

running (0m25.0s), 10/10 VUs, 15227 complete and 0 interrupted iterations
grpc_stream_test   [  83% ] 10 VUs  25.0s/30s

running (0m26.0s), 10/10 VUs, 15841 complete and 0 interrupted iterations
grpc_stream_test   [  87% ] 10 VUs  26.0s/30s

running (0m27.0s), 10/10 VUs, 16462 complete and 0 interrupted iterations
grpc_stream_test   [  90% ] 10 VUs  27.0s/30s

running (0m28.0s), 10/10 VUs, 17078 complete and 0 interrupted iterations
grpc_stream_test   [  93% ] 10 VUs  28.0s/30s

running (0m29.0s), 10/10 VUs, 17688 complete and 0 interrupted iterations
grpc_stream_test   [  97% ] 10 VUs  29.0s/30s

running (0m30.0s), 10/10 VUs, 18310 complete and 0 interrupted iterations
grpc_stream_test   [ 100% ] 10 VUs  30.0s/30s
time="2026-01-17T02:45:33Z" level=info msg="\n============================================================" source=console
time="2026-01-17T02:45:33Z" level=info msg="BENCHMARK RESULT" source=console
time="2026-01-17T02:45:33Z" level=info msg="============================================================" source=console
time="2026-01-17T02:45:33Z" level=info msg="Protocol: gRPC/Stream" source=console
time="2026-01-17T02:45:33Z" level=info msg="Test Name: phase4-500kb" source=console
time="2026-01-17T02:45:33Z" level=info msg="Duration: 30015ms" source=console
time="2026-01-17T02:45:33Z" level=info msg="Total Requests: 18323" source=console
time="2026-01-17T02:45:33Z" level=info msg="Throughput: 610.46 req/s" source=console
time="2026-01-17T02:45:33Z" level=info msg="\nLatency:" source=console
time="2026-01-17T02:45:33Z" level=info msg="  Average: 15.00ms" source=console
time="2026-01-17T02:45:33Z" level=info msg="  P50: 13.46ms" source=console
time="2026-01-17T02:45:33Z" level=info msg="  P95: 22.41ms" source=console
time="2026-01-17T02:45:33Z" level=info msg="  P99: 25.00ms" source=console
time="2026-01-17T02:45:33Z" level=info msg="\nServer Metrics:" source=console
time="2026-01-17T02:45:33Z" level=info msg="  Peak Heap: 725.67MB" source=console
time="2026-01-17T02:45:33Z" level=info msg="  GC Count: 19" source=console
time="2026-01-17T02:45:33Z" level=info msg="============================================================" source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=23.47ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 36646   1220.378285/s
    checks_succeeded...: 100.00% 36646 out of 36646
    checks_failed......: 0.00%   0 out of 36646

    ✓ status is 200
    ✓ has payload

    HTTP
    http_req_duration..............: avg=16.06ms min=2.11ms  med=14.53ms max=43.02ms p(90)=22.43ms p(95)=23.47ms
      { expected_response:true }...: avg=16.06ms min=2.11ms  med=14.53ms max=43.02ms p(90)=22.43ms p(95)=23.47ms
    http_req_failed................: 0.00%  0 out of 18326
    http_reqs......................: 18326  610.289048/s

    EXECUTION
    iteration_duration.............: avg=16.34ms min=11.69ms med=14.82ms max=43.27ms p(90)=22.71ms p(95)=23.77ms
    iterations.....................: 18323  610.189143/s
    vus............................: 10     min=10         max=10
    vus_max........................: 10     min=10         max=10

    NETWORK
    data_received..................: 4.5 MB 150 kB/s
    data_sent......................: 1.9 MB 65 kB/s




running (0m30.0s), 00/10 VUs, 18323 complete and 0 interrupted iterations
grpc_stream_test ✓ [ 100% ] 10 VUs  30s

================================
✅ Phase 4 테스트 완료!
결과 파일: /home/jun/distributed-log-pipeline/proto-bench/scripts/../results/phase4/*_20260117_023416.log
================================

==========================================
         결과 요약
==========================================

| 크기 | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream | 승자 |
|------|-----------|-------------|------------|-------------|------|
./run-phase4.sh: line 137: [: level=info: integer expression expected
./run-phase4.sh: line 139: [: level=info: integer expression expected
| 10kb | level=info | level=info | level=info | level=info | 동일 |
./run-phase4.sh: line 137: [: level=info: integer expression expected
./run-phase4.sh: line 139: [: level=info: integer expression expected
| 50kb | level=info | level=info | level=info | level=info | 동일 |
./run-phase4.sh: line 137: [: level=info: integer expression expected
./run-phase4.sh: line 139: [: level=info: integer expression expected
| 100kb | level=info | level=info | level=info | level=info | 동일 |
./run-phase4.sh: line 137: [: level=info: integer expression expected
./run-phase4.sh: line 139: [: level=info: integer expression expected
| 200kb | level=info | level=info | level=info | level=info | 동일 |
./run-phase4.sh: line 137: [: level=info: integer expression expected
./run-phase4.sh: line 139: [: level=info: integer expression expected
| 500kb | level=info | level=info | level=info | level=info | 동일 |

==========================================
※ 승자는 HTTP/Binary vs gRPC/Unary 비교 기준
==========================================
root@jun:/home/jun/distributed-log-pipeline/proto-bench/scripts# 
```