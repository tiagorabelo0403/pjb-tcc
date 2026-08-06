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
@Table(name = "tb_marketplace_client_app",
        indexes = {
                @Index(name = "idx_marketplace_client_app_client_id", columnList = "client_id", unique = true),
                @Index(name = "idx_marketplace_client_app_status", columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class MarketplaceClientApp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, unique = true, length = 120)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false, length = 128)
    private String clientSecretHash;

    @Column(name = "display_name", nullable = false, length = 180)
    private String displayName;

    @Column(name = "owner_name", length = 180)
    private String ownerName;

    @Column(name = "owner_email", length = 180)
    private String ownerEmail;

    @Column(name = "allowed_scopes", nullable = false, length = 500)
    private String allowedScopes;

    @Column(name = "allowed_grants", nullable = false, length = 120)
    private String allowedGrants;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "trusted_origin", length = 220)
    private String trustedOrigin;

    @Column(name = "access_token_ttl_seconds")
    private Integer accessTokenTtlSeconds;

    @Column(name = "last_authenticated_at")
    private Instant lastAuthenticatedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (allowedScopes == null || allowedScopes.isBlank()) {
            allowedScopes = "processos:protocolar processos:documentos";
        }
        if (allowedGrants == null || allowedGrants.isBlank()) {
            allowedGrants = "client_credentials";
        }
        if (status == null || status.isBlank()) {
            status = "ATIVO";
        }
        if (accessTokenTtlSeconds == null || accessTokenTtlSeconds <= 0) {
            accessTokenTtlSeconds = 900;
        }
    }
}
