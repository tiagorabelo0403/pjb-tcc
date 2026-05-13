package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;

@Component
public class JudicialTerritorialCatalog {

    private final TerritorialForumRegistry territorialForumRegistry;

    public JudicialTerritorialCatalog(TerritorialForumRegistry territorialForumRegistry) {
        this.territorialForumRegistry = territorialForumRegistry;
    }

    private static final Set<String> TJM_STATES = Set.of("MG", "RS", "SP");

    private static final Map<String, String> TRF_BY_UF = Map.ofEntries(
            Map.entry("AC", "TRF1"), Map.entry("AM", "TRF1"), Map.entry("AP", "TRF1"), Map.entry("BA", "TRF1"),
            Map.entry("DF", "TRF1"), Map.entry("GO", "TRF1"), Map.entry("MA", "TRF1"), Map.entry("MT", "TRF1"),
            Map.entry("PA", "TRF1"), Map.entry("PI", "TRF1"), Map.entry("RO", "TRF1"), Map.entry("RR", "TRF1"),
            Map.entry("TO", "TRF1"), Map.entry("ES", "TRF2"), Map.entry("RJ", "TRF2"), Map.entry("MS", "TRF3"),
            Map.entry("SP", "TRF3"), Map.entry("PR", "TRF4"), Map.entry("RS", "TRF4"), Map.entry("SC", "TRF4"),
            Map.entry("AL", "TRF5"), Map.entry("CE", "TRF5"), Map.entry("PB", "TRF5"), Map.entry("PE", "TRF5"),
            Map.entry("RN", "TRF5"), Map.entry("SE", "TRF5"), Map.entry("MG", "TRF6")
    );

    private static final Map<String, String> TRT_BY_UF = Map.ofEntries(
            Map.entry("RJ", "TRT1"), Map.entry("SP_CAPITAL", "TRT2"), Map.entry("MG", "TRT3"), Map.entry("RS", "TRT4"),
            Map.entry("BA", "TRT5"), Map.entry("PE", "TRT6"), Map.entry("CE", "TRT7"), Map.entry("PA", "TRT8"),
            Map.entry("AP", "TRT8"), Map.entry("PR", "TRT9"), Map.entry("DF", "TRT10"), Map.entry("TO", "TRT10"),
            Map.entry("AM", "TRT11"), Map.entry("RR", "TRT11"), Map.entry("SC", "TRT12"), Map.entry("PB", "TRT13"),
            Map.entry("RO", "TRT14"), Map.entry("AC", "TRT14"), Map.entry("SP_INTERIOR", "TRT15"), Map.entry("MA", "TRT16"),
            Map.entry("ES", "TRT17"), Map.entry("GO", "TRT18"), Map.entry("AL", "TRT19"), Map.entry("SE", "TRT20"),
            Map.entry("RN", "TRT21"), Map.entry("PI", "TRT22"), Map.entry("MT", "TRT23"), Map.entry("MS", "TRT24")
    );

    private static final Map<String, String> CJM_BY_UF = Map.ofEntries(
            Map.entry("RJ", "1ª CJM"), Map.entry("ES", "1ª CJM"),
            Map.entry("SP", "2ª CJM"),
            Map.entry("RS", "3ª CJM"),
            Map.entry("MG", "4ª CJM"),
            Map.entry("PR", "5ª CJM"), Map.entry("SC", "5ª CJM"),
            Map.entry("BA", "6ª CJM"), Map.entry("SE", "6ª CJM"),
            Map.entry("PE", "7ª CJM"), Map.entry("AL", "7ª CJM"), Map.entry("PB", "7ª CJM"), Map.entry("RN", "7ª CJM"),
            Map.entry("PA", "8ª CJM"), Map.entry("AP", "8ª CJM"), Map.entry("MA", "8ª CJM"),
            Map.entry("MS", "9ª CJM"), Map.entry("MT", "9ª CJM"),
            Map.entry("CE", "10ª CJM"), Map.entry("PI", "10ª CJM"),
            Map.entry("DF", "11ª CJM"), Map.entry("GO", "11ª CJM"), Map.entry("TO", "11ª CJM"),
            Map.entry("AM", "12ª CJM"), Map.entry("AC", "12ª CJM"), Map.entry("RR", "12ª CJM"), Map.entry("RO", "12ª CJM")
    );

