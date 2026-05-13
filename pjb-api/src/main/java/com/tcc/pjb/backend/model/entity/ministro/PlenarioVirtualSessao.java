package com.tcc.pjb.backend.model.entity.ministro;

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

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_plenario_virtual_sessao",
        indexes = {
                @Index(name = "idx_plenario_virtual_sessao_codigo", columnList = "codigo", unique = true),
                @Index(name = "idx_plenario_virtual_sessao_status", columnList = "status"),
                @Index(name = "idx_plenario_virtual_sessao_processo", columnList = "processo_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PlenarioVirtualSessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 120)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relator_id", nullable = false)
    private Usuario relator;

    @Column(name = "orgao_julgador", nullable = false, length = 120)
    private String orgaoJulgador;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "materia_resumo", columnDefinition = "TEXT")
    private String materiaResumo;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "segredo_ate_proclamacao", nullable = false)
    private boolean segredoAteProclamacao;

    @Column(name = "quorum_minimo")
    private Integer quorumMinimo;

    @Column(name = "votos_recebidos")
    private Integer votosRecebidos;

    @Column(name = "votos_acompanham_relator")
    private Integer votosAcompanhamRelator;

    @Column(name = "votos_divergentes")
    private Integer votosDivergentes;

    @Column(name = "votos_parciais")
    private Integer votosParciais;

    @Column(name = "resultado_final", length = 180)
    private String resultadoFinal;

    @Column(name = "ata_hash", length = 128)
    private String ataHash;

    @Column(name = "prova_integridade_raiz", length = 128)
    private String provaIntegridadeRaiz;

    @Column(name = "aberta_em")
    private Instant abertaEm;

    @Column(name = "proclamada_em")
    private Instant proclamadaEm;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "ABERTA";
        }
        if (quorumMinimo == null || quorumMinimo <= 0) {
            quorumMinimo = 6;
        }
        if (abertaEm == null) {
            abertaEm = Instant.now();
        }
        if (votosRecebidos == null) {
            votosRecebidos = 0;
        }
        if (votosAcompanhamRelator == null) {
            votosAcompanhamRelator = 0;
        }
        if (votosDivergentes == null) {
            votosDivergentes = 0;
        }
        if (votosParciais == null) {
            votosParciais = 0;
        }
    }
}
