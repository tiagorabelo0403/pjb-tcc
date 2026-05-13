package com.tcc.pjb.backend.service.processual.recursal.operational;

import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalDecisionCarryOverAssembler;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalFactType;
import com.tcc.pjb.backend.core.kernel.recursal.model.AutuationPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalGraphResponse;
import com.tcc.pjb.backend.model.dto.processual.routing.NationalProcessRoutingRequest;
import com.tcc.pjb.backend.model.dto.processual.routing.NationalProcessRoutingResponse;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.surface.ProcessualOperationalSurfaceFacadeService;
import com.tcc.pjb.backend.service.recursal.RecursalFactIdempotentIngestService;
import com.tcc.pjb.backend.service.recursal.RecursalIntelligenceFacadeService;
import java.nio.charset.StandardCharsets;
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
public class RecursalAutuacaoDestinoService {

    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final RecursalIntelligenceFacadeService recursalIntelligenceFacadeService;
    private final RecursalFactIdempotentIngestService recursalFactIdempotentIngestService;
    private final ProcessualOperationalSurfaceFacadeService processualOperationalSurfaceFacadeService;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final DocumentoPaginaRepository documentoPaginaRepository;

    public RecursalAutuacaoDestinoService(ProcessoRepository processoRepository,
                                          PjbAuthorizationService authorizationService,
                                          RecursalIntelligenceFacadeService recursalIntelligenceFacadeService,
                                          RecursalFactIdempotentIngestService recursalFactIdempotentIngestService,
                                          ProcessualOperationalSurfaceFacadeService processualOperationalSurfaceFacadeService,
                                          DocumentoProcessualRepository documentoProcessualRepository,
                                          DocumentoPaginaRepository documentoPaginaRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.recursalIntelligenceFacadeService = Objects.requireNonNull(recursalIntelligenceFacadeService);
        this.recursalFactIdempotentIngestService = Objects.requireNonNull(recursalFactIdempotentIngestService);
        this.processualOperationalSurfaceFacadeService = Objects.requireNonNull(processualOperationalSurfaceFacadeService);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.documentoPaginaRepository = Objects.requireNonNull(documentoPaginaRepository);
    }

