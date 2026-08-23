package com.tcc.pjb.backend.service.processual.peticionamento.rascunho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.AutosaveRascunhoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.RascunhoConteudoResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.entity.peticionamento.PeticaoDraftVersao;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.model.repository.PeticaoDraftVersaoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PeticaoDraftVersionamentoServiceTest {

    private LaianePeticaoInicialDraftSessionRepository draftRepository;
    private PeticaoDraftVersaoRepository versaoRepository;
    private CurrentUserService currentUserService;
    private PeticaoDraftVersionamentoService service;

    @BeforeEach
    void setUp() {
        draftRepository = mock(LaianePeticaoInicialDraftSessionRepository.class);
        versaoRepository = mock(PeticaoDraftVersaoRepository.class);
        currentUserService = mock(CurrentUserService.class);
        com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextFormatCatalog catalog =
                new com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextFormatCatalog();
        service = new PeticaoDraftVersionamentoService(draftRepository, versaoRepository, currentUserService, new ObjectMapper(),
                new com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextDocumentSanitizer(new ObjectMapper(), catalog),
                new com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextHtmlRenderer());
        when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        authenticateAs(TipoUsuario.ADVOGADO, 7L);
    }

    private void authenticateAs(TipoUsuario tipo, long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoUsuario(tipo);
        when(currentUserService.getRequired()).thenReturn(usuario);
    }

    private LaianePeticaoInicialDraftSession existingDraft(long id, String hash, String minuta) {
        LaianePeticaoInicialDraftSession draft = new LaianePeticaoInicialDraftSession();
        draft.setId(id);
        draft.setTituloCaso("Caso");
        draft.setMinutaInicial(minuta);
        draft.setHashIntegridade(hash);
        draft.setStatus("RASCUNHO");
        return draft;
    }

    @Test
    void naoDonoNaoAutosalva() {
        when(draftRepository.findByIdAndSolicitante_Id(5L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.autosalvar(5L, new AutosaveRascunhoRequest(null, null, "x", null, null, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void naoPeticionanteEhBloqueado() {
        authenticateAs(TipoUsuario.CIDADAO, 7L);
        assertThatThrownBy(() -> service.listarVersoes(5L)).isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void autosaveComMudancaAtualizaRascunhoEGeraVersao() {
        LaianePeticaoInicialDraftSession draft = existingDraft(5L, "HASH_ANTIGO", "<p>antigo</p>");
        when(draftRepository.findByIdAndSolicitante_Id(5L, 7L)).thenReturn(Optional.of(draft));
        when(versaoRepository.maxVersaoSeq(5L)).thenReturn(3);
        when(versaoRepository.countByDraftId(5L)).thenReturn(4L);

        RascunhoConteudoResponse resp = service.autosalvar(5L,
                new AutosaveRascunhoRequest("Caso", null, "<p>novo conteudo</p>", List.of("fato 1"), null, null, null));

        assertThat(resp.alterado()).isTrue();
        assertThat(resp.minutaHtml()).isEqualTo("<p>novo conteudo</p>");
        assertThat(resp.versaoAtual()).isEqualTo(4);
        assertThat(draft.getMinutaInicial()).isEqualTo("<p>novo conteudo</p>");
        assertThat(draft.getHashIntegridade()).isNotEqualTo("HASH_ANTIGO");

        ArgumentCaptor<PeticaoDraftVersao> captor = ArgumentCaptor.forClass(PeticaoDraftVersao.class);
        verify(versaoRepository).save(captor.capture());
        assertThat(captor.getValue().getOrigem()).isEqualTo("AUTOSAVE");
        assertThat(captor.getValue().getVersaoSeq()).isEqualTo(4);
        assertThat(captor.getValue().getMinutaHtml()).isEqualTo("<p>novo conteudo</p>");
    }

    @Test
    void autosaveSemMudancaRealNaoGeraVersaoNemSalva() {
        // hash pre-calculado a partir do mesmo conteudo que sera reenviado
        String minuta = "<p>igual</p>";
        LaianePeticaoInicialDraftSession draft = existingDraft(5L, "provisorio", minuta);
        when(draftRepository.findByIdAndSolicitante_Id(5L, 7L)).thenReturn(Optional.of(draft));
        // primeiro autosave estabelece o hash canonico
        when(versaoRepository.maxVersaoSeq(5L)).thenReturn(0);
        when(versaoRepository.countByDraftId(5L)).thenReturn(1L);
        RascunhoConteudoResponse primeiro = service.autosalvar(5L,
                new AutosaveRascunhoRequest("Caso", null, minuta, List.of(), List.of(), List.of(), List.of()));
        assertThat(primeiro.alterado()).isTrue();
        String hashCanonico = draft.getHashIntegridade();

        // reenvio identico -> sem mudanca
        when(versaoRepository.maxVersaoSeq(5L)).thenReturn(1);
        RascunhoConteudoResponse repetido = service.autosalvar(5L,
                new AutosaveRascunhoRequest("Caso", null, minuta, List.of(), List.of(), List.of(), List.of()));

        assertThat(repetido.alterado()).isFalse();
        assertThat(draft.getHashIntegridade()).isEqualTo(hashCanonico);
        // save de versao ocorreu 1x (do primeiro autosave), nao no reenvio identico
        verify(versaoRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void restaurarCopiaConteudoDaVersaoParaORascunho() {
        LaianePeticaoInicialDraftSession draft = existingDraft(5L, "HASH_ATUAL", "<p>atual</p>");
        when(draftRepository.findByIdAndSolicitante_Id(5L, 7L)).thenReturn(Optional.of(draft));
        PeticaoDraftVersao versao = new PeticaoDraftVersao(5L, 2, "AUTOSAVE");
        versao.setTituloCaso("Caso restaurado");
        versao.setMinutaHtml("<p>versao 2</p>");
        versao.setHashIntegridade("HASH_V2");
        when(versaoRepository.findByDraftIdAndVersaoSeq(5L, 2)).thenReturn(Optional.of(versao));
        when(versaoRepository.maxVersaoSeq(5L)).thenReturn(5);
        when(versaoRepository.countByDraftId(5L)).thenReturn(6L);

        RascunhoConteudoResponse resp = service.restaurar(5L, 2);

        assertThat(resp.minutaHtml()).isEqualTo("<p>versao 2</p>");
        assertThat(draft.getMinutaInicial()).isEqualTo("<p>versao 2</p>");
        assertThat(resp.versaoAtual()).isEqualTo(6);
        ArgumentCaptor<PeticaoDraftVersao> captor = ArgumentCaptor.forClass(PeticaoDraftVersao.class);
        verify(versaoRepository).save(captor.capture());
        assertThat(captor.getValue().getOrigem()).isEqualTo("RESTAURACAO");
    }

    @Test
    void restaurarVersaoInexistenteLanca() {
        LaianePeticaoInicialDraftSession draft = existingDraft(5L, "H", "<p>x</p>");
        when(draftRepository.findByIdAndSolicitante_Id(5L, 7L)).thenReturn(Optional.of(draft));
        when(versaoRepository.findByDraftIdAndVersaoSeq(5L, 99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.restaurar(5L, 99)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void pruneRemoveVersoesExcedentes() {
        LaianePeticaoInicialDraftSession draft = existingDraft(5L, "HASH_ANTIGO", "<p>a</p>");
        when(draftRepository.findByIdAndSolicitante_Id(5L, 7L)).thenReturn(Optional.of(draft));
        when(versaoRepository.maxVersaoSeq(5L)).thenReturn(31);
        when(versaoRepository.countByDraftId(5L)).thenReturn(32L);
        when(versaoRepository.idsByDraftAscending(5L)).thenReturn(java.util.stream.LongStream.rangeClosed(1, 32).boxed().toList());

        service.autosalvar(5L, new AutosaveRascunhoRequest("Caso", null, "<p>mudou</p>", null, null, null, null));

        // 32 versoes apos gravar, retencao 30 -> remove as 2 mais antigas
        verify(versaoRepository).deleteById(1L);
        verify(versaoRepository).deleteById(2L);
        verify(versaoRepository, never()).deleteById(3L);
    }

    @Test
    void autosaveComDocumentoJsonSanitizaEDerivaHtmlSeguro() throws Exception {
        LaianePeticaoInicialDraftSession draft = existingDraft(5L, "HASH_ANTIGO", "<p>antigo</p>");
        when(draftRepository.findByIdAndSolicitante_Id(5L, 7L)).thenReturn(Optional.of(draft));
        when(versaoRepository.maxVersaoSeq(5L)).thenReturn(0);
        when(versaoRepository.countByDraftId(5L)).thenReturn(1L);
        com.fasterxml.jackson.databind.JsonNode doc = new ObjectMapper().readTree("""
                {"type":"doc","content":[
                  {"type":"script","content":[{"type":"text","text":"alert(1)"}]},
                  {"type":"paragraph","content":[{"type":"text","text":"petição","marks":[{"type":"bold"}]}]}
                ]}""");

        RascunhoConteudoResponse resp = service.autosalvar(5L,
                new AutosaveRascunhoRequest("Caso", doc, null, null, null, null, null));

        assertThat(resp.alterado()).isTrue();
        // JSON autoritativo persistido, sem o no perigoso
        assertThat(draft.getConteudoJson()).contains("paragraph").doesNotContain("script").doesNotContain("alert(1)");
        // HTML e' projecao derivada e segura do JSON sanitizado (nao o que o cliente mandaria)
        assertThat(draft.getMinutaInicial()).isEqualTo("<p><strong>petição</strong></p>");
        assertThat(resp.conteudoJson()).isNotNull();
        assertThat(resp.conteudoJson().get("content").size()).isEqualTo(1);
    }

    @Test
    void versaoGuardaOJsonAutoritativoNoSnapshot() throws Exception {
        LaianePeticaoInicialDraftSession draft = existingDraft(5L, "HASH_ANTIGO", "<p>antigo</p>");
        when(draftRepository.findByIdAndSolicitante_Id(5L, 7L)).thenReturn(Optional.of(draft));
        when(versaoRepository.maxVersaoSeq(5L)).thenReturn(0);
        when(versaoRepository.countByDraftId(5L)).thenReturn(1L);
        com.fasterxml.jackson.databind.JsonNode doc = new ObjectMapper().readTree("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"corpo"}]}]}""");

        service.autosalvar(5L, new AutosaveRascunhoRequest("Caso", doc, null, null, null, null, null));

        ArgumentCaptor<PeticaoDraftVersao> captor = ArgumentCaptor.forClass(PeticaoDraftVersao.class);
        verify(versaoRepository).save(captor.capture());
        assertThat(captor.getValue().getConteudoJson()).contains("paragraph").contains("corpo");
        assertThat(captor.getValue().getMinutaHtml()).isEqualTo("<p>corpo</p>");
    }
}
