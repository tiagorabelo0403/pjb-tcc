package com.tcc.pjb.backend.model.entity.peticionamento;

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
import java.time.Instant;

/**
 * Snapshot imutável de uma versão do conteúdo de um rascunho de peça inicial
 * ({@code tb_laiane_peticao_inicial_draft}). Cada autosave que muda o conteúdo grava uma versão,
 * permitindo recuperar o rascunho após queda de energia/conexão e voltar a estados anteriores.
 */
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_peticao_draft_versao",
        indexes = {
                @Index(name = "idx_peticao_draft_versao_draft", columnList = "draft_id,versao_seq")
        })
public class PeticaoDraftVersao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "draft_id", nullable = false)
    private Long draftId;

    @Column(name = "versao_seq", nullable = false)
    private int versaoSeq;

    @Column(name = "origem", nullable = false, length = 20)
    private String origem;

    @Column(name = "titulo_caso", length = 180)
    private String tituloCaso;

    @Column(name = "minuta_html", columnDefinition = "TEXT")
    private String minutaHtml;

    @Column(name = "conteudo_json", columnDefinition = "TEXT")
    private String conteudoJson;

    @Column(name = "fatos_json", columnDefinition = "TEXT")
    private String fatosJson;

    @Column(name = "pedidos_json", columnDefinition = "TEXT")
    private String pedidosJson;

    @Column(name = "fundamentos_json", columnDefinition = "TEXT")
    private String fundamentosJson;

    @Column(name = "provas_json", columnDefinition = "TEXT")
    private String provasJson;

    @Column(name = "hash_integridade", nullable = false, length = 64)
    private String hashIntegridade;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PeticaoDraftVersao() {
    }

    public PeticaoDraftVersao(Long draftId, int versaoSeq, String origem) {
        this.draftId = draftId;
        this.versaoSeq = versaoSeq;
        this.origem = origem;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getDraftId() {
        return draftId;
    }

    public int getVersaoSeq() {
        return versaoSeq;
    }

    public String getOrigem() {
        return origem;
    }

    public String getTituloCaso() {
        return tituloCaso;
    }

    public void setTituloCaso(String tituloCaso) {
        this.tituloCaso = tituloCaso;
    }

    public String getMinutaHtml() {
        return minutaHtml;
    }

    public void setMinutaHtml(String minutaHtml) {
        this.minutaHtml = minutaHtml;
    }

    public String getConteudoJson() {
        return conteudoJson;
    }

    public void setConteudoJson(String conteudoJson) {
        this.conteudoJson = conteudoJson;
    }

    public String getFatosJson() {
        return fatosJson;
    }

    public void setFatosJson(String fatosJson) {
        this.fatosJson = fatosJson;
    }

    public String getPedidosJson() {
        return pedidosJson;
    }

    public void setPedidosJson(String pedidosJson) {
        this.pedidosJson = pedidosJson;
    }

    public String getFundamentosJson() {
        return fundamentosJson;
    }

    public void setFundamentosJson(String fundamentosJson) {
        this.fundamentosJson = fundamentosJson;
    }

    public String getProvasJson() {
        return provasJson;
    }

    public void setProvasJson(String provasJson) {
        this.provasJson = provasJson;
    }

    public String getHashIntegridade() {
        return hashIntegridade;
    }

    public void setHashIntegridade(String hashIntegridade) {
        this.hashIntegridade = hashIntegridade;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
