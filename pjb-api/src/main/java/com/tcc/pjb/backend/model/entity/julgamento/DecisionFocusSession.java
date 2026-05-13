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
@Table(name = "tb_decision_focus_session",
        indexes = {
                @Index(name = "idx_decision_focus_user_status", columnList = "usuario_id,status"),
                @Index(name = "idx_decision_focus_process_status", columnList = "processo_id,status"),
                @Index(name = "idx_decision_focus_token", columnList = "session_token", unique = true)
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class DecisionFocusSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "session_token", nullable = false, unique = true, length = 120)
    private String sessionToken;

    @Column(name = "process_fingerprint", nullable = false, length = 128)
    private String processFingerprint;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "window_binding", length = 160)
    private String windowBinding;

    @Column(name = "tab_binding", length = 160)
    private String tabBinding;

    @Column(name = "route_binding", length = 240)
    private String routeBinding;

    @Column(name = "binding_fingerprint", length = 128)
    private String bindingFingerprint;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "numero_snapshot", length = 60)
    private String numeroSnapshot;

    @Column(name = "classe_snapshot", length = 160)
    private String classeSnapshot;

    @Column(name = "autor_snapshot", length = 220)
    private String autorSnapshot;

    @Column(name = "reu_snapshot", length = 220)
    private String reuSnapshot;

    @Column(name = "assunto_snapshot", length = 240)
    private String assuntoSnapshot;

    @Column(name = "summary_snapshot", length = 600)
    private String summarySnapshot;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "armed_at")
    private Instant armedAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "OPEN";
        }
        if (openedAt == null) {
            openedAt = Instant.now();
        }
        if (expiresAt == null) {
            expiresAt = openedAt.plusSeconds(15 * 60L);
        }
    }
}
