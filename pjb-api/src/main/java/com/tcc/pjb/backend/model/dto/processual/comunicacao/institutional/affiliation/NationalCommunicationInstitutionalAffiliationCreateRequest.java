package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

import java.util.List;

public record NationalCommunicationInstitutionalAffiliationCreateRequest(
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
        String emailContatoSeguranca,
        String representativeRole,
        String trustFloor,
        Boolean requerDuplaAprovacaoAdministrador,
        Boolean requerCertificadoICP,
        Boolean restringeCertificadoRedeInstitucional,
        Boolean permiteUsoRemotoComAutorizacao,
        List<String> canaisHabilitados,
        List<String> politicaCiencia,
        List<String> sla,
        List<String> regrasFallback,
        List<String> conveniosIntegracoes,
        List<String> fundamentos
) {
}
