package com.tcc.pjb.backend.core.processual.routing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix.RamoJusticaNacional;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;
import jakarta.inject.Inject;

@Service
public class NationalProcessRoutingService {

    private final TribunalRuleEngine tribunalRuleEngine;
    private final TerritorialRoutingResolver territorialRoutingResolver;
    private final RelationalRoutingResolver relationalRoutingResolver;
    private final FracionaryOrganRoutingResolver fracionaryOrganRoutingResolver;
    private final ProceduralCoverageResolver proceduralCoverageResolver;
    private final NationalProcessRoutingSupport support;
    private final NationalProcessRoutingDecisionPolicy decisionPolicy;
    private final NationalProcessRoutingNarrativeFactory narrativeFactory;
    private final NationalProcessRoutingMetadataFactory metadataFactory;

    @Inject
    public NationalProcessRoutingService(TribunalRuleEngine tribunalRuleEngine,
                                         TerritorialRoutingResolver territorialRoutingResolver,
                                         RelationalRoutingResolver relationalRoutingResolver,
                                         FracionaryOrganRoutingResolver fracionaryOrganRoutingResolver,
                                         ProceduralCoverageResolver proceduralCoverageResolver) {
        this(
                tribunalRuleEngine,
                territorialRoutingResolver,
                relationalRoutingResolver,
                fracionaryOrganRoutingResolver,
                proceduralCoverageResolver,
                new NationalProcessRoutingSupport(),
                null,
                null,
                null
        );
    }

    NationalProcessRoutingService(TribunalRuleEngine tribunalRuleEngine,
                                  TerritorialRoutingResolver territorialRoutingResolver,
                                  RelationalRoutingResolver relationalRoutingResolver,
                                  FracionaryOrganRoutingResolver fracionaryOrganRoutingResolver,
                                  ProceduralCoverageResolver proceduralCoverageResolver,
                                  NationalProcessRoutingSupport support,
                                  NationalProcessRoutingDecisionPolicy decisionPolicy,
                                  NationalProcessRoutingNarrativeFactory narrativeFactory,
                                  NationalProcessRoutingMetadataFactory metadataFactory) {
        this.tribunalRuleEngine = Objects.requireNonNull(tribunalRuleEngine);
        this.territorialRoutingResolver = Objects.requireNonNull(territorialRoutingResolver);
        this.relationalRoutingResolver = Objects.requireNonNull(relationalRoutingResolver);
        this.fracionaryOrganRoutingResolver = Objects.requireNonNull(fracionaryOrganRoutingResolver);
        this.proceduralCoverageResolver = Objects.requireNonNull(proceduralCoverageResolver);
        this.support = Objects.requireNonNull(support);
        this.decisionPolicy = Objects.requireNonNullElseGet(decisionPolicy, () -> new NationalProcessRoutingDecisionPolicy(this.support));
        this.narrativeFactory = Objects.requireNonNullElseGet(narrativeFactory, () -> new NationalProcessRoutingNarrativeFactory(this.support, this.decisionPolicy));
        this.metadataFactory = Objects.requireNonNullElseGet(metadataFactory, () -> new NationalProcessRoutingMetadataFactory(this.support));
    }

