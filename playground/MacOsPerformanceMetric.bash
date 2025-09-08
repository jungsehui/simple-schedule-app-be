#!/bin/bash

# 인자 확인
if [ -z "$1" ] || [ -z "$2" ]; then
  echo "사용법: $0 <PID> <측정할 시간(초)>"
  exit 1
fi

pid=$1         # PID
duration=$2    # 측정할 시간
interval=1     # 1초마다 측정

cpu_usage=()
memory_usage=()

# 지정한 시간 동안 특정 PID의 CPU와 메모리 사용량 측정
end_time=$((SECONDS + duration))
while [ $SECONDS -lt $end_time ]; do
    # PID에 해당하는 CPU 사용량 측정
    cpu=$(ps -p $pid -o %cpu=)

    # PID에 해당하는 메모리 사용량 측정
    memory=$(ps -p $pid -o rss=)  # 메모리 사용량 (KB 단위)

    # 메모리 사용량을 MB로 변환
    memory_mb=$(echo "$memory / 1024" | bc)

    # 배열에 저장
    cpu_usage+=($cpu)
    memory_usage+=($memory_mb)

    # 1초 대기
    sleep $interval
done

# 최대값, 평균값 계산
max_cpu=$(echo "${cpu_usage[@]}" | tr ' ' '\n' | sort -nr | head -n 1)
avg_cpu=$(echo "${cpu_usage[@]}" | awk '{sum=0; count=0; for(i=1;i<=NF;i++) {sum+=$i; count++} print sum/count}')
max_memory=$(echo "${memory_usage[@]}" | tr ' ' '\n' | sort -nr | head -n 1)
avg_memory=$(echo "${memory_usage[@]}" | awk '{sum=0; count=0; for(i=1;i<=NF;i++) {sum+=$i; count++} print sum/count}')

# 결과 출력
echo "PID $pid 의 CPU 사용량 - 최대값: ${max_cpu}%, 평균값: ${avg_cpu}%"
echo "PID $pid 의 메모리 사용량 - 최대값: ${max_memory}MB, 평균값: ${avg_memory}MB"
