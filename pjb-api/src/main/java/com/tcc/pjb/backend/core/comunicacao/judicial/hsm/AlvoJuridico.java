package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

import java.util.Locale;

public record AlvoJuridico(
        Long processoId,
        String processoNumero,
        String documentoUnico,
        String nomeDestinatario,
        boolean pessoaJuridica,
        String emailPrincipal,
        String telefonePrincipal,
        String enderecoPrincipal,
        boolean exigeCadeiaCertificacao,
        String fundamentoLegal,
        long timeoutMs
) {
    public AlvoJuridico {
        if (documentoUnico == null || documentoUnico.isBlank()) {
            throw new IllegalArgumentException("documentoUnico é obrigatório");
        }
        processoNumero = trimToNull(processoNumero);
        documentoUnico = normalizarDocumento(documentoUnico);
        nomeDestinatario = trimToNull(nomeDestinatario);
        emailPrincipal = lowerTrim(emailPrincipal);
        telefonePrincipal = digitsOrNull(telefonePrincipal);
        enderecoPrincipal = trimToNull(enderecoPrincipal);
        fundamentoLegal = trimToNull(fundamentoLegal);
        timeoutMs = timeoutMs > 0 ? timeoutMs : 8_000L;
    }

    public AlvoJuridico(Long processoId,
                        String processoNumero,
                        String documentoUnico,
                        String nomeDestinatario,
                        boolean pessoaJuridica,
                        String emailPrincipal,
                        String telefonePrincipal,
                        String enderecoPrincipal,
                        long timeoutMs) {
        this(
                processoId,
                processoNumero,
                documentoUnico,
                nomeDestinatario,
                pessoaJuridica,
                emailPrincipal,
                telefonePrincipal,
                enderecoPrincipal,
                false,
                null,
                timeoutMs
        );
    }

    public AlvoJuridico(String documentoUnico,
                        boolean exigeCadeiaCertificacao,
                        String processoNumero,
                        Long processoId,
                        String nomeDestinatario,
                        boolean pessoaJuridica,
                        String fundamentoLegal,
                        int timeoutMs) {
        this(
                processoId,
                processoNumero,
                documentoUnico,
                nomeDestinatario,
                pessoaJuridica,
                null,
                null,
                null,
                exigeCadeiaCertificacao,
                fundamentoLegal,
                timeoutMs
        );
    }

    public AlvoJuridico(String documentoUnico, boolean exigeCadeiaCertificacao) {
        this(documentoUnico, exigeCadeiaCertificacao, null, null, null, false, null, 8_000);
    }

    public String nomeExibicao() {
        return nomeDestinatario;
    }

    public boolean possuiContatoDigital() {
        return emailPrincipal != null || telefonePrincipal != null;
    }

    public boolean possuiEnderecoFisico() {
        return enderecoPrincipal != null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String lowerTrim(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static String digitsOrNull(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String digits = trimmed.replaceAll("\\D+", "");
        return digits.isBlank() ? null : digits;
    }

    private static String normalizarDocumento(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String digits = trimmed.replaceAll("\\D+", "");
        return digits.isBlank() ? trimmed.toUpperCase(Locale.ROOT) : digits;
    }
}
