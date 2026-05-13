package com.tcc.pjb.backend.service.processual.malha.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaActorContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

class ProcessoMalhaAuthorizationServiceTest {

    private final CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
    private final ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
    private final ProcessoMalhaAuthorizationService service = new ProcessoMalhaAuthorizationService(currentUserService, processoRepository);

    @Test
    void shouldResolveElevatedViewerUsingProfileFallbackAndRequestedRole() {
        Usuario usuario = usuario(10L, "Servidor Elevado", "111.222.333-44", null, "SERVIDOR_FORUM");
        Processo processo = processo(91L, RamoDireito.PENAL, "99999999999", "88888888888", null);
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(processoRepository.findContextoCompletoById(91L)).thenReturn(Optional.of(processo));

        ProcessoMalhaActorContext context = service.resolver(91L, "ADVOGADO", "CIVIL");

        assertEquals(TipoUsuario.SERVIDOR_FORUM, context.tipoUsuario());
        assertEquals(TipoUsuario.ADVOGADO, context.papelEfetivo());
        assertEquals(RamoDireito.CIVIL, context.ramoEfetivo());
        assertTrue(context.visualizacaoElevada());
        assertTrue(context.visualizacaoContextual());
        assertFalse(context.parteRelacionada());
        assertTrue(context.roles().contains("SERVIDOR_FORUM"));
        assertTrue(context.roles().contains("MALHA_VISUALIZACAO_ELEVADA"));
    }

    @Test
    void shouldFallbackToAdvogadoAndKeepOwnRoleForRelatedParty() {
        Usuario usuario = usuario(11L, "Advogado Parte", "123.456.789-00", "12345/CE", null);
        Processo processo = processo(92L, RamoDireito.TRABALHISTA, "12345678900", "55555555555", null);
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(processoRepository.findContextoCompletoById(92L)).thenReturn(Optional.of(processo));

        ProcessoMalhaActorContext context = service.resolver(92L, "MAGISTRADO", "PENAL");

        assertEquals(TipoUsuario.ADVOGADO, context.tipoUsuario());
        assertEquals(TipoUsuario.ADVOGADO, context.papelEfetivo());
        assertEquals(RamoDireito.TRABALHISTA, context.ramoEfetivo());
        assertFalse(context.visualizacaoElevada());
        assertTrue(context.visualizacaoContextual());
        assertTrue(context.parteRelacionada());
        assertTrue(context.roles().contains("MALHA_PARTE_RELACIONADA"));
    }

    @Test
    void shouldDenyUnrelatedCitizenWithoutContextualPrivilege() {
        Usuario usuario = usuario(12L, "Cidadao", "000.111.222-33", null, null);
        Processo processo = processo(93L, RamoDireito.CIVIL, "44444444444", "55555555555", null);
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(processoRepository.findContextoCompletoById(93L)).thenReturn(Optional.of(processo));

        assertThrows(AccessDeniedException.class, () -> service.resolver(93L, "ADVOGADO", "PENAL"));
    }

    @Test
    void shouldAllowRequestedRoleOnlyForMatchingOrElevatedActor() {
        Usuario advogado = usuario(21L, "Advogado", "12312312312", "123/CE", null);
        when(currentUserService.getOptional()).thenReturn(Optional.of(advogado));
        assertTrue(service.canAccessRequestedRole("ADVOGADO"));
        assertFalse(service.canAccessRequestedRole("MAGISTRADO"));

        Usuario servidor = usuario(22L, "Servidor", "99999999999", null, null);
        servidor.setTipoUsuario(TipoUsuario.SERVIDOR_FORUM);
        when(currentUserService.getOptional()).thenReturn(Optional.of(servidor));
        assertTrue(service.canAccessRequestedRole("MAGISTRADO"));
    }

    private static Usuario usuario(Long id,
                                   String nome,
                                   String cpf,
                                   String oab,
                                   String perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setCpf(cpf);
        usuario.setOab(oab);
        usuario.setPerfil(perfil);
        return usuario;
    }

    private static Processo processo(Long id,
                                     RamoDireito ramoDireito,
                                     String cpfAutor,
                                     String cpfReu,
                                     Usuario usuarioVinculado) {
        Processo processo = new Processo();
        processo.setId(id);
        processo.setRamoDireito(ramoDireito);
        processo.setParteAutoraCpf(cpfAutor);
        processo.setParteReuCpf(cpfReu);
        processo.setUsuario(usuarioVinculado);
        return processo;
    }
}
