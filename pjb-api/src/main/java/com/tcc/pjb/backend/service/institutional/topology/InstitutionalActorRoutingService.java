package com.tcc.pjb.backend.service.institutional.topology;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.secretariat.topology.JudicialTopologySegregationMeshService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalActorRoutingService {

    private final JudicialTopologySegregationMeshService segregationMeshService;
    private final InstitutionalActorTopologyMeshService actorTopologyMeshService;

    public InstitutionalActorRoutingService(JudicialTopologySegregationMeshService segregationMeshService,
                                            InstitutionalActorTopologyMeshService actorTopologyMeshService) {
        this.segregationMeshService = Objects.requireNonNull(segregationMeshService);
        this.actorTopologyMeshService = Objects.requireNonNull(actorTopologyMeshService);
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute secretaryExecution(Long processoId, String actionAxis) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        return new InstitutionalRoute(
                stringOf(mesh.secretaria().get("executionQueueCode")),
                stringOf(mesh.secretaria().get("executionInboxKey")),
                TipoUsuario.SERVIDOR_FORUM,
                normalizeToken(actionAxis, "EXECUCAO_SECRETARIA"),
                mesh.topologyKey(),
                "Encaminhamento para execução da secretaria topológica do processo.",
                routeMetadata(mesh, null)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute secretaryReceipt(Long processoId, String actionAxis) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        return new InstitutionalRoute(
                stringOf(mesh.secretaria().get("receiptQueueCode")),
                stringOf(mesh.secretaria().get("receiptInboxKey")),
                TipoUsuario.SERVIDOR_FORUM,
                normalizeToken(actionAxis, "RECEBIMENTO_SECRETARIA"),
                mesh.topologyKey(),
                "Encaminhamento para recebimento cartorário da secretaria topológica.",
                routeMetadata(mesh, null)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute secretarySaneamento(Long processoId, String actionAxis) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        return new InstitutionalRoute(
                stringOf(mesh.secretaria().get("saneamentoQueueCode")),
                stringOf(mesh.secretaria().get("saneamentoInboxKey")),
                TipoUsuario.SERVIDOR_FORUM,
                normalizeToken(actionAxis, "SANEAMENTO_SECRETARIA"),
                mesh.topologyKey(),
                "Encaminhamento para saneamento cartorário da secretaria topológica.",
                routeMetadata(mesh, null)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute secretaryAudience(Long processoId, String actionAxis) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        return new InstitutionalRoute(
                stringOf(mesh.secretaria().get("audienceQueueCode")),
                stringOf(mesh.secretaria().get("audienceInboxKey")),
                TipoUsuario.SERVIDOR_FORUM,
                normalizeToken(actionAxis, "AUDIENCIA_SECRETARIA"),
                mesh.topologyKey(),
                "Encaminhamento para preparação e pauta de audiência da secretaria topológica.",
                routeMetadata(mesh, null)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute gabineteDecision(Long processoId, String actionAxis) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        TipoUsuario role = resolveJudicialRole(mesh.tipoJustica(), mesh.instanciaAxis());
        return new InstitutionalRoute(
                joinQueue("GABINETE", actionAxis, mesh.instanciaAxis(), mesh.laneAxis()),
                stringOf(mesh.gabinete().get("gabineteInboxKey")),
                role,
                normalizeToken(actionAxis, "GABINETE_DECISION"),
                mesh.topologyKey(),
                "Encaminhamento para o gabinete judicial compatível com a instância e a lane do processo.",
                routeMetadata(mesh, null)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute gabineteReview(Long processoId, String actionAxis) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        TipoUsuario role = resolveAssessoriaRole(mesh.tipoJustica(), mesh.instanciaAxis());
        return new InstitutionalRoute(
                joinQueue("GABINETE", actionAxis, "REVISAO", mesh.laneAxis()),
                stringOf(mesh.gabinete().get("gabineteInboxKey")),
                role,
                normalizeToken(actionAxis, "GABINETE_REVIEW"),
                mesh.topologyKey(),
                "Encaminhamento para revisão e assessoramento do gabinete compatível com a instância e a lane do processo.",
                routeMetadata(mesh, null)
        );
    }


    @Transactional(readOnly = true)
    public InstitutionalRoute resolveByAssignedRole(Long processoId, TipoUsuario assignedRole, String actionAxis) {
        if (assignedRole == null) {
            return secretaryExecution(processoId, actionAxis);
        }
        return switch (assignedRole) {
            case SERVIDOR_FORUM -> secretaryExecution(processoId, actionAxis);
            case JUIZ, JUIZ_FEDERAL, JUIZ_ELEITORAL, JUIZ_TRABALHISTA, JUIZ_MILITAR,
                    ASSESSOR_JUDICIAL -> gabineteDecision(processoId, actionAxis);
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL, ASSESSOR_DESEMBARGADOR -> colegiado(processoId, actionAxis);
            case MINISTRO, ASSESSOR_MINISTRO -> superiorCourt(processoId, actionAxis, false);
            case DEFENSOR_PUBLICO, DEFENSOR_PUBLICO_FEDERAL -> defensoria(processoId, actionAxis);
            case MEMBRO_MINISTERIO_PUBLICO, PROMOTOR_ELEITORAL, PROMOTOR_TRABALHISTA, PROCURADOR_GERAL_REPUBLICA -> ministerioPublico(processoId, actionAxis);
            case DELEGADO_POLICIA, DELEGADO_POLICIA_FEDERAL -> policeDiligence(processoId);
            case OFICIAL_JUSTICA -> officialJustice(processoId, false, actionAxis);
            case OFICIAL_JUSTICA_AVALIADOR -> officialJustice(processoId, true, actionAxis);
            default -> secretaryExecution(processoId, actionAxis);
        };
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute policeDiligence(Long processoId) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        TipoUsuario role = isFederal(mesh) ? TipoUsuario.DELEGADO_POLICIA_FEDERAL : TipoUsuario.DELEGADO_POLICIA;
        InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot actor = actorTopologyMeshService.snapshotForActor(processoId, role);
        return new InstitutionalRoute(
                joinQueue("POLICIA", "DILIGENCIA", actor.institutionalScope(), mesh.laneAxis()),
                actor.primaryInboxKey(),
                role,
                "DILIGENCIA_POLICIAL",
                mesh.topologyKey(),
                "Diligência encaminhada para a polícia judiciária segregada por esfera, tribunal-base e lane.",
                routeMetadata(mesh, actor)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute ministerioPublico(Long processoId, String actionAxis) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        TipoUsuario role = resolveMinisterioPublicoRole(mesh);
        InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot actor = actorTopologyMeshService.snapshotForActor(processoId, role);
        return new InstitutionalRoute(
                joinQueue("MP", actionAxis, actor.institutionalScope(), mesh.laneAxis()),
                actor.primaryInboxKey(),
                role,
                normalizeToken(actionAxis, "ATUACAO_MP"),
                mesh.topologyKey(),
                "Encaminhamento para o Ministério Público compatível com a esfera e a lane do processo.",
                routeMetadata(mesh, actor)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute defensoria(Long processoId, String actionAxis) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        TipoUsuario role = isFederal(mesh) ? TipoUsuario.DEFENSOR_PUBLICO_FEDERAL : TipoUsuario.DEFENSOR_PUBLICO;
        InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot actor = actorTopologyMeshService.snapshotForActor(processoId, role);
        return new InstitutionalRoute(
                joinQueue("DEFENSORIA", actionAxis, actor.institutionalScope(), mesh.laneAxis()),
                actor.primaryInboxKey(),
                role,
                normalizeToken(actionAxis, "ATUACAO_DEFENSORIA"),
                mesh.topologyKey(),
                "Atuação da Defensoria Pública segregada por esfera e lane processual.",
                routeMetadata(mesh, actor)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute officialJustice(Long processoId, boolean avaliador, String actionAxis) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        TipoUsuario role = avaliador ? TipoUsuario.OFICIAL_JUSTICA_AVALIADOR : TipoUsuario.OFICIAL_JUSTICA;
        InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot actor = actorTopologyMeshService.snapshotForActor(processoId, role);
        return new InstitutionalRoute(
                joinQueue("OFICIAL", actionAxis, mesh.instanciaAxis(), mesh.laneAxis()),
                actor.primaryInboxKey(),
                role,
                normalizeToken(actionAxis, "ATUACAO_OFICIAL"),
                mesh.topologyKey(),
                "Cumprimento por oficial de justiça alinhado ao foro e à lane do processo.",
                routeMetadata(mesh, actor)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute colegiado(Long processoId, String actionAxis) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        TipoUsuario role = isFederal(mesh) ? TipoUsuario.DESEMBARGADOR_FEDERAL : TipoUsuario.DESEMBARGADOR;
        InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot actor = actorTopologyMeshService.snapshotForActor(processoId, role);
        return new InstitutionalRoute(
                joinQueue("COLEGIADO", actionAxis, actor.institutionalScope(), mesh.laneAxis()),
                actor.primaryInboxKey(),
                assessoriaDoColegiado(role),
                normalizeToken(actionAxis, "ATUACAO_COLEGIADA"),
                mesh.topologyKey(),
                "Fluxo colegiado derivado da malha institucional de segundo grau do processo.",
                routeMetadata(mesh, actor)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute colegiadoPublication(Long processoId, String actionAxis) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        TipoUsuario role = isFederal(mesh) ? TipoUsuario.DESEMBARGADOR_FEDERAL : TipoUsuario.DESEMBARGADOR;
        InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot actor = actorTopologyMeshService.snapshotForActor(processoId, role);
        return new InstitutionalRoute(
                joinQueue("COLEGIADO", actionAxis, "PUBLICACAO", mesh.laneAxis()),
                actor.publicationInboxKey(),
                assessoriaDoColegiado(role),
                normalizeToken(actionAxis, "PUBLICACAO_COLEGIADA"),
                mesh.topologyKey(),
                "Publicação colegiada derivada da malha topológica do tribunal de segundo grau.",
                routeMetadata(mesh, actor)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalRoute superiorCourt(Long processoId, String actionAxis, boolean publication) {
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot actor = actorTopologyMeshService.snapshotForActor(processoId, TipoUsuario.MINISTRO);
        return new InstitutionalRoute(
                joinQueue("CORTE_SUPERIOR", actionAxis, actor.institutionalScope(), mesh.laneAxis()),
                publication ? actor.publicationInboxKey() : actor.primaryInboxKey(),
                TipoUsuario.ASSESSOR_MINISTRO,
                normalizeToken(actionAxis, publication ? "PUBLICACAO_CORTE_SUPERIOR" : "ATUACAO_CORTE_SUPERIOR"),
                mesh.topologyKey(),
                publication
                        ? "Publicação encaminhada pela malha de corte superior do processo."
                        : "Fluxo de gabinete/plenário de corte superior alinhado à corte e à lane do processo.",
                routeMetadata(mesh, actor)
        );
    }

    private Map<String, Object> routeMetadata(JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh,
                                              InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot actor) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tipoJustica", mesh.tipoJustica());
        metadata.put("instanciaAxis", mesh.instanciaAxis());
        metadata.put("laneAxis", mesh.laneAxis());
        metadata.put("tribunalCode", stringOf(mesh.tribunal().get("codigo")));
        metadata.put("forumSeat", stringOf(mesh.forum().get("seatMunicipality")));
        metadata.put("secretariatInbox", stringOf(mesh.secretaria().get("executionInboxKey")));
        metadata.put("gabineteInbox", stringOf(mesh.gabinete().get("gabineteInboxKey")));
        if (actor != null) {
            metadata.put("actorAxis", actor.actorAxis());
            metadata.put("institutionalScope", actor.institutionalScope());
            metadata.put("officeCode", actor.officeCode());
            metadata.put("primaryInboxKey", actor.primaryInboxKey());
            metadata.put("publicationInboxKey", actor.publicationInboxKey());
        }
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return Collections.unmodifiableMap(metadata);
    }

    private boolean isFederal(JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh) {
        String tipoJustica = normalizeToken(mesh.tipoJustica(), "");
        String tribunalCode = normalizeToken(stringOf(mesh.tribunal().get("codigo")), "");
        return tipoJustica.contains("FEDERAL") || tribunalCode.startsWith("TRF") || "STJ".equals(tribunalCode) || "STF".equals(tribunalCode);
    }

    private TipoUsuario resolveJudicialRole(String tipoJustica, String instanciaAxis) {
        String justica = normalizeToken(tipoJustica, "ESTADUAL");
        String instancia = normalizeToken(instanciaAxis, "1G");
        if (instancia.startsWith("SUP")) {
            return TipoUsuario.MINISTRO;
        }
        if (instancia.startsWith("2G")) {
            return justica.contains("FEDERAL") ? TipoUsuario.DESEMBARGADOR_FEDERAL : TipoUsuario.DESEMBARGADOR;
        }
        if (justica.contains("FEDERAL")) {
            return TipoUsuario.JUIZ_FEDERAL;
        }
        if (justica.contains("ELEITORAL")) {
            return TipoUsuario.JUIZ_ELEITORAL;
        }
        if (justica.contains("TRABALHISTA")) {
            return TipoUsuario.JUIZ_TRABALHISTA;
        }
        if (justica.contains("MILITAR")) {
            return TipoUsuario.JUIZ_MILITAR;
        }
        return TipoUsuario.JUIZ;
    }

    private TipoUsuario resolveMinisterioPublicoRole(JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh) {
        String lane = normalizeToken(mesh.laneAxis(), "COMUM");
        if (lane.contains("ELEITORAL")) {
            return TipoUsuario.PROMOTOR_ELEITORAL;
        }
        if (lane.contains("TRABALHISTA")) {
            return TipoUsuario.PROMOTOR_TRABALHISTA;
        }
        String tribunalCode = normalizeToken(stringOf(mesh.tribunal().get("codigo")), "");
        if ("STF".equals(tribunalCode) || "STJ".equals(tribunalCode)) {
            return TipoUsuario.PROCURADOR_GERAL_REPUBLICA;
        }
        return TipoUsuario.MEMBRO_MINISTERIO_PUBLICO;
    }

    private TipoUsuario assessoriaDoColegiado(TipoUsuario colegiadoRole) {
        return colegiadoRole == TipoUsuario.DESEMBARGADOR_FEDERAL || colegiadoRole == TipoUsuario.DESEMBARGADOR
                ? TipoUsuario.ASSESSOR_DESEMBARGADOR
                : TipoUsuario.ASSESSOR_JUDICIAL;
    }

    private TipoUsuario resolveAssessoriaRole(String tipoJustica, String instanciaAxis) {
        String instancia = normalizeToken(instanciaAxis, "1G");
        if (instancia.startsWith("SUP")) {
            return TipoUsuario.ASSESSOR_MINISTRO;
        }
        if (instancia.startsWith("2G")) {
            return TipoUsuario.ASSESSOR_DESEMBARGADOR;
        }
        return TipoUsuario.ASSESSOR_JUDICIAL;
    }

    private String joinQueue(String prefix, String actionAxis, String scope, String lane) {
        StringBuilder builder = new StringBuilder(normalizeToken(prefix, "ROUTE"));
        if (actionAxis != null && !actionAxis.isBlank()) {
            builder.append('_').append(normalizeToken(actionAxis, "FLOW"));
        }
        if (scope != null && !scope.isBlank()) {
            builder.append('_').append(normalizeToken(scope, "SCOPE"));
        }
        if (lane != null && !lane.isBlank()) {
            builder.append('_').append(normalizeToken(lane, "LANE"));
        }
        return builder.toString();
    }

    private static String stringOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String normalizeToken(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? fallback : normalized;
    }

    public record InstitutionalRoute(
            String queueCode,
            String inboxKey,
            TipoUsuario assignedRole,
            String routeAxis,
            String topologyKey,
            String rationale,
            Map<String, Object> metadata
    ) {
        public String routeKey() {
            return topologyKey;
        }
    }
}
