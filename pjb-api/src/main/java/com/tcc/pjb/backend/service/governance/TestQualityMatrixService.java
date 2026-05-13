package com.tcc.pjb.backend.service.governance;

import com.tcc.pjb.backend.model.dto.governance.StructuralAutoRemediationReportResponse;
import com.tcc.pjb.backend.model.dto.governance.StructuralGovernanceReportResponse;
import com.tcc.pjb.backend.model.dto.governance.TestQualityMatrixResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.flow.NationalCommunicationFlowService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class TestQualityMatrixService {

    private static final List<String> CRITICAL_MODULE_KEYS = List.of(
            "ProcessualOperationGuardService",
            "NationalExternalIntegrationGatewayService",
            "NationalCommunicationFlowService",
            "PrazoProcessualNacionalService",
            "OficialJusticaOperationalIntelligenceService",
            "TransitoJulgadoArquivamentoEngine",
            "SensitiveDataAccessControlService"
    );

    private final ApplicationContext applicationContext;
    private final StructuralGovernanceScannerService scannerService;

    public TestQualityMatrixService(ApplicationContext applicationContext,
                                    StructuralGovernanceScannerService scannerService) {
        this.applicationContext = Objects.requireNonNull(applicationContext);
        this.scannerService = Objects.requireNonNull(scannerService);
    }

    public TestQualityMatrixResponse verify() {
        StructuralGovernanceReportResponse summary = scannerService.scan();
        StructuralAutoRemediationReportResponse detailed = scannerService.scanDetailed();
        int processualServices = countProcessualServices();
        List<String> criticalModules = resolveCriticalModules();
        LinkedHashSet<String> risks = new LinkedHashSet<>();
        if (!detailed.duplicatePathMappings().isEmpty()) {
            risks.add("Rotas duplicadas comprometem contratos HTTP e precisam sair do pipeline de promoção.");
        }
        if (!detailed.requestBodiesMissingValidation().isEmpty()) {
            risks.add("Requests sem validação tipada enfraquecem testes de contrato e de segurança.");
        }
        if (!detailed.servicesWithoutController().isEmpty()) {
            risks.add("Serviços processuais sem surface controlada dificultam testes end-to-end rastreáveis.");
        }
        if (risks.isEmpty()) {
            risks.add("Snapshot estrutural sem bloqueadores críticos para a matriz de testes desta rodada.");
        }
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Manter ao menos um contrato HTTP por controller exposto.");
        recommendations.add("Manter ao menos um teste de integração por serviço processual com transição relevante.");
        recommendations.add("Manter suíte de carga para módulos críticos de fila, prazo, comunicação, integração e segurança.");
        recommendations.addAll(detailed.remediationPriorities());
        return new TestQualityMatrixResponse(
                summary.totalControllers(),
                processualServices,
                Math.max(1, summary.totalControllers()),
                Math.max(1, processualServices),
                Math.max(1, criticalModules.size()),
                criticalModules,
                List.copyOf(risks),
                List.copyOf(new LinkedHashSet<>(recommendations))
        );
    }

    private int countProcessualServices() {
        Map<String, Object> services = applicationContext.getBeansWithAnnotation(org.springframework.stereotype.Service.class);
        int total = 0;
        for (Object bean : services.values()) {
            Class<?> type = AopUtils.getTargetClass(bean);
            if (type == null || type.getPackage() == null) {
                continue;
            }
            if (type.getPackage().getName().contains(".service.processual.")) {
                total++;
            }
        }
        return total;
    }

    private List<String> resolveCriticalModules() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        Map<String, Object> services = applicationContext.getBeansWithAnnotation(org.springframework.stereotype.Service.class);
        for (Object bean : services.values()) {
            Class<?> type = AopUtils.getTargetClass(bean);
            if (type == null) {
                continue;
            }
            String simple = type.getSimpleName();
            if (CRITICAL_MODULE_KEYS.contains(simple)) {
                out.add(simple);
            }
        }
        return List.copyOf(out);
    }
}