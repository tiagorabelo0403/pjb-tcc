package com.tcc.pjb.backend.domain.valueobject;

import java.time.Year;
import java.util.Objects;
import java.util.regex.Pattern;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public final class NumeroProcesso {

    private static final Pattern CNJ_PATTERN = Pattern.compile("\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}");

    private final String valor;

    private NumeroProcesso(String valor) {
        this.valor = valor;
    }

    public static NumeroProcesso gerar(
            TipoJustica justica,
            RamoDireito ramo,
            String codigoUF,
            String codigoComarca,
            String codigoUnidade
    ) {
        int ano = Year.now().getValue();
        int segmento = numericValue(justica == null ? TipoJustica.ESTADUAL.getCodigoCNJ() : justica.getCodigoCNJ(), 8);
        int tribunal = tribunalPadrao(justica, codigoUF);
        int unidade = numericValue(firstNonBlank(codigoUnidade, codigoComarca), 1);
        return gerarCnj(sequencialPadrao(), ano, segmento, tribunal, unidade);
    }

    public static NumeroProcesso gerarCnj(long sequencial, int ano, int segmentoJudiciario, int tribunal, int unidadeOrigem) {
        String sequencialFormatado = format(sequencial, 7, 0, 9_999_999);
        String anoFormatado = format(ano, 4, 1900, 9999);
        String segmentoFormatado = format(segmentoJudiciario, 1, 1, 9);
        String tribunalFormatado = format(tribunal, 2, 0, 99);
        String unidadeFormatada = format(unidadeOrigem, 4, 0, 9999);
        String dv = calcularDigitoVerificador(sequencialFormatado, anoFormatado, segmentoFormatado, tribunalFormatado, unidadeFormatada);
        return new NumeroProcesso(sequencialFormatado + "-" + dv + "." + anoFormatado + "." + segmentoFormatado + "." + tribunalFormatado + "." + unidadeFormatada);
    }

    public static NumeroProcesso of(String valor) {
        String normalized = normalize(valor);
        if (!validar(normalized)) {
            throw new IllegalArgumentException("Número CNJ inválido");
        }
        return new NumeroProcesso(normalized);
    }

    public static NumeroProcesso from(String valor) {
        return of(valor);
    }

    public static String calcularDigitoVerificador(long sequencial, int ano, int segmentoJudiciario, int tribunal, int unidadeOrigem) {
        return calcularDigitoVerificador(
                format(sequencial, 7, 0, 9_999_999),
                format(ano, 4, 1900, 9999),
                format(segmentoJudiciario, 1, 1, 9),
                format(tribunal, 2, 0, 99),
                format(unidadeOrigem, 4, 0, 9999)
        );
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NumeroProcesso && Objects.equals(valor, ((NumeroProcesso) o).valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    public static boolean validar(String numero) {
        String normalized = normalize(numero);
        if (normalized == null || !CNJ_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        String digits = onlyDigits(normalized);
        String sequencial = digits.substring(0, 7);
        String dv = digits.substring(7, 9);
        String ano = digits.substring(9, 13);
        String segmento = digits.substring(13, 14);
        String tribunal = digits.substring(14, 16);
        String unidade = digits.substring(16, 20);
        return dv.equals(calcularDigitoVerificador(sequencial, ano, segmento, tribunal, unidade))
                && mod97(sequencial + ano + segmento + tribunal + unidade + dv) == 1;
    }

    public static NumeroProcesso gerar() {
        return gerar(TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                "0001", "0001", "06");
    }

    @Override
    public String toString() {
        return valor;
    }

    private static String calcularDigitoVerificador(String sequencial, String ano, String segmentoJudiciario, String tribunal, String unidadeOrigem) {
        int resto = mod97(sequencial + ano + segmentoJudiciario + tribunal + unidadeOrigem + "00");
        return String.format("%02d", 98 - resto);
    }

    private static int mod97(String digits) {
        int mod = 0;
        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            if (!Character.isDigit(c)) {
                throw new IllegalArgumentException("Sequência numérica inválida");
            }
            mod = (mod * 10 + Character.digit(c, 10)) % 97;
        }
        return mod;
    }

    private static String format(long value, int width, long min, long max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("Valor fora do intervalo CNJ");
        }
        return String.format("%0" + width + "d", value);
    }

    private static long sequencialPadrao() {
        long value = Math.floorMod(System.currentTimeMillis(), 9_999_999L);
        return value == 0L ? 1L : value;
    }

    private static int tribunalPadrao(TipoJustica justica, String codigoUF) {
        if (justica == TipoJustica.FEDERAL) {
            return 5;
        }
        return switch (normalizeText(codigoUF)) {
            case "AC" -> 1;
            case "AL" -> 2;
            case "AP" -> 3;
            case "AM" -> 4;
            case "BA" -> 5;
            case "CE" -> 6;
            case "DF" -> 7;
            case "ES" -> 8;
            case "GO" -> 9;
            case "MA" -> 10;
            case "MT" -> 11;
            case "MS" -> 12;
            case "MG" -> 13;
            case "PA" -> 14;
            case "PB" -> 15;
            case "PR" -> 16;
            case "PE" -> 17;
            case "PI" -> 18;
            case "RJ" -> 19;
            case "RN" -> 20;
            case "RS" -> 21;
            case "RO" -> 22;
            case "RR" -> 23;
            case "SC" -> 24;
            case "SE" -> 25;
            case "SP" -> 26;
            case "TO" -> 27;
            default -> 6;
        };
    }

    private static int numericValue(String raw, int fallback) {
        String digits = onlyDigits(raw);
        if (digits == null || digits.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(digits.length() > 4 ? digits.substring(digits.length() - 4) : digits);
    }

    private static String normalize(String numero) {
        return numero == null ? null : numero.trim();
    }

    private static String onlyDigits(String raw) {
        return raw == null ? null : raw.replaceAll("\\D+", "");
    }

    private static String firstNonBlank(String first, String second) {
        String a = first == null ? null : first.trim();
        if (a != null && !a.isBlank()) {
            return a;
        }
        String b = second == null ? null : second.trim();
        return b == null || b.isBlank() ? null : b;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
