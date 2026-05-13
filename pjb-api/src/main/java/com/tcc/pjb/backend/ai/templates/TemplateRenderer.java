package com.tcc.pjb.backend.ai.templates;


public final class TemplateRenderer {

    public static String render(PromptTemplate template) {
        return switch (template) {
            case PETICAO_INICIAL -> "Estruture: Endereçamento, Fatos, Fundamentos, Pedido...";
            case PARECER -> "Estruture: Consulta, Fatos, Fundamentos, Conclusão...";
            case CONTESTACAO -> "Estruture: Preliminares, Mérito, Provas...";
            case RECURSO -> "Estruture: Tempestividade, Cabimento, Pedido de reforma...";
            case RELATORIO -> "Estruture: Histórico, Pontos controvertidos, Análise...";
            case NONE -> "Estrutura jurídica padrão.";
            default -> "Estrutura jurídica padrão.";
        };
    }
}