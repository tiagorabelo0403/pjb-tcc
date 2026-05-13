package com.tcc.pjb.backend.service.secretariat.query.operational;

import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalDeskModelService.OperationalDeskSnapshot;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalDeskModelService.OperationalDeskView;
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
public class SecretariatOperationalActionModelService {

    public OperationalActionSnapshot resolve(String inboxKey,
                                             String queueCode,
                                             SecretariatSpecializationProfile specialization,
                                             OperationalDeskSnapshot deskSnapshot,
                                             SecretariatJudicialIntegrationProfile integrationProfile) {
        Objects.requireNonNull(specialization, "specialization");
        Objects.requireNonNull(deskSnapshot, "deskSnapshot");
        Objects.requireNonNull(integrationProfile, "integrationProfile");

        String journeyMode = safeToken(deskSnapshot.journeyMode());
        String branchClass = safeToken(specialization.secretariatBranchClass());
        List<OperationalDeskActionView> actions = buildActions(deskSnapshot.desks());
        List<String> gaps = buildGaps(journeyMode, queueCode, branchClass, deskSnapshot.desks(), actions, integrationProfile);
        List<String> labels = buildLabels(journeyMode, branchClass, actions, gaps);
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("journeyMode", deskSnapshot.journeyMode());
        diagnostics.put("actionCount", actions.size());
        diagnostics.put("desksWithActions", actions.stream().map(OperationalDeskActionView::deskCode).distinct().count());
        diagnostics.put("batchCapableActions", actions.stream().filter(OperationalDeskActionView::batchCapable).count());
        diagnostics.put("evidenceRequiredActions", actions.stream().filter(OperationalDeskActionView::evidenceRequired).count());
        diagnostics.put("supportsElectoralCorregedoriaActions", branchClass.equals("ELEITORAL"));
        diagnostics.put("supportsMidiasProcessuaisActions", branchClass.equals("TRABALHISTA"));
        diagnostics.put("supportsPlantaoMilitarActions", branchClass.equals("MILITAR"));
        diagnostics.put("supportsCollegiatePublicationActions", journeyMode.contains("TRIBUNAL") || journeyMode.contains("ELECTORAL") || journeyMode.contains("LABOUR") || journeyMode.contains("MILITARY"));
        diagnostics.put("queueCode", queueCode);
        diagnostics.put("inboxKey", inboxKey);
        diagnostics.values().removeIf(this::emptyValue);
        return new OperationalActionSnapshot(deskSnapshot.journeyMode(), actions, gaps, labels, Map.copyOf(diagnostics));
    }

    public OperationalActionCatalogView catalog() {
        return new OperationalActionCatalogView(List.of(
                row("FIRST_INSTANCE_SECRETARIAT", "Ações operacionais da secretaria de 1º grau", buildActions(buildCatalogDesks("FIRST_INSTANCE_SECRETARIAT"))),
                row("TRIBUNAL_COLLEGIATE_SECRETARIAT", "Ações operacionais da secretaria colegiada de tribunal", buildActions(buildCatalogDesks("TRIBUNAL_COLLEGIATE_SECRETARIAT"))),
                row("ELECTORAL_JUDICIAL_SECRETARIAT", "Ações operacionais da secretaria eleitoral do PJB", buildActions(buildCatalogDesks("ELECTORAL_JUDICIAL_SECRETARIAT"))),
                row("LABOUR_JUDICIAL_SECRETARIAT", "Ações operacionais da secretaria trabalhista do PJB", buildActions(buildCatalogDesks("LABOUR_JUDICIAL_SECRETARIAT"))),
                row("MILITARY_JUDICIAL_SECRETARIAT", "Ações operacionais da secretaria militar do PJB", buildActions(buildCatalogDesks("MILITARY_JUDICIAL_SECRETARIAT")))
        ));
    }

    private OperationalActionCatalogRow row(String journeyMode, String descriptor, List<OperationalDeskActionView> actions) {
        return new OperationalActionCatalogRow(journeyMode, descriptor, actions);
    }