    @Transactional
    public Map<String, Object> registrar(Long processoId,
                                         String numeroAutuacaoDestino,
                                         InstanceLevel instanciaDestinoHint,
                                         String tribunalDestinoHint,
                                         String unidadeDistribuicao,
                                         String observacoes) {
        if (processoId == null) {
            throw new IllegalArgumentException("processoId é obrigatório para a autuação recursal de destino.");
        }
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireWriteProcesso(processo);

        List<String> avisos = new ArrayList<>();
        String numeroOrigem = safeNumeroProcesso(processo);
        String numeroDestino = resolveNumeroDestino(numeroAutuacaoDestino, numeroOrigem, avisos);
        RecursalGraphResponse graphBefore = safeGraphSnapshot(processoId, avisos);
        TargetResolution target = resolveTarget(processo, graphBefore, instanciaDestinoHint, tribunalDestinoHint, avisos);
        NationalProcessRoutingResponse routing = safeTargetRouting(processo, target, avisos);

        String distributionUnit = firstNonBlank(
                normalizeNullable(unidadeDistribuicao),
                routing == null ? null : routing.internalOrganLabel(),
                routing == null ? null : routing.metadataString("fracionary.internalOrgan.secretariatDesk"),
                routing == null ? null : routing.metadataString("fracionary.catalog.secretariatDesk"),
                target.distributionUnitHint()
        );

        AutuationPayload payload = new AutuationPayload(
                numeroDestino,
                target.instanceLevel(),
                target.court(),
                distributionUnit
        );
        String sourceProceedingNumber = numeroOrigem;
        String externalId = UUID.nameUUIDFromBytes(("AUTUATED_IN_TARGET:" + processoId + ':' + numeroDestino + ':' + target.instanceLevel().name() + ':' + target.court()).getBytes(StandardCharsets.UTF_8)).toString();
        CanonicalFact fact = new CanonicalFact(
                null,
                RecursalFactType.AUTUATED_IN_TARGET,
                LegalIntegrationSystem.MANUAL,
                externalId,
                sourceProceedingNumber,
                payload,
                Instant.now()
        );
        RecursalFactIngestRequest request = new RecursalFactIngestRequest(
                RecursalFactType.AUTUATED_IN_TARGET,
                LegalIntegrationSystem.MANUAL,
                externalId,
                sourceProceedingNumber,
                payload,
                fact.observedAt()
        );
        RecursalFactIngestResponse ingestResponse = recursalFactIdempotentIngestService.ingest(processoId, fact, request, sourceProceedingNumber);

        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("status", "REGISTRO_DESTINO_RECURSAL_CONFIRMADO");
        response.put("processoId", processoId);
        response.put("numeroProcessoOrigem", numeroOrigem);
        response.put("numeroDestino", numeroDestino);
        response.put("numeroAutuacaoDestino", numeroDestino);
        response.put("modoNumeracaoDestino", Objects.equals(numeroDestino, numeroOrigem) ? "MESMA_NUMERACAO_CNJ" : "AUTUACAO_AUTONOMA_DESTINO");
        response.put("sourceTimelineMode", Objects.equals(numeroDestino, numeroOrigem) ? "SOMENTE_REMESSA_E_RETORNO_NO_GRAU_REMETENTE" : "REFERENCIA_A_AUTUACAO_AUTONOMA_NO_GRAU_REMETENTE");
        response.put("targetTimelineMode", Objects.equals(numeroDestino, numeroOrigem) ? "TRAMITACAO_ATIVA_NO_GRAU_DESTINO" : "TRAMITACAO_ATIVA_NA_AUTUACAO_AUTONOMA_DESTINO");
        response.put("instanciaDestino", target.instanceLevel().name());
        response.put("tribunalDestino", target.court());
        putIfNotNull(response, "unidadeDistribuicao", distributionUnit);
        putIfNotNull(response, "observacoes", normalizeNullable(observacoes));
        response.put("autuacaoDestino", buildAutuacaoProjection(ingestResponse, target, distributionUnit, numeroOrigem, numeroDestino));
        Map<String, Object> cadernoDecisorioOrigem = ProcessoRecursalDecisionCarryOverAssembler.asMap(
                ProcessoRecursalDecisionCarryOverAssembler.assemble(
                        processo,
                        Objects.equals(numeroDestino, numeroOrigem) ? "RECURSO_GRAU_SUPERIOR" : "AUTUACAO_AUTONOMA_VINCULADA",
                        Objects.equals(numeroDestino, numeroOrigem) ? "SOMENTE_REMESSA_E_RETORNO_NO_GRAU_REMETENTE" : "REFERENCIA_A_AUTUACAO_AUTONOMA_NO_GRAU_REMETENTE",
                        Objects.equals(numeroDestino, numeroOrigem) ? "TRAMITACAO_ATIVA_NO_GRAU_DESTINO" : "TRAMITACAO_ATIVA_NA_AUTUACAO_AUTONOMA_DESTINO",
                        documentoProcessualRepository,
                        documentoPaginaRepository
                )
        );
        if (!cadernoDecisorioOrigem.isEmpty()) {
            response.put("cadernoDecisorioOrigem", cadernoDecisorioOrigem);
        }
        response.put("prevencaoRecursal", buildPreventionProjection(target, graphBefore, routing));
        response.put("unidadeFracionariaDestino", buildFracionaryProjection(target, routing));
        response.put("inteligenciaRecursal", buildGraphProjection(ingestResponse.graph(), processoId));
        response.put("avisos", List.copyOf(avisos));
        return response;
    }

