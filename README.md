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

## 결과 예시

```json
{
  "protocol": "gRPC",
  "duration_ms": 30000,
  "total_requests": 15420,
  "throughput_rps": 514.0,
  "latency": {
    "avg_ms": 12.3,
    "p50_ms": 10.1,
    "p95_ms": 25.4,
    "p99_ms": 48.2
  }
}
```

## 라이선스

MIT License