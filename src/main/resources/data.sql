-- 회원 추가
INSERT INTO member (username, password, name, age, phone_number)
VALUES ('youmom1', 'encrypted-pass', '유맘', 42, '01012345678');

-- 학부모 역할 추가
INSERT INTO parent (member_id, children_number)
VALUES (1, 2);

-- 자녀로 학생 등록
INSERT INTO member (username, password, name, age, phone_number)
VALUES ('jihoon5', 'pass', '지훈', 11, '01055551111');

INSERT INTO student (member_id, school)
VALUES (2, '서울초등학교');

-- 선생님 등록
INSERT INTO member (username, password, name, age, phone_number)
VALUES ('kimteacher', 'pass', '김쌤', 30, '01099998888');

INSERT INTO tutor (member_id, career_period)
VALUES (3, 5);
