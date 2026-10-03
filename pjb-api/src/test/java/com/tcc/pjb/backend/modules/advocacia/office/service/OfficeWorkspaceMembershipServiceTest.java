package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeMembershipView;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficeDelegacaoRegra;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoRegraRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OfficeWorkspaceMembershipServiceTest {

    private final MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
    private final EquipeOfficePolicyRepository policyRepository = mock(EquipeOfficePolicyRepository.class);
    private final EquipeOfficeDelegacaoRegraRepository regraRepository = mock(EquipeOfficeDelegacaoRegraRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final OfficeTrustScoreService trustScoreService = mock(OfficeTrustScoreService.class);
    private final OfficeWorkspaceMembershipService service = new OfficeWorkspaceMembershipService(
            membroEquipeRepository, policyRepository, regraRepository, usuarioRepository, trustScoreService);

    @Test
    void membershipsVazioQuandoNaoHaVinculos() {
        when(membroEquipeRepository.carregarComEquipe(10L)).thenReturn(List.of());
        assertThat(service.memberships(10L)).isEmpty();
    }

    @Test
    void membershipsIgnoraVinculoInativo() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        Equipe equipe = new Equipe();
        equipe.setId(44L);
        equipe.setAtivo(true);
        MembroEquipe membro = new MembroEquipe();
        membro.setUsuario(usuario);
        membro.setEquipe(equipe);
        membro.setAtivo(false);
        when(membroEquipeRepository.carregarComEquipe(10L)).thenReturn(List.of(membro));

        assertThat(service.memberships(10L)).isEmpty();
    }

    @Test
    void membershipsMaterializaViewComPolicyRegraETrust() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Tiago");
        usuario.setAtivo(true);

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

        when(membroEquipeRepository.carregarComEquipe(10L)).thenReturn(List.of(membro));
        when(policyRepository.findByEquipeId(44L)).thenReturn(Optional.of(policy));
        when(regraRepository.findByEquipeAndUser(44L, 10L)).thenReturn(Optional.of(regra));
        when(usuarioRepository.findById(77L)).thenReturn(Optional.of(senior));
        when(trustScoreService.avaliar(10L, 44L)).thenReturn(new OfficeTrustScoreService.TrustScore(6, false, true, true, true, false));

        List<PjbFrontendOfficeMembershipView> memberships = service.memberships(10L);

        assertThat(memberships).hasSize(1);
        PjbFrontendOfficeMembershipView view = memberships.get(0);
        assertThat(view.equipeId()).isEqualTo(44L);
        assertThat(view.equipeNome()).isEqualTo("Rocha & Silva");
        assertThat(view.seniorUserId()).isEqualTo(77L);
        assertThat(view.seniorNome()).isEqualTo("Dr. Senior");
        assertThat(view.officePolicyEnabled()).isTrue();
        assertThat(view.allowedRamos()).containsExactly("CIVIL");
        assertThat(view.canViewAllRamos()).isFalse();
        assertThat(view.trustScore()).isEqualTo(6);
        assertThat(view.trustLevel()).isEqualTo("ELEVADO");
        assertThat(view.minTrustRequired()).isEqualTo(8);
        assertThat(view.patronCertificateRequired()).isTrue();
    }
}
