package com.tcc.pjb.backend.model.dto.profile.operational;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DelegadoInqueritoMultimidiaRequestTest {

    @Test
    void deveSanitizarColecoesENormalizarTipo() {
        DelegadoInqueritoMultimidiaRequest request = new DelegadoInqueritoMultimidiaRequest(
                " ",
                "Narrativa",
                null,
                null,
                java.util.Arrays.asList(" documento-1 ", "documento-1", null),
                List.of(" pessoa ", "pessoa"),
                List.of(" rep "),
                List.of(" anexo "),
                Boolean.TRUE,
                Boolean.TRUE
        );
        assertEquals("RELATORIO_INQUERITO", request.tipoPeca());
        assertEquals(1, request.provasDocumentais().size());
        assertEquals(1, request.documentosPessoais().size());
        assertTrue(request.prepararPacoteProtocoloResolvido());
        assertTrue(request.sigiloSensivelResolvido());
    }
}
