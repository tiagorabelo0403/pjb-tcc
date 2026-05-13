package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.EstruturaCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDelegationAssignment;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDraftManifestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalOperationalCoverageRule;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalTriageSuggestion;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalTriageSuggestionDashboard;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDelegationAssignmentStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDraftManifestationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalOperationalCoverageRuleStateRepository;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoCaixaInstitucional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalTriageSuggestionApplicationService {

    private final InstitutionalInboxApplicationService inboxApplicationService;
    private final InstitutionalInboxStateRepository inboxRepository;
    private final InstitutionalDelegationAssignmentStateRepository delegationRepository;
    private final InstitutionalDraftManifestationStateRepository draftRepository;
    private final InstitutionalOperationalCoverageRuleStateRepository coverageRepository;
    private final CatalogoInstitucionalUnificadoService catalogService;
    private final EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService;

    public InstitutionalTriageSuggestionApplicationService(InstitutionalInboxApplicationService inboxApplicationService,
                                                          InstitutionalInboxStateRepository inboxRepository,
                                                          InstitutionalDelegationAssignmentStateRepository delegationRepository,
                                                          InstitutionalDraftManifestationStateRepository draftRepository,
                                                          InstitutionalOperationalCoverageRuleStateRepository coverageRepository,
                                                          CatalogoInstitucionalUnificadoService catalogService,
                                                          EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService) {
        this.inboxApplicationService = Objects.requireNonNull(inboxApplicationService);
        this.inboxRepository = Objects.requireNonNull(inboxRepository);
        this.delegationRepository = Objects.requireNonNull(delegationRepository);
        this.draftRepository = Objects.requireNonNull(draftRepository);
        this.coverageRepository = Objects.requireNonNull(coverageRepository);
        this.catalogService = Objects.requireNonNull(catalogService);
        this.estruturaCaixaInstitucionalService = Objects.requireNonNull(estruturaCaixaInstitucionalService);
    }

    @Transactional(readOnly = true)
    public InstitutionalTriageSuggestionDashboard suggest(String expedicaoUuid) {
        InstitutionalInboxItem item = inboxApplicationService.loadVisible(expedicaoUuid);
        Instant now = Instant.now();
        List<InstitutionalTriageSuggestion> suggestions = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        Map<Long, Long> workloadByUser = new LinkedHashMap<>();
        inboxRepository.findByUnidadeCodigo(item.unidadeCodigo()).stream()
                .filter(i -> i.status() != StatusComunicacaoInstitucional.CUMPRIDA)
                .filter(i -> i.atribuidoUsuarioId() != null)
                .forEach(i -> workloadByUser.merge(i.atribuidoUsuarioId(), 1L, Long::sum));

        if (item.atribuidoUsuarioId() != null) {
            suggestions.add(userSuggestion(item, item.atribuidoUsuarioId(), "USUARIO_ATRIBUIDO", 95,
                    List.of("Expediente já possui atribuição ativa.", "Manter continuidade reduz contexto perdido."), workloadByUser));
        }
        for (InstitutionalOperationalCoverageRule rule : coverageRepository.findByUnidadeCodigo(item.unidadeCodigo())) {
            if (rule.caixaCodigo().equalsIgnoreCase(item.caixaCodigoAtual()) && rule.ativaEm(now)) {
                suggestions.add(userSuggestion(item, rule.coberturaUsuarioId(), rule.tipoCobertura().name(), 92,
                        List.of("Cobertura operacional vigente para a caixa.", "Janela de substituição/delegação programada ativa."), workloadByUser));
                notes.add("Há cobertura operacional ativa para a caixa atual.");
            }
        }
        for (InstitutionalDelegationAssignment assignment : delegationRepository.findByExpedicaoUuid(expedicaoUuid)) {
            if (assignment.ativaEm(now)) {
                suggestions.add(userSuggestion(item, assignment.delegadoUsuarioId(), assignment.tipoFluxo().name(), assignment.tipoFluxo().isSubstituicao() ? 90 : 88,
                        List.of("Delegação/substituição já registrada no expediente.", "Capacidades transitórias disponíveis."), workloadByUser));
            }
        }
        draftRepository.findByExpedicaoUuid(expedicaoUuid).stream()
                .filter(draft -> draft.aprovadorUsuarioId() != null)
                .max(Comparator.comparing(InstitutionalDraftManifestation::updatedAt))
                .ifPresent(draft -> suggestions.add(userSuggestion(item, draft.aprovadorUsuarioId(), "APROVADOR_MINUTA", 84,
                        List.of("Minuta já circula para aprovação.", "Encaminhar ao aprovador tende a reduzir retrabalho."), workloadByUser)));

        UnidadeInstitucional unidade = catalogService.listarPorTipo(item.destinatarioKind()).stream()
                .filter(candidate -> candidate.codigo().equalsIgnoreCase(item.unidadeCodigo()))
                .findFirst()
                .orElse(null);
        if (unidade != null) {
            estruturaCaixaInstitucionalService.expandir(unidade).stream()
                    .filter(caixa -> !caixa.codigo().equalsIgnoreCase(item.caixaCodigoAtual()))
                    .filter(caixa -> caixa.tipoCaixa() == TipoCaixaInstitucional.CAIXA_TRIAGEM || caixa.tipoCaixa() == TipoCaixaInstitucional.CAIXA_COORDENACAO || caixa.tipoCaixa() == TipoCaixaInstitucional.CAIXA_GABINETE_FUNCIONAL)
                    .forEach(caixa -> suggestions.add(new InstitutionalTriageSuggestion(
                            deterministic(item.expedicaoUuid(), caixa.codigo(), "CAIXA"),
                            item.expedicaoUuid(),
                            item.unidadeCodigo(),
                            item.caixaCodigoAtual(),
                            caixa.codigo(),
                            null,
                            "CAIXA_" + caixa.tipoCaixa().name(),
                            scoreForBox(caixa),
                            List.of(
                                    "Caixa sugerida pela topologia institucional da unidade.",
                                    caixa.tipoCaixa() == TipoCaixaInstitucional.CAIXA_TRIAGEM ? "Triagem inteligente para recebimento em lote e distribuição." : "Caixa especializada para coordenação/gabinete."
                            )
                    )));
        }
        if (suggestions.isEmpty()) {
            notes.add("Nenhum candidato humano foi encontrado. Sugerir atuação pela caixa de triagem ou coordenação.");
        }
        List<InstitutionalTriageSuggestion> ranked = suggestions.stream()
                .collect(java.util.stream.Collectors.toMap(InstitutionalTriageSuggestion::suggestionId, s -> s, (a, b) -> a.score() >= b.score() ? a : b, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparingInt(InstitutionalTriageSuggestion::score).reversed())
                .limit(10)
                .toList();
        return new InstitutionalTriageSuggestionDashboard(item.expedicaoUuid(), item.unidadeCodigo(), item.caixaCodigoAtual(), ranked, List.copyOf(notes), now);
    }

    private InstitutionalTriageSuggestion userSuggestion(InstitutionalInboxItem item,
                                                         Long usuarioId,
                                                         String tipo,
                                                         int baseScore,
                                                         List<String> fundamentos,
                                                         Map<Long, Long> workloadByUser) {
        long workload = workloadByUser.getOrDefault(usuarioId, 0L);
        int score = Math.max(40, baseScore - Math.toIntExact(Math.min(30, workload * 3)));
        List<String> reasons = new ArrayList<>(fundamentos);
        reasons.add("Carga atual estimada do usuário: " + workload + " expediente(s) aberto(s).");
        return new InstitutionalTriageSuggestion(
                deterministic(item.expedicaoUuid(), String.valueOf(usuarioId), tipo),
                item.expedicaoUuid(),
                item.unidadeCodigo(),
                item.caixaCodigoAtual(),
                item.caixaCodigoAtual(),
                usuarioId,
                tipo.toUpperCase(Locale.ROOT),
                score,
                reasons
        );
    }

    private int scoreForBox(CaixaInstitucional caixa) {
        return switch (caixa.tipoCaixa()) {
            case CAIXA_TRIAGEM -> 82;
            case CAIXA_COORDENACAO -> 74;
            case CAIXA_GABINETE_FUNCIONAL -> 78;
            default -> 60;
        };
    }

    private String deterministic(String expedicaoUuid, String target, String kind) {
        return UUID.nameUUIDFromBytes((expedicaoUuid + "|" + target + "|" + kind).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }
}
