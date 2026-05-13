package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CalculatorHelpMessages {

    private static final String GLOBAL_INTRO = "A aba Calculadora do PJB concentra o acesso direto às memórias trabalhista, fazendária, de custas e federal/JEF previdenciária, com versão manual tradicional e versão assistida por IA, resumo fixo na lateral, ajuda contextual e conclusão viva para todos os perfis.";

    private static final List<String> TRABALHISTA_MESSAGES = List.of(
            "Comece pelas datas de admissão e demissão para o sistema calcular avos, competências e trilha temporal do vínculo.",
            "Informe salário-base e parcelas fixas mensais apenas quando elas realmente integrem a remuneração do caso.",
            "Use a jornada mensal padrão de 220 horas quando não houver regime especial ou norma coletiva diferente.",
            "Preencha horas extras 50%, horas extras 100% e intervalo intrajornada em campos separados para evitar mistura de critérios.",
            "Adicional noturno, insalubridade e periculosidade devem ser preenchidos somente quando houver base fática e jurídica no processo.",
            "Ative saldo de salário, 13º, férias e aviso prévio conforme a forma de desligamento e o objeto do pedido.",
            "Reflexos em 13º, férias e FGTS devem permanecer ligados quando houver habitualidade das verbas variáveis.",
            "Em dispensa sem justa causa, mantenha o FGTS mensal e a multa de 40% habilitados salvo estratégia processual diversa.",
            "Multas dos artigos 467 e 477 da CLT só devem ser habilitadas quando o caso concreto realmente comportar a incidência.",
            "Use observações técnicas para registrar premissas periciais, norma coletiva, limitação de pedido ou divergência metodológica."
    );

    private static final List<String> FAZENDA_MESSAGES = List.of(
            "Informe o valor principal, a data de vencimento e a data do cálculo antes de configurar juros, multas e descontos.",
            "A multa de mora diária pode seguir o padrão federal de 0,33% ao dia com teto de 20%, mas o campo permanece parametrizado.",
            "Preencha a série SELIC oficial quando a memória exigir atualização acumulada por competências mensais.",
            "Ative o adicional de 1% no mês do pagamento apenas quando o regime jurídico adotado exigir esse complemento.",
            "Redução de multa, desconto de programa e abatimento por garantia exigem base normativa ou adesão comprovada.",
            "Depósitos, seguro garantia e penhora devem ser lançados como abatimento controlado para não mascarar o saldo real.",
            "Honorários, encargo legal e custas devem ser parametrizados de acordo com a espécie da cobrança e o estágio procedimental.",
            "Registre marcos temporais específicos de juros e observações técnicas quando houver decisão, transação ou ato administrativo próprio."
    );

    private static final List<String> CUSTAS_MESSAGES = List.of(
            "Informe valor da causa, percentual da taxa judiciária e piso mínimo quando a tabela do tribunal exigir projeção com valor mínimo.",
            "Use preparo recursal apenas quando o ato processual realmente exigir preparo autônomo além da taxa principal.",
            "Lance despesas postais, diligências, pesquisas conveniadas, editais e porte em rubricas separadas para facilitar a conferência da guia.",
            "Se o tribunal utilizar unidade de referência local, informe o nome e o valor monetário vigente para manter a memória explicável.",
            "Depósito judicial já realizado deve entrar como abatimento controlado, nunca escondido dentro do total de custas.",
            "A atualização das custas deve permanecer parametrizada para refletir tabela prática, índice do tribunal ou decisão específica do caso."
    );

    private static final List<String> FEDERAL_PREVIDENCIARIO_MESSAGES = List.of(
            "Informe DIB, data do cálculo e renda mensal antes de configurar correção, juros, honorários e classificação do pagamento.",
            "Use a data de ajuizamento quando quiser aplicar corte prescricional quinquenal de forma controlada e auditável.",
            "Lance pagamentos administrativos e tutela em rubricas separadas para impedir duplicidade na conta de atrasados.",
            "Inclua abono anual apenas quando a memória precisar refletir competências vencidas com décimo terceiro previdenciário.",
            "Se houver série oficial de correção, preencha por competência; caso contrário, use fator consolidado apenas como aproximação controlada.",
            "Informe salário mínimo de referência e teto em salários mínimos quando quiser classificar o resultado entre RPV e precatório."
    );

    private static final List<String> GLOBAL_DAILY_BEHAVIOR = List.of(
            "A pessoa entra pela aba Calculadora e escolhe o domínio sem navegar por menus escondidos.",
            "O resumo financeiro fica sempre visível para reduzir retorno de tela e perda de contexto.",
            "As mensagens de ajuda aparecem por seção, sem depender de atendimento externo para compreender o formulário.",
            "O sistema mostra validações inline antes do cálculo final para evitar parâmetros incompatíveis.",
            "A mesma experiência funciona para cidadão, advocacia, magistratura, contador judicial, procuradoria e área técnica institucional."
    );

    private static final List<String> SAFE_AUTOMATION_CAPABILITIES = List.of(
            "preencher valores padrão conservadores quando o campo admitir padrão seguro e reversível",
            "calcular automaticamente avos, competências e dias de aviso a partir das datas informadas",
            "sugerir ativação de FGTS, reflexos e multa rescisória quando a hipótese narrada indicar dispensa sem justa causa",
            "alertar inconsistências temporais, percentuais acima do razoável e campos jurídicos conflitantes",
            "explicar cada campo em linguagem simples sem alterar a fórmula jurídica do motor de cálculo"
    );

    private static final List<String> IA_GUARDRAILS = List.of(
            "A assistência do PJB não altera silenciosamente parâmetros críticos.",
            "Toda automação é reversível e precisa permanecer auditável na memória final.",
            "A IA assistiva pode sugerir, resumir e validar coerência, mas não substitui a decisão jurídica do usuário.",
            "Quando houver risco de erro material, o sistema prioriza bloqueio ou confirmação explícita em vez de autopreenchimento cego."
    );

    private CalculatorHelpMessages() {
    }

    private static CalculoJudicialSolicitantePerfil effectiveProfile(CalculoJudicialSolicitantePerfil perfil) {
        return perfil == null ? CalculoJudicialSolicitantePerfil.CIDADAO : perfil;
    }

    public static String globalIntro() {
        return GLOBAL_INTRO;
    }

    public static List<String> trabalhistaMessages() {
        return TRABALHISTA_MESSAGES;
    }

    public static List<String> fazendaMessages() {
        return FAZENDA_MESSAGES;
    }


    public static List<String> custasMessages() {
        return CUSTAS_MESSAGES;
    }

    public static List<String> federalPrevidenciarioMessages() {
        return FEDERAL_PREVIDENCIARIO_MESSAGES;
    }

    public static List<String> dailyBehavior() {
        return GLOBAL_DAILY_BEHAVIOR;
    }

    public static List<String> safeAutomationCapabilities() {
        return SAFE_AUTOMATION_CAPABILITIES;
    }


    public static List<String> financialIaMessages() {
        return List.of(
                "A IA financeira do PJB não calcula por conta própria fora da calculadora real: ela preenche apenas o que for determinístico e depois chama o domínio correto, mas o usuário sempre pode preferir a versão manual tradicional.",
                "Quando faltar dado essencial, a IA financeira para em modo pendente em vez de inventar salário, principal, DIB, valor da causa ou marco temporal.",
                "A IA financeira pode aplicar defaults prudenciais em jornada padrão, SELIC, teto de multa, teto RPV e campos booleanos reversíveis, sempre registrando o que foi autopreenchido.",
                "O resultado final continua vindo da calculadora oficial do PJB, com trilha auditável, PDF e rotas canônicas do domínio selecionado."
        );
    }


    public static List<String> financialIa2026Methods() {
        return List.of(
                "roteamento tipado por domínio com payload estruturado e schema rígido antes da execução",
                "autopreenchimento determinístico com registro explícito de cada valor inferido",
                "gating por confiança operacional: READY, PENDING_INPUT e BLOCKED",
                "execução sempre delegada ao motor oficial do domínio em vez de cálculo livre pela IA",
                "passo verificador final com trilha de consistência, totais e pontos de confirmação humana",
                "catálogo de capacidades e guardrails exposto ao front para operação previsível e auditável"
        );
    }

    public static List<String> recursalIaMessages() {
        return List.of(
                "A IA de conferência recursal não substitui o motor recursal do PJB: ela organiza a checagem, chama a admissibilidade real e devolve uma conferência guiada.",
                "Ela destaca tempestividade, preparo, preclusão, competência, canal de protocolo e risco operacional antes do protocolo efetivo.",
                "Quando faltar dado essencial ou houver incoerência forte, a IA para em pendência ou bloqueio em vez de mascarar o risco recursal.",
                "O resultado final permanece preso à trilha recursal oficial do PJB, com fundamentos, alertas e metadata auditável."
        );
    }

    public static List<String> iaGuardrails() {
        return IA_GUARDRAILS;
    }

    public static List<String> smartWorkspaceSignals(CalculoJudicialSolicitantePerfil perfil) {
        return effectiveProfile(perfil).citizenLike()
                ? List.of(
                "A tela deve reagir com linguagem curta, clara e segura à medida que o cálculo evolui.",
                "Quando faltar dado crítico, o sistema avisa sem travar toda a experiência desnecessariamente.",
                "Quando tudo estiver coerente, o usuário percebe imediatamente que já pode calcular ou baixar o PDF."
        )
                : List.of(
                "A experiência deve refletir estado operacional real em cada etapa: entrada, validação, cálculo, memória e PDF.",
                "A interface deve separar avisos, bloqueios e automações reversíveis com rastreabilidade explícita.",
                "A conclusão precisa expor badge, ações rápidas, resumo financeiro e trilha auditável sem exigir navegação extra."
        );
    }

    public static List<Map<String, Object>> quickStartJourney(String dominio, CalculoJudicialSolicitantePerfil perfil) {
        if ("TRABALHISTA_CLT".equals(dominio)) {
            return List.of(
                    step("entrada", "Abrir calculadora trabalhista", "Entrar direto no formulário com resumo lateral ativo.", "abrir calculadora"),
                    step("parametros", "Informar vínculo e remuneração", "Preencher datas, salário-base, jornada e verbas essenciais.", "preencher dados iniciais"),
                    step("validacao", "Revisar reflexos e FGTS", effectiveProfile(perfil).citizenLike()
                            ? "O sistema mostra o que vale a pena conferir antes do cálculo final."
                            : "O sistema destaca reflexos, FGTS, multas e pontos de auditoria antes do cálculo final.", "revisar critérios"),
                    step("conclusao", "Gerar memória e PDF", "Ao concluir, a interface acende estado de pronto com ações rápidas.", "abrir memória agora")
            );
        }
        if ("CUSTAS_PROCESSUAIS".equals(dominio)) {
            return List.of(
                    step("entrada", "Abrir calculadora de custas", "Entrar direto no formulário de taxa, preparo, despesas e depósito judicial.", "abrir calculadora"),
                    step("parametros", "Informar taxa e rubricas", "Preencher valor da causa, taxa judiciária, preparo e despesas segregadas.", "preencher dados iniciais"),
                    step("validacao", "Revisar guia e abatimentos", effectiveProfile(perfil).citizenLike()
                            ? "O sistema mostra o que deve ser conferido antes de emitir a memória."
                            : "O sistema evidencia rubricas, depósito vinculado, atualização e pontos de conferência da guia.", "revisar critérios"),
                    step("conclusao", "Gerar memória e PDF", "Ao concluir, a interface acende estado de pronto com ações rápidas.", "abrir memória agora")
            );
        }
        if ("FEDERAL_PREVIDENCIARIO_CJF".equals(dominio)) {
            return List.of(
                    step("entrada", "Abrir calculadora federal/JEF previdenciária", "Entrar direto no formulário de atrasados, abono anual, correção e classificação do pagamento.", "abrir calculadora"),
                    step("parametros", "Informar benefício e marcos temporais", "Preencher DIB, data do cálculo, renda mensal, ajuizamento, citação e abatimentos essenciais.", "preencher dados iniciais"),
                    step("validacao", "Revisar atrasados, compensações e RPV/precatório", effectiveProfile(perfil).citizenLike()
                            ? "O sistema mostra o que deve ser conferido antes de fechar a memória previdenciária."
                            : "O sistema evidencia competências vencidas, abono anual, abatimentos, juros e classificação do pagamento.", "revisar critérios"),
                    step("conclusao", "Gerar memória e PDF", "Ao concluir, a interface acende estado de pronto com ações rápidas.", "abrir memória agora")
            );
        }
        return List.of(
                step("entrada", "Abrir calculadora fazendária", "Entrar direto no formulário com juros, mora e descontos em visão estruturada.", "abrir calculadora"),
                step("parametros", "Informar principal e marcos temporais", "Preencher principal, vencimento, data do cálculo e parâmetros básicos.", "preencher dados iniciais"),
                step("validacao", "Revisar SELIC, multas e abatimentos", effectiveProfile(perfil).citizenLike()
                        ? "O sistema mostra o que precisa ser conferido antes de fechar a memória."
                        : "O sistema evidencia SELIC, mora, descontos, garantias e critérios financeiros para conferência.", "revisar critérios"),
                step("conclusao", "Gerar memória e PDF", "Ao concluir, a interface acende estado de pronto com ações rápidas.", "abrir memória agora")
        );
    }

    public static Map<String, Object> liveComponentDesign(String dominio, CalculoJudicialSolicitantePerfil perfil) {
        String domainLabel = "TRABALHISTA_CLT".equals(dominio) ? "trabalhista" : "CUSTAS_PROCESSUAIS".equals(dominio) ? "de custas" : "FEDERAL_PREVIDENCIARIO_CJF".equals(dominio) ? "federal previdenciária" : "fazendária";
        return linked(
                "stack", List.of("toast", "banner superior", "card de conclusão", "badge de estado", "barra de ações rápidas"),
                "defaultTone", effectiveProfile(perfil).citizenLike() ? "vivo guiado" : "vivo auditável",
                "presence", List.of("microcópias por etapa", "sinal de progresso", "estado pronto visível", "atalho abrir memória agora"),
                "badgeReady", iaBadge(false),
                "badgeIaAssistida", iaBadge(true),
                "heroMessage", effectiveProfile(perfil).citizenLike()
                        ? "A calculadora " + domainLabel + " guia o preenchimento e mostra claramente quando a memória já pode ser aberta."
                        : "A calculadora " + domainLabel + " mantém estado operacional explícito, com conclusão auditável e ações rápidas.",
                "emptyState", emptyState(dominio, perfil),
                "quickStartJourney", quickStartJourney(dominio, perfil)
        );
    }

    public static List<String> completionMessages(String dominio, CalculoJudicialSolicitantePerfil perfil) {
        return effectiveProfile(perfil).citizenLike()
                ? citizenCompletionMessages(dominio)
                : technicalCompletionMessages(dominio);
    }

    public static Map<String, Object> readyNotificationTemplate(String dominio, CalculoJudicialSolicitantePerfil perfil, boolean iaAssistida) {
        String title = iaAssistida
                ? "Cálculo concluído com apoio da IA assistiva"
                : "Cálculo concluído";
        String body = effectiveProfile(perfil).citizenLike()
                ? citizenReadyBody(dominio, iaAssistida)
                : technicalReadyBody(dominio, iaAssistida);
        return linked(
                "status", "READY",
                "canal", iaAssistida ? "ia_assistida" : "manual",
                "titulo", title,
                "mensagem", body,
                "acoes", defaultActions(dominio),
                "badgePrincipal", iaBadge(iaAssistida),
                "badges", completionBadges(dominio, iaAssistida),
                "toast", toastModel(title, body, iaAssistida),
                "banner", bannerModel(dominio, perfil, iaAssistida, null),
                "cardConclusao", completionCard(dominio, perfil, iaAssistida, null),
                "actionBar", actionBar(dominio, iaAssistida),
                "nextSteps", domainNextSteps(dominio, perfil, iaAssistida),
                "pulse", pulseModel(iaAssistida)
        );
    }

    public static Map<String, Object> readyNotificationPayload(String dominio,
                                                               CalculoJudicialSolicitantePerfil perfil,
                                                               BigDecimal totalGeral,
                                                               boolean iaAssistida) {
        String title = iaAssistida ? "A IA terminou a montagem da memória" : "A memória foi gerada com sucesso";
        String body = effectiveProfile(perfil).citizenLike()
                ? citizenResultBody(dominio, totalGeral, iaAssistida)
                : technicalResultBody(dominio, totalGeral, iaAssistida);
        return linked(
                "status", "READY",
                "canal", iaAssistida ? "ia_assistida" : "manual",
                "titulo", title,
                "mensagem", body,
                "totalGeral", CalculoJudicialMetadataSupport.money(totalGeral),
                "acoes", defaultActions(dominio),
                "badgePrincipal", iaBadge(iaAssistida),
                "badges", completionBadges(dominio, iaAssistida),
                "toast", toastModel(title, body, iaAssistida),
                "banner", bannerModel(dominio, perfil, iaAssistida, totalGeral),
                "cardConclusao", completionCard(dominio, perfil, iaAssistida, totalGeral),
                "actionBar", actionBar(dominio, iaAssistida),
                "nextSteps", domainNextSteps(dominio, perfil, iaAssistida),
                "pulse", pulseModel(iaAssistida),
                "highlightStrip", highlightStrip(dominio, perfil, totalGeral, iaAssistida),
                "uiState", linked(
                        "toastOnLoad", Boolean.TRUE,
                        "bannerVisible", Boolean.TRUE,
                        "openSummaryPinned", Boolean.TRUE,
                        "showBadge", Boolean.TRUE,
                        "mode", iaAssistida ? "ready_ia_assistida" : "ready_manual"
                )
        );
    }

    private static List<String> citizenCompletionMessages(String dominio) {
        if ("TRABALHISTA_CLT".equals(dominio)) {
            return List.of(
                    "Quando o cálculo ficar pronto, mostre uma mensagem curta dizendo que a memória trabalhista já pode ser aberta e revisada.",
                    "Se a IA tiver ajudado, destaque que ela só montou a primeira versão e que o usuário ainda pode revisar verbas, reflexos e FGTS.",
                    "Deixe botões claros para abrir a memória, baixar o PDF e conferir os parâmetros usados."
            );
        }
        if ("CUSTAS_PROCESSUAIS".equals(dominio)) {
            return List.of(
                    "Quando o cálculo ficar pronto, mostre uma mensagem curta dizendo que a memória de custas já pode ser aberta e conferida.",
                    "Se a IA tiver ajudado, destaque que ela apenas organizou a primeira versão da taxa, preparo, despesas e depósito vinculado.",
                    "Deixe botões claros para abrir a memória, baixar o PDF e revisar as rubricas da guia."
            );
        }
        if ("FEDERAL_PREVIDENCIARIO_CJF".equals(dominio)) {
            return List.of(
                    "Quando o cálculo ficar pronto, mostre uma mensagem curta dizendo que a memória previdenciária federal já pode ser aberta e conferida.",
                    "Se a IA tiver ajudado, destaque que ela apenas organizou a primeira versão dos atrasados, correção, juros e abatimentos.",
                    "Deixe botões claros para abrir a memória, baixar o PDF e revisar a classificação entre RPV e precatório."
            );
        }
        return List.of(
                "Quando o cálculo ficar pronto, mostre uma mensagem curta dizendo que a memória fazendária já pode ser aberta e revisada.",
                "Se a IA tiver ajudado, destaque que ela apenas organizou a primeira versão e que juros, multas e descontos continuam auditáveis.",
                "Deixe botões claros para abrir a memória, baixar o PDF e conferir os critérios financeiros aplicados."
        );
    }

    private static List<String> technicalCompletionMessages(String dominio) {
        if ("TRABALHISTA_CLT".equals(dominio)) {
            return List.of(
                    "Ao concluir, a interface deve sinalizar que a memória trabalhista está pronta para conferência técnica.",
                    "Quando houver assistência da IA, a mensagem precisa afirmar que os parâmetros críticos permaneceram rastreáveis e reversíveis.",
                    "A conclusão deve oferecer acesso imediato à memória, PDF, trilha de auditoria e revisão das rubricas calculadas."
            );
        }
        if ("CUSTAS_PROCESSUAIS".equals(dominio)) {
            return List.of(
                    "Ao concluir, a interface deve sinalizar que a memória de custas está pronta para conferência técnica e emissão de guia.",
                    "Quando houver assistência da IA, a mensagem precisa afirmar que taxa, preparo, despesas e depósito permaneceram segregados e revisáveis.",
                    "A conclusão deve oferecer acesso imediato à memória, PDF, trilha de auditoria e revisão das rubricas da conta."
            );
        }
        if ("FEDERAL_PREVIDENCIARIO_CJF".equals(dominio)) {
            return List.of(
                    "Ao concluir, a interface deve sinalizar que a memória previdenciária federal está pronta para conferência técnica.",
                    "Quando houver assistência da IA, a mensagem precisa afirmar que corte prescricional, juros, compensações e classificação do pagamento permaneceram rastreáveis e revisáveis.",
                    "A conclusão deve oferecer acesso imediato à memória, PDF, trilha de auditoria e revisão dos critérios dos atrasados."
            );
        }
        return List.of(
                "Ao concluir, a interface deve sinalizar que a memória fazendária está pronta para conferência técnica.",
                "Quando houver assistência da IA, a mensagem precisa afirmar que juros, mora, descontos e garantias permaneceram rastreáveis e reversíveis.",
                "A conclusão deve oferecer acesso imediato à memória, PDF, trilha de auditoria e revisão dos critérios financeiros aplicados."
        );
    }

    private static String citizenReadyBody(String dominio, boolean iaAssistida) {
        if ("TRABALHISTA_CLT".equals(dominio)) {
            return iaAssistida
                    ? "A IA terminou a primeira versão do cálculo trabalhista. Agora você já pode abrir a memória, conferir as verbas e baixar o PDF."
                    : "Seu cálculo trabalhista ficou pronto. Agora você já pode abrir a memória, conferir as verbas e baixar o PDF.";
        }
        if ("CUSTAS_PROCESSUAIS".equals(dominio)) {
            return iaAssistida
                    ? "A IA terminou a primeira versão do cálculo de custas. Agora você já pode abrir a memória, conferir taxa, preparo, despesas e baixar o PDF."
                    : "Seu cálculo de custas ficou pronto. Agora você já pode abrir a memória, conferir taxa, preparo, despesas e baixar o PDF.";
        }
        if ("FEDERAL_PREVIDENCIARIO_CJF".equals(dominio)) {
            return iaAssistida
                    ? "A IA terminou a primeira versão do cálculo previdenciário federal. Agora você já pode abrir a memória, conferir atrasados, abatimentos e baixar o PDF."
                    : "Seu cálculo previdenciário federal ficou pronto. Agora você já pode abrir a memória, conferir atrasados, abatimentos e baixar o PDF.";
        }
        return iaAssistida
                ? "A IA terminou a primeira versão do cálculo fazendário. Agora você já pode abrir a memória, conferir juros, multas e descontos e baixar o PDF."
                : "Seu cálculo fazendário ficou pronto. Agora você já pode abrir a memória, conferir juros, multas e descontos e baixar o PDF.";
    }

    private static String technicalReadyBody(String dominio, boolean iaAssistida) {
        if ("TRABALHISTA_CLT".equals(dominio)) {
            return iaAssistida
                    ? "A memória trabalhista foi concluída com assistência da IA, sem alteração silenciosa de parâmetros críticos, e já está pronta para revisão técnica."
                    : "A memória trabalhista foi concluída e já está pronta para revisão técnica, emissão de PDF e conferência da trilha auditável.";
        }
        if ("CUSTAS_PROCESSUAIS".equals(dominio)) {
            return iaAssistida
                    ? "A memória de custas foi concluída com assistência da IA, mantendo taxa, preparo, despesas e depósito em rubricas rastreáveis."
                    : "A memória de custas foi concluída e já está pronta para revisão técnica, emissão de PDF e conferência da trilha auditável.";
        }
        if ("FEDERAL_PREVIDENCIARIO_CJF".equals(dominio)) {
            return iaAssistida
                    ? "A memória previdenciária federal foi concluída com assistência da IA, mantendo competências, juros, compensações e classificação do pagamento em trilha rastreável."
                    : "A memória previdenciária federal foi concluída e já está pronta para revisão técnica, emissão de PDF e conferência da trilha auditável.";
        }
        return iaAssistida
                ? "A memória fazendária foi concluída com assistência da IA, mantendo rastreabilidade integral de juros, multas, descontos e garantias."
                : "A memória fazendária foi concluída e já está pronta para revisão técnica, emissão de PDF e conferência da trilha auditável.";
    }

    private static String citizenResultBody(String dominio, BigDecimal totalGeral, boolean iaAssistida) {
        String total = CalculoJudicialMetadataSupport.money(totalGeral);
        if ("TRABALHISTA_CLT".equals(dominio)) {
            return iaAssistida
                    ? "A IA terminou a montagem do cálculo trabalhista. O total estimado ficou em " + total + " e a memória já está pronta para revisão e PDF."
                    : "O cálculo trabalhista foi finalizado. O total estimado ficou em " + total + " e a memória já está pronta para revisão e PDF.";
        }
        if ("CUSTAS_PROCESSUAIS".equals(dominio)) {
            return iaAssistida
                    ? "A IA terminou a montagem do cálculo de custas. O total estimado ficou em " + total + " e a memória já está pronta para revisão e PDF."
                    : "O cálculo de custas foi finalizado. O total estimado ficou em " + total + " e a memória já está pronta para revisão e PDF.";
        }
        if ("FEDERAL_PREVIDENCIARIO_CJF".equals(dominio)) {
            return iaAssistida
                    ? "A IA terminou a montagem do cálculo previdenciário federal. O total estimado ficou em " + total + " e a memória já está pronta para revisão e PDF."
                    : "O cálculo previdenciário federal foi finalizado. O total estimado ficou em " + total + " e a memória já está pronta para revisão e PDF.";
        }
        return iaAssistida
                ? "A IA terminou a montagem do cálculo fazendário. O total estimado ficou em " + total + " e a memória já está pronta para revisão e PDF."
                : "O cálculo fazendário foi finalizado. O total estimado ficou em " + total + " e a memória já está pronta para revisão e PDF.";
    }

    private static String technicalResultBody(String dominio, BigDecimal totalGeral, boolean iaAssistida) {
        String total = CalculoJudicialMetadataSupport.money(totalGeral);
        if ("TRABALHISTA_CLT".equals(dominio)) {
            return iaAssistida
                    ? "A memória trabalhista assistida por IA foi concluída com total consolidado de " + total + ", preservando parâmetros críticos revisáveis."
                    : "A memória trabalhista foi concluída com total consolidado de " + total + ", pronta para conferência técnica, PDF e trilha auditável.";
        }
        if ("CUSTAS_PROCESSUAIS".equals(dominio)) {
            return iaAssistida
                    ? "A memória de custas assistida por IA foi concluída com total consolidado de " + total + ", preservando taxa, preparo, despesas e abatimentos revisáveis."
                    : "A memória de custas foi concluída com total consolidado de " + total + ", pronta para conferência técnica, PDF e trilha auditável.";
        }
        if ("FEDERAL_PREVIDENCIARIO_CJF".equals(dominio)) {
            return iaAssistida
                    ? "A memória previdenciária federal assistida por IA foi concluída com total consolidado de " + total + ", preservando competências, juros, compensações e classificação do pagamento revisáveis."
                    : "A memória previdenciária federal foi concluída com total consolidado de " + total + ", pronta para conferência técnica, PDF e trilha auditável.";
        }
        return iaAssistida
                ? "A memória fazendária assistida por IA foi concluída com total consolidado de " + total + ", preservando rastreabilidade integral dos critérios financeiros."
                : "A memória fazendária foi concluída com total consolidado de " + total + ", pronta para conferência técnica, PDF e trilha auditável.";
    }

    private static List<String> defaultActions(String dominio) {
        return "TRABALHISTA_CLT".equals(dominio)
                ? List.of("abrir memória agora", "baixar PDF", "revisar verbas", "ver trilha de auditoria")
                : "CUSTAS_PROCESSUAIS".equals(dominio)
                ? List.of("abrir memória agora", "baixar PDF", "revisar taxa e despesas", "ver trilha de auditoria")
                : "FEDERAL_PREVIDENCIARIO_CJF".equals(dominio)
                ? List.of("abrir memória agora", "baixar PDF", "revisar atrasados e abatimentos", "ver trilha de auditoria")
                : List.of("abrir memória agora", "baixar PDF", "revisar critérios financeiros", "ver trilha de auditoria");
    }

    private static Map<String, Object> toastModel(String title, String body, boolean iaAssistida) {
        return linked(
                "variant", iaAssistida ? "success_assistido" : "success",
                "icon", iaAssistida ? "sparkles-check" : "check-circle",
                "title", title,
                "message", body,
                "autoCloseMs", 6200,
                "position", "top-right"
        );
    }

    private static Map<String, Object> bannerModel(String dominio, CalculoJudicialSolicitantePerfil perfil, boolean iaAssistida, BigDecimal totalGeral) {
        String subtitle = totalGeral == null
                ? (effectiveProfile(perfil).citizenLike() ? "Tudo pronto para abrir a memória e revisar com calma." : "Estado pronto para conferência técnica, PDF e trilha auditável.")
                : "Total consolidado: " + CalculoJudicialMetadataSupport.money(totalGeral);
        return linked(
                "eyebrow", iaAssistida ? "feito com IA assistida" : "cálculo pronto",
                "title", "TRABALHISTA_CLT".equals(dominio) ? "Memória trabalhista disponível" : "CUSTAS_PROCESSUAIS".equals(dominio) ? "Memória de custas disponível" : "FEDERAL_PREVIDENCIARIO_CJF".equals(dominio) ? "Memória previdenciária federal disponível" : "Memória fazendária disponível",
                "subtitle", subtitle,
                "primaryAction", "abrir memória agora",
                "secondaryActions", List.of("baixar PDF", "ver auditoria"),
                "badge", iaBadge(iaAssistida)
        );
    }

    private static Map<String, Object> completionCard(String dominio, CalculoJudicialSolicitantePerfil perfil, boolean iaAssistida, BigDecimal totalGeral) {
        String body = effectiveProfile(perfil).citizenLike()
                ? "Você pode abrir a memória, conferir os critérios e baixar o PDF sem sair da tela principal."
                : "A memória consolidada já está pronta para revisão técnica com ações rápidas de auditoria e emissão de PDF.";
        return linked(
                "eyebrow", iaAssistida ? "conclusão assistida" : "conclusão direta",
                "title", "TRABALHISTA_CLT".equals(dominio) ? "Cálculo trabalhista pronto" : "CUSTAS_PROCESSUAIS".equals(dominio) ? "Cálculo de custas pronto" : "FEDERAL_PREVIDENCIARIO_CJF".equals(dominio) ? "Cálculo previdenciário federal pronto" : "Cálculo fazendário pronto",
                "body", body,
                "highlight", totalGeral == null ? "PDF e memória ficarão disponíveis ao concluir." : "Total estimado: " + CalculoJudicialMetadataSupport.money(totalGeral),
                "primaryAction", linked("label", "abrir memória agora", "emphasis", "strong"),
                "secondaryActions", List.of(
                        linked("label", "baixar PDF", "emphasis", "medium"),
                        linked("label", "ver trilha de auditoria", "emphasis", "medium")
                )
        );
    }

    private static List<Map<String, Object>> actionBar(String dominio, boolean iaAssistida) {
        return List.of(
                linked("acao", "abrir memória agora", "tipo", "primary"),
                linked("acao", "baixar PDF", "tipo", "secondary"),
                linked("acao", iaAssistida ? "revisar parâmetros sugeridos" : "revisar parâmetros", "tipo", "secondary"),
                linked("acao", "ver trilha de auditoria", "tipo", "ghost")
        );
    }

    private static List<String> domainNextSteps(String dominio, CalculoJudicialSolicitantePerfil perfil, boolean iaAssistida) {
        if ("TRABALHISTA_CLT".equals(dominio)) {
            return effectiveProfile(perfil).citizenLike()
                    ? List.of(
                    "Conferir se as verbas e os reflexos fazem sentido para o caso.",
                    iaAssistida ? "Revisar as sugestões da IA antes de compartilhar ou imprimir." : "Baixar o PDF e guardar a memória final.",
                    "Abrir a trilha de auditoria se quiser entender como cada valor foi montado."
            )
                    : List.of(
                    "Conferir rubricas principais, reflexos e FGTS na memória consolidada.",
                    iaAssistida ? "Validar os parâmetros sugeridos pela IA e confirmar a estratégia de verbas e penalidades." : "Emitir PDF e seguir para revisão técnica final.",
                    "Inspecionar a trilha auditável antes de fixar a versão de trabalho."
            );
        }
        if ("FEDERAL_PREVIDENCIARIO_CJF".equals(dominio)) {
            return effectiveProfile(perfil).citizenLike()
                    ? List.of(
                    "Conferir competências vencidas, abono anual e abatimentos antes de fechar a versão final.",
                    iaAssistida ? "Revisar as sugestões da IA antes de compartilhar ou imprimir." : "Baixar o PDF e guardar a memória final.",
                    "Abrir a trilha de auditoria para entender a classificação entre RPV e precatório."
            )
                    : List.of(
                    "Conferir atrasados, abono anual, compensações e enquadramento do pagamento na memória consolidada.",
                    iaAssistida ? "Validar os parâmetros sugeridos pela IA e confirmar corte prescricional, juros e abatimentos." : "Emitir PDF e seguir para revisão técnica final.",
                    "Inspecionar a trilha auditável antes de fixar a versão de trabalho."
            );
        }
        return effectiveProfile(perfil).citizenLike()
                ? List.of(
                "Conferir juros, multas, descontos e abatimentos antes de fechar a versão final.",
                iaAssistida ? "Revisar as sugestões da IA antes de compartilhar ou imprimir." : "Baixar o PDF e guardar a memória final.",
                "Abrir a trilha de auditoria se quiser entender como o saldo foi montado."
        )
                : List.of(
                "Conferir principal, SELIC, mora, descontos e garantias na memória consolidada.",
                iaAssistida ? "Validar os parâmetros sugeridos pela IA e confirmar o regime financeiro aplicável." : "Emitir PDF e seguir para revisão técnica final.",
                "Inspecionar a trilha auditável antes de fixar a versão de trabalho."
        );
    }

    private static List<String> completionBadges(String dominio, boolean iaAssistida) {
        return "TRABALHISTA_CLT".equals(dominio)
                ? List.of("pronto", iaAssistida ? "feito com IA assistida" : "feito sem automação crítica", "PDF disponível", "auditoria pronta")
                : "CUSTAS_PROCESSUAIS".equals(dominio)
                ? List.of("pronto", iaAssistida ? "feito com IA assistida" : "feito sem automação crítica", "PDF disponível", "guia auditável")
                : "FEDERAL_PREVIDENCIARIO_CJF".equals(dominio)
                ? List.of("pronto", iaAssistida ? "feito com IA assistida" : "feito sem automação crítica", "PDF disponível", "RPV/precatório projetado")
                : List.of("pronto", iaAssistida ? "feito com IA assistida" : "feito sem automação crítica", "PDF disponível", "critérios financeiros auditáveis");
    }

    private static Map<String, Object> pulseModel(boolean iaAssistida) {
        return linked(
                "intensidade", iaAssistida ? "suave assistida" : "suave direta",
                "animacao", "soft-pulse",
                "duracaoMs", 1600,
                "repeticao", 2
        );
    }

    private static Map<String, Object> highlightStrip(String dominio, CalculoJudicialSolicitantePerfil perfil, BigDecimal totalGeral, boolean iaAssistida) {
        String domainLabel = "TRABALHISTA_CLT".equals(dominio) ? "trabalhista" : "CUSTAS_PROCESSUAIS".equals(dominio) ? "de custas" : "FEDERAL_PREVIDENCIARIO_CJF".equals(dominio) ? "federal previdenciária" : "fazendária";
        return linked(
                "title", iaAssistida ? "Versão assistida pronta" : "Versão pronta",
                "message", effectiveProfile(perfil).citizenLike()
                        ? "A memória " + domainLabel + " já pode ser aberta agora, com total estimado de " + CalculoJudicialMetadataSupport.money(totalGeral) + "."
                        : "A memória " + domainLabel + " consolidada está pronta, com total de " + CalculoJudicialMetadataSupport.money(totalGeral) + " e estado auditável ativo.",
                "badge", iaBadge(iaAssistida)
        );
    }

    private static Map<String, Object> iaBadge(boolean iaAssistida) {
        return linked(
                "label", iaAssistida ? "feito com IA assistida" : "pronto para revisão",
                "variant", iaAssistida ? "assistido" : "ready"
        );
    }

    private static Map<String, Object> emptyState(String dominio, CalculoJudicialSolicitantePerfil perfil) {
        return linked(
                "title", "TRABALHISTA_CLT".equals(dominio) ? "Comece pela memória trabalhista" : "CUSTAS_PROCESSUAIS".equals(dominio) ? "Comece pela memória de custas" : "FEDERAL_PREVIDENCIARIO_CJF".equals(dominio) ? "Comece pela memória previdenciária federal" : "Comece pela memória fazendária",
                "message", effectiveProfile(perfil).citizenLike()
                        ? "Preencha apenas o que existir no caso e o sistema vai te conduzir até a conclusão."
                        : "Informe os parâmetros essenciais e siga para a validação assistida antes da geração da memória.",
                "primaryAction", "começar agora"
        );
    }

    private static Map<String, Object> step(String codigo, String titulo, String descricao, String acao) {
        return linked(
                "codigo", codigo,
                "titulo", titulo,
                "descricao", descricao,
                "acao", acao
        );
    }

    private static Map<String, Object> linked(Object... items) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < items.length; i += 2) {
            map.put(String.valueOf(items[i]), items[i + 1]);
        }
        map.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(map);
    }

    public static List<Map<String, Object>> officialBenchmarkSignals() {
        return List.of(
                linked("sistema", "PJE_CALC", "foco", "cálculo trabalhista institucional", "sinais", List.of("manual e tutorial oficiais", "tabelas auxiliares mensais", "funcionalidades equivalentes à versão institucional", "exportação PJC e atualização de tabelas")),
                linked("sistema", "EPROC", "foco", "módulo de cálculos e contadoria", "sinais", List.of("módulo de cálculos para advogados", "contadoria judicial", "alinhamento ao Manual de Cálculos da Justiça Federal", "programas e automação de cálculos")),
                linked("sistema", "CRETA", "foco", "juizados federais e planilhas", "sinais", List.of("manual do sistema", "planilhas de atualização com juros", "planilhas de implantação de benefício mínimo", "uso de Manual de Cálculos do CJF")),
                linked("sistema", "E_SAJ", "foco", "custas, depósitos e conferência", "sinais", List.of("portal de custas e depósitos", "emissão de guias", "planilhas de taxa judiciária e custas finais", "despesas e diligências segregadas")),
                linked("sistema", "PROJUDI", "foco", "custas e guias públicas", "sinais", List.of("calculadora de custas", "formulários de guias iniciais", "locomoção e atos específicos", "foco forte em cobrança processual e tabelas")),
                linked("sistema", "CJF_JEF", "foco", "cálculos federais e previdenciários", "sinais", List.of("manual de cálculos da justiça federal", "SICOM com tabelas de correção monetária", "planilhas oficiais de atrasados previdenciários", "classificação prática de pagamentos e memória institucional"))
        );
    }

    public static List<Map<String, Object>> expansionIdeas() {
        return List.of(
                linked("codigo", "CUSTAS_PROCESSUAIS", "status", "implemented", "valor", "unificar taxa judiciária, preparo, despesas, diligências, depósitos e custas finais em memória auditável"),
                linked("codigo", "FEDERAL_PREVIDENCIARIO_CJF", "status", "implemented", "valor", "cobrir atrasados previdenciários federais/JEF com abono anual, correção, juros, compensações e classificação RPV/precatório"),
                linked("codigo", "PJE_EXPORT_INTEROPERABILIDADE", "status", "planned", "valor", "permitir exportações estruturadas, anexação processual e trilha de compatibilidade com ambientes externos"),
                linked("codigo", "TRABALHISTA_MALHA_PJECALC", "status", "planned", "valor", "cobrir faltas, histórico salarial, cartão de ponto, salário-família, seguro-desemprego, previdência privada e pensão alimentícia de forma parametrizada")
        );
    }
}
