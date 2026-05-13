package com.tcc.pjb.backend.service.secretariat.topology;

import com.tcc.pjb.backend.service.juiz.handoff.JuizGabineteHandoffService;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudicialTopologyCoordinationMatrixService {

    private final JudicialTopologySegregationMeshService segregationMeshService;
    private final JuizGabineteHandoffService handoffService;

    public JudicialTopologyCoordinationMatrixService(JudicialTopologySegregationMeshService segregationMeshService,
                                                     JuizGabineteHandoffService handoffService) {
        this.segregationMeshService = Objects.requireNonNull(segregationMeshService);
        this.handoffService = Objects.requireNonNull(handoffService);
    }

    @Transactional(readOnly = true)
    public JudicialTopologyCoordinationMatrixSnapshot snapshot(Long processoId) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        JuizGabineteHandoffService.HandoffSnapshot handoff = handoffService.snapshot(processoId);
        JuizGabineteRoutingProfile gabineteRouting = handoff.routing();
        SecretariatOperationalRoutingProfile secretariatRouting = gabineteRouting == null ? null : gabineteRouting.secretariatRouting();

        List<MatrixSignal> signals = new ArrayList<>();
        signals.add(signal("TRIBUNAL_E_FORO_PRESENTES", "CRITICA",
                nonBlank(mesh.tipoJustica()) && nonBlank(stringOf(mesh.tribunal().get("codigo"))) && nonBlank(stringOf(mesh.forum().get("seatMunicipality"))),
                "A malha deve trazer tribunal, justiça e foro/cobertura territorial explícitos."));
        signals.add(signal("SECRETARIA_E_GABINETE_PRESENTES", "CRITICA",
                secretariatRouting != null && gabineteRouting != null,
                "Secretaria e gabinete precisam estar resolvidos na mesma matriz topológica."));
        signals.add(signal("INBOX_GABINETE_DERIVADO_DA_SECRETARIA", "ALTA",
                inboxDerivado(secretariatRouting, gabineteRouting),
                "O inbox do gabinete deve derivar do mesmo eixo topológico do inbox da secretaria."));
        signals.add(signal("LANE_ALINHADA", "ALTA",
                laneAlinhada(mesh, gabineteRouting, secretariatRouting),
                "Lane processual do tribunal, da secretaria e do gabinete precisa permanecer alinhada."));
        signals.add(signal("INSTANCIA_ALINHADA", "ALTA",
                instanciaAlinhada(mesh, gabineteRouting),
                "Instância do tribunal e do gabinete precisa permanecer alinhada."));
        signals.add(signal("FORO_NAO_GENERICO", "MEDIA",
                !genericAxis(stringOf(mesh.tribunal().get("forumAxis"))) && !genericAxis(stringOf(mesh.forum().get("unitDescriptor"))),
                "A matriz não deve depender de eixo genérico de foro/unidade quando houver topologia concreta."));
        signals.add(signal("SEM_DRIFT_DE_HANDOFF", "ALTA",
                handoff.driftItems() == null || handoff.driftItems().isEmpty(),
                "Não deve haver drift de handoff entre gabinete, assessoria e secretaria."));
        signals.add(signal("SECRETARIA_DE_RETORNO_ALINHADA", "ALTA",
                secretariatRetornoAlinhada(mesh, handoff),
                "A secretaria de retorno do handoff deve coincidir com a secretaria topológica resolvida na malha."));

        String recommendedAction = resolveRecommendedAction(signals);
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("signalCount", signals.size());
        metrics.put("blockingSignals", signals.stream().filter(MatrixSignal::blocking).count());
        metrics.put("unsatisfiedSignals", signals.stream().filter(signal -> !signal.satisfied()).count());
        metrics.put("driftItems", handoff.driftItems() == null ? 0 : handoff.driftItems().size());
        metrics.put("gabineteItems", handoff.gabineteItems() == null ? 0 : handoff.gabineteItems().size());
        metrics.put("assessoriaItems", handoff.assessoriaItems() == null ? 0 : handoff.assessoriaItems().size());
        metrics.put("secretariaItems", handoff.secretariaItems() == null ? 0 : handoff.secretariaItems().size());
        metrics.put("recommendedAction", recommendedAction);
        metrics.entrySet().removeIf(entry -> entry.getValue() == null);

        LinkedHashMap<String, Object> segregation = new LinkedHashMap<>();
        segregation.put("tipoJustica", mesh.tipoJustica());
        segregation.put("instanciaAxis", mesh.instanciaAxis());
        segregation.put("regimeAxis", mesh.regimeAxis());
        segregation.put("laneAxis", mesh.laneAxis());
        segregation.put("organizationalPath", mesh.organizationalPath());
        segregation.put("barriers", mesh.barriers());
        segregation.put("tribunal", mesh.tribunal());
        segregation.put("forum", mesh.forum());
        segregation.put("secretaria", mesh.secretaria());
        segregation.put("gabinete", mesh.gabinete());
        segregation.entrySet().removeIf(entry -> entry.getValue() == null);

        LinkedHashMap<String, Object> coordination = new LinkedHashMap<>();
        coordination.put("handoffRecommendedAction", handoff.recommendedAction());
        coordination.put("judgeGuardAllowed", handoff.judgeGuardRail() == null ? null : handoff.judgeGuardRail().allowed());
        coordination.put("judgeVerdictBand", handoff.judgeGuardRail() == null ? null : handoff.judgeGuardRail().verdictBand());
        coordination.put("routeKey", gabineteRouting == null ? null : gabineteRouting.routeKey());
        coordination.put("gabineteDesk", gabineteRouting == null ? null : gabineteRouting.gabineteDesk());
        coordination.put("advisoryDesk", gabineteRouting == null ? null : gabineteRouting.advisoryDesk());
        coordination.put("secretariatCode", secretariatRouting == null ? null : secretariatRouting.secretariatCode());
        coordination.put("secretariatInbox", secretariatRouting == null ? null : secretariatRouting.receiptInboxKey());
        coordination.put("gabineteInbox", gabineteRouting == null ? null : gabineteRouting.gabineteInboxKey());
        coordination.put("historyEntries", handoff.history() == null ? 0 : handoff.history().size());
        coordination.put("signals", handoff.signals());
        coordination.entrySet().removeIf(entry -> entry.getValue() == null);

        return new JudicialTopologyCoordinationMatrixSnapshot(
                mesh.processoId(),
                mesh.numeroProcesso(),
                mesh.topologyKey(),
                Map.copyOf(segregation),
                Map.copyOf(coordination),
                List.copyOf(signals),
                recommendedAction,
                Map.copyOf(metrics)
        );
    }

    private boolean inboxDerivado(SecretariatOperationalRoutingProfile secretariatRouting,
                                  JuizGabineteRoutingProfile gabineteRouting) {
        if (secretariatRouting == null || gabineteRouting == null) {
            return false;
        }
        String sec = stringOf(secretariatRouting.receiptInboxKey());
        String gab = stringOf(gabineteRouting.gabineteInboxKey());
        if (!nonBlank(sec) || !nonBlank(gab)) {
            return false;
        }
        if (sec.startsWith("SEC:") && gab.startsWith("GAB:")) {
            return gab.equals("GAB:" + sec.substring(4));
        }
        return normalized(sec).equals(normalized(gab));
    }

    private boolean laneAlinhada(JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh,
                                 JuizGabineteRoutingProfile gabineteRouting,
                                 SecretariatOperationalRoutingProfile secretariatRouting) {
        String laneMesh = normalized(mesh.laneAxis());
        String laneGabinete = gabineteRouting == null || gabineteRouting.topology() == null ? null : normalized(gabineteRouting.topology().laneAxis());
        String laneSecretaria = secretariatRouting == null ? null : normalized(secretariatRouting.ramoAxis());
        if (!nonBlank(laneMesh) || !nonBlank(laneGabinete) || !nonBlank(laneSecretaria)) {
            return false;
        }
        return laneGabinete.equals(laneMesh) && (laneSecretaria.equals(laneMesh) || laneMesh.contains(laneSecretaria) || laneSecretaria.contains(laneMesh));
    }

    private boolean instanciaAlinhada(JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh,
                                      JuizGabineteRoutingProfile gabineteRouting) {
        String left = normalized(mesh.instanciaAxis());
        String right = gabineteRouting == null || gabineteRouting.topology() == null ? null : normalized(gabineteRouting.topology().instanceAxis());
        return nonBlank(left) && nonBlank(right) && left.equals(right);
    }

    private boolean secretariatRetornoAlinhada(JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh,
                                               JuizGabineteHandoffService.HandoffSnapshot handoff) {
        if (handoff == null || handoff.routing() == null || handoff.routing().secretariatRouting() == null) {
            return false;
        }
        String matrixCode = normalized(stringOf(mesh.secretaria().get("secretariatCode")));
        String handoffCode = normalized(handoff.routing().secretariatRouting().secretariatCode());
        return nonBlank(matrixCode) && nonBlank(handoffCode) && matrixCode.equals(handoffCode);
    }

    private String resolveRecommendedAction(List<MatrixSignal> signals) {
        if (signals.stream().anyMatch(signal -> signal.blocking() && !signal.satisfied() && "TRIBUNAL_E_FORO_PRESENTES".equals(signal.code()))) {
            return "REFINAR_MALHA_TOPOLOGICA_DO_FORO";
        }
        if (signals.stream().anyMatch(signal -> signal.blocking() && !signal.satisfied() && "SECRETARIA_E_GABINETE_PRESENTES".equals(signal.code()))) {
            return "RESTAURAR_COORDENACAO_ENTRE_SECRETARIA_E_GABINETE";
        }
        if (signals.stream().anyMatch(signal -> signal.blocking() && !signal.satisfied() && "SEM_DRIFT_DE_HANDOFF".equals(signal.code()))) {
            return "ESTABILIZAR_HANDOFF_E_LANES";
        }
        if (signals.stream().anyMatch(signal -> signal.blocking() && !signal.satisfied())) {
            return "CORRIGIR_ROTAS_TOPOLOGICAS_CRITICAS";
        }
        if (signals.stream().anyMatch(signal -> !signal.satisfied())) {
            return "REFINAR_CATALOGO_E_UNIDADE_TOPOLOGICA";
        }
        return "MALHA_TOPOLOGICA_COORDENADA";
    }

    private MatrixSignal signal(String code, String level, boolean satisfied, String message) {
        boolean blocking = "CRITICA".equals(level) || "ALTA".equals(level);
        return new MatrixSignal(code, level, blocking, satisfied, message);
    }

    private boolean genericAxis(String value) {
        String normalized = normalized(value);
        return !nonBlank(normalized)
                || normalized.equals("FORO_COMUM")
                || normalized.equals("UNIDADE_BASE")
                || normalized.equals("BASE")
                || normalized.contains("CENTRAL");
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String stringOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalized(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    public record JudicialTopologyCoordinationMatrixSnapshot(
            Long processoId,
            String numeroProcesso,
            String topologyKey,
            Map<String, Object> segregation,
            Map<String, Object> coordination,
            List<MatrixSignal> signals,
            String recommendedAction,
            Map<String, Object> metrics
    ) {
    }

    public record MatrixSignal(
            String code,
            String level,
            boolean blocking,
            boolean satisfied,
            String message
    ) {
    }
}
