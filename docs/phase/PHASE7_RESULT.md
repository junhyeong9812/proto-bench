# Phase 7: CPU 사용량 포함 극한 복잡도 성능 분석 결과

## 개요

| 항목 | 값 |
|------|-----|
| 데이터 복잡도 | Ultra (~150 필드) → Extreme (~500 필드) |
| 동시 사용자 (VU) | 10 |
| 테스트 시간 | 각 30초 |
| 테스트 일시 | 2026-01-17 08:45 ~ 08:50 (KST) |
| 비고 | CPU 메트릭 추가 측정 (Phase 6 결과 검증) |

## 가설

> "gRPC/Protobuf는 극한 복잡도(~500 필드, 4단계 중첩)에서  
> 빌더 객체 생성으로 인해 JSON보다 더 많은 CPU를 사용하며,  
> 이것이 성능 역전의 주된 원인이다"

### 배경: Phase 6 결과 재확인

**Phase 6에서 확인된 역전 현상:**

| 복잡도 | 필드 수 | HTTP/JSON | gRPC/Unary | 승자 |
|--------|--------|-----------|------------|------|
| Simple | ~5개 | 3,602 | 6,007 | gRPC +67% |
| Medium | ~13개 | 3,273 | 5,527 | gRPC +69% |
| Complex | ~50개 | 3,154 | 4,415 | gRPC +40% |
| **Ultra** | **~150개** | **2,074** | **1,847** | **HTTP +12%** 🔥 |
| **Extreme** | **~500개** | **419** | **407** | **HTTP +3%** 🔥 |

**Phase 7 검증 목표:**
- gRPC가 실제로 더 많은 CPU를 사용하는지 측정
- CPU 효율성(Throughput/CPU) 관점에서 어떤 프로토콜이 유리한지 분석
- 역전의 근본 원인이 빌더 객체 생성 비용인지 검증

---

## 테스트 결과

### Ultra 복잡도 (~150 필드, 3단계 중첩)

#### Throughput & Latency

| 프로토콜 | Throughput (req/s) | Latency avg | Latency P95 | Peak Heap | GC Count | 순위 |
|----------|-------------------|-------------|-------------|-----------|----------|------|
| HTTP/Binary | 2,349.16 | 3.08ms | 4.18ms | 631 MB | 11 | 🥇 |
| **HTTP/JSON** | **1,996.03** | 3.68ms | 5.40ms | 636 MB | 36 | 🥈 |
| gRPC/Stream | 1,981.00 | 3.46ms | 5.11ms | 639 MB | 22 | 🥉 |
| gRPC/Unary | 1,917.48 | 3.60ms | 5.27ms | 640 MB | 21 | 4위 |

**🔥 역전 재확인!**
- **HTTP/JSON이 gRPC/Unary 대비 +4% 더 빠름**
- HTTP/Binary가 gRPC/Unary 대비 +23% 더 빠름

#### CPU 사용량

| 프로토콜 | Avg System CPU | Peak System CPU | Avg Process CPU | Peak Process CPU |
|----------|---------------|-----------------|-----------------|------------------|
| HTTP/JSON | 43.20% | 67.23% | **16.74%** | 19.41% |
| HTTP/Binary | 44.31% | 78.21% | **11.85%** | 13.19% |
| gRPC/Unary | 43.07% | 83.12% | **13.58%** | 15.68% |
| gRPC/Stream | 48.89% | 77.54% | **13.19%** | 15.55% |

---

### Extreme 복잡도 (~500 필드, 4단계 중첩)

#### Throughput & Latency

| 프로토콜 | Throughput (req/s) | Latency avg | Latency P95 | Peak Heap | GC Count | 순위 |
|----------|-------------------|-------------|-------------|-----------|----------|------|
| HTTP/Binary | 450.18 | 20.64ms | 27.36ms | 634 MB | 8 | 🥇 |
| **HTTP/JSON** | **413.16** | 22.78ms | 28.62ms | 645 MB | 76 | 🥈 |
| gRPC/Stream | 392.44 | 22.24ms | 27.85ms | 645 MB | 41 | 🥉 |
| gRPC/Unary | 392.42 | 22.23ms | 28.00ms | 644 MB | 41 | 4위 |

**분석:**
- **HTTP/JSON이 gRPC/Unary 대비 +5% 더 빠름**
- HTTP/Binary가 gRPC/Unary 대비 **+15% 더 빠름**

#### CPU 사용량

