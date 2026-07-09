#!/usr/bin/env bash
# 부하 테스트 종료 후 정합성을 검증한다.
#
# 검증 항목:
#   1) DB: special_lecture_enrollment.count(special_lecture_id=1) == 100 (정원)
#   2) Redis: special_lecture:1:available == 0 (정원 다 찼을 때)
#   3) DB: special_lecture.version (UPDATE 횟수가 정상 범위인지 sanity-check)
#
# 종료 코드:
#   0  → ✅ 정합성 OK
#   1  → ❌ 정원 초과 (overshoot — A 시나리오에서 발생 가능)
#   2  → ⚠️ 정원 유실 (undershoot — 부하 미달 또는 보상 실패)
#   3  → ⚠️ Redis ↔ DB 불일치
#
# 사용:
#   ./verify.sh
#   ./verify.sh A   # 시나리오 라벨을 출력에 추가

set -euo pipefail

MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PWD=${MYSQL_PWD:-1234}
MYSQL_DB=${MYSQL_DB:-course_db}

REDIS_HOST=${REDIS_HOST:-127.0.0.1}
REDIS_PORT=${REDIS_PORT:-6379}

LECTURE_ID=${LECTURE_ID:-1}
EXPECTED_CAPACITY=${EXPECTED_CAPACITY:-100}
SCENARIO=${1:-?}

DB_COUNT=$(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PWD" "$MYSQL_DB" -N -B -e \
  "SELECT COUNT(*) FROM special_lecture_enrollment WHERE special_lecture_id = ${LECTURE_ID};")

REDIS_REMAIN=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" GET "special_lecture:${LECTURE_ID}:available")
REDIS_REMAIN=${REDIS_REMAIN:-(nil)}

VERSION=$(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PWD" "$MYSQL_DB" -N -B -e \
  "SELECT version FROM schedule WHERE schedule_id = ${LECTURE_ID};")

echo "================================================"
echo " 시나리오 ${SCENARIO} — 정합성 검증"
echo "------------------------------------------------"
printf " DB enrollment count : %s (기대값 %s)\n" "$DB_COUNT" "$EXPECTED_CAPACITY"
printf " Redis 잔여 정원      : %s (기대값 0)\n"   "$REDIS_REMAIN"
printf " schedule.version    : %s (UPDATE 횟수 sanity)\n" "$VERSION"
echo "------------------------------------------------"

EXIT=0
if [ "$DB_COUNT" -gt "$EXPECTED_CAPACITY" ]; then
    echo " ❌ 정원 초과 — 1~3차 방어선 어딘가에서 race condition 발생"
    EXIT=1
elif [ "$DB_COUNT" -lt "$EXPECTED_CAPACITY" ]; then
    echo " ⚠️ 정원 유실 — 부하 미달이거나 보상 트랜잭션이 과도하게 동작"
    EXIT=2
else
    echo " ✅ DB 정합성 OK (정원 == enrollment)"
fi

if [ "$REDIS_REMAIN" != "0" ] && [ "$DB_COUNT" -eq "$EXPECTED_CAPACITY" ]; then
    echo " ⚠️ Redis ↔ DB 불일치 — Redis는 ${REDIS_REMAIN}, DB는 정원 만료. 보상 누락 또는 동기화 미흡."
    [ $EXIT -eq 0 ] && EXIT=3
fi

echo "================================================"
exit $EXIT
