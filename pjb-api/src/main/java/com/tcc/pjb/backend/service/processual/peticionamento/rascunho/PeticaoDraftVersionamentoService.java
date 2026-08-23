package com.tcc.pjb.backend.service.processual.peticionamento.rascunho;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextDocumentSanitizer;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextHtmlRenderer;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.AutosaveRascunhoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.DraftVersaoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.RascunhoConteudoResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.entity.peticionamento.PeticaoDraftVersao;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.model.repository.PeticaoDraftVersaoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autosave resiliente + versionamento do rascunho de peça inicial. O rascunho ativo é atualizado
 * no lugar (update-in-place) a cada autosave — de modo que uma queda de energia/conexão nunca perde
 * o último conteúdo salvo —, e cada mudança real de conteúdo grava um snapshot imutável em
 * {@code tb_peticao_draft_versao}, permitindo voltar a estados anteriores. Isolamento por ator: só
 * o dono do rascunho (solicitante) autosalva, lista versões ou restaura; ninguém vê rascunho alheio.
 */
@Service
public class PeticaoDraftVersionamentoService {

    public static final int MAX_VERSOES_RETIDAS = 30;

    private final LaianePeticaoInicialDraftSessionRepository draftRepository;
    private final PeticaoDraftVersaoRepository versaoRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final RichTextDocumentSanitizer sanitizer;
    private final RichTextHtmlRenderer htmlRenderer;

    public PeticaoDraftVersionamentoService(LaianePeticaoInicialDraftSessionRepository draftRepository,
                                            PeticaoDraftVersaoRepository versaoRepository,
                                            CurrentUserService currentUserService,
                                            ObjectMapper objectMapper,
                                            RichTextDocumentSanitizer sanitizer,
                                            RichTextHtmlRenderer htmlRenderer) {
        this.draftRepository = Objects.requireNonNull(draftRepository, "draftRepository");
        this.versaoRepository = Objects.requireNonNull(versaoRepository, "versaoRepository");
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
        this.htmlRenderer = Objects.requireNonNull(htmlRenderer, "htmlRenderer");
    }

    @Transactional
    public RascunhoConteudoResponse autosalvar(Long draftId, AutosaveRascunhoRequest request) {
        LaianePeticaoInicialDraftSession draft = requireOwnedDraft(draftId);

        String titulo = firstNonBlank(request.tituloCaso(), draft.getTituloCaso());

        // JSON validado é a fonte de verdade: quando o editor envia o documento, ele é sanitizado no
        // servidor e o HTML da minuta passa a ser projeção derivada e segura desse JSON (o minutaHtml
        // que o cliente mandou é descartado). Sem documentoJson, mantém o comportamento legado (HTML).
        String conteudoJson = draft.getConteudoJson();
        String minuta = request.minutaHtml() != null ? request.minutaHtml() : draft.getMinutaInicial();
        if (request.documentoJson() != null) {
            JsonNode limpo = sanitizer.sanitize(request.documentoJson()).documento();
            conteudoJson = limpo.toString();
            minuta = htmlRenderer.toHtml(limpo);
        }
        String fatosJson = request.fatos() != null ? writeList(request.fatos()) : draft.getFatosJson();
        String pedidosJson = request.pedidos() != null ? writeList(request.pedidos()) : draft.getPedidosJson();
        String fundamentosJson = request.fundamentos() != null ? writeList(request.fundamentos()) : draft.getFundamentosJson();
        String provasJson = request.provas() != null ? writeList(request.provas()) : draft.getProvasJson();

        String novoHash = computeHash(titulo, conteudoJson, minuta, fatosJson, pedidosJson, fundamentosJson, provasJson);
        if (novoHash.equals(draft.getHashIntegridade())) {
            return toConteudo(draft, versaoRepository.maxVersaoSeq(draft.getId()), false);
        }

        draft.setTituloCaso(titulo == null ? draft.getTituloCaso() : titulo);
        draft.setConteudoJson(conteudoJson);
        draft.setMinutaInicial(minuta == null ? "" : minuta);
        draft.setFatosJson(fatosJson);
        draft.setPedidosJson(pedidosJson);
        draft.setFundamentosJson(fundamentosJson);
        draft.setProvasJson(provasJson);
        draft.setHashIntegridade(novoHash);
        draft.setStatus("RASCUNHO");
        LaianePeticaoInicialDraftSession salvo = draftRepository.save(draft);

        int versao = snapshot(salvo, "AUTOSAVE");
        return toConteudo(salvo, versao, true);
    }