    private LinkedHashMap<String, Object> buildAutuacaoProjection(RecursalFactIngestResponse ingestResponse,
                                                                  TargetResolution target,
                                                                  String distributionUnit,
                                                                  String numeroOrigem,
                                                                  String numeroDestino) {
        LinkedHashMap<String, Object> projection = new LinkedHashMap<>();
        projection.put("factType", RecursalFactType.AUTUATED_IN_TARGET.name());
        projection.put("factId", ingestResponse.factId());
        projection.put("dedupKey", ingestResponse.dedupKey());
        projection.put("systemTag", ingestResponse.systemTag());
        projection.put("timelineMovementId", ingestResponse.timelineMovementId());
        projection.put("instanciaDestino", target.instanceLevel().name());
        projection.put("tribunalDestino", target.court());
        projection.put("numeroProcessoOrigem", numeroOrigem);
        projection.put("numeroDestino", numeroDestino);
        projection.put("modoNumeracaoDestino", Objects.equals(numeroDestino, numeroOrigem) ? "MESMA_NUMERACAO_CNJ" : "AUTUACAO_AUTONOMA_DESTINO");
        projection.put("sourceTimelineMode", Objects.equals(numeroDestino, numeroOrigem) ? "SOMENTE_REMESSA_E_RETORNO_NO_GRAU_REMETENTE" : "REFERENCIA_A_AUTUACAO_AUTONOMA_NO_GRAU_REMETENTE");
        projection.put("targetTimelineMode", Objects.equals(numeroDestino, numeroOrigem) ? "TRAMITACAO_ATIVA_NO_GRAU_DESTINO" : "TRAMITACAO_ATIVA_NA_AUTUACAO_AUTONOMA_DESTINO");
        putIfNotNull(projection, "unidadeDistribuicao", distributionUnit);
        putIfNotNull(projection, "shadowProceedingVinculado", target.shadowProceedingKey());
        if (ingestResponse.plan() != null) {
            projection.put("proceedingsPlanejados", ingestResponse.plan().proceedings().size());
            projection.put("edgesPlanejadas", ingestResponse.plan().edges().size());
            projection.put("syncsPlanejados", ingestResponse.plan().sync().size());
            projection.put("workItemsPlanejados", ingestResponse.plan().workItems().size());
            projection.put("notas", ingestResponse.plan().notes());
        }
        return projection;
    }

    private LinkedHashMap<String, Object> buildPreventionProjection(TargetResolution target,
                                                                    RecursalGraphResponse graphBefore,
                                                                    NationalProcessRoutingResponse routing) {
        LinkedHashMap<String, Object> projection = new LinkedHashMap<>();
        projection.put("modo", firstNonBlank(
                routing == null ? null : routing.preventionMode(),
                routing == null ? null : routing.metadataString("relational.linkageMode"),
                target.shadowProceedingKey() == null ? "SEM_SOMBRA_PREVIA" : "PREVENCAO_POR_SOMBRA_RECURSAL"
        ));
        putIfNotNull(projection, "shadowProceedingReferencia", target.shadowProceedingKey());
        putIfNotNull(projection, "bindingFingerprint", routing == null ? null : routing.metadataString("relational.binding.preventionFingerprint"));
        putIfNotNull(projection, "internalPreventionClass", routing == null ? null : routing.metadataString("fracionary.internalOrgan.chamber.preventionClass"));
        putIfNotNull(projection, "bindingStrength", routing == null ? null : routing.metadataString("relational.binding.bindingStrength"));
        if (graphBefore != null && graphBefore.summary() != null) {
            projection.put("graphNodesAntesAutuacao", graphBefore.summary().totalNodes());
            projection.put("maxInstanceAntesAutuacao", graphBefore.summary().maxInstance() == null ? null : graphBefore.summary().maxInstance().name());
        }
        projection.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return projection;
    }

    private LinkedHashMap<String, Object> buildFracionaryProjection(TargetResolution target,
                                                                    NationalProcessRoutingResponse routing) {
        LinkedHashMap<String, Object> projection = new LinkedHashMap<>();
        projection.put("instanciaDestino", target.instanceLevel().name());
        projection.put("tribunalDestino", target.court());
        if (routing != null) {
            putIfNotNull(projection, "tribunalCodigo", routing.tribunalCodigo());
            putIfNotNull(projection, "tribunalNome", routing.tribunalNome());
            putIfNotNull(projection, "orgaoJulgadorSugerido", routing.orgaoJulgadorSugerido());
            putIfNotNull(projection, "fracionarySnapshotLabel", routing.fracionarySnapshotLabel());
            putIfNotNull(projection, "internalOrganLabel", routing.internalOrganLabel());
            putIfNotNull(projection, "mesaTriagem", routing.mesaTriagem());
            putIfNotNull(projection, "suggestedDeskProfile", routing.suggestedDeskProfile());
            putIfNotNull(projection, "internalOrganDesk", routing.metadataString("fracionary.internalOrgan.secretariatDesk"));
            putIfNotNull(projection, "internalGabineteDesk", routing.metadataString("fracionary.internalOrgan.gabineteDesk"));
            putIfNotNull(projection, "internalRelatoriaDesk", routing.metadataString("fracionary.internalOrgan.chamber.relatoriaDesk"));
            putIfNotNull(projection, "internalSessionRoom", routing.metadataString("fracionary.internalOrgan.chamber.sessionRoom"));
            putIfNotNull(projection, "internalPublicationDesk", routing.metadataString("fracionary.internalOrgan.specificOrganProfile.publicationDesk"));
            putIfNotNull(projection, "internalReviewDesk", routing.metadataString("fracionary.internalOrgan.sessionTopology.internalReviewDesk"));
            putIfNotNull(projection, "internalPanelComposition", routing.metadataString("fracionary.internalOrgan.panelComposition.panelCompositionLabel"));
            putIfNotNull(projection, "internalDeliberationMode", routing.metadataString("fracionary.internalOrgan.deliberationCycle.deliberationMode"));
            putIfNotNull(projection, "uniformizationHub", routing.metadataString("fracionary.bridge.uniformizationHub"));
        }
        projection.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return projection;
    }

