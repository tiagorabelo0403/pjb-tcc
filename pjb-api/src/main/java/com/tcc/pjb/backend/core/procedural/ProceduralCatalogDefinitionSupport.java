package com.tcc.pjb.backend.core.procedural;

import static com.tcc.pjb.backend.core.procedural.ProceduralCatalogStageSupport.*;

import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.DefinitionSnapshot;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.DocumentSpec;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.PartyRoleSpec;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import java.util.List;

final class ProceduralCatalogDefinitionSupport {

    private static final List<String> BASE_CIVIL_COMPETENCE = List.of("vara civel", "juizado especial", "camara civel", "tribunal competente");
    private static final List<String> BASE_TRABALHO_COMPETENCE = List.of("vara do trabalho", "trt competente", "tst em sede recursal");
    private static final List<String> BASE_PENAL_COMPETENCE = List.of("vara criminal", "juizado especial criminal", "tribunal do juri", "tribunal competente");
    private static final List<String> BASE_ELEITORAL_COMPETENCE = List.of("zona eleitoral", "tre competente", "tse em sede recursal ou originaria");
    private static final List<String> BASE_MILITAR_COMPETENCE = List.of("auditoria militar", "conselho de justica", "stm ou tjm competente");
    private static final List<String> BASE_TRIBUTARIO_COMPETENCE = List.of("vara da fazenda publica", "vara federal", "juizado fazendario", "camara de direito publico");
    private static final List<String> BASE_PREVIDENCIARIO_COMPETENCE = List.of("vara federal", "jef competente", "trf competente");

    private ProceduralCatalogDefinitionSupport() {
    }

    public static DefinitionSnapshot snapshot(RitoProcessual rito) {
        RitoProcessual resolved = rito == null ? RitoProcessual.COMUM_ORDINARIO : rito;
        return switch (resolved) {
            case ESPECIAL_HABEAS_CORPUS, PENAL_HABEAS_CORPUS_PREVENTIVO, MILITAR_HABEAS_CORPUS_MILITAR -> habeasCorpus(resolved);
            case ESPECIAL_MANDADO_SEGURANCA, ESPECIAL_MANDADO_SEGURANCA_COLETIVO, TRIBUTARIO_MANDADO_SEGURANCA, TRABALHISTA_MANDADO_SEGURANCA -> mandadoSeguranca(resolved);
            case ELEITORAL_PROPAGANDA, ELEITORAL_DIREITO_RESPOSTA -> eleitoralPropaganda(resolved);
            case ELEITORAL_REGISTRO_CANDIDATURA -> eleitoralRegistroCandidatura(resolved);
            case ELEITORAL_AIRC -> eleitoralAirc(resolved);
            case ELEITORAL_AIJE, ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO -> eleitoralAije(resolved);
            case ELEITORAL_AIME, ELEITORAL_RCED, ELEITORAL_INELEGIBILIDADE -> eleitoralControleMandato(resolved);
            case ELEITORAL_PRESTACAO_CONTAS -> eleitoralPrestacaoContas(resolved);
            case MILITAR_IPM -> militarIpm(resolved);
            case MILITAR_PROCESSO_PENAL_MILITAR, MILITAR, MILITAR_CONSELHO_JUSTICA -> militarPenal(resolved);
            case MILITAR_PAD -> militarPad(resolved);
            case TRIBUNAL_JURI -> tribunalJuri(resolved);
            case EXECUCAO_PENAL -> execucaoPenal(resolved);
            case TRABALHISTA_DISSIDIO_COLETIVO -> trabalhistaDissidio(resolved);
            case TRABALHISTA_SUMARIO_ALCADA -> trabalhistaAlcada(resolved);
            case TRABALHISTA_INQUERITO_FALTA_GRAVE -> trabalhistaInqueritoFaltaGrave(resolved);
            case TRABALHISTA_ACAO_CUMPRIMENTO -> trabalhistaAcaoCumprimento(resolved);
            case TRABALHISTA_ORDINARIO, TRABALHISTA_SUMARISSIMO, TRABALHISTA_ACIDENTE_TRABALHO, TRABALHISTA_EXECUCAO,
                    TRABALHISTA_CUMPRIMENTO_SENTENCA, TRABALHISTA_ACAO_RESCISORIA, TRABALHISTA_TUTELA_CAUTELAR -> trabalhistaIndividual(resolved);
            case PREVIDENCIARIO_JEF, PREVIDENCIARIO_COMUM, PREVIDENCIARIO_BPC_LOAS, PREVIDENCIARIO_AUXILIO_INCAPACIDADE,
                    PREVIDENCIARIO_APOSENTADORIA, PREVIDENCIARIO_REVISAO_BENEFICIO, PREVIDENCIARIO_RESTABELECIMENTO,
                    PREVIDENCIARIO_ACIDENTARIO, PREVIDENCIARIO_SALARIO_MATERNIDADE, PREVIDENCIARIO_PENSAO_MORTE,
                    PREVIDENCIARIO_RURAL, PREVIDENCIARIO_ESPECIAL, PREVIDENCIARIO_RPPS -> previdenciario(resolved);
            case EXECUCAO_FISCAL, TRIBUTARIO_ANULATORIA_DEBITO, TRIBUTARIO_REPETICAO_INDEBITO, TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL,
                    TRIBUTARIO_DECLARATORIA, TRIBUTARIO_CAUTELAR_FISCAL, FAZENDA_PUBLICA_CONHECIMENTO, FAZENDA_PUBLICA_EXECUCAO,
                    IMPROBIDADE_ADMINISTRATIVA, ADMINISTRATIVO_ACAO_POPULAR, ADMINISTRATIVO_ACAO_CIVIL_PUBLICA_ADM -> tributarioFazenda(resolved);
            case ADMINISTRATIVO_PAD -> administrativoPad(resolved);
            case ADMINISTRATIVO_CONCURSO_PUBLICO -> administrativoConcursoPublico(resolved);
            case ADMINISTRATIVO_SERVIDORES -> administrativoServidores(resolved);
            case RECUPERACAO_JUDICIAL, RECUPERACAO_EXTRAJUDICIAL, FALENCIA, INCIDENTE_DESCONSIDERACAO_PERSONALIDADE_JURIDICA -> empresarial(resolved);
            case INFANCIA_JUVENTUDE_ECA, INFANCIA_JUVENTUDE_ADOCAO, INFANCIA_JUVENTUDE_INFRACIONAL, INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR -> infancia(resolved);
            case AGRARIO_DESAPROPRIACAO, AGRARIO_USUCAPIAO_RURAL, AGRARIO_ACP_AGRARIA, AGRARIO_POSSE_TERRA -> agrario(resolved);
            case AMBIENTAL_ACP, AMBIENTAL_CRIMINAL, AMBIENTAL_TUTELA_URGENTE -> ambiental(resolved);
            case HOMOLOGACAO_SENTENCA_ESTRANGEIRA, CARTA_ROGATORIA, COOPERACAO_JURIDICA_INTERNACIONAL -> internacional(resolved);
            default -> byGroup(resolved);
        };
    }


    private static DefinitionSnapshot byGroup(RitoProcessual rito) {
        return switch (rito.group()) {
            case "ELEITORAL" -> eleitoralGeral(rito);
            case "MILITAR" -> militarPenal(rito);
            case "PENAL" -> penalGeral(rito);
            case "TRABALHISTA" -> trabalhistaIndividual(rito);
            case "PREVIDENCIARIO" -> previdenciario(rito);
            case "TRIBUTARIO_FAZENDA" -> tributarioFazenda(rito);
            case "EMPRESARIAL" -> empresarial(rito);
            case "INFANCIA_JUVENTUDE" -> infancia(rito);
            case "AGRARIO" -> agrario(rito);
            case "AMBIENTAL" -> ambiental(rito);
            case "INTERNACIONAL" -> internacional(rito);
            case "METODOS_AUTOCOMPOSITIVOS" -> metodosAutocompositivos(rito);
            case "ESPECIAL_CONSTITUCIONAL" -> especialConstitucional(rito);
            default -> civilGeral(rito);
        };
    }

