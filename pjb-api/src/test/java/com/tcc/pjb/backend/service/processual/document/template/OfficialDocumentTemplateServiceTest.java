package com.tcc.pjb.backend.service.processual.document.template;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.governance.DocumentTrustChainService;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OfficialDocumentTemplateServiceTest {

    @Test
    void shouldRenderWithoutPersistingWhenRequested() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        DocumentoProcessualRepository documentoRepository = Mockito.mock(DocumentoProcessualRepository.class);
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        DocumentTrustChainService trustChainService = Mockito.mock(DocumentTrustChainService.class);
        QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);
        Processo processo = new Processo();
        processo.setId(2L);
        processo.setNumeroProcesso("0002");
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(processoRepository.findById(2L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(qualifiedDocumentSignatureEnvelopeService.signOfficialTemplate(Mockito.eq(processo), Mockito.eq(usuario), Mockito.eq(TemplateDocumentoOficial.DESPACHO), Mockito.anyString(), Mockito.anyString(), Mockito.eq(true)))
                .thenReturn(new QualifiedDocumentSignatureEnvelopeService.SignedContent(
                        "CONTEUDO_ASSINADO\nRubrica eletrônica: PJB-RUB-TESTE",
                        "hash-assinado",
                        Map.of("rubrica", "PJB-RUB-TESTE", "governanceTags", List.of("assinatura_qualificada_completa")),
                        Map.of("status", "VALIDO")
                ));
        OfficialDocumentTemplateService service = new OfficialDocumentTemplateService(
                processoRepository,
                documentoRepository,
                currentUserService,
                authorizationService,
                trustChainService,
                qualifiedDocumentSignatureEnvelopeService
        );
        var response = service.renderizar(new OfficialDocumentTemplateRenderRequest(
                2L,
                TemplateDocumentoOficial.DESPACHO,
                null,
                Map.of("fundamentacao", "fund", "determinacao", "det"),
                false,
                false
        ));
        assertTrue(response.variaveisAusentes().isEmpty());
        assertFalse(response.conteudoRenderizado().isBlank());
        assertTrue(response.conteudoRenderizado().contains("Rubrica eletrônica"));
        assertTrue(response.assinaturaQualificada().containsKey("rubrica"));
        assertTrue(response.validacaoSoberana().containsKey("status"));
    }
}
