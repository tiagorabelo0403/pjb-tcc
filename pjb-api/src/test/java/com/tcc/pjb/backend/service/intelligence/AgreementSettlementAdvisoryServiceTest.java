package com.tcc.pjb.backend.service.intelligence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgreementSettlementAdvisoryServiceTest {

    private final ProcessoRitoSnapshotService processoRitoSnapshotService = mock(ProcessoRitoSnapshotService.class);
    private final ProcessMaterialDossierService processMaterialDossierService = mock(ProcessMaterialDossierService.class);
    private final ProcessMaterialStrategyService processMaterialStrategyService = mock(ProcessMaterialStrategyService.class);
    private final SettlementAdvisoryService settlementAdvisoryService = mock(SettlementAdvisoryService.class);
    private final AgreementSettlementAdvisoryService service = new AgreementSettlementAdvisoryService(
            processoRitoSnapshotService, processMaterialDossierService, processMaterialStrategyService, settlementAdvisoryService);

    @Test
    void buildOrquestraDossieEstrategiaEParecerComRito() {
        Processo processo = mock(Processo.class);
        ProcessMaterialDossierReport dossier = mock(ProcessMaterialDossierReport.class);
        when(dossier.settlementLevers()).thenReturn(List.of("lever"));
        ProcessMaterialStrategyReport strategy = mock(ProcessMaterialStrategyReport.class);
        when(strategy.negotiationGuardrails()).thenReturn(List.of("guard"));
        ProcessoRitoSnapshotService.ProcessoRitoSnapshot snapshot = mock(ProcessoRitoSnapshotService.ProcessoRitoSnapshot.class);
        when(snapshot.ritoCode()).thenReturn("COMUM_ORDINARIO");
        SettlementAdvisoryReport report = mock(SettlementAdvisoryReport.class);

        when(processMaterialDossierService.analyzeProcess(eq(processo), anyList())).thenReturn(dossier);
        when(processMaterialStrategyService.analyzeProcess(eq(processo), eq(dossier), anyList())).thenReturn(strategy);
        when(processoRitoSnapshotService.resolve(processo, null)).thenReturn(snapshot);
        when(settlementAdvisoryService.analyze(eq(processo), eq("COMUM_ORDINARIO"), isNull(), anyList(), isNull())).thenReturn(report);

        SettlementAdvisoryReport out = service.build(processo, null);

        assertThat(out).isSameAs(report);
        verify(processMaterialStrategyService).analyzeProcess(eq(processo), eq(dossier), anyList());
        verify(settlementAdvisoryService).analyze(eq(processo), eq("COMUM_ORDINARIO"), isNull(), anyList(), isNull());
    }
}
