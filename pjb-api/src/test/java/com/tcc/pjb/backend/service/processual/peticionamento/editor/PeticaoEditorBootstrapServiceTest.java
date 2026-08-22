package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.editor.EditorBootstrapResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.peticionamento.identidade.PeticaoIdentidadeVisualService;
import com.tcc.pjb.backend.service.processual.peticionamento.rascunho.PeticaoDraftVersionamentoService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeticaoEditorBootstrapServiceTest {

    private CurrentUserService currentUserService;
    private PeticaoIdentidadeVisualService identidadeVisualService;
    private PeticaoEditorBootstrapService service;

    @BeforeEach
    void setUp() {
        currentUserService = mock(CurrentUserService.class);
        identidadeVisualService = mock(PeticaoIdentidadeVisualService.class);
        service = new PeticaoEditorBootstrapService(currentUserService, new RichTextFormatCatalog(), identidadeVisualService);
    }

    private Usuario ator(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(tipo);
        return u;
    }

    @Test
    void bootstrapReuneFormatoIdentidadeRascunhoEMidiaNumContratoUnico() {
        when(currentUserService.getRequired()).thenReturn(ator(TipoUsuario.ADVOGADO));
        when(identidadeVisualService.resolvePresetParaAtor(any())).thenReturn(Optional.of(Map.of(
                "classeIdentidade", "PROFISSIONAL_INDIVIDUAL",
                "registroLabel", "OAB",
                "nomeExibicao", "Escritório X")));

        EditorBootstrapResponse resp = service.bootstrap();

        assertThat(resp.formato().model()).isEqualTo("TIPTAP_PROSEMIRROR_JSON");
        assertThat(resp.formato().enforcement()).isEqualTo("BACKEND_SANITIZE_JSON");
        assertThat(resp.formato().marks()).contains("bold", "italic");
        assertThat(resp.formato().fonts()).contains("Times New Roman");
        assertThat(resp.identidadeVisual().classeIdentidade()).isEqualTo("PROFISSIONAL_INDIVIDUAL");
        assertThat(resp.identidadeVisual().registroLabel()).isEqualTo("OAB");
        assertThat(resp.rascunho().autosaveUrlTemplate()).contains("/rascunhos/{draftId}/autosave");
        assertThat(resp.rascunho().maxVersoesRetidas()).isEqualTo(PeticaoDraftVersionamentoService.MAX_VERSOES_RETIDAS);
        assertThat(resp.rascunho().dedupPorHash()).isTrue();
        assertThat(resp.midia().tiposImagemAceitos()).containsExactly("image/jpeg", "image/png");
        assertThat(resp.midia().logoInstitucionalUrlTemplate()).contains("{escopoRef}");
    }

    @Test
    void bootstrapDeAtorInstitucionalTrazIdentidadeDoOrgao() {
        when(currentUserService.getRequired()).thenReturn(ator(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO));
        when(identidadeVisualService.resolvePresetParaAtor(any())).thenReturn(Optional.of(Map.of(
                "classeIdentidade", "INSTITUCIONAL",
                "poderRamo", "MINISTERIO_PUBLICO",
                "escopoRef", "MP-EST-BA",
                "cabecalhoSugerido", List.of("MINISTÉRIO PÚBLICO DO ESTADO (BA)"),
                "brasaoCoresOrigem", "DEFAULT_PJB_SUBSTITUIVEL")));

        EditorBootstrapResponse resp = service.bootstrap();

        assertThat(resp.identidadeVisual().classeIdentidade()).isEqualTo("INSTITUCIONAL");
        assertThat(resp.identidadeVisual().poderRamo()).isEqualTo("MINISTERIO_PUBLICO");
        assertThat(resp.identidadeVisual().escopoRef()).isEqualTo("MP-EST-BA");
        assertThat(resp.identidadeVisual().cabecalhoSugerido()).contains("MINISTÉRIO PÚBLICO DO ESTADO (BA)");
        assertThat(resp.identidadeVisual().brasaoCoresOrigem()).isEqualTo("DEFAULT_PJB_SUBSTITUIVEL");
    }

    @Test
    void bootstrapSemPresetNaoQuebra() {
        when(currentUserService.getRequired()).thenReturn(ator(TipoUsuario.ADVOGADO));
        when(identidadeVisualService.resolvePresetParaAtor(any())).thenReturn(Optional.empty());

        EditorBootstrapResponse resp = service.bootstrap();

        assertThat(resp.identidadeVisual()).isNotNull();
        assertThat(resp.identidadeVisual().classeIdentidade()).isNull();
        assertThat(resp.formato()).isNotNull();
    }
}
