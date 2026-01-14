#!/bin/bash

# 전체 벤치마크 실행 스크립트

echo "================================"
echo "Proto-Bench: HTTP vs gRPC 성능 테스트"
echo "================================"

API_SERVER="http://localhost:8080"
RESULTS_DIR="../results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 결과 디렉토리 생성
mkdir -p $RESULTS_DIR

# 서버 헬스체크
echo ""
echo "[1/5] 서버 상태 확인..."
curl -s $API_SERVER/api/health > /dev/null
if [ $? -ne 0 ]; then
    echo "❌ apiServer가 실행되지 않았습니다. 먼저 서버를 시작하세요."
    exit 1
fi
echo "✅ 서버 정상"

# JIT 워밍업
echo ""
echo "[2/5] JIT 워밍업 (각 엔드포인트 100회 호출)..."
for endpoint in "http/json" "http/binary" "grpc" "grpc/stream"; do
    for i in {1..100}; do
        curl -s "$API_SERVER/api/data/$endpoint" > /dev/null
    done
    echo "  - /api/data/$endpoint 워밍업 완료"
done
echo "✅ 워밍업 완료"

# 테스트 실행
echo ""
echo "[3/5] HTTP/JSON 테스트..."
k6 run http-json-test.js 2>&1 | tee "$RESULTS_DIR/http-json_$TIMESTAMP.log"

sleep 5

echo ""
echo "[4/5] HTTP/Binary 테스트..."
k6 run http-binary-test.js 2>&1 | tee "$RESULTS_DIR/http-binary_$TIMESTAMP.log"

sleep 5

echo ""
echo "[5/5] gRPC 테스트..."
k6 run grpc-test.js 2>&1 | tee "$RESULTS_DIR/grpc_$TIMESTAMP.log"

sleep 5

echo ""
echo "[Bonus] gRPC Streaming 테스트..."
k6 run grpc-stream-test.js 2>&1 | tee "$RESULTS_DIR/grpc-stream_$TIMESTAMP.log"

echo ""
echo "================================"
echo "✅ 모든 테스트 완료!"
echo "결과 파일: $RESULTS_DIR/*_$TIMESTAMP.log"
echo "================================"