package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalDocumentSecurityGate;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstitutionalDocumentSecurityGateApplicationServiceTest {

    private CurrentUserService currentUserService;
    private InstitutionalNominationStateRepository nominationRepository;
    private InstitutionalSensitiveActAuthorizationApplicationService authorizationService;
    private InstitutionalDocumentSecurityGateApplicationService service;

    private static final InstitutionalSensitiveAct ACT = InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO;

    @BeforeEach
    void setUp() {
        currentUserService = mock(CurrentUserService.class);
        nominationRepository = mock(InstitutionalNominationStateRepository.class);
        authorizationService = mock(InstitutionalSensitiveActAuthorizationApplicationService.class);
        service = new InstitutionalDocumentSecurityGateApplicationService(
                currentUserService, nominationRepository, authorizationService);
    }

    @Test
    void usuarioNaoResolvivel_comFallbackLegado_naoLancaEDegradaParaNaoBloqueante() {
        when(currentUserService.getOrNull()).thenReturn(null);

        InstitutionalDocumentSecurityGate gate = service.enforce(null, null, ACT, "RECURSAL_UNIFICADO", true);

        assertThat(gate.enforced()).isFalse();
        assertThat(gate.allowed()).isTrue();
        assertThat(gate.blocked()).isFalse();
    }

    @Test
    void usuarioNaoResolvivel_semFallbackLegado_bloqueiaComRegraNegocio() {
        when(currentUserService.getOrNull()).thenReturn(null);

        assertThatThrownBy(() -> service.enforce("UNI-1", null, ACT, "EXPEDIR_COMUNICACAO_INSTITUCIONAL", false))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void usuarioResolvidoSemNomeacao_comFallbackLegado_naoBloqueia() {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        when(currentUserService.getOrNull()).thenReturn(usuario);
        when(nominationRepository.findByNominatedUserId(42L)).thenReturn(List.of());

        InstitutionalDocumentSecurityGate gate = service.enforce(null, null, ACT, "MP_RECURSO", true);

        assertThat(gate.enforced()).isFalse();
        assertThat(gate.allowed()).isTrue();
    }

    @Test
    void usuarioResolvidoSemNomeacao_semFallbackLegado_bloqueia() {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        when(currentUserService.getOrNull()).thenReturn(usuario);
        when(nominationRepository.findActiveFor(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(nominationRepository.findByNominatedUserId(42L)).thenReturn(List.of());

        InstitutionalDocumentSecurityGate gate = service.avaliar("UNI-1", null, ACT, "EXPEDIR_COMUNICACAO_INSTITUCIONAL", false);

        assertThat(gate.enforced()).isTrue();
        assertThat(gate.allowed()).isFalse();
        assertThat(gate.blocked()).isTrue();
        assertThatThrownBy(() -> service.enforce("UNI-1", null, ACT, "EXPEDIR_COMUNICACAO_INSTITUCIONAL", false))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void generatedAt_semprePresenteNoGate() {
        when(currentUserService.getOrNull()).thenReturn(null);

        InstitutionalDocumentSecurityGate gate = service.avaliar(null, null, ACT, "RECURSAL_UNIFICADO", true);

        assertThat(gate.generatedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
    }
}
