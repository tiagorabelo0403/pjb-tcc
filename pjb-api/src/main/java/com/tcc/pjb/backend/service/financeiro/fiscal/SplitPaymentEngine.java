package com.tcc.pjb.backend.service.financeiro.fiscal;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.Processo;

@Component
public class SplitPaymentEngine {

    
    public void validarSplitPayment(Processo processo) {
        if (processo == null) {
            throw new IllegalArgumentException("Processo não informado");
        }
        BigDecimal valor = processo.getValorCausa();
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalStateException("Valor da causa inválido para cálculo tributário");
        }
    }
}
