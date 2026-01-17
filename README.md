# Proto-Bench

HTTP vs gRPC 프로토콜 성능 비교 벤치마크 프로젝트

## 프로젝트 목적

동일한 조건에서 HTTP와 gRPC의 실제 성능 차이를 측정하고 수치화합니다.

## 아키텍처

```
┌─────────┐      ┌─────────────┐      ┌─────────────┐
│   k6    │ ──── │  apiServer  │ ──── │ dataServer  │
│ (Load)  │      │  (Gateway)  │      │  (1MB Data) │
└─────────┘      └─────────────┘      └─────────────┘
                       │                     │
                 HTTP or gRPC          HTTP or gRPC
```

## 테스트 시나리오

| 케이스 | 프로토콜 | 직렬화 | 비고 |
|--------|----------|--------|------|
| Case 1 | HTTP/1.1 | JSON | 기본 |
| Case 2 | HTTP/2 | JSON | 멀티플렉싱 |
| Case 3 | HTTP/2 | JSON + gzip | 압축 |
| Case 4 | gRPC | Protobuf | 바이너리 |
| Case 5 | gRPC | Protobuf + Stream | 스트리밍 |

## 측정 지표

- **Latency**: avg, p50, p95, p99
- **Throughput**: requests/sec
- **Server Metrics**: CPU, Memory, GC
- **Data Transfer**: total bytes, bandwidth

## 프로젝트 구조

```
proto-bench/
├── apiServer/          # Gateway 서버 (Kotlin + Spring Boot)
│   ├── src/
│   ├── build.gradle.kts
│   └── ...
├── dataServer/         # 데이터 서버 (Kotlin + Spring Boot)
│   ├── src/
│   ├── build.gradle.kts
│   └── ...
├── scripts/            # k6 테스트 스크립트
│   ├── http-test.js
│   ├── grpc-test.js
│   └── ...
├── proto/              # gRPC Proto 파일 (공용)
│   └── data.proto
├── results/            # 벤치마크 결과 저장
├── .gitignore
└── README.md
```

## 벤치마크 엔드포인트

### apiServer

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/benchmark/start` | 측정 시작 |
| GET | `/api/data` | HTTP로 데이터 요청 |
| POST | `/benchmark/end` | 측정 종료 및 결과 반환 |

### dataServer

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/data` | 1MB 데이터 응답 (HTTP) |
| gRPC | `DataService/GetData` | 1MB 데이터 응답 (gRPC) |

## 기술 스택

- **Language**: Kotlin
- **Framework**: Spring Boot 3.x
- **gRPC**: grpc-kotlin
- **Build**: Gradle (Kotlin DSL)
- **Load Test**: k6

## 실행 방법

```bash
# 1. dataServer 실행
cd dataServer
./gradlew bootRun

# 2. apiServer 실행
cd apiServer
./gradlew bootRun

# 3. k6 테스트 실행
cd scripts
k6 run http-test.js
k6 run grpc-test.js
```

## 벤치마크 결과

> 테스트 환경: 10 VUs, 30초 지속, 1MB 페이로드
> 테스트 일시: 2026-01-14

### 성능 비교 요약

| 프로토콜 | Throughput (req/s) | Latency avg | Latency p95 | Latency p99 | 총 요청 수 |
|----------|-------------------|-------------|-------------|-------------|-----------|
| **HTTP/Binary** | **2,506.24** | **2.70ms** | **4.00ms** | **4.65ms** | 77,711 |
| gRPC/Unary | 1,186.64 | 6.87ms | 11.23ms | 14.85ms | 36,806 |
| HTTP/JSON | 514.54 | 17.42ms | 23.81ms | 26.36ms | 15,964 |
| gRPC/Stream | 210.72 | 44.53ms | 67.08ms | 75.31ms | 6,540 |

### 상세 결과

#### 1. HTTP/JSON (Base64 인코딩)
```json
{
  "protocol": "HTTP/JSON",
  "testName": "k6-http-json",
  "durationMs": 31026,
  "totalRequests": 15964,
  "successRequests": 15964,
  "failedRequests": 0,
  "throughputRps": 514.54,
  "latency": {
    "avgMs": 17.42,
    "minMs": 11.93,
    "maxMs": 72.02,
    "p50Ms": 16.63,
    "p95Ms": 23.81,
    "p99Ms": 26.36
  },
  "serverMetrics": {
    "startHeapMb": 41.88,
    "endHeapMb": 726.55,
    "peakHeapMb": 783.51,
    "gcCount": 147,
    "gcTimeMs": 302
  },
  "dataTransfer": {
    "totalBytes": 16739467264,
    "avgResponseBytes": 1048576
  }
}
```

