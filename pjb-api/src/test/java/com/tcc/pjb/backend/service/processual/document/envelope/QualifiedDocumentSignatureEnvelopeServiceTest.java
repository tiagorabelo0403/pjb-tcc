package com.tcc.pjb.backend.service.processual.document.envelope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalSessionSecuritySignalService;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class QualifiedDocumentSignatureEnvelopeServiceTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void signFreeContent_retornaEnvelopeCanonico() {
        InstitutionalSessionSecuritySignalService securitySignalService = Mockito.mock(InstitutionalSessionSecuritySignalService.class);
        when(securitySignalService.collect(Mockito.any())).thenReturn(new InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal(
                IdentidadeJuridicaNacional.GovBrNivel.OURO,
                true,
                true,
                true,
                true,
                true,
                true,
                List.of("govbr=OURO")
        ));
        QualifiedDocumentSignatureEnvelopeService service = new QualifiedDocumentSignatureEnvelopeService(securitySignalService, new QualifiedSignatureIdentityContextService(), officeScopeProvider(null));
        Usuario usuario = new Usuario();
        usuario.setNome("Promotor");
        usuario.setCpf("12345678901");

        SignedDocumentEnvelope signed = service.signFreeContent(
                null,
                usuario,
                "Ofício",
                "Conteúdo oficial",
                "MINISTERIO_PUBLICO",
                "MINISTERIO_PUBLICO_QUALIFICADA_SOBERANA",
                true,
                List.of("LAIANE_MP")
        );

        assertEquals("Ofício", signed.tituloDocumento());
        assertNotNull(signed.assinaturaQualificada());
        assertNotNull(signed.validacaoSoberana());
        assertTrue(signed.assinaturaQualificada().rubrica());
    }

    @Test
    void signFreeContent_retornaEnvelopeComRubricaERegras() {
        InstitutionalSessionSecuritySignalService securitySignalService = Mockito.mock(InstitutionalSessionSecuritySignalService.class);
        when(securitySignalService.collect(Mockito.any())).thenReturn(new InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal(
                IdentidadeJuridicaNacional.GovBrNivel.OURO,
                true,
                true,
                true,
                true,
                true,
                true,
                List.of("govbr=OURO")
        ));
        QualifiedDocumentSignatureEnvelopeService service = new QualifiedDocumentSignatureEnvelopeService(securitySignalService, new QualifiedSignatureIdentityContextService(), officeScopeProvider(null));
        Usuario usuario = new Usuario();
        usuario.setNome("Promotor");
        usuario.setCpf("12345678901");

        SignedDocumentEnvelope signed = service.signFreeContent(
                null,
                usuario,
                "Ofício",
                "Conteúdo oficial",
                "MINISTERIO_PUBLICO",
                "MINISTERIO_PUBLICO_QUALIFICADA_SOBERANA",
                true,
                List.of("LAIANE_MP")
        );

        assertNotNull(signed.renderedContent());
        assertNotNull(signed.contentHash());
        assertNotNull(signed.assinaturaQualificada().rubricaEletronica());
        assertNotNull(signed.validacaoSoberana().regrasAplicadas());
    }

    @Test
    void signFreeContent_camposCriticosNaoNulos() {
        InstitutionalSessionSecuritySignalService securitySignalService = Mockito.mock(InstitutionalSessionSecuritySignalService.class);
        when(securitySignalService.collect(Mockito.any())).thenReturn(new InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal(
                IdentidadeJuridicaNacional.GovBrNivel.OURO,
                true,
                true,
                true,
                true,
                true,
                true,
                List.of("govbr=OURO")
        ));
        QualifiedDocumentSignatureEnvelopeService service = new QualifiedDocumentSignatureEnvelopeService(securitySignalService, new QualifiedSignatureIdentityContextService(), officeScopeProvider(null));
        Usuario usuario = new Usuario();
        usuario.setNome("Promotor");
        usuario.setCpf("12345678901");

        SignedDocumentEnvelope signed = service.signFreeContent(
                null,
                usuario,
                "Ofício",
                "Conteúdo oficial",
                "MINISTERIO_PUBLICO",
                "MINISTERIO_PUBLICO_QUALIFICADA_SOBERANA",
                true,
                List.of("LAIANE_MP")
        );

        assertNotNull(signed.contentHash());
        assertNotNull(signed.renderedContent());
        assertNotNull(signed.assinaturaQualificada());
        assertNotNull(signed.validacaoSoberana());
        assertNotNull(signed.assinaturaQualificada().envelopeId());
        assertNotNull(signed.assinaturaQualificada().documentoAssinadoHash());
        assertNotNull(signed.validacaoSoberana().documentoAssinadoHash());
    }

    @Test
    void shouldMaterializeQualifiedSignatureEnvelopeWithRubricDateTimeAndPlace() {
        InstitutionalSessionSecuritySignalService securitySignalService = Mockito.mock(InstitutionalSessionSecuritySignalService.class);
        when(securitySignalService.collect(Mockito.any())).thenReturn(new InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal(
                IdentidadeJuridicaNacional.GovBrNivel.OURO,
                true,
                true,
                true,
                true,
                true,
                true,
                List.of("govbr=OURO", "mfa_ativo", "dispositivo_homologado")
        ));
        QualifiedDocumentSignatureEnvelopeService service = new QualifiedDocumentSignatureEnvelopeService(securitySignalService, new QualifiedSignatureIdentityContextService(), officeScopeProvider(null));
        Processo processo = new Processo();
        processo.setNumeroProcesso("0001234-56.2026.8.06.0001");
        processo.setComarca("Morada Nova");
        processo.setUf("CE");
        Usuario usuario = new Usuario();
        usuario.setNome("João da Silva");
        usuario.setCpf("12345678901");
        var signed = service.signFreeContent(
                processo,
                usuario,
                "Mandado de citação",
                "Conteúdo oficial do mandado",
                "OFICIAL_JUSTICA",
                "OFICIAL_JUSTICA_QUALIFICADA_SOBERANA",
                true,
                List.of("mandado", "citacao")
        );
        assertTrue(signed.renderedContent().contains("ASSINATURA QUALIFICADA PJB"));
        assertTrue(signed.renderedContent().contains("Rubrica eletrônica:"));
        assertTrue(signed.renderedContent().contains("Local: Morada Nova/CE"));
        assertNotNull(signed.assinaturaQualificada().rubricaEletronica());
        assertNotNull(signed.assinaturaQualificada().data());
        assertNotNull(signed.assinaturaQualificada().hora());
        assertEquals("VALIDO", signed.validacaoSoberana().status());
        assertTrue(signed.validacaoSoberana().assinaturaCompletaMaterializada());
    }

    @Test
    void shouldRecognizeElectoralJudgeAndBindEntryCertificateMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.20.30.40");
        request.addHeader("X-PJB-ENTRY-CERT-SUBJECT", "CN=Maria de Souza,OU=Juiz Eleitoral,O=TRE-CE,SERIALNUMBER=ICP-1234567890,C=BR");
        request.addHeader("X-PJB-Client-Cert-Serial", "00A1B2C3");
        request.addHeader("X-PJB-Client-Cert-Fingerprint", "AA:BB:CC:DD:EE:FF");
        request.addHeader("X-PJB-ENTRY-CERT-TRIBUNAL", "TRE-CE");
        request.addHeader("X-PJB-ENTRY-CERT-ZONA-ELEITORAL", "87ª Zona Eleitoral de Fortaleza");
        request.addHeader("X-PJB-ENTRY-CERT-UNIT-CODE", "TRE-CE-ZE-0087");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        InstitutionalSessionSecuritySignalService securitySignalService = Mockito.mock(InstitutionalSessionSecuritySignalService.class);
        when(securitySignalService.collect(Mockito.any())).thenReturn(new InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal(
                IdentidadeJuridicaNacional.GovBrNivel.OURO,
                true,
                true,
                true,
                true,
                true,
                true,
                List.of("govbr=OURO", "certificado_detectado")
        ));
        QualifiedDocumentSignatureEnvelopeService service = new QualifiedDocumentSignatureEnvelopeService(securitySignalService, new QualifiedSignatureIdentityContextService(), officeScopeProvider(null));
        Processo processo = new Processo();
        processo.setTribunal("TRE-CE");
        processo.setComarca("Fortaleza");
        processo.setUf("CE");
        processo.setTipoJustica(TipoJustica.ELEITORAL);
        Usuario usuario = new Usuario();
        usuario.setNome("Maria de Souza");
        usuario.setCpf("12345678901");
        usuario.setTipoUsuario(TipoUsuario.JUIZ_ELEITORAL);
        usuario.setComarca("Fortaleza");
        usuario.setUf("CE");

        var signed = service.signOfficialTemplate(
                processo,
                usuario,
                com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial.DECISAO,
                "Decisão",
                "Conteúdo oficial",
                true
        );

        assertEquals("JUIZ_ELEITORAL_PRIMEIRO_GRAU", signed.assinaturaQualificada().papelAssinanteDetalhado());
        assertEquals("ELEITORAL", signed.assinaturaQualificada().ramoJustica());
        assertEquals("PRIMEIRO_GRAU", signed.assinaturaQualificada().instancia());
        assertTrue(signed.renderedContent().contains("Certificado de entrada: FP=AABBCCDDEEFF"));
        assertTrue(signed.renderedContent().contains("Lotação: TRE-CE | 87ª Zona Eleitoral de Fortaleza | COD=TRE-CE-ZE-0087 | Fortaleza/CE"));
        assertEquals("TRE-CE | 87ª Zona Eleitoral de Fortaleza | COD=TRE-CE-ZE-0087 | Fortaleza/CE", signed.assinaturaQualificada().lotacaoAssinante());
        assertTrue(signed.validacaoSoberana().certificadoEntradaVinculado());
        assertTrue(signed.assinaturaQualificada().rubrica());
    }

    @Test
    void shouldRecognizeFederalJudgeLotacaoByCertificateAndUnitCode() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.1.20");
        request.addHeader("X-PJB-ENTRY-CERT-SUBJECT", "CN=Ricardo Lima,OU=2ª Vara Federal de Sobral,O=TRF5,SERIALNUMBER=ICP-009988,C=BR");
        request.addHeader("X-PJB-ENTRY-CERT-TRIBUNAL", "TRF5");
        request.addHeader("X-PJB-ENTRY-CERT-VARA", "2ª Vara Federal de Sobral");
        request.addHeader("X-PJB-ENTRY-CERT-UNIT-CODE", "JFCE-SOBRAL-2VF");
        request.addHeader("X-PJB-ENTRY-CERT-NAME", "Ricardo Lima");
        request.addHeader("X-PJB-ENTRY-CERT-CPF", "12345678901");
        request.addHeader("X-PJB-ENTRY-CERT-SECRETARIA-RECURSAL", "Secretaria da 2ª Vara Federal de Sobral");
        request.addHeader("X-PJB-ENTRY-CERT-SECRETARIA-EMBARGOS", "Mesa de Embargos da 2ª Vara Federal de Sobral");
        request.addHeader("X-PJB-Client-Cert-Fingerprint", "11:22:33:44");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        InstitutionalSessionSecuritySignalService securitySignalService = Mockito.mock(InstitutionalSessionSecuritySignalService.class);
        when(securitySignalService.collect(Mockito.any())).thenReturn(new InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal(
                IdentidadeJuridicaNacional.GovBrNivel.OURO,
                true,
                true,
                true,
                true,
                true,
                true,
                List.of("govbr=OURO", "certificado_detectado")
        ));
        QualifiedDocumentSignatureEnvelopeService service = new QualifiedDocumentSignatureEnvelopeService(securitySignalService, new QualifiedSignatureIdentityContextService(), officeScopeProvider(null));
        Processo processo = new Processo();
        processo.setTribunal("TRF5");
        processo.setComarca("Sobral");
        processo.setUf("CE");
        processo.setTipoJustica(TipoJustica.FEDERAL);
        processo.setVara("2ª Vara Federal de Sobral");
        processo.setUnidadeJudiciariaCodigo("JFCE-SOBRAL-2VF");
        Usuario usuario = new Usuario();
        usuario.setNome("Ricardo Lima");
        usuario.setCpf("12345678901");
        usuario.setTipoUsuario(TipoUsuario.JUIZ_FEDERAL);
        usuario.setComarca("Sobral");
        usuario.setUf("CE");

        var signed = service.signOfficialTemplate(
                processo,
                usuario,
                com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial.DECISAO,
                "Decisão",
                "Conteúdo oficial",
                true
        );

        assertEquals("JUIZ_FEDERAL_PRIMEIRO_GRAU", signed.assinaturaQualificada().papelAssinanteDetalhado());
        assertEquals("FEDERAL", signed.assinaturaQualificada().ramoJustica());
        assertEquals("TRF5 | 2ª Vara Federal de Sobral | COD=JFCE-SOBRAL-2VF | Sobral/CE", signed.assinaturaQualificada().lotacaoAssinante());
        assertEquals("USUARIO_CERTIFICADO_MEDIA", signed.assinaturaQualificada().coerenciaCertificadoUsuarioResumo());
        assertTrue(signed.renderedContent().contains("Secretaria recursal: Secretaria da 2ª Vara Federal de Sobral"));
        assertTrue(signed.renderedContent().contains("Secretaria de embargos: Mesa de Embargos da 2ª Vara Federal de Sobral"));
        assertTrue(signed.renderedContent().contains("COD=JFCE-SOBRAL-2VF"));
        assertNotNull(signed.assinaturaQualificada().coerenciaCertificadoUsuarioResumo());
    }
    @Test
    void shouldClassifySecondInstanceElectoralSecretariatAndExposePjbPanel() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.20.10");
        request.addHeader("X-PJB-ENTRY-CERT-SUBJECT", "CN=Clara Nunes,OU=Secretaria Judiciária,O=TRE-CE,TITLE=Servidora Judiciária,C=BR");
        request.addHeader("X-PJB-ENTRY-CERT-TRIBUNAL", "TRE-CE");
        request.addHeader("X-PJB-ENTRY-CERT-INSTANCE", "SEGUNDO_GRAU");
        request.addHeader("X-PJB-ENTRY-CERT-SECRETARIA-ELEITORAL", "Secretaria Judiciária Eleitoral - TRE-CE");
        request.addHeader("X-PJB-ENTRY-CERT-NAME", "Clara Nunes");
        request.addHeader("X-PJB-ENTRY-CERT-CPF", "12345678901");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        InstitutionalSessionSecuritySignalService securitySignalService = Mockito.mock(InstitutionalSessionSecuritySignalService.class);
        when(securitySignalService.collect(Mockito.any())).thenReturn(new InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal(
                IdentidadeJuridicaNacional.GovBrNivel.OURO,
                true,
                true,
                true,
                true,
                true,
                true,
                List.of("govbr=OURO", "certificado_detectado")
        ));
        QualifiedDocumentSignatureEnvelopeService service = new QualifiedDocumentSignatureEnvelopeService(securitySignalService, new QualifiedSignatureIdentityContextService(), officeScopeProvider(null));
        Processo processo = new Processo();
        processo.setTribunal("TRE-CE");
        processo.setComarca("Fortaleza");
        processo.setUf("CE");
        processo.setTipoJustica(TipoJustica.ELEITORAL);
        Usuario usuario = new Usuario();
        usuario.setNome("Clara Nunes");
        usuario.setCpf("12345678901");
        usuario.setTipoUsuario(TipoUsuario.SERVIDOR_FORUM);
        usuario.setComarca("Fortaleza");
        usuario.setUf("CE");

        var signed = service.signOfficialTemplate(
                processo,
                usuario,
                com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial.CERTIDAO,
                "Certidão",
                "Conteúdo oficial",
                true
        );

        assertEquals("SECRETARIA_SEGUNDA_INSTANCIA_ELEITORAL", signed.assinaturaQualificada().papelAssinanteDetalhado());
        assertTrue(signed.renderedContent().contains("Namespace PJB: PJB_SEGUNDA_INSTANCIA"));
        assertTrue(signed.renderedContent().contains("Painel PJB: PJB Segunda Instância | ELEITORAL"));
        assertTrue(signed.renderedContent().contains("Secretaria especializada: Secretaria Judiciária Eleitoral - TRE-CE"));
    }

    @Test
    void shouldRequireWorkspaceAccessWhenSigningProcessContentInOfficeContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.10.10.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        InstitutionalSessionSecuritySignalService securitySignalService = Mockito.mock(InstitutionalSessionSecuritySignalService.class);
        when(securitySignalService.collect(Mockito.any())).thenReturn(new InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal(
                IdentidadeJuridicaNacional.GovBrNivel.OURO,
                true,
                true,
                true,
                true,
                true,
                true,
                List.of("govbr=OURO", "mfa_ativo")
        ));
        OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService = Mockito.mock(OfficeProcessWorkspaceScopeService.class);
        when(officeProcessWorkspaceScopeService.supportsCurrentUser()).thenReturn(true);

        QualifiedDocumentSignatureEnvelopeService service = new QualifiedDocumentSignatureEnvelopeService(
                securitySignalService,
                new QualifiedSignatureIdentityContextService(),
                officeScopeProvider(officeProcessWorkspaceScopeService)
        );

        Processo processo = new Processo();
        processo.setId(55L);
        processo.setNumeroProcesso("0001234-56.2026.8.06.0001");
        processo.setComarca("Morada Nova");
        processo.setUf("CE");
        Usuario usuario = new Usuario();
        usuario.setNome("João da Silva");
        usuario.setCpf("12345678901");

        service.signFreeContent(
                processo,
                usuario,
                "Petição",
                "Conteúdo",
                "ADVOGADO",
                "ADVOGADO_QUALIFICADA",
                true,
                List.of("peticao")
        );

        verify(officeProcessWorkspaceScopeService).requireAccess(Mockito.eq(55L), Mockito.any(), Mockito.any());
    }

    private static ObjectProvider<OfficeProcessWorkspaceScopeService> officeScopeProvider(OfficeProcessWorkspaceScopeService service) {
        if (service == null) {
            return new StaticListableBeanFactory().getBeanProvider(OfficeProcessWorkspaceScopeService.class);
        }
        return new StaticListableBeanFactory(java.util.Map.of("officeProcessWorkspaceScopeService", service))
                .getBeanProvider(OfficeProcessWorkspaceScopeService.class);
    }
}
