# Phase 1: 대용량 단일 페이로드 성능 비교

## 실험 개요

### 목적
HTTP와 gRPC 프로토콜의 대용량 데이터(1MB) 전송 성능을 비교하여, 어떤 프로토콜이 더 효율적인지 검증합니다.

### 가설
> "gRPC는 HTTP/2 기반이고 Protobuf를 사용하므로 HTTP/JSON보다 빠를 것이다"

이것은 많은 개발자들이 가지고 있는 일반적인 가정입니다.

### 테스트 환경

| 항목 | 값 |
|------|-----|
| 페이로드 크기 | 1MB (1,048,576 bytes) |
| 동시 사용자 (VU) | 10 |
| 테스트 시간 | 30초 |
| JIT 워밍업 | 각 테스트 전 5초 워밍업 |

### 테스트 시나리오

4가지 프로토콜 방식을 비교했습니다:

1. **HTTP/JSON**: HTTP/1.1 + JSON (Base64 인코딩)
2. **HTTP/Binary**: HTTP/1.1 + Raw bytes (application/octet-stream)
3. **gRPC/Unary**: HTTP/2 + Protobuf (단일 요청-응답)
4. **gRPC/Stream**: HTTP/2 + Protobuf (서버 스트리밍, 64KB 청크)

---

## 실험 결과

### 성능 비교 요약

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | 총 요청 수 |
|----------|-------------------|-------------|-------------|-----------|
| **HTTP/Binary** | **2,506.24** | **2.70ms** | **4.00ms** | 77,711 |
| gRPC/Unary | 1,186.64 | 6.87ms | 11.23ms | 36,806 |
| HTTP/JSON | 514.54 | 17.42ms | 23.81ms | 15,964 |
| gRPC/Stream | 210.72 | 44.53ms | 67.08ms | 6,540 |

### 시각화

```
처리량 (Requests/Second)
═══════════════════════════════════════════════════════════════════

HTTP/Binary  ████████████████████████████████████████████████████ 2,506
gRPC/Unary   ████████████████████████                              1,187
HTTP/JSON    ██████████                                              515
gRPC/Stream  ████                                                     211

응답 시간 (Latency - ms, 낮을수록 좋음)
═══════════════════════════════════════════════════════════════════

HTTP/Binary  ███                                                    2.70
gRPC/Unary   ███████                                               6.87
HTTP/JSON    █████████████████                                    17.42
gRPC/Stream  █████████████████████████████████████████████       44.53
```

### 상세 결과 데이터

#### HTTP/Binary (1위)
```
Protocol: HTTP/Binary
Throughput: 2,506.24 req/s
Latency:
  - Average: 2.70ms
  - P50: 2.00ms
  - P95: 4.00ms
  - P99: 6.00ms
Data Transfer: 81.5 GB (30초)
Server Metrics:
  - Peak Heap: 794.71 MB
  - GC Count: 134
  - GC Time: 278ms
```

#### gRPC/Unary (2위)
```
Protocol: gRPC/Unary
Throughput: 1,186.64 req/s
Latency:
  - Average: 6.87ms
  - P50: 6.00ms
  - P95: 11.23ms
  - P99: 15.00ms
Data Transfer: 38.6 GB (30초)
```

#### HTTP/JSON (3위)
```
Protocol: HTTP/JSON
Throughput: 514.54 req/s
Latency:
  - Average: 17.42ms
  - P50: 16.00ms
  - P95: 23.81ms
  - P99: 30.00ms
Data Transfer: 16.7 GB (30초)
```

#### gRPC/Stream (4위)
```
Protocol: gRPC/Stream
Throughput: 210.72 req/s
Latency:
  - Average: 44.53ms
  - P50: 42.00ms
  - P95: 67.08ms
  - P99: 85.00ms
Data Transfer: 6.9 GB (30초)
```

---

## 결과 분석

### 예상과 다른 결과

**결론: gRPC가 HTTP보다 느렸다!**

이 결과는 많은 개발자들의 예상과 반대입니다. 왜 이런 결과가 나왔을까요?

### HTTP/Binary가 가장 빠른 이유

1. **직렬화 오버헤드 없음**
   ```
   HTTP/Binary 처리 과정:
   byte[] → 그대로 전송 → byte[] 수신
   
   gRPC 처리 과정:
   byte[] → ByteString.copyFrom() → Protobuf 직렬화 
         → 전송 → Protobuf 역직렬화 → ByteString → byte[]
   ```

2. **Base64 인코딩 불필요**
    - HTTP/JSON은 바이너리를 Base64로 인코딩 (33% 크기 증가)
    - HTTP/Binary는 raw bytes 그대로 전송