#### 2. HTTP/Binary (Raw Bytes)
```json
{
  "protocol": "HTTP/Binary",
  "testName": "k6-http-binary",
  "durationMs": 31007,
  "totalRequests": 77711,
  "successRequests": 77711,
  "failedRequests": 0,
  "throughputRps": 2506.24,
  "latency": {
    "avgMs": 2.70,
    "minMs": 1.66,
    "maxMs": 9.99,
    "p50Ms": 2.46,
    "p95Ms": 4.00,
    "p99Ms": 4.65
  },
  "serverMetrics": {
    "startHeapMb": 726.55,
    "endHeapMb": 480.12,
    "peakHeapMb": 794.71,
    "gcCount": 134,
    "gcTimeMs": 278
  },
  "dataTransfer": {
    "totalBytes": 81485889536,
    "avgResponseBytes": 1048576
  }
}
```

#### 3. gRPC/Unary (Protobuf)
```json
{
  "protocol": "gRPC/Unary",
  "testName": "k6-grpc",
  "durationMs": 31017,
  "totalRequests": 36806,
  "successRequests": 36806,
  "failedRequests": 0,
  "throughputRps": 1186.64,
  "latency": {
    "avgMs": 6.87,
    "minMs": 1.53,
    "maxMs": 59.11,
    "p50Ms": 6.46,
    "p95Ms": 11.23,
    "p99Ms": 14.85
  },
  "serverMetrics": {
    "startHeapMb": 480.12,
    "endHeapMb": 236.73,
    "peakHeapMb": 704.12,
    "gcCount": 168,
    "gcTimeMs": 250
  },
  "dataTransfer": {
    "totalBytes": 38593888256,
    "avgResponseBytes": 1048576
  }
}
```

#### 4. gRPC/Stream (청크 전송)
```json
{
  "protocol": "gRPC/Stream",
  "testName": "k6-grpc-stream",
  "durationMs": 31036,
  "totalRequests": 6540,
  "successRequests": 6540,
  "failedRequests": 0,
  "throughputRps": 210.72,
  "latency": {
    "avgMs": 44.53,
    "minMs": 28.81,
    "maxMs": 87.75,
    "p50Ms": 42.06,
    "p95Ms": 67.08,
    "p99Ms": 75.31
  },
  "serverMetrics": {
    "startHeapMb": 236.73,
    "endHeapMb": 78.65,
    "peakHeapMb": 316.73,
    "gcCount": 27,
    "gcTimeMs": 37
  },
  "dataTransfer": {
    "totalBytes": 6857687040,
    "avgResponseBytes": 1048576
  }
}
```

### 분석

#### 🏆 HTTP/Binary가 가장 빠른 이유
1. **직렬화 오버헤드 없음**: Raw bytes 그대로 전송
2. **Base64 인코딩 불필요**: JSON 대비 33% 데이터 절감
3. **단순한 프로토콜**: HTTP/1.1의 단순함이 오히려 장점

#### 🤔 gRPC가 예상보다 느린 이유
1. **Protobuf 직렬화/역직렬화 비용**: 1MB 대용량에서 오버헤드 발생
2. **HTTP/2 멀티플렉싱 오버헤드**: 단일 요청에서는 이점 없음
3. **프레임 처리**: HTTP/2 프레임 분할/조립 비용

#### 📊 gRPC가 유리한 상황
- 작은 페이로드 (< 100KB)
- 양방향 스트리밍
- 다중 동시 요청 (HTTP/2 멀티플렉싱)
- 스키마 기반 타입 안정성 필요 시

#### 💡 결론
> **대용량 단일 페이로드 전송에서는 HTTP/Binary가 gRPC보다 2배 이상 빠르다.**
> gRPC의 장점은 작은 메시지의 대량 처리, 스트리밍, 타입 안정성에서 발휘된다.

## 라이선스

MIT License