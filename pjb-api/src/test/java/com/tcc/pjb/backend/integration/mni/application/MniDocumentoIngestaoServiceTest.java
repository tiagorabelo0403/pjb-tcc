package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.sigilo.DocumentoSigiloClassifier;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.storage.ObjectWriteResult;
import com.tcc.pjb.backend.integration.mni.adapter.MniDocumentoParsed;
import com.tcc.pjb.backend.integration.mni.adapter.MniTipoDocumentoKeywordMatcher;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.document.DocumentoEstadoOperacional;
import com.tcc.pjb.backend.model.entity.enums.document.DocumentoOrigemSistema;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.document.DocumentContentValidator;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MniDocumentoIngestaoServiceTest {

    private DocumentoProcessualRepository documentoRepository;
    private ObjectStoragePort objectStorage;
    private MniDocumentoIngestaoService service;
    private Processo processo;

    @BeforeEach
    void setUp() throws Exception {
        documentoRepository = mock(DocumentoProcessualRepository.class);
        objectStorage = mock(ObjectStoragePort.class);
        when(objectStorage.put(any(), any(), anyLong(), any(), any()))
                .thenReturn(new ObjectWriteResult("k", URI.create("http://x/objects/k"), 10L, "sha256x", "sha384x"));
        when(documentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new MniDocumentoIngestaoService(documentoRepository, objectStorage,
                new DocumentContentValidator(), new DocumentoSigiloClassifier(), new MniTipoDocumentoKeywordMatcher());
        processo = Processo.builder().id(1L).build();
    }

    @Test
    void documentoComTipoReconhecidoEhIngeridoComTipoDocumentoDireto() {
        MniDocumentoParsed documento = new MniDocumentoParsed("peticao_inicial.pdf", "Peticao Inicial",
                "application/pdf", pdf(1), null);

        service.ingestar(processo, List.of(documento));

        ArgumentCaptor<DocumentoProcessual> captor = ArgumentCaptor.forClass(DocumentoProcessual.class);
        verify(documentoRepository).save(captor.capture());
        DocumentoProcessual salvo = captor.getValue();
        assertThat(salvo.getTipoDocumento()).isEqualTo(TipoDocumento.PETICAO_INICIAL);
        assertThat(salvo.getEstadoOperacional()).isNull();
        assertThat(salvo.getOrigemSistema()).isEqualTo(DocumentoOrigemSistema.MNI.name());
        assertThat(salvo.getStorageBackend()).isEqualTo("LOCALFS");
        assertThat(salvo.getStorageUri()).startsWith("mni/1/");
        assertThat(salvo.getPdf()).isNull();
    }

    @Test
    void documentoSemTipoReconhecivelEhIngeridoComoAguardandoClassificacaoSemPerderConteudo() throws Exception {
        MniDocumentoParsed documento = new MniDocumentoParsed("anexo_diverso.pdf", "Documento diverso",
                "application/pdf", pdf(1), null);

        service.ingestar(processo, List.of(documento));

        ArgumentCaptor<DocumentoProcessual> captor = ArgumentCaptor.forClass(DocumentoProcessual.class);
        verify(documentoRepository).save(captor.capture());
        DocumentoProcessual salvo = captor.getValue();
        assertThat(salvo.getTipoDocumento()).isNull();
        assertThat(salvo.getEstadoOperacional()).isEqualTo(DocumentoEstadoOperacional.AGUARDANDO_CLASSIFICACAO.name());
        assertThat(salvo.getSha256()).isNotBlank();
        verify(objectStorage).put(any(), any(), anyLong(), any(), any());
    }

    @Test
    void documentoDuplicadoPorSha256NoMesmoProcessoEhIgnorado() {
        when(documentoRepository.existsByProcessoIdAndSha256(eq(1L), any())).thenReturn(true);
        MniDocumentoParsed documento = new MniDocumentoParsed("peticao_inicial.pdf", "Peticao Inicial",
                "application/pdf", pdf(1), null);

        service.ingestar(processo, List.of(documento));

        verify(documentoRepository, never()).save(any());
    }

    @Test
    void documentoComPdfCorrompidoEhPuladoSemDerrubarOsDemais() {
        MniDocumentoParsed corrompido = new MniDocumentoParsed("corrompido.pdf", "Corrompido",
                "application/pdf", "não é um pdf de verdade".getBytes(), null);
        MniDocumentoParsed valido = new MniDocumentoParsed("peticao_inicial.pdf", "Peticao Inicial",
                "application/pdf", pdf(1), null);

        service.ingestar(processo, List.of(corrompido, valido));

        verify(documentoRepository, times(1)).save(any());
    }

    @Test
    void confirmarClassificacaoAtualizaTipoDocumentoELimpaEstadoPendente() {
        java.util.UUID id = java.util.UUID.randomUUID();
        DocumentoProcessual pendente = DocumentoProcessual.builder().id(id).build();
        pendente.setEstadoOperacional(DocumentoEstadoOperacional.AGUARDANDO_CLASSIFICACAO.name());
        when(documentoRepository.findById(id)).thenReturn(java.util.Optional.of(pendente));

        DocumentoProcessual atualizado = service.confirmarClassificacao(id, TipoDocumento.CERTIDAO_OBITO);

        assertThat(atualizado.getTipoDocumento()).isEqualTo(TipoDocumento.CERTIDAO_OBITO);
        assertThat(atualizado.getEstadoOperacional()).isNull();
        verify(documentoRepository).save(pendente);
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
