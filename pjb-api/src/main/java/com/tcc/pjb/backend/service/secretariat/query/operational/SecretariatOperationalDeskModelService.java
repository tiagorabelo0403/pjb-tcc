package com.tcc.pjb.backend.service.secretariat.query.operational;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatDeskLoadProfile;
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
public class SecretariatOperationalDeskModelService {

    public OperationalDeskSnapshot resolve(String inboxKey,
                                           String queueCode,
                                           SecretariatSpecializationProfile specialization,
                                           ForumDeskPortfolioProfile portfolio,
                                           SecretariatDeskLoadProfile deskProfile,
                                           SecretariatFlowBridgeProfile bridgeProfile,
                                           SecretariatJudicialIntegrationProfile integrationProfile) {
        Objects.requireNonNull(specialization, "specialization");
        Objects.requireNonNull(portfolio, "portfolio");
        Objects.requireNonNull(deskProfile, "deskProfile");
        Objects.requireNonNull(bridgeProfile, "bridgeProfile");
        Objects.requireNonNull(integrationProfile, "integrationProfile");

        String instanceClass = safeToken(specialization.secretariatInstanceClass());
        String branchClass = safeToken(specialization.secretariatBranchClass());
        String journeyMode = resolveJourneyMode(instanceClass, branchClass);
        List<OperationalDeskView> desks = buildDesks(journeyMode, branchClass, portfolio, deskProfile, bridgeProfile, integrationProfile);
        List<String> gaps = buildGaps(journeyMode, queueCode, branchClass, bridgeProfile, integrationProfile);
        String activeDeskCode = resolveActiveDeskCode(queueCode, desks);
        List<String> labels = buildLabels(journeyMode, instanceClass, branchClass, desks, gaps);
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("journeyMode", journeyMode);
        diagnostics.put("activeDeskCode", activeDeskCode);
        diagnostics.put("deskCount", desks.size());
        diagnostics.put("mandatoryDeskCount", desks.stream().filter(OperationalDeskView::mandatory).count());
        diagnostics.put("supportsCollegiateAgenda", isCollegiate(instanceClass));
        diagnostics.put("supportsVirtualSustentacao", isCollegiate(instanceClass) || branchClass.equals("MILITAR"));
        diagnostics.put("supportsElectoralCorregedoriaDesk", branchClass.equals("ELEITORAL"));
        diagnostics.put("supportsGruDesk", branchClass.equals("TRABALHISTA"));
        diagnostics.put("supportsMidiasProcessuaisDesk", branchClass.equals("TRABALHISTA"));
        diagnostics.put("supportsPlantaoDesk", branchClass.equals("MILITAR"));
        diagnostics.put("supportsBalcaoVirtualDesk", branchClass.equals("MILITAR"));
        diagnostics.put("ownerDesks", desks.stream().map(OperationalDeskView::ownerDesk).distinct().toList());
        diagnostics.put("handoffTargets", desks.stream().map(OperationalDeskView::handoffDesk).filter(this::present).distinct().toList());
        diagnostics.put("queueCode", queueCode);
        diagnostics.put("inboxKey", inboxKey);
        diagnostics.values().removeIf(this::emptyValue);
        return new OperationalDeskSnapshot(journeyMode, desks, gaps, labels, Map.copyOf(diagnostics));
    }

    public OperationalDeskCatalogView catalog() {
        List<OperationalDeskCatalogRow> rows = List.of(
                row("FIRST_INSTANCE_SECRETARIAT", "Secretaria de 1º grau com recebimento, saneamento, expedientes, audiência e cumprimento.", buildCatalogDesks("PRIMEIRA_INSTANCIA", "ESTADUAL")),
                row("TRIBUNAL_COLLEGIATE_SECRETARIAT", "Secretaria de câmara/turma com admissibilidade, pauta, sustentação oral, sessão, acórdão e baixa.", buildCatalogDesks("SEGUNDA_INSTANCIA", "ESTADUAL")),
                row("ELECTORAL_JUDICIAL_SECRETARIAT", "Secretaria eleitoral com mesa de autuação/distribuição, corregedoria eleitoral e agenda colegiada.", buildCatalogDesks("SEGUNDA_INSTANCIA", "ELEITORAL")),
                row("LABOUR_JUDICIAL_SECRETARIAT", "Secretaria trabalhista com GRU Judicial, acervo digital, central de mídias processuais e execução integrada.", buildCatalogDesks("SEGUNDA_INSTANCIA", "TRABALHISTA")),
                row("MILITARY_JUDICIAL_SECRETARIAT", "Secretaria militar com auditoria/colegiado, plantão, balcão virtual e sessão militar.", buildCatalogDesks("SEGUNDA_INSTANCIA", "MILITAR"))
        );
        return new OperationalDeskCatalogView(rows);
    }

