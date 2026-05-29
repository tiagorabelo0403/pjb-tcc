package com.tcc.pjb.backend.modules.laiane.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.context.CurrentAuthenticationContext;
import com.tcc.pjb.backend.core.security.context.CurrentAuthenticationContextService;
import com.tcc.pjb.backend.integration.govbr.GovBrAssuranceLevel;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeOficio;
import com.tcc.pjb.backend.modules.laiane.model.LaianeOficioStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

class LaianeOficioAccessGuardTest {

    @Test
    void usuarioOrigemPodeLer() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        Usuario origem = usuario(1L);
        LaianeOficio oficio = oficio(origem, usuario(2L));
        when(currentUserService.get()).thenReturn(origem);

        LaianeOficioAccessGuard guard = new LaianeOficioAccessGuard(currentUserService, Mockito.mock(CurrentAuthenticationContextService.class));

        assertThatCode(() -> guard.requireRead(oficio)).doesNotThrowAnyException();
    }

    @Test
    void usuarioDestinoPodeLer() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        Usuario destino = usuario(2L);
        LaianeOficio oficio = oficio(usuario(1L), destino);
        when(currentUserService.get()).thenReturn(destino);

        LaianeOficioAccessGuard guard = new LaianeOficioAccessGuard(currentUserService, Mockito.mock(CurrentAuthenticationContextService.class));

        assertThatCode(() -> guard.requireRead(oficio)).doesNotThrowAnyException();
    }

    @Test
    void usuarioAlheioNaoPodeLer() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        LaianeOficio oficio = oficio(usuario(1L), usuario(2L));
        when(currentUserService.get()).thenReturn(usuario(3L));

        LaianeOficioAccessGuard guard = new LaianeOficioAccessGuard(currentUserService, Mockito.mock(CurrentAuthenticationContextService.class));

        assertThatThrownBy(() -> guard.requireRead(oficio))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void apenasDestinoPodeAtualizarStatus() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        Usuario origem = usuario(1L);
        LaianeOficio oficio = oficio(origem, usuario(2L));
        when(currentUserService.get()).thenReturn(origem);

        LaianeOficioAccessGuard guard = new LaianeOficioAccessGuard(currentUserService, Mockito.mock(CurrentAuthenticationContextService.class));

        assertThatThrownBy(() -> guard.requireUpdateStatus(oficio))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void mensagemNaoVazaExistencia() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        UUID trackingCode = UUID.fromString("01963c1a-7e3f-7000-8000-000000000001");
        LaianeOficio oficio = oficio(usuario(1L), usuario(2L));
        oficio.setTrackingCode(trackingCode);
        when(currentUserService.get()).thenReturn(usuario(3L));

        LaianeOficioAccessGuard guard = new LaianeOficioAccessGuard(currentUserService, Mockito.mock(CurrentAuthenticationContextService.class));

        assertThatThrownBy(() -> guard.requireRead(oficio))
                .isInstanceOf(AccessDeniedException.class)
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(trackingCode.toString()));
    }

    @Test
    void cancelamento_exigeNivelOuro_recusaComPrata() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        CurrentAuthenticationContextService authCtx = Mockito.mock(CurrentAuthenticationContextService.class);

        CurrentAuthenticationContext ctx = new CurrentAuthenticationContext(
                true, true, true, "JWT", "GOVBR", "mp@test", null, null, null, null, null,
                GovBrAssuranceLevel.PRATA, List.of(), List.of(), false, Instant.now());
        when(authCtx.current()).thenReturn(ctx);

        LaianeOficioAccessGuard guard = new LaianeOficioAccessGuard(currentUserService, authCtx);

        assertThatThrownBy(() -> guard.requireHighAssuranceForCancellation(LaianeOficioStatus.CANCELADO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancelamento_passaComNivelOuro() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        CurrentAuthenticationContextService authCtx = Mockito.mock(CurrentAuthenticationContextService.class);

        CurrentAuthenticationContext ctx = new CurrentAuthenticationContext(
                true, true, true, "JWT", "GOVBR", "mp@test", null, null, null, null, null,
                GovBrAssuranceLevel.OURO, List.of(), List.of(), false, Instant.now());
        when(authCtx.current()).thenReturn(ctx);

        LaianeOficioAccessGuard guard = new LaianeOficioAccessGuard(currentUserService, authCtx);

        assertThatCode(() -> guard.requireHighAssuranceForCancellation(LaianeOficioStatus.CANCELADO))
                .doesNotThrowAnyException();
    }

    private LaianeOficio oficio(Usuario origem, Usuario destino) {
        return LaianeOficio.builder()
                .origem(origem)
                .destino(destino)
                .trackingCode(UUID.fromString("01963c1a-7e3f-7000-8000-000000000002"))
                .build();
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }
}