| 프로토콜 | Avg System CPU | Peak System CPU | Avg Process CPU | Peak Process CPU |
|----------|---------------|-----------------|-----------------|------------------|
| HTTP/JSON | 45.79% | 63.98% | **13.09%** | 14.89% |
| HTTP/Binary | 47.35% | 82.92% | **3.39%** | 4.26% |
| gRPC/Unary | 45.91% | 81.59% | **9.40%** | 11.11% |
| gRPC/Stream | 45.73% | 79.92% | **9.38%** | 10.92% |

---

## CPU 효율성 분석

### CPU 효율성 = Throughput / Avg Process CPU

> 의미: CPU 1% 당 처리할 수 있는 초당 요청 수  
> 높을수록 CPU를 효율적으로 사용하는 것

#### Ultra 복잡도

| 프로토콜 | Throughput | Avg Process CPU | 효율성 (req/s/%) | 순위 |
|----------|------------|-----------------|------------------|------|
| HTTP/Binary | 2,349.16 | 11.85% | **198.24** | 🥇 |
| gRPC/Stream | 1,981.00 | 13.19% | 150.18 | 🥈 |
| gRPC/Unary | 1,917.48 | 13.58% | 141.19 | 🥉 |
| HTTP/JSON | 1,996.03 | 16.74% | 119.23 | 4위 |

#### Extreme 복잡도

| 프로토콜 | Throughput | Avg Process CPU | 효율성 (req/s/%) | 순위 |
|----------|------------|-----------------|------------------|------|
| HTTP/Binary | 450.18 | 3.39% | **132.79** | 🥇 |
| gRPC/Stream | 392.44 | 9.38% | 41.83 | 🥈 |
| gRPC/Unary | 392.42 | 9.40% | 41.74 | 🥉 |
| HTTP/JSON | 413.16 | 13.09% | 31.56 | 4위 |

---

## 핵심 인사이트

### 1. 가설 검증 결과: ⚠️ 부분적으로 기각

| 가설 | 예상 | 실제 결과 | 판정 |
|------|------|----------|------|
| gRPC가 더 많은 CPU 사용 | gRPC > HTTP | **HTTP/JSON이 더 많음** | ❌ 기각 |
| 빌더 생성이 CPU 집약적 | Process CPU 차이 | **HTTP/JSON이 가장 높음** | ❌ 기각 |
| HTTP가 더 효율적 | HTTP 효율성 > gRPC | **HTTP/Binary만 해당** | ⚠️ 부분 확인 |

### 2. 예상과 다른 결과 분석

#### Process CPU 사용량 (예상 vs 실제)

| 복잡도 | 프로토콜 | 예상 | 실제 | 차이 |
|--------|----------|------|------|------|
| Ultra | HTTP/JSON | ~40% | **16.74%** | 낮음 |
| Ultra | gRPC/Unary | ~55% | **13.58%** | 훨씬 낮음 |
| Extreme | HTTP/JSON | ~60% | **13.09%** | 낮음 |
| Extreme | gRPC/Unary | ~75% | **9.40%** | 훨씬 낮음 |

**의외의 발견:**
- **HTTP/JSON이 가장 높은 Process CPU 사용** (Ultra: 16.74%, Extreme: 13.09%)
- **HTTP/Binary가 가장 낮은 Process CPU 사용** (Ultra: 11.85%, Extreme: 3.39%)
- gRPC는 중간 수준의 CPU 사용

### 3. 역전 원인 재분석

#### 원래 가설 (기각됨)
```
❌ "gRPC 빌더 객체 생성이 CPU 집약적이어서 역전 발생"
```

#### 새로운 해석
```
✅ "HTTP/Binary가 Protobuf 직렬화의 장점(작은 크기, 빠른 파싱)을 
   HTTP의 단순한 요청/응답 모델과 결합하여 최고 효율 달성"
```

### 4. 프로토콜별 특성 분석

| 프로토콜 | CPU 사용 패턴 | 성능 특성 | 효율성 |
|----------|-------------|----------|--------|
| HTTP/Binary | 매우 낮은 CPU | 최고 Throughput | 🥇 최고 |
| gRPC/Unary | 중간 CPU | 낮은 Throughput | 중간 |
| gRPC/Stream | 중간 CPU | 낮은 Throughput | 중간 |
| HTTP/JSON | 높은 CPU | 중간 Throughput | 낮음 |

#### 해석

