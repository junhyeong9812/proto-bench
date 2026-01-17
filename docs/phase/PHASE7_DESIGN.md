# Phase 7: CPU 사용량 포함 극한 복잡도 성능 분석

## 목적

Phase 6에서 발견한 **HTTP/JSON이 gRPC를 역전하는 현상**의 원인을 CPU 사용량 관점에서 분석한다.  
Protobuf 빌더 객체 생성이 실제로 CPU 집약적인지, 그리고 어떤 프로토콜이 CPU 효율적인지 검증한다.

## 가설

> "gRPC/Protobuf는 극한 복잡도(~500 필드, 4단계 중첩)에서  
> 빌더 객체 생성으로 인해 JSON보다 더 많은 CPU를 사용하며,  
> 이것이 성능 역전의 주된 원인이다"

## 배경: Phase 6 결과 분석

### Phase 6에서 확인된 역전 현상

| 복잡도 | 필드 수 | HTTP/JSON | gRPC/Unary | 승자 |
|--------|--------|-----------|------------|------|
| Simple | ~5개 | 3,602 | 6,007 | gRPC +67% |
| Medium | ~13개 | 3,273 | 5,527 | gRPC +69% |
| Complex | ~50개 | 3,154 | 4,415 | gRPC +40% |
| **Ultra** | **~150개** | **2,074** | **1,847** | **HTTP +12%** 🔥 |
| **Extreme** | **~500개** | **419** | **407** | **HTTP +3%** 🔥 |

### 미해결 질문

1. **역전의 근본 원인은 무엇인가?**
   - 가설: Protobuf 빌더 객체 생성 비용
   - 검증 필요: CPU 사용량 측정

2. **CPU 사용량과 Throughput의 관계는?**
   - 더 많은 CPU를 사용하면 더 빠른가?
   - 아니면 비효율적인 CPU 사용이 병목인가?

3. **프로토콜별 CPU 효율성 차이는?**
   - CPU 1% 당 처리할 수 있는 요청 수
   - 어떤 프로토콜이 가장 효율적인가?

---

## 새로운 측정 항목

### 기존 측정 항목 (Phase 1~6)

- Throughput (req/s)
- Latency (avg, p50, p95, p99)
- Memory (Peak Heap, GC Count, GC Time)
- Data Transfer (Total Bytes)

### Phase 7 추가 측정 항목

| 메트릭 | 설명 | 수집 방법 |
|--------|------|----------|
| **Avg System CPU (%)** | 테스트 중 시스템 전체 평균 CPU 사용률 | `OperatingSystemMXBean.cpuLoad` |
| **Peak System CPU (%)** | 시스템 전체 최대 CPU 사용률 | 100ms 간격 샘플링 최대값 |
| **Avg Process CPU (%)** | JVM 프로세스 평균 CPU 사용률 | `OperatingSystemMXBean.processCpuLoad` |
| **Peak Process CPU (%)** | JVM 프로세스 최대 CPU 사용률 | 100ms 간격 샘플링 최대값 |
| **CPU 효율성** | Throughput / Avg Process CPU | 계산값 (req/s/%) |

### CPU 효율성 정의

```
CPU 효율성 = Throughput (req/s) / Avg Process CPU (%)

의미: CPU 1% 당 처리할 수 있는 초당 요청 수
높을수록 CPU를 효율적으로 사용하는 것
```

---

## 테스트 시나리오

### 복잡도 설정 (Phase 6과 동일)

| 복잡도 | 필드 수 | 중첩 깊이 | 빌더 호출 | 예상 JSON 크기 | 예상 Protobuf 크기 |
|--------|--------|----------|----------|---------------|-------------------|
| Ultra | ~150개 | 3단계 | ~200회 | ~15 KB | ~5 KB |
| Extreme | ~500개 | 4단계 | ~800회 | ~50 KB | ~15 KB |

### 테스트 조건

| 항목 | 값 |
|------|-----|
| 동시 사용자 (VU) | 10 |
| 테스트 시간 | 30초 |
| CPU 샘플링 간격 | 100ms |
| JIT 워밍업 | 각 복잡도별 50회 × 4 프로토콜 |

---

## 예상 결과

### CPU 사용량 예상

| 복잡도 | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream |
|--------|-----------|-------------|------------|-------------|
| Ultra | ~40% | ~45% | **~55%** | ~55% |
| Extreme | ~60% | ~65% | **~75%** | ~75% |

**예상 근거:**
- Protobuf 빌더 객체 생성은 Java 객체 할당 + 초기화 필요
- JSON은 StringBuilder 기반 문자열 연결 (상대적으로 가벼움)
- 빌더 호출 수가 많을수록 (200회 → 800회) 차이 확대

### CPU 효율성 예상

| 복잡도 | HTTP/JSON | gRPC/Unary | 효율성 비교 |
|--------|-----------|------------|------------|
| Ultra | ~52 req/s/% | ~34 req/s/% | HTTP 1.5배 효율적 |
| Extreme | ~7 req/s/% | ~5 req/s/% | HTTP 1.4배 효율적 |