    private OperationalDeskCatalogRow row(String journeyMode, String descriptor, List<OperationalDeskView> desks) {
        return new OperationalDeskCatalogRow(journeyMode, descriptor, desks);
    }

    private List<OperationalDeskView> buildCatalogDesks(String instanceClass, String branchClass) {
        ForumDeskPortfolioProfile portfolio = new ForumDeskPortfolioProfile(
                "TRIAGEM",
                "GABINETE",
                "AUDIENCIA",
                "CUMPRIMENTO",
                "ESCALONAMENTO",
                "ASSISTENTE",
                "COORDENACAO",
                "REDISTRIBUICAO",
                "SECRETARIA",
                List.of(),
                new LinkedHashMap<>()
        );
        SecretariatDeskLoadProfile deskProfile = new SecretariatDeskLoadProfile(
                "CATALOGO",
                "TRIAGEM",
                0,
                0,
                0,
                0,
                0,
                "BASE",
                "REDISTRIBUICAO",
                "GABINETE",
                "FLOW_STANDARD",
                false,
                false,
                false,
                List.of(),
                new LinkedHashMap<>()
        );
        SecretariatFlowBridgeProfile bridgeProfile = new SecretariatFlowBridgeProfile(
                "COLEGIADO",
                "LOCALIZADOR",
                "DISTRIBUICAO",
                "GABINETE_RELATOR",
                "PAUTA",
                "ADMISSIBILIDADE",
                true,
                true,
                true,
                List.of(),
                new LinkedHashMap<>()
        );
        SecretariatJudicialIntegrationProfile integrationProfile = new SecretariatJudicialIntegrationProfile(
                branchClass.equals("MILITAR") ? "EPROC" : "PJE",
                branchClass.equals("ELEITORAL") ? "AUTUACAO_DISTRIBUICAO" : "PROTOCOLO",
                "ASYNC",
                "CERTIFICADO",
                "PADRAO",
                "SISTEMA",
                "EXTERNO",
                "REVISAO",
                "CATALOGO",
                "CIENCIA_SISTEMA",
                "REPLAY",
                "EXPONENTIAL",
                "EVIDENCIA",
                "JANELA_24H",
                "CAT",
                "CATALOGO",
                branchClass.equals("MILITAR") ? "EPROC" : "PJE",
                branchClass,
                "https://pjb.local",
                "COLEGIADO",
                "PJB_ONLY",
                "CONTINGENCIA",
                "DLQ.REPLAY",
                "RETENCAO_PADRAO",
                branchClass.equals("MILITAR") ? "BALCAO_VIRTUAL" : "MANUAL",
                "TELEMETRIA",
                "CANAL",
                "DLQ",
                "RECONCILIACAO",
                "AUDITORIA",
                "SLA_PROTOCOLO",
                "ESCALACAO",
                "RECIBO",
                branchClass.equals("TRABALHISTA") ? "MIDIAS_E_ACERVO" : "PRINCIPAL_ACESSORIO",
                "JANELA_48H",
                true,
                true,
                List.of(),
                List.of(),
                new LinkedHashMap<>()
        );
        return buildDesks(resolveJourneyMode(instanceClass, branchClass), branchClass, portfolio, deskProfile, bridgeProfile, integrationProfile);
    }

    private String resolveJourneyMode(String instanceClass, String branchClass) {
        if (branchClass.equals("ELEITORAL")) {
            return "ELECTORAL_JUDICIAL_SECRETARIAT";
        }
        if (branchClass.equals("TRABALHISTA")) {
            return "LABOUR_JUDICIAL_SECRETARIAT";
        }
        if (branchClass.equals("MILITAR")) {
            return "MILITARY_JUDICIAL_SECRETARIAT";
        }
        if (isCollegiate(instanceClass)) {
            return "TRIBUNAL_COLLEGIATE_SECRETARIAT";
        }
        return "FIRST_INSTANCE_SECRETARIAT";
    }

