package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoValidateResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer.LaianeLawyerAttachmentValidationResponse;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;

@Service
public class ProtocolDryRunService {

    public ProtocolDryRunReport simulateRequest(LaianePeticaoAssistRequest request,
                                                CanonicalContext canonical,
                                                String ritoName,
                                                LaianePeticaoValidateResponse validator,
                                                LaianeLawyerAttachmentValidationResponse attachments,
                                                DynamicCompetenceDistributionResponse competencia,
                                                TetoProcessualService.DiagnosticoTetoProcessual teto,
                                                LegalCoherenceReport coherenceReport,
                                                double readinessScore) {
        Objects.requireNonNull(request, "request");
        List<ProtocolDryRunReport.Check> checks = new ArrayList<>();
        LinkedHashSet<String> nextActions = new LinkedHashSet<>();

        checks.add(check("CANONICAL_CONTEXT", "Contexto canônico", canonical != null && canonical.rito() != null && canonical.classeTpuCodigo() != null, "CRITICAL", canonical != null ? "Contexto canônico consolidado." : "Contexto canônico incompleto."));
        checks.add(check("PETITION_VALIDATION", "Validação estrutural da peça", validator != null && validator.isOk(), "CRITICAL", validator != null && validator.isOk() ? "A peça está estruturalmente válida." : "Existem erros estruturais bloqueantes na peça."));
        checks.add(check("ATTACHMENTS", "Schema documental", attachments == null || attachments.isOk(), attachments == null ? "MEDIUM" : "CRITICAL", attachments == null || attachments.isOk() ? "O conjunto documental está aderente ao rito." : "Faltam documentos obrigatórios para o protocolo."));
        checks.add(check("COMPETENCE", "Competência e unidade julgadora", competencia != null && competencia.distribuicaoAutomatica(), competencia == null ? "CRITICAL" : "HIGH", competencia != null && competencia.distribuicaoAutomatica() ? "Competência resolvida com distribuição automática." : "Competência ainda depende de revisão humana ou não foi resolvida."));
        checks.add(check("ECONOMIC_LIMIT", "Teto procedimental", teto == null || !teto.bloqueante(), teto != null && teto.bloqueante() ? "CRITICAL" : "MEDIUM", teto == null || !teto.bloqueante() ? "Sem bloqueio econômico impeditivo." : "O valor da causa bloqueia o fluxo pretendido."));
        checks.add(check("COHERENCE", "Coerência jurídica", coherenceReport == null || !coherenceReport.blocking(), coherenceReport != null && coherenceReport.blocking() ? "CRITICAL" : "HIGH", coherenceReport == null || !coherenceReport.blocking() ? "Não há incoerência jurídica bloqueante adicional." : "Foram detectadas incoerências jurídicas bloqueantes."));

        boolean apto = checks.stream().allMatch(ProtocolDryRunReport.Check::passed) && readinessScore >= 0.80d;
        if (!apto) {
            checks.add(check("READINESS_SCORE", "Índice de prontidão", false, "MEDIUM", "A prontidão consolidada ficou abaixo do limiar mínimo para protocolo assistido."));
        } else {
            checks.add(check("READINESS_SCORE", "Índice de prontidão", true, "LOW", "A prontidão consolidada é suficiente para protocolo assistido."));
        }

        if (validator != null && !validator.isOk()) {
            nextActions.add("Corrigir a estrutura da petição e regenerar a versão final.");
        }
        if (attachments != null && !attachments.isOk()) {
            nextActions.add("Completar os anexos obrigatórios do rito e reexecutar o ensaio de protocolo.");
        }
        if (competencia == null || !competencia.distribuicaoAutomatica()) {
            nextActions.add("Fechar a competência judicial com revisão humana ou dados adicionais antes do protocolo real.");
        }
        if (teto != null && teto.bloqueante()) {
            nextActions.add("Recalibrar valor da causa, rito ou competência para remover o bloqueio econômico.");
        }
        if (coherenceReport != null) {
            nextActions.addAll(coherenceReport.strategicRecommendations());
        }
        if (nextActions.isEmpty()) {
            nextActions.add("Fluxo apto para protocolo assistido com monitoramento normal.");
        }

        Map<String, Object> diagnostics = PayloadMaps.ofEntries(
                "ritoName", ritoName,
                "classeTpu", canonical != null ? canonical.classeTpuCodigo() : null,
                "ramoDireito", canonical != null ? canonical.ramoDireito() : null,
                "tribunalCodigo", competencia != null ? competencia.tribunalCodigo() : canonical != null ? canonical.tribunalCodigo() : null,
                "unidadeCodigo", competencia != null ? competencia.unidadeCodigo() : null,
                "readinessScore", readinessScore,
                "requestKind", request.getKind()
        );
        String status = apto ? "READY_FOR_REAL_PROTOCOL" : checks.stream().anyMatch(c -> !c.passed() && "CRITICAL".equals(c.severity())) ? "BLOCKED_IN_DRY_RUN" : "REVIEW_REQUIRED_IN_DRY_RUN";
        return new ProtocolDryRunReport(status, apto, List.copyOf(checks), List.copyOf(nextActions), diagnostics);
    }

