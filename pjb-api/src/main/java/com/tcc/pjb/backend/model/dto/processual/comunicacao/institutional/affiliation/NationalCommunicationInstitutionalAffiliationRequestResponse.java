package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalBootstrapAdministratorRequest;
import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalAffiliationRequestResponse(
        String requestId,
        String destinatarioInstitucionalKind,
        String organizationScope,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String unidadeNome,
        String uf,
        String comarca,
        String cnpj,
        String esferaAdministrativa,
        List<String> ramosMateriais,
        List<String> abrangenciasTerritoriais,
        String dominioInstitucional,
        String autoridadeAderenteCargo,
        Long representanteUsuarioId,
        String representanteNome,
        String representativeRole,
        List<NationalCommunicationInstitutionalBootstrapAdministratorRequest> bootstrapAdministrators,
        String trustFloorProposto,
        boolean requerDuplaAprovacaoAdministrador,
        boolean requerCertificadoICP,
        boolean restringeCertificadoRedeInstitucional,
        boolean permiteUsoRemotoComAutorizacao,
        List<String> canaisHabilitados,
        List<String> politicaCiencia,
        List<String> sla,
        List<String> regrasFallback,
        List<String> conveniosIntegracoes,
        List<NationalCommunicationInstitutionalAffiliationDocumentResponse> documentos,
        String status,
        String materializedAffiliationId,
        List<String> fundamentos,
        Instant createdAt,
        Instant decidedAt,
        Instant updatedAt
) {
}