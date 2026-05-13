package com.tcc.pjb.backend.model.dto.processual.recursal.documental;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;

public record RecursalDocumentalArtifactRequest(
        RecursalAutomationRequest contexto,
        String processoReferencia,
        String artefatoId,
        String categoriaArtefato,
        boolean sigiloso,
        boolean certificadoDisponivel,
        boolean assinaturaQualificada,
        boolean midiaAudiovisual) {
}
