package com.tcc.pjb.backend.model.entity.enums;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.util.EnumText;

public enum NivelSigilo {

    
    PUBLICO(0, false, "Público", "UNLOCK", "NORMAL", ""),

    
    SEGREDO_JUSTICA(1, true, "Segredo de Justiça", "LOCK", "RED", "Processo em segredo de justiça"),

    
    SIGILO_N2(2, true, "Acesso restrito", "LOCK", "AMBER", "Acesso restrito"),

    
    SIGILO_N3(3, true, "Acesso muito restrito", "LOCK", "ORANGE", "Acesso muito restrito"),

    
    SIGILO_N4(4, true, "Restrição máxima", "LOCK", "PURPLE", "Acesso restrito (nível máximo)"),

    
    SEGREDO_ESTADO(5, true, "Segredo de Estado", "LOCK", "BLACK", "Processo em segredo de justiça (nível máximo)");

    private final int nivel;
    private final boolean exigeCredencial;
    private final String label;
    private final String icon;
    private final String color;
    private final String mensagemPublica;

    public static final NivelSigilo SEGREDO_DE_JUSTICA = SEGREDO_JUSTICA;

    private static final Map<String, NivelSigilo> ALIASES = Map.ofEntries(
            
            Map.entry("PUBLIC", PUBLICO),
            Map.entry("PUBLICA", PUBLICO),
            Map.entry("PUB", PUBLICO),

            
            Map.entry("SEGREDO_DE_JUSTICA", SEGREDO_JUSTICA),
            Map.entry("SEGREDO_JUSTICA", SEGREDO_JUSTICA),
            Map.entry("SECRETO_JUSTICA", SEGREDO_JUSTICA),
            Map.entry("JUSTICA", SEGREDO_JUSTICA),
            Map.entry("N1", SEGREDO_JUSTICA),
            Map.entry("1", SEGREDO_JUSTICA),

            
            Map.entry("SIGILO_N2", SIGILO_N2),
            Map.entry("N2", SIGILO_N2),
            Map.entry("2", SIGILO_N2),
            Map.entry("SIGILO_N3", SIGILO_N3),
            Map.entry("N3", SIGILO_N3),
            Map.entry("3", SIGILO_N3),
            Map.entry("SIGILO_N4", SIGILO_N4),
            Map.entry("N4", SIGILO_N4),
            Map.entry("4", SIGILO_N4),

            
            Map.entry("SEGREDO_DE_ESTADO", SEGREDO_ESTADO),
            Map.entry("SEGREDO_ESTADO", SEGREDO_ESTADO),
            Map.entry("ESTADO", SEGREDO_ESTADO),
            Map.entry("SIGILO_N5", SEGREDO_ESTADO),
            Map.entry("N5", SEGREDO_ESTADO),
            Map.entry("5", SEGREDO_ESTADO)
    );

    NivelSigilo(int nivel,
               boolean exigeCredencial,
               String label,
               String icon,
               String color,
               String mensagemPublica) {
        this.nivel = nivel;
        this.exigeCredencial = exigeCredencial;
        this.label = label;
        this.icon = icon;
        this.color = color;
        this.mensagemPublica = mensagemPublica;
    }

    
    public int nivel() {
        return nivel;
    }

    
    public int getNivel() {
        return nivel;
    }

    public boolean exigeCredencial() {
        return exigeCredencial;
    }

    
    public boolean isExigeCredencial() {
        return exigeCredencial;
    }

    
    public String label() {
        return label;
    }

    
    public String icon() {
        return icon;
    }

    
    public String color() {
        return color;
    }

    
    public String mensagemPublica() {
        return mensagemPublica;
    }

    
    public static NivelSigilo fromString(String raw) {
        if (raw == null || raw.isBlank()) return PUBLICO;
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) return PUBLICO;
        NivelSigilo alias = ALIASES.get(token);
        if (alias != null) return alias;
        try {
            return NivelSigilo.valueOf(token);
        } catch (Exception ignored) {
            return PUBLICO;
        }
    }

    @JsonCreator
    public static NivelSigilo jsonCreator(String raw) {
        return fromString(raw);
    }

    
    public static final NivelSigilo RESTRITO = SIGILO_N2;


    public static final NivelSigilo SECRETO = SEGREDO_JUSTICA;
}
