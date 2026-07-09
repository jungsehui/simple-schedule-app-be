// 시나리오 A: Baseline (1차 Redis Atomic + 4차 UK 제약만)
//
// 서버 띄울 때 환경변수:
//   DEFENSE_DISTRIBUTED_LOCK=false
//   DEFENSE_OPTIMISTIC_LOCK_HANDLING=false
//
// 기대 결과:
//   - TPS: 가장 높음 (락 없음)
//   - 정합성: 극단적 부하에서 정원 초과/유실 가능 ← K6 후 verify.sh로 확인
//   - 분류: 1차에서 9,900명 거절 (CAPACITY_EXCEEDED), 100명 통과 (SUCCESS)

import { runEnrollmentScenario, scenarioOptions } from './common/enrollment.js';

export const options = scenarioOptions('A');

export default function () {
    runEnrollmentScenario('A');
}
