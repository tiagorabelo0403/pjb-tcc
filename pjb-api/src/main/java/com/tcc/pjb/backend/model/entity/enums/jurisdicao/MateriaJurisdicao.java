package com.tcc.pjb.backend.model.entity.enums.jurisdicao;

import java.text.Normalizer;
import java.util.Locale;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public enum MateriaJurisdicao {
    CIVIL,
    EMPRESARIAL,
    PENAL,
    EXECUCAO_PENAL,
    TRABALHISTA,
    ELEITORAL,
    MILITAR,
    ADMINISTRATIVO,
    AMBIENTAL,
    TRIBUTARIA,
    FAMILIA,
    SUCESSOES,
    INFANCIA_JUVENTUDE,
    PREVIDENCIARIA,
    CONSUMIDOR,
    FALENCIAS,
    REGISTROS_PUBLICOS,
    EXECUCAO_FISCAL,
    SAUDE,
    EDUCACAO,
    URBANISMO,
    AGRARIO,
    MULTIMATERIA,
    CONSTITUCIONAL;

    
    public static final MateriaJurisdicao CIVEL = CIVIL;
    public static final MateriaJurisdicao TRIBUTARIO = TRIBUTARIA;
    public static final MateriaJurisdicao PREVIDENCIARIO = PREVIDENCIARIA;

    public static MateriaJurisdicao fromRamo(RamoDireito ramo) {
        if (ramo == null) return MULTIMATERIA;
        return switch (ramo) {
            case CIVIL -> CIVIL;
            case EMPRESARIAL -> EMPRESARIAL;
            case PENAL -> PENAL;
            case TRABALHISTA -> TRABALHISTA;
            case ELEITORAL -> ELEITORAL;
            case MILITAR -> MILITAR;
            case ADMINISTRATIVO -> ADMINISTRATIVO;
            case TRIBUTARIO -> TRIBUTARIA;
            case CONSUMIDOR -> CONSUMIDOR;
            case PREVIDENCIARIO -> PREVIDENCIARIA;
            case AMBIENTAL -> AMBIENTAL;
            case CONSTITUCIONAL -> CONSTITUCIONAL;
            case INFANCIA_JUVENTUDE -> INFANCIA_JUVENTUDE;
            case FAMILIA -> FAMILIA;
            case AGRARIO -> AGRARIO;
            default -> MULTIMATERIA;
        };
    }

    
    public static MateriaJurisdicao fromString(String texto) {
        if (texto == null || texto.isBlank()) return MULTIMATERIA;

        String token = normalizeToken(texto);
        if (token.isBlank()) return MULTIMATERIA;

        
        token = switch (token) {
            case "ADMINISTRATIVA" -> "ADMINISTRATIVO";
            case "OUTROS", "OUTRAS", "OUTRO" -> "MULTIMATERIA";
            case "TRIBUTARIO" -> "TRIBUTARIA";
            case "PREVIDENCIARIO" -> "PREVIDENCIARIA";
            case "EMPRESA" -> "EMPRESARIAL";
            case "EMPRESARIAL" -> "EMPRESARIAL";
            case "FALENCIA", "FALENCIAS" -> "FALENCIAS";
            case "SUCESSAO", "SUCESSOES" -> "SUCESSOES";
            case "INFANCIA_JUVENTUDE", "INFANCIA_E_JUVENTUDE" -> "INFANCIA_JUVENTUDE";
            case "EXECUCAO_PENAL", "PENA", "LEP" -> "EXECUCAO_PENAL";
            case "AGRARIA" -> "AGRARIO";
            default -> token;
        };

        try {
            return MateriaJurisdicao.valueOf(token);
        } catch (Exception ignored) {
            return MULTIMATERIA;
        }
    }


    public RamoDireito getRamo() {
        return switch (this) {
            case CIVIL, CONSUMIDOR, FAMILIA, SUCESSOES, SAUDE, EDUCACAO, URBANISMO, REGISTROS_PUBLICOS, FALENCIAS, EMPRESARIAL, EXECUCAO_FISCAL, AGRARIO -> RamoDireito.CIVIL;
            case PENAL, EXECUCAO_PENAL -> RamoDireito.PENAL;
            case TRABALHISTA -> RamoDireito.TRABALHISTA;
            case ELEITORAL -> RamoDireito.ELEITORAL;
            case MILITAR -> RamoDireito.MILITAR;
            case ADMINISTRATIVO -> RamoDireito.ADMINISTRATIVO;
            case AMBIENTAL -> RamoDireito.AMBIENTAL;
            case TRIBUTARIA -> RamoDireito.TRIBUTARIO;
            case INFANCIA_JUVENTUDE -> RamoDireito.INFANCIA_JUVENTUDE;
            case PREVIDENCIARIA -> RamoDireito.PREVIDENCIARIO;
            case CONSTITUCIONAL -> RamoDireito.CONSTITUCIONAL;
            case MULTIMATERIA -> RamoDireito.CIVIL;
        };
    }

    private static String normalizeToken(String raw) {
        String v = raw.trim().toUpperCase(Locale.ROOT);
        
        v = Normalizer.normalize(v, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        
        v = v.replace('-', '_').replace(' ', '_').replace('/', '_');
        
        v = v.replaceAll("[^A-Z0-9_]", "");
        
        v = v.replaceAll("_+", "_");
        
        v = v.replaceAll("^_+", "").replaceAll("_+$", "");
        return v;
    }
}
