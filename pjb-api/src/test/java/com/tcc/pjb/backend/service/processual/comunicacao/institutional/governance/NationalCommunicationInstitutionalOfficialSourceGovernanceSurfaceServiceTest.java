package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOfficialSourceDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalPublicRecognitionGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialIdentifierDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceAttestationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceConnectorCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceConnectorProbeApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestation;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalOfficialSourceGovernanceSurfaceServiceTest {

    private final InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService = mock(InstitutionalPublicRecognitionGateApplicationService.class);
    private final InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService = mock(InstitutionalOfficialSourceDossierApplicationService.class);
    private final InstitutionalOfficialIdentifierDossierApplicationService officialIdentifierDossierApplicationService = mock(InstitutionalOfficialIdentifierDossierApplicationService.class);
    private final InstitutionalOfficialSourceAttestationApplicationService officialSourceAttestationApplicationService = mock(InstitutionalOfficialSourceAttestationApplicationService.class);
    private final InstitutionalOfficialSourceConnectorCatalogApplicationService officialSourceConnectorCatalogApplicationService = mock(InstitutionalOfficialSourceConnectorCatalogApplicationService.class);
    private final InstitutionalOfficialSourceConnectorProbeApplicationService officialSourceConnectorProbeApplicationService = mock(InstitutionalOfficialSourceConnectorProbeApplicationService.class);
    private final NationalCommunicationInstitutionalOfficialSourceGovernanceSurfaceService service = new NationalCommunicationInstitutionalOfficialSourceGovernanceSurfaceService(
            publicRecognitionGateApplicationService, officialSourceDossierApplicationService, officialIdentifierDossierApplicationService,
            officialSourceAttestationApplicationService, officialSourceConnectorCatalogApplicationService, officialSourceConnectorProbeApplicationService);

    @Test
    void reconhecimentoPublicoDelegaSemMapeamentoExtra() {
        var response = mock(AdminInstitutionalPublicRecognitionResponse.class);
        when(publicRecognitionGateApplicationService.avaliarAfiliacao("aff-1")).thenReturn(response);

        assertThat(service.reconhecimentoPublicoAfiliacao("aff-1")).isSameAs(response);
    }

    @Test
    void catalogoConectoresDelegaSemMapeamentoExtra() {
        var response = mock(NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse.class);
        when(officialSourceConnectorCatalogApplicationService.listar()).thenReturn(response);

        assertThat(service.catalogoConectoresFontesOficiais()).isSameAs(response);
    }

    @Test
    void sondarConectoresDelegaSemMapeamentoExtra() {
        var response = mock(NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse.class);
        when(officialSourceConnectorProbeApplicationService.sondarTodos()).thenReturn(response);

        assertThat(service.sondarConectoresFontesOficiais()).isSameAs(response);
    }

    @Test
    void sondarConectorEspecificoDelegaComSourceCode() {
        var response = mock(NationalCommunicationInstitutionalOfficialSourceConnectorResponse.class);
        when(officialSourceConnectorProbeApplicationService.sondar("CNJ")).thenReturn(response);

        assertThat(service.sondarConectorFonteOficial("CNJ")).isSameAs(response);
    }

    @Test
    void atestacaoFontesOficiaisMapeiaTodosOsCamposSemPerda() {
        Instant agora = Instant.parse("2026-09-01T00:00:00Z");
        var attestation = new InstitutionalOfficialSourceAttestation(
                "ORGAO", "sujeito-1", "aff-2", "req-1", "ESTADUAL", "TJCE", "UNI-1",
                "RECONHECIDO", "ATESTADO", true, false, true, agora, agora,
                List.of(), List.of(), List.of("fundamento-1"), "hash-attestation");
        when(officialSourceAttestationApplicationService.consultarAfiliacao("aff-2")).thenReturn(attestation);

        var response = service.atestacaoFontesOficiaisAfiliacao("aff-2");

        assertThat(response.subjectId()).isEqualTo("sujeito-1");
        assertThat(response.affiliationId()).isEqualTo("aff-2");
        assertThat(response.integrityHash()).isEqualTo("hash-attestation");
        assertThat(response.fundamentos()).containsExactly("fundamento-1");
    }

    @Test
    void revalidarFontesOficiaisUsaListaVaziaQuandoRequestNulo() {
        Instant agora = Instant.parse("2026-09-01T00:00:00Z");
        var attestation = new InstitutionalOfficialSourceAttestation(
                "ORGAO", "sujeito-2", "aff-3", "req-2", "ESTADUAL", "TJCE", "UNI-1",
                "RECONHECIDO", "ATESTADO", true, false, true, agora, agora,
                List.of(), List.of(), List.of(), "hash-2");
        when(officialSourceAttestationApplicationService.revalidarAfiliacao("aff-3", List.of())).thenReturn(attestation);

        assertThat(service.revalidarFontesOficiaisAfiliacao("aff-3", null).affiliationId()).isEqualTo("aff-3");
    }
}
