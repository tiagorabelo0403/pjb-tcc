package com.tcc.pjb.backend.model.entity.enums.jurisdicao;

import java.util.*;
import java.util.regex.Pattern;

public enum TipoJurisdicao {

    COMUM(100, "Jurisdição Comum", "Competência geral (Federal ou Estadual).",
            "Ex.: ações cíveis, penais comuns.",
            Set.of("COMUM", "GERAL", "JUSTIÇA COMUM"),
            List.of("justiça comum", "competência geral"),
            "CF/88, arts. 109 e 125",
            "STF, STJ, TJ",
            Categoria.JUDICIAL, false, 10,
            List.of("BR"), List.of(), Rite.CIVIL, false,
            List.of(Pattern.compile("(c[ií]vel|penal comum|responsabilidade civil)", Pattern.CASE_INSENSITIVE))) {
        @Override public String comportamento() { return "Encaminhar conforme matéria: União → Federal; caso contrário → Estadual."; }
    },

    FEDERAL(200, "Justiça Federal", "Matérias envolvendo União, autarquias e empresas públicas federais.",
            "Ex.: INSS, Caixa, AGU.",
            Set.of("FEDERAL", "JF", "UNIÃO", "TRF"),
            List.of("justiça federal", "trf", "união"),
            "CF/88, art. 109",
            "STF, STJ, TRFs",
            Categoria.JUDICIAL, false, 90,
            List.of("BR"), List.of(), Rite.CIVIL, false,
            List.of(Pattern.compile("(INSS|Uni[aã]o|Caixa|Autarquia Federal|AGU)", Pattern.CASE_INSENSITIVE))) {
        @Override public String comportamento() { return "Competência da União e suas entidades."; }
    },

    ESTADUAL(300, "Justiça Estadual", "Competência residual, tudo que não for Federal.",
            "Ex.: família, consumidor, responsabilidade civil.",
            Set.of("ESTADUAL", "TJ"),
            List.of("justiça estadual", "turma recursal"),
            "CF/88, art. 125",
            "TJ, Turmas Recursais",
            Categoria.JUDICIAL, false, 70,
            List.of("BR"), List.of(), Rite.CIVIL, false,
            List.of(Pattern.compile("(fam[ií]lia|consumidor|responsabilidade civil)", Pattern.CASE_INSENSITIVE))) {
        @Override public String comportamento() { return "Julga o que não for da União."; }
    },

    EXTRAJUDICIAL(800, "Atividade Extrajudicial", "Cartórios e serviços notariais.",
            "Ex.: protestos, registros, autenticações.",
            Set.of("EXTRAJUDICIAL", "CARTÓRIO"),
            List.of("cartório", "registro", "notarial"),
            "Lei 8.935/94",
            "Cartórios",
            Categoria.EXTRAJUDICIAL, false, 50,
            List.of("BR"), List.of(), Rite.EXTRAJUDICIAL, false,
            List.of(Pattern.compile("(cart[oó]rio|registro|protesto|notarial)", Pattern.CASE_INSENSITIVE))) {
        @Override public String comportamento() { return "Atos de fé pública guiados ao cartório."; }
    },

    INTERNACIONAL(1000, "Jurisdição Internacional", "Casos com tratados, pactos e convenções internacionais.",
            "Ex.: Corte Interamericana, ONU, Mercosul.",
            Set.of("INTERNACIONAL", "ONU", "MERCOSUL"),
            List.of("tratado", "pacto", "convenção"),
            "CF/88, art. 5º §2º",
            "Corte Interamericana, ONU",
            Categoria.INTERNACIONAL, true, 95,
            List.of("BR", "OEA", "ONU"),
            List.of("CADH", "PIDCP", "Tratado de Assunção"),
            Rite.INTERNACIONAL, false,
            List.of(Pattern.compile("(tratado|pacto|ONU|Mercosul|CIDH)", Pattern.CASE_INSENSITIVE))) {
        @Override public String comportamento() { return "Aplicar tratados e convenções internacionais ratificados pelo Brasil."; }
    };

    
    
    
    public final int codigo;
    public final String label;
    public final String descricao;
    public final String exemplos;
    public final Set<String> palavrasChave;
    public final List<String> aliases;
    public final String baseLegal;
    public final String orgaoCompetente;
    public final Categoria categoria;
    public final boolean internacional;
    public final int prioridade;
    public final List<String> paises;
    public final List<String> tratados;
    public final Rite rito;
    public final boolean outreach;
    public final List<Pattern> regex;

    TipoJurisdicao(int codigo, String label, String descricao, String exemplos,
                   Set<String> palavrasChave, List<String> aliases,
                   String baseLegal, String orgaoCompetente,
                   Categoria categoria, boolean internacional, int prioridade,
                   List<String> paises, List<String> tratados,
                   Rite rito, boolean outreach, List<Pattern> regex) {
        this.codigo = codigo;
        this.label = label;
        this.descricao = descricao;
        this.exemplos = exemplos;
        this.palavrasChave = palavrasChave;
        this.aliases = aliases;
        this.baseLegal = baseLegal;
        this.orgaoCompetente = orgaoCompetente;
        this.categoria = categoria;
        this.internacional = internacional;
        this.prioridade = prioridade;
        this.paises = paises;
        this.tratados = tratados;
        this.rito = rito;
        this.outreach = outreach;
        this.regex = regex;
    }

    public abstract String comportamento();

    
    
    
    public String toJson() {
        return "{"
                + "\"codigo\":" + codigo + ","
                + "\"label\":\"" + esc(label) + "\","
                + "\"categoria\":\"" + categoria.name() + "\","
                + "\"baseLegal\":\"" + esc(baseLegal) + "\","
                + "\"orgaoCompetente\":\"" + esc(orgaoCompetente) + "\","
                + "\"internacional\":" + internacional + ","
                + "\"paises\":" + listJson(paises) + ","
                + "\"tratados\":" + listJson(tratados) + ","
                + "\"rito\":\"" + rito.name() + "\""
                + "}";
    }

    private static String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String listJson(List<String> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(esc(list.get(i))).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    
    
    
    public enum Categoria { JUDICIAL, ESPECIALIZADA, ADMINISTRATIVA, EXTRAJUDICIAL, AUTOCOMPOSITIVA, INTERNACIONAL }
    public enum Rite { CIVIL, PENAL, TRABALHISTA, ELEITORAL, MILITAR, ADMINISTRATIVO, EXTRAJUDICIAL, AUTOCOMPOSITIVO, INTERNACIONAL }
}