# Phase 4: 복잡한 데이터 구조 직렬화 성능

## 목적

단순 byte[] 대신 복잡한 객체(여러 필드, 중첩 구조)를 전송할 때  
JSON 파싱 vs Protobuf 파싱 성능 차이를 검증한다.

## 가설

> "필드가 많고 중첩된 복잡한 객체에서는  
> JSON 파싱 비용이 Protobuf보다 커서 gRPC가 유리할 것이다"

## 배경 지식

### JSON 파싱
- 텍스트 기반 → 문자열 파싱 필요
- 키 이름 반복 전송 ("name": "value")
- 동적 타입 → 런타임 타입 체크

### Protobuf 파싱
- 바이너리 기반 → 직접 메모리 매핑
- 필드 번호만 전송 (키 이름 없음)
- 정적 타입 → 컴파일 타임 최적화

## 테스트 데이터 구조

```protobuf
message ComplexData {
    string id = 1;
    string name = 2;
    int32 age = 3;
    double score = 4;
    bool is_active = 5;
    repeated string tags = 6;           // 배열 (10개)
    Address address = 7;                 // 중첩 객체
    repeated Order orders = 8;           // 중첩 배열 (5개)
    map<string, string> metadata = 9;    // 맵 (10쌍)
}

message Address {
    string city = 1;
    string street = 2;
    string zipcode = 3;
    string country = 4;
}

message Order {
    string order_id = 1;
    double amount = 2;
    int64 timestamp = 3;
    repeated Item items = 4;            // 이중 중첩 (3개)
}

message Item {
    string product_id = 1;
    string name = 2;
    int32 quantity = 3;
    double price = 4;
}
```

## 테스트 시나리오

| 시나리오 | 필드 수 | 중첩 깊이 | 배열 크기 |
|---------|--------|----------|----------|
| Simple | 5개 | 0 | 0 |
| Medium | 15개 | 1 | 10 |
| Complex | 50+개 | 2 | 10×5×3 |

## 측정 포인트

1. **직렬화 시간** - 객체 → 바이트 변환
2. **역직렬화 시간** - 바이트 → 객체 변환
3. **전송 크기** - 동일 데이터의 바이트 수
4. **메모리 사용량** - 파싱 중 메모리 할당

## 예상 결과

| 복잡도 | HTTP/JSON | gRPC/Protobuf | 예상 |
|--------|-----------|---------------|------|
| Simple | 빠름 | 비슷 | HTTP ≈ gRPC |
| Medium | 느려짐 | 유지 | gRPC 우위 |
| Complex | 급락 | 약간 감소 | gRPC 압도 |

### 크기 비교 예상

```
Simple 데이터:
- JSON: ~200 bytes
- Protobuf: ~100 bytes (50% 절감)

Complex 데이터:
- JSON: ~5KB
- Protobuf: ~1.5KB (70% 절감)
```

## 실행 방법

```bash
cd scripts
./run-phase4.sh simple
./run-phase4.sh medium
./run-phase4.sh complex
```

## 상태

📋 **예정**