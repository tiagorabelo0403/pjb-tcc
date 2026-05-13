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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_marketplace_webhook_delivery",
        indexes = {
                @Index(name = "idx_marketplace_webhook_delivery_endpoint", columnList = "endpoint_id"),
                @Index(name = "idx_marketplace_webhook_delivery_status", columnList = "status"),
                @Index(name = "idx_marketplace_webhook_delivery_event_type", columnList = "event_type")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class MarketplaceWebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private MarketplaceWebhookEndpoint endpoint;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "attempts")
    private Integer attempts;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "last_dispatch_at")
    private Instant lastDispatchAt;

    @Column(name = "response_excerpt", length = 500)
    private String responseExcerpt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "PENDENTE";
        }
        if (attempts == null || attempts < 0) {
            attempts = 0;
        }
    }
}
