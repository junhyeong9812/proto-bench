# gRPC 완벽 가이드

> gRPC를 처음 접하는 개발자를 위한 상세 학습 문서

## 목차

1. [gRPC란 무엇인가?](#1-grpc란-무엇인가)
2. [핵심 개념](#2-핵심-개념)
3. [Protocol Buffers (Protobuf)](#3-protocol-buffers-protobuf)
4. [4가지 RPC 패턴](#4-4가지-rpc-패턴)
5. [HTTP/2 기반](#5-http2-기반)
6. [gRPC vs REST](#6-grpc-vs-rest)
7. [실무에서의 활용](#7-실무에서의-활용)
8. [장단점 정리](#8-장단점-정리)

---

## 1. gRPC란 무엇인가?

### 정의

gRPC는 **Google이 개발한 고성능 원격 프로시저 호출(RPC) 프레임워크**입니다.

```
g = gRPC 자체를 의미 (재귀적 약어)
   또는 Google, good, general 등 다양한 의미
RPC = Remote Procedure Call (원격 프로시저 호출)
```

### 원격 프로시저 호출(RPC)이란?

**로컬 함수를 호출하듯이 원격 서버의 함수를 호출**하는 기술입니다.

```kotlin
// 일반적인 로컬 함수 호출
val result = calculator.add(1, 2)

// RPC를 사용하면 원격 서버의 함수도 동일하게 호출
val result = remoteCalculator.add(1, 2)  // 실제로는 네트워크 통신 발생
```

개발자는 네트워크 통신의 복잡성(소켓, 직렬화, 에러 처리 등)을 신경 쓰지 않아도 됩니다.

### gRPC의 탄생 배경

Google 내부에서 사용하던 **Stubby**라는 RPC 시스템을 오픈소스화한 것입니다.

- 2015년 처음 공개
- 현재 CNCF(Cloud Native Computing Foundation) 프로젝트
- 마이크로서비스 아키텍처의 표준 통신 방식 중 하나

---

## 2. 핵심 개념

### 2.1 서비스 정의 (Service Definition)

gRPC는 **스키마 우선(Schema-First)** 접근 방식을 사용합니다.

```protobuf
// .proto 파일에서 서비스 정의
service UserService {
  rpc GetUser(GetUserRequest) returns (User);
  rpc CreateUser(CreateUserRequest) returns (User);
  rpc ListUsers(ListUsersRequest) returns (stream User);
}
```

이 정의에서 클라이언트와 서버 코드가 자동 생성됩니다.

### 2.2 스텁 (Stub)

**스텁**은 원격 서비스를 로컬처럼 호출할 수 있게 해주는 클라이언트 객체입니다.

```kotlin
// 스텁 생성
val stub = UserServiceGrpcKt.UserServiceCoroutineStub(channel)

// 마치 로컬 함수처럼 호출
val user = stub.getUser(request)
```

### 2.3 채널 (Channel)

**채널**은 서버와의 연결을 관리하는 객체입니다.

```kotlin
// 채널 생성
val channel = ManagedChannelBuilder
    .forAddress("localhost", 9091)
    .usePlaintext()
    .build()

// 채널 특징:
// - 연결 풀 관리
// - 자동 재연결
// - 로드밸런싱 지원
// - 스레드 안전
```

### 2.4 메시지 (Message)

통신에 사용되는 데이터 구조입니다.

```protobuf
message User {
  int64 id = 1;
  string name = 2;
  string email = 3;
  repeated string roles = 4;  // 배열
}
```

---

## 3. Protocol Buffers (Protobuf)

### 3.1 Protobuf란?

**Protocol Buffers**는 구조화된 데이터를 직렬화하는 Google의 데이터 형식입니다.

```
JSON (텍스트 기반)           Protobuf (바이너리 기반)
{                           [바이너리 데이터]
  "name": "John",           - 더 작은 크기
  "age": 30                 - 더 빠른 파싱
}                           - 스키마 필수
```

### 3.2 왜 JSON 대신 Protobuf를 사용하나?

| 항목 | JSON | Protobuf |
|------|------|----------|
| 포맷 | 텍스트 | 바이너리 |
| 크기 | 큼 | 작음 (약 3~10배) |
| 파싱 속도 | 느림 | 빠름 (약 10~100배) |
| 스키마 | 선택적 | 필수 |
| 사람 읽기 | 가능 | 불가능 |
| 타입 안전성 | 약함 | 강함 |

### 3.3 .proto 파일 문법

```protobuf
// 버전 선언 (proto3 권장)
syntax = "proto3";

// 패키지 (네임스페이스)
package myapp;

// Java 옵션
option java_multiple_files = true;
option java_package = "com.example.myapp";

// 메시지 정의
message Person {
  // 타입 필드명 = 필드번호;
  string name = 1;      // 문자열
  int32 age = 2;        // 32비트 정수
  bool is_active = 3;   // 불리언
  bytes avatar = 4;     // 바이너리
  
  // 배열 (repeated)
  repeated string hobbies = 5;
  
  // 중첩 메시지
  Address address = 6;
  
  // 열거형 사용
  Status status = 7;
}

// 중첩 가능한 메시지
message Address {
  string city = 1;
  string street = 2;
}

// 열거형
enum Status {
  UNKNOWN = 0;  // 첫 번째는 반드시 0
  ACTIVE = 1;
  INACTIVE = 2;
}
```

### 3.4 필드 번호 규칙

```protobuf
message Example {
  string field_a = 1;   // 1-15: 1바이트로 인코딩 (자주 사용하는 필드)
  string field_b = 16;  // 16-2047: 2바이트로 인코딩
  
  // 예약된 번호 (삭제된 필드)
  reserved 3, 4, 10 to 12;
  reserved "old_field";
}
```

**주의**: 한번 할당된 필드 번호는 절대 변경하면 안 됩니다!

### 3.5 타입 매핑

| Protobuf | Kotlin/Java | 설명 |
|----------|-------------|------|
| double | Double | 64비트 부동소수점 |
| float | Float | 32비트 부동소수점 |
| int32 | Int | 32비트 정수 |
| int64 | Long | 64비트 정수 |
| bool | Boolean | 불리언 |
| string | String | UTF-8 문자열 |
| bytes | ByteString | 바이너리 데이터 |

---

## 4. 4가지 RPC 패턴

### 4.1 Unary RPC (단항)

가장 기본적인 패턴: **요청 1개 → 응답 1개**

```protobuf
// Proto 정의
rpc GetUser(GetUserRequest) returns (User);
```

```kotlin
// 클라이언트
val user = stub.getUser(request)

// 서버
override suspend fun getUser(request: GetUserRequest): User {
    return userRepository.findById(request.id)
}
```

**사용 사례**: 단일 리소스 조회, 간단한 CRUD

### 4.2 Server Streaming RPC (서버 스트리밍)

**요청 1개 → 응답 여러 개 (스트림)**

```protobuf
// Proto 정의 - returns 뒤에 stream 키워드
rpc ListUsers(ListUsersRequest) returns (stream User);
```

```kotlin
// 클라이언트 - Flow로 수신
stub.listUsers(request).collect { user ->
    println("Received: $user")
}

// 서버 - Flow로 전송
override fun listUsers(request: ListUsersRequest): Flow<User> = flow {
    userRepository.findAll().forEach { user ->
        emit(user)  // 한 명씩 스트림으로 전송
    }
}
```

**사용 사례**:
- 대용량 데이터 다운로드
- 실시간 피드 (뉴스, 주식 시세)
- 검색 결과 점진적 반환

### 4.3 Client Streaming RPC (클라이언트 스트리밍)

**요청 여러 개 (스트림) → 응답 1개**

```protobuf
// Proto 정의 - 파라미터에 stream 키워드
rpc UploadFile(stream FileChunk) returns (UploadResult);
```

```kotlin
// 클라이언트 - Flow로 전송
val chunks = flow {
    file.chunked(64 * 1024).forEach { chunk ->
        emit(FileChunk.newBuilder().setData(chunk).build())
    }
}
val result = stub.uploadFile(chunks)

// 서버 - Flow로 수신
override suspend fun uploadFile(requests: Flow<FileChunk>): UploadResult {
    var totalSize = 0L
    requests.collect { chunk ->
        totalSize += chunk.data.size()
        // 파일 저장 로직
    }
    return UploadResult.newBuilder().setTotalSize(totalSize).build()
}
```

**사용 사례**:
- 파일 업로드
- 로그 수집
- 센서 데이터 전송

### 4.4 Bidirectional Streaming RPC (양방향 스트리밍)

**요청 여러 개 ↔ 응답 여러 개 (동시에)**

```protobuf
// Proto 정의 - 양쪽 모두 stream
rpc Chat(stream ChatMessage) returns (stream ChatMessage);
```

```kotlin
// 클라이언트
val outgoing = Channel<ChatMessage>()
launch {
    stub.chat(outgoing.consumeAsFlow()).collect { message ->
        println("Received: ${message.content}")
    }
}
outgoing.send(ChatMessage.newBuilder().setContent("Hello").build())

// 서버
override fun chat(requests: Flow<ChatMessage>): Flow<ChatMessage> = flow {
    requests.collect { message ->
        // 받은 메시지 처리 후 응답
        emit(ChatMessage.newBuilder()
            .setContent("Echo: ${message.content}")
            .build())
    }
}
```

**사용 사례**:
- 실시간 채팅
- 게임 상태 동기화
- 협업 편집 (Google Docs 같은)

---

## 5. HTTP/2 기반

### 5.1 왜 HTTP/2인가?

gRPC는 HTTP/2를 전송 계층으로 사용합니다.

```
HTTP/1.1의 문제점:
┌─────────────┐     요청1 ──────────→
│   Client    │     ←────────── 응답1
│             │     요청2 ──────────→  (앞 요청 완료 후에야 전송)
│             │     ←────────── 응답2
└─────────────┘
     ↑ Head-of-Line Blocking

HTTP/2의 해결:
┌─────────────┐     요청1 ──────────→
│   Client    │     요청2 ──────────→  (동시 전송!)
│             │     ←────────── 응답2
│             │     ←────────── 응답1
└─────────────┘
     ↑ 멀티플렉싱
```

### 5.2 HTTP/2의 주요 특징

| 특징 | 설명 | gRPC에서의 이점 |
|------|------|-----------------|
| 멀티플렉싱 | 하나의 연결로 여러 요청/응답 동시 처리 | 연결 오버헤드 감소 |
| 헤더 압축 (HPACK) | 반복되는 헤더 압축 | 네트워크 효율 향상 |
| 바이너리 프레이밍 | 바이너리 형식으로 전송 | 파싱 속도 향상 |
| 서버 푸시 | 서버가 먼저 데이터 전송 가능 | 스트리밍 지원 |
| 스트림 우선순위 | 중요한 요청 먼저 처리 | QoS 지원 |

### 5.3 프레이밍 구조

```
HTTP/2 프레임 구조:
┌─────────────────────────────────────┐
│ Length (24 bits)                    │
├─────────────────────────────────────┤
│ Type (8 bits) │ Flags (8 bits)     │
├─────────────────────────────────────┤
│ Stream Identifier (31 bits)         │
├─────────────────────────────────────┤
│ Frame Payload (가변)                 │
└─────────────────────────────────────┘

gRPC 메시지 프레이밍:
┌─────────────────────────────────────┐
│ Compressed flag (1 byte)            │
├─────────────────────────────────────┤
│ Message length (4 bytes)            │
├─────────────────────────────────────┤
│ Protobuf message (가변)              │
└─────────────────────────────────────┘
```

---

## 6. gRPC vs REST

### 6.1 비교표

| 항목 | REST | gRPC |
|------|------|------|
| 프로토콜 | HTTP/1.1 (주로) | HTTP/2 |
| 데이터 형식 | JSON (주로) | Protobuf |
| API 정의 | OpenAPI/Swagger (선택) | .proto (필수) |
| 코드 생성 | 선택적 | 자동 |
| 브라우저 지원 | 완벽 | 제한적 (gRPC-Web) |
| 스트리밍 | 제한적 (WebSocket) | 네이티브 지원 |
| 성능 | 보통 | 높음 |
| 학습 곡선 | 낮음 | 높음 |

### 6.2 언제 무엇을 사용?

**REST를 사용해야 할 때:**
- 공개 API (외부 개발자용)
- 웹 브라우저에서 직접 호출
- 단순한 CRUD 작업
- 팀의 gRPC 경험이 부족할 때

**gRPC를 사용해야 할 때:**
- 마이크로서비스 간 내부 통신
- 고성능이 필요한 경우
- 양방향 스트리밍이 필요한 경우
- 다양한 언어로 구성된 시스템
- 강타입 API가 필요한 경우

### 6.3 하이브리드 접근

많은 실제 시스템에서는 둘 다 사용합니다:

```
┌────────────────────────────────────────────────┐
│                    Internet                     │
└──────────────────────┬─────────────────────────┘
                       │
                       │ REST API (JSON)
                       ▼
┌──────────────────────────────────────────────┐
│              API Gateway                      │
│         (외부 요청을 내부로 라우팅)             │
└───────┬──────────────┬───────────────┬───────┘
        │              │               │
        │ gRPC         │ gRPC          │ gRPC
        ▼              ▼               ▼
   ┌─────────┐   ┌─────────┐    ┌─────────┐
   │Service A│   │Service B│    │Service C│
   └─────────┘   └─────────┘    └─────────┘
```

---

## 7. 실무에서의 활용

### 7.1 에러 처리

gRPC는 표준화된 상태 코드를 제공합니다:

```kotlin
// 서버에서 에러 반환
throw StatusException(
    Status.NOT_FOUND
        .withDescription("User not found: ${request.id}")
)

// 클라이언트에서 에러 처리
try {
    val user = stub.getUser(request)
} catch (e: StatusException) {
    when (e.status.code) {
        Status.Code.NOT_FOUND -> println("사용자 없음")
        Status.Code.PERMISSION_DENIED -> println("권한 없음")
        Status.Code.UNAVAILABLE -> println("서비스 불가")
        else -> println("에러: ${e.status.description}")
    }
}
```

### 7.2 인터셉터 (미들웨어)

요청/응답을 가로채서 처리할 수 있습니다:

```kotlin
// 로깅 인터셉터
class LoggingInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        println("Method: ${call.methodDescriptor.fullMethodName}")
        return next.startCall(call, headers)
    }
}

// 인터셉터 등록
ServerBuilder.forPort(9091)
    .addService(myService)
    .intercept(LoggingInterceptor())
    .build()
```

### 7.3 메타데이터 (헤더)

HTTP 헤더처럼 메타데이터를 전달할 수 있습니다:

```kotlin
// 클라이언트에서 메타데이터 전송
val metadata = Metadata().apply {
    put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer token")
}
val stub = MetadataUtils.attachHeaders(originalStub, metadata)

// 서버에서 메타데이터 수신
val token = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER))
```

### 7.4 데드라인/타임아웃

요청에 시간 제한을 설정할 수 있습니다:

```kotlin
// 5초 타임아웃 설정
val stub = originalStub.withDeadlineAfter(5, TimeUnit.SECONDS)

try {
    val result = stub.longRunningOperation(request)
} catch (e: StatusException) {
    if (e.status.code == Status.Code.DEADLINE_EXCEEDED) {
        println("타임아웃!")
    }
}
```

---

## 8. 장단점 정리

### 장점

1. **고성능**
    - 바이너리 직렬화 (Protobuf)
    - HTTP/2 멀티플렉싱
    - 헤더 압축

2. **강타입 API**
    - 컴파일 타임 타입 체크
    - IDE 자동완성 지원
    - 문서 역할을 하는 스키마

3. **코드 자동 생성**
    - 12개 이상 언어 지원
    - 클라이언트/서버 코드 자동 생성
    - 버전 호환성 관리

4. **스트리밍 네이티브 지원**
    - 양방향 스트리밍
    - 실시간 통신에 적합

5. **언어 중립적**
    - Go, Java, Python, C++, Kotlin 등
    - 서로 다른 언어 간 통신 용이

### 단점

1. **학습 곡선**
    - Protobuf 문법 학습 필요
    - 새로운 도구 체인

2. **브라우저 제한**
    - 직접 호출 불가
    - gRPC-Web 필요

3. **디버깅 어려움**
    - 바이너리 형식으로 사람이 읽기 어려움
    - 특별한 도구 필요 (grpcurl, Postman)

4. **인프라 요구사항**
    - HTTP/2 지원 필요
    - 일부 프록시/로드밸런서 호환성 문제

5. **유연성 감소**
    - 스키마 변경 시 재배포 필요
    - 동적 쿼리 어려움

---

## 추가 학습 자료

- [공식 문서](https://grpc.io/docs/)
- [Protobuf 언어 가이드](https://developers.google.com/protocol-buffers/docs/proto3)
- [gRPC-Kotlin 가이드](https://grpc.io/docs/languages/kotlin/)
- [Awesome gRPC](https://github.com/grpc-ecosystem/awesome-grpc)