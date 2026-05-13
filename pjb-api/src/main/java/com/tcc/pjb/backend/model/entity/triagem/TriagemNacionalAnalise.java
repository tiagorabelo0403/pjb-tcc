package com.tcc.pjb.backend.model.entity.triagem;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_triagem_nacional_analise",
        indexes = {
                @Index(name = "idx_triagem_nacional_nupn", columnList = "nupn_provisorio"),
                @Index(name = "idx_triagem_nacional_processo", columnList = "processo_id"),
                @Index(name = "idx_triagem_nacional_doc_autor", columnList = "documento_autor"),
                @Index(name = "idx_triagem_nacional_veredito", columnList = "veredito")
        }
)
public class TriagemNacionalAnalise {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nupn_provisorio", nullable = false, length = 80)
    private String nupnProvisorio;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "documento_autor", length = 14)
    private String documentoAutor;

    @Column(name = "documento_reu", length = 14)
    private String documentoReu;

    @Column(name = "veredito", nullable = false, length = 40)
    private String veredito;

    @Column(name = "confianca_geral", precision = 8, scale = 4)
    private BigDecimal confiancaGeral;

    @Column(name = "aprovacao_automatica", nullable = false)
    private boolean aprovacaoAutomatica;

    @Column(name = "classe_tpu", length = 120)
    private String classeTpu;

    @Column(name = "assunto_tpu", length = 180)
    private String assuntoTpu;

    @Column(name = "prescricao_aparente", nullable = false)
    private boolean prescricaoAparente;

    @Column(name = "decadencia_aparente", nullable = false)
    private boolean decadenciaAparente;

    @Column(name = "competencia_sugerida", length = 40)
    private String competenciaSugerida;

    @Column(name = "rito_sugerido", length = 80)
    private String ritoSugerido;

    @Column(name = "pendencias_json", columnDefinition = "TEXT")
    private String pendenciasJson;

    @Column(name = "conexos_json", columnDefinition = "TEXT")
    private String conexosJson;

    @Column(name = "resumo_decisao", columnDefinition = "TEXT")
    private String resumoDecisao;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "triado_em", nullable = false)
    private Instant triadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNupnProvisorio() {
        return nupnProvisorio;
    }

    public void setNupnProvisorio(String nupnProvisorio) {
        this.nupnProvisorio = nupnProvisorio;
    }

    public Long getProcessoId() {
        return processoId;
    }

    public void setProcessoId(Long processoId) {
        this.processoId = processoId;
    }

    public String getDocumentoAutor() {
        return documentoAutor;
    }

    public void setDocumentoAutor(String documentoAutor) {
        this.documentoAutor = documentoAutor;
    }

    public String getDocumentoReu() {
        return documentoReu;
    }

    public void setDocumentoReu(String documentoReu) {
        this.documentoReu = documentoReu;
    }

    public String getVeredito() {
        return veredito;
    }

    public void setVeredito(String veredito) {
        this.veredito = veredito;
    }

    public BigDecimal getConfiancaGeral() {
        return confiancaGeral;
    }

    public void setConfiancaGeral(BigDecimal confiancaGeral) {
        this.confiancaGeral = confiancaGeral;
    }

    public boolean isAprovacaoAutomatica() {
        return aprovacaoAutomatica;
    }

    public void setAprovacaoAutomatica(boolean aprovacaoAutomatica) {
        this.aprovacaoAutomatica = aprovacaoAutomatica;
    }

    public String getClasseTpu() {
        return classeTpu;
    }

    public void setClasseTpu(String classeTpu) {
        this.classeTpu = classeTpu;
    }

    public String getAssuntoTpu() {
        return assuntoTpu;
    }

    public void setAssuntoTpu(String assuntoTpu) {
        this.assuntoTpu = assuntoTpu;
    }

    public boolean isPrescricaoAparente() {
        return prescricaoAparente;
    }

    public void setPrescricaoAparente(boolean prescricaoAparente) {
        this.prescricaoAparente = prescricaoAparente;
    }

    public boolean isDecadenciaAparente() {
        return decadenciaAparente;
    }

    public void setDecadenciaAparente(boolean decadenciaAparente) {
        this.decadenciaAparente = decadenciaAparente;
    }

    public String getCompetenciaSugerida() {
        return competenciaSugerida;
    }

    public void setCompetenciaSugerida(String competenciaSugerida) {
        this.competenciaSugerida = competenciaSugerida;
    }

    public String getRitoSugerido() {
        return ritoSugerido;
    }

    public void setRitoSugerido(String ritoSugerido) {
        this.ritoSugerido = ritoSugerido;
    }

    public String getPendenciasJson() {
        return pendenciasJson;
    }

    public void setPendenciasJson(String pendenciasJson) {
        this.pendenciasJson = pendenciasJson;
    }

    public String getConexosJson() {
        return conexosJson;
    }

    public void setConexosJson(String conexosJson) {
        this.conexosJson = conexosJson;
    }

    public String getResumoDecisao() {
        return resumoDecisao;
    }

    public void setResumoDecisao(String resumoDecisao) {
        this.resumoDecisao = resumoDecisao;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public Instant getTriadoEm() {
        return triadoEm;
    }

    public void setTriadoEm(Instant triadoEm) {
        this.triadoEm = triadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (triadoEm == null) {
            triadoEm = now;
        }
        atualizadoEm = now;
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = Instant.now();
    }
}
