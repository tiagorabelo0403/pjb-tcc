package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.sigilo.DocumentoSigiloClassifier;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.storage.ObjectWriteResult;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.document.DocumentContentValidator;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MarketplaceDocumentoPersistenceServiceTest {

    private DocumentoProcessualRepository documentoRepository;
    private ObjectStoragePort objectStorage;
    private MarketplaceDocumentoPersistenceService service;
    private Processo processo;

    @BeforeEach
    void setUp() throws Exception {
        documentoRepository = mock(DocumentoProcessualRepository.class);
        objectStorage = mock(ObjectStoragePort.class);
        when(objectStorage.put(any(), any(), anyLong(), any(), any()))
                .thenReturn(new ObjectWriteResult("k", URI.create("http://x/objects/k"), 10L, "sha256x", "sha384x"));
        when(documentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new MarketplaceDocumentoPersistenceService(documentoRepository, objectStorage,
                new DocumentContentValidator(), new DocumentoSigiloClassifier());
        processo = Processo.builder().id(1L).build();
    }

    @Test
    void tipoDocumentoAusenteLancaErroDeValidacao() {
        Attachment semTipo = Attachment.builder().name("x.pdf").content(pdf(1)).contentType("application/pdf").build();

        assertThatThrownBy(() -> service.persistirSeNovo(processo, semTipo, true))
                .isInstanceOf(ErroDeValidacaoException.class);

        verify(documentoRepository, never()).save(any());
    }

    @Test
    void conteudoAusenteToleradoQuandoPermitido() {
        Attachment semConteudo = Attachment.builder().tipoDocumento(TipoDocumento.PROCURACAO).build();

        Optional<String> resultado = service.persistirSeNovo(processo, semConteudo, true);

        assertThat(resultado).isEmpty();
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void conteudoAusenteRejeitadoQuandoNaoPermitido() {
        Attachment semConteudo = Attachment.builder().tipoDocumento(TipoDocumento.PROCURACAO).build();

        assertThatThrownBy(() -> service.persistirSeNovo(processo, semConteudo, false))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("Formato");

        verify(documentoRepository, never()).save(any());
    }

    @Test
    void duplicataPorSha256EhIgnoradaSemGravar() {
        when(documentoRepository.existsByProcessoIdAndSha256(eq(1L), any())).thenReturn(true);
        Attachment repetido = attachment(TipoDocumento.PETICAO_INICIAL);

        Optional<String> resultado = service.persistirSeNovo(processo, repetido, false);

        assertThat(resultado).isEmpty();
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void documentoValidoEhPersistidoComStorageExternalizadoENaoInline() {
        Attachment anexo = attachment(TipoDocumento.PETICAO_INICIAL);

        Optional<String> resultado = service.persistirSeNovo(processo, anexo, false);

        assertThat(resultado).contains(TipoDocumento.PETICAO_INICIAL.name());

        ArgumentCaptor<DocumentoProcessual> captor = ArgumentCaptor.forClass(DocumentoProcessual.class);
        verify(documentoRepository).save(captor.capture());
        DocumentoProcessual salvo = captor.getValue();
        assertThat(salvo.getTipoDocumento()).isEqualTo(TipoDocumento.PETICAO_INICIAL);
        assertThat(salvo.getStorageBackend()).isEqualTo("LOCALFS");
        assertThat(salvo.getStorageUri()).startsWith("marketplace/1/");
        assertThat(salvo.getPdf()).isNull();
        assertThat(salvo.getOrigemSistema()).isEqualTo("MARKETPLACE_API");
    }

    @Test
    void categoriaNaoSugeridaPeloClassificadorRecebeDefaultPublico() {
        // PDF em branco, sem texto e sem nome com marcador de sensibilidade —
        // DocumentoSigiloClassifier nunca sugere PESSOAL para esse caso.
        Attachment anexo = attachment(TipoDocumento.PETICAO_INICIAL);

        service.persistirSeNovo(processo, anexo, false);

        ArgumentCaptor<DocumentoProcessual> captor = ArgumentCaptor.forClass(DocumentoProcessual.class);
        verify(documentoRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoria()).isEqualTo(DocumentoCategoria.PUBLICO);
    }

    private Attachment attachment(TipoDocumento tipo) {
        return Attachment.builder()
                .tipoDocumento(tipo)
                .name(tipo.name().toLowerCase() + ".pdf")
                .contentType("application/pdf")
                .content(pdf(1))
                .build();
    }

    private static byte[] pdf(int paginas) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < paginas; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar PDF de teste", e);
        }
    }
}
