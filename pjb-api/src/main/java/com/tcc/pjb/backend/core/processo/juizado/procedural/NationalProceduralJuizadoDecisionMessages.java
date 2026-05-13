package com.tcc.pjb.backend.core.processo.juizado.procedural;

import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJuizadoDecisionMessages {

    public String excludedBySpecialNatureAlert() {
        return "A natureza da ação aponta trilha especial ou excluída do regime dos juizados.";
    }

    public String excludedBySpecialNatureChecklist() {
        return "Confirmar exclusão do sistema dos juizados conforme a classe processual efetiva.";
    }

    public String federalJuizadoValueMissingAlert() {
        return "Valor da causa ausente para fechar aderência ao JEF.";
    }

    public String federalJuizadoValueMissingChecklist() {
        return "Informar valor da causa para testar alçada do juizado federal.";
    }

    public String federalJuizadoReason() {
        return "Parte federal e faixa econômica compatível com Juizado Especial Federal.";
    }

    public String federalJuizadoLegalBase() {
        return "Lei 10.259/2001";
    }

    public String federalJuizadoComplexEvidenceAlert() {
        return "Prova técnica densa recomenda revisão humana antes do fechamento em JEF.";
    }

    public String federalJuizadoComplexEvidenceChecklist() {
        return "Verificar se a prova admitirá técnica simplificada compatível com juizado.";
    }

    public String fazendaJuizadoValueMissingAlert() {
        return "Valor da causa ausente para validar alçada da Fazenda Pública em juizado.";
    }

    public String fazendaJuizadoValueMissingChecklist() {
        return "Informar valor da causa e ente público demandado para confirmar cabimento do JFP.";
    }

    public String fazendaJuizadoReason() {
        return "Ente estadual ou municipal e valor compatível com Juizado Especial da Fazenda Pública.";
    }

    public String fazendaJuizadoLegalBase() {
        return "Lei 12.153/2009";
    }

    public String civelJuizadoValueMissingAlert() {
        return "Valor da causa ausente para teste de aderência ao Juizado Especial Cível.";
    }

    public String civelJuizadoValueMissingChecklist() {
        return "Informar valor da causa para aferir alçada do JEC.";
    }

    public String civelJuizadoReason() {
        return "Matéria de menor complexidade com valor compatível com Juizado Especial Cível.";
    }

    public String civelJuizadoLegalBase() {
        return "Lei 9.099/1995";
    }

    public String civelJuizadoComplexEvidenceAlert() {
        return "Complexidade probatória reduz a segurança do fechamento em juizado.";
    }

    public String civelJuizadoComplexEvidenceChecklist() {
        return "Verificar o objeto da prova e a possibilidade de técnica simplificada antes de manter o juizado.";
    }

    public String jecrimReason() {
        return "Marcadores de menor potencial ofensivo sugerem trilha do Juizado Especial Criminal, sujeita à conferência da capitulação final.";
    }

    public String jecrimLegalBase() {
        return "Lei 9.099/1995";
    }

    public String jecrimChecklist() {
        return "Confirmar a capitulação penal e a pena em abstrato antes de travar o fluxo do JECRIM.";
    }
}