1. **HTTP/Binary가 최고인 이유:**
   - Protobuf의 작은 크기 + 빠른 직렬화
   - HTTP의 단순한 프로토콜 오버헤드
   - gRPC 프레이밍/멀티플렉싱 오버헤드 없음

2. **HTTP/JSON이 높은 CPU를 사용하는 이유:**
   - JSON 문자열 파싱/생성 비용
   - 큰 페이로드 크기로 인한 I/O 처리
   - 그럼에도 gRPC보다 빠른 이유: 프로토콜 단순성

3. **gRPC가 중간 CPU + 낮은 성능인 이유:**
   - HTTP/2 프레이밍 오버헤드
   - 멀티플렉싱 관리 비용
   - 빌더 객체 생성은 CPU보다 **메모리/GC 영향**이 큼

### 5. GC와 성능의 관계

| 복잡도 | 프로토콜 | GC Count | Throughput | 관계 |
|--------|----------|----------|------------|------|
| Ultra | HTTP/Binary | 11 | 2,349 | 낮은 GC → 높은 성능 |
| Ultra | gRPC/Unary | 21 | 1,917 | 중간 GC → 중간 성능 |
| Ultra | HTTP/JSON | 36 | 1,996 | 높은 GC → 중간 성능 |
| Extreme | HTTP/Binary | 8 | 450 | 낮은 GC → 높은 성능 |
| Extreme | gRPC/Unary | 41 | 392 | 높은 GC → 낮은 성능 |
| Extreme | HTTP/JSON | 76 | 413 | 매우 높은 GC → 중간 성능 |

**핵심 발견:**
- GC Count와 성능은 **반비례 관계**
- HTTP/Binary의 낮은 GC가 높은 성능의 핵심 요인
- HTTP/JSON은 높은 GC에도 불구하고 gRPC보다 빠름 (프로토콜 단순성 효과)

---

## Phase 7 테스트 재현성 검증 (1차 vs 2차)

### 동일 조건에서 2회 테스트 수행

| 복잡도 | 프로토콜 | 1차 Throughput | 2차 Throughput | 차이 |
|--------|----------|---------------|---------------|------|
| Ultra | HTTP/JSON | 1,996 | 2,013 | +1% |
| Ultra | HTTP/Binary | 2,349 | 2,248 | -4% |
| Ultra | gRPC/Unary | 1,917 | 1,880 | -2% |
| Ultra | gRPC/Stream | 1,981 | 1,913 | -3% |
| Extreme | HTTP/JSON | 413 | 394 | -5% |
| Extreme | HTTP/Binary | 450 | 455 | +1% |
| Extreme | gRPC/Unary | 392 | 371 | -5% |
| Extreme | gRPC/Stream | 392 | 402 | +3% |

### 역전 패턴 재현성

| 복잡도 | 1차 (JSON vs gRPC) | 2차 (JSON vs gRPC) | 일관성 |
|--------|-------------------|-------------------|--------|
| Ultra | JSON +4% | **JSON +7%** | ✅ HTTP 우위 유지 |
| Extreme | JSON +5% | **JSON +6%** | ✅ HTTP 우위 유지 |

### CPU 효율성 재현성

| 복잡도 | 프로토콜 | 1차 효율성 | 2차 효율성 | 차이 |
|--------|----------|-----------|-----------|------|
| Ultra | HTTP/Binary | 198.24 | 188.41 | -5% |
| Ultra | gRPC/Unary | 141.19 | 135.81 | -4% |
| Extreme | HTTP/Binary | 132.79 | 135.96 | +2% |
| Extreme | gRPC/Unary | 41.74 | 40.09 | -4% |

**결론:** 오차 범위(±5%) 내에서 결과가 일관되게 재현됨 ✅

---

## Phase 6 vs Phase 7 비교

### Throughput 비교

| 복잡도 | 프로토콜 | Phase 6 | Phase 7 (1차) | Phase 7 (2차) |
|--------|----------|---------|--------------|--------------|
| Ultra | HTTP/JSON | 2,074 | 1,996 | 2,013 |
| Ultra | HTTP/Binary | 2,154 | 2,349 | 2,248 |
| Ultra | gRPC/Unary | 1,847 | 1,917 | 1,880 |
| Extreme | HTTP/JSON | 419 | 413 | 394 |
| Extreme | HTTP/Binary | 482 | 450 | 455 |
| Extreme | gRPC/Unary | 407 | 392 | 371 |

**결론:** 역전 현상은 3회 테스트 모두에서 일관되게 재현됨

### 역전 패턴 확인 (3회 테스트)

