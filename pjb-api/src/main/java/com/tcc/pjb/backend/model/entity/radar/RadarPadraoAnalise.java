package com.tcc.pjb.backend.model.entity.radar;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "tb_radar_padrao_analise", indexes = {
        @Index(name = "idx_radar_analise_processo", columnList = "processo_id, gerado_em"),
        @Index(name = "idx_radar_analise_nupn", columnList = "nupn, gerado_em"),
        @Index(name = "idx_radar_analise_escritorio", columnList = "escritorio_oab_hash, gerado_em"),
        @Index(name = "idx_radar_analise_autor", columnList = "documento_autor_hash, gerado_em"),
        @Index(name = "idx_radar_analise_reu", columnList = "documento_reu_hash, gerado_em"),
        @Index(name = "idx_radar_analise_fp1", columnList = "fingerprint_estrutura_hash, gerado_em"),
        @Index(name = "idx_radar_analise_fp2", columnList = "fingerprint_conteudo_hash, gerado_em")
})
public class RadarPadraoAnalise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @Column(name = "nupn", length = 80, nullable = false)
    private String nupn;

    @Column(name = "tribunal_codigo", length = 30)
    private String tribunalCodigo;

    @Column(name = "documento_autor_hash", length = 64)
    private String documentoAutorHash;

    @Column(name = "documento_reu_hash", length = 64)
    private String documentoReuHash;

    @Column(name = "escritorio_oab_hash", length = 64)
    private String escritorioOabHash;

    @Column(name = "fingerprint_estrutura_hash", length = 64)
    private String fingerprintEstruturaHash;

    @Column(name = "fingerprint_conteudo_hash", length = 64)
    private String fingerprintConteudoHash;

    @Column(name = "numero_paragrafos")
    private Integer numeroParagrafos;

    @Column(name = "total_palavras")
    private Integer totalPalavras;

    @Column(name = "densidade_jargao", precision = 10, scale = 6)
    private BigDecimal densidadeJargao;

    @Column(name = "diversidade_lexica", precision = 10, scale = 6)
    private BigDecimal diversidadeLexica;

    @Column(name = "valor_causa", precision = 19, scale = 2)
    private BigDecimal valorCausa;

    @Column(name = "data_ajuizamento")
    private LocalDate dataAjuizamento;

    @Column(name = "score_geral", precision = 10, scale = 6)
    private BigDecimal scoreGeral;

    @Column(name = "nivel_mais_alto", length = 30)
    private String nivelMaisAlto;

    @Column(name = "total_alertas")
    private Integer totalAlertas;

    @Column(name = "tipos_detectados", length = 600)
    private String tiposDetectados;

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
