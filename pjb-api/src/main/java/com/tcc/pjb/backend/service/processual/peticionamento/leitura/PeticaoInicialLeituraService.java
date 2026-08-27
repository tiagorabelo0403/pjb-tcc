package com.tcc.pjb.backend.service.processual.peticionamento.leitura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processo.ProcessoAccessApplicationService;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextDocumentSanitizer;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextHtmlRenderer;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lê a peça inicial publicada de um processo e a devolve como HTML seguro para exibição.
 *
 * <p>Fecha o par escrever→ler com a MESMA fonte de verdade sanitizada: o corpo é renderizado a
 * partir do {@code conteudo_json} autoritativo (o documento TipTap que o peticionante salvou),
 * passando de novo pelo {@link RichTextDocumentSanitizer} antes do {@link RichTextHtmlRenderer} —
 * nunca do HTML do cliente. Quando o processo é anterior ao editor rico (sem {@code conteudo_json}),
 * a minuta legada é escapada como texto puro; jamais se emite HTML não validado.</p>
 *
 * <p>O acesso é gateado exatamente como o download de documento
 * ({@link com.tcc.pjb.backend.controller.DocumentoController}): carrega o processo, resolve o sigilo
 * efetivo e exige leitura no ABAC ({@link PjbAuthorizationService#requireReadProcessoAtSecrecy}).
 * Nenhum gate paralelo ou mais permissivo é introduzido aqui.</p>
 */
@Service
public class PeticaoInicialLeituraService {

    private static final Logger log = LoggerFactory.getLogger(PeticaoInicialLeituraService.class);

    private final LaianePeticaoInicialDraftSessionRepository repository;
    private final ProcessoAccessApplicationService processoAccess;
    private final RecursalEffectiveSecrecyService secrecyService;
    private final PjbAuthorizationService authorizationService;
    private final RichTextDocumentSanitizer sanitizer;
    private final RichTextHtmlRenderer htmlRenderer;
    private final ObjectMapper objectMapper;

    public PeticaoInicialLeituraService(LaianePeticaoInicialDraftSessionRepository repository,
                                        ProcessoAccessApplicationService processoAccess,
                                        RecursalEffectiveSecrecyService secrecyService,
                                        PjbAuthorizationService authorizationService,
                                        RichTextDocumentSanitizer sanitizer,
                                        RichTextHtmlRenderer htmlRenderer,
                                        ObjectMapper objectMapper) {
        this.repository = repository;
        this.processoAccess = processoAccess;
        this.secrecyService = secrecyService;
        this.authorizationService = authorizationService;
        this.sanitizer = sanitizer;
        this.htmlRenderer = htmlRenderer;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PecaInicialLeituraResponse lerPorProcesso(Long processoId) {
        Processo processo = processoAccess.load(processoId);
        NivelSigilo efetivo = secrecyService.effectiveSecrecyForProcesso(processoId);
        authorizationService.requireReadProcessoAtSecrecy(processo, efetivo);

        LaianePeticaoInicialDraftSession peca = repository.findByProcesso_Id(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("PeticaoInicial", processoId));

        Conteudo conteudo = renderConteudo(peca);
        boolean sigiloso = efetivo != null && efetivo != NivelSigilo.PUBLICO;

        return new PecaInicialLeituraResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                peca.getTituloCaso(),
                peca.getRitoSugerido(),
                conteudo.html(),
                conteudo.origem(),
                sigiloso,
                peca.getUpdatedAt()
        );
    }

    private Conteudo renderConteudo(LaianePeticaoInicialDraftSession peca) {
        String json = peca.getConteudoJson();
        if (json != null && !json.isBlank()) {
            try {
                JsonNode doc = objectMapper.readTree(json);
                JsonNode limpo = sanitizer.sanitize(doc).documento();
                return new Conteudo(htmlRenderer.toHtml(limpo), "JSON_SANITIZADO");
            } catch (Exception e) {
                // conteudo_json corrompido: cai para a minuta legada (escapada), nunca para HTML bruto.
                log.warn("conteudo_json inválido na peça {} do processo {}; usando minuta legada como texto: {}",
                        peca.getId(), peca.getProcesso() != null ? peca.getProcesso().getId() : null, e.getMessage());
            }
        }
        String minuta = peca.getMinutaInicial();
        if (minuta != null && !minuta.isBlank()) {
            return new Conteudo(escaparComoTexto(minuta), "MINUTA_TEXTO");
        }
        return new Conteudo("", "VAZIO");
    }

    /**
     * Converte texto puro (minuta legada) em HTML seguro: escapa todo caractere ativo e preserva
     * quebras de linha como parágrafos/&lt;br&gt;. Nunca interpreta o conteúdo como marcação.
     */
    private String escaparComoTexto(String texto) {
        String esc = texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
        StringBuilder sb = new StringBuilder();
        for (String bloco : esc.split("\\r?\\n\\r?\\n")) {
            if (bloco.isBlank()) {
                continue;
            }
            sb.append("<p>").append(bloco.replace("\r\n", "\n").replace("\n", "<br/>")).append("</p>");
        }
        return sb.toString();
    }

    private record Conteudo(String html, String origem) {
    }
}
