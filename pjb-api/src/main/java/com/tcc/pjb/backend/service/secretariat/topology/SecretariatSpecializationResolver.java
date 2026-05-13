package com.tcc.pjb.backend.service.secretariat.topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SecretariatSpecializationResolver {

    public SecretariatSpecializationProfile resolve(String tribunalCodigo,
                                                   String instanciaAxis,
                                                   String regimeAxis,
                                                   String ramoAxis,
                                                   String secretariatCode,
                                                   String receiptInboxKey,
                                                   String saneamentoQueueCode,
                                                   String audienceQueueCode,
                                                   String executionQueueCode,
                                                   String organizationalPath,
                                                   Map<String, Object> topologyMetadata) {
        String instanceClass = resolveInstanceClass(instanciaAxis);
        String branchClass = resolveBranchClass(tribunalCodigo, regimeAxis, ramoAxis, topologyMetadata);
        String namespacePjb = resolveNamespace(instanceClass, branchClass);
        String painelPjb = resolveDisplayName(namespacePjb, instanceClass, branchClass);
        String secretariatClass = resolveSecretariatClass(instanceClass, branchClass);
        String specializedSecretariatName = resolveSpecializedSecretariatName(instanceClass, branchClass, tribunalCodigo, topologyMetadata);
        String specializedSecretariatCode = buildSpecializedSecretariatCode(secretariatCode, secretariatClass, branchClass);
        String panelSlug = normalizeToken(namespacePjb) + ':' + normalizeToken(branchClass) + ':' + normalizeToken(instanceClass);
        List<String> connectedCapabilities = buildCapabilities(instanceClass, branchClass);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", blankToNull(tribunalCodigo));
        metadata.put("organizationalPath", blankToNull(organizationalPath));
        metadata.put("receiptInboxKey", blankToNull(receiptInboxKey));
        metadata.put("connectedQueues", List.of(
                safeToken(saneamentoQueueCode),
                safeToken(audienceQueueCode),
                safeToken(executionQueueCode)
        ));
        metadata.put("topologyMetadataKeys", topologyMetadata == null ? List.of() : List.copyOf(topologyMetadata.keySet()));
        metadata.put("panelSlug", panelSlug);
        metadata.put("institutionBirthMode", "INSTITUICAO_JUDICIARIA_RAIZ");
        metadata.put("institutionOperatingModel", resolveInstitutionOperatingModel(instanceClass, branchClass));
        metadata.put("institutionRootCode", blankToNull(tribunalCodigo));
        metadata.put("secretariatVisibilityScope", secretariatClass + ':' + branchClass + ':' + instanceClass);
        metadata.put("unitCode", topologyMetadata == null ? null : blankToNull(asString(topologyMetadata.get("unitCode"))));
        metadata.put("unitDescriptor", topologyMetadata == null ? null : blankToNull(asString(topologyMetadata.get("unitDescriptor"))));
        metadata.values().removeIf(this::isEmptyValue);
        return new SecretariatSpecializationProfile(
                secretariatClass,
                instanceClass,
                branchClass,
                namespacePjb,
                painelPjb,
                panelSlug,
                specializedSecretariatCode,
                specializedSecretariatName,
                blankToNull(receiptInboxKey),
                List.copyOf(connectedCapabilities),
                Collections.unmodifiableMap(metadata)
        );
    }

    private String resolveInstanceClass(String instanciaAxis) {
        String token = normalizeToken(instanciaAxis);
        if (token.contains("SEGUNDO") || token.contains("SEGUNDA") || token.endsWith("2G") || token.contains("2_G")) {
            return "SEGUNDA_INSTANCIA";
        }
        if (token.contains("SUPERIOR") || token.contains("TRIBUNAL_SUPERIOR") || token.contains("CONSTITUCIONAL")) {
            return "INSTANCIA_SUPERIOR";
        }
        return "PRIMEIRA_INSTANCIA";
    }

    private String resolveBranchClass(String tribunalCodigo,
                                      String regimeAxis,
                                      String ramoAxis,
                                      Map<String, Object> topologyMetadata) {
        String laneAxis = topologyMetadata == null ? null : asString(topologyMetadata.get("laneAxis"));
        String forumAxis = topologyMetadata == null ? null : asString(topologyMetadata.get("forumAxis"));
        String token = String.join("|",
                safeToken(tribunalCodigo),
                safeToken(regimeAxis),
                safeToken(ramoAxis),
                safeToken(laneAxis),
                safeToken(forumAxis));
        if (token.contains("JUIZADO") || token.contains("TURMA_RECURSAL") || token.contains("JE")) {
            return "JUIZADO_ESPECIAL";
        }
        if (token.contains("ELEITORAL") || token.startsWith("TRE") || token.startsWith("TSE")) {
            return "ELEITORAL";
        }
        if (token.contains("TRABALHO") || token.contains("TRABALHISTA") || token.startsWith("TRT") || token.startsWith("TST")) {
            return "TRABALHISTA";
        }
        if (token.contains("MILITAR") || token.startsWith("TJM") || token.startsWith("STM") || token.contains("AUDITORIA")) {
            return "MILITAR";
        }
        if (token.contains("FEDERAL") || token.startsWith("TRF") || token.startsWith("STJ") || token.startsWith("JF")) {
            return "FEDERAL";
        }
        return "ESTADUAL";
    }

    private String resolveNamespace(String instanceClass, String branchClass) {
        if ("SEGUNDA_INSTANCIA".equals(instanceClass)) {
            return "PJB_SEGUNDA_INSTANCIA";
        }
        if ("INSTANCIA_SUPERIOR".equals(instanceClass)) {
            return "PJB_INSTANCIA_SUPERIOR";
        }
        return switch (branchClass) {
            case "JUIZADO_ESPECIAL" -> "PJB_JUIZADO_ESPECIAL";
            case "TRABALHISTA" -> "PJB_TRABALHISTA";
            case "ELEITORAL" -> "PJB_ELEITORAL";
            case "MILITAR" -> "PJB_MILITAR";
            case "FEDERAL" -> "PJB_FEDERAL";
            default -> "PJB_ESTADUAL";
        };
    }

    private String resolveDisplayName(String namespacePjb, String instanceClass, String branchClass) {
        String base = switch (namespacePjb) {
            case "PJB_SEGUNDA_INSTANCIA" -> "PJB Segunda Instância";
            case "PJB_INSTANCIA_SUPERIOR" -> "PJB Instância Superior";
            case "PJB_JUIZADO_ESPECIAL" -> "PJB Juizado Especial";
            case "PJB_TRABALHISTA" -> "PJB Trabalhista";
            case "PJB_ELEITORAL" -> "PJB Eleitoral";
            case "PJB_MILITAR" -> "PJB Militar";
            case "PJB_FEDERAL" -> "PJB Federal";
            default -> "PJB Estadual";
        };
        if (namespacePjb.startsWith("PJB_") && (namespacePjb.contains("SEGUNDA_INSTANCIA") || namespacePjb.contains("INSTANCIA_SUPERIOR"))) {
            return base + " | " + humanize(branchClass);
        }
        return base + " | " + humanize(instanceClass);
    }

    private String resolveSecretariatClass(String instanceClass, String branchClass) {
        return "SECRETARIA_" + normalizeToken(instanceClass) + '_' + normalizeToken(branchClass);
    }

    private String buildSpecializedSecretariatCode(String secretariatCode, String secretariatClass, String branchClass) {
        String base = blankToNull(secretariatCode);
        if (base == null) {
            return secretariatClass;
        }
        if (normalizeToken(base).contains(normalizeToken(branchClass))) {
            return base;
        }
        return base + '_' + normalizeToken(branchClass);
    }

    private String resolveSpecializedSecretariatName(String instanceClass,
                                                     String branchClass,
                                                     String tribunalCodigo,
                                                     Map<String, Object> topologyMetadata) {
        String tribunal = blankToNull(tribunalCodigo);
        String unitDescriptor = topologyMetadata == null ? null : asString(topologyMetadata.get("unitDescriptor"));
        String prefix = switch (branchClass) {
            case "ELEITORAL" -> "Secretaria Judiciária Eleitoral";
            case "TRABALHISTA" -> "Secretaria-Geral Judiciária Trabalhista";
            case "MILITAR" -> "Secretaria Judiciária Militar";
            case "JUIZADO_ESPECIAL" -> "Secretaria do Juizado Especial";
            case "FEDERAL" -> "Secretaria Judiciária Federal";
            default -> "Secretaria Judiciária Estadual";
        };
        String suffix = switch (instanceClass) {
            case "SEGUNDA_INSTANCIA" -> "de Segunda Instância";
            case "INSTANCIA_SUPERIOR" -> "de Instância Superior";
            default -> "de Primeira Instância";
        };
        StringBuilder builder = new StringBuilder(prefix).append(' ').append(suffix);
        if (tribunal != null) {
            builder.append(" - ").append(tribunal);
        }
        if (unitDescriptor != null && !normalizeToken(unitDescriptor).equals("BASE")) {
            builder.append(" | ").append(unitDescriptor);
        }
        return builder.toString();
    }

    private List<String> buildCapabilities(String instanceClass, String branchClass) {
        LinkedHashMap<String, String> capabilities = new LinkedHashMap<>();
        capabilities.put("RECEBIMENTO", "Recebimento institucional");
        capabilities.put("SANEAMENTO", "Saneamento cartorário");
        capabilities.put("PAUTA", "Preparação e registro de pauta");
        capabilities.put("ATOS_OFICIAIS", "Atos oficiais e expedição");
        capabilities.put("MINUTA_JUNTADA", "Minutas e juntadas");
        capabilities.put("EXPEDICAO_LOTE", "Expedição em lote");
        capabilities.put("TRIAGEM_DOCUMENTAL", "Triagem documental");
        capabilities.put("CHECKLIST_OPERACIONAL", "Checklist operacional");
        capabilities.put("DISTRIBUICAO_INTERNA", "Distribuição interna");
        capabilities.put("SLA", "SLA e alertas");
        capabilities.put("REDISTRIBUICAO", "Redistribuição topológica");
        capabilities.put("RADAR_GARGALOS", "Radar de gargalos");
        capabilities.put("ESTABILIDADE", "Estabilidade operacional");
        capabilities.put("PRESENCA_AUDIENCIA", "Presença e recursos de audiência");
        capabilities.put("GAVETAS_ATOS", "Gavetas de atos oficiais");
        capabilities.put("MALHA_TOPOLOGICA", "Malha e segregação topológica");
        capabilities.put("HANDOFF_GABINETE", "Handoff gabinete-secretaria");
        if (!"PRIMEIRA_INSTANCIA".equals(instanceClass)) {
            capabilities.put("FLUXO_RECURSAL", "Fluxo recursal conectado");
            capabilities.put("COLEGIADO", "Fluxo colegiado");
            capabilities.put("ADMISSIBILIDADE_RECURSAL", "Mesa de admissibilidade recursal");
            capabilities.put("REVISAO_RECURSAL", "Mesa de revisão recursal");
            capabilities.put("EMBARGOS", "Mesa de embargos");
            capabilities.put("DIVISAO_CAMARA_TURMA", "Divisão por câmaras, turmas e órgãos julgadores");
            capabilities.put("SECRETARIA_CAMARA_TURMA", "Secretaria de câmara, turma, seção ou plenário");
            capabilities.put("GABINETE_RELATOR", "Coordenação com gabinete do relator e revisores");
            capabilities.put("PAUTA_COLEGIADA", "Pauta colegiada e sessão de julgamento");
            capabilities.put("MESA_SESSAO", "Mesa de sessão e registro do julgamento");
            capabilities.put("PUBLICACAO_PAUTA", "Publicação de pauta e comunicações colegiadas");
            capabilities.put("SUSTENTACAO_ORAL", "Recepção e coordenação de sustentação oral");
            capabilities.put("ACORDAO", "Lavratura, publicação e controle de acórdão");
            capabilities.put("MESA_ACORDAO", "Mesa específica de acórdão e publicação colegiada");
            capabilities.put("BAIXA_ORIGEM", "Baixa e retorno à origem após colegiado");
        }
        if ("JUIZADO_ESPECIAL".equals(branchClass)) {
            capabilities.put("TURMA_RECURSAL", "Turma recursal e juizado especial");
        }
        if ("ELEITORAL".equals(branchClass)) {
            capabilities.put("PAUTA_ELEITORAL", "Pauta e secretaria judiciária eleitoral");
            capabilities.put("CARTORIO_ELEITORAL", "Integração com cartórios eleitorais e zonas");
            capabilities.put("AUTUACAO_DISTRIBUICAO_ELEITORAL", "Autuação, distribuição e informações processuais eleitorais");
            capabilities.put("SUPORTE_MALHA_ELEITORAL_PJB", "Suporte à malha eleitoral do PJB com compatibilidade institucional");
            capabilities.put("CORREGEDORIA_ELEITORAL_PJB", "Fluxos disciplinares e correicionais eleitorais dentro da malha PJB");
            capabilities.put("PESQUISAS_ELEITORAIS", "Controle de demandas de pesquisas e registros eleitorais");
            capabilities.put("DADOS_PARTIDARIOS", "Dados partidários e prestação de contas");
            capabilities.put("INSPECAO_CORREGEDORIA", "Ciclos de inspeção e coordenação correicional");
        }
        if ("TRABALHISTA".equals(branchClass)) {
            capabilities.put("PAUTA_TRABALHISTA", "Pauta e secretaria judiciária trabalhista");
            capabilities.put("CUSTAS_GRU_JUDICIAL", "Custas e emolumentos via GRU Judicial");
            capabilities.put("ACERVO_DIGITAL", "Acervo digital para anexos audiovisuais");
            capabilities.put("MIDIAS_PROCESSUAIS_PJB", "Gestão de mídias e audiovisuais processuais dentro do PJB");
            capabilities.put("PROCURADORIAS_CREDENCIAMENTO", "Fluxos de procuradorias e credenciamento");
            capabilities.put("EXECUCAO_INTEGRADA", "Execução e cumprimento com apoio integrado");
        }
        if ("MILITAR".equals(branchClass)) {
            capabilities.put("AUDITORIA_MILITAR", "Auditoria, conselho ou colegiado militar");
            capabilities.put("EPROC_JMU", "Fluxo processual militar orientado a eproc");
            capabilities.put("PLANTAO_JUDICIARIO", "Plantão judiciário e urgências militares");
            capabilities.put("BALCAO_VIRTUAL", "Atendimento e balcão virtual institucional");
            capabilities.put("SUSTENTACAO_VIDEO", "Sustentação remota e apoio à videoconferência");
            capabilities.put("LINHA_EVENTOS_MILITAR", "Linha de eventos e atos militares especializados");
        }
        return new ArrayList<>(capabilities.keySet());
    }

    private String resolveInstitutionOperatingModel(String instanceClass, String branchClass) {
        if ("ELEITORAL".equals(branchClass)) {
            return "SECRETARIA_JUDICIARIA_ELEITORAL_COM_CARTORIOS_CORREGEDORIA_E_MALHA_PJB";
        }
        if ("TRABALHISTA".equals(branchClass)) {
            return "SECRETARIA_TRABALHISTA_COM_PAUTA_GRU_ACERVO_DIGITAL_E_MIDIAS_PROCESSUAIS_PJB";
        }
        if ("MILITAR".equals(branchClass)) {
            return "SECRETARIA_MILITAR_COM_AUDITORIA_EPROC_PLANTAO_E_BALCAO_VIRTUAL";
        }
        if (!"PRIMEIRA_INSTANCIA".equals(instanceClass)) {
            return "SECRETARIA_COLEGIADA_COM_CAMARA_TURMA_RELATOR_PAUTA_SESSAO_E_ACORDAO";
        }
        return "SECRETARIA_DE_UNIDADE_COM_RECEBIMENTO_SANEAMENTO_COMUNICACAO_E_CUMPRIMENTO";
    }

    private String humanize(String value) {
        String token = normalizeToken(value).replace('_', ' ').trim();
        if (token.isBlank()) {
            return "BASE";
        }
        String[] pieces = token.split(" +");
        StringBuilder builder = new StringBuilder();
        for (String piece : pieces) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(piece.charAt(0)).append(piece.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "BASE";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safeToken(String value) {
        return blankToNull(value) == null ? "BASE" : value.trim();
    }

    private boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence sequence) {
            return sequence.toString().isBlank();
        }
        if (value instanceof List<?> list) {
            return list.isEmpty();
        }
        return false;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public record SecretariatSpecializationProfile(
            String secretariatClass,
            String secretariatInstanceClass,
            String secretariatBranchClass,
            String namespacePjb,
            String painelPjb,
            String panelSlug,
            String specializedSecretariatCode,
            String specializedSecretariatName,
            String inheritedInboxKey,
            List<String> connectedCapabilities,
            Map<String, Object> metadata
    ) {

        public SecretariatSpecializationProfile {
            Objects.requireNonNull(secretariatClass);
            Objects.requireNonNull(secretariatInstanceClass);
            Objects.requireNonNull(secretariatBranchClass);
            Objects.requireNonNull(namespacePjb);
            Objects.requireNonNull(painelPjb);
            connectedCapabilities = connectedCapabilities == null ? List.of() : List.copyOf(connectedCapabilities);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
        }

        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("secretariatClass", secretariatClass);
            out.put("secretariatInstanceClass", secretariatInstanceClass);
            out.put("secretariatBranchClass", secretariatBranchClass);
            out.put("namespacePjb", namespacePjb);
            out.put("painelPjb", painelPjb);
            out.put("panelSlug", panelSlug);
            out.put("specializedSecretariatCode", specializedSecretariatCode);
            out.put("specializedSecretariatName", specializedSecretariatName);
            out.put("inheritedInboxKey", inheritedInboxKey);
            out.put("connectedCapabilities", connectedCapabilities);
            out.put("metadata", metadata);
            out.values().removeIf(value -> value == null || (value instanceof CharSequence c && c.toString().isBlank()));
            return Collections.unmodifiableMap(out);
        }
    }
}
