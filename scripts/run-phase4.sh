#!/bin/bash

# =============================================================================
# Phase 4: 역전 포인트 탐색 (10KB ~ 500KB)
# =============================================================================
#
# 목적: gRPC와 HTTP 성능 역전 포인트 탐색
#
# Phase 2 결과: 10KB에서 gRPC가 43% 더 빠름
# Phase 1 결과: 1MB에서 HTTP가 2배 더 빠름
# → 역전 포인트는 10KB ~ 1MB 사이에 존재
#
# =============================================================================

echo "================================"
echo "Phase 4: 역전 포인트 탐색 테스트"
echo "================================"

API_SERVER="${API_SERVER:-http://192.168.55.114:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/../results/phase4"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 테스트할 크기 목록 (환경변수로 개별 지정 가능)
SIZE_LIST="${SIZE_LIST:-10kb 50kb 100kb 200kb 500kb}"

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

# JIT 워밍업
echo ""
echo "[Warmup] JIT 워밍업..."
for size in $SIZE_LIST; do
    for endpoint in "http/json" "http/binary" "grpc" "grpc/stream"; do
        for i in {1..30}; do
            curl -s "$API_SERVER/api/data/$endpoint?size=$size" > /dev/null 2>&1
        done
    done
done
echo "✅ 워밍업 완료"

# 테스트 카운터
TOTAL_TESTS=$(echo $SIZE_LIST | wc -w)
TOTAL_TESTS=$((TOTAL_TESTS * 4))  # HTTP/JSON, HTTP/Binary, gRPC/Unary, gRPC/Stream
CURRENT_TEST=0

# 각 크기별 테스트
for SIZE in $SIZE_LIST; do
    echo ""
    echo "=========================================="
    echo "         ${SIZE} 페이로드 테스트"
    echo "=========================================="

    # HTTP/JSON 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] HTTP/JSON - ${SIZE} 테스트..."
    k6 run \
        -e SIZE=$SIZE \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase4/http-json-test.js" 2>&1 | tee "$RESULTS_DIR/http-json_${SIZE}_$TIMESTAMP.log"
    sleep 3

    # HTTP/Binary 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] HTTP/Binary - ${SIZE} 테스트..."
    k6 run \
        -e SIZE=$SIZE \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase4/http-binary-test.js" 2>&1 | tee "$RESULTS_DIR/http-binary_${SIZE}_$TIMESTAMP.log"
    sleep 3

    # gRPC/Unary 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] gRPC/Unary - ${SIZE} 테스트..."
    k6 run \
        -e SIZE=$SIZE \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase4/grpc-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-unary_${SIZE}_$TIMESTAMP.log"
    sleep 3

    # gRPC/Stream 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] gRPC/Stream - ${SIZE} 테스트..."
    k6 run \
        -e SIZE=$SIZE \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase4/grpc-stream-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-stream_${SIZE}_$TIMESTAMP.log"
    sleep 3
done

echo ""
echo "================================"
echo "✅ Phase 4 테스트 완료!"
echo "결과 파일: $RESULTS_DIR/*_$TIMESTAMP.log"
echo "================================"

# 결과 요약 출력
echo ""
echo "=========================================="
echo "         결과 요약"
echo "=========================================="
echo ""
echo "| 크기 | HTTP/JSON | HTTP/Binary | gRPC/Unary | gRPC/Stream | 승자 |"
echo "|------|-----------|-------------|------------|-------------|------|"

for SIZE in $SIZE_LIST; do
    HTTP_JSON_LOG="$RESULTS_DIR/http-json_${SIZE}_$TIMESTAMP.log"
    HTTP_BINARY_LOG="$RESULTS_DIR/http-binary_${SIZE}_$TIMESTAMP.log"
    GRPC_UNARY_LOG="$RESULTS_DIR/grpc-unary_${SIZE}_$TIMESTAMP.log"
    GRPC_STREAM_LOG="$RESULTS_DIR/grpc-stream_${SIZE}_$TIMESTAMP.log"

    # Throughput 추출 (BENCHMARK RESULT 섹션에서)
    HTTP_JSON_RPS=$(grep "Throughput:" "$HTTP_JSON_LOG" 2>/dev/null | tail -1 | awk '{print $2}')
    HTTP_BINARY_RPS=$(grep "Throughput:" "$HTTP_BINARY_LOG" 2>/dev/null | tail -1 | awk '{print $2}')
    GRPC_UNARY_RPS=$(grep "Throughput:" "$GRPC_UNARY_LOG" 2>/dev/null | tail -1 | awk '{print $2}')
    GRPC_STREAM_RPS=$(grep "Throughput:" "$GRPC_STREAM_LOG" 2>/dev/null | tail -1 | awk '{print $2}')

    # 승자 판정 (HTTP/Binary vs gRPC/Unary 비교)
    if [ -n "$HTTP_BINARY_RPS" ] && [ -n "$GRPC_UNARY_RPS" ]; then
        HTTP_INT=$(echo "$HTTP_BINARY_RPS" | cut -d. -f1)
        GRPC_INT=$(echo "$GRPC_UNARY_RPS" | cut -d. -f1)

        if [ "$HTTP_INT" -gt "$GRPC_INT" ]; then
            WINNER="HTTP"
        elif [ "$GRPC_INT" -gt "$HTTP_INT" ]; then
            WINNER="gRPC"
        else
            WINNER="동일"
        fi

        printf "| %s | %s | %s | %s | %s | %s |\n" \
            "$SIZE" \
            "${HTTP_JSON_RPS:-N/A}" \
            "${HTTP_BINARY_RPS:-N/A}" \
            "${GRPC_UNARY_RPS:-N/A}" \
            "${GRPC_STREAM_RPS:-N/A}" \
            "$WINNER"
    fi
done

echo ""
echo "=========================================="
echo "※ 승자는 HTTP/Binary vs gRPC/Unary 비교 기준"
echo "=========================================="