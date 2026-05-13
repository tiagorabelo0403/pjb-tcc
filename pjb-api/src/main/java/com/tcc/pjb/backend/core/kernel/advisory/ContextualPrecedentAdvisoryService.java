package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.dto.twin.PrecedenteEvidenceDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;

@Service
public class ContextualPrecedentAdvisoryService {

    public ContextualPrecedentAdvisoryReport analyzeRequest(LaianePeticaoAssistRequest request,
                                                            CanonicalContext canonical,
                                                            String ritoName,
                                                            DynamicCompetenceDistributionResponse competencia,
                                                            LegalCoherenceReport coherence,
                                                            StrategicCopilotReport copilot) {
        Objects.requireNonNull(request, "request");
        Set<String> dimensions = new LinkedHashSet<>();
        Set<String> queries = new LinkedHashSet<>();
        Set<String> profiles = new LinkedHashSet<>();
        Set<String> angles = new LinkedHashSet<>();
        Set<String> cautions = new LinkedHashSet<>();
        String canonicalClasseTpu = canonical != null ? canonical.classeTpuCodigo() : null;
        String canonicalRamoDireito = canonical != null ? canonical.ramoDireito() : null;
        String competenciaTribunal = competencia != null ? competencia.tribunalCodigo() : null;
        double adherence = 0.55d;

        add(dimensions, !blank(ritoName), "Rito efetivo");
        add(dimensions, !blank(canonicalClasseTpu), "Classe TPU");
        add(dimensions, !blank(canonicalRamoDireito), "Ramo do direito");
        add(dimensions, !blank(competenciaTribunal), "Tribunal alvo");

        add(queries, !blank(request.getTextoFatosResumido()), normalizedQuery(request.getTextoFatosResumido(), ritoName, canonicalRamoDireito));
        add(queries, !blank(request.getMateriaPrincipal()), request.getMateriaPrincipal().trim() + " tese principal " + normalize(ritoName));
        add(queries, !blank(canonicalClasseTpu), canonicalClasseTpu + " prova documental tutela competência");

        add(profiles, !blank(competenciaTribunal), "Órgão alvo: " + competenciaTribunal);
        add(profiles, truthy(request.getRequerLiminar()), "Precedentes de tutela de urgência e reversibilidade");
        add(profiles, truthy(request.getRequerJuizadoEspecial()), "Precedentes de simplicidade procedimental e conciliação");
        add(profiles, true, "Precedentes de mérito aderentes ao rito nominal");

        add(angles, true, "Separar pesquisa de mérito, urgência, competência e saneamento probatório.");
        add(angles, truthy(request.getRequerLiminar()), "Priorizar julgados que conectem probabilidade do direito, perigo de dano e reversibilidade.");
        add(angles, coherence != null && coherence.blocking(), "Curar precedentes que ajudem a neutralizar a incoerência material já detectada.");

        if (competencia == null || !competencia.distribuicaoAutomatica()) {
            cautions.add("Evitar ancoragem excessiva em precedentes locais enquanto o destino judicial permanecer aberto.");
            adherence -= 0.09d;
        } else {
            adherence += 0.06d;
        }
        if (coherence != null && coherence.blocking()) {
            cautions.add("Não usar precedente para mascarar lacuna fática ou documental bloqueante.");
            adherence -= 0.07d;
        }
        if (copilot != null && !copilot.jurisprudentialActions().isEmpty()) {
            copilot.jurisprudentialActions().stream().map(StrategicCopilotReport.Action::rationale).filter(Objects::nonNull).limit(3).forEach(angles::add);
            adherence += 0.04d;
        }

        String status = cautions.isEmpty() ? "PRECEDENT_CONTEXT_READY" : "PRECEDENT_CONTEXT_ATTENTION";
        return new ContextualPrecedentAdvisoryReport(
                "PETITION_ASSIST",
                status,
                round(clamp(adherence)),
                List.copyOf(dimensions),
                List.copyOf(queries),
                List.copyOf(profiles),
                List.copyOf(angles),
                List.copyOf(cautions),
                PayloadMaps.ofEntries(
                        "scope", "PETITION_ASSIST",
                        "ritoName", ritoName,
                        "classeTpu", canonicalClasseTpu,
                        "tribunal", competenciaTribunal,
                        "blockingCoherence", coherence != null && coherence.blocking()
                )
        );
    }

