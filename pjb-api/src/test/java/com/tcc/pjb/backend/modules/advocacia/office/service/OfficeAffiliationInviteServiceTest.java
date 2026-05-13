package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyBeginResult;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyStatus;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.GovBrAssuranceExtractor;
import com.tcc.pjb.backend.core.security.GovBrAssurancePolicy;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationInviteRequest;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.EquipeRepository;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeAffiliationInvite;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspaceProfile;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeAffiliationInviteStatus;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeWorkspaceMode;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeAffiliationInviteRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspacePreferenceRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspaceProfileRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoRegraRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OfficeAffiliationInviteServiceTest {

    @Test
    void createInvite_deveMaterializarConviteComEscopo() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        EquipeRepository equipeRepository = mock(EquipeRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
        AdvOfficeWorkspaceProfileRepository profileRepository = mock(AdvOfficeWorkspaceProfileRepository.class);
        EquipeOfficePolicyRepository policyRepository = mock(EquipeOfficePolicyRepository.class);
        EquipeOfficeDelegacaoRegraRepository regraRepository = mock(EquipeOfficeDelegacaoRegraRepository.class);
        AdvOfficeWorkspacePreferenceRepository preferenceRepository = mock(AdvOfficeWorkspacePreferenceRepository.class);
        AdvOfficeAffiliationInviteRepository inviteRepository = mock(AdvOfficeAffiliationInviteRepository.class);
        OfficeWorkspaceModeService officeWorkspaceModeService = mock(OfficeWorkspaceModeService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        RequestIdempotencyService requestIdempotencyService = mock(RequestIdempotencyService.class);
        GovBrAssuranceExtractor assuranceExtractor = mock(GovBrAssuranceExtractor.class);
        GovBrAssurancePolicy assurancePolicy = mock(GovBrAssurancePolicy.class);

        Usuario owner = new Usuario();
        owner.setId(10L);
        owner.setNome("Tiago Silva");
        owner.setEmail("tiago@office.com");
        owner.setTipoUsuario(TipoUsuario.ADVOGADO);

        Equipe equipe = new Equipe();
        equipe.setId(44L);
        equipe.setNome("Escritorio Tiago Silva");

        AdvOfficeWorkspaceProfile profile = new AdvOfficeWorkspaceProfile();
        profile.setEquipe(equipe);
        profile.setOwnerUserId(10L);
        profile.setDisplayName("Escritorio Tiago Silva");

        when(currentUserService.getRequired()).thenReturn(owner);
        when(equipeRepository.findById(44L)).thenReturn(Optional.of(equipe));
        when(profileRepository.findByEquipe_Id(44L)).thenReturn(Optional.of(profile));
        when(usuarioRepository.findByEmail("maria@office.com")).thenReturn(Optional.empty());
        when(inviteRepository.findOpenIdentityConflicts(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        OfficeAffiliationInviteService service = new OfficeAffiliationInviteService(
                currentUserService,
                equipeRepository,
                usuarioRepository,
                membroEquipeRepository,
                profileRepository,
                policyRepository,
                regraRepository,
                preferenceRepository,
                inviteRepository,
                officeWorkspaceModeService,
                auditLedgerService,
                requestIdempotencyService,
                assuranceExtractor,
                assurancePolicy);

        var view = service.createInvite(new FrontendOfficeAffiliationInviteRequest(44L, "Maria Lima", "maria@office.com", null, "12345/CE", PapelEquipe.ADVOGADO_JUNIOR, "Associada", false, EnumSet.of(RamoDireito.CIVIL, RamoDireito.PROCESSUAL_CIVIL), 8, 40, false, true, "HYBRID", 30));

        assertThat(view.equipeId()).isEqualTo(44L);
        assertThat(view.papelEquipe()).isEqualTo("ADVOGADO_JUNIOR");
        assertThat(view.allowedRamos()).contains("CIVIL", "PROCESSUAL_CIVIL");
        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.reinforcedFlow()).isTrue();
        assertThat(view.workspacePriority()).isEqualTo(30);
    }

    @Test
    void acceptInvite_deveAfiliarAdvogadoEAtivarModoEscritorio() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        EquipeRepository equipeRepository = mock(EquipeRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
        AdvOfficeWorkspaceProfileRepository profileRepository = mock(AdvOfficeWorkspaceProfileRepository.class);
        EquipeOfficePolicyRepository policyRepository = mock(EquipeOfficePolicyRepository.class);
        EquipeOfficeDelegacaoRegraRepository regraRepository = mock(EquipeOfficeDelegacaoRegraRepository.class);
        AdvOfficeWorkspacePreferenceRepository preferenceRepository = mock(AdvOfficeWorkspacePreferenceRepository.class);
        AdvOfficeAffiliationInviteRepository inviteRepository = mock(AdvOfficeAffiliationInviteRepository.class);
        OfficeWorkspaceModeService officeWorkspaceModeService = mock(OfficeWorkspaceModeService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        RequestIdempotencyService requestIdempotencyService = mock(RequestIdempotencyService.class);
        GovBrAssuranceExtractor assuranceExtractor = mock(GovBrAssuranceExtractor.class);
        GovBrAssurancePolicy assurancePolicy = mock(GovBrAssurancePolicy.class);

        Usuario advogado = new Usuario();
        advogado.setId(99L);
        advogado.setNome("Maria Lima");
        advogado.setEmail("maria@office.com");
        advogado.setCpf("12345678901");
        advogado.setOab("12345/CE");
        advogado.setTipoUsuario(TipoUsuario.ADVOGADO);

        Equipe equipe = new Equipe();
        equipe.setId(44L);
        equipe.setNome("Escritorio Tiago Silva");

        AdvOfficeAffiliationInvite invite = new AdvOfficeAffiliationInvite();
        invite.setId(91L);
        invite.setEquipe(equipe);
        invite.setTargetUserId(99L);
        invite.setInvitedEmail("maria@office.com");
        invite.setInvitedCpf("12345678901");
        invite.setInvitedOab("12345/CE");
        invite.setPapelEquipe(PapelEquipe.ADVOGADO_JUNIOR);
        invite.setCargo("Associada");
        invite.setStatus(OfficeAffiliationInviteStatus.PENDING);
        invite.setCreatedAt(Instant.now());
        invite.setExpiresAt(Instant.now().plusSeconds(3600));
        invite.setAllowAllRamos(false);
        invite.setAllowedRamosOverride(EnumSet.of(RamoDireito.CIVIL));
        invite.setMinTrustForAuto(6);
        invite.setMaxAutoPorDia(40);
        invite.setBlockPersonalCases(false);
        invite.setAutoActivateOnAccept(true);
        invite.setModeOnAccept(OfficeWorkspaceMode.HYBRID);
        invite.setWorkspacePriority(10);
        invite.setRequiredAssuranceLevel("prata");
        invite.setRequiresFinalApproval(false);

        when(currentUserService.getRequired()).thenReturn(advogado);
        when(inviteRepository.fetchById(91L)).thenReturn(Optional.of(invite));
        when(membroEquipeRepository.findByUsuario_IdAndEquipe_Id(99L, 44L)).thenReturn(Optional.empty());
        when(regraRepository.findByEquipeAndUser(44L, 99L)).thenReturn(Optional.empty());
        when(preferenceRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());
        when(assuranceExtractor.extract(any())).thenReturn("prata");
        when(requestIdempotencyService.begin(any(), any(), any())).thenReturn(new RequestIdempotencyBeginResult(RequestIdempotencyStatus.IN_PROGRESS, true, null, null, null, null));
        when(officeWorkspaceModeService.update(any())).thenReturn(new PjbFrontendOfficeModeView("HYBRID", 44L, "Escritorio Tiago Silva", 10L, "Tiago Silva", true, true, true, false, false, false, List.of(), List.of("Documentos assinados em modo escritorio saem com o certificado do patrono Tiago Silva."), List.of("CIVIL"), false, 6, "ELEVADO", 8, true, 10L, "Tiago Silva"));
        when(officeWorkspaceModeService.current(any())).thenReturn(new PjbFrontendOfficeModeView("PERSONAL", null, null, null, null, false, true, false, false, false, false, List.of(), List.of("Processos proprios em primeiro plano."), List.of("CIVIL"), true, null, null, null, false, 99L, "Maria Lima"));

        OfficeAffiliationInviteService service = new OfficeAffiliationInviteService(
                currentUserService,
                equipeRepository,
                usuarioRepository,
                membroEquipeRepository,
                profileRepository,
                policyRepository,
                regraRepository,
                preferenceRepository,
                inviteRepository,
                officeWorkspaceModeService,
                auditLedgerService,
                requestIdempotencyService,
                assuranceExtractor,
                assurancePolicy);

        var result = service.acceptInvite(91L, new FrontendOfficeAffiliationDecisionRequest(true, true, "HYBRID", true, "abc"));

        assertThat(result.activated()).isTrue();
        assertThat(result.officeMode().mode()).isEqualTo("HYBRID");
        assertThat(result.officeMode().activeEquipeId()).isEqualTo(44L);
        assertThat(result.invite().status()).isEqualTo("ACCEPTED");
    }
}
