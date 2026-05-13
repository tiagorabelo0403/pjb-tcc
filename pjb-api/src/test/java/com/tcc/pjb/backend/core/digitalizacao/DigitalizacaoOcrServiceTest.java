package com.tcc.pjb.backend.core.digitalizacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoJob;
import com.tcc.pjb.backend.model.repository.DigitalizacaoJobRepository;
import com.tcc.pjb.backend.model.repository.DigitalizacaoPaginaRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DigitalizacaoOcrServiceTest {

    @Test
    void deveMarcarRevisaoQuandoConfiancaBaixa() {
        DigitalizacaoJobRepository jobRepository = mock(DigitalizacaoJobRepository.class);
        DigitalizacaoPaginaRepository paginaRepository = mock(DigitalizacaoPaginaRepository.class);
        DigitalizacaoJob job = DigitalizacaoJob.builder().id(1L).status("PENDENTE").paginasProcessadas(0).ocrEngine("TESSERACT_5").idioma("por").createdAt(Instant.now()).build();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paginaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DigitalizacaoOcrService service = new DigitalizacaoOcrService(jobRepository, paginaRepository, (img, idioma) -> new OcrPageResult("sentenca", 50.0), new PecaClassificadorService(), mock(AuditLedgerService.class), new DigitalizacaoProperties(true, 70.0, "por", 20, 60, 300000), mock(ReadAfterWriteConsistencyPolicy.class), new SimpleMeterRegistry());
        var result = service.processar(1L, List.of(new byte[]{1,2,3}));
        assertThat(result.paginasComRevisao()).isEqualTo(1);
        assertThat(result.confiancaMedia()).isEqualTo(50.0);
    }
}
