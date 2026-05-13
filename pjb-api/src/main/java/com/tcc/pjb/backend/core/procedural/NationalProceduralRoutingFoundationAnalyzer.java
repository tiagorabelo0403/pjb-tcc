package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.service.competencia.CompetenceResolverService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate.GateIssue;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate.GateResult;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate.Status;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecisionResolver;

@Component
public class NationalProceduralRoutingFoundationAnalyzer {

    private final CanonicalRitoSelector canonicalRitoSelector;
    private final CompetenceResolverService competenceResolverService;
    private final NationalProceduralCompetenceRequestFactory competenceRequestFactory;
    private final NationalProceduralTetoDiagnosticResolver tetoDiagnosticResolver;
    private final NationalProceduralActionProfileResolver actionProfileResolver;
    private final NationalProceduralJuizadoDecisionResolver juizadoDecisionResolver;
    private final NationalProceduralPartyProfileResolver partyProfileResolver;
    private final NationalProceduralHeuristicRitoResolver heuristicRitoResolver;
    private final NationalProceduralProbatoryProfileResolver probatoryProfileResolver;

    public NationalProceduralRoutingFoundationAnalyzer(CanonicalRitoSelector canonicalRitoSelector,
                                                       CompetenceResolverService competenceResolverService,
                                                       NationalProceduralCompetenceRequestFactory competenceRequestFactory,
                                                       NationalProceduralTetoDiagnosticResolver tetoDiagnosticResolver,
                                                       NationalProceduralActionProfileResolver actionProfileResolver,
                                                       NationalProceduralJuizadoDecisionResolver juizadoDecisionResolver,
                                                       NationalProceduralPartyProfileResolver partyProfileResolver,
                                                       NationalProceduralHeuristicRitoResolver heuristicRitoResolver,
                                                       NationalProceduralProbatoryProfileResolver probatoryProfileResolver) {
        this.canonicalRitoSelector = Objects.requireNonNull(canonicalRitoSelector);
        this.competenceResolverService = Objects.requireNonNull(competenceResolverService);
        this.competenceRequestFactory = Objects.requireNonNull(competenceRequestFactory);
        this.tetoDiagnosticResolver = Objects.requireNonNull(tetoDiagnosticResolver);
        this.actionProfileResolver = Objects.requireNonNull(actionProfileResolver);
        this.juizadoDecisionResolver = Objects.requireNonNull(juizadoDecisionResolver);
        this.partyProfileResolver = Objects.requireNonNull(partyProfileResolver);
        this.heuristicRitoResolver = Objects.requireNonNull(heuristicRitoResolver);
        this.probatoryProfileResolver = Objects.requireNonNull(probatoryProfileResolver);
    }

    NationalProceduralRoutingFoundationResolution analyze(Map<String, Object> payload, String sourceLabel) {
        Map<String, Object> safePayload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        String corpus = NationalProceduralRoutingSupport.normalize(NationalProceduralRoutingSupport.buildCorpus(safePayload));
        NationalProceduralPartyProfile partyProfile = partyProfileResolver.resolve(safePayload, corpus);
        com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual heuristicRito = heuristicRitoResolver.resolve(safePayload, corpus, partyProfile);
        CanonicalRitoSelector.SelectedRito selectedRito = canonicalRitoSelector.select(
                safePayload,
                heuristicRito != null ? heuristicRito.name() : null,
                sourceLabel
        );
        if (selectedRito == null) {
            GateResult fallbackGate = new GateResult(
                    Status.CANONICAL_CONTEXT_INCOMPLETE.name(),
                    false,
                    List.of(new GateIssue(
                            Status.CANONICAL_CONTEXT_INCOMPLETE,
                            "canonicalRitoSelector",
                            "Selector canônico não retornou rito; fallback explícito aplicado com revisão obrigatória.",
                            true
                    )),
                    Map.of("fallback", "selector_returned_null"),
                    Instant.now()
            );
            selectedRito = new CanonicalRitoSelector.SelectedRito(
                    Instant.now(),
                    sourceLabel == null || sourceLabel.isBlank() ? "unspecified" : sourceLabel,
                    new ProceduralCanonicalResolver.CanonicalContext(),
                    fallbackGate,
                    com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.COMUM_ORDINARIO,
                    "LEGACY_DEFAULT_APPLIED",
                    false,
                    true,
                    Map.of("fallback", "selector_returned_null")
            );
        }
        ProceduralCanonicalResolver.CanonicalContext canonical = selectedRito.canonicalContext();
        CompetenceResolveResponse competence = competenceResolverService.resolve(
                competenceRequestFactory.create(safePayload, canonical, partyProfile)
        );
        NationalProceduralActionProfile actionProfile = actionProfileResolver.resolve(safePayload, canonical, corpus, partyProfile);
        String probatoryProfile = probatoryProfileResolver.resolve(safePayload, corpus);
        TetoProcessualService.DiagnosticoTetoProcessual teto = tetoDiagnosticResolver.resolve(
                new NationalProceduralTetoDiagnosticContext(safePayload, competence, canonical, selectedRito)
        );
        NationalProceduralJuizadoDecision juizadoDecision = juizadoDecisionResolver.resolve(
                safePayload,
                competence,
                actionProfile,
                partyProfile,
                teto,
                corpus
        );
        return new NationalProceduralRoutingFoundationResolution(
                safePayload,
                corpus,
                sourceLabel,
                partyProfile,
                selectedRito,
                canonical,
                competence,
                actionProfile,
                probatoryProfile,
                teto,
                juizadoDecision
        );
    }
}
