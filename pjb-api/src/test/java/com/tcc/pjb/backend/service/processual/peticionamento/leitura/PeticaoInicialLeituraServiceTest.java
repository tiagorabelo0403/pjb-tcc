package com.tcc.pjb.backend.service.processual.peticionamento.leitura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processo.ProcessoAccessApplicationService;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextDocumentSanitizer;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextFormatCatalog;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextHtmlRenderer;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class PeticaoInicialLeituraServiceTest {

    private static final Long PROCESSO_ID = 77L;

    private LaianePeticaoInicialDraftSessionRepository repository;
    private ProcessoAccessApplicationService processoAccess;
    private RecursalEffectiveSecrecyService secrecyService;
    private PjbAuthorizationService authorizationService;
    private Processo processo;

    private PeticaoInicialLeituraService service;

    @BeforeEach
    void setUp() {
        repository = mock(LaianePeticaoInicialDraftSessionRepository.class);
        processoAccess = mock(ProcessoAccessApplicationService.class);
        secrecyService = mock(RecursalEffectiveSecrecyService.class);
        authorizationService = mock(PjbAuthorizationService.class);

        processo = mock(Processo.class);
        when(processo.getId()).thenReturn(PROCESSO_ID);
        when(processo.getNumeroProcesso()).thenReturn("0000001-11.2026.5.07.0001");
        when(processoAccess.load(PROCESSO_ID)).thenReturn(processo);

        ObjectMapper mapper = new ObjectMapper();
        RichTextDocumentSanitizer sanitizer = new RichTextDocumentSanitizer(mapper, new RichTextFormatCatalog());
        RichTextHtmlRenderer renderer = new RichTextHtmlRenderer();

        service = new PeticaoInicialLeituraService(
                repository, processoAccess, secrecyService, authorizationService, sanitizer, renderer, mapper);
    }

    private LaianePeticaoInicialDraftSession peca(String conteudoJson, String minuta) {
        LaianePeticaoInicialDraftSession p = new LaianePeticaoInicialDraftSession();
        p.setId(1L);
        p.setProcesso(processo);
        p.setTituloCaso("Ação de cobrança");
        p.setRitoSugerido("COMUM_ORDINARIO");
        p.setConteudoJson(conteudoJson);
        if (minuta != null) {
            p.setMinutaInicial(minuta);
        }
        return p;
    }

    @Test
    void renderizaConteudoJsonComoHtmlSeguroAplicandoOGateDeSigilo() {
        when(secrecyService.effectiveSecrecyForProcesso(PROCESSO_ID)).thenReturn(NivelSigilo.PUBLICO);
        String conteudoJson = """
                {"type":"doc","content":[{"type":"paragraph","content":[
                  {"type":"text","text":"forte","marks":[{"type":"bold"}]}
                ]}]}""";
        when(repository.findByProcesso_Id(PROCESSO_ID))
                .thenReturn(java.util.Optional.of(peca(conteudoJson, "")));

        PecaInicialLeituraResponse resp = service.lerPorProcesso(PROCESSO_ID);

        verify(authorizationService).requireReadProcessoAtSecrecy(processo, NivelSigilo.PUBLICO);
        assertThat(resp.processoId()).isEqualTo(PROCESSO_ID);
        assertThat(resp.origemConteudo()).isEqualTo("JSON_SANITIZADO");
        assertThat(resp.conteudoHtml()).contains("<strong>forte</strong>");
        assertThat(resp.sigiloso()).isFalse();
        assertThat(resp.rito()).isEqualTo("COMUM_ORDINARIO");
    }

    @Test
    void semJsonAMinutaLegadaEhEscapadaNuncaEmiteHtmlBruto() {
        when(secrecyService.effectiveSecrecyForProcesso(PROCESSO_ID)).thenReturn(NivelSigilo.PUBLICO);
        when(repository.findByProcesso_Id(PROCESSO_ID))
                .thenReturn(java.util.Optional.of(peca(null, "Excelentíssimo\n<script>alert(1)</script>")));

        PecaInicialLeituraResponse resp = service.lerPorProcesso(PROCESSO_ID);

        assertThat(resp.origemConteudo()).isEqualTo("MINUTA_TEXTO");
        assertThat(resp.conteudoHtml()).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
        assertThat(resp.conteudoHtml()).doesNotContain("<script>");
    }

    @Test
    void sinalizaSigilosoQuandoNivelEfetivoNaoEPublico() {
        when(secrecyService.effectiveSecrecyForProcesso(PROCESSO_ID)).thenReturn(NivelSigilo.SEGREDO_JUSTICA);
        when(repository.findByProcesso_Id(PROCESSO_ID))
                .thenReturn(java.util.Optional.of(peca(null, "corpo")));

        PecaInicialLeituraResponse resp = service.lerPorProcesso(PROCESSO_ID);

        assertThat(resp.sigiloso()).isTrue();
    }

    @Test
    void negaLeituraAntesDeBuscarAPecaQuandoOSigiloBloqueia() {
        when(secrecyService.effectiveSecrecyForProcesso(PROCESSO_ID)).thenReturn(NivelSigilo.SEGREDO_JUSTICA);
        doThrow(new AccessDeniedException("negado"))
                .when(authorizationService).requireReadProcessoAtSecrecy(any(), eq(NivelSigilo.SEGREDO_JUSTICA));

        assertThatThrownBy(() -> service.lerPorProcesso(PROCESSO_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(repository, never()).findByProcesso_Id(any());
    }

    @Test
    void pecaInexistenteResultaEmRecursoNaoEncontrado() {
        when(secrecyService.effectiveSecrecyForProcesso(PROCESSO_ID)).thenReturn(NivelSigilo.PUBLICO);
        when(repository.findByProcesso_Id(PROCESSO_ID)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.lerPorProcesso(PROCESSO_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