    public ProtocolDryRunReport simulateProcess(String ritoName,
                                                Long processoId,
                                                String numeroUnificado,
                                                RitoPlanDto ritoPlan,
                                                boolean hasEvidence,
                                                LegalCoherenceReport coherenceReport) {
        List<ProtocolDryRunReport.Check> checks = new ArrayList<>();
        checks.add(check("PROCESS_ID", "Identidade do processo", processoId != null, "CRITICAL", processoId != null ? "Processo identificado no twin." : "Processo sem identidade persistida."));
        checks.add(check("RITO", "Rito efetivo", ritoName != null && !ritoName.isBlank(), "CRITICAL", ritoName != null && !ritoName.isBlank() ? "Rito efetivo consolidado." : "Rito efetivo ausente."));
        checks.add(check("WORKFLOW_BLOCKING", "Checklist bloqueante", ritoPlan == null || ritoPlan.getBlockingOpen() == null || ritoPlan.getBlockingOpen().isEmpty(), "HIGH", ritoPlan == null || ritoPlan.getBlockingOpen() == null || ritoPlan.getBlockingOpen().isEmpty() ? "Sem work items bloqueantes em aberto." : "Há work items bloqueantes abertos no rito."));
        checks.add(check("EVIDENCE", "Ancoragem jurisprudencial", hasEvidence, "LOW", hasEvidence ? "Foram encontrados precedentes compatíveis para sustentação." : "Não foram localizados precedentes suficientes na trilha atual."));
        checks.add(check("COHERENCE", "Coerência do processo", coherenceReport == null || !coherenceReport.blocking(), coherenceReport != null && coherenceReport.blocking() ? "CRITICAL" : "HIGH", coherenceReport == null || !coherenceReport.blocking() ? "Sem incoerência processual bloqueante adicional." : "O twin detectou incoerência processual bloqueante."));
        boolean apto = checks.stream().allMatch(ProtocolDryRunReport.Check::passed);
        LinkedHashSet<String> next = new LinkedHashSet<>();
        if (!apto) {
            next.add("Saneie as pendências bloqueantes antes da próxima transição do processo.");
        }
        if (coherenceReport != null) {
            next.addAll(coherenceReport.strategicRecommendations());
        }
        if (next.isEmpty()) {
            next.add("O processo está pronto para seguir para a próxima etapa do rito.");
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("processoId", processoId);
        diagnostics.put("numeroUnificado", numeroUnificado);
        diagnostics.put("ritoName", ritoName);
        return new ProtocolDryRunReport(
                apto ? "PROCESS_READY" : "PROCESS_REVIEW_REQUIRED",
                apto,
                List.copyOf(checks),
                List.copyOf(next),
                PayloadMaps.copyWithoutNulls(diagnostics)
        );
    }

    private ProtocolDryRunReport.Check check(String code, String title, boolean passed, String severity, String message) {
        return new ProtocolDryRunReport.Check(code, title, severity, passed, message);
    }
}
