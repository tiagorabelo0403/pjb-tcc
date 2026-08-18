package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@FilterDef(name = "filtroUnidadeInstitucional", parameters = @ParamDef(name = "unidadeInstitucionalIdParam", type = Long.class))
@Filter(name = "filtroUnidadeInstitucional", condition = "unidade_institucional_id = :unidadeInstitucionalIdParam")
@Table(name = "secretaria_institucional_item")
public class SecretariaInstitucionalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "unidade_institucional_id")
    private Long unidadeInstitucionalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_instituicao_alvo", nullable = false, length = 60)
    private TipoUnidadeInstitucional tipoInstituicaoAlvo;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, length = 20)
    private MotivoEnfileiramentoInstitucional motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private StatusSecretariaInstitucionalItem status;

    @Column(name = "prazo_base_dias", nullable = false)
    private Integer prazoBaseDias;

    @Column(name = "prazo_em_dobro", nullable = false)
    private boolean prazoEmDobro;

    @Column(name = "intimado_em")
    private Instant intimadoEm;

    @Column(name = "intimacao_tacita_em")
    private Instant intimacaoTacitaEm;

    @Column(name = "prazo_fatal")
    private Instant prazoFatal;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public Long getId() { return id; }
    public Long getProcessoId() { return processoId; }
    public void setProcessoId(Long processoId) { this.processoId = processoId; }
    public Long getUnidadeInstitucionalId() { return unidadeInstitucionalId; }
    public void setUnidadeInstitucionalId(Long unidadeInstitucionalId) { this.unidadeInstitucionalId = unidadeInstitucionalId; }
    public TipoUnidadeInstitucional getTipoInstituicaoAlvo() { return tipoInstituicaoAlvo; }
    public void setTipoInstituicaoAlvo(TipoUnidadeInstitucional tipoInstituicaoAlvo) { this.tipoInstituicaoAlvo = tipoInstituicaoAlvo; }
    public MotivoEnfileiramentoInstitucional getMotivo() { return motivo; }
    public void setMotivo(MotivoEnfileiramentoInstitucional motivo) { this.motivo = motivo; }
    public StatusSecretariaInstitucionalItem getStatus() { return status; }
    public void setStatus(StatusSecretariaInstitucionalItem status) { this.status = status; }
    public Integer getPrazoBaseDias() { return prazoBaseDias; }
    public void setPrazoBaseDias(Integer prazoBaseDias) { this.prazoBaseDias = prazoBaseDias; }
    public boolean isPrazoEmDobro() { return prazoEmDobro; }
    public void setPrazoEmDobro(boolean prazoEmDobro) { this.prazoEmDobro = prazoEmDobro; }
    public Instant getIntimadoEm() { return intimadoEm; }
    public void setIntimadoEm(Instant intimadoEm) { this.intimadoEm = intimadoEm; }
    public Instant getIntimacaoTacitaEm() { return intimacaoTacitaEm; }
    public void setIntimacaoTacitaEm(Instant intimacaoTacitaEm) { this.intimacaoTacitaEm = intimacaoTacitaEm; }
    public Instant getPrazoFatal() { return prazoFatal; }
    public void setPrazoFatal(Instant prazoFatal) { this.prazoFatal = prazoFatal; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
