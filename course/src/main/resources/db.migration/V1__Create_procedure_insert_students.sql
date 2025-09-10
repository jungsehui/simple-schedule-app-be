-- 대량의 학생 데이터를 생성하는 스토어드 프로시저를 생성합니다.
CREATE PROCEDURE insert_students()
BEGIN
    DECLARE i INT DEFAULT 1;
    -- 1부터 2000까지 반복하여 학생 데이터를 삽입합니다.
    WHILE i <= 2000 DO
            -- 1. member 테이블에 공통 정보 삽입
            INSERT INTO member (
                member_id, username, password, name, age, phone_number,
                role, created_date, updated_date, deleted_date
            ) VALUES (
                         i,
                         CONCAT('student', i),
                         '$2a$10$...hashed...password...', -- 예시용 해시된 비밀번호입니다. 실제 값으로 교체하세요.
                         CONCAT('학생', i),
                         20,
                         CONCAT('010-0000-', LPAD(i, 4, '0')),
                         'STUDENT', -- DiscriminatorValue
                         NOW(),
                         NOW(),
                         NULL
                     );

            -- 2. student 테이블에 학생 고유 정보 삽입
            INSERT INTO student (
                member_id, school
            ) VALUES (
                         i,
                         '테스트대학교'
                     );
            SET i = i + 1;
        END WHILE;
END

-- 위에서 생성한 스토어드 프로시저를 호출하여 데이터를 삽입합니다.
CALL insert_students();
