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
@Table(name = "tb_marketplace_access_token",
        indexes = {
                @Index(name = "idx_marketplace_access_token_jti", columnList = "jti", unique = true),
                @Index(name = "idx_marketplace_access_token_status", columnList = "status"),
                @Index(name = "idx_marketplace_access_token_client", columnList = "client_app_id,status")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class MarketplaceAccessTokenRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jti", nullable = false, unique = true, length = 120)
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_app_id", nullable = false)
    private MarketplaceClientApp clientApp;

    @Column(name = "scope", nullable = false, length = 500)
    private String scope;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_introspection_at")
    private Instant lastIntrospectionAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

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
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
    }
}
