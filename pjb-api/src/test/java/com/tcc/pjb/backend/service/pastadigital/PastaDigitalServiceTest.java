package com.tcc.pjb.backend.service.pastadigital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.sigilo.DocumentoSigiloClassifier;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PastaDigitalServiceTest {

    private ProcessoRepository processoRepository;
    private DocumentoProcessualRepository documentoRepository;
    private DocumentoPaginaRepository paginaRepository;
    private PjbAuthorizationService authorizationService;
    private PastaDigitalService service;

    @BeforeEach
    void setUp() {
        processoRepository = mock(ProcessoRepository.class);
        documentoRepository = mock(DocumentoProcessualRepository.class);
        paginaRepository = mock(DocumentoPaginaRepository.class);
        authorizationService = mock(PjbAuthorizationService.class);
        Processo processo = Processo.builder().id(1L).build();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        doNothing().when(authorizationService).requireWriteProcesso(processo);
        when(documentoRepository.existsByProcessoIdAndSha256(any(), any())).thenReturn(false);
        when(documentoRepository.save(any())).thenAnswer(inv -> {
            DocumentoProcessual d = inv.getArgument(0);
            d.setId(java.util.UUID.randomUUID());
            return d;
        });
        service = new PastaDigitalService(processoRepository, documentoRepository, paginaRepository,
                authorizationService, new DocumentoSigiloClassifier());
    }

    @Test
    void anexaPdfValidoComUmaPaginaComSucesso() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "peticao.pdf",
                "application/pdf", pdfComPaginas(1));

        var resp = service.anexarDocumentoPdf(1L, arquivo, "Peticao", 10L, "API", null, null);

        assertThat(resp.getNumeroPaginas()).isEqualTo(1);
        assertThat(resp.getNomeOriginal()).isEqualTo("peticao.pdf");
        verify(documentoRepository).save(any());
    }

    @Test
    void rejeitaArquivoVazio() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "vazio.pdf",
                "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("Formato");
    }

    @Test
    void rejeitaArquivoAcimaDoLimiteDeTamanho() {
        byte[] grande = new byte[6 * 1024 * 1024];
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "grande.pdf",
                "application/pdf", grande);

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("excede o limite");
    }

    @Test
    void rejeitaArquivoNaoPdf() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.png",
                "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("Formato");
    }

    @Test
    void rejeitaPdfCriptografado() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "protegido.pdf",
                "application/pdf", pdfCriptografado());

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("corrompido");
    }

    @Test
    void rejeitaPdfSemPaginas() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "corrompido.pdf",
                "application/pdf", pdfComPaginas(0));

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("corrompido");
    }

    @Test
    void rejeitaDuplicataPorSha256() throws Exception {
        when(documentoRepository.existsByProcessoIdAndSha256(any(), any())).thenReturn(true);
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "repetido.pdf",
                "application/pdf", pdfComPaginas(1));

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("duplicado");
    }

    private static byte[] pdfComPaginas(int quantidade) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < quantidade; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] pdfCriptografado() throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy("owner-pass", "user-pass", ap);
            doc.protect(spp);
            doc.save(out);
            return out.toByteArray();
        }
    }
}
