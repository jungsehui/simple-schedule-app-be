package com.example.simplescheduleapp.member.infrastructure.persistence;

import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import com.example.simplescheduleapp.parent.domain.Parent;
import com.example.simplescheduleapp.parent.domain.ParentRepository;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 회원 JOINED 상속 계층의 다형 조회 검증 (ADR-0004).
 *
 * <p>{@code MemberRepository}는 {@code Optional<Member>}를 반환하지만 실제 값은 항상 구체
 * 서브타입(Student/Tutor/Parent)이다. Hibernate가 다형 조회로 구체 서브타입 엔티티를 주고
 * {@code MemberMapper}가 이를 대응하는 도메인 서브타입으로 디스패치해야 {@code getRole()}이
 * 정확해진다 — 로그인이 역할 판별에 이를 사용하므로 이 경로가 핵심이다.
 *
 * <p>또한 비밀번호가 엔티티에서 {@code String} 컬럼으로 보관됐다가 {@code Password} VO로
 * 복원되어도 로그인(해시 비교)이 성립하는지 함께 검증한다.
 */
// 각 테스트를 트랜잭션으로 격리해 롤백(ApplicationTest는 @Transactional이 없고 ddl-auto:create라
// 테스트 간 member.username 유니크 제약이 남는 문제 방지 — LectureServiceTest와 동일한 이유)
@Transactional
class MemberRepositoryAdapterTest extends ApplicationTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    TutorRepository tutorRepository;

    @Autowired
    ParentRepository parentRepository;

    @PersistenceContext
    EntityManager entityManager;

    /**
     * 영속성 컨텍스트를 비워 이후 조회가 실제 DB 로드가 되게 한다.
     *
     * <p>이게 없으면 방금 저장한 구체 엔티티 인스턴스가 1차 캐시에서 그대로 반환되어,
     * Hibernate가 discriminator("role")로 구체 서브타입을 해석하는 경로(로그인 시의 실제 경로)를
     * 검증하지 못한 채 테스트가 통과한다.
     */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 학생으로_저장한_회원은_다형_조회에서_Student로_복원된다() {
        // given
        Student saved = studentRepository.save(
                new Student("student1", "Password1!", "학생", 20, "01011112222", "테스트고등학교"));

        flushAndClear();

        // when
        Member found = memberRepository.findByUsername("student1").orElseThrow();

        // then
        assertThat(found).isInstanceOf(Student.class);
        assertThat(found.getRole()).isEqualTo(Role.STUDENT);
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(((Student) found).getSchool()).isEqualTo("테스트고등학교");
    }

    @Test
    void 강사로_저장한_회원은_다형_조회에서_Tutor로_복원된다() {
        // given
        Tutor saved = tutorRepository.save(
                new Tutor("tutor1", "Password1!", "강사", 30, "01033334444", 5));

        flushAndClear();

        // when
        Member found = memberRepository.findByUsername("tutor1").orElseThrow();

        // then
        assertThat(found).isInstanceOf(Tutor.class);
        assertThat(found.getRole()).isEqualTo(Role.TUTOR);
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(((Tutor) found).getCareerPeriod()).isEqualTo(5);
    }

    @Test
    void 학부모로_저장한_회원은_다형_조회에서_Parent로_복원된다() {
        // given
        Parent saved = parentRepository.save(
                new Parent("parent1", "Password1!", "학부모", 45, "01055556666", 2));

        flushAndClear();

        // when
        Member found = memberRepository.findByUsername("parent1").orElseThrow();

        // then
        assertThat(found).isInstanceOf(Parent.class);
        assertThat(found.getRole()).isEqualTo(Role.PARENT);
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(((Parent) found).getChildrenNumber()).isEqualTo(2);
    }

    @Test
    void ID로_조회해도_구체_서브타입으로_복원된다() {
        // given
        Tutor saved = tutorRepository.save(
                new Tutor("tutor2", "Password1!", "강사", 30, "01077778888", 3));

        flushAndClear();

        // when
        Member found = memberRepository.getById(saved.getId());

        // then
        assertThat(found).isInstanceOf(Tutor.class);
        assertThat(found.getRole()).isEqualTo(Role.TUTOR);
    }

    @Test
    void 복원된_회원은_원래_평문_비밀번호로_로그인할_수_있다() {
        // given: 저장 시 해싱된 비밀번호가 password 컬럼에 문자열로 보관된다
        studentRepository.save(
                new Student("student2", "Password1!", "학생", 20, "01099990000", "테스트고등학교"));

        flushAndClear();

        // when: 조회하면 매퍼가 해시 문자열을 Password VO로 복원한다
        Member found = memberRepository.findByUsername("student2").orElseThrow();

        // then: 해시 비교가 성립해 로그인이 통과한다
        assertThatCode(() -> found.login("Password1!")).doesNotThrowAnyException();
    }

    @Test
    void 서브타입_포트로_조회해도_구체_도메인으로_복원된다() {
        // given
        Student saved = studentRepository.save(
                new Student("student3", "Password1!", "학생", 21, "01012121212", "다른고등학교"));

        flushAndClear();

        // when
        Student found = studentRepository.getById(saved.getId());

        // then
        assertThat(found.getSchool()).isEqualTo("다른고등학교");
        assertThat(found.getRole()).isEqualTo(Role.STUDENT);
        assertThat(found.getUsername()).isEqualTo("student3");
    }
}
