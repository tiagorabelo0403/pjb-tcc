package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialTabelaOficialItemResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialTabelaOficialResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialTabelaOficialService {

    private static final String VERSION = "v1";
    private static final String ALGORITHM = "SHA-256";
    private static final Instant GENERATED_AT = Instant.parse("2026-03-29T16:00:00Z");

    public CalculoJudicialTabelaOficialResponse catalog(String dominio) {
        String canonical = dominio == null || dominio.isBlank() ? null : CalculoJudicialDomainSupport.requireSupported(dominio);
        List<CalculoJudicialTabelaOficialItemResponse> items = allItems().stream()
                .filter(item -> canonical == null || canonical.equals(item.dominio()))
                .toList();
        return new CalculoJudicialTabelaOficialResponse(
                VERSION,
                profileFingerprint(canonical, items),
                canonical,
                routeMap(canonical),
                items,
                updatePolicy(canonical),
                GENERATED_AT
        );
    }

    public Map<String, Object> profile(String dominio) {
        String canonical = CalculoJudicialDomainSupport.requireSupported(dominio);
        CalculoJudicialTabelaOficialResponse response = catalog(canonical);
        return ordered(
                "route", CalculoJudicialDomainSupport.officialTablesRoute(canonical),
                "version", response.version(),
                "fingerprint", response.fingerprint(),
                "dominio", canonical,
                "coverageLevel", coverageLevel(canonical),
                "primaryAuthority", response.tabelas().isEmpty() ? null : response.tabelas().get(0).orgaoOficial(),
                "primaryMode", response.tabelas().isEmpty() ? null : response.tabelas().get(0).modoAdocaoPjb(),
                "references", response.tabelas().stream().map(item -> ordered(
                        "codigo", item.codigo(),
                        "titulo", item.titulo(),
                        "orgaoOficial", item.orgaoOficial(),
                        "urlOficial", item.urlOficial(),
                        "referenciaTemporal", item.referenciaTemporal(),
                        "fingerprint", item.fingerprint()
                )).toList(),
                "updatePolicy", response.politicaAtualizacao()
        );
    }

    private List<CalculoJudicialTabelaOficialItemResponse> allItems() {
        return List.of(
                item(
                        "TRABALHISTA_CSJT_PJE_CALC",
                        "TRABALHISTA_CLT",
                        "Portal oficial do PJe-Calc com versões, compatibilidade, manual e tutorial",
                        "CSJT",
                        "https://www.csjt.jus.br/web/csjt/pje-calc",
                        "espelho_parametrizado_com_referencia_oficial",
                        "snapshot_2026-03-29",
                        ordered(
                                "signals", List.of("manual", "tutorial", "compatibilidade", "versoes"),
                                "applicability", "trabalhista_clt",
                                "sourceFamily", "PJE_CALC_CSJT"
                        ),
                        ordered(
                                "target", "PJB_TRABALHISTA_CLT",
                                "interoperabilityMode", "metadata_and_route_alignment",
                                "manualRoute", "https://pje.csjt.jus.br/manual/index.php/PJE-Calc",
                                "compatibilityReference", "https://pje.csjt.jus.br/"
                        ),
                        ordered(
                                "addedInPjb", List.of("profile-aware frontend contract", "official route policy", "pdf audit trail", "workspace + bootstrap + help routes"),
                                "gapAddressed", "manual_and_compatibility_visibility",
                                "nextStep", "formal interchange package for external calculators"
                        ),
                        List.of(
                                trail("r42", "contract_cache_capabilities_front", "base de contrato do front consolidada"),
                                trail("r44", "observabilidade_headers_requestid_metricas", "headers, request id e métricas canônicas"),
                                trail("r47", "tabelas_oficiais_interoperabilidade", "perfil oficial de tabelas e trilha de atualização trabalhista")
                        )
                ),
                item(
                        "FAZENDA_CJF_SICOM_CORRECAO",
                        "FAZENDA_TRIBUTARIO",
                        "Tabela oficial de correção monetária e manual de cálculos federais como referência para atualização parametrizada",
                        "CJF / SICOM",
                        "https://sicom.cjf.jus.br/tabelaCorMor.php",
                        "fonte_oficial_de_indice_com_parametrizacao_controlada",
                        "snapshot_2026-03-29",
                        ordered(
                                "signals", List.of("tabelas_correcao", "manual_calculos", "uniformizacao_indices"),
                                "applicability", "fazenda_tributario_publico_federal",
                                "sourceFamily", "CJF_SICOM"
                        ),
                        ordered(
                                "target", "PJB_FAZENDA_TRIBUTARIO",
                                "interoperabilityMode", "series_and_source_registry",
                                "manualRoute", "https://sicom.cjf.jus.br/arquivos/pdf/manual_de_calculos_2025_vf.pdf",
                                "adoptionRule", "selic_and_public_correction_series_remain_parametrized"
                        ),
                        ordered(
                                "addedInPjb", List.of("source fingerprint", "official source registry", "vigencia de tabela", "diff summary"),
                                "gapAddressed", "official source traceability for rates and indices",
                                "nextStep", "sync de séries mensais oficializadas por ente/regime"
                        ),
                        List.of(
                                trail("r41", "bootstrap_http_examples_front", "bootstrap e exemplos HTTP"),
                                trail("r45", "benchmark_oficial_custas_expansao", "benchmark oficial consolidado"),
                                trail("r47", "tabelas_oficiais_interoperabilidade", "registro de fonte oficial para atualização fazendária")
                        )
                ),
                item(
                        "CUSTAS_TRIBUNAIS_PORTAIS",
                        "CUSTAS_PROCESSUAIS",
                        "Portais oficiais de custas, guias, diligências e depósitos judiciais dos tribunais",
                        "TJSP / TJPR / TJGO / TJSC",
                        "https://www.tjsp.jus.br/PortalCustas",
                        "benchmark_operacional_multitribunal_com_parametrizacao_local",
                        "snapshot_2026-03-29",
                        ordered(
                                "signals", List.of("portal_custas", "guias_publicas", "diligencias", "depositos_judiciais", "simuladores"),
                                "applicability", "custas_processuais_e_despesas_locais",
                                "sourceFamily", "TRIBUNAIS_PORTAIS_CUSTAS",
                                "additionalOfficialSources", List.of(
                                        "https://www.tjpr.jus.br/calculadora-de-custas",
                                        "https://projudi.tjgo.jus.br/GuiaInicial1GrauPublica?PaginaAtual=4",
                                        "https://www.tjsc.jus.br/custas"
                                )
                        ),
                        ordered(
                                "target", "PJB_CUSTAS_PROCESSUAIS",
                                "interoperabilityMode", "route_and_table_profile_alignment",
                                "tableValueExamples", List.of("taxa_judiciaria", "diligencia_oficial", "despesas_postais", "deposito_judicial"),
                                "adoptionRule", "parametros locais continuam configuraveis com rastreio da origem oficial"
                        ),
                        ordered(
                                "addedInPjb", List.of("custas + despesas + deposito", "official table profile", "route for official tables", "front-ready table registry"),
                                "gapAddressed", "local tribunal fee table visibility",
                                "nextStep", "adapters por tribunal e sincronização de tabela local"
                        ),
                        List.of(
                                trail("r45", "benchmark_oficial_custas_expansao", "domínio de custas e despesas criado"),
                                trail("r46", "federal_previdenciario_cjf", "expansão do benchmark multissistema"),
                                trail("r47", "tabelas_oficiais_interoperabilidade", "perfil oficial de custas multitribunal")
                        )
                ),
                item(
                        "FEDERAL_CJF_SICOM_MANUAL",
                        "FEDERAL_PREVIDENCIARIO_CJF",
                        "Manual de Cálculos da Justiça Federal e tabelas de correção monetária do SICOM/CJF",
                        "CJF / SICOM",
                        "https://sicom.cjf.jus.br/arquivos/pdf/manual_de_calculos_2025_vf.pdf",
                        "fonte_oficial_prioritaria_com_classificacao_jef",
                        "snapshot_2026-03-29",
                        ordered(
                                "signals", List.of("manual_calculos", "tabelas_correcao", "uniformizacao_federal", "jef_previdenciario"),
                                "applicability", "federal_previdenciario_cjf",
                                "sourceFamily", "CJF_SICOM",
                                "tableRoute", "https://sicom.cjf.jus.br/tabelaCorMor.php"
                        ),
                        ordered(
                                "target", "PJB_FEDERAL_PREVIDENCIARIO_CJF",
                                "interoperabilityMode", "manual_and_table_registry",
                                "paymentClassifier", List.of("RPV", "PRECATORIO"),
                                "adoptionRule", "marcos previdenciarios e classificação de pagamento permanecem auditáveis e parametrizados"
                        ),
                        ordered(
                                "addedInPjb", List.of("DIB/DIP/DCB", "prescrição quinquenal", "abono anual", "classificação RPV/precatório", "official table profile"),
                                "gapAddressed", "manual de cálculo federal e tabela oficial por domínio",
                                "nextStep", "sync controlado de índices por competência e histórico do manual"
                        ),
                        List.of(
                                trail("r45", "benchmark_oficial_custas_expansao", "benchmark oficial apontou lacuna federal/JEF"),
                                trail("r46", "federal_previdenciario_cjf", "domínio federal previdenciário criado"),
                                trail("r47", "tabelas_oficiais_interoperabilidade", "perfil oficial de manual e tabelas CJF/SICOM")
                        )
                )
        );
    }

    private CalculoJudicialTabelaOficialItemResponse item(String codigo,
                                                          String dominio,
                                                          String titulo,
                                                          String orgaoOficial,
                                                          String urlOficial,
                                                          String modoAdocaoPjb,
                                                          String referenciaTemporal,
                                                          Map<String, Object> cobertura,
                                                          Map<String, Object> interoperabilidade,
                                                          Map<String, Object> diffAtual,
                                                          List<Map<String, Object>> trilhaAtualizacao) {
        String canonical = CalculoJudicialDomainSupport.requireSupported(dominio);
        String material = codigo + '|' + canonical + '|' + titulo + '|' + orgaoOficial + '|' + urlOficial + '|' + referenciaTemporal;
        return new CalculoJudicialTabelaOficialItemResponse(
                codigo,
                canonical,
                titulo,
                orgaoOficial,
                urlOficial,
                modoAdocaoPjb,
                referenciaTemporal,
                "2026-03-29",
                null,
                sha256(material),
                ALGORITHM,
                Map.copyOf(cobertura),
                Map.copyOf(interoperabilidade),
                Map.copyOf(diffAtual),
                List.copyOf(trilhaAtualizacao)
        );
    }

    private Map<String, String> routeMap(String dominio) {
        if (dominio == null || dominio.isBlank()) {
            return Map.of(
                    "catalogo", CalculoJudicialDomainSupport.officialTablesRoute(),
                    "workspace", CalculoJudicialDomainSupport.workspaceRoute(),
                    "frontendCatalogo", CalculoJudicialDomainSupport.catalogRoute()
            );
        }
        String canonical = CalculoJudicialDomainSupport.requireSupported(dominio);
        return Map.of(
                "catalogo", CalculoJudicialDomainSupport.officialTablesRoute(canonical),
                "workspace", CalculoJudicialDomainSupport.workspaceRoute(canonical),
                "frontendCatalogo", CalculoJudicialDomainSupport.catalogRoute(canonical),
                "bootstrap", CalculoJudicialDomainSupport.bootstrapRoute(canonical)
        );
    }

    private Map<String, Object> updatePolicy(String dominio) {
        return ordered(
                "mode", "registry_snapshot_with_manual_review",
                "scope", dominio == null ? "global" : CalculoJudicialDomainSupport.requireSupported(dominio),
                "manualReviewRequired", Boolean.TRUE,
                "tracks", List.of("source_url", "vigencia_pjb", "fingerprint", "diff_summary", "update_trail"),
                "officialFamilies", List.of("CSJT_PJE_CALC", "CJF_SICOM", "TRIBUNAIS_PORTAIS_CUSTAS"),
                "nextStep", "sync controlado de tabelas e séries oficiais por competência"
        );
    }

    private String coverageLevel(String dominio) {
        return switch (CalculoJudicialDomainSupport.requireSupported(dominio)) {
            case "TRABALHISTA_CLT", "FEDERAL_PREVIDENCIARIO_CJF" -> "high";
            case "FAZENDA_TRIBUTARIO", "CUSTAS_PROCESSUAIS" -> "medium";
            default -> "baseline";
        };
    }

    private String profileFingerprint(String dominio, List<CalculoJudicialTabelaOficialItemResponse> items) {
        String canonical = dominio == null || dominio.isBlank() ? "GLOBAL" : CalculoJudicialDomainSupport.requireSupported(dominio);
        return sha256(canonical + '|' + items.stream().map(CalculoJudicialTabelaOficialItemResponse::fingerprint).collect(Collectors.joining("|")));
    }

    private Map<String, Object> ordered(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        map.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(map);
    }

    private Map<String, Object> trail(String round, String code, String detail) {
        return ordered(
                "round", round,
                "code", code,
                "detail", detail,
                "appliedAt", GENERATED_AT.toString()
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("sha_256_not_available", ex);
        }
    }
}
