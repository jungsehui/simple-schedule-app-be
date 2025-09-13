// k6-scripts/enrollment-test.js
import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        // '1만 명 동시 요청' 시나리오
        spike_test: {
            executor: 'ramping-arrival-rate',

            // 테스트를 위해 가상 유저(VU)를 미리 충분히 할당해 둡니다.
            preAllocatedVUs: 10000,

            // 요청(iteration) 도착률을 제어하는 시간 단위입니다.
            timeUnit: '1s',

            // 테스트 단계 설정
            stages: [
                // 1. 테스트 시작 후 1초 동안 요청 발생률을 10000까지 끌어올립니다.
                //    -> 사실상 1초 안에 1만 개의 요청이 생성됩니다.
                { duration: '1s', target: 10000 },

                // 2. 이후 10초간은 새로운 요청을 보내지 않고,
                //    이미 보낸 요청들이 응답을 완료할 때까지 기다립니다.
                { duration: '10s', target: 0 },
            ],
        },
    },
};

export default function () {
    const specialLectureId = 1;
    const studentId = __VU; // __VU는 1부터 시작하는 고유 ID입니다.
    const url = `http://host.docker.internal:8080/special-lectures/${specialLectureId}/enrollments?studentId=${studentId}`;

    const res = http.post(url);

    check(res, {
        'is status 200 or 4xx': (r) => r.status === 200 || (r.status >= 400 && r.status < 500),
    });
}
