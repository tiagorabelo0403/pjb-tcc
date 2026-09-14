package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.peticionamento.identidade.PeticaoIdentidadeVisualService;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocxExportServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private CurrentUserService currentUserService;
    private PeticaoIdentidadeVisualService identidadeVisualService;
    private DocxExportService service;

    @BeforeEach
    void setUp() {
        currentUserService = mock(CurrentUserService.class);
        identidadeVisualService = mock(PeticaoIdentidadeVisualService.class);
        service = new DocxExportService(new RichTextDocumentSanitizer(mapper, new RichTextFormatCatalog()),
                currentUserService, identidadeVisualService);
        when(currentUserService.getOrNull()).thenReturn(null);
        when(identidadeVisualService.resolvePresetParaAtor(any())).thenReturn(Optional.empty());
    }

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, String> unzip(byte[] docx) {
        Map<String, String> parts = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                parts.put(e.getName(), new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return parts;
    }

    @Test
    void gerarDocxProduzPacoteOoxmlValidoComAsPartesObrigatorias() {
        byte[] docx = service.exportar(parse("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Olá"}]}]}"""), "Ação de Cobrança");
        Map<String, String> parts = unzip(docx);
        assertThat(parts).containsKeys("[Content_Types].xml", "_rels/.rels", "word/document.xml");
        assertThat(parts.get("word/document.xml")).contains("<w:document").contains("<w:body>").contains("Ação de Cobrança");
    }

    @Test
    void marcasViramWordprocessingML() {
        byte[] docx = service.exportar(parse("""
                {"type":"doc","content":[{"type":"paragraph","attrs":{"textAlign":"justify"},"content":[
                  {"type":"text","text":"forte","marks":[{"type":"bold"}]},
                  {"type":"text","text":"grifo","marks":[{"type":"underline"}]},
                  {"type":"text","text":"arial","marks":[{"type":"textStyle","attrs":{"fontFamily":"Arial","fontSize":"12pt"}}]}
                ]}]}"""), null);
        String doc = unzip(docx).get("word/document.xml");
        assertThat(doc).contains("<w:b/>");
        assertThat(doc).contains("<w:u w:val=\"single\"/>");
        assertThat(doc).contains("<w:jc w:val=\"both\"/>");
        assertThat(doc).contains("w:ascii=\"Arial\"");
        assertThat(doc).contains("<w:sz w:val=\"24\"/>"); // 12pt -> 24 meio-pontos
    }

    @Test
    void listasTitulosETabelaSaoRenderizados() {
        byte[] docx = service.exportar(parse("""
                {"type":"doc","content":[
                  {"type":"heading","attrs":{"level":1},"content":[{"type":"text","text":"DOS FATOS"}]},
                  {"type":"bulletList","content":[{"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"item um"}]}]}]},
                  {"type":"table","content":[{"type":"tableRow","content":[{"type":"tableCell","content":[{"type":"paragraph","content":[{"type":"text","text":"celula"}]}]}]}]}
                ]}"""), null);
        String doc = unzip(docx).get("word/document.xml");
        assertThat(doc).contains("DOS FATOS").contains("<w:b/>");
        assertThat(doc).contains("•").contains("item um");
        assertThat(doc).contains("<w:tbl>").contains("celula");
    }

    @Test
    void conteudoPerigosoEhSanitizadoAntesDeExportar() {
        byte[] docx = service.exportar(parse("""
                {"type":"doc","content":[
                  {"type":"script","content":[{"type":"text","text":"alert(1)"}]},
                  {"type":"paragraph","content":[{"type":"text","text":"legítimo","marks":[{"type":"link","attrs":{"href":"javascript:alert(1)"}}]}]}
                ]}"""), null);
        String doc = unzip(docx).get("word/document.xml");
        assertThat(doc).doesNotContain("alert(1)").doesNotContain("javascript");
        assertThat(doc).contains("legítimo");
    }

    @Test
    void timbreDoAtorEntraNoTopoQuandoResolvido() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        when(currentUserService.getOrNull()).thenReturn(u);
        when(identidadeVisualService.resolvePresetParaAtor(any())).thenReturn(Optional.of(Map.of(
                "cabecalhoSugerido", List.of("MINISTÉRIO PÚBLICO DO ESTADO (BA)"))));

        byte[] docx = service.exportar(parse("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"corpo"}]}]}"""), null);
        String doc = unzip(docx).get("word/document.xml");
        assertThat(doc).contains("MINISTÉRIO PÚBLICO DO ESTADO (BA)");
    }

    @Test
    void escapaXmlDoTextoDoUsuario() {
        byte[] docx = service.exportar(parse("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"a < b & c > d"}]}]}"""), null);
        String doc = unzip(docx).get("word/document.xml");
        assertThat(doc).contains("a &lt; b &amp; c &gt; d");
    }
}
