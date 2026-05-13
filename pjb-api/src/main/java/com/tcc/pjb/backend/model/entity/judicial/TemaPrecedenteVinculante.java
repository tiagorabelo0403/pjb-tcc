package com.tcc.pjb.backend.model.entity.judicial;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_tema_precedente_vinculante",
        indexes = {
                @Index(name = "idx_tema_precedente_codigo", columnList = "codigo", unique = true),
                @Index(name = "idx_tema_precedente_status", columnList = "status"),
                @Index(name = "idx_tema_precedente_tipo", columnList = "tipo")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class TemaPrecedenteVinculante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 80, unique = true)
    private String codigo;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "ementa", columnDefinition = "TEXT", nullable = false)
    private String ementa;

    @Column(name = "tese_firmada", columnDefinition = "TEXT")
    private String teseFirmada;

    @Column(name = "efeitos_processuais", columnDefinition = "TEXT")
    private String efeitosProcessuais;

    @Column(name = "abrangencia", length = 80)
    private String abrangencia;

    @Column(name = "score_corte")
    private Integer scoreCorte;

    @Column(name = "processos_sobrestados")
    private Integer processosSobrestados;

    @Column(name = "processos_aplicados")
    private Integer processosAplicados;

    @Column(name = "fundamentos_resumo", columnDefinition = "TEXT")
    private String fundamentosResumo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leading_case_processo_id")
    private Processo leadingCaseProcesso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relator_id")
    private Usuario relator;

    @Column(name = "julgado_em")
    private Instant julgadoEm;

    @Column(name = "aplicado_em")
    private Instant aplicadoEm;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "RECONHECIDO";
        }
        if (processosSobrestados == null) {
            processosSobrestados = 0;
        }
        if (processosAplicados == null) {
            processosAplicados = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        if (processosSobrestados == null) {
            processosSobrestados = 0;
        }
        if (processosAplicados == null) {
            processosAplicados = 0;
        }
    }
}
