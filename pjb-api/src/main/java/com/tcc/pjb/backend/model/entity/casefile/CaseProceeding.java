package com.tcc.pjb.backend.model.entity.casefile;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.*;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_case_proceeding",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_case_proceeding_key", columnNames = {"proceeding_key"})
        },
        indexes = {
                @Index(name = "ix_case_proceeding_case_file", columnList = "case_file_id"),
                @Index(name = "ix_case_proceeding_numero", columnList = "numero_unificado")
        })
public class CaseProceeding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_file_id", nullable = false)
    private Long caseFileId;

    @Column(name = "proceeding_key", nullable = false, length = 64)
    private String proceedingKey;

    @Column(name = "shadow", nullable = false)
    private boolean shadow;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CaseProceedingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "instance_level", nullable = false, length = 30)
    private InstanceLevel instanceLevel;

    @Column(name = "court", length = 120)
    private String court;

    @Column(name = "numero_unificado", length = 50)
    private String numeroUnificado;

    @Column(name = "linked_processo_id")
    private Long linkedProcessoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "continuity_track", nullable = false, length = 30)
    @Builder.Default
    private CaseContinuityTrack continuityTrack = CaseContinuityTrack.CONHECIMENTO;

    @Enumerated(EnumType.STRING)
    @Column(name = "proceeding_role", nullable = false, length = 30)
    @Builder.Default
    private CaseProceedingRole proceedingRole = CaseProceedingRole.ROOT;

    @Column(name = "parent_proceeding_key", length = 64)
    private String parentProceedingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_fase_processual", length = 60)
    private FaseProcessual sourceFaseProcessual;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_status_processo", length = 60)
    private StatusProcesso sourceStatusProcesso;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "secrecy", nullable = false, length = 40)
    @Builder.Default
    private NivelSigilo secrecy = NivelSigilo.PUBLICO;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false, length = 30)
    @Builder.Default
    private LegalIntegrationSystem sourceSystem = LegalIntegrationSystem.OTHER;

    
    @Version
    @Column(name = "ver", nullable = false)
    private long ver;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = shadow ? CaseProceedingStatus.PREDICTED : CaseProceedingStatus.ACTIVE;
        if (continuityTrack == null) continuityTrack = shadow ? CaseContinuityTrack.RECURSAL : CaseContinuityTrack.CONHECIMENTO;
        if (proceedingRole == null) proceedingRole = shadow ? CaseProceedingRole.RECURSAL : CaseProceedingRole.ROOT;
        if (secrecy == null) secrecy = NivelSigilo.PUBLICO;
        if (sourceSystem == null) sourceSystem = LegalIntegrationSystem.OTHER;
        if (lastSyncAt == null) lastSyncAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        if (continuityTrack == null) continuityTrack = shadow ? CaseContinuityTrack.RECURSAL : CaseContinuityTrack.CONHECIMENTO;
        if (proceedingRole == null) proceedingRole = shadow ? CaseProceedingRole.RECURSAL : CaseProceedingRole.ROOT;
        if (secrecy == null) secrecy = NivelSigilo.PUBLICO;
        if (sourceSystem == null) sourceSystem = LegalIntegrationSystem.OTHER;
        lastSyncAt = Instant.now();
    }
}
