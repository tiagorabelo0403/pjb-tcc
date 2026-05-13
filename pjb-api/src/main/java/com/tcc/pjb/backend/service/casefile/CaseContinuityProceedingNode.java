package com.tcc.pjb.backend.service.casefile;

import java.time.Duration;
import java.time.Instant;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingRole;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

public record CaseContinuityProceedingNode(
        String proceedingKey,
        Long linkedProcessoId,
        boolean shadow,
        CaseProceedingStatus status,
        CaseContinuityTrack continuityTrack,
        CaseProceedingRole role,
        InstanceLevel instanceLevel,
        String court,
        String numeroUnificado,
        String parentProceedingKey,
        FaseProcessual sourceFaseProcessual,
        StatusProcesso sourceStatusProcesso,
        NivelSigilo secrecy,
        Instant lastSyncAt
) {
    public boolean isRootLike() {
        return role != null && role.isRootLike();
    }

    public boolean isKnowledgeBranch() {
        return (role != null && role.isKnowledgeBranch()) || (continuityTrack != null && continuityTrack.isKnowledgeState());
    }

    public boolean isExecutoryBranch() {
        return (role != null && role.isExecutoryBranch()) || (continuityTrack != null && continuityTrack.isExecutory());
    }

    public boolean isArchivedState() {
        return (continuityTrack != null && continuityTrack.isTerminalState()) || (role != null && role.isTerminalBranch());
    }

    public boolean isRecursalBranch() {
        return (continuityTrack != null && continuityTrack.isRecursalState()) || (role != null && role.isRecursalBranch());
    }

    public boolean belongsToTrack(CaseContinuityTrack track) {
        return track != null && continuityTrack == track;
    }

    public boolean isStale(Instant reference, Duration threshold) {
        if (lastSyncAt == null || reference == null || threshold == null || threshold.isNegative()) {
            return false;
        }
        return lastSyncAt.isBefore(reference.minus(threshold));
    }
}
