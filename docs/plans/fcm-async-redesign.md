# FCM 비동기 모델 재설계 — 방향 위반 5건 상환

## Context

`followup-debt-paydown.md`에서 분리했다. **이 계획의 유일한 Task는 프로덕션 동작을 바꾸고, 실패 모드가 조용하다** — 잘못 고치면 `FcmFailureRecorder`가 실패를 기록하지 않게 되는데 아무것도 빨간불이 되지 않는다. 부채 상환 묶음에 섞으면 FCM 사고가 났을 때 bisect가 여섯 커밋짜리 브랜치에 떨어진다.

**서두를 이유는 없다.** 동결된 기준선 5가 이미 제 일을 하고 있다 — 새 위반은 차단되고 부채는 근거와 함께 보인다.

## 착수 전에 알아야 할 실측 (2026-08-14 확인)

`retryFcmNotification`의 **호출자는 하나뿐이고 즉시 블로킹한다**:

```
notification/src/main/java/com/example/simplescheduleapp/notification/application/NotificationRetryService.java:26
    fcmService.retryFcmNotification(request.toFcmSendRequest()).get();
```

`.get()` 직후 성공이면 `failedNotificationRepository.delete()`, `InterruptedException`이면 인터럽트 복원 + 재시도 카운트 증가, 그 외 예외면 카운트 증가다.

→ **재시도 경로는 동기 포트로 안전하게 풀린다.** `ApiFuture`를 반환할 이유가 없다. 이 절반은 작고 안전하다.

→ **`sendFcmNotification` 경로는 다르다.** `ApiFutures.addCallback(future, callback, MoreExecutors.directExecutor())`는 **완료한 스레드에서** 콜백을 돌린다. 포트로 뒤집으면 실패를 보고하는 스레드가 바뀐다. 게다가 `NotificationIntegrationTest`에는 이 영역의 async static-mock 취약성 때문에 `@Disabled`된 케이스가 이미 있어 **회귀 그물이 다른 곳보다 얇다.**

**권고 순서:** 재시도 경로(동기 포트)를 먼저 별도 커밋으로 내고, 발송 콜백 경로는 그 다음에 손댄다. 두 경로를 한 커밋에 섞지 마라.

## 작업 내용

**목표:** `notification`의 마지막 `application → infrastructure` 위반 5건을 없앤다.

**현재 (결함):**
```java
// FcmService (application)
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.MoreExecutors;
import com.example.simplescheduleapp.fcm.infrastructure.FcmApiFutureCallback;
import com.example.simplescheduleapp.fcm.infrastructure.FcmMessageSender;

ApiFuture<String> future = fcmMessageSender.sendFcmNotificationAsync(fcmToken, title, body);
FcmApiFutureCallback callback = new FcmApiFutureCallback(event, fcmToken.getFcmToken(), fcmFailureRecorder);
ApiFutures.addCallback(future, callback, MoreExecutors.directExecutor());
```

**왜 단순 포트로 안 되는가:** `sendFcmNotificationAsync()`가 `ApiFuture<String>`을 반환한다. Google SDK 타입이다. 포트를 씌워도 **시그니처에 벤더 SDK가 그대로 노출**돼 규칙만 초록이 되는 가짜 포트가 된다. `FcmService`는 이미 `ApiFuture`·`ApiFutures`·`MoreExecutors`를 직접 import한다.

**하는 일:**

1. `fcm/application/port/out/`에 벤더 중립 발송 포트를 정의한다. 콜백을 **뒤집어** SDK 타입이 application에 닿지 않게 한다. 예: 발송 결과를 application이 정의한 콜백 인터페이스로 받는다.
2. `FcmMessageSender`(infrastructure)가 그 포트를 구현하고, `ApiFuture`·`ApiFutures`·`MoreExecutors`·`FcmApiFutureCallback` 사용을 **전부 infrastructure 안에 가둔다**.
3. `retryFcmNotification`이 `ApiFuture<String>`을 호출자에게 반환하는 것도 정리 대상이다. **먼저 호출자를 찾아 그 Future로 무엇을 하는지 확인하라** — 블로킹 대기라면 포트가 동기 결과를 주는 편이 정직하다. 호출자의 동작을 바꾸지 말고 모양만 바꿔라.

**⚠ 이 Task는 동작 보존이 가장 어렵다.** 콜백 등록 시점, 실패 보고 경로(`FcmFailureRecorder`), 재시도 경로가 그대로여야 한다. 조금이라도 확신이 안 서면 **BLOCKED로 보고**하라 — 잘못 고치면 알림 실패가 조용히 기록되지 않는다.

**완료 조건:**
- `notification` freeze `application → infrastructure` 기준선이 **0**. 0이 아니면 남은 항목 전문을 보고
- `fcm/application/**`에서 `com.google..` import가 0
- 4모듈 빌드 green
- **Kafka DTO 없이, 그리고 Google SDK 타입 없이** FCM 발송 성공/실패 경로를 검증하는 테스트가 있을 것
- 반증: 실패 보고 경로를 끊었을 때 테스트가 빨간불이 되는지 확인 후 원복

---

