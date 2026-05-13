package com.tcc.pjb.backend.modules.auditoria;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationLanguageHeuristics;

@Component
public class MotorAnaliseComportamental {

    public double analisarRisco(String acao, String detalhes) {
        String texto = normalize(acao) + " " + normalize(detalhes);
        if (texto.contains("exclusão") || texto.contains("alteração")) return 0.9;
        if (texto.contains("erro") || texto.contains("falha")) return 0.7;
        if (containsAcordoPositivo(texto) || texto.contains("juiz")) return 0.4;
        return 0.2;
    }

    public String definirPerfil(String acao, String detalhes) {
        String texto = normalize(acao) + " " + normalize(detalhes);
        if (containsAcordoPositivo(texto)) return "jurisdicional_formal";
        if (texto.contains("cliente")) return "advocacia_operacional";
        if (texto.contains("processo")) return "gestao_documental";
        return "neutro";
    }

    private String normalize(String valor) {
        return valor == null ? "" : valor.toLowerCase();
    }

    private boolean containsAcordoPositivo(String texto) {
        return NegotiationLanguageHeuristics.containsPositiveSettlementSignal(texto);
    }
}
