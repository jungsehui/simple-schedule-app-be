#!/usr/bin/env bash
# 시나리오 간 상태 초기화: enrollment 데이터 삭제 + Redis 정원 카운터를 100으로 SET.
#
# 사용:
#   ./reset.sh
#
# 환경변수:
#   MYSQL_HOST(기본 127.0.0.1) MYSQL_PORT(3306) MYSQL_USER(root) MYSQL_PWD(1234) MYSQL_DB(course_db)
#   REDIS_HOST(127.0.0.1) REDIS_PORT(6379)

set -euo pipefail

MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PWD=${MYSQL_PWD:-1234}
MYSQL_DB=${MYSQL_DB:-course_db}

REDIS_HOST=${REDIS_HOST:-127.0.0.1}
REDIS_PORT=${REDIS_PORT:-6379}

LECTURE_ID=${LECTURE_ID:-1}
CAPACITY=${CAPACITY:-100}

echo "[reset] DB enrollment 삭제 + special_lecture.version 초기화"
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PWD" "$MYSQL_DB" <<SQL
DELETE FROM special_lecture_enrollment WHERE special_lecture_id = ${LECTURE_ID};
UPDATE schedule SET version = 0 WHERE schedule_id = ${LECTURE_ID};
SQL

echo "[reset] Redis 정원 카운터 = ${CAPACITY}"
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" SET "special_lecture:${LECTURE_ID}:available" "$CAPACITY"

echo "[reset] 완료"
