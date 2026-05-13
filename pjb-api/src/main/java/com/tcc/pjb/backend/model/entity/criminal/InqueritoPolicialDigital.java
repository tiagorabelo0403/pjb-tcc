package com.tcc.pjb.backend.model.entity.criminal;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_inquerito_policial_digital",
        indexes = {
                @Index(name = "idx_inquerito_numero", columnList = "numero_procedimento", unique = true),
                @Index(name = "idx_inquerito_status", columnList = "status"),
                @Index(name = "idx_inquerito_autoridade", columnList = "autoridade_responsavel_id,status"),
                @Index(name = "idx_inquerito_processo", columnList = "processo_vinculado_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class InqueritoPolicialDigital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_procedimento", nullable = false, unique = true, length = 80)
    private String numeroProcedimento;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "fase_atual", nullable = false, length = 40)
    private String faseAtual;

    @Column(name = "natureza_fato", nullable = false, length = 180)
    private String naturezaFato;

    @Column(name = "resumo_fatos", columnDefinition = "TEXT", nullable = false)
    private String resumoFatos;

    @Column(name = "investigados_resumo", columnDefinition = "TEXT")
    private String investigadosResumo;

    @Column(name = "vitimas_resumo", columnDefinition = "TEXT")
    private String vitimasResumo;

    @Column(name = "indicios_resumo", columnDefinition = "TEXT")
    private String indiciosResumo;

    @Column(name = "diligencias_pendentes", columnDefinition = "TEXT")
    private String diligenciasPendentes;

    @Column(name = "ultima_movimentacao_resumo", columnDefinition = "TEXT")
    private String ultimaMovimentacaoResumo;

    @Column(name = "cadeia_custodia_hash", length = 128)
    private String cadeiaCustodiaHash;

    @Column(name = "orgao_apuracao", length = 120)
    private String orgaoApuracao;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "municipio", length = 120)
    private String municipio;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_sigilo", length = 40)
    private NivelSigilo nivelSigilo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autoridade_responsavel_id")
    private Usuario autoridadeResponsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_vinculado_id")
    private Processo processoVinculado;

    @Column(name = "instaurado_em")
    private Instant instauradoEm;

    @Column(name = "remetido_ao_mp_em")
    private Instant remetidoAoMpEm;

    @Column(name = "prazo_conclusao")
    private LocalDate prazoConclusao;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (tipo == null || tipo.isBlank()) {
            tipo = "INQUERITO_POLICIAL";
        }
        if (status == null || status.isBlank()) {
            status = "INSTAURADO";
        }
        if (faseAtual == null || faseAtual.isBlank()) {
            faseAtual = "INVESTIGACAO";
        }
        if (nivelSigilo == null) {
            nivelSigilo = NivelSigilo.SIGILO_N2;
        }
        if (instauradoEm == null) {
            instauradoEm = Instant.now();
        }
    }
}
