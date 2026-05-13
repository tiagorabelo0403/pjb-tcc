package com.tcc.pjb.backend.model.entity.enums.jurisdicao;

import java.util.*;
import java.util.regex.Pattern;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public enum EsferaJurisdicao {

    JUSTICA_FEDERAL(
            101,
            "Justiça Federal",
            "Causas envolvendo a União, autarquias e empresas públicas federais.",
            "TRF, Varas Federais, Juizados Especiais Federais",
            Set.of("TRF", "FEDERAL", "JF", "UNIAO", "JEF"),
            4,
            "STJ",
            "Art. 109, CF/88",
            true,
            Set.of(
                    RitoProcessual.COMUM_ORDINARIO,
                    RitoProcessual.JUIZADO_ESPECIAL,
                    RitoProcessual.ESPECIAL_MANDADO_SEGURANCA
            )
    ) {
        @Override
        public boolean validarCompetencia(String tribunal, String numeroProcesso) {
            return matchTexto(tribunal, "TRF", "FEDERAL") || matchCnj(numeroProcesso, 4);
        }
    },

    JUSTICA_ESTADUAL(
            102,
            "Justiça Estadual",
            "Competência residual.",
            "TJ, Varas Cíveis, Tribunal do Júri",
            Set.of("TJ", "ESTADUAL", "CIVEL", "CRIMINAL"),
            8,
            "STJ",
            "Art. 125, CF/88",
            false,
            Set.of(
                    RitoProcessual.COMUM_ORDINARIO,
                    RitoProcessual.SUMARIO,
                    RitoProcessual.JUIZADO_ESPECIAL
            )
    ) {
        @Override
        public boolean validarCompetencia(String tribunal, String numeroProcesso) {
            return matchTexto(tribunal, "TJ", "VARA") || matchCnj(numeroProcesso, 8);
        }
    },

    JUSTICA_TRABALHO(
            103,
            "Justiça do Trabalho",
            "Relações de trabalho.",
            "TRT, Varas do Trabalho",
            Set.of("TRT", "TRABALHO", "CLT", "VT"),
            5,
            "TST",
            "Art. 114, CF/88",
            true,
            Set.of(
                    RitoProcessual.TRABALHISTA_ORDINARIO,
                    RitoProcessual.TRABALHISTA_SUMARISSIMO,
                    RitoProcessual.TRABALHISTA_SUMARIO_ALCADA,
                    RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE,
                    RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO,
                    RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO
            )
    ) {
        @Override
        public boolean validarCompetencia(String tribunal, String numeroProcesso) {
            return matchTexto(tribunal, "TRT", "TRABALHO") || matchCnj(numeroProcesso, 5);
        }
    },

    JUSTICA_ELEITORAL(
            104,
            "Justiça Eleitoral",
            "Matéria eleitoral.",
            "TRE, TSE",
            Set.of("TRE", "TSE", "ELEITORAL"),
            6,
            "TSE",
            "Art. 118, CF/88",
            true,
            Set.of(RitoProcessual.ELEITORAL)
    ) {
        @Override
        public boolean validarCompetencia(String tribunal, String numeroProcesso) {
            return matchTexto(tribunal, "TRE", "ELEITORAL") || matchCnj(numeroProcesso, 6);
        }
    },

    JUSTICA_MILITAR(
            105,
            "Justiça Militar",
            "Crimes militares.",
            "STM, Auditorias Militares",
            Set.of("STM", "MILITAR"),
            7,
            "STM",
            "Art. 122, CF/88",
            true,
            Set.of(RitoProcessual.MILITAR)
    ) {
        @Override
        public boolean validarCompetencia(String tribunal, String numeroProcesso) {
            return matchTexto(tribunal, "MILITAR") || matchCnj(numeroProcesso, 7) || matchCnj(numeroProcesso, 9);
        }
    };

    
    
    

    private final int codigo;
    private final String label;
    private final String descricao;
    private final String exemplos;
    private final Set<String> sinonimos;
    private final Integer digitoCnj;
    private final String orgaoCupula;
    private final String fundamentacaoLegal;
    private final boolean especializada;
    private final Set<RitoProcessual> ritosPermitidos;

    private static final Pattern REGEX_CNJ =
            Pattern.compile("\\d{7}-\\d{2}\\.\\d{4}\\.(\\d)\\.\\d{2}\\.\\d{4}");

    EsferaJurisdicao(
            int codigo,
            String label,
            String descricao,
            String exemplos,
            Set<String> sinonimos,
            Integer digitoCnj,
            String orgaoCupula,
            String fundamentacaoLegal,
            boolean especializada,
            Set<RitoProcessual> ritosPermitidos
    ) {
        this.codigo = codigo;
        this.label = label;
        this.descricao = descricao;
        this.exemplos = exemplos;
        this.sinonimos = Collections.unmodifiableSet(sinonimos);
        this.digitoCnj = digitoCnj;
        this.orgaoCupula = orgaoCupula;
        this.fundamentacaoLegal = fundamentacaoLegal;
        this.especializada = especializada;
        this.ritosPermitidos = Collections.unmodifiableSet(ritosPermitidos);
    }

    
    
    

    public abstract boolean validarCompetencia(String tribunal, String numeroProcesso);

    
    
    

    
    public boolean aceitaRito(RitoProcessual rito) {
        return rito != null && ritosPermitidos.contains(rito);
    }

    
    public boolean validarProcesso(String tribunal, String numeroProcesso, RitoProcessual rito) {
        return validarCompetencia(tribunal, numeroProcesso) && aceitaRito(rito);
    }

    
    public Optional<String> explicarIncompatibilidade(RitoProcessual rito) {
        if (aceitaRito(rito)) return Optional.empty();
        return Optional.of(
                "Rito '" + rito +
                        "' incompatível com a esfera '" + label +
                        "'. Ritos aceitos: " + ritosPermitidos
        );
    }

    
    
    

    protected boolean matchTexto(String texto, String... palavras) {
        if (texto == null) return false;
        String t = texto.toUpperCase();
        for (String p : palavras) if (t.contains(p)) return true;
        return false;
    }

    protected boolean matchCnj(String numero, int esperado) {
        if (numero == null) return false;
        var m = REGEX_CNJ.matcher(numero);
        return m.find() && Integer.parseInt(m.group(1)) == esperado;
    }

    
    
    

    public Set<RitoProcessual> getRitosPermitidos() {
        return ritosPermitidos;
    }
}
