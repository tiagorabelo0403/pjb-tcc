package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.document.DocumentContentService;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Tag("integration")
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false",
        "pjb.outbox.ingress.enabled=false"
})
class MarketplaceDocumentoComplementarServiceIT extends PjbIntegrationTestBase {

    @Autowired
    private MarketplaceDocumentoComplementarService service;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private DocumentoProcessualRepository documentoRepository;

    @Autowired
    private DocumentContentService documentContentService;

    @MockitoBean
    private MarketplaceGovernanceService governanceService;

    @Test
    void complementaDocumentoGravaConteudoRealNoObjectStorageELeDeVolta() throws Exception {
        Processo processo = Processo.builder()
                .numeroUnificado("0009999-40.2026.8.06.0001")
                .numeroProcesso("0009999-40.2026.8.06.0001")
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .connectorSystem("MARKETPLACE_API")
                .connectorClientId("client-it")
                .connectorProtocolReference("client-it:ref-40")
                .connectorSubmissionStatus("PENDENTE_DOCUMENTACAO")
                .dataCriacao(LocalDateTime.now())
                .build();
        processo = processoRepository.save(processo);

        byte[] pdfBytes = pdf(1);
        Attachment anexo = Attachment.builder()
                .name("peticao.pdf")
                .contentType("application/pdf")
                .tipoDocumento(TipoDocumento.PETICAO_INICIAL)
                .content(pdfBytes)
                .build();

        var resp = service.complementar(processo.getId(), List.of(anexo), "client-it");

        assertThat(resp.documentosRecebidos()).contains(TipoDocumento.PETICAO_INICIAL.name());

        var salvos = documentoRepository.findByProcessoId(processo.getId());
        assertThat(salvos).hasSize(1);
        assertThat(salvos.get(0).getTipoDocumento()).isEqualTo(TipoDocumento.PETICAO_INICIAL);
        assertThat(salvos.get(0).getStorageBackend()).isEqualTo("LOCALFS");
        assertThat(salvos.get(0).getStorageUri()).isNotBlank();
        assertThat(salvos.get(0).getStorageUri()).startsWith("marketplace/" + processo.getId() + "/");
        assertThat(salvos.get(0).getPdf()).isNull();
        assertThat(salvos.get(0).getCategoria()).isEqualTo(DocumentoCategoria.PUBLICO);

        var lido = documentContentService.resolvePdf(salvos.get(0));
        assertThat(lido.inlineDb()).isFalse();
        assertThat(lido.contentLength()).isEqualTo(pdfBytes.length);
        assertThat(lido.resource().getInputStream().readAllBytes()).isEqualTo(pdfBytes);
    }

    private static byte[] pdf(int paginas) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < paginas; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
