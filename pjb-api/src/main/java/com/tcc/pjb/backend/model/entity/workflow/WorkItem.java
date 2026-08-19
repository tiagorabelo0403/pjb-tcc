package com.tcc.pjb.backend.model.entity.workflow;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_work_item",
        indexes = {
                @Index(name = "idx_workitem_status", columnList = "status"),
                @Index(name = "idx_workitem_role", columnList = "assigned_role,status"),
                @Index(name = "idx_workitem_processo", columnList = "processo_id,status"),
                @Index(name = "idx_workitem_template", columnList = "template_code"),
                @Index(name = "idx_workitem_assigned_due", columnList = "assigned_user_id,status,due_at"),
                @Index(name = "idx_workitem_assigned_update", columnList = "assigned_user_id,status,updated_at"),
                @Index(name = "idx_workitem_assigned_role_due", columnList = "assigned_role,status,due_at"),
                @Index(name = "idx_workitem_role_territory_due", columnList = "assigned_role,uf,comarca,status,due_at"),
                @Index(name = "idx_workitem_assigned_visible_due", columnList = "assigned_user_id,assigned_role,sem_interesse,status,due_at"),
                @Index(name = "idx_workitem_process_visible_update", columnList = "processo_id,sem_interesse,status,updated_at"),
                @Index(name = "idx_workitem_queue_template_status", columnList = "queue_code,template_code,status,updated_at"),
                @Index(name = "idx_workitem_inbox_template_status", columnList = "inbox_key,template_code,status,updated_at")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "fase_origem", length = 60)
    private FaseProcessual faseOrigem;

    
    @Column(name = "template_code", length = 120)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 40)
    private WorkItemType type;

    @Column(name = "titulo", length = 220, nullable = false)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    
    @Column(name = "queue_code", length = 120)
    private String queueCode;

    
    @Column(name = "inbox_key", length = 120)
    private String inboxKey;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_role", length = 60)
    private TipoUsuario assignedRole;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private Usuario assignedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private WorkItemStatus status = WorkItemStatus.PENDENTE;

    
    @Column(name = "prioridade")
    @Builder.Default
    private Integer prioridade = 3;

    
    @Column(name = "blocking", nullable = false)
    @Builder.Default
    private boolean blocking = false;

    
    @Column(name = "due_at")
    private Instant dueAt;

    
    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "comarca", length = 120)
    private String comarca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comarca_id")
    private Comarca comarcaEntidade;


    @Column(name = "base_legal", columnDefinition = "TEXT")
    private String baseLegal;

    @Column(name = "sem_interesse", nullable = false)
    @Builder.Default
    private boolean semInteresse = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;


    public void setFaseOrigem(FaseProcessual faseOrigem) {
        this.faseOrigem = faseOrigem;
    }

    public void setFaseOrigem(String faseOrigem) {
        this.faseOrigem = FaseProcessual.fromString(faseOrigem).orElse(null);
    }

    public Long getProcessoId() {
        return processo == null ? null : processo.getId();
    }

    public static class WorkItemBuilder {
        public WorkItemBuilder faseOrigem(FaseProcessual faseOrigem) {
            this.faseOrigem = faseOrigem;
            return this;
        }

        public WorkItemBuilder faseOrigem(String faseOrigem) {
            this.faseOrigem = FaseProcessual.fromString(faseOrigem).orElse(null);
            return this;
        }
    }
}