    private List<OperationalDeskView> buildDesks(String journeyMode,
                                                 String branchClass,
                                                 ForumDeskPortfolioProfile portfolio,
                                                 SecretariatDeskLoadProfile deskProfile,
                                                 SecretariatFlowBridgeProfile bridgeProfile,
                                                 SecretariatJudicialIntegrationProfile integrationProfile) {
        LinkedHashMap<String, OperationalDeskView> out = new LinkedHashMap<>();
        addDesk(out, desk("MESA_RECEBIMENTO", "Mesa de recebimento", "Recebe protocolo, triagem e distribuição inicial da secretaria.", "RECEBIMENTO", portfolio.triageDesk(), portfolio.coordinationDesk(), true, true, false, "Sempre ativa no início do fluxo."));
        addDesk(out, desk("MESA_SANEAMENTO", "Mesa de saneamento", "Confere regularidade, documentos, impulso e pendências internas.", "SANEAMENTO", portfolio.triageDesk(), portfolio.assistantDesk(), true, true, false, "Ativa para verificação cartorária e regularização."));
        addDesk(out, desk("MESA_EXPEDIENTES", "Mesa de expedientes", "Controla comunicações, intimações, citações e atos seriados.", "EXPEDIENTES", portfolio.coordinationDesk(), portfolio.executionDesk(), true, true, false, "Ativa quando há comunicações formais e atos cartorários."));
        addDesk(out, desk("MESA_ATOS_JUNTADAS", "Mesa de atos e juntadas", "Organiza juntadas, minutas e documentos acessórios.", "ATOS_JUNTADAS", portfolio.assistantDesk(), portfolio.coordinationDesk(), true, true, false, "Ativa para documentos avulsos, minutas e organização do acervo."));
        if (journeyMode.equals("FIRST_INSTANCE_SECRETARIAT")) {
            addDesk(out, desk("MESA_AUDIENCIA", "Mesa de audiência", "Prepara pauta, recursos e presença para audiência da unidade.", "AUDIENCIA", portfolio.hearingDesk(), portfolio.coordinationDesk(), true, false, true, "Ativa quando houver pauta de audiência ou sessão conciliatória."));
            addDesk(out, desk("MESA_CUMPRIMENTO", "Mesa de cumprimento", "Executa cumprimento, baixa operacional e redistribuição interna.", "CUMPRIMENTO", portfolio.executionDesk(), deskProfile.redistributionDesk(), true, true, false, "Ativa após ato jurisdicional ou mandado de cumprimento."));
        }
        if (journeyMode.equals("TRIBUNAL_COLLEGIATE_SECRETARIAT")
                || journeyMode.equals("ELECTORAL_JUDICIAL_SECRETARIAT")
                || journeyMode.equals("LABOUR_JUDICIAL_SECRETARIAT")
                || journeyMode.equals("MILITARY_JUDICIAL_SECRETARIAT")) {
            String colegiadoOwner = firstNonBlank(bridgeProfile.recursalDesk(), portfolio.coordinationDesk(), deskProfile.gabineteSupportDesk(), portfolio.gabineteDesk());
            addDesk(out, desk("MESA_ADMISSIBILIDADE", "Mesa de admissibilidade", "Verifica preparo, tempestividade, admissibilidade e regularidade recursal.", "ADMISSIBILIDADE", colegiadoOwner, "MESA_GABINETE_RELATOR", true, true, false, "Ativa na entrada recursal e antes da conclusão ao relator."));
            addDesk(out, desk("MESA_GABINETE_RELATOR", "Mesa gabinete-relator", "Organiza conclusão, handoff e pendências do gabinete do relator.", "GABINETE_RELATOR", firstNonBlank(deskProfile.gabineteSupportDesk(), portfolio.gabineteDesk(), colegiadoOwner), "MESA_PAUTA_COLEGIADA", true, false, false, "Ativa para relatoria, revisão, pedido de vista e saneamento colegiado."));
            addDesk(out, desk("MESA_PAUTA_COLEGIADA", "Mesa de pauta colegiada", "Consolida inclusão em pauta e agenda do órgão julgador.", "PAUTA", colegiadoOwner, "MESA_PUBLICACAO_PAUTA", true, true, true, "Ativa para inclusão em pauta, reserva de sessão e preparação colegiada."));
            addDesk(out, desk("MESA_PUBLICACAO_PAUTA", "Mesa de publicação de pauta", "Publica edital de pauta e sincroniza ciência das partes.", "PUBLICACAO_PAUTA", colegiadoOwner, "MESA_SUSTENTACAO_ORAL", true, true, false, "Ativa após formação da pauta e antes da sessão."));
            addDesk(out, desk("MESA_SUSTENTACAO_ORAL", "Mesa de sustentação oral", "Recebe inscrições, mídias, preferências e valida sustentação oral.", "SUSTENTACAO_ORAL", colegiadoOwner, "MESA_SESSAO", true, false, true, "Ativa quando houver sessão com sustentação oral presencial ou remota."));
            addDesk(out, desk("MESA_SESSAO", "Mesa de sessão", "Registra resultado do julgamento colegiado e eventos de sessão.", "SESSAO", colegiadoOwner, "MESA_ACORDAO_PUBLICACAO", true, false, true, "Ativa na realização de sessão presencial, híbrida ou virtual."));
            addDesk(out, desk("MESA_ACORDAO_PUBLICACAO", "Mesa de acórdão e publicação", "Controla lavratura, publicação e disponibilização do acórdão.", "ACORDAO", colegiadoOwner, "MESA_BAIXA_ORIGEM", true, true, false, "Ativa após julgamento para acórdão, publicação e prazos subsequentes."));
            addDesk(out, desk("MESA_BAIXA_ORIGEM", "Mesa de baixa e retorno à origem", "Envia os autos à origem ou ao destino final após fase recursal.", "BAIXA_ORIGEM", colegiadoOwner, portfolio.redistributionDesk(), true, true, false, "Ativa no encerramento do fluxo colegiado e retorno à origem."));
        }
        if (journeyMode.equals("ELECTORAL_JUDICIAL_SECRETARIAT")) {
            String protocolOwner = firstNonBlank(integrationProfile.protocolDesk(), portfolio.coordinationDesk(), portfolio.triageDesk());
            addDesk(out, desk("MESA_AUTUACAO_DISTRIBUICAO_ELEITORAL", "Mesa de autuação e distribuição eleitoral", "Organiza autuação, distribuição e informações processuais eleitorais.", "AUTUACAO_DISTRIBUICAO", protocolOwner, "MESA_ADMISSIBILIDADE", true, true, false, "Ativa para feitos eleitorais originários e redistribuições internas."));
            addDesk(out, desk("MESA_CORREGEDORIA_ELEITORAL", "Mesa de corregedoria eleitoral", "Trata fluxos correicionais, disciplinares e inspeções da Justiça Eleitoral no PJB.", "CORREGEDORIA_ELEITORAL", protocolOwner, "MESA_EXPEDIENTES", true, true, false, "Ativa para procedimentos correicionais e demandas de corregedoria eleitoral."));
            addDesk(out, desk("MESA_PESQUISAS_ELEITORAIS", "Mesa de pesquisas e registros eleitorais", "Coordena registros obrigatórios e demandas típicas do calendário eleitoral.", "PESQUISAS_ELEITORAIS", protocolOwner, "MESA_EXPEDIENTES", false, true, false, "Ativa em ciclos eleitorais e demandas partidárias específicas."));
            addDesk(out, desk("MESA_INSPECAO_CORREGEDORIA", "Mesa de inspeção da corregedoria", "Acompanha inspeções, relatórios e saneamento correicional.", "INSPECAO_CORREGEDORIA", protocolOwner, "MESA_CORREGEDORIA_ELEITORAL", false, true, false, "Ativa para ciclos de inspeção e acompanhamento da corregedoria eleitoral."));
        }
        if (journeyMode.equals("LABOUR_JUDICIAL_SECRETARIAT")) {
            String labourOwner = firstNonBlank(integrationProfile.protocolDesk(), portfolio.coordinationDesk(), portfolio.executionDesk());
            addDesk(out, desk("MESA_CUSTAS_GRU", "Mesa de custas e GRU Judicial", "Confere custas, recolhimentos e integração financeira do fluxo trabalhista.", "GRU_CUSTAS", labourOwner, "MESA_ADMISSIBILIDADE", true, true, false, "Ativa quando houver preparo, custas ou recolhimento via GRU Judicial."));
            addDesk(out, desk("MESA_ACERVO_DIGITAL", "Mesa de acervo digital", "Organiza anexos audiovisuais e evidências digitais do processo trabalhista.", "ACERVO_DIGITAL", labourOwner, "MESA_MIDIAS_PROCESSUAIS", true, true, true, "Ativa para anexos audiovisuais, arquivos grandes e acervo digital."));
            addDesk(out, desk("MESA_MIDIAS_PROCESSUAIS", "Mesa de mídias processuais", "Gerencia mídias processuais, gravações e sustentação com suporte audiovisual dentro do PJB.", "MIDIAS_PROCESSUAIS", labourOwner, "MESA_SESSAO", true, false, true, "Ativa para audiência, sessão ou prova apoiada por mídia."));
            addDesk(out, desk("MESA_EXECUCAO_TRABALHISTA", "Mesa de execução trabalhista", "Orquestra cumprimento patrimonial e execução integrada da Justiça do Trabalho.", "EXECUCAO_TRABALHISTA", portfolio.executionDesk(), portfolio.coordinationDesk(), true, true, false, "Ativa após liquidação, cumprimento ou atos executivos."));
        }
        if (journeyMode.equals("MILITARY_JUDICIAL_SECRETARIAT")) {
            String militaryOwner = firstNonBlank(integrationProfile.protocolDesk(), portfolio.coordinationDesk(), portfolio.executionDesk());
            addDesk(out, desk("MESA_AUDITORIA_MILITAR", "Mesa de auditoria militar", "Coordena auditoria, conselhos e particularidades do rito militar.", "AUDITORIA_MILITAR", militaryOwner, "MESA_GABINETE_RELATOR", true, true, false, "Ativa para instrução e preparação militar especializada."));
            addDesk(out, desk("MESA_PLANTAO_MILITAR", "Mesa de plantão militar", "Recebe urgências e atos do plantão judiciário militar.", "PLANTAO_MILITAR", firstNonBlank(integrationProfile.manualSubmissionDesk(), militaryOwner), "MESA_EXPEDIENTES", true, true, true, "Ativa em urgência, custódia ou medidas sensíveis do plantão."));
            addDesk(out, desk("MESA_BALCAO_VIRTUAL_MILITAR", "Mesa de balcão virtual militar", "Centraliza atendimento institucional, videoconferência e canal de suporte.", "BALCAO_VIRTUAL", firstNonBlank(integrationProfile.manualSubmissionDesk(), militaryOwner), "MESA_EXPEDIENTES", true, false, true, "Ativa para atendimento e interface digital institucional."));
            addDesk(out, desk("MESA_SESSAO_MILITAR", "Mesa de sessão militar", "Acompanha sessão militar, videoconferência e registro colegiado especializado.", "SESSAO_MILITAR", militaryOwner, "MESA_ACORDAO_PUBLICACAO", true, false, true, "Ativa para sessões militares presenciais, híbridas ou remotas."));
        }
        return List.copyOf(out.values());
    }

