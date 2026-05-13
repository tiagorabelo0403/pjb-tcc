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
@Table(name = "tb_processual_read_model_projection",
        indexes = {
                @Index(name = "idx_prm_projection_domain_key", columnList = "domain,materialization_key", unique = true),
                @Index(name = "idx_prm_projection_scope", columnList = "domain,tribunal_code,ramo_code,scope_key"),
                @Index(name = "idx_prm_projection_status", columnList = "status"),
                @Index(name = "idx_prm_projection_updated", columnList = "updated_at")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ProcessualReadModelProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "domain", nullable = false, length = 120)
    private String domain;

    @Column(name = "materialization_key", nullable = false, length = 240)
    private String materializationKey;

    @Column(name = "scope_key", length = 240)
    private String scopeKey;

    @Column(name = "aggregate_type", length = 120)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 160)
    private String aggregateId;

    @Column(name = "tribunal_code", length = 80)
    private String tribunalCode;

    @Column(name = "ramo_code", length = 80)
    private String ramoCode;

    @Column(name = "projection_version", nullable = false)
    private Long projectionVersion;

    @Column(name = "last_event_type", length = 160)
    private String lastEventType;

    @Column(name = "source", length = 80)
    private String source;

    @Column(name = "payload_hash", length = 96)
    private String payloadHash;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "freshness_at")
    private Instant freshnessAt;

    @Column(name = "last_materialized_at")
    private Instant lastMaterializedAt;

    @Column(name = "last_recomposition_requested_at")
    private Instant lastRecompositionRequestedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (projectionVersion == null || projectionVersion <= 0L) {
            projectionVersion = 1L;
        }
        if (status == null || status.isBlank()) {
            status = "MATERIALIZED";
        }
        if (freshnessAt == null) {
            freshnessAt = Instant.now();
        }
        if (lastMaterializedAt == null) {
            lastMaterializedAt = freshnessAt;
        }
    }
}
