package com.example.simplescheduleapp.special.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.special.exception.SpecialLectureExceptionCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialLectureRepository extends JpaRepository<SpecialLecture, Long> {

    default SpecialLecture getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(SpecialLectureExceptionCode.SPECIAL_LECTURE_NOT_FOUND));
    }

    // 비관적 락
    // default 쓰면 안 됨
    // --> 어노테이션이 안 먹힘
    // 스프링은 구현이 안 되어 있는 껍데기 메서드를 프록시를 통해 구현함
    // 직접 구현하고 싶으면 QueryDSL 써야 할 거 같음
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sl from SpecialLecture sl where sl.id = :id")
    Optional<SpecialLecture> findByIdWithPessimisticLock(@Param("id") Long id);

//    // DB 내에서 update 쿼리로 쓰기 락 걸어 보자는 취지
//    // 영속성 비우기 --> 어떻게 동작하는지는 잘 모르겠음
//    @Modifying(clearAutomatically = true)
//    @Query(
//            value = "UPDATE special_lecture SET enrolled_count = enrolled_count + 1 WHERE schedule_id = :id AND enrolled_count < capacity",
//            nativeQuery = true
//    )
//    int increaseSpecialLectureEnrollmentCount(@Param("id") Long id);

    // JPQL 쓰면 여기서 HT 뭐시기로 SQL 따로 해석해서 쿼리 날림
    @Modifying
    @Query("UPDATE SpecialLecture sl SET sl.enrolledCount = sl.enrolledCount + 1 WHERE sl.id = :id AND sl.enrolledCount < sl.capacity")
    int increaseSpecialLectureEnrollmentCount(@Param("id") Long id);

    // 네임드 락 설정
    @Query(value = "SELECT GET_LOCK(:lockName, :timeoutSeconds)", nativeQuery = true)
    Integer getLock(@Param("lockName") String lockName, @Param("timeoutSeconds") int timeoutSeconds);

    // 네임드 락 해제
    @Query(value = "SELECT RELEASE_LOCK(:lockName)", nativeQuery = true)
    Integer releaseLock(@Param("lockName") String lockName);
}
