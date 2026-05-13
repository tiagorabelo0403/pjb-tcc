package com.tcc.pjb.backend.model.entity.api;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "tb_marketplace_client_subscription",
        indexes = {
                @Index(name = "idx_marketplace_subscription_client", columnList = "client_app_id"),
                @Index(name = "idx_marketplace_subscription_status", columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class MarketplaceClientSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_app_id", nullable = false)
    private MarketplaceClientApp clientApp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private MarketplaceIntegrationPlan plan;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "protocolos_dia_atual")
    private Integer protocolosDiaAtual;

    @Column(name = "ultimo_reset_contador")
    private Instant ultimoResetContador;

    @Column(name = "webhook_endpoint_limit_override")
    private Integer webhookEndpointLimitOverride;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "ATIVA";
        }
        if (startedAt == null) {
            startedAt = Instant.now();
        }
        if (protocolosDiaAtual == null || protocolosDiaAtual < 0) {
            protocolosDiaAtual = 0;
        }
        if (ultimoResetContador == null) {
            ultimoResetContador = Instant.now();
        }
    }
}
