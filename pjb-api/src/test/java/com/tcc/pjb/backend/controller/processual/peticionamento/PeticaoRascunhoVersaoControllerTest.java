package com.tcc.pjb.backend.controller.processual.peticionamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.AutosaveRascunhoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.DraftVersaoPreviewResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.DraftVersaoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.RascunhoConteudoResponse;
import com.tcc.pjb.backend.service.processual.peticionamento.rascunho.PeticaoDraftVersionamentoService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeticaoRascunhoVersaoControllerTest {

    private PeticaoDraftVersionamentoService service;
    private PeticaoRascunhoVersaoController controller;

    @BeforeEach
    void setUp() {
        service = mock(PeticaoDraftVersionamentoService.class);
        controller = new PeticaoRascunhoVersaoController(service);
    }

    @Test
    void autosalvarDelega() {
        AutosaveRascunhoRequest req = new AutosaveRascunhoRequest("Caso", null, "<p>x</p>", null, null, null, null);
        RascunhoConteudoResponse resp = new RascunhoConteudoResponse(5L, "RASCUNHO", "Caso", null, "<p>x</p>", "h", 2, true, Instant.now());
        when(service.autosalvar(5L, req)).thenReturn(resp);

        var out = controller.autosalvar(5L, req);

        assertThat(out.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(out.getBody().versaoAtual()).isEqualTo(2);
        verify(service).autosalvar(5L, req);
    }

    @Test
    void listarVersoesDelega() {
        when(service.listarVersoes(5L)).thenReturn(List.of(
                new DraftVersaoResponse(2, "AUTOSAVE", "Caso", "h2", 120, Instant.now())));
        var out = controller.versoes(5L);
        assertThat(out.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(out.getBody()).hasSize(1);
    }

    @Test
    void previsualizarVersaoDelega() {
        DraftVersaoPreviewResponse preview = new DraftVersaoPreviewResponse(
                5L, 2, "AUTOSAVE", "Caso", "<p><strong>forte</strong></p>", "JSON_SANITIZADO", "h2", Instant.now());
        when(service.previsualizarVersao(5L, 2)).thenReturn(preview);

        var out = controller.previsualizarVersao(5L, 2);

        assertThat(out.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(out.getBody().conteudoHtml()).isEqualTo("<p><strong>forte</strong></p>");
        verify(service).previsualizarVersao(5L, 2);
    }

    @Test
    void restaurarDelega() {
        RascunhoConteudoResponse resp = new RascunhoConteudoResponse(5L, "RASCUNHO", "Caso", null, "<p>v2</p>", "h", 6, true, Instant.now());
        when(service.restaurar(5L, 2)).thenReturn(resp);
        var out = controller.restaurar(5L, 2);
        assertThat(out.getStatusCode().is2xxSuccessful()).isTrue();
        verify(service).restaurar(5L, 2);
    }
}