    @Transactional(readOnly = true)
    public List<DraftVersaoResponse> listarVersoes(Long draftId) {
        requireOwnedDraft(draftId);
        return versaoRepository.findTop50ByDraftIdOrderByVersaoSeqDesc(draftId).stream()
                .map(DraftVersaoResponse::from)
                .toList();
    }

    @Transactional
    public RascunhoConteudoResponse restaurar(Long draftId, int versaoSeq) {
        LaianePeticaoInicialDraftSession draft = requireOwnedDraft(draftId);
        PeticaoDraftVersao versao = versaoRepository.findByDraftIdAndVersaoSeq(draftId, versaoSeq)
                .orElseThrow(() -> new RecursoNaoEncontradoException("PeticaoDraftVersao", (long) versaoSeq));

        draft.setTituloCaso(versao.getTituloCaso() == null ? draft.getTituloCaso() : versao.getTituloCaso());
        draft.setConteudoJson(versao.getConteudoJson());
        draft.setMinutaInicial(versao.getMinutaHtml() == null ? "" : versao.getMinutaHtml());
        draft.setFatosJson(versao.getFatosJson());
        draft.setPedidosJson(versao.getPedidosJson());
        draft.setFundamentosJson(versao.getFundamentosJson());
        draft.setProvasJson(versao.getProvasJson());
        draft.setHashIntegridade(versao.getHashIntegridade());
        draft.setStatus("RASCUNHO");
        LaianePeticaoInicialDraftSession salvo = draftRepository.save(draft);

        int novaVersao = snapshot(salvo, "RESTAURACAO");
        return toConteudo(salvo, novaVersao, true);
    }

    private int snapshot(LaianePeticaoInicialDraftSession draft, String origem) {
        int proximaVersao = versaoRepository.maxVersaoSeq(draft.getId()) + 1;
        PeticaoDraftVersao versao = new PeticaoDraftVersao(draft.getId(), proximaVersao, origem);
        versao.setTituloCaso(draft.getTituloCaso());
        versao.setConteudoJson(draft.getConteudoJson());
        versao.setMinutaHtml(draft.getMinutaInicial());
        versao.setFatosJson(draft.getFatosJson());
        versao.setPedidosJson(draft.getPedidosJson());
        versao.setFundamentosJson(draft.getFundamentosJson());
        versao.setProvasJson(draft.getProvasJson());
        versao.setHashIntegridade(draft.getHashIntegridade());
        versaoRepository.save(versao);
        pruneOldVersions(draft.getId());
        return proximaVersao;
    }

    private void pruneOldVersions(Long draftId) {
        long total = versaoRepository.countByDraftId(draftId);
        if (total <= MAX_VERSOES_RETIDAS) {
            return;
        }
        List<Long> ascending = versaoRepository.idsByDraftAscending(draftId);
        int excedente = (int) (total - MAX_VERSOES_RETIDAS);
        for (int i = 0; i < excedente && i < ascending.size(); i++) {
            versaoRepository.deleteById(ascending.get(i));
        }
    }

    private LaianePeticaoInicialDraftSession requireOwnedDraft(Long draftId) {
        Usuario usuario = requirePeticionante();
        return draftRepository.findByIdAndSolicitante_Id(draftId, usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("LaianePeticaoInicialDraftSession", draftId));
    }

    private Usuario requirePeticionante() {
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        boolean permitido = tipo != null && (tipo.isAdvocacia() || tipo.isDefensoriaPublica() || tipo.isProcuradoria() || tipo.isMinisterioPublico());
        if (!permitido) {
            throw new AccessDeniedPjbException("O rascunho de peticionamento é exclusivo para advocacia, defensoria, procuradoria e Ministério Público.");
        }
        return usuario;
    }

    private RascunhoConteudoResponse toConteudo(LaianePeticaoInicialDraftSession draft, int versaoAtual, boolean alterado) {
        return new RascunhoConteudoResponse(
                draft.getId(),
                draft.getStatus(),
                draft.getTituloCaso(),
                parseJson(draft.getConteudoJson()),
                draft.getMinutaInicial(),
                draft.getHashIntegridade(),
                versaoAtual,
                alterado,
                draft.getUpdatedAt());
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String computeHash(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part == null ? "" : part).append('\u0001');
        }
        return Hashes.sha256Hex(sb.toString());
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        return b;
    }
}
