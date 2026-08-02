package com.example.simplescheduleapp.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@NoArgsConstructor(access = PROTECTED)
@Getter
public abstract class SoftDeletedDomain extends BaseDomain {

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;
}
