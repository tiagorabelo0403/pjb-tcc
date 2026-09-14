package com.tcc.pjb.backend.service.advogado;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.PeticaoInicialPdfExportService;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextDocumentSanitizer;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextPlainTextExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Materializa a peça inicial da Laiane como {@link DocumentoProcessual} real (PDF de verdade, via
 * {@link PeticaoInicialPdfExportService}, mesma técnica hand-rolled com Apache PDFBox já usada em
 * peças recursais). Extraído de {@link LaianePeticaoInicialProtocolarService} porque estes 5
 * colaboradores são usados exclusivamente por este fluxo -- nenhum é tocado pelo resto do
 * protocolo (validação de completude, ajuizamento, recibo, roteamento institucional).
 */
@Service
public class LaianePeticaoInicialPecaMaterializationService {

    private final ObjectMapper objectMapper;
    private final RichTextDocumentSanitizer richTextDocumentSanitizer;
    private final RichTextPlainTextExtractor richTextPlainTextExtractor;
    private final PeticaoInicialPdfExportService peticaoInicialPdfExportService;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final DocumentoPaginaRepository documentoPaginaRepository;

    public LaianePeticaoInicialPecaMaterializationService(ObjectMapper objectMapper,
                                                            RichTextDocumentSanitizer richTextDocumentSanitizer,
                                                            RichTextPlainTextExtractor richTextPlainTextExtractor,
                                                            PeticaoInicialPdfExportService peticaoInicialPdfExportService,
                                                            DocumentoProcessualRepository documentoProcessualRepository,
                                                            DocumentoPaginaRepository documentoPaginaRepository) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.richTextDocumentSanitizer = Objects.requireNonNull(richTextDocumentSanitizer);
        this.richTextPlainTextExtractor = Objects.requireNonNull(richTextPlainTextExtractor);
        this.peticaoInicialPdfExportService = Objects.requireNonNull(peticaoInicialPdfExportService);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.documentoPaginaRepository = Objects.requireNonNull(documentoPaginaRepository);
    }

    /**
     * Antes desta materialização, o corpo da petição só existia como JSON/HTML no rascunho --
     * invisível ao painel de leitura documental, à timeline e ao download de documento que juiz,
     * servidor e parte usam para todo o resto do processo. Fecha essa lacuna sem gate novo: reusa
     * o mesmo ABAC/sigilo que já protege qualquer outro {@link DocumentoProcessual}.
     */
    public void materializar(Processo processo, Usuario usuario, LaianePeticaoInicialDraftSession draft) {
        List<String> linhas = extrairLinhasDaPeca(draft);
        PeticaoInicialPdfExportService.PeticaoInicialPdfArtifact artifact =
                peticaoInicialPdfExportService.export("Petição Inicial", processo, usuario, linhas);
        NivelSigilo sigilo = processo.getSigilo() == null ? NivelSigilo.PUBLICO : processo.getSigilo();
        DocumentoProcessual documento = DocumentoProcessual.builder()
                .processo(processo)
                .titulo("Petição Inicial")
                .nomeOriginal("peticao-inicial-" + safeFileToken(processo.getNumeroProcesso()) + ".pdf")
                .sha256(artifact.sha256())
                .sha384(artifact.sha384())
                .contentType("application/pdf")
                .tamanhoBytes((long) artifact.bytes().length)
                .pdf(artifact.bytes())
                .origemSistema("LAIANE_PETICAO_INICIAL")
                .categoria(sigilo == NivelSigilo.PUBLICO ? DocumentoCategoria.PUBLICO : DocumentoCategoria.PESSOAL)
                .tipoDocumento(TipoDocumento.PETICAO_INICIAL)
                .nivelSigilo(sigilo)
                .criadoPor(usuario == null ? null : usuario.getId())
                .criadoEm(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        documento.setQuantidadePaginas(artifact.paginas());
        DocumentoProcessual salvo = documentoProcessualRepository.save(documento);
        documentoPaginaRepository.save(DocumentoPagina.builder()
                .documento(salvo)
                .pageNumber(1)
                .pageId("peticao-inicial-" + salvo.getId())
                .fingerprint(artifact.sha256())
                .textoExtraido(String.join("\n", linhas))
                .criadoEm(LocalDateTime.now(ZoneOffset.UTC))
                .build());
    }

    private List<String> extrairLinhasDaPeca(LaianePeticaoInicialDraftSession draft) {
        String json = draft.getConteudoJson();
        if (json != null && !json.isBlank()) {
            try {
                com.fasterxml.jackson.databind.JsonNode doc = objectMapper.readTree(json);
                com.fasterxml.jackson.databind.JsonNode limpo = richTextDocumentSanitizer.sanitize(doc).documento();
                return richTextPlainTextExtractor.extract(limpo);
            } catch (Exception e) {
                // conteudo_json inválido: cai para a minuta legada como texto puro abaixo, nunca falha o protocolo.
            }
        }
        String minuta = draft.getMinutaInicial();
        if (minuta == null || minuta.isBlank()) {
            return List.of();
        }
        List<String> linhas = new ArrayList<>();
        for (String paragrafo : minuta.replace("\r\n", "\n").split("\n\\s*\n")) {
            String limpo = paragrafo.trim().replace('\n', ' ');
            if (!limpo.isBlank()) {
                linhas.add(limpo);
            }
        }
        return linhas;
    }

    private String safeFileToken(String value) {
        if (value == null || value.isBlank()) {
            return "documento";
        }
        return value.replaceAll("[^A-Za-z0-9-]", "-");
    }
}
