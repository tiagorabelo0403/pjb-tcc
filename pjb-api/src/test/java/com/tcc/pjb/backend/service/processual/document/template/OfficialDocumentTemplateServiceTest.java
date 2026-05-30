package com.tcc.pjb.backend.service.processual.document.template;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;
import java.time.LocalDate;
import java.time.LocalTime;
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
        var validacao = new SovereignValidationResult(
                "VALIDO", "PJB_QUALIFIED_SIGNATURE_SPINE", "MAGISTRATURA_QUALIFICADA_SOBERANA",
                true, true, true, true, false,
                "MAGISTRATURA", "ESTADUAL", "PRIMEIRO_GRAU", "COMARCA/UF",
                "session", "replay", "docHash", List.of());
        var assinatura = new QualifiedSignatureMetadata(
                "PJB-ENV-TESTE", "hashAssinatura", "hashBase", "docHash", true, "PJB-RUB-TESTE",
                LocalDate.now(), LocalTime.now(), "COMARCA/UF", "Assinante", "MAGISTRATURA",
                "MAGISTRATURA", "JUDICIARIO", "ESTADUAL", "ESTADUAL", "PRIMEIRO_GRAU",
                "ORGAO", "LOTACAO", "REGISTRO", false, "COERENCIA", "session", "replay", validacao);
        when(qualifiedDocumentSignatureEnvelopeService.signOfficialTemplate(Mockito.eq(processo), Mockito.eq(usuario), Mockito.eq(TemplateDocumentoOficial.DESPACHO), Mockito.anyString(), Mockito.anyString(), Mockito.eq(true)))
                .thenReturn(new SignedDocumentEnvelope(
                        "Despacho — 0002",
                        "CONTEUDO_ASSINADO\nRubrica eletrônica: PJB-RUB-TESTE",
                        "hash-assinado",
                        true,
                        assinatura,
                        validacao
                ));
        ObjectMapper testObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OfficialDocumentTemplateService service = new OfficialDocumentTemplateService(
                processoRepository,
                documentoRepository,
                currentUserService,
                authorizationService,
                trustChainService,
                qualifiedDocumentSignatureEnvelopeService,
                testObjectMapper
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
        assertTrue(response.assinaturaQualificada().containsKey("rubricaEletronica"));
        assertTrue(response.validacaoSoberana().containsKey("status"));
    }
}
