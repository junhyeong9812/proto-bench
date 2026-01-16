#!/bin/bash

# Phase 2: 소용량 페이로드 벤치마크 (1KB, 10KB)

echo "================================"
echo "Phase 2: 소용량 페이로드 테스트"
echo "================================"

API_SERVER="${API_SERVER:-http://192.168.55.114:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/../results/phase2"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p $RESULTS_DIR

# 서버 헬스체크
echo ""
echo "[0/8] 서버 상태 확인..."
curl -s $API_SERVER/api/health > /dev/null
if [ $? -ne 0 ]; then
    echo "❌ apiServer가 실행되지 않았습니다."
    exit 1
fi
echo "✅ 서버 정상"

# JIT 워밍업
echo ""
echo "[Warmup] JIT 워밍업 (각 엔드포인트 50회)..."
for size in "1kb" "10kb"; do
    for endpoint in "http/json" "http/binary" "grpc" "grpc/stream"; do
        for i in {1..50}; do
            curl -s "$API_SERVER/api/data/$endpoint?size=$size" > /dev/null
        done
    done
done
echo "✅ 워밍업 완료"

# === 1KB 테스트 ===
echo ""
echo "========== 1KB 페이로드 테스트 =========="

echo ""
echo "[1/8] HTTP/JSON 1KB 테스트..."
k6 run -e SIZE=1kb -e API_SERVER=$API_SERVER "$SCRIPT_DIR/phase2/http-json-test.js" 2>&1 | tee "$RESULTS_DIR/http-json_1kb_$TIMESTAMP.log"
sleep 3

echo ""
echo "[2/8] HTTP/Binary 1KB 테스트..."
k6 run -e SIZE=1kb -e API_SERVER=$API_SERVER "$SCRIPT_DIR/phase2/http-binary-test.js" 2>&1 | tee "$RESULTS_DIR/http-binary_1kb_$TIMESTAMP.log"
sleep 3

echo ""
echo "[3/8] gRPC/Unary 1KB 테스트..."
k6 run -e SIZE=1kb -e API_SERVER=$API_SERVER "$SCRIPT_DIR/phase2/grpc-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-unary_1kb_$TIMESTAMP.log"
sleep 3

echo ""
echo "[4/8] gRPC/Stream 1KB 테스트..."
k6 run -e SIZE=1kb -e API_SERVER=$API_SERVER "$SCRIPT_DIR/phase2/grpc-stream-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-stream_1kb_$TIMESTAMP.log"
sleep 3

# === 10KB 테스트 ===
echo ""
echo "========== 10KB 페이로드 테스트 =========="

echo ""
echo "[5/8] HTTP/JSON 10KB 테스트..."
k6 run -e SIZE=10kb -e API_SERVER=$API_SERVER "$SCRIPT_DIR/phase2/http-json-test.js" 2>&1 | tee "$RESULTS_DIR/http-json_10kb_$TIMESTAMP.log"
sleep 3

echo ""
echo "[6/8] HTTP/Binary 10KB 테스트..."
k6 run -e SIZE=10kb -e API_SERVER=$API_SERVER "$SCRIPT_DIR/phase2/http-binary-test.js" 2>&1 | tee "$RESULTS_DIR/http-binary_10kb_$TIMESTAMP.log"
sleep 3

echo ""
echo "[7/8] gRPC/Unary 10KB 테스트..."
k6 run -e SIZE=10kb -e API_SERVER=$API_SERVER "$SCRIPT_DIR/phase2/grpc-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-unary_10kb_$TIMESTAMP.log"
sleep 3

echo ""
echo "[8/8] gRPC/Stream 10KB 테스트..."
k6 run -e SIZE=10kb -e API_SERVER=$API_SERVER "$SCRIPT_DIR/phase2/grpc-stream-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-stream_10kb_$TIMESTAMP.log"

echo ""
echo "================================"
echo "✅ Phase 2 테스트 완료!"
echo "결과 파일: $RESULTS_DIR/*_$TIMESTAMP.log"
echo "================================"