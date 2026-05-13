package com.tcc.pjb.backend.model.entity.processo;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_processo_leitura_ator", indexes = {
        @Index(name = "idx_proc_leitura_proc_last", columnList = "processo_id,last_read_at"),
        @Index(name = "idx_proc_leitura_user_last", columnList = "usuario_id,last_read_at"),
        @Index(name = "idx_proc_leitura_cluster_last", columnList = "processo_id,actor_cluster,last_read_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessoLeituraAtor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "actor_role", nullable = false, length = 60)
    private String actorRole;

    @Column(name = "actor_cluster", nullable = false, length = 60)
    private String actorCluster;

    @Column(name = "actor_display_name", nullable = false, length = 180)
    private String actorDisplayName;

    @Column(name = "first_read_at", nullable = false)
    private LocalDateTime firstReadAt;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    @Column(name = "read_count", nullable = false)
    private Long readCount;

    @Column(name = "last_channel", length = 80)
    private String lastChannel;

    @Column(name = "last_request_id", length = 120)
    private String lastRequestId;

    @Column(name = "last_justificativa", length = 500)
    private String lastJustificativa;

    @Column(name = "last_step_up_satisfied", nullable = false)
    @Builder.Default
    private boolean lastStepUpSatisfied = false;

    @Column(name = "last_party_signal_at")
    private LocalDateTime lastPartySignalAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
