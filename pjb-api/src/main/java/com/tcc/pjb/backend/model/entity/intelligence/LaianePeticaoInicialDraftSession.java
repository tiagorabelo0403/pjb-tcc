package com.tcc.pjb.backend.model.entity.intelligence;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_laiane_peticao_inicial_draft",
        indexes = {
                @Index(name = "idx_laiane_initial_user", columnList = "solicitante_id,created_at"),
                @Index(name = "idx_laiane_initial_status", columnList = "status,ramo_direito"),
                @Index(name = "idx_laiane_initial_processo", columnList = "processo_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class LaianePeticaoInicialDraftSession {

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final LaianePeticaoInicialDraftSession target = new LaianePeticaoInicialDraftSession();
        public Builder id(Long id) { target.setId(id); return this; }
        public Builder status(String status) { target.setStatus(status); return this; }
        public Builder conteudoHtml(String conteudoHtml) { target.setMinutaInicial(conteudoHtml); return this; }
        public Builder minutaInicial(String minutaInicial) { target.setMinutaInicial(minutaInicial); return this; }
        public Builder tituloCaso(String tituloCaso) { target.setTituloCaso(tituloCaso); return this; }
        public Builder numeroProtocolo(String numeroProtocolo) { return this; }
        public Builder numeroProcesso(String numeroProcesso) { return this; }
        public Builder hashIntegridade(String hashIntegridade) { target.setHashIntegridade(hashIntegridade); return this; }
        public Builder solicitante(Usuario solicitante) { target.setSolicitante(solicitante); return this; }
        public Builder processo(Processo processo) { target.setProcesso(processo); return this; }
        public LaianePeticaoInicialDraftSession build() {
            if (target.getMinutaInicial() == null) target.setMinutaInicial("");
            if (target.getHashIntegridade() == null) target.setHashIntegridade("TEST-HASH");
            if (target.getTituloCaso() == null) target.setTituloCaso("RASCUNHO");
            return target;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @Column(name = "titulo_caso", nullable = false, length = 180)
    private String tituloCaso;

    @Column(name = "ramo_direito", length = 40)
    private String ramoDireito;

    @Column(name = "rito_sugerido", length = 80)
    private String ritoSugerido;

    @Column(name = "classe_sugerida", length = 120)
    private String classeSugerida;

    @Column(name = "urgencia_score")
    private Integer urgenciaScore;

    @Column(name = "readiness_score")
    private Integer readinessScore;

    @Column(name = "fatos_json", columnDefinition = "TEXT")
    private String fatosJson;

    @Column(name = "pedidos_json", columnDefinition = "TEXT")
    private String pedidosJson;

    @Column(name = "fundamentos_json", columnDefinition = "TEXT")
    private String fundamentosJson;

    @Column(name = "provas_json", columnDefinition = "TEXT")
    private String provasJson;

    @Column(name = "checklist_json", columnDefinition = "TEXT")
    private String checklistJson;

    @Column(name = "uf_autor", length = 2)
    private String ufAutor;

    @Column(name = "comarca_autor", length = 120)
    private String comarcaAutor;

    @Column(name = "uf_reu", length = 2)
    private String ufReu;

    @Column(name = "comarca_reu", length = 120)
    private String comarcaReu;

    @Column(name = "endereco_reu_desconhecido", nullable = false)
    private boolean enderecoReuDesconhecido;

    @Column(name = "minuta_inicial", columnDefinition = "TEXT", nullable = false)
    private String minutaInicial;

    @Column(name = "conteudo_json", columnDefinition = "TEXT")
    private String conteudoJson;

    @Column(name = "hash_integridade", length = 64, nullable = false)
    private String hashIntegridade;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "RASCUNHO";
        }
        if (urgenciaScore == null) {
            urgenciaScore = 0;
        }
        if (readinessScore == null) {
            readinessScore = 0;
        }
    }
}
