// 시나리오 B: 1차 + 2차(Redisson FairLock) + 4차
//
// 서버 띄울 때 환경변수:
//   DEFENSE_DISTRIBUTED_LOCK=true
//   DEFENSE_OPTIMISTIC_LOCK_HANDLING=false
//
// 기대 결과:
//   - TPS: A보다 낮음 (락 직렬화)
//   - 정합성: 100% 보장 ← 분산 락이 critical section 보호
//   - 분류: 1차에서 9,900명 거절, 2차 통과한 ~100명만 DB에 도달 (락 획득 시간 ~수십ms)
//
// 의의: 2차 락이 1차 위에 얹혀서 정합성 갭을 막는다.

import { runEnrollmentScenario, scenarioOptions } from './common/enrollment.js';

export const options = scenarioOptions('B');

export default function () {
    runEnrollmentScenario('B');
}
