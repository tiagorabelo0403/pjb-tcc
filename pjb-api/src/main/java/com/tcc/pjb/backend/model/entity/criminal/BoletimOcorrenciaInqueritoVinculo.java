package com.tcc.pjb.backend.model.entity.criminal;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.model.entity.Usuario;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_boletim_ocorrencia_inquerito_vinculo",
        uniqueConstraints = @UniqueConstraint(name = "uq_boletim_ocorrencia_vinculo_boletim", columnNames = "boletim_id"),
        indexes = @Index(name = "idx_boletim_ocorrencia_vinculo_inquerito_jpa", columnList = "inquerito_id,vinculado_em"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class BoletimOcorrenciaInqueritoVinculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boletim_id", nullable = false)
    private BoletimOcorrenciaDigital boletim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquerito_id", nullable = false)
    private InqueritoPolicialDigital inquerito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vinculado_por_id", nullable = false)
    private Usuario vinculadoPor;

    @CreatedDate
    @Column(name = "vinculado_em", nullable = false, updatable = false)
    private Instant vinculadoEm;

    @Column(name = "cadeia_custodia_hash", nullable = false, length = 128)
    private String cadeiaCustodiaHash;

    @PrePersist
    void prePersist() {
        if (vinculadoEm == null) {
            vinculadoEm = Instant.now();
        }
    }
}
