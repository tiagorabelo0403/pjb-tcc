package com.tcc.pjb.backend.core.comunicacao.institucional.closure;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalTrustMatrixApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustMatrixEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalDelegatedGovernanceClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationApprovalTrailApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationValidationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalIntegrationSecurityPolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalRecertificationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationApprovalTrail;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationValidationReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationSecurityPolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRecertificationCycle;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRiskSeverity;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalOperationalLifecycleApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalStructuralDiagnosticApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalLifecycle;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalStructuralDiagnosticFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalStructuralDiagnosticReport;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOperationalLifecycleStage;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstitutionalDelegatedGovernanceClosureApplicationServiceTest {

    @Test
    void deveConsolidarForumComAdesaoDelegadaESegurancaFechada() {
        InstitutionalOperationalLifecycleApplicationService lifecycleService = mock(InstitutionalOperationalLifecycleApplicationService.class);
        InstitutionalStructuralDiagnosticApplicationService structuralService = mock(InstitutionalStructuralDiagnosticApplicationService.class);
        InstitutionalRecertificationApplicationService recertificationService = mock(InstitutionalRecertificationApplicationService.class);
        InstitutionalAffiliationValidationApplicationService validationService = mock(InstitutionalAffiliationValidationApplicationService.class);
        InstitutionalAffiliationApprovalTrailApplicationService approvalTrailService = mock(InstitutionalAffiliationApprovalTrailApplicationService.class);
        InstitutionalIntegrationSecurityPolicyApplicationService integrationSecurityPolicyService = mock(InstitutionalIntegrationSecurityPolicyApplicationService.class);
        InstitutionalTrustMatrixApplicationService trustMatrixService = mock(InstitutionalTrustMatrixApplicationService.class);
        InstitutionalEntryContextApplicationService entryContextService = mock(InstitutionalEntryContextApplicationService.class);

        InstitutionalDelegatedGovernanceClosureApplicationService service = new InstitutionalDelegatedGovernanceClosureApplicationService(
                lifecycleService,
                structuralService,
                recertificationService,
                validationService,
                approvalTrailService,
                integrationSecurityPolicyService,
                trustMatrixService,
                entryContextService
        );

        Instant now = Instant.now();
        InstitutionalOperationalLifecycle lifecycle = new InstitutionalOperationalLifecycle(
                "AFF-FORUM-1",
                "REQ-FORUM-1",
                DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                InstitutionalOrganizationScope.FORUM,
                "TJCE",
                "Fórum Clóvis Beviláqua",
                "FORUM-FOR-001",
                "Diretoria do Fórum",
                "CE",
                "Fortaleza",
                null,
                "ESTADO",
                List.of("CIVEL"),
                List.of("FORTALEZA"),
                "tjce.jus.br",
                "Diretor do fórum",
                InstitutionalOperationalLifecycleStage.OPERACAO_ATIVA,
                true,
                true,
                true,
                3,
                3,
                2,
                2,
                2,
                List.of("FORUM-FOR-001::CAIXA-Triagem", "FORUM-FOR-001::CAIXA-Gabinete"),
                List.of("PJB_INBOX", "EMAIL"),
                List.of("CIENCIA_PESSOAL"),
                List.of("48H"),
                List.of("EMAIL_CONTINGENCIA"),
                List.of("API_TRIBUNAL"),
                List.of("LOGIN_GOVBR", "MFA_ATIVO"),
                List.of("ORGAO", "UNIDADE", "CAIXA", "CAPACIDADE"),
                List.of("homologacao_pjb_aprovada", "orgao_nomeia_pessoas_e_pjb_homologa"),
                now
        );

        InstitutionalAffiliationValidationReport validation = new InstitutionalAffiliationValidationReport(
                "VAL-1",
                "REQ-FORUM-1",
                "FORUM",
                "TJCE",
                "FORUM-FOR-001",
                true,
                true,
                true,
                true,
                true,
                true,
                List.of(),
                List.of("validacao_ok"),
                now,
                null
        );

        InstitutionalAffiliationApprovalTrail trail = new InstitutionalAffiliationApprovalTrail(
                "TRAIL-1",
                "REQ-FORUM-1",
                11L,
                "Diretor do Fórum",
                true,
                now.minusSeconds(120),
                99L,
                "Administrador PJB",
                true,
                now.minusSeconds(60),
                true,
                "HOMOLOGADA",
                List.of("segunda_chave_pjb_homologacao"),
                now,
                null
        );

        InstitutionalRecertificationCycle recertification = new InstitutionalRecertificationCycle(
                "AFF-FORUM-1",
                "FORUM",
                "TJCE",
                "Fórum Clóvis Beviláqua",
                "FORUM-FOR-001",
                "Diretoria do Fórum",
                "HOMOLOGADA",
                2,
                2,
                3,
                true,
                true,
                false,
                true,
                now.minusSeconds(3600),
                now.plusSeconds(86400),
                List.of(),
                List.of("recertificacao_ok"),
                now
        );

        InstitutionalStructuralDiagnosticReport structural = new InstitutionalStructuralDiagnosticReport(
                "AFF-FORUM-1",
                true,
                0,
                0,
                List.of(),
                List.of("scanner_estrutural_ok"),
                now
        );

        InstitutionalIntegrationSecurityPolicy policy = new InstitutionalIntegrationSecurityPolicy(
                "AFF-FORUM-1",
                "AFILIACAO_ATIVA",
                "FORUM",
                "Fórum Clóvis Beviláqua / Diretoria do Fórum",
                "NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO",
                List.of("PJB_INBOX"),
                List.of("API_TRIBUNAL"),
                true,
                true,
                true,
                true,
                true,
                30,
                List.of("IDEMPOTENCIA", "CORRELACAO_DE_REQUISICOES", "TRILHA_FORENSE_POR_CHAMADA"),
                List.of("policy_ok"),
                now
        );

        List<InstitutionalTrustMatrixEntry> trustMatrix = List.of(
                new InstitutionalTrustMatrixEntry(
                        "CIDADAO_DIRETO",
                        "PERFIL_DIRETO",
                        "Cidadão e parte",
                        InstitutionalEntryMode.DIRETO_PESSOA.name(),
                        null,
                        null,
                        InstitutionalProcessProfile.PERFIL_HIBRIDO.name(),
                        InstitutionalEntryLandingPanel.PAINEL_UNIDADE.name(),
                        InstitutionalTrustLevel.NIVEL_1_IDENTIDADE_FEDERADA.name(),
                        List.of("LOGIN_GOVBR"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of("perfil_direto_sem_adesao_institucional"),
                        List.of("/api/v1/pessoal"),
                        List.of("perfil_direto")
                ),
                new InstitutionalTrustMatrixEntry(
                        "FORUM__DIRETORIA",
                        "FORUM",
                        "Fórum e direção de unidade judiciária / Diretoria do fórum",
                        InstitutionalEntryMode.INSTITUCIONAL_AFILIADO.name(),
                        "DIRETORIA",
                        "DIRETORIA_FORUM",
                        InstitutionalProcessProfile.DIRETOR_FORUM.name(),
                        InstitutionalEntryLandingPanel.PAINEL_DIRETORIA_FORUM.name(),
                        InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO.name(),
                        List.of("LOGIN_GOVBR", "NOMEACAO_ATIVA", "AFILIACAO_HOMOLOGADA"),
                        List.of("AUTORIZACAO_REMOTA_CERTIFICADO"),
                        List.of("VISUALIZAR"),
                        List.of("Sem atuação fora do escopo homologado"),
                        List.of("orgao_nomeia_pessoas_e_pjb_homologa", "recertificacao_periodica_obrigatoria"),
                        List.of(InstitutionalApiRoutes.painelExecutivo()),
                        List.of("blueprint_forum")
                )
        );

        when(lifecycleService.listar()).thenReturn(List.of(lifecycle));
        when(validationService.buscarUltimo("REQ-FORUM-1")).thenReturn(Optional.of(validation));
        when(approvalTrailService.buscarUltima("REQ-FORUM-1")).thenReturn(Optional.of(trail));
        when(structuralService.diagnosticar("AFF-FORUM-1")).thenReturn(structural);
        when(recertificationService.listar(null)).thenReturn(List.of(recertification));
        when(integrationSecurityPolicyService.listar("FORUM", "AFF-FORUM-1")).thenReturn(List.of(policy));
        when(trustMatrixService.listar(null)).thenReturn(trustMatrix);

        var report = service.consolidar(null);

        assertEquals(1, report.itens().size());
        assertTrue(report.escoposDelegados().stream().anyMatch(item -> item.organizationScope().equals("FORUM") && item.forumOrJudicialUnit()));
        var item = report.itens().getFirst();
        assertTrue(item.afiliacaoHomologada());
        assertTrue(item.duplaChaveSatisfeita());
        assertTrue(item.quatroNiveisFechados());
        assertTrue(item.recertificacaoEmDia());
        assertTrue(item.diagnosticoEstruturalOk());
        assertTrue(item.integracaoEndurecida());
        assertTrue(item.orgaoNomeiaEPjbHomologa());
        assertFalse(item.missingPillars().contains("modelo_orgao_unidade_caixa_usuario_capacidade"));
    }

    @Test
    void deveExporEntradaAtualComFluxoDiretoEContextoDelegado() {
        InstitutionalOperationalLifecycleApplicationService lifecycleService = mock(InstitutionalOperationalLifecycleApplicationService.class);
        InstitutionalStructuralDiagnosticApplicationService structuralService = mock(InstitutionalStructuralDiagnosticApplicationService.class);
        InstitutionalRecertificationApplicationService recertificationService = mock(InstitutionalRecertificationApplicationService.class);
        InstitutionalAffiliationValidationApplicationService validationService = mock(InstitutionalAffiliationValidationApplicationService.class);
        InstitutionalAffiliationApprovalTrailApplicationService approvalTrailService = mock(InstitutionalAffiliationApprovalTrailApplicationService.class);
        InstitutionalIntegrationSecurityPolicyApplicationService integrationSecurityPolicyService = mock(InstitutionalIntegrationSecurityPolicyApplicationService.class);
        InstitutionalTrustMatrixApplicationService trustMatrixService = mock(InstitutionalTrustMatrixApplicationService.class);
        InstitutionalEntryContextApplicationService entryContextService = mock(InstitutionalEntryContextApplicationService.class);

        InstitutionalDelegatedGovernanceClosureApplicationService service = new InstitutionalDelegatedGovernanceClosureApplicationService(
                lifecycleService,
                structuralService,
                recertificationService,
                validationService,
                approvalTrailService,
                integrationSecurityPolicyService,
                trustMatrixService,
                entryContextService
        );

        when(trustMatrixService.listar(null)).thenReturn(List.of(
                new InstitutionalTrustMatrixEntry(
                        "CIDADAO_DIRETO",
                        "PERFIL_DIRETO",
                        "Cidadão e parte",
                        InstitutionalEntryMode.DIRETO_PESSOA.name(),
                        null,
                        null,
                        InstitutionalProcessProfile.PERFIL_HIBRIDO.name(),
                        InstitutionalEntryLandingPanel.PAINEL_UNIDADE.name(),
                        InstitutionalTrustLevel.NIVEL_1_IDENTIDADE_FEDERADA.name(),
                        List.of("LOGIN_GOVBR"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                )
        ));
        when(entryContextService.resolverEntradaAtual()).thenReturn(new InstitutionalEntrySummary(
                55L,
                "Ana Operadora",
                TipoUsuario.SERVIDOR,
                new InstitutionalIdentityBaseProfile(
                        "SERVIDOR_BASE",
                        TipoUsuario.SERVIDOR,
                        true,
                        InstitutionalEntryMode.DIRETO_PESSOA,
                        InstitutionalProcessProfile.PERFIL_HIBRIDO,
                        InstitutionalEntryLandingPanel.PAINEL_UNIDADE,
                        InstitutionalTrustLevel.NIVEL_1_IDENTIDADE_FEDERADA,
                        true,
                        List.of("identidade_pessoal_forte")
                ),
                true,
                true,
                List.of(),
                null,
                Instant.now()
        ));

        var current = service.entradaAtual();

        assertTrue(current.possuiAmbientePessoal());
        assertTrue(current.possuiAmbienteInstitucional());
        assertTrue(current.possuiPerfilDiretoAutorizado());
        assertEquals("SERVIDOR_BASE", current.identityCode());
        assertTrue(current.perfisDiretosPermitidos().contains("Cidadão e parte"));
    }
}
