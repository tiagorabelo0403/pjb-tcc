package com.tcc.pjb.backend.model.entity.enums.processual;

import java.util.Arrays;
import java.util.Optional;

public enum FaseProcessual {

    
    
    
    CONHECIMENTO(1, "Discussão de mérito e formação do convencimento judicial", "CNH"),
    LIQUIDACAO(2, "Liquidação para apuração de valores", "LIQ"),
    CUMPRIMENTO_SENTENCA(3, "Cumprimento de sentença / cobrança forçada", "CSE"),
    EXECUCAO(4, "Execução de título (judicial/extrajudicial conforme o caso)", "EXE"),
    RECURSAL(5, "Fase recursal / reexame por órgão ad quem", "REC"),

    
    
    
    
    COGNITIVA(6, "Alias: fase cognitiva (compatível com fluxo de conhecimento)", "COG"),
    EXECUTORIA(7, "Alias: fase executória (compatível com execução/cumprimento)", "EXC"),
    INSTRUTORIA(8, "Alias: fase instrutória (produção de prova)", "INS"),
    CONSTITUCIONAL(9, "Fase constitucional (controle concentrado/abstrato etc.)", "CST"),

    
    
    

    
    AUDIENCIA_CUSTODIA(10, "Audiência de custódia (controle judicial da prisão em flagrante)", "ACU"),
    PRONUNCIA(11, "Decisão de pronúncia (admissibilidade da acusação no Tribunal do Júri)", "PRO"),
    PLENARIO_JURI(12, "Sessão de julgamento em plenário do Tribunal do Júri", "PJU"),

    
    PERICIA_TECNICA(15, "Produção de prova pericial/técnica (complexidade variável)", "PET"),

    
    PENHORA(20, "Ato de constrição patrimonial (penhora/bloqueio) para satisfação do crédito", "PEN"),

    AUTUACAO(30, "Autuação e registro do processo no sistema", "AUT"),
    DISTRIBUICAO(31, "Distribuição do processo ao órgão julgador competente", "DIS"),
    CITACAO(32, "Citação da parte ré para integrar a relação processual", "CIT"),
    RESPOSTA(33, "Fase de resposta e defesa do réu", "RSP"),
    SANEAMENTO(34, "Saneamento do processo e fixação dos pontos controvertidos", "SAN"),
    JULGAMENTO(35, "Julgamento do mérito pelo órgão competente", "JUL"),
    ARQUIVAMENTO(36, "Arquivamento definitivo do processo encerrado", "ARQ");

    private final int ordem;
    private final String descricao;
    private final String codigo;

    FaseProcessual(int ordem, String descricao, String codigo) {
        this.ordem = ordem;
        this.descricao = descricao;
        this.codigo = codigo;
    }

    public int getOrdem() {
        return ordem;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    
    
    

    public static final FaseProcessual INSTRUCAO = INSTRUTORIA;
    public static final FaseProcessual INICIAL = CONHECIMENTO;
    public static final FaseProcessual POSTULATORIA = CONHECIMENTO;

    public static Optional<FaseProcessual> fromString(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String token = texto.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return switch (token) {
            case "CUMPRIMENTO", "CUMPRIMENTO_DE_SENTENCA", "CUMPRIMENTO_SENTENCA" -> Optional.of(CUMPRIMENTO_SENTENCA);
            case "CONHECIMENTO", "COGNITIVA", "COGNICAO" -> Optional.of(CONHECIMENTO);
            case "EXECUCAO", "EXECUTORIA" -> Optional.of(EXECUCAO);
            case "RECURSAL", "RECURSO" -> Optional.of(RECURSAL);
            case "LIQUIDACAO" -> Optional.of(LIQUIDACAO);
            case "INSTRUTORIA", "INSTRUCAO" -> Optional.of(INSTRUTORIA);
            case "AUDIENCIA_DE_CUSTODIA", "CUSTODIA" -> Optional.of(AUDIENCIA_CUSTODIA);
            default -> {
                try {
                    yield Optional.of(FaseProcessual.valueOf(token));
                } catch (IllegalArgumentException ex) {
                    yield fromCodigo(token);
                }
            }
        };
    }

    public static Optional<FaseProcessual> fromCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) return Optional.empty();
        String c = codigo.trim();
        return Arrays.stream(values())
                .filter(f -> f.codigo.equalsIgnoreCase(c))
                .findFirst();
    }

    public static Optional<FaseProcessual> fromOrdem(int ordem) {
        return Arrays.stream(values())
                .filter(f -> f.ordem == ordem)
                .findFirst();
    }

    
    public Optional<FaseProcessual> proximaFaseMacro() {
        if (!isMacroFase()) return Optional.empty();
        return fromOrdem(this.ordem + 1);
    }

    public boolean isFinalMacro() {
        return this == RECURSAL;
    }

    public boolean isMacroFase() {
        return this.ordem >= 1 && this.ordem <= 5;
    }

    public boolean isMicroFase() {
        return this.ordem >= 10;
    }



    public String codigoOuNome() {
        return codigo == null || codigo.isBlank() ? name() : codigo;
    }

    public boolean isTerminalLike() {
        return this == EXECUCAO || this == PENHORA;
    }

    public boolean isRecursal() {
        return this == RECURSAL;
    }

    public boolean isKnowledgeLike() {
        return this == CONHECIMENTO
                || this == COGNITIVA
                || this == INSTRUTORIA
                || this == LIQUIDACAO
                || this == AUDIENCIA_CUSTODIA
                || this == PRONUNCIA
                || this == PLENARIO_JURI
                || this == PERICIA_TECNICA;
    }

    public boolean isExecutionLike() {
        return this == CUMPRIMENTO_SENTENCA || this == EXECUCAO || this == EXECUTORIA || this == PENHORA;
    }

    
    
    

    
    public boolean isPenalOnly() {
        return this == AUDIENCIA_CUSTODIA || this == PRONUNCIA || this == PLENARIO_JURI;
    }

    
    public boolean exigeRitoJuri() {
        return this == PRONUNCIA || this == PLENARIO_JURI;
    }

    
    public boolean isExecucaoPatrimonial() {
        return this == PENHORA;
    }

    
    public boolean isTecnicoPericial() {
        return this == PERICIA_TECNICA;
    }

    
    public boolean isCivilExecutoria() {
        return this == CUMPRIMENTO_SENTENCA || this == EXECUCAO || this == EXECUTORIA || this == PENHORA;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s", name(), codigo, descricao);
    }
}
