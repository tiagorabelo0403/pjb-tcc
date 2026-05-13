package com.tcc.pjb.backend.ai.juridica.conversation;

import java.util.List;

public final class JuridicaVirtualTrendsCatalog {

    public static final String RELATOR_ESTRUTURAL = "RELATOR_ESTRUTURAL";
    public static final String LEGISLADOR_NORMATIVO = "LEGISLADOR_NORMATIVO";
    public static final String PRECEDENTES_ESTRATEGICOS = "PRECEDENTES_ESTRATEGICOS";
    public static final String AUDITOR_SIMBOLICO = "AUDITOR_SIMBOLICO";
    public static final String REDATOR_CONVERSACIONAL = "REDATOR_CONVERSACIONAL";

    private JuridicaVirtualTrendsCatalog() {
    }

    public static List<LegalVirtualTrendProfile> profiles() {
        return List.of(
                new LegalVirtualTrendProfile(
                        RELATOR_ESTRUTURAL,
                        "organiza os fatos, a fase e o pedido útil",
                        "estruturar a pergunta"
                ),
                new LegalVirtualTrendProfile(
                        LEGISLADOR_NORMATIVO,
                        "exige base normativa vigente e compatível com o rito",
                        "confirmar fundamento normativo"
                ),
                new LegalVirtualTrendProfile(
                        PRECEDENTES_ESTRATEGICOS,
                        "procura precedentes, teses e convergência hermenêutica",
                        "buscar precedentes aplicáveis"
                ),
                new LegalVirtualTrendProfile(
                        AUDITOR_SIMBOLICO,
                        "confere prazo, cabimento, sigilo, fase e compatibilidade processual",
                        "validar restrições procedimentais"
                ),
                new LegalVirtualTrendProfile(
                        REDATOR_CONVERSACIONAL,
                        "transforma o resultado técnico em resposta natural, clara e segura",
                        "responder em linguagem natural"
                )
        );
    }

    public record LegalVirtualTrendProfile(String code,
                                           String role,
                                           String action) {
    }
}
