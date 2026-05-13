package com.tcc.pjb.backend.service.institutional.topology;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.secretariat.topology.JudicialTopologyCoordinationMatrixService;
import com.tcc.pjb.backend.service.secretariat.topology.JudicialTopologySegregationMeshService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalActorTopologyMeshService {

    private final ProcessoRepository processoRepository;
    private final PerfilDashboardContextFactory contextFactory;
    private final JudicialTopologySegregationMeshService segregationMeshService;
    private final JudicialTopologyCoordinationMatrixService coordinationMatrixService;

    public InstitutionalActorTopologyMeshService(ProcessoRepository processoRepository,
                                                 PerfilDashboardContextFactory contextFactory,
                                                 JudicialTopologySegregationMeshService segregationMeshService,
                                                 JudicialTopologyCoordinationMatrixService coordinationMatrixService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.segregationMeshService = Objects.requireNonNull(segregationMeshService);
        this.coordinationMatrixService = Objects.requireNonNull(coordinationMatrixService);
    }

    @Transactional(readOnly = true)
    public InstitutionalActorTopologyMeshSnapshot snapshot(Long processoId) {
        Usuario usuario = contextFactory.build().usuario();
        return snapshotInternal(processoId, usuario == null ? null : usuario.getTipoUsuario(), usuario);
    }

    @Transactional(readOnly = true)
    public InstitutionalActorTopologyMeshSnapshot snapshotForActor(Long processoId, TipoUsuario actorType) {
        return snapshotInternal(processoId, actorType, null);
    }

    private InstitutionalActorTopologyMeshSnapshot snapshotInternal(Long processoId, TipoUsuario actorType, Usuario referenceUser) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh = segregationMeshService.snapshot(processoId);
        JudicialTopologyCoordinationMatrixService.JudicialTopologyCoordinationMatrixSnapshot matrix = coordinationMatrixService.snapshot(processoId);
        ActorAxisProfile actor = resolveActor(actorType, referenceUser, mesh);

        LinkedHashMap<String, Object> actorMap = new LinkedHashMap<>();
        actorMap.put("tipoUsuario", actorType == null ? null : actorType.name());
        actorMap.put("papelArquitetural", actorType == null ? null : actorType.papelArquitetural());
        actorMap.put("actorAxis", actor.actorAxis());
        actorMap.put("institutionalScope", actor.institutionalScope());
        actorMap.put("officeCode", actor.officeCode());
        actorMap.put("primaryInboxKey", actor.primaryInboxKey());
        actorMap.put("monitoringInboxKey", actor.monitoringInboxKey());
        actorMap.put("coordinationDesk", actor.coordinationDesk());
        actorMap.put("publicationInboxKey", actor.publicationInboxKey());
        actorMap.put("sessionChannel", actor.sessionChannel());
        actorMap.put("counterpartRoleAxis", actor.counterpartRoleAxis());
        actorMap.put("laneAxis", mesh.laneAxis());
        actorMap.put("instanciaAxis", mesh.instanciaAxis());
        actorMap.put("tipoJustica", mesh.tipoJustica());
        actorMap.put("tribunalCode", stringOf(mesh.tribunal().get("codigo")));
        actorMap.put("forumSeat", stringOf(mesh.forum().get("seatMunicipality")));
        actorMap.entrySet().removeIf(entry -> entry.getValue() == null);

        LinkedHashMap<String, Object> counterparts = new LinkedHashMap<>();
        counterparts.put("secretariat", mesh.secretaria());
        counterparts.put("gabinete", mesh.gabinete());
        counterparts.put("tribunal", mesh.tribunal());
        counterparts.put("forum", mesh.forum());
        counterparts.put("recommendedAction", matrix.recommendedAction());
        counterparts.put("coordinationMetrics", matrix.metrics());
        counterparts.entrySet().removeIf(entry -> entry.getValue() == null);

        ArrayList<String> barriers = new ArrayList<>(mesh.barriers());
        barriers.addAll(actor.actorBarriers());

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("processoId", processo.getId());
        metadata.put("numeroProcesso", processo.getNumeroProcesso());
        metadata.put("topologyKey", mesh.topologyKey());
        metadata.put("organizationalPath", mesh.organizationalPath());
        metadata.put("coordinationSignalCount", matrix.signals().size());
        metadata.put("blockingSignalCount", matrix.signals().stream().filter(JudicialTopologyCoordinationMatrixService.MatrixSignal::blocking).count());
        metadata.put("satisfiedSignalCount", matrix.signals().stream().filter(JudicialTopologyCoordinationMatrixService.MatrixSignal::satisfied).count());
        metadata.put("segregation", mesh.metadata());
        metadata.put("matrix", matrix.coordination());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);

        return new InstitutionalActorTopologyMeshSnapshot(
                processoId,
                processo.getNumeroProcesso(),
                actor.actorAxis(),
                actor.institutionalScope(),
                actor.officeCode(),
                actor.primaryInboxKey(),
                actor.monitoringInboxKey(),
                actor.publicationInboxKey(),
                actor.sessionChannel(),
                List.copyOf(barriers),
                Map.copyOf(actorMap),
                Map.copyOf(counterparts),
                Collections.unmodifiableMap(metadata)
        );
    }

    private ActorAxisProfile resolveActor(TipoUsuario tipoUsuario,
                                          Usuario referenceUser,
                                          JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot mesh) {
        String tribunalCode = normalizeToken(stringOf(mesh.tribunal().get("codigo")), "NACIONAL");
        String instanciaAxis = normalizeToken(mesh.instanciaAxis(), "1G");
        String laneAxis = normalizeToken(mesh.laneAxis(), "COMUM");
        String forumSeat = normalizeToken(stringOf(mesh.forum().get("seatMunicipality")), normalizeToken(referenceUser == null ? null : referenceUser.getComarca(), "BASE"));
        String uf = normalizeToken(firstNonBlank(referenceUser == null ? null : referenceUser.getUf(), stringOf(mesh.forum().get("sourceUf"))), "BR");
        if (tipoUsuario == null) {
            return new ActorAxisProfile(
                    "INSTITUCIONAL",
                    "GERAL",
                    "INST:" + tribunalCode + ':' + laneAxis,
                    "INST:" + tribunalCode + ':' + instanciaAxis + ':' + laneAxis,
                    "INST_MONITOR:" + tribunalCode + ':' + laneAxis,
                    "INST_COORD:" + tribunalCode + ':' + laneAxis,
                    stringOf(mesh.secretaria().get("executionInboxKey")),
                    "INST:" + tribunalCode + ":SESSAO",
                    "SECRETARIA",
                    List.of("Perfil institucional genérico opera apenas com leitura coordenada da malha topológica.")
            );
        }
        if (tipoUsuario.isMinisterioPublico()) {
            String scope = ministerioPublicoScope(tipoUsuario);
            String office = "MP:" + scope + ':' + tribunalCode + ':' + laneAxis + ':' + forumSeat;
            return new ActorAxisProfile(
                    "MINISTERIO_PUBLICO",
                    scope,
                    office,
                    office + ":ATUACAO",
                    office + ":PRAZOS",
                    office + ":COORD",
                    stringOf(mesh.secretaria().get("executionInboxKey")),
                    office + ":SESSAO",
                    "SECRETARIA",
                    List.of(
                            "Promotorias e órgãos do Ministério Público ficam separados por esfera, tribunal-base e lane processual.",
                            "A atuação ministerial usa a mesma malha judicial da secretaria para evitar mistura entre federal, estadual, eleitoral e trabalhista."
                    )
            );
        }
        if (tipoUsuario != null && tipoUsuario.isDefensoriaPublica()) {
            String scope = tipoUsuario == TipoUsuario.DEFENSOR_PUBLICO_FEDERAL ? "FEDERAL" : "ESTADUAL";
            String office = "DEFENSORIA:" + scope + ':' + tribunalCode + ':' + laneAxis + ':' + forumSeat;
            return new ActorAxisProfile(
                    "DEFENSORIA",
                    scope,
                    office,
                    office + ":ATUACAO",
                    office + ":PRAZOS",
                    office + ":COORD",
                    stringOf(mesh.secretaria().get("executionInboxKey")),
                    office + ":SESSAO",
                    "SECRETARIA",
                    List.of(
                            "Defensorias ficam segregadas por esfera, tribunal-base e lane do processo.",
                            "A juntada e o encaminhamento de peças da Defensoria usam a secretaria e o gabinete topológicos, sem fila genérica."
                    )
            );
        }
        if (tipoUsuario != null && (tipoUsuario == TipoUsuario.OFICIAL_JUSTICA || tipoUsuario == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR)) {
            String scope = tipoUsuario == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR ? "AVALIADOR" : "CUMPRIMENTO";
            String office = "OFICIAL:" + tribunalCode + ':' + laneAxis + ':' + forumSeat;
            return new ActorAxisProfile(
                    "OFICIAL_JUSTICA",
                    scope,
                    office,
                    office + ":MANDADOS",
                    office + ":MONITORAMENTO",
                    office + ":COORD",
                    stringOf(mesh.secretaria().get("executionInboxKey")),
                    office + ":SESSAO",
                    "SECRETARIA",
                    List.of(
                            "Oficiais de justiça operam por foro de cobertura, tribunal e lane processual.",
                            "Cumprimentos, avaliações e certidões retornam à secretaria topológica, não a uma caixa abstrata."
                    )
            );
        }
        if (tipoUsuario != null && (tipoUsuario.isDelegadoOuAgente() || tipoUsuario == TipoUsuario.ESCRIVAO_POLICIAL)) {
            String scope = tipoUsuario == TipoUsuario.DELEGADO_POLICIA_FEDERAL ? "FEDERAL" : "ESTADUAL";
            String office = "POLICIA:" + scope + ':' + tribunalCode + ':' + laneAxis + ':' + forumSeat;
            return new ActorAxisProfile(
                    "POLICIA_JUDICIARIA",
                    scope,
                    office,
                    office + ":DILIGENCIA",
                    office + ":MONITORAMENTO",
                    office + ":COORD",
                    stringOf(mesh.secretaria().get("executionInboxKey")),
                    office + ":SESSAO",
                    "MINISTERIO_PUBLICO",
                    List.of(
                            "Polícia judiciária fica segregada por esfera, foro de cobertura e lane do processo.",
                            "As requisições de diligência entram na mesma malha topológica e evitam confundir polícia estadual e federal."
                    )
            );
        }
        if (tipoUsuario != null && tipoUsuario.isProcuradoria()) {
            String scope = procuradoriaScope(tipoUsuario, referenceUser);
            String office = "PROC:" + scope + ':' + tribunalCode + ':' + laneAxis + ':' + forumSeat;
            return new ActorAxisProfile(
                    "PROCURADORIA",
                    scope,
                    office,
                    office + ":ATUACAO",
                    office + ":PRAZOS",
                    office + ":COORD",
                    stringOf(mesh.secretaria().get("executionInboxKey")),
                    office + ":SESSAO",
                    "SECRETARIA",
                    List.of(
                            "Procuradorias ficam segregadas por esfera municipal, estadual ou federal sem compartilhar inbox genérico.",
                            "A devolução de peças e manifestações usa a secretaria topológica do processo, não uma secretaria abstrata." 
                    )
            );
        }
        if (tipoUsuario == TipoUsuario.DESEMBARGADOR || tipoUsuario == TipoUsuario.DESEMBARGADOR_FEDERAL) {
            String office = "COLEGIADO:" + tribunalCode + ':' + laneAxis + ':' + forumSeat;
            return new ActorAxisProfile(
                    "DESEMBARGADOR",
                    "SEGUNDO_GRAU",
                    office,
                    office + ":RELATORIA",
                    office + ":MONITORAMENTO",
                    office + ":COORD",
                    office + ":PUBLICACAO",
                    office + ":SESSAO",
                    "SECRETARIA_2G",
                    List.of(
                            "Câmaras e colegiados de segundo grau permanecem separados do primeiro grau e dos gabinetes singulares.",
                            "A publicação e o monitoramento colegiado derivam do mesmo tribunal e lane do processo." 
                    )
            );
        }
        if (tipoUsuario == TipoUsuario.MINISTRO) {
            String courtScope = superiorCourtScope(tribunalCode);
            String office = "CORTE_SUPERIOR:" + courtScope + ':' + laneAxis;
            return new ActorAxisProfile(
                    "MINISTRO",
                    courtScope,
                    office,
                    office + ":GABINETE",
                    office + ":MONITORAMENTO",
                    office + ":COORD",
                    "SECRETARIA_PUBLICACAO_" + (courtScope.equals("SUP") ? tribunalCode : courtScope),
                    office + ":PLENARIO",
                    "ASSESSORIA_MINISTRO",
                    List.of(
                            "Ministros operam em malha de corte superior, separados por corte, plenario/turma e lane processual.",
                            "O fluxo de publicação e plenário de tribunal superior não compartilha filas de segundo ou primeiro grau." 
                    )
            );
        }
        if (tipoUsuario.isMagistratura()) {
            String office = stringOf(mesh.gabinete().get("gabineteInboxKey"));
            return new ActorAxisProfile(
                    "MAGISTRATURA",
                    instanciaAxis,
                    office,
                    office,
                    office == null ? null : office + ":MONITORAMENTO",
                    stringOf(mesh.gabinete().get("coordinationDesk")),
                    stringOf(mesh.secretaria().get("executionInboxKey")),
                    stringOf(mesh.gabinete().get("sessionChannel")),
                    "SECRETARIA",
                    List.of("Magistratura singular permanece vinculada ao gabinete topológico do processo e à secretaria correspondente.")
            );
        }
        if (tipoUsuario.isAssessor()) {
            String office = stringOf(mesh.gabinete().get("advisoryDesk"));
            return new ActorAxisProfile(
                    "ASSESSORIA",
                    instanciaAxis,
                    office,
                    office,
                    office == null ? null : office + ":MONITORAMENTO",
                    stringOf(mesh.gabinete().get("coordinationDesk")),
                    stringOf(mesh.secretaria().get("executionInboxKey")),
                    stringOf(mesh.gabinete().get("sessionChannel")),
                    "GABINETE",
                    List.of("Assessoria atua apenas dentro da mesma malha topológica do gabinete, com retorno formal e lane compatível.")
            );
        }
        if (tipoUsuario.isServidorJudiciario()) {
            String office = stringOf(mesh.secretaria().get("secretariatCode"));
            return new ActorAxisProfile(
                    "SECRETARIA",
                    instanciaAxis,
                    office,
                    stringOf(mesh.secretaria().get("receiptInboxKey")),
                    stringOf(mesh.secretaria().get("executionInboxKey")),
                    office == null ? null : office + ":COORD",
                    stringOf(mesh.secretaria().get("executionInboxKey")),
                    stringOf(mesh.gabinete().get("sessionChannel")),
                    "GABINETE",
                    List.of("Servidores e secretarias mantêm isolamento por foro, tribunal, instância e lane processual.")
            );
        }
        String office = "INST:" + tribunalCode + ':' + laneAxis + ':' + uf;
        return new ActorAxisProfile(
                tipoUsuario.papelArquitetural(),
                "GERAL",
                office,
                office,
                office + ":MONITORAMENTO",
                office + ":COORD",
                stringOf(mesh.secretaria().get("executionInboxKey")),
                office + ":SESSAO",
                "SECRETARIA",
                List.of("Perfil institucional derivado da malha judicial do processo para evitar filas genéricas sem tribunal e lane.")
        );
    }

    private String ministerioPublicoScope(TipoUsuario tipoUsuario) {
        return switch (tipoUsuario) {
            case PROMOTOR_ELEITORAL -> "ELEITORAL";
            case PROMOTOR_TRABALHISTA -> "TRABALHISTA";
            case PROCURADOR_GERAL_REPUBLICA -> "FEDERAL_SUPERIOR";
            default -> "ESTADUAL";
        };
    }

    private String procuradoriaScope(TipoUsuario tipoUsuario, Usuario referenceUser) {
        return switch (tipoUsuario) {
            case PROCURADORIA_MUNICIPAL -> "MUNICIPAL";
            case PROCURADORIA_ESTADUAL -> "ESTADUAL";
            case PROCURADORIA_FEDERAL, PROCURADOR_GERAL_REPUBLICA -> "FEDERAL";
            default -> referenceUser != null && referenceUser.atuaNaUniao() ? "FEDERAL" : (referenceUser != null && referenceUser.atuaNoEstado() ? "ESTADUAL" : "MUNICIPAL");
        };
    }

    private String superiorCourtScope(String tribunalCode) {
        if ("STF".equalsIgnoreCase(tribunalCode)) {
            return "STF";
        }
        if ("STJ".equalsIgnoreCase(tribunalCode)) {
            return "STJ";
        }
        if ("TST".equalsIgnoreCase(tribunalCode)) {
            return "TST";
        }
        if ("TSE".equalsIgnoreCase(tribunalCode)) {
            return "TSE";
        }
        return "SUP";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
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

    public record InstitutionalActorTopologyMeshSnapshot(
            Long processoId,
            String numeroProcesso,
            String actorAxis,
            String institutionalScope,
            String officeCode,
            String primaryInboxKey,
            String monitoringInboxKey,
            String publicationInboxKey,
            String sessionChannel,
            List<String> barriers,
            Map<String, Object> actor,
            Map<String, Object> counterparts,
            Map<String, Object> metadata
    ) {
    }

    private record ActorAxisProfile(
            String actorAxis,
            String institutionalScope,
            String officeCode,
            String primaryInboxKey,
            String monitoringInboxKey,
            String coordinationDesk,
            String publicationInboxKey,
            String sessionChannel,
            String counterpartRoleAxis,
            List<String> actorBarriers
    ) {
    }
}