    private List<OperationalDeskView> buildCatalogDesks(String journeyMode) {
        List<OperationalDeskView> desks = new ArrayList<>();
        desks.add(desk("MESA_RECEBIMENTO", true, true, false));
        desks.add(desk("MESA_SANEAMENTO", true, true, false));
        desks.add(desk("MESA_EXPEDIENTES", true, true, false));
        desks.add(desk("MESA_ATOS_JUNTADAS", true, true, false));
        if (journeyMode.equals("FIRST_INSTANCE_SECRETARIAT")) {
            desks.add(desk("MESA_AUDIENCIA", true, false, true));
            desks.add(desk("MESA_CUMPRIMENTO", true, true, false));
        }
        if (!journeyMode.equals("FIRST_INSTANCE_SECRETARIAT")) {
            desks.add(desk("MESA_ADMISSIBILIDADE", true, true, false));
            desks.add(desk("MESA_GABINETE_RELATOR", true, false, false));
            desks.add(desk("MESA_PAUTA_COLEGIADA", true, true, true));
            desks.add(desk("MESA_PUBLICACAO_PAUTA", true, true, false));
            desks.add(desk("MESA_SUSTENTACAO_ORAL", true, false, true));
            desks.add(desk("MESA_SESSAO", true, false, true));
            desks.add(desk("MESA_ACORDAO_PUBLICACAO", true, true, false));
            desks.add(desk("MESA_BAIXA_ORIGEM", true, true, false));
        }
        if (journeyMode.equals("ELECTORAL_JUDICIAL_SECRETARIAT")) {
            desks.add(desk("MESA_AUTUACAO_DISTRIBUICAO_ELEITORAL", true, true, false));
            desks.add(desk("MESA_CORREGEDORIA_ELEITORAL", true, true, false));
            desks.add(desk("MESA_PESQUISAS_ELEITORAIS", false, true, false));
            desks.add(desk("MESA_INSPECAO_CORREGEDORIA", false, true, false));
        }
        if (journeyMode.equals("LABOUR_JUDICIAL_SECRETARIAT")) {
            desks.add(desk("MESA_CUSTAS_GRU", true, true, false));
            desks.add(desk("MESA_ACERVO_DIGITAL", true, true, true));
            desks.add(desk("MESA_MIDIAS_PROCESSUAIS", true, false, true));
            desks.add(desk("MESA_EXECUCAO_TRABALHISTA", true, true, false));
        }
        if (journeyMode.equals("MILITARY_JUDICIAL_SECRETARIAT")) {
            desks.add(desk("MESA_AUDITORIA_MILITAR", true, true, false));
            desks.add(desk("MESA_PLANTAO_MILITAR", true, true, true));
            desks.add(desk("MESA_BALCAO_VIRTUAL_MILITAR", true, false, true));
            desks.add(desk("MESA_SESSAO_MILITAR", true, false, true));
        }
        return List.copyOf(desks);
    }

    private OperationalDeskView desk(String deskCode, boolean mandatory, boolean batchCapable, boolean virtualCapable) {
        return new OperationalDeskView(deskCode, deskCode, deskCode, deskCode, "SECRETARIA", null, mandatory, batchCapable, virtualCapable, "CATALOGO");
    }

