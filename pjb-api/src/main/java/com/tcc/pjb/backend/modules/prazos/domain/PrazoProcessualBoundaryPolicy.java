package com.tcc.pjb.backend.modules.prazos.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class PrazoProcessualBoundaryPolicy {

    private static final Pattern CODIGO_TRIBUNAL = Pattern.compile("[A-Z0-9._-]{2,16}");
    private static final Pattern UF = Pattern.compile("[A-Z]{2}");
    private static final Pattern TOKEN_ENUM = Pattern.compile("[A-Z0-9_]{2,80}");
    private static final int LIMITE_DIAS_OVERRIDE = 3650;
    private static final LocalDate DATA_MINIMA = LocalDate.of(1900, 1, 1);
    private static final LocalDate DATA_MAXIMA = LocalDate.of(2100, 12, 31);

    public PrazoProcessualParametros validarCalculo(LocalDate dataInicio,
                                                    String tipoPrazo,
                                                    String ramo,
                                                    String grau,
                                                    String tribunalCodigo,
                                                    String uf,
                                                    String comarca,
                                                    Integer diasOverride) {
        LocalDate data = validarData(dataInicio, "Data inicial obrigatoria.");
        String tipo = normalizarTokenObrigatorio(tipoPrazo, "Tipo de prazo obrigatorio.");
        String ramoNormalizado = normalizarTokenObrigatorio(ramo, "Ramo do direito obrigatorio.");
        String grauNormalizado = normalizarTokenObrigatorio(grau, "Grau de jurisdicao obrigatorio.");
        String tribunal = normalizarTribunal(tribunalCodigo);
        String ufNormalizada = normalizarUf(uf);
        String comarcaNormalizada = normalizarComarca(comarca);
        Integer dias = validarDiasOverride(diasOverride);
        return new PrazoProcessualParametros(data, tipo, ramoNormalizado, grauNormalizado, tribunal, ufNormalizada, comarcaNormalizada, dias);
    }

    public PrazoProcessualParametros validarDiaForense(LocalDate data,
                                                       String tribunalCodigo,
                                                       String uf,
                                                       String comarca,
                                                       String ramo,
                                                       String grau) {
        LocalDate dataNormalizada = validarData(data, "Data de analise obrigatoria.");
        String ramoNormalizado = normalizarTokenObrigatorio(ramo, "Ramo do direito obrigatorio.");
        String grauNormalizado = normalizarTokenObrigatorio(grau, "Grau de jurisdicao obrigatorio.");
        String tribunal = normalizarTribunal(tribunalCodigo);
        String ufNormalizada = normalizarUf(uf);
        String comarcaNormalizada = normalizarComarca(comarca);
        return new PrazoProcessualParametros(dataNormalizada, null, ramoNormalizado, grauNormalizado, tribunal, ufNormalizada, comarcaNormalizada, null);
    }

    public boolean exigeConferenciaManual(PrazoProcessualParametros parametros,
                                          boolean marcoInicialDiaUtil,
                                          List<String> advertencias) {
        if (parametros.diasOverride() != null) {
            return true;
        }
        if (!marcoInicialDiaUtil) {
            return true;
        }
        return advertencias != null && advertencias.stream().anyMatch(item -> item != null && !item.isBlank());
    }

    public boolean exigeConferenciaManualDiaForense(PrazoProcessualParametros parametros,
                                                    boolean diaUtil,
                                                    String motivo) {
        if (!diaUtil) {
            return true;
        }
        return motivo != null && motivo.toUpperCase(Locale.ROOT).contains("REVISAO");
    }

    private LocalDate validarData(LocalDate data, String message) {
        if (data == null) {
            throw new PrazoProcessualDomainException(message);
        }
        if (data.isBefore(DATA_MINIMA) || data.isAfter(DATA_MAXIMA)) {
            throw new PrazoProcessualDomainException("Data fora da janela operacional do modulo de prazos.");
        }
        return data;
    }

    private String normalizarTokenObrigatorio(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PrazoProcessualDomainException(message);
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (!TOKEN_ENUM.matcher(normalized).matches()) {
            throw new PrazoProcessualDomainException("Token processual invalido para prazos.");
        }
        return normalized;
    }

    private String normalizarTribunal(String value) {
        if (value == null || value.isBlank()) {
            throw new PrazoProcessualDomainException("Codigo do tribunal obrigatorio.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!CODIGO_TRIBUNAL.matcher(normalized).matches()) {
            throw new PrazoProcessualDomainException("Codigo do tribunal invalido.");
        }
        return normalized;
    }

    private String normalizarUf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!UF.matcher(normalized).matches()) {
            throw new PrazoProcessualDomainException("UF invalida.");
        }
        return normalized;
    }

    private String normalizarComarca(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 120) {
            throw new PrazoProcessualDomainException("Comarca excede tamanho operacional permitido.");
        }
        return normalized;
    }

    private Integer validarDiasOverride(Integer diasOverride) {
        if (diasOverride == null) {
            return null;
        }
        if (diasOverride < 1 || diasOverride > LIMITE_DIAS_OVERRIDE) {
            throw new PrazoProcessualDomainException("Dias de prazo fora do limite operacional permitido.");
        }
        return diasOverride;
    }
}