**예상 근거:**
- HTTP/JSON: 낮은 CPU 사용 + 높은 Throughput = 높은 효율성
- gRPC/Unary: 높은 CPU 사용 + 낮은 Throughput = 낮은 효율성

---

## 구현 변경 사항

### 1. BenchmarkCollector.kt 수정

```kotlin
// 기존
data class ServerMetrics(
    val startHeapMb: Double,
    val endHeapMb: Double,
    val peakHeapMb: Double,
    val gcCount: Long,
    val gcTimeMs: Long
)

// Phase 7 추가
data class ServerMetrics(
    val startHeapMb: Double,
    val endHeapMb: Double,
    val peakHeapMb: Double,
    val gcCount: Long,
    val gcTimeMs: Long,
    // CPU 메트릭 추가
    val avgCpuUsagePercent: Double,      // 시스템 전체 평균 CPU
    val peakCpuUsagePercent: Double,     // 시스템 전체 피크 CPU
    val avgProcessCpuPercent: Double,    // JVM 프로세스 평균 CPU
    val peakProcessCpuPercent: Double    // JVM 프로세스 피크 CPU
)
```

### 2. CPU 샘플링 구현

```kotlin
// OperatingSystemMXBean 사용
private val osMXBean = ManagementFactory.getOperatingSystemMXBean() 
    as com.sun.management.OperatingSystemMXBean

// 100ms 간격 샘플링 스레드
private var cpuSamplingThread: Thread? = null

fun start(...) {
    // ... 기존 코드 ...
    
    // CPU 샘플링 시작
    cpuSamplingThread = Thread {
        while (isRunning.get()) {
            val systemCpu = osMXBean.cpuLoad * 100
            val processCpu = osMXBean.processCpuLoad * 100
            
            cpuUsageSamples.add(systemCpu)
            processCpuSamples.add(processCpu)
            
            if (systemCpu > peakCpuUsage) peakCpuUsage = systemCpu
            if (processCpu > peakProcessCpu) peakProcessCpu = processCpu
            
            Thread.sleep(100)
        }
    }.apply { isDaemon = true; start() }
}
```

### 3. k6 스크립트 출력 추가

```javascript
// teardown 함수에서 CPU 메트릭 출력
console.log('\nCPU Metrics:');
console.log(`  Avg System CPU: ${result.serverMetrics.avgCpuUsagePercent.toFixed(2)}%`);
console.log(`  Peak System CPU: ${result.serverMetrics.peakCpuUsagePercent.toFixed(2)}%`);
console.log(`  Avg Process CPU: ${result.serverMetrics.avgProcessCpuPercent.toFixed(2)}%`);
console.log(`  Peak Process CPU: ${result.serverMetrics.peakProcessCpuPercent.toFixed(2)}%`);
```

---

## 결과 분석 프레임워크

### 1. Throughput vs CPU 사용량 관계

```
케이스 A: 높은 Throughput + 낮은 CPU = 최고 (효율적)
케이스 B: 높은 Throughput + 높은 CPU = 양호 (CPU 활용)
케이스 C: 낮은 Throughput + 높은 CPU = 문제 (비효율적) ← gRPC 예상
케이스 D: 낮은 Throughput + 낮은 CPU = I/O 병목 (해당 없음)
```

### 2. 역전 원인 검증

| 가설 | 검증 방법 | 예상 결과 |
|------|----------|----------|
| Protobuf 빌더 비용 | gRPC CPU > HTTP CPU | gRPC가 10~20% 더 많은 CPU 사용 |
| 객체 생성 오버헤드 | GC Count 비교 | gRPC GC가 더 많음 |
| 직렬화 비용 | Process CPU 비교 | gRPC Process CPU가 더 높음 |

### 3. CPU 효율성 분석

```
효율성 순위 예상:
1. HTTP/JSON     - 가장 효율적 (문자열 연결이 가벼움)
2. HTTP/Binary   - 2위 (Protobuf 빌딩 but HTTP 오버헤드 적음)
3. gRPC/Unary    - 3위 (Protobuf + gRPC 오버헤드)
4. gRPC/Stream   - 4위 (스트리밍 관리 추가 비용)
```

---

## 실행 방법

```bash
cd scripts

# Ultra 단독 테스트
COMPLEXITY_LIST="ultra" ./run-phase7.sh

# Extreme 단독 테스트
COMPLEXITY_LIST="extreme" ./run-phase7.sh

# 전체 비교 (Ultra vs Extreme)
./run-phase7.sh
```

---

## 출력 형식

### 1. Throughput & Latency 테이블

```
| 복잡도  | 프로토콜    | Throughput | Latency P95 | JSON vs gRPC |
|---------|------------|------------|-------------|--------------|
| ultra   | HTTP/JSON  |    2,074   |    5.09ms   | **JSON +12%**|
| ultra   | HTTP/Binary|    2,154   |    4.91ms   |              |
| ultra   | gRPC/Unary |    1,847   |    5.50ms   |              |
| ultra   | gRPC/Stream|    1,834   |    5.49ms   |              |
```