    private void addDesk(Map<String, OperationalDeskView> out, OperationalDeskView desk) {
        out.putIfAbsent(desk.deskCode(), desk);
    }

    private OperationalDeskView desk(String deskCode,
                                     String displayName,
                                     String purpose,
                                     String primaryQueueFamily,
                                     String ownerDesk,
                                     String handoffDesk,
                                     boolean mandatory,
                                     boolean batchCapable,
                                     boolean virtualCapable,
                                     String activationRule) {
        return new OperationalDeskView(
                deskCode,
                displayName,
                purpose,
                primaryQueueFamily,
                ownerDesk,
                handoffDesk,
                mandatory,
                batchCapable,
                virtualCapable,
                activationRule
        );
    }

    private List<String> buildGaps(String journeyMode,
                                   String queueCode,
                                   String branchClass,
                                   SecretariatFlowBridgeProfile bridgeProfile,
                                   SecretariatJudicialIntegrationProfile integrationProfile) {
        List<String> gaps = new ArrayList<>();
        String queue = safeToken(queueCode);
        String protocolDesk = safeToken(integrationProfile.protocolDesk());
        String targetSystem = safeToken(integrationProfile.targetSystem());
        String connectorSystem = safeToken(integrationProfile.connectorSystem());
        String proofBundleMode = safeToken(integrationProfile.proofBundleMode());
        String manualDesk = safeToken(integrationProfile.manualSubmissionDesk());
        String ackChannel = safeToken(integrationProfile.ackChannel());
        String bridgeMode = safeToken(bridgeProfile.bridgeMode());
        if (!journeyMode.equals("FIRST_INSTANCE_SECRETARIAT") && !containsAny(queue, "ADMISS", "PAUTA", "SESSAO", "ACORDAO", "BAIXA")) {
            gaps.add("mesas-colegiadas-ainda-nao-invocadas-no-contexto-atual");
        }
        if (!journeyMode.equals("FIRST_INSTANCE_SECRETARIAT") && !containsAny(ackChannel, "DIARIO", "CIENCIA", "SISTEMA", "ELETRONICA")) {
            gaps.add("publicacao-e-ciencia-colegiada-ainda-sem-canal-explicito");
        }
        if (!journeyMode.equals("FIRST_INSTANCE_SECRETARIAT") && !containsAny(bridgeMode, "COLEGIADO", "LOCALIZADOR", "FILA", "GABINETE")) {
            gaps.add("handoff-entre-secretaria-e-gabinete-ainda-nao-explicito");
        }
        if (branchClass.equals("ELEITORAL") && !containsAny(protocolDesk, "AUTUACAO", "DISTRIBUICAO", "PROTOCOLO")) {
            gaps.add("mesa-eleitoral-de-autuacao-distribuicao-ainda-sem-ancora-explicita");
        }
        if (branchClass.equals("ELEITORAL") && !containsAny(targetSystem, "PJECOR", "PJE", "CORREGEDORIA")) {
            gaps.add("mesa-de-corregedoria-eleitoral-ainda-sem-alvo-explicito");
        }
        if (branchClass.equals("TRABALHISTA") && !containsAny(queue, "GRU", "CUSTAS", "MIDIAS", "ACERVO", "EXECUCAO")) {
            gaps.add("mesas-trabalhistas-ainda-nao-invocadas-no-contexto-atual");
        }
        if (branchClass.equals("TRABALHISTA") && !containsAny(proofBundleMode, "MIDIA", "ACERVO", "AUDIO", "VIDEO")) {
            gaps.add("midias-processuais-e-acervo-digital-ainda-sem-modo-explicito");
        }
        if (branchClass.equals("MILITAR") && !containsAny(connectorSystem, "EPROC")) {
            gaps.add("mesa-militar-ainda-sem-conector-eproc-explicito");
        }
        if (branchClass.equals("MILITAR") && !containsAny(manualDesk, "BALCAO", "PLANTAO", "ATENDIMENTO", "MANUAL")) {
            gaps.add("plantao-ou-balcao-virtual-militar-ainda-sem-canal-explicito");
        }
        return List.copyOf(gaps);
    }

