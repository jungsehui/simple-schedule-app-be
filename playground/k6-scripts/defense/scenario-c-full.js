// 시나리오 C: 전체 4단계 방어 (1차 + 2차 + 3차 + 4차)
//
// 서버 띄울 때 환경변수:
//   DEFENSE_DISTRIBUTED_LOCK=true
//   DEFENSE_OPTIMISTIC_LOCK_HANDLING=true
//
// 기대 결과:
//   - TPS: B와 거의 동일 (정상 흐름에서 3차는 발동하지 않음 — 비용 0에 가까움)
//   - 정합성: 100% 보장
//   - 분류: B와 거의 동일하지만, 분산 락 leaseTime 만료/네트워크 파티션 같은
//          비정상 상황에서만 enroll_optimistic_failed가 증가한다.
//
// 의의:
//   B vs C의 TPS 차이가 무시할 만큼 작다는 것을 데이터로 증명하면,
//   "낙관적 락은 비싸지 않다, 정상 시 비용 0이고 비정상 시에만 발동한다"는 본질을 수치로 보여줄 수 있다.
//   이게 블로그의 핵심 인사이트.

import { runEnrollmentScenario, scenarioOptions } from './common/enrollment.js';

export const options = scenarioOptions('C');

export default function () {
    runEnrollmentScenario('C');
}
