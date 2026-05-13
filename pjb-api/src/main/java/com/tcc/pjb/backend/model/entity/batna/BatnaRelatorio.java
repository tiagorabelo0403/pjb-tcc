package com.tcc.pjb.backend.model.entity.batna;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_batna_relatorio", indexes = {
        @Index(name = "idx_batna_processo", columnList = "processo_id, gerado_em"),
        @Index(name = "idx_batna_proposta", columnList = "proposta_acordo_id, gerado_em"),
        @Index(name = "idx_batna_hash", columnList = "hash_contexto"),
        @Index(name = "idx_batna_nupn", columnList = "nupn")
})
public class BatnaRelatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_acordo_id")
    private PropostaAcordo propostaAcordo;

    @Column(name = "nupn", length = 80, nullable = false)
    private String nupn;

    @Column(name = "tribunal_codigo", length = 30)
    private String tribunalCodigo;

    @Column(name = "ramo_direito", length = 80)
    private String ramoDireito;

    @Column(name = "classe_tpu", length = 120)
    private String classeTpu;

    @Column(name = "fase_processual", length = 80)
    private String faseProcessual;

    @Column(name = "valor_causa", precision = 19, scale = 2)
    private BigDecimal valorCausa;

    @Column(name = "valor_pedido_principal", precision = 19, scale = 2)
    private BigDecimal valorPedidoPrincipal;

    @Column(name = "valor_acordo_em_discussao", precision = 19, scale = 2)
    private BigDecimal valorAcordoEmDiscussao;

    @Column(name = "custo_autor_total", precision = 19, scale = 2)
    private BigDecimal custoAutorTotal;

    @Column(name = "custo_reu_total", precision = 19, scale = 2)
    private BigDecimal custoReuTotal;

    @Column(name = "custo_combinado", precision = 19, scale = 2)
    private BigDecimal custoCombinado;

    @Column(name = "dias_restantes_sem_recurso")
    private Integer diasRestantesSemRecurso;

    @Column(name = "dias_restantes_com_recurso")
    private Integer diasRestantesComRecurso;

    @Column(name = "prob_recurso", precision = 10, scale = 6)
    private BigDecimal probabilidadeRecurso;

    @Column(name = "prob_reforma", precision = 10, scale = 6)
    private BigDecimal probabilidadeReforma;

    @Column(name = "indice_pressao_economica", precision = 10, scale = 6)
    private BigDecimal indicePressaoEconomica;

    @Column(name = "confianca_geral", precision = 10, scale = 6)
    private BigDecimal confiancaGeral;

    @Column(name = "teto_violacao")
    private Boolean tetoViolacao;

    @Column(name = "teto_tipo", length = 80)
    private String tetoTipo;

    @Column(name = "teto_limite", precision = 19, scale = 2)
    private BigDecimal tetoLimite;

    @Column(name = "teto_excedente", precision = 19, scale = 2)
    private BigDecimal tetoExcedente;

    @Column(name = "hash_contexto", length = 64, nullable = false)
    private String hashContexto;

    @Column(name = "resumo_narrativo", columnDefinition = "text")
    private String resumoNarrativo;

    @Column(name = "resumo_tecnico", columnDefinition = "text")
    private String resumoTecnico;

    @Column(name = "request_json", columnDefinition = "text")
    private String requestJson;

    @Column(name = "response_json", columnDefinition = "text")
    private String responseJson;

    @Column(name = "gerado_em", nullable = false)
    private Instant geradoEm;

    @PrePersist
    void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (geradoEm == null) {
            geradoEm = Instant.now();
        }
    }
}
