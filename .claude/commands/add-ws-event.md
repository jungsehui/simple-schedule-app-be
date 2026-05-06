---
description: WebSocket 이벤트 (inbound 또는 outbound) + 도메인 이벤트 + listener 추가
argument-hint: <event-name> <direction:inbound|outbound>
allowed-tools: Read, Write, Edit, Bash, Grep
---

WebSocket 이벤트를 추가한다.

## Inbound (Client → Server)

### 절차
1. **JSON envelope** — `{ "type": "<event-name>", "data": {...} }` 형식 가정.

2. **`ChatWebSocketHandler.kt`의 `handleTextMessage` when 절에 분기 추가**:
   ```kotlin
   "<event-name>" -> handle<EventName>(session, userId, data)
   ```

3. **핸들러 메서드 작성**:
   ```kotlin
   private fun handle<EventName>(session: WebSocketSession, userId: String, data: JsonNode?) {
       val foo = data?.get("foo")?.asText()
           ?: return sendError(session, "INVALID_DATA", "foo is required")
       // 서비스 호출
       service.action(...).fold(
           onLeft = { sendError(session, "<CODE>", it.message) },
           onRight = { /* ack 또는 broadcast */ },
       )
   }
   ```

4. **테스트** — `WebSocketIntegrationTest.kt`에 시나리오 추가.

## Outbound (Server → Room/Client)

### 절차
1. **도메인 이벤트** — `domain/event/ChatEvent.kt` sealed class에 새 case 추가:
   ```kotlin
   data class <EventName>(
       val roomId: String,
       val foo: String,
   ) : ChatEvent()
   ```

2. **서비스에서 이벤트 발행**:
   ```kotlin
   eventPublisher.publishEvent(ChatEvent.<EventName>(roomId, foo))
   ```
   - 일반 비즈니스 액션 후 → `@TransactionalEventListener(AFTER_COMMIT)` 사용
   - 스케줄러/스탠드얼론 → `@EventListener` 사용

3. **`ChatEventListener.kt`에 핸들러 추가**:
   ```kotlin
   @Async
   @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
   fun on<EventName>(event: ChatEvent.<EventName>) {
       broadcaster.broadcastToRoom(
           roomId = event.roomId,
           message = WsOutMessage(
               type = "<event-name>",
               data = mapOf("roomId" to event.roomId, "foo" to event.foo),
           ),
       )
   }
   ```

4. **테스트** — listener 단위 테스트 + 통합 테스트.

## 공통

5. **`docs/WEBSOCKET.md` 업데이트** — payload 예시 + 에러 코드 (있으면).

6. **빌드 검증**:
   ```bash
   ./gradlew test
   ```

## 체크리스트
- [ ] JSON 형식이 일관됨 (`{ type, data }`)
- [ ] 에러 코드는 `WEBSOCKET.md`의 코드 표에 추가
- [ ] 단위/통합 테스트 통과
