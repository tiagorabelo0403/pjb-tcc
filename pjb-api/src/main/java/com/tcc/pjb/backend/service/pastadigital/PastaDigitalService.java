package com.tcc.pjb.backend.service.pastadigital;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.sigilo.DocumentoSigiloClassifier;
import com.tcc.pjb.backend.core.util.Ids;
import com.tcc.pjb.backend.model.dto.pastadigital.*;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PastaDigitalService {

    
    private static final long LIMITE_BYTES = 5L * 1024L * 1024L;

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final DocumentoPaginaRepository paginaRepository;
    private final PjbAuthorizationService authorizationService;
    private final DocumentoSigiloClassifier sigiloClassifier;

    @Transactional
    public DocumentoIndexadoResponse anexarDocumentoPdf(Long processoId,
                                                        MultipartFile arquivo,
                                                        String titulo,
                                                        Long criadoPor,
                                                        String origemSistema,
                                                        String categoriaRaw,
                                                        String nivelSigiloRaw) {
        DocumentoCategoria categoria = DocumentoCategoria.fromString(categoriaRaw);
        NivelSigilo sigiloDocInput = NivelSigilo.fromString(nivelSigiloRaw);

        if (arquivo == null || arquivo.isEmpty()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, "arquivo")
                    .addMetadado("motivo", "arquivo ausente ou vazio");
        }
        if (arquivo.getSize() > LIMITE_BYTES) {
            throw new ErroDeValidacaoException(TipoErroValidacao.TAMANHO_EXCEDIDO, arquivo.getOriginalFilename())
                    .addMetadado("tamanho_atual", arquivo.getSize())
                    .addMetadado("tamanho_limite", LIMITE_BYTES);
        }

        String nomeOriginal = arquivo.getOriginalFilename();
        String contentType = arquivo.getContentType();
        if (!isPdf(arquivo)) {
            throw new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, nomeOriginal)
                    .addMetadado("tipo_recebido", contentType)
                    .addMetadado("tipo_esperado", MediaType.APPLICATION_PDF_VALUE);
        }

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

        
        
        authorizationService.requireWriteProcesso(processo);

        NivelSigilo procSigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();

        try {
            byte[] bytes = arquivo.getBytes();
            String sha256 = sha256Hex(bytes);

            if (documentoRepository.existsByProcessoIdAndSha256(processoId, sha256)) {
                throw new ErroDeValidacaoException(TipoErroValidacao.DUPLICIDADE_DETECTADA, nomeOriginal)
                        .addMetadado("motivo", "Documento com mesma hash já anexado ao processo")
                        .addMetadado("sha256", sha256);
            }

            try (PDDocument pdf = Loader.loadPDF(bytes)) {
                if (pdf.isEncrypted()) {
                    throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_PROTEGIDO, nomeOriginal)
                            .addMetadado("motivo", "Documento possui senha");
                }
                int n = pdf.getNumberOfPages();
                if (n <= 0) {
                    throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nomeOriginal)
                            .addMetadado("motivo", "PDF com 0 páginas");
                }

                
                String sampleText = extractSampleText(pdf, Math.min(2, n));
                DocumentoSigiloClassifier.Classification cls = sigiloClassifier.classify(nomeOriginal, sampleText);
                DocumentoCategoria categoriaFinal = categoria;
                if (categoriaFinal == null && cls.suggestedCategoria() != null) {
                    categoriaFinal = cls.suggestedCategoria();
                }
                
                if (categoriaFinal != DocumentoCategoria.PESSOAL && cls.suggestedCategoria() == DocumentoCategoria.PESSOAL && cls.confidence() >= 0.65) {
                    categoriaFinal = DocumentoCategoria.PESSOAL;
                }

                NivelSigilo minCategoria = (categoriaFinal == DocumentoCategoria.PESSOAL) ? NivelSigilo.SIGILO_N2 : NivelSigilo.PUBLICO;
                NivelSigilo sigiloDoc = maxSigilo(procSigilo, maxSigilo(sigiloDocInput, maxSigilo(minCategoria, cls.minSigilo())));

                DocumentoProcessual doc = DocumentoProcessual.builder()
                        .processo(processo)
                        .nomeOriginal(nomeOriginal)
                        .titulo((titulo == null || titulo.isBlank()) ? nomeOriginal : titulo)
                        .contentType(contentType != null ? contentType : MediaType.APPLICATION_PDF_VALUE)
                        .tamanhoBytes(arquivo.getSize())
                        .sha256(sha256)
                        .pdf(bytes)
                        .nivelSigilo(sigiloDoc)
                        .categoria(categoriaFinal)
                        .criadoPor(criadoPor)
                        .origemSistema(origemSistema != null ? origemSistema : "API")
                        .criadoEm(LocalDateTime.now())
                        .build();

                doc = documentoRepository.save(doc);

                PDFTextStripper stripper = new PDFTextStripper();
                List<PageRefDTO> pages = new ArrayList<>(n);

                for (int page = 1; page <= n; page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String pageText = stripper.getText(pdf);
                    String normalized = normalizeText(pageText);
                    String fingerprint = sha256Hex((normalized + "|p=" + page).getBytes(StandardCharsets.UTF_8));

                    String pageId = nextUniquePageId();

                    DocumentoPagina p = DocumentoPagina.builder()
                            .documento(doc)
                            .pageNumber(page)
                            .pageId(pageId)
                            .fingerprint(fingerprint)
                            .textoExtraido(pageText)
                            .criadoEm(LocalDateTime.now())
                            .build();

                    paginaRepository.save(p);

                    pages.add(PageRefDTO.builder()
                            .pageNumber(page)
                            .pageId(pageId)
                            .fingerprint(fingerprint)
                            .preview(preview(pageText))
                            .build());
                }

                log.info("PastaDigital: documento indexado. processoId={} docId={} pages={}", processoId, doc.getId(), n);

                if (cls.suggestedCategoria() == DocumentoCategoria.PESSOAL && cls.confidence() >= 0.65) {
                    log.warn("SigiloClassifier: documento marcado como PESSOAL por sinais fortes. processoId={} docId={} confidence={} reasons={}",
                            processoId, doc.getId(), cls.confidence(), cls.reasons());
                }

                return DocumentoIndexadoResponse.builder()
                        .documentoId(doc.getId())
                        .processoId(processoId)
                        .nomeOriginal(doc.getNomeOriginal())
                        .titulo(doc.getTitulo())
                        .contentType(doc.getContentType())
                        .tamanhoBytes(doc.getTamanhoBytes())
                        .sha256(doc.getSha256())
                        .numeroPaginas(n)
                        .pages(pages)
                        .build();
            }

        } catch (IOException e) {
            throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nomeOriginal, e)
                    .addMetadado("erro_tecnico", e.getClass().getSimpleName());
        }
    }

    private static String extractSampleText(PDDocument pdf, int pages) throws IOException {
        if (pdf == null || pages <= 0) return "";
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(pages);
        return stripper.getText(pdf);
    }

    @Transactional(readOnly = true)
    public PastaDigitalResponse pastaDigital(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

        
        authorizationService.requireReadProcesso(processo);

        var docs = documentoRepository.findByProcessoId(processoId);
        var resumos = docs.stream().map(d -> {
            int paginas = paginaRepository.findByDocumentoId(d.getId()).size();
            return DocumentoResumoDTO.builder()
                    .documentoId(d.getId())
                    .nomeOriginal(d.getNomeOriginal())
                    .titulo(d.getTitulo())
                    .contentType(d.getContentType())
                    .tamanhoBytes(d.getTamanhoBytes())
                    .numeroPaginas(paginas)
                    .criadoEm(d.getCriadoEm())
                    .build();
        }).toList();

        return PastaDigitalResponse.builder()
                .processoId(processoId)
                .documentos(resumos)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResolveResponse resolverPageId(String pageId) {
        DocumentoPagina p = paginaRepository.findByPageId(pageId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Página", pageId));

        DocumentoProcessual d = p.getDocumento();
        
        Long processoId = d.getProcesso() != null ? d.getProcesso().getId() : null;

        if (processoId != null) {
            Processo processo = processoRepository.findById(processoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
            authorizationService.requireReadProcesso(processo);
        }

        return PageResolveResponse.builder()
                .pageId(p.getPageId())
                .documentoId(d.getId())
                .processoId(processoId)
                .pageNumber(p.getPageNumber())
                .fingerprint(p.getFingerprint())
                .texto(p.getTextoExtraido())
                .build();
    }

    @Transactional(readOnly = true)
    public PageSearchResponse buscarNoProcesso(Long processoId, String q, int limit) {
        if (q == null || q.trim().length() < 2) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "q")
                    .addMetadado("motivo", "termo de busca muito curto");
        }
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);

        int lim = Math.max(1, Math.min(limit, 100));
                
        var allowedDocIds = documentoRepository.findByProcessoId(processoId).stream()
                .filter(d -> authorizationService.canReadDocumento(processo, d).allowed())
                .map(DocumentoProcessual::getId)
                .collect(java.util.stream.Collectors.toSet());

        
        int overfetch = Math.min(lim * 5, 500);
        var hits = paginaRepository.searchInProcess(processoId, q.trim(), overfetch).stream()
                .filter(p -> p.getDocumento() != null && allowedDocIds.contains(p.getDocumento().getId()))
                .limit(lim)
                .toList();

        var dto = hits.stream().map(p -> PageSearchHitDTO.builder()
                .pageId(p.getPageId())
                .documentoId(p.getDocumento().getId())
                .pageNumber(p.getPageNumber())
                .fingerprint(p.getFingerprint())
                .preview(preview(p.getTextoExtraido()))
                .build()).toList();

        return PageSearchResponse.builder()
                .processoId(processoId)
                .query(q.trim())
                .limit(lim)
                .hits(dto)
                .build();
    }

    private static NivelSigilo maxSigilo(NivelSigilo a, NivelSigilo b) {
        NivelSigilo x = (a == null) ? NivelSigilo.PUBLICO : a;
        NivelSigilo y = (b == null) ? NivelSigilo.PUBLICO : b;
        return (x.getNivel() >= y.getNivel()) ? x : y;
    }

    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        String nome = file.getOriginalFilename();
        return (contentType != null && contentType.equals(MediaType.APPLICATION_PDF_VALUE)) ||
                (nome != null && nome.toLowerCase().endsWith(".pdf"));
    }

    private String nextUniquePageId() {
        
        for (int i = 0; i < 5; i++) {
            String id = Ids.opaqueId("PGID");
            if (paginaRepository.findByPageId(id).isEmpty()) return id;
        }
        
        return Ids.opaqueId("PGID") + "-" + Ids.opaqueId("X");
    }

    private static String preview(String text) {
        if (text == null) return "";
        String s = text.replaceAll("\\s+", " ").trim();
        if (s.length() <= 240) return s;
        return s.substring(0, 240) + "…";
    }

    private static String normalizeText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
