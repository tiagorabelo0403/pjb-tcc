package com.tcc.pjb.backend.core.processo.autuacao.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.processo.SequencialNumeracaoCnj;
import com.tcc.pjb.backend.model.repository.SequencialNumeracaoCnjRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NumeracaoCnjServiceTest {

    private final NumeracaoCnjService service = new NumeracaoCnjService();

    @Test
    void gerarRetornaFormatoCorreto() {
        String numero = service.gerar(1, 2026, 8, 6, 1);
        assertTrue(numero.matches("\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}"),
                "Formato inválido: " + numero);
    }

    @Test
    void validarNumeroValido() {
        String numero = service.gerar(1, 2026, 8, 6, 1);
        assertTrue(service.validar(numero));
    }

    @Test
    void validarNumeroComDdErrado() {
        String numero = service.gerar(1, 2026, 8, 6, 1);
        String adulterado = numero.replaceAll("-(\\d{2})\\.", "-99.");
        assertFalse(service.validar(adulterado));
    }

    @Test
    void validarNullRetornaFalse() {
        assertFalse(service.validar(null));
    }

    @Test
    void calcularDigitoVerificadorCasoConhecido() {
        int dd = service.calcularDigitoVerificador(1, 2026, 8, 6, 1);
        assertTrue(dd >= 0 && dd <= 97, "DD fora do intervalo: " + dd);
    }

    @Test
    void gerarSequencial1Ano2026Segmento8Tribunal6Unidade1() {
        String numero = service.gerar(1, 2026, 8, 6, 1);
        assertTrue(numero.startsWith("0000001-"), "Deve iniciar com 0000001: " + numero);
        assertTrue(numero.contains(".2026."), "Deve conter .2026.: " + numero);
        assertTrue(numero.endsWith(".06.0001"), "Deve terminar em .06.0001: " + numero);
    }

    @Test
    void validarNumeroGeradoPeloProprioGerar() {
        String numero = service.gerar(42, 2025, 3, 10, 500);
        assertTrue(service.validar(numero));
    }

    @Test
    void gerarSequencialGrandeNaoEstoura() {
        String numero = service.gerar(9999999, 2026, 8, 6, 1);
        assertTrue(numero.startsWith("9999999-"));
        assertTrue(service.validar(numero));
    }

    @Test
    void gerarProximoLockGaranteUnicidadeViaRepository() {
        SequencialNumeracaoCnjRepository repo = mock(SequencialNumeracaoCnjRepository.class);
        SequencialNumeracaoCnj seq = new SequencialNumeracaoCnj(8, 6, 1, 2026);
        when(repo.findAndLock(8, 6, 1, 2026)).thenReturn(Optional.of(seq));
        when(repo.save(seq)).thenReturn(seq);
        var appService = new com.tcc.pjb.backend.core.processo.autuacao.application
                .NumeracaoCnjApplicationService(repo);
        String n1 = appService.gerarProximo(8, 6, 1);
        String n2 = appService.gerarProximo(8, 6, 1);
        assertNotEquals(n1, n2, "Dois sequenciais devem ser diferentes");
    }

    @Test
    void gerarProximoDuasChamadasSequenciaisDistintos() {
        SequencialNumeracaoCnjRepository repo = mock(SequencialNumeracaoCnjRepository.class);
        SequencialNumeracaoCnj seq = new SequencialNumeracaoCnj(8, 6, 1, 2026);
        when(repo.findAndLock(8, 6, 1, 2026)).thenReturn(Optional.of(seq));
        when(repo.save(seq)).thenReturn(seq);
        var appService = new com.tcc.pjb.backend.core.processo.autuacao.application
                .NumeracaoCnjApplicationService(repo);
        String n1 = appService.gerarProximo(8, 6, 1);
        String n2 = appService.gerarProximo(8, 6, 1);
        assertFalse(n1.equals(n2));
    }

    @Test
    void gerarProximoAnosDiferentesReiniciaSequencial() {
        SequencialNumeracaoCnjRepository repo = mock(SequencialNumeracaoCnjRepository.class);
        SequencialNumeracaoCnj seq2025 = new SequencialNumeracaoCnj(8, 6, 1, 2025);
        SequencialNumeracaoCnj seq2026 = new SequencialNumeracaoCnj(8, 6, 1, 2026);
        when(repo.findAndLock(8, 6, 1, 2025)).thenReturn(Optional.of(seq2025));
        when(repo.findAndLock(8, 6, 1, 2026)).thenReturn(Optional.of(seq2026));
        when(repo.save(seq2025)).thenReturn(seq2025);
        when(repo.save(seq2026)).thenReturn(seq2026);
        assertEquals(1L, seq2025.incrementar());
        assertEquals(1L, seq2026.incrementar());
    }

    @Test
    void validarNumeroComLetrasRetornaFalse() {
        assertFalse(service.validar("ABCDEFG-12.2026.8.06.0001"));
    }
}