| 복잡도 | Phase 6 | Phase 7 (1차) | Phase 7 (2차) | 일관성 |
|--------|---------|--------------|--------------|--------|
| Ultra | JSON +12% | JSON +4% | JSON +7% | ✅ HTTP 우위 |
| Extreme | JSON +3% | JSON +5% | JSON +6% | ✅ HTTP 우위 |

---

## 수정된 결론

### 역전 원인 (수정)

| 기존 가설 | 수정된 분석 |
|----------|------------|
| Protobuf 빌더가 CPU 집약적 | ❌ CPU 사용량은 오히려 낮음 |
| JSON이 CPU 효율적 | ⚠️ HTTP/Binary만 해당, HTTP/JSON은 비효율적 |
| 빌더 호출 수가 핵심 | ⚠️ **GC 압박 가설은 추가 검증 필요** |

### ⚠️ GC 압박 가설의 한계

현재 테스트에서 GC 압박이 역전의 주 원인이라고 단정짓기에는 근거가 부족합니다:

**현재 관찰된 것:**
- GC Count와 성능이 반비례 관계 (상관관계)
- HTTP/Binary의 GC가 적고 성능이 높음

**추가 검증이 필요한 것:**
- GC가 실제로 병목인지 (인과관계 검증 필요)
- Heap 크기 변경에 따른 성능 변화
- GC 알고리즘 변경에 따른 성능 변화

### 추가 검증을 위한 Phase 8 제안

GC 압박 가설을 검증하려면 다음 테스트가 필요합니다:

#### 1. Heap 크기 변경 테스트

```bash
# 작은 Heap (256MB) - GC 압박 증가
java -Xms256m -Xmx256m -jar server.jar

# 기본 Heap (현재)
java -Xms512m -Xmx1g -jar server.jar

# 큰 Heap (2GB) - GC 압박 감소
java -Xms2g -Xmx2g -jar server.jar
```

**예상:** Heap이 작을수록 gRPC 성능이 더 나빠지면 GC 압박 가설 확인

#### 2. GC 알고리즘 변경 테스트

```bash
# G1GC (기본)
java -XX:+UseG1GC -jar server.jar

# ZGC (저지연)
java -XX:+UseZGC -jar server.jar

# Parallel GC (처리량 최적화)
java -XX:+UseParallelGC -jar server.jar
```

**예상:** ZGC에서 gRPC 성능이 개선되면 GC 압박 가설 확인

#### 3. GC 상세 로깅

```bash
java -Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10M -jar server.jar
```

분석 항목:
- GC Pause Time 분포
- Young GC vs Full GC 빈도
- 프로토콜별 메모리 할당 패턴

#### 4. 객체 할당 프로파일링

```bash
# async-profiler 사용
./profiler.sh -e alloc -d 30 -f alloc.html <pid>
```

분석 항목:
- Protobuf Builder 객체 할당량
- JSON String 객체 할당량
- 프로토콜별 할당 핫스팟

### 새로운 역전 메커니즘

```
극한 복잡도에서의 성능 결정 요인:

1. HTTP/Binary 🥇
   - Protobuf 직렬화 장점 (작은 크기, 빠른 파싱)
   - HTTP의 단순한 요청/응답 모델
   - 낮은 GC 압박 (Binary 재사용 가능)
   
2. HTTP/JSON 🥈  
   - 프로토콜 단순성
   - 높은 GC지만 gRPC 오버헤드 없음
   - 문자열 처리 최적화 (JVM String Pool)

3. gRPC/Unary 🥉
   - HTTP/2 프레이밍 오버헤드
   - 빌더 객체 → GC 압박
   - 멀티플렉싱이 소규모 테스트에서 불리
```

### 프로토콜 선택 가이드 (최종)

| 데이터 복잡도 | 필드 수 | 추천 프로토콜 | 성능 우위 | 근거 |
|--------------|--------|--------------|----------|------|
| Simple | ~5개 | **gRPC/Unary** | +67% | HTTP/2 멀티플렉싱 효과 |
| Medium | ~13개 | **gRPC/Unary** | +69% | 중첩 구조에서도 우위 |
| Complex | ~50개 | **gRPC/Unary** | +40% | 여전히 최고 성능 |
| **Ultra** | **~150개** | **HTTP/Binary** | **+23%** | 🔥 역전, 최고 효율 |
| **Extreme** | **~500개** | **HTTP/Binary** | **+15%** | HTTP 우위 지속 |

