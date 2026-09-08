package com.tcc.pjb.backend.service.advogado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.PeticaoInicialPdfExportService;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextDocumentSanitizer;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextPlainTextExtractor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LaianePeticaoInicialPecaMaterializationServiceTest {

    private ObjectMapper objectMapper;
    private RichTextDocumentSanitizer sanitizer;
    private RichTextPlainTextExtractor extractor;
    private PeticaoInicialPdfExportService pdfExportService;
    private DocumentoProcessualRepository documentoProcessualRepository;
    private DocumentoPaginaRepository documentoPaginaRepository;
    private LaianePeticaoInicialPecaMaterializationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sanitizer = mock(RichTextDocumentSanitizer.class);
        extractor = mock(RichTextPlainTextExtractor.class);
        pdfExportService = mock(PeticaoInicialPdfExportService.class);
        documentoProcessualRepository = mock(DocumentoProcessualRepository.class);
        documentoPaginaRepository = mock(DocumentoPaginaRepository.class);
        service = new LaianePeticaoInicialPecaMaterializationService(
                objectMapper, sanitizer, extractor, pdfExportService,
                documentoProcessualRepository, documentoPaginaRepository);
    }

    @Test
    void materializarUsaConteudoJsonQuandoPresenteESalvaDocumentoEPagina() {
        Processo processo = new Processo();
        processo.setNumeroProcesso("0001234-56.2026.8.06.0001");
        processo.setSigilo(NivelSigilo.PUBLICO);
        Usuario usuario = new Usuario();
        usuario.setId(7L);

        LaianePeticaoInicialDraftSession draft = new LaianePeticaoInicialDraftSession();
        draft.setConteudoJson("{\"tipo\":\"doc\"}");
        draft.setMinutaInicial("minuta legada, nunca usada aqui");

        when(sanitizer.sanitize(any())).thenReturn(new RichTextDocumentSanitizer.SanitizeResult(new TextNode("doc"), List.of(), false));
        when(extractor.extract(any())).thenReturn(List.of("Excelentíssimo Juiz", "Fundamentos do pedido"));
        when(pdfExportService.export(any(), any(), any(), anyList()))
                .thenReturn(new PeticaoInicialPdfExportService.PeticaoInicialPdfArtifact(
                        new byte[]{1, 2, 3}, "sha256hex", "sha384hex", 2));
        when(documentoProcessualRepository.save(any())).thenAnswer(inv -> {
            DocumentoProcessual d = inv.getArgument(0);
            d.setId(java.util.UUID.randomUUID());
            return d;
        });

        service.materializar(processo, usuario, draft);

        ArgumentCaptor<DocumentoProcessual> docCaptor = ArgumentCaptor.forClass(DocumentoProcessual.class);
        verify(documentoProcessualRepository).save(docCaptor.capture());
        DocumentoProcessual salvo = docCaptor.getValue();
        assertThat(salvo.getTitulo()).isEqualTo("Petição Inicial");
        assertThat(salvo.getNomeOriginal()).contains("0001234-56-2026-8-06-0001");
        assertThat(salvo.getSha256()).isEqualTo("sha256hex");
        assertThat(salvo.getQuantidadePaginas()).isEqualTo(2);

        ArgumentCaptor<DocumentoPagina> paginaCaptor = ArgumentCaptor.forClass(DocumentoPagina.class);
        verify(documentoPaginaRepository).save(paginaCaptor.capture());
        assertThat(paginaCaptor.getValue().getTextoExtraido()).contains("Excelentíssimo Juiz");
    }

    @Test
    void materializarCaiParaMinutaLegadaQuandoConteudoJsonInvalido() {
        Processo processo = new Processo();
        processo.setNumeroProcesso("proc-1");
        Usuario usuario = new Usuario();

        LaianePeticaoInicialDraftSession draft = new LaianePeticaoInicialDraftSession();
        draft.setConteudoJson("{ json invalido");
        draft.setMinutaInicial("Primeiro parágrafo.\n\nSegundo parágrafo.");

        when(pdfExportService.export(any(), any(), any(), anyList()))
                .thenReturn(new PeticaoInicialPdfExportService.PeticaoInicialPdfArtifact(
                        new byte[]{9}, "h256", "h384", 1));
        when(documentoProcessualRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.materializar(processo, usuario, draft);

        ArgumentCaptor<List<String>> linhasCaptor = ArgumentCaptor.forClass(List.class);
        verify(pdfExportService).export(any(), any(), any(), linhasCaptor.capture());
        assertThat(linhasCaptor.getValue()).containsExactly("Primeiro parágrafo.", "Segundo parágrafo.");
    }
}
