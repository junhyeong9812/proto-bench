#!/bin/bash

# =============================================================================
# Phase 7: 극한 복잡도에서의 역전 포인트 탐색 (CPU 사용량 측정 포함)
# =============================================================================

echo "============================================"
echo "Phase 7: CPU 사용량 포함 성능 분석"
echo "============================================"

API_SERVER="${API_SERVER:-http://192.168.55.114:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/../results/phase7"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

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
        for i in {1..50}; do
            curl -s "$API_SERVER/api/$endpoint?complexity=$complexity" > /dev/null 2>&1
        done
    done
done
echo "✅ 워밍업 완료"

# 테스트 카운터
TOTAL_TESTS=$(echo $COMPLEXITY_LIST | wc -w)
TOTAL_TESTS=$((TOTAL_TESTS * 4))
CURRENT_TEST=0

# 각 복잡도별 테스트
for COMPLEXITY in $COMPLEXITY_LIST; do
    echo ""
    echo "=========================================="
    echo "         ${COMPLEXITY} 복잡도 테스트"
    echo "=========================================="

    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] HTTP/JSON - ${COMPLEXITY} 테스트..."
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase7/http-json-test.js" 2>&1 | tee "$RESULTS_DIR/http-json_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 5

    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] HTTP/Binary - ${COMPLEXITY} 테스트..."
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase7/http-binary-test.js" 2>&1 | tee "$RESULTS_DIR/http-binary_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 5

    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] gRPC/Unary - ${COMPLEXITY} 테스트..."
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase7/grpc-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-unary_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 5

    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] gRPC/Stream - ${COMPLEXITY} 테스트..."
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase7/grpc-stream-test.js" 2>&1 | tee "$RESULTS_DIR/grpc-stream_${COMPLEXITY}_$TIMESTAMP.log"
    sleep 5
done

echo ""
echo "============================================"
echo "✅ Phase 7 테스트 완료!"
echo "결과 파일: $RESULTS_DIR/*_$TIMESTAMP.log"
echo "============================================"

# 결과 요약 출력
echo ""
echo "=========================================="
echo "         결과 요약 - Throughput & Latency"
echo "=========================================="
echo ""
echo "| 복잡도  | 프로토콜    | Throughput | Latency P95 | JSON vs gRPC |"
echo "|---------|------------|------------|-------------|--------------|"

