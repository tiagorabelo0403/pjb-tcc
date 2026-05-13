package com.tcc.pjb.backend.modules.laiane.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.advisory.ContextualPrecedentAdvisoryService;
import com.tcc.pjb.backend.model.dto.twin.PrecedenteEvidenceDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeRadarHitDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeRadarJurisprudenciaResponse;
import com.tcc.pjb.backend.modules.laiane.util.LaianeRoleGuard;
import com.tcc.pjb.backend.service.jurisprudencia.graph.CitationExtractor;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import com.tcc.pjb.backend.service.semantic.SemanticPrecedentSearchService;

@Service
public class LaianeJudgeRadarJurisprudenciaService {

    private final LaianeRoleGuard guard;
    private final ProcessoRepository processoRepository;
    private final ProcessoRitoSnapshotService processoRitoSnapshotService;
    private final SemanticPrecedentSearchService semanticPrecedentSearchService;
    private final CitationExtractor citationExtractor;
    private final ContextualPrecedentAdvisoryService contextualPrecedentAdvisoryService;

    public LaianeJudgeRadarJurisprudenciaService(LaianeRoleGuard guard,
                                                 ProcessoRepository processoRepository,
                                                 ProcessoRitoSnapshotService processoRitoSnapshotService,
                                                 SemanticPrecedentSearchService semanticPrecedentSearchService,
                                                 CitationExtractor citationExtractor,
                                                 ContextualPrecedentAdvisoryService contextualPrecedentAdvisoryService) {
        this.guard = Objects.requireNonNull(guard);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoRitoSnapshotService = Objects.requireNonNull(processoRitoSnapshotService);
        this.semanticPrecedentSearchService = Objects.requireNonNull(semanticPrecedentSearchService);
        this.citationExtractor = Objects.requireNonNull(citationExtractor);
        this.contextualPrecedentAdvisoryService = Objects.requireNonNull(contextualPrecedentAdvisoryService);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "jurisprudencia_semantica", key = "'judge-radar:' + #processoId")
    public LaianeJudgeRadarJurisprudenciaResponse analisar(Long processoId) {
        guard.requireMagistratura();
        if (processoId == null) {
            throw new IllegalArgumentException("processoId é obrigatório");
        }
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado"));

        var ritoSnapshot = processoRitoSnapshotService.resolve(processo, null);
        String query = buildQuery(processo);
        List<Precedente> precedentes = semanticPrecedentSearchService.semanticSearch(
                processo.getRamoDireito(),
                ritoSnapshot.rito(),
                query,
                12
        );
        if (precedentes.isEmpty()) {
            precedentes = processo.getRamoDireito() == null
                    ? List.of()
                    : semanticPrecedentSearchService.semanticSearch(processo.getRamoDireito(), ritoSnapshot.rito(), processo.getAssunto(), 8);
        }

        List<PrecedenteEvidenceDto> evidence = precedentes.stream()
                .map(this::toEvidence)
                .toList();
        var advisory = contextualPrecedentAdvisoryService.analyzeProcess(
                processo,
                ritoSnapshot.ritoCode(),
                null,
                evidence,
                null,
                null
        );

        List<LaianeJudgeRadarHitDto> hits = new ArrayList<>(precedentes.size());
        for (int i = 0; i < precedentes.size(); i++) {
            Precedente precedente = precedentes.get(i);
            List<String> citacoes = citationExtractor.extract(
                            precedente.getTitulo(),
                            precedente.getTese(),
                            precedente.getEmentaResumo())
                    .stream()
                    .limit(5)
                    .map(ref -> ref.targetRef())
                    .toList();
            hits.add(new LaianeJudgeRadarHitDto(
                    precedente.getId(),
                    precedente.getFonte() != null ? precedente.getFonte().name() : null,
                    precedente.getTipo() != null ? precedente.getTipo().name() : null,
                    precedente.getIdentificador(),
                    precedente.getTitulo(),
                    precedente.getUrlReferencia(),
                    precedente.getDataPublicacao(),
                    estimateScore(precedente, query, i),
                    citacoes
            ));
        }

        return new LaianeJudgeRadarJurisprudenciaResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                ritoSnapshot.ritoCode(),
                processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                advisory.status(),
                advisory.adherence(),
                advisory.recommendedQueries(),
                advisory.cautionPoints(),
                advisory.narrativeAngles(),
                List.copyOf(hits),
                Instant.now()
        );
    }

    private PrecedenteEvidenceDto toEvidence(Precedente precedente) {
        return new PrecedenteEvidenceDto(
                precedente.getId(),
                precedente.getFonte() != null ? precedente.getFonte().name() : null,
                precedente.getTipo() != null ? precedente.getTipo().name() : null,
                precedente.getIdentificador(),
                precedente.getTitulo(),
                precedente.getUrlReferencia(),
                precedente.getDataPublicacao()
        );
    }

    private String buildQuery(Processo processo) {
        StringBuilder builder = new StringBuilder(512);
        append(builder, processo.getClasseProcessual());
        append(builder, processo.getAssunto());
        append(builder, processo.getObjetoProcessual());
        append(builder, processo.getPedidoPrincipal());
        append(builder, processo.getPedidosConsolidados());
        append(builder, processo.getResumoIA());
        append(builder, processo.getMaterialProbatorioResumo());
        if (builder.length() == 0) {
            append(builder, processo.getNumeroProcesso());
        }
        return builder.toString();
    }

    private void append(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ').append('·').append(' ');
        }
        builder.append(value.trim());
    }

    private double estimateScore(Precedente precedente, String query, int index) {
        double score = Math.max(0.35d, 0.96d - (index * 0.06d));
        String haystack = ((precedente.getTitulo() == null ? "" : precedente.getTitulo()) + " "
                + (precedente.getTese() == null ? "" : precedente.getTese()) + " "
                + (precedente.getEmentaResumo() == null ? "" : precedente.getEmentaResumo())).toLowerCase();
        for (String token : query.toLowerCase().split("\\W+")) {
            if (token.length() < 4) {
                continue;
            }
            if (haystack.contains(token)) {
                score += 0.01d;
            }
        }
        return Math.min(0.99d, Math.round(score * 100d) / 100d);
    }
}
