package com.tcc.pjb.backend.model.entity.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.tcc.pjb.backend.core.util.EnumText;
public enum RamoDireito {

    CIVIL("01", "Direito Civil", "Privado") {
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    FAMILIA("02", "Família", "Privado") {
        @Override
        public boolean geraSigiloAutomatico() { return true; }
        @Override
        public boolean exigeAtuacaoMP() { return true; }
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    CONSUMIDOR("03", "Direito do Consumidor", "Privado") {
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    EMPRESARIAL("04", "Direito Empresarial", "Privado"),
    PENAL("05", "Direito Penal", "Público") {
        @Override
        public boolean exigeAtuacaoMP() { return true; }
        @Override
        public boolean geraSigiloAutomatico() { return true; }
    },
    MILITAR("06", "Direito Militar", "Público") {
        @Override
        public boolean exigeAtuacaoMP() { return true; }
        @Override
        public boolean geraSigiloAutomatico() { return true; }
    },
    ELEITORAL("07", "Direito Eleitoral", "Público") {
        @Override
        public boolean exigeAtuacaoMP() { return true; }
    },
    ADMINISTRATIVO("08", "Direito Administrativo", "Público"),
    TRIBUTARIO("09", "Direito Tributário", "Público"),
    CONSTITUCIONAL("10", "Direito Constitucional", "Público"),
    AMBIENTAL("11", "Direito Ambiental", "Difuso") {
        @Override
        public boolean exigeAtuacaoMP() { return true; }
    },
    TRABALHISTA("12", "Direito do Trabalho", "Social") {
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    PREVIDENCIARIO("13", "Direito Previdenciário", "Social"),
    INFANCIA_JUVENTUDE("14", "Infância e Juventude", "Social") {
        @Override
        public boolean geraSigiloAutomatico() { return true; }
        @Override
        public boolean exigeAtuacaoMP() { return true; }
    },
    AGRARIO("15", "Direito Agrário", "Público"),
    INTERNACIONAL("16", "Direito Internacional", "Público") {
        @Override
        public boolean exigeAtuacaoMP() { return true; }
    },
    PROCESSUAL_CIVIL("17", "Direito Processual Civil", "Privado") {
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    SUCESSOES("18", "Direito das Sucessões", "Privado") {
        @Override
        public boolean geraSigiloAutomatico() { return true; }
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    CONTRATUAL("19", "Direito Contratual", "Privado") {
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    RESPONSABILIDADE_CIVIL("20", "Responsabilidade Civil", "Privado") {
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    IMOBILIARIO("21", "Direito Imobiliário", "Privado") {
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    BANCARIO("22", "Direito Bancário", "Privado"),
    FALIMENTAR_RECUPERACIONAL("23", "Falimentar e Recuperacional", "Privado"),
    REGISTRAL_NOTARIAL("24", "Registral e Notarial", "Privado"),
    ARBITRAGEM_MEDIACAO("25", "Arbitragem, Mediação e ADR", "Privado") {
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    DIGITAL_PROTECAO_DADOS("26", "Direito Digital e Proteção de Dados", "Privado"),
    SAUDE_SUPLEMENTAR("27", "Saúde Suplementar e Direito Médico", "Social") {
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    PROCESSUAL_PENAL("28", "Direito Processual Penal", "Público") {
        @Override
        public boolean exigeAtuacaoMP() { return true; }
        @Override
        public boolean geraSigiloAutomatico() { return true; }
    },
    EXECUCAO_PENAL("29", "Execução Penal", "Público") {
        @Override
        public boolean exigeAtuacaoMP() { return true; }
        @Override
        public boolean geraSigiloAutomatico() { return true; }
    },
    PROCESSUAL_ELEITORAL("30", "Direito Processual Eleitoral", "Público") {
        @Override
        public boolean exigeAtuacaoMP() { return true; }
    },
    LICITACOES_CONTRATOS("31", "Licitações e Contratos Administrativos", "Público"),
    IMPROBIDADE_ADMINISTRATIVA("32", "Improbidade Administrativa", "Público"),
    SERVIDOR_PUBLICO("33", "Servidor Público", "Público"),
    REGULATORIO("34", "Direito Regulatório", "Público"),
    ADUANEIRO("35", "Direito Aduaneiro", "Público"),
    PROCESSUAL_TRABALHISTA("36", "Direito Processual do Trabalho", "Social") {
        @Override
        public boolean admiteConciliacao() { return true; }
    },
    ACIDENTARIO("37", "Direito Acidentário", "Social"),
    URBANISTICO("38", "Direito Urbanístico", "Difuso") {
        @Override
        public boolean exigeAtuacaoMP() { return true; }
    },
    MINERARIO("39", "Direito Minerário", "Público"),
    ENERGETICO("40", "Direito Energético", "Público"),
    CIVIL_PUBLICA_COLETIVO("41", "Ações Coletivas e Processo Coletivo", "Difuso") {
        @Override
        public boolean exigeAtuacaoMP() { return true; }
    },
    EXECUCAO_FISCAL("42", "Execução Fiscal", "Público");

    private final String codigo;
    private final String descricao;
    private final String categoria;

    RamoDireito(String codigo, String descricao, String categoria) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.categoria = categoria;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public static final RamoDireito CIVEL = CIVIL;
    public static final RamoDireito FAZENDA_PUBLICA = ADMINISTRATIVO;

    public boolean geraSigiloAutomatico() { return false; }
    public boolean exigeAtuacaoMP() { return false; }
    public boolean admiteConciliacao() { return false; }

    public static Optional<RamoDireito> fromCodigo(String codigo) {
        return Arrays.stream(values()).filter(r -> r.codigo.equals(codigo)).findFirst();
    }

    public static RamoDireito fromNullable(String texto) {
        return fromString(texto);
    }

    public static RamoDireito fromString(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String token = EnumText.normalizeToken(texto);
        if (token.isBlank()) {
            return null;
        }
        RamoDireito alias = ALIASES.get(token);
        if (alias != null) {
            return alias;
        }
        Optional<RamoDireito> byCodigo = fromCodigo(token);
        if (byCodigo.isPresent()) {
            return byCodigo.get();
        }
        try {
            return RamoDireito.valueOf(token);
        } catch (Exception ignored) {
            String upper = texto.trim().toUpperCase();
            return Arrays.stream(values()).filter(r -> r.descricao.toUpperCase().contains(upper)).findFirst().orElse(null);
        }
    }

    public boolean isCivelLike() {
        return Set.of(CIVIL, PROCESSUAL_CIVIL, FAMILIA, SUCESSOES, CONSUMIDOR, EMPRESARIAL, CONTRATUAL,
                RESPONSABILIDADE_CIVIL, IMOBILIARIO, BANCARIO, FALIMENTAR_RECUPERACIONAL, REGISTRAL_NOTARIAL,
                ARBITRAGEM_MEDIACAO, DIGITAL_PROTECAO_DADOS, SAUDE_SUPLEMENTAR).contains(this);
    }

    public boolean isPenalLike() {
        return Set.of(PENAL, PROCESSUAL_PENAL, EXECUCAO_PENAL, MILITAR).contains(this);
    }

    public boolean isFazendaLike() {
        return Set.of(ADMINISTRATIVO, LICITACOES_CONTRATOS, IMPROBIDADE_ADMINISTRATIVA, SERVIDOR_PUBLICO,
                REGULATORIO, TRIBUTARIO, ADUANEIRO, PREVIDENCIARIO, CONSTITUCIONAL, EXECUCAO_FISCAL).contains(this);
    }

    public String verticalPrincipal() {
        if (this == ELEITORAL || this == PROCESSUAL_ELEITORAL) {
            return "ELEITORAL";
        }
        if (isPenalLike()) {
            return "PENAL";
        }
        if (this == TRABALHISTA || this == PROCESSUAL_TRABALHISTA || this == ACIDENTARIO) {
            return "TRABALHISTA";
        }
        if (isFazendaLike()) {
            return "FAZENDA";
        }
        if (Set.of(AMBIENTAL, URBANISTICO, AGRARIO, MINERARIO, ENERGETICO, CIVIL_PUBLICA_COLETIVO).contains(this)) {
            return "DIFUSO";
        }
        return "CIVEL";
    }

    private static final Map<String, RamoDireito> ALIASES = Map.ofEntries(
            Map.entry("CIVEL", CIVIL),
            Map.entry("DIREITO_CIVIL", CIVIL),
            Map.entry("PROCESSUAL_CIVIL", PROCESSUAL_CIVIL),
            Map.entry("PROC_CIVIL", PROCESSUAL_CIVIL),
            Map.entry("FAMILIA_E_SUCESSOES", FAMILIA),
            Map.entry("SUCESSOES", SUCESSOES),
            Map.entry("CONTRATOS", CONTRATUAL),
            Map.entry("RESP_CIVIL", RESPONSABILIDADE_CIVIL),
            Map.entry("IMOBILIARIA", IMOBILIARIO),
            Map.entry("EMPRESA", EMPRESARIAL),
            Map.entry("EMPRESARIAL", EMPRESARIAL),
            Map.entry("RECUPERACAO_JUDICIAL", FALIMENTAR_RECUPERACIONAL),
            Map.entry("FALIMENTAR", FALIMENTAR_RECUPERACIONAL),
            Map.entry("NOTARIAL", REGISTRAL_NOTARIAL),
            Map.entry("REGISTRAL", REGISTRAL_NOTARIAL),
            Map.entry("ARBITRAGEM", ARBITRAGEM_MEDIACAO),
            Map.entry("MEDIACAO", ARBITRAGEM_MEDIACAO),
            Map.entry("PROTECAO_DE_DADOS", DIGITAL_PROTECAO_DADOS),
            Map.entry("LGPD", DIGITAL_PROTECAO_DADOS),
            Map.entry("MEDICO", SAUDE_SUPLEMENTAR),
            Map.entry("SAUDE", SAUDE_SUPLEMENTAR),
            Map.entry("PENAL_MILITAR", MILITAR),
            Map.entry("CRIMINAL", PENAL),
            Map.entry("PROCESSUAL_PENAL", PROCESSUAL_PENAL),
            Map.entry("EXECUCAO_PENAL", EXECUCAO_PENAL),
            Map.entry("PROCESSUAL_ELEITORAL", PROCESSUAL_ELEITORAL),
            Map.entry("FAZENDA", ADMINISTRATIVO),
            Map.entry("FAZENDA_PUBLICA", ADMINISTRATIVO),
            Map.entry("LICITACOES", LICITACOES_CONTRATOS),
            Map.entry("IMPROBIDADE", IMPROBIDADE_ADMINISTRATIVA),
            Map.entry("SERVIDOR", SERVIDOR_PUBLICO),
            Map.entry("TRIBUTARIO", TRIBUTARIO),
            Map.entry("TRIBUTARIA", TRIBUTARIO),
            Map.entry("ADUANEIRO", ADUANEIRO),
            Map.entry("PREVIDENCIARIO", PREVIDENCIARIO),
            Map.entry("TRABALHO", TRABALHISTA),
            Map.entry("PROCESSUAL_TRABALHISTA", PROCESSUAL_TRABALHISTA),
            Map.entry("ACIDENTARIO", ACIDENTARIO),
            Map.entry("INFANCIA_E_JUVENTUDE", INFANCIA_JUVENTUDE),
            Map.entry("INFANCIA_JUVENTUDE", INFANCIA_JUVENTUDE),
            Map.entry("URBANISTICO", URBANISTICO),
            Map.entry("MINERARIO", MINERARIO),
            Map.entry("ENERGETICO", ENERGETICO),
            Map.entry("ACAO_CIVIL_PUBLICA", CIVIL_PUBLICA_COLETIVO),
            Map.entry("PROCESSO_COLETIVO", CIVIL_PUBLICA_COLETIVO),
            Map.entry("EXECUCAO_FISCAL", EXECUCAO_FISCAL),
            Map.entry("DIREITO_INTERNACIONAL", INTERNACIONAL),
            Map.entry("COOPERACAO_INTERNACIONAL", INTERNACIONAL),
            Map.entry("JURIDICA_INTERNACIONAL", INTERNACIONAL),
            Map.entry("INTERNACIONAL_PUBLICO", INTERNACIONAL),
            Map.entry("INTERNACIONAL_PRIVADO", INTERNACIONAL)
    );
}
