package com.example.simplescheduleapp.parent.infrastructure.persistence;

import com.example.simplescheduleapp.parent.domain.Parent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentJpaRepository extends JpaRepository<Parent, Long> {
}
