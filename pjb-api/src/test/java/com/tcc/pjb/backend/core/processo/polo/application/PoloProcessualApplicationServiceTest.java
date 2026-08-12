package com.tcc.pjb.backend.core.processo.polo.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.secretariat.institucional.PoloInstitucionalComposicaoEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class PoloProcessualApplicationServiceTest {

    private PoloProcessualRepository repository;
    private ProcessoRepository processoRepository;
    private ApplicationEventPublisher eventPublisher;
    private PoloProcessualApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(PoloProcessualRepository.class);
        processoRepository = mock(ProcessoRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new PoloProcessualApplicationService(repository, processoRepository, eventPublisher);
        when(repository.save(any(PoloProcessual.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findByProcessoIdAndAtivo(anyLong(), anyBoolean())).thenReturn(List.of());
    }

    @Test
    void incluirPoloAtivoPersisteComAtivoTrue() {
        PoloProcessual polo = service.incluir(1L, TipoPolo.ATIVO, TipoParte.AUTOR,
                "João Silva", "12345678900", "CPF", null, null, 10L, null, null);
        assertTrue(polo.isAtivo());
        verify(repository).save(any(PoloProcessual.class));
    }

    @Test
    void incluirPoloPassivoComCnpjDocumentoTipoCnpj() {
        PoloProcessual polo = service.incluir(1L, TipoPolo.PASSIVO, TipoParte.REU,
                "Empresa SA", "12345678000190", "CNPJ", null, null, null, null, null);
        assertTrue(polo.isPessoaJuridica());
    }

    @Test
    void incluirAmicusCuriaePodeRecorrerFalse() {
        assertFalse(TipoPolo.AMICUS_CURIAE.podeRecorrer());
    }

    @Test
    void incluirMpIsInstitucionalTrue() {
        PoloProcessual polo = service.incluir(1L, TipoPolo.MINISTERIO_PUBLICO,
                TipoParte.MINISTERIO_PUBLICO, "MP Federal", null, null, null, null, 5L, null, null);
        assertTrue(polo.getTipoPolo().isInstitucional());
    }

    @Test
    void excluirDesativaAtivoFalseSemDeletarFisicamente() {
        PoloProcessual existente = new PoloProcessual(1L, TipoPolo.ATIVO, TipoParte.AUTOR,
                "João", null, null, null, null, null, null, null, 0);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        service.excluir(1L, 99L);

        assertFalse(existente.isAtivo());
        verify(repository).save(existente);
    }

    @Test
    void listarDestinatariosCienciaFiltraPorRecebeCiencia() {
        when(repository.findDestinatariosCiencia(1L)).thenReturn(List.of());
        List<PoloProcessual> result = service.listarDestinatariosCiencia(1L);
        assertNotNull(result);
        verify(repository).findDestinatariosCiencia(1L);
    }

    @Test
    void tipoPoloAtivoIsPrincipalTrue() {
        assertTrue(TipoPolo.ATIVO.isPrincipal());
    }

    @Test
    void tipoPoloAmicusCuriaeIsPrincipalFalse() {
        assertFalse(TipoPolo.AMICUS_CURIAE.isPrincipal());
    }

    @Test
    void tipoPoloAmicusCuriaeIsIntervenienteTrue() {
        assertTrue(TipoPolo.AMICUS_CURIAE.isInterveniente());
    }

    @Test
    void tipoPoloMinisterioPublicoIsInstitucionalTrue() {
        assertTrue(TipoPolo.MINISTERIO_PUBLICO.isInstitucional());
    }

    @Test
    void tipoPoloAtivoPodeRecorrerTrue() {
        assertTrue(TipoPolo.ATIVO.podeRecorrer());
    }

    @Test
    void incluirRepresentadoPorAdvogadoPreencheRepresentadoPorId() {
        PoloProcessual polo = service.incluir(1L, TipoPolo.ATIVO, TipoParte.AUTOR,
                "João", null, null, "12345", "SP", 10L, null, 99L);
        assertEquals(99L, polo.getRepresentadoPorId());
    }

    @Test
    void listarPorProcessoRetornaApenasAtivos() {
        when(repository.findByProcessoIdAndAtivo(1L, true)).thenReturn(List.of());
        List<PoloProcessual> result = service.listarPorProcesso(1L);
        assertNotNull(result);
        verify(repository).findByProcessoIdAndAtivo(1L, true);
    }

    @Test
    void incluirLitisconsorcioAtivoOrdemPoloMaiorQueZero() {
        PoloProcessual existente = new PoloProcessual(1L, TipoPolo.ATIVO, TipoParte.AUTOR,
                "Parte1", null, null, null, null, null, null, null, 0);
        when(repository.findByProcessoIdAndAtivo(1L, true)).thenReturn(List.of(existente));

        PoloProcessual polo = service.incluir(1L, TipoPolo.ATIVO, TipoParte.AUTOR,
                "Parte2", null, null, null, null, 11L, null, null);

        assertTrue(polo.getOrdemPolo() > 0, "ordemPolo deve ser > 0 para litisconsorte: " + polo.getOrdemPolo());
    }

    @Test
    void incluirSemUsuarioIdNemIdentidadeIdPersisteComAmbosNull() {
        PoloProcessual polo = service.incluir(1L, TipoPolo.ATIVO, TipoParte.AUTOR,
                "João", null, null, null, null, null, null, null);
        assertNull(polo.getUsuarioId());
        assertNull(polo.getIdentidadeId());
    }

    @Test
    void incluirParteAutorNuncaPublicaEventoInstitucional() {
        service.incluir(1L, TipoPolo.ATIVO, TipoParte.AUTOR,
                "João Silva", "12345678900", "CPF", null, null, 10L, null, null);

        verify(eventPublisher, never()).publishEvent(any(PoloInstitucionalComposicaoEvent.class));
    }

    @Test
    void incluirMinisterioPublicoPublicaEventoComComarcaDoProcesso() {
        when(processoRepository.findById(1L))
                .thenReturn(Optional.of(Processo.builder().id(1L).comarca("Fortaleza").build()));

        service.incluir(1L, TipoPolo.MINISTERIO_PUBLICO, TipoParte.MINISTERIO_PUBLICO,
                "MP Federal", null, null, null, null, 5L, null, null);

        verify(eventPublisher).publishEvent(
                new PoloInstitucionalComposicaoEvent(1L, "Fortaleza", TipoUnidadeInstitucional.PROMOTORIA));
    }

    @Test
    void incluirDefensoriaPublicaEventoNucleoDefensoria() {
        when(processoRepository.findById(2L))
                .thenReturn(Optional.of(Processo.builder().id(2L).comarca("Sobral").build()));

        service.incluir(2L, TipoPolo.DEFENSORIA, TipoParte.REU,
                "Defensoria Publica", null, null, null, null, null, null, null,
                "CE", "Sobral", "Sobral");

        verify(eventPublisher).publishEvent(
                new PoloInstitucionalComposicaoEvent(2L, "Sobral", TipoUnidadeInstitucional.NUCLEO_DEFENSORIA));
    }

    @Test
    void incluirProcuradoriaPublicaEventoProcuradoriaPublica() {
        when(processoRepository.findById(3L))
                .thenReturn(Optional.of(Processo.builder().id(3L).comarca("Fortaleza").build()));

        service.incluir(3L, TipoPolo.PROCURADORIA, TipoParte.REU,
                "Procuradoria Municipal", null, null, null, null, null, null, null,
                "CE", "Fortaleza", "Fortaleza", "PGM Fortaleza");

        verify(eventPublisher).publishEvent(
                new PoloInstitucionalComposicaoEvent(3L, "Fortaleza", TipoUnidadeInstitucional.PROCURADORIA_PUBLICA));
    }

    @Test
    void incluirInstitucionalSemProcessoResolvidoNaoPublicaEvento() {
        when(processoRepository.findById(4L)).thenReturn(Optional.empty());

        service.incluir(4L, TipoPolo.MINISTERIO_PUBLICO, TipoParte.MINISTERIO_PUBLICO,
                "MP Federal", null, null, null, null, null, null, null);

        verify(eventPublisher, never()).publishEvent(any(PoloInstitucionalComposicaoEvent.class));
    }

    @Test
    void excluirNuncaPublicaEventoInstitucional() {
        PoloProcessual existente = new PoloProcessual(1L, TipoPolo.MINISTERIO_PUBLICO, TipoParte.MINISTERIO_PUBLICO,
                "MP", null, null, null, null, null, null, null, 0);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        service.excluir(1L, 99L);

        verify(eventPublisher, never()).publishEvent(any(PoloInstitucionalComposicaoEvent.class));
    }
}
