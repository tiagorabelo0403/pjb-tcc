package com.tcc.pjb.backend.tribunal.regras.snapshot;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;

public record RegraResolvida(
        TribunalRuleEngine.ChaveRegra chave,
        Object valor,
        TribunalRuleEngine.TipoValor tipoValor,
        TribunalRuleEngine.NivelRegra nivelUsado,
        String escopoUsado,
        String fundamentacao,
        boolean usouFallback,
        boolean modoRestringirAplicado,
        List<String> trilhaAuditoria,
        Instant resolvidoEm,
        String tribunalCodigoContexto,
        RamoDireito ramoContexto,
        GrauJurisdicao grauContexto
) {
    public String texto() {
        return valor == null ? null : String.valueOf(valor);
    }

    public int inteiro() {
        if (valor instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(valor).trim());
        } catch (Exception e) {
            throw new IllegalStateException("Regra '" + chave.canonical() + "' não pôde ser convertida para inteiro: " + valor);
        }
    }

    public boolean booleano() {
        if (valor instanceof Boolean b) {
            return b;
        }
        String token = normalizeToken(String.valueOf(valor));
        return Set.of("TRUE", "SIM", "YES", "Y", "1", "VERDADEIRO", "ATIVO").contains(token);
    }

    public BigDecimal decimal() {
        if (valor instanceof BigDecimal bd) {
            return bd.setScale(2, RoundingMode.HALF_UP);
        }
        if (valor instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(normalizeDecimalString(String.valueOf(valor))).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new IllegalStateException("Regra '" + chave.canonical() + "' não pôde ser convertida para decimal: " + valor);
        }
    }

    public List<String> lista() {
        if (valor == null) {
            return List.of();
        }
        if (valor instanceof Collection<?> c) {
            return c.stream().filter(Objects::nonNull).map(String::valueOf).map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
        }
        if (valor instanceof String s) {
            return Arrays.stream(s.split(",")).map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
        }
        return List.of(String.valueOf(valor));
    }

    public String resumoAuditoria() {
        return "[" + chave.canonical() + "] " + tipoValor + "=" + valor + " via " + nivelUsado.name() + "[" + escopoUsado + "]"
                + (usouFallback ? " fallback" : "")
                + (modoRestringirAplicado ? " restringir" : "");
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static String normalizeDecimalString(String value) {
        String cleaned = value == null ? "0" : value.trim();
        if (cleaned.isBlank()) {
            return "0";
        }
        int lastDot = cleaned.lastIndexOf('.');
        int lastComma = cleaned.lastIndexOf(',');
        if (lastDot >= 0 && lastComma >= 0) {
            if (lastComma > lastDot) {
                return cleaned.replace(".", "").replace(",", ".");
            }
            return cleaned.replace(",", "");
        }
        if (lastComma >= 0) {
            return cleaned.replace(",", ".");
        }
        return cleaned;
    }
}