    private List<OperationalDeskActionView> buildActions(List<OperationalDeskView> desks) {
        LinkedHashMap<String, OperationalDeskActionView> out = new LinkedHashMap<>();
        for (OperationalDeskView desk : desks) {
            switch (safeToken(desk.deskCode())) {
                case "MESA_RECEBIMENTO" -> {
                    add(out, action(desk, "REGISTRAR_RECEBIMENTO", "Registrar recebimento", "TRANSACTIONAL", false, true, true, "MESA_SANEAMENTO"));
                    add(out, action(desk, "CLASSIFICAR_ENTRADA", "Classificar entrada", "TRANSACTIONAL", false, true, false, "MESA_SANEAMENTO"));
                }
                case "MESA_SANEAMENTO" -> {
                    add(out, action(desk, "VALIDAR_REGULARIDADE", "Validar regularidade", "TRANSACTIONAL", true, false, true, "MESA_EXPEDIENTES"));
                    add(out, action(desk, "SOLICITAR_REGULARIZACAO", "Solicitar regularização", "TRANSACTIONAL", true, false, true, "MESA_EXPEDIENTES"));
                }
                case "MESA_EXPEDIENTES" -> {
                    add(out, action(desk, "EMITIR_COMUNICACAO", "Emitir comunicação", "TRANSACTIONAL", true, true, true, "COMUNICACAO_PUBLICADA"));
                    add(out, action(desk, "CONTROLAR_CIENCIA_PRAZO", "Controlar ciência e prazo", "TRANSACTIONAL", false, true, true, "PRAZO_MONITORADO"));
                }
                case "MESA_ATOS_JUNTADAS" -> {
                    add(out, action(desk, "JUNTAR_PETICAO_AVULSA", "Juntar petição avulsa", "TRANSACTIONAL", false, true, true, "DOCUMENTO_JUNTADO"));
                    add(out, action(desk, "MATERIALIZAR_ATO_ACESSORIO", "Materializar ato acessório", "TRANSACTIONAL", true, true, true, "ATO_MATERIALIZADO"));
                }
                case "MESA_AUDIENCIA" -> {
                    add(out, action(desk, "PREPARAR_PAUTA_AUDIENCIA", "Preparar pauta de audiência", "TRANSACTIONAL", true, true, true, "AUDIENCIA_AGENDADA"));
                    add(out, action(desk, "REGISTRAR_COMPARECIMENTO_AUDIENCIA", "Registrar comparecimento", "TRANSACTIONAL", false, false, true, "PRESENCA_REGISTRADA"));
                }
                case "MESA_CUMPRIMENTO" -> {
                    add(out, action(desk, "IMPULSIONAR_CUMPRIMENTO", "Impulsionar cumprimento", "TRANSACTIONAL", true, true, true, "CUMPRIMENTO_INICIADO"));
                    add(out, action(desk, "REDISTRIBUIR_APOS_CUMPRIMENTO", "Redistribuir após cumprimento", "TRANSACTIONAL", true, true, true, "REDISTRIBUICAO_INTERNA"));
                }
                case "MESA_ADMISSIBILIDADE" -> {
                    add(out, action(desk, "VALIDAR_PREPARO_RECURSAL", "Validar preparo recursal", "TRANSACTIONAL", true, false, true, "ADMISSIBILIDADE_ANALISADA"));
                    add(out, action(desk, "VALIDAR_TEMPESTIVIDADE_RECURSAL", "Validar tempestividade recursal", "TRANSACTIONAL", true, false, true, "ADMISSIBILIDADE_ANALISADA"));
                }
                case "MESA_GABINETE_RELATOR" -> {
                    add(out, action(desk, "CONCLUIR_AO_RELATOR", "Concluir ao relator", "TRANSACTIONAL", true, false, true, "MESA_PAUTA_COLEGIADA"));
                    add(out, action(desk, "DEVOLVER_PARA_SANEAMENTO", "Devolver para saneamento", "TRANSACTIONAL", true, false, true, "MESA_SANEAMENTO"));
                }
                case "MESA_PAUTA_COLEGIADA" -> {
                    add(out, action(desk, "INCLUIR_EM_PAUTA", "Incluir em pauta", "TRANSACTIONAL", true, true, true, "MESA_PUBLICACAO_PAUTA"));
                    add(out, action(desk, "RESERVAR_SESSAO_COLEGIADA", "Reservar sessão colegiada", "TRANSACTIONAL", true, true, true, "MESA_PUBLICACAO_PAUTA"));
                }
                case "MESA_PUBLICACAO_PAUTA" -> {
                    add(out, action(desk, "PUBLICAR_PAUTA", "Publicar pauta", "TRANSACTIONAL", true, true, true, "MESA_SUSTENTACAO_ORAL"));
                    add(out, action(desk, "INTIMAR_PARA_SESSAO", "Intimar para sessão", "TRANSACTIONAL", true, true, true, "CIENCIA_SESSAO"));
                }
                case "MESA_SUSTENTACAO_ORAL" -> {
                    add(out, action(desk, "RECEBER_PEDIDO_SUSTENTACAO_ORAL", "Receber pedido de sustentação oral", "TRANSACTIONAL", true, false, true, "MESA_SESSAO"));
                    add(out, action(desk, "VALIDAR_ARQUIVO_SUSTENTACAO", "Validar arquivo de sustentação", "TRANSACTIONAL", true, false, true, "MESA_SESSAO"));
                }
                case "MESA_SESSAO" -> {
                    add(out, action(desk, "REGISTRAR_RESULTADO_SESSAO", "Registrar resultado da sessão", "TRANSACTIONAL", true, false, true, "MESA_ACORDAO_PUBLICACAO"));
                    add(out, action(desk, "GERAR_CERTIDAO_SESSAO", "Gerar certidão de sessão", "TRANSACTIONAL", true, true, true, "MESA_ACORDAO_PUBLICACAO"));
                }
                case "MESA_ACORDAO_PUBLICACAO" -> {
                    add(out, action(desk, "CONSOLIDAR_ACORDAO", "Consolidar acórdão", "TRANSACTIONAL", true, false, true, "ACORDAO_CONSOLIDADO"));
                    add(out, action(desk, "PUBLICAR_ACORDAO", "Publicar acórdão", "TRANSACTIONAL", true, true, true, "MESA_BAIXA_ORIGEM"));
                }
                case "MESA_BAIXA_ORIGEM" -> {
                    add(out, action(desk, "BAIXAR_A_ORIGEM", "Baixar à origem", "TRANSACTIONAL", true, true, true, "PROCESSO_RETORNADO_ORIGEM"));
                    add(out, action(desk, "ENCAMINHAR_DESTINO_FINAL", "Encaminhar destino final", "TRANSACTIONAL", true, true, true, "PROCESSO_DESTINO_FINAL"));
                }
                case "MESA_AUTUACAO_DISTRIBUICAO_ELEITORAL" -> {
                    add(out, action(desk, "AUTUAR_FEITO_ELEITORAL", "Autuar feito eleitoral", "TRANSACTIONAL", true, true, true, "DISTRIBUICAO_ELEITORAL"));
                    add(out, action(desk, "DISTRIBUIR_FEITO_ELEITORAL", "Distribuir feito eleitoral", "TRANSACTIONAL", true, true, true, "MESA_ADMISSIBILIDADE"));
                }
                case "MESA_CORREGEDORIA_ELEITORAL" -> {
                    add(out, action(desk, "INSTAURAR_PROCEDIMENTO_CORREGEDOR", "Instaurar procedimento correicional", "TRANSACTIONAL", true, false, true, "CORREGEDORIA_EM_ANDAMENTO"));
                    add(out, action(desk, "REGISTRAR_DILIGENCIA_CORREGEDORIA", "Registrar diligência correicional", "TRANSACTIONAL", true, true, true, "CORREGEDORIA_EM_ANDAMENTO"));
                }
                case "MESA_PESQUISAS_ELEITORAIS" -> {
                    add(out, action(desk, "VALIDAR_REGISTRO_PESQUISA", "Validar registro de pesquisa", "TRANSACTIONAL", true, true, true, "REGISTRO_VALIDADO"));
                    add(out, action(desk, "CONSOLIDAR_DEMANDA_PARTIDARIA", "Consolidar demanda partidária", "TRANSACTIONAL", true, true, true, "DEMANDA_PARTIDARIA_CONSOLIDADA"));
                }
                case "MESA_INSPECAO_CORREGEDORIA" -> {
                    add(out, action(desk, "ABRIR_CICLO_INSPECAO", "Abrir ciclo de inspeção", "TRANSACTIONAL", true, false, true, "INSPECAO_ABERTA"));
                    add(out, action(desk, "CONCLUIR_RELATORIO_INSPECAO", "Concluir relatório de inspeção", "TRANSACTIONAL", true, false, true, "INSPECAO_CONCLUIDA"));
                }
                case "MESA_CUSTAS_GRU" -> {
                    add(out, action(desk, "VALIDAR_CUSTAS_GRU", "Validar custas e GRU", "TRANSACTIONAL", true, true, true, "CUSTAS_VALIDADAS"));
                    add(out, action(desk, "CONCILIAR_RECOLHIMENTO", "Conciliar recolhimento", "TRANSACTIONAL", true, true, true, "RECOLHIMENTO_CONCILIADO"));
                }
                case "MESA_ACERVO_DIGITAL" -> {
                    add(out, action(desk, "RECEBER_MIDIA_ACERVO", "Receber mídia do acervo", "TRANSACTIONAL", true, true, true, "MESA_MIDIAS_PROCESSUAIS"));
                    add(out, action(desk, "ORGANIZAR_PROVA_AUDIOVISUAL", "Organizar prova audiovisual", "TRANSACTIONAL", false, true, true, "MESA_MIDIAS_PROCESSUAIS"));
                }
                case "MESA_MIDIAS_PROCESSUAIS" -> {
                    add(out, action(desk, "VALIDAR_MIDIA_PROCESSUAL", "Validar mídia processual", "TRANSACTIONAL", true, false, true, "MIDIA_VALIDADA"));
                    add(out, action(desk, "DISPONIBILIZAR_MIDIA_PJB", "Disponibilizar mídia no PJB", "TRANSACTIONAL", true, true, true, "MIDIA_DISPONIBILIZADA"));
                }
                case "MESA_EXECUCAO_TRABALHISTA" -> {
                    add(out, action(desk, "IMPULSIONAR_EXECUCAO_TRABALHISTA", "Impulsionar execução trabalhista", "TRANSACTIONAL", true, true, true, "EXECUCAO_IMPULSIONADA"));
                    add(out, action(desk, "CONTROLAR_GARANTIA_EXECUCAO", "Controlar garantia da execução", "TRANSACTIONAL", true, true, true, "GARANTIA_CONTROLADA"));
                }
                case "MESA_AUDITORIA_MILITAR" -> {
                    add(out, action(desk, "PREPARAR_AUDITORIA_MILITAR", "Preparar auditoria militar", "TRANSACTIONAL", true, true, true, "AUDITORIA_PREPARADA"));
                    add(out, action(desk, "CONSOLIDAR_PECA_MILITAR", "Consolidar peça militar", "TRANSACTIONAL", true, true, true, "PECA_MILITAR_CONSOLIDADA"));
                }
                case "MESA_PLANTAO_MILITAR" -> {
                    add(out, action(desk, "RECEBER_URGENCIA_PLANTAO", "Receber urgência de plantão", "TRANSACTIONAL", true, true, true, "PLANTAO_ATIVADO"));
                    add(out, action(desk, "ESCALAR_MAGISTRADO_PLANTAO", "Escalar magistrado de plantão", "TRANSACTIONAL", true, false, true, "PLANTAO_ESCALADO"));
                }
                case "MESA_BALCAO_VIRTUAL_MILITAR" -> {
                    add(out, action(desk, "REGISTRAR_ATENDIMENTO_VIRTUAL", "Registrar atendimento virtual", "TRANSACTIONAL", false, false, true, "ATENDIMENTO_REGISTRADO"));
                    add(out, action(desk, "ANEXAR_EVIDENCIA_ATENDIMENTO", "Anexar evidência do atendimento", "TRANSACTIONAL", false, true, true, "ATENDIMENTO_EVIDENCIADO"));
                }
                case "MESA_SESSAO_MILITAR" -> {
                    add(out, action(desk, "REGISTRAR_RESULTADO_SESSAO_MILITAR", "Registrar resultado da sessão militar", "TRANSACTIONAL", true, false, true, "MESA_ACORDAO_PUBLICACAO"));
                    add(out, action(desk, "PUBLICAR_RESULTADO_SESSAO_MILITAR", "Publicar resultado da sessão militar", "TRANSACTIONAL", true, true, true, "PUBLICACAO_CONCLUIDA"));
                }
                default -> {
                }
            }
        }
        return List.copyOf(out.values());
    }