    public JudicialTerritorialProfile resolve(TipoJustica tipoJustica,
                                              String uf,
                                              String cidadeAnchor,
                                              String comarcaHint,
                                              String foroHint,
                                              String secaoHint,
                                              String subsecaoHint,
                                              String circunscricaoHint,
                                              GrauJurisdicao grau) {
        String normalizedUf = normalizeUf(uf);
        String cidade = trimToNull(cidadeAnchor);
        String comarca = trimToNull(comarcaHint);
        String foro = trimToNull(foroHint);
        String secao = trimToNull(secaoHint);
        String subsecao = trimToNull(subsecaoHint);
        String circunscricao = trimToNull(circunscricaoHint);
        TipoJustica effective = tipoJustica == null ? TipoJustica.ESTADUAL : tipoJustica;
        TerritorialForumRegistryProfile forumRegistry = territorialForumRegistry.resolve(effective, normalizedUf, cidade, comarca, foro, secao, subsecao, circunscricao, grau);

        JudicialTerritorialProfile base = switch (effective) {
            case FEDERAL -> resolveFederal(normalizedUf, cidade, comarca, foro, secao, subsecao, circunscricao, grau);
            case TRABALHO -> resolveTrabalho(normalizedUf, cidade, comarca, foro, circunscricao, grau);
            case ELEITORAL -> resolveEleitoral(normalizedUf, cidade, comarca, foro, circunscricao, grau);
            case MILITAR_ESTADUAL -> resolveMilitarEstadual(normalizedUf, cidade, comarca, foro, circunscricao, grau);
            case MILITAR_FEDERAL -> resolveMilitarFederal(normalizedUf, cidade, comarca, foro, circunscricao, grau);
            case SUPERIOR -> resolveSuperior(normalizedUf, cidade, comarca, foro, circunscricao, grau);
            default -> resolveEstadual(normalizedUf, cidade, comarca, foro, circunscricao, grau);
        };

        LinkedHashSet<String> warnings = new LinkedHashSet<>(base.warnings());
        warnings.addAll(forumRegistry.warnings());
        String mergedCity = forumRegistry.effectiveMunicipalAnchor(base.cidadeAnchor());
        String mergedComarca = forumRegistry.effectiveJudicialDistrict(base.comarca());
        String mergedForo = forumRegistry.effectivePrimaryForum(base.foro());
        String mergedCircunscricao = firstNonBlank(base.circunscricao(), forumRegistry.effectiveSecondaryForum(null), forumRegistry.effectiveJudicialDistrict(null));
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(base.metadata());
        metadata.put("forumRegistry", forumRegistry.toMap());
        metadata.put("territorialSupportDesk", forumRegistry.effectiveSupportDesk(null));
        metadata.put("forumDistributionCluster", forumRegistry.effectiveDistributionCluster(null));
        metadata.put("forumRegistryReviewChecklist", forumRegistry.reviewChecklist());
        metadata.put("forumRegistryFundamentos", forumRegistry.fundamentos());
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new JudicialTerritorialProfile(
                base.tribunalCodigo(),
                base.tribunalNome(),
                mergedCity,
                mergedComarca,
                mergedForo,
                base.secaoJudiciaria(),
                base.subsecaoJudiciaria(),
                mergedCircunscricao,
                firstNonBlank(base.primeiraInstanciaLabel(), mergedForo, mergedComarca),
                base.segundaInstanciaLabel(),
                base.superiorLabel(),
                base.unidadeBase(),
                firstNonBlank(base.territorialRegistry(), forumRegistry.effectiveDistributionCluster(null), mergedForo, mergedComarca, mergedCity),
                base.specialTerritorialReview() || !forumRegistry.reviewChecklist().isEmpty(),
                List.copyOf(warnings),
                metadata
        );
    }

