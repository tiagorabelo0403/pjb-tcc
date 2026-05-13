package com.tcc.pjb.backend.core.comunicacao.institucional.registry.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalPublicRecognitionGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalLifecycle;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationRequestStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOperationalLifecycleStage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalOperationalLifecycleApplicationService {

    private static final Duration LIST_CACHE_TTL = Duration.ofSeconds(20);

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalAffiliationRequestStateRepository requestRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService;
    private final AtomicReference<CachedLifecycleList> listCache = new AtomicReference<>();

    public InstitutionalOperationalLifecycleApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                              InstitutionalAffiliationRequestStateRepository requestRepository,
                                                              InstitutionalNominationStateRepository nominationRepository,
                                                              InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.requestRepository = Objects.requireNonNull(requestRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.publicRecognitionGateApplicationService = Objects.requireNonNull(publicRecognitionGateApplicationService);
    }

    public List<InstitutionalOperationalLifecycle> listar() {
        CachedLifecycleList cache = listCache.get();
        if (isFresh(cache)) {
            return cache.items();
        }
        Instant now = Instant.now();
        List<InstitutionalAffiliation> affiliations = affiliationRepository.findAll();
        List<InstitutionalAffiliationRequest> boundRequests = requestRepository.findLatestByMaterializedAffiliationIds(
                affiliations.stream().map(InstitutionalAffiliation::affiliationId).toList());
        List<InstitutionalAffiliationRequest> pendingRequests = requestRepository.findWithoutMaterializedAffiliation();
        List<InstitutionalNomination> nominations = nominationRepository.findByAffiliationIds(affiliations.stream().map(InstitutionalAffiliation::affiliationId).toList());
        Map<String, InstitutionalAffiliationRequest> requestByAffiliation = boundRequests.stream()
                .collect(java.util.stream.Collectors.toMap(
                        InstitutionalAffiliationRequest::materializedAffiliationId,
                        item -> item,
                        (left, right) -> right,
                        LinkedHashMap::new));
        List<InstitutionalOperationalLifecycle> result = new ArrayList<>();
        for (InstitutionalAffiliation affiliation : affiliations) {
            result.add(mapAffiliation(affiliation, requestByAffiliation.get(affiliation.affiliationId()), nominations, now));
        }
        for (InstitutionalAffiliationRequest request : pendingRequests) {
            if (request.materializedAffiliationId() == null || request.materializedAffiliationId().isBlank()) {
                result.add(mapPendingRequest(request, now));
            }
        }
        List<InstitutionalOperationalLifecycle> ordered = result.stream()
                .sorted(Comparator.comparing(InstitutionalOperationalLifecycle::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(item -> item.orgaoSigla() == null ? "" : item.orgaoSigla())
                        .thenComparing(item -> item.unidadeCodigo() == null ? "" : item.unidadeCodigo()))
                .toList();
        listCache.set(new CachedLifecycleList(ordered, Instant.now().plus(LIST_CACHE_TTL)));
        return ordered;
    }

    public Optional<InstitutionalOperationalLifecycle> detalharAfiliacao(String affiliationId) {
        CachedLifecycleList cache = listCache.get();
        if (isFresh(cache)) {
            Optional<InstitutionalOperationalLifecycle> cached = cache.items().stream()
                    .filter(item -> Objects.equals(item.affiliationId(), affiliationId))
                    .findFirst();
            if (cached.isPresent()) {
                return cached;
            }
        }
        Instant now = Instant.now();
        return affiliationRepository.findByAffiliationId(affiliationId)
                .map(item -> mapAffiliation(
                        item,
                        requestRepository.findLatestByMaterializedAffiliationId(item.affiliationId()).orElse(null),
                        nominationRepository.findByAffiliationId(item.affiliationId()),
                        now));
    }

    public Optional<InstitutionalOperationalLifecycle> detalharSolicitacao(String requestId) {
        CachedLifecycleList cache = listCache.get();
        if (isFresh(cache)) {
            Optional<InstitutionalOperationalLifecycle> cached = cache.items().stream()
                    .filter(item -> Objects.equals(item.requestId(), requestId))
                    .findFirst();
            if (cached.isPresent()) {
                return cached;
            }
        }
        Instant now = Instant.now();
        return requestRepository.findByRequestId(requestId)
                .map(item -> item.materializedAffiliationId() == null || item.materializedAffiliationId().isBlank()
                        ? mapPendingRequest(item, now)
                        : detalharAfiliacao(item.materializedAffiliationId()).orElseGet(() -> mapPendingRequest(item, now)));
    }


    private boolean isFresh(CachedLifecycleList cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private InstitutionalOperationalLifecycle mapAffiliation(InstitutionalAffiliation affiliation,
                                                             InstitutionalAffiliationRequest request,
                                                             List<InstitutionalNomination> allNominations,
                                                             Instant now) {
        List<InstitutionalNomination> nominations = allNominations.stream()
                .filter(item -> item.affiliationId().equals(affiliation.affiliationId()))
                .toList();
        List<InstitutionalNomination> activeNominations = nominations.stream()
                .filter(item -> item.ativaEm(now))
                .toList();
        List<String> caixas = nominations.stream()
                .map(item -> item.unidadeCodigo() + "::" + item.caixaCodigo())
                .distinct()
                .sorted()
                .toList();
        List<String> activeCaixas = activeNominations.stream()
                .map(item -> item.unidadeCodigo() + "::" + item.caixaCodigo())
                .distinct()
                .sorted()
                .toList();
        long totalAdministradores = activeNominations.stream()
                .filter(item -> item.nominationRole() != null && item.nominationRole().isGestaoMestre())
                .count();
        var recognition = publicRecognitionGateApplicationService.avaliarAfiliacao(affiliation);
        boolean ready = affiliation.ativa() && recognition.recognized() && !activeNominations.isEmpty() && !activeCaixas.isEmpty() && totalAdministradores > 0;
        InstitutionalOperationalLifecycleStage stage = resolveStage(affiliation.status(), request == null ? null : request.status(), activeNominations.isEmpty(), ready);
        return new InstitutionalOperationalLifecycle(
                affiliation.affiliationId(),
                request == null ? null : request.requestId(),
                affiliation.destinatarioKind(),
                affiliation.organizationScope(),
                affiliation.orgaoSigla(),
                affiliation.orgaoNome(),
                affiliation.unidadeCodigo(),
                affiliation.unidadeNome(),
                affiliation.uf(),
                affiliation.comarca(),
                affiliation.cnpj(),
                affiliation.esferaAdministrativa(),
                affiliation.ramosMateriais(),
                affiliation.abrangenciasTerritoriais(),
                affiliation.dominioInstitucional(),
                affiliation.autoridadeAderenteCargo(),
                stage,
                affiliation.ativa(),
                !activeNominations.isEmpty(),
                ready,
                nominations.size(),
                activeNominations.size(),
                caixas.size(),
                activeCaixas.size(),
                totalAdministradores,
                caixas,
                choose(affiliation.canaisHabilitados(), request == null ? List.of() : request.canaisHabilitados()),
                choose(affiliation.politicaCiencia(), request == null ? List.of() : request.politicaCiencia()),
                choose(affiliation.sla(), request == null ? List.of() : request.sla()),
                choose(affiliation.regrasFallback(), request == null ? List.of() : request.regrasFallback()),
                choose(affiliation.conveniosIntegracoes(), request == null ? List.of() : request.conveniosIntegracoes()),
                trilhosAutenticacao(affiliation),
                eixosAutorizacao(),
                fundamentos(affiliation, request, recognition, stage, nominations, activeNominations, ready),
                max(affiliation.updatedAt(), request == null ? null : request.updatedAt())
        );
    }

    private InstitutionalOperationalLifecycle mapPendingRequest(InstitutionalAffiliationRequest request, Instant now) {
        var recognition = publicRecognitionGateApplicationService.avaliarSolicitacao(request);
        InstitutionalOperationalLifecycleStage stage = switch (request.status()) {
            case RASCUNHO, PENDENTE_VALIDACAO -> InstitutionalOperationalLifecycleStage.ETAPA_HABILITACAO_INSTITUCIONAL;
            case EM_HOMOLOGACAO -> InstitutionalOperationalLifecycleStage.ETAPA_HOMOLOGACAO_INSTITUCIONAL;
            case REJEITADA, REVOGADA -> InstitutionalOperationalLifecycleStage.SUSPENSA;
            case HOMOLOGADA -> InstitutionalOperationalLifecycleStage.ETAPA_VINCULACAO_USUARIOS;
        };
        return new InstitutionalOperationalLifecycle(
                request.materializedAffiliationId(),
                request.requestId(),
                request.destinatarioKind(),
                request.organizationScope(),
                request.orgaoSigla(),
                request.orgaoNome(),
                request.unidadeCodigo(),
                request.unidadeNome(),
                request.uf(),
                request.comarca(),
                request.cnpj(),
                request.esferaAdministrativa(),
                request.ramosMateriais(),
                request.abrangenciasTerritoriais(),
                request.dominioInstitucional(),
                request.autoridadeAderenteCargo(),
                stage,
                false,
                false,
                false,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                sanitize(request.canaisHabilitados()),
                sanitize(request.politicaCiencia()),
                sanitize(request.sla()),
                sanitize(request.regrasFallback()),
                sanitize(request.conveniosIntegracoes()),
                trilhosAutenticacao(request),
                eixosAutorizacao(),
                fundamentos(request, recognition, stage, now),
                request.updatedAt()
        );
    }

    private InstitutionalOperationalLifecycleStage resolveStage(InstitutionalAffiliationStatus affiliationStatus,
                                                                InstitutionalAffiliationRequestStatus requestStatus,
                                                                boolean semNomeacoesAtivas,
                                                                boolean ready) {
        if (affiliationStatus == InstitutionalAffiliationStatus.SUSPENSA || affiliationStatus == InstitutionalAffiliationStatus.REVOGADA) {
            return InstitutionalOperationalLifecycleStage.SUSPENSA;
        }
        if (affiliationStatus == InstitutionalAffiliationStatus.SOLICITADA) {
            return InstitutionalOperationalLifecycleStage.ETAPA_HABILITACAO_INSTITUCIONAL;
        }
        if (affiliationStatus == InstitutionalAffiliationStatus.EM_VALIDACAO_PJB || requestStatus == InstitutionalAffiliationRequestStatus.EM_HOMOLOGACAO) {
            return InstitutionalOperationalLifecycleStage.ETAPA_HOMOLOGACAO_INSTITUCIONAL;
        }
        if (semNomeacoesAtivas) {
            return InstitutionalOperationalLifecycleStage.ETAPA_VINCULACAO_USUARIOS;
        }
        return ready ? InstitutionalOperationalLifecycleStage.OPERACAO_ATIVA : InstitutionalOperationalLifecycleStage.ETAPA_ATIVACAO_OPERACIONAL;
    }

    private List<String> trilhosAutenticacao(InstitutionalAffiliation affiliation) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("identidade_pessoal_forte");
        values.add("mfa_perfis_sensiveis");
        values.add("sso_institucional_quando_houver");
        values.add("trilha_forense_completa");
        if (affiliation.requerCertificadoICP()) {
            values.add("certificado_qualificado_quando_o_ato_exigir");
        }
        if (affiliation.restringeCertificadoRedeInstitucional()) {
            values.add("rede_institucional_ou_autorizacao_remota");
        }
        return List.copyOf(values);
    }

    private List<String> trilhosAutenticacao(InstitutionalAffiliationRequest request) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("identidade_pessoal_forte");
        values.add("mfa_perfis_sensiveis");
        values.add("sso_institucional_quando_houver");
        values.add("trilha_forense_completa");
        if (request.requerCertificadoICP()) {
            values.add("certificado_qualificado_quando_o_ato_exigir");
        }
        if (request.restringeCertificadoRedeInstitucional()) {
            values.add("rede_institucional_ou_autorizacao_remota");
        }
        return List.copyOf(values);
    }

    private List<String> eixosAutorizacao() {
        return List.of(
                "orgao",
                "unidade",
                "caixa",
                "papel",
                "capacidade",
                "plantao",
                "substituicao",
                "delegacao"
        );
    }

    private List<String> fundamentos(InstitutionalAffiliation affiliation,
                                     InstitutionalAffiliationRequest request,
                                     com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse recognition,
                                     InstitutionalOperationalLifecycleStage stage,
                                     List<InstitutionalNomination> nominations,
                                     List<InstitutionalNomination> activeNominations,
                                     boolean ready) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("fluxo=orgao_unidade_caixa_capacidade");
        values.add("modelo_responsabilidade=identidade_pessoal_raiz_com_contexto_institucional_delegado");
        values.add("stage=" + stage.name());
        values.add("nomeacoes_total=" + nominations.size());
        values.add("nomeacoes_ativas=" + activeNominations.size());
        values.add("ativacao_pronta=" + ready);
        values.add("reconhecimento_publico_status=" + recognition.statusCode());
        values.add("reconhecimento_publico_reconhecida=" + recognition.recognized());
        values.add("reconhecimento_publico_autoativavel=" + recognition.autoActivatable());
        values.addAll(recognition.blockers().stream().map(item -> "reconhecimento_publico_blocker=" + item).toList());
        if (affiliation.ativa()) {
            values.add("afiliacao_homologada");
        }
        if (request != null) {
            values.add("solicitacao_origem=" + request.requestId());
        }
        values.addAll(affiliation.fundamentos());
        if (request != null) {
            values.addAll(request.fundamentos());
        }
        return List.copyOf(values);
    }

    private List<String> fundamentos(InstitutionalAffiliationRequest request,
                                     com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse recognition,
                                     InstitutionalOperationalLifecycleStage stage,
                                     Instant now) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("fluxo=orgao_unidade_caixa_capacidade");
        values.add("modelo_responsabilidade=identidade_pessoal_raiz_com_contexto_institucional_delegado");
        values.add("stage=" + stage.name());
        values.add("request_status=" + request.status().name());
        values.add("snapshot=" + now.toString());
        values.add("reconhecimento_publico_status=" + recognition.statusCode());
        values.add("reconhecimento_publico_reconhecida=" + recognition.recognized());
        values.add("reconhecimento_publico_homologacao_humana=" + recognition.humanReviewRequired());
        values.addAll(recognition.blockers().stream().map(item -> "reconhecimento_publico_blocker=" + item).toList());
        values.addAll(request.fundamentos());
        return List.copyOf(values);
    }

    private List<String> choose(List<String> primary, List<String> fallback) {
        List<String> sanitizedPrimary = sanitize(primary);
        return sanitizedPrimary.isEmpty() ? sanitize(fallback) : sanitizedPrimary;
    }

    private List<String> sanitize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private Instant max(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }


    private record CachedLifecycleList(List<InstitutionalOperationalLifecycle> items, Instant expiresAt) { }
}
