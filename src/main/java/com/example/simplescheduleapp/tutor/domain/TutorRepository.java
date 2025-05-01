package com.example.simplescheduleapp.tutor.domain.repository;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.tutor.domain.entity.Tutor;
import com.example.simplescheduleapp.tutor.exception.TutorExceptionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    Optional<Tutor> findById(Long id);

    default Tutor getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(TutorExceptionCode.TUTOR_NOT_FOUND));
    }
}
