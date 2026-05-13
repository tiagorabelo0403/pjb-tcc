package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOfficialSourceDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRecertificationCycle;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalRecertificationApplicationServiceTest {

    private final InstitutionalAffiliationStateRepository affiliationRepository = mock(InstitutionalAffiliationStateRepository.class);
    private final InstitutionalAffiliationRequestStateRepository requestRepository = mock(InstitutionalAffiliationRequestStateRepository.class);
    private final InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final InstitutionalOfficialSourceDossierApplicationService dossierApplicationService = mock(InstitutionalOfficialSourceDossierApplicationService.class);
    private final InstitutionalRecertificationApplicationService service = new InstitutionalRecertificationApplicationService(
            affiliationRepository,
            requestRepository,
            nominationRepository,
            currentUserService,
            dossierApplicationService
    );

    @Test
    void shouldEscalatePendingIssuesWhenSovereignDossierIsNotReady() {
        InstitutionalAffiliation affiliation = new InstitutionalAffiliation(
                "aff-1",
                DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "FORUM-LN",
                "Fórum de Limoeiro do Norte",
                InstitutionalOrganizationScope.FORUM,
                null,
                "CE",
                "Limoeiro do Norte",
                "27.000.000/0001-00",
                "ESTADUAL",
                List.of("CIVEL"),
                List.of("CE:Limoeiro do Norte"),
                "tjce.jus.br",
                "Presidência",
                10L,
                InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL,
                "seguranca@tjce.jus.br",
                List.of("PORTAL"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                false,
                true,
                false,
                true,
                InstitutionalAffiliationStatus.HOMOLOGADA,
                List.of(),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-10T00:00:00Z"),
                null
        );
        when(affiliationRepository.findAll()).thenReturn(List.of(affiliation));
        when(requestRepository.findAll()).thenReturn(List.of());
        when(nominationRepository.findAll()).thenReturn(List.of());
        when(dossierApplicationService.gerarAfiliacao(affiliation)).thenReturn(new InstitutionalOfficialSourceDossier(
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
                Instant.parse("2026-01-15T00:00:00Z"),
                List.of("instituicao_pai_nao_reconhecida"),
                List.of(),
                List.of("status_reconhecimento_publico=PENDENTE_EVIDENCIAS"),
                Instant.parse("2026-04-05T12:00:00Z")
        ));

        InstitutionalRecertificationCycle cycle = service.listar(null).getFirst();

        assertTrue(cycle.pendingIssues().contains("reconhecimento_soberano_insuficiente"));
        assertTrue(cycle.pendingIssues().contains("recertificacao_soberana_pendente"));
        assertTrue(cycle.pendingIssues().contains("instituicao_pai_nao_reconhecida"));
    }
}
