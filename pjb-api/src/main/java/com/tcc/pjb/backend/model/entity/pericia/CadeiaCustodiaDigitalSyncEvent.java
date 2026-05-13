package com.tcc.pjb.backend.model.entity.pericia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import com.tcc.pjb.backend.model.entity.enums.CadeiaCustodiaSyncDirection;
import com.tcc.pjb.backend.model.entity.enums.CadeiaCustodiaSyncOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.DOCUMENTOS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_cadeia_custodia_digital_sync_event", indexes = {
        @Index(name = "idx_custodia_sync_event_chave_data", columnList = "chave_custodia, occurred_at"),
        @Index(name = "idx_custodia_sync_event_nonce", columnList = "request_nonce"),
        @Index(name = "idx_custodia_sync_event_payload", columnList = "payload_digest_sha256"),
        @Index(name = "idx_custodia_sync_event_request", columnList = "request_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CadeiaCustodiaDigitalSyncEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_custodia", nullable = false, length = 32)
    private String chaveCustodia;

    @Column(name = "digest_colecao_sha256", nullable = false, length = 64)
    private String digestColecaoSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "direcao", nullable = false, length = 20)
    private CadeiaCustodiaSyncDirection direcao;

    @Enumerated(EnumType.STRING)
    @Column(name = "operacao", nullable = false, length = 20)
    private CadeiaCustodiaSyncOperation operacao;

    @Column(name = "parceiro_institucional", nullable = false, length = 120)
    private String parceiroInstitucional;

    @Column(name = "no_origem", nullable = false, length = 120)
    private String noOrigem;

    @Column(name = "request_nonce", nullable = false, length = 80)
    private String requestNonce;

    @Column(name = "payload_digest_sha256", nullable = false, length = 64)
    private String payloadDigestSha256;

    @Column(name = "assinatura_hmac_sha256", nullable = false, length = 64)
    private String assinaturaHmacSha256;

    @Column(name = "integridade_ok", nullable = false)
    private boolean integridadeOk;

    @Column(name = "assinatura_ok", nullable = false)
    private boolean assinaturaOk;

    @Column(name = "correspondencia_local_ok", nullable = false)
    private boolean correspondenciaLocalOk;

    @Column(name = "total_entradas", nullable = false)
    private int totalEntradas;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_perfil", nullable = false, length = 80)
    private String actorPerfil;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "ip", length = 80)
    private String ip;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        occurredAt = occurredAt == null ? createdAt : occurredAt;
    }
}
