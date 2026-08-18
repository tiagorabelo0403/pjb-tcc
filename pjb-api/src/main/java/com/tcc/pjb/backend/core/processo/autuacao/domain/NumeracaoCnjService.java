package com.tcc.pjb.backend.core.processo.autuacao.domain;

public final class NumeracaoCnjService {

    public String gerar(int sequencial, int ano, int segmento,
                        int codigoTribunal, int codigoUnidade) {
        int dd = calcularDigitoVerificador(sequencial, ano, segmento, codigoTribunal, codigoUnidade);
        return String.format("%07d-%02d.%04d.%d.%02d.%04d",
                sequencial, dd, ano, segmento, codigoTribunal, codigoUnidade);
    }

    public boolean validar(String numeroUnificado) {
        if (numeroUnificado == null || numeroUnificado.isBlank()) {
            return false;
        }
        String limpo = numeroUnificado.replaceAll("[^0-9]", "");
        if (limpo.length() != 20) {
            return false;
        }
        try {
            int seq = Integer.parseInt(limpo.substring(0, 7));
            int dd = Integer.parseInt(limpo.substring(7, 9));
            int ano = Integer.parseInt(limpo.substring(9, 13));
            int seg = Integer.parseInt(limpo.substring(13, 14));
            int trib = Integer.parseInt(limpo.substring(14, 16));
            int uni = Integer.parseInt(limpo.substring(16, 20));
            int ddCalculado = calcularDigitoVerificador(seq, ano, seg, trib, uni);
            return dd == ddCalculado;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public int calcularDigitoVerificador(long sequencial, int ano,
                                          int segmento, int tribunal, int unidade) {
        String semDd = String.format("%07d%04d%d%02d%04d", sequencial, ano, segmento, tribunal, unidade);
        long numero = Long.parseLong(semDd);
        long resto = (numero * 100L) % 97L;
        int resultado = (int) (98L - resto);
        if (resultado > 97) {
            resultado -= 97;
        }
        return resultado;
    }
}
