package com.tcc.pjb.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.configs.EquipeSwitchInterceptor;
import com.tcc.pjb.backend.configs.SecurityConfig;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.document.DocumentContentService;
import com.tcc.pjb.backend.service.document.DocumentoPdfDownloadService;
import com.tcc.pjb.backend.service.processo.ProcessoAccessApplicationService;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rede de segurança do download de PDF. Os seis casos foram escritos contra o controller antigo, que
 * fazia tudo, e seguem valendo sem uma alteração depois de a lógica migrar para
 * {@link DocumentoPdfDownloadService} — que é a prova de que a migração preservou o comportamento. Por
 * isso o serviço real é importado e só os colaboradores dele são mockados. O endpoint
 * decide acesso a documento sob segredo de justiça: autoriza duas vezes — no processo e no documento —
 * sempre contra o sigilo <b>efetivo</b>, que é calculado e pode ser mais restritivo que o gravado.
 * São essas garantias que a migração não pode afrouxar.
 */
@WebMvcTest(
        controllers = DocumentoController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, EquipeSwitchInterceptor.class}))
@Import({WebMvcTestSecurityConfig.class, DocumentoPdfDownloadService.class})
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class DocumentoControllerTest {

    private static final UUID DOCUMENTO_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String PDF = "/api/v1/documentos/" + DOCUMENTO_ID + "/pdf";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentoProcessualRepository documentoRepository;

    @MockitoBean
    private ProcessoAccessApplicationService processoAccessApplicationService;

    @MockitoBean
    private PjbAuthorizationService authorizationService;

    @MockitoBean
    private DocumentContentService contentService;

    @MockitoBean
    private RecursalEffectiveSecrecyService secrecyService;

    private Processo processo(Long id) {
        Processo processo = new Processo();
        processo.setId(id);
        return processo;
    }

    private DocumentoProcessual documentoDoProcesso(Processo processo) {
        DocumentoProcessual documento = mock(DocumentoProcessual.class);
        when(documento.getProcesso()).thenReturn(processo);
        return documento;
    }

    private DocumentoProcessual cenarioAutorizado() {
        Processo processo = processo(7L);
        DocumentoProcessual documento = documentoDoProcesso(processo);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(processoAccessApplicationService.load(7L)).thenReturn(processo);
        when(secrecyService.effectiveSecrecyForProcesso(7L)).thenReturn(NivelSigilo.SEGREDO_JUSTICA);
        when(contentService.resolvePdf(documento)).thenReturn(
                new DocumentContentService.ResolvedDocumentContent(
                        new ByteArrayResource("conteudo".getBytes()), 8L, "application/pdf", true));
        return documento;
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void documentoInexistenteRecebeNotFoundESemAvaliarAutorizacao() throws Exception {
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(PDF)).andExpect(status().isNotFound());

        verify(authorizationService, never()).requireReadProcessoAtSecrecy(any(), any());
        verify(contentService, never()).resolvePdf(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void documentoSemProcessoVinculadoRecebeNotFound() throws Exception {
        // Montado ANTES do when() externo: helper que faz stub dentro de thenReturn deixa o Mockito
        // com um when() em aberto e estoura UnfinishedStubbing. Terceira vez que caio nisto.
        DocumentoProcessual semProcesso = documentoDoProcesso(null);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(semProcesso));

        mockMvc.perform(get(PDF)).andExpect(status().isNotFound());

        verify(contentService, never()).resolvePdf(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void autorizacaoEAvaliadaContraOSigiloEfetivoNoProcessoENoDocumento() throws Exception {
        DocumentoProcessual documento = cenarioAutorizado();

        mockMvc.perform(get(PDF)).andExpect(status().isOk());

        verify(authorizationService)
                .requireReadProcessoAtSecrecy(any(Processo.class), eq(NivelSigilo.SEGREDO_JUSTICA));
        verify(authorizationService)
                .requireReadDocumentoAtSecrecy(any(Processo.class), eq(documento), eq(NivelSigilo.SEGREDO_JUSTICA));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void negativaNoProcessoImpedeQueOConteudoSejaResolvido() throws Exception {
        cenarioAutorizado();
        doThrow(new AccessDeniedPjbException("sem vinculo com o processo"))
                .when(authorizationService).requireReadProcessoAtSecrecy(any(), any());

        mockMvc.perform(get(PDF)).andExpect(status().isForbidden());

        verify(contentService, never()).resolvePdf(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void negativaNoDocumentoImpedeQueOConteudoSejaResolvido() throws Exception {
        cenarioAutorizado();
        doThrow(new AccessDeniedPjbException("documento fora do escopo concedido"))
                .when(authorizationService).requireReadDocumentoAtSecrecy(any(), any(), any());

        mockMvc.perform(get(PDF)).andExpect(status().isForbidden());

        verify(contentService, never()).resolvePdf(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void pdfAutorizadoSaiComOsCabecalhosQueImpedemCacheEIndexacao() throws Exception {
        cenarioAutorizado();

        mockMvc.perform(get(PDF))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow, noarchive"))
                // O controller pede "none", mas o ResourceHttpMessageConverter do Spring sobrescreve com
                // "bytes" ao escrever um Resource. O teste afirma o que a resposta REALMENTE leva, e a
                // divergencia entre intencao e efeito fica registrada no DEBT_LOG.
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"documento_" + DOCUMENTO_ID + ".pdf\""));
    }
}
