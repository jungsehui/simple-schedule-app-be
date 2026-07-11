// k6-scripts/enrollment-test.js
import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        // 10000명의 사용자가 동시에 테스트를 시작하도록 설정
        contacts: {
            executor: 'per-vu-iterations',
            vus: 10000,
            iterations: 1,
            maxDuration: '1m', // 최대 1분 간 테스트 진행
        },
    },
};

export default function () {
    const specialLectureId = 1; // 테스트할 특강 ID

    // __VU는 k6가 제공하는 가상 사용자별 고유 ID입니다.
    // 이를 studentId로 사용하여 모든 요청자가 다른 학생인 것처럼 만듭니다.
    const studentId = __VU;

    const url = `http://host.docker.internal:8080/special-lectures/${specialLectureId}/enrollments?studentId=${studentId}`;

    // POST 요청으로 수강 신청 API 호출
    const res = http.post(url);

    // 응답 코드가 200(성공) 또는 4xx(클라이언트 에러, 예: 마감)인지 확인합니다.
    check(res, {
        'is status 200 or 4xx': (r) => r.status === 200 || (r.status >= 400 && r.status < 500),
    });
}
