package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessLaneBlueprintResponse;
import java.util.List;

public record NationalCommunicationInstitutionalOrganizationBlueprintResponse(
        String codigo,
        String scope,
        String nomeExibicao,
        String destinatarioInstitucionalKind,
        String organizacaoKind,
        String entryMode,
        String trustFloorPadrao,
        boolean requerCertificadoICP,
        boolean restringeCertificadoRedeInstitucional,
        boolean permiteUsoRemotoComAutorizacao,
        boolean requerDuplaAprovacaoAdministrador,
        List<NationalCommunicationInstitutionalAccessLaneBlueprintResponse> lanes,
        List<String> fundamentos
) {
}