package com.tcc.pjb.backend.service.casefile;

import static org.assertj.core.api.Assertions.assertThat;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalRelationType;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingRole;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaseContinuitySnapshotTest {
    @Test
    void deveDetectarRamificacoesExecutivasEArquivadas() {
        CaseContinuitySnapshot snapshot = new CaseContinuitySnapshot(
                10L,
                100L,
                100L,
                "ROOT",
                CaseContinuityTrack.EXECUCAO,
                List.of(
                        new CaseContinuityProceedingNode("ROOT", 100L, false, CaseProceedingStatus.ACTIVE, CaseContinuityTrack.CONHECIMENTO, CaseProceedingRole.ROOT, InstanceLevel.FIRST_INSTANCE, "TJCE", "1", null, FaseProcessual.CONHECIMENTO, StatusProcesso.EM_ANDAMENTO, NivelSigilo.PUBLICO, Instant.now()),
                        new CaseContinuityProceedingNode("EXEC", 100L, false, CaseProceedingStatus.ACTIVE, CaseContinuityTrack.EXECUCAO, CaseProceedingRole.EXECUCAO, InstanceLevel.FIRST_INSTANCE, "TJCE", "1", "ROOT", FaseProcessual.EXECUCAO, StatusProcesso.CUMPRIMENTO_SENTENCA, NivelSigilo.PUBLICO, Instant.now()),
                        new CaseContinuityProceedingNode("ARCH", 100L, false, CaseProceedingStatus.CLOSED, CaseContinuityTrack.ARQUIVADO, CaseProceedingRole.TERMINAL, InstanceLevel.FIRST_INSTANCE, "TJCE", "1", "ROOT", FaseProcessual.EXECUCAO, StatusProcesso.ARQUIVADO, NivelSigilo.PUBLICO, Instant.now())
                ),
                List.of(new CaseContinuityEdgeLink("ROOT", "EXEC", RecursalRelationType.EXECUTION_CONTINUATION, LegalAppealType.OUTRO)),
                List.of()
        );

        assertThat(snapshot.isUnifiedRoot()).isTrue();
        assertThat(snapshot.hasExecutoryBranch()).isTrue();
        assertThat(snapshot.hasArchivedBranch()).isTrue();
        assertThat(snapshot.isRootRequestedProcesso()).isTrue();
        assertThat(snapshot.requiresAttention()).isFalse();
    }
}
