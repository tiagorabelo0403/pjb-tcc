package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalDelegatedAffiliationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOfficialSourceDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalPublicRecognitionGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalTrustMatrixApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustMatrixEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalDelegatedGovernanceClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalDelegatedGovernanceClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialIdentifierDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceAttestationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestationItem;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationRequestResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegatedAffiliationDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustMatrixEntryResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalAffiliationGovernanceSurfaceServiceTest {

    private final InstitutionalDelegatedGovernanceClosureApplicationService delegatedGovernanceClosureApplicationService = mock(InstitutionalDelegatedGovernanceClosureApplicationService.class);
    private final InstitutionalDelegatedAffiliationApplicationService delegatedAffiliationApplicationService = mock(InstitutionalDelegatedAffiliationApplicationService.class);
    private final InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService = mock(InstitutionalPublicRecognitionGateApplicationService.class);
    private final InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService = mock(InstitutionalOfficialSourceDossierApplicationService.class);
    private final InstitutionalOfficialIdentifierDossierApplicationService officialIdentifierDossierApplicationService = mock(InstitutionalOfficialIdentifierDossierApplicationService.class);
    private final InstitutionalOfficialSourceAttestationApplicationService officialSourceAttestationApplicationService = mock(InstitutionalOfficialSourceAttestationApplicationService.class);
    private final InstitutionalTrustMatrixApplicationService trustMatrixApplicationService = mock(InstitutionalTrustMatrixApplicationService.class);
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport = mock(NationalCommunicationInstitutionalSurfaceAssemblerSupport.class);
    private final NationalCommunicationInstitutionalAffiliationGovernanceSurfaceService service = new NationalCommunicationInstitutionalAffiliationGovernanceSurfaceService(
            delegatedGovernanceClosureApplicationService, delegatedAffiliationApplicationService, publicRecognitionGateApplicationService,
            officialSourceDossierApplicationService, officialIdentifierDossierApplicationService, officialSourceAttestationApplicationService,
            trustMatrixApplicationService, surfaceAssemblerSupport);

    @Test
    void fechamentoDelegadoDelegaEMapeia() {
        var domain = mock(InstitutionalDelegatedGovernanceClosure.class);
        var response = mock(NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse.class);
        when(delegatedGovernanceClosureApplicationService.consolidar("BR")).thenReturn(domain);
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.fechamentoDelegado("BR")).isSameAs(response);
    }

    @Test
    void homologarAdesaoDelegadaPassaAprovarEFundamentosDoRequest() {
        var request = new NationalCommunicationInstitutionalDelegatedAffiliationDecisionRequest(true, List.of("fundamento-1"));
        var domain = mock(InstitutionalAffiliationRequest.class);
        var response = mock(NationalCommunicationInstitutionalAffiliationRequestResponse.class);
        when(delegatedAffiliationApplicationService.homologarSolicitacao("req-1", true, List.of("fundamento-1"))).thenReturn(domain);
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.homologarAdesaoDelegada("req-1", request)).isSameAs(response);
    }

    @Test
    void reconhecimentoPublicoAdesaoDelegadaDelegaSemMapeamentoExtra() {
        var response = mock(AdminInstitutionalPublicRecognitionResponse.class);
        when(publicRecognitionGateApplicationService.avaliarSolicitacao("req-2")).thenReturn(response);

        assertThat(service.reconhecimentoPublicoAdesaoDelegada("req-2")).isSameAs(response);
    }

    @Test
    void atestacaoFontesOficiaisAdesaoDelegadaMapeiaTodosOsCamposSemPerda() {
        Instant agora = Instant.parse("2026-08-31T12:00:00Z");
        var item = new InstitutionalOfficialSourceAttestationItem(
                "CNJ", "Conselho Nacional de Justiça", "AUTORIDADE_JUDICIARIA", "NACIONAL", "API", "AUTOMATICO",
                true, true, true, true, true, false, false, 95, "ALTA", agora, agora, "hash-item",
                "CONECTADO", true, true, "https://cnj.jus.br", agora, agora, List.of("sinal-1"), List.of(), List.of("evidencia-1"), List.of(), List.of("proximo-passo-1"), List.of("fundamento-item")
        );
        var attestation = new InstitutionalOfficialSourceAttestation(
                "ORGAO", "sujeito-1", "aff-1", "req-3", "ESTADUAL", "TJCE", "UNI-1",
                "RECONHECIDO", "ATESTADO", true, false, true, agora, agora,
                List.of(), List.of(item), List.of("fundamento-attestation"), "hash-attestation"
        );
        when(officialSourceAttestationApplicationService.consultarSolicitacao("req-3")).thenReturn(attestation);

        var response = service.atestacaoFontesOficiaisAdesaoDelegada("req-3");

        assertThat(response.subjectId()).isEqualTo("sujeito-1");
        assertThat(response.requestId()).isEqualTo("req-3");
        assertThat(response.integrityHash()).isEqualTo("hash-attestation");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).sourceCode()).isEqualTo("CNJ");
        assertThat(response.sources().get(0).confidenceScore()).isEqualTo(95);
        assertThat(response.sources().get(0).fundamentos()).containsExactly("fundamento-item");
    }

    @Test
    void revalidarFontesOficiaisAdesaoDelegadaUsaListaVaziaQuandoRequestNulo() {
        Instant agora = Instant.parse("2026-08-31T12:00:00Z");
        var attestation = new InstitutionalOfficialSourceAttestation(
                "ORGAO", "sujeito-2", "aff-2", "req-4", "ESTADUAL", "TJCE", "UNI-1",
                "RECONHECIDO", "ATESTADO", true, false, true, agora, agora,
                List.of(), List.of(), List.of(), "hash-2"
        );
        when(officialSourceAttestationApplicationService.revalidarSolicitacao("req-4", List.of())).thenReturn(attestation);

        assertThat(service.revalidarFontesOficiaisAdesaoDelegada("req-4", null).requestId()).isEqualTo("req-4");
    }

    @Test
    void matrizConfiabilidadeDelegaEMapeiaLista() {
        var domain = mock(InstitutionalTrustMatrixEntry.class);
        var response = mock(NationalCommunicationInstitutionalTrustMatrixEntryResponse.class);
        when(trustMatrixApplicationService.listar("BR")).thenReturn(List.of(domain));
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.matrizConfiabilidade("BR")).containsExactly(response);
    }
}
