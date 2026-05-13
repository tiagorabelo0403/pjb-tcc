package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateDocumentLinkRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidaoDocumento;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoDocumentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiligenceCertificateEvidenceServiceTest {

    @Test
    void vinculaDocumentosDaMesmaPastaProcessual() {
        DiligenciaOperadorCertidaoRepository certidaoRepository = Mockito.mock(DiligenciaOperadorCertidaoRepository.class);
        DiligenciaOperadorCertidaoDocumentoRepository vinculoRepository = Mockito.mock(DiligenciaOperadorCertidaoDocumentoRepository.class);
        DocumentoProcessualRepository documentoRepository = Mockito.mock(DocumentoProcessualRepository.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        DiligenceCertificateEvidenceService service = new DiligenceCertificateEvidenceService(certidaoRepository, vinculoRepository, documentoRepository, processoRepository, authorizationService);
        Processo processo = new Processo();
        processo.setId(501L);
        DiligenciaOperadorCertidao certidao = DiligenciaOperadorCertidao.builder().id(900L).processoId(501L).build();
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();
        DocumentoProcessual a = DocumentoProcessual.builder().id(docA).processo(processo).titulo("Certidão geolocalizada").sha256("aa".repeat(32)).criadoEm(LocalDateTime.parse("2026-03-11T18:00:00")).build();
        DocumentoProcessual b = DocumentoProcessual.builder().id(docB).processo(processo).titulo("Fotografias de diligência").sha256("bb".repeat(32)).criadoEm(LocalDateTime.parse("2026-03-11T18:05:00")).build();
        List<DiligenciaOperadorCertidaoDocumento> store = new ArrayList<>();
        when(certidaoRepository.findById(900L)).thenReturn(Optional.of(certidao));
        when(processoRepository.findById(501L)).thenReturn(Optional.of(processo));
        when(documentoRepository.findById(docA)).thenReturn(Optional.of(a));
        when(documentoRepository.findById(docB)).thenReturn(Optional.of(b));
        when(vinculoRepository.existsByCertidaoIdAndDocumentoId(any(), any())).thenReturn(false);
        when(vinculoRepository.save(any())).thenAnswer(inv -> {
            DiligenciaOperadorCertidaoDocumento entity = inv.getArgument(0);
            entity.setId((long) (store.size() + 1));
            entity.setCreatedAt(java.time.Instant.parse("2026-03-11T18:10:00Z"));
            store.add(entity);
            return entity;
        });
        when(vinculoRepository.findByCertidaoIdOrderByCreatedAtDesc(900L)).thenAnswer(inv -> List.copyOf(store));

        var response = service.bind(900L, new DiligenceCertificateDocumentLinkRequest(List.of(docA, docB)));

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().certidaoId()).isEqualTo(900L);
        assertThat(response.stream().map(r -> r.documentoId()).toList()).containsExactlyInAnyOrder(docA, docB);
    }
}
