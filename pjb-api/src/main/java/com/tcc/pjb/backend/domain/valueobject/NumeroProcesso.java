package com.tcc.pjb.backend.domain.valueobject;

import java.time.Year;
import java.util.Objects;
import java.util.UUID;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public final class NumeroProcesso {

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
        String sequencialUnico = UUID.randomUUID().toString().substring(0, 7).replace("-", "");

        String base = String.format(
                "%s-%d.%s.%s.%s.%s.%s",
                sequencialUnico,
                ano,
                justica.getCodigoCNJ(),
                ramo.getCodigo(),
                codigoUF,
                codigoComarca,
                codigoUnidade
        );

        int dv = calcularDV(base);
        return new NumeroProcesso(base + "-" + dv);
    }

    private static int calcularDV(String base) {
        int soma = 0;
        for (char c : base.toCharArray()) {
            soma += Character.isDigit(c) ? Character.getNumericValue(c) : 0;
        }
        return soma % 97;
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
        if (numero == null) return false;
        
        
        String n = numero.replaceAll("\\D", "");
        
        return n.length() == 20;
    }

    
    public static NumeroProcesso gerar() {
        return gerar(com.tcc.pjb.backend.domain.enums.TipoJustica.ESTADUAL,
                com.tcc.pjb.backend.model.entity.enums.RamoDireito.CIVIL,
                "0001", "0001", "06");
    }

}