    private LinkedHashMap<String, Object> buildGraphProjection(RecursalGraphResponse graphSnapshot, Long processoId) {
        LinkedHashMap<String, Object> graph = new LinkedHashMap<>();
        graph.put("graphEndpoint", "/api/v1/intelligence/recursal/processo/" + processoId + "/graph");
        graph.put("factsEndpoint", "/api/v1/intelligence/recursal/processo/" + processoId + "/facts");
        graph.put("autuacaoDestinoEndpoint", "/api/v1/intelligence/recursal/processo/" + processoId + "/autuacao-destino");
        if (graphSnapshot != null) {
            graph.put("graphSummary", graphSnapshot.summary());
            graph.put("graphNodes", graphSnapshot.nodes().size());
            graph.put("graphEdges", graphSnapshot.edges().size());
        }
        return graph;
    }

    private TargetResolution resolveTarget(Processo processo,
                                           RecursalGraphResponse graph,
                                           InstanceLevel instanceHint,
                                           String courtHint,
                                           List<String> avisos) {
        String normalizedCourtHint = normalizeNullable(courtHint);
        if (instanceHint != null && normalizedCourtHint != null) {
            return new TargetResolution(instanceHint, normalizedCourtHint, null, null);
        }

        RecursalGraphResponse.NodeDto candidate = graph == null ? null : graph.nodes().stream()
                .filter(Objects::nonNull)
                .filter(node -> node.instanceLevel() != null)
                .filter(node -> normalizedCourtHint == null || equalsIgnoreCase(node.court(), normalizedCourtHint))
                .filter(node -> instanceHint == null || node.instanceLevel() == instanceHint)
                .filter(node -> !equalsIgnoreCase(node.proceedingKey(), graph.anchorProceedingKey()))
                .sorted(Comparator
                        .comparing(RecursalGraphResponse.NodeDto::shadow).reversed()
                        .thenComparing((RecursalGraphResponse.NodeDto node) -> statusRank(node.status())).reversed()
                        .thenComparing(node -> instanceRank(node.instanceLevel())).reversed())
                .findFirst()
                .orElse(null);

        if (candidate != null) {
            String inferredDistributionUnit = candidate.displayLabel() == null || candidate.displayLabel().isBlank() ? null : candidate.displayLabel();
            return new TargetResolution(
                    instanceHint != null ? instanceHint : candidate.instanceLevel(),
                    normalizedCourtHint != null ? normalizedCourtHint : defaultCourt(processo, candidate.court(), instanceHint != null ? instanceHint : candidate.instanceLevel()),
                    candidate.shadow() ? candidate.proceedingKey() : null,
                    inferredDistributionUnit
            );
        }

        InstanceLevel fallbackInstance = instanceHint != null ? instanceHint : inferInstanceByCourt(normalizedCourtHint);
        String fallbackCourt = defaultCourt(processo, normalizedCourtHint, fallbackInstance);
        avisos.add("Autuação recursal consolidada sem sombra específica encontrada no grafo; aplicada inferência por instância/corte de destino.");
        return new TargetResolution(fallbackInstance, fallbackCourt, null, null);
    }

