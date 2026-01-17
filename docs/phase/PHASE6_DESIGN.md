# Phase 6: 극한 복잡도에서의 역전 포인트 탐색

## 목적

Phase 5에서 발견한 "복잡도 증가 시 gRPC 우위 감소" 패턴을 확장하여,  
**구조적 복잡성만으로 HTTP가 gRPC를 역전하는 포인트**를 찾는다.

## 가설

> "Protobuf 빌더 객체 생성 오버헤드가 JSON 문자열 연결보다 커지는  
> 임계점(~500개 필드, 4단계 중첩)에서 HTTP가 gRPC를 추월할 것이다"

## 배경: Phase 5 결과 분석

### 복잡도별 성능 격차

| 복잡도 | 필드 수 | 중첩 | gRPC/Unary | HTTP/JSON | gRPC 우위 |
|--------|--------|------|-----------|-----------|----------|
| Simple | 5개 | 0단계 | 6,007 | 3,602 | **+67%** |
| Medium | 13개 | 1단계 | 5,527 | 3,273 | **+69%** |
| Complex | 50개 | 2단계 | 4,415 | 3,154 | **+40%** |

### 성능 감소 패턴

| 구간 | gRPC 감소율 | HTTP 감소율 | 격차 변화 |
|------|-----------|-----------|----------|
| Simple→Medium | -8% | -9% | +2% |
| Medium→Complex | -20% | -4% | **-27%** |

**핵심 발견:** HTTP가 복잡도 증가에 더 잘 버틴다!

### 역전 포인트 추정

```
현재 추세 (Complex 이후):
- gRPC: 매 단계 ~20% 감소
- HTTP: 매 단계 ~4% 감소

예상 역전 포인트:
- Ultra (~150 필드): gRPC +15~20%
- Extreme (~500 필드): HTTP 우위 시작?
```

---

## 테스트 데이터 구조

### 현재 Complex (기준점)

```
Complex (50개 필드, 2단계 중첩):
├── 기본 필드 10개
├── tags[10]
├── address{}
├── billingAddress{}
├── orders[5]
│   └── items[3] (이중 중첩)
├── metadata{10}
├── scores{5}
├── permissions[10]
└── addresses[3]

총 빌더 호출: ~30회
```

### Ultra (신규)

```
Ultra (~150개 필드, 3단계 중첩):
├── 기본 필드 15개
├── tags[20]
├── addresses[10]
│   └── contacts[5] (이중 중첩)
├── orders[10]
│   └── items[5]
│       └── attributes[3] (삼중 중첩)
├── metadata{20}
├── scores{10}
├── permissions[20]
├── categories[10]
│   └── subcategories[5]
└── history[20]
    └── changes[3]

총 빌더 호출: ~200회
예상 JSON 크기: ~15KB
예상 Protobuf 크기: ~5KB
```

### Extreme (신규)

```
Extreme (~500개 필드, 4단계 중첩):
├── 기본 필드 20개
├── tags[50]
├── organizations[10]
│   └── departments[5]
│       └── teams[5]
│           └── members[3] (4중 중첩)
├── orders[20]
│   └── items[10]
│       └── attributes[5]
│           └── values[3]
├── metadata{50}
├── scores{20}
├── permissions[50]
├── addresses[20]
│   └── contacts[5]
└── events[30]
    └── participants[5]
        └── roles[3]

총 빌더 호출: ~800회
예상 JSON 크기: ~50KB
예상 Protobuf 크기: ~15KB
```

---

## 테스트 시나리오

| 시나리오 | 필드 수 | 중첩 깊이 | 배열 요소 | 빌더 호출 |
|---------|--------|----------|----------|----------|
| Complex | ~50개 | 2단계 | 10×5×3 | ~30회 |
| **Ultra** | ~150개 | 3단계 | 10×10×5×3 | ~200회 |
| **Extreme** | ~500개 | 4단계 | 10×20×10×5×3 | ~800회 |

---

## 예상 결과

| 복잡도 | gRPC/Unary | HTTP/JSON | 예상 승자 | 예상 격차 |
|--------|-----------|-----------|----------|----------|
| Complex | 4,415 | 3,154 | gRPC | +40% |
| Ultra | ~3,500 | ~3,000 | gRPC | +15~20% |
| Extreme | ~2,800 | ~2,900 | **HTTP?** | **+3~5%** |

