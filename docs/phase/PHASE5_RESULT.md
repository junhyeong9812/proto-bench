# Phase 5: 복잡한 데이터 구조 직렬화 성능 테스트 결과

## 개요

| 항목 | 값 |
|------|-----|
| 데이터 복잡도 | Simple → Medium → Complex |
| 동시 사용자 (VU) | 10 |
| 테스트 시간 | 각 30초 |
| 테스트 일시 | 2026-01-17 03:43 ~ 03:50 (KST) |

## 가설

> "필드가 많고 중첩된 복잡한 객체에서는  
> JSON 파싱 비용이 Protobuf보다 커서 gRPC가 유리할 것이다"

### 배경: 이전 Phase 결과

**Phase 4에서 발견한 역전 포인트:**
- **≤100KB**: gRPC/Unary가 30~67% 더 빠름
- **≥200KB**: HTTP/Binary가 14~60% 더 빠름

**Phase 5에서 검증할 질문:**
- 동일한 "작은" 데이터라도 **구조의 복잡도**가 증가하면 성능 차이가 어떻게 변하는가?
- JSON 파싱 오버헤드 vs Protobuf 파싱 오버헤드의 실제 영향은?

---

## 테스트 데이터 구조

| 복잡도 | 필드 수 | 중첩 깊이 | 배열 요소 | 예상 JSON 크기 | 예상 Protobuf 크기 |
|--------|--------|----------|----------|---------------|-------------------|
| Simple | 5개 | 0 | 0 | ~150 bytes | ~50 bytes |
| Medium | 13개 | 1단계 | 10 (tags) + 5 (metadata) | ~800 bytes | ~300 bytes |
| Complex | 20개 | 2단계 | 10×5×3 (중첩 배열) | ~5,000 bytes | ~1,500 bytes |

### 데이터 구조 상세

```
Simple:  id, name, age, score, isActive (5개 primitive 필드)
Medium:  Simple + email, phone, timestamps, status, tags[], address{}, metadata{} 
Complex: Medium + billingAddress{}, orders[items[]], scores{}, permissions[], addresses[]
```

---

## 테스트 결과

### Simple 복잡도 (5 필드, 중첩 없음)

| 프로토콜 | Throughput (req/s) | Latency avg | Latency P95 | Peak Heap | GC Count | 순위 |
|----------|-------------------|-------------|-------------|-----------|----------|------|
| **gRPC/Unary** | **6,006.67** | 0.69ms | 1.27ms | 243 MB | 53 | 🥇 |
| gRPC/Stream | 5,915.18 | 0.67ms | 1.22ms | 347 MB | 28 | 🥈 |
| HTTP/Binary | 3,627.42 | 1.28ms | 1.97ms | 147 MB | 65 | 🥉 |
| HTTP/JSON | 3,601.55 | 1.32ms | 2.21ms | 135 MB | 119 | 4위 |

**분석:**
- gRPC/Unary가 HTTP/JSON 대비 **67% 더 빠름**
- gRPC/Unary가 HTTP/Binary 대비 **66% 더 빠름**
- 단순한 데이터에서도 gRPC가 압도적 우위

---

### Medium 복잡도 (13 필드, 1단계 중첩)

| 프로토콜 | Throughput (req/s) | Latency avg | Latency P95 | Peak Heap | GC Count | 순위 |
|----------|-------------------|-------------|-------------|-----------|----------|------|
| **gRPC/Unary** | **5,526.81** | 0.85ms | 1.56ms | 419 MB | 24 | 🥇 |
| HTTP/Binary | 3,393.10 | 1.45ms | 2.11ms | 351 MB | 19 | 🥈 |
| HTTP/JSON | 3,273.09 | 1.55ms | 2.30ms | 351 MB | 21 | 🥉 |
| gRPC/Stream | 3,057.16 | 1.61ms | 3.31ms | 429 MB | 12 | 4위 |

**분석:**
- gRPC/Unary가 HTTP/JSON 대비 **69% 더 빠름**
- gRPC/Unary가 HTTP/Binary 대비 **63% 더 빠름**
- 중첩 구조가 추가되어도 gRPC 우위 유지
- gRPC/Stream은 중첩 데이터 처리에서 성능 저하

---

### Complex 복잡도 (20 필드, 2단계 중첩)

| 프로토콜 | Throughput (req/s) | Latency avg | Latency P95 | Peak Heap | GC Count | 순위 |
|----------|-------------------|-------------|-------------|-----------|----------|------|
| **gRPC/Unary** | **4,414.52** | 1.10ms | 1.95ms | 432 MB | 20 | 🥇 |
| gRPC/Stream | 4,400.45 | 1.00ms | 1.82ms | 438 MB | 20 | 🥈 |
| HTTP/JSON | 3,153.67 | 1.74ms | 2.65ms | 425 MB | 21 | 🥉 |
| HTTP/Binary | 2,955.34 | 1.80ms | 2.51ms | 424 MB | 15 | 4위 |

