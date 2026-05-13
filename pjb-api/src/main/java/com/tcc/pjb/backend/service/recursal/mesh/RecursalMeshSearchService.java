package com.tcc.pjb.backend.service.recursal.mesh;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPrecedentTrace;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshProcessLinkView;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSearchRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSearchResponse;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryModel;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryRepository;

@Service
public class RecursalMeshSearchService {

    private final RecursalProcessIntegrationStateRepository projectionRepository;
    private final RecursalMeshProjectionService projectionService;
    private final ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider;
    private final ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider;
    private final ObjectMapper objectMapper;

    public RecursalMeshSearchService(RecursalProcessIntegrationStateRepository projectionRepository,
                                     RecursalMeshProjectionService projectionService,
                                     ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider,
                                     ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider,
                                     ObjectMapper objectMapper) {
        this.projectionRepository = Objects.requireNonNull(projectionRepository);
        this.projectionService = Objects.requireNonNull(projectionService);
        this.elasticsearchOperationsProvider = Objects.requireNonNull(elasticsearchOperationsProvider);
        this.queryRepositoryProvider = Objects.requireNonNull(queryRepositoryProvider);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public RecursalMeshSearchResponse search(RecursalMeshSearchRequest request) {
        RecursalMeshSearchRequest normalized = normalize(request);
        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        RecursalMeshQueryRepository repository = queryRepositoryProvider.getIfAvailable();
        if (operations != null && repository != null) {
            try {
                return searchInIndex(normalized, operations);
            } catch (RuntimeException ex) {
                return fallback(normalized, "RELATIONAL_FALLBACK");
            }
        }
        return fallback(normalized, "RELATIONAL_FALLBACK");
    }

    private RecursalMeshSearchRequest normalize(RecursalMeshSearchRequest request) {
        return request == null
                ? new RecursalMeshSearchRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 50)
                : request;
    }