    private NationalProcessRoutingResponse safeTargetRouting(Processo processo,
                                                             TargetResolution target,
                                                             List<String> avisos) {
        try {
            NationalProcessRoutingRequest request = new NationalProcessRoutingRequest(
                    processo.getRito() == null ? RitoProcessual.COMUM_ORDINARIO : processo.getRito(),
                    processo.getRamoDireito() == null ? inferRamo(processo.getRito()) : processo.getRamoDireito(),
                    mapGrau(target.instanceLevel()),
                    resolveUf(processo),
                    firstNonBlank(processo.getComarca(), jurisdicaoComarca(processo)),
                    processo.getValorCausa(),
                    processo.getClasseProcessual(),
                    firstNonBlank(processo.getAssunto(), processo.getObjetoProcessual(), processo.getPedidoPrincipal()),
                    Instant.now(),
                    safeNumeroProcesso(processo),
                    resolveCidade(processo),
                    resolveForo(processo),
                    resolveSecao(processo),
                    resolveSubsecao(processo),
                    resolveCircunscricao(processo),
                    null,
                    null,
                    resolveCidade(processo),
                    resolveCidade(processo),
                    safeNumeroProcesso(processo),
                    safeNumeroProcesso(processo),
                    target.court(),
                    false,
                    false,
                    false,
                    false,
                    false,
                    processo.isSigiloso(),
                    false
            );
            return processualOperationalSurfaceFacadeService.diagnosticarRouting(request);
        } catch (RuntimeException ex) {
            avisos.add("Diagnóstico da unidade fracionária de destino não pôde ser consolidado: " + safeMessage(ex));
            return null;
        }
    }

    private RecursalGraphResponse safeGraphSnapshot(Long processoId, List<String> avisos) {
        try {
            return recursalIntelligenceFacadeService.graph(processoId);
        } catch (RuntimeException ex) {
            avisos.add("Grafo recursal indisponível para inferência de destino: " + safeMessage(ex));
            return null;
        }
    }

    private static int statusRank(String status) {
        if (status == null || status.isBlank()) {
            return 0;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "PREDICTED" -> 40;
            case "ACTIVE" -> 30;
            case "RECONCILED" -> 20;
            default -> 10;
        };
    }

    private static int instanceRank(InstanceLevel instance) {
        if (instance == null) {
            return 0;
        }
        return switch (instance) {
            case FIRST_INSTANCE -> 1;
            case SECOND_INSTANCE -> 2;
            case SUPERIOR -> 3;
            case EXTRAORDINARY -> 4;
        };
    }

    private static GrauJurisdicao mapGrau(InstanceLevel instance) {
        if (instance == null) {
            return GrauJurisdicao.SEGUNDO_GRAU;
        }
        return switch (instance) {
            case FIRST_INSTANCE, SECOND_INSTANCE -> GrauJurisdicao.SEGUNDO_GRAU;
            case SUPERIOR -> GrauJurisdicao.SUPERIOR;
            case EXTRAORDINARY -> GrauJurisdicao.CONSTITUCIONAL;
        };
    }

    private static RamoDireito inferRamo(RitoProcessual rito) {
        if (rito == null) {
            return RamoDireito.CIVIL;
        }
        if (rito.isTrabalhista()) {
            return RamoDireito.TRABALHISTA;
        }
        if (rito.isEleitoral()) {
            return RamoDireito.ELEITORAL;
        }
        if (rito.isMilitar()) {
            return RamoDireito.MILITAR;
        }
        if (rito.isEspecialConstitucional()) {
            return RamoDireito.CONSTITUCIONAL;
        }
        return RamoDireito.CIVIL;
    }

    private static InstanceLevel inferInstanceByCourt(String court) {
        String normalized = normalizeNullable(court);
        if (normalized == null) {
            return InstanceLevel.SECOND_INSTANCE;
        }
        if (normalized.startsWith("STF")) {
            return InstanceLevel.EXTRAORDINARY;
        }
        if (normalized.startsWith("STJ") || normalized.startsWith("TST") || normalized.startsWith("TSE") || normalized.startsWith("STM")) {
            return InstanceLevel.SUPERIOR;
        }
        return InstanceLevel.SECOND_INSTANCE;
    }

