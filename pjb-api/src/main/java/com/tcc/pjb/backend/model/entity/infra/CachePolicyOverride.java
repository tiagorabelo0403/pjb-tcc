package com.tcc.pjb.backend.model.entity.infra;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_cache_policy_override",
        indexes = {
                @Index(name = "idx_cache_policy_override_cache_role", columnList = "cache_name,role_name", unique = true),
                @Index(name = "idx_cache_policy_override_enabled", columnList = "enabled")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class CachePolicyOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_name", nullable = false, length = 120)
    private String cacheName;

    @Column(name = "role_name", nullable = false, length = 120)
    private String roleName;

    @Column(name = "ttl_seconds", nullable = false)
    private Integer ttlSeconds;

    @Column(name = "stale_while_revalidate_seconds")
    private Integer staleWhileRevalidateSeconds;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (ttlSeconds == null || ttlSeconds <= 0) {
            ttlSeconds = 60;
        }
        if (staleWhileRevalidateSeconds == null || staleWhileRevalidateSeconds < 0) {
            staleWhileRevalidateSeconds = 0;
        }
    }
}
