package com.tcc.pjb.backend.model.entity.api;

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

@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_marketplace_integration_plan",
        indexes = {
                @Index(name = "idx_marketplace_plan_code", columnList = "code", unique = true),
                @Index(name = "idx_marketplace_plan_status", columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class MarketplaceIntegrationPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "max_protocolos_dia")
    private Integer maxProtocolosDia;

    @Column(name = "max_webhook_endpoints")
    private Integer maxWebhookEndpoints;

    @Column(name = "allow_streaming", nullable = false)
    private boolean allowStreaming;

    @Column(name = "allow_high_volume", nullable = false)
    private boolean allowHighVolume;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "ATIVO";
        }
        if (maxProtocolosDia == null || maxProtocolosDia <= 0) {
            maxProtocolosDia = 500;
        }
        if (maxWebhookEndpoints == null || maxWebhookEndpoints <= 0) {
            maxWebhookEndpoints = 3;
        }
    }
}
