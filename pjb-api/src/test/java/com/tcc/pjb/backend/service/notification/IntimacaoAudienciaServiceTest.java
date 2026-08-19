package com.tcc.pjb.backend.service.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.intimacao.CriarIntimacaoRequest;
import com.tcc.pjb.backend.model.dto.intimacao.IntimacaoAudienciaResponse;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.IntimacaoAudiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.TipoDestinatarioIntimacao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.IntimacaoAudienciaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntimacaoAudienciaServiceTest {

    private IntimacaoAudienciaRepository intimacaoRepository;
    private AudienciaRepository audienciaRepository;
    private PjbAuthorizationService authorizationService;
    private CurrentUserService currentUserService;
    private IntimacaoAudienciaService service;

    @BeforeEach
    void setUp() {
        intimacaoRepository = mock(IntimacaoAudienciaRepository.class);
        audienciaRepository = mock(AudienciaRepository.class);
        authorizationService = mock(PjbAuthorizationService.class);
        currentUserService = mock(CurrentUserService.class);
        service = new IntimacaoAudienciaService(intimacaoRepository, audienciaRepository, authorizationService, currentUserService);
        when(currentUserService.getRequired()).thenReturn(servidor());
    }

    private Usuario servidor() {
        return Usuario.builder().id(1L).tipoUsuario(TipoUsuario.SERVIDOR_FORUM).build();
    }

    private Usuario administrador() {
        return Usuario.builder().id(2L).tipoUsuario(TipoUsuario.ADMINISTRADOR).build();
    }

    private Audiencia audienciaComProcesso() {
        Processo processo = new Processo();
        processo.setUnidadeJudiciariaCodigo("UNIDADE-1");
        return Audiencia.builder()
                .id(1L)
                .processo(processo)
                .build();
    }

    private CriarIntimacaoRequest request() {
        return new CriarIntimacaoRequest(
                "Fulano de Tal",
                TipoDestinatarioIntimacao.ADVOGADO_AUTOR,
                "12345",
                "fulano@example.com",
                "EMAIL",
                null
        );
    }

    @Test
    void intimarComFuncaoAtivaCriaIntimacao() {
        Audiencia audiencia = audienciaComProcesso();
        when(audienciaRepository.findById(1L)).thenReturn(Optional.of(audiencia));
        doNothing().when(authorizationService)
                .requireFuncaoServidorCapability(any(Processo.class), eq(AcaoProcessualServidor.INTIMAR));
        when(intimacaoRepository.save(any(IntimacaoAudiencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Optional<IntimacaoAudienciaResponse> resultado = service.intimar(1L, request());

        assertTrue(resultado.isPresent());
        assertEquals("Fulano de Tal", resultado.get().destinatarioNome());
        verify(authorizationService)
                .requireFuncaoServidorCapability(eq(audiencia.getProcesso()), eq(AcaoProcessualServidor.INTIMAR));
    }

    @Test
    void intimarSemFuncaoAtivaLancaExcecaoEPropagaAtravesDoMap() {
        Audiencia audiencia = audienciaComProcesso();
        when(audienciaRepository.findById(1L)).thenReturn(Optional.of(audiencia));
        doThrow(new AccessDeniedPjbException("Acesso negado à ação processual do servidor"))
                .when(authorizationService)
                .requireFuncaoServidorCapability(any(Processo.class), eq(AcaoProcessualServidor.INTIMAR));

        assertThrows(AccessDeniedPjbException.class, () -> service.intimar(1L, request()));

        verify(intimacaoRepository, never()).save(any());
    }

    @Test
    void intimarAudienciaInexistenteRetornaOptionalVazioSemChamarAutorizacao() {
        when(audienciaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<IntimacaoAudienciaResponse> resultado = service.intimar(99L, request());

        assertTrue(resultado.isEmpty());
        verify(authorizationService, never())
                .requireFuncaoServidorCapability(any(), any());
    }

    @Test
    void intimarLoteComFuncaoAtivaCriaTodasAsIntimacoes() {
        Audiencia audiencia = audienciaComProcesso();
        when(audienciaRepository.findById(1L)).thenReturn(Optional.of(audiencia));
        doNothing().when(authorizationService)
                .requireFuncaoServidorCapability(any(Processo.class), eq(AcaoProcessualServidor.INTIMAR));
        when(intimacaoRepository.save(any(IntimacaoAudiencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var resultado = service.intimarLote(1L, List.of(request(), request()));

        assertEquals(2, resultado.size());
        verify(authorizationService)
                .requireFuncaoServidorCapability(eq(audiencia.getProcesso()), eq(AcaoProcessualServidor.INTIMAR));
    }

    @Test
    void intimarLoteSemFuncaoAtivaLancaExcecaoSemSalvarNenhumaIntimacao() {
        Audiencia audiencia = audienciaComProcesso();
        when(audienciaRepository.findById(1L)).thenReturn(Optional.of(audiencia));
        doThrow(new AccessDeniedPjbException("Acesso negado à ação processual do servidor"))
                .when(authorizationService)
                .requireFuncaoServidorCapability(any(Processo.class), eq(AcaoProcessualServidor.INTIMAR));

        assertThrows(AccessDeniedPjbException.class,
                () -> service.intimarLote(1L, List.of(request(), request())));

        verify(intimacaoRepository, never()).save(any());
    }

    @Test
    void administradorIntimaSemSerBloqueadoPelaNovaChecagemDeFuncaoServidor() {
        Audiencia audiencia = audienciaComProcesso();
        when(audienciaRepository.findById(1L)).thenReturn(Optional.of(audiencia));
        when(currentUserService.getRequired()).thenReturn(administrador());
        when(intimacaoRepository.save(any(IntimacaoAudiencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Optional<IntimacaoAudienciaResponse> resultado = service.intimar(1L, request());

        assertTrue(resultado.isPresent());
        verify(authorizationService, never())
                .requireFuncaoServidorCapability(any(), any());
    }

    @Test
    void administradorIntimaLoteSemSerBloqueadoPelaNovaChecagemDeFuncaoServidor() {
        Audiencia audiencia = audienciaComProcesso();
        when(audienciaRepository.findById(1L)).thenReturn(Optional.of(audiencia));
        when(currentUserService.getRequired()).thenReturn(administrador());
        when(intimacaoRepository.save(any(IntimacaoAudiencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var resultado = service.intimarLote(1L, List.of(request(), request()));

        assertEquals(2, resultado.size());
        verify(authorizationService, never())
                .requireFuncaoServidorCapability(any(), any());
    }
}
