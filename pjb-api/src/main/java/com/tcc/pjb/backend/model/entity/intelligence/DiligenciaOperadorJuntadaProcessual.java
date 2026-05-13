package com.tcc.pjb.backend.model.entity.intelligence;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
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
@Table(name = "tb_diligencia_operador_juntada_processual", indexes = {
        @Index(name = "idx_diligencia_juntada_user_ref", columnList = "operator_user_id, canal, diligence_reference, created_at"),
        @Index(name = "idx_diligencia_juntada_processo", columnList = "processo_id, created_at"),
        @Index(name = "idx_diligencia_juntada_formalizacao", columnList = "formalizacao_id"),
        @Index(name = "idx_diligencia_juntada_idempotencia", columnList = "operator_user_id, canal, diligence_reference, idempotency_key", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiligenciaOperadorJuntadaProcessual {

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

    @Column(name = "formalizacao_id", nullable = false)
    private Long formalizacaoId;

    @Column(name = "encerramento_id")
    private Long encerramentoId;

    @Column(name = "certidao_id")
    private Long certidaoId;

    @Column(name = "movimentacao_id")
    private Long movimentacaoId;

    @Column(name = "movimentacao_event_seq")
    private Long movimentacaoEventSeq;

    @Column(name = "pacote_documento_id")
    private UUID pacoteDocumentoId;

    @Column(name = "pacote_event_seq")
    private Long pacoteEventSeq;

    @Column(name = "minuta_documento_id")
    private UUID minutaDocumentoId;

    @Column(name = "pacote_titulo", length = 255)
    private String pacoteTitulo;

    @Column(name = "pacote_sha256", length = 64)
    private String pacoteSha256;

    @Column(name = "certidao_digest_sha256", length = 64)
    private String certidaoDigestSha256;

    @Column(name = "formalization_digest_sha256", length = 64)
    private String formalizationDigestSha256;

    @Column(name = "evidence_chave_custodia", length = 32)
    private String evidenceChaveCustodia;

    @Column(name = "evidence_integrity_ok")
    private Boolean evidenceIntegrityOk;

    @Column(name = "documentos_referenciados")
    private Integer documentosReferenciados;

    @Column(name = "exportar_malha_externa")
    private Boolean exportarMalhaExterna;

    @Column(name = "external_system_code", length = 40)
    private String externalSystemCode;

    @Column(name = "bundle_reference", length = 160)
    private String bundleReference;

    @Column(name = "bundle_digest_sha256", nullable = false, length = 64)
    private String bundleDigestSha256;

    @Column(name = "bundle_signature_hmac_sha256", nullable = false, length = 64)
    private String bundleSignatureHmacSha256;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        documentosReferenciados = documentosReferenciados == null || documentosReferenciados < 0 ? 0 : documentosReferenciados;
        exportarMalhaExterna = exportarMalhaExterna != null && exportarMalhaExterna;
        externalSystemCode = normalize(externalSystemCode, 40);
        bundleReference = normalize(bundleReference, 160);
        pacoteTitulo = normalize(pacoteTitulo, 255);
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
