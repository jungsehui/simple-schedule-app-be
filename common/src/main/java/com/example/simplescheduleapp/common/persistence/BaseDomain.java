package com.example.simplescheduleapp.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@NoArgsConstructor(access = PROTECTED)
@Getter
public abstract class BaseDomain {

    /**
     * 생성 시각 — UPDATE 대상에서 제외한다.
     *
     * <p>도메인 순수화(ADR-0004) 이후 리포지토리 어댑터의 {@code save()}는 매퍼로 새 엔티티
     * 인스턴스를 만들어 넘긴다. 순수 도메인은 감사 필드를 들고 있지 않으므로 그 인스턴스의
     * {@code createdDate}는 null이고, id가 있으면 Spring Data가 {@code merge()}를 호출해
     * 그 null을 DB에 그대로 쓴다. {@code updatable = false}가 이 컬럼을 UPDATE 문에서 빼서 막는다.
     * ({@code updatedDate}는 @PreUpdate가 매번 다시 채우고, {@code deletedDate}는 @SQLDelete의
     * 별도 UPDATE로만 바뀌므로 같은 처리가 필요 없다.)
     *
     * <p>OutboxAuditingPreservationTest가 이를 가드한다 — 이 값이 NULL이 되면
     * {@code findByStatusAndCreatedDateBefore} 류의 질의가 해당 행을 영영 찾지 못한다.
     */
    @Column(updatable = false)
    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;
}
