package com.tcc.pjb.backend.model.entity.orgao;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import com.tcc.pjb.backend.model.entity.Processo;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_processo_orgao_oficial",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_proc_orgao", columnNames = {"processo_id", "orgao_tipo"})
        },
        indexes = {
                @Index(name = "idx_proc_orgao_processo", columnList = "processo_id"),
                @Index(name = "idx_proc_orgao_tipo", columnList = "orgao_tipo")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessoOrgaoOficial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Enumerated(EnumType.STRING)
    @Column(name = "orgao_tipo", length = 40, nullable = false)
    private OrgaoOficialTipo orgaoTipo;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
