# 대기 수강신청 목록 조회 — 승인·거절의 막다른 골목 해소

## Context

`followup-debt-paydown.md`에서 분리했다. 저쪽은 프로덕션 동작 변경이 0인 부채 상환이고, 이건 **새 엔드포인트 + 계약 변경**이라 성격이 다르다. 웹 세션이 이 화면을 기다리고 있으므로 리팩터 커밋 여섯 개에 묶이지 않고 독립 PR로 나가야 한다.

**완료 후 할 일:** `orchestration/API-CONTRACT.md` 갱신 + 웹 세션에 통지. 내가 두 세션에 "갭 3종 전부 구현됨"이라고 이미 알렸으므로, 이건 그들이 모르는 **네 번째** 엔드포인트다.

## 작업 내용

**목표:** 강사가 승인·거절할 대상을 API로 식별할 수 있게 한다.

**실측한 결함** (웹 세션이 `StudentInfoResponse`에 `pendingId`가 없다고 제보했고, 파 보니 더 컸다):

- `POST /enrollments/accept`·`/reject`는 `PendingAcceptRequest { Long pendingId }`를 받는다
- 그런데 **`pendingId`를 돌려주는 엔드포인트가 하나도 없다**
- `GET /lectures/{lectureId}/enrollments`는 **확정** 수강(`LectureEnrollment`)을 돌려주고, 항목은 `StudentInfoResponse { name, phoneNumber }`로 **식별자가 전혀 없다**
- `PendingLectureEnrollmentRepository`에 목록 조회 메서드 자체가 없다(`findById`, `findByLectureIdAndStudentId`뿐)

**결론: 승인·거절 API는 현재 실질적으로 호출 불가능하다.** DTO에 필드를 하나 더하는 문제가 아니라 조회 경로가 없는 문제다.

**하는 일:**

1. `PendingLectureEnrollmentRepository`에 `List<PendingLectureEnrollment> findAllByLectureId(Long lectureId)`를 추가한다. **`Optional`로 감싸지 마라** — "대기 중인 신청이 없다"는 정상 상태다(같은 파일의 `findAllByLectureId`가 예외로 번역하는 옛 계약을 따라가지 마라).
2. 어댑터와 Spring Data 쿼리를 구현한다.
3. `GET /lectures/{lectureId}/pending-enrollments`를 낸다. `@RequireRole(Role.TUTOR)`이고 **소유권(자기 강의인가)을 반드시 검증**하라 — 기존 `getLectureEnrollmentDetail`이 어떻게 하는지 보고 같은 방식을 쓴다.
4. 응답 항목에 **`pendingId`와 학생 식별 정보**를 함께 싣는다. `pendingId`가 없으면 이 엔드포인트의 존재 이유가 없다.

**판단할 것:** 확정 명단(`StudentInfoResponse`)에도 `studentId`를 추가할지. 학생을 특정해 후속 동작(예: 개별 취소)을 하려면 필요하지만, 지금 그런 동작이 없다면 추가하지 마라(YAGNI). 판단 근거를 보고하라. 추가한다면 **필드 추가이므로 기존 클라이언트에 하위호환**이다.

**완료 조건:**
- 강사가 `GET /lectures/{lectureId}/pending-enrollments` → `pendingId` 획득 → `POST /enrollments/accept`가 **끊김 없이 이어지는** 테스트
- 남의 강의를 조회하면 거부되는 테스트 (소유권)
- 대기 신청이 없으면 빈 목록 + 200
- 4모듈 빌드 green
- 반증: 소유권 검증을 제거하면 테스트가 빨간불이 되는지 확인 후 원복

---

