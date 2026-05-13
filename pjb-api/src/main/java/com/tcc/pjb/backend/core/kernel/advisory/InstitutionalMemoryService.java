package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;

@Service
public class InstitutionalMemoryService {

    public InstitutionalMemoryReport analyzeRequest(LaianePeticaoAssistRequest request,
                                                    CanonicalContext canonical,
                                                    String ritoName,
                                                    DynamicCompetenceDistributionResponse competencia,
                                                    LegalCoherenceReport coherence,
                                                    ProcessIntegrityRadarReport radar,
                                                    StrategicCopilotReport copilot) {
        Objects.requireNonNull(request, "request");
        Set<String> patterns = new LinkedHashSet<>();
        Set<String> failures = new LinkedHashSet<>();
        Set<String> playbooks = new LinkedHashSet<>();
        Set<String> officeAlerts = new LinkedHashSet<>();
        Set<String> memoryKeys = new LinkedHashSet<>();
        String canonicalClasseTpu = canonical != null ? canonical.classeTpuCodigo() : null;
        String canonicalRamoDireito = canonical != null ? canonical.ramoDireito() : null;
        double confidence = 0.57d;

        addWhen(patterns, !blank(ritoName), "Assinatura procedimental consolidada em rito nominal estável.");
        addWhen(patterns, !blank(canonicalClasseTpu), "Classe TPU consolidada antes do protocolo assistido.");
        addWhen(patterns, !blank(canonicalRamoDireito), "Ramo jurídico coerente com o núcleo do caso.");
        addWhen(memoryKeys, !blank(ritoName), "rito:" + ritoName);
        addWhen(memoryKeys, !blank(canonicalClasseTpu), "classeTpu:" + canonicalClasseTpu);
        addWhen(memoryKeys, !blank(request.getMateriaPrincipal()), "area:" + request.getMateriaPrincipal().trim().toUpperCase());

        if (coherence != null && coherence.blocking()) {
            failures.add("Coerência jurídica bloqueante exige saneamento antes de reaproveitar a estratégia do escritório.");
            playbooks.add("Executar trilha de saneamento da tese, prova e competência antes de gerar protocolo.");
            officeAlerts.add("Não reutilizar modelo-base sem revisão humana do núcleo fático e documental.");
            confidence -= 0.12d;
        } else {
            patterns.add("Peça pode alimentar playbook institucional de peticionamento assistido.");
            confidence += 0.06d;
        }

        if (radar != null && radar.blocking()) {
            failures.add("Radar de integridade sinalizou risco relevante de prazo, nulidade ou defeito recursal.");
            officeAlerts.add("Acionar conferência de integridade antes de expor a peça como modelo reutilizável.");
            confidence -= 0.10d;
        }

        if (competencia == null || !competencia.distribuicaoAutomatica()) {
            failures.add("Competência ainda não fechou com distribuição automática segura.");
            playbooks.add("Reforçar coleta territorial e material para consolidar competência e unidade julgadora.");
            confidence -= 0.08d;
        } else {
            patterns.add("Destino judicial fechou com trilha de distribuição automática utilizável como memória institucional.");
            confidence += 0.05d;
        }

        if (truthy(request.getRequerLiminar())) {
            patterns.add("Caso com urgência recomenda playbook probatório e de reversibilidade dedicado.");
            playbooks.add("Separar narrativa de urgência, risco de dano e reversibilidade em bloco autônomo.");
            memoryKeys.add("lane:urgent-relief");
        }

        if (truthy(request.getRequerJuizadoEspecial())) {
            patterns.add("Contexto aderente a trilha simplificada de juizado e composição antecipada.");
            playbooks.add("Priorizar cálculo simples, prova documental concentrada e proposta conciliatória objetiva.");
            memoryKeys.add("lane:juizado");
        }

        if (blank(request.getCpfCnpjAutor()) || blank(request.getCpfCnpjReu())) {
            failures.add("Qualificação incompleta reduz reaproveitamento institucional e aumenta rejeição em borda operacional.");
            officeAlerts.add("Exigir check obrigatório de qualificação das partes antes de transformar a peça em playbook do escritório.");
            confidence -= 0.06d;
        }

        if (copilot != null && !copilot.watchpoints().isEmpty()) {
            copilot.watchpoints().stream().limit(4).forEach(officeAlerts::add);
            confidence += 0.03d;
        }

        String status = failures.isEmpty() ? "INSTITUTIONAL_MEMORY_READY" : "INSTITUTIONAL_MEMORY_REVIEW";
        return new InstitutionalMemoryReport(
                "PETITION_ASSIST",
                status,
                round(clamp(confidence)),
                List.copyOf(patterns),
                List.copyOf(failures),
                List.copyOf(playbooks),
                List.copyOf(officeAlerts),
                List.copyOf(memoryKeys),
                PayloadMaps.ofEntries(
                        "scope", "PETITION_ASSIST",
                        "ritoName", ritoName,
                        "classeTpu", canonical != null ? canonical.classeTpuCodigo() : null,
                        "competenciaStatus", competencia != null ? competenceStatus(competencia) : null,
                        "coherenceBlocking", coherence != null && coherence.blocking(),
                        "integrityStatus", radar != null ? radar.status() : null
                )
        );
    }

