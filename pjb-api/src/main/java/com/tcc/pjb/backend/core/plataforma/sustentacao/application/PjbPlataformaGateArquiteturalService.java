package com.tcc.pjb.backend.core.plataforma.sustentacao.application;

import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoEixo;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceIssue;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityIssue;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import com.tcc.pjb.backend.service.procedural.ProceduralArchitectureSanityService;
import com.tcc.pjb.backend.service.procedural.ProceduralLegacyBoundaryAuditService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbPlataformaGateArquiteturalService {

    private final PjbCodebaseSanityApplicationService codebaseSanityApplicationService;
    private final PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService;
    private final ProceduralArchitectureSanityService proceduralArchitectureSanityService;
    private final ProceduralLegacyBoundaryAuditService proceduralLegacyBoundaryAuditService;
    private final BuildGateGovernanceService buildGateGovernanceService;

    public PjbPlataformaGateArquiteturalService(PjbCodebaseSanityApplicationService codebaseSanityApplicationService,
                                                PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService,
                                                ProceduralArchitectureSanityService proceduralArchitectureSanityService,
                                                ProceduralLegacyBoundaryAuditService proceduralLegacyBoundaryAuditService,
                                                BuildGateGovernanceService buildGateGovernanceService) {
        this.codebaseSanityApplicationService = Objects.requireNonNull(codebaseSanityApplicationService);
        this.apiSurfaceSanityApplicationService = Objects.requireNonNull(apiSurfaceSanityApplicationService);
        this.proceduralArchitectureSanityService = Objects.requireNonNull(proceduralArchitectureSanityService);
        this.proceduralLegacyBoundaryAuditService = Objects.requireNonNull(proceduralLegacyBoundaryAuditService);
        this.buildGateGovernanceService = Objects.requireNonNull(buildGateGovernanceService);
    }

    public PjbPlataformaSustentacaoEixo avaliar() {
        PjbCodebaseSanityAggregate codebase = codebaseSanityApplicationService.auditar();
        PjbApiSurfaceSanityAggregate apiSurface = apiSurfaceSanityApplicationService.auditar();
        ProceduralArchitectureSanityService.SanityReport architecture = proceduralArchitectureSanityService.report();
        ProceduralLegacyBoundaryAuditService.BoundaryReport boundary = proceduralLegacyBoundaryAuditService.report();
        BuildGateEvaluationResponse buildGate = buildGateGovernanceService.evaluate();
        int score = average(
                codebase.score(),
                apiSurface.score(),
                architecture.healthy() ? 95 : 58,
                boundary.clean() ? 92 : 52,
                buildGate.approved() ? 96 : 54
        );
        LinkedHashSet<String> sinais = new LinkedHashSet<>();
        sinais.add(codebase.resumo());
        sinais.add("apiSurfaceScore=" + apiSurface.score());
        sinais.add("architectureHealthy=" + architecture.healthy());
        sinais.add("legacyBoundaryClean=" + boundary.clean());
        sinais.add("buildGateApproved=" + buildGate.approved());
        sinais.add("routeGateApproved=" + buildGate.routeGateApproved());
        sinais.add("validationGateApproved=" + buildGate.validationGateApproved());
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>(buildGate.outstandingIssues());
        bloqueadores.addAll(architecture.issues());
        bloqueadores.addAll(boundary.violations().stream().map(ProceduralLegacyBoundaryAuditService.BoundaryViolation::reason).toList());
        bloqueadores.addAll(codebase.issues().stream().map(PjbCodebaseSanityIssue::codigo).toList());
        bloqueadores.addAll(apiSurface.issues().stream().map(PjbApiSurfaceIssue::codigo).toList());
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>(buildGate.nextActions());
        if (!architecture.healthy()) {
            proximasAcoes.add("NORMALIZAR_CATALOGO_PROCEDURAL_E_CONNECTORES_PREFERIDOS");
        }
        if (!boundary.clean()) {
            proximasAcoes.add("EXPULSAR_REFERENCIAS_DIRETAS_A_ENUMS_LEGADOS_FORA_DA_CAMADA_CANONICA");
        }
        LinkedHashMap<String, Object> evidencias = new LinkedHashMap<>();
        evidencias.put("codebaseScore", codebase.score());
        evidencias.put("codebaseIssues", codebase.issues().size());
        evidencias.put("apiSurfaceScore", apiSurface.score());
        evidencias.put("apiSurfaceIssues", apiSurface.issues().size());
        evidencias.put("architectureIssues", architecture.issues().size());
        evidencias.put("legacyBoundaryViolations", boundary.violations().size());
        evidencias.put("buildGateOutstandingIssues", buildGate.totalOutstandingIssues());
        return eixo(
                "gate.arquitetural",
                "Gate arquitetural, surface e build",
                score,
                codebase.limpo() && apiSurface.limpo() && architecture.healthy() && boundary.clean() && buildGate.approved(),
                sinais,
                bloqueadores,
                proximasAcoes,
                evidencias
        );
    }

    private int average(int... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return Math.max(0, Math.min(100, total / values.length));
    }

    private PjbPlataformaSustentacaoEixo eixo(String codigo,
                                              String titulo,
                                              int score,
                                              boolean pronto,
                                              LinkedHashSet<String> sinais,
                                              LinkedHashSet<String> bloqueadores,
                                              LinkedHashSet<String> proximasAcoes,
                                              Map<String, Object> evidencias) {
        return new PjbPlataformaSustentacaoEixo(
                codigo,
                titulo,
                Math.max(0, Math.min(100, score)),
                pronto ? "PRONTO" : score >= 70 ? "PARCIAL" : "BLOQUEADO",
                pronto,
                List.copyOf(sinais),
                List.copyOf(bloqueadores),
                List.copyOf(proximasAcoes),
                cleanMap(evidencias)
        );
    }

    private Map<String, Object> cleanMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    out.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(out);
    }
}
