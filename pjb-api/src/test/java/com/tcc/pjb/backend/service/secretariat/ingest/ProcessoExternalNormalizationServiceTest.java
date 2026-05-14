package com.tcc.pjb.backend.service.secretariat.ingest;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessoExternalNormalizationServiceTest {

    private final ProcessoExternalNormalizationService service = new ProcessoExternalNormalizationService();

    @Test
    void normalizar_snapshotCompleto_quandoInputValido() {
        var input = new ProcessoExternalNormalizationService.NormalizacaoInput(
                "12345678920250100001", SistemaProcessualOrigem.PJE,
                "Acao de Alimentos", Map.of("autor", "Fulano"));
        var snapshot = service.normalizar(input);
        assertThat(snapshot.processoId()).isNotNull();
        assertThat(snapshot.normalizacaoConcluida()).isTrue();
        assertThat(snapshot.origem()).isEqualTo(SistemaProcessualOrigem.PJE);
    }

    @Test
    void normalizar_inferePenal_quandoClasseContemPenal() {
        var input = new ProcessoExternalNormalizationService.NormalizacaoInput(
                "00001", SistemaProcessualOrigem.EPROC,
                "Acao Penal", Map.of());
        var snapshot = service.normalizar(input);
        assertThat(snapshot.ritoDetectado().name()).contains("PENAL");
    }

    @Test
    void normalizar_infereJuizado_quandoClasseContemJuizado() {
        var input = new ProcessoExternalNormalizationService.NormalizacaoInput(
                "00002", SistemaProcessualOrigem.ESAJ,
                "Juizado Especial", Map.of());
        var snapshot = service.normalizar(input);
        assertThat(snapshot.ritoDetectado().name()).contains("JUIZADO");
    }

    @Test
    void normalizar_camposNormalizadosContemClasseENpu() {
        var input = new ProcessoExternalNormalizationService.NormalizacaoInput(
                "10020002120250000100", SistemaProcessualOrigem.PJE,
                "Execucao Fiscal", Map.of());
        var snapshot = service.normalizar(input);
        assertThat(snapshot.camposNormalizados()).containsKey("classeNormalizada");
        assertThat(snapshot.camposNormalizados()).containsKey("numeroNacionalUnificado");
    }
}
