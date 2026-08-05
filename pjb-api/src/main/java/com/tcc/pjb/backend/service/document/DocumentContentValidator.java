package com.tcc.pjb.backend.service.document;

import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import java.io.IOException;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class DocumentContentValidator {

    private static final long LIMITE_BYTES = 5L * 1024L * 1024L;

    public record ValidatedPdf(PDDocument document, int numeroPaginas) implements java.io.Closeable {
        @Override
        public void close() throws IOException {
            document.close();
        }
    }

    public void validarTamanho(long tamanhoBytes, String nomeOriginal) {
        if (tamanhoBytes <= 0) {
            throw new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, "arquivo")
                    .addMetadado("motivo", "arquivo ausente ou vazio");
        }
        if (tamanhoBytes > LIMITE_BYTES) {
            throw new ErroDeValidacaoException(TipoErroValidacao.TAMANHO_EXCEDIDO, nomeOriginal)
                    .addMetadado("tamanho_atual", tamanhoBytes)
                    .addMetadado("tamanho_limite", LIMITE_BYTES);
        }
    }

    public void validarExtensaoOuContentType(String nomeOriginal, String contentTypeDeclarado) {
        boolean pdf = (contentTypeDeclarado != null && contentTypeDeclarado.equals(MediaType.APPLICATION_PDF_VALUE))
                || (nomeOriginal != null && nomeOriginal.toLowerCase(Locale.ROOT).endsWith(".pdf"));
        if (!pdf) {
            throw new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, nomeOriginal)
                    .addMetadado("tipo_recebido", contentTypeDeclarado)
                    .addMetadado("tipo_esperado", MediaType.APPLICATION_PDF_VALUE);
        }
    }

    public ValidatedPdf validarEstruturaPdf(byte[] bytes, String nomeOriginal) throws IOException {
        PDDocument pdf = Loader.loadPDF(bytes);
        try {
            if (pdf.isEncrypted()) {
                throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_PROTEGIDO, nomeOriginal)
                        .addMetadado("motivo", "Documento possui senha");
            }
            int n = pdf.getNumberOfPages();
            if (n <= 0) {
                throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nomeOriginal)
                        .addMetadado("motivo", "PDF com 0 páginas");
            }
            return new ValidatedPdf(pdf, n);
        } catch (RuntimeException e) {
            pdf.close();
            throw e;
        }
    }
}
