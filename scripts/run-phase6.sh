#!/bin/bash

# =============================================================================
# Phase 6: 극한 복잡도에서의 역전 포인트 탐색
# =============================================================================
#
# 목적: Phase 5에서 발견한 "복잡도 증가 시 gRPC 우위 감소" 패턴을 확장하여,
#       구조적 복잡성만으로 HTTP가 gRPC를 역전하는 포인트를 찾는다.
#
# 가설: "Protobuf 빌더 객체 생성 오버헤드가 JSON 문자열 연결보다 커지는
#        임계점(~500개 필드, 4단계 중첩)에서 HTTP가 gRPC를 추월할 것이다"
#
# 복잡도:
#   - ultra: ~150필드, 3단계 중첩, ~200회 빌더 호출
#   - extreme: ~500필드, 4단계 중첩, ~800회 빌더 호출
#
# =============================================================================

echo "============================================"
echo "Phase 6: 극한 복잡도에서의 역전 포인트 탐색"
echo "============================================"

API_SERVER="${API_SERVER:-http://192.168.55.114:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/../results/phase6"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 테스트할 복잡도 목록 (기본: ultra와 extreme만)
COMPLEXITY_LIST="${COMPLEXITY_LIST:-ultra extreme}"

mkdir -p $RESULTS_DIR

# 서버 헬스체크
echo ""
echo "[0] 서버 상태 확인..."
HEALTH_RESPONSE=$(curl -s -w "%{http_code}" $API_SERVER/api/health)
HTTP_CODE="${HEALTH_RESPONSE: -3}"
if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ apiServer가 실행되지 않았습니다. (HTTP $HTTP_CODE)"
    exit 1
fi
echo "✅ 서버 정상"

# 복잡도 정보 확인
echo ""
echo "[1] 지원 복잡도 확인..."
curl -s "$API_SERVER/api/complexities" | python3 -m json.tool 2>/dev/null || curl -s "$API_SERVER/api/complexities"
echo ""

# JIT 워밍업
echo ""
echo "[2] JIT 워밍업 (극한 복잡도는 더 긴 워밍업 필요)..."
for complexity in $COMPLEXITY_LIST; do
    echo "  워밍업 중: $complexity"
    for endpoint in "complex/http/json" "complex/http/binary" "complex/grpc" "complex/grpc/stream"; do
        for i in {1..30}; do
            curl -s "$API_SERVER/api/$endpoint?complexity=$complexity" > /dev/null 2>&1
        done
    done
done
echo "✅ 워밍업 완료"

# 테스트 카운터
TOTAL_TESTS=$(echo $COMPLEXITY_LIST | wc -w)
TOTAL_TESTS=$((TOTAL_TESTS * 4))  # HTTP/JSON, HTTP/Binary, gRPC/Unary, gRPC/Stream
CURRENT_TEST=0

# 각 복잡도별 테스트
for COMPLEXITY in $COMPLEXITY_LIST; do
    echo ""
    echo "=========================================="
    echo "         ${COMPLEXITY} 복잡도 테스트"
    echo "=========================================="

    # HTTP/JSON 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] HTTP/JSON - ${COMPLEXITY} 테스트..."
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase6/http-json-test.js" 2>&1 | tee "$RESULTS_DIR/http-json_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 5  # 극한 복잡도이므로 쿨다운 시간 증가

    # HTTP/Binary (Protobuf) 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] HTTP/Binary (Protobuf) - ${COMPLEXITY} 테스트..."
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase6/http-binary-test.js" 2>&1 | tee "$RESULTS_DIR/http-binary_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 5

    # gRPC/Unary 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] gRPC/Unary - ${COMPLEXITY} 테스트..."
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase6/grpc-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-unary_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 5

    # gRPC/Stream 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] gRPC/Stream - ${COMPLEXITY} 테스트..."
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase6/grpc-stream-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-stream_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 5
done

echo ""
echo "============================================"
echo "✅ Phase 6 테스트 완료!"
echo "결과 파일: $RESULTS_DIR/*_$TIMESTAMP.log"
echo "============================================"

# 결과 요약 출력
echo ""
echo "=========================================="
echo "         결과 요약"
echo "=========================================="
echo ""
echo "| 복잡도  | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream | JSON vs gRPC |"
echo "|---------|-----------|-------------|------------|-------------|--------------|"

