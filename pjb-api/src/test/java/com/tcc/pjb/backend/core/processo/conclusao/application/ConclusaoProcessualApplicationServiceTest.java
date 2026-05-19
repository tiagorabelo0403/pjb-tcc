package com.tcc.pjb.backend.core.processo.conclusao.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.estado.application.ProcessoEstadoApplicationService;
import com.tcc.pjb.backend.core.servidor.application.FuncaoServidorApplicationService;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.processo.ConclusaoProcessual;
import com.tcc.pjb.backend.model.repository.ConclusaoProcessualRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class ConclusaoProcessualApplicationServiceTest {

    private ConclusaoProcessualRepository conclusaoRepository;
    private FuncaoServidorApplicationService funcaoServidorService;
    private ProcessoEstadoApplicationService estadoService;
    private ConclusaoProcessualApplicationService service;

    @BeforeEach
    void setUp() {
        conclusaoRepository = mock(ConclusaoProcessualRepository.class);
        funcaoServidorService = mock(FuncaoServidorApplicationService.class);
        estadoService = mock(ProcessoEstadoApplicationService.class);
        service = new ConclusaoProcessualApplicationService(
                conclusaoRepository, funcaoServidorService, estadoService);
        when(conclusaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void concluirRetornaStatusPendenteEDataLimite10DiasUteis() {
        when(funcaoServidorService.temPermissaoEmQualquerUnidade(1L, "concluir")).thenReturn(true);
        ConclusaoProcessual c = service.concluir(10L, 5L, 1L, "PARA_DECISAO", "urgente");
        assertEquals("PENDENTE", c.getStatus());
        assertNotNull(c.getDataLimite());
        assertTrue(c.getDataLimite().isAfter(Instant.now().plus(9, ChronoUnit.DAYS)));
    }

    @Test
    void concluirServidorSemPodeConcluirLancaExcecao() {
        when(funcaoServidorService.temPermissaoEmQualquerUnidade(1L, "concluir")).thenReturn(false);
        assertThrows(SecurityException.class,
                () -> service.concluir(10L, 5L, 1L, "PARA_DECISAO", null));
    }

    @Test
    void devolverPeloMagistradoCorretoSetaDevolvida() {
        ConclusaoProcessual conclusao = new ConclusaoProcessual(10L, 5L, 1L,
                "PARA_DECISAO", null, Instant.now().plusSeconds(86400));
        when(conclusaoRepository.findById(1L)).thenReturn(Optional.of(conclusao));
        service.devolver(1L, 5L);
        assertEquals("DEVOLVIDA", conclusao.getStatus());
    }

    @Test
    void devolverPorMagistradoDiferenteLancaExcecao() {
        ConclusaoProcessual conclusao = new ConclusaoProcessual(10L, 5L, 1L,
                "PARA_DECISAO", null, Instant.now().plusSeconds(86400));
        when(conclusaoRepository.findById(1L)).thenReturn(Optional.of(conclusao));
        assertThrows(SecurityException.class, () -> service.devolver(1L, 99L));
    }

    @Test
    void devolverStatusJaDevolvidaLancaExcecao() {
        ConclusaoProcessual conclusao = new ConclusaoProcessual(10L, 5L, 1L,
                "PARA_DECISAO", null, Instant.now().plusSeconds(86400));
        conclusao.devolver();
        when(conclusaoRepository.findById(1L)).thenReturn(Optional.of(conclusao));
        assertThrows(IllegalStateException.class, () -> service.devolver(1L, 5L));
    }

    @Test
    void processarExpiradasAlteraParaExpirada() {
        ConclusaoProcessual pendente = new ConclusaoProcessual(10L, 5L, 1L,
                "PARA_DECISAO", null, Instant.now().minusSeconds(3600));
        when(conclusaoRepository.findExpiradas(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(pendente));
        when(conclusaoRepository.saveAll(any())).thenReturn(List.of(pendente));
        int count = service.processarExpiradas();
        assertEquals("EXPIRADA", pendente.getStatus());
        assertEquals(1, count);
    }

    @Test
    void processarExpiradasNaoAlteraDevolvidas() {
        ConclusaoProcessual devolvida = new ConclusaoProcessual(10L, 5L, 1L,
                "PARA_DECISAO", null, Instant.now().minusSeconds(3600));
        devolvida.devolver();
        when(conclusaoRepository.findExpiradas(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        int count = service.processarExpiradas();
        assertEquals(0, count);
    }

    @Test
    void processarExpiradasRespeita50Lote() {
        when(conclusaoRepository.findExpiradas(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        service.processarExpiradas();
        verify(conclusaoRepository).findExpiradas(any(Instant.class),
                eq(org.springframework.data.domain.PageRequest.of(0, 50)));
    }

    @Test
    void pendentesDoMagistradoRetornaApenasComStatusPendente() {
        when(conclusaoRepository.findByMagistradoIdAndStatus(5L, "PENDENTE")).thenReturn(List.of());
        var result = service.pendentesDoMagistrado(5L);
        assertNotNull(result);
        verify(conclusaoRepository).findByMagistradoIdAndStatus(5L, "PENDENTE");
    }

    @Test
    void concluirTransitaProcessoParaConclusoJuiz() {
        when(funcaoServidorService.temPermissaoEmQualquerUnidade(1L, "concluir")).thenReturn(true);
        service.concluir(10L, 5L, 1L, "PARA_DECISAO", null);
        verify(estadoService).transitar(eq(10L), eq(StatusProcesso.CONCLUSO_JUIZ), eq(1L), anyString());
    }

    @Test
    void devolverTransitaProcessoParaDistribuido() {
        ConclusaoProcessual conclusao = new ConclusaoProcessual(10L, 5L, 1L,
                "PARA_DECISAO", null, Instant.now().plusSeconds(86400));
        when(conclusaoRepository.findById(1L)).thenReturn(Optional.of(conclusao));
        service.devolver(1L, 5L);
        verify(estadoService).transitar(eq(10L), eq(StatusProcesso.DISTRIBUIDO), eq(5L), anyString());
    }

    @Test
    void dataLimiteIgnoraSabadosEDomingos() {
        when(funcaoServidorService.temPermissaoEmQualquerUnidade(1L, "concluir")).thenReturn(true);
        ConclusaoProcessual c = service.concluir(10L, 5L, 1L, "PARA_DECISAO", null);
        assertTrue(c.getDataLimite().isAfter(Instant.now().plus(13, ChronoUnit.DAYS)),
                "10 dias úteis deve ser > 13 dias corridos (inclui finais de semana)");
    }
}
