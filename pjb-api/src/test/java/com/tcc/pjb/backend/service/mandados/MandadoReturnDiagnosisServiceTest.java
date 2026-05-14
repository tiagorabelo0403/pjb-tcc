package com.tcc.pjb.backend.service.mandados;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MandadoReturnDiagnosisServiceTest {

    private final MandadoReturnDiagnosisService service = new MandadoReturnDiagnosisService();

    @Test
    void diagnosticar_enderecoDesatualizado_quandoMotivoContemEndereco() {
        var result = service.diagnosticar(UUID.randomUUID(), "Endereço desatualizado");
        assertThat(result.causa()).isEqualTo(MandadoReturnDiagnosisService.MandadoRetornoCausa.ENDERECO_DESATUALIZADO);
        assertThat(result.exigeNovoEndereco()).isTrue();
    }

    @Test
    void diagnosticar_parteFaleceu_quandoMotivoContemFalecimento() {
        var result = service.diagnosticar(UUID.randomUUID(), "Parte faleceu");
        assertThat(result.causa()).isEqualTo(MandadoReturnDiagnosisService.MandadoRetornoCausa.PARTE_FALECEU);
        assertThat(result.exigeNovoEndereco()).isFalse();
        assertThat(result.acaoSugerida()).contains("herdeiros");
    }

    @Test
    void diagnosticar_recusaRecepcao_quandoMotivoContemRecusa() {
        var result = service.diagnosticar(UUID.randomUUID(), "Recusou receber");
        assertThat(result.causa()).isEqualTo(MandadoReturnDiagnosisService.MandadoRetornoCausa.RECUSA_RECEPCAO);
    }

    @Test
    void diagnosticar_outra_quandoMotivoNulo() {
        var result = service.diagnosticar(UUID.randomUUID(), null);
        assertThat(result.causa()).isEqualTo(MandadoReturnDiagnosisService.MandadoRetornoCausa.OUTRA);
    }

    @Test
    void diagnosticar_estabelecimentoFechado_quandoMotivoContemFechado() {
        var result = service.diagnosticar(UUID.randomUUID(), "Estabelecimento fechado");
        assertThat(result.causa()).isEqualTo(MandadoReturnDiagnosisService.MandadoRetornoCausa.ESTABELECIMENTO_FECHADO);
        assertThat(result.acaoSugerida()).contains("CNPJ");
    }
}
