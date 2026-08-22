package com.tcc.pjb.backend.controller.mni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.integration.mni.application.MniDocumentoIngestaoService;
import com.tcc.pjb.backend.model.dto.mni.ConfirmarClassificacaoDocumentoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MniDocumentoClassificacaoControllerTest {

    @Test
    void listarPendentesMapeiaEntidadeParaDtoSemExporProcessoOuBytesCrus() {
        MniDocumentoIngestaoService service = mock(MniDocumentoIngestaoService.class);
        UUID id = UUID.randomUUID();
        Processo processo = Processo.builder().id(7L).build();
        DocumentoProcessual pendente = DocumentoProcessual.builder()
                .id(id)
                .processo(processo)
                .nomeOriginal("anexo_diverso.pdf")
                .sha256("abc123")
                .build();
        when(service.listarPendentesDeClassificacao()).thenReturn(List.of(pendente));

        var response = new MniDocumentoClassificacaoController(service).listarPendentes();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        var dto = response.getBody().get(0);
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.processoId()).isEqualTo(7L);
        assertThat(dto.nomeOriginal()).isEqualTo("anexo_diverso.pdf");
        assertThat(dto.sha256()).isEqualTo("abc123");
    }

    @Test
    void confirmarClassificacaoDelegaParaOServicoERetornaTipoConfirmado() {
        MniDocumentoIngestaoService service = mock(MniDocumentoIngestaoService.class);
        UUID id = UUID.randomUUID();
        DocumentoProcessual classificado = DocumentoProcessual.builder().id(id).build();
        classificado.setTipoDocumento(TipoDocumento.CERTIDAO_OBITO);
        when(service.confirmarClassificacao(id, TipoDocumento.CERTIDAO_OBITO)).thenReturn(classificado);

        var response = new MniDocumentoClassificacaoController(service)
                .confirmarClassificacao(id, new ConfirmarClassificacaoDocumentoRequest(TipoDocumento.CERTIDAO_OBITO));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().id()).isEqualTo(id);
        assertThat(response.getBody().tipoDocumento()).isEqualTo("CERTIDAO_OBITO");
    }
}
