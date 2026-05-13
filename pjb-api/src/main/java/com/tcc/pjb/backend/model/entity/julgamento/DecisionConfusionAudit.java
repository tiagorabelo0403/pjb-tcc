package com.tcc.pjb.backend.model.entity.julgamento;

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
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_decision_confusion_audit",
        indexes = {
                @Index(name = "idx_decision_confusion_process_created", columnList = "processo_id,created_at"),
                @Index(name = "idx_decision_confusion_user_created", columnList = "usuario_id,created_at"),
                @Index(name = "idx_decision_confusion_status", columnList = "result_status,created_at")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class DecisionConfusionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "focus_session_id")
    private DecisionFocusSession focusSession;

    @Column(name = "act_type", nullable = false, length = 50)
    private String actType;

    @Column(name = "target_process_fingerprint", nullable = false, length = 128)
    private String targetProcessFingerprint;

    @Column(name = "request_text_hash", nullable = false, length = 128)
    private String requestTextHash;

    @Column(name = "result_status", nullable = false, length = 30)
    private String resultStatus;

    @Column(name = "semantic_score")
    private Integer semanticScore;

    @Column(name = "competing_score")
    private Integer competingScore;

    @Column(name = "competing_processo_id")
    private Long competingProcessoId;

    @Column(name = "reasons_json", columnDefinition = "TEXT")
    private String reasonsJson;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
