package com.tcc.pjb.backend.model.entity.cidadao;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_processo_visibilidade_pessoal", uniqueConstraints = {
        @UniqueConstraint(name = "uk_processo_visibilidade_pessoal", columnNames = {"nupn", "escopo"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessoVisibilidadePessoalOverride {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nupn", nullable = false, length = 50)
    private String nupn;

    @Column(name = "processo_local_id")
    private Long processoLocalId;

    @Column(name = "escopo", nullable = false, length = 40)
    private String escopo;

    @Column(name = "visivel", nullable = false)
    private boolean visivel;

    @Column(name = "fundamento", length = 500)
    private String fundamento;

    @Column(name = "concedido_por_usuario_id")
    private Long concedidoPorUsuarioId;

    @Column(name = "concedido_por_perfil", length = 60)
    private String concedidoPorPerfil;

    @Column(name = "expira_em")
    private Instant expiraEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (criadoEm == null) {
            criadoEm = now;
        }
        atualizadoEm = now;
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = Instant.now();
    }

    public boolean ativa(Instant referencia) {
        return expiraEm == null || expiraEm.isAfter(referencia == null ? Instant.now() : referencia);
    }
}
