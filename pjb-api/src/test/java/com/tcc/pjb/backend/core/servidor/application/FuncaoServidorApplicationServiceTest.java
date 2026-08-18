package com.tcc.pjb.backend.core.servidor.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class FuncaoServidorApplicationServiceTest {

    private FuncaoServidorJudiciarioRepository repository;
    private FuncaoServidorApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(FuncaoServidorJudiciarioRepository.class);
        service = new FuncaoServidorApplicationService(repository);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void designarRetornaAtivoTrue() {
        when(repository.findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(any(), any(), any(), anyBoolean()))
                .thenReturn(Optional.empty());
        FuncaoServidorJudiciarioEntity e = service.designar(1L, 10L,
                FuncaoServidorJudiciario.ANALISTA_JUDICIARIO,
                LocalDate.now(), 99L, "Portaria 1/2026");
        assertTrue(e.isAtivo());
    }

    @Test
    void designarMesmoUsuarioUnidadeFuncaoLancaExcecao() {
        FuncaoServidorJudiciarioEntity existente = new FuncaoServidorJudiciarioEntity(
                1L, 10L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO,
                LocalDate.now(), null, null);
        when(repository.findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(
                1L, 10L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO, true))
                .thenReturn(Optional.of(existente));

        assertThrows(DataIntegrityViolationException.class,
                () -> service.designar(1L, 10L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO,
                        LocalDate.now(), null, null));
    }

    @Test
    void encerrarSetaAtivoFalseEDataFim() {
        FuncaoServidorJudiciarioEntity entity = new FuncaoServidorJudiciarioEntity(
                1L, 10L, FuncaoServidorJudiciario.TECNICO_JUDICIARIO,
                LocalDate.now().minusDays(10), null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.encerrar(1L, LocalDate.now(), 99L);

        assertFalse(entity.isAtivo());
    }

    @Test
    void funcaoAtivaAposEncerrarRetornaEmpty() {
        FuncaoServidorJudiciarioEntity entity = new FuncaoServidorJudiciarioEntity(
                1L, 10L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO,
                LocalDate.now(), null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        service.encerrar(1L, LocalDate.now(), 99L);

        when(repository.findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(
                1L, 10L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO, true))
                .thenReturn(Optional.empty());

        assertTrue(service.funcaoAtiva(1L, 10L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO).isEmpty());
    }

    @Test
    void diretorSecretariaPoderProferirTrue() {
        assertTrue(FuncaoServidorJudiciario.DIRETOR_SECRETARIA.podeProferir());
    }

    @Test
    void tecnicoJudiciarioPoderProferirFalse() {
        assertFalse(FuncaoServidorJudiciario.TECNICO_JUDICIARIO.podeProferir());
    }

    @Test
    void escrivaoJudicialPoderIntimarTrue() {
        assertTrue(FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL.podeIntimar());
    }

    @Test
    void podeExecutarAnalistaJudiciarioConcluirTrue() {
        FuncaoServidorJudiciarioEntity entity = new FuncaoServidorJudiciarioEntity(
                1L, 10L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO,
                LocalDate.now(), null, null);
        when(repository.findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(
                1L, 10L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO, true))
                .thenReturn(Optional.of(entity));

        assertTrue(service.podeExecutar(1L, 10L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO, "concluir"));
    }

    @Test
    void podeExecutarAssistenteJudiciarioArquivarFalse() {
        FuncaoServidorJudiciarioEntity entity = new FuncaoServidorJudiciarioEntity(
                1L, 10L, FuncaoServidorJudiciario.ASSISTENTE_JUDICIARIO,
                LocalDate.now(), null, null);
        when(repository.findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(
                1L, 10L, FuncaoServidorJudiciario.ASSISTENTE_JUDICIARIO, true))
                .thenReturn(Optional.of(entity));

        assertFalse(service.podeExecutar(1L, 10L, FuncaoServidorJudiciario.ASSISTENTE_JUDICIARIO, "arquivar"));
    }

    @Test
    void isVigenteDataAntesDataInicioFalse() {
        FuncaoServidorJudiciarioEntity entity = new FuncaoServidorJudiciarioEntity(
                1L, 10L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO,
                LocalDate.of(2026, 6, 1), null, null);
        assertFalse(entity.isVigente(LocalDate.of(2026, 5, 31)));
    }

    @Test
    void isVigenteDataAposDataFimFalse() {
        FuncaoServidorJudiciarioEntity entity = new FuncaoServidorJudiciarioEntity(
                1L, 10L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO,
                LocalDate.of(2026, 1, 1), null, null);
        entity.encerrar(LocalDate.of(2026, 3, 31));
        assertFalse(entity.isVigente(LocalDate.of(2026, 4, 1)));
    }

    @Test
    void listarPorUnidadeRetornaApenasAtivos() {
        when(repository.findByUnidadeIdAndAtivo(10L, true)).thenReturn(List.of());
        var result = service.listarPorUnidade(10L);
        assertNotNull(result);
        verify(repository).findByUnidadeIdAndAtivo(10L, true);
    }
}
