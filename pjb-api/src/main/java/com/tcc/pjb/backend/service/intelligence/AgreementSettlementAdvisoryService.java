package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AgreementSettlementAdvisoryService {

    private final ProcessoRitoSnapshotService processoRitoSnapshotService;
    private final ProcessMaterialDossierService processMaterialDossierService;
    private final ProcessMaterialStrategyService processMaterialStrategyService;
    private final SettlementAdvisoryService settlementAdvisoryService;

    public AgreementSettlementAdvisoryService(ProcessoRitoSnapshotService processoRitoSnapshotService,
                                              ProcessMaterialDossierService processMaterialDossierService,
                                              ProcessMaterialStrategyService processMaterialStrategyService,
                                              SettlementAdvisoryService settlementAdvisoryService) {
        this.processoRitoSnapshotService = Objects.requireNonNull(processoRitoSnapshotService);
        this.processMaterialDossierService = Objects.requireNonNull(processMaterialDossierService);
        this.processMaterialStrategyService = Objects.requireNonNull(processMaterialStrategyService);
        this.settlementAdvisoryService = Objects.requireNonNull(settlementAdvisoryService);
    }

    public SettlementAdvisoryReport build(Processo processo, PropostaAcordo proposta) {
        List<String> baseSignals = buildNegotiationSignals(processo);
        ProcessMaterialDossierReport dossier = processMaterialDossierService.analyzeProcess(processo, baseSignals);
        ProcessMaterialStrategyReport strategy = processMaterialStrategyService.analyzeProcess(processo, dossier, baseSignals);
        ArrayList<String> mergedSignals = new ArrayList<>(baseSignals);
        mergedSignals.addAll(safeList(dossier.settlementLevers()));
        mergedSignals.addAll(safeList(strategy.negotiationGuardrails()));
        return settlementAdvisoryService.analyze(
                processo,
                processoRitoSnapshotService.resolve(processo, null).ritoCode(),
                proposta != null ? proposta.getValorAcordo() : null,
                List.copyOf(mergedSignals.stream().filter(Objects::nonNull).filter(v -> !v.isBlank()).distinct().toList()),
                null
        );
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<String> buildNegotiationSignals(Processo processo) {
        ArrayList<String> signals = new ArrayList<>();
        if (processo.getFaseAtual() != null) {
            signals.add("Fase atual: " + processo.getFaseAtual().name());
        }
        if (processo.getStatusProcesso() != null) {
            signals.add("Status do processo: " + processo.getStatusProcesso().name());
        }
        if (processo.getResultadoFinal() != null && !processo.getResultadoFinal().isBlank()) {
            signals.add("Resultado atual: " + processo.getResultadoFinal().trim());
        }
        if (processo.getAssunto() != null && !processo.getAssunto().isBlank()) {
            signals.add("Assunto: " + processo.getAssunto().trim());
        }
        return List.copyOf(signals);
    }
}
