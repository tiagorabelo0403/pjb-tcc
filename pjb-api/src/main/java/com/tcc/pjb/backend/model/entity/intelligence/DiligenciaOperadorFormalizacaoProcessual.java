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
@Table(name = "tb_diligencia_operador_formalizacao_processual", indexes = {
        @Index(name = "idx_diligencia_formalizacao_user_ref", columnList = "operator_user_id, canal, diligence_reference, created_at"),
        @Index(name = "idx_diligencia_formalizacao_processo", columnList = "processo_id, created_at"),
        @Index(name = "idx_diligencia_formalizacao_certidao", columnList = "certidao_id"),
        @Index(name = "idx_diligencia_formalizacao_idempotencia", columnList = "operator_user_id, canal, diligence_reference, idempotency_key", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiligenciaOperadorFormalizacaoProcessual {

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

    @Column(name = "encerramento_id")
    private Long encerramentoId;

    @Column(name = "certidao_id", nullable = false)
    private Long certidaoId;

    @Column(name = "checkpoint_event_id")
    private Long checkpointEventId;

    @Column(name = "movimentacao_id")
    private Long movimentacaoId;

    @Column(name = "movimentacao_event_seq")
    private Long movimentacaoEventSeq;

    @Column(name = "minuta_documento_id")
    private UUID minutaDocumentoId;

    @Column(name = "minuta_event_seq")
    private Long minutaEventSeq;

    @Column(name = "minuta_titulo", length = 255)
    private String minutaTitulo;

    @Column(name = "minuta_sha256", length = 64)
    private String minutaSha256;

    @Column(name = "minuta_sha384", length = 96)
    private String minutaSha384;

    @Column(name = "certidao_digest_sha256", length = 64)
    private String certidaoDigestSha256;

    @Column(name = "evidence_chave_custodia", length = 32)
    private String evidenceChaveCustodia;

    @Column(name = "evidence_integrity_ok")
    private Boolean evidenceIntegrityOk;

    @Column(name = "documentos_referenciados")
    private Integer documentosReferenciados;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "formalization_digest_sha256", nullable = false, length = 64)
    private String formalizationDigestSha256;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