**분석:**
- gRPC/Unary가 HTTP/JSON 대비 **40% 더 빠름**
- gRPC/Unary가 HTTP/Binary 대비 **49% 더 빠름**
- 복잡한 중첩 구조에서 gRPC/Stream이 HTTP보다 우위 회복
- **HTTP/Binary가 HTTP/JSON보다 느림** (역전 현상)

---

## 복잡도별 성능 변화 추이

### Throughput (req/s) 비교표

| 복잡도 | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream | gRPC 우위 |
|--------|-----------|-------------|------------|-------------|-----------|
| Simple | 3,602 | 3,627 | **6,007** | 5,915 | +67% |
| Medium | 3,273 | 3,393 | **5,527** | 3,057 | +69% |
| Complex | 3,154 | 2,955 | **4,415** | 4,400 | +40% |

### Latency P95 비교 (ms)

| 복잡도 | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream |
|--------|-----------|-------------|------------|-------------|
| Simple | 2.21 | 1.97 | **1.27** | 1.22 |
| Medium | 2.30 | 2.11 | **1.56** | 3.31 |
| Complex | 2.65 | 2.51 | **1.95** | 1.82 |

### 복잡도 증가에 따른 Throughput 감소율

| 프로토콜 | Simple→Complex | 특성 |
|----------|----------------|------|
| HTTP/JSON | -12% | 비교적 안정적 |
| HTTP/Binary | -19% | 복잡한 구조에서 약화 |
| gRPC/Unary | -27% | 성능 감소하나 여전히 최고 |
| gRPC/Stream | -26% | Simple→Medium에서 급락 후 회복 |

---

## 핵심 인사이트

### 1. 가설 검증: ✅ 부분적 확인

**예상과 다른 결과:**
- 가설: "복잡한 구조에서 gRPC가 **더** 유리해질 것"
- 실제: gRPC가 **모든 복잡도에서 우위**이나, 복잡도가 증가할수록 **격차가 줄어듦**

| 복잡도 | gRPC/Unary 우위 (vs HTTP/JSON) |
|--------|-------------------------------|
| Simple | +67% |
| Medium | +69% |
| Complex | +40% |

**해석:** Protobuf의 효율성은 복잡한 구조에서도 유지되지만, JSON 파싱 최적화(Jackson 등)도 상당히 효율적임.

### 2. 놀라운 발견: Complex에서 HTTP/Binary 역전

| 복잡도 | HTTP/JSON vs HTTP/Binary |
|--------|--------------------------|
| Simple | JSON < Binary (+1%) |
| Medium | JSON < Binary (+4%) |
| **Complex** | **JSON > Binary (-6%)** |

**원인 분석:**
- HTTP/Binary(Protobuf over HTTP)는 **역직렬화 불필요** 시 효율적
- Complex 데이터에서 Protobuf 빌딩 오버헤드가 JSON 직렬화보다 커짐
- 서버에서 DTO → Protobuf 변환 비용이 DTO → JSON 변환보다 높음

### 3. gRPC/Stream 특이 패턴

| 복잡도 | gRPC/Stream 성능 |
|--------|------------------|
| Simple | 5,915 req/s (2위) |
| Medium | 3,057 req/s (4위) ⬇️ 급락 |
| Complex | 4,400 req/s (2위) ⬆️ 회복 |

**해석:**
- Medium 데이터(배열 10개 + 맵 5개)에서 스트림 오버헤드 최대화
- Complex 데이터에서는 스트림의 청크 단위 전송이 효율적으로 작동

### 4. Phase 4와의 비교

| 비교 기준 | Phase 4 (페이로드 크기) | Phase 5 (구조 복잡도) |
|----------|----------------------|---------------------|
| gRPC 우위 조건 | ≤100KB | 모든 복잡도 |
| HTTP 우위 조건 | ≥200KB | 없음 |
| 역전 포인트 | 100~200KB | 없음 |

**결론:** 구조적 복잡성만으로는 HTTP가 gRPC를 추월하지 못함. **절대적 데이터 크기**가 역전의 핵심 요인.

---

## 직렬화 효율성 비교

### 예상 크기 vs 실제 성능

| 복잡도 | JSON 예상 | Protobuf 예상 | 절감률 | 성능 차이 |
|--------|----------|--------------|--------|----------|
| Simple | ~150 bytes | ~50 bytes | ~67% | gRPC +67% |
| Medium | ~800 bytes | ~300 bytes | ~63% | gRPC +69% |
| Complex | ~5,000 bytes | ~1,500 bytes | ~70% | gRPC +40% |

