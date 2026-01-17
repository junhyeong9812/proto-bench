#!/bin/bash

# =============================================================================
# Phase 7: 극한 복잡도에서의 역전 포인트 탐색 (CPU 사용량 측정 포함)
# =============================================================================
#
# 목적: Phase 6에서 발견한 역전 현상을 CPU 사용량과 함께 분석하여,
#       프로토콜별 CPU 효율성을 비교한다.
#
# 가설: "gRPC/Protobuf는 극한 복잡도에서 빌더 객체 생성으로 인해
#        JSON보다 더 많은 CPU를 사용할 것이다"
#
# 측정 항목:
#   - Throughput (req/s)
#   - Latency (avg, p95)
#   - CPU Usage (avg, peak) - 시스템 전체
#   - Process CPU (avg, peak) - JVM 프로세스
#   - Memory (heap, gc)
#
# 복잡도:
#   - ultra: ~150필드, 3단계 중첩, ~200회 빌더 호출
#   - extreme: ~500필드, 4단계 중첩, ~800회 빌더 호출
#
# =============================================================================

echo "============================================"
echo "Phase 7: CPU 사용량 포함 성능 분석"
echo "============================================"

API_SERVER="${API_SERVER:-http://192.168.55.114:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/../results/phase7"
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

# 결과 저장용 배열
declare -A RESULTS

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
    LOG_FILE="$RESULTS_DIR/http-json_${COMPLEXITY}_$TIMESTAMP.log"
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase7/http-json-test.js" 2>&1 | tee "$LOG_FILE"
    sleep 5  # 극한 복잡도이므로 쿨다운 시간 증가

    # HTTP/Binary (Protobuf) 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] HTTP/Binary - ${COMPLEXITY} 테스트..."
    LOG_FILE="$RESULTS_DIR/http-binary_${COMPLEXITY}_$TIMESTAMP.log"
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase7/http-binary-test.js" 2>&1 | tee "$LOG_FILE"
    sleep 5

    # gRPC/Unary 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] gRPC/Unary - ${COMPLEXITY} 테스트..."
    LOG_FILE="$RESULTS_DIR/grpc-unary_${COMPLEXITY}_$TIMESTAMP.log"
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase7/grpc-test.js" 2>&1 | tee "$LOG_FILE"
    sleep 5

    # gRPC/Stream 테스트
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo ""
    echo "[$CURRENT_TEST/$TOTAL_TESTS] gRPC/Stream - ${COMPLEXITY} 테스트..."
    LOG_FILE="$RESULTS_DIR/grpc-stream_${COMPLEXITY}_$TIMESTAMP.log"
    k6 run \
        -e COMPLEXITY=$COMPLEXITY \
        -e API_SERVER=$API_SERVER \
        "$SCRIPT_DIR/phase7/grpc-stream-test.js" 2>&1 | tee "$LOG_FILE"
    sleep 5
done

echo ""
echo "============================================"
echo "✅ Phase 7 테스트 완료!"
echo "결과 파일: $RESULTS_DIR/*_$TIMESTAMP.log"
echo "============================================"

