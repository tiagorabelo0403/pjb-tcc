package com.tcc.pjb.backend.core.distribuicao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcc.pjb.backend.model.dto.distribuicao.DistributionWorkbenchActionResponse;
import com.tcc.pjb.backend.model.dto.distribuicao.DistributionWorkbenchLaneResponse;
import com.tcc.pjb.backend.model.dto.distribuicao.DistributionWorkbenchResponse;
import com.tcc.pjb.backend.model.dto.distribuicao.DistributionWorkbenchSummaryResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributionWorkbenchAssemblerService {

    private final DistribuicaoProcessualNacionalEngine engine;
    private final Cache<String, DistributionWorkbenchResponse> workbenchCache;

    public DistributionWorkbenchAssemblerService(DistribuicaoProcessualNacionalEngine engine) {
        this.engine = engine;
        this.workbenchCache = Caffeine.newBuilder()
                .expireAfterWrite(java.time.Duration.ofSeconds(2))
                .maximumSize(512)
                .build();
    }

    public DistributionWorkbenchResponse assemble(String numeroProcesso) {
        String cacheKey = safeNumber(numeroProcesso);
        return workbenchCache.get(cacheKey, this::assembleFresh);
    }

    private DistributionWorkbenchResponse assembleFresh(String numeroProcesso) {
        Map<String, Object> distribution = engine.consultarDistribuicao(numeroProcesso);
        boolean encontrado = booleanValue(distribution.get("encontrado"));
        DistributionWorkbenchSummaryResponse summary = new DistributionWorkbenchSummaryResponse(
                stringValue(distribution.get("tribunalCodigoRoteado")),
                stringValue(distribution.get("unidadeJudiciariaCodigo")),
                stringValue(distribution.get("ultimaFilaDistribuicao")),
                stringValue(distribution.get("ultimaInboxKey")),
                intValue(distribution.get("ultimaPrioridade")),
                stringValue(distribution.get("routingRiskLevel")),
                stringValue(distribution.get("connectorSystem")),
                stringValue(distribution.get("competenciaTerritorialModo")),
                stringValue(distribution.get("preventionMode")),
                stringValue(distribution.get("faseAtual")),
                stringValue(distribution.get("ultimoStatusOperacional"))
        );
        List<DistributionWorkbenchLaneResponse> lanes = List.of(
                lane("TERRITORIO", summary.competenciaTerritorialModo(), distribution, "comarca", "uf", "vara"),
                lane("PREVENCAO", summary.preventionMode(), distribution, "preventionMode", "faseAtual"),
                lane("ESPECIALIZACAO", stringValue(distribution.get("specializedTrack")), distribution, "specializedTrack", "vara", "tribunalCodigoRoteado"),
                lane("OPERACAO", summary.filaDistribuicao(), distribution, "ultimaFilaDistribuicao", "ultimaInboxKey", "ultimoStatusOperacional", "ultimoPrazoOperacional"),
                lane("CONECTOR", summary.connectorSystem(), distribution, "connectorSystem", "routingRiskLevel", "tribunalCodigoRoteado")
        );
        List<DistributionWorkbenchActionResponse> actions = suggestedActions(numeroProcesso, encontrado, summary, distribution);
        LinkedHashMap<String, Object> integrity = new LinkedHashMap<>();
        integrity.put("hasProcessoId", distribution.get("processoId") != null);
        integrity.put("hasWorkItem", distribution.get("ultimoWorkItemId") != null);
        integrity.put("territorialConsistency", !blank(summary.competenciaTerritorialModo()) && !blank(stringValue(distribution.get("comarca"))) && !blank(stringValue(distribution.get("uf"))));
        integrity.put("operationalConsistency", !blank(summary.filaDistribuicao()) && !blank(summary.inboxKey()));
        integrity.put("connectorConsistency", !blank(summary.connectorSystem()) || !blank(summary.routingRiskLevel()));
        LinkedHashMap<String, Object> frontend = new LinkedHashMap<>();
        frontend.put("defaultTab", resolveDefaultTab(summary));
        frontend.put("tabs", lanes.stream().map(DistributionWorkbenchLaneResponse::code).toList());
        frontend.put("refreshEndpoint", "/api/v1/distribuicao/processual/workbench?numeroProcesso=" + safeNumber(numeroProcesso));
        frontend.put("diagnosticEndpoint", "/api/v1/distribuicao/processual/diagnostico");
        frontend.put("distributionEndpoint", "/api/v1/distribuicao/processual");
        return new DistributionWorkbenchResponse(numeroProcesso, encontrado, summary, lanes, actions, integrity, frontend);
    }

    private List<DistributionWorkbenchActionResponse> suggestedActions(String numeroProcesso,
                                                                      boolean encontrado,
                                                                      DistributionWorkbenchSummaryResponse summary,
                                                                      Map<String, Object> distribution) {
        List<DistributionWorkbenchActionResponse> actions = new ArrayList<>();
        if (!encontrado) {
            actions.add(action("DISTRIBUIR_AGORA", "Submeter distribuição inicial", "critical", true, "/api/v1/distribuicao/processual", payloadOf("numeroProcesso", numeroProcesso)));
            actions.add(action("RODAR_DIAGNOSTICO", "Rodar diagnóstico pré-distribuição", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
            return List.copyOf(actions);
        }
        if (blank(summary.filaDistribuicao()) || blank(summary.inboxKey())) {
            actions.add(action("RECONSTRUIR_TRILHA_OPERACIONAL", "Reconstruir fila e inbox operacionais", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("ALTO".equalsIgnoreCase(summary.routingRiskLevel()) || "CRITICO".equalsIgnoreCase(summary.routingRiskLevel())) {
            actions.add(action("REVISAR_RISCO_TERRITORIAL", "Revisar risco territorial e competência", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if (!blank(summary.preventionMode()) && summary.preventionMode().toUpperCase(Locale.ROOT).contains("PENDENTE")) {
            actions.add(action("ANALISAR_PREVENCAO", "Analisar prevenção, conexão e dependência", "medium", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        String specializedTrack = stringValue(distribution.get("specializedTrack"));
        if ("CUSTODIA".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_CUSTODIA", "Validar apresentação e mesa de custódia", "critical", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("EXECUCAO_FISCAL".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_TITULO_FAZENDARIO", "Conferir CDA e prevenção fazendária", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if (specializedTrack != null && specializedTrack.startsWith("JUIZADO")) {
            actions.add(action("VALIDAR_TETO_JUIZADO", "Conferir teto e especialização do juizado", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("FEDERAL".equalsIgnoreCase(specializedTrack) || "PREVIDENCIARIO".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("CONFIRMAR_SECAO_SUBSECAO", "Confirmar seção e subseção judiciária", "medium", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("TRABALHISTA".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_VARA_TRABALHO", "Confirmar vara do trabalho e prevenção laboral", "medium", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("ELEITORAL".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_ZONA_ELEITORAL", "Conferir zona e circunscrição eleitorais", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("MILITAR".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_AUDITORIA_MILITAR", "Conferir auditoria ou conselho de justiça militar", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("FAMILIA_SUCESSOES".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("REVISAR_SIGILO_FAMILIA", "Revisar prevenção e sigilo de família e sucessões", "medium", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("INFANCIA_JUVENTUDE".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_REDE_PROTETIVA", "Confirmar trilha protetiva ou infracional", "critical", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("EXECUCAO_PENAL".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_GUIA_EXECUCAO", "Conferir guia e unidade de execução penal", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("AMBIENTAL".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_COMPETENCIA_AMBIENTAL", "Confirmar dano ambiental e base territorial", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("AGRARIO".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_CONFLITO_AGRARIO", "Conferir imóvel rural e âncora territorial agrária", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("EMPRESARIAL".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_JUIZO_UNIVERSAL", "Confirmar juízo universal de recuperação ou falência", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("ADMINISTRATIVO_IMPROBIDADE".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_COMPETENCIA_PUBLICA", "Conferir unidade administrativa ou fazendária competente", "medium", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("INTERNACIONAL".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_AUTORIDADE_CENTRAL", "Confirmar autoridade central e cooperação internacional", "high", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("AUTOCOMPOSICAO".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_CAMARA_CONSENSUAL", "Confirmar CEJUSC, câmara ou cláusula consensual", "medium", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if ("CONSTITUCIONAL".equalsIgnoreCase(specializedTrack)) {
            actions.add(action("VALIDAR_COMPETENCIA_CONSTITUCIONAL", "Conferir órgão constitucional competente", "critical", true, "/api/v1/distribuicao/processual/diagnostico", payloadOf("numeroProcesso", numeroProcesso)));
        }
        if (distribution.get("processoId") != null) {
            actions.add(action("REDISTRIBUIR_IMPEDIMENTO", "Solicitar redistribuição por impedimento", "medium", true, "/api/v1/distribuicao/processual/processos/" + distribution.get("processoId") + "/redistribuicao", Map.of("motivoImpedimento", "IMPEDIMENTO_SUPERVENIENTE")));
        }
        return List.copyOf(actions);
    }

    private DistributionWorkbenchLaneResponse lane(String code, String reference, Map<String, Object> distribution, String... keys) {
        LinkedHashSet<String> highlights = new LinkedHashSet<>();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        for (String key : keys) {
            String value = stringValue(distribution.get(key));
            if (!blank(value)) {
                metadata.put(key, value);
                highlights.add(key + ": " + value);
            }
        }
        String status = highlights.isEmpty() ? "IDLE" : blank(reference) ? "READY" : "ACTIVE";
        String descriptor = !blank(reference) ? reference : highlights.stream().findFirst().orElse("Sem trilha operacional imediata");
        return new DistributionWorkbenchLaneResponse(code, status, descriptor, highlights.stream().limit(4).toList(), metadata);
    }

    private DistributionWorkbenchActionResponse action(String action, String label, String severity, boolean enabled, String endpoint, Map<String, Object> payload) {
        return new DistributionWorkbenchActionResponse(action, label, severity, enabled, endpoint, payload);
    }

    private Map<String, Object> payloadOf(String key, Object value) {
        if (value == null) {
            return Map.of();
        }
        return Map.of(key, value);
    }

    private String resolveDefaultTab(DistributionWorkbenchSummaryResponse summary) {
        if (!blank(summary.routingRiskLevel()) && ("ALTO".equalsIgnoreCase(summary.routingRiskLevel()) || "CRITICO".equalsIgnoreCase(summary.routingRiskLevel()))) {
            return "CONECTOR";
        }
        if (!blank(summary.preventionMode())) {
            return "PREVENCAO";
        }
        return "OPERACAO";
    }

    private static String safeNumber(String numeroProcesso) {
        return numeroProcesso == null ? "" : numeroProcesso;
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean b && b;
    }

    private static Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
