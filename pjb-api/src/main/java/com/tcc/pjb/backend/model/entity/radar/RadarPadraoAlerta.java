package com.tcc.pjb.backend.model.entity.radar;

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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_radar_padrao_alerta", indexes = {
        @Index(name = "idx_radar_alerta_processo", columnList = "processo_id, detectado_em"),
        @Index(name = "idx_radar_alerta_nupn", columnList = "nupn, detectado_em"),
        @Index(name = "idx_radar_alerta_tipo", columnList = "tipo_padrao, nivel")
})
public class RadarPadraoAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analise_id")
    private RadarPadraoAnalise analise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @Column(name = "nupn", length = 80, nullable = false)
    private String nupn;

    @Column(name = "tipo_padrao", length = 80, nullable = false)
    private String tipoPadrao;

    @Column(name = "nivel", length = 30, nullable = false)
    private String nivel;

    @Column(name = "score", precision = 10, scale = 6)
    private BigDecimal score;

    @Column(name = "descricao_tecnica", columnDefinition = "text")
    private String descricaoTecnica;

    @Column(name = "evidencias_objetivas", columnDefinition = "text")
    private String evidenciasObjetivas;

    @Column(name = "orientacao_magistrado", columnDefinition = "text")
    private String orientacaoMagistrado;

    @Column(name = "processo_nao_bloqueado", nullable = false)
    private boolean processoNaoBloqueado = true;

    @Column(name = "referencia_teto", length = 80)
    private String referenciaTeto;

    @Column(name = "explicacao_financeira_ia", columnDefinition = "text")
    private String explicacaoFinanceiraIa;

    @Column(name = "nupns_relacionados_json", columnDefinition = "text")
    private String nupnsRelacionadosJson;

    @Column(name = "chave_deteccao", length = 64, unique = true)
    private String chaveDeteccao;

    @Column(name = "detectado_em", nullable = false)
    private Instant detectadoEm;

    @PrePersist
    void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (detectadoEm == null) {
            detectadoEm = Instant.now();
        }
    }
}
