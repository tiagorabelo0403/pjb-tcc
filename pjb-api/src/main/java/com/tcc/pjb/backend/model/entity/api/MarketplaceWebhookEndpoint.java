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
@Table(name = "tb_marketplace_webhook_endpoint",
        indexes = {
                @Index(name = "idx_marketplace_webhook_client", columnList = "client_app_id"),
                @Index(name = "idx_marketplace_webhook_status", columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class MarketplaceWebhookEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_app_id", nullable = false)
    private MarketplaceClientApp clientApp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private MarketplaceClientSubscription subscription;

    @Column(name = "callback_url", nullable = false, length = 320)
    private String callbackUrl;

    @Column(name = "event_filter", nullable = false, length = 260)
    private String eventFilter;

    @Column(name = "signing_secret_hash", nullable = false, length = 128)
    private String signingSecretHash;

    @Column(name = "signing_secret_ciphertext", columnDefinition = "TEXT")
    private String signingSecretCiphertext;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "last_error_message", length = 300)
    private String lastErrorMessage;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (eventFilter == null || eventFilter.isBlank()) {
            eventFilter = "PROCESSO_PROTOCOLADO";
        }
        if (status == null || status.isBlank()) {
            status = "ATIVO";
        }
    }
}