for COMPLEXITY in $COMPLEXITY_LIST; do
    HTTP_JSON_LOG="$RESULTS_DIR/http-json_${COMPLEXITY}_$TIMESTAMP.log"
    HTTP_BINARY_LOG="$RESULTS_DIR/http-binary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_UNARY_LOG="$RESULTS_DIR/grpc-unary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_STREAM_LOG="$RESULTS_DIR/grpc-stream_${COMPLEXITY}_$TIMESTAMP.log"

    # Throughput 추출
    HTTP_JSON_RPS=$(grep "Throughput:" "$HTTP_JSON_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    HTTP_BINARY_RPS=$(grep "Throughput:" "$HTTP_BINARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_UNARY_RPS=$(grep "Throughput:" "$GRPC_UNARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_STREAM_RPS=$(grep "Throughput:" "$GRPC_STREAM_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)

    # Latency P95 추출
    HTTP_JSON_P95=$(grep "P95:" "$HTTP_JSON_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    HTTP_BINARY_P95=$(grep "P95:" "$HTTP_BINARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_UNARY_P95=$(grep "P95:" "$GRPC_UNARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_STREAM_P95=$(grep "P95:" "$GRPC_STREAM_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)

    # 기본값 설정
    HTTP_JSON_RPS="${HTTP_JSON_RPS:-N/A}"
    HTTP_BINARY_RPS="${HTTP_BINARY_RPS:-N/A}"
    GRPC_UNARY_RPS="${GRPC_UNARY_RPS:-N/A}"
    GRPC_STREAM_RPS="${GRPC_STREAM_RPS:-N/A}"
    HTTP_JSON_P95="${HTTP_JSON_P95:-N/A}"
    HTTP_BINARY_P95="${HTTP_BINARY_P95:-N/A}"
    GRPC_UNARY_P95="${GRPC_UNARY_P95:-N/A}"
    GRPC_STREAM_P95="${GRPC_STREAM_P95:-N/A}"

    # JSON vs gRPC/Unary 비교
    WINNER="N/A"
    if [ "$HTTP_JSON_RPS" != "N/A" ] && [ "$GRPC_UNARY_RPS" != "N/A" ]; then
        HTTP_INT=$(echo "$HTTP_JSON_RPS" | cut -d. -f1)
        GRPC_INT=$(echo "$GRPC_UNARY_RPS" | cut -d. -f1)

        if [[ "$HTTP_INT" =~ ^[0-9]+$ ]] && [[ "$GRPC_INT" =~ ^[0-9]+$ ]]; then
            if [ "$HTTP_INT" -gt 0 ] && [ "$GRPC_INT" -gt 0 ]; then
                if [ "$GRPC_INT" -gt "$HTTP_INT" ]; then
                    DIFF=$(( (GRPC_INT - HTTP_INT) * 100 / HTTP_INT ))
                    WINNER="gRPC +${DIFF}%"
                elif [ "$HTTP_INT" -gt "$GRPC_INT" ]; then
                    DIFF=$(( (HTTP_INT - GRPC_INT) * 100 / GRPC_INT ))
                    WINNER="**JSON +${DIFF}%**"
                else
                    WINNER="동일"
                fi
            fi
        fi
    fi

    printf "| %-7s | HTTP/JSON  | %10s | %11s | %12s |\n" "$COMPLEXITY" "$HTTP_JSON_RPS" "$HTTP_JSON_P95" "$WINNER"
    printf "|         | HTTP/Binary| %10s | %11s |              |\n" "$HTTP_BINARY_RPS" "$HTTP_BINARY_P95"
    printf "|         | gRPC/Unary | %10s | %11s |              |\n" "$GRPC_UNARY_RPS" "$GRPC_UNARY_P95"
    printf "|         | gRPC/Stream| %10s | %11s |              |\n" "$GRPC_STREAM_RPS" "$GRPC_STREAM_P95"
    echo "|---------|------------|------------|-------------|--------------|"
done

echo ""
echo "=========================================="
echo "         결과 요약 - CPU 사용량"
echo "=========================================="
echo ""
echo "| 복잡도  | 프로토콜    | Avg CPU(%) | Peak CPU(%) | Avg Proc(%) | Peak Proc(%) |"
echo "|---------|------------|------------|-------------|-------------|--------------|"

for COMPLEXITY in $COMPLEXITY_LIST; do
    HTTP_JSON_LOG="$RESULTS_DIR/http-json_${COMPLEXITY}_$TIMESTAMP.log"
    HTTP_BINARY_LOG="$RESULTS_DIR/http-binary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_UNARY_LOG="$RESULTS_DIR/grpc-unary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_STREAM_LOG="$RESULTS_DIR/grpc-stream_${COMPLEXITY}_$TIMESTAMP.log"

    # CPU 메트릭 추출
    HTTP_JSON_AVG_CPU=$(grep "Avg System CPU:" "$HTTP_JSON_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    HTTP_JSON_PEAK_CPU=$(grep "Peak System CPU:" "$HTTP_JSON_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    HTTP_JSON_AVG_PROC=$(grep "Avg Process CPU:" "$HTTP_JSON_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    HTTP_JSON_PEAK_PROC=$(grep "Peak Process CPU:" "$HTTP_JSON_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)

    HTTP_BINARY_AVG_CPU=$(grep "Avg System CPU:" "$HTTP_BINARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    HTTP_BINARY_PEAK_CPU=$(grep "Peak System CPU:" "$HTTP_BINARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    HTTP_BINARY_AVG_PROC=$(grep "Avg Process CPU:" "$HTTP_BINARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    HTTP_BINARY_PEAK_PROC=$(grep "Peak Process CPU:" "$HTTP_BINARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)

    GRPC_UNARY_AVG_CPU=$(grep "Avg System CPU:" "$GRPC_UNARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_UNARY_PEAK_CPU=$(grep "Peak System CPU:" "$GRPC_UNARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_UNARY_AVG_PROC=$(grep "Avg Process CPU:" "$GRPC_UNARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_UNARY_PEAK_PROC=$(grep "Peak Process CPU:" "$GRPC_UNARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)

    GRPC_STREAM_AVG_CPU=$(grep "Avg System CPU:" "$GRPC_STREAM_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_STREAM_PEAK_CPU=$(grep "Peak System CPU:" "$GRPC_STREAM_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_STREAM_AVG_PROC=$(grep "Avg Process CPU:" "$GRPC_STREAM_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_STREAM_PEAK_PROC=$(grep "Peak Process CPU:" "$GRPC_STREAM_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)

    # 기본값 설정
    HTTP_JSON_AVG_CPU="${HTTP_JSON_AVG_CPU:-N/A}"
    HTTP_JSON_PEAK_CPU="${HTTP_JSON_PEAK_CPU:-N/A}"
    HTTP_JSON_AVG_PROC="${HTTP_JSON_AVG_PROC:-N/A}"
    HTTP_JSON_PEAK_PROC="${HTTP_JSON_PEAK_PROC:-N/A}"
    HTTP_BINARY_AVG_CPU="${HTTP_BINARY_AVG_CPU:-N/A}"
    HTTP_BINARY_PEAK_CPU="${HTTP_BINARY_PEAK_CPU:-N/A}"
    HTTP_BINARY_AVG_PROC="${HTTP_BINARY_AVG_PROC:-N/A}"
    HTTP_BINARY_PEAK_PROC="${HTTP_BINARY_PEAK_PROC:-N/A}"
    GRPC_UNARY_AVG_CPU="${GRPC_UNARY_AVG_CPU:-N/A}"
    GRPC_UNARY_PEAK_CPU="${GRPC_UNARY_PEAK_CPU:-N/A}"
    GRPC_UNARY_AVG_PROC="${GRPC_UNARY_AVG_PROC:-N/A}"
    GRPC_UNARY_PEAK_PROC="${GRPC_UNARY_PEAK_PROC:-N/A}"
    GRPC_STREAM_AVG_CPU="${GRPC_STREAM_AVG_CPU:-N/A}"
    GRPC_STREAM_PEAK_CPU="${GRPC_STREAM_PEAK_CPU:-N/A}"
    GRPC_STREAM_AVG_PROC="${GRPC_STREAM_AVG_PROC:-N/A}"
    GRPC_STREAM_PEAK_PROC="${GRPC_STREAM_PEAK_PROC:-N/A}"

    printf "| %-7s | HTTP/JSON  | %10s | %11s | %11s | %12s |\n" "$COMPLEXITY" "$HTTP_JSON_AVG_CPU" "$HTTP_JSON_PEAK_CPU" "$HTTP_JSON_AVG_PROC" "$HTTP_JSON_PEAK_PROC"
    printf "|         | HTTP/Binary| %10s | %11s | %11s | %12s |\n" "$HTTP_BINARY_AVG_CPU" "$HTTP_BINARY_PEAK_CPU" "$HTTP_BINARY_AVG_PROC" "$HTTP_BINARY_PEAK_PROC"
    printf "|         | gRPC/Unary | %10s | %11s | %11s | %12s |\n" "$GRPC_UNARY_AVG_CPU" "$GRPC_UNARY_PEAK_CPU" "$GRPC_UNARY_AVG_PROC" "$GRPC_UNARY_PEAK_PROC"
    printf "|         | gRPC/Stream| %10s | %11s | %11s | %12s |\n" "$GRPC_STREAM_AVG_CPU" "$GRPC_STREAM_PEAK_CPU" "$GRPC_STREAM_AVG_PROC" "$GRPC_STREAM_PEAK_PROC"
    echo "|---------|------------|------------|-------------|-------------|--------------|"
done

echo ""
echo "=========================================="
echo "         결과 요약 - Memory & GC"
echo "=========================================="
echo ""
echo "| 복잡도  | 프로토콜    | Peak Heap  | GC Count |"
echo "|---------|------------|------------|----------|"

for COMPLEXITY in $COMPLEXITY_LIST; do
    HTTP_JSON_LOG="$RESULTS_DIR/http-json_${COMPLEXITY}_$TIMESTAMP.log"
    HTTP_BINARY_LOG="$RESULTS_DIR/http-binary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_UNARY_LOG="$RESULTS_DIR/grpc-unary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_STREAM_LOG="$RESULTS_DIR/grpc-stream_${COMPLEXITY}_$TIMESTAMP.log"

    # Memory 메트릭 추출
    HTTP_JSON_HEAP=$(grep "Peak Heap:" "$HTTP_JSON_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    HTTP_JSON_GC=$(grep "GC Count:" "$HTTP_JSON_LOG" 2>/dev/null | tail -1 | grep -oP '\d+' | head -1)
    HTTP_BINARY_HEAP=$(grep "Peak Heap:" "$HTTP_BINARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    HTTP_BINARY_GC=$(grep "GC Count:" "$HTTP_BINARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+' | head -1)
    GRPC_UNARY_HEAP=$(grep "Peak Heap:" "$GRPC_UNARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_UNARY_GC=$(grep "GC Count:" "$GRPC_UNARY_LOG" 2>/dev/null | tail -1 | grep -oP '\d+' | head -1)
    GRPC_STREAM_HEAP=$(grep "Peak Heap:" "$GRPC_STREAM_LOG" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
    GRPC_STREAM_GC=$(grep "GC Count:" "$GRPC_STREAM_LOG" 2>/dev/null | tail -1 | grep -oP '\d+' | head -1)

    # 기본값 설정
    HTTP_JSON_HEAP="${HTTP_JSON_HEAP:-N/A}"
    HTTP_JSON_GC="${HTTP_JSON_GC:-N/A}"
    HTTP_BINARY_HEAP="${HTTP_BINARY_HEAP:-N/A}"
    HTTP_BINARY_GC="${HTTP_BINARY_GC:-N/A}"
    GRPC_UNARY_HEAP="${GRPC_UNARY_HEAP:-N/A}"
    GRPC_UNARY_GC="${GRPC_UNARY_GC:-N/A}"
    GRPC_STREAM_HEAP="${GRPC_STREAM_HEAP:-N/A}"
    GRPC_STREAM_GC="${GRPC_STREAM_GC:-N/A}"

    printf "| %-7s | HTTP/JSON  | %8sMB | %8s |\n" "$COMPLEXITY" "$HTTP_JSON_HEAP" "$HTTP_JSON_GC"
    printf "|         | HTTP/Binary| %8sMB | %8s |\n" "$HTTP_BINARY_HEAP" "$HTTP_BINARY_GC"
    printf "|         | gRPC/Unary | %8sMB | %8s |\n" "$GRPC_UNARY_HEAP" "$GRPC_UNARY_GC"
    printf "|         | gRPC/Stream| %8sMB | %8s |\n" "$GRPC_STREAM_HEAP" "$GRPC_STREAM_GC"
    echo "|---------|------------|------------|----------|"
done

echo ""
echo "=========================================="
echo "         CPU 효율성 분석"
echo "=========================================="
echo ""
echo "CPU 효율성 = Throughput / Avg Process CPU"
echo "(높을수록 CPU 1% 당 더 많은 요청 처리)"
echo ""
echo "| 복잡도  | 프로토콜    | Throughput | Avg Proc CPU | 효율성 |"
echo "|---------|------------|------------|--------------|--------|"

for COMPLEXITY in $COMPLEXITY_LIST; do
    HTTP_JSON_LOG="$RESULTS_DIR/http-json_${COMPLEXITY}_$TIMESTAMP.log"
    HTTP_BINARY_LOG="$RESULTS_DIR/http-binary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_UNARY_LOG="$RESULTS_DIR/grpc-unary_${COMPLEXITY}_$TIMESTAMP.log"
    GRPC_STREAM_LOG="$RESULTS_DIR/grpc-stream_${COMPLEXITY}_$TIMESTAMP.log"

    for protocol in "HTTP/JSON" "HTTP/Binary" "gRPC/Unary" "gRPC/Stream"; do
        case $protocol in
            "HTTP/JSON") LOG_FILE="$HTTP_JSON_LOG" ;;
            "HTTP/Binary") LOG_FILE="$HTTP_BINARY_LOG" ;;
            "gRPC/Unary") LOG_FILE="$GRPC_UNARY_LOG" ;;
            "gRPC/Stream") LOG_FILE="$GRPC_STREAM_LOG" ;;
        esac

        RPS=$(grep "Throughput:" "$LOG_FILE" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)
        AVG_CPU=$(grep "Avg Process CPU:" "$LOG_FILE" 2>/dev/null | tail -1 | grep -oP '\d+\.\d+' | head -1)

        RPS="${RPS:-N/A}"
        AVG_CPU="${AVG_CPU:-N/A}"

        EFFICIENCY="N/A"
        if [ "$RPS" != "N/A" ] && [ "$AVG_CPU" != "N/A" ]; then
            EFFICIENCY=$(echo "scale=2; $RPS / $AVG_CPU" | bc 2>/dev/null || echo "N/A")
        fi

        if [ "$protocol" = "HTTP/JSON" ]; then
            printf "| %-7s | %-11s | %10s | %12s | %6s |\n" "$COMPLEXITY" "$protocol" "$RPS" "$AVG_CPU" "$EFFICIENCY"
        else
            printf "|         | %-11s | %10s | %12s | %6s |\n" "$protocol" "$RPS" "$AVG_CPU" "$EFFICIENCY"
        fi
    done
    echo "|---------|------------|------------|--------------|--------|"
done

echo ""
echo "=========================================="
echo "         가설 검증 포인트"
echo "=========================================="
echo ""
echo "1. CPU 사용량 비교"
echo "   - gRPC가 JSON보다 더 많은 CPU를 사용하는가?"
echo "   - Protobuf 빌더 생성이 CPU 집약적인가?"
echo ""
echo "2. CPU 효율성 비교"
echo "   - 어떤 프로토콜이 CPU 1% 당 더 많은 요청을 처리하는가?"
echo "   - 극한 복잡도에서 효율성 역전이 발생하는가?"
echo ""
echo "3. 역전 원인 분석"
echo "   - 높은 CPU 사용량 = 높은 성능? (No)"
echo "   - 빌더 객체 생성 오버헤드가 CPU 사용량 증가의 주 원인"
echo ""
echo "=========================================="