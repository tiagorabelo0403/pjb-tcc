package com.tcc.pjb.backend.service.processual.arquivamento;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ArquivamentoPendenciaCheckerTest {

    private final ArquivamentoPendenciaChecker checker = newChecker();

    private static ArquivamentoPendenciaChecker newChecker() {
        try {
            var c = ArquivamentoPendenciaChecker.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void verificar_semPendencias_quandoTudoOk() {
        var input = new ArquivamentoPendenciaChecker.ArquivamentoInput(
                true, false, false, false, true, true);
        var pendencias = checker.verificar(input);
        assertThat(pendencias).isEmpty();
    }

    @Test
    void verificar_pendenciaBloqueante_quandoCustasPendentes() {
        var input = new ArquivamentoPendenciaChecker.ArquivamentoInput(
                false, false, false, false, true, true);
        var pendencias = checker.verificar(input);
        assertThat(pendencias).anyMatch(p -> p.bloqueante() && p.descricao().contains("Custas"));
    }

    @Test
    void verificar_pendenciaBloqueante_quandoRecursoPossivel() {
        var input = new ArquivamentoPendenciaChecker.ArquivamentoInput(
                true, false, true, false, true, true);
        var pendencias = checker.verificar(input);
        assertThat(pendencias).anyMatch(p -> p.bloqueante() && p.descricao().contains("recursal"));
    }

    @Test
    void verificar_pendenciaNaoBloqueante_quandoDocumentoNaoLido() {
        var input = new ArquivamentoPendenciaChecker.ArquivamentoInput(
                true, false, false, true, true, true);
        var pendencias = checker.verificar(input);
        assertThat(pendencias).anyMatch(p -> !p.bloqueante());
    }
}
