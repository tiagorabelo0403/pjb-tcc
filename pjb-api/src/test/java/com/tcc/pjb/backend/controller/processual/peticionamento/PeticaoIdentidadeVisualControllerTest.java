package com.tcc.pjb.backend.controller.processual.peticionamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.identidade.IdentidadeVisualResponse;
import com.tcc.pjb.backend.service.processual.peticionamento.identidade.PeticaoIdentidadeVisualService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class PeticaoIdentidadeVisualControllerTest {

    private PeticaoIdentidadeVisualService service;
    private PeticaoIdentidadeVisualController controller;

    @BeforeEach
    void setUp() {
        service = mock(PeticaoIdentidadeVisualService.class);
        controller = new PeticaoIdentidadeVisualController(service);
    }

    @Test
    void obterDelegaAoServico() {
        when(service.obterMinha()).thenReturn(IdentidadeVisualResponse.vazia());
        var resp = controller.obter();
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody().temLogo()).isFalse();
    }

    @Test
    void uploadLogoRejeitaTipoNaoImagem() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.txt", "text/plain", new byte[]{1, 2, 3});
        var resp = controller.uploadLogo(file);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verify(service, never()).uploadLogo(any(), any());
    }

    @Test
    void uploadLogoVazioRetorna400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[0]);
        var resp = controller.uploadLogo(file);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(service, never()).uploadLogo(any(), any());
    }

    @Test
    void uploadLogoPngValidoDelega() throws Exception {
        when(service.uploadLogo(any(), any())).thenReturn(IdentidadeVisualResponse.vazia());
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        var resp = controller.uploadLogo(file);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(service).uploadLogo(any(), any());
    }

    @Test
    void lerLogoSemLogoRetorna404() throws Exception {
        when(service.lerLogo()).thenReturn(null);
        var resp = controller.lerLogo(new org.springframework.mock.web.MockHttpServletRequest());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
