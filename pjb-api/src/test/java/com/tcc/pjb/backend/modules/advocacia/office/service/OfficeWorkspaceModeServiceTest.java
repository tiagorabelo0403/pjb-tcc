package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeModeUpdateRequest;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspacePreference;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficeDelegacaoRegra;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspacePreferenceRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoRegraRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class OfficeWorkspaceModeServiceTest {

    @Test
    void current_devePriorizarEscritorioNoLoginComAutoAtivacao() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
        EquipeOfficePolicyRepository policyRepository = mock(EquipeOfficePolicyRepository.class);
        EquipeOfficeDelegacaoRegraRepository regraRepository = mock(EquipeOfficeDelegacaoRegraRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AdvOfficeWorkspacePreferenceRepository preferenceRepository = mock(AdvOfficeWorkspacePreferenceRepository.class);
        OfficePersonalScopeService personalScopeService = mock(OfficePersonalScopeService.class);
        OfficeTrustScoreService trustScoreService = mock(OfficeTrustScoreService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Tiago");
        usuario.setAtivo(true);
        usuario.setOab("12345/CE");

        Equipe equipe = new Equipe();
        equipe.setId(44L);
        equipe.setNome("Rocha & Silva");
        equipe.setAtivo(true);

        Usuario senior = new Usuario();
        senior.setId(77L);
        senior.setNome("Dr. Senior");

        MembroEquipe membro = new MembroEquipe();
        membro.setUsuario(usuario);
        membro.setEquipe(equipe);
        membro.setPapel(PapelEquipe.ADVOGADO_JUNIOR);
        membro.setCargo("Associado");
        membro.setAtivo(true);

        EquipeOfficePolicy policy = new EquipeOfficePolicy();
        policy.setEnabled(true);
        policy.setSignerUserId(77L);
        policy.setBloqueiaCausasProprias(false);
        policy.setForcePatronoCertificate(true);
        policy.setMinTrustAuto(7);
        policy.setAllowedRamos(EnumSet.of(RamoDireito.CIVIL, RamoDireito.PENAL));

        EquipeOfficeDelegacaoRegra regra = new EquipeOfficeDelegacaoRegra();
        regra.setAtivo(true);
        regra.setAllowedRamosOverride(EnumSet.of(RamoDireito.CIVIL));
        regra.setMinTrustAutoOverride(8);

        AdvOfficeWorkspacePreference preference = new AdvOfficeWorkspacePreference();
        preference.setUsuarioId(10L);
        preference.setPreferredEquipeId(44L);
        preference.setMode("HYBRID");
        preference.setAutoActivateOnLogin(true);
        preference.setAllowPersonalOwnCases(true);

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(currentUserService.getOrNull()).thenReturn(usuario);
        when(membroEquipeRepository.carregarComEquipe(10L)).thenReturn(List.of(membro));
        when(policyRepository.findByEquipeId(44L)).thenReturn(Optional.of(policy));
        when(regraRepository.findByEquipeAndUser(44L, 10L)).thenReturn(Optional.of(regra));
        when(usuarioRepository.findById(77L)).thenReturn(Optional.of(senior));
        when(preferenceRepository.findByUsuarioId(10L)).thenReturn(Optional.of(preference));
        when(personalScopeService.decide(10L)).thenReturn(new OfficePersonalScopeService.ScopeDecision(false, null, false, List.of(44L)));
        when(preferenceRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(trustScoreService.avaliar(10L, 44L)).thenReturn(new OfficeTrustScoreService.TrustScore(6, false, true, true, true, false));

        OfficeWorkspaceModeService service = new OfficeWorkspaceModeService(
                currentUserService,
                membroEquipeRepository,
                policyRepository,
                regraRepository,
                usuarioRepository,
                preferenceRepository,
                personalScopeService,
                trustScoreService,
                auditLedgerService);

        var current = service.current(new MockHttpServletRequest());
        var updated = service.update(new FrontendOfficeModeUpdateRequest(44L, "HYBRID", true, true));

        assertThat(current.mode()).isEqualTo("HYBRID");
        assertThat(current.activeEquipeNome()).isEqualTo("Rocha & Silva");
        assertThat(current.activeSeniorNome()).isEqualTo("Dr. Senior");
        assertThat(current.canOpenPersonalOwnCases()).isTrue();
        assertThat(current.effectiveAllowedRamos()).containsExactly("CIVIL");
        assertThat(current.currentTrustLevel()).isEqualTo("ELEVADO");
        assertThat(current.requiredMinTrustForAuto()).isEqualTo(8);
        assertThat(current.patronCertificateRequired()).isTrue();
        assertThat(current.effectiveSignerUserId()).isEqualTo(77L);
        assertThat(updated.mode()).isEqualTo("HYBRID");
    }
}
