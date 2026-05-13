package com.tcc.pjb.backend.model.entity.pericia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.DOCUMENTOS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_cadeia_custodia_digital", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cadeia_custodia_lote_item", columnNames = {"digest_colecao_sha256", "evidence_id", "digest_sha256"}),
        @UniqueConstraint(name = "uk_cadeia_custodia_entry_hash", columnNames = "entry_hash")
}, indexes = {
        @Index(name = "idx_cadeia_custodia_chave_data", columnList = "chave_custodia, sealed_at"),
        @Index(name = "idx_cadeia_custodia_digest_data", columnList = "digest_colecao_sha256, sealed_at"),
        @Index(name = "idx_cadeia_custodia_actor_data", columnList = "sealed_by_user_id, sealed_at"),
        @Index(name = "idx_cadeia_custodia_request", columnList = "request_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CadeiaCustodiaDigitalLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "digest_colecao_sha256", nullable = false, length = 64)
    private String digestColecaoSha256;

    @Column(name = "chave_custodia", nullable = false, length = 32)
    private String chaveCustodia;

    @Column(name = "lote_referencia", length = 160)
    private String loteReferencia;

    @Column(name = "ordem_lote", nullable = false)
    private int ordemLote;

    @Column(name = "evidence_id", nullable = false, length = 120)
    private String evidenceId;

    @Column(name = "evidence_nome", nullable = false, length = 255)
    private String evidenceNome;

    @Column(name = "digest_sha256", nullable = false, length = 64)
    private String digestSha256;

    @Column(name = "evidence_chave_custodia", nullable = false, length = 32)
    private String evidenceChaveCustodia;

    @Column(name = "metadata_digest_sha256", nullable = false, length = 64)
    private String metadataDigestSha256;

    @Column(name = "metadata_canonica", columnDefinition = "TEXT")
    private String metadataCanonica;

    @Column(name = "sealed_by_user_id")
    private Long sealedByUserId;

    @Column(name = "sealed_by_perfil", nullable = false, length = 80)
    private String sealedByPerfil;

    @Column(name = "sealed_at", nullable = false)
    private Instant sealedAt;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "ip", length = 80)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "prev_hash", length = 64)
    private String prevHash;

    @Column(name = "entry_hash", nullable = false, length = 64)
    private String entryHash;

    @PrePersist
    void prePersist() {
        sealedAt = sealedAt == null ? Instant.now() : sealedAt;
    }
}
