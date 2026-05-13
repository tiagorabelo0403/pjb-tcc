package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalAffiliationResponse(
        String affiliationId,
        String destinatarioInstitucionalKind,
        String organizationScope,
        String blueprintCode,
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
        String representativeRole,
        List<String> canaisHabilitados,
        List<String> politicaCiencia,
        List<String> sla,
        List<String> regrasFallback,
        List<String> conveniosIntegracoes,
        String trustFloor,
        boolean requerDuplaAprovacaoAdministrador,
        boolean requerCertificadoICP,
        boolean restringeCertificadoRedeInstitucional,
        boolean permiteUsoRemotoComAutorizacao,
        String status,
        List<String> fundamentos,
        Instant createdAt,
        Instant updatedAt
) {
}
