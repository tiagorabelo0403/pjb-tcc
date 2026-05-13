package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferPreviewView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeProcessTransferRequest;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.EquipeRepository;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficeDelegacaoRegra;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoRegraRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OfficeProcessTransferPreviewServiceTest {

    @Test
    void preview_deveBloquearQuandoDestinoNaoTemRamoOuTrustParaProcessoSensivel() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        EquipeRepository equipeRepository = mock(EquipeRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
        EquipeOfficePolicyRepository policyRepository = mock(EquipeOfficePolicyRepository.class);
        EquipeOfficeDelegacaoRegraRepository regraRepository = mock(EquipeOfficeDelegacaoRegraRepository.class);
        OfficeTrustScoreService trustScoreService = mock(OfficeTrustScoreService.class);

        OfficeProcessTransferPreviewService service = new OfficeProcessTransferPreviewService(
                currentUserService,
                equipeRepository,
                usuarioRepository,
                processoRepository,
                membroEquipeRepository,
                policyRepository,
                regraRepository,
                trustScoreService);

        Usuario actor = new Usuario();
        actor.setId(10L);
        actor.setNome("Tiago Silva");
        actor.setTipoUsuario(TipoUsuario.ADVOGADO);

        Equipe source = new Equipe();
        source.setId(44L);
        source.setNome("Escritorio Rocha & Silva");

        Equipe target = new Equipe();
        target.setId(45L);
        target.setNome("Escritorio Lima");

        Usuario targetResponsible = new Usuario();
        targetResponsible.setId(99L);
        targetResponsible.setNome("Maria Lima");
        targetResponsible.setCpf("12345678901");
        targetResponsible.setTipoUsuario(TipoUsuario.ADVOGADO);

        MembroEquipe membership = new MembroEquipe();
        membership.setUsuario(targetResponsible);
        membership.setEquipe(target);
        membership.setAtivo(true);
        membership.setPapel(PapelEquipe.ADVOGADO_JUNIOR);

        EquipeOfficePolicy policy = new EquipeOfficePolicy();
        policy.setEquipe(target);
        policy.setEnabled(true);
        policy.setSignerUserId(77L);
        policy.setForcePatronoCertificate(true);
        policy.setMinTrustAuto(8);
        policy.setAllowedRamos(Set.of(RamoDireito.CIVIL));

        EquipeOfficeDelegacaoRegra regra = new EquipeOfficeDelegacaoRegra();
        regra.setEquipe(target);
        regra.setUsuario(targetResponsible);
        regra.setAtivo(true);
        regra.setAllowedRamosOverride(Set.of(RamoDireito.CIVIL));
        regra.setMinTrustAutoOverride(8);

        Processo processo = new Processo();
        processo.setId(1001L);
        processo.setEquipe(source);
        processo.setNumeroUnificado("0000001-10.2026.8.06.0001");
        processo.setRamoDireito(RamoDireito.PENAL);
        processo.setNivelSigilo(NivelSigilo.SEGREDO_JUSTICA);

        when(currentUserService.getRequired()).thenReturn(actor);
        when(equipeRepository.findById(44L)).thenReturn(Optional.of(source));
        when(equipeRepository.findById(45L)).thenReturn(Optional.of(target));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(targetResponsible));
        when(processoRepository.findAllById(List.of(1001L))).thenReturn(List.of(processo));
        when(membroEquipeRepository.findByUsuario_IdAndEquipe_Id(99L, 45L)).thenReturn(Optional.of(membership));
        when(policyRepository.findByEquipeId(45L)).thenReturn(Optional.of(policy));
        when(regraRepository.findByEquipeAndUser(45L, 99L)).thenReturn(Optional.of(regra));
        when(trustScoreService.avaliar(99L, 45L)).thenReturn(new OfficeTrustScoreService.TrustScore(5, false, true, true, true, false));

        PjbFrontendOfficeProcessTransferPreviewView preview = service.preview(new FrontendOfficeProcessTransferRequest(
                44L,
                45L,
                99L,
                List.of(1001L),
                "Redistribuicao penal",
                "Carteira sensivel",
                "tx-prev",
                null));

        assertThat(preview.valid()).isFalse();
        assertThat(preview.blockers()).contains("RAMO_NAO_AUTORIZADO", "TRUST_INSUFICIENTE_PARA_SIGILO");
        assertThat(preview.items()).hasSize(1);
        assertThat(preview.items().get(0).warnings()).contains("ASSINATURA_PATRONAL_OBRIGATORIA", "PROCESSO_SIGILOSO");
    }
}