for COMPLEXITY in $COMPLEXITY_LIST; do
    HTTP_JSON_LOG="$RESULTS_DIR/http-json_${COMPLEXITY}_$TIMESTAMP.log"
    HTTP_BINARY_LOG="$RESULTS_DIR/http-binary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_UNARY_LOG="$RESULTS_DIR/grpc-unary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_STREAM_LOG="$RESULTS_DIR/grpc-stream_${COMPLEXITY}_$TIMESTAMP.log"

    # Throughput 추출 (k6 로그 포맷: level=info msg="Throughput: 3601.55 req/s")
    HTTP_JSON_RPS=$(grep "Throughput:" "$HTTP_JSON_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    HTTP_BINARY_RPS=$(grep "Throughput:" "$HTTP_BINARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_UNARY_RPS=$(grep "Throughput:" "$GRPC_UNARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_STREAM_RPS=$(grep "Throughput:" "$GRPC_STREAM_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)

    # 값이 없으면 N/A로 설정
    HTTP_JSON_RPS="${HTTP_JSON_RPS:-N/A}"
    HTTP_BINARY_RPS="${HTTP_BINARY_RPS:-N/A}"
    GRPC_UNARY_RPS="${GRPC_UNARY_RPS:-N/A}"
    GRPC_STREAM_RPS="${GRPC_STREAM_RPS:-N/A}"

    # JSON vs gRPC/Unary 비교
    WINNER="N/A"
    if [ "$HTTP_JSON_RPS" != "N/A" ] && [ "$GRPC_UNARY_RPS" != "N/A" ]; then
        # 소수점 제거하여 정수로 변환
        HTTP_INT=$(echo "$HTTP_JSON_RPS" | cut -d. -f1)
        GRPC_INT=$(echo "$GRPC_UNARY_RPS" | cut -d. -f1)

        # 정수 검증 후 비교
        if [[ "$HTTP_INT" =~ ^[0-9]+$ ]] && [[ "$GRPC_INT" =~ ^[0-9]+$ ]]; then
            if [ "$HTTP_INT" -gt 0 ] && [ "$GRPC_INT" -gt 0 ]; then
                if [ "$GRPC_INT" -gt "$HTTP_INT" ]; then
                    DIFF=$(( (GRPC_INT - HTTP_INT) * 100 / HTTP_INT ))
                    WINNER="gRPC +${DIFF}%"
                elif [ "$HTTP_INT" -gt "$GRPC_INT" ]; then
                    DIFF=$(( (HTTP_INT - GRPC_INT) * 100 / GRPC_INT ))
                    WINNER="**JSON +${DIFF}%**"  # 역전 시 강조
                else
                    WINNER="동일"
                fi
            fi
        fi
    fi

    printf "| %-7s | %9s | %11s | %10s | %11s | %12s |\n" \
        "$COMPLEXITY" \
        "$HTTP_JSON_RPS" \
        "$HTTP_BINARY_RPS" \
        "$GRPC_UNARY_RPS" \
        "$GRPC_STREAM_RPS" \
        "$WINNER"
done

echo ""
echo "=========================================="
echo "         Phase 5 vs Phase 6 비교"
echo "=========================================="
echo ""
echo "| 복잡도  | 필드 수 | 중첩 깊이 | 빌더 호출 | 예상 gRPC 우위 |"
echo "|---------|--------|----------|----------|---------------|"
echo "| simple  | ~5개   | 0단계    | ~5회     | +67%          |"
echo "| medium  | ~13개  | 1단계    | ~15회    | +69%          |"
echo "| complex | ~50개  | 2단계    | ~30회    | +40%          |"
echo "| ultra   | ~150개 | 3단계    | ~200회   | +15~20%?      |"
echo "| extreme | ~500개 | 4단계    | ~800회   | **역전?**     |"
echo ""

echo "=========================================="
echo "         크기 비교 (예상)"
echo "=========================================="
echo ""
echo "| 복잡도  | JSON 예상 크기 | Protobuf 예상 크기 | 절감률 |"
echo "|---------|---------------|-------------------|--------|"
echo "| simple  | ~150 bytes    | ~50 bytes         | ~67%   |"
echo "| medium  | ~800 bytes    | ~300 bytes        | ~63%   |"
echo "| complex | ~5,000 bytes  | ~1,500 bytes      | ~70%   |"
echo "| ultra   | ~15,000 bytes | ~5,000 bytes      | ~67%   |"
echo "| extreme | ~50,000 bytes | ~15,000 bytes     | ~70%   |"
echo ""

echo "=========================================="
echo "         가설 검증 포인트"
echo "=========================================="
echo ""
echo "1. Ultra에서 격차가 얼마나 줄어드는가?"
echo "   - 예상: gRPC +15~20%"
echo "   - 검증: 빌더 오버헤드 증가 확인"
echo ""
echo "2. Extreme에서 실제로 역전이 발생하는가?"
echo "   - 예상: HTTP +3~5%"
echo "   - 검증: 구조적 복잡성만으로 역전 가능 여부"
echo ""
echo "3. 역전 원인 분석"
echo "   - Protobuf 빌더 생성 비용 vs JSON 문자열 연결 비용"
echo "   - 메모리 할당 패턴 차이"
echo "   - GC 영향도"
echo ""
echo "=========================================="