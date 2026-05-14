package com.tcc.pjb.backend.service.secretariat.ingest;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessoExternoCargaServiceTest {

    private final ProcessoExternoOrigemResolver resolver = new ProcessoExternoOrigemResolver();
    private final ProcessoExternalNormalizationService normalizationService =
            new ProcessoExternalNormalizationService();
    private final ProcessoExternoImportacaoService importacaoService =
            new ProcessoExternoImportacaoService(resolver, normalizationService);
    private final ProcessoExternoCargaService service =
            new ProcessoExternoCargaService(importacaoService);

    @Test
    void processarLote_importaTodos_quandoValidos() {
        var itens = List.of(
                item("PJE", null, "0001234-12.2025.8.06.0001", "Acao Civil"),
                item("ESAJ", null, "0002345-12.2025.8.26.0001", "Execucao Fiscal"));
        var resultado = service.processarLote(itens);
        assertThat(resultado.total()).isEqualTo(2);
        assertThat(resultado.importados() + resultado.comDivergencia()).isEqualTo(2);
    }

    @Test
    void processarLote_contadoresCorretos() {
        var itens = List.of(item("PJE", null, "0001", "Acao"));
        var resultado = service.processarLote(itens);
        assertThat(resultado.total()).isEqualTo(1);
        assertThat(resultado.detalhes()).hasSize(1);
    }

    private ProcessoExternoCargaService.CargaItem item(
            String sistema, String formato, String numero, String classe) {
        return new ProcessoExternoCargaService.CargaItem(
                sistema, formato, numero, classe, Map.of());
    }
}