# JSON 결과 파싱 함수 (k6 로그 포맷에 맞게 수정)
parse_json_result() {
    local log_file="$1"
    local field="$2"

    # k6 로그에서 JSON RESULT 라인 찾기
    # 형식: time="..." level=info msg="{\"protocol\":...}" source=console
    # "=== JSON RESULT ===" 다음 줄에서 msg="..." 안의 JSON 추출

    python3 << EOF
import re
import json
import sys

try:
    with open('$log_file', 'r') as f:
        content = f.read()

    # "=== JSON RESULT ===" 이후의 내용에서 msg="..." 패턴 찾기
    # k6 로그 형식: msg="{\"protocol\":...}"
    pattern = r'=== JSON RESULT ===.*?msg="(\{.*?\})"'
    match = re.search(pattern, content, re.DOTALL)

    if not match:
        # 다른 형식 시도: msg='...' 또는 직접 JSON
        pattern2 = r'=== JSON RESULT ===.*?level=info msg="(\{[^"]+\})"'
        match = re.search(pattern2, content, re.DOTALL)

    if not match:
        # 이스케이프된 JSON 찾기
        pattern3 = r'msg="(\{\\\"protocol\\\".*?\})"'
        match = re.search(pattern3, content)

    if match:
        json_str = match.group(1)
        # 이스케이프된 따옴표 복원
        json_str = json_str.replace('\\"', '"')
        json_str = json_str.replace('\\\\', '\\')

        data = json.loads(json_str)

        # 중첩 필드 접근
        fields = '$field'.split('.')
        result = data
        for f in fields:
            result = result[f]

        if isinstance(result, float):
            print(f'{result:.2f}')
        else:
            print(result)
    else:
        print('N/A')
except Exception as e:
    print('N/A')
EOF
}

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
    HTTP_JSON_RPS=$(parse_json_result "$HTTP_JSON_LOG" "throughputRps")
    HTTP_BINARY_RPS=$(parse_json_result "$HTTP_BINARY_LOG" "throughputRps")
    GRPC_UNARY_RPS=$(parse_json_result "$GRPC_UNARY_LOG" "throughputRps")
    GRPC_STREAM_RPS=$(parse_json_result "$GRPC_STREAM_LOG" "throughputRps")

    # Latency P95 추출
    HTTP_JSON_P95=$(parse_json_result "$HTTP_JSON_LOG" "latency.p95Ms")
    HTTP_BINARY_P95=$(parse_json_result "$HTTP_BINARY_LOG" "latency.p95Ms")
    GRPC_UNARY_P95=$(parse_json_result "$GRPC_UNARY_LOG" "latency.p95Ms")
    GRPC_STREAM_P95=$(parse_json_result "$GRPC_STREAM_LOG" "latency.p95Ms")

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
    printf "| %-7s | HTTP/Binary| %10s | %11s |              |\n" "" "$HTTP_BINARY_RPS" "$HTTP_BINARY_P95"
    printf "| %-7s | gRPC/Unary | %10s | %11s |              |\n" "" "$GRPC_UNARY_RPS" "$GRPC_UNARY_P95"
    printf "| %-7s | gRPC/Stream| %10s | %11s |              |\n" "" "$GRPC_STREAM_RPS" "$GRPC_STREAM_P95"
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
    HTTP_JSON_AVG_CPU=$(parse_json_result "$HTTP_JSON_LOG" "serverMetrics.avgCpuUsagePercent")
    HTTP_JSON_PEAK_CPU=$(parse_json_result "$HTTP_JSON_LOG" "serverMetrics.peakCpuUsagePercent")
    HTTP_JSON_AVG_PROC=$(parse_json_result "$HTTP_JSON_LOG" "serverMetrics.avgProcessCpuPercent")
    HTTP_JSON_PEAK_PROC=$(parse_json_result "$HTTP_JSON_LOG" "serverMetrics.peakProcessCpuPercent")

    HTTP_BINARY_AVG_CPU=$(parse_json_result "$HTTP_BINARY_LOG" "serverMetrics.avgCpuUsagePercent")
    HTTP_BINARY_PEAK_CPU=$(parse_json_result "$HTTP_BINARY_LOG" "serverMetrics.peakCpuUsagePercent")
    HTTP_BINARY_AVG_PROC=$(parse_json_result "$HTTP_BINARY_LOG" "serverMetrics.avgProcessCpuPercent")
    HTTP_BINARY_PEAK_PROC=$(parse_json_result "$HTTP_BINARY_LOG" "serverMetrics.peakProcessCpuPercent")

    GRPC_UNARY_AVG_CPU=$(parse_json_result "$GRPC_UNARY_LOG" "serverMetrics.avgCpuUsagePercent")
    GRPC_UNARY_PEAK_CPU=$(parse_json_result "$GRPC_UNARY_LOG" "serverMetrics.peakCpuUsagePercent")
    GRPC_UNARY_AVG_PROC=$(parse_json_result "$GRPC_UNARY_LOG" "serverMetrics.avgProcessCpuPercent")
    GRPC_UNARY_PEAK_PROC=$(parse_json_result "$GRPC_UNARY_LOG" "serverMetrics.peakProcessCpuPercent")

    GRPC_STREAM_AVG_CPU=$(parse_json_result "$GRPC_STREAM_LOG" "serverMetrics.avgCpuUsagePercent")
    GRPC_STREAM_PEAK_CPU=$(parse_json_result "$GRPC_STREAM_LOG" "serverMetrics.peakCpuUsagePercent")
    GRPC_STREAM_AVG_PROC=$(parse_json_result "$GRPC_STREAM_LOG" "serverMetrics.avgProcessCpuPercent")
    GRPC_STREAM_PEAK_PROC=$(parse_json_result "$GRPC_STREAM_LOG" "serverMetrics.peakProcessCpuPercent")

    printf "| %-7s | HTTP/JSON  | %10s | %11s | %11s | %12s |\n" "$COMPLEXITY" "$HTTP_JSON_AVG_CPU" "$HTTP_JSON_PEAK_CPU" "$HTTP_JSON_AVG_PROC" "$HTTP_JSON_PEAK_PROC"
    printf "| %-7s | HTTP/Binary| %10s | %11s | %11s | %12s |\n" "" "$HTTP_BINARY_AVG_CPU" "$HTTP_BINARY_PEAK_CPU" "$HTTP_BINARY_AVG_PROC" "$HTTP_BINARY_PEAK_PROC"
    printf "| %-7s | gRPC/Unary | %10s | %11s | %11s | %12s |\n" "" "$GRPC_UNARY_AVG_CPU" "$GRPC_UNARY_PEAK_CPU" "$GRPC_UNARY_AVG_PROC" "$GRPC_UNARY_PEAK_PROC"
    printf "| %-7s | gRPC/Stream| %10s | %11s | %11s | %12s |\n" "" "$GRPC_STREAM_AVG_CPU" "$GRPC_STREAM_PEAK_CPU" "$GRPC_STREAM_AVG_PROC" "$GRPC_STREAM_PEAK_PROC"
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
    HTTP_JSON_HEAP=$(parse_json_result "$HTTP_JSON_LOG" "serverMetrics.peakHeapMb")
    HTTP_JSON_GC=$(parse_json_result "$HTTP_JSON_LOG" "serverMetrics.gcCount")

    HTTP_BINARY_HEAP=$(parse_json_result "$HTTP_BINARY_LOG" "serverMetrics.peakHeapMb")
    HTTP_BINARY_GC=$(parse_json_result "$HTTP_BINARY_LOG" "serverMetrics.gcCount")

    GRPC_UNARY_HEAP=$(parse_json_result "$GRPC_UNARY_LOG" "serverMetrics.peakHeapMb")
    GRPC_UNARY_GC=$(parse_json_result "$GRPC_UNARY_LOG" "serverMetrics.gcCount")

    GRPC_STREAM_HEAP=$(parse_json_result "$GRPC_STREAM_LOG" "serverMetrics.peakHeapMb")
    GRPC_STREAM_GC=$(parse_json_result "$GRPC_STREAM_LOG" "serverMetrics.gcCount")

    printf "| %-7s | HTTP/JSON  | %10s | %8s |\n" "$COMPLEXITY" "${HTTP_JSON_HEAP}MB" "$HTTP_JSON_GC"
    printf "| %-7s | HTTP/Binary| %10s | %8s |\n" "" "${HTTP_BINARY_HEAP}MB" "$HTTP_BINARY_GC"
    printf "| %-7s | gRPC/Unary | %10s | %8s |\n" "" "${GRPC_UNARY_HEAP}MB" "$GRPC_UNARY_GC"
    printf "| %-7s | gRPC/Stream| %10s | %8s |\n" "" "${GRPC_STREAM_HEAP}MB" "$GRPC_STREAM_GC"
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
echo "| 복잡도  | 프로토콜    | Throughput | Avg CPU | 효율성 (req/s/%) |"
echo "|---------|------------|------------|---------|-----------------|"

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

        RPS=$(parse_json_result "$LOG_FILE" "throughputRps")
        AVG_CPU=$(parse_json_result "$LOG_FILE" "serverMetrics.avgProcessCpuPercent")

        EFFICIENCY="N/A"
        if [ "$RPS" != "N/A" ] && [ "$AVG_CPU" != "N/A" ]; then
            # 소수점 계산
            EFFICIENCY=$(python3 -c "
rps = float('$RPS')
cpu = float('$AVG_CPU')
if cpu > 0:
    print(f'{rps/cpu:.2f}')
else:
    print('N/A')
" 2>/dev/null)
        fi

        if [ "$protocol" = "HTTP/JSON" ]; then
            printf "| %-7s | %-10s | %10s | %7s | %15s |\n" "$COMPLEXITY" "$protocol" "$RPS" "$AVG_CPU" "$EFFICIENCY"
        else
            printf "| %-7s | %-10s | %10s | %7s | %15s |\n" "" "$protocol" "$RPS" "$AVG_CPU" "$EFFICIENCY"
        fi
    done
    echo "|---------|------------|------------|---------|-----------------|"
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

# JSON 형식으로 전체 결과 저장
echo ""
echo "[*] 전체 결과를 JSON으로 저장 중..."

SUMMARY_FILE="$RESULTS_DIR/summary_$TIMESTAMP.json"

# Python을 사용하여 JSON 파일 생성
python3 << EOF
import re
import json
import os

results_dir = "$RESULTS_DIR"
timestamp = "$TIMESTAMP"
complexity_list = "$COMPLEXITY_LIST".split()

def extract_json_from_log(log_file):
    try:
        with open(log_file, 'r') as f:
            content = f.read()

        # k6 로그에서 JSON 추출
        pattern = r'msg="(\{\\\"protocol\\\".*?\})"'
        match = re.search(pattern, content)

        if match:
            json_str = match.group(1)
            json_str = json_str.replace('\\"', '"')
            json_str = json_str.replace('\\\\', '\\\\')
            return json.loads(json_str)
    except Exception as e:
        pass
    return None

summary = {
    "timestamp": timestamp,
    "complexities": complexity_list,
    "results": {}
}

for complexity in complexity_list:
    summary["results"][complexity] = {}

    protocols = [
        ("http_json", "http-json"),
        ("http_binary", "http-binary"),
        ("grpc_unary", "grpc-unary"),
        ("grpc_stream", "grpc-stream")
    ]

    for key, prefix in protocols:
        log_file = os.path.join(results_dir, f"{prefix}_{complexity}_{timestamp}.log")
        data = extract_json_from_log(log_file)
        summary["results"][complexity][key] = data

with open("$SUMMARY_FILE", 'w') as f:
    json.dump(summary, f, indent=2)

print(f"✅ JSON 요약 저장: $SUMMARY_FILE")
EOF

echo ""
echo "=========================================="