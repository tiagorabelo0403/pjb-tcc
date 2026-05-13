package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoEditorBlueprintCatalogService {

    public ResolvedEditorBlueprint resolve(ResolveRequest request) {
        ResolveRequest safe = request == null ? new ResolveRequest(null, null, null, null, null, null, null, null, false, false, false, Map.of()) : request;
        String ramo = normalize(safe.ramoDireito());
        String rito = normalize(safe.ritoProcessual());
        String tipoJustica = normalize(safe.tipoJustica());
        String classeProcessual = normalize(safe.classeProcessual());
        String assuntoTpu = normalize(safe.assuntoTpu());
        String materiaPrincipal = normalize(safe.materiaPrincipal());
        String naturezaJuridica = normalize(safe.naturezaJuridica());
        RamoDireito ramoResolvido = RamoDireito.fromString(ramo);
        String trilha = resolveTrack(ramoResolvido, rito, tipoJustica);
        String familiaProcedimental = resolveProcedureFamily(trilha, ramoResolvido, rito, classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica, tipoJustica, safe.tutelaUrgencia());
        List<SectionDefinition> secoes = buildSections(trilha, safe.tipoUsuario(), safe.petitionDetected(), safe.tutelaUrgencia(), safe.visualIdentity());
        List<Map<String, Object>> blocosEspecializados = buildQuestionBlocks(trilha, familiaProcedimental, ramoResolvido, rito, tipoJustica, classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica, safe.tipoUsuario(), safe.tutelaUrgencia(), safe.contextoConsensual());
        List<Map<String, Object>> modelos = buildPetitionModels(trilha, familiaProcedimental, ramoResolvido, rito, tipoJustica, classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica, safe.tipoUsuario());
        List<String> documentos = buildRequiredDocuments(trilha, familiaProcedimental, ramoResolvido, rito, classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica, safe.tutelaUrgencia(), safe.contextoConsensual());
        LinkedHashMap<String, Object> editor = new LinkedHashMap<>();
        editor.put("surface", "EDITOR_NATIVO_PJB");
        editor.put("version", "2026.04");
        editor.put("editorMode", safe.petitionDetected() ? "UPLOAD_ASSISTIDO_COM_REVISAO" : "ESTRUTURADO_POR_BLOCOS");
        editor.put("resolvedTrack", trilha);
        put(editor, "resolvedRamoDireito", ramoResolvido == null ? ramo : ramoResolvido.name());
        put(editor, "resolvedRitoProcessual", rito);
        put(editor, "resolvedTipoJustica", tipoJustica);
        put(editor, "resolvedProfile", safe.tipoUsuario() == null ? null : safe.tipoUsuario().papelArquitetural());
        put(editor, "resolvedClasseProcessual", classeProcessual);
        put(editor, "resolvedAssuntoTpu", assuntoTpu);
        put(editor, "resolvedMateriaPrincipal", materiaPrincipal);
        put(editor, "resolvedNaturezaJuridica", naturezaJuridica);
        put(editor, "resolvedProcedureFamily", familiaProcedimental);
        editor.put("supportsBranding", true);
        editor.put("supportsUploadBridge", true);
        editor.put("supportsQuestionnaire", true);
        editor.put("supportsAutoRouting", true);
        editor.put("supportsProtocolSeed", true);
        editor.put("supportsStructuredSections", true);
        editor.put("supportsProcedureCatalog", true);
        editor.put("supportsJurisdictionIntakeMatrix", true);
        editor.put("supportsFactsFirstIntake", true);
        editor.put("supportsConditionalJurisdictionQuestions", true);
        editor.put("technicalSelectionOptional", true);
        editor.put("noviceSafeFiling", true);
        editor.put("assistantQuestionMode", "FATOS_PRIMEIRO_COM_PERGUNTAS_CONDICIONAIS");
        editor.put("requiresPartyQualificationFirst", true);
        editor.put("userMayChooseTribunal", false);
        editor.put("userMayChooseForum", false);
        editor.put("userMayChooseJudicialUnit", false);
        editor.put("unitResolutionMode", "MOTOR_COMPETENCIA_E_DISTRIBUICAO");
        editor.put("distributionDisclosure", "O usuário começa pelos fatos do caso; o PJB identifica a justiça, o grau e a unidade competente sem exigir escolha técnica prévia.");
        editor.put("sections", secoes.stream().map(SectionDefinition::toMap).toList());
        editor.put("mandatoryDocuments", List.copyOf(documentos));
        editor.put("brandingPolicy", buildBrandingPolicy(safe.tipoUsuario(), safe.visualIdentity()));
        editor.put("signaturePolicy", buildSignaturePolicy(safe.tipoUsuario(), tipoJustica));
        editor.put("recommendedFlow", buildRecommendedFlow(trilha, familiaProcedimental, safe.petitionDetected(), safe.contextoConsensual()));
        put(editor, "recommendedModelCode", selectRecommendedModelCode(familiaProcedimental));
        editor.put("petitionModels", modelos);
        return new ResolvedEditorBlueprint(Map.copyOf(editor), List.copyOf(blocosEspecializados), List.copyOf(modelos), List.copyOf(documentos));
    }

    private List<SectionDefinition> buildSections(String trilha,
                                                  TipoUsuario tipoUsuario,
                                                  boolean petitionDetected,
                                                  boolean tutelaUrgencia,
                                                  Map<String, Object> visualIdentity) {
        ArrayList<SectionDefinition> sections = new ArrayList<>();
        sections.add(section("CABECALHO_VISUAL", "Cabeçalho visual e identificação", 10, true, false,
                List.of("nomeExibicao", "nomeInstituicao", "brasaoOuLogomarcaUri", "cabecalhoLivre", "rodapeLivre")));
        sections.add(section("QUALIFICACAO_PARTES", "Qualificação das partes", 20, true, true,
                List.of("parteAutora", "parteRe", "qualificacaoComplementar", "representacaoProcessual")));
        sections.add(section("COMPETENCIA_E_PROTOCOLO", "Onde o caso aconteceu e como ele se conecta", 30, true, false,
                List.of("cidadeFato", "ufFato", "referenciaTerritorialOpcional", "naturezaJuridica", "textoFatosResumido", "materiaPrincipal")));
        if (petitionDetected) {
            sections.add(section("LEITURA_ASSISTIDA", "Leitura assistida da peça enviada", 35, false, false,
                    List.of("textoPeticaoLivre", "mapeamentoSecoes", "autopreenchimento", "alertasConversao")));
        }
        sections.add(section("FATOS", "Fatos e cronologia relevante", 40, true, true,
                List.of("textoFatosResumido", "fatos", "contextoTemporal", "contextoTerritorial")));
        sections.add(section("FUNDAMENTOS", "Fundamentos jurídicos e enquadramento", 50, true, true,
                List.of("fundamentosJuridicos", "classeProcessual", "assuntoTpu", "materiaPrincipal")));
        sections.add(section("PEDIDOS", "Pedidos, providências e resultados esperados", 60, true, true,
                List.of("pedidos", "pedidosUrgentes", "pedidoPrincipal", "pedidoSubsidiario")));
        sections.add(section("PROVAS_E_DOCUMENTOS", "Provas, documentos e anexos essenciais", 70, true, true,
                List.of("provasIndicadas", "documentosAnexados", "documentosEssenciais")));
        if (tutelaUrgencia) {
            sections.add(section("URGENTE", "Tutela de urgência e risco processual", 75, true, false,
                    List.of("tutelaUrgencia", "requerLiminar", "perigoDano", "probabilidadeDireito")));
        }
        if (isConsensualTrack(trilha)) {
            sections.add(section("CONSENSUAL", "Sessão consensual, acordo e poderes de transigir", 80, false, false,
                    List.of("contextoConsensual", "poderesEspeciaisTransigir", "propostaAcordo", "historicoNegociacao")));
        }
        sections.add(section("VALOR_E_FECHAMENTO", "Valor, fechamento, assinatura e protocolo", 90, true, false,
                List.of("valorCausa", "encerramento", "assinatura", "prepararPacoteProtocolo")));
        if (tipoUsuario != null && (tipoUsuario.isProcuradoria() || tipoUsuario.isDefensoriaPublica() || tipoUsuario.isMinisterioPublico())) {
            sections.add(section("IDENTIDADE_INSTITUCIONAL", "Identidade institucional e peça padronizada do órgão", 15, false, false,
                    List.of("brasaoOuLogomarcaUri", "cabecalhoLivre", "rodapeLivre", "modeloInstitucional")));
        }
        if (visualIdentity != null && !visualIdentity.isEmpty()) {
            sections.sort((left, right) -> Integer.compare(left.order(), right.order()));
        }
        return List.copyOf(sections);
    }

    private List<Map<String, Object>> buildQuestionBlocks(String trilha,
                                                          String familiaProcedimental,
                                                          RamoDireito ramo,
                                                          String rito,
                                                          String tipoJustica,
                                                          String classeProcessual,
                                                          String assuntoTpu,
                                                          String materiaPrincipal,
                                                          String naturezaJuridica,
                                                          TipoUsuario tipoUsuario,
                                                          boolean tutelaUrgencia,
                                                          boolean contextoConsensual) {
        ArrayList<Map<String, Object>> blocks = new ArrayList<>();
        blocks.add(block("QUALIFICACAO_COMPLETA", "Qualificação detalhada das partes", true,
                List.of(
                        prompt("nomeCompletoOuRazaoSocial", "Confirmar nome completo, CPF/CNPJ e qualificação mínima de cada polo.", true),
                        prompt("enderecoCompleto", "Informar endereço com CEP para autopreenchimento territorial e citação.", true),
                        prompt("representacaoProcessual", "Indicar procuração, instrumento legal, mandato institucional ou dispensa legal.", true)
                )));
        blocks.add(block("LOCAL_FATO_TRIAGEM_ASSISTIDA", "Conexão territorial e triagem assistida de competência", true,
                List.of(
                        prompt("localFato", "Em que cidade e UF o fato ocorreu, onde seus efeitos foram sentidos e qual é o principal ponto de conexão territorial do caso.", true),
                        prompt("referenciaTerritorialOpcional", "Se algum documento já trouxer uma referência do caso, informe aqui. Pode ser zona eleitoral, auditoria, comarca, agência, órgão ou outro local citado. Esse dado é opcional e o PJB não usa isso como escolha livre de unidade.", false),
                        prompt("marcadoresDoCaso", "Explique se o caso envolve eleição, trabalho, crime, militar, benefício, criança, ente público, urgência ou outra especialização. O PJB usa isso para inferir a justiça competente.", true)
                )));
        blocks.addAll(buildBranchBlocks(trilha, ramo, rito, tipoJustica, tutelaUrgencia, contextoConsensual));
        blocks.addAll(buildProcedureSpecificBlocks(familiaProcedimental, ramo, rito, classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica, tutelaUrgencia));
        if (tipoUsuario != null && (tipoUsuario.isAdvocacia() || tipoUsuario.isProcuradoria() || tipoUsuario.isDefensoriaPublica() || tipoUsuario.isMinisterioPublico())) {
            blocks.add(block("IDENTIDADE_PECA", "Identidade visual, assinatura e apresentação da peça", false,
                    List.of(
                            prompt("brasaoOuLogomarca", "Definir se a peça usará brasão, logomarca, cabeçalho institucional ou timbre do escritório.", false),
                            prompt("registroProfissional", "Exibir OAB, matrícula funcional ou identificação institucional no rodapé.", false)
                    )));
        }
        return blocks.stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(item -> String.valueOf(item.get("code")), java.util.function.Function.identity(), (left, right) -> left, java.util.LinkedHashMap::new),
                        map -> List.copyOf(map.values())
                ));
    }

    private List<Map<String, Object>> buildBranchBlocks(String trilha,
                                                        RamoDireito ramo,
                                                        String rito,
                                                        String tipoJustica,
                                                        boolean tutelaUrgencia,
                                                        boolean contextoConsensual) {
        ArrayList<Map<String, Object>> blocks = new ArrayList<>();
        if (ramo == RamoDireito.PENAL || containsAny(trilha, "PENAL", "JURI", "CUSTODIA", "EXECUCAO_PENAL")) {
            blocks.add(block("PENAL_FATO_AUTORIA", "Fato típico, autoria, materialidade e providência criminal", true,
                    List.of(
                            prompt("fatoTipico", "Descrever fato, data, local, autoria, vítima, eventual conexão e enquadramento penal sugerido.", true),
                            prompt("materialidade", "Indicar boletim, laudo, auto de prisão, inquérito, depoimentos ou outras peças de materialidade.", true),
                            prompt("medidaCriminal", "Definir se há prisão, custódia, júri, execução penal, protetiva, interesse federal ou prerrogativa de função.", true)
                    )));
        }
        if (ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE || containsAny(trilha, "FAMILIA", "INFANCIA")) {
            blocks.add(block("FAMILIA_INFANCIA", "Relação familiar, crianças, alimentos, guarda e proteção", true,
                    List.of(
                            prompt("vinculoFamiliar", "Informar vínculo familiar, guarda, alimentos, convivência, adoção ou tutela envolvida.", true),
                            prompt("criancaAdolescente", "Apontar existência de criança ou adolescente e risco atual, se houver.", ramo == RamoDireito.INFANCIA_JUVENTUDE),
                            prompt("medidaProtetiva", "Indicar se o caso exige sigilo, prioridade, estudo psicossocial ou tutela imediata.", false)
                    )));
        }
        if (ramo == RamoDireito.TRABALHISTA || containsAny(trilha, "TRABALHISTA")) {
            blocks.add(block("TRABALHO_CONTRATO_VERBAS", "Contrato de trabalho, jornada, verbas e provas laborais", true,
                    List.of(
                            prompt("contratoTrabalho", "Informar função, salário, início, término, jornada, forma de contratação e local da prestação dos serviços.", true),
                            prompt("verbasPostuladas", "Especificar verbas rescisórias, extras, FGTS, estabilidade, danos, reintegração e se a demanda é individual ou coletiva.", true),
                            prompt("provasLaborais", "Apontar CTPS, holerites, controles de jornada, mensagens, sindicato e testemunhas quando cabível.", true)
                    )));
        }
        if (ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.PREVIDENCIARIO || containsAny(trilha, "FAZENDA", "EXECUCAO_FISCAL", "PREVIDENCIARIO", "CONSTITUCIONAL")) {
            blocks.add(block("FAZENDA_E_ADMINISTRACAO", "Ato administrativo, débito, benefício ou relação com ente público", true,
                    List.of(
                            prompt("entePublico", "Identificar ente público, autoridade, órgão emissor ou unidade administrativa responsável, distinguindo União, autarquia federal, empresa pública federal, estado ou município.", true),
                            prompt("atoImpugnado", "Descrever ato impugnado, débito, inscrição, benefício, omissão ou negativa administrativa.", true),
                            prompt("provaPreconstituida", "Indicar processo administrativo, CDA, CNIS, indeferimento, guia, documento pré-constituído e eventual base federal do caso.", true)
                    )));
        }
        if (ramo == RamoDireito.ELEITORAL || containsAny(trilha, "ELEITORAL")) {
            blocks.add(block("ELEITORAL_ZONA_PLEITO", "Zona, pleito, candidatura, propaganda ou prestação de contas", true,
                    List.of(
                            prompt("zonaEleitoral", "Informar zona, município, pleito, ano eleitoral e período afetado.", true),
                            prompt("atoEleitoral", "Especificar registro, propaganda, AIME, AIJE, AIRC, RCED, contas, captação ilícita ou direito de resposta.", true),
                            prompt("riscoCalendario", "Indicar cargo ou mandato discutido, risco de perecimento pelo calendário eleitoral e eventual necessidade de liminar.", false)
                    )));
        }
        if (ramo == RamoDireito.MILITAR || containsAny(trilha, "MILITAR")) {
            blocks.add(block("MILITAR_HIERARQUIA", "Organização militar, posto, unidade e disciplina", true,
                    List.of(
                            prompt("unidadeMilitar", "Identificar se o caso é da Justiça Militar da União ou Estadual, a corporação, a unidade, o posto/graduação e o comandante relacionado ao caso.", true),
                            prompt("contextoDisciplinarOuPenal", "Dizer se o caso é disciplinar, penal militar, administrativo militar e se o agente é militar ou civil.", true),
                            prompt("documentosMilitares", "Anexar sindicância, IPM, assentamentos, peças disciplinares e informação territorial da auditoria ou circunscrição quando houver.", true)
                    )));
        }
        if (ramo == RamoDireito.AMBIENTAL || ramo == RamoDireito.AGRARIO || ramo == RamoDireito.EMPRESARIAL || ramo == RamoDireito.INTERNACIONAL) {
            blocks.add(block("ESPECIALIZADO_MATERIAL", "Dados materiais e documentos do ramo especializado", true,
                    buildSpecializedPrompts(ramo, tutelaUrgencia)));
        }
        if (containsAny(trilha, "JUIZADO")) {
            blocks.add(block("JUIZADO_LIMITES", "Limites do juizado e simplicidade procedimental", true,
                    List.of(
                            prompt("limiteEconomico", "Conferir compatibilidade do valor, complexidade e prova com a trilha de juizado.", true),
                            prompt("enderecoCitacao", "Indicar endereço simples e suficiente para citação/intimação célere.", true)
                    )));
        }
        if (containsAny(trilha, "FEDERAL")) {
            blocks.add(block("FEDERAL_SECAO_SUBSECAO", "Conexão com órgão, serviço ou interesse federal", true,
                    List.of(
                            prompt("interesseFederal", "Descrever qual órgão, serviço, benefício, autarquia, empresa pública federal, tratado ou outro vínculo com a União aparece no caso.", true),
                            prompt("vinculoTerritorialFederal", "Informar onde o fato ocorreu, onde fica o órgão ou serviço federal relacionado e em qual cidade a pessoa sofreu o impacto ou buscou atendimento. O PJB usa isso para resolver a unidade federal automaticamente.", true)
                    )));
        }
        if (containsAny(trilha, "COLEGIADO", "SEGUNDO_GRAU")) {
            blocks.add(block("COLEGIADO_E_ORGAO_JULGADOR", "Decisão anterior e vínculo com outro processo", true,
                    List.of(
                            prompt("decisaoRecorridaResumo", "Descrever qual decisão está sendo atacada, de qual grau ela veio e se já houve julgamento anterior relacionado.", true),
                            prompt("vinculoProcessualAnterior", "Informar número do processo de origem, recurso anterior conexo ou relatoria conhecida, se souber.", false)
                    )));
        }
        if (contextoConsensual || containsAny(trilha, "CONSENSUAL", "CEJUSC", "MEDIACAO", "CONCILIACAO")) {
            blocks.add(block("CONSENSUAL_E_ACORDO", "Autocomposição, proposta e poderes de negociação", false,
                    List.of(
                            prompt("propostaAcordo", "Registrar se já existe proposta, histórico de negociação e margem de acordo.", false),
                            prompt("poderesNegociacao", "Confirmar poderes específicos para transigir, receber e dar quitação.", false)
                    )));
        }
        return blocks;
    }

    private List<Map<String, Object>> buildSpecializedPrompts(RamoDireito ramo, boolean tutelaUrgencia) {
        if (ramo == RamoDireito.AMBIENTAL) {
            return List.of(
                    prompt("localImpacto", "Identificar área impactada, município, bioma, licenciamento e órgão ambiental envolvido.", true),
                    prompt("danoAmbiental", "Descrever dano, risco de continuidade, nexo e provas técnicas disponíveis.", true),
                    prompt("medidaInibitória", "Informar se há pedido inibitório, reparatório, embargo ou recuperação ambiental.", tutelaUrgencia)
            );
        }
        if (ramo == RamoDireito.AGRARIO) {
            return List.of(
                    prompt("imovelRural", "Indicar imóvel rural, matrícula, posse, confrontações e histórico dominial.", true),
                    prompt("conflitoFundiario", "Descrever conflito possessório, coletivo, desapropriatório ou de uso da terra.", true),
                    prompt("provasPossessorias", "Apontar CAR, matrícula, georreferenciamento, fotos e documentos de posse.", true)
            );
        }
        if (ramo == RamoDireito.EMPRESARIAL) {
            return List.of(
                    prompt("sociedadeOuEmpresa", "Identificar sociedade, CNPJ, tipo societário e documentos societários essenciais.", true),
                    prompt("situacaoEmpresarial", "Descrever crise, insolvência, recuperação, falência, contrato empresarial ou ato societário impugnado.", true),
                    prompt("juizoUniversal", "Informar eventual juízo universal, administrador judicial ou prevenção empresarial.", false)
            );
        }
        if (ramo == RamoDireito.INTERNACIONAL) {
            return List.of(
                    prompt("elementoEstrangeiro", "Indicar país, autoridade estrangeira, parte estrangeira ou ato internacional envolvido.", true),
                    prompt("cooperacaoInternacional", "Especificar carta rogatória, homologação, cooperação direta ou autoridade central.", true),
                    prompt("documentosTraduzidos", "Informar tradução juramentada, apostila ou formalidade internacional exigível.", true)
            );
        }
        return List.of(
                prompt("elementoEspecializado", "Descrever elementos especializados do ramo escolhido.", true)
        );
    }


    private List<Map<String, Object>> buildProcedureSpecificBlocks(String familiaProcedimental,
                                                                   RamoDireito ramo,
                                                                   String rito,
                                                                   String classeProcessual,
                                                                   String assuntoTpu,
                                                                   String materiaPrincipal,
                                                                   String naturezaJuridica,
                                                                   boolean tutelaUrgencia) {
        if (familiaProcedimental == null) {
            return List.of();
        }
        return switch (familiaProcedimental) {
            case "CIVEL_INDENIZATORIA" -> List.of(block("CIVEL_INDENIZACAO_DANO_NEXO", "Dano, nexo causal e reparação", true,
                    List.of(
                            prompt("eventoDanoso", "Descrever o evento danoso, a conduta imputada, o nexo causal e a extensão do dano.", true),
                            prompt("danosReclamados", "Separar danos materiais, morais, estéticos, lucros cessantes ou obrigação compensatória pretendida.", true),
                            prompt("tentativaSolucao", "Informar notificação prévia, tentativa administrativa de solução ou resistência do réu.", false)
                    )));
            case "CIVEL_OBRIGACAO_FAZER" -> List.of(block("CIVEL_OBRIGACAO_CUMPRIMENTO", "Obrigação de fazer, não fazer ou entrega", true,
                    List.of(
                            prompt("vinculoObrigacional", "Indicar o vínculo contratual, legal ou regulatório que gera a obrigação exigida.", true),
                            prompt("inadimplementoEspecifico", "Descrever o descumprimento, a urgência do adimplemento e o resultado prático esperado.", true),
                            prompt("astreintes", "Informar se haverá pedido de multa diária ou tutela específica para compelir o cumprimento.", tutelaUrgencia)
                    )));
            case "CONSUMIDOR_REVISIONAL" -> List.of(block("CONSUMIDOR_CONTRATO_COBRANCA", "Relação de consumo, cláusulas abusivas e cobrança", true,
                    List.of(
                            prompt("produtoServico", "Descrever produto ou serviço, fornecedor responsável e contexto contratual ou fático da relação de consumo.", true),
                            prompt("abusividade", "Indicar cláusulas abusivas, falha do serviço, cobrança indevida ou vício alegado.", true),
                            prompt("protocoloAtendimento", "Informar protocolos, reclamações administrativas, SAC, Procon ou tentativa prévia de resolução.", false)
                    )));
            case "FAMILIA_ALIMENTOS" -> List.of(block("FAMILIA_ALIMENTOS_BINOMIO", "Necessidade, possibilidade e vínculo alimentar", true,
                    List.of(
                            prompt("vinculoAlimentar", "Identificar o vínculo alimentar, a dependência econômica e o contexto familiar da obrigação.", true),
                            prompt("necessidadeAlimentando", "Descrever necessidades do alimentando com despesas essenciais e rotina de subsistência.", true),
                            prompt("capacidadeAlimentante", "Informar renda, padrão de vida, indícios patrimoniais ou resistência do alimentante.", true)
                    )));
            case "FAMILIA_GUARDA_CONVIVENCIA" -> List.of(block("FAMILIA_GUARDA_MELHOR_INTERESSE", "Guarda, convivência e melhor interesse", true,
                    List.of(
                            prompt("rotinaCrianca", "Descrever rotina da criança ou adolescente, referência de cuidado e rede de apoio existente.", true),
                            prompt("conflitoGuarda", "Explicar o conflito de guarda, convivência, risco ou necessidade de organização parental.", true),
                            prompt("medidaProtetivaFamiliar", "Informar se há urgência, histórico de violência, alienação ou necessidade de estudo psicossocial.", false)
                    )));
            case "FAMILIA_DIVORCIO_PARTILHA" -> List.of(block("FAMILIA_DIVORCIO_PATRIMONIO", "Dissolução, regime de bens e partilha", true,
                    List.of(
                            prompt("regimeBens", "Informar casamento ou união estável, regime de bens e data de início e término da convivência.", true),
                            prompt("patrimonioPartilha", "Relacionar bens, dívidas, valores, administração patrimonial e controvérsias sobre partilha.", true),
                            prompt("medidasCorrelatas", "Indicar se há cumulação com alimentos, guarda, nome, uso de imóvel ou tutela provisória.", false)
                    )));
            case "TRABALHISTA_RECLAMACAO" -> List.of(block("TRABALHISTA_CREDITO_JORNADA", "Crédito trabalhista, jornada e verbas", true,
                    List.of(
                            prompt("periodoContratual", "Detalhar admissão, função, salário, jornada, forma de dispensa e mudanças contratuais relevantes.", true),
                            prompt("verbasPretendidas", "Separar verbas principais, reflexos, multas, FGTS, horas extras, estabilidade ou reintegração.", true),
                            prompt("provaJornadaSubordinacao", "Indicar controles, mensagens, testemunhas e elementos de subordinação e jornada.", true)
                    )));
            case "TRABALHISTA_EXECUCAO" -> List.of(block("TRABALHISTA_EXECUCAO_CUMPRIMENTO", "Título executivo e fase de execução trabalhista", true,
                    List.of(
                            prompt("tituloExecutivo", "Informar sentença, acordo, decisão homologatória ou título trabalhista que sustenta a execução.", true),
                            prompt("creditoExequendo", "Apontar memória do crédito, atualização, juros, contribuições e itens já satisfeitos ou pendentes.", true),
                            prompt("meioExecutivo", "Indicar medidas de execução pretendidas, garantia do juízo, bloqueio, pesquisa patrimonial ou expropriação.", true)
                    )));
            case "FAZENDA_EXECUCAO_FISCAL" -> List.of(block("FAZENDA_CDA_PRESCRICAO", "CDA, prescrição e garantia em execução fiscal", true,
                    List.of(
                            prompt("cda", "Informar número da CDA, origem do crédito, inscrição, atualização e ente exequente.", true),
                            prompt("garantiaJuizo", "Descrever garantia, penhora, parcelamento, suspensão, pagamento ou inexistência de bens constritos.", false),
                            prompt("prescricaoFiscal", "Indicar prescrição, nulidade da CDA, ilegitimidade, excesso ou outro fundamento defensivo relevante.", true)
                    )));
            case "CONSTITUCIONAL_MANDADO_SEGURANCA" -> List.of(block("MANDADO_SEGURANCA_AUTORIDADE", "Autoridade coatora, direito líquido e prova pré-constituída", true,
                    List.of(
                            prompt("autoridadeCoatora", "Identificar autoridade coatora, cargo, órgão e ato ou omissão impugnados.", true),
                            prompt("direitoLiquidoCerto", "Descrever o direito líquido e certo alegado e o risco de ineficácia do provimento final.", true),
                            prompt("provaPreconstituidaMs", "Relacionar a prova pré-constituída indispensável e a data da ciência do ato impugnado.", true)
                    )));
            case "PENAL_CUSTODIA" -> List.of(block("CUSTODIA_AUTO_FLAGRANTE", "Flagrante, integridade e medidas cautelares", true,
                    List.of(
                            prompt("autoPrisao", "Informar auto de prisão, horário da captura, autoridade policial e peças já juntadas.", true),
                            prompt("integridadeCustodiado", "Descrever integridade física, exame de corpo de delito, condições de custódia e eventuais ilegalidades.", true),
                            prompt("medidaCautelarAlternativa", "Indicar pedido de relaxamento, liberdade provisória, cautelares diversas ou encaminhamento protetivo.", true)
                    )));
            case "PENAL_JURI" -> List.of(block("JURI_CRIME_DOSO_VIDA", "Crime doloso contra a vida e contexto do júri", true,
                    List.of(
                            prompt("fatoJuri", "Narrar o fato com foco no crime doloso contra a vida, circunstâncias, vítima e autoria.", true),
                            prompt("provaPronuncia", "Indicar peças de materialidade e elementos que sustentam ou afastam a submissão ao júri.", true),
                            prompt("tesesJuri", "Apontar qualificadoras, privilégio, desclassificação, negativa de autoria ou outras teses centrais.", false)
                    )));
            case "PENAL_HABEAS" -> List.of(block("PENAL_HABEAS_CONSTRANGIMENTO", "Constrangimento ilegal e liberdade de locomoção", true,
                    List.of(
                            prompt("autoridadeCoatoraHc", "Indicar autoridade coatora, processo de origem e decisão que gera o constrangimento.", true),
                            prompt("ilegalidadeHc", "Descrever o constrangimento ilegal, excesso de prazo, nulidade ou ausência de fundamentação.", true),
                            prompt("liminarHc", "Informar urgência concreta e necessidade de liminar em favor do paciente.", tutelaUrgencia)
                    )));
            case "PREVIDENCIARIO_BENEFICIO" -> List.of(block("PREVIDENCIARIO_BENEFICIO_REQUISITOS", "Benefício, DER e indeferimento administrativo", true,
                    List.of(
                            prompt("beneficioPretendido", "Identificar benefício, DER, NB, espécie e histórico do requerimento administrativo.", true),
                            prompt("requisitosPrevidenciarios", "Descrever carência, qualidade de segurado, incapacidade, tempo de contribuição ou requisito específico.", true),
                            prompt("provasPrevidenciarias", "Relacionar CNIS, laudos, PPP, vínculos, documentos rurais ou provas médicas pertinentes.", true)
                    )));
            case "ELEITORAL_AIRC" -> List.of(block("ELEITORAL_AIRC_ELEGIBILIDADE", "Registro de candidatura e inelegibilidade", true,
                    List.of(
                            prompt("candidatura", "Identificar candidatura, partido, coligação ou federação e fase do registro.", true),
                            prompt("causaInelegibilidade", "Apontar causa de inelegibilidade, ausência de requisito ou irregularidade do registro.", true),
                            prompt("provaRegistro", "Relacionar documentos do registro, certidões e prova do fato impeditivo ou desabonador.", true)
                    )));
            case "ELEITORAL_AIJE" -> List.of(block("ELEITORAL_AIJE_ABUSO", "Abuso de poder e desequilíbrio do pleito", true,
                    List.of(
                            prompt("abusoPoder", "Descrever abuso econômico, político, uso indevido de meios ou conduta com potencial de afetar a legitimidade do pleito.", true),
                            prompt("provasAbuso", "Indicar provas, mídias, documentos, testemunhas e contexto temporal da irregularidade.", true),
                            prompt("pedidoSancaoEleitoral", "Definir cassação, inelegibilidade, multa ou outras sanções buscadas.", true)
                    )));
            case "ELEITORAL_AIME" -> List.of(block("ELEITORAL_AIME_MANDATO", "Mandato eletivo e prova constitucional do ilícito", true,
                    List.of(
                            prompt("diplomacaoMandato", "Informar mandato questionado, data da diplomação e contexto do pleito.", true),
                            prompt("fundamentoConstitucionalAime", "Indicar abuso, corrupção ou fraude com descrição objetiva dos fatos e sua gravidade.", true),
                            prompt("provaAime", "Relacionar prova documental, testemunhal ou técnica já disponível para a ação de impugnação.", true)
                    )));
            case "ELEITORAL_PROPAGANDA" -> List.of(block("ELEITORAL_PROPAGANDA_ILICITA", "Propaganda irregular e urgência eleitoral", true,
                    List.of(
                            prompt("veiculacao", "Descrever meio de veiculação, data, alcance e conteúdo da propaganda questionada.", true),
                            prompt("ilicitudePropaganda", "Apontar irregularidade, impulsionamento vedado, desinformação ou afronta às regras do pleito.", true),
                            prompt("retiradaUrgente", "Informar se há pedido liminar de retirada, direito de resposta ou multa.", tutelaUrgencia)
                    )));
            default -> List.of();
        };
    }

    private String resolveProcedureFamily(String trilha,
                                          RamoDireito ramo,
                                          String rito,
                                          String classeProcessual,
                                          String assuntoTpu,
                                          String materiaPrincipal,
                                          String naturezaJuridica,
                                          String tipoJustica,
                                          boolean tutelaUrgencia) {
        if (containsAny(rito, "MANDADO_SEGURANCA") || containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "MANDADO_SEGURANCA", "DIREITO_LIQUIDO_E_CERTO")) {
            return "CONSTITUCIONAL_MANDADO_SEGURANCA";
        }
        if (containsAny(rito, "CUSTODIA")) {
            return "PENAL_CUSTODIA";
        }
        if (containsAny(rito, "JURI")) {
            return "PENAL_JURI";
        }
        if (ramo == RamoDireito.PENAL || containsAny(trilha, "PENAL")) {
            if (containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "HABEAS_CORPUS", "LIBERDADE")) {
                return "PENAL_HABEAS";
            }
            return "PENAL_REPRESENTACAO";
        }
        if (ramo == RamoDireito.TRABALHISTA || containsAny(trilha, "TRABALHISTA")) {
            if (containsAny(rito, "EXECUCAO")) {
                return "TRABALHISTA_EXECUCAO";
            }
            return "TRABALHISTA_RECLAMACAO";
        }
        if (ramo == RamoDireito.FAMILIA || containsAny(trilha, "FAMILIA")) {
            if (containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "ALIMENTOS", "ALIMENTAR", "PENSAO")) {
                return "FAMILIA_ALIMENTOS";
            }
            if (containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "GUARDA", "CONVIVENCIA", "VISITAS")) {
                return "FAMILIA_GUARDA_CONVIVENCIA";
            }
            if (containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "DIVORCIO", "PARTILHA", "DISSOLUCAO")) {
                return "FAMILIA_DIVORCIO_PARTILHA";
            }
            return "FAMILIA_PROTECAO_GERAL";
        }
        if (ramo == RamoDireito.CIVIL || ramo == RamoDireito.CONSUMIDOR || containsAny(trilha, "CIVEL", "JUIZADO_CIVEL")) {
            if (ramo == RamoDireito.CONSUMIDOR || containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "CONSUMIDOR", "REVISIONAL", "ABUSIV", "COBRANCA_INDEVIDA")) {
                return "CONSUMIDOR_REVISIONAL";
            }
            if (containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "INDENIZ", "DANO_MORAL", "DANO_MATERIAL", "RESPONSABILIDADE_CIVIL")) {
                return "CIVEL_INDENIZATORIA";
            }
            if (containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "OBRIGACAO_DE_FAZER", "OBRIGACAO_DE_NAO_FAZER", "TUTELA_ESPECIFICA", "ENTREGA_DE_COISA")) {
                return "CIVEL_OBRIGACAO_FAZER";
            }
            return "CIVEL_GERAL";
        }
        if (containsAny(rito, "EXECUCAO_FISCAL") || containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "EXECUCAO_FISCAL", "CDA", "DEBITO_FISCAL")) {
            return "FAZENDA_EXECUCAO_FISCAL";
        }
        if (ramo == RamoDireito.PREVIDENCIARIO || containsAny(trilha, "PREVIDENCIARIO") || containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "BENEFICIO", "INSS", "AUXILIO", "APOSENTADORIA", "BPC", "LOAS", "PENSAO_MORTE", "SALARIO_MATERNIDADE")) {
            return "PREVIDENCIARIO_BENEFICIO";
        }
        if (ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.CONSTITUCIONAL || containsAny(trilha, "FAZENDA", "CONSTITUCIONAL")) {
            return "FAZENDA_CONTROLE_LEGALIDADE";
        }
        if (ramo == RamoDireito.ELEITORAL || containsAny(trilha, "ELEITORAL")) {
            if (containsAnyAcross(new String[]{rito, classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "AIRC", "REGISTRO_CANDIDATURA")) {
                return "ELEITORAL_AIRC";
            }
            if (containsAnyAcross(new String[]{rito, classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "AIJE")) {
                return "ELEITORAL_AIJE";
            }
            if (containsAnyAcross(new String[]{rito, classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "AIME")) {
                return "ELEITORAL_AIME";
            }
            if (containsAnyAcross(new String[]{rito, classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "PROPAGANDA", "DIREITO_RESPOSTA")) {
                return "ELEITORAL_PROPAGANDA";
            }
            return "ELEITORAL_GERAL";
        }
        if (ramo == RamoDireito.EMPRESARIAL || containsAny(trilha, "EMPRESARIAL")) {
            if (containsAny(rito, "RECUPERACAO", "FALENCIA")) {
                return "EMPRESARIAL_RECUPERACAO";
            }
            if (containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "SOCIETAR", "APURACAO_DE_HAVERES", "DISSOLUCAO")) {
                return "EMPRESARIAL_SOCIETARIO";
            }
            return "EMPRESARIAL_CONTRATUAL";
        }
        if (ramo == RamoDireito.AMBIENTAL || containsAny(trilha, "AMBIENTAL")) {
            return tutelaUrgencia ? "AMBIENTAL_INIBITORIA" : "AMBIENTAL_REPARACAO";
        }
        if (ramo == RamoDireito.AGRARIO || containsAny(trilha, "AGRARIO")) {
            return "AGRARIO_POSSE_CONFLITO";
        }
        if (ramo == RamoDireito.INFANCIA_JUVENTUDE || containsAny(trilha, "INFANCIA")) {
            return "INFANCIA_MEDIDA_PROTETIVA";
        }
        if (ramo == RamoDireito.INTERNACIONAL || containsAny(trilha, "INTERNACIONAL") || containsAny(tipoJustica, "FEDERAL") && containsAnyAcross(new String[]{classeProcessual, assuntoTpu, materiaPrincipal, naturezaJuridica}, "COOPERACAO", "CARTA_ROGATORIA", "HOMOLOGACAO_SENTENCA_ESTRANGEIRA")) {
            return "INTERNACIONAL_COOPERACAO";
        }
        if (ramo == RamoDireito.MILITAR || containsAny(trilha, "MILITAR")) {
            return "MILITAR_INICIAL";
        }
        return containsAny(tipoJustica, "FEDERAL") ? "FEDERAL_GERAL" : "BASE_GERAL";
    }

    private String selectRecommendedModelCode(String familiaProcedimental) {
        if (familiaProcedimental == null) {
            return null;
        }
        return switch (familiaProcedimental) {
            case "CIVEL_INDENIZATORIA" -> "CIVEL_INICIAL_INDENIZATORIA";
            case "CIVEL_OBRIGACAO_FAZER" -> "CIVEL_OBRIGACAO_FAZER";
            case "CONSUMIDOR_REVISIONAL" -> "CONSUMIDOR_REVISIONAL_CONTRATUAL";
            case "FAMILIA_ALIMENTOS" -> "FAMILIA_ALIMENTOS";
            case "FAMILIA_GUARDA_CONVIVENCIA" -> "FAMILIA_GUARDA_CONVIVENCIA";
            case "FAMILIA_DIVORCIO_PARTILHA" -> "FAMILIA_DIVORCIO_PARTILHA";
            case "TRABALHISTA_RECLAMACAO" -> "TRABALHISTA_RECLAMACAO";
            case "TRABALHISTA_EXECUCAO" -> "TRABALHISTA_EXECUCAO";
            case "FAZENDA_EXECUCAO_FISCAL" -> "FAZENDA_EXECUCAO_FISCAL";
            case "CONSTITUCIONAL_MANDADO_SEGURANCA" -> "CONSTITUCIONAL_MANDADO_SEGURANCA";
            case "PENAL_CUSTODIA" -> "PENAL_CUSTODIA";
            case "PENAL_JURI" -> "PENAL_JURI";
            case "PENAL_HABEAS" -> "PENAL_HABEAS_CORPUS";
            case "PREVIDENCIARIO_BENEFICIO" -> "PREVIDENCIARIO_BENEFICIO";
            case "ELEITORAL_AIRC" -> "ELEITORAL_AIRC";
            case "ELEITORAL_AIJE" -> "ELEITORAL_AIJE";
            case "ELEITORAL_AIME" -> "ELEITORAL_AIME";
            case "ELEITORAL_PROPAGANDA" -> "ELEITORAL_PROPAGANDA";
            case "EMPRESARIAL_RECUPERACAO" -> "EMPRESARIAL_RECUPERACAO";
            case "EMPRESARIAL_SOCIETARIO" -> "EMPRESARIAL_SOCIETARIO";
            case "AMBIENTAL_REPARACAO" -> "AMBIENTAL_REPARACAO";
            case "AMBIENTAL_INIBITORIA" -> "AMBIENTAL_INIBITORIA";
            case "AGRARIO_POSSE_CONFLITO" -> "AGRARIO_POSSE_CONFLITO";
            case "INFANCIA_MEDIDA_PROTETIVA" -> "INFANCIA_MEDIDA_PROTETIVA";
            case "INTERNACIONAL_COOPERACAO" -> "INTERNACIONAL_COOPERACAO";
            case "MILITAR_INICIAL" -> "MILITAR_INICIAL";
            default -> "INICIAL_PADRAO_ESTRUTURADA";
        };
    }

    private List<Map<String, Object>> prioritizeModels(List<Map<String, Object>> modelos, String familiaProcedimental) {
        String recomendado = selectRecommendedModelCode(familiaProcedimental);
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> modelo : modelos) {
            LinkedHashMap<String, Object> enriched = new LinkedHashMap<>(modelo);
            boolean match = recomendado != null && recomendado.equals(modelo.get("code"));
            enriched.put("recommended", match);
            enriched.put("procedureFamily", familiaProcedimental);
            enriched.put("priority", match ? 100 : 50);
            out.add(Map.copyOf(enriched));
        }
        out.sort((left, right) -> Integer.compare((Integer) right.get("priority"), (Integer) left.get("priority")));
        LinkedHashMap<String, Map<String, Object>> dedup = new LinkedHashMap<>();
        for (Map<String, Object> item : out) {
            dedup.putIfAbsent(String.valueOf(item.get("code")), item);
        }
        return List.copyOf(dedup.values());
    }

    private List<Map<String, Object>> buildPetitionModels(String trilha,
                                                          String familiaProcedimental,
                                                          RamoDireito ramo,
                                                          String rito,
                                                          String tipoJustica,
                                                          String classeProcessual,
                                                          String assuntoTpu,
                                                          String materiaPrincipal,
                                                          String naturezaJuridica,
                                                          TipoUsuario tipoUsuario) {
        ArrayList<Map<String, Object>> modelos = new ArrayList<>();
        modelos.add(model("INICIAL_PADRAO_ESTRUTURADA", "Petição inicial estruturada do PJB", "BASE", "Modelo-base por blocos com cabeçalho, partes, fatos, fundamentos, pedidos e provas."));
        if (ramo == RamoDireito.CIVIL || ramo == RamoDireito.CONSUMIDOR) {
            modelos.add(model("CIVEL_OBRIGACAO_INDENIZACAO", "Obrigação/indenização cível", "CIVEL", "Estrutura pronta para responsabilidade civil, obrigação de fazer, não fazer ou pagar."));
            modelos.add(model("CIVEL_INICIAL_INDENIZATORIA", "Cível: ação indenizatória", "CIVEL", "Modelo com dano, nexo causal, resistência do réu e quantificação dos prejuízos."));
            modelos.add(model("CIVEL_OBRIGACAO_FAZER", "Cível: obrigação de fazer/não fazer", "CIVEL", "Estrutura focada em tutela específica, adimplemento e multa coercitiva."));
            modelos.add(model("CONSUMIDOR_REVISIONAL_CONTRATUAL", "Consumidor: revisional/falha do serviço", "CONSUMIDOR", "Modelo para cláusula abusiva, cobrança indevida, vício e reparação consumerista."));
        }
        if (ramo == RamoDireito.FAMILIA) {
            modelos.add(model("FAMILIA_GUARDA_ALIMENTOS", "Família: guarda, alimentos e convivência", "FAMILIA", "Blocos próprios para crianças, guarda, convivência, alimentos e urgência familiar."));
            modelos.add(model("FAMILIA_ALIMENTOS", "Família: alimentos", "FAMILIA", "Estrutura orientada para binômio necessidade-possibilidade e urgência alimentar."));
            modelos.add(model("FAMILIA_GUARDA_CONVIVENCIA", "Família: guarda e convivência", "FAMILIA", "Modelo focado em melhor interesse, rotina da criança e organização parental."));
            modelos.add(model("FAMILIA_DIVORCIO_PARTILHA", "Família: divórcio e partilha", "FAMILIA", "Fluxo para dissolução, regime de bens, partilha e pedidos correlatos."));
        }
        if (ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            modelos.add(model("INFANCIA_MEDIDA_PROTETIVA", "Infância e juventude", "INFANCIA", "Fluxo orientado para proteção integral, sigilo e interesse de criança ou adolescente."));
        }
        if (ramo == RamoDireito.PENAL) {
            modelos.add(model(containsAny(trilha, "CUSTODIA") ? "PENAL_CUSTODIA" : "PENAL_REPRESENTACAO", containsAny(trilha, "CUSTODIA") ? "Penal: custódia" : "Penal: representação/peça criminal", "PENAL", "Estrutura com fato típico, autoria, materialidade, urgência penal e medidas requeridas."));
            modelos.add(model("PENAL_JURI", "Penal: tribunal do júri", "PENAL", "Modelo específico para crime doloso contra a vida, autoria, materialidade e teses do júri."));
            modelos.add(model("PENAL_HABEAS_CORPUS", "Penal: habeas corpus", "PENAL", "Estrutura para constrangimento ilegal, autoridade coatora, liminar e liberdade de locomoção."));
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            modelos.add(model("TRABALHISTA_RECLAMACAO", "Trabalhista: reclamação inicial", "TRABALHISTA", "Fluxo para contrato de trabalho, jornada, verbas, reflexos e provas laborais."));
            modelos.add(model("TRABALHISTA_EXECUCAO", "Trabalhista: execução", "TRABALHISTA", "Modelo para título executivo, memória do crédito e atos executivos trabalhistas."));
        }
        if (ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.PREVIDENCIARIO || ramo == RamoDireito.CONSTITUCIONAL) {
            modelos.add(model("FAZENDA_AUTORIDADE_PUBLICA", "Fazenda/administrativo/benefício", "PUBLICO", "Modelo para ato administrativo, benefício, débito, mandamental ou controle de legalidade."));
            modelos.add(model("FAZENDA_EXECUCAO_FISCAL", "Fazenda: execução fiscal/embargos", "PUBLICO", "Estrutura para CDA, prescrição, garantia e defesa em cobrança fiscal."));
            modelos.add(model("CONSTITUCIONAL_MANDADO_SEGURANCA", "Constitucional: mandado de segurança", "CONSTITUCIONAL", "Modelo com autoridade coatora, direito líquido e prova pré-constituída."));
            modelos.add(model("PREVIDENCIARIO_BENEFICIO", "Previdenciário: concessão/revisão de benefício", "PREVIDENCIARIO", "Fluxo para benefício, DER, CNIS, indeferimento e requisitos legais."));
        }
        if (ramo == RamoDireito.ELEITORAL) {
            modelos.add(model("ELEITORAL_REPRESENTACAO", "Eleitoral: representação inicial", "ELEITORAL", "Estrutura com zona, pleito, propaganda, candidatura ou contas."));
            modelos.add(model("ELEITORAL_AIRC", "Eleitoral: AIRC", "ELEITORAL", "Modelo para impugnação de registro de candidatura e causas de inelegibilidade."));
            modelos.add(model("ELEITORAL_AIJE", "Eleitoral: AIJE", "ELEITORAL", "Fluxo para abuso de poder, provas e sanções eleitorais."));
            modelos.add(model("ELEITORAL_AIME", "Eleitoral: AIME", "ELEITORAL", "Estrutura para mandato eletivo, fraude, corrupção ou abuso com prova adequada."));
            modelos.add(model("ELEITORAL_PROPAGANDA", "Eleitoral: propaganda/direito de resposta", "ELEITORAL", "Modelo para propaganda irregular, retirada urgente e tutela eleitoral."));
        }
        if (ramo == RamoDireito.MILITAR) {
            modelos.add(model("MILITAR_INICIAL", "Militar: peça inicial", "MILITAR", "Modelo para contexto disciplinar ou penal militar, com unidade, posto e cadeia funcional."));
        }
        if (ramo == RamoDireito.EMPRESARIAL) {
            modelos.add(model("EMPRESARIAL_RECUPERACAO", "Empresarial/recuperacional", "EMPRESARIAL", "Fluxo voltado para empresa, crise, documentos societários e juízo universal."));
            modelos.add(model("EMPRESARIAL_SOCIETARIO", "Empresarial: societário e haveres", "EMPRESARIAL", "Modelo para dissolução, conflito societário, administração e apuração de haveres."));
        }
        if (ramo == RamoDireito.AMBIENTAL) {
            modelos.add(model("AMBIENTAL_REPARACAO", "Ambiental: tutela/reparação", "AMBIENTAL", "Modelo para dano ambiental, órgão competente, prova técnica e pedido reparatório."));
            modelos.add(model("AMBIENTAL_INIBITORIA", "Ambiental: tutela inibitória", "AMBIENTAL", "Estrutura para cessação imediata do dano e medidas liminares ambientais."));
        }
        if (ramo == RamoDireito.AGRARIO) {
            modelos.add(model("AGRARIO_POSSE_CONFLITO", "Agrário/possessório rural", "AGRARIO", "Estrutura para terra, posse, desapropriação, matrícula e conflito fundiário."));
        }
        if (ramo == RamoDireito.INTERNACIONAL) {
            modelos.add(model("INTERNACIONAL_COOPERACAO", "Internacional: cooperação e elemento estrangeiro", "INTERNACIONAL", "Modelo para cooperação jurídica, homologação, carta rogatória ou autoridade central."));
        }
        if (tipoUsuario != null && (tipoUsuario.isProcuradoria() || tipoUsuario.isDefensoriaPublica() || tipoUsuario.isMinisterioPublico())) {
            modelos.add(model("MODELO_INSTITUCIONAL_PADRONIZADO", "Modelo institucional com brasão e padronização do órgão", "IDENTIDADE", "Estrutura para órgãos públicos com timbre, rodapé funcional e assinatura institucional."));
        }
        if (containsAny(tipoJustica, "FEDERAL")) {
            modelos.add(model("FEDERAL_SECAO_SUBSECAO", "Modelo federal guiado por fatos e conexão", "FEDERAL", "Estrutura focada em órgão, serviço ou interesse federal, local do fato e conexão territorial relevante."));
        }
        if (classeProcessual != null || assuntoTpu != null || materiaPrincipal != null || naturezaJuridica != null || familiaProcedimental != null) {
            modelos.add(model("GUIA_PROCEDIMENTO_ESPECIALIZADO", "Guia PJB do procedimento detectado", "INTAKE", "Modelo-guia para revisar o encaixe entre classe, assunto, matéria, natureza e rito antes do protocolo."));
        }
        return prioritizeModels(modelos, familiaProcedimental);
    }

    private List<String> buildRequiredDocuments(String trilha,
                                                String familiaProcedimental,
                                                RamoDireito ramo,
                                                String rito,
                                                String classeProcessual,
                                                String assuntoTpu,
                                                String materiaPrincipal,
                                                String naturezaJuridica,
                                                boolean tutelaUrgencia,
                                                boolean contextoConsensual) {
        LinkedHashSet<String> documentos = new LinkedHashSet<>();
        documentos.add("Documento principal da petição inicial ou peça-base convertida para editor do PJB.");
        documentos.add("Documentos de qualificação das partes e instrumento de representação, quando cabível.");
        documentos.add("Documentos probatórios mínimos relacionados aos fatos narrados.");
        documentos.add("Documento que mostre onde o caso aconteceu ou a qual local, órgão, serviço, pleito, unidade ou atendimento ele se conecta.");
        if (tutelaUrgencia) {
            documentos.add("Documentos que demonstrem risco concreto, perigo de dano ou necessidade de medida urgente.");
        }
        if (ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            documentos.add("Documentos de vínculo familiar, guarda, alimentos, nascimento, escola, saúde ou risco social, conforme o caso.");
        }
        if (ramo == RamoDireito.PENAL) {
            documentos.add("Boletim, auto, laudo, depoimentos, peças informativas ou documentos de materialidade/autoria disponíveis.");
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            documentos.add("CTPS, contracheques, cartões de ponto, mensagens, TRCT ou outros documentos laborais úteis.");
        }
        if (ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.PREVIDENCIARIO || ramo == RamoDireito.CONSTITUCIONAL) {
            documentos.add("Ato administrativo, decisão, CDA, processo administrativo, CNIS ou documento pré-constituído pertinente.");
        }
        if (ramo == RamoDireito.ELEITORAL) {
            documentos.add("Documento eleitoral, prova de propaganda, candidatura, contas ou calendário processual relacionado ao pleito.");
        }
        if (ramo == RamoDireito.MILITAR) {
            documentos.add("Peças disciplinares, sindicância, IPM, assentamentos ou documentos da cadeia funcional militar.");
        }
        if (ramo == RamoDireito.EMPRESARIAL) {
            documentos.add("Contrato social, alterações, balanços, procurações societárias e documentos empresariais essenciais.");
        }
        if (ramo == RamoDireito.AMBIENTAL) {
            documentos.add("Autos, laudos, licenças, notificações, imagens, coordenadas e prova técnica ambiental disponível.");
        }
        if (ramo == RamoDireito.AGRARIO) {
            documentos.add("Matrícula, CAR, georreferenciamento, documentos possessórios, contratos agrários ou prova fundiária pertinente.");
        }
        if (ramo == RamoDireito.INTERNACIONAL) {
            documentos.add("Documentos estrangeiros, traduções, apostilas, prova de cooperação ou ato internacional relacionado.");
        }
        if (containsAny(trilha, "JUIZADO")) {
            documentos.add("Provas simples e adequadas à tramitação do juizado, evitando complexidade incompatível.");
        }
        if (containsAny(rito, "MANDADO_SEGURANCA", "HABEAS", "AIME", "AIJE", "AIRC")) {
            documentos.add("Prova pré-constituída compatível com a natureza mandamental ou rito especial escolhido.");
        }
        if (contextoConsensual) {
            documentos.add("Histórico de negociação, proposta de acordo ou indicação de poderes para transigir, se existentes.");
        }
        if (familiaProcedimental != null) {
            switch (familiaProcedimental) {
                case "CIVEL_INDENIZATORIA" -> documentos.add("Orçamentos, notas, mensagens, laudos, fotos e prova do dano material ou moral alegado.");
                case "CIVEL_OBRIGACAO_FAZER" -> documentos.add("Contrato, regulação aplicável, notificação prévia e prova do inadimplemento da obrigação específica.");
                case "CONSUMIDOR_REVISIONAL" -> documentos.add("Contrato, faturas, protocolos, reclamações administrativas e histórico de cobrança ou falha do serviço.");
                case "FAMILIA_ALIMENTOS" -> documentos.add("Comprovantes de renda, despesas essenciais, certidão de nascimento e prova do vínculo alimentar.");
                case "FAMILIA_GUARDA_CONVIVENCIA" -> documentos.add("Documentos escolares, médicos, mensagens e elementos sobre rotina, convivência e rede de apoio da criança.");
                case "FAMILIA_DIVORCIO_PARTILHA" -> documentos.add("Certidão de casamento/união estável, pacto antenupcial, documentos de bens, dívidas e comprovantes patrimoniais.");
                case "TRABALHISTA_RECLAMACAO" -> documentos.add("TRCT, comprovantes salariais, controles de jornada e comunicações do contrato de trabalho.");
                case "TRABALHISTA_EXECUCAO" -> documentos.add("Sentença, acordo homologado, cálculos, planilhas atualizadas e demonstrativos do crédito trabalhista.");
                case "FAZENDA_EXECUCAO_FISCAL" -> documentos.add("CDA, demonstrativo do débito, certidão de inscrição e documentos de garantia, penhora ou prescrição.");
                case "CONSTITUCIONAL_MANDADO_SEGURANCA" -> documentos.add("Cópia integral do ato coator, prova da ciência e documentos pré-constituídos do direito alegado.");
                case "PENAL_CUSTODIA" -> documentos.add("Auto de prisão em flagrante, exame de corpo de delito e certidão de apresentação em audiência de custódia.");
                case "PENAL_JURI" -> documentos.add("Laudos, depoimentos, pronúncia ou peças que demonstrem autoria e materialidade em crime doloso contra a vida.");
                case "PENAL_HABEAS" -> documentos.add("Decisão impugnada, certidão do processo, peças essenciais e prova do constrangimento ilegal.");
                case "PREVIDENCIARIO_BENEFICIO" -> documentos.add("CNIS, indeferimento administrativo, laudos médicos, PPP e documentos contributivos do benefício pretendido.");
                case "ELEITORAL_AIRC" -> documentos.add("Registro de candidatura, certidões e prova documental da causa de inelegibilidade ou irregularidade.");
                case "ELEITORAL_AIJE" -> documentos.add("Mídias, relatórios, notas, eventos, testemunhos e prova do abuso de poder alegado.");
                case "ELEITORAL_AIME" -> documentos.add("Provas robustas de fraude, corrupção ou abuso e documentos do mandato impugnado.");
                case "ELEITORAL_PROPAGANDA" -> documentos.add("Capturas de tela, links, mídia, ata notarial ou outros elementos da propaganda irregular.");
                case "EMPRESARIAL_RECUPERACAO" -> documentos.add("Balanços, relação de credores, documentos societários e elementos sobre crise econômico-financeira.");
                case "EMPRESARIAL_SOCIETARIO" -> documentos.add("Contrato social, atas, demonstrativos e documentos da controvérsia societária ou haveres.");
                case "AMBIENTAL_REPARACAO", "AMBIENTAL_INIBITORIA" -> documentos.add("Laudos, autos de infração, fotos, coordenadas, licenças e pareceres técnicos ambientais.");
                case "AGRARIO_POSSE_CONFLITO" -> documentos.add("Matrícula, CAR, mapas, georreferenciamento e prova possessória/fundiária do imóvel rural.");
                case "INFANCIA_MEDIDA_PROTETIVA" -> documentos.add("Relatórios da rede protetiva, certidões, documentos da criança/adolescente e prova do risco atual.");
                case "INTERNACIONAL_COOPERACAO" -> documentos.add("Documentos estrangeiros, traduções juramentadas, apostila, formulários e atos de cooperação internacional.");
                case "MILITAR_INICIAL" -> documentos.add("IPM, sindicância, boletim interno, ato disciplinar e documentos funcionais militares.");
                default -> {
                }
            }
        }
        if (classeProcessual != null || assuntoTpu != null || materiaPrincipal != null || naturezaJuridica != null) {
            documentos.add("Revisar aderência entre classe, assunto TPU, matéria principal e natureza jurídica antes do protocolo.");
        }
        return List.copyOf(documentos);
    }

    private Map<String, Object> buildBrandingPolicy(TipoUsuario tipoUsuario, Map<String, Object> visualIdentity) {
        LinkedHashMap<String, Object> branding = new LinkedHashMap<>();
        branding.put("allowed", true);
        branding.put("supportsBrasaoOuLogomarca", true);
        branding.put("supportsCabecalhoLivre", true);
        branding.put("supportsRodapeLivre", true);
        branding.put("supportsInstitutionalPalette", true);
        put(branding, "actorKind", tipoUsuario == null ? "EXTERNO" : tipoUsuario.papelArquitetural());
        branding.put("preferredMode", tipoUsuario != null && (tipoUsuario.isProcuradoria() || tipoUsuario.isDefensoriaPublica() || tipoUsuario.isMinisterioPublico()) ? "INSTITUCIONAL_PADRONIZADO" : "LIVRE_COM_GOVERNANCA");
        if (visualIdentity != null && !visualIdentity.isEmpty()) {
            branding.put("preset", Map.copyOf(visualIdentity));
        }
        return Map.copyOf(branding);
    }

    private Map<String, Object> buildSignaturePolicy(TipoUsuario tipoUsuario, String tipoJustica) {
        LinkedHashMap<String, Object> signature = new LinkedHashMap<>();
        signature.put("requiresStructuredSignature", true);
        signature.put("supportsCertificateChain", true);
        signature.put("supportsInstitutionalIdentity", true);
        signature.put("supportsProfessionalRegistryFooter", tipoUsuario != null && (tipoUsuario.isAdvocacia() || tipoUsuario.isDefensoriaPublica() || tipoUsuario.isProcuradoria() || tipoUsuario.isMinisterioPublico()));
        put(signature, "justiceSegment", tipoJustica);
        signature.put("defaultActorLabel", tipoUsuario == null ? "PETICIONANTE" : tipoUsuario.papelArquitetural());
        return Map.copyOf(signature);
    }

    private List<String> buildRecommendedFlow(String trilha, String familiaProcedimental, boolean petitionDetected, boolean contextoConsensual) {
        ArrayList<String> flow = new ArrayList<>();
        flow.add(petitionDetected ? "Ler a peça anexada e revisar a conversão para blocos do editor." : "Preencher primeiro as partes, os fatos centrais e os marcadores materiais do caso.");
        flow.add("Confirmar onde o fato ocorreu, quais conexões territoriais existem e deixar que o PJB feche justiça, ramo, rito e unidade competente.");
        flow.add("Consolidar fatos, fundamentos, pedidos, provas e documentos essenciais antes do preflight.");
        if (containsAny(trilha, "PENAL", "CUSTODIA", "JURI")) {
            flow.add("Validar urgência criminal, materialidade, autoria e unidade especializada antes do protocolo.");
        }
        if (containsAny(trilha, "FAZENDA", "EXECUCAO_FISCAL", "PREVIDENCIARIO", "CONSTITUCIONAL")) {
            flow.add("Confirmar autoridade, ente público e prova pré-constituída para reduzir erro de distribuição.");
        }
        if (containsAny(trilha, "FEDERAL")) {
            flow.add("Conferir o vínculo com órgão ou serviço federal e deixar que o PJB resolva a unidade federal adequada.");
        }
        if (familiaProcedimental != null) {
            switch (familiaProcedimental) {
                case "FAMILIA_ALIMENTOS" -> flow.add("Validar binômio necessidade-possibilidade, documentos de vínculo e eventual pedido liminar alimentar.");
                case "FAMILIA_GUARDA_CONVIVENCIA" -> flow.add("Conferir melhor interesse da criança, rotina de convivência e necessidade de estudo psicossocial.");
                case "TRABALHISTA_RECLAMACAO" -> flow.add("Separar contrato, jornada, verbas e reflexos para reduzir retrabalho na triagem trabalhista.");
                case "TRABALHISTA_EXECUCAO" -> flow.add("Anexar título executivo e memória atualizada do crédito antes de acionar atos executivos.");
                case "FAZENDA_EXECUCAO_FISCAL" -> flow.add("Conferir CDA, prescrição e garantia antes de consolidar o protocolo fazendário/executivo.");
                case "CONSTITUCIONAL_MANDADO_SEGURANCA" -> flow.add("Checar prova pré-constituída, autoridade coatora e prazo decadencial do mandado de segurança.");
                case "PENAL_CUSTODIA" -> flow.add("Confirmar auto de prisão, integridade do custodiado e pedido de liberdade/cautelares antes do protocolo.");
                case "PENAL_JURI" -> flow.add("Consolidar peças do crime doloso contra a vida e teses centrais do júri antes da distribuição.");
                case "PREVIDENCIARIO_BENEFICIO" -> flow.add("Conferir DER, benefício, indeferimento administrativo e prova previdenciária principal.");
                case "ELEITORAL_AIRC", "ELEITORAL_AIJE", "ELEITORAL_AIME", "ELEITORAL_PROPAGANDA" -> flow.add("Conferir janela temporal do pleito, prova eleitoral e urgência compatível com o calendário.");
                case "CIVEL_INDENIZATORIA", "CIVEL_OBRIGACAO_FAZER", "CONSUMIDOR_REVISIONAL" -> flow.add("Revisar contrato, dano, resistência do réu e adequação do procedimento antes do protocolo cível/consumerista.");
                default -> {
                }
            }
        }
        if (contextoConsensual || isConsensualTrack(trilha)) {
            flow.add("Registrar histórico de negociação, proposta e poderes para autocomposição quando a trilha admitir acordo.");
        }
        flow.add("Gerar seed do protocolo, executar preflight e seguir para assinatura final.");
        return List.copyOf(flow);
    }

    private String resolveTrack(RamoDireito ramo, String rito, String tipoJustica) {
        String normalizedRito = normalize(rito);
        String normalizedJustica = normalize(tipoJustica);
        if (containsAny(normalizedRito, "JUIZADO")) {
            return containsAny(normalizedRito, "FEDERAL") ? "JUIZADO_FEDERAL" : containsAny(normalizedRito, "FAZENDA") ? "JUIZADO_FAZENDA" : containsAny(normalizedRito, "CRIMINAL") ? "JUIZADO_CRIMINAL" : "JUIZADO_CIVEL";
        }
        if (containsAny(normalizedRito, "CUSTODIA")) {
            return "PENAL_CUSTODIA";
        }
        if (containsAny(normalizedRito, "JURI")) {
            return "PENAL_JURI";
        }
        if (containsAny(normalizedRito, "EXECUCAO_PENAL")) {
            return "PENAL_EXECUCAO";
        }
        if (containsAny(normalizedRito, "EXECUCAO_FISCAL")) {
            return "FAZENDA_EXECUCAO_FISCAL";
        }
        if (containsAny(normalizedRito, "TRABALHISTA")) {
            return "TRABALHISTA";
        }
        if (containsAny(normalizedRito, "ELEITORAL")) {
            return "ELEITORAL";
        }
        if (containsAny(normalizedRito, "MILITAR")) {
            return "MILITAR";
        }
        if (containsAny(normalizedRito, "MEDIACAO", "CONCILIACAO", "ARBITRAGEM")) {
            return "CONSENSUAL";
        }
        if (containsAny(normalizedRito, "FALENCIA", "RECUPERACAO")) {
            return "EMPRESARIAL_RECUPERACIONAL";
        }
        if (containsAny(normalizedRito, "CARTA_ROGATORIA", "HOMOLOGACAO_SENTENCA_ESTRANGEIRA", "COOPERACAO_JURIDICA_INTERNACIONAL")) {
            return "INTERNACIONAL";
        }
        if (containsAny(normalizedRito, "INFANCIA")) {
            return "INFANCIA";
        }
        if (containsAny(normalizedRito, "FAMILIA")) {
            return "FAMILIA";
        }
        if (containsAny(normalizedRito, "AMBIENTAL")) {
            return "AMBIENTAL";
        }
        if (containsAny(normalizedRito, "AGRARIO")) {
            return "AGRARIO";
        }
        if (containsAny(normalizedRito, "ESPECIAL", "MANDADO_SEGURANCA", "HABEAS", "ADI", "ADC", "ADPF", "MANDADO_INJUNCAO")) {
            return "CONSTITUCIONAL_ESPECIAL";
        }
        if (containsAny(normalizedJustica, "FEDERAL")) {
            return ramo == RamoDireito.PREVIDENCIARIO ? "FEDERAL_PREVIDENCIARIO" : "FEDERAL";
        }
        if (ramo == null) {
            return "CIVEL_GERAL";
        }
        return switch (ramo) {
            case CIVIL, CONSUMIDOR -> "CIVEL_GERAL";
            case FAMILIA -> "FAMILIA";
            case EMPRESARIAL -> "EMPRESARIAL";
            case PENAL -> "PENAL_GERAL";
            case MILITAR -> "MILITAR";
            case ELEITORAL -> "ELEITORAL";
            case ADMINISTRATIVO, TRIBUTARIO -> "FAZENDA_GERAL";
            case CONSTITUCIONAL -> "CONSTITUCIONAL_ESPECIAL";
            case AMBIENTAL -> "AMBIENTAL";
            case TRABALHISTA -> "TRABALHISTA";
            case PREVIDENCIARIO -> "PREVIDENCIARIO";
            case INFANCIA_JUVENTUDE -> "INFANCIA";
            case AGRARIO -> "AGRARIO";
            case INTERNACIONAL -> "INTERNACIONAL";
            default -> "CIVEL_GERAL";
        };
    }

    private boolean isConsensualTrack(String trilha) {
        return containsAny(trilha, "CONSENSUAL", "CEJUSC", "MEDIACAO", "CONCILIACAO");
    }

    private SectionDefinition section(String code, String label, int order, boolean required, boolean repeatable, List<String> fields) {
        return new SectionDefinition(code, label, order, required, repeatable, List.copyOf(fields));
    }

    private Map<String, Object> block(String code, String label, boolean required, List<Map<String, Object>> prompts) {
        LinkedHashMap<String, Object> block = new LinkedHashMap<>();
        block.put("code", code);
        block.put("label", label);
        block.put("required", required);
        block.put("prompts", List.copyOf(prompts));
        return Map.copyOf(block);
    }

    private Map<String, Object> prompt(String field, String question, boolean required) {
        LinkedHashMap<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("field", field);
        prompt.put("label", promptLabel(field));
        prompt.put("helperText", promptHelper(field));
        prompt.put("question", question);
        prompt.put("required", required);
        prompt.put("plainLanguage", true);
        return Map.copyOf(prompt);
    }

    private String promptLabel(String field) {
        if (field == null || field.isBlank()) {
            return "Pergunta guiada";
        }
        return switch (field) {
            case "nomeCompletoOuRazaoSocial" -> "Quem são as pessoas ou empresas envolvidas?";
            case "enderecoCompleto" -> "Onde cada parte pode ser localizada?";
            case "representacaoProcessual" -> "Quem está representando cada parte?";
            case "localFato" -> "Onde isso aconteceu?";
            case "referenciaTerritorialOpcional" -> "Existe alguma referência do caso em documento ou atendimento?";
            case "marcadoresDoCaso" -> "Que tipo de problema o caso envolve?";
            case "interesseFederal" -> "Qual é a ligação do caso com a União ou com órgão federal?";
            case "vinculoTerritorialFederal" -> "Onde está a conexão federal do caso?";
            case "decisaoRecorridaResumo" -> "Qual decisão anterior está sendo atacada?";
            case "vinculoProcessualAnterior" -> "Existe processo anterior ou número que ajude a localizar o caso?";
            case "zonaEleitoral" -> "Onde foi o pleito ou o fato eleitoral?";
            case "atoEleitoral" -> "Qual foi o problema eleitoral?";
            case "unidadeMilitar" -> "Qual é a corporação, unidade ou contexto militar do caso?";
            case "contextoDisciplinarOuPenal" -> "O caso é disciplinar, penal militar ou administrativo?";
            case "contratoTrabalho" -> "Como era a relação de trabalho?";
            case "verbasPostuladas" -> "O que está sendo pedido no caso trabalhista?";
            case "entePublico" -> "Qual órgão ou ente público está envolvido?";
            default -> humanizeField(field);
        };
    }

    private String promptHelper(String field) {
        if (field == null || field.isBlank()) {
            return "";
        }
        return switch (field) {
            case "referenciaTerritorialOpcional" -> "Esse campo ajuda quando algum documento já traz uma pista do local ou da unidade, mas ele não é obrigatório.";
            case "vinculoTerritorialFederal" -> "Você não precisa saber seção ou subseção. Basta dizer onde está o órgão, onde ocorreu o fato ou onde houve atendimento.";
            case "decisaoRecorridaResumo" -> "Descreva a decisão em linguagem simples. O sistema identifica o grau e a trilha recursal.";
            case "zonaEleitoral" -> "Se não souber a zona, informe município, pleito e ano. O sistema continua guiando.";
            case "unidadeMilitar" -> "Se não souber a auditoria, informe força, corporação, unidade ou contexto funcional.";
            case "contratoTrabalho" -> "Basta informar função, salário, datas principais, jornada e local do trabalho.";
            default -> "";
        };
    }

    private String humanizeField(String field) {
        String normalized = field == null ? "" : field.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ').trim();
        if (normalized.isBlank()) {
            return "Pergunta guiada";
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1) + '?';
    }

    private Map<String, Object> model(String code, String label, String family, String description) {
        LinkedHashMap<String, Object> model = new LinkedHashMap<>();
        model.put("code", code);
        model.put("label", label);
        model.put("family", family);
        model.put("description", description);
        return Map.copyOf(model);
    }

    private boolean containsAnyAcross(String[] values, String... needles) {
        if (values == null || values.length == 0) {
            return false;
        }
        for (String value : values) {
            if (containsAny(value, needles)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String value, String... needles) {
        String normalized = normalize(value);
        if (normalized == null || needles == null || needles.length == 0) {
            return false;
        }
        for (String needle : needles) {
            String candidate = normalize(needle);
            if (candidate != null && normalized.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized
                .toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9_ ]+", "_")
                .replaceAll("_+", "_")
                .replace(' ', '_');
        return normalized;
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }
        target.put(key, value);
    }

    public record ResolveRequest(
            String ramoDireito,
            String ritoProcessual,
            String tipoJustica,
            String classeProcessual,
            String assuntoTpu,
            String materiaPrincipal,
            String naturezaJuridica,
            TipoUsuario tipoUsuario,
            boolean petitionDetected,
            boolean tutelaUrgencia,
            boolean contextoConsensual,
            Map<String, Object> visualIdentity
    ) {
        public ResolveRequest {
            visualIdentity = visualIdentity == null ? Map.of() : Map.copyOf(visualIdentity);
        }
    }

    public record ResolvedEditorBlueprint(
            Map<String, Object> editorBlueprint,
            List<Map<String, Object>> specializedQuestionBlocks,
            List<Map<String, Object>> petitionModels,
            List<String> requiredDocuments
    ) {
        public ResolvedEditorBlueprint {
            editorBlueprint = editorBlueprint == null ? Map.of() : Map.copyOf(editorBlueprint);
            specializedQuestionBlocks = specializedQuestionBlocks == null ? List.of() : List.copyOf(specializedQuestionBlocks);
            petitionModels = petitionModels == null ? List.of() : List.copyOf(petitionModels);
            requiredDocuments = requiredDocuments == null ? List.of() : List.copyOf(requiredDocuments);
        }
    }

    private record SectionDefinition(
            String code,
            String label,
            int order,
            boolean required,
            boolean repeatable,
            List<String> fields
    ) {
        private SectionDefinition {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(label, "label");
            fields = fields == null ? List.of() : List.copyOf(fields);
        }

        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("code", code);
            map.put("label", label);
            map.put("order", order);
            map.put("required", required);
            map.put("repeatable", repeatable);
            map.put("fields", fields);
            return Map.copyOf(map);
        }
    }
}