3. **프로토콜 단순함**
    - HTTP/1.1은 단순한 텍스트 프로토콜
    - 1MB 단일 페이로드에서는 복잡한 기능이 필요 없음

### gRPC가 예상보다 느린 이유

1. **Protobuf 직렬화/역직렬화 비용**
   ```kotlin
   // 1MB 데이터를 ByteString으로 변환하는 비용
   ByteString.copyFrom(payload)  // 내부적으로 복사 발생
   ```
    - 대용량 바이너리에서 Protobuf의 장점이 없음
    - 오히려 변환 오버헤드만 추가됨

2. **HTTP/2 프레이밍 오버헤드**
   ```
   HTTP/2 프레임 구조:
   ┌─────────────────────┐
   │ Frame Header (9B)   │ ← 모든 프레임마다 추가
   ├─────────────────────┤
   │ Payload             │
   └─────────────────────┘
   
   1MB 데이터 = 약 62개 프레임 (16KB 기본 프레임 크기)
   62 × 9 bytes = 558 bytes 오버헤드
   ```

3. **단일 요청에서 멀티플렉싱 무의미**
    - HTTP/2의 장점인 멀티플렉싱은 동시 요청에서 빛남
    - 순차적 요청에서는 HTTP/1.1과 차이 없음

4. **gRPC 스트리밍의 추가 오버헤드**
   ```
   Unary: 1 요청 → 1 응답
   Stream: 1 요청 → 16 청크 응답 (64KB × 16 = 1MB)
   
   각 청크마다:
   - Protobuf 메시지 생성 (RequestId, ChunkIndex 등)
   - Frame 헤더 추가
   - 네트워크 왕복
   ```

### HTTP/JSON이 gRPC/Stream보다 빠른 이유

```
HTTP/JSON 오버헤드:
- Base64 인코딩 (+33% 크기)
- JSON 구조 (괄호, 콜론 등)
총 증가: 약 35%

gRPC/Stream 오버헤드:
- 16개의 개별 메시지
- 각 메시지마다 헤더 필드들
- 프레임 분할/조립
- 스트림 관리
```

놀랍게도, 단순한 크기 증가보다 메시지 분할의 오버헤드가 더 큽니다.

---

## 핵심 인사이트

### gRPC가 유리한 상황 (이 실험에서 확인 안 됨)

| 시나리오 | 이유 |
|---------|------|
| 작은 페이로드 (< 10KB) | Protobuf 직렬화 이점 발휘 |
| 동시 다중 요청 | HTTP/2 멀티플렉싱 활용 |
| 복잡한 데이터 구조 | JSON 파싱 vs Protobuf 파싱 |
| 양방향 스트리밍 | gRPC의 고유 기능 |
| 다국어 시스템 | 스키마 기반 코드 생성 |

### HTTP/Binary가 유리한 상황 (이 실험에서 확인됨)

| 시나리오 | 이유 |
|---------|------|
| 대용량 바이너리 전송 | 변환 없이 그대로 전송 |
| 단순한 API | 복잡한 기능 불필요 |
| 브라우저 직접 호출 | HTTP 네이티브 지원 |

---

## 결론

### Phase 1 실험 결론

> **"대용량 단일 페이로드(1MB) 전송에서는 HTTP/Binary가 gRPC보다 약 2배 빠르다"**

이는 gRPC의 강점이 이 시나리오에서 발휘되지 않기 때문입니다:
- Protobuf는 복잡한 객체에서 강점
- HTTP/2 멀티플렉싱은 동시 요청에서 강점
- 스트리밍은 실시간 데이터에서 강점

### 다음 단계

gRPC의 강점을 확인하기 위해 추가 실험이 필요합니다:

1. **Phase 2**: 작은 페이로드 (1KB, 10KB)로 테스트
2. **Phase 3**: 높은 동시성 (100+ VU)으로 멀티플렉싱 효과 확인
3. **Phase 4**: 복잡한 데이터 구조로 직렬화 성능 비교

---

## 부록: 실험 재현 방법

```bash
# 1. dataServer 실행
cd dataServer && ./gradlew bootRun

# 2. apiServer 실행 (새 터미널)
cd apiServer && ./gradlew bootRun

# 3. 테스트 실행 (새 터미널)
cd scripts && ./run-all.sh
```

### 결과 파일 위치
- `results/http_json_YYYYMMDD_HHMMSS.log`
- `results/http_binary_YYYYMMDD_HHMMSS.log`
- `results/grpc_unary_YYYYMMDD_HHMMSS.log`
- `results/grpc_stream_YYYYMMDD_HHMMSS.log`