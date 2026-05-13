package com.tcc.pjb.backend.core.comunicacao.institucional.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalHorizontalDataPlaneApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOperationalProfileProjectionApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalTrustGovernanceOrchestrationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustGovernanceProfile;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InstitutionalOperationalProfileProjectionApplicationServiceTest {

    @Test
    void mustMaterializeInstitutionalProfileInsidePjbUsingNominationTrustAndDataPlane() {
        InstitutionalAffiliationStateRepository affiliationRepository = Mockito.mock(InstitutionalAffiliationStateRepository.class);
        InstitutionalNominationStateRepository nominationRepository = Mockito.mock(InstitutionalNominationStateRepository.class);
        InstitutionalTrustGovernanceOrchestrationApplicationService trustService = Mockito.mock(InstitutionalTrustGovernanceOrchestrationApplicationService.class);
        InstitutionalHorizontalDataPlaneApplicationService dataPlaneService = Mockito.mock(InstitutionalHorizontalDataPlaneApplicationService.class);
        InstitutionalOperationalProfileProjectionApplicationService service = new InstitutionalOperationalProfileProjectionApplicationService(
                affiliationRepository,
                nominationRepository,
                trustService,
                dataPlaneService);
        Instant now = Instant.now();
        InstitutionalAffiliation affiliation = new InstitutionalAffiliation(
                "AFF-1",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                "MPCE",
                "Ministerio Publico do Ceara",
                "UNI-1",
                "Promotoria de Morada Nova",
                InstitutionalOrganizationScope.PROMOTORIA,
                "BP-1",
                "CE",
                "Morada Nova",
                null,
                "ESTADUAL",
                List.of("PENAL"),
                List.of("MORADA NOVA"),
                "mp.ce.gov.br",
                "Procurador-Geral",
                10L,
                InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL,
                "seguranca@mp.ce.gov.br",
                List.of("PJB"),
                List.of("CARGA"),
                List.of("SLA_2H"),
                List.of("FALLBACK_SEDE"),
                List.of("SSO_CNJ"),
                InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true,
                true,
                true,
                false,
                InstitutionalAffiliationStatus.HOMOLOGADA,
                List.of("fundamento_afiliacao"),
                now,
                now,
                null);
        InstitutionalNomination nomination = new InstitutionalNomination(
                "NOM-1",
                "AFF-1",
                99L,
                "Maria Servidora",
                TipoUsuario.SERVIDOR_FORUM,
                InstitutionalAccessLaneKind.SECRETARIA,
                InstitutionalNominationRole.SECRETARIA_FORUM,
                FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                InstitutionalProcessProfile.SECRETARIA_FORUM,
                "UNI-1",
                "CX-TRIAGEM",
                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO),
                InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                InstitutionalEntryLandingPanel.PAINEL_SECRETARIA_FORUM,
                InstitutionalNominationStatus.ATIVA,
                now.minusSeconds(60),
                now.plusSeconds(3600),
                true,
                false,
                true,
                false,
                null,
                now,
                now);
        InstitutionalTrustGovernanceProfile trustProfile = new InstitutionalTrustGovernanceProfile(
                "PROFILE-1",
                "AFF-1",
                "NOM-1",
                99L,
                "Maria Servidora",
                "SERVIDOR_FORUM",
                "PROMOTORIA",
                "MINISTERIO_PUBLICO",
                "UNI-1",
                "CX-TRIAGEM",
                "PAINEL_SECRETARIA_FORUM",
                "/app/institucional/secretaria/uni-1/cx-triagem",
                "amber",
                "SECRETARIA_FORUM",
                "NIVEL_2_MFA_FORTE",
                true,
                false,
                true,
                false,
                true,
                List.of("PJB", "DIRETOR_GERAL"),
                List.of("PJB", "DIRETOR_GERAL"),
                List.of(),
                true,
                true,
                false,
                "CE|MPCE|UNI-1|CX-TRIAGEM|B12",
                List.of(),
                List.of("fundamento_trust"),
                now);
        InstitutionalHorizontalDataPlanePlan dataPlanePlan = new InstitutionalHorizontalDataPlanePlan(
                "PROFILE-1",
                "AFF-1",
                "NOM-1",
                "PROMOTORIA",
                "MINISTERIO_PUBLICO",
                "Morada Nova",
                "CE",
                "MPCE",
                "UNI-1",
                "Promotoria de Morada Nova",
                "Morada Nova",
                "CX-TRIAGEM",
                "PAINEL_SECRETARIA_FORUM",
                "/app/institucional/secretaria/uni-1/cx-triagem",
                true,
                false,
                true,
                "LOCAL",
                "CE|MPCE|UNI-1|CX-TRIAGEM|B12",
                "CE|MPCE|UNI-1|CX-TRIAGEM",
                "RR-NE-01",
                12,
                32,
                "CE|MPCE|UNI-1|ARQUIVO",
                List.of("UF", "TRIBUNAL_OU_ORGAO", "UNIDADE", "CAIXA"),
                Map.of("X-PJB-Unidade", "UNI-1"),
                List.of("PJB", "DIRETOR_GERAL"),
                List.of("PJB", "DIRETOR_GERAL"),
                List.of(),
                List.of(),
                List.of("fundamento_plano"),
                now);
        when(nominationRepository.findByNominationId("NOM-1")).thenReturn(Optional.of(nomination));
        when(affiliationRepository.findByAffiliationId("AFF-1")).thenReturn(Optional.of(affiliation));
        when(trustService.avaliarAtual("AFF-1", "NOM-1")).thenReturn(trustProfile);
        when(dataPlaneService.avaliarAtual("AFF-1", "NOM-1")).thenReturn(dataPlanePlan);

        InstitutionalOperationalProfileProjection projection = service.materializar("AFF-1", "NOM-1");

        assertTrue(projection.visibleInPjb());
        assertEquals("ATIVO_NO_PJB", projection.profileState());
        assertEquals("PROFILE-1", projection.profileKey());
        assertEquals("PROMOTORIA", projection.organizationScope());
        assertEquals("MINISTERIO_PUBLICO", projection.destinatarioKind());
        assertEquals("PAINEL_SECRETARIA_FORUM", projection.panelCode());
        assertEquals("/app/institucional/secretaria/uni-1/cx-triagem", projection.landingPath());
        assertEquals("CE|MPCE|UNI-1|CX-TRIAGEM|B12", projection.horizontalDataPlaneKey());
        assertEquals(List.of("VISUALIZAR", "RECEBER_COMUNICACAO"), projection.capacidades());
        assertTrue(projection.fundamentos().contains("perfil_materializado_e_visivel_no_pjb"));
    }
}
