package com.tcc.pjb.backend.core.processo.integracao.application;

import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoCanal;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoEvento;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoIdentity;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessService;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService.RoutingDecision;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoIntegracaoApplicationService {

    private final ProcessoRepository processoRepository;
    private final TribunalProtocolRoutingService tribunalProtocolRoutingService;
    private final JudicialConnectorReadinessService judicialConnectorReadinessService;

    public ProcessoIntegracaoApplicationService(ProcessoRepository processoRepository,
                                                TribunalProtocolRoutingService tribunalProtocolRoutingService,
                                                JudicialConnectorReadinessService judicialConnectorReadinessService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.tribunalProtocolRoutingService = Objects.requireNonNull(tribunalProtocolRoutingService);
        this.judicialConnectorReadinessService = Objects.requireNonNull(judicialConnectorReadinessService);
    }

    public ProcessoIntegracaoAggregate detalhar(Long processoId) {
        Processo processo = loadProcesso(processoId);
        Map<String, Object> payload = payload(processo);
        RoutingDecision routing = tribunalProtocolRoutingService.resolve(payload, processo.getRito(), safeName(processo.getRamoDireito()), processo.getCompetenciaTerritorialModo(), isRecursal(processo));
        ProtocolSubmissionRequest request = request(processo, routing);
        JudicialSubmissionCapability capability = routing.capability();
        JudicialConnectorReadinessReport readiness = judicialConnectorReadinessService.analyze(routing.judicialSystem(), capability, request);
        List<ProcessoIntegracaoCanal> canais = buildCanais(processo, routing, readiness);
        List<ProcessoIntegracaoEvento> eventos = buildEventos(processo, routing, readiness);
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        if (!readiness.blockers().isEmpty()) {
            readiness.blockers().forEach(item -> proximasAcoes.add("REMOVER_BLOQUEIO:" + item));
        }
        if (!readiness.readyForSubmission()) {
            proximasAcoes.add("CONCLUIR_PRONTIDAO_DE_ENVIO");
        }
        if (!readiness.readyForDryRun()) {
            proximasAcoes.add("CONCLUIR_PRONTIDAO_DE_SHADOW");
        }
        if (processo.getConnectorProtocolReference() == null || processo.getConnectorProtocolReference().isBlank()) {
            proximasAcoes.add("MATERIALIZAR_REFERENCIA_DE_PROTOCOLO_EXTERNO");
        }
        LinkedHashSet<String> alertas = new LinkedHashSet<>(readiness.warnings());
        if (systemName(processo).equals("NAO_DEFINIDO")) {
            alertas.add("Processo sem sistema legado ou conector atual explicitamente materializado.");
        }
        if (!Objects.equals(systemName(processo), routing.judicialSystem().name())) {
            alertas.add("Conector atual do processo difere do conector nacionalmente sugerido para o contexto informado.");
        }
        return new ProcessoIntegracaoAggregate(
                identity(processo),
                routing.judicialSystem().name(),
                readiness.readyForSubmission() ? "READY" : "BLOCKED",
                readiness.readyForDryRun() ? "READY" : "BLOCKED",
                canais,
                eventos,
                List.copyOf(proximasAcoes),
                List.copyOf(alertas),
                Instant.now()
        );
    }

    private List<ProcessoIntegracaoCanal> buildCanais(Processo processo,
                                                      RoutingDecision routing,
                                                      JudicialConnectorReadinessReport readiness) {
        ArrayList<ProcessoIntegracaoCanal> canais = new ArrayList<>();
        LinkedHashMap<String, Object> principalMetadata = new LinkedHashMap<>(readiness.metadata());
        principalMetadata.put("tribunalCodigo", routing.tribunalCodigo());
        principalMetadata.put("tribunalNome", routing.tribunalNome());
        principalMetadata.put("competenceHint", routing.competenceHint());
        principalMetadata.put("connectorAtual", systemName(processo));
        canais.add(new ProcessoIntegracaoCanal(
                "CANAL_SUBMISSAO_PRINCIPAL",
                "Canal principal de protocolo e sincronização",
                routing.judicialSystem().name(),
                readiness.connectorEnabled(),
                readiness.readyForSubmission(),
                stringValue(readiness.metadata().get("authMode")),
                readiness.certificateSatisfied() || routing.certificateRequired(),
                readiness.readyForDryRun(),
                readiness.syncPathResolved(),
                readiness.blockers(),
                readiness.warnings(),
                principalMetadata
        ));
        canais.add(new ProcessoIntegracaoCanal(
                "CANAL_SHADOW_MODE",
                "Canal de espelhamento e validação comparativa",
                routing.judicialSystem().name(),
                readiness.connectorEnabled(),
                readiness.readyForDryRun(),
                stringValue(readiness.metadata().get("authMode")),
                routing.certificateRequired(),
                readiness.readyForDryRun(),
                readiness.syncPathResolved(),
                readiness.readyForDryRun() ? List.of() : List.of("SHADOW_NOT_READY"),
                readiness.warnings(),
                Map.of(
                        "stepUpRequired", routing.stepUpRequired(),
                        "syncAvailable", readiness.syncPathResolved(),
                        "protocolReference", nonBlank(processo.getConnectorProtocolReference())
                )
        ));
        return List.copyOf(canais);
    }

    private List<ProcessoIntegracaoEvento> buildEventos(Processo processo,
                                                        RoutingDecision routing,
                                                        JudicialConnectorReadinessReport readiness) {
        ArrayList<ProcessoIntegracaoEvento> eventos = new ArrayList<>();
        eventos.add(new ProcessoIntegracaoEvento(
                "ROTEAMENTO_CONECTOR",
                "Roteamento nacional do conector",
                "ROUTING",
                readiness.readyForSubmission() ? "READY" : "REVIEW",
                routing.resolvedAt(),
                routing.tribunalCodigo(),
                List.of(routing.judicialSystem().name(), routing.tribunalNome(), routing.competenceHint())
        ));
        addEvento(eventos, "SUBMISSAO_EXTERNA", "Estado da submissão externa", "SUBMISSAO", safeStatus(processo.getConnectorSubmissionStatus()), toInstant(processo.getConnectorSubmissionProcessedAt()), nonBlank(processo.getConnectorProtocolReference(), processo.getNumeroProcesso()), List.of(nonBlank(processo.getConnectorSubmissionMessage()), "attempts=" + safeInt(processo.getConnectorSubmissionAttempts())));
        addEvento(eventos, "SINCRONIZACAO_SNAPSHOT", "Sincronização de snapshot externo", "SYNC", safeStatus(processo.getConnectorSyncStatus()), toInstant(processo.getConnectorSnapshotSyncedAt()), nonBlank(processo.getConnectorProtocolReference(), processo.getNumeroProcesso()), List.of(nonBlank(processo.getConnectorSyncMessage()), "syncAttempts=" + safeInt(processo.getConnectorSyncAttempts())));
        addEvento(eventos, "SINCRONIZACAO_EVENTOS", "Sincronização de eventos externos", "SYNC", readiness.syncPathResolved() ? "AVAILABLE" : "MISSING", toInstant(processo.getConnectorEventsSyncedAt()), nonBlank(processo.getConnectorProtocolReference(), processo.getNumeroProcesso()), List.of("connector=" + routing.judicialSystem().name(), "dryRun=" + readiness.readyForDryRun()));
        eventos.sort(Comparator.comparing(item -> item.occurredAt() == null ? Instant.EPOCH : item.occurredAt()));
        return List.copyOf(eventos);
    }

    private void addEvento(List<ProcessoIntegracaoEvento> eventos,
                           String codigo,
                           String titulo,
                           String eixo,
                           String status,
                           Instant occurredAt,
                           String correlationKey,
                           List<String> details) {
        eventos.add(new ProcessoIntegracaoEvento(codigo, titulo, eixo, status, occurredAt, correlationKey, sanitize(details)));
    }

    private ProcessoIntegracaoIdentity identity(Processo processo) {
        LinkedHashSet<String> marcadores = new LinkedHashSet<>();
        if (processo.getTribunal() != null) marcadores.add(processo.getTribunal());
        if (processo.getRamoDireito() != null) marcadores.add(processo.getRamoDireito().name());
        if (processo.getRito() != null) marcadores.add(processo.getRito().name());
        if (processo.getConnectorSystem() != null && !processo.getConnectorSystem().isBlank()) marcadores.add(processo.getConnectorSystem().trim().toUpperCase(Locale.ROOT));
        return new ProcessoIntegracaoIdentity(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getTribunal(),
                processo.getVara(),
                safeName(processo.getRamoDireito()),
                safeName(processo.getRito()),
                systemName(processo),
                List.copyOf(marcadores)
        );
    }

    private Map<String, Object> payload(Processo processo) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("tribunalCodigo", processo.getTribunal());
        payload.put("unidadeJudiciariaCodigo", processo.getVara());
        payload.put("classeTpu", processo.getClasseTpuCodigo());
        payload.put("classeProcessual", processo.getClasseProcessual());
        payload.put("assunto", processo.getAssunto());
        payload.put("numeroUnificado", processo.getNumeroUnificado());
        payload.put("connectorSystem", processo.getConnectorSystem());
        payload.put("competenciaTerritorialModo", processo.getCompetenciaTerritorialModo());
        payload.put("uf", processo.getUf());
        payload.put("comarca", processo.getComarca());
        return payload;
    }

    private ProtocolSubmissionRequest request(Processo processo, RoutingDecision routing) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("connectorSystem", routing.judicialSystem().name());
        metadata.put("connectorProtocolReference", processo.getConnectorProtocolReference());
        metadata.put("connectorSyncStatus", processo.getConnectorSyncStatus());
        metadata.put("connectorSubmissionStatus", processo.getConnectorSubmissionStatus());
        metadata.put("stepUpGovBr", routing.stepUpRequired());
        return new ProtocolSubmissionRequest(
                processo.getNumeroUnificado(),
                processo.getNumeroUnificado(),
                nonBlank(processo.getClasseProcessual(), "PROCESSO_UNIFICADO"),
                routing.tribunalCodigo(),
                nonBlank(processo.getVara(), routing.tribunalCodigo()),
                nonBlank(processo.getVara(), "UNIDADE_PADRAO"),
                safeName(processo.getRito()),
                processo.getClasseTpuCodigo(),
                safeName(processo.getRamoDireito()),
                "{}",
                nonBlank(processo.getMaterialProbatorioHash(), processo.getNumeroUnificado()),
                processo.getUsuario() == null ? null : processo.getUsuario().getId(),
                processo.getUsuario() == null ? null : processo.getUsuario().getId(),
                true,
                metadata
        );
    }

    private boolean isRecursal(Processo processo) {
        return processo.getFaseAtual() != null && processo.getFaseAtual().isRecursal();
    }

    private String systemName(Processo processo) {
        return processo.getConnectorSystem() == null || processo.getConnectorSystem().isBlank()
                ? "NAO_DEFINIDO"
                : processo.getConnectorSystem().trim().toUpperCase(Locale.ROOT);
    }

    private String safeStatus(String value) {
        return value == null || value.isBlank() ? "NAO_INFORMADO" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private List<String> sanitize(List<String> values) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (values != null) {
            values.stream().filter(Objects::nonNull).map(String::trim).filter(text -> !text.isBlank()).forEach(out::add);
        }
        return List.copyOf(out);
    }

    private String nonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "NAO_INFORMADO";
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }
}
