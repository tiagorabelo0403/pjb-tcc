package com.tcc.pjb.backend.integration.mni.application;

import com.tcc.pjb.backend.core.security.sigilo.DocumentoSigiloClassifier;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.mni.adapter.MniDocumentoParsed;
import com.tcc.pjb.backend.integration.mni.adapter.MniTipoDocumentoKeywordMatcher;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.document.DocumentoEstadoOperacional;
import com.tcc.pjb.backend.model.entity.enums.document.DocumentoOrigemSistema;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.document.DocumentContentValidator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingestão de documentos importados via MNI, reaproveitando as mesmas primitivas validadas de
 * {@code MarketplaceDocumentoPersistenceService} (validação estrutural de PDF, classificação de
 * sigilo por conteúdo, armazenamento de objeto, dedupe por sha256) em vez de gravar bytes crus.
 *
 * <p>TipoDocumento não tem valor genérico de fallback: quando o casamento por palavra-chave
 * ({@link MniTipoDocumentoKeywordMatcher}) não é conclusivo, o documento é gravado mesmo assim
 * (conteúdo nunca se perde) mas com tipoDocumento nulo e
 * {@link DocumentoEstadoOperacional#AGUARDANDO_CLASSIFICACAO}, para um servidor confirmar depois
 * via {@link #confirmarClassificacao(UUID, TipoDocumento)} — nunca chuta um tipo às cegas.
 *
 * <p>Falha em um documento (PDF corrompido, tamanho fora do limite, tipo não suportado) não pode
 * derrubar a recepção inteira do processo: é registrada e o documento é pulado, os demais e o
 * restante da recepção seguem normalmente.
 */
@Service
public class MniDocumentoIngestaoService {

    private static final Logger log = LoggerFactory.getLogger(MniDocumentoIngestaoService.class);

    private final DocumentoProcessualRepository documentoRepository;
    private final ObjectStoragePort objectStorage;
    private final DocumentContentValidator contentValidator;
    private final DocumentoSigiloClassifier sigiloClassifier;
    private final MniTipoDocumentoKeywordMatcher tipoDocumentoMatcher;

    public MniDocumentoIngestaoService(DocumentoProcessualRepository documentoRepository,
                                       ObjectStoragePort objectStorage,
                                       DocumentContentValidator contentValidator,
                                       DocumentoSigiloClassifier sigiloClassifier,
                                       MniTipoDocumentoKeywordMatcher tipoDocumentoMatcher) {
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.objectStorage = Objects.requireNonNull(objectStorage);
        this.contentValidator = Objects.requireNonNull(contentValidator);
        this.sigiloClassifier = Objects.requireNonNull(sigiloClassifier);
        this.tipoDocumentoMatcher = Objects.requireNonNull(tipoDocumentoMatcher);
    }

    @Transactional
    public void ingestar(Processo processo, List<MniDocumentoParsed> documentos) {
        if (processo == null || processo.getId() == null || documentos.isEmpty()) {
            return;
        }
        for (MniDocumentoParsed documento : documentos) {
            try {
                ingestarUm(processo, documento);
            } catch (Exception e) {
                log.warn("Documento MNI descartado por falha na ingestão: processoId={} nome={} motivo={}",
                        processo.getId(), documento.nome(), e.getMessage());
            }
        }
    }

    private void ingestarUm(Processo processo, MniDocumentoParsed documento) throws IOException {
        byte[] bytes = documento.conteudo();
        String nome = documento.nome() == null || documento.nome().isBlank() ? "documento_mni.pdf" : documento.nome();

        contentValidator.validarTamanho(bytes.length, nome);
        contentValidator.validarExtensaoOuContentType(nome, documento.mimetype());

        String sha256 = Hashes.sha256HexBytes(bytes);
        if (documentoRepository.existsByProcessoIdAndSha256(processo.getId(), sha256)) {
            return;
        }

        String sampleText;
        try (var validado = contentValidator.validarEstruturaPdf(bytes, nome)) {
            sampleText = extractSampleText(validado);
        }

        var cls = sigiloClassifier.classify(nome, sampleText);
        DocumentoCategoria categoria = cls.suggestedCategoria() == null ? DocumentoCategoria.PUBLICO : cls.suggestedCategoria();
        NivelSigilo procSigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        NivelSigilo sigiloDoc = maxSigilo(procSigilo, cls.minSigilo());

        String key = "mni/" + processo.getId() + "/" + UUID.randomUUID();
        objectStorage.put(key, new ByteArrayInputStream(bytes), bytes.length, "application/pdf", Map.of());

        TipoDocumento tipoDocumento = tipoDocumentoMatcher.match(documento.nome(), documento.descricao()).orElse(null);

        DocumentoProcessual doc = DocumentoProcessual.builder()
                .processo(processo)
                .nomeOriginal(nome)
                .titulo(documento.descricao() != null && !documento.descricao().isBlank() ? documento.descricao() : nome)
                .contentType("application/pdf")
                .tamanhoBytes((long) bytes.length)
                .sha256(sha256)
                .storageBackend("LOCALFS")
                .storageUri(key)
                .tipoDocumento(tipoDocumento)
                .categoria(categoria)
                .nivelSigilo(sigiloDoc)
                .origemSistema(DocumentoOrigemSistema.MNI.name())
                .criadoEm(documento.dataHora() != null ? LocalDateTime.ofInstant(documento.dataHora(), ZoneOffset.UTC) : LocalDateTime.now())
                .build();
        if (tipoDocumento == null) {
            doc.setEstadoOperacional(DocumentoEstadoOperacional.AGUARDANDO_CLASSIFICACAO.name());
        }
        documentoRepository.save(doc);
    }

    @Transactional
    public DocumentoProcessual confirmarClassificacao(UUID documentoId, TipoDocumento tipoDocumento) {
        Objects.requireNonNull(documentoId, "documentoId");
        Objects.requireNonNull(tipoDocumento, "tipoDocumento");
        DocumentoProcessual doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado: " + documentoId));
        doc.setTipoDocumento(tipoDocumento);
        doc.setEstadoOperacional(null);
        return documentoRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentoProcessual> listarPendentesDeClassificacao() {
        return documentoRepository.findByEstadoOperacionalOrderByCriadoEmAsc(
                DocumentoEstadoOperacional.AGUARDANDO_CLASSIFICACAO.name());
    }

    private static String extractSampleText(DocumentContentValidator.ValidatedPdf validado) throws IOException {
        int paginas = Math.min(2, validado.numeroPaginas());
        if (paginas <= 0) {
            return "";
        }
        var stripper = new org.apache.pdfbox.text.PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(paginas);
        return stripper.getText(validado.document());
    }

    private static NivelSigilo maxSigilo(NivelSigilo a, NivelSigilo b) {
        NivelSigilo x = a == null ? NivelSigilo.PUBLICO : a;
        NivelSigilo y = b == null ? NivelSigilo.PUBLICO : b;
        return x.getNivel() >= y.getNivel() ? x : y;
    }
}
