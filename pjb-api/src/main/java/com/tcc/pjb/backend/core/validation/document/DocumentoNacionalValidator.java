package com.tcc.pjb.backend.core.validation.document;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DocumentoNacionalValidator {

    private static final Pattern NON_DIGIT = Pattern.compile("\\D+");
    private static final Pattern MULTISPACE = Pattern.compile("\\s+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern SEARCH_KEY_SANITIZER = Pattern.compile("[^A-Z0-9 ]+");

    public enum TipoDocumento {
        CPF,
        CNPJ
    }

    public String normalizarDocumento(String raw) {
        if (raw == null) {
            return "";
        }
        return NON_DIGIT.matcher(raw).replaceAll("");
    }

    public TipoDocumento detectarTipoDocumento(String raw) {
        String documento = normalizarDocumento(raw);
        if (documento.length() == 11) {
            return TipoDocumento.CPF;
        }
        if (documento.length() == 14) {
            return TipoDocumento.CNPJ;
        }
        throw new IllegalArgumentException("Documento deve possuir 11 ou 14 digitos: " + documento);
    }

    public boolean cpfValido(String raw) {
        String cpf = normalizarDocumento(raw);
        if (cpf.length() != 11 || todosDigitosIguais(cpf)) {
            return false;
        }

        int[] digits = toDigits(cpf);

        int soma1 = 0;
        for (int i = 0; i < 9; i++) {
            soma1 += digits[i] * (10 - i);
        }
        int dv1 = (soma1 * 10) % 11;
        if (dv1 == 10) {
            dv1 = 0;
        }
        if (dv1 != digits[9]) {
            return false;
        }

        int soma2 = 0;
        for (int i = 0; i < 10; i++) {
            soma2 += digits[i] * (11 - i);
        }
        int dv2 = (soma2 * 10) % 11;
        if (dv2 == 10) {
            dv2 = 0;
        }
        return dv2 == digits[10];
    }

    public boolean cnpjValido(String raw) {
        String cnpj = normalizarDocumento(raw);
        if (cnpj.length() != 14 || todosDigitosIguais(cnpj)) {
            return false;
        }

        int[] digits = toDigits(cnpj);
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int soma1 = 0;
        for (int i = 0; i < 12; i++) {
            soma1 += digits[i] * pesos1[i];
        }
        int resto1 = soma1 % 11;
        int dv1 = resto1 < 2 ? 0 : 11 - resto1;
        if (dv1 != digits[12]) {
            return false;
        }

        int soma2 = 0;
        for (int i = 0; i < 13; i++) {
            soma2 += digits[i] * pesos2[i];
        }
        int resto2 = soma2 % 11;
        int dv2 = resto2 < 2 ? 0 : 11 - resto2;
        return dv2 == digits[13];
    }

    public void validarDocumento(String raw) {
        TipoDocumento tipo = detectarTipoDocumento(raw);
        boolean valido = tipo == TipoDocumento.CPF ? cpfValido(raw) : cnpjValido(raw);
        if (!valido) {
            throw new IllegalArgumentException(tipo + " invalido: " + normalizarDocumento(raw));
        }
    }

    public String formatarDocumento(String raw) {
        String documento = normalizarDocumento(raw);
        TipoDocumento tipo = detectarTipoDocumento(documento);
        return tipo == TipoDocumento.CPF ? formatarCpf(documento) : formatarCnpj(documento);
    }

    public String mascararDocumento(String raw) {
        String documento = normalizarDocumento(raw);
        TipoDocumento tipo = detectarTipoDocumento(documento);
        return tipo == TipoDocumento.CPF
                ? "***.***.***-" + documento.substring(9, 11)
                                        : "**.**.***/***-" + documento.substring(12, 14);
    }

    public String buildSearchKey(String raw) {
        if (raw == null) return "";
        String normalized = Normalizer.normalize(raw.toUpperCase(Locale.ROOT), Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        normalized = SEARCH_KEY_SANITIZER.matcher(normalized).replaceAll("");
        return MULTISPACE.matcher(normalized).replaceAll(" ").trim();
    }

    public String normalizarNomeCanonico(String raw) {
        return buildSearchKey(raw);
    }

    public String gerarChavePesquisa(String raw) {
        return buildSearchKey(raw);
    }

    private String formatarCpf(String doc) {
        return doc.substring(0, 3) + "." + doc.substring(3, 6) + "." + doc.substring(6, 9) + "-" + doc.substring(9, 11);
    }

    private String formatarCnpj(String doc) {
        return doc.substring(0, 2) + "." + doc.substring(2, 5) + "." + doc.substring(5, 8)
                + "/" + doc.substring(8, 12) + "-" + doc.substring(12, 14);
    }

    private boolean todosDigitosIguais(String s) {
        char c = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != c) return false;
        }
        return true;
    }

    private int[] toDigits(String s) {
        int[] d = new int[s.length()];
        for (int i = 0; i < s.length(); i++) {
            d[i] = s.charAt(i) - '0';
        }
        return d;
    }
}