    public RoutingDecision route(RoutingCommand command) {
        Objects.requireNonNull(command);
        if (command.rito() == null) {
            throw new IllegalArgumentException(NationalProcessRoutingMessages.ritoObrigatorio());
        }
        if (command.grau() == null) {
            throw new IllegalArgumentException(NationalProcessRoutingMessages.grauObrigatorio());
        }

        String uf = support.normalizeUf(command.uf());
        RamoDireito ramo = command.ramo() == null ? command.rito().suggestedRamo() : command.ramo();
        TipoJustica tipoJustica = support.resolveTipoJustica(command, ramo);
        RamoJusticaNacional ramoJustica = support.resolveRamoJustica(tipoJustica, command.grau(), command.rito());
        NationalCompetenceMatrix competencia = support.resolveCompetence(uf, ramoJustica, command.grau(), command.rito());
        TerritorialRoutingProfile territorial = Objects.requireNonNullElseGet(
                territorialRoutingResolver.resolve(command, tipoJustica, competencia),
                () -> emptyTerritorial(command, uf)
        );

        TribunalRuleEngine.ContextoResolucao contexto = TribunalRuleEngine.ContextoResolucao.agora(
                support.firstNonBlank(command.tribunalCodigoHint(), competencia.codigo()),
                support.firstNonBlank(territorial.comarca(), territorial.cidade(), command.comarca()),
                decisionPolicy.resolveVaraToken(command.rito(), command.grau(), territorial, competencia),
                ramo,
                command.grau()
        );

        int prazoTriagemHoras = tribunalRuleEngine.resolverPrazoDias(
                TribunalRuleEngine.ChaveRegra.TRIAGEM_PRAZO_ANALISE_H,
                contexto,
                decisionPolicy.resolveTriagemPadrao(command.grau(), command.rito(), tipoJustica)
        );
        boolean conciliacaoObrigatoria = tribunalRuleEngine.resolverBooleano(
                TribunalRuleEngine.ChaveRegra.AUDIENCIA_CONCIL_OBRIG,
                contexto,
                decisionPolicy.shouldDefaultConciliation(command.rito())
        );
        BigDecimal limiteJuizado = tribunalRuleEngine.resolverLimiteJuizadoEmReais(contexto).setScale(2, RoundingMode.HALF_UP);

        String instancia = decisionPolicy.resolveInstancia(command.grau(), tipoJustica, competencia);
        String specializationAxis = decisionPolicy.resolveSpecializationAxis(command.rito(), ramo, tipoJustica);
        RelationalRoutingProfile relational = Objects.requireNonNullElseGet(
                relationalRoutingResolver.resolve(command, tipoJustica, territorial),
                NationalProcessRoutingService::emptyRelational
        );
        String distributionMode = relational.effectiveDistributionMode(decisionPolicy.resolveDistributionMode(command, territorial, tipoJustica));
        String linkageMode = relational.effectiveLinkageMode(decisionPolicy.resolveLinkageMode(command));
        String preventionMode = relational.effectivePreventionMode(territorial.preventionMode());
        FracionaryOrganRoutingProfile fracionary = Objects.requireNonNullElseGet(
                fracionaryOrganRoutingResolver.resolve(command, tipoJustica, competencia, territorial, specializationAxis),
                NationalProcessRoutingService::emptyFracionary
        );
        ProceduralCoverageProfile coverage = Objects.requireNonNullElseGet(
                proceduralCoverageResolver.resolve(command, tipoJustica, competencia, territorial, relational, fracionary, specializationAxis),
                NationalProcessRoutingService::emptyCoverage
        );
        String orgaoJulgador = fracionary.effectiveOrgaoJulgador(decisionPolicy.resolveOrgaoJulgador(command.rito(), command.grau(), tipoJustica, territorial));
        String unidade = decisionPolicy.resolveUnidadeJudiciaria(command.rito(), command.grau(), competencia, territorial);
        String fila = decisionPolicy.resolveFila(command.rito(), command.grau(), territorial);
        JudicialSystem primario = competencia.sistemaJudicialPrimario();
        JudicialSystem fallback = competencia.sistemaJudicialFallback();
        String mesaTriagem = fracionary.effectiveMesaTriagem(decisionPolicy.resolveMesaTriagem(command.grau(), tipoJustica, territorial, command));
        String allocationStrategy = fracionary.effectiveAllocationStrategy(decisionPolicy.resolveAllocationStrategy(command, tipoJustica, territorial, distributionMode, specializationAxis));
        String competenceEnvelope = decisionPolicy.buildCompetenceEnvelope(command.grau(), tipoJustica, competencia, territorial, specializationAxis);
        String routingRiskLevel = decisionPolicy.resolveRoutingRiskLevel(command, territorial, distributionMode, linkageMode);
        String suggestedDeskProfile = fracionary.effectiveDeskProfile(relational.effectiveDeskProfile(decisionPolicy.resolveSuggestedDeskProfile(command, tipoJustica, specializationAxis, territorial, command.grau())));

        LinkedHashSet<String> alertas = new LinkedHashSet<>(narrativeFactory.buildWarnings(command, ramo, tipoJustica, competencia, limiteJuizado, conciliacaoObrigatoria, territorial, distributionMode, linkageMode, routingRiskLevel));
        alertas.addAll(relational.warnings());
        alertas.addAll(fracionary.warnings());
        alertas.addAll(coverage.warnings());

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(narrativeFactory.buildFundamentos(command, contexto, competencia, prazoTriagemHoras, conciliacaoObrigatoria, limiteJuizado, territorial, orgaoJulgador, fila, specializationAxis, allocationStrategy, linkageMode, competenceEnvelope, routingRiskLevel));
        fundamentos.addAll(relational.fundamentos());
        fundamentos.addAll(fracionary.fundamentos());
        fundamentos.addAll(coverage.fundamentos());

        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>(narrativeFactory.buildReviewChecklist(command, tipoJustica, territorial, distributionMode, command.grau(), linkageMode, suggestedDeskProfile));
        reviewChecklist.addAll(relational.reviewChecklist());
        reviewChecklist.addAll(fracionary.reviewChecklist());
        reviewChecklist.addAll(coverage.reviewChecklist());

        LinkedHashMap<String, Object> metadata = metadataFactory.build(
                competencia,
                territorial,
                relational,
                fracionary,
                coverage,
                distributionMode,
                preventionMode,
                mesaTriagem,
                orgaoJulgador,
                fila,
                unidade,
                limiteJuizado,
                conciliacaoObrigatoria,
                specializationAxis,
                allocationStrategy,
                linkageMode,
                competenceEnvelope,
                routingRiskLevel,
                suggestedDeskProfile,
                List.copyOf(reviewChecklist)
        );

        return new RoutingDecision(
                command.rito(),
                ramo,
                command.grau(),
                tipoJustica,
                support.firstNonBlank(command.tribunalCodigoHint(), competencia.codigo()),
                competencia.nome(),
                competencia.ramoJusticaNacional().name(),
                primario.name(),
                fallback.name(),
                instancia,
                orgaoJulgador,
                unidade,
                fila,
                command.rito().requiresSegredoByDefault(),
                tipoJustica.admiteJuizado(),
                conciliacaoObrigatoria,
                prazoTriagemHoras,
                limiteJuizado,
                territorial.cidade(),
                territorial.comarca(),
                territorial.foro(),
                territorial.secaoJudiciaria(),
                territorial.subsecaoJudiciaria(),
                territorial.circunscricao(),
                territorial.mode(),
                preventionMode,
                distributionMode,
                specializationAxis,
                allocationStrategy,
                linkageMode,
                competenceEnvelope,
                routingRiskLevel,
                suggestedDeskProfile,
                mesaTriagem,
                List.copyOf(alertas),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private TerritorialRoutingProfile emptyTerritorial(RoutingCommand command, String uf) {
        return new TerritorialRoutingProfile(
                "PADRAO",
                uf,
                command.comarca(),
                command.comarca(),
                command.comarca(),
                null,
                null,
                null,
                null,
                "NENHUM_SINAL",
                true,
                List.of(),
                List.of(),
                new LinkedHashMap<>()
        );
    }

    private static RelationalRoutingProfile emptyRelational() {
        return new RelationalRoutingProfile(null, null, null, null, null, null, null, null, null, null, null, false, List.of(), List.of(), List.of(), new LinkedHashMap<>());
    }

    private static FracionaryOrganRoutingProfile emptyFracionary() {
        return new FracionaryOrganRoutingProfile(null, null, null, null, null, null, null, null, null, false, List.of(), List.of(), List.of(), new LinkedHashMap<>());
    }

    private static ProceduralCoverageProfile emptyCoverage() {
        return new ProceduralCoverageProfile(null, null, null, null, null, null, null, null, null, null, null, List.of(), List.of(), List.of(), new LinkedHashMap<>());
    }

    public record RoutingCommand(
            RitoProcessual rito,
            RamoDireito ramo,
            GrauJurisdicao grau,
            String uf,
            String comarca,
            BigDecimal valorCausa,
            String classeProcessual,
            String assunto,
            Instant referenceAt,
            String numeroProcesso,
            String cidade,
            String foro,
            String secaoJudiciaria,
            String subsecaoJudiciaria,
            String circunscricao,
            String cidadeAutor,
            String cidadeReu,
            String cidadeFato,
            String municipioFato,
            String preventionReference,
            String processoReferencia,
            String tribunalCodigoHint,
            boolean dependenciaDeclarada,
            boolean conexaoDeclarada,
            boolean continenciaDeclarada,
            boolean pedidoLiminar,
            boolean plantaoJudicial,
            boolean segredoSolicitado,
            boolean redistribuicaoImpedimento) {

        public RoutingCommand(RitoProcessual rito,
                              RamoDireito ramo,
                              GrauJurisdicao grau,
                              String uf,
                              String comarca,
                              BigDecimal valorCausa,
                              String classeProcessual,
                              String assunto,
                              Instant referenceAt) {
            this(rito, ramo, grau, uf, comarca, valorCausa, classeProcessual, assunto, referenceAt,
                    null, null, null, null, null, null, null, null, null, null, null, null, null,
                    false, false, false, false, false, false, false);
        }

        public RoutingCommand {
            uf = uf == null ? null : uf.trim().toUpperCase(Locale.ROOT);
            comarca = comarca == null ? null : comarca.trim();
            classeProcessual = classeProcessual == null ? null : classeProcessual.trim();
            assunto = assunto == null ? null : assunto.trim();
            numeroProcesso = numeroProcesso == null ? null : numeroProcesso.trim();
            cidade = cidade == null ? null : cidade.trim();
            foro = foro == null ? null : foro.trim();
            secaoJudiciaria = secaoJudiciaria == null ? null : secaoJudiciaria.trim();
            subsecaoJudiciaria = subsecaoJudiciaria == null ? null : subsecaoJudiciaria.trim();
            circunscricao = circunscricao == null ? null : circunscricao.trim();
            cidadeAutor = cidadeAutor == null ? null : cidadeAutor.trim();
            cidadeReu = cidadeReu == null ? null : cidadeReu.trim();
            cidadeFato = cidadeFato == null ? null : cidadeFato.trim();
            municipioFato = municipioFato == null ? null : municipioFato.trim();
            preventionReference = preventionReference == null ? null : preventionReference.trim();
            processoReferencia = processoReferencia == null ? null : processoReferencia.trim();
            tribunalCodigoHint = tribunalCodigoHint == null ? null : tribunalCodigoHint.trim().toUpperCase(Locale.ROOT);
            referenceAt = referenceAt == null ? Instant.now() : referenceAt;
            valorCausa = valorCausa == null ? null : valorCausa.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public record RoutingDecision(
            RitoProcessual rito,
            RamoDireito ramoDireito,
            GrauJurisdicao grau,
            TipoJustica tipoJustica,
            String tribunalCodigo,
            String tribunalNome,
            String ramoJusticaNacional,
            String sistemaPrimario,
            String sistemaFallback,
            String instancia,
            String orgaoJulgadorSugerido,
            String unidadeJudiciariaCodigo,
            String filaDistribuicao,
            boolean sigiloPadrao,
            boolean admiteJuizado,
            boolean conciliacaoObrigatoria,
            int prazoTriagemHoras,
            BigDecimal limiteJuizado,
            String cidadeSugerida,
            String comarcaSugerida,
            String foroSugerido,
            String secaoJudiciariaSugerida,
            String subsecaoJudiciariaSugerida,
            String circunscricaoJudiciariaSugerida,
            String territorialMode,
            String preventionMode,
            String distributionMode,
            String specializationAxis,
            String allocationStrategy,
            String linkageMode,
            String competenceEnvelope,
            String routingRiskLevel,
            String suggestedDeskProfile,
            String mesaTriagem,
            List<String> alertas,
            List<String> fundamentos,
            List<String> reviewChecklist,
            LinkedHashMap<String, Object> metadata) {
        public RoutingDecision {
            alertas = safeRoutingList(alertas);
            fundamentos = safeRoutingList(fundamentos);
            reviewChecklist = safeRoutingList(reviewChecklist);
            metadata = safeRoutingMetadata(metadata);
        }

        private static List<String> safeRoutingList(List<String> input) {
            if (input == null || input.isEmpty()) {
                return List.of();
            }
            return input.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .distinct()
                    .toList();
        }

        private static LinkedHashMap<String, Object> safeRoutingMetadata(LinkedHashMap<String, Object> input) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            if (input != null) {
                input.forEach((key, value) -> {
                    if (key != null && value != null) {
                        out.put(key, value);
                    }
                });
            }
            return out;
        }
    }
}