    private JudicialTerritorialProfile resolveFederal(String uf,
                                                      String cidade,
                                                      String comarca,
                                                      String foro,
                                                      String secao,
                                                      String subsecao,
                                                      String circunscricao,
                                                      GrauJurisdicao grau) {
        String tribunalCodigo = TRF_BY_UF.getOrDefault(defaultUf(uf), "TRF1");
        RecursalTribunalDetalhado detailed = RecursalTribunalDetalhado.fromString(tribunalCodigo);
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (subsecao == null && cidade == null) {
            warnings.add("Malha federal sem subseção/cidade de referência; revisão territorial federal recomendada.");
        }
        String resolvedSecao = firstNonBlank(secao, uf == null ? null : "Seção Judiciária do " + uf);
        String resolvedSubsecao = firstNonBlank(subsecao, cidade == null ? null : "Subseção Judiciária de " + cidade + suffixUf(uf));
        String resolvedForo = firstNonBlank(foro, resolvedSubsecao, cidade == null ? null : "Foro Federal de " + cidade + suffixUf(uf));
        String resolvedComarca = firstNonBlank(comarca, cidade);
        String resolvedCircunscricao = firstNonBlank(circunscricao, resolvedSubsecao, resolvedSecao);
        String unidadeBase = grau == GrauJurisdicao.SEGUNDO_GRAU ? "TURMA_REGIONAL_FEDERAL" : grau == GrauJurisdicao.SUPERIOR ? "TURMA_SUPERIOR" : "VARA_FEDERAL";
        LinkedHashMap<String, Object> metadata = baseMetadata("FEDERAL", tribunalCodigo, detailed, uf, cidade, resolvedComarca, resolvedForo, resolvedSecao, resolvedSubsecao, resolvedCircunscricao);
        metadata.put("recursalCourtCode", tribunalCodigo);
        metadata.put("recursalCourtName", detailed != null ? detailed.descricao() : "Tribunal Regional Federal competente");
        metadata.put("specializedMesh", "SECAO_SUBSECAO_VARA_FEDERAL");
        return new JudicialTerritorialProfile(
                tribunalCodigo,
                detailed != null ? detailed.descricao() : "Tribunal Regional Federal competente",
                cidade,
                resolvedComarca,
                resolvedForo,
                resolvedSecao,
                resolvedSubsecao,
                resolvedCircunscricao,
                firstNonBlank(resolvedSubsecao, resolvedSecao, "Vara Federal competente"),
                detailed != null ? detailed.descricao() : tribunalCodigo,
                "Superior Tribunal de Justiça / Supremo Tribunal Federal",
                unidadeBase,
                buildRegistry(uf, resolvedSubsecao, resolvedForo, resolvedComarca, cidade),
                !warnings.isEmpty(),
                List.copyOf(warnings),
                metadata
        );
    }

    private JudicialTerritorialProfile resolveTrabalho(String uf,
                                                       String cidade,
                                                       String comarca,
                                                       String foro,
                                                       String circunscricao,
                                                       GrauJurisdicao grau) {
        String tribunalCodigo = resolveTrtCode(uf, cidade);
        RecursalTribunalDetalhado detailed = RecursalTribunalDetalhado.fromString(tribunalCodigo);
        String resolvedForo = firstNonBlank(foro, cidade == null ? null : "Foro Trabalhista de " + cidade + suffixUf(uf));
        String resolvedCircunscricao = firstNonBlank(circunscricao, detailed != null ? detailed.descricao() : tribunalCodigo);
        LinkedHashMap<String, Object> metadata = baseMetadata("TRABALHO", tribunalCodigo, detailed, uf, cidade, comarca, resolvedForo, null, null, resolvedCircunscricao);
        metadata.put("regionalCode", tribunalCodigo);
        metadata.put("regionalName", detailed != null ? detailed.descricao() : tribunalCodigo);
        metadata.put("specializedMesh", "FORO_TRABALHISTA_VARA_TRABALHO_TRT");
        return new JudicialTerritorialProfile(
                tribunalCodigo,
                detailed != null ? detailed.descricao() : "Tribunal Regional do Trabalho competente",
                cidade,
                firstNonBlank(comarca, cidade),
                resolvedForo,
                null,
                null,
                resolvedCircunscricao,
                firstNonBlank(resolvedForo, "Vara do Trabalho competente"),
                detailed != null ? detailed.descricao() : tribunalCodigo,
                "Tribunal Superior do Trabalho",
                grau == GrauJurisdicao.SEGUNDO_GRAU ? "TURMA_REGIONAL_TRABALHISTA" : grau == GrauJurisdicao.SUPERIOR ? "TURMA_SUPERIOR" : "VARA_TRABALHO",
                buildRegistry(uf, resolvedForo, firstNonBlank(comarca, cidade), cityToken(cidade)),
                false,
                List.of(),
                metadata
        );
    }

