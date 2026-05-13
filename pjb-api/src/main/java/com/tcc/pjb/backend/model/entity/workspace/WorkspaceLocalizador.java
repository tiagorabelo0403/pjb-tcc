package com.tcc.pjb.backend.model.entity.workspace;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_workspace_localizador")
public class WorkspaceLocalizador {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "descricao", length = 400)
    private String descricao;

    @Column(name = "criterio_json", nullable = false, length = 12000, columnDefinition = "varchar(12000)")
    private String criterioJson;

    @Column(name = "compartilhado")
    private boolean compartilhado;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        if (criadoEm == null) criadoEm = now;
        atualizadoEm = now;
        if (nome != null) nome = nome.trim();
        if (descricao != null) descricao = descricao.trim();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
        if (nome != null) nome = nome.trim();
        if (descricao != null) descricao = descricao.trim();
    }
}
