package com.tcc.pjb.backend.service.processual.peticionamento.journey;

import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;

final class PeticionamentoProtocolProgressSupport {

    private PeticionamentoProtocolProgressSupport() {
    }

    static String resolveStepStatus(String code, PeticionamentoSessaoRequest request, ProceduralSubmissionBlueprintReport blueprint) {
        return switch (PeticionamentoJourneyPayloadSupport.normalizeUpper(code)) {
            case "TRIAGEM_MATERIAL" -> hasMinimalCaseIdentity(request) ? "CONCLUIDA" : "PENDENTE";
            case "COMPETENCIA_E_ORGAO" -> blueprint != null && blueprint.blockingIssues().isEmpty() ? "CONCLUIDA" : "BLOQUEADA";
            case "PARTES_E_REPRESENTACAO" -> hasPartyData(request) ? "CONCLUIDA" : "PENDENTE";
            case "PROVA_E_DOCUMENTOS" -> hasEvidence(request) ? "CONCLUIDA" : "PENDENTE";
            case "PEDIDOS_E_URGENCIA" -> hasClaims(request) ? "CONCLUIDA" : "PENDENTE";
            case "ASSINATURA_E_PROTOCOLO" -> blueprint != null && blueprint.readyForRealConnectorSubmission() ? "PRONTA" : blueprint != null && blueprint.readyForAssistedSubmission() ? "ASSISTIDA" : "PENDENTE";
            case "DISTRIBUICAO_E_ACOMPANHAMENTO" -> blueprint != null && blueprint.readyForRealConnectorSubmission() ? "POS_PROTOCOLO" : "AGUARDANDO_PROTOCOLO";
            default -> "PENDENTE";
        };
    }

    static boolean hasMinimalCaseIdentity(PeticionamentoSessaoRequest request) {
        return PeticionamentoJourneyPayloadSupport.filled(request == null ? null : request.getTituloCaso())
                || PeticionamentoJourneyPayloadSupport.filled(request == null ? null : request.getClasseProcessual())
                || PeticionamentoJourneyPayloadSupport.filled(request == null ? null : request.getTextoFatosResumido())
                || !PeticionamentoJourneyPayloadSupport.safeList(request == null ? null : request.getFatos()).isEmpty();
    }

    static boolean hasPartyData(PeticionamentoSessaoRequest request) {
        return PeticionamentoJourneyPayloadSupport.filled(request == null ? null : request.getParteAutora())
                || PeticionamentoJourneyPayloadSupport.filled(request == null ? null : request.getParteRe());
    }

    static boolean hasEvidence(PeticionamentoSessaoRequest request) {
        return !PeticionamentoJourneyPayloadSupport.safeList(request == null ? null : request.getProvasIndicadas()).isEmpty()
                || !PeticionamentoJourneyPayloadSupport.safeList(request == null ? null : request.getDocumentosAnexados()).isEmpty()
                || !PeticionamentoJourneyPayloadSupport.safeList(request == null ? null : request.getProvasDocumentais()).isEmpty()
                || !PeticionamentoJourneyPayloadSupport.safeList(request == null ? null : request.getMidiaInline()).isEmpty();
    }

    static boolean hasClaims(PeticionamentoSessaoRequest request) {
        return !PeticionamentoJourneyPayloadSupport.safeList(request == null ? null : request.getPedidos()).isEmpty()
                || PeticionamentoJourneyPayloadSupport.filled(request == null ? null : request.getTextoPeticaoLivre())
                || Boolean.TRUE.equals(request == null ? null : request.getRequerLiminar())
                || Boolean.TRUE.equals(request == null ? null : request.getTutelaUrgencia());
    }
}
