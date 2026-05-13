package com.tcc.pjb.backend.service.secretariat.topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskKey;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioResolver;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskResolver;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.secretariat.topology.MunicipalCoverageResolutionEngine.MunicipalCoverageProfile;

@Service
public class NationalJudicialTopologyService {

    private final ForumDeskResolver forumDeskResolver;
    private final ForumDeskPortfolioResolver forumDeskPortfolioResolver;
    private final MunicipalCoverageResolutionEngine coverageResolutionEngine;

    public NationalJudicialTopologyService(ForumDeskResolver forumDeskResolver,
                                           ForumDeskPortfolioResolver forumDeskPortfolioResolver,
                                           MunicipalCoverageResolutionEngine coverageResolutionEngine) {
        this.forumDeskResolver = Objects.requireNonNull(forumDeskResolver);
        this.forumDeskPortfolioResolver = Objects.requireNonNull(forumDeskPortfolioResolver);
        this.coverageResolutionEngine = Objects.requireNonNull(coverageResolutionEngine);
    }

    public NationalJudicialTopologyProfile resolveForProcess(Processo processo, TipoJustica tipoJustica) {
        Objects.requireNonNull(processo, "processo");
        ForumDeskKey deskKey = forumDeskResolver.resolveForProcess(processo);
        ForumDeskPortfolioProfile portfolio = forumDeskPortfolioResolver.resolve(deskKey);
        MunicipalCoverageProfile coverage = coverageResolutionEngine.resolve(processo, tipoJustica, deskKey);
        Jurisdicao jurisdicao = processo.getJurisdicao();
        String laneAxis = deskKey.lane().name();
        String instanceAxis = switch (deskKey.instance()) {
            case FIRST -> "PRIMEIRO_GRAU";
            case SECOND -> "SEGUNDO_GRAU";
            case SUPERIOR -> "TRIBUNAL_SUPERIOR";
        };
        String forumAxis = resolveForumAxis(processo, tipoJustica, laneAxis);
        String unitDescriptor = firstNonBlank(
                processo.getVara(),
                processo.getUnidadeJudiciariaCodigo(),
                jurisdicao != null ? jurisdicao.getNome() : null,
                deskKey.unitHint(),
                "UNIDADE_BASE"
        );
        String secretariatUnitCode = buildSecretariatUnitCode(deskKey, coverage, unitDescriptor, laneAxis, instanceAxis);
        String topologyKey = normalize(deskKey.organ().code()) + ':' + normalize(instanceAxis) + ':' + normalize(laneAxis) + ':' + normalize(coverage.coverageKey());
        String organizationalPath = String.join(">",
                List.of(
                        normalize(tipoJustica == null ? "ESTADUAL" : tipoJustica.name()),
                        normalize(deskKey.organ().code()),
                        normalize(instanceAxis),
                        normalize(forumAxis),
                        normalize(laneAxis),
                        normalize(coverage.seatMunicipality()),
                        normalize(secretariatUnitCode)
                )
        );
        List<String> isolationBarriers = new ArrayList<>();
        isolationBarriers.add("O inbox base da secretaria segue o padrão institucional SEC para integrar com a malha existente do fórum.");
        isolationBarriers.add("A lane processual resolve a secretaria especializada e impede mistura entre cível, penal, juizado, previdenciário, família e demais trilhas.");
        isolationBarriers.add("A cobertura municipal separa município de origem da sede competente, evitando que ausência de fórum local contamine a unidade errada.");
        if (deskKey.isSecondInstance()) {
            isolationBarriers.add("Fluxo colegiado isolado da secretaria de primeiro grau.");
        }
        if (deskKey.lane().isJuizado()) {
            isolationBarriers.add("Regime de juizado isolado do procedimento comum.");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("deskDescriptor", deskKey.descriptor());
        metadata.put("inboxKey", deskKey.inboxKey());
        metadata.put("organCode", deskKey.organ().code());
        metadata.put("organKind", deskKey.organ().kind().name());
        metadata.put("organDisplayName", deskKey.organ().displayName());
        metadata.put("laneToken", deskKey.lane().token());
        metadata.put("dashboardBucket", portfolio.dashboardBucket());
        metadata.put("triageDesk", portfolio.triageDesk());
        metadata.put("assistantDesk", portfolio.assistantDesk());
        metadata.put("hearingDesk", portfolio.hearingDesk());
        metadata.put("complianceDesk", portfolio.complianceDesk());
        metadata.put("coordinationDesk", portfolio.coordinationDesk());
        metadata.put("redistributionDesk", portfolio.redistributionDesk());
        metadata.put("gabineteDesk", portfolio.gabineteDesk());
        metadata.put("forumAxis", forumAxis);
        metadata.put("unitDescriptor", unitDescriptor);
        metadata.put("coverage", coverage.toMap());
        metadata.put("labels", portfolio.labels());
        metadata.put("portfolioMetadata", portfolio.metadata());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new NationalJudicialTopologyProfile(
                topologyKey,
                deskKey.organ().code(),
                deskKey.organ().displayName(),
                deskKey.organ().kind().name(),
                instanceAxis,
                laneAxis,
                forumAxis,
                unitDescriptor,
                secretariatUnitCode,
                deskKey.inboxKey(),
                portfolio.triageDesk(),
                portfolio.assistantDesk(),
                portfolio.hearingDesk(),
                portfolio.complianceDesk(),
                portfolio.coordinationDesk(),
                portfolio.redistributionDesk(),
                portfolio.gabineteDesk(),
                coverage,
                organizationalPath,
                List.copyOf(isolationBarriers),
                Collections.unmodifiableMap(metadata)
        );
    }

    private String resolveForumAxis(Processo processo, TipoJustica tipoJustica, String laneAxis) {
        String tribunal = firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), processo.getConnectorSystem(), "NACIONAL");
        RamoDireito ramo = processo.getRamoDireito();
        if (laneAxis.startsWith("JE")) {
            return tipoJustica == TipoJustica.FEDERAL ? "JUIZADO_FEDERAL" : "JUIZADO_ESTADUAL";
        }
        if (tipoJustica == TipoJustica.FEDERAL) {
            return "FORO_FEDERAL";
        }
        if (tipoJustica == TipoJustica.TRABALHO) {
            return "FORO_TRABALHISTA";
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return "FORO_ELEITORAL";
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
            return "FORO_MILITAR";
        }
        if (tribunal != null && tribunal.toUpperCase(Locale.ROOT).startsWith("TJ")) {
            return ramo != null && ramo.isPenalLike() ? "FORO_COMUM_CRIMINAL" : "FORO_COMUM_CIVEL";
        }
        return "FORO_COMUM";
    }

    private String buildSecretariatUnitCode(ForumDeskKey deskKey,
                                            MunicipalCoverageProfile coverage,
                                            String unitDescriptor,
                                            String laneAxis,
                                            String instanceAxis) {
        List<String> pieces = new ArrayList<>();
        pieces.add("SECRETARIA");
        pieces.add(normalize(deskKey.organ().code()));
        pieces.add(normalize(instanceAxis));
        pieces.add(normalize(laneAxis));
        pieces.add(normalize(coverage.seatMunicipality()));
        pieces.add(normalize(unitDescriptor));
        return String.join("_", pieces);
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

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "BASE";
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    public record NationalJudicialTopologyProfile(
            String topologyKey,
            String judicialOrganCode,
            String judicialOrganName,
            String judicialOrganKind,
            String instanceAxis,
            String laneAxis,
            String forumAxis,
            String unitDescriptor,
            String secretariatUnitCode,
            String baseInboxKey,
            String triageDesk,
            String assistantDesk,
            String hearingDesk,
            String complianceDesk,
            String coordinationDesk,
            String redistributionDesk,
            String gabineteDesk,
            MunicipalCoverageProfile coverage,
            String organizationalPath,
            List<String> isolationBarriers,
            Map<String, Object> metadata
    ) {

        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("topologyKey", topologyKey);
            out.put("judicialOrganCode", judicialOrganCode);
            out.put("judicialOrganName", judicialOrganName);
            out.put("judicialOrganKind", judicialOrganKind);
            out.put("instanceAxis", instanceAxis);
            out.put("laneAxis", laneAxis);
            out.put("forumAxis", forumAxis);
            out.put("unitDescriptor", unitDescriptor);
            out.put("secretariatUnitCode", secretariatUnitCode);
            out.put("baseInboxKey", baseInboxKey);
            out.put("triageDesk", triageDesk);
            out.put("assistantDesk", assistantDesk);
            out.put("hearingDesk", hearingDesk);
            out.put("complianceDesk", complianceDesk);
            out.put("coordinationDesk", coordinationDesk);
            out.put("redistributionDesk", redistributionDesk);
            out.put("gabineteDesk", gabineteDesk);
            out.put("coverage", coverage == null ? null : coverage.toMap());
            out.put("organizationalPath", organizationalPath);
            out.put("isolationBarriers", isolationBarriers);
            out.put("metadata", metadata);
            out.entrySet().removeIf(entry -> entry.getValue() == null);
            return out;
        }
    }
}