    private static String defaultCourt(Processo processo, String candidate, InstanceLevel instance) {
        String normalizedCandidate = normalizeNullable(candidate);
        if (normalizedCandidate != null) {
            return normalizedCandidate;
        }
        if (instance == InstanceLevel.SUPERIOR) {
            return superiorCourtFor(processo);
        }
        if (instance == InstanceLevel.EXTRAORDINARY) {
            return "STF";
        }
        return firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), tribunalFromUf(resolveUf(processo)));
    }

    private static String superiorCourtFor(Processo processo) {
        RitoProcessual rito = processo.getRito();
        RamoDireito ramo = processo.getRamoDireito();
        if ((rito != null && rito.isTrabalhista()) || ramo == RamoDireito.TRABALHISTA) {
            return "TST";
        }
        if ((rito != null && rito.isEleitoral()) || ramo == RamoDireito.ELEITORAL) {
            return "TSE";
        }
        if ((rito != null && rito.isMilitar()) || ramo == RamoDireito.MILITAR) {
            return "STM";
        }
        return "STJ";
    }

    private static String tribunalFromUf(String uf) {
        String normalizedUf = normalizeNullable(uf);
        return normalizedUf == null ? "TJSP" : "TJ" + normalizedUf.toUpperCase(Locale.ROOT);
    }

    private static String safeNumeroProcesso(Processo processo) {
        if (processo == null) {
            return "PROCESSO_SEM_NUMERO";
        }
        String numeroUnificado = normalizeNullable(processo.getNumeroUnificado());
        if (numeroUnificado != null) {
            return numeroUnificado;
        }
        String numeroProcesso = normalizeNullable(processo.getNumeroProcesso());
        if (numeroProcesso != null) {
            return numeroProcesso;
        }
        String numero = normalizeNullable(processo.getNumero());
        if (numero != null) {
            return numero;
        }
        return processo.getId() == null ? "PROCESSO_SEM_NUMERO" : "PROCESSO-" + processo.getId();
    }

    private static String resolveUf(Processo processo) {
        String uf = normalizeNullable(processo.getUf());
        if (uf != null) {
            return uf;
        }
        Jurisdicao jurisdicao = processo.getJurisdicao();
        return jurisdicao == null ? null : normalizeNullable(jurisdicao.getUf());
    }

    private static String resolveCidade(Processo processo) {
        Jurisdicao jurisdicao = processo.getJurisdicao();
        return firstNonBlank(processo.getComarca(), jurisdicao == null ? null : jurisdicao.getCidade());
    }

    private static String resolveForo(Processo processo) {
        Jurisdicao jurisdicao = processo.getJurisdicao();
        return firstNonBlank(processo.getVara(), jurisdicao == null ? null : jurisdicao.getForo());
    }

    private static String resolveSecao(Processo processo) {
        Jurisdicao jurisdicao = processo.getJurisdicao();
        return jurisdicao == null ? null : normalizeNullable(jurisdicao.getSecaoJudiciaria());
    }

    private static String resolveSubsecao(Processo processo) {
        Jurisdicao jurisdicao = processo.getJurisdicao();
        return jurisdicao == null ? null : normalizeNullable(jurisdicao.getSubsecaoJudiciaria());
    }

    private static String resolveCircunscricao(Processo processo) {
        Jurisdicao jurisdicao = processo.getJurisdicao();
        return jurisdicao == null ? null : normalizeNullable(jurisdicao.getCircunscricao());
    }

    private static String jurisdicaoComarca(Processo processo) {
        Jurisdicao jurisdicao = processo.getJurisdicao();
        return jurisdicao == null ? null : normalizeNullable(jurisdicao.getComarca());
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "falha recursal não especificada";
        }
        String message = normalizeNullable(throwable.getMessage());
        return message == null ? throwable.getClass().getSimpleName() : message;
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (target != null && key != null && value != null) {
            target.put(key, value);
        }
    }

    private String resolveNumeroDestino(String numeroAutuacaoDestino, String numeroOrigem, List<String> avisos) {
        String numeroInformado = normalizeNullable(numeroAutuacaoDestino);
        String numeroBase = normalizeNullable(numeroOrigem);
        if (numeroInformado == null || numeroInformado.isBlank()) {
            if (numeroBase != null) {
                avisos.add("Destino recursal consolidado com a mesma numeração CNJ do processo de origem.");
                return numeroBase;
            }
            throw new IllegalArgumentException("Não foi possível resolver a numeração recursal de destino.");
        }
        if (numeroBase != null && !numeroBase.equalsIgnoreCase(numeroInformado)) {
            avisos.add("Numeração de destino distinta da origem registrada. Confirme que o procedimento exige autuação autônoma.");
        }
        return numeroInformado;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeNullable(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        String left = normalizeNullable(a);
        String right = normalizeNullable(b);
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record TargetResolution(InstanceLevel instanceLevel,
                                    String court,
                                    String shadowProceedingKey,
                                    String distributionUnitHint) {
    }
}