    private RecursalMeshSearchResponse searchInIndex(RecursalMeshSearchRequest request,
                                                     ElasticsearchOperations operations) {
        Criteria criteria = buildCriteria(request);
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(0, request.maxResults()));
        query.addSort(Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("lastTransitionAt")));
        SearchHits<RecursalMeshQueryModel> hits = operations.search(query, RecursalMeshQueryModel.class);
        List<RecursalMeshProcessLinkView> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toView)
                .limit(request.maxResults())
                .toList();
        return new RecursalMeshSearchResponse("SEARCH_INDEX", items.size(), items);
    }

    private Criteria buildCriteria(RecursalMeshSearchRequest request) {
        List<Criteria> clauses = new ArrayList<>();
        if (request.processoId() != null) {
            clauses.add(new Criteria("processoId").is(request.processoId()));
        } else if (!request.processoIds().isEmpty()) {
            clauses.add(new Criteria("processoId").in(request.processoIds()));
        }
        if (hasText(request.speciesCode())) {
            clauses.add(new Criteria("speciesCode").is(request.speciesCode().trim().toUpperCase(Locale.ROOT)));
        }
        if (request.currentState() != null) {
            clauses.add(new Criteria("currentState").is(request.currentState().name()));
        }
        if (request.tribunalAtual() != null) {
            clauses.add(new Criteria("tribunalAtual").is(request.tribunalAtual().name()));
        }
        if (request.tribunalDetalhadoAtual() != null) {
            clauses.add(new Criteria("tribunalDetalhadoAtual").is(request.tribunalDetalhadoAtual().name()));
        }
        if (request.autoridadeAtual() != null) {
            clauses.add(new Criteria("autoridadeAtual").is(request.autoridadeAtual().name()));
        }
        if (hasText(request.precedenteCodigo())) {
            clauses.add(new Criteria("precedenteCodigo").is(request.precedenteCodigo().trim().toUpperCase(Locale.ROOT)));
        }
        if (hasText(request.precedenteTribunal())) {
            clauses.add(new Criteria("precedenteTribunal").is(request.precedenteTribunal().trim().toUpperCase(Locale.ROOT)));
        }
        if (hasText(request.precedenteTema())) {
            clauses.add(new Criteria("precedenteTema").matchesAll(request.precedenteTema().trim()));
        }
        if (Boolean.TRUE.equals(request.sobrestadoPrecedente())) {
            clauses.add(new Criteria("sobrestadoPrecedente").is(true));
        }
        if (Boolean.TRUE.equals(request.precedenteAplicado())) {
            clauses.add(new Criteria("precedenteAplicado").is(true));
        }
        if (Boolean.TRUE.equals(request.precedenteDistinguido())) {
            clauses.add(new Criteria("precedenteDistinguido").is(true));
        }
        if (Boolean.TRUE.equals(request.transitadoEmJulgado())) {
            clauses.add(new Criteria("transitadoEmJulgado").is(true));
        }
        if (Boolean.TRUE.equals(request.slaVencido())) {
            clauses.add(new Criteria("slaVencido").is(true));
        }
        if (Boolean.TRUE.equals(request.slaFatalParaPartes())) {
            clauses.add(new Criteria("slaFatalParaPartes").is(true));
        }
        if (hasText(request.q())) {
            clauses.add(new Criteria("searchableText").matchesAll(request.q().trim()));
        }
        if (clauses.isEmpty()) {
            return new Criteria("recursoId").exists();
        }
        if (clauses.size() == 1) {
            return clauses.getFirst();
        }
        return Criteria.and().and(clauses.toArray(Criteria[]::new));
    }

    private RecursalMeshSearchResponse fallback(RecursalMeshSearchRequest request, String source) {
        List<RecursalProcessIntegrationState> projections;
        if (request.processoId() != null) {
            projections = projectionRepository.findTop200ByProcesso_IdOrderByUpdatedAtDesc(request.processoId());
        } else if (!request.processoIds().isEmpty()) {
            projections = projectionRepository.findByProcesso_IdIn(
                    request.processoIds(),
                    PageRequest.of(0, Math.max(request.maxResults(), 200), Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("lastTransitionAt")))
            ).getContent();
        } else {
            projections = projectionRepository.findTop200ByOrderByUpdatedAtDesc();
        }

        List<RecursalMeshProcessLinkView> items = projections.stream()
                .filter(projection -> matchesFallback(projection, request))
                .sorted(Comparator
                        .comparing(RecursalProcessIntegrationState::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RecursalProcessIntegrationState::getLastTransitionAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(request.maxResults())
                .map(projectionService::viewOf)
                .toList();
        return new RecursalMeshSearchResponse(source, items.size(), items);
    }

    private boolean matchesFallback(RecursalProcessIntegrationState projection, RecursalMeshSearchRequest request) {
        if (projection == null) {
            return false;
        }
        Long processoId = projection.getProcesso() == null ? null : projection.getProcesso().getId();
        if (request.processoId() != null && !Objects.equals(processoId, request.processoId())) {
            return false;
        }
        if (request.processoId() == null && !request.processoIds().isEmpty() && !request.processoIds().contains(processoId)) {
            return false;
        }
        if (hasText(request.speciesCode()) && !equalsIgnoreCase(projection.getSpeciesCode(), request.speciesCode())) {
            return false;
        }
        if (request.currentState() != null && projection.getCurrentState() != request.currentState()) {
            return false;
        }
        if (request.tribunalAtual() != null && projection.getTribunalAtual() != request.tribunalAtual()) {
            return false;
        }
        if (request.tribunalDetalhadoAtual() != null && projection.getTribunalDetalhadoAtual() != request.tribunalDetalhadoAtual()) {
            return false;
        }
        if (request.autoridadeAtual() != null && projection.getAutoridadeAtual() != request.autoridadeAtual()) {
            return false;
        }
        RecursalStateSnapshot snapshot = snapshotOf(projection);
        RecursalPrecedentTrace precedentTrace = snapshot == null ? RecursalPrecedentTrace.empty() : snapshot.precedentTrace();
        if (hasText(request.precedenteCodigo()) && !equalsIgnoreCase(precedentTrace.precedenteCodigo(), request.precedenteCodigo())) {
            return false;
        }
        if (hasText(request.precedenteTribunal()) && !equalsIgnoreCase(precedentTrace.precedenteTribunal(), request.precedenteTribunal())) {
            return false;
        }
        if (hasText(request.precedenteTema()) && !containsIgnoreCase(precedentTrace.precedenteTema(), request.precedenteTema())) {
            return false;
        }
        if (Boolean.TRUE.equals(request.sobrestadoPrecedente()) && (snapshot == null || !snapshot.sobrestadoPorPrecedente())) {
            return false;
        }
        if (Boolean.TRUE.equals(request.precedenteAplicado()) && !precedentTrace.aplicado()) {
            return false;
        }
        if (Boolean.TRUE.equals(request.precedenteDistinguido()) && !precedentTrace.distinguido()) {
            return false;
        }
        if (Boolean.TRUE.equals(request.transitadoEmJulgado()) && !projection.isTransitadoEmJulgado()) {
            return false;
        }
        RecursalSlaSnapshot sla = projectionService.slaSnapshotOf(projection).orElse(null);
        if (Boolean.TRUE.equals(request.slaVencido()) && (sla == null || !sla.vencido())) {
            return false;
        }
        if (Boolean.TRUE.equals(request.slaFatalParaPartes()) && (sla == null || !sla.fatalParaPartes())) {
            return false;
        }
        if (hasText(request.q())) {
            String haystack = fallbackHaystack(projection, sla, precedentTrace, snapshot);
            if (!queryMatches(haystack, request.q())) {
                return false;
            }
        }
        return true;
    }

    private RecursalMeshProcessLinkView toView(RecursalMeshQueryModel model) {
        return new RecursalMeshProcessLinkView(
                model.getRecursoId(),
                model.getProcessoId(),
                model.getNumeroProcesso(),
                model.getSpeciesCode(),
                model.getProfileName(),
                parseEnum(RecursalLifecycleState.class, model.getCurrentState()),
                parseEnum(RecursalTribunal.class, model.getTribunalAtual()),
                parseEnum(RecursalTribunalDetalhado.class, model.getTribunalDetalhadoAtual()),
                parseEnum(InstanceLevel.class, model.getInstanciaAtual()),
                parseEnum(RecursalAuthority.class, model.getAutoridadeAtual()),
                parseEnum(RecursalTransitionEvent.class, model.getLastEvent()),
                model.getCurrentRevision() == null ? 0 : model.getCurrentRevision(),
                model.getTotalTransitions() == null ? 0 : model.getTotalTransitions(),
                model.getIteracoesEmbargos() == null ? 0 : model.getIteracoesEmbargos(),
                Boolean.TRUE.equals(model.getTransitadoEmJulgado()),
                model.getLastActor(),
                model.getLastTransitionAt(),
                projectionService.toSlaSnapshot(model),
                model.getCreatedAt(),
                model.getUpdatedAt()
        );
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

    private String fallbackHaystack(RecursalProcessIntegrationState projection,
                                    RecursalSlaSnapshot sla,
                                    RecursalPrecedentTrace precedentTrace,
                                    RecursalStateSnapshot snapshot) {
        return Stream.of(
                        projection.getRecursoId(),
                        projection.getNumeroProcesso(),
                        projection.getSpeciesCode(),
                        projection.getProfileName(),
                        enumName(projection.getCurrentState()),
                        enumName(projection.getTribunalAtual()),
                        enumName(projection.getTribunalDetalhadoAtual()),
                        enumName(projection.getAutoridadeAtual()),
                        projection.getLastActor(),
                        projection.getProcesso() == null ? null : projection.getProcesso().getTribunal(),
                        projection.getProcesso() == null ? null : projection.getProcesso().getVara(),
                        projection.getProcesso() == null ? null : projection.getProcesso().getComarca(),
                        projection.getProcesso() == null ? null : projection.getProcesso().getUf(),
                        projection.getProcesso() == null ? null : projection.getProcesso().getAssunto(),
                        precedentTrace.precedenteCodigo(),
                        precedentTrace.precedenteTribunal(),
                        precedentTrace.precedenteTema(),
                        precedentTrace.fundamentoDistincao(),
                        snapshot != null && snapshot.sobrestadoPorPrecedente() ? "sobrestado precedente" : null,
                        precedentTrace.aplicado() ? "precedente aplicado" : null,
                        precedentTrace.distinguido() ? "caso distinguido" : null,
                        sla == null ? null : sla.severidade(),
                        sla == null ? null : sla.fundamentoLegal())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .reduce((left, right) -> left + " | " + right)
                .orElse("");
    }

    private static boolean queryMatches(String haystack, String query) {
        if (!hasText(query)) {
            return true;
        }
        String normalizedHaystack = haystack == null ? "" : haystack.toLowerCase(Locale.ROOT);
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        if (normalizedHaystack.contains(normalizedQuery)) {
            return true;
        }
        for (String token : normalizedQuery.split("\\s+")) {
            if (!token.isBlank() && !normalizedHaystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right.trim());
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && needle != null && haystack.toLowerCase(Locale.ROOT).contains(needle.trim().toLowerCase(Locale.ROOT));
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
