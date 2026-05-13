package com.tcc.pjb.backend.service.oficial_justica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OficialJusticaCommunicationFormalModelServiceTest {

    @Test
    void buildProfileIdentifiesFederalPersonalCitationAndFormalModel() {
        OfficialDocumentTemplateService templateService = Mockito.mock(OfficialDocumentTemplateService.class);
        OficialJusticaCommunicationFormalModelService service = new OficialJusticaCommunicationFormalModelService(
                templateService,
                new OficialJusticaContextEnvelopeService()
        );
        Processo processo = processoFederal();
        WorkItem item = WorkItem.builder()
                .id(77L)
                .processo(processo)
                .titulo("Mandado de citação pessoal")
                .descricao("Citar o réu pessoalmente")
                .queueCode("TRF5:MANDADO")
                .inboxKey("OFICIAL:TRF5")
                .build();
        Usuario oficial = oficialFederal();

        Map<String, Object> profile = service.buildProfile(processo, item, oficial);

        assertEquals("FEDERAL", profile.get("justicaAxis"));
        assertEquals("CITACAO_PESSOAL", profile.get("naturezaComunicacao"));
        assertEquals(Boolean.TRUE, profile.get("servicoPessoalExigido"));
        assertEquals("OFICIAL_JUSTICA_FEDERAL", profile.get("officialLaneCode"));
        assertNotNull(profile.get("manualActions"));
        assertNotNull(profile.get("automaticActions"));
    }

    @Test
    void formalizeOutcomeGeneratesMandadoAndCompanionCertificateForPositivePersonalCitation() {
        OfficialDocumentTemplateService templateService = Mockito.mock(OfficialDocumentTemplateService.class);
        when(templateService.renderizar(any(OfficialDocumentTemplateRenderRequest.class))).thenAnswer(invocation -> {
            OfficialDocumentTemplateRenderRequest request = invocation.getArgument(0);
            return new OfficialDocumentTemplateRenderResponse(
                    request.processoId(),
                    "0001234-55.2026.4.05.8100",
                    request.template(),
                    request.tituloCustomizado(),
                    request.template().variaveisObrigatorias(),
                    List.of(),
                    "conteudo",
                    "hash",
                    10L,
                    DocumentoCategoria.PUBLICO,
                    NivelSigilo.PUBLICO,
                    true,
                    true,
                    List.of(),
                    Map.of("status", "OK"),
                    Map.of("integridade", "OK")
            );
        });
        OficialJusticaCommunicationFormalModelService service = new OficialJusticaCommunicationFormalModelService(
                templateService,
                new OficialJusticaContextEnvelopeService()
        );
        Processo processo = processoFederal();
        WorkItem item = WorkItem.builder()
                .id(88L)
                .processo(processo)
                .titulo("Mandado de citação pessoal")
                .descricao("Citar o réu pessoalmente")
                .queueCode("TRF5:MANDADO")
                .inboxKey("OFICIAL:TRF5")
                .build();
        Usuario oficial = oficialFederal();

        Map<String, Object> result = service.formalizeOutcome(
                processo,
                item,
                oficial,
                Map.of(
                        "operationMode", "AUTOMATICO",
                        "resultadoComunicacao", "CITADO_PESSOALMENTE",
                        "destinatarioNome", "Empresa Ré"
                ),
                false
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> generated = (List<Map<String, Object>>) result.get("generatedDocuments");
        @SuppressWarnings("unchecked")
        Map<String, Object> primary = (Map<String, Object>) result.get("primaryDocument");
        assertEquals(2, generated.size());
        assertEquals(TemplateDocumentoOficial.MANDADO.name(), primary.get("template"));
        verify(templateService, times(2)).renderizar(any(OfficialDocumentTemplateRenderRequest.class));
    }

    @Test
    void formalizeOutcomeReturnsAlertWhenProcessIsUnavailable() {
        OfficialDocumentTemplateService templateService = Mockito.mock(OfficialDocumentTemplateService.class);
        OficialJusticaCommunicationFormalModelService service = new OficialJusticaCommunicationFormalModelService(
                templateService,
                new OficialJusticaContextEnvelopeService()
        );

        Map<String, Object> result = service.formalizeOutcome(null, null, null, Map.of(), true);

        @SuppressWarnings("unchecked")
        List<String> alerts = (List<String>) result.get("alerts");
        assertTrue(alerts.contains("processo_nao_identificado_para_materializacao_documental"));
    }

    private Processo processoFederal() {
        Processo processo = new Processo();
        processo.setId(10L);
        processo.setNumeroProcesso("0001234-55.2026.4.05.8100");
        processo.setTribunal("TRF5");
        processo.setTribunalCodigoRoteado("TRF5");
        processo.setVara("1ª Vara Federal");
        processo.setComarca("Fortaleza");
        processo.setUf("CE");
        processo.setTipoJustica(TipoJustica.FEDERAL);
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setRito(RitoProcessual.JUIZADO_ESPECIAL_FEDERAL);
        processo.setClasseProcessual("Ação de cobrança");
        processo.setAssunto("Citação pessoal do réu");
        processo.setParteAutoraNome("Autor Federal");
        processo.setParteReuNome("Empresa Ré");
        return processo;
    }

    private Usuario oficialFederal() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setNome("Oficial Federal");
        usuario.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        usuario.setPerfil("TRF5");
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        return usuario;
    }
}