**관찰:** 크기 절감률과 성능 향상이 비례하지 않음
- Simple: 크기 67% 절감 → 성능 67% 향상 ✅ 일치
- Complex: 크기 70% 절감 → 성능 40% 향상 ❌ 불일치

**원인:** 복잡한 중첩 구조에서 Protobuf 빌딩/파싱 오버헤드 증가

---

## 서버 메트릭 분석

### Peak Heap 메모리 사용량

| 복잡도 | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream |
|--------|-----------|-------------|------------|-------------|
| Simple | 135 MB | 147 MB | 243 MB | 347 MB |
| Medium | 351 MB | 351 MB | 419 MB | 429 MB |
| Complex | 425 MB | 424 MB | 432 MB | 438 MB |

**관찰:** gRPC가 더 많은 메모리 사용, 특히 Simple 데이터에서 차이가 큼

### GC Count

| 복잡도 | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream |
|--------|-----------|-------------|------------|-------------|
| Simple | 119 | 65 | 53 | 28 |
| Medium | 21 | 19 | 24 | 12 |
| Complex | 21 | 15 | 20 | 20 |

**관찰:**
- Simple에서 HTTP/JSON의 GC가 매우 많음 (119회)
- gRPC는 메모리를 더 쓰지만 GC 압박은 적음

---

## 최종 결론

### 프로토콜 선택 가이드 (복잡도 기준)

| 데이터 복잡도 | 추천 프로토콜 | 성능 우위 | 근거 |
|--------------|--------------|----------|------|
| **Simple** (5 필드) | **gRPC/Unary** | +67% | HTTP 대비 압도적 |
| **Medium** (13 필드) | **gRPC/Unary** | +69% | 중첩 구조에서도 우위 |
| **Complex** (20+ 필드) | **gRPC/Unary** | +40% | 여전히 최고 성능 |

### Phase 1~5 종합 권장사항

| 조건 | 추천 프로토콜 | 근거 |
|------|--------------|------|
| 소용량 API (≤100KB) | **gRPC/Unary** | 30~67% 더 빠름 |
| 대용량 전송 (≥200KB) | **HTTP/Binary** | 14~60% 더 빠름 |
| 복잡한 구조 (중첩 객체) | **gRPC/Unary** | 40~69% 더 빠름 |
| 고동시성 (200+ VU) | **gRPC/Unary** | 멀티플렉싱 효과 |
| 실시간 양방향 | **gRPC/Stream** | 기능적 요구 |

### 핵심 발견

```
1. gRPC/Unary는 모든 복잡도에서 HTTP를 능가함
2. 구조적 복잡성 증가 → gRPC 우위 감소 (67% → 40%)
3. 크기 절감률 ≠ 성능 향상률 (중첩 구조 빌딩 비용)
4. Complex 데이터: HTTP/Binary < HTTP/JSON (역전 현상)
5. gRPC/Stream: Medium에서 성능 급락, Complex에서 회복
```

---

## 테스트 환경

- **서버**: Spring Boot + Kotlin (Coroutine)
- **gRPC 서버**: 포트 9091
- **HTTP 서버**: 포트 8081
- **로드 테스터**: k6 (Grafana)
- **테스트 일시**: 2026-01-17 03:43 ~ 03:50 (KST)
- **총 테스트 시간**: 약 7분 (12개 테스트 × 30초)

---

## 부록: 원시 데이터

### Simple 복잡도

```
HTTP/JSON:    3,601.55 req/s, avg 1.32ms, p95 2.21ms
HTTP/Binary:  3,627.42 req/s, avg 1.28ms, p95 1.97ms
gRPC/Unary:   6,006.67 req/s, avg 0.69ms, p95 1.27ms
gRPC/Stream:  5,915.18 req/s, avg 0.67ms, p95 1.22ms
```

### Medium 복잡도

```
HTTP/JSON:    3,273.09 req/s, avg 1.55ms, p95 2.30ms
HTTP/Binary:  3,393.10 req/s, avg 1.45ms, p95 2.11ms
gRPC/Unary:   5,526.81 req/s, avg 0.85ms, p95 1.56ms
gRPC/Stream:  3,057.16 req/s, avg 1.61ms, p95 3.31ms
```

### Complex 복잡도

```
HTTP/JSON:    3,153.67 req/s, avg 1.74ms, p95 2.65ms
HTTP/Binary:  2,955.34 req/s, avg 1.80ms, p95 2.51ms
gRPC/Unary:   4,414.52 req/s, avg 1.10ms, p95 1.95ms
gRPC/Stream:  4,400.45 req/s, avg 1.00ms, p95 1.82ms
```