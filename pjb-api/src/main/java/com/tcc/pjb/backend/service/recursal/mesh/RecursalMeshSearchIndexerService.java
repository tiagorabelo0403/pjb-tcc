package com.tcc.pjb.backend.service.recursal.mesh;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPrecedentTrace;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryModel;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryRepository;

@Service
@ConditionalOnProperty(prefix = "pjb.search", name = "enabled", havingValue = "true")
public class RecursalMeshSearchIndexerService {

    private final RecursalMeshQueryRepository repository;
    private final RecursalMeshSlaService slaService;
    private final ObjectMapper objectMapper;

    public RecursalMeshSearchIndexerService(RecursalMeshQueryRepository repository,
                                            RecursalMeshSlaService slaService,
                                            ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository);
        this.slaService = Objects.requireNonNull(slaService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public void index(RecursalProcessIntegrationState projection) {
        if (projection == null || projection.getRecursoId() == null || projection.getRecursoId().isBlank()) {
            return;
        }
        repository.save(toDocument(projection));
    }

    RecursalMeshQueryModel toDocument(RecursalProcessIntegrationState projection) {
        Processo processo = projection.getProcesso();
        RecursalSlaSnapshot sla = slaService.snapshot(
                projection.getCurrentState(),
                projection.getTribunalAtual(),
                projection.getTribunalDetalhadoAtual(),
                projection.getLastTransitionAt() == null ? projection.getUpdatedAt() : projection.getLastTransitionAt(),
                processo == null ? null : processo.getUf(),
                processo == null ? null : processo.getComarca()
        ).orElse(null);
        RecursalStateSnapshot snapshot = snapshotOf(projection);
        RecursalPrecedentTrace precedentTrace = snapshot == null ? RecursalPrecedentTrace.empty() : snapshot.precedentTrace();

        RecursalMeshQueryModel model = RecursalMeshQueryModel.builder()
                .recursoId(projection.getRecursoId())
                .processoId(processo == null ? null : processo.getId())
                .numeroProcesso(firstNonBlank(projection.getNumeroProcesso(), processo == null ? null : processo.getNumeroUnificado(), processo == null ? null : processo.getNumeroProcesso()))
                .speciesCode(projection.getSpeciesCode())
                .profileName(projection.getProfileName())
                .currentState(enumName(projection.getCurrentState()))
                .tribunalAtual(enumName(projection.getTribunalAtual()))
                .tribunalDetalhadoAtual(enumName(projection.getTribunalDetalhadoAtual()))
                .instanciaAtual(enumName(projection.getInstanciaAtual()))
                .autoridadeAtual(enumName(projection.getAutoridadeAtual()))
                .lastEvent(enumName(projection.getLastEvent()))
                .currentRevision(projection.getCurrentRevision())
                .totalTransitions(projection.getTotalTransitions())
                .iteracoesEmbargos(projection.getIteracoesEmbargos())
                .transitadoEmJulgado(projection.isTransitadoEmJulgado())
                .lastActor(trimToNull(projection.getLastActor()))
                .tribunalProcesso(processo == null ? null : trimToNull(processo.getTribunal()))
                .varaProcesso(processo == null ? null : trimToNull(processo.getVara()))
                .comarcaProcesso(processo == null ? null : trimToNull(processo.getComarca()))
                .ufProcesso(processo == null ? null : trimToNull(processo.getUf()))
                .sigiloProcesso(processo == null || processo.getNivelSigilo() == null ? null : processo.getNivelSigilo().name())
                .ramoProcesso(processo == null || processo.getRamoDireito() == null ? null : processo.getRamoDireito().name())
                .ritoProcesso(processo == null || processo.getRito() == null ? null : processo.getRito().name())
                .assuntoProcesso(processo == null ? null : trimToNull(processo.getAssunto()))
                .sobrestadoPrecedente(snapshot != null && snapshot.sobrestadoPorPrecedente())
                .precedenteCodigo(trimToNull(precedentTrace.precedenteCodigo()))
                .precedenteTribunal(trimToNull(precedentTrace.precedenteTribunal()))
                .precedenteTema(trimToNull(precedentTrace.precedenteTema()))
                .precedenteAplicado(precedentTrace.aplicado())
                .precedenteDistinguido(precedentTrace.distinguido())
                .fundamentoDistincao(trimToNull(precedentTrace.fundamentoDistincao()))
                .lastTransitionAt(projection.getLastTransitionAt())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .build();

        if (sla != null) {
            model.setSlaDataPrevistaSaida(sla.dataPrevistaSaida());
            model.setSlaDiasUteisEsperados(sla.diasUteis());
            model.setSlaFatalParaPartes(sla.fatalParaPartes());
            model.setSlaVencido(sla.vencido());
            model.setSlaDiasUteisExcedidos(sla.diasUteisExcedidos());
            model.setSlaSeveridade(trimToNull(sla.severidade()));
            model.setSlaFundamentoLegal(trimToNull(sla.fundamentoLegal()));
        }

        addTag(model, projection.getSpeciesCode());
        addTag(model, projection.getProfileName());
        addTag(model, enumName(projection.getCurrentState()));
        addTag(model, enumName(projection.getTribunalAtual()));
        addTag(model, enumName(projection.getTribunalDetalhadoAtual()));
        addTag(model, enumName(projection.getAutoridadeAtual()));
        addTag(model, enumName(projection.getLastEvent()));
        addTag(model, processo == null || processo.getRamoDireito() == null ? null : processo.getRamoDireito().name());
        addTag(model, processo == null || processo.getRito() == null ? null : processo.getRito().name());
        addTag(model, model.getSlaSeveridade());
        addTag(model, model.getPrecedenteCodigo());
        addTag(model, model.getPrecedenteTribunal());
        if (Boolean.TRUE.equals(model.getSlaVencido())) {
            addTag(model, "SLA_VENCIDO");
        }
        if (Boolean.TRUE.equals(model.getSlaFatalParaPartes())) {
            addTag(model, "SLA_FATAL_PARTES");
        }
        if (Boolean.TRUE.equals(model.getTransitadoEmJulgado())) {
            addTag(model, "TRANSITADO_EM_JULGADO");
        }
        if (Boolean.TRUE.equals(model.getSobrestadoPrecedente())) {
            addTag(model, "SOBRESTADO_PRECEDENTE");
        }
        if (Boolean.TRUE.equals(model.getPrecedenteAplicado())) {
            addTag(model, "PRECEDENTE_APLICADO");
        }
        if (Boolean.TRUE.equals(model.getPrecedenteDistinguido())) {
            addTag(model, "CASO_DISTINGUIDO");
        }
        model.setSearchableText(buildSearchableText(model, processo));
        return model;
    }

    private RecursalStateSnapshot snapshotOf(RecursalProcessIntegrationState projection) {
        if (projection == null || projection.getSnapshotJson() == null || projection.getSnapshotJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(projection.getSnapshotJson(), RecursalStateSnapshot.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void addTag(RecursalMeshQueryModel model, String value) {
        if (model != null) {
            model.addTag(value);
        }
    }

    private static String buildSearchableText(RecursalMeshQueryModel model, Processo processo) {
        return Stream.of(
                        model.getRecursoId(),
                        model.getNumeroProcesso(),
                        model.getSpeciesCode(),
                        model.getProfileName(),
                        model.getCurrentState(),
                        model.getTribunalAtual(),
                        model.getTribunalDetalhadoAtual(),
                        model.getInstanciaAtual(),
                        model.getAutoridadeAtual(),
                        model.getLastEvent(),
                        model.getLastActor(),
                        model.getTribunalProcesso(),
                        model.getVaraProcesso(),
                        model.getComarcaProcesso(),
                        model.getUfProcesso(),
                        model.getRamoProcesso(),
                        model.getRitoProcesso(),
                        model.getAssuntoProcesso(),
                        model.getSlaSeveridade(),
                        model.getSlaFundamentoLegal(),
                        model.getPrecedenteCodigo(),
                        model.getPrecedenteTribunal(),
                        model.getPrecedenteTema(),
                        model.getFundamentoDistincao(),
                        processo == null ? null : processo.getClasseProcessual(),
                        processo == null ? null : processo.getObjetoProcessual(),
                        processo == null ? null : processo.getPedidoPrincipal())
                .map(RecursalMeshSearchIndexerService::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .reduce((left, right) -> left + " | " + right)
                .orElse(model.getRecursoId());
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
