package com.tcc.pjb.backend.service.processual.document.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecursalQualifiedDocumentMaterializerServiceTest {

    @Mock
    private OfficialDocumentTemplateService officialDocumentTemplateService;

    private RecursalQualifiedDocumentMaterializerService service;

    @BeforeEach
    void setup() {
        service = new RecursalQualifiedDocumentMaterializerService(officialDocumentTemplateService);
    }

    @Test
    void deveMaterializarAcordaoComEnvelopeQualificado() {
        UUID documentoId = UUID.randomUUID();
        when(officialDocumentTemplateService.renderizar(any(OfficialDocumentTemplateRenderRequest.class))).thenReturn(
                new OfficialDocumentTemplateRenderResponse(
                        10L,
                        "0001234-56.2026.8.06.0001",
                        TemplateDocumentoOficial.ACORDAO,
                        "Acórdão — 0001234-56.2026.8.06.0001",
                        List.of("ementa", "fundamentacao", "dispositivo"),
                        List.of(),
                        "conteudo-assinado",
                        "abc123",
                        documentoId,
                        DocumentoCategoria.PUBLICO,
                        null,
                        true,
                        true,
                        List.of(),
                        Map.of("rubrica", "RELATOR", "envelopeId", "env-1"),
                        Map.of("status", "VALIDO", "documentoAssinadoHash", "hash-1")
                )
        );

        Map<String, Object> out = service.materializarAcordao(
                10L,
                "Acórdão — 0001234-56.2026.8.06.0001",
                "Ementa",
                "Fundamentação",
                "Dispositivo",
                "2ª Câmara de Direito Público",
                "SEGUNDO_GRAU",
                "ACORDAO_LAVRADO"
        );

        assertEquals("ACORDAO", out.get("templateDocumentoOficial"));
        assertEquals("conteudo-assinado", out.get("conteudoAssinado"));
        assertEquals("abc123", out.get("hashSha256"));
        assertEquals(documentoId, out.get("documentoId"));
        assertTrue(((Map<?, ?>) out.get("assinaturaQualificada")).containsKey("envelopeId"));
        assertEquals("VALIDO", ((Map<?, ?>) out.get("validacaoSoberana")).get("status"));
    }
}