    private OperationalDeskActionView action(OperationalDeskView desk,
                                             String actionCode,
                                             String displayName,
                                             String executionMode,
                                             boolean requiresConfirmation,
                                             boolean batchCapable,
                                             boolean evidenceRequired,
                                             String resultTransition) {
        return new OperationalDeskActionView(
                desk.deskCode(),
                actionCode,
                displayName,
                executionMode,
                requiresConfirmation,
                batchCapable,
                evidenceRequired,
                resultTransition
        );
    }

    private void add(Map<String, OperationalDeskActionView> out, OperationalDeskActionView view) {
        out.putIfAbsent(view.deskCode() + ':' + view.actionCode(), view);
    }

    private List<String> buildGaps(String journeyMode,
                                   String queueCode,
                                   String branchClass,
                                   List<OperationalDeskView> desks,
                                   List<OperationalDeskActionView> actions,
                                   SecretariatJudicialIntegrationProfile integrationProfile) {
        List<String> gaps = new ArrayList<>();
        String queue = safeToken(queueCode);
        String connectorSystem = safeToken(integrationProfile.connectorSystem());
        if (!journeyMode.equals("FIRST_INSTANCE_SECRETARIAT") && actions.stream().noneMatch(action -> action.actionCode().equals("INCLUIR_EM_PAUTA"))) {
            gaps.add("acao-de-inclusao-em-pauta-ainda-nao-explicita");
        }
        if (!journeyMode.equals("FIRST_INSTANCE_SECRETARIAT") && actions.stream().noneMatch(action -> action.actionCode().equals("PUBLICAR_ACORDAO"))) {
            gaps.add("acao-de-publicacao-de-acordao-ainda-nao-explicita");
        }
        if (!journeyMode.equals("FIRST_INSTANCE_SECRETARIAT") && actions.stream().noneMatch(action -> action.actionCode().equals("BAIXAR_A_ORIGEM"))) {
            gaps.add("acao-de-baixa-a-origem-ainda-nao-explicita");
        }
        if (branchClass.equals("ELEITORAL") && actions.stream().noneMatch(action -> action.actionCode().contains("CORREGEDOR"))) {
            gaps.add("acao-de-corregedoria-eleitoral-ainda-nao-explicita");
        }
        if (branchClass.equals("TRABALHISTA") && actions.stream().noneMatch(action -> action.actionCode().contains("MIDIA"))) {
            gaps.add("acao-de-midias-processuais-ainda-nao-explicita");
        }
        if (branchClass.equals("MILITAR") && !containsAny(connectorSystem, "EPROC")) {
            gaps.add("acao-militar-ainda-sem-conector-eproc-explicito");
        }
        if (branchClass.equals("MILITAR") && actions.stream().noneMatch(action -> action.actionCode().contains("PLANTAO") || action.actionCode().contains("ATENDIMENTO"))) {
            gaps.add("acao-de-plantao-ou-atendimento-militar-ainda-nao-explicita");
        }
        if (!journeyMode.equals("FIRST_INSTANCE_SECRETARIAT") && !containsAny(queue, "PAUTA", "SESSAO", "ACORDAO", "BAIXA", "SUSTENT")) {
            gaps.add("contexto-atual-ainda-nao-traz-fila-colegiada-explicita");
        }
        if (desks.isEmpty()) {
            gaps.add("nenhuma-mesa-operacional-disponivel-no-contexto-atual");
        }
        return List.copyOf(gaps);
    }

    private List<String> buildLabels(String journeyMode, String branchClass, List<OperationalDeskActionView> actions, List<String> gaps) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(journeyMode);
        out.add(branchClass);
        if (actions.stream().anyMatch(OperationalDeskActionView::evidenceRequired)) {
            out.add("ACOES_COM_EVIDENCIA");
        }
        if (actions.stream().anyMatch(OperationalDeskActionView::batchCapable)) {
            out.add("ACOES_EM_LOTE");
        }
        if (gaps.isEmpty()) {
            out.add("ACOES_OPERACIONAIS_NATIVAS");
        } else {
            out.add("ACOES_OPERACIONAIS_COM_GAPS");
        }
        return List.copyOf(out);
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

    public record OperationalActionSnapshot(
            String journeyMode,
            List<OperationalDeskActionView> actions,
            List<String> gaps,
            List<String> labels,
            Map<String, Object> diagnostics
    ) {
    }

    public record OperationalActionCatalogView(List<OperationalActionCatalogRow> rows) {
    }

    public record OperationalActionCatalogRow(
            String journeyMode,
            String descriptor,
            List<OperationalDeskActionView> actions
    ) {
    }

    public record OperationalDeskActionView(
            String deskCode,
            String actionCode,
            String displayName,
            String executionMode,
            boolean requiresConfirmation,
            boolean batchCapable,
            boolean evidenceRequired,
            String resultTransition
    ) {
    }
}
