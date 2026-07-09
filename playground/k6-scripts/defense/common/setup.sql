-- 4단계 방어 구조 부하 테스트용 데이터 셋업.
--
-- 사전 조건: course_db 스키마가 떠 있고, JPA가 이미 테이블을 생성했어야 한다.
-- 실행: mysql -h 127.0.0.1 -P 3306 -uroot -p1234 course_db < setup.sql

-- 0) 깨끗한 상태로 리셋
DELETE FROM special_lecture_enrollment;
DELETE FROM lecture_enrollment;
DELETE FROM pending_lecture_enrollment;
DELETE FROM consultation_attendee;
DELETE FROM special_lecture;
DELETE FROM consultation;
DELETE FROM lecture;
DELETE FROM schedule;     -- @Inheritance(JOINED)이라 자식 row 삭제 후 부모 삭제
DELETE FROM student;
DELETE FROM tutor;
DELETE FROM parent;
DELETE FROM member;

-- 1) 튜터 1명
INSERT INTO member (member_id, username, password, name, age, phone_number, role, created_date, updated_date)
VALUES (1, 'tutor1', 'Password1!', '튜터1', 30, '01000000001', 'TUTOR', NOW(), NOW());
INSERT INTO tutor (member_id, career_period) VALUES (1, 5);

-- 2) 학생 10,000명 — 한 줄짜리 generator (CTE/recursive)
-- MySQL 8.x recursive CTE 사용
INSERT INTO member (member_id, username, password, name, age, phone_number, role, created_date, updated_date)
WITH RECURSIVE seq AS (
    SELECT 2 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 10001
)
SELECT n,
       CONCAT('student', n - 1),
       'Password1!',
       CONCAT('학생', n - 1),
       20,
       CONCAT('010', LPAD(n - 1, 8, '0')),
       'STUDENT',
       NOW(),
       NOW()
FROM seq;

INSERT INTO student (member_id, school)
SELECT member_id, '테스트학교' FROM member WHERE role = 'STUDENT';

-- 3) 특강 1개 (capacity = 100)
-- Schedule(JOINED) 부모 row 먼저, SpecialLecture 자식 row 다음
INSERT INTO schedule (schedule_id, type, title, start_time, end_time, memo, version, created_date, updated_date)
VALUES (1, 'SPECIAL_LECTURE', '동시성 테스트 특강', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), '4단계 방어 테스트', 0, NOW(), NOW());

INSERT INTO special_lecture (schedule_id, tutor_id, capacity)
VALUES (1, 1, 100);

SELECT '== 셋업 완료 ==' AS status,
       (SELECT COUNT(*) FROM student) AS students,
       (SELECT capacity FROM special_lecture WHERE schedule_id = 1) AS lecture_capacity;
