package com.tcc.pjb.backend.model.entity.judicial;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_federated_integrity_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederatedIntegritySnapshot {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "scope_type", nullable = false, length = 40)
    private String scopeType;

    @Column(name = "scope_value", length = 80)
    private String scopeValue;

    @Column(name = "source_kind", nullable = false, length = 40)
    private String sourceKind;

    @Column(name = "horizon_start", nullable = false)
    private Instant horizonStart;

    @Column(name = "horizon_end", nullable = false)
    private Instant horizonEnd;

    @Column(name = "leaf_count", nullable = false)
    private int leafCount;

    @Column(name = "root_hash", nullable = false, length = 64)
    private String rootHash;

    @Column(name = "previous_root_hash", length = 64)
    private String previousRootHash;

    @Column(name = "drift_status", nullable = false, length = 40)
    private String driftStatus;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
