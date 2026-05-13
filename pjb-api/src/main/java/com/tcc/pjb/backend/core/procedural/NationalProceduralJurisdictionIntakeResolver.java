package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJurisdictionIntakeResolver {

    private final NationalProceduralJurisdictionIntakeMessages messages;

    public NationalProceduralJurisdictionIntakeResolver(NationalProceduralJurisdictionIntakeMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    public ProceduralJurisdictionIntakeReport resolve(NationalProceduralRoutingMetadataContext context) {
        Objects.requireNonNull(context);
        Map<String, Object> payload = context.payload() == null ? Map.of() : context.payload();
        String rito = normalize(context.ritoSugerido());
        String actionNature = normalize(context.actionNature());
        String actionFamily = normalize(context.actionFamily());
        TipoJustica tipoJustica = context.tipoJustica();
        String corpus = normalize(NationalProceduralRoutingSupport.buildCorpus(payload));

        LinkedHashSet<String> mandatorySignals = new LinkedHashSet<>();
        LinkedHashSet<String> territorialSignals = new LinkedHashSet<>();
        LinkedHashSet<String> institutionalSignals = new LinkedHashSet<>();
        LinkedHashSet<String> distributionRules = new LinkedHashSet<>(messages.baseDistributionRules());
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        LinkedHashMap<String, Object> resolutionPolicy = new LinkedHashMap<>();

        mandatorySignals.addAll(List.of("classeProcessual", "assuntoOuObjetoProcessual", "pedidoPrincipal", "partes"));
        territorialSignals.addAll(List.of("ufBase", "cidadeOuMunicipioBase", "fatoGeradorTerritorial"));
        institutionalSignals.add("motorCompetenciaDistribuicao");

        warnings.add(messages.noviceSafeWarning());
        warnings.add(messages.factsFirstWarning());
        if (manualHintsProvided(payload)) {
            warnings.add(messages.manualHintWarning());
            warnings.add(messages.nonSelectableUnitWarning());
        }

        String branchProfile = "GERAL";
        String filingTier = "PRIMEIRO_GRAU_PADRAO";
        String competenceDefinitionMode = "MOTOR_COMPETENCIA_E_DISTRIBUICAO";
        String territorialAnchorMode = "FATO_GERADOR_E_PARTES";
        String defaultEntryMode = "DISTRIBUICAO_AUTOMATICA";
        String intakeMode = "FATOS_PRIMARIOS_COM_TRIAGEM_ASSISTIDA";
        String questionStrategy = "PERGUNTAS_FATUAIS_PRIMEIRO_E_FECHAMENTO_TECNICO_POSTERIOR";
        boolean firstInstanceDefault = true;
        boolean mayStartAtTribunal = false;

        if (isEleitoral(tipoJustica, rito, actionFamily, corpus)) {
            branchProfile = "ELEITORAL";
            filingTier = "ZONA_ELEITORAL_PADRAO";
            competenceDefinitionMode = "ZONA_TRE_TSE_POR_CARGO_ATO_E_FASE";
            territorialAnchorMode = "ZONA_ELEITORAL_E_MUNICIPIO_DO_PLEITO";
            mayStartAtTribunal = true;
            mandatorySignals.addAll(List.of("anoPleito", "municipioPleito", "zonaEleitoral", "cargoOuMandato", "atoEleitoralImpugnado"));
            territorialSignals.addAll(List.of("municipioPleito", "zonaEleitoral", "ufPleito"));
            institutionalSignals.addAll(List.of("partidoOuFederacao", "candidatoOuMandato", "faseDoCalendarioEleitoral"));
            distributionRules.add(messages.eleitoralDistributionRule());
        } else if (isMilitar(tipoJustica, rito, actionFamily, corpus)) {
            branchProfile = "MILITAR";
            filingTier = tipoJustica == TipoJustica.MILITAR_FEDERAL ? "AUDITORIA_MILITAR_PADRAO" : "AUDITORIA_MILITAR_ESTADUAL_PADRAO";
            competenceDefinitionMode = "JUSTICA_MILITAR_POR_ESCOPO_AGENTE_POSTO_E_FATO";
            territorialAnchorMode = "CIRCUNSCRICAO_AUDITORIA_OU_UNIDADE_MILITAR";
            mayStartAtTribunal = true;
            mandatorySignals.addAll(List.of("escopoJusticaMilitar", "corporacaoOuForca", "unidadeMilitar", "postoOuGraduacao", "condicaoMilitarOuCivil", "naturezaDoFatoMilitar"));
            territorialSignals.addAll(List.of("localDoFatoMilitar", "circunscricaoJudiciariaMilitarOuAuditoria"));
            institutionalSignals.addAll(List.of("procedimentoMilitarOrigem", "cadeiaDeComandoOuAutoridadeCoatora"));
            distributionRules.add(messages.militarDistributionRule());
        } else if (isTrabalhista(tipoJustica, rito, actionFamily, corpus)) {
            branchProfile = "TRABALHISTA";
            filingTier = "VARA_DO_TRABALHO_PADRAO";
            competenceDefinitionMode = "VARA_TRT_TST_POR_LOCAL_DA_PRESTACAO_E_NATUREZA_DA_ACAO";
            territorialAnchorMode = "LOCAL_DA_PRESTACAO_DOS_SERVICOS";
            mayStartAtTribunal = containsAny(rito, "DISSIDIO", "MANDADO_SEGURANCA", "ACAO_RESCISORIA") || containsAny(actionNature, "DISSIDIO", "MANDADO_SEGURANCA");
            mandatorySignals.addAll(List.of("localPrestacaoServicos", "naturezaDoEmpregador", "vinculoDeTrabalhoOuCategoria"));
            territorialSignals.addAll(List.of("municipioDaPrestacao", "ufDaPrestacao", "baseTerritorialSindicalQuandoCabivel"));
            institutionalSignals.addAll(List.of("demandaIndividualOuColetiva", "sindicatoOuCategoriaQuandoCabivel", "entePublicoQuandoCabivel"));
            distributionRules.add(messages.trabalhistaDistributionRule());
        } else if (isPenal(tipoJustica, rito, actionFamily, corpus)) {
            branchProfile = "PENAL";
            filingTier = tipoJustica == TipoJustica.FEDERAL ? "VARA_FEDERAL_CRIMINAL_PADRAO" : "VARA_CRIMINAL_PADRAO";
            competenceDefinitionMode = "COMPETENCIA_PENAL_POR_LOCAL_DO_FATO_MATERIA_E_PRERROGATIVA";
            territorialAnchorMode = "LOCAL_DA_CONSUMACAO_E_PREVENCAO";
            mayStartAtTribunal = containsAny(actionNature, "HABEAS_CORPUS", "REVISAO_CRIMINAL") || containsAny(corpus, "PRERROGATIVA", "FORO POR PRERROGATIVA", "COMPETENCIA ORIGINARIA");
            mandatorySignals.addAll(List.of("localDoFatoPenal", "naturezaDaInfracao", "procedimentoInvestigatorioOuOrigem", "situacaoDoInvestigadoOuReu"));
            territorialSignals.addAll(List.of("municipioDoFato", "ufDoFato", "localDaConsumacaoOuUltimoAto"));
            institutionalSignals.addAll(List.of("interesseFederalOuResidualEstadual", "prerrogativaDeFuncaoQuandoCabivel", "vinculoComInqueritoOuAutoDePrisao"));
            distributionRules.add(messages.penalDistributionRule());
        } else if (tipoJustica == TipoJustica.FEDERAL) {
            branchProfile = "FEDERAL";
            filingTier = "VARA_FEDERAL_PADRAO";
            competenceDefinitionMode = "JUSTICA_FEDERAL_POR_ART_109_E_REGRAS_ESPECIALIZADAS";
            territorialAnchorMode = "SECAO_OU_SUBSECAO_POR_FATO_E_ENTE_FEDERAL";
            mayStartAtTribunal = containsAny(actionNature, "MANDADO_SEGURANCA", "HABEAS_DATA", "CONFLITO") || containsAny(corpus, "COMPETENCIA ORIGINARIA");
            mandatorySignals.addAll(List.of("enteOuInteresseFederal", "baseConstitucionalFederal", "vinculoTerritorialFederal"));
            territorialSignals.addAll(List.of("cidadeOuUFDeConexao", "localDoFatoOuAtendimentoFederal", "orgaoOuServicoFederalRelacionado"));
            institutionalSignals.addAll(List.of("uniaoAutarquiaOuEmpresaPublicaFederal", "tratadoOuServicoFederalQuandoCabivel"));
            distributionRules.add(messages.federalDistributionRule());
        } else {
            branchProfile = "ESTADUAL_COMUM_OU_RESIDUAL";
            filingTier = "PRIMEIRO_GRAU_ESTADUAL_PADRAO";
            competenceDefinitionMode = "JUSTICA_ESTADUAL_RESIDUAL_OU_ORIGINARIA_CONFORME_CONSTITUICAO";
            territorialAnchorMode = "COMARCA_FORO_E_CONEXAO_TERRITORIAL";
            mayStartAtTribunal = containsAny(actionNature, "MANDADO_SEGURANCA", "HABEAS_DATA", "ACAO_RESCISORIA") || containsAny(corpus, "COMPETENCIA ORIGINARIA", "TJ", "TRIBUNAL DE JUSTICA");
            distributionRules.add(messages.estadualDistributionRule());
        }

        List<Map<String, Object>> guidedQuestions = buildGuidedQuestions(branchProfile, mayStartAtTribunal);
        List<Map<String, Object>> ambiguityQuestions = buildAmbiguityQuestions(branchProfile, mayStartAtTribunal);
        resolutionPolicy.put("selectionMode", "FATOS_PRIMEIRO_COM_PERGUNTAS_CONDICIONAIS");
        resolutionPolicy.put("technicalSelectionOptional", true);
        resolutionPolicy.put("manualHintsWeight", "INDICATIVO_NAO_VINCULANTE");
        resolutionPolicy.put("noviceSafe", true);
        resolutionPolicy.put("humanFallback", "TRIAGEM_ASSISTIDA_SE_CONFIANCA_BAIXA_OU_CONFLITO_NORMATIVO");
        resolutionPolicy.put("branchProfile", branchProfile);

        metadata.put("suggestedTribunalCode", context.forumAllocation() != null ? context.forumAllocation().tribunalCodigo() : null);
        metadata.put("suggestedUnitCode", context.forumAllocation() != null ? context.forumAllocation().unidadeJudiciariaCodigo() : null);
        metadata.put("suggestedUnitLabel", context.forumAllocation() != null ? context.forumAllocation().varaSugerida() : null);
        metadata.put("connectorSystem", context.forumAllocation() != null ? context.forumAllocation().connectorSystem() : null);
        metadata.put("distributionAutomatic", context.forumAllocation() != null ? context.forumAllocation().distribuicaoAutomatica() : null);
        metadata.put("sourceHints", sourceHints(payload));
        metadata.put("guidedQuestionCount", guidedQuestions.size());
        metadata.put("ambiguityQuestionCount", ambiguityQuestions.size());
        metadata.put("technicalSelectionOptional", true);
        metadata.put("noviceSafe", true);
        metadata.entrySet().removeIf(entry -> entry.getValue() == null || entry.getKey() == null);

        return new ProceduralJurisdictionIntakeReport(
                Instant.now(),
                branchProfile,
                filingTier,
                competenceDefinitionMode,
                territorialAnchorMode,
                defaultEntryMode,
                intakeMode,
                questionStrategy,
                firstInstanceDefault,
                mayStartAtTribunal,
                false,
                false,
                false,
                manualHintsProvided(payload),
                true,
                true,
                List.copyOf(mandatorySignals),
                List.copyOf(territorialSignals),
                List.copyOf(institutionalSignals),
                List.copyOf(distributionRules),
                List.copyOf(warnings),
                List.copyOf(guidedQuestions),
                List.copyOf(ambiguityQuestions),
                Map.copyOf(resolutionPolicy),
                Collections.unmodifiableMap(metadata)
        );
    }

    private List<Map<String, Object>> buildGuidedQuestions(String branchProfile, boolean mayStartAtTribunal) {
        ArrayList<Map<String, Object>> questions = new ArrayList<>();
        questions.add(question("FATO_CENTRAL", "O que aconteceu?", "Explique, em linguagem simples, qual foi o problema principal e o que você quer que o Judiciário faça.", List.of("pedidoPrincipal", "causaDePedir", "fatosCentrais"), true));
        questions.add(question("LOCAL_CONEXAO", "Onde isso aconteceu?", "Informe cidade, UF e qualquer referência territorial útil do fato, da prestação do serviço, da votação, da unidade militar ou do órgão envolvido.", List.of("cidadeBase", "ufBase", "fatoGeradorTerritorial"), true));
        questions.add(question("PARTES_ENVOLVIDAS", "Quem está envolvido?", "Indique autor, réu, investigado, empregador, ente público, partido, corporação ou autoridade relacionada ao caso.", List.of("partes", "enteOuAutoridade", "qualidadeProcessual"), true));
        questions.add(question("MARCADORES_MATERIAIS", "O caso envolve trabalho, crime, eleição, militar, benefício, criança ou ente público?", "Marque ou descreva os fatos que ajudam o PJB a inferir o ramo correto sem exigir escolha técnica de vara ou tribunal.", List.of("ramoMaterial", "marcadoresDeCompetencia", "naturezaJuridica"), false));
        switch (branchProfile) {
            case "ELEITORAL" -> {
                questions.add(question("ELEITORAL_PLEITO", "Qual eleição e qual cargo estão em jogo?", "Informe ano do pleito, município, zona eleitoral se souber, cargo ou mandato e o ato eleitoral questionado.", List.of("anoPleito", "municipioPleito", "cargoOuMandato", "atoEleitoralImpugnado"), true));
                questions.add(question("ELEITORAL_CALENDARIO", "Há urgência por calendário eleitoral?", "Diga se há propaganda em curso, diplomação, registro, prestação de contas, cassação ou outra etapa com prazo sensível.", List.of("faseDoCalendarioEleitoral", "urgenciaEleitoral"), false));
            }
            case "MILITAR" -> {
                questions.add(question("MILITAR_ESCOPO", "O caso é da Justiça Militar da União ou Estadual?", "Informe força/corporação, posto ou graduação, se a pessoa é militar ou civil e se o fato é penal, disciplinar ou administrativo militar.", List.of("escopoJusticaMilitar", "corporacaoOuForca", "condicaoMilitarOuCivil", "naturezaDoFatoMilitar"), true));
                questions.add(question("MILITAR_TERRITORIO", "Onde ocorreu o fato militar?", "Informe unidade, auditoria, cidade ou circunscrição relacionada ao fato ou à autoridade militar.", List.of("localDoFatoMilitar", "circunscricaoJudiciariaMilitarOuAuditoria"), true));
            }
            case "TRABALHISTA" -> {
                questions.add(question("TRABALHO_PRESTACAO", "Onde o trabalho era prestado e quem era o empregador?", "Informe local da prestação, natureza do empregador, vínculo, categoria e se a demanda é individual ou coletiva.", List.of("localPrestacaoServicos", "naturezaDoEmpregador", "vinculoDeTrabalhoOuCategoria"), true));
                questions.add(question("TRABALHO_ORIGEM", "É ação individual, dissídio coletivo ou caso originário no tribunal?", "Descreva se o caso trata de verbas, greve, dissídio, mandado de segurança, ação rescisória ou outra hipótese especial.", List.of("demandaIndividualOuColetiva", "hipoteseOriginariaTrabalhista"), false));
            }
            case "PENAL" -> {
                questions.add(question("PENAL_FATO", "Qual foi a infração e onde ocorreu?", "Descreva o fato, o local, a natureza da infração, a origem do procedimento investigatório e se há urgência penal.", List.of("localDoFatoPenal", "naturezaDaInfracao", "procedimentoInvestigatorioOuOrigem"), true));
                questions.add(question("PENAL_COMPETENCIA", "Existe interesse federal, júri, violência doméstica ou prerrogativa?", "Explique se há bem, serviço, autoridade federal, crime doloso contra a vida, Maria da Penha, custódia ou foro originário.", List.of("interesseFederalOuResidualEstadual", "prerrogativaDeFuncaoQuandoCabivel", "subsistemaPenal"), false));
            }
            case "FEDERAL" -> {
                questions.add(question("FEDERAL_INTERESSE", "Qual é a ligação do caso com a União ou com órgão federal?", "Informe se há União, autarquia federal, empresa pública federal, tratado, serviço federal, benefício federal ou outra base constitucional do art. 109.", List.of("enteOuInteresseFederal", "baseConstitucionalFederal"), true));
                questions.add(question("FEDERAL_TERRITORIO", "Onde está a conexão federal do caso?", "Diga onde ocorreu o fato, onde fica o órgão ou serviço federal relacionado e em qual cidade a pessoa sofreu o impacto ou buscou atendimento. O PJB resolve a unidade federal automaticamente.", List.of("vinculoTerritorialFederal", "municipioOuUFDeConexao"), false));
            }
            default -> {
                questions.add(question("ESTADUAL_CONEXAO", "Existe ente público, relação de consumo, família, infância ou outro marcador especializado?", "Descreva os elementos materiais do caso para que o PJB diferencie a vara especializada sem exigir escolha manual da unidade.", List.of("marcadoresDeEspecializacao", "entePublicoQuandoCabivel", "subsistemaEspecializado"), false));
            }
        }
        if (mayStartAtTribunal) {
            questions.add(question("ORIGINARIA_TRIBUNAL", "Há indício de competência originária em tribunal?", "Explique se o caso envolve autoridade com prerrogativa, ação originária constitucional, dissídio coletivo, mandado de segurança originário ou outra hipótese legal específica.", List.of("hipoteseOriginaria", "competenciaOriginaria"), false));
        }
        return List.copyOf(questions);
    }

    private List<Map<String, Object>> buildAmbiguityQuestions(String branchProfile, boolean mayStartAtTribunal) {
        ArrayList<Map<String, Object>> questions = new ArrayList<>();
        questions.add(question("AMBIGUIDADE_TERRITORIAL", "Há mais de uma cidade ou local importante para o caso?", "Se o caso toca mais de um lugar, diga onde o fato começou, onde produziu efeito e se já existe processo relacionado.", List.of("multiplaConexaoTerritorial", "processoRelacionado"), false));
        switch (branchProfile) {
            case "ELEITORAL" -> questions.add(question("AMBIGUIDADE_ELEITORAL", "O conflito é sobre propaganda, registro, mandato, contas ou investigação eleitoral?", "Essa distinção ajuda o sistema a separar zona eleitoral, TRE e TSE sem exigir conhecimento técnico do usuário.", List.of("atoEleitoralImpugnado", "classeEleitoralFina"), false));
            case "MILITAR" -> questions.add(question("AMBIGUIDADE_MILITAR", "O fato é penal militar, disciplinar ou administrativo?", "Essa resposta muda a auditoria, o conselho ou a competência originária aplicável.", List.of("naturezaDoFatoMilitar", "procedimentoMilitarOrigem"), false));
            case "TRABALHISTA" -> questions.add(question("AMBIGUIDADE_TRABALHISTA", "O caso é individual, coletivo ou ação originária?", "Essa distinção evita erro entre Vara do Trabalho, TRT e TST.", List.of("demandaIndividualOuColetiva", "hipoteseOriginariaTrabalhista"), false));
            case "PENAL" -> questions.add(question("AMBIGUIDADE_PENAL", "Existe júri, juizado, violência doméstica, custódia ou competência federal?", "Esses marcadores mudam o rito e a unidade sem exigir escolha manual da vara.", List.of("subsistemaPenal", "interesseFederalOuResidualEstadual"), false));
            case "FEDERAL" -> questions.add(question("AMBIGUIDADE_FEDERAL", "O interesse federal é direto, reflexo ou inexistente?", "Essa resposta evita distribuir caso estadual residual como se fosse federal.", List.of("enteOuInteresseFederal", "grauDeAtracaoFederal"), false));
            default -> questions.add(question("AMBIGUIDADE_ESPECIALIZACAO", "Há criança, relação de consumo, massa empresarial, tutela coletiva ou fazenda pública?", "Esses marcadores ajudam a distinguir especialização sem pedir ao usuário que escolha vara.", List.of("marcadoresDeEspecializacao", "entePublicoQuandoCabivel"), false));
        }
        if (mayStartAtTribunal) {
            questions.add(question("AMBIGUIDADE_ORIGINARIA", "A causa começa no primeiro grau ou já nasce em tribunal?", "Se houver dúvida, o PJB faz pergunta adicional ou envia para triagem assistida antes do protocolo final.", List.of("competenciaOriginaria", "grauDeIngresso"), false));
        }
        return List.copyOf(questions);
    }

    private Map<String, Object> question(String code, String label, String question, List<String> expectedSignals, boolean required) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code);
        out.put("label", label);
        out.put("question", question);
        out.put("expectedSignals", expectedSignals == null ? List.of() : List.copyOf(expectedSignals));
        out.put("required", required);
        out.put("technicalSelectionAllowed", false);
        out.put("plainLanguage", true);
        return Collections.unmodifiableMap(out);
    }

    private boolean isEleitoral(TipoJustica tipoJustica, String rito, String actionFamily, String corpus) {
        return tipoJustica == TipoJustica.ELEITORAL || containsAny(rito, "ELEITORAL", "AIRC", "AIJE", "AIME", "RCED") || containsAny(actionFamily, "ELEITORAL") || containsAny(corpus, "ELEICAO", "ELEITORAL", "ZONA ELEITORAL", "TRE", "TSE");
    }

    private boolean isMilitar(TipoJustica tipoJustica, String rito, String actionFamily, String corpus) {
        return tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL || containsAny(rito, "MILITAR", "IPM", "CONSELHO_JUSTICA") || containsAny(actionFamily, "MILITAR") || containsAny(corpus, "AUDITORIA MILITAR", "STM", "TJM", "JUSTICA MILITAR");
    }

    private boolean isTrabalhista(TipoJustica tipoJustica, String rito, String actionFamily, String corpus) {
        return tipoJustica == TipoJustica.TRABALHO || containsAny(rito, "TRABALHISTA", "DISSIDIO") || containsAny(actionFamily, "TRABALHISTA") || containsAny(corpus, "VARA DO TRABALHO", "TRT", "TST", "RELACAO DE TRABALHO");
    }

    private boolean isPenal(TipoJustica tipoJustica, String rito, String actionFamily, String corpus) {
        return containsAny(rito, "PENAL", "CRIMINAL", "JURI", "CUSTODIA", "MARIA_DA_PENHA") || containsAny(actionFamily, "PENAL") || containsAny(corpus, "CRIME", "INQUERITO", "AUTO DE PRISAO", "REU", "DENUNCIA") || tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL && containsAny(rito, "HABEAS", "PENAL");
    }

    private static boolean manualHintsProvided(Map<String, Object> payload) {
        return hasAnyText(payload, "varaPretendida", "tribunalCodigo", "foro", "secaoJudiciaria", "subsecaoJudiciaria", "circunscricao");
    }

    private static Map<String, Object> sourceHints(Map<String, Object> payload) {
        LinkedHashMap<String, Object> hints = new LinkedHashMap<>();
        putIfPresent(hints, "foro", payload.get("foro"));
        putIfPresent(hints, "varaPretendida", payload.get("varaPretendida"));
        putIfPresent(hints, "tribunalCodigo", payload.get("tribunalCodigo"));
        putIfPresent(hints, "secaoJudiciaria", payload.get("secaoJudiciaria"));
        putIfPresent(hints, "subsecaoJudiciaria", payload.get("subsecaoJudiciaria"));
        putIfPresent(hints, "circunscricao", payload.get("circunscricao"));
        return hints.isEmpty() ? Map.of() : Map.copyOf(hints);
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        String text = NationalProceduralRoutingSupport.text(value);
        if (!NationalProceduralRoutingSupport.isBlank(text)) {
            target.put(key, text);
        }
    }

    private static boolean hasAnyText(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) {
            return false;
        }
        for (String key : keys) {
            if (!NationalProceduralRoutingSupport.isBlank(NationalProceduralRoutingSupport.text(payload.get(key)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String value, String... keys) {
        return NationalProceduralRoutingSupport.containsAny(normalize(value), keys);
    }

    private static String normalize(String value) {
        return NationalProceduralRoutingSupport.normalize(value);
    }
}
