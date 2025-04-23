package com.example.simplescheduleappback.parent.domain.repository;

import com.example.simplescheduleappback.parent.domain.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {

    Optional<Parent> findById(Long id);
}
