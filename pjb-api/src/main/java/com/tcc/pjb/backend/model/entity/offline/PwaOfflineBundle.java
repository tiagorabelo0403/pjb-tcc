package com.tcc.pjb.backend.model.entity.offline;

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
@Table(name = "tb_pwa_offline_bundle",
        indexes = {
                @Index(name = "idx_pwa_offline_bundle_token", columnList = "bundle_token", unique = true),
                @Index(name = "idx_pwa_offline_bundle_user_status", columnList = "solicitante_id,status"),
                @Index(name = "idx_pwa_offline_bundle_processo", columnList = "processo_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PwaOfflineBundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bundle_token", nullable = false, unique = true, length = 120)
    private String bundleToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id")
    private Usuario solicitante;

    @Column(name = "device_fingerprint", length = 180)
    private String deviceFingerprint;

    @Column(name = "escopo", nullable = false, length = 80)
    private String escopo;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "manifest_hash", nullable = false, length = 128)
    private String manifestHash;

    @Column(name = "manifest_json", nullable = false, columnDefinition = "TEXT")
    private String manifestJson;

    @Column(name = "replay_acoes_json", columnDefinition = "TEXT")
    private String replayAcoesJson;

    @Column(name = "conflito_resumo", columnDefinition = "TEXT")
    private String conflitoResumo;

    @Column(name = "aberto_em")
    private Instant abertoEm;

    @Column(name = "sincronizado_em")
    private Instant sincronizadoEm;

    @Column(name = "expira_em")
    private Instant expiraEm;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (escopo == null || escopo.isBlank()) {
            escopo = "PROCESSO_TIMELINE";
        }
        if (status == null || status.isBlank()) {
            status = "ABERTO";
        }
        if (abertoEm == null) {
            abertoEm = Instant.now();
        }
        if (expiraEm == null) {
            expiraEm = abertoEm.plusSeconds(60L * 60L * 24L * 3L);
        }
    }
}
