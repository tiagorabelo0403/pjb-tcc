package com.tcc.pjb.backend.core.processo.unificado.application;

import com.tcc.pjb.backend.core.processo.anomalia.application.ProcessoAnomaliaGovernancaApplicationService;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaGovernancaAggregate;
import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelRiscoMalhaApplicationService;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelRiscoMalhaAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimePreparationApplicationService;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaObservabilidadeAggregate;
import com.tcc.pjb.backend.model.dto.processual.observability.business.ProcessBusinessObservabilityResponse;
import com.tcc.pjb.backend.service.observabilidade.AlertaOperacional;
import com.tcc.pjb.backend.service.observabilidade.NationalDashboard;
import com.tcc.pjb.backend.service.observabilidade.NationalObservabilityService;
import com.tcc.pjb.backend.service.processual.observability.business.ProcessBusinessObservabilityService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoMalhaObservabilidadeApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService;
    private final ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService;
    private final ProcessoPainelRiscoMalhaApplicationService processoPainelRiscoMalhaApplicationService;
    private final ProcessoAnomaliaGovernancaApplicationService processoAnomaliaGovernancaApplicationService;
    private final NationalObservabilityService nationalObservabilityService;
    private final ProcessBusinessObservabilityService processBusinessObservabilityService;

    public ProcessoMalhaObservabilidadeApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                          ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService,
                                                          ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService,
                                                          ProcessoPainelRiscoMalhaApplicationService processoPainelRiscoMalhaApplicationService,
                                                          ProcessoAnomaliaGovernancaApplicationService processoAnomaliaGovernancaApplicationService,
                                                          ObjectProvider<NationalObservabilityService> nationalObservabilityServiceProvider,
                                                          ObjectProvider<ProcessBusinessObservabilityService> processBusinessObservabilityServiceProvider) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoRuntimePreparationApplicationService = Objects.requireNonNull(processoRuntimePreparationApplicationService);
        this.processoMalhaNacionalApplicationService = Objects.requireNonNull(processoMalhaNacionalApplicationService);
        this.processoPainelRiscoMalhaApplicationService = Objects.requireNonNull(processoPainelRiscoMalhaApplicationService);
        this.processoAnomaliaGovernancaApplicationService = Objects.requireNonNull(processoAnomaliaGovernancaApplicationService);
        this.nationalObservabilityService = nationalObservabilityServiceProvider.getIfAvailable();
        this.processBusinessObservabilityService = processBusinessObservabilityServiceProvider.getIfAvailable();
    }

    @Transactional(readOnly = true)
    public ProcessoMalhaObservabilidadeAggregate detalhar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoRuntimePreparationAggregate runtime = processoRuntimePreparationApplicationService.avaliar(contexto);
        ProcessoMalhaNacionalAggregate malha = processoMalhaNacionalApplicationService.detalhar(processoId);
        ProcessoPainelRiscoMalhaAggregate painelRisco = processoPainelRiscoMalhaApplicationService.detalhar(processoId);
        ProcessoAnomaliaGovernancaAggregate governanca = processoAnomaliaGovernancaApplicationService.escalarSeNecessario(processoId);
        NationalDashboard dashboard = safeDashboard();
        ProcessBusinessObservabilityResponse processual = safeProcessualSnapshot();
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.addAll(runtime.alertas());
        fundamentos.addAll(malha.fundamentos());
        fundamentos.addAll(painelRisco.fundamentos());
        fundamentos.addAll(governanca.fundamentos());
        fundamentos.add("runtime.pronto=" + runtime.prontoParaMalhaCompleta());
        fundamentos.add("runtime.prontidao=" + runtime.integrationStatus().percentualProntidao());
        if (dashboard != null) {
            dashboard.alertas().stream().map(AlertaOperacional::mensagem).forEach(alertas::add);
            alertas.addAll(dashboard.filasComBacklog().stream().limit(4).map(fila -> "Backlog nacional na fila " + fila).toList());
        }
        if (processual != null) {
            alertas.addAll(processual.alertas());
            fundamentos.addAll(processual.alertas());
        }
        alertas.addAll(runtime.alertas());
        if (malha.travaDistribuicaoOuFluxo()) {
            alertas.add("A malha nacional identificou bloqueio operacional imediato no fluxo do processo.");
        }
        if (governanca.exigiuPersistencia()) {
            alertas.add("A governança exigiu persistência e trilha reforçada de segurança para o caso.");
        }
        String saudeInstitucional = dashboard == null ? "DESACOPLADA" : dashboard.nivelSaude();
        if (!runtime.prontoParaMalhaCompleta() && "DESACOPLADA".equals(saudeInstitucional)) {
            saudeInstitucional = "ASSISTIDA";
        }
        return new ProcessoMalhaObservabilidadeAggregate(
                processoId,
                contexto.numeroReferencia(),
                saudeInstitucional,
                painelRisco.statusGeral(),
                painelRisco.scoreGlobal(),
                dashboard == null ? 0L : dashboard.workItemsPendentes(),
                processual == null ? 0L : processual.workItemsVencidos(),
                dashboard == null ? List.of() : dashboard.filasComBacklog(),
                List.copyOf(alertas.stream().limit(24).toList()),
                List.copyOf(fundamentos.stream().limit(120).toList()),
                Instant.now()
        );
    }

    private NationalDashboard safeDashboard() {
        if (nationalObservabilityService == null) {
            return null;
        }
        try {
            return nationalObservabilityService.nationalDashboard();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private ProcessBusinessObservabilityResponse safeProcessualSnapshot() {
        if (processBusinessObservabilityService == null) {
            return null;
        }
        try {
            return processBusinessObservabilityService.snapshot();
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
