package com.tcc.pjb.backend.core.procedural;

import org.springframework.stereotype.Component;

@Component
public class NationalProceduralForumAllocationMessages {

    public String connectorNotHomologated() {
        return "Conector judicial ainda não homologado para protocolo completo no tribunal sugerido.";
    }

    public String connectorCertificateChecklist() {
        return "Providenciar assinatura qualificada compatível com o conector do tribunal.";
    }

    public String connectorGovBrChecklist() {
        return "Exigir step-up Gov.br antes do envio do protocolo real.";
    }

    public String territorialExpressForumReason() {
        return "Âncora territorial extraída de indicação expressa no pedido ou no payload.";
    }

    public String territorialFederalDomicileReason() {
        return "Competência federal/previdenciária ancorada no domicílio do autor.";
    }

    public String territorialLaborReason() {
        return "Competência trabalhista prioriza o local da prestação de serviços.";
    }

    public String territorialPenalReason() {
        return "Indícios penais favorecem a territorialidade associada ao local do fato ou da persecução.";
    }

    public String territorialResolvedBaseReason() {
        return "Base territorial consolidada a partir da resolução competencial primária.";
    }

    public String territorialAuthorFallbackReason() {
        return "Fallback territorial pelo domicílio do autor.";
    }

    public String territorialDefendantFallbackReason() {
        return "Fallback territorial pelo domicílio do réu.";
    }

    public String territorialUndefinedReason() {
        return "Não houve elementos territoriais suficientes para consolidar comarca e UF.";
    }

    public String linkageContinenciaReason() {
        return "Texto e payload sugerem continência material.";
    }

    public String linkageDependenciaReason() {
        return "Texto e payload sugerem distribuição por dependência.";
    }

    public String linkageConexaoReason() {
        return "Texto e payload sugerem conexão entre feitos.";
    }

    public String linkagePreventionReason() {
        return "Há sinais de prevenção do juízo.";
    }

    public String linkageRelatedReason() {
        return "Foram identificados processos relacionados no payload/corpus.";
    }

    public String linkageRelatedCountReason(int total) {
        return "Números CNJ relacionados detectados: " + Math.max(total, 0) + '.';
    }
}
