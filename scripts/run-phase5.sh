#!/bin/bash

# =============================================================================
# Phase 5: 복잡한 데이터 구조 직렬화 성능 테스트
# =============================================================================
#
# 목적: JSON 파싱 vs Protobuf 파싱 성능 비교
#
# 가설: "필드가 많고 중첩된 복잡한 객체에서는
#        JSON 파싱 비용이 Protobuf보다 커서 gRPC가 유리할 것이다"
#
# 복잡도:
#   - simple: 5필드, 중첩 없음
#   - medium: 13필드, 1단계 중첩, 배열
#   - complex: 20필드, 2단계 중첩, 배열, 맵
#
# =============================================================================

echo "================================"
echo "Phase 5: 복잡한 데이터 구조 직렬화 성능 테스트"
echo "================================"

API_SERVER="${API_SERVER:-http://192.168.55.114:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/../results/phase5"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 테스트할 복잡도 목록
COMPLEXITY_LIST="${COMPLEXITY_LIST:-simple medium complex}"

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
echo "[2] JIT 워밍업..."
for complexity in $COMPLEXITY_LIST; do
    for endpoint in "complex/http/json" "complex/http/binary" "complex/grpc" "complex/grpc/stream"; do
        for i in {1..50}; do
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
        "$SCRIPT_DIR/phase5/http-json-test.js" 2>&1 | tee "$RESULTS_DIR/http-json_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 3

    # HTTP/Binary (Protobuf) 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] HTTP/Binary (Protobuf) - ${COMPLEXITY} 테스트..."
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase5/http-binary-test.js" 2>&1 | tee "$RESULTS_DIR/http-binary_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 3

    # gRPC/Unary 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] gRPC/Unary - ${COMPLEXITY} 테스트..."
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase5/grpc-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-unary_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 3

    # gRPC/Stream 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] gRPC/Stream - ${COMPLEXITY} 테스트..."
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase5/grpc-stream-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-stream_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 3
done

echo ""
echo "================================"
echo "✅ Phase 5 테스트 완료!"
echo "결과 파일: $RESULTS_DIR/*_$TIMESTAMP.log"
echo "================================"

# 결과 요약 출력
echo ""
echo "=========================================="
echo "         결과 요약"
echo "=========================================="
echo ""
echo "| 복잡도 | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream | JSON vs gRPC |"
echo "|--------|-----------|-------------|------------|-------------|--------------|"

for COMPLEXITY in $COMPLEXITY_LIST; do
    HTTP_JSON_LOG="$RESULTS_DIR/http-json_${COMPLEXITY}_$TIMESTAMP.log"
    HTTP_BINARY_LOG="$RESULTS_DIR/http-binary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_UNARY_LOG="$RESULTS_DIR/grpc-unary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_STREAM_LOG="$RESULTS_DIR/grpc-stream_${COMPLEXITY}_$TIMESTAMP.log"

    # Throughput 추출 (BENCHMARK RESULT 섹션에서)
    HTTP_JSON_RPS=$(grep "Throughput:" "$HTTP_JSON_LOG" 2>/dev/null | tail -1 | awk '{print $2}')
    HTTP_BINARY_RPS=$(grep "Throughput:" "$HTTP_BINARY_LOG" 2>/dev/null | tail -1 | awk '{print $2}')
    GRPC_UNARY_RPS=$(grep "Throughput:" "$GRPC_UNARY_LOG" 2>/dev/null | tail -1 | awk '{print $2}')
    GRPC_STREAM_RPS=$(grep "Throughput:" "$GRPC_STREAM_LOG" 2>/dev/null | tail -1 | awk '{print $2}')

    # JSON vs gRPC/Unary 비교
    if [ -n "$HTTP_JSON_RPS" ] && [ -n "$GRPC_UNARY_RPS" ]; then
        HTTP_INT=$(echo "$HTTP_JSON_RPS" | cut -d. -f1)
        GRPC_INT=$(echo "$GRPC_UNARY_RPS" | cut -d. -f1)

        if [ "$HTTP_INT" -gt 0 ] && [ "$GRPC_INT" -gt 0 ]; then
            if [ "$GRPC_INT" -gt "$HTTP_INT" ]; then
                DIFF=$(( (GRPC_INT - HTTP_INT) * 100 / HTTP_INT ))
                WINNER="gRPC +${DIFF}%"
            elif [ "$HTTP_INT" -gt "$GRPC_INT" ]; then
                DIFF=$(( (HTTP_INT - GRPC_INT) * 100 / GRPC_INT ))
                WINNER="JSON +${DIFF}%"
            else
                WINNER="동일"
            fi
        else
            WINNER="N/A"
        fi

        printf "| %s | %s | %s | %s | %s | %s |\n" \
            "$COMPLEXITY" \
            "${HTTP_JSON_RPS:-N/A}" \
            "${HTTP_BINARY_RPS:-N/A}" \
            "${GRPC_UNARY_RPS:-N/A}" \
            "${GRPC_STREAM_RPS:-N/A}" \
            "$WINNER"
    fi
done

echo ""
echo "=========================================="
echo "         크기 비교 (예상)"
echo "=========================================="
echo ""
echo "| 복잡도 | JSON 예상 크기 | Protobuf 예상 크기 | 절감률 |"
echo "|--------|---------------|-------------------|--------|"
echo "| simple | ~150 bytes    | ~50 bytes         | ~67%   |"
echo "| medium | ~800 bytes    | ~300 bytes        | ~63%   |"
echo "| complex| ~5,000 bytes  | ~1,500 bytes      | ~70%   |"
echo ""
echo "=========================================="
echo "※ Protobuf는 필드명을 전송하지 않아 복잡한 구조에서 더 효율적"
echo "=========================================="
```