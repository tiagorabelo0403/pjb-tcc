package com.tcc.pjb.backend.service.certidao;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CertidaoAutoPreparationServiceTest {

    private final CertidaoPendenciaDetector detector = new CertidaoPendenciaDetector();
    private final CertidaoAutoPreparationService service =
            new CertidaoAutoPreparationService(detector);

    @Test
    void preparar_pronta_quandoApto() {
        var input = new CertidaoAutoPreparationService.PreparacaoInput(
                UUID.randomUUID(), "TRANSITO_EM_JULGADO", true,
                List.of("processo ativo", "sem recurso"));
        var certidao = service.preparar(input);
        assertThat(certidao.pronta()).isTrue();
        assertThat(certidao.conteudoMinuta()).isNotNull();
        assertThat(certidao.aviso()).contains("revisar e assinar");
    }

    @Test
    void preparar_naoPreparada_quandoNaoApto() {
        var input = new CertidaoAutoPreparationService.PreparacaoInput(
                UUID.randomUUID(), "OBJETO", false, List.of());
        var certidao = service.preparar(input);
        assertThat(certidao.pronta()).isFalse();
        assertThat(certidao.conteudoMinuta()).isNull();
        assertThat(certidao.aviso()).contains("verificar pendências");
    }

    @Test
    void preparar_conteudoContemTipoEProcesso() {
        var processoId = UUID.randomUUID();
        var input = new CertidaoAutoPreparationService.PreparacaoInput(
                processoId, "OBJETO_E_PE", true, List.of("dado-1", "dado-2"));
        var certidao = service.preparar(input);
        assertThat(certidao.conteudoMinuta()).contains("OBJETO_E_PE");
        assertThat(certidao.conteudoMinuta()).contains(processoId.toString());
    }
}
