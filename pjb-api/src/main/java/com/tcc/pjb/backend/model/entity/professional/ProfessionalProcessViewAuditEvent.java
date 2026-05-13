package com.tcc.pjb.backend.model.entity.professional;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_professional_process_view_audit", indexes = {
        @Index(name = "idx_prof_view_audit_user_time", columnList = "usuario_id,acessado_em"),
        @Index(name = "idx_prof_view_audit_proc_time", columnList = "processo_id,acessado_em"),
        @Index(name = "idx_prof_view_audit_actor_time", columnList = "actor_class,acessado_em")
})
public class ProfessionalProcessViewAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "usuario_nome", nullable = false, length = 180)
    private String usuarioNome;

    @Column(name = "oab_ou_matricula", length = 80)
    private String oabOuMatricula;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "numero_processo", nullable = false, length = 80)
    private String numeroProcesso;

    @Column(name = "documento_id", length = 80)
    private String documentoId;

    @Column(name = "actor_class", nullable = false, length = 40)
    private String actorClass;

    @Column(name = "panel_mode", nullable = false, length = 60)
    private String panelMode;

    @Column(name = "access_basis", nullable = false, length = 80)
    private String accessBasis;

    @Column(name = "operation_type", nullable = false, length = 40)
    private String operationType;

    @Column(name = "query_type", length = 40)
    private String queryType;

    @Column(name = "query_value_masked", length = 180)
    private String queryValueMasked;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "step_up_mode", length = 60)
    private String stepUpMode;

    @Column(name = "sucesso", nullable = false)
    private boolean sucesso;

    @Column(name = "client_fingerprint_hash", length = 128)
    private String clientFingerprintHash;

    @Column(name = "acessado_em", nullable = false)
    private LocalDateTime acessadoEm;

    @PrePersist
    void prePersist() {
        if (acessadoEm == null) {
            acessadoEm = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }
    public String getOabOuMatricula() { return oabOuMatricula; }
    public void setOabOuMatricula(String oabOuMatricula) { this.oabOuMatricula = oabOuMatricula; }
    public Long getProcessoId() { return processoId; }
    public void setProcessoId(Long processoId) { this.processoId = processoId; }
    public String getNumeroProcesso() { return numeroProcesso; }
    public void setNumeroProcesso(String numeroProcesso) { this.numeroProcesso = numeroProcesso; }
    public String getDocumentoId() { return documentoId; }
    public void setDocumentoId(String documentoId) { this.documentoId = documentoId; }
    public String getActorClass() { return actorClass; }
    public void setActorClass(String actorClass) { this.actorClass = actorClass; }
    public String getPanelMode() { return panelMode; }
    public void setPanelMode(String panelMode) { this.panelMode = panelMode; }
    public String getAccessBasis() { return accessBasis; }
    public void setAccessBasis(String accessBasis) { this.accessBasis = accessBasis; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
    public String getQueryValueMasked() { return queryValueMasked; }
    public void setQueryValueMasked(String queryValueMasked) { this.queryValueMasked = queryValueMasked; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStepUpMode() { return stepUpMode; }
    public void setStepUpMode(String stepUpMode) { this.stepUpMode = stepUpMode; }
    public boolean isSucesso() { return sucesso; }
    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }
    public String getClientFingerprintHash() { return clientFingerprintHash; }
    public void setClientFingerprintHash(String clientFingerprintHash) { this.clientFingerprintHash = clientFingerprintHash; }
    public LocalDateTime getAcessadoEm() { return acessadoEm; }
    public void setAcessadoEm(LocalDateTime acessadoEm) { this.acessadoEm = acessadoEm; }
}
