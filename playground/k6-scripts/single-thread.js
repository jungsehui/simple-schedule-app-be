import http from 'k6/http';
import { check, sleep } from 'k6';

// k6 테스트의 실행 옵션을 정의하는 부분입니다.
export const options = {
    // 'stages'는 시간에 따라 부하를 어떻게 조절할지 정의하는 시나리오입니다.
    stages: [
        // 1. 웜업 및 램프업(Ramp-up) 단계
        // 30초 동안 초당 요청 수를 0에서 2000까지 서서히 늘립니다.
        { duration: '30s', target: 2000 },

        // 2. 본 테스트 (Sustained Load) 단계
        // 초당 2000개의 요청을 3분 20초 동안 꾸준히 유지합니다.
        { duration: '3m20s', target: 2000 },

        // 3. 램프다운(Ramp-down) 단계
        // 10초 동안 초당 요청 수를 0으로 서서히 줄입니다.
        { duration: '10s', target: 0 },
    ],
};

export default function () {
    const res = http.get('http://host.docker.internal:8082/test/async/single-thread');

    // 응답 상태 코드가 200인지 확인합니다.
    check(res, { 'status is 200': (r) => r.status === 200 });

    // 요청 사이에 약간의 대기 시간을 줍니다.
    sleep(1);
}
