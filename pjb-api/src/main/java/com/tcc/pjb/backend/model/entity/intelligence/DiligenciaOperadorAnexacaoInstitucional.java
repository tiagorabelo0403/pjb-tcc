package com.tcc.pjb.backend.model.entity.intelligence;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
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
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_diligencia_operador_anexacao_institucional", indexes = {
        @Index(name = "idx_diligencia_anexacao_user_ref", columnList = "operator_user_id, canal, diligence_reference, created_at"),
        @Index(name = "idx_diligencia_anexacao_processo", columnList = "processo_id, created_at"),
        @Index(name = "idx_diligencia_anexacao_juntada", columnList = "juntada_id"),
        @Index(name = "idx_diligencia_anexacao_chain_key", columnList = "operator_user_id, canal, diligence_reference, chain_idempotency_key", unique = true),
        @Index(name = "idx_diligencia_anexacao_ack", columnList = "external_system_code, ack_protocol")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiligenciaOperadorAnexacaoInstitucional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_user_id", nullable = false)
    private Long operatorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator_tipo_usuario", nullable = false, length = 80)
    private TipoUsuario operatorTipoUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 40)
    private TelemetriaOperacionalCanal canal;

    @Column(name = "diligence_reference", nullable = false, length = 120)
    private String diligenceReference;

    @Column(name = "work_item_id")
    private Long workItemId;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "processo_numero", length = 32)
    private String processoNumero;

    @Column(name = "juntada_id", nullable = false)
    private Long juntadaId;

    @Column(name = "formalizacao_id")
    private Long formalizacaoId;

    @Column(name = "encerramento_id")
    private Long encerramentoId;

    @Column(name = "certidao_id")
    private Long certidaoId;

    @Column(name = "pacote_documento_id")
    private UUID pacoteDocumentoId;

    @Column(name = "bundle_reference", length = 160)
    private String bundleReference;

    @Column(name = "bundle_digest_sha256", nullable = false, length = 64)
    private String bundleDigestSha256;

    @Column(name = "bundle_signature_hmac_sha256", nullable = false, length = 64)
    private String bundleSignatureHmacSha256;

    @Column(name = "external_system_code", nullable = false, length = 40)
    private String externalSystemCode;

    @Column(name = "destination_box", nullable = false, length = 160)
    private String destinationBox;

    @Column(name = "ack_protocol", nullable = false, length = 120)
    private String ackProtocol;

    @Column(name = "ack_reference", nullable = false, length = 160)
    private String ackReference;

    @Column(name = "annexation_status", nullable = false, length = 40)
    private String annexationStatus;

    @Column(name = "external_receipt_digest_sha256", nullable = false, length = 64)
    private String externalReceiptDigestSha256;

    @Column(name = "chain_idempotency_key", nullable = false, length = 64)
    private String chainIdempotencyKey;

    @Column(name = "process_event_seq")
    private Long processEventSeq;

    @Column(name = "request_hash_sha256", nullable = false, length = 64)
    private String requestHashSha256;

    @Column(name = "execution_digest_sha256", nullable = false, length = 64)
    private String executionDigestSha256;

    @Column(name = "observacoes", length = 3000)
    private String observacoes;

    @Column(name = "externalized_at")
    private OffsetDateTime externalizedAt;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        bundleReference = normalize(bundleReference, 160);
        externalSystemCode = normalize(externalSystemCode, 40);
        destinationBox = normalize(destinationBox, 160);
        ackProtocol = normalize(ackProtocol, 120);
        ackReference = normalize(ackReference, 160);
        annexationStatus = normalize(annexationStatus, 40);
    }

    private static String normalize(String value,
                                    int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
