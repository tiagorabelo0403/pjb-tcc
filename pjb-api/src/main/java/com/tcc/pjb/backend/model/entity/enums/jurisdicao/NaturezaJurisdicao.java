package com.tcc.pjb.backend.model.entity.enums.jurisdicao;

import java.text.Normalizer;
import java.util.*;
import java.util.Locale;

public enum NaturezaJurisdicao {

    

    CONTENCIOSA(
            Area.CIVIL,
            Rito.GERAL,
            "Conflito entre partes com pretensão resistida",
            kw("acao", "conflito", "litigio", "contestacao", "indenizacao", "cobranca", "responsabilidade civil")
    ),

    VOLUNTARIA(
            Area.CIVIL,
            Rito.ESPECIAL,
            "Atos sem litígio entre partes",
            kw("homologacao", "registro", "inventario consensual", "acordo extrajudicial", "curatela")
    ),

    EXECUTIVA(
            Area.CIVIL,
            Rito.EXECUCAO,
            "Execução ou cumprimento de título",
            kw("execucao", "cumprimento de sentenca", "penhora", "bloqueio", "sisbaJud", "titulo executivo")
    ),

    

    PENAL_COMUM(
            Area.PENAL,
            Rito.ORDINARIO,
            "Processo penal comum (CPP)",
            kw("crime", "denuncia", "inquerito", "acao penal", "jecrim", "transacao penal")
    ),

    JURI(
            Area.PENAL,
            Rito.ESPECIAL,
            "Tribunal do Júri",
            kw("homicidio", "aborto", "infanticidio", "juri", "pronuncia")
    ),

    MARIA_DA_PENHA(
            Area.PENAL,
            Rito.ESPECIAL,
            "Violência doméstica e familiar",
            kw("maria da penha", "medida protetiva", "violencia domestica")
    ),

    

    TRABALHISTA(
            Area.TRABALHISTA,
            Rito.ORDINARIO,
            "Processo trabalhista",
            kw("clt", "reclamacao trabalhista", "verbas rescisorias", "justa causa", "horas extras")
    ),

    

    EXECUCAO_FISCAL(
            Area.TRIBUTARIO,
            Rito.EXECUCAO,
            "Execução fiscal",
            kw("execucao fiscal", "divida ativa", "cda", "pgfn", "sefaz")
    ),

    

    PREVIDENCIARIO(
            Area.PREVIDENCIARIO,
            Rito.ORDINARIO,
            "Benefícios previdenciários",
            kw("inss", "aposentadoria", "auxilio-doenca", "bpc", "revisao de beneficio")
    ),

    

    RECURSAL(
            Area.GERAL,
            Rito.RECURSAL,
            "Instância recursal",
            kw("apelacao", "agravo", "embargos", "recurso especial", "recurso extraordinario")
    );

    

    

    public enum Area {
        GERAL,
        CIVIL,
        PENAL,
        TRABALHISTA,
        TRIBUTARIO,
        PREVIDENCIARIO
    }

    public enum Rito {
        GERAL,
        ORDINARIO,
        ESPECIAL,
        EXECUCAO,
        RECURSAL
    }

    public enum FlagConformidade {
        NAO_SUBSTITUI_DECISAO_HUMANA,
        REQUER_REVISAO,
        POSSIVEL_RITO_ESPECIAL,
        POSSIVEL_JUIZADO,
        ALTA_CONFIANCA
    }

    

    private final Area area;
    private final Rito rito;
    private final String descricao;
    private final Set<String> palavrasChave;

    NaturezaJurisdicao(
            Area area,
            Rito rito,
            String descricao,
            Set<String> palavrasChave
    ) {
        this.area = Objects.requireNonNull(area, "Área obrigatória");
        this.rito = Objects.requireNonNull(rito, "Rito obrigatório");
        this.descricao = Objects.requireNonNull(descricao, "Descrição obrigatória");
        this.palavrasChave = Set.copyOf(palavrasChave);
    }

    

    public Area getArea() {
        return area;
    }

    public Rito getRito() {
        return rito;
    }

    public String getDescricao() {
        return descricao;
    }

    public Set<String> getPalavrasChave() {
        return palavrasChave;
    }

    

    
    public record ContextoPJe(
            String assunto,
            String classe,
            String orgao,
            boolean recurso,
            boolean execucao,
            boolean haConflito
    ) {}

    
    public record Recomendacao(
            NaturezaJurisdicao natureza,
            int pontuacao,
            String justificativa,
            Set<FlagConformidade> flags
    ) {}

    

    public static List<Recomendacao> recomendar(ContextoPJe ctx) {

        String texto = normalizar(
                (ctx.assunto() == null ? "" : ctx.assunto()) + " " +
                        (ctx.classe() == null ? "" : ctx.classe()) + " " +
                        (ctx.orgao() == null ? "" : ctx.orgao())
        );

        List<Recomendacao> resultado = new ArrayList<>();

        for (NaturezaJurisdicao natureza : values()) {

            int score = 0;
            List<String> motivos = new ArrayList<>();

            for (String kw : natureza.palavrasChave) {
                if (texto.contains(kw)) {
                    score += 10;
                    motivos.add("Termo identificado: " + kw);
                }
            }

            if (ctx.recurso() && natureza.rito == Rito.RECURSAL) {
                score += 25;
                motivos.add("Fluxo recursal identificado");
            }

            if (ctx.execucao() && natureza.rito == Rito.EXECUCAO) {
                score += 25;
                motivos.add("Execução identificada");
            }

            if (!ctx.haConflito() && natureza == VOLUNTARIA) {
                score += 20;
                motivos.add("Ausência de litígio");
            }

            if (score <= 0) {
                continue;
            }

            score = Math.min(score, 100);

            Set<FlagConformidade> flags = EnumSet.of(
                    FlagConformidade.NAO_SUBSTITUI_DECISAO_HUMANA
            );

            if (score < 40) {
                flags.add(FlagConformidade.REQUER_REVISAO);
            }

            if (score >= 70) {
                flags.add(FlagConformidade.ALTA_CONFIANCA);
            }

            resultado.add(
                    new Recomendacao(
                            natureza,
                            score,
                            String.join("; ", motivos),
                            Set.copyOf(flags)
                    )
            );
        }

        resultado.sort(Comparator.comparingInt(Recomendacao::pontuacao).reversed());
        return List.copyOf(resultado);
    }

    

    private static Set<String> kw(String... termos) {
        Set<String> set = new LinkedHashSet<>();
        for (String t : termos) {
            set.add(normalizar(t));
        }
        return Set.copyOf(set);
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        String n = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
