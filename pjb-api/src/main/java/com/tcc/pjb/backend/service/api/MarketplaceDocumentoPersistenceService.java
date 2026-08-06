package com.tcc.pjb.backend.service.api;

import com.tcc.pjb.backend.core.security.sigilo.DocumentoSigiloClassifier;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.document.DocumentContentValidator;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class MarketplaceDocumentoPersistenceService {

    private final DocumentoProcessualRepository documentoRepository;
    private final ObjectStoragePort objectStorage;
    private final DocumentContentValidator contentValidator;
    private final DocumentoSigiloClassifier sigiloClassifier;

    public MarketplaceDocumentoPersistenceService(DocumentoProcessualRepository documentoRepository,
                                                  ObjectStoragePort objectStorage,
                                                  DocumentContentValidator contentValidator,
                                                  DocumentoSigiloClassifier sigiloClassifier) {
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.objectStorage = Objects.requireNonNull(objectStorage);
        this.contentValidator = Objects.requireNonNull(contentValidator);
        this.sigiloClassifier = Objects.requireNonNull(sigiloClassifier);
    }

    public Optional<String> persistirSeNovo(Processo processo, Attachment attachment, boolean permitirAusenciaDeConteudo) {
        if (attachment.getTipoDocumento() == null) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "tipoDocumento")
                    .addMetadado("motivo", "tipoDocumento obrigatório para cada documento enviado");
        }
        byte[] bytes = attachment.getContent();
        if (permitirAusenciaDeConteudo && (bytes == null || bytes.length == 0)) {
            return Optional.empty();
        }
        String nome = attachment.getName();
        contentValidator.validarTamanho(bytes == null ? 0 : bytes.length, nome);
        contentValidator.validarExtensaoOuContentType(nome, attachment.getContentType());

        String sha256 = Hashes.sha256HexBytes(bytes);
        if (documentoRepository.existsByProcessoIdAndSha256(processo.getId(), sha256)) {
            return Optional.empty();
        }

        String sampleText;
        int numeroPaginas;
        try (var validado = contentValidator.validarEstruturaPdf(bytes, nome)) {
            numeroPaginas = validado.numeroPaginas();
            sampleText = extractSampleText(validado.document(), Math.min(2, numeroPaginas));
        } catch (IOException e) {
            throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nome, e)
                    .addMetadado("erro_tecnico", e.getClass().getSimpleName());
        }

        var cls = sigiloClassifier.classify(nome, sampleText);
        DocumentoCategoria categoria = cls.suggestedCategoria() == null ? DocumentoCategoria.PUBLICO : cls.suggestedCategoria();
        NivelSigilo procSigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        NivelSigilo sigiloDoc = maxSigilo(procSigilo, cls.minSigilo());

        String key = "marketplace/" + processo.getId() + "/" + UUID.randomUUID();
        try {
            objectStorage.put(key, new ByteArrayInputStream(bytes), bytes.length, attachment.getContentType(), Map.of());
        } catch (IOException e) {
            throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nome, e)
                    .addMetadado("erro_tecnico", "falha ao gravar no armazenamento de objetos");
        }

        DocumentoProcessual doc = DocumentoProcessual.builder()
                .processo(processo)
                .nomeOriginal(nome)
                .titulo(nome)
                .contentType(attachment.getContentType())
                .tamanhoBytes((long) bytes.length)
                .sha256(sha256)
                .storageBackend("LOCALFS")
                .storageUri(key)
                .tipoDocumento(attachment.getTipoDocumento())
                .categoria(categoria)
                .nivelSigilo(sigiloDoc)
                .origemSistema("MARKETPLACE_API")
                .criadoEm(LocalDateTime.now())
                .build();
        documentoRepository.save(doc);
        return Optional.of(attachment.getTipoDocumento().name());
    }

    private static String extractSampleText(PDDocument pdf, int pages) throws IOException {
        if (pdf == null || pages <= 0) {
            return "";
        }
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(pages);
        return stripper.getText(pdf);
    }

    private static NivelSigilo maxSigilo(NivelSigilo a, NivelSigilo b) {
        NivelSigilo x = a == null ? NivelSigilo.PUBLICO : a;
        NivelSigilo y = b == null ? NivelSigilo.PUBLICO : b;
        return x.getNivel() >= y.getNivel() ? x : y;
    }
}
