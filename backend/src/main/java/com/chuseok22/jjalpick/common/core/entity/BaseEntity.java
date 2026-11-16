package com.chuseok22.jjalpick.common.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모든 엔티티의 공통 부모. PK는 시간순 정렬이 되는 UUID v7이고, 생성/수정 시각은 주입된 Clock 기준으로 채운다.
 * 삭제는 soft delete이며 조회 시 {@code deleted = false} 조건은 리포지토리 메서드에서 명시한다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    private Instant deletedAt;

    /**
     * soft delete 처리한다. 이미 삭제된 엔티티는 최초 삭제 시각을 유지한다(멱등).
     */
    public void delete(Clock clock) {
        if (deleted) {
            return;
        }
        this.deleted = true;
        this.deletedAt = clock.instant();
    }

}
