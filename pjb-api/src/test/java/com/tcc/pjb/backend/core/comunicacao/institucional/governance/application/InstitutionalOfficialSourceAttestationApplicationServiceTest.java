package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOfficialSourceDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceEvidence;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceAttestationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorProperties;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorRegistry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorRuntimeStateRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstitutionalOfficialSourceAttestationApplicationServiceTest {

    private final InstitutionalOfficialSourceDossierApplicationService dossierApplicationService = mock(InstitutionalOfficialSourceDossierApplicationService.class);
    private final InstitutionalOfficialSourceAttestationStateRepository stateRepository = mock(InstitutionalOfficialSourceAttestationStateRepository.class);
    private final InstitutionalAffiliationStateRepository affiliationRepository = mock(InstitutionalAffiliationStateRepository.class);
    private final InstitutionalAffiliationRequestStateRepository requestRepository = mock(InstitutionalAffiliationRequestStateRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-05T18:00:00Z"), ZoneOffset.UTC);
    private final InstitutionalOfficialSourceCatalogService catalogService = new InstitutionalOfficialSourceCatalogService();
    private final InstitutionalOfficialSourceConnectorProperties connectorProperties = connectorProperties();
    private final InstitutionalOfficialSourceConnectorRegistry connectorRegistry = new InstitutionalOfficialSourceConnectorRegistry(catalogService, connectorProperties, new InstitutionalOfficialSourceConnectorRuntimeStateRepository(), clock);
    private final InstitutionalOfficialSourceAttestationApplicationService service = new InstitutionalOfficialSourceAttestationApplicationService(
            dossierApplicationService,
            stateRepository,
            affiliationRepository,
            requestRepository,
            catalogService,
            connectorRegistry,
            clock
    );

    @Test
    void shouldCreateAutomaticAttestationWhenOnlyAutomaticSourcesNeedRefresh() {
        when(stateRepository.findByAffiliationId("aff-1")).thenReturn(Optional.empty());
        when(dossierApplicationService.gerarAfiliacao("aff-1")).thenReturn(new InstitutionalOfficialSourceDossier(
                "AFILIACAO",
                "aff-1",
                "aff-1",
                null,
                "FORUM",
                "TJCE",
                "FORUM-LN",
                "PENDENTE_EVIDENCIAS",
                false,
                true,
                Instant.parse("2026-04-05T19:00:00Z"),
                List.of("catalogo_cnj_datajud_ausente_ou_nao_confirmado"),
                List.of(
                        new InstitutionalOfficialSourceEvidence("CNJ_DATAJUD", "Base CNJ/DataJud", "CNJ_DATAJUD_OU_SIORG", true, false, true, true,
                                Instant.parse("2026-03-01T00:00:00Z"), Instant.parse("2026-04-05T19:00:00Z"), List.of("TJCE", "FORUM-LN"), List.of("catalogo_cnj_datajud_ausente_ou_nao_confirmado"), List.of("escopo_judiciario")),
                        new InstitutionalOfficialSourceEvidence("RECEITA_CNPJ", "Receita/CNPJ", "RECEITA_CNPJ", true, true, true, false,
                                Instant.parse("2026-04-01T00:00:00Z"), Instant.parse("2026-05-01T00:00:00Z"), List.of("27.000.000/0001-00"), List.of(), List.of("identidade_juridica"))
                ),
                List.of("status_reconhecimento_publico=PENDENTE_EVIDENCIAS"),
                Instant.parse("2026-04-05T18:00:00Z")
        ));
        when(stateRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        InstitutionalOfficialSourceAttestation response = service.consultarAfiliacao("aff-1");

        assertEquals("REVALIDACAO_AUTOMATICA_PENDENTE", response.attestationStatus());
        assertTrue(response.automaticRefreshEligible());
        assertTrue(response.sources().stream().anyMatch(item -> item.sourceCode().equals("CNJ_DATAJUD") && item.autoRefreshSupported() && item.refreshRecommended()));
    }

    @Test
    void shouldRequireHumanReviewWhenManualLegalActSourceIsPending() {
        when(stateRepository.findByRequestId("req-1")).thenReturn(Optional.empty());
        when(dossierApplicationService.gerarSolicitacao("req-1")).thenReturn(new InstitutionalOfficialSourceDossier(
                "SOLICITACAO",
                "req-1",
                "aff-parent",
                "req-1",
                "VARA",
                "TJCE",
                "VARA-01",
                "PENDENTE_EVIDENCIAS",
                false,
                true,
                Instant.parse("2026-04-05T19:00:00Z"),
                List.of("ato_formal_ou_delegacao_nao_confirmado"),
                List.of(
                        new InstitutionalOfficialSourceEvidence("ATO_PUBLICADO", "Ato formal", "ATO_PUBLICADO", true, false, true, false,
                                Instant.parse("2026-04-01T00:00:00Z"), Instant.parse("2026-04-06T00:00:00Z"), List.of("Diretoria do Fórum"), List.of("ato_formal_ou_delegacao_nao_confirmado"), List.of("base_legal_para_ativacao_ou_subunidade")),
                        new InstitutionalOfficialSourceEvidence("GOVBR", "Gov.br", "GOVBR", true, true, true, false,
                                Instant.parse("2026-04-05T10:00:00Z"), Instant.parse("2026-05-05T10:00:00Z"), List.of("representante=10"), List.of(), List.of("identidade_raiz"))
                ),
                List.of("status_reconhecimento_publico=PENDENTE_EVIDENCIAS"),
                Instant.parse("2026-04-05T18:00:00Z")
        ));
        when(stateRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        InstitutionalOfficialSourceAttestation response = service.consultarSolicitacao("req-1");

        assertFalse(response.automaticRefreshEligible());
        assertEquals("PENDENTE_HOMOLOGACAO_SOBERANA", response.attestationStatus());
        assertTrue(response.sources().stream().anyMatch(item -> item.sourceCode().equals("ATO_PUBLICADO") && !item.autoRefreshSupported() && item.refreshRecommended()));
    }

    @Test
    void shouldRevalidateActiveSubjectsWithoutScanningHistoricalUniverse() {
        when(stateRepository.findDueAffiliationIds(any(), anyInt())).thenReturn(List.of());
        when(stateRepository.findDueRequestIds(any(), anyInt())).thenReturn(List.of());
        when(affiliationRepository.findActive()).thenReturn(List.of(
                new com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation(
                        "aff-ativa",
                        com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                        "MPCE",
                        "Ministério Público",
                        "UNIDADE-01",
                        "Unidade 01",
                        com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope.PROMOTORIA,
                        null,
                        "CE",
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        null,
                        null,
                        10L,
                        com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole.TITULAR_INSTITUCIONAL,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                        true,
                        true,
                        true,
                        false,
                        com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus.HOMOLOGADA,
                        List.of(),
                        Instant.parse("2026-04-01T00:00:00Z"),
                        Instant.parse("2026-04-05T10:00:00Z"),
                        "hash-aff")));
        when(requestRepository.findGovernanceActive()).thenReturn(List.of(
                new com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest(
                        "req-ativa",
                        com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                        com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope.PROMOTORIA,
                        "MPCE",
                        "Ministério Público",
                        "UNIDADE-01",
                        "Unidade 01",
                        "CE",
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        null,
                        null,
                        10L,
                        "Representante",
                        com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole.TITULAR_INSTITUCIONAL,
                        java.util.Map.of(10L, "Representante"),
                        com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                        true,
                        true,
                        true,
                        false,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationRequestStatus.PENDENTE_VALIDACAO,
                        null,
                        List.of(),
                        Instant.parse("2026-04-01T00:00:00Z"),
                        null,
                        Instant.parse("2026-04-05T10:00:00Z"),
                        "hash-req")));
        when(stateRepository.findByAffiliationId("aff-ativa")).thenReturn(Optional.empty());
        when(stateRepository.findByRequestId("req-ativa")).thenReturn(Optional.empty());
        when(dossierApplicationService.gerarAfiliacao("aff-ativa")).thenReturn(new InstitutionalOfficialSourceDossier(
                "AFILIACAO",
                "aff-ativa",
                "aff-ativa",
                null,
                "VARA",
                "MPCE",
                "UNIDADE-01",
                "RECONHECIDA",
                true,
                false,
                Instant.parse("2026-05-01T00:00:00Z"),
                List.of(),
                List.of(),
                List.of(),
                Instant.parse("2026-04-05T18:00:00Z")
        ));
        when(dossierApplicationService.gerarSolicitacao("req-ativa")).thenReturn(new InstitutionalOfficialSourceDossier(
                "SOLICITACAO",
                "req-ativa",
                null,
                "req-ativa",
                "VARA",
                "MPCE",
                "UNIDADE-01",
                "PENDENTE_EVIDENCIAS",
                false,
                true,
                Instant.parse("2026-04-06T00:00:00Z"),
                List.of(),
                List.of(),
                List.of(),
                Instant.parse("2026-04-05T18:00:00Z")
        ));
        when(stateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = service.revalidarPendencias(10);

        assertEquals(2, processed);
        verify(affiliationRepository).findActive();
        verify(requestRepository).findGovernanceActive();
        verify(affiliationRepository, never()).findAll();
        verify(requestRepository, never()).findAll();
    }

    private static InstitutionalOfficialSourceConnectorProperties connectorProperties() {
        InstitutionalOfficialSourceConnectorProperties properties = new InstitutionalOfficialSourceConnectorProperties();
        InstitutionalOfficialSourceConnectorProperties.SourceConfig cnj = new InstitutionalOfficialSourceConnectorProperties.SourceConfig();
        cnj.setBaseUrl("https://cnj.example.internal");
        cnj.setDryRun(false);
        properties.getSources().put("CNJ_DATAJUD", cnj);
        InstitutionalOfficialSourceConnectorProperties.SourceConfig govbr = new InstitutionalOfficialSourceConnectorProperties.SourceConfig();
        govbr.setBaseUrl("https://govbr.example.internal");
        govbr.setDryRun(false);
        properties.getSources().put("GOVBR", govbr);
        return properties;
    }

}
