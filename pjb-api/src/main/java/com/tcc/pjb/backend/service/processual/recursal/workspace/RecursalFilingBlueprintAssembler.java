package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecursalFilingBlueprintAssembler {

    private static final String FAMILY_SEGUNDO_GRAU_COMUM = "SEGUNDO_GRAU_COMUM";
    private static final String FAMILY_TURMA_RECURSAL = "TURMA_RECURSAL";
    private static final String FAMILY_TRT_TST = "TRT_TST";
    private static final String FAMILY_TRE_TSE = "TRE_TSE";
    private static final String FAMILY_TJM_STM = "TJM_STM";
    private static final String FAMILY_STJ_STF = "STJ_STF";

    private RecursalFilingBlueprintAssembler() {
    }

    public static Map<String, Object> assemble(Processo processo,
                                               LegalAppealType appealType,
                                               RecursalAdmissibilityResponse admissibility,
                                               boolean pedidoEfeitoSuspensivo,
                                               boolean preparoDispensado) {
        if (appealType == null) {
            return Map.of();
        }
        RamoDireito ramo = processo == null ? null : processo.getRamoDireito();
        RitoProcessual rito = processo == null ? null : processo.getRito();
        String institutionalFamily = institutionalFamily(appealType, ramo, admissibility);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "BLUEPRINT_RECURSAL_ASSISTIDO");
        out.put("difereDaPeticaoInicial", true);
        out.put("noviceSafe", true);
        out.put("plainLanguage", true);
        out.put("tipoRecursalCanonico", appealType.name());
        out.put("familiaOrgaoJulgadorDestino", institutionalFamily);
        out.put("destinoInstitucionalExplicado", institutionalFamilyLabel(institutionalFamily));
        put(out, "ramoDireito", ramo == null ? null : ramo.name());
        put(out, "ritoProcessualOrigem", rito == null ? null : rito.name());
        put(out, "routeKind", admissibility == null ? null : admissibility.routeKind());
        put(out, "sessionMode", admissibility == null ? null : admissibility.sessionMode());
        put(out, "counterReasonsMode", admissibility == null ? null : admissibility.counterReasonsMode());
        put(out, "tribunalDestino", admissibility == null ? null : admissibility.tribunalDestino());
        put(out, "instanciaDestino", admissibility == null ? null : admissibility.instanciaDestino());
        out.put("pedidoEfeitoSuspensivoSolicitado", pedidoEfeitoSuspensivo);
        out.put("preparoDispensado", preparoDispensado);
        out.put("atoPrincipal", principalActLabel(appealType));
        out.put("destinoPeticionamento", destinationModeLabel(appealType, admissibility));
        out.put("blocosObrigatorios", mandatoryBlocks(appealType, institutionalFamily, pedidoEfeitoSuspensivo, preparoDispensado));
        out.put("camposObrigatorios", requiredFields(appealType, ramo, institutionalFamily, pedidoEfeitoSuspensivo, preparoDispensado));
        out.put("camposCondicionados", conditionalFields(appealType, ramo, institutionalFamily, pedidoEfeitoSuspensivo, preparoDispensado));
        out.put("documentosObrigatorios", requiredDocuments(appealType, ramo, institutionalFamily, preparoDispensado));
        out.put("dossieDocumentalEssencial", essentialDocumentDossier(appealType, ramo, institutionalFamily));
        out.put("travasDeValidacao", validationGates(appealType, ramo, institutionalFamily, pedidoEfeitoSuspensivo, preparoDispensado, admissibility));
        out.put("avisosOperacionais", operationalWarnings(appealType, ramo, institutionalFamily, admissibility));
        out.put("assistantPrompts", assistantPrompts(appealType, ramo, institutionalFamily));
        return Collections.unmodifiableMap(out);
    }

    private static List<Map<String, Object>> mandatoryBlocks(LegalAppealType appealType,
                                                             String institutionalFamily,
                                                             boolean pedidoEfeitoSuspensivo,
                                                             boolean preparoDispensado) {
        ArrayList<Map<String, Object>> blocks = new ArrayList<>();
        blocks.add(block("identificacao_decisao", "Decisão atacada", "Informe qual decisão, sentença ou acórdão está sendo impugnado e a data da ciência."));
        blocks.add(block("tempestividade", "Tempestividade", "O PJB precisa fechar a data da intimação/publicação e o marco final do prazo recursal."));
        blocks.add(block("razoes_recursais", "Razões recursais", "A petição recursal precisa expor erro, nulidade, omissão, contradição ou desacerto do pronunciamento recorrido."));
        blocks.add(block("pedido_recursal", "Pedido recursal", "Indique o que se pretende: reforma, invalidação, integração, retratação, destrancamento ou outro provimento próprio da espécie."));
        blocks.add(block("regularidade_formal", "Regularidade formal", "O sistema valida representação, assinatura, preparo, peças obrigatórias e pressupostos específicos do recurso."));
        if (pedidoEfeitoSuspensivo) {
            blocks.add(block("efeito_suspensivo", "Tutela ou efeito suspensivo", "Explique risco de dano, utilidade do provimento e urgência para o efeito recursal pretendido."));
        }
        if (!preparoDispensado) {
            blocks.add(block("preparo", "Preparo e custas", "Quando a lei exigir preparo, o recurso deve carregar o comprovante ou a razão jurídica da dispensa."));
        }
        switch (appealType) {
            case EMBARGOS_DECLARACAO -> blocks.add(block("vicio_embargado", "Vício da decisão", "Nos embargos, a peça deve apontar omissão, contradição, obscuridade ou erro material da decisão embargada."));
            case AGRAVO_INSTRUMENTO, AGRAVO_RECURSO_REVISTA -> blocks.add(block("pecas_instrumentais", "Peças do instrumento", "O agravo instrumental exige seleção rigorosa das peças que viabilizam o imediato exame do recurso."));
            case RESP, RE -> blocks.add(block("admissibilidade_excepcional", "Pressupostos excepcionais", "O recurso excepcional exige tese federal ou constitucional, prequestionamento e filtros próprios do tribunal."));
            case RECURSO_REVISTA -> blocks.add(block("filtro_tst", "Filtros do recurso de revista", "A peça precisa destacar transcendência e o trecho do acórdão regional que concentra a controvérsia."));
            case AGRAVO_PETICAO -> blocks.add(block("delimitacao_execucao", "Delimitação da execução", "No agravo de petição, o sistema exige delimitação justificada das matérias e dos valores impugnados."));
            default -> {
            }
        }
        switch (institutionalFamily) {
            case FAMILY_SEGUNDO_GRAU_COMUM -> blocks.add(block("dialogo_com_decisao_origem", "Diálogo com a decisão de origem", "O recurso de segundo grau precisa enfrentar os capítulos da sentença ou decisão de origem com ataque organizado."));
            case FAMILY_TURMA_RECURSAL -> blocks.add(block("microssistema_juizados", "Microssistema dos Juizados", "O PJB ajusta prazo, preparo, linguagem e peças para o ambiente da turma recursal ou da uniformização."));
            case FAMILY_TRT_TST -> blocks.add(block("grade_trabalhista_colegiada", "Colegiado trabalhista", "O recurso precisa respeitar o regime recursal trabalhista, inclusive filtros próprios de TRT, TST e execução trabalhista."));
            case FAMILY_TRE_TSE -> blocks.add(block("matriz_eleitoral", "Contexto eleitoral", "O recurso eleitoral deve deixar claro pleito, cargo, fase do processo eleitoral e órgão de origem."));
            case FAMILY_TJM_STM -> blocks.add(block("matriz_militar", "Contexto militar", "A trilha militar exige identificação da auditoria, conselho ou colegiado militar, além da origem JMU ou estadual."));
            case FAMILY_STJ_STF -> blocks.add(block("filtro_superior", "Filtros dos tribunais superiores", "O PJB exige peça alinhada com admissibilidade estrita, prequestionamento, temas qualificados e competência superior."));
            default -> {
            }
        }
        return List.copyOf(blocks);
    }

    private static List<Map<String, Object>> requiredFields(LegalAppealType appealType,
                                                            RamoDireito ramo,
                                                            String institutionalFamily,
                                                            boolean pedidoEfeitoSuspensivo,
                                                            boolean preparoDispensado) {
        LinkedHashSet<Map<String, Object>> fields = new LinkedHashSet<>();
        fields.add(field("decisaoRecorridaId", "Decisão atacada", "Escolha a decisão, sentença ou acórdão que será impugnado.", true, "DECISAO"));
        fields.add(field("dataCienciaDecisao", "Data da ciência da decisão", "Informe a data da intimação, publicação ou ciência válida para o prazo recursal.", true, "TEMPORAL"));
        fields.add(field("pedidoRecursal", "Pedido recursal", "Descreva o resultado pretendido com o recurso.", true, "PEDIDO"));
        fields.add(field("fundamentoNuclear", "Erro ou vício principal", "Explique o principal erro, nulidade, omissão, contradição ou desacerto que motivou o recurso.", true, "FUNDAMENTO"));
        if (pedidoEfeitoSuspensivo) {
            fields.add(field("urgenciaRecursal", "Risco da demora", "Explique por que o recurso precisa de tutela imediata ou efeito suspensivo.", true, "URGÊNCIA"));
        }
        if (!preparoDispensado) {
            fields.add(field("comprovacaoPreparo", "Preparo", "Anexe o comprovante do preparo ou indique a razão legal para eventual dispensa.", true, "CUSTAS"));
        }
        switch (appealType) {
            case EMBARGOS_DECLARACAO -> {
                fields.add(field("vicioEmbargado", "Vício da decisão", "Marque se há omissão, contradição, obscuridade ou erro material.", true, "EMBARGOS"));
                fields.add(field("trechoEmbargado", "Trecho da decisão", "Aponte o trecho exato da decisão que contém o vício.", true, "EMBARGOS"));
            }
            case AGRAVO_INSTRUMENTO -> {
                fields.add(field("decisaoInterlocutoria", "Decisão interlocutória agravada", "Identifique a decisão interlocutória agravada e o contexto do incidente.", true, "AGRAVO"));
                fields.add(field("pecasInstrumentaisSelecionadas", "Peças do instrumento", "Selecione as peças indispensáveis para o tribunal compreender o agravo.", true, "AGRAVO"));
            }
            case AGRAVO_INTERNO, AGRAVO_REGIMENTAL -> fields.add(field("decisaoMonocratica", "Decisão monocrática", "Indique a decisão monocrática submetida ao colegiado.", true, "COLEGIADO"));
            case RESP -> {
                fields.add(field("questaoFederal", "Questão federal", "Indique qual dispositivo federal foi violado, contrariadamente interpretado ou dissidente.", true, "EXCEPCIONAL"));
                fields.add(field("prequestionamento", "Prequestionamento", "Aponte onde o acórdão recorrido enfrentou a questão federal.", true, "EXCEPCIONAL"));
            }
            case RE -> {
                fields.add(field("questaoConstitucional", "Questão constitucional", "Indique o dispositivo constitucional violado.", true, "EXCEPCIONAL"));
                fields.add(field("prequestionamento", "Prequestionamento", "Aponte onde o acórdão recorrido enfrentou a matéria constitucional.", true, "EXCEPCIONAL"));
                fields.add(field("repercussaoGeral", "Repercussão geral", "Explique por que a matéria ultrapassa o interesse subjetivo do caso.", true, "EXCEPCIONAL"));
            }
            case RECURSO_INOMINADO -> fields.add(field("razoesRecorrente", "Razões do recorrente", "No recurso inominado, a petição deve trazer razões recursais e o pedido do recorrente.", true, "JUIZADO"));
            case PEDIDO_UNIFORMIZACAO -> {
                fields.add(field("paradigmaUniformizacao", "Paradigma de uniformização", "Indique o precedente, acórdão ou orientação divergente que sustenta a uniformização.", true, "JUIZADO_FEDERAL"));
                fields.add(field("pontoDivergencia", "Ponto de divergência", "Explique com precisão a divergência que justifica o pedido de uniformização.", true, "JUIZADO_FEDERAL"));
            }
            case RECURSO_ORDINARIO_TRABALHISTA -> fields.add(field("capitulosSentenca", "Capítulos impugnados", "Indique quais capítulos da sentença trabalhista estão sendo atacados.", true, "TRABALHISTA"));
            case RECURSO_REVISTA -> {
                fields.add(field("trechoAcordaoRegional", "Trecho do acórdão regional", "Transcreva ou destaque o trecho do acórdão regional que contém a controvérsia.", true, "TRABALHISTA_TST"));
                fields.add(field("transcendencia", "Transcendência", "Explique a transcendência econômica, política, social ou jurídica.", true, "TRABALHISTA_TST"));
            }
            case AGRAVO_RECURSO_REVISTA -> fields.add(field("decisaoDenegatoria", "Decisão denegatória", "Indique a decisão que negou seguimento ao recurso principal.", true, "TRABALHISTA_TST"));
            case AGRAVO_PETICAO -> {
                fields.add(field("materiasImpugnadas", "Matérias impugnadas", "Delimite as matérias atacadas na execução.", true, "EXECUCAO_TRABALHISTA"));
                fields.add(field("valoresImpugnados", "Valores impugnados", "Delimite os valores controvertidos de forma justificada.", true, "EXECUCAO_TRABALHISTA"));
            }
            case EMBARGOS_EXECUCAO, EMBARGOS_EXECUCAO_FISCAL, EMBARGOS_TERCEIRO -> {
                fields.add(field("vinculoExecucao", "Vínculo com a execução", "Indique o processo de execução, penhora, constrição ou ato executivo relacionado.", true, "EXECUCAO"));
                fields.add(field("fatoGeradorIncidente", "Fato gerador do incidente", "Explique qual ato executivo justifica os embargos.", true, "EXECUCAO"));
            }
            case APELACAO_PENAL, RESE, HABEAS_CORPUS -> {
                fields.add(field("pontoPenalImpugnado", "Ponto penal impugnado", "Indique se o ataque recai sobre mérito, nulidade, dosimetria, cautelaridade ou outro ponto penal específico.", true, "PENAL"));
                fields.add(field("situacaoCustodia", "Situação da pessoa recorrente", "Informe se a pessoa está presa, solta, foragida ou submetida a medida cautelar.", true, "PENAL"));
            }
            default -> {
            }
        }
        switch (institutionalFamily) {
            case FAMILY_SEGUNDO_GRAU_COMUM -> fields.add(field("capitulosAtacadosOrigem", "Capítulos atacados", "Indique quais capítulos da decisão de origem precisam de reforma, invalidação ou integração.", true, "SEGUNDO_GRAU"));
            case FAMILY_TURMA_RECURSAL -> {
                fields.add(field("microssistemaOrigem", "Microssistema de origem", "Informe se a origem está no Juizado Especial Cível, Criminal, Federal ou Fazenda Pública.", true, "TURMA_RECURSAL"));
                fields.add(field("pedidoSinteticoTurma", "Pedido para a turma recursal", "Resuma de forma objetiva o que a turma recursal deve modificar ou uniformizar.", true, "TURMA_RECURSAL"));
            }
            case FAMILY_TRT_TST -> fields.add(field("orgaoTrabalhistaOrigem", "Órgão trabalhista de origem", "Indique se a decisão atacada veio de vara do trabalho, TRT ou TST e qual foi o órgão prolator.", true, "TRABALHISTA_COLEGIADO"));
            case FAMILY_TRE_TSE -> {
                fields.add(field("faseProcessualEleitoral", "Fase processual eleitoral", "Informe se o caso está em registro, propaganda, investigação, prestação de contas, diplomação ou outro momento do pleito.", true, "ELEITORAL"));
                fields.add(field("orgaoEleitoralOrigem", "Órgão eleitoral de origem", "Indique se a decisão veio de zona eleitoral, TRE ou TSE.", true, "ELEITORAL"));
            }
            case FAMILY_TJM_STM -> {
                fields.add(field("justicaMilitarOrigem", "Justiça Militar de origem", "Informe se a decisão veio da Justiça Militar da União ou da Justiça Militar Estadual.", true, "MILITAR"));
                fields.add(field("orgaoMilitarOrigem", "Órgão militar de origem", "Indique se a decisão veio de auditoria, conselho de justiça, TJM ou STM.", true, "MILITAR"));
            }
            case FAMILY_STJ_STF -> fields.add(field("fundamentoAcessoTribunalSuperior", "Fundamento de acesso ao tribunal superior", "Explique por que este recurso ou incidente chega ao tribunal superior e qual filtro institucional precisa ser vencido.", true, "TRIBUNAL_SUPERIOR"));
            default -> {
            }
        }
        if (ramo == RamoDireito.ELEITORAL) {
            fields.add(field("atoEleitoralImpugnado", "Ato eleitoral impugnado", "Indique qual ato, decisão ou fase do pleito está sendo atacado.", true, "ELEITORAL"));
        }
        if (ramo == RamoDireito.MILITAR) {
            fields.add(field("contextoMilitarOrigem", "Contexto militar", "Explique a corporação, a auditoria ou o conselho de justiça de origem.", true, "MILITAR"));
        }
        return List.copyOf(fields.stream().toList());
    }

    private static List<Map<String, Object>> conditionalFields(LegalAppealType appealType,
                                                               RamoDireito ramo,
                                                               String institutionalFamily,
                                                               boolean pedidoEfeitoSuspensivo,
                                                               boolean preparoDispensado) {
        ArrayList<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("pedidoJusticaGratuitaRecursal", "Justiça gratuita recursal", "Preencha apenas se houver pedido ou impugnação de gratuidade no recurso.", false, "ACESSORIO"));
        fields.add(field("fatoSupervenienteRecursal", "Fato superveniente", "Use este campo se ocorreu fato novo relevante após a decisão recorrida.", false, "ACESSORIO"));
        if (pedidoEfeitoSuspensivo) {
            fields.add(field("perigoDanoRecursal", "Perigo de dano", "Complemente com risco concreto, dano grave ou inutilidade do recurso.", true, "URGÊNCIA"));
        }
        if (appealType == LegalAppealType.RE || appealType == LegalAppealType.RESP) {
            fields.add(field("precedentesQualificados", "Precedentes qualificados", "Indique temas, repetitivos, repercussão geral ou súmulas relevantes, se existirem.", false, "EXCEPCIONAL"));
        }
        if (appealType == LegalAppealType.AGRAVO_INSTRUMENTO || appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA) {
            fields.add(field("pecasComplementaresInstrumento", "Peças complementares", "Acrescente outras peças úteis se o instrumento básico não bastar para o julgamento.", false, "AGRAVO"));
        }
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            fields.add(field("efeitoModificativoPretendido", "Efeito modificativo", "Preencha somente se os embargos puderem alterar o resultado e houver necessidade de contraditório.", false, "EMBARGOS"));
        }
        if (appealType == LegalAppealType.RECURSO_ORDINARIO_TRABALHISTA || appealType == LegalAppealType.RECURSO_REVISTA || appealType == LegalAppealType.AGRAVO_PETICAO) {
            fields.add(field("depositoRecursal", "Depósito recursal", "Preencha quando houver depósito recursal ou justificativa de isenção.", !preparoDispensado, "TRABALHISTA"));
        }
        if (ramo == RamoDireito.ELEITORAL) {
            fields.add(field("pleitoAnoCargo", "Pleito, ano e cargo", "Complete quando a controvérsia depender do cargo e do pleito envolvidos.", false, "ELEITORAL"));
        }
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            fields.add(field("pedidoSustentacaoOral", "Sustentação oral", "Preencha se houver pretensão de sustentação oral ou cautela urgente em sessão.", false, "PENAL_MILITAR"));
        }
        switch (institutionalFamily) {
            case FAMILY_TURMA_RECURSAL -> fields.add(field("pedidoOralNaoConvertido", "Pedido oral de origem", "Use este campo quando a origem do Juizado tiver pedido oral ou simplificado que precise ser contextualizado no recurso.", false, "TURMA_RECURSAL"));
            case FAMILY_TRE_TSE -> fields.add(field("calendarioPleitoUrgente", "Janela do calendário eleitoral", "Complete quando o recurso depender de urgência ligada ao calendário do pleito.", false, "ELEITORAL"));
            case FAMILY_TJM_STM -> fields.add(field("reflexoHierarquiaDisciplina", "Reflexo em hierarquia ou disciplina", "Use este campo se o recurso depender de contexto disciplinar, hierárquico ou funcional militar.", false, "MILITAR"));
            case FAMILY_STJ_STF -> fields.add(field("temaQualificadoSuperior", "Tema qualificado", "Preencha quando houver tema repetitivo, repercussão geral, súmula ou paradigma qualificado relevante.", false, "TRIBUNAL_SUPERIOR"));
            default -> {
            }
        }
        return List.copyOf(fields);
    }

    private static List<Map<String, Object>> requiredDocuments(LegalAppealType appealType,
                                                               RamoDireito ramo,
                                                               String institutionalFamily,
                                                               boolean preparoDispensado) {
        ArrayList<Map<String, Object>> documents = new ArrayList<>();
        documents.add(document("decisaoRecorrida", "Decisão recorrida", "A decisão atacada deve acompanhar a trilha recursal com integridade e autenticidade."));
        documents.add(document("comprovacaoIntimacao", "Comprovação da intimação", "Documento ou metadado que permita fechar a tempestividade do recurso."));
        if (!preparoDispensado) {
            documents.add(document("comprovantePreparo", "Comprovante de preparo", "Comprovante de custas, depósito ou informação válida de dispensa."));
        }
        switch (appealType) {
            case AGRAVO_INSTRUMENTO -> {
                documents.add(document("decisaoAgravada", "Decisão agravada", "Peça obrigatória do instrumento."));
                documents.add(document("procuracoes", "Procurações", "Procurações do agravante e do agravado quando exigíveis."));
                documents.add(document("pecasObrigatoriasInstrumento", "Peças obrigatórias do instrumento", "O agravo deve carregar as peças indispensáveis para o tribunal compreender a controvérsia."));
            }
            case RECURSO_INOMINADO -> documents.add(document("razoesRecursais", "Razões e pedido", "A petição escrita do recurso inominado deve conter razões e pedido do recorrente."));
            case RESP, RE -> {
                documents.add(document("acordaoRecorrido", "Acórdão recorrido", "O recurso excepcional deve trazer o acórdão recorrido como ato principal impugnado."));
                documents.add(document("certidaoPublicacaoAcordao", "Certidão de publicação do acórdão", "Serve para tempestividade e admissibilidade."));
            }
            case RECURSO_REVISTA -> {
                documents.add(document("acordaoRegional", "Acórdão regional", "O recurso de revista se estrutura sobre o acórdão regional recorrido."));
                documents.add(document("trechoTranscrito", "Trecho transcrito", "Trecho destacado do acórdão com a controvérsia recursal."));
            }
            case AGRAVO_RECURSO_REVISTA -> {
                documents.add(document("decisaoDenegatoria", "Decisão denegatória", "A decisão que negou seguimento ao recurso principal deve acompanhar o agravo."));
                documents.add(document("pecasDestrancamento", "Peças para destrancamento", "O instrumento deve permitir, se provido, o imediato julgamento do recurso principal."));
            }
            case AGRAVO_PETICAO -> documents.add(document("demonstrativoExecucao", "Demonstrativo da execução", "Documento que permita conferir as matérias e os valores delimitados."));
            case EMBARGOS_EXECUCAO, EMBARGOS_EXECUCAO_FISCAL, EMBARGOS_TERCEIRO -> documents.add(document("atoExecutivoImpugnado", "Ato executivo impugnado", "Anexe a penhora, constrição ou ato executivo que motivou os embargos."));
            default -> {
            }
        }
        switch (institutionalFamily) {
            case FAMILY_SEGUNDO_GRAU_COMUM -> documents.add(document("dossieOrigemEssencial", "Peças essenciais da origem", "Leve para o segundo grau a sentença e as peças centrais que sustentam o capítulo impugnado."));
            case FAMILY_TURMA_RECURSAL -> documents.add(document("sentencaOuDecisaoJuizado", "Sentença ou decisão do Juizado", "A turma recursal precisa ver a decisão do Juizado e as peças essenciais do microssistema de origem."));
            case FAMILY_TRT_TST -> documents.add(document("registroColegiadoTrabalhista", "Registro do colegiado trabalhista", "Leve o acórdão, a certidão de publicação e as peças trabalhistas centrais conforme o destino TRT ou TST."));
            case FAMILY_TRE_TSE -> documents.add(document("pecasCentraisEleitorais", "Peças centrais eleitorais", "Leve a decisão, a prova central do ato eleitoral impugnado e o registro da fase do pleito."));
            case FAMILY_TJM_STM -> documents.add(document("pecasCentraisMilitares", "Peças centrais militares", "Leve a decisão militar recorrida, a peça acusatória ou disciplinar central e o contexto funcional relevante."));
            case FAMILY_STJ_STF -> documents.add(document("filtroSuperiorDocumental", "Peças para tribunal superior", "Leve o acórdão recorrido, certidão de publicação, trechos de prequestionamento e documentos essenciais ao filtro superior."));
            default -> {
            }
        }
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            documents.add(document("pecaProcessualPenalCentral", "Peça central do ato penal", "Leve também a peça penal essencial para o tribunal compreender o ponto impugnado."));
        }
        return List.copyOf(documents);
    }

    private static Map<String, Object> essentialDocumentDossier(LegalAppealType appealType,
                                                               RamoDireito ramo,
                                                               String institutionalFamily) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("strategyCode", "DOSSIE_DOCUMENTAL_ESSENCIAL_RECURSAL");
        out.put("familiaOrgaoJulgadorDestino", institutionalFamily);
        out.put("destinoInstitucionalExplicado", institutionalFamilyLabel(institutionalFamily));
        out.put("tipoRecursalCanonico", appealType.name());
        put(out, "ramoDireito", ramo == null ? null : ramo.name());
        out.put("resumoSubstituiOriginal", false);
        out.put("integraPronunciamentoImpugnadoObrigatoria", true);
        out.put("cadeiaDecisoriaAnteriorObrigatoria", true);
        out.put("politicaAcervo", acervoPolicyLabel(appealType));
        out.put("ordemLeituraSugerida", readingOrder(appealType, institutionalFamily));
        out.put("pacotesDocumentais", documentPackages(appealType, ramo, institutionalFamily));
        out.put("prioridadeExibicao", displayPriority(appealType, institutionalFamily));
        out.put("avisosDossie", dossierWarnings(appealType, ramo, institutionalFamily));
        return Collections.unmodifiableMap(out);
    }

    private static List<String> readingOrder(LegalAppealType appealType, String institutionalFamily) {
        ArrayList<String> order = new ArrayList<>();
        order.add("pronunciamento_integral_impugnado");
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            order.add("trecho_exato_embargado");
        }
        order.add("cadeia_decisoria_anterior");
        order.add("razoes_recursais_e_contrarrazoes");
        order.add("pecas_nucleares_da_controversia");
        if (FAMILY_STJ_STF.equals(institutionalFamily)) {
            order.add("filtros_superiores_e_prequestionamento");
        }
        return List.copyOf(order);
    }

    private static List<Map<String, Object>> documentPackages(LegalAppealType appealType,
                                                              RamoDireito ramo,
                                                              String institutionalFamily) {
        ArrayList<Map<String, Object>> packages = new ArrayList<>();
        packages.add(documentPackage(
                "pronunciamento_impugnado",
                "Pronunciamento impugnado",
                true,
                List.of(
                        "decisao_ou_acordao_original_assinado",
                        "pdf_original_do_pronunciamento",
                        "texto_integral_extraido_quando_existir",
                        "hashes_metadados_e_autenticidade"
                )
        ));
        packages.add(documentPackage(
                "cadeia_decisoria",
                "Cadeia decisória anterior",
                true,
                FAMILY_STJ_STF.equals(institutionalFamily)
                        ? List.of("sentenca_de_primeiro_grau", "acordao_de_segundo_grau", "decisoes_intermediarias_relevantes", "embargos_anteriores_quando_existirem")
                        : List.of("decisoes_anteriores_relevantes", "certidoes_de_publicacao", "historico_de_integracao_ou_retratacao")
        ));
        packages.add(documentPackage(
                "contraditorio_recursal",
                "Razões e contraditório",
                true,
                List.of("peticao_recursal", "contrarrazoes_ou_manifestacao_equivalente", "certidoes_de_intimacao_recursal")
        ));

        switch (institutionalFamily) {
            case FAMILY_SEGUNDO_GRAU_COMUM -> {
                packages.add(documentPackage("pecas_nucleares_origem", "Peças nucleares da origem", true, List.of(
                        "peticao_inicial_ou_equivalente",
                        "contestacao_ou_resposta",
                        "ata_de_audiencia_e_depoimentos",
                        "laudos_e_documentos_centrais",
                        "parecer_mp_quando_existir"
                )));
                packages.add(documentPackage("capitulos_impugnados", "Capítulos impugnados", true, List.of(
                        "marcacao_dos_capitulos_impugnados",
                        "provas_associadas_a_cada_capitulo",
                        "fundamentos_contrarios_da_parte_adversa"
                )));
            }
            case FAMILY_TURMA_RECURSAL -> {
                packages.add(documentPackage("microssistema_juizados", "Pacote do Juizado", true, List.of(
                        "sentenca_ou_decisao_do_juizado",
                        "pedido_oral_ou_reducao_a_termo_quando_existir",
                        "ata_da_audiencia_ou_registro_equivalente",
                        "comprovante_de_preparo_quando_exigivel"
                )));
            }
            case FAMILY_TRT_TST -> {
                packages.add(documentPackage("dossie_trabalhista", "Dossiê trabalhista essencial", true, List.of(
                        "sentenca_ou_acordao_trabalhista_recorrido",
                        "ata_de_audiencia_e_depoimentos",
                        "cartoes_ponto_recibos_ou_documentos_laborais_centrais",
                        "contrarrazoes_e_certidao_de_publicacao",
                        "deposito_recursal_ou_dispensa_quando_cabivel"
                )));
                if (appealType == LegalAppealType.RECURSO_REVISTA || appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA) {
                    packages.add(documentPackage("filtro_tst", "Filtro do TST", true, List.of(
                            "trecho_do_acordao_regional",
                            "demonstracao_de_transcendencia",
                            "decisao_denegatoria_quando_houver"
                    )));
                }
            }
            case FAMILY_TRE_TSE -> {
                packages.add(documentPackage("dossie_eleitoral", "Dossiê eleitoral essencial", true, List.of(
                        "decisao_eleitoral_recorrida",
                        "ato_eleitoral_impugnado_ou_prova_central",
                        "registro_da_fase_do_pleito",
                        "documentos_de_candidatura_partido_ou_propaganda_quando_pertinentes",
                        "certidoes_eleitorais_relevantes"
                )));
            }
            case FAMILY_TJM_STM -> {
                packages.add(documentPackage("dossie_militar", "Dossiê militar essencial", true, List.of(
                        "decisao_militar_recorrida",
                        "denuncia_representacao_ou_ato_disciplinar_central",
                        "ata_do_conselho_ou_registro_da_auditoria",
                        "pecas_funcionais_ou_contexto_hierarquico_relevante",
                        "prova_tecnica_ou_pericial_militar"
                )));
            }
            case FAMILY_STJ_STF -> {
                packages.add(documentPackage("filtro_superior", "Filtro superior", true, List.of(
                        "acordao_recorrido",
                        "embargos_declaratorios_para_prequestionamento_quando_existirem",
                        "certidao_de_publicacao_do_acordao",
                        "decisao_de_admissibilidade_ou_denegacao",
                        "tema_repetitivo_repercussao_geral_ou_paradigma_qualificado"
                )));
                packages.add(documentPackage("pecas_nucleares_remissivas", "Peças nucleares remissivas", true, List.of(
                        "peticao_inicial_ou_denuncia_quando_relevante",
                        "provas_estritamente_necessarias_para_o_filtro",
                        "trechos_de_prequestionamento_ou_enfrentamento_anterior"
                )));
            }
            default -> {
            }
        }

        if (appealType == LegalAppealType.AGRAVO_INSTRUMENTO) {
            packages.add(documentPackage("instrumento", "Instrumento do agravo", true, List.of(
                    "decisao_interlocutoria_agravada",
                    "certidao_de_intimacao",
                    "procuracoes",
                    "pecas_obrigatorias_do_instrumento",
                    "pecas_facultativas_estrategicas"
            )));
        }
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            packages.add(documentPackage("integracao_da_decisao", "Integração da decisão", true, List.of(
                    "trecho_exato_embargado",
                    "pronunciamento_integral_embargado",
                    "historico_de_embargos_anteriores_quando_existir"
            )));
        }
        if (appealType == LegalAppealType.AGRAVO_PETICAO) {
            packages.add(documentPackage("execucao_trabalhista", "Execução trabalhista", true, List.of(
                    "demonstrativo_da_execucao",
                    "delimitacao_das_materias_impugnadas",
                    "delimitacao_dos_valores_impugnados"
            )));
        }
        if (appealType == LegalAppealType.EMBARGOS_EXECUCAO || appealType == LegalAppealType.EMBARGOS_EXECUCAO_FISCAL || appealType == LegalAppealType.EMBARGOS_TERCEIRO) {
            packages.add(documentPackage("apartado_dependencia", "Apartado por dependência", true, List.of(
                    "ato_executivo_impugnado",
                    "auto_de_penhora_ou_constricao",
                    "vinculo_com_a_execucao_principal"
            )));
        }
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            packages.add(documentPackage("nucleo_penal", "Núcleo penal essencial", true, List.of(
                    "denuncia_queixa_ou_resposta",
                    "ata_de_instrucao_e_julgamento",
                    "laudos_e_provas_penais_centrais",
                    "parecer_mp_quando_existir"
            )));
        }
        return List.copyOf(packages);
    }

    private static List<String> displayPriority(LegalAppealType appealType, String institutionalFamily) {
        ArrayList<String> priority = new ArrayList<>();
        priority.add("pronunciamento_integral_impugnado");
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            priority.add("trecho_exato_embargado");
        }
        if (FAMILY_STJ_STF.equals(institutionalFamily)) {
            priority.add("filtro_superior");
        }
        priority.add("cadeia_decisoria");
        priority.add("contraditorio_recursal");
        priority.add("pecas_nucleares_origem");
        return List.copyOf(priority);
    }

    private static List<String> dossierWarnings(LegalAppealType appealType,
                                                RamoDireito ramo,
                                                String institutionalFamily) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        warnings.add("Resumo auxiliar não substitui a decisão, sentença ou acórdão original íntegro.");
        warnings.add("O órgão revisor deve receber a cadeia decisória anterior e as peças essenciais da controvérsia.");
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            warnings.add("Nos embargos, a prioridade absoluta é a decisão embargada integral e o trecho exato do vício alegado.");
        }
        if (FAMILY_STJ_STF.equals(institutionalFamily)) {
            warnings.add("Nos tribunais superiores, o dossiê precisa destacar prequestionamento, acórdão recorrido e filtros de admissibilidade.");
        }
        if (ramo == RamoDireito.ELEITORAL) {
            warnings.add("Na trilha eleitoral, o dossiê deve carregar a prova central do ato impugnado e a fase do pleito.");
        }
        if (ramo == RamoDireito.MILITAR) {
            warnings.add("Na trilha militar, o dossiê deve preservar o contexto funcional e a identificação da origem JMU ou estadual.");
        }
        return List.copyOf(warnings);
    }

    private static String acervoPolicyLabel(LegalAppealType appealType) {
        return switch (appealType) {
            case EMBARGOS_DECLARACAO -> "DECISAO_EMBARGADA_INTEGRAL_NO_MESMO_GRAU";
            case EMBARGOS_EXECUCAO, EMBARGOS_EXECUCAO_FISCAL, EMBARGOS_TERCEIRO -> "APARTADO_POR_DEPENDENCIA_COM_VINCULO_A_EXECUCAO";
            case AGRAVO_INSTRUMENTO, AGRAVO_RECURSO_REVISTA -> "INSTRUMENTO_AUTONOMO_COM_PECAS_SELECIONADAS";
            default -> "REMESSA_RECURSAL_COM_DECISAO_INTEGRAL_E_ACERVO_ESSENCIAL";
        };
    }

    private static Map<String, Object> documentPackage(String code, String label, boolean required, List<String> items) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code);
        out.put("label", label);
        out.put("required", required);
        out.put("items", List.copyOf(items));
        out.put("plainLanguage", true);
        return Collections.unmodifiableMap(out);
    }

    private static List<String> validationGates(LegalAppealType appealType,
                                                RamoDireito ramo,
                                                String institutionalFamily,
                                                boolean pedidoEfeitoSuspensivo,
                                                boolean preparoDispensado,
                                                RecursalAdmissibilityResponse admissibility) {
        LinkedHashSet<String> gates = new LinkedHashSet<>();
        gates.add("Fechar tempestividade com base na ciência válida da decisão recorrida.");
        gates.add("Confirmar regularidade da representação, assinatura e legitimidade recursal.");
        if (!preparoDispensado) {
            gates.add("Conferir preparo, custas e depósito recursal quando a lei exigir.");
        }
        if (pedidoEfeitoSuspensivo) {
            gates.add("Validar urgência, risco e utilidade do provimento para o efeito suspensivo ou tutela recursal.");
        }
        switch (appealType) {
            case EMBARGOS_DECLARACAO -> gates.add("Limitar o cabimento aos vícios próprios da decisão embargada.");
            case AGRAVO_INSTRUMENTO -> gates.add("Validar a formação do instrumento com peças obrigatórias e seleção documental suficiente.");
            case RESP -> gates.add("Exigir questão federal e prequestionamento explícito ou demonstrável no acórdão recorrido.");
            case RE -> gates.add("Exigir questão constitucional, prequestionamento e demonstração de repercussão geral.");
            case RECURSO_INOMINADO -> gates.add("Controlar prazo curto e preparo em 48 horas quando exigível no microssistema dos juizados.");
            case RECURSO_REVISTA -> gates.add("Exigir trecho do acórdão regional, transcendência e fundamento específico de admissibilidade trabalhista.");
            case AGRAVO_PETICAO -> gates.add("Exigir delimitação justificada das matérias e dos valores impugnados na execução.");
            case AGRAVO_RECURSO_REVISTA -> gates.add("Exigir peça apta ao destrancamento do recurso principal com instrumentação suficiente.");
            default -> {
            }
        }
        switch (institutionalFamily) {
            case FAMILY_SEGUNDO_GRAU_COMUM -> gates.add("Conferir que a peça enfrenta os capítulos da decisão de origem sem transformar o recurso em nova petição inicial.");
            case FAMILY_TURMA_RECURSAL -> gates.add("Controlar rito do microssistema dos Juizados, inclusive preparo, síntese recursal e ambiente da turma recursal.");
            case FAMILY_TRT_TST -> gates.add("Conferir filtro institucional trabalhista, depósito recursal, transcendência ou delimitação executória quando aplicável.");
            case FAMILY_TRE_TSE -> gates.add("Conferir fase do pleito, cargo, calendário eleitoral e órgão eleitoral competente antes da distribuição recursal.");
            case FAMILY_TJM_STM -> gates.add("Conferir se a trilha identifica corretamente JMU ou Justiça Militar Estadual, auditoria, conselho, TJM ou STM competentes.");
            case FAMILY_STJ_STF -> gates.add("Exigir aderência ao filtro próprio do tribunal superior, inclusive competência, prequestionamento e paradigma qualificado quando cabíveis.");
            default -> {
            }
        }
        if (ramo == RamoDireito.ELEITORAL) {
            gates.add("Conferir se o recurso respeita a fase do pleito, o cargo disputado e o órgão eleitoral competente.");
        }
        if (ramo == RamoDireito.MILITAR) {
            gates.add("Conferir se a trilha identifica corretamente JMU ou Justiça Militar Estadual, auditoria ou órgão colegiado competente.");
        }
        if (admissibility != null && Boolean.TRUE.equals(admissibility.certificateRequired())) {
            gates.add("Exigir assinatura forte ou certificado quando o canal institucional do tribunal assim determinar.");
        }
        return List.copyOf(gates);
    }

    private static List<String> operationalWarnings(LegalAppealType appealType,
                                                    RamoDireito ramo,
                                                    String institutionalFamily,
                                                    RecursalAdmissibilityResponse admissibility) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        warnings.add("A petição recursal não replica automaticamente a petição inicial: o PJB exige estrutura própria do recurso.");
        warnings.add("O órgão julgador é definido pela malha de competência e distribuição; a parte não escolhe a câmara, turma ou relator.");
        if (appealType.isExceptional()) {
            warnings.add("Recursos excepcionais dependem de filtros de admissibilidade mais rígidos no tribunal de origem e no tribunal superior.");
        }
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            warnings.add("Embargos de declaração não substituem recurso amplo: servem para integrar ou corrigir a própria decisão embargada.");
        }
        if (appealType == LegalAppealType.AGRAVO_INSTRUMENTO || appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA) {
            warnings.add("No agravo instrumental, a seleção documental incompleta compromete o exame imediato do recurso.");
        }
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            warnings.add("Na trilha penal ou militar, o PJB deve preservar o histórico decisório, a custódia e as peças centrais do ato impugnado.");
        }
        if (admissibility != null && normalize(admissibility.counterReasonsMode()).contains("CONTROL")) {
            warnings.add("O contraditório recursal pode seguir regime controlado por sigilo, urgência ou competência especializada.");
        }
        switch (institutionalFamily) {
            case FAMILY_TURMA_RECURSAL -> warnings.add("No ambiente de turma recursal, o PJB simplifica a linguagem, mas mantém prazo, preparo e técnica recursal do microssistema.");
            case FAMILY_TRT_TST -> warnings.add("No destino trabalhista colegiado, o PJB ajusta depósito recursal, acórdão regional, transcendência e execução conforme a espécie recursal.");
            case FAMILY_TRE_TSE -> warnings.add("No destino eleitoral, o calendário do pleito e a fase processual podem exigir leitura mais urgente e documentalmente precisa.");
            case FAMILY_TJM_STM -> warnings.add("No destino militar, a peça precisa identificar com precisão a origem JMU ou estadual, a auditoria ou o colegiado militar competente.");
            case FAMILY_STJ_STF -> warnings.add("No destino superior, o PJB intensifica filtros de admissibilidade e não aceita peça genérica sem diálogo com o acórdão recorrido.");
            default -> {
            }
        }
        return List.copyOf(warnings);
    }

    private static List<String> assistantPrompts(LegalAppealType appealType,
                                                 RamoDireito ramo,
                                                 String institutionalFamily) {
        ArrayList<String> prompts = new ArrayList<>();
        prompts.add("Qual decisão você quer impugnar e em que data houve a ciência válida?");
        prompts.add("O que exatamente precisa ser reformado, integrado, invalidado ou destrancado?");
        prompts.add("Quais são os capítulos da decisão que concentram o erro ou vício principal?");
        if (appealType == LegalAppealType.AGRAVO_INSTRUMENTO) {
            prompts.add("Quais peças do processo de origem o tribunal precisa ver imediatamente para compreender o agravo?");
        }
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            prompts.add("Há omissão, contradição, obscuridade ou erro material? Em qual trecho da decisão?");
        }
        if (appealType == LegalAppealType.RE || appealType == LegalAppealType.RESP) {
            prompts.add("Onde o acórdão recorrido enfrentou a tese federal ou constitucional e por que o filtro excepcional está atendido?");
        }
        if (appealType == LegalAppealType.RECURSO_REVISTA) {
            prompts.add("Qual trecho do acórdão regional deve ser destacado e por que a causa apresenta transcendência?");
        }
        if (appealType == LegalAppealType.AGRAVO_PETICAO) {
            prompts.add("Quais matérias e valores da execução estão sendo delimitados no agravo de petição?");
        }
        if (ramo == RamoDireito.ELEITORAL) {
            prompts.add("Qual ato eleitoral, cargo, pleito e fase processual estão envolvidos na impugnação?");
        }
        if (ramo == RamoDireito.MILITAR) {
            prompts.add("A origem está na Justiça Militar da União ou Estadual e qual foi o órgão julgador de origem?");
        }
        switch (institutionalFamily) {
            case FAMILY_SEGUNDO_GRAU_COMUM -> prompts.add("Quais capítulos da decisão de origem precisam ser atacados no segundo grau e com quais provas ou peças principais?");
            case FAMILY_TURMA_RECURSAL -> prompts.add("A origem veio de qual Juizado e o que a turma recursal precisa corrigir sem repetir a petição inicial?");
            case FAMILY_TRT_TST -> prompts.add("O recurso vai para TRT ou TST, e qual filtro trabalhista específico — acórdão regional, transcendência, execução ou depósito — precisa ser enfrentado?");
            case FAMILY_TRE_TSE -> prompts.add("O destino é TRE ou TSE, e como o recurso se conecta ao pleito, ao cargo e à fase eleitoral concreta?");
            case FAMILY_TJM_STM -> prompts.add("O destino é TJM ou STM, e qual auditoria, conselho ou colegiado militar proferiu a decisão impugnada?");
            case FAMILY_STJ_STF -> prompts.add("Qual filtro do tribunal superior precisa ser vencido e onde isso já aparece no acórdão recorrido?");
            default -> {
            }
        }
        return List.copyOf(prompts);
    }

    private static String principalActLabel(LegalAppealType appealType) {
        return switch (appealType) {
            case EMBARGOS_DECLARACAO -> "PETICAO_INTEGRATIVA_DA_PROPRIA_DECISAO";
            case AGRAVO_INSTRUMENTO, AGRAVO_RECURSO_REVISTA -> "PETICAO_INSTRUMENTAL_AUTONOMA";
            case EMBARGOS_EXECUCAO, EMBARGOS_EXECUCAO_FISCAL, EMBARGOS_TERCEIRO -> "INCIDENTE_EM_APARTADO_POR_DEPENDENCIA";
            case RESP, RE -> "PETICAO_EXCEPCIONAL_COM_FILTROS_SUPERIORES";
            default -> "PETICAO_RECURSAL_COM_RAZOES_E_PEDIDO";
        };
    }

    private static String destinationModeLabel(LegalAppealType appealType, RecursalAdmissibilityResponse admissibility) {
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO || appealType == LegalAppealType.AGRAVO_INTERNO || appealType == LegalAppealType.AGRAVO_REGIMENTAL) {
            return "MESMO_GRAU_COM_REDIRECIONAMENTO_PARA_AUTORIDADE_COMPETENTE";
        }
        if (appealType == LegalAppealType.EMBARGOS_EXECUCAO || appealType == LegalAppealType.EMBARGOS_EXECUCAO_FISCAL || appealType == LegalAppealType.EMBARGOS_TERCEIRO) {
            return "APARTADO_POR_DEPENDENCIA_NO_MESMO_GRAU";
        }
        if (admissibility != null && admissibility.instanciaDestino() != null && !admissibility.instanciaDestino().isBlank()) {
            return "REMESSA_E_DISTRIBUICAO_PARA_" + admissibility.instanciaDestino().toUpperCase(Locale.ROOT);
        }
        return "REMESSA_E_DISTRIBUICAO_CONFORME_A_MALHA_RECURSAL";
    }

    private static String institutionalFamily(LegalAppealType appealType,
                                              RamoDireito ramo,
                                              RecursalAdmissibilityResponse admissibility) {
        String tribunal = normalize(admissibility == null ? null : admissibility.tribunalDestino());
        String instancia = normalize(admissibility == null ? null : admissibility.instanciaDestino());
        String routeKind = normalize(admissibility == null ? null : admissibility.routeKind());
        if (tribunal.contains("TURMA_RECURSAL") || instancia.contains("TURMA_RECURSAL") || routeKind.contains("TURMA_RECURSAL")
                || appealType == LegalAppealType.RECURSO_INOMINADO || appealType == LegalAppealType.PEDIDO_UNIFORMIZACAO) {
            return FAMILY_TURMA_RECURSAL;
        }
        if (tribunal.startsWith("TRT") || "TST".equals(tribunal)
                || ramo == RamoDireito.TRABALHISTA
                || appealType == LegalAppealType.RECURSO_ORDINARIO_TRABALHISTA
                || appealType == LegalAppealType.RECURSO_REVISTA
                || appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA
                || appealType == LegalAppealType.AGRAVO_PETICAO) {
            return FAMILY_TRT_TST;
        }
        if (tribunal.startsWith("TRE") || "TSE".equals(tribunal) || ramo == RamoDireito.ELEITORAL) {
            return FAMILY_TRE_TSE;
        }
        if (tribunal.startsWith("TJM") || "STM".equals(tribunal) || ramo == RamoDireito.MILITAR) {
            return FAMILY_TJM_STM;
        }
        if ("STJ".equals(tribunal) || "STF".equals(tribunal)
                || appealType.isExceptional()
                || appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL
                || appealType == LegalAppealType.CONFLITO_COMPETENCIA
                || appealType == LegalAppealType.RECURSO_ORDINARIO_CONSTITUCIONAL) {
            return FAMILY_STJ_STF;
        }
        return FAMILY_SEGUNDO_GRAU_COMUM;
    }

    private static String institutionalFamilyLabel(String institutionalFamily) {
        return switch (institutionalFamily) {
            case FAMILY_TURMA_RECURSAL -> "Turma recursal ou órgão de uniformização";
            case FAMILY_TRT_TST -> "Colegiado trabalhista de TRT ou TST";
            case FAMILY_TRE_TSE -> "Colegiado eleitoral de TRE ou TSE";
            case FAMILY_TJM_STM -> "Colegiado da Justiça Militar ou STM";
            case FAMILY_STJ_STF -> "Tribunal superior, corte excepcional ou corte constitucional";
            default -> "Segundo grau comum, regional ou estadual";
        };
    }

    private static Map<String, Object> block(String code, String label, String helperText) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code);
        out.put("label", label);
        out.put("helperText", helperText);
        out.put("plainLanguage", true);
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Object> field(String code, String label, String helperText, boolean required, String group) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code);
        out.put("label", label);
        out.put("helperText", helperText);
        out.put("required", required);
        out.put("group", group);
        out.put("plainLanguage", true);
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Object> document(String code, String label, String helperText) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code);
        out.put("label", label);
        out.put("helperText", helperText);
        out.put("plainLanguage", true);
        return Collections.unmodifiableMap(out);
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || value == null) {
            return;
        }
        target.put(key, value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