    private JudicialTerritorialProfile resolveEleitoral(String uf,
                                                        String cidade,
                                                        String comarca,
                                                        String foro,
                                                        String circunscricao,
                                                        GrauJurisdicao grau) {
        String tribunalCodigo = resolveTreCode(uf);
        RecursalTribunalDetalhado detailed = RecursalTribunalDetalhado.fromString(tribunalCodigo);
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (circunscricao == null && cidade == null) {
            warnings.add("Justiça Eleitoral sem zona eleitoral/cidade âncora; validar cartório e zona antes do protocolo.");
        }
        String resolvedCircunscricao = firstNonBlank(circunscricao, cidade == null ? null : "Zona Eleitoral de " + cidade + suffixUf(uf));
        String resolvedForo = firstNonBlank(foro, resolvedCircunscricao, cidade == null ? null : "Cartório Eleitoral de " + cidade + suffixUf(uf));
        LinkedHashMap<String, Object> metadata = baseMetadata("ELEITORAL", tribunalCodigo, detailed, uf, cidade, comarca, resolvedForo, null, null, resolvedCircunscricao);
        metadata.put("treCode", tribunalCodigo);
        metadata.put("treName", detailed != null ? detailed.descricao() : tribunalCodigo);
        metadata.put("specializedMesh", "TRE_ZONA_ELEITORAL_TSE");
        return new JudicialTerritorialProfile(
                tribunalCodigo,
                detailed != null ? detailed.descricao() : "Tribunal Regional Eleitoral competente",
                cidade,
                firstNonBlank(comarca, cidade),
                resolvedForo,
                null,
                null,
                resolvedCircunscricao,
                firstNonBlank(resolvedCircunscricao, resolvedForo, "Zona Eleitoral competente"),
                detailed != null ? detailed.descricao() : tribunalCodigo,
                "Tribunal Superior Eleitoral",
                grau == GrauJurisdicao.SEGUNDO_GRAU ? "PLENARIO_REGIONAL_ELEITORAL" : grau == GrauJurisdicao.SUPERIOR ? "PLENARIO_SUPERIOR" : "ZONA_ELEITORAL",
                buildRegistry(uf, resolvedCircunscricao, resolvedForo, cityToken(cidade)),
                !warnings.isEmpty(),
                List.copyOf(warnings),
                metadata
        );
    }