    public InstitutionalMemoryReport analyzeProcess(Processo processo,
                                                    String ritoName,
                                                    RitoPlanDto ritoPlan,
                                                    ProcessIntegrityRadarReport radar,
                                                    StrategicCopilotReport copilot,
                                                    SettlementAdvisoryReport settlement) {
        Objects.requireNonNull(processo, "processo");
        Set<String> patterns = new LinkedHashSet<>();
        Set<String> failures = new LinkedHashSet<>();
        Set<String> playbooks = new LinkedHashSet<>();
        Set<String> officeAlerts = new LinkedHashSet<>();
        Set<String> memoryKeys = new LinkedHashSet<>();
        double confidence = 0.63d;

        Long processoId = processo.getId();
        String faseAtual = processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null;

        addWhen(memoryKeys, processoId != null, "processo:" + processoId);
        addWhen(memoryKeys, !blank(processo.getNumeroUnificado()), "numero:" + processo.getNumeroUnificado());
        addWhen(memoryKeys, !blank(ritoName), "rito:" + ritoName);
        addWhen(memoryKeys, !blank(faseAtual), "fase:" + faseAtual);

        addWhen(patterns, !blank(ritoName), "Rito efetivo do processo está consolidado para reaproveitamento de estratégia institucional.");
        addWhen(patterns, !blank(faseAtual), "Fase atual registrada permite compor playbook evolutivo por etapa do processo.");

        if (ritoPlan != null && ritoPlan.getBlockingOpen() != null && !ritoPlan.getBlockingOpen().isEmpty()) {
            failures.add("Workflow contém pendências bloqueantes que reduzem a qualidade da memória operacional do caso.");
            playbooks.add("Associar backlog de work items ao playbook de transição de fase antes de reutilizar a trilha.");
            confidence -= 0.10d;
        } else {
            patterns.add("Workflow está livre de bloqueios abertos relevantes para captura de aprendizado institucional.");
            confidence += 0.04d;
        }

        if (radar != null && radar.blocking()) {
            failures.add("Radar de integridade detectou risco material que impede consolidar a trilha como modelo seguro.");
            officeAlerts.addAll(radar.nextActions());
            confidence -= 0.12d;
        }

        if (settlement != null && settlement.window() != null && settlement.window().favorable()) {
            patterns.add("Caso apresenta janela negocial reaproveitável para matriz de autocomposição institucional.");
            playbooks.addAll(limit(settlement.executionSafeguards(), 4));
            confidence += 0.05d;
        }

        if (copilot != null && !copilot.proceduralActions().isEmpty()) {
            playbooks.addAll(copilot.proceduralActions().stream().map(StrategicCopilotReport.Action::title).limit(3).toList());
        }

        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            officeAlerts.add("Processo sigiloso deve produzir memória institucional com filtragem reforçada por credencial.");
            confidence -= 0.03d;
        }

        String status = failures.isEmpty() ? "PROCESS_MEMORY_STABLE" : "PROCESS_MEMORY_REVIEW";
        return new InstitutionalMemoryReport(
                "PROCESS_TWIN",
                status,
                round(clamp(confidence)),
                List.copyOf(patterns),
                List.copyOf(failures),
                List.copyOf(playbooks),
                List.copyOf(officeAlerts),
                List.copyOf(memoryKeys),
                PayloadMaps.ofEntries(
                        "scope", "PROCESS_TWIN",
                        "processoId", processo.getId(),
                        "ritoName", ritoName,
                        "faseAtual", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                        "integrityStatus", radar != null ? radar.status() : null,
                        "negotiable", settlement != null && settlement.window() != null && settlement.window().favorable()
                )
        );
    }

    private static String competenceStatus(DynamicCompetenceDistributionResponse competencia) {
        if (competencia == null) {
            return null;
        }
        return competencia.distribuicaoAutomatica() ? "COMPETENCE_RESOLVED" : "COMPETENCE_REVIEW";
    }

    private static List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).limit(max).toList();
    }

    private static void addWhen(Set<String> target, boolean condition, String value) {
        if (condition && value != null && !value.isBlank()) {
            target.add(value);
        }
    }

    private static boolean truthy(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static double clamp(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    private static double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
