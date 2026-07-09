// 4단계 방어 구조 — 시나리오 A/B/C 공통 모듈
//
// 사용법:
//   import { runEnrollmentScenario, scenarioOptions } from './common/enrollment.js';
//   export const options = scenarioOptions('A');
//   export default function () { runEnrollmentScenario('A'); }
//
// 측정 지표:
// - http_req_duration / TPS (k6 내장)
// - enroll_success_count        (Counter): HTTP 200
// - enroll_capacity_exceeded    (Counter): 1차 Redis 거절
// - enroll_already_enrolled     (Counter): 4차 UK 충돌 (중복 신청)
// - enroll_lock_failed          (Counter): 2차 분산 락 획득 실패
// - enroll_optimistic_failed    (Counter): 3차 낙관적 락 충돌
// - enroll_unknown_failed       (Counter): 분류되지 않은 실패
//
// 정합성은 K6 종료 후 별도 SQL/Redis 조회로 검증한다 (./common/verify.sh).

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

// 공통 메트릭
export const successCount         = new Counter('enroll_success_count');
export const capacityExceededCount = new Counter('enroll_capacity_exceeded');
export const alreadyEnrolledCount  = new Counter('enroll_already_enrolled');
export const lockFailedCount       = new Counter('enroll_lock_failed');
export const optimisticFailedCount = new Counter('enroll_optimistic_failed');
export const unknownFailedCount    = new Counter('enroll_unknown_failed');

// 테스트 대상 특강 / 서버 주소 (환경변수로 오버라이드 가능)
const SPECIAL_LECTURE_ID = __ENV.SPECIAL_LECTURE_ID || 1;
const TARGET_HOST        = __ENV.TARGET_HOST || 'http://host.docker.internal:8080';

// 시나리오 공통 옵션
// per-vu-iterations: VU 1명당 1회 요청 → "10,000명이 동시에 누른다"를 그대로 모사
export function scenarioOptions(scenarioTag) {
    return {
        scenarios: {
            concurrent: {
                executor: 'per-vu-iterations',
                vus: 10000,
                iterations: 1,
                maxDuration: '1m',
                tags: { scenario: scenarioTag },
            },
        },
        // 4단계 방어가 정합성을 보장한다는 가정 하의 기준선
        thresholds: {
            // p95 응답시간이 1초 이내일 것 (분산 락 대기 포함)
            'http_req_duration{scenario:' + scenarioTag + '}': ['p(95) < 1000'],
            // 실패율: 정원 초과/중복은 정상 동작이므로 카운트만 하고 별도 임계값은 두지 않는다
            // (정합성은 verify.sh에서 SQL로 직접 검증)
        },
    };
}

export function runEnrollmentScenario(scenarioTag) {
    const studentId = __VU;  // 1..10000 unique
    const url = `${TARGET_HOST}/special-lectures/${SPECIAL_LECTURE_ID}/enrollments?studentId=${studentId}`;

    const res = http.post(url, null, { tags: { scenario: scenarioTag } });

    // 응답 분류: 결과 코드(없으면 raw text)에 따라 메트릭 누적
    classify(res);

    check(res, {
        'is 200 or 4xx': (r) => r.status === 200 || (r.status >= 400 && r.status < 500),
        'no 5xx server error': (r) => r.status < 500,
    });
}

// 응답 본문에 도메인 에러 코드(SLE001/SLE003 등)가 들어있다고 가정.
// ApplicationException을 던질 때 ExceptionResponse가 직렬화되며 code 필드가 포함됨.
function classify(res) {
    if (res.status === 200) {
        successCount.add(1);
        return;
    }

    let code = '';
    try {
        const body = res.json();
        code = (body && body.code) || '';
    } catch (_e) {
        code = res.body ? String(res.body).slice(0, 200) : '';
    }

    // 우선순위 순서:
    // 1) ALREADY_ENROLLED (SLE003) — 4차 UK 충돌
    // 2) CAPACITY_EXCEEDED          — 1차 Redis 거절
    // 3) UNKNOWN_EXCEPTION          — 2차 락 획득 실패
    // 4) SPECIAL_LECTURE_ENROLLMENT_FAILED (SLE001) — 3차 낙관적 락 또는 기타
    // 5) 그 외                       — 미분류
    if (code === 'SLE003') {
        alreadyEnrolledCount.add(1);
    } else if (code.startsWith('LE') || /CAPACITY/.test(code) || /CAPACITY/.test(res.body || '')) {
        // LectureExceptionCode.CAPACITY_EXCEEDED는 LE 시리즈 (정확한 코드는 프로젝트의 ExceptionCode enum 참조)
        capacityExceededCount.add(1);
    } else if (/UNKNOWN_EXCEPTION/.test(res.body || '')) {
        lockFailedCount.add(1);
    } else if (code === 'SLE001') {
        // 낙관적 락도 SLE001로 매핑되므로, 더 정확히 분리하려면 별도 코드 신설 필요(향후 개선)
        optimisticFailedCount.add(1);
    } else {
        unknownFailedCount.add(1);
    }
}