    public ContextualPrecedentAdvisoryReport analyzeProcess(Processo processo,
                                                            String ritoName,
                                                            RitoPlanDto ritoPlan,
                                                            List<PrecedenteEvidenceDto> evidence,
                                                            SettlementAdvisoryReport settlement,
                                                            ProcessIntegrityRadarReport radar) {
        Objects.requireNonNull(processo, "processo");
        Set<String> dimensions = new LinkedHashSet<>();
        Set<String> queries = new LinkedHashSet<>();
        Set<String> profiles = new LinkedHashSet<>();
        Set<String> angles = new LinkedHashSet<>();
        Set<String> cautions = new LinkedHashSet<>();
        double adherence = 0.6d;

        String ramoName = processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null;
        String faseAtualName = processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null;
        boolean faseRecursal = faseAtualName != null && faseAtualName.contains("RECUR");

        add(dimensions, !blank(ritoName), "Rito efetivo");
        add(dimensions, !blank(ramoName), "Ramo material");
        add(dimensions, !blank(faseAtualName), "Fase processual");
        add(dimensions, !blank(processo.getAssunto()), "Assunto nuclear");

        add(queries, !blank(processo.getAssunto()), normalizedQuery(processo.getAssunto(), ritoName, ramoName));
        add(queries, faseAtualName != null, faseAtualName + " " + normalize(ritoName) + " decisão útil");
        add(queries, settlement != null && settlement.window() != null && settlement.window().favorable(), normalize(ritoName) + " homologação acordo executabilidade");

        add(profiles, faseRecursal, "Precedentes de admissibilidade e reforma");
        add(profiles, !faseRecursal, "Precedentes de mérito e saneamento");
        add(profiles, settlement != null && settlement.window() != null && settlement.window().favorable(), "Precedentes de homologação, cláusulas executáveis e extinção segura");

        add(angles, evidence != null && !evidence.isEmpty(), "Ancorar a tese em precedentes já localizados e complementar por fase processual.");
        add(angles, ritoPlan != null && ritoPlan.getBlockingOpen() != null && !ritoPlan.getBlockingOpen().isEmpty(), "Buscar julgados sobre saneamento de pendências da fase antes do próximo ato.");
        add(angles, true, "Separar precedente estrutural, precedente de contexto e precedente de reforço argumentativo.");

        if (evidence == null || evidence.isEmpty()) {
            cautions.add("Trilha atual ainda não tem evidência jurisprudencial suficiente e precisa de reforço contextual.");
            adherence -= 0.1d;
        } else {
            adherence += 0.05d;
        }
        if (radar != null && radar.blocking()) {
            cautions.add("Não utilizar precedente como substituto de correção de risco material detectado no radar de integridade.");
            adherence -= 0.07d;
        }

        String status = cautions.isEmpty() ? "PROCESS_PRECEDENT_CONTEXT_READY" : "PROCESS_PRECEDENT_CONTEXT_ATTENTION";
        return new ContextualPrecedentAdvisoryReport(
                "PROCESS_TWIN",
                status,
                round(clamp(adherence)),
                List.copyOf(dimensions),
                List.copyOf(queries),
                List.copyOf(profiles),
                List.copyOf(angles),
                List.copyOf(cautions),
                PayloadMaps.ofEntries(
                        "scope", "PROCESS_TWIN",
                        "processoId", processo.getId(),
                        "ritoName", ritoName,
                        "faseAtual", faseAtualName,
                        "evidenceCount", evidence != null ? evidence.size() : 0
                )
        );
    }

    private static String normalizedQuery(String base, String ritoName, String ramo) {
        String source = normalize(base);
        String rito = normalize(ritoName);
        String ramoNorm = normalize(ramo);
        return (source + " " + ramoNorm + " " + rito).trim();
    }

    private static void add(Set<String> target, boolean condition, String value) {
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

    private static String normalize(String value) {
        return blank(value) ? "" : value.trim();
    }

    private static double clamp(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    private static double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
