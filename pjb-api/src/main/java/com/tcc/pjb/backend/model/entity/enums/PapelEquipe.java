package com.tcc.pjb.backend.model.entity.enums;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.util.EnumText;

public enum PapelEquipe {

    
    ADMINISTRADOR(100, "Acesso total ao sistema e gerenciamento"),
    COORDENADOR(90, "Gestão da equipe e visão ampla"),
    JUIZ_GABINETE(100, "Juiz Titular (Dono do Gabinete)"), 
    ADVOGADO_SENIOR(70, "Atuação completa"),
    ADVOGADO_JUNIOR(70, "Acesso limitado"),
    ESTAGIARIO(30, "Apoio operacional"),
    CONSULTOR(15, "Somente leitura"),

    
    DELEGADO(80, "Autoridade Policial (Inquéritos)"),
    SERVIDOR(50, "Servidor do Judiciário (Cartório)"),
    CIDADAO(0, "Acesso Público Limitado");

    private final int nivel;
    private final String descricao;

    private static final Map<String, PapelEquipe> ALIASES = Map.ofEntries(
            Map.entry("JUIZ", JUIZ_GABINETE),
            Map.entry("MAGISTRADO", JUIZ_GABINETE),
            Map.entry("GABINETE", JUIZ_GABINETE),
            Map.entry("ADVOGADO", ADVOGADO_SENIOR),
            Map.entry("ADVOGADO_PLENO", ADVOGADO_SENIOR),
            Map.entry("ADVOGADO_JR", ADVOGADO_JUNIOR),
            Map.entry("ADV_JUNIOR", ADVOGADO_JUNIOR),
            Map.entry("SECRETARIA", SERVIDOR),
            Map.entry("SERVIDOR_FORUM", SERVIDOR),
            Map.entry("CITIZEN", CIDADAO)
    );

    PapelEquipe(int nivel, String descricao) {
        this.nivel = nivel;
        this.descricao = descricao;
    }

    public int getNivel() { return nivel; }
    public String getDescricao() { return descricao; }

    
    public static PapelEquipe fromString(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) return null;
        PapelEquipe alias = ALIASES.get(token);
        if (alias != null) return alias;
        try {
            return PapelEquipe.valueOf(token);
        } catch (Exception ignored) {
            return null;
        }
    }

    @JsonCreator
    public static PapelEquipe jsonCreator(String raw) {
        return fromString(raw);
    }
}