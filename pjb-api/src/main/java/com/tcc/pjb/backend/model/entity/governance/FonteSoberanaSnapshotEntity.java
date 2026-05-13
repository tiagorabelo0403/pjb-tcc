package com.tcc.pjb.backend.model.entity.governance;

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
import jakarta.persistence.Table;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_fonte_soberana_snapshot", indexes = {
        @Index(name = "idx_fonte_soberana_processo", columnList = "processo_id", unique = true),
        @Index(name = "idx_fonte_soberana_digest", columnList = "digest")
})
public class FonteSoberanaSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "numero_processo", length = 80, nullable = false)
    private String numeroProcesso;

    @Column(name = "confiabilidade_media", nullable = false)
    private Integer confiabilidadeMedia;

    @Column(name = "possui_conflito", nullable = false)
    private boolean possuiConflito;

    @Column(name = "exige_refresh", nullable = false)
    private boolean exigeRefresh;

    @Column(name = "digest", length = 128, nullable = false)
    private String digest;

    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public Long getId() {
        return id;
    }

    public Long getProcessoId() {
        return processoId;
    }

    public void setProcessoId(Long processoId) {
        this.processoId = processoId;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public Integer getConfiabilidadeMedia() {
        return confiabilidadeMedia;
    }

    public void setConfiabilidadeMedia(Integer confiabilidadeMedia) {
        this.confiabilidadeMedia = confiabilidadeMedia;
    }

    public boolean isPossuiConflito() {
        return possuiConflito;
    }

    public void setPossuiConflito(boolean possuiConflito) {
        this.possuiConflito = possuiConflito;
    }

    public boolean isExigeRefresh() {
        return exigeRefresh;
    }

    public void setExigeRefresh(boolean exigeRefresh) {
        this.exigeRefresh = exigeRefresh;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
