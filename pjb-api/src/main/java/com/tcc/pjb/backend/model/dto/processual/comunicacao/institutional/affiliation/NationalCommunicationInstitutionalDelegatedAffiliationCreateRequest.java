package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalBootstrapAdministratorRequest;
import java.util.List;

public record NationalCommunicationInstitutionalDelegatedAffiliationCreateRequest(
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
        String representativeName,
        String representativeRole,
        List<NationalCommunicationInstitutionalBootstrapAdministratorRequest> bootstrapAdministrators,
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
        List<NationalCommunicationInstitutionalAffiliationDocumentRequest> documentos,
        List<String> fundamentos
) {
}