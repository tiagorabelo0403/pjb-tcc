package com.tcc.pjb.backend.service.secretariat.query.reference;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.core.forum.routing.SecretariatInboxKeyParser;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatFlowBridgeProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatJudicialIntegrationProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SecretariatJudicialReferenceModelService {

    public ReferenceModelSnapshot resolve(String inboxKey,
                                          String queueCode,
                                          ForumDeskPortfolioProfile portfolio,
                                          SecretariatDeskLoadProfile deskProfile,
                                          SecretariatFlowBridgeProfile bridgeProfile,
                                          SecretariatJudicialIntegrationProfile integrationProfile) {
        Objects.requireNonNull(portfolio, "portfolio");
        Objects.requireNonNull(deskProfile, "deskProfile");
        Objects.requireNonNull(bridgeProfile, "bridgeProfile");
        Objects.requireNonNull(integrationProfile, "integrationProfile");

        String instanceClass = resolveInstanceClass(inboxKey, portfolio, integrationProfile, queueCode);
        String branchClass = resolveBranchClass(inboxKey, portfolio, integrationProfile, queueCode);
        boolean secondInstance = "SEGUNDA_INSTANCIA".equals(instanceClass) || "INSTANCIA_SUPERIOR".equals(instanceClass);
        LinkedHashMap<String, Object> models = new LinkedHashMap<>();
        models.put("PJE", pjeModel(integrationProfile, portfolio, deskProfile));
        models.put("E_SAJ", esajModel(integrationProfile, deskProfile));
        models.put("EPROC", eprocModel(queueCode, bridgeProfile, integrationProfile, deskProfile));
        if (secondInstance) {
            models.put("TRIBUNAL_COLEGIADO", collegiateModel(queueCode, integrationProfile, bridgeProfile));
        }
        if ("ELEITORAL".equals(branchClass)) {
            models.put("ELEITORAL", eleitoralModel(secondInstance, integrationProfile, bridgeProfile));
        }
        if ("TRABALHISTA".equals(branchClass)) {
            models.put("TRABALHISTA", trabalhistaModel(secondInstance, integrationProfile, queueCode));
        }
        if ("MILITAR".equals(branchClass)) {
            models.put("MILITAR", militarModel(secondInstance, integrationProfile, bridgeProfile));
        }

        List<String> gaps = buildGaps(instanceClass, branchClass, queueCode, portfolio, deskProfile, bridgeProfile, integrationProfile);
        List<String> labels = buildLabels(instanceClass, branchClass, queueCode, gaps);
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("instanceClass", instanceClass);
        diagnostics.put("branchClass", branchClass);
        diagnostics.put("operationalMode", secondInstance ? "COLEGIADO" : "UNIDADE_SINGULAR");
        diagnostics.put("queueFamilies", queueFamilies(instanceClass, branchClass));
        diagnostics.put("suggestedDesks", suggestedDesks(instanceClass, branchClass, portfolio, bridgeProfile, integrationProfile));
        diagnostics.put("supportsCollegiateAgenda", secondInstance);
        diagnostics.put("supportsSustentacaoOralDesk", secondInstance || "MILITAR".equals(branchClass) || "ELEITORAL".equals(branchClass));
        diagnostics.put("supportsAcordaoPublicationDesk", secondInstance);
        diagnostics.put("supportsBaixaOrigem", secondInstance);
        diagnostics.put("supportsInspecaoCorregedoria", "ELEITORAL".equals(branchClass));
        diagnostics.put("supportsMidiasProcessuais", "TRABALHISTA".equals(branchClass) || "MILITAR".equals(branchClass) || "ELEITORAL".equals(branchClass));
        diagnostics.values().removeIf(this::emptyValue);

        return new ReferenceModelSnapshot(
                instanceClass,
                branchClass,
                Map.copyOf(models),
                List.copyOf(gaps),
                List.copyOf(labels),
                Map.copyOf(diagnostics)
        );
    }

    public ReferenceCatalogView catalog() {
        List<ReferenceCatalogRow> rows = List.of(
                row("PRIMEIRA_INSTANCIA", "ESTADUAL", "Cumprimento, expediente, juntada, audiência e redistribuição", queueFamilies("PRIMEIRA_INSTANCIA", "ESTADUAL")),
                row("SEGUNDA_INSTANCIA", "ESTADUAL", "Admissibilidade, relatoria, pauta, sessão, acórdão e baixa", queueFamilies("SEGUNDA_INSTANCIA", "ESTADUAL")),
                row("SEGUNDA_INSTANCIA", "JUIZADO_ESPECIAL", "Turma recursal, admissibilidade simplificada, pauta e acórdão", queueFamilies("SEGUNDA_INSTANCIA", "JUIZADO_ESPECIAL")),
                row("SEGUNDA_INSTANCIA", "ELEITORAL", "Secretaria judiciária eleitoral com agenda colegiada, corregedoria e integração institucional", queueFamilies("SEGUNDA_INSTANCIA", "ELEITORAL")),
                row("SEGUNDA_INSTANCIA", "TRABALHISTA", "Colegiado trabalhista, custas GRU Judicial, execução e mídias", queueFamilies("SEGUNDA_INSTANCIA", "TRABALHISTA")),
                row("SEGUNDA_INSTANCIA", "MILITAR", "Câmara/auditoria militar, sessão colegiada e sustentação por videoconferência", queueFamilies("SEGUNDA_INSTANCIA", "MILITAR")),
                row("INSTANCIA_SUPERIOR", "FEDERAL", "Seções e turmas superiores com pauta, sessão, acórdão e triagem recursal", queueFamilies("INSTANCIA_SUPERIOR", "FEDERAL")),
                row("INSTANCIA_SUPERIOR", "TRABALHISTA", "TST e secretaria judiciária superior com pauta e publicação colegiada", queueFamilies("INSTANCIA_SUPERIOR", "TRABALHISTA")),
                row("INSTANCIA_SUPERIOR", "ELEITORAL", "TSE/TRE com agenda colegiada, corregedoria e apoio às funções judiciárias e disciplinares", queueFamilies("INSTANCIA_SUPERIOR", "ELEITORAL")),
                row("INSTANCIA_SUPERIOR", "MILITAR", "STM e secretarias de julgamento com pauta e sustentação remota", queueFamilies("INSTANCIA_SUPERIOR", "MILITAR"))
        );
        return new ReferenceCatalogView(rows);
    }

    private ReferenceCatalogRow row(String instanceClass, String branchClass, String descriptor, List<String> queueFamilies) {
        return new ReferenceCatalogRow(instanceClass, branchClass, descriptor, queueFamilies, buildCatalogCapabilities(instanceClass, branchClass));
    }

    private Map<String, String> pjeModel(SecretariatJudicialIntegrationProfile integrationProfile,
                                         ForumDeskPortfolioProfile portfolio,
                                         SecretariatDeskLoadProfile deskProfile) {
        return Map.of(
                "workflow", "ATENDIDO",
                "painelExpedientes", integrationProfile.ackChannel() == null ? "PARCIAL" : "ATENDIDO",
                "prepararComunicacao", integrationProfile.protocolDesk() == null ? "PARCIAL" : "ATENDIDO",
                "analiseJuntada", portfolio.triageDesk() == null ? "PARCIAL" : "ATENDIDO",
                "secretariaUnificada", deskProfile.coordinationMode() == null ? "PARCIAL" : "ATENDIDO"
        );
    }

    private Map<String, String> esajModel(SecretariatJudicialIntegrationProfile integrationProfile,
                                          SecretariatDeskLoadProfile deskProfile) {
        return Map.of(
                "filasDeTrabalho", "ATENDIDO",
                "controleVisualPrazo", deskProfile.secrecyPressure() || deskProfile.hearingPressure() || deskProfile.forceRedistribution() ? "ATENDIDO" : "PARCIAL",
                "atosEmLote", integrationProfile.dispatchWindow() == null ? "PARCIAL" : "ATENDIDO",
                "cienciaEletronica", integrationProfile.ackChannel() == null ? "PARCIAL" : "ATENDIDO",
                "principalAcessorio", integrationProfile.proofBundleMode() == null ? "PARCIAL" : "ATENDIDO"
        );
    }

    private Map<String, String> eprocModel(String queueCode,
                                           SecretariatFlowBridgeProfile bridgeProfile,
                                           SecretariatJudicialIntegrationProfile integrationProfile,
                                           SecretariatDeskLoadProfile deskProfile) {
        return Map.of(
                "localizadores", bridgeProfile.bridgeMode() == null ? "PARCIAL" : "ATENDIDO",
                "linhaDoTempoEventos", isEventQueue(queueCode) ? "ATENDIDO" : "PARCIAL",
                "automacao", integrationProfile.syncMode() == null ? "PARCIAL" : "ATENDIDO",
                "secretariaMultiunidade", deskProfile.redistributionDesk() == null ? "PARCIAL" : "ATENDIDO",
                "roteamentoFlexivel", bridgeProfile.downstreamAxis() == null ? "PARCIAL" : "ATENDIDO"
        );
    }

    private Map<String, String> collegiateModel(String queueCode,
                                                SecretariatJudicialIntegrationProfile integrationProfile,
                                                SecretariatFlowBridgeProfile bridgeProfile) {
        boolean pautaAware = containsAny(queueCode, "PAUTA", "SESSAO", "JULGAMENTO");
        boolean acordaoAware = containsAny(queueCode, "ACORDAO", "ACÓRDAO", "PUBLICACAO", "PUBLICAÇÃO");
        boolean baixaAware = containsAny(queueCode, "BAIXA", "RETORNO", "ORIGEM");
        return Map.of(
                "divisaoCamaraTurma", "ATENDIDO",
                "admissibilidade", integrationProfile.reviewDesk() == null ? "PARCIAL" : "ATENDIDO",
                "gabineteRelator", bridgeProfile.recursalDesk() == null ? "PARCIAL" : "ATENDIDO",
                "inclusaoPauta", pautaAware ? "ATENDIDO" : "PARCIAL",
                "publicacaoPauta", integrationProfile.ackChannel() == null ? "PARCIAL" : "ATENDIDO",
                "sustentacaoOral", pautaAware ? "ATENDIDO" : "PARCIAL",
                "sessaoColegiada", pautaAware ? "ATENDIDO" : "PARCIAL",
                "lavraturaAcordao", acordaoAware ? "ATENDIDO" : "PARCIAL",
                "baixaOrigem", baixaAware ? "ATENDIDO" : "PARCIAL"
        );
    }

    private Map<String, String> eleitoralModel(boolean secondInstance,
                                               SecretariatJudicialIntegrationProfile integrationProfile,
                                               SecretariatFlowBridgeProfile bridgeProfile) {
        return Map.of(
                "fluxoTarefasExpedientes", "ATENDIDO",
                "corregedoriaEleitoral", "ATENDIDO",
                "cartorioZonaOuSecretariaJudiciaria", secondInstance ? "ATENDIDO" : "ATENDIDO",
                "pesquisasERitosEleitorais", integrationProfile.protocolDesk() == null ? "PARCIAL" : "ATENDIDO",
                "inspecaoCorregedoria", bridgeProfile.bridgeMode() == null ? "PARCIAL" : "ATENDIDO"
        );
    }

    private Map<String, String> trabalhistaModel(boolean secondInstance,
                                                 SecretariatJudicialIntegrationProfile integrationProfile,
                                                 String queueCode) {
        return Map.of(
                "fluxoTrabalhistaPadrao", "ATENDIDO",
                "custasGruJudicial", "ATENDIDO",
                "execucaoIntegrada", integrationProfile.protocolDesk() == null ? "PARCIAL" : "ATENDIDO",
                "midiasProcessuais", containsAny(queueCode, "MIDIA", "MÍDIA", "AUDIO", "VIDEO", "VÍDEO") ? "ATENDIDO" : "PARCIAL",
                "colegiadoTrabalhista", secondInstance ? "ATENDIDO" : "PARCIAL"
        );
    }

    private Map<String, String> militarModel(boolean secondInstance,
                                             SecretariatJudicialIntegrationProfile integrationProfile,
                                             SecretariatFlowBridgeProfile bridgeProfile) {
        return Map.of(
                "jmuEprocOuMilitarEstadual", "ATENDIDO",
                "distribuicaoAutomatica", integrationProfile.syncMode() == null ? "PARCIAL" : "ATENDIDO",
                "auditoriaOuColegiadoMilitar", secondInstance ? "ATENDIDO" : "PARCIAL",
                "sustentacaoRemota", "ATENDIDO",
                "fluxoEventosMilitares", bridgeProfile.bridgeMode() == null ? "PARCIAL" : "ATENDIDO"
        );
    }

    private List<String> buildGaps(String instanceClass,
                                   String branchClass,
                                   String queueCode,
                                   ForumDeskPortfolioProfile portfolio,
                                   SecretariatDeskLoadProfile deskProfile,
                                   SecretariatFlowBridgeProfile bridgeProfile,
                                   SecretariatJudicialIntegrationProfile integrationProfile) {
        ArrayList<String> gaps = new ArrayList<>();
        if (integrationProfile.proofBundleMode() == null) {
            gaps.add("separacao-explicita-entre-documento-principal-e-acessorios");
        }
        if (bridgeProfile.bridgeMode() == null || bridgeProfile.bridgeMode().isBlank()) {
            gaps.add("localizadores-dinamicos-mais-livres-por-unidade");
        }
        if (deskProfile.coordinationMode() == null || deskProfile.coordinationMode().isBlank()) {
            gaps.add("painel-unificado-multi-vara-multi-secretaria-explicito");
        }
        if (isSecondInstance(instanceClass)) {
            if (!containsAny(queueCode, "ADMISS", "PRAZO_RECURSAL", "PREPARO")) {
                gaps.add("mesa-explicita-de-admissibilidade-recursal");
            }
            if (!containsAny(queueCode, "PAUTA", "SESSAO", "JULGAMENTO")) {
                gaps.add("fila-explicita-de-pauta-publicacao-e-sessao-colegiada");
            }
            if (!containsAny(queueCode, "SUSTENTACAO", "SUSTENTAÇÃO", "ORAL")) {
                gaps.add("mesa-explicita-de-sustentacao-oral");
            }
            if (!containsAny(queueCode, "ACORDAO", "ACÓRDAO")) {
                gaps.add("fila-explicita-de-lavratura-e-publicacao-de-acordao");
            }
            if (!containsAny(queueCode, "BAIXA", "RETORNO", "ORIGEM")) {
                gaps.add("baixa-pos-colegiado-e-retorno-a-origem-explicitos");
            }
        }
        if ("ELEITORAL".equals(branchClass) && integrationProfile.targetSystem() != null && !integrationProfile.targetSystem().toUpperCase(Locale.ROOT).contains("PJE")) {
            gaps.add("alinhamento-explicito-com-modelo-eleitoral-institucional");
        }
        if ("TRABALHISTA".equals(branchClass) && !containsAny(queueCode, "MIDIA", "MÍDIA", "AUDIO", "VIDEO", "VÍDEO", "EXECUCAO", "EXECUÇÃO")) {
            gaps.add("fila-explicita-para-midias-e-execucao-trabalhista");
        }
        if ("MILITAR".equals(branchClass) && !containsAny(queueCode, "AUDITORIA", "COLEGIADO", "SESSAO", "SESSÃO")) {
            gaps.add("mesa-explicita-de-auditoria-ou-julgamento-militar");
        }
        if ("JUIZADO_ESPECIAL".equals(branchClass) && !containsAny(queueCode, "TURMA_RECURSAL", "RECURSAL")) {
            gaps.add("trilha-explicita-de-turma-recursal");
        }
        if (portfolio.gabineteDesk() == null || portfolio.gabineteDesk().isBlank()) {
            gaps.add("ponte-explicita-secretaria-gabinete");
        }
        return List.copyOf(new LinkedHashSet<>(gaps));
    }

    private List<String> buildLabels(String instanceClass, String branchClass, String queueCode, List<String> gaps) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(instanceClass);
        labels.add(branchClass);
        if (isSecondInstance(instanceClass)) {
            labels.add("COLEGIADO");
        }
        if (containsAny(queueCode, "PAUTA", "SESSAO", "SESSÃO")) {
            labels.add("PAUTA");
        }
        if (containsAny(queueCode, "ACORDAO", "ACÓRDAO")) {
            labels.add("ACORDAO");
        }
        if (containsAny(queueCode, "SUSTENTACAO", "SUSTENTAÇÃO")) {
            labels.add("SUSTENTACAO_ORAL");
        }
        if (!gaps.isEmpty()) {
            labels.add("REFERENCE_GAPS");
        }
        return List.copyOf(labels);
    }

    private List<String> queueFamilies(String instanceClass, String branchClass) {
        ArrayList<String> families = new ArrayList<>();
        families.add("RECEBIMENTO");
        families.add("SANEAMENTO");
        families.add("COMUNICACAO");
        families.add("EXPEDICAO");
        if (isSecondInstance(instanceClass)) {
            families.add("ADMISSIBILIDADE");
            families.add("GABINETE_RELATOR");
            families.add("PAUTA");
            families.add("PUBLICACAO_PAUTA");
            families.add("SUSTENTACAO_ORAL");
            families.add("SESSAO");
            families.add("ACORDAO");
            families.add("BAIXA_ORIGEM");
        } else {
            families.add("AUDIENCIA");
            families.add("CUMPRIMENTO");
        }
        if ("ELEITORAL".equals(branchClass)) {
            families.add("CORREGEDORIA_ELEITORAL");
            families.add("PESQUISAS_ELEITORAIS");
            families.add("INSPECAO");
        }
        if ("TRABALHISTA".equals(branchClass)) {
            families.add("CUSTAS_GRU");
            families.add("EXECUCAO");
            families.add("MIDIAS");
        }
        if ("MILITAR".equals(branchClass)) {
            families.add("AUDITORIA");
            families.add("SESSAO_MILITAR");
        }
        if ("JUIZADO_ESPECIAL".equals(branchClass)) {
            families.add("TURMA_RECURSAL");
        }
        return List.copyOf(families);
    }

    private Map<String, String> suggestedDesks(String instanceClass,
                                               String branchClass,
                                               ForumDeskPortfolioProfile portfolio,
                                               SecretariatFlowBridgeProfile bridgeProfile,
                                               SecretariatJudicialIntegrationProfile integrationProfile) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        out.put("triageDesk", portfolio.triageDesk());
        out.put("assistantDesk", portfolio.assistantDesk());
        out.put("gabineteDesk", portfolio.gabineteDesk());
        out.put("coordinationDesk", portfolio.coordinationDesk());
        out.put("redistributionDesk", portfolio.redistributionDesk());
        if (isSecondInstance(instanceClass)) {
            out.put("admissibilityDesk", firstNonBlank(integrationProfile.reviewDesk(), bridgeProfile.admissibilityDesk(), portfolio.assistantDesk()));
            out.put("pautaDesk", firstNonBlank(bridgeProfile.recursalDesk(), portfolio.gabineteDesk(), portfolio.coordinationDesk()));
            out.put("sustentacaoDesk", firstNonBlank(bridgeProfile.recursalDesk(), portfolio.coordinationDesk(), portfolio.assistantDesk()));
            out.put("acordaoDesk", firstNonBlank(integrationProfile.reviewDesk(), portfolio.gabineteDesk(), portfolio.coordinationDesk()));
            out.put("baixaDesk", firstNonBlank(integrationProfile.reconciliationDesk(), portfolio.redistributionDesk(), portfolio.assistantDesk()));
        }
        if ("ELEITORAL".equals(branchClass)) {
            out.put("electoralCorregedoriaDesk", firstNonBlank(integrationProfile.reviewDesk(), portfolio.coordinationDesk(), portfolio.assistantDesk()));
        }
        if ("TRABALHISTA".equals(branchClass)) {
            out.put("custasDesk", firstNonBlank(integrationProfile.protocolDesk(), portfolio.assistantDesk(), portfolio.triageDesk()));
            out.put("midiasDesk", firstNonBlank(portfolio.assistantDesk(), portfolio.coordinationDesk()));
        }
        if ("MILITAR".equals(branchClass)) {
            out.put("auditoriaDesk", firstNonBlank(portfolio.gabineteDesk(), portfolio.coordinationDesk(), portfolio.assistantDesk()));
        }
        out.values().removeIf(this::emptyValue);
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildCatalogCapabilities(String instanceClass, String branchClass) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("supportsCollegiateAgenda", isSecondInstance(instanceClass));
        out.put("supportsAdmissibilityDesk", isSecondInstance(instanceClass));
        out.put("supportsSustentacaoOralDesk", isSecondInstance(instanceClass) || "MILITAR".equals(branchClass));
        out.put("supportsElectoralCorregedoria", "ELEITORAL".equals(branchClass));
        out.put("supportsGruJudicial", "TRABALHISTA".equals(branchClass));
        out.put("supportsAuditoriaMilitar", "MILITAR".equals(branchClass));
        out.put("supportsJuizadoRecursal", "JUIZADO_ESPECIAL".equals(branchClass));
        out.put("queueFamilies", queueFamilies(instanceClass, branchClass));
        return Collections.unmodifiableMap(out);
    }

    private String resolveInstanceClass(String inboxKey,
                                        ForumDeskPortfolioProfile portfolio,
                                        SecretariatJudicialIntegrationProfile integrationProfile,
                                        String queueCode) {
        String token = join(inboxKey, portfolio.operationalDescriptor(), integrationProfile.targetSystem(), queueCode);
        if (containsAny(token, "SUPERIOR", "TRIBUNAL_SUPERIOR", "MINISTRO", "PLENARIO", "SECAO")) {
            return "INSTANCIA_SUPERIOR";
        }
        if (containsAny(token, "SEGUNDO_GRAU", "SEGUNDA_INSTANCIA", "CAMARA", "CÂMARA", "COLEGIADO", "DESEMBARGADOR", "TURMA_RECURSAL", "RECURSAL")) {
            return "SEGUNDA_INSTANCIA";
        }
        return SecretariatInboxKeyParser.parse(inboxKey)
                .map(parts -> containsAny(parts.instance(), "SECOND", "SEGUNDO", "2G", "TURMA_RECURSAL") ? "SEGUNDA_INSTANCIA" : "PRIMEIRA_INSTANCIA")
                .orElse("PRIMEIRA_INSTANCIA");
    }

    private String resolveBranchClass(String inboxKey,
                                      ForumDeskPortfolioProfile portfolio,
                                      SecretariatJudicialIntegrationProfile integrationProfile,
                                      String queueCode) {
        String token = join(inboxKey, portfolio.operationalDescriptor(), integrationProfile.targetSystem(), integrationProfile.tribunalCodigo(), queueCode);
        if (containsAny(token, "JUIZADO", "TURMA_RECURSAL", "JEC", "JEF")) {
            return "JUIZADO_ESPECIAL";
        }
        if (containsAny(token, "ELEITORAL", "TRE", "TSE", "ZONA_ELEITORAL", "PJECOR")) {
            return "ELEITORAL";
        }
        if (containsAny(token, "TRABALHISTA", "TRABALHO", "TRT", "TST", "GRU")) {
            return "TRABALHISTA";
        }
        if (containsAny(token, "MILITAR", "STM", "TJM", "AUDITORIA")) {
            return "MILITAR";
        }
        if (containsAny(token, "FEDERAL", "TRF", "JF", "SECAO_JUDICIARIA")) {
            return "FEDERAL";
        }
        return "ESTADUAL";
    }

    private boolean isSecondInstance(String instanceClass) {
        return "SEGUNDA_INSTANCIA".equals(instanceClass) || "INSTANCIA_SUPERIOR".equals(instanceClass);
    }

    private boolean isEventQueue(String queueCode) {
        return containsAny(queueCode, "EVENT", "ATO", "PUBLICACAO", "PUBLICAÇÃO", "EXPEDIENTE", "SESSAO", "SESSÃO", "ACORDAO", "ACÓRDAO");
    }

    private boolean containsAny(String source, String... options) {
        String token = normalize(source);
        if (token.isBlank()) {
            return false;
        }
        for (String option : options) {
            if (!normalize(option).isBlank() && token.contains(normalize(option))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }

    private String join(String... values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                if (!out.isEmpty()) {
                    out.append('|');
                }
                out.append(value.trim());
            }
        }
        return out.toString();
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

    private boolean emptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence sequence) {
            return sequence.toString().isBlank();
        }
        if (value instanceof List<?> list) {
            return list.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return false;
    }

    public record ReferenceModelSnapshot(String instanceClass,
                                         String branchClass,
                                         Map<String, Object> models,
                                         List<String> gaps,
                                         List<String> labels,
                                         Map<String, Object> diagnostics) {
    }

    public record ReferenceCatalogView(List<ReferenceCatalogRow> rows) {
    }

    public record ReferenceCatalogRow(String instanceClass,
                                      String branchClass,
                                      String descriptor,
                                      List<String> queueFamilies,
                                      Map<String, Object> capabilities) {
    }
}
