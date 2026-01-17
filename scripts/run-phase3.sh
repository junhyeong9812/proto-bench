#!/bin/bash

# Phase 3: 고동시성 벤치마크 (50, 100, 200, 500 VU)

echo "================================"
echo "Phase 3: 고동시성 멀티플렉싱 테스트"
echo "================================"

API_SERVER="${API_SERVER:-http://192.168.55.114:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/../results/phase3"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
SIZE="${SIZE:-10kb}"

mkdir -p $RESULTS_DIR

# 서버 헬스체크
echo ""
echo "[0] 서버 상태 확인..."
curl -s $API_SERVER/api/health > /dev/null
if [ $? -ne 0 ]; then
    echo "❌ apiServer가 실행되지 않았습니다."
    exit 1
fi
echo "✅ 서버 정상"

# JIT 워밍업
echo ""
echo "[Warmup] JIT 워밍업..."
for endpoint in "http/json" "http/binary" "grpc" "grpc/stream"; do
    for i in {1..100}; do
        curl -s "$API_SERVER/api/data/$endpoint?size=$SIZE" > /dev/null
    done
done
echo "✅ 워밍업 완료"

# VU 목록 (기본: 50 100 200 500)
VU_LIST="${VU_LIST:-50 100 200 500}"

run_test() {
    local vus=$1
    local protocol=$2
    local script=$3
    local name=$4

    echo ""
    echo "[$name] ${protocol} - ${vus} VUs 테스트..."
    k6 run -e VUS=$vus -e SIZE=$SIZE -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase3/$script" 2>&1 | tee "$RESULTS_DIR/${protocol}_${vus}vu_$TIMESTAMP.log"
    sleep 3
}

test_num=1
total_tests=$(($(echo $VU_LIST | wc -w) * 4))

for vus in $VU_LIST; do
    echo ""
    echo "=========================================="
    echo "         ${vus} VU 테스트 시작"
    echo "=========================================="

    run_test $vus "http-json" "http-json-test.js" "$test_num/$total_tests"
    ((test_num++))

    run_test $vus "http-binary" "http-binary-test.js" "$test_num/$total_tests"
    ((test_num++))

    run_test $vus "grpc-unary" "grpc-test.js" "$test_num/$total_tests"
    ((test_num++))

    run_test $vus "grpc-stream" "grpc-stream-test.js" "$test_num/$total_tests"
    ((test_num++))
done

echo ""
echo "================================"
echo "✅ Phase 3 테스트 완료!"
echo "결과 파일: $RESULTS_DIR/*_$TIMESTAMP.log"
echo "================================"