### CPU 효율성 기준 추천

| 환경 | 추천 프로토콜 | 근거 |
|------|--------------|------|
| CPU 리소스 제한 | **HTTP/Binary** | 최고 효율 (132~198 req/s/%) |
| CPU 리소스 충분 | 복잡도에 따라 선택 | gRPC (단순), HTTP (복잡) |
| 멀티코어 스케일링 필요 | **gRPC** | 비동기 처리 장점 |

---

## 핵심 발견 요약

```
1. 🔄 가설 재검토 필요: CPU 사용량이 아닌 다른 요인이 역전의 원인
2. 🥇 HTTP/Binary가 CPU 효율성 측면에서 압도적 (188~198 vs 136~141 req/s/%)
3. ⚠️ HTTP/JSON은 높은 CPU 사용에도 gRPC보다 빠름 (프로토콜 단순성)
4. 📊 GC Count와 성능은 반비례 (상관관계), 인과관계는 추가 검증 필요
5. ✅ 역전 현상은 3회 테스트(Phase 6 + Phase 7 x2) 모두 일관되게 재현됨
```

### 현재까지 확인된 사실

| 항목 | 확인 상태 | 근거 |
|------|----------|------|
| Ultra/Extreme에서 HTTP > gRPC | ✅ 확인됨 | 3회 테스트 일관 |
| HTTP/Binary가 최고 효율 | ✅ 확인됨 | CPU 효율성 압도적 |
| HTTP/JSON이 가장 높은 CPU 사용 | ✅ 확인됨 | Process CPU 측정 |
| GC Count와 성능 반비례 | ✅ 확인됨 | 상관관계 관찰 |
| **GC 압박이 역전의 원인** | ⚠️ 미확인 | 인과관계 검증 필요 |

---

## 테스트 환경

### 환경
- **서버**: Spring Boot + Kotlin (Coroutine)
- **gRPC 서버**: 포트 9091
- **HTTP 서버**: 포트 8081
- **API 서버**: 포트 8080 (프록시)
- **로드 테스터**: k6 (Grafana)
- **CPU 샘플링**: 100ms 간격
- **테스트 일시**: 2026-01-17 08:45 ~ 08:50 (KST)

### 측정 항목
| 메트릭 | 설명 | 수집 방법 |
|--------|------|----------|
| Avg System CPU (%) | 시스템 전체 평균 CPU | `OperatingSystemMXBean.cpuLoad` |
| Peak System CPU (%) | 시스템 전체 최대 CPU | 100ms 샘플링 최대값 |
| Avg Process CPU (%) | JVM 프로세스 평균 CPU | `OperatingSystemMXBean.processCpuLoad` |
| Peak Process CPU (%) | JVM 프로세스 최대 CPU | 100ms 샘플링 최대값 |

---

## 부록: 원시 데이터

### Ultra 복잡도 (~150 필드, 3단계 중첩)

```
HTTP/JSON:    1,996.03 req/s, avg 3.68ms, p95 5.40ms, heap 636MB, gc 36
              Avg CPU: 43.20%, Peak CPU: 67.23%, Avg Proc: 16.74%, Peak Proc: 19.41%
              
HTTP/Binary:  2,349.16 req/s, avg 3.08ms, p95 4.18ms, heap 631MB, gc 11
              Avg CPU: 44.31%, Peak CPU: 78.21%, Avg Proc: 11.85%, Peak Proc: 13.19%
              
gRPC/Unary:   1,917.48 req/s, avg 3.60ms, p95 5.27ms, heap 640MB, gc 21
              Avg CPU: 43.07%, Peak CPU: 83.12%, Avg Proc: 13.58%, Peak Proc: 15.68%
              
gRPC/Stream:  1,981.00 req/s, avg 3.46ms, p95 5.11ms, heap 639MB, gc 22
              Avg CPU: 48.89%, Peak CPU: 77.54%, Avg Proc: 13.19%, Peak Proc: 15.55%
```

### Extreme 복잡도 (~500 필드, 4단계 중첩)

```
HTTP/JSON:    413.16 req/s, avg 22.78ms, p95 28.62ms, heap 645MB, gc 76
              Avg CPU: 45.79%, Peak CPU: 63.98%, Avg Proc: 13.09%, Peak Proc: 14.89%
              
HTTP/Binary:  450.18 req/s, avg 20.64ms, p95 27.36ms, heap 634MB, gc 8
              Avg CPU: 47.35%, Peak CPU: 82.92%, Avg Proc: 3.39%, Peak Proc: 4.26%
              
gRPC/Unary:   392.42 req/s, avg 22.23ms, p95 28.00ms, heap 644MB, gc 41
              Avg CPU: 45.91%, Peak CPU: 81.59%, Avg Proc: 9.40%, Peak Proc: 11.11%
              
gRPC/Stream:  392.44 req/s, avg 22.24ms, p95 27.85ms, heap 645MB, gc 41
              Avg CPU: 45.73%, Peak CPU: 79.92%, Avg Proc: 9.38%, Peak Proc: 10.92%
```

---

## 다음 단계 제안

### 🔬 향후 별도 프로젝트로 진행 가능한 주제들

현재 Phase 7까지의 벤치마크로 핵심 패턴은 확인되었습니다.  
아래 주제들은 별도 프로젝트로 깊이 있게 다룰 수 있습니다:

---

#### 프로젝트 A: GC 압박 가설 검증

**목적:** GC가 실제로 gRPC 성능 저하의 원인인지 인과관계 검증

**테스트 항목:**
- Heap 크기 변경 (256MB / 1GB / 2GB)
- GC 알고리즘 변경 (G1GC / ZGC / Parallel GC)
- GC 로그 상세 분석
- async-profiler로 객체 할당 프로파일링

**예상 소요:** 1~2일

---

#### 프로젝트 B: 네트워크 지연 환경 테스트

**목적:** 실제 네트워크 환경에서 Protobuf 크기 절감 효과 검증

**테스트 항목:**
- tc 명령으로 지연 추가 (10ms / 50ms / 100ms)
- 패킷 손실 시뮬레이션
- 대역폭 제한 환경

**예상 소요:** 1일

---

#### 프로젝트 C: 고동시성 스케일링 테스트

**목적:** VU 증가 시 gRPC 멀티플렉싱 효과 검증

**테스트 항목:**
- VU 50 / 100 / 200 / 500 단계별 테스트
- Connection Pool 설정 최적화
- HTTP/1.1 vs HTTP/2 비교

**예상 소요:** 1일

---

#### 프로젝트 D: 실제 애플리케이션 시나리오

**목적:** DB 조회 등 I/O가 포함된 현실적 시나리오에서 비교

**테스트 항목:**
- DB 조회 + 직렬화 복합 테스트
- 캐시 적용 시나리오
- 마이크로서비스 간 통신 시뮬레이션

**예상 소요:** 2~3일

---

## 마무리

### Phase 1~7 벤치마크 시리즈 완료 🎉

| Phase | 주제 | 핵심 발견 |
|-------|------|----------|
| 1 | 기본 성능 비교 | gRPC가 단순 데이터에서 우위 |
| 2 | 페이로드 크기별 | 대용량에서 HTTP/Binary 우위 |
| 3 | 동시성 테스트 | gRPC 멀티플렉싱 효과 확인 |
| 4 | 스트리밍 비교 | gRPC Stream의 장점 |
| 5 | 데이터 복잡도 | 복잡도 증가 시 gRPC 우위 감소 |
| 6 | 극한 복잡도 | **HTTP가 gRPC 역전!** 🔥 |
| 7 | CPU 분석 | CPU 사용량 ≠ 역전 원인, 추가 검증 필요 |

### 최종 프로토콜 선택 가이드

```
┌─────────────────────────────────────────────────────────────┐
│                    프로토콜 선택 의사결정 트리                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  데이터 구조가 복잡한가? (150+ 필드, 3단계+ 중첩)            │
│       │                                                     │
│       ├─ YES → HTTP/Binary (Protobuf over HTTP)            │
│       │                                                     │
│       └─ NO → 대용량 전송인가? (200KB+)                     │
│               │                                             │
│               ├─ YES → HTTP/Binary                          │
│               │                                             │
│               └─ NO → 고동시성인가? (100+ VU)               │
│                       │                                     │
│                       ├─ YES → gRPC/Unary                   │
│                       │                                     │
│                       └─ NO → 양방향 스트리밍 필요?         │
│                               │                             │
│                               ├─ YES → gRPC/Stream          │
│                               │                             │
│                               └─ NO → gRPC/Unary (기본)     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 테스트 환경 정보

- **서버**: Spring Boot 3.x + Kotlin Coroutine
- **로드 테스터**: k6 (Grafana)
- **테스트 일시**: 2026-01-17
- **전체 소스코드**: `/home/jun/distributed-log-pipeline/proto-bench/`