    private static DefinitionSnapshot civilGeral(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.CIVIL.name(),
                parties(role("AUTOR", true, true), role("REU", true, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.DOCUMENTO_IDENTIDADE, true, "Identificação da parte"),
                        doc(TipoDocumento.COMPROVANTE_ENDERECO, true, "Competência territorial e intimações"),
                        doc(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS, true, "Lastro mínimo da causa"),
                        doc(TipoDocumento.MEMORIA_CALCULO_VALOR_CAUSA, false, "Compatibilização do valor atribuído")
                ),
                macroStages(rito, true, false, false),
                BASE_CIVIL_COMPETENCE
        );
    }

    private static DefinitionSnapshot especialConstitucional(RitoProcessual rito) {
        if (rito == RitoProcessual.ESPECIAL_ACAO_POPULAR || rito == RitoProcessual.ADMINISTRATIVO_ACAO_POPULAR) {
            return definition(
                    rito,
                    humanize(rito.name()),
                    RamoDireito.CONSTITUCIONAL.name(),
                    parties(role("AUTOR_POPULAR", true, true), role("REU", true, true), role("MINISTERIO_PUBLICO", false, true)),
                    documents(
                            doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                            doc(TipoDocumento.PROVA_CIDADANIA_ATIVA, true, "Legitimidade ativa"),
                            doc(TipoDocumento.PROVA_PRE_CONSTITUIDA, true, "Demonstração do ato impugnado"),
                            doc(TipoDocumento.DOCUMENTOS_ADMINISTRATIVOS, false, "Instrução probatória inicial")
                    ),
                    macroStages(rito, true, false, false),
                    List.of("vara da fazenda publica", "tribunal competente", "stf ou stj quando cabível")
            );
        }
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.CONSTITUCIONAL.name(),
                parties(role("REQUERENTE", true, true), role("REQUERIDO", false, true), role("MINISTERIO_PUBLICO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROVA_PRE_CONSTITUIDA, true, "Demonstração do direito invocado"),
                        doc(TipoDocumento.PARECER_TECNICO, false, "Material técnico de suporte"),
                        doc(TipoDocumento.LEGISLACAO_PERTINENTE, false, "Normas questionadas ou aplicáveis")
                ),
                macroStages(rito, true, false, false),
                List.of("tribunal competente", "stf", "stj", "vara ou juizado competente")
        );
    }

    private static DefinitionSnapshot mandadoSeguranca(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                rito == RitoProcessual.TRABALHISTA_MANDADO_SEGURANCA ? RamoDireito.TRABALHISTA.name() : rito == RitoProcessual.TRIBUTARIO_MANDADO_SEGURANCA ? RamoDireito.TRIBUTARIO.name() : RamoDireito.CONSTITUCIONAL.name(),
                parties(role("IMPETRANTE", true, true), role("AUTORIDADE_COATORA", true, false), role("IMPETRADO", true, true), role("MINISTERIO_PUBLICO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.PROVA_PRE_CONSTITUIDA, true, "Direito líquido e certo"),
                        doc(TipoDocumento.ATO_COATOR, true, "Identificação do ato impugnado"),
                        doc(TipoDocumento.DOCUMENTO_AUTORIDADE_COATORA, true, "Qualificação da autoridade coatora"),
                        doc(TipoDocumento.PEDIDO_LIMINAR_FUNDAMENTADO, false, "Tutela de urgência em writ"),
                        doc(TipoDocumento.COMPROVANTE_ENDERECO, false, "Competência territorial")
                ),
                stagesForWrit(rito),
                List.of("vara competente", "tribunal competente", "gabinete originário", "colegiado recursal")
        );
    }

    private static DefinitionSnapshot habeasCorpus(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                rito.isMilitar() ? RamoDireito.MILITAR.name() : RamoDireito.PENAL.name(),
                parties(role("IMPETRANTE", true, true), role("PACIENTE", true, true), role("AUTORIDADE_COATORA", true, false), role("MINISTERIO_PUBLICO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça de impetração"),
                        doc(TipoDocumento.DOCUMENTO_PACIENTE, false, "Qualificação do paciente"),
                        doc(TipoDocumento.DECISAO_COATORA, false, "Ato impugnado"),
                        doc(TipoDocumento.PECAS_RELEVANTES, true, "Lastro mínimo para exame da coação"),
                        doc(TipoDocumento.PROVA_ILEGALIDADE, true, "Constrangimento ilegal demonstrado"),
                        doc(TipoDocumento.PEDIDO_LIMINAR, false, "Pedido liminar quando houver urgência")
                ),
                stagesForHabeasCorpus(rito),
                rito.isMilitar() ? BASE_MILITAR_COMPETENCE : BASE_PENAL_COMPETENCE
        );
    }

    private static DefinitionSnapshot penalGeral(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.PENAL.name(),
                parties(role("ACUSACAO", true, true), role("ACUSADO", true, true), role("VITIMA", false, true), role("MINISTERIO_PUBLICO", false, true), role("ASSISTENTE_ACUSACAO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça acusatória ou defensiva inaugural"),
                        doc(TipoDocumento.PROCURACAO, false, "Representação defensiva"),
                        doc(TipoDocumento.BOLETIM_OCORRENCIA, false, "Notícia-crime"),
                        doc(TipoDocumento.LAUDO_PERICIAL, false, "Prova técnica"),
                        doc(TipoDocumento.ROL_TESTEMUNHAS, false, "Organização da instrução"),
                        doc(TipoDocumento.MIDIA_PROBATORIA, false, "Suporte audiovisual ou digital"),
                        doc(TipoDocumento.CADEIA_CUSTODIA_DIGITAL, false, "Integridade da prova eletrônica")
                ),
                macroStages(rito, true, true, true),
                BASE_PENAL_COMPETENCE
        );
    }

    private static DefinitionSnapshot tribunalJuri(RitoProcessual rito) {
        List<RitoStage> stages = List.of(
                stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.PRONUNCIA), List.of(
                        work("DEFESA_PREVIA_ADV", WorkItemType.MANIFESTACAO, "Defesa prévia e teses preliminares", TipoUsuario.ADVOGADO, 1, true, 5),
                        work("ROL_TESTEMUNHAS_ADV", WorkItemType.JUNTADA, "Apresentar rol de testemunhas e requerimentos probatórios", TipoUsuario.ADVOGADO, 1, true, 5),
                        work("ANALISE_INICIAL_CARTORIO", WorkItemType.JUNTADA, "Conferir regularidade formal da denúncia e da defesa", TipoUsuario.SERVIDOR_FORUM, 2, true, 2),
                        work("DECISAO_SANEAMENTO", WorkItemType.DECISAO, "Deliberar sobre instrução preliminar", TipoUsuario.JUIZ, 1, true, 3)
                )),
                stage(FaseProcessual.PRONUNCIA, List.of(FaseProcessual.PLENARIO_JURI, FaseProcessual.RECURSAL), List.of(
                        work("MEMORIAIS_PRONUNCIA_ADV", WorkItemType.MANIFESTACAO, "Apresentar memoriais prévios à decisão de pronúncia", TipoUsuario.ADVOGADO, 1, true, 5),
                        work("DECISAO_PRONUNCIA", WorkItemType.DECISAO, "Proferir decisão de pronúncia, impronúncia, absolvição sumária ou desclassificação", TipoUsuario.JUIZ, 1, true, 5)
                )),
                stage(FaseProcessual.PLENARIO_JURI, List.of(FaseProcessual.RECURSAL), List.of(
                        work("QUESITOS_PLENARIO_ADV", WorkItemType.MANIFESTACAO, "Apresentar quesitos e preparar estratégia de plenário", TipoUsuario.ADVOGADO, 1, true, 3),
                        work("ORGANIZACAO_PLENARIO_CARTORIO", WorkItemType.AUDIENCIA, "Organizar sessão plenária e intimações finais", TipoUsuario.SERVIDOR_FORUM, 2, true, 3),
                        work("SESSAO_JURI", WorkItemType.AUDIENCIA, "Realizar sessão plenária do júri", TipoUsuario.JUIZ, 1, true, 1)
                )),
                stage(FaseProcessual.RECURSAL, List.of(), List.of(
                        work("APELACAO_JURI_ADV", WorkItemType.RECURSO, "Interpor recurso cabível contra decisão do júri", TipoUsuario.ADVOGADO, 1, true, 5),
                        work("ADMISSIBILIDADE_RECURSAL", WorkItemType.DESPACHO, "Examinar admissibilidade do recurso", TipoUsuario.JUIZ, 1, true, 2)
                ))
        );
        return definition(
                rito,
                "Tribunal do Júri",
                RamoDireito.PENAL.name(),
                parties(role("ACUSACAO", true, true), role("ACUSADO", true, true), role("VITIMA", false, true), role("MINISTERIO_PUBLICO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.ROL_TESTEMUNHAS, true, "Instrução e plenário"),
                        doc(TipoDocumento.LAUDO_PERICIAL, true, "Prova material"),
                        doc(TipoDocumento.QUESITACAO_SUGERIDA, false, "Organização do plenário"),
                        doc(TipoDocumento.MIDIA_PROBATORIA, false, "Provas digitais e audiovisuais")
                ),
                stages,
                BASE_PENAL_COMPETENCE
        );
    }

    private static DefinitionSnapshot execucaoPenal(RitoProcessual rito) {
        return definition(
                rito,
                "Execução penal",
                RamoDireito.PENAL.name(),
                parties(role("EXECUTADO", true, true), role("DEFESA", false, true), role("MINISTERIO_PUBLICO", false, true), role("ADMINISTRACAO_PRISIONAL", false, false)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.GUIA_EXECUCAO, true, "Guia e cálculo de pena"),
                        doc(TipoDocumento.ATESTADO_CONDUTA, false, "Avaliação de progressão ou benefícios"),
                        doc(TipoDocumento.BOLETIM_DISCIPLINAR, false, "Acompanhamento da execução"),
                        doc(TipoDocumento.CALCULO_PENA, true, "Controle de remição e progressão")
                ),
                List.of(
                        stage(FaseProcessual.EXECUCAO, List.of(FaseProcessual.RECURSAL), List.of(
                                work("REQUERIMENTO_BENEFICIO_ADV", WorkItemType.MANIFESTACAO, "Requerer progressão, remição ou incidente", TipoUsuario.ADVOGADO, 1, true, 5),
                                work("ANALISE_CARTORIO_VEP", WorkItemType.JUNTADA, "Conferir guia e cálculo de pena", TipoUsuario.SERVIDOR_FORUM, 2, true, 2),
                                work("DECISAO_EXECUCAO_PENAL", WorkItemType.DECISAO, "Deliberar sobre o benefício ou incidente", TipoUsuario.JUIZ, 1, true, 3)
                        )),
                        stage(FaseProcessual.RECURSAL, List.of(), List.of(
                                work("AGRAVO_EXECUCAO_ADV", WorkItemType.RECURSO, "Interpor recurso cabível na execução penal", TipoUsuario.ADVOGADO, 1, true, 5),
                                work("ADMISSIBILIDADE_RECURSO_EXECUCAO", WorkItemType.DESPACHO, "Examinar admissibilidade do recurso", TipoUsuario.JUIZ, 1, true, 2)
                        ))
                ),
                BASE_PENAL_COMPETENCE
        );
    }

    private static DefinitionSnapshot trabalhistaIndividual(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.TRABALHISTA.name(),
                parties(role("RECLAMANTE", true, true), role("RECLAMADA", true, true), role("MINISTERIO_PUBLICO_TRABALHO", false, true), role("TERCEIRO_INTERESSADO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.CTPS, true, "Vínculo laboral"),
                        doc(TipoDocumento.CONTRATO_TRABALHO, false, "Relação contratual"),
                        doc(TipoDocumento.HOLERITES, false, "Prova de remuneração"),
                        doc(TipoDocumento.CARTAO_PONTO, false, "Prova de jornada"),
                        doc(TipoDocumento.CALCULO_INICIAL, true, "Liquidação estimada dos pedidos")
                ),
                macroStages(rito, true, true, false),
                BASE_TRABALHO_COMPETENCE
        );
    }

    private static DefinitionSnapshot trabalhistaAlcada(RitoProcessual rito) {
        return definition(
                rito,
                "Reclamação trabalhista de alçada",
                RamoDireito.TRABALHISTA.name(),
                parties(role("RECLAMANTE", true, true), role("RECLAMADA", true, true), role("MINISTERIO_PUBLICO_TRABALHO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.CTPS, true, "Vínculo laboral"),
                        doc(TipoDocumento.MEMORIA_VALOR_CAUSA, true, "Fixação da alçada do art. 2º da Lei 5.584/1970"),
                        doc(TipoDocumento.ATA_AUDIENCIA, false, "Suporte para eventual revisão do valor da causa"),
                        doc(TipoDocumento.DOCUMENTOS_LIQUIDEZ_PEDIDOS, true, "Liquidez mínima dos pedidos e alçada reduzida")
                ),
                List.of(
                        stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.RECURSAL), List.of(
                                work("PROTOCOLO_ALCADA_ADV", WorkItemType.MANIFESTACAO, "Protocolar reclamação trabalhista de alçada com valor definido", TipoUsuario.ADVOGADO, 1, true, 2),
                                work("FIXACAO_VALOR_ALCADA", WorkItemType.DESPACHO, "Fixar ou revisar o valor da causa para determinação da alçada", TipoUsuario.JUIZ, 1, true, 1),
                                work("AUDIENCIA_UNA_ALCADA", WorkItemType.AUDIENCIA, "Conduzir audiência una e colher razões finais em sequência concentrada", TipoUsuario.JUIZ, 1, true, 1)
                        )),
                        stage(FaseProcessual.RECURSAL, List.of(), List.of(
                                work("REVISAO_VALOR_ALCADA", WorkItemType.RECURSO, "Registrar pedido de revisão do valor da causa ao TRT quando cabível", TipoUsuario.ADVOGADO, 1, false, 2),
                                work("CONTROLE_RECORRIBILIDADE_ALCADA", WorkItemType.DESPACHO, "Controlar restrição recursal própria da alçada trabalhista", TipoUsuario.JUIZ, 1, true, 1)
                        ))
                ),
                BASE_TRABALHO_COMPETENCE
        );
    }

    private static DefinitionSnapshot trabalhistaInqueritoFaltaGrave(RitoProcessual rito) {
        return definition(
                rito,
                "Inquérito judicial para apuração de falta grave",
                RamoDireito.TRABALHISTA.name(),
                parties(role("EMPREGADOR", true, true), role("EMPREGADO_ESTAVEL", true, true), role("MINISTERIO_PUBLICO_TRABALHO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.DOCUMENTO_ESTABILIDADE, true, "Comprovação da estabilidade do empregado"),
                        doc(TipoDocumento.ATO_SUSPENSAO, true, "Suspensão do empregado com marco temporal certo"),
                        doc(TipoDocumento.PROVA_FALTA_GRAVE, true, "Lastro documental da falta imputada"),
                        doc(TipoDocumento.CONTROLE_PRAZO_DECADENCIAL, true, "Contagem do prazo decadencial de 30 dias"),
                        doc(TipoDocumento.ROL_TESTEMUNHAS, false, "Planejamento da instrução")
                ),
                List.of(
                        stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.RECURSAL), List.of(
                                work("PROTOCOLO_INQUERITO_EMPREGADOR", WorkItemType.MANIFESTACAO, "Protocolar inquérito judicial para apuração de falta grave", TipoUsuario.ADVOGADO, 1, true, 2),
                                work("TRIAGEM_DECADENCIA_INQUERITO", WorkItemType.JUNTADA, "Conferir estabilidade e prazo decadencial do art. 853 da CLT", TipoUsuario.SERVIDOR_FORUM, 1, true, 1),
                                work("DESPACHO_INICIAL_INQUERITO", WorkItemType.DESPACHO, "Deliberar sobre processamento e instrução do inquérito", TipoUsuario.JUIZ, 1, true, 2)
                        )),
                        stage(FaseProcessual.RECURSAL, List.of(), List.of(
                                work("RECURSO_INQUERITO_ADV", WorkItemType.RECURSO, "Interpor recurso cabível no inquérito de falta grave", TipoUsuario.ADVOGADO, 1, true, 5),
                                work("ADMISSIBILIDADE_RECURSO_INQUERITO", WorkItemType.DESPACHO, "Examinar admissibilidade do recurso do inquérito", TipoUsuario.JUIZ, 1, true, 2)
                        ))
                ),
                BASE_TRABALHO_COMPETENCE
        );
    }

    private static DefinitionSnapshot trabalhistaAcaoCumprimento(RitoProcessual rito) {
        return definition(
                rito,
                "Ação de cumprimento trabalhista",
                RamoDireito.TRABALHISTA.name(),
                parties(role("SUBSTITUTO_PROCESSUAL", true, true), role("EMPREGADOR", true, true), role("TRABALHADOR_BENEFICIARIO", false, true), role("MINISTERIO_PUBLICO_TRABALHO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.INSTRUMENTO_COLETIVO, true, "Convenção, acordo ou sentença normativa"),
                        doc(TipoDocumento.CLAUSULA_DESCUMPRIDA, true, "Delimitação da obrigação descumprida"),
                        doc(TipoDocumento.PROVAS_DESCUMPRIMENTO, true, "Documentação mínima do inadimplemento"),
                        doc(TipoDocumento.DOCUMENTOS_SINDICAIS, false, "Representação coletiva ou substituição processual")
                ),
                List.of(
                        stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.CUMPRIMENTO_SENTENCA, FaseProcessual.RECURSAL), List.of(
                                work("PROTOCOLO_ACAO_CUMPRIMENTO", WorkItemType.MANIFESTACAO, "Protocolar ação de cumprimento com base no título coletivo", TipoUsuario.ADVOGADO, 1, true, 3),
                                work("TRIAGEM_TITULO_COLETIVO", WorkItemType.JUNTADA, "Conferir instrumento coletivo, cláusula exigível e representação processual", TipoUsuario.SERVIDOR_FORUM, 1, true, 2),
                                work("DESPACHO_INICIAL_ACAO_CUMPRIMENTO", WorkItemType.DESPACHO, "Deliberar sobre processamento inicial da ação de cumprimento", TipoUsuario.JUIZ, 1, true, 2)
                        )),
                        stage(FaseProcessual.CUMPRIMENTO_SENTENCA, List.of(FaseProcessual.RECURSAL), List.of(
                                work("REQUERIMENTO_CUMPRIMENTO_COLETIVO", WorkItemType.MANIFESTACAO, "Requerer cumprimento da obrigação reconhecida na ação de cumprimento", TipoUsuario.ADVOGADO, 1, true, 3),
                                work("DECISAO_CUMPRIMENTO_COLETIVO", WorkItemType.DECISAO, "Deliberar sobre liquidação e cumprimento do título coletivo reconhecido", TipoUsuario.JUIZ, 1, true, 3)
                        )),
                        stage(FaseProcessual.RECURSAL, List.of(), List.of(
                                work("RECURSO_ACAO_CUMPRIMENTO", WorkItemType.RECURSO, "Interpor recurso cabível na ação de cumprimento", TipoUsuario.ADVOGADO, 1, true, 5),
                                work("ADMISSIBILIDADE_RECURSO_ACAO_CUMPRIMENTO", WorkItemType.DESPACHO, "Examinar admissibilidade recursal da ação de cumprimento", TipoUsuario.JUIZ, 1, true, 2)
                        ))
                ),
                BASE_TRABALHO_COMPETENCE
        );
    }

    private static DefinitionSnapshot trabalhistaDissidio(RitoProcessual rito) {
        return definition(
                rito,
                "Dissídio coletivo",
                RamoDireito.TRABALHISTA.name(),
                parties(role("SUSCITANTE", true, true), role("SUSCITADO", true, true), role("MINISTERIO_PUBLICO_TRABALHO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.ATA_ASSEMBLEIA, true, "Deliberação da categoria"),
                        doc(TipoDocumento.PAUTA_REIVINDICACOES, true, "Objeto do dissídio"),
                        doc(TipoDocumento.COMPROVACAO_NEGOCIACAO_PREVIA, true, "Tentativa de autocomposição"),
                        doc(TipoDocumento.INSTRUMENTOS_COLETIVOS_ANTERIORES, false, "Contextualização histórica")
                ),
                List.of(
                        stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.RECURSAL), List.of(
                                work("PROTOCOLO_DISSIDIO_ADV", WorkItemType.MANIFESTACAO, "Protocolar dissídio coletivo com documentação sindical", TipoUsuario.ADVOGADO, 1, true, 3),
                                work("CONFERENCIA_REPRESENTATIVIDADE", WorkItemType.JUNTADA, "Conferir legitimidade e documentação sindical", TipoUsuario.SERVIDOR_FORUM, 2, true, 2),
                                work("DESPACHO_COLEGIADO_INICIAL", WorkItemType.DESPACHO, "Deliberar sobre processamento e mediação", TipoUsuario.JUIZ, 1, true, 2)
                        )),
                        stage(FaseProcessual.RECURSAL, List.of(), List.of(
                                work("RECURSO_COLETIVO_ADV", WorkItemType.RECURSO, "Interpor recurso coletivo cabível", TipoUsuario.ADVOGADO, 1, true, 5),
                                work("ADMISSIBILIDADE_RECURSO_COLETIVO", WorkItemType.DESPACHO, "Examinar admissibilidade recursal", TipoUsuario.JUIZ, 1, true, 2)
                        ))
                ),
                BASE_TRABALHO_COMPETENCE
        );
    }

    private static DefinitionSnapshot previdenciario(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.PREVIDENCIARIO.name(),
                parties(role("SEGURADO", true, true), role("INSS", true, false), role("DEPENDENTE", false, true), role("MINISTERIO_PUBLICO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.DOCUMENTO_IDENTIDADE, true, "Identificação do segurado"),
                        doc(TipoDocumento.CNIS, true, "Histórico contributivo"),
                        doc(TipoDocumento.REQUERIMENTO_ADMINISTRATIVO, true, "Prévio requerimento"),
                        doc(TipoDocumento.CARTA_INDEFERIMENTO, false, "Motivação administrativa"),
                        doc(TipoDocumento.LAUDO_MEDICO, rito == RitoProcessual.PREVIDENCIARIO_AUXILIO_INCAPACIDADE || rito == RitoProcessual.PREVIDENCIARIO_ACIDENTARIO, "Prova médica especializada"),
                        doc(TipoDocumento.EXAMES, rito == RitoProcessual.PREVIDENCIARIO_AUXILIO_INCAPACIDADE || rito == RitoProcessual.PREVIDENCIARIO_ACIDENTARIO, "Apoio pericial"),
                        doc(TipoDocumento.DOCUMENTOS_RURAIS, rito == RitoProcessual.PREVIDENCIARIO_RURAL, "Comprovação de labor rural")
                ),
                macroStages(rito, true, true, false),
                BASE_PREVIDENCIARIO_COMPETENCE
        );
    }

    private static DefinitionSnapshot tributarioFazenda(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                rito == RitoProcessual.IMPROBIDADE_ADMINISTRATIVA || rito.name().startsWith("ADMINISTRATIVO") ? RamoDireito.ADMINISTRATIVO.name() : RamoDireito.TRIBUTARIO.name(),
                parties(role("AUTOR", true, true), role("REU", true, true), role("FAZENDA_PUBLICA", false, false), role("MINISTERIO_PUBLICO", false, true), role("EXEQUENTE", false, true), role("EXECUTADO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.DOCUMENTO_IDENTIDADE, true, "Qualificação da parte"),
                        doc(TipoDocumento.ATO_ADMINISTRATIVO_OU_TRIBUTARIO, true, "Ato impugnado ou título executivo"),
                        doc(TipoDocumento.AUTO_INFRACAO_OU_CDA, rito == RitoProcessual.EXECUCAO_FISCAL || rito == RitoProcessual.TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL || rito == RitoProcessual.TRIBUTARIO_CAUTELAR_FISCAL, "Título administrativo-fiscal"),
                        doc(TipoDocumento.PLANILHA_DEBITO, rito == RitoProcessual.EXECUCAO_FISCAL || rito == RitoProcessual.FAZENDA_PUBLICA_EXECUCAO, "Memória do crédito"),
                        doc(TipoDocumento.COMPROVANTE_RECOLHIMENTO, false, "Prova de pagamento ou depósito"),
                        doc(TipoDocumento.PROVA_PRE_CONSTITUIDA, true, "Documentação mínima da controvérsia")
                ),
                macroStages(rito, true, true, false),
                BASE_TRIBUTARIO_COMPETENCE
        );
    }

    private static DefinitionSnapshot administrativoPad(RitoProcessual rito) {
        return definition(
                rito,
                "Processo administrativo disciplinar",
                RamoDireito.ADMINISTRATIVO.name(),
                parties(role("AUTOR", true, true), role("REU", true, true), role("AUTORIDADE_DISCIPLINAR", false, false), role("COMISSAO_PROCESSANTE", false, false), role("MINISTERIO_PUBLICO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.PORTARIA_INSTAURACAO, true, "Ato formal de instauração do PAD"),
                        doc(TipoDocumento.DESIGNACAO_COMISSAO, true, "Comissão processante e competência"),
                        doc(TipoDocumento.TERMO_CITACAO_ACUSADO, true, "Ciência e contraditório do acusado"),
                        doc(TipoDocumento.PECAS_INSTRUTORIAS_PAD, true, "Autos essenciais do procedimento disciplinar"),
                        doc(TipoDocumento.CRONOLOGIA_DISCIPLINAR, false, "Linha do tempo dos atos do PAD"),
                        doc(TipoDocumento.PROVA_PRE_CONSTITUIDA, false, "Suporte documental mínimo da infração"),
                        doc(TipoDocumento.DECISAO_AUTORIDADE_DISCIPLINAR, false, "Ato final ou decisão impugnada")
                ),
                List.of(
                        stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.RECURSAL), List.of(
                                work("PROTOCOLO_CONTROLE_PAD", WorkItemType.MANIFESTACAO, "Protocolar controle judicial ou incidental do PAD", TipoUsuario.ADVOGADO, 1, true, 3),
                                work("CONFERENCIA_PORTARIA_PAD", WorkItemType.JUNTADA, "Conferir portaria, comissão e ciência do acusado", TipoUsuario.SERVIDOR_FORUM, 1, true, 2),
                                work("ANALISE_GARANTIAS_PAD", WorkItemType.DESPACHO, "Analisar contraditório, ampla defesa e competência disciplinar", TipoUsuario.JUIZ, 1, true, 2)
                        )),
                        stage(FaseProcessual.RECURSAL, List.of(), List.of(
                                work("RECURSO_CONTROLE_PAD", WorkItemType.RECURSO, "Interpor recurso ou medida revisional cabível", TipoUsuario.ADVOGADO, 1, true, 5),
                                work("ADMISSIBILIDADE_RECURSO_PAD_CIVIL", WorkItemType.DESPACHO, "Examinar admissibilidade recursal do controle disciplinar", TipoUsuario.JUIZ, 1, true, 2)
                        ))
                ),
                BASE_TRIBUTARIO_COMPETENCE
        );
    }

    private static DefinitionSnapshot administrativoConcursoPublico(RitoProcessual rito) {
        return definition(
                rito,
                "Concurso público",
                RamoDireito.ADMINISTRATIVO.name(),
                parties(role("CANDIDATO", true, true), role("ENTE_PUBLICO", true, false), role("BANCA_EXAMINADORA", false, false), role("MINISTERIO_PUBLICO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.EDITAL_CONCURSO, true, "Regra vinculante do certame"),
                        doc(TipoDocumento.ATO_IMPUGNADO, true, "Resultado, eliminação ou indeferimento questionado"),
                        doc(TipoDocumento.COMPROVANTES_INSCRICAO_RESULTADO, true, "Vínculo do candidato com o certame"),
                        doc(TipoDocumento.ESPELHO_CORRECAO_OU_NOTAS, false, "Material probatório do concurso"),
                        doc(TipoDocumento.CRONOLOGIA_CERTAME, false, "Linha do tempo do concurso")
                ),
                macroStages(rito, true, true, false),
                BASE_TRIBUTARIO_COMPETENCE
        );
    }

    private static DefinitionSnapshot administrativoServidores(RitoProcessual rito) {
        return definition(
                rito,
                "Direitos funcionais de servidor",
                RamoDireito.ADMINISTRATIVO.name(),
                parties(role("SERVIDOR", true, true), role("ENTE_PUBLICO", true, false), role("ORGAO_GESTOR", false, false), role("MINISTERIO_PUBLICO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.ATO_FUNCIONAL_IMPUGNADO, true, "Ato administrativo questionado"),
                        doc(TipoDocumento.FICHA_FUNCIONAL, true, "Histórico funcional do servidor"),
                        doc(TipoDocumento.COMPROVANTES_REMUNERATORIOS, false, "Contracheques e reflexos"),
                        doc(TipoDocumento.REQUERIMENTO_ADMINISTRATIVO, false, "Prévio requerimento quando cabível"),
                        doc(TipoDocumento.PLANILHA_VALORES, false, "Cálculo funcional ou remuneratório")
                ),
                macroStages(rito, true, true, false),
                BASE_TRIBUTARIO_COMPETENCE
        );
    }

    private static DefinitionSnapshot eleitoralGeral(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.ELEITORAL.name(),
                parties(role("REPRESENTANTE", true, true), role("REPRESENTADO", true, true), role("MINISTERIO_PUBLICO_ELEITORAL", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.PROVA_PRE_CONSTITUIDA, true, "Lastro probatório inicial"),
                        doc(TipoDocumento.MIDIA_DIGITAL, false, "Conteúdo audiovisual ou eletrônico"),
                        doc(TipoDocumento.ATA_NOTARIAL_OU_HASH, false, "Integridade da prova digital"),
                        doc(TipoDocumento.CRONOLOGIA_FATOS, false, "Linha do tempo eleitoral")
                ),
                macroStagesEleitorais(rito),
                BASE_ELEITORAL_COMPETENCE
        );
    }

    private static DefinitionSnapshot eleitoralPropaganda(RitoProcessual rito) {
        return definition(
                rito,
                rito == RitoProcessual.ELEITORAL_DIREITO_RESPOSTA ? "Direito de resposta eleitoral" : "Representação por propaganda eleitoral",
                RamoDireito.ELEITORAL.name(),
                parties(role("REPRESENTANTE", true, true), role("REPRESENTADO", true, true), role("MINISTERIO_PUBLICO_ELEITORAL", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.PROVA_PRE_CONSTITUIDA, true, "Conteúdo impugnado"),
                        doc(TipoDocumento.MIDIA_PROPAGANDA, true, "Peça publicitária ou mídia da publicação"),
                        doc(TipoDocumento.ATA_NOTARIAL_OU_HASH, true, "Integridade da prova digital"),
                        doc(TipoDocumento.URL_IDENTIFICADA, false, "Localização exata da propaganda"),
                        doc(TipoDocumento.PEDIDO_TUTELA_URGENTE, false, "Remoção ou direito de resposta imediato")
                ),
                macroStagesEleitorais(rito),
                BASE_ELEITORAL_COMPETENCE
        );
    }

    private static DefinitionSnapshot eleitoralRegistroCandidatura(RitoProcessual rito) {
        return definition(
                rito,
                "Registro de candidatura",
                RamoDireito.ELEITORAL.name(),
                parties(role("REQUERENTE", true, true), role("CANDIDATO", true, true), role("PARTIDO_FEDERACAO_COLIGACAO", true, true), role("MINISTERIO_PUBLICO_ELEITORAL", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.DRAP, true, "Demonstrativo de regularidade do partido ou federação"),
                        doc(TipoDocumento.ATA_CONVENCAO, true, "Escolha em convenção"),
                        doc(TipoDocumento.FILIACAO_PARTIDARIA, true, "Comprovação de filiação"),
                        doc(TipoDocumento.CERTIDAO_QUITACAO_ELEITORAL, true, "Condição de elegibilidade"),
                        doc(TipoDocumento.CERTIDOES_CRIMINAIS, true, "Verificação de inelegibilidades"),
                        doc(TipoDocumento.DOCUMENTOS_PESSOAIS_CANDIDATO, true, "Qualificação do candidato")
                ),
                macroStagesEleitorais(rito),
                BASE_ELEITORAL_COMPETENCE
        );
    }

    private static DefinitionSnapshot eleitoralAirc(RitoProcessual rito) {
        return definition(
                rito,
                "AIRC",
                RamoDireito.ELEITORAL.name(),
                parties(role("IMPUGNANTE", true, true), role("IMPUGNADO", true, true), role("CANDIDATO", false, true), role("PARTIDO_FEDERACAO_COLIGACAO", false, true), role("MINISTERIO_PUBLICO_ELEITORAL", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.PROVA_PRE_CONSTITUIDA, true, "Demonstração da causa de inelegibilidade ou ausência de condição"),
                        doc(TipoDocumento.FILIACAO_PARTIDARIA, false, "Quando a impugnação envolver filiação"),
                        doc(TipoDocumento.CERTIDOES_ELEGIBILIDADE, false, "Prova documental de elegibilidade ou inelegibilidade"),
                        doc(TipoDocumento.DRAP_E_DOCUMENTOS_CANDIDATURA, false, "Contexto do registro"),
                        doc(TipoDocumento.CRONOLOGIA_ELEITORAL, false, "Linha do tempo do caso")
                ),
                macroStagesEleitorais(rito),
                BASE_ELEITORAL_COMPETENCE
        );
    }

    private static DefinitionSnapshot eleitoralAije(RitoProcessual rito) {
        return definition(
                rito,
                rito == RitoProcessual.ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO ? "Captação ilícita de sufrágio" : "AIJE",
                RamoDireito.ELEITORAL.name(),
                parties(role("AUTOR", true, true), role("INVESTIGADO", true, true), role("BENEFICIARIO", false, true), role("MINISTERIO_PUBLICO_ELEITORAL", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.PROVA_PRE_CONSTITUIDA, true, "Fatos e abuso narrados"),
                        doc(TipoDocumento.MIDIA_PROPAGANDA, false, "Mídias audiovisuais e digitais"),
                        doc(TipoDocumento.ATA_NOTARIAL_OU_HASH, false, "Integridade da prova eletrônica"),
                        doc(TipoDocumento.RELATORIO_ANALITICO, false, "Organização técnica dos fatos"),
                        doc(TipoDocumento.FILIACAO_PARTIDARIA, false, "Quando necessário ao enquadramento")
                ),
                macroStagesEleitorais(rito),
                BASE_ELEITORAL_COMPETENCE
        );
    }

    private static DefinitionSnapshot eleitoralControleMandato(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.ELEITORAL.name(),
                parties(role("AUTOR", true, true), role("IMPUGNADO", true, true), role("BENEFICIARIO", false, true), role("MINISTERIO_PUBLICO_ELEITORAL", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.PROVA_PRE_CONSTITUIDA, true, "Prova do ilícito eleitoral"),
                        doc(TipoDocumento.DIPLOMA_OU_MANDATO, false, "Comprovação da diplomação ou exercício do mandato"),
                        doc(TipoDocumento.ATA_NOTARIAL_OU_HASH, false, "Integridade da prova digital"),
                        doc(TipoDocumento.CRONOLOGIA_ELEITORAL, false, "Linha do tempo dos fatos")
                ),
                macroStagesEleitorais(rito),
                BASE_ELEITORAL_COMPETENCE
        );
    }

    private static DefinitionSnapshot eleitoralPrestacaoContas(RitoProcessual rito) {
        return definition(
                rito,
                "Prestação de contas eleitoral",
                RamoDireito.ELEITORAL.name(),
                parties(role("PRESTADOR_CONTAS", true, true), role("MINISTERIO_PUBLICO_ELEITORAL", false, true), role("ORGAO_TECNICO", false, false)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural ou requerimento"),
                        doc(TipoDocumento.EXTRATOS_BANCARIOS, true, "Movimentação financeira da campanha"),
                        doc(TipoDocumento.RECIBOS_ELEITORAIS, true, "Documentação financeira"),
                        doc(TipoDocumento.RELACAO_RECEITAS_DESPESAS, true, "Consolidação das contas"),
                        doc(TipoDocumento.NOTAS_FISCAIS, false, "Suporte dos gastos"),
                        doc(TipoDocumento.PARECER_TECNICO_CONTAS, false, "Análise técnica"),
                        doc(TipoDocumento.MIDIA_DIGITAL, false, "Arquivos eletrônicos das contas")
                ),
                macroStagesEleitorais(rito),
                BASE_ELEITORAL_COMPETENCE
        );
    }

    private static DefinitionSnapshot militarIpm(RitoProcessual rito) {
        return definition(
                rito,
                "Inquérito policial militar",
                RamoDireito.MILITAR.name(),
                parties(role("AUTORIDADE_MILITAR", true, false), role("INVESTIGADO", true, true), role("OFENDIDO", false, true), role("MINISTERIO_PUBLICO_MILITAR", false, true), role("DEFESA", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Requerimento, notícia ou requerimento incidente"),
                        doc(TipoDocumento.PORTARIA_INSTAURACAO, true, "Formalização do IPM"),
                        doc(TipoDocumento.DESIGNACAO_AUTORIDADE, true, "Competência da autoridade instauradora"),
                        doc(TipoDocumento.PECAS_IPM, true, "Autos essenciais do procedimento"),
                        doc(TipoDocumento.ASSENTAMENTOS_FUNCIONAIS, false, "Dados funcionais do militar"),
                        doc(TipoDocumento.LAUDO_PERICIAL, false, "Prova técnica"),
                        doc(TipoDocumento.CADEIA_CUSTODIA_DIGITAL, false, "Integridade da prova eletrônica")
                ),
                List.of(
                        stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.RECURSAL), List.of(
                                work("PETICAO_INCIDENTE_DEFESA", WorkItemType.MANIFESTACAO, "Protocolar requerimento defensivo ou incidente no IPM", TipoUsuario.ADVOGADO, 1, true, 3),
                                work("JUNTADA_PECAS_IPM", WorkItemType.JUNTADA, "Juntar peças formais do IPM e anexos militares", TipoUsuario.SERVIDOR_FORUM, 2, true, 2),
                                work("ANALISE_AUTORIDADE_MILITAR", WorkItemType.DILIGENCIA, "Determinar diligências e saneamento do IPM", TipoUsuario.JUIZ, 1, true, 3)
                        )),
                        stage(FaseProcessual.RECURSAL, List.of(), List.of(
                                work("RECURSO_INCIDENTE_MILITAR", WorkItemType.RECURSO, "Interpor recurso ou correição cabível", TipoUsuario.ADVOGADO, 1, true, 5),
                                work("ADMISSIBILIDADE_RECURSO_MILITAR", WorkItemType.DESPACHO, "Analisar admissibilidade do recurso militar", TipoUsuario.JUIZ, 1, true, 2)
                        ))
                ),
                BASE_MILITAR_COMPETENCE
        );
    }

    private static DefinitionSnapshot militarPenal(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.MILITAR.name(),
                parties(role("ACUSACAO", true, true), role("ACUSADO", true, true), role("OFENDIDO", false, true), role("MINISTERIO_PUBLICO_MILITAR", false, true), role("AUTORIDADE_MILITAR", false, false)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação defensiva"),
                        doc(TipoDocumento.PORTARIA_INSTAURACAO, false, "Contexto do fato militar"),
                        doc(TipoDocumento.DESIGNACAO_AUTORIDADE, false, "Competência da autoridade militar"),
                        doc(TipoDocumento.PECAS_IPM, true, "Elementos informativos do procedimento militar"),
                        doc(TipoDocumento.ASSENTAMENTOS_FUNCIONAIS, false, "Histórico funcional do militar"),
                        doc(TipoDocumento.LAUDO_PERICIAL, false, "Prova técnica"),
                        doc(TipoDocumento.ROL_TESTEMUNHAS, false, "Planejamento da instrução")
                ),
                macroStagesMilitares(rito),
                BASE_MILITAR_COMPETENCE
        );
    }

    private static DefinitionSnapshot militarPad(RitoProcessual rito) {
        return definition(
                rito,
                "Processo administrativo disciplinar militar",
                RamoDireito.MILITAR.name(),
                parties(role("AUTORIDADE_MILITAR", true, false), role("INVESTIGADO", true, true), role("DEFESA", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural ou requerimento defensivo"),
                        doc(TipoDocumento.PORTARIA_INSTAURACAO, true, "Ato de instauração"),
                        doc(TipoDocumento.DESIGNACAO_COMISSAO, true, "Comissão disciplinar competente"),
                        doc(TipoDocumento.ASSENTAMENTOS_FUNCIONAIS, true, "Dados funcionais do militar"),
                        doc(TipoDocumento.LAUDO_PERICIAL, false, "Prova técnica"),
                        doc(TipoDocumento.ATA_ATOS_DISCIPLINARES, false, "Registro dos atos apuratórios")
                ),
                List.of(
                        stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.RECURSAL), List.of(
                                work("DEFESA_PREVIA_PAD_MILITAR", WorkItemType.MANIFESTACAO, "Apresentar defesa prévia disciplinar", TipoUsuario.ADVOGADO, 1, true, 5),
                                work("SANEAMENTO_PAD_MILITAR", WorkItemType.JUNTADA, "Conferir documentos da comissão disciplinar", TipoUsuario.SERVIDOR_FORUM, 2, true, 2),
                                work("DECISAO_AUTORIDADE_DISCIPLINAR", WorkItemType.DECISAO, "Deliberar sobre o PAD militar", TipoUsuario.JUIZ, 1, true, 3)
                        )),
                        stage(FaseProcessual.RECURSAL, List.of(), List.of(
                                work("RECURSO_PAD_MILITAR", WorkItemType.RECURSO, "Interpor recurso administrativo ou judicial cabível", TipoUsuario.ADVOGADO, 1, true, 5),
                                work("ADMISSIBILIDADE_RECURSO_PAD", WorkItemType.DESPACHO, "Examinar admissibilidade recursal", TipoUsuario.JUIZ, 1, true, 2)
                        ))
                ),
                BASE_MILITAR_COMPETENCE
        );
    }

    private static DefinitionSnapshot empresarial(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.EMPRESARIAL.name(),
                parties(role("DEVEDOR", true, true), role("CREDOR", false, true), role("ADMINISTRADOR_JUDICIAL", false, false), role("MINISTERIO_PUBLICO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.BALANCOS_DEMONSTRACOES, true, "Situação econômica da empresa"),
                        doc(TipoDocumento.RELACAO_CREDORES, true, "Quadro de credores"),
                        doc(TipoDocumento.FLUXO_CAIXA, false, "Planejamento econômico"),
                        doc(TipoDocumento.PLANO_RECUPERACAO, rito == RitoProcessual.RECUPERACAO_JUDICIAL || rito == RitoProcessual.RECUPERACAO_EXTRAJUDICIAL, "Plano de soerguimento"),
                        doc(TipoDocumento.CONTRATOS_ESSENCIAIS, false, "Continuidade empresarial")
                ),
                macroStages(rito, true, true, false),
                List.of("vara empresarial", "vara de falencias e recuperacoes", "camara empresarial")
        );
    }

    private static DefinitionSnapshot infancia(RitoProcessual rito) {
        List<PartyRoleSpec> parties = switch (rito) {
            case INFANCIA_JUVENTUDE_INFRACIONAL -> parties(role("ADOLESCENTE", true, true), role("REPRESENTANTE_LEGAL", false, true), role("MINISTERIO_PUBLICO", true, true), role("DEFENSORIA_OU_DEFESA", false, true), role("EQUIPE_TECNICA", false, false));
            case INFANCIA_JUVENTUDE_ADOCAO -> parties(role("PRETENDENTE_ADOTANTE", true, true), role("CRIANCA_ADOLESCENTE", true, true), role("MINISTERIO_PUBLICO", false, true), role("EQUIPE_TECNICA", false, false), role("REPRESENTANTE_LEGAL", false, true));
            case INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR -> parties(role("REQUERENTE", true, true), role("MENOR", true, true), role("REPRESENTANTE_LEGAL", false, true), role("MINISTERIO_PUBLICO", false, true), role("CONSELHO_TUTELAR", false, false));
            default -> parties(role("CRIANCA_ADOLESCENTE", true, true), role("REPRESENTANTE_LEGAL", true, true), role("MINISTERIO_PUBLICO", false, true), role("CONSELHO_TUTELAR", false, false));
        };
        List<DocumentSpec> documents = switch (rito) {
            case INFANCIA_JUVENTUDE_INFRACIONAL -> documents(
                    doc(TipoDocumento.PETICAO_INICIAL, true, "Representação ou peça inaugural"),
                    doc(TipoDocumento.BOLETIM_OU_REPRESENTACAO, true, "Notícia do ato infracional"),
                    doc(TipoDocumento.QUALIFICACAO_ADOLESCENTE, true, "Identificação do adolescente"),
                    doc(TipoDocumento.RELATORIO_SOCIOEDUCATIVO, false, "Subsídio técnico inicial"),
                    doc(TipoDocumento.PROVA_PRE_CONSTITUIDA, false, "Mídias, laudos e documentos mínimos"),
                    doc(TipoDocumento.RESPONSAVEIS_LEGAIS, false, "Dados para intimação e acompanhamento")
            );
            case INFANCIA_JUVENTUDE_ADOCAO -> documents(
                    doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                    doc(TipoDocumento.DOCUMENTOS_PESSOAIS, true, "Qualificação dos envolvidos"),
                    doc(TipoDocumento.HABILITACAO_ADOTANTE, true, "Habilitação ou status do cadastro de adoção"),
                    doc(TipoDocumento.ESTUDO_PSICOSSOCIAL, true, "Estudo psicossocial ou social"),
                    doc(TipoDocumento.CERTIDAO_NASCIMENTO, true, "Identificação civil da criança ou adolescente"),
                    doc(TipoDocumento.RELATORIO_ESTAGIO_CONVIVENCIA, false, "Quando já iniciada a convivência")
            );
            case INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR -> documents(
                    doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                    doc(TipoDocumento.DOCUMENTOS_PESSOAIS, true, "Qualificação das partes"),
                    doc(TipoDocumento.CERTIDAO_NASCIMENTO, true, "Identificação do menor"),
                    doc(TipoDocumento.ESTUDO_SOCIAL, false, "Avaliação protetiva ou sociofamiliar"),
                    doc(TipoDocumento.COMPROVANTE_VINCULO_FAMILIAR, false, "Relação ou responsabilidade invocada"),
                    doc(TipoDocumento.PARECER_CONSELHO_TUTELAR, false, "Apoio técnico protetivo")
            );
            default -> documents(
                    doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                    doc(TipoDocumento.DOCUMENTOS_PESSOAIS, true, "Qualificação dos envolvidos"),
                    doc(TipoDocumento.ESTUDO_SOCIAL, false, "Avaliação técnica psicossocial"),
                    doc(TipoDocumento.PARECER_CONSELHO_TUTELAR, false, "Informações protetivas"),
                    doc(TipoDocumento.RELATORIO_ESCOLAR_MEDICO, false, "Evidências complementares"),
                    doc(TipoDocumento.PROCURACAO, false, "Representação técnica")
            );
        };
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.INFANCIA_JUVENTUDE.name(),
                parties,
                documents,
                macroStages(rito, true, false, false),
                List.of("vara da infancia e juventude", "juizado da infancia", "camara especializada")
        );
    }

    private static DefinitionSnapshot agrario(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.AGRARIO.name(),
                parties(role("AUTOR", true, true), role("REU", true, true), role("INCRA", false, false), role("MINISTERIO_PUBLICO", false, true), role("COMUNIDADE_TRADICIONAL", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.MATRICULA_IMOVEL, true, "Titularidade ou registro do imóvel"),
                        doc(TipoDocumento.MAPA_GEOREFERENCIADO, false, "Delimitação da área"),
                        doc(TipoDocumento.CADEIA_DOMINIAL, false, "Histórico da propriedade"),
                        doc(TipoDocumento.LAUDO_SOCIOAMBIENTAL, false, "Contexto coletivo e territorial"),
                        doc(TipoDocumento.DOCUMENTOS_POSSE, true, "Comprovação possessória")
                ),
                macroStages(rito, true, true, false),
                List.of("vara agraria", "vara civel especializada", "vara federal quando houver ente federal")
        );
    }

    private static DefinitionSnapshot ambiental(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.AMBIENTAL.name(),
                parties(role("AUTOR", true, true), role("REU", true, true), role("MINISTERIO_PUBLICO", false, true), role("ORGAO_AMBIENTAL", false, false)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.PROCURACAO, true, "Representação processual"),
                        doc(TipoDocumento.AUTO_INFRACAO_AMBIENTAL, false, "Contexto administrativo"),
                        doc(TipoDocumento.LAUDO_TECNICO_AMBIENTAL, true, "Demonstração do dano"),
                        doc(TipoDocumento.MAPA_AREA_AFETADA, false, "Delimitação do dano"),
                        doc(TipoDocumento.FOTOGRAFIAS_MIDIAS, false, "Registro visual"),
                        doc(TipoDocumento.PLANO_RECUPERACAO_AREA, false, "Medidas de recomposição")
                ),
                macroStages(rito, true, true, false),
                List.of("vara ambiental", "vara federal ambiental", "camara especializada")
        );
    }

    private static DefinitionSnapshot internacional(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.INTERNACIONAL.name(),
                parties(role("REQUERENTE", true, true), role("REQUERIDO", false, true), role("AUTORIDADE_CENTRAL", false, false), role("MINISTERIO_PUBLICO", false, true)),
                documents(
                        doc(TipoDocumento.PETICAO_INICIAL, true, "Peça inaugural"),
                        doc(TipoDocumento.TRADUCAO_JURAMENTADA, true, "Documentos estrangeiros"),
                        doc(TipoDocumento.DECISAO_ESTRANGEIRA, rito == RitoProcessual.HOMOLOGACAO_SENTENCA_ESTRANGEIRA, "Título estrangeiro"),
                        doc(TipoDocumento.CARTA_ROGATORIA, rito == RitoProcessual.CARTA_ROGATORIA, "Pedido de cooperação"),
                        doc(TipoDocumento.PROVA_AUTENTICIDADE, true, "Validação documental"),
                        doc(TipoDocumento.COMPROVANTE_CITACAO_OU_DEFESA, false, "Garantias do contraditório")
                ),
                macroStages(rito, true, false, false),
                List.of("stj", "autoridade central", "tribunal competente")
        );
    }

    private static DefinitionSnapshot metodosAutocompositivos(RitoProcessual rito) {
        return definition(
                rito,
                humanize(rito.name()),
                RamoDireito.CIVIL.name(),
                parties(role("REQUERENTE", true, true), role("REQUERIDO", true, true), role("MEDIADOR_ARBITRO", false, false)),
                documents(
                        doc(TipoDocumento.REQUERIMENTO_INICIAL, true, "Pedido de instauração"),
                        doc(TipoDocumento.CONVENCAO_ARBITRAGEM_OU_TERMO, false, "Base consensual do procedimento"),
                        doc(TipoDocumento.PROPOSTA_ACORDO, false, "Material negocial"),
                        doc(TipoDocumento.DOCUMENTOS_SUPORTE, false, "Subsídios documentais")
                ),
                List.of(
                        stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.RECURSAL), List.of(
                                work("PROTOCOLO_AUTOCOMPOSICAO", WorkItemType.MANIFESTACAO, "Protocolar requerimento de mediação, conciliação ou arbitragem", TipoUsuario.ADVOGADO, 1, true, 3),
                                work("TRIAGEM_AUTOCOMPOSITIVA", WorkItemType.DILIGENCIA, "Triar admissibilidade do método autocompositivo", TipoUsuario.SERVIDOR_FORUM, 2, true, 2),
                                work("SESSAO_AUTOCOMPOSITIVA", WorkItemType.AUDIENCIA, "Realizar sessão autocompositiva", TipoUsuario.JUIZ, 1, true, 5)
                        )),
                        stage(FaseProcessual.RECURSAL, List.of(), List.of(
                                work("IMPUGNACAO_AJUSTES_AUTOCOMPOSICAO", WorkItemType.MANIFESTACAO, "Apresentar impugnação ou ressalvas ao ajuste", TipoUsuario.ADVOGADO, 1, false, 5)
                        ))
                ),
                BASE_CIVIL_COMPETENCE
        );
    }

}
