package com.tcc.pjb.backend.core.procedural;

import java.util.Collection;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingMessages {

    public String economicValueMissingReason() {
        return "Valor da causa ainda não permite fechar a aderência econômica do procedimento.";
    }

    public String economicExceededReason(String fundamento) {
        return nonBlankOrDefault(fundamento, "Excesso econômico detectado para a via selecionada.");
    }

    public String economicNearThresholdReason() {
        return "Faixa econômica no limite operacional do procedimento selecionado.";
    }

    public String economicCompatibleReason() {
        return "Faixa econômica compatível com o rito econômico sugerido.";
    }

    public String economicNoThresholdReason() {
        return "Não foi identificado teto econômico legal vinculante para a trilha sugerida.";
    }

    public String rerouteFederalCommon() {
        return "Migrar para vara federal comum.";
    }

    public String rerouteStateCommon() {
        return "Migrar para vara cível comum.";
    }

    public String rerouteFederalPrevidenciarioCommon() {
        return "Migrar para vara federal previdenciária comum.";
    }

    public String rerouteFazendaComum() {
        return "Migrar para vara da Fazenda Pública com rito comum.";
    }

    public String rerouteTrabalhistaOrdinario() {
        return "Migrar para reclamação trabalhista em rito ordinário.";
    }

    public String rerouteGenericReview() {
        return "Revisar competência e rito econômico antes do protocolo.";
    }

    public String rerouteCompetenciaContingencia(String competencia) {
        return "Competência de contingência: " + Objects.requireNonNull(competencia).trim() + ".";
    }

    public String rerouteRitoContingencia(String rito) {
        return "Rito de contingência: " + Objects.requireNonNull(rito).trim() + ".";
    }

    public String economicInputsChecklist(Collection<String> missingInputs) {
        return "Completar insumos mínimos para memória econômica segura: " + String.join(", ", missingInputs) + ".";
    }

    private String nonBlankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
