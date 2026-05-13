package com.tcc.pjb.backend.model.entity.workspace;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import com.tcc.pjb.backend.model.entity.Processo;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_workspace_processo_etiqueta",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_workspace_pe_proc_et", columnNames = {"processo_id", "etiqueta_id"})
        })
public class WorkspaceProcessoEtiqueta {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etiqueta_id", nullable = false)
    private WorkspaceEtiqueta etiqueta;

    @Column(name = "atribuido_por")
    private Long atribuidoPor;

    @Column(name = "atribuido_em")
    private LocalDateTime atribuidoEm;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (atribuidoEm == null) atribuidoEm = LocalDateTime.now();
    }
}
