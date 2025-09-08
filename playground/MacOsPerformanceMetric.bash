#!/bin/bash

# #############################################################################
# MacOsPerformanceMetric.bash (개선된 버전)
#
# macOS에서 특정 PID의 CPU 및 메모리 사용량을 측정하여
# 최댓값과 평균값을 출력합니다. 숫자 값만 필터링하여 안정성을 높였습니다.
#
# 사용법:
# ./MacOsPerformanceMetric.bash <PID> <측정시간(초)>
# #############################################################################

# 입력값 검증
if [ "$#" -ne 2 ]; then
    echo "사용법: $0 <PID> <측정시간(초)>"
    exit 1
fi

PID=$1
DURATION=$2
COUNT=0

cpu_samples=()
mem_samples=()

# 숫자 검증을 위한 정규식
regex_numeric='^[0-9]+([.][0-9]+)?$'

echo "PID ${PID}의 성능 측정을 ${DURATION}초 동안 시작합니다..."

end_time=$((SECONDS + DURATION))
while [ $SECONDS -lt $end_time ]; do
    # -stats 옵션을 사용하여 컬럼을 명시적으로 지정, -n 0으로 헤더 제거
    stats=$(top -l 1 -pid "${PID}" -n 0 -stats pid,cpu,mem)

    # 프로세스가 종료되면 루프 중단
    if [ -z "$stats" ]; then
        echo ""
        echo "경고: PID ${PID}를 찾을 수 없습니다. 프로세스가 종료되어 측정을 중단합니다."
        break
    fi

    cpu=$(echo "$stats" | awk '{print $2}')
    mem=$(echo "$stats" | awk '{print $3}')

    # CPU 값이 숫자인 경우에만 배열에 추가
    if [[ $cpu =~ $regex_numeric ]]; then
        cpu_samples+=($cpu)
    else
        echo -n "!" # 숫자 아닌 값은 느낌표로 표시
        continue
    fi

    # 메모리 값(MB) 추출 및 변환
    mem_mb=$(echo "$mem" | sed 's/M//' | sed 's/G//' | sed 's/K//')
    if [[ "$mem" == *G ]]; then
        mem_mb=$(echo "$mem_mb * 1024" | bc)
    elif [[ "$mem" == *K ]]; then
        mem_mb=$(echo "scale=2; $mem_mb / 1024" | bc)
    fi

    if [[ $mem_mb =~ $regex_numeric ]]; then
        mem_samples+=($mem_mb)
    fi

    COUNT=$((COUNT + 1))
    echo -n "."
    sleep 1
done

echo ""
echo "${COUNT}번의 측정이 완료되었습니다. 결과 분석 중..."

if [ ${#cpu_samples[@]} -eq 0 ]; then
    echo "오류: 유효한 성능 데이터를 수집하지 못했습니다."
    exit 1
fi

# bc를 이용한 계산
max_cpu=$(printf "%s\n" "${cpu_samples[@]}" | sort -n | tail -n1)
sum_cpu=$(echo "${cpu_samples[@]}" | tr ' ' '+' | bc)
avg_cpu=$(echo "scale=4; ${sum_cpu} / ${#cpu_samples[@]}" | bc)

max_mem=$(printf "%s\n" "${mem_samples[@]}" | sort -n | tail -n1)
sum_mem=$(echo "${mem_samples[@]}" | tr ' ' '+' | bc)
avg_mem=$(echo "scale=4; ${sum_mem} / ${#mem_samples[@]}" | bc)

# 최종 결과 출력
echo "--------------------------------------------------------"
echo "PID ${PID}의 CPU 사용량 - 최대값: ${max_cpu}%, 평균값: ${avg_cpu}%"
echo "PID ${PID}의 메모리 사용량 - 최대값: ${max_mem}MB, 평균값: ${avg_mem}MB"
echo "--------------------------------------------------------"
