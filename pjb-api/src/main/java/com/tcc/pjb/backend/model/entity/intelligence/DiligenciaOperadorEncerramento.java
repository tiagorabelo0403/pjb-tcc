package com.tcc.pjb.backend.model.entity.intelligence;

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
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_diligencia_operador_encerramento", indexes = {
        @Index(name = "idx_diligencia_encerramento_user_ref", columnList = "operator_user_id, canal, diligence_reference, created_at"),
        @Index(name = "idx_diligencia_encerramento_actor_workitem_time", columnList = "operator_user_id, canal, work_item_id, created_at"),
        @Index(name = "idx_diligencia_encerramento_certidao", columnList = "certidao_id"),
        @Index(name = "idx_diligencia_encerramento_idempotency", columnList = "operator_user_id, canal, diligence_reference, idempotency_key", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiligenciaOperadorEncerramento {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 40)
    private DiligenciaEncerramentoTipo outcome;

    @Column(name = "work_item_id")
    private Long workItemId;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "processo_numero", length = 32)
    private String processoNumero;

    @Column(name = "certidao_id")
    private Long certidaoId;

    @Column(name = "checkpoint_event_id")
    private Long checkpointEventId;

    @Column(name = "certidao_digest_sha256", length = 64)
    private String certidaoDigestSha256;

    @Column(name = "work_item_status_final", length = 30)
    private String workItemStatusFinal;

    @Column(name = "followup_work_item_id")
    private Long followupWorkItemId;

    @Column(name = "documentos_vinculados")
    private Integer documentosVinculados;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "execution_digest_sha256", nullable = false, length = 64)
    private String executionDigestSha256;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        documentosVinculados = documentosVinculados == null || documentosVinculados < 0 ? 0 : documentosVinculados;
    }
}
