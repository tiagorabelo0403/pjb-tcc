package com.tcc.pjb.backend.core.processo.carga.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.processo.CargaProcesso;
import com.tcc.pjb.backend.model.repository.CargaProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CargaProcessoApplicationServiceTest {

    private CargaProcessoRepository repository;
    private AuditLedgerService auditLedgerService;
    private CargaProcessoApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(CargaProcessoRepository.class);
        auditLedgerService = mock(AuditLedgerService.class);
        service = new CargaProcessoApplicationService(repository, auditLedgerService);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registrarSaidaStatusAtiva() {
        when(repository.findCargaAtivaByProcessoId(1L)).thenReturn(Optional.empty());
        CargaProcesso carga = service.registrarSaida(1L, 10L, 20L, "CONCLUSO_GABINETE",
                null, 1, "teste");
        assertTrue(carga.isAtiva());
        assertEquals("ATIVA", carga.getStatus());
    }

    @Test
    void registrarSaidaComCargaJaAtivaLancaExcecao() {
        CargaProcesso existente = new CargaProcesso(1L, 10L, 20L, "CONCLUSO_GABINETE",
                null, 1, null);
        when(repository.findCargaAtivaByProcessoId(1L)).thenReturn(Optional.of(existente));
        assertThrows(IllegalStateException.class,
                () -> service.registrarSaida(1L, 10L, 20L, "CONCLUSO_GABINETE", null, 1, null));
    }

    @Test
    void registrarRetornoSetaStatusDevolvida() {
        CargaProcesso carga = new CargaProcesso(1L, 10L, 20L, "CONCLUSO_GABINETE",
                null, 1, null);
        when(repository.findById(1L)).thenReturn(Optional.of(carga));
        service.registrarRetorno(1L, 99L);
        assertEquals("DEVOLVIDA", carga.getStatus());
        assertNotNull(carga.getDataRetorno());
    }

    @Test
    void registrarExtravioCargaExtraviada() {
        CargaProcesso carga = new CargaProcesso(1L, 10L, 20L, "CONCLUSO_GABINETE",
                null, 1, null);
        when(repository.findById(1L)).thenReturn(Optional.of(carga));
        service.registrarExtravio(1L, 99L, "sumiu");
        assertEquals("EXTRAVIADA", carga.getStatus());
    }

    @Test
    void cargaAtivaAposRetornoRetornaEmpty() {
        when(repository.findCargaAtivaByProcessoId(1L)).thenReturn(Optional.empty());
        assertTrue(service.cargaAtiva(1L).isEmpty());
    }

    @Test
    void cargaAtivaComCargaAtivaRetornaOf() {
        CargaProcesso carga = new CargaProcesso(1L, 10L, 20L, "CONCLUSO_GABINETE",
                null, 1, null);
        when(repository.findCargaAtivaByProcessoId(1L)).thenReturn(Optional.of(carga));
        assertTrue(service.cargaAtiva(1L).isPresent());
    }

    @Test
    void historicoRetornaTodosEmOrdemCronologica() {
        when(repository.findByProcessoIdOrderByDataSaidaAsc(1L)).thenReturn(List.of());
        List<CargaProcesso> hist = service.historico(1L);
        assertNotNull(hist);
        verify(repository).findByProcessoIdOrderByDataSaidaAsc(1L);
    }

    @Test
    void registrarRetornoStatusJaDevolvidaLancaExcecao() {
        CargaProcesso carga = new CargaProcesso(1L, 10L, 20L, "CONCLUSO_GABINETE",
                null, 1, null);
        carga.registrarRetorno();
        when(repository.findById(1L)).thenReturn(Optional.of(carga));
        assertThrows(IllegalStateException.class, () -> service.registrarRetorno(1L, 99L));
    }

    @Test
    void volumesDevemSerMaiorOuIgualA1() {
        when(repository.findCargaAtivaByProcessoId(1L)).thenReturn(Optional.empty());
        CargaProcesso carga = service.registrarSaida(1L, 10L, 20L, "ARQUIVO",
                null, 1, null);
        assertTrue(carga.getVolumes() >= 1);
    }

    @Test
    void registrarExtravioPersistObservacao() {
        CargaProcesso carga = new CargaProcesso(1L, 10L, 20L, "ARQUIVO",
                null, 1, null);
        when(repository.findById(1L)).thenReturn(Optional.of(carga));
        service.registrarExtravio(1L, 99L, "processo não encontrado");
        assertEquals("processo não encontrado", carga.getObservacao());
    }
}