    private String resolveActiveDeskCode(String queueCode, List<OperationalDeskView> desks) {
        String queue = safeToken(queueCode);
        if (queue.isBlank() || queue.equals("BASE")) {
            return desks.isEmpty() ? null : desks.getFirst().deskCode();
        }
        for (OperationalDeskView desk : desks) {
            if (containsAny(queue, desk.primaryQueueFamily(), desk.deskCode())) {
                return desk.deskCode();
            }
        }
        if (containsAny(queue, "JULGAMENTO", "PAUTA")) {
            return "MESA_PAUTA_COLEGIADA";
        }
        if (containsAny(queue, "PUBLICACAO")) {
            return "MESA_PUBLICACAO_PAUTA";
        }
        if (containsAny(queue, "SUSTENT")) {
            return "MESA_SUSTENTACAO_ORAL";
        }
        return null;
    }

    private List<String> buildLabels(String journeyMode,
                                     String instanceClass,
                                     String branchClass,
                                     List<OperationalDeskView> desks,
                                     List<String> gaps) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(journeyMode);
        out.add(instanceClass);
        out.add(branchClass);
        if (isCollegiate(instanceClass)) {
            out.add("COLEGIADO");
        }
        if (desks.stream().anyMatch(OperationalDeskView::virtualCapable)) {
            out.add("SUPORTE_VIRTUAL");
        }
        if (gaps.isEmpty()) {
            out.add("MESAS_OPERACIONAIS_NATIVAS");
        } else {
            out.add("MESAS_OPERACIONAIS_COM_GAPS");
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
            if (present(token) && normalized.contains(safeToken(token))) {
                return true;
            }
        }
        return false;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank() && !safeToken(value).equals("BASE");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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

    public record OperationalDeskSnapshot(
            String journeyMode,
            List<OperationalDeskView> desks,
            List<String> gaps,
            List<String> labels,
            Map<String, Object> diagnostics
    ) {
    }

    public record OperationalDeskCatalogView(List<OperationalDeskCatalogRow> rows) {
    }

    public record OperationalDeskCatalogRow(
            String journeyMode,
            String descriptor,
            List<OperationalDeskView> desks
    ) {
    }

    public record OperationalDeskView(
            String deskCode,
            String displayName,
            String purpose,
            String primaryQueueFamily,
            String ownerDesk,
            String handoffDesk,
            boolean mandatory,
            boolean batchCapable,
            boolean virtualCapable,
            String activationRule
    ) {
    }
}
