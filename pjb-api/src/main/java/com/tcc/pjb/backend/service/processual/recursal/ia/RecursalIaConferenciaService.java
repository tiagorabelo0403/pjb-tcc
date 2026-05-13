package com.tcc.pjb.backend.service.processual.recursal.ia;

import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaStructuredAnalysis;
import com.tcc.pjb.backend.service.processual.calculo.CalculatorHelpMessages;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialDomainSupport;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialFrontendContractService;
import com.tcc.pjb.backend.service.processual.surface.ProcessualOperationalSurfaceFacadeService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RecursalIaConferenciaService {

    private static final String AGENTE = "IA_CONFERENCIA_RECURSAL_PJB";

    private final ProcessualOperationalSurfaceFacadeService facadeService;
    private final CalculoJudicialFrontendContractService frontendContractService;
    private final RecursalIaPlannerService plannerService;

    public RecursalIaConferenciaService(ProcessualOperationalSurfaceFacadeService facadeService,
                                        CalculoJudicialFrontendContractService frontendContractService,
                                        RecursalIaPlannerService plannerService) {
        this.facadeService = Objects.requireNonNull(facadeService);
        this.frontendContractService = Objects.requireNonNull(frontendContractService);
        this.plannerService = Objects.requireNonNull(plannerService);
    }

    public RecursalIaConferenciaResponse conferir(RecursalIaConferenciaRequest request) {
        List<String> pendencias = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();
        List<String> ajustes = new ArrayList<>();
        List<String> confirmacoes = new ArrayList<>();
        if (request == null || request.admissibilidade() == null) {
            pendencias.add("Informar o bloco base de admissibilidade recursal.");
            return response("PENDING_INPUT", false, pendencias, bloqueios, List.of(), ajustes, confirmacoes, null, null, request);
        }
        RecursalAdmissibilityRequest admissibilidade = request.admissibilidade();
        validate(admissibilidade, request, pendencias, bloqueios, confirmacoes);
        if (!bloqueios.isEmpty()) {
            return response("BLOCKED", false, pendencias, bloqueios, List.of(), ajustes, confirmacoes, null, admissibilidade, request);
        }
        if (!pendencias.isEmpty() || !request.executarAdmissibilidadeReal()) {
            if (!request.executarAdmissibilidadeReal()) {
                pendencias.add("Habilitar a execução real da admissibilidade para a IA concluir a conferência recursal.");
            }
            return response("PENDING_INPUT", false, pendencias, bloqueios, List.of(), ajustes, confirmacoes, null, admissibilidade, request);
        }
        RecursalAdmissibilityResponse result = facadeService.avaliarRecursal(admissibilidade);
        List<String> alertasCriticos = criticalAlerts(result, request);
        String status = alertasCriticos.isEmpty() ? "READY" : "READY_WITH_ALERTS";
        return response(status, true, pendencias, bloqueios, alertasCriticos, ajustes, confirmacoes, result, admissibilidade, request);
    }

    private void validate(RecursalAdmissibilityRequest request,
                          RecursalIaConferenciaRequest command,
                          List<String> pendencias,
                          List<String> bloqueios,
                          List<String> confirmacoes) {
        if (request.dataIntimacao() == null) {
            pendencias.add("Informar data de intimação.");
        }
        if (request.dataProtocolo() == null) {
            pendencias.add("Informar data de protocolo.");
        }
        if (request.dataIntimacao() != null && request.dataProtocolo() != null && request.dataProtocolo().isBefore(request.dataIntimacao())) {
            bloqueios.add("A data de protocolo não pode ser anterior à data de intimação.");
        }
        if (request.preparoRecolhido() && request.preparoDispensado()) {
            confirmacoes.add("Confirmar se o preparo foi recolhido de fato ou se a tese correta é dispensa de preparo.");
        }
        if (request.tribunalCodigo() == null || request.tribunalCodigo().isBlank()) {
            pendencias.add("Informar código do tribunal.");
        }
        if (command.exigirConferenciaCompetencia() && (request.uf() == null || request.uf().isBlank())) {
            pendencias.add("Informar UF para a checagem de competência e prazo local.");
        }
    }

    private List<String> criticalAlerts(RecursalAdmissibilityResponse result, RecursalIaConferenciaRequest command) {
        List<String> alerts = new ArrayList<>();
        if (command.exigirConferenciaTempestividade() && !result.tempestivo()) {
            alerts.add("Tempestividade negativa na conferência recursal.");
        }
        if (command.exigirConferenciaPreparo() && result.preparoExigido() && !result.preparoSatisfeito()) {
            alerts.add("Preparo exigido sem satisfação reconhecida.");
        }
        if (result.preclusao() != null && !"NENHUMA".equals(result.preclusao().name())) {
            alerts.add("Há indicativo de preclusão " + result.preclusao().name().toLowerCase() + ".");
        }
        if (result.stepUpRequired()) {
            alerts.add("A submissão recursal exige step-up de credencial.");
        }
        if (result.certificateRequired()) {
            alerts.add("A submissão recursal exige certificado ou credencial reforçada.");
        }
        if (result.alertas() != null) {
            result.alertas().stream().limit(5).forEach(alerts::add);
        }
        return List.copyOf(alerts);
    }

    private RecursalIaConferenciaResponse response(String status,
                                                   boolean executed,
                                                   List<String> pendencias,
                                                   List<String> bloqueios,
                                                   List<String> alertasCriticos,
                                                   List<String> ajustes,
                                                   List<String> confirmacoes,
                                                   RecursalAdmissibilityResponse result,
                                                   RecursalAdmissibilityRequest request,
                                                   RecursalIaConferenciaRequest command) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("agentCode", AGENTE);
        metadata.put("entryRoute", CalculoJudicialDomainSupport.recursalAiRoute());
        metadata.put("admissibilityRoute", CalculoJudicialDomainSupport.recursalAdmissibilityRoute());
        metadata.put("executionModel", "structured_case_memory_precedent_jurimetry_signature_protocol_planner_checker");
        metadata.put("executorLane", "aiMeshExecutor_via_pjbBurstExecutorService");
        metadata.put("schemaDiscipline", "strict_recursal_payload");
        metadata.put("verificationMode", "admissibility_real_plus_invalidation_guardrails");
        metadata.put("methods2026", CalculatorHelpMessages.recursalIaMessages());
        metadata.put("guardrails", CalculatorHelpMessages.iaGuardrails());
        if (command != null && command.pedidoUsuario() != null && !command.pedidoUsuario().isBlank()) {
            metadata.put("pedidoUsuario", command.pedidoUsuario().trim());
        }
        if (command != null) {
            metadata.put("processoId", command.processoId());
            metadata.put("tipoRecursoInformado", command.tipoRecursoInformado());
            metadata.put("aprofundarBaseProcessual", command.aprofundarBaseProcessual());
            metadata.put("aprofundarJurisprudencia", command.aprofundarJurisprudencia());
            metadata.put("aprofundarJurimetria", command.aprofundarJurimetria());
            metadata.put("considerarHistoricoPericial", command.considerarHistoricoPericial());
            metadata.put("exigirBlindagemAnulacao", command.exigirBlindagemAnulacao());
        }
        metadata.put("frontendContracts", frontendContractService.aiAgentsCatalog().get("conferenciaRecursal"));
        metadata.put("reviewPriority", result == null ? "pending" : reviewPriority(result));
        metadata.put("verificationScore", verificationScore(result, alertasCriticos));
        if (request != null) {
            metadata.put("tribunalCodigo", request.tribunalCodigo());
            metadata.put("recursoId", request.recursoId());
            metadata.put("preparoRecolhido", request.preparoRecolhido());
            metadata.put("preparoDispensado", request.preparoDispensado());
        }
        if (result != null) {
            metadata.put("riskLevel", result.riskLevel());
            metadata.put("routeKind", result.routeKind());
            metadata.put("connectorSystem", result.connectorSystem());
            metadata.put("protocolDesk", result.protocolDesk());
            metadata.put("supportDesk", result.supportDesk());
        }
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        String message = executed
                ? "A IA recursal executou a admissibilidade real do PJB e agregou memória processual, blindagem contra nulidade, jurisprudência, jurimetria, blueprint estruturado, contrarrazões, embargos especializados, assinatura e protocolo externo."
                : bloqueios.isEmpty()
                ? "A IA de conferência recursal organizou a pré-checagem, mas ainda depende de dados mínimos para rodar a admissibilidade real com segurança."
                : "A IA de conferência recursal travou a execução automática porque encontrou incoerências que comprometem a análise segura do recurso.";
        var analiseEstruturada = request == null
                ? new com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaStructuredAnalysis(
                        Map.of("status", status),
                        Map.of(),
                        List.copyOf(alertasCriticos),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of())
                : plannerService.analyze(command == null
                ? new RecursalIaConferenciaRequest(request, null, false, false, false, false, request.context().processoId(), request.recursoId(), request.context().ramo().name(), request.context().rito().name(), true, true, true, true, true)
                : command,
                result);
        return new RecursalIaConferenciaResponse(
                AGENTE,
                status,
                executed,
                message,
                List.copyOf(pendencias),
                List.copyOf(bloqueios),
                List.copyOf(alertasCriticos),
                List.copyOf(ajustes),
                List.copyOf(confirmacoes),
                Collections.unmodifiableMap(metadata),
                result,
                analiseEstruturada,
                Instant.now()
        );
    }

    private String reviewPriority(RecursalAdmissibilityResponse result) {
        if (result == null) {
            return "pending";
        }
        if (!result.tempestivo() || (result.preparoExigido() && !result.preparoSatisfeito())) {
            return "critical";
        }
        if ("HIGH".equalsIgnoreCase(result.riskLevel()) || result.automaticSuspensiveEffect() || "SUSPENSIVO".equalsIgnoreCase(result.effectMode())) {
            return "high";
        }
        return "standard";
    }

    private BigDecimal verificationScore(RecursalAdmissibilityResponse result, List<String> alertasCriticos) {
        if (result == null) {
            return new BigDecimal("0.40");
        }
        BigDecimal score = new BigDecimal("1.00");
        if (!result.tempestivo()) {
            score = score.subtract(new BigDecimal("0.35"));
        }
        if (result.preparoExigido() && !result.preparoSatisfeito()) {
            score = score.subtract(new BigDecimal("0.25"));
        }
        if (result.preclusao() != null && !"NENHUMA".equals(result.preclusao().name())) {
            score = score.subtract(new BigDecimal("0.20"));
        }
        if (alertasCriticos != null && !alertasCriticos.isEmpty()) {
            score = score.subtract(new BigDecimal(Math.min(15, alertasCriticos.size() * 3)).movePointLeft(2));
        }
        return score.max(new BigDecimal("0.05"));
    }
}
