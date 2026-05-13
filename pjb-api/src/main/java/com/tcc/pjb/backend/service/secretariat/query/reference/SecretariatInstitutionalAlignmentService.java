package com.tcc.pjb.backend.service.secretariat.query.reference;

import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatFlowBridgeProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatJudicialIntegrationProfile;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver.SecretariatSpecializationProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SecretariatInstitutionalAlignmentService {

    public InstitutionalAlignmentSnapshot resolve(String inboxKey,
                                                 String queueCode,
                                                 SecretariatSpecializationProfile specialization,
                                                 SecretariatFlowBridgeProfile bridgeProfile,
                                                 SecretariatJudicialIntegrationProfile integrationProfile) {
        Objects.requireNonNull(specialization, "specialization");
        Objects.requireNonNull(bridgeProfile, "bridgeProfile");
        Objects.requireNonNull(integrationProfile, "integrationProfile");

        String instanceClass = safeToken(specialization.secretariatInstanceClass());
        String branchClass = safeToken(specialization.secretariatBranchClass());
        String axis = resolveInstitutionalAxis(instanceClass, branchClass);
        List<String> cells = buildInstitutionalCells(axis, instanceClass, branchClass);
        List<String> touchpoints = buildInstitutionalTouchpoints(axis, instanceClass, branchClass);
        List<String> gaps = buildGaps(axis, queueCode, specialization, bridgeProfile, integrationProfile);
        List<String> labels = buildLabels(axis, instanceClass, branchClass, gaps);
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("institutionalAxis", axis);
        diagnostics.put("institutionalCells", cells);
        diagnostics.put("touchpoints", touchpoints);
        diagnostics.put("supportsCartorioEleitoral", branchClass.equals("ELEITORAL"));
        diagnostics.put("supportsElectoralCorregedoria", branchClass.equals("ELEITORAL"));
        diagnostics.put("supportsMidiasProcessuais", branchClass.equals("TRABALHISTA"));
        diagnostics.put("supportsGruJudicial", branchClass.equals("TRABALHISTA"));
        diagnostics.put("supportsPlantaoBalcaoVirtual", branchClass.equals("MILITAR"));
        diagnostics.put("supportsSecretariaTurmaCamara", isCollegiate(instanceClass));
        diagnostics.put("supportsMesaAcordao", isCollegiate(instanceClass));
        diagnostics.put("supportsMesaSustentacaoOral", isCollegiate(instanceClass) || branchClass.equals("MILITAR"));
        diagnostics.put("specializedSecretariatCode", specialization.specializedSecretariatCode());
        diagnostics.put("specializedSecretariatName", specialization.specializedSecretariatName());
        diagnostics.put("connectorSystem", integrationProfile.connectorSystem());
        diagnostics.put("targetSystem", integrationProfile.targetSystem());
        diagnostics.put("manualSubmissionDesk", integrationProfile.manualSubmissionDesk());
        diagnostics.put("protocolDesk", integrationProfile.protocolDesk());
        diagnostics.values().removeIf(this::emptyValue);
        return new InstitutionalAlignmentSnapshot(axis, cells, touchpoints, gaps, labels, Map.copyOf(diagnostics));
    }

    public InstitutionalCatalogView catalog() {
        List<InstitutionalCatalogRow> rows = List.of(
                row("PRIMEIRA_INSTANCIA_COMUM", "Primeira instância comum com secretaria da unidade, gabinete e centrais de comunicação", buildInstitutionalCells("PRIMEIRA_INSTANCIA_COMUM", "PRIMEIRA_INSTANCIA", "ESTADUAL"), buildInstitutionalTouchpoints("PRIMEIRA_INSTANCIA_COMUM", "PRIMEIRA_INSTANCIA", "ESTADUAL")),
                row("TRIBUNAL_COLEGIADO", "Secretaria de câmara/turma/seção com relatoria, pauta, sessão, acórdão e baixa", buildInstitutionalCells("TRIBUNAL_COLEGIADO", "SEGUNDA_INSTANCIA", "ESTADUAL"), buildInstitutionalTouchpoints("TRIBUNAL_COLEGIADO", "SEGUNDA_INSTANCIA", "ESTADUAL")),
                row("ELEITORAL_JUDICIARIO", "Secretaria judiciária eleitoral com cartório/2º grau, autuação-distribuição, corregedoria eleitoral e malha PJB eleitoral", buildInstitutionalCells("ELEITORAL_JUDICIARIO", "SEGUNDA_INSTANCIA", "ELEITORAL"), buildInstitutionalTouchpoints("ELEITORAL_JUDICIARIO", "SEGUNDA_INSTANCIA", "ELEITORAL")),
                row("TRABALHISTA_JUDICIARIO", "Secretaria trabalhista com PJB, pauta, GRU Judicial, acervo digital e central de mídias processuais", buildInstitutionalCells("TRABALHISTA_JUDICIARIO", "SEGUNDA_INSTANCIA", "TRABALHISTA"), buildInstitutionalTouchpoints("TRABALHISTA_JUDICIARIO", "SEGUNDA_INSTANCIA", "TRABALHISTA")),
                row("MILITAR_JUDICIARIO", "Secretaria militar com auditoria/colegiado, eproc, plantão e balcão virtual", buildInstitutionalCells("MILITAR_JUDICIARIO", "SEGUNDA_INSTANCIA", "MILITAR"), buildInstitutionalTouchpoints("MILITAR_JUDICIARIO", "SEGUNDA_INSTANCIA", "MILITAR"))
        );
        return new InstitutionalCatalogView(rows);
    }

    private InstitutionalCatalogRow row(String axis, String descriptor, List<String> cells, List<String> touchpoints) {
        return new InstitutionalCatalogRow(axis, descriptor, cells, touchpoints);
    }

    private String resolveInstitutionalAxis(String instanceClass, String branchClass) {
        if (branchClass.equals("ELEITORAL")) {
            return "ELEITORAL_JUDICIARIO";
        }
        if (branchClass.equals("TRABALHISTA")) {
            return "TRABALHISTA_JUDICIARIO";
        }
        if (branchClass.equals("MILITAR")) {
            return "MILITAR_JUDICIARIO";
        }
        if (isCollegiate(instanceClass)) {
            return "TRIBUNAL_COLEGIADO";
        }
        return "PRIMEIRA_INSTANCIA_COMUM";
    }

    private List<String> buildInstitutionalCells(String axis, String instanceClass, String branchClass) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("SECRETARIA_UNIDADE");
        out.add("EXPEDIENTES_E_COMUNICACOES");
        out.add("ATOS_E_JUNTADAS");
        if (isCollegiate(instanceClass)) {
            out.add("SECRETARIA_CAMARA_TURMA");
            out.add("GABINETE_RELATOR");
            out.add("GABINETE_REVISOR_OU_VISTA");
            out.add("MESA_PAUTA");
            out.add("MESA_SESSAO");
            out.add("MESA_ACORDAO");
            out.add("BAIXA_ORIGEM");
        }
        switch (axis) {
            case "ELEITORAL_JUDICIARIO" -> {
                out.add("CARTORIO_ELEITORAL");
                out.add("AUTUACAO_DISTRIBUICAO_INFORMACOES_PROCESSUAIS");
                out.add("SUPORTE_MALHA_ELEITORAL_PJB");
                out.add("CORREGEDORIA_ELEITORAL_PJB");
                out.add("DADOS_PARTIDARIOS_PRESTACAO_CONTAS");
            }
            case "TRABALHISTA_JUDICIARIO" -> {
                out.add("PAUTA_AUDIENCIAS_TRABALHISTAS");
                out.add("GRU_JUDICIAL_CUSTAS");
                out.add("ACERVO_DIGITAL");
                out.add("MIDIAS_PROCESSUAIS_PJB");
                out.add("PROCURADORIAS_E_CREDENCIAMENTO");
                out.add("EXECUCAO_INTEGRADA");
            }
            case "MILITAR_JUDICIARIO" -> {
                out.add("AUDITORIA_MILITAR");
                out.add("EPROC_JMU");
                out.add("PLANTAO_JUDICIARIO");
                out.add("BALCAO_VIRTUAL");
                out.add("DISTRIBUICAO_AUTOMATICA");
            }
            default -> {
            }
        }
        if (branchClass.equals("JUIZADO_ESPECIAL")) {
            out.add("TURMA_RECURSAL");
        }
        return List.copyOf(out);
    }

    private List<String> buildInstitutionalTouchpoints(String axis, String instanceClass, String branchClass) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("ADVOCACIA_E_PARTES");
        out.add("MINISTERIO_PUBLICO");
        out.add("DEFENSORIA_PUBLICA");
        out.add("PROCURADORIAS");
        out.add("CENTRAL_MANDADOS");
        if (isCollegiate(instanceClass)) {
            out.add("DESPACHO_RELATOR");
            out.add("EDITAL_PAUTA");
            out.add("SUSTENTACAO_ORAL");
            out.add("PUBLICACAO_ACORDAO");
        }
        if (axis.equals("ELEITORAL_JUDICIARIO")) {
            out.add("CORREGEDORIA_ELEITORAL");
            out.add("CARTORIOS_ELEITORAIS");
        }
        if (axis.equals("TRABALHISTA_JUDICIARIO")) {
            out.add("MIDIAS_PROCESSUAIS_INSTITUCIONAIS");
            out.add("GRU_JUDICIAL");
        }
        if (axis.equals("MILITAR_JUDICIARIO")) {
            out.add("BALCAO_VIRTUAL_CNJ");
            out.add("PLANTAO_JMU");
        }
        if (branchClass.equals("JUIZADO_ESPECIAL")) {
            out.add("TURMA_RECURSAL");
        }
        return List.copyOf(out);
    }

    private List<String> buildGaps(String axis,
                                   String queueCode,
                                   SecretariatSpecializationProfile specialization,
                                   SecretariatFlowBridgeProfile bridgeProfile,
                                   SecretariatJudicialIntegrationProfile integrationProfile) {
        List<String> gaps = new ArrayList<>();
        String queue = safeToken(queueCode);
        String connectorSystem = safeToken(integrationProfile.connectorSystem());
        String targetSystem = safeToken(integrationProfile.targetSystem());
        String proofBundleMode = safeToken(integrationProfile.proofBundleMode());
        String ackChannel = safeToken(integrationProfile.ackChannel());
        String protocolDesk = safeToken(integrationProfile.protocolDesk());
        String manualDesk = safeToken(integrationProfile.manualSubmissionDesk());
        String syncMode = safeToken(integrationProfile.syncMode());
        String bridgeMode = safeToken(bridgeProfile.bridgeMode());

        if (isCollegiate(specialization.secretariatInstanceClass()) && !containsAny(queue, "PAUTA", "SESSAO", "JULGAMENTO")) {
            gaps.add("mesa-de-pauta-e-sessao-colegiada-ainda-nao-explicita");
        }
        if (isCollegiate(specialization.secretariatInstanceClass()) && !containsAny(queue, "ACORDAO", "PUBLICACAO")) {
            gaps.add("mesa-de-acordao-e-publicacao-colegiada-ainda-nao-explicita");
        }
        if (axis.equals("TRIBUNAL_COLEGIADO") && !containsAny(queue, "BAIXA", "RETORNO", "ORIGEM")) {
            gaps.add("baixa-automatica-para-origem-ainda-nao-explicita");
        }
        if (axis.equals("ELEITORAL_JUDICIARIO") && !containsAny(connectorSystem, "PJE", "PJE_TRE")) {
            gaps.add("alinhamento-eleitoral-institucional-ainda-nao-explicito");
        }
        if (axis.equals("ELEITORAL_JUDICIARIO") && !containsAny(targetSystem, "PJECOR", "PJECOR", "PJE")) {
            gaps.add("trilha-de-corregedoria-eleitoral-ainda-nao-explicita");
        }
        if (axis.equals("ELEITORAL_JUDICIARIO") && !containsAny(protocolDesk, "AUTUACAO", "DISTRIBUICAO", "SADIP", "PROTOCOLO")) {
            gaps.add("mesa-de-autuacao-distribuicao-eleitoral-ainda-nao-explicita");
        }
        if (axis.equals("TRABALHISTA_JUDICIARIO") && !containsAny(proofBundleMode, "MIDIA", "ACERVO", "AUDIO", "VIDEO")) {
            gaps.add("acervo-digital-e-midias-processuais-ainda-nao-explicitos");
        }
        if (axis.equals("TRABALHISTA_JUDICIARIO") && !containsAny(queue, "GRU", "CUSTAS", "DEPOSITO", "EXECUCAO")) {
            gaps.add("custas-gru-judicial-e-execucao-trabalhista-ainda-nao-explicitas");
        }
        if (axis.equals("MILITAR_JUDICIARIO") && !containsAny(connectorSystem, "EPROC")) {
            gaps.add("eixo-eproc-da-justica-militar-ainda-nao-explicito");
        }
        if (axis.equals("MILITAR_JUDICIARIO") && !containsAny(manualDesk, "BALCAO", "PLANTAO", "ATENDIMENTO", "MANUAL")) {
            gaps.add("balcao-virtual-ou-plantao-militar-ainda-nao-explicito");
        }
        if (!containsAny(ackChannel, "CIENCIA", "SISTEMA", "DIARIO", "ELETRONICA")) {
            gaps.add("canal-formal-de-ciencia-e-comunicacao-ainda-nao-explicito");
        }
        if (!containsAny(syncMode, "ASYNC", "SISTEMA", "INTEGRADO")) {
            gaps.add("modo-de-integracao-institucional-ainda-nao-explicito");
        }
        if (!containsAny(bridgeMode, "LOCALIZADOR", "FILA", "COLEGIADO", "GABINETE")) {
            gaps.add("ponte-institucional-entre-secretaria-e-gabinete-ainda-nao-explicita");
        }
        return List.copyOf(gaps);
    }

    private List<String> buildLabels(String axis, String instanceClass, String branchClass, List<String> gaps) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(axis);
        out.add(instanceClass);
        out.add(branchClass);
        if (isCollegiate(instanceClass)) {
            out.add("COLEGIADO");
        }
        if (gaps.isEmpty()) {
            out.add("ALINHADO_INSTITUCIONALMENTE");
        } else {
            out.add("COM_GAPS_INSTITUCIONAIS");
        }
        return List.copyOf(out);
    }

    private boolean isCollegiate(String instanceClass) {
        String token = safeToken(instanceClass);
        return token.contains("SEGUNDA") || token.contains("SUPERIOR");
    }

    private boolean containsAny(String value, String... tokens) {
        String normalized = safeToken(value);
        if (tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && normalized.contains(safeToken(token))) {
                return true;
            }
        }
        return false;
    }

    private String safeToken(String value) {
        if (value == null || value.isBlank()) {
            return "BASE";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
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
        return false;
    }

    public record InstitutionalAlignmentSnapshot(
            String institutionalAxis,
            List<String> cells,
            List<String> touchpoints,
            List<String> gaps,
            List<String> labels,
            Map<String, Object> diagnostics
    ) {
    }

    public record InstitutionalCatalogView(List<InstitutionalCatalogRow> rows) {
    }

    public record InstitutionalCatalogRow(
            String institutionalAxis,
            String descriptor,
            List<String> cells,
            List<String> touchpoints
    ) {
    }
}