### 2. CPU 사용량 테이블

```
| 복잡도  | 프로토콜    | Avg CPU(%) | Peak CPU(%) | Avg Proc(%) | Peak Proc(%) |
|---------|------------|------------|-------------|-------------|--------------|
| ultra   | HTTP/JSON  |    35.2    |    48.5     |    32.1     |    45.3      |
| ultra   | HTTP/Binary|    38.7    |    52.1     |    35.4     |    48.7      |
| ultra   | gRPC/Unary |    45.3    |    58.9     |    42.8     |    55.2      |
| ultra   | gRPC/Stream|    46.1    |    59.5     |    43.5     |    56.1      |
```

### 3. CPU 효율성 테이블

```
| 복잡도  | 프로토콜    | Throughput | Avg CPU | 효율성 (req/s/%) |
|---------|------------|------------|---------|-----------------|
| ultra   | HTTP/JSON  |    2,074   |   32.1  |      64.6       |
| ultra   | HTTP/Binary|    2,154   |   35.4  |      60.8       |
| ultra   | gRPC/Unary |    1,847   |   42.8  |      43.2       |
| ultra   | gRPC/Stream|    1,834   |   43.5  |      42.2       |
```

---

## 검증할 질문

### 1. CPU 사용량 비교

- [ ] gRPC가 HTTP보다 더 많은 CPU를 사용하는가?
- [ ] 복잡도 증가에 따른 CPU 증가율은 어떻게 다른가?
- [ ] Peak CPU vs Avg CPU 차이는 프로토콜별로 다른가?

### 2. CPU 효율성 비교

- [ ] 어떤 프로토콜이 CPU 1% 당 가장 많은 요청을 처리하는가?
- [ ] 극한 복잡도에서 효율성 역전이 발생하는가?
- [ ] 효율성 차이는 복잡도에 따라 확대되는가?

### 3. 역전 원인 검증

- [ ] 높은 CPU 사용량이 낮은 성능의 원인인가?
- [ ] Protobuf 빌더 생성이 CPU 집약적임을 확인할 수 있는가?
- [ ] GC 빈도와 CPU 사용량의 상관관계는?

---

## 예상 결론

### Phase 7 가설 검증 예상 결과

| 가설 | 예상 결과 |
|------|----------|
| "gRPC가 더 많은 CPU 사용" | ✅ 확인될 것 (10~20% 더 높음) |
| "빌더 생성이 CPU 집약적" | ✅ 확인될 것 (Process CPU 차이) |
| "HTTP가 더 효율적" | ✅ 확인될 것 (효율성 1.3~1.5배) |

### 최종 인사이트 예상

```
역전의 핵심 원인:
1. Protobuf 빌더 객체 생성/해제 비용이 복잡도에 비례하여 증가
2. JSON은 StringBuilder 기반으로 상대적으로 가벼움
3. gRPC는 더 많은 CPU를 사용하지만 더 낮은 Throughput
4. CPU 효율성 관점에서 HTTP/JSON이 가장 효율적

실무 적용:
- 극한 복잡도 데이터 처리 시 HTTP/JSON 고려
- CPU 리소스가 제한된 환경에서는 HTTP가 유리
- gRPC는 중간 복잡도 이하에서 사용 권장
```

---

## 파일 구조

```
scripts/
├── phase7/
│   ├── http-json-test.js      # HTTP/JSON 테스트 (CPU 메트릭 출력)
│   ├── http-binary-test.js    # HTTP/Binary 테스트 (CPU 메트릭 출력)
│   ├── grpc-test.js           # gRPC/Unary 테스트 (CPU 메트릭 출력)
│   └── grpc-stream-test.js    # gRPC/Stream 테스트 (CPU 메트릭 출력)
├── run-phase7.sh              # 전체 실행 스크립트 (CPU 분석 포함)
└── results/
    └── phase7/
        ├── http-json_ultra_YYYYMMDD_HHMMSS.log
        ├── http-binary_ultra_YYYYMMDD_HHMMSS.log
        ├── grpc-unary_ultra_YYYYMMDD_HHMMSS.log
        ├── grpc-stream_ultra_YYYYMMDD_HHMMSS.log
        └── summary_YYYYMMDD_HHMMSS.json
```

---

## 상태

📋 **설계 완료 - 구현 완료**

---

## 다음 단계

### Phase 8 후보

1. **네트워크 지연 시뮬레이션**
   - 실제 네트워크 환경에서의 CPU 효율성 변화
   - Protobuf 크기 절감 효과 vs CPU 오버헤드 트레이드오프

2. **멀티코어 스케일링 테스트**
   - CPU 코어 수 제한 시 프로토콜별 성능 변화
   - gRPC의 비동기 처리가 멀티코어에서 유리한지 검증

3. **Protobuf Arena Allocator 적용**
   - 빌더 객체 재사용으로 CPU 오버헤드 감소 가능 여부
   - Java/Kotlin에서의 적용 가능성 검토