### 크기 비교 예상

| 복잡도 | JSON 크기 | Protobuf 크기 | 절감률 |
|--------|----------|--------------|--------|
| Complex | ~5KB | ~1.5KB | ~70% |
| Ultra | ~15KB | ~5KB | ~67% |
| Extreme | ~50KB | ~15KB | ~70% |

---

## 구현 계획

### 1. Proto 파일 확장

```protobuf
// Ultra 데이터 (3단계 중첩)
message UltraData {
    // 기본 필드 15개
    string id = 1;
    // ... 

    // 3단계 중첩 구조
    repeated Category categories = 20;
    repeated HistoryEntry history = 21;
}

message Category {
    string id = 1;
    string name = 2;
    repeated SubCategory subcategories = 3;
}

message SubCategory {
    string id = 1;
    string name = 2;
    repeated string items = 3;
}

// Extreme 데이터 (4단계 중첩)
message ExtremeData {
    // 기본 필드 20개
    // ...

    // 4단계 중첩 구조
    repeated Organization organizations = 30;
    repeated Event events = 31;
}

message Organization {
    string id = 1;
    repeated Department departments = 2;
}

message Department {
    string id = 1;
    repeated Team teams = 2;
}

message Team {
    string id = 1;
    repeated Member members = 2;
}

message Member {
    string id = 1;
    string name = 2;
    string role = 3;
}
```

### 2. DataService 확장

```kotlin
// DataService.kt에 추가

companion object {
    // Ultra 설정
    const val ULTRA_TAGS_COUNT = 20
    const val ULTRA_ADDRESSES_COUNT = 10
    const val ULTRA_CONTACTS_PER_ADDRESS = 5
    const val ULTRA_ORDERS_COUNT = 10
    const val ULTRA_ITEMS_PER_ORDER = 5
    const val ULTRA_ATTRIBUTES_PER_ITEM = 3
    const val ULTRA_CATEGORIES_COUNT = 10
    const val ULTRA_SUBCATEGORIES_COUNT = 5
    const val ULTRA_HISTORY_COUNT = 20
    const val ULTRA_CHANGES_PER_HISTORY = 3

    // Extreme 설정
    const val EXTREME_TAGS_COUNT = 50
    const val EXTREME_ORGS_COUNT = 10
    const val EXTREME_DEPTS_PER_ORG = 5
    const val EXTREME_TEAMS_PER_DEPT = 5
    const val EXTREME_MEMBERS_PER_TEAM = 3
    const val EXTREME_EVENTS_COUNT = 30
    const val EXTREME_PARTICIPANTS_PER_EVENT = 5
    const val EXTREME_ROLES_PER_PARTICIPANT = 3
}

fun generateUltraData(requestId: String): UltraDataDto { ... }
fun generateExtremeData(requestId: String): ExtremeDataDto { ... }
```

### 3. 컨트롤러/gRPC 서비스 확장

- `DataController.kt`: ultra, extreme 복잡도 처리 추가
- `GrpcDataService.kt`: ultra, extreme 빌더 메서드 추가

---

## 측정 포인트

1. **Throughput (req/s)** - 초당 처리량
2. **Latency P95** - 95퍼센타일 응답 시간
3. **빌더 호출 수** - Protobuf 객체 생성 횟수
4. **메모리 사용량** - Peak Heap, GC Count

---

## 실행 방법

```bash
cd scripts

# Ultra 단독 테스트
COMPLEXITY_LIST="ultra" ./run-phase6.sh

# Extreme 단독 테스트
COMPLEXITY_LIST="extreme" ./run-phase6.sh

# 전체 비교 (Complex vs Ultra vs Extreme)
COMPLEXITY_LIST="complex ultra extreme" ./run-phase6.sh
```

---

## 검증할 질문

1. **Ultra에서 격차가 얼마나 줄어드는가?**
    - 예상: gRPC +15~20%
    - 검증: 빌더 오버헤드 증가 확인

2. **Extreme에서 실제로 역전이 발생하는가?**
    - 예상: HTTP +3~5%
    - 검증: 구조적 복잡성만으로 역전 가능 여부

3. **역전 원인 분석**
    - Protobuf 빌더 생성 비용 vs JSON 문자열 연결 비용
    - 메모리 할당 패턴 차이
    - GC 영향도

---

## 상태

📋 **설계 완료 **

---