    private JudicialTerritorialProfile resolveMilitarEstadual(String uf,
                                                              String cidade,
                                                              String comarca,
                                                              String foro,
                                                              String circunscricao,
                                                              GrauJurisdicao grau) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        String tribunalCodigo = TJM_STATES.contains(defaultUf(uf)) ? "TJM" + defaultUf(uf) : stateCourtCode(uf);
        String tribunalNome = TJM_STATES.contains(defaultUf(uf)) ? "Tribunal de Justiça Militar do Estado de " + labelUf(uf) : detailedDescription(stateCourtCode(uf), "Tribunal de Justiça competente");
        if (!TJM_STATES.contains(defaultUf(uf))) {
            warnings.add("UF sem Tribunal de Justiça Militar próprio; revisar câmara militar ou órgão especializado no Tribunal de Justiça.");
        }
        String resolvedCircunscricao = firstNonBlank(circunscricao, cidade == null ? null : "Circunscrição/Auditoria Militar de " + cidade + suffixUf(uf));
        String resolvedForo = firstNonBlank(foro, resolvedCircunscricao, cidade == null ? null : "Auditoria Militar de " + cidade + suffixUf(uf));
        LinkedHashMap<String, Object> metadata = baseMetadata("MILITAR_ESTADUAL", tribunalCodigo, null, uf, cidade, comarca, resolvedForo, null, null, resolvedCircunscricao);
        metadata.put("specializedMesh", TJM_STATES.contains(defaultUf(uf)) ? "AUDITORIA_TJM" : "AUDITORIA_CAMARA_MILITAR_TJ");
        metadata.put("hasOwnMilitaryCourt", TJM_STATES.contains(defaultUf(uf)));
        return new JudicialTerritorialProfile(
                tribunalCodigo,
                tribunalNome,
                cidade,
                firstNonBlank(comarca, cidade),
                resolvedForo,
                null,
                null,
                resolvedCircunscricao,
                firstNonBlank(resolvedForo, "Auditoria Militar competente"),
                tribunalNome,
                "Superior Tribunal Militar / Supremo Tribunal Federal",
                grau == GrauJurisdicao.SEGUNDO_GRAU ? "CAMARA_MILITAR" : grau == GrauJurisdicao.SUPERIOR ? "PLENARIO_SUPERIOR" : "AUDITORIA_MILITAR",
                buildRegistry(uf, resolvedCircunscricao, resolvedForo, cityToken(cidade)),
                !warnings.isEmpty(),
                List.copyOf(warnings),
                metadata
        );
    }

    private JudicialTerritorialProfile resolveMilitarFederal(String uf,
                                                             String cidade,
                                                             String comarca,
                                                             String foro,
                                                             String circunscricao,
                                                             GrauJurisdicao grau) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        String cjm = CJM_BY_UF.get(defaultUf(uf));
        if (cjm == null) {
            warnings.add("UF sem CJM identificada no catálogo militar; confirmar auditoria competente na JMU.");
        }
        String resolvedCircunscricao = firstNonBlank(circunscricao, cjm, cidade == null ? null : cjm + " — " + cidade + suffixUf(uf));
        String resolvedForo = firstNonBlank(foro, cjm == null ? null : "Auditoria da " + cjm, cidade == null ? null : "Auditoria Militar da União em " + cidade + suffixUf(uf));
        LinkedHashMap<String, Object> metadata = baseMetadata("MILITAR_FEDERAL", "STM", null, uf, cidade, comarca, resolvedForo, null, null, resolvedCircunscricao);
        metadata.put("cjm", cjm);
        metadata.put("specializedMesh", "CJM_AUDITORIA_STM");
        return new JudicialTerritorialProfile(
                "STM",
                "Superior Tribunal Militar",
                cidade,
                firstNonBlank(comarca, cidade),
                resolvedForo,
                null,
                null,
                resolvedCircunscricao,
                firstNonBlank(resolvedForo, cjm, "Auditoria da Justiça Militar da União"),
                "Superior Tribunal Militar",
                "Supremo Tribunal Federal",
                grau == GrauJurisdicao.SEGUNDO_GRAU ? "PLENARIO_STM" : grau == GrauJurisdicao.SUPERIOR ? "PLENARIO_STM" : "AUDITORIA_MILITAR_UNIAO",
                buildRegistry(uf, cjm, resolvedForo, cityToken(cidade)),
                !warnings.isEmpty(),
                List.copyOf(warnings),
                metadata
        );
    }

    private JudicialTerritorialProfile resolveSuperior(String uf,
                                                       String cidade,
                                                       String comarca,
                                                       String foro,
                                                       String circunscricao,
                                                       GrauJurisdicao grau) {
        String tribunalCodigo = grau == GrauJurisdicao.CONSTITUCIONAL ? "STF" : "STJ";
        String tribunalNome = "STF".equals(tribunalCodigo) ? "Supremo Tribunal Federal" : "Superior Tribunal de Justiça";
        LinkedHashMap<String, Object> metadata = baseMetadata("SUPERIOR", tribunalCodigo, RecursalTribunalDetalhado.fromString(tribunalCodigo), uf, cidade, comarca, foro, null, null, circunscricao);
        metadata.put("specializedMesh", "TRIBUNAL_SUPERIOR_COLEGIADO");
        return new JudicialTerritorialProfile(
                tribunalCodigo,
                tribunalNome,
                cidade,
                comarca,
                firstNonBlank(foro, tribunalNome),
                null,
                null,
                firstNonBlank(circunscricao, tribunalNome),
                tribunalNome,
                tribunalNome,
                tribunalNome,
                grau == GrauJurisdicao.CONSTITUCIONAL ? "PLENARIO" : "TRIBUNAL_SUPERIOR",
                buildRegistry(uf, tribunalCodigo, firstNonBlank(circunscricao, foro, comarca, cidade)),
                false,
                List.of(),
                metadata
        );
    }

    private JudicialTerritorialProfile resolveEstadual(String uf,
                                                       String cidade,
                                                       String comarca,
                                                       String foro,
                                                       String circunscricao,
                                                       GrauJurisdicao grau) {
        String tribunalCodigo = stateCourtCode(uf);
        RecursalTribunalDetalhado detailed = RecursalTribunalDetalhado.fromString(tribunalCodigo);
        String resolvedComarca = firstNonBlank(comarca, cidade);
        String resolvedForo = firstNonBlank(foro, resolvedComarca == null ? null : "Foro da Comarca de " + resolvedComarca + suffixUf(uf), cidade == null ? null : "Foro de " + cidade + suffixUf(uf));
        String resolvedCircunscricao = firstNonBlank(circunscricao, resolvedForo, resolvedComarca);
        LinkedHashMap<String, Object> metadata = baseMetadata("ESTADUAL", tribunalCodigo, detailed, uf, cidade, resolvedComarca, resolvedForo, null, null, resolvedCircunscricao);
        metadata.put("specializedMesh", "COMARCA_FORO_VARA_TJ");
        return new JudicialTerritorialProfile(
                tribunalCodigo,
                detailed != null ? detailed.descricao() : "Tribunal de Justiça competente",
                cidade,
                resolvedComarca,
                resolvedForo,
                null,
                null,
                resolvedCircunscricao,
                firstNonBlank(resolvedForo, resolvedComarca, "Vara estadual competente"),
                detailed != null ? detailed.descricao() : tribunalCodigo,
                "Superior Tribunal de Justiça / Supremo Tribunal Federal",
                grau == GrauJurisdicao.SEGUNDO_GRAU ? "CAMARA_ESTADUAL" : grau == GrauJurisdicao.SUPERIOR ? "TURMA_SUPERIOR" : "VARA_ESTADUAL",
                buildRegistry(uf, resolvedComarca, resolvedForo, cityToken(cidade)),
                false,
                List.of(),
                metadata
        );
    }

    private String resolveTrtCode(String uf, String cidade) {
        String normalizedUf = defaultUf(uf);
        if ("SP".equals(normalizedUf)) {
            return isCapital(cidade) ? "TRT2" : "TRT15";
        }
        return TRT_BY_UF.getOrDefault(normalizedUf, "TRT1");
    }

    private String resolveTreCode(String uf) {
        String normalizedUf = defaultUf(uf);
        return "TRE" + ("DF".equals(normalizedUf) ? "DF" : normalizedUf);
    }

    private String stateCourtCode(String uf) {
        String normalizedUf = defaultUf(uf);
        return "DF".equals(normalizedUf) ? "TJDFT" : "TJ" + normalizedUf;
    }

    private String detailedDescription(String code, String fallback) {
        RecursalTribunalDetalhado detailed = RecursalTribunalDetalhado.fromString(code);
        return detailed != null ? detailed.descricao() : fallback;
    }

    private LinkedHashMap<String, Object> baseMetadata(String branch,
                                                       String tribunalCodigo,
                                                       RecursalTribunalDetalhado detailed,
                                                       String uf,
                                                       String cidade,
                                                       String comarca,
                                                       String foro,
                                                       String secao,
                                                       String subsecao,
                                                       String circunscricao) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("branch", branch);
        metadata.put("tribunalCodigo", tribunalCodigo);
        metadata.put("tribunalNome", detailed != null ? detailed.descricao() : null);
        metadata.put("uf", uf);
        metadata.put("cidade", cidade);
        metadata.put("comarca", comarca);
        metadata.put("foro", foro);
        metadata.put("secaoJudiciaria", secao);
        metadata.put("subsecaoJudiciaria", subsecao);
        metadata.put("circunscricao", circunscricao);
        metadata.put("regionalRecursalCode", detailed != null ? detailed.name() : tribunalCodigo);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return metadata;
    }

    private String buildRegistry(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(normalizeToken(value));
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private boolean isCapital(String cidade) {
        String token = normalizeToken(cidade);
        return "SAO_PAULO".equals(token) || "S_PAOLO".equals(token);
    }

    private String cityToken(String cidade) {
        return cidade == null ? null : normalizeToken(cidade);
    }

    private String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
    }

    private String normalizeUf(String uf) {
        if (uf == null || uf.isBlank()) {
            return null;
        }
        return uf.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultUf(String uf) {
        return firstNonBlank(normalizeUf(uf), "DF");
    }

    private String labelUf(String uf) {
        return defaultUf(uf);
    }

    private String suffixUf(String uf) {
        return uf == null || uf.isBlank() ? "" : "/" + uf;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
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
}
