package com.tcc.pjb.backend.service.certidao;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CertidaoBatchEmissionServiceTest {

    private final CertidaoPendenciaDetector detector = new CertidaoPendenciaDetector();
    private final CertidaoAutoPreparationService preparationService =
            new CertidaoAutoPreparationService(detector);
    private final CertidaoBatchEmissionService service =
            new CertidaoBatchEmissionService(preparationService);

    @Test
    void emitirLote_todos_quandoTodosAptos() {
        var itens = List.of(
                new CertidaoBatchEmissionService.CertidaoBatchItem(UUID.randomUUID(), "TRANSITO", true),
                new CertidaoBatchEmissionService.CertidaoBatchItem(UUID.randomUUID(), "OBJETO", true));
        var resultado = service.emitirLote(itens);
        assertThat(resultado.total()).isEqualTo(2);
        assertThat(resultado.emitidas()).isEqualTo(2);
        assertThat(resultado.bloqueadas()).isEqualTo(0);
    }

    @Test
    void emitirLote_bloqueiaInaptosEEmiteProntos() {
        var itens = List.of(
                new CertidaoBatchEmissionService.CertidaoBatchItem(UUID.randomUUID(), "TRANSITO", true),
                new CertidaoBatchEmissionService.CertidaoBatchItem(UUID.randomUUID(), "OBJETO", false));
        var resultado = service.emitirLote(itens);
        assertThat(resultado.total()).isEqualTo(2);
        assertThat(resultado.emitidas()).isEqualTo(1);
        assertThat(resultado.bloqueadas()).isEqualTo(1);
    }

    @Test
    void emitirLote_nenhum_quandoTodosBloqueados() {
        var itens = List.of(
                new CertidaoBatchEmissionService.CertidaoBatchItem(UUID.randomUUID(), "TRANSITO", false));
        var resultado = service.emitirLote(itens);
        assertThat(resultado.emitidas()).isEqualTo(0);
        assertThat(resultado.numerosEmitidos()).isEmpty();
    }
}
