package com.example.simplescheduleapp.tutor.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    default Tutor getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(MemberExceptionCode.TUTOR_NOT_FOUND));
    }
}
