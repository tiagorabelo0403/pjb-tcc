package com.tcc.pjb.backend.service.oficial_justica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OficialJusticaProdutividadeServiceTest {

    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository = mock(DiligenciaOperadorEncerramentoRepository.class);
    private final OficialJusticaProdutividadeService service = new OficialJusticaProdutividadeService(encerramentoRepository);

    private DiligenciaOperadorEncerramento encerramento(Long id, DiligenciaEncerramentoTipo outcome, Instant createdAt) {
        return DiligenciaOperadorEncerramento.builder().id(id).operatorUserId(20L)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA).diligenceReference("MANDADO-" + id)
                .outcome(outcome).processoId(80L + id).processoNumero("PROC-" + id)
                .idempotencyKey("k" + id).executionDigestSha256("digest" + id).createdAt(createdAt).build();
    }

    @Test
    void calculaBreakdownPorOutcomeETaxaDeSucesso() {
        when(encerramentoRepository.findByOperatorUserIdAndCanalAndCreatedAtAfterOrderByCreatedAtDesc(eq(20L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any()))
                .thenReturn(List.of(
                        encerramento(3L, DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO, Instant.parse("2026-04-10T12:00:00Z")),
                        encerramento(2L, DiligenciaEncerramentoTipo.CUMPRIMENTO_FRUSTRADO, Instant.parse("2026-04-10T08:00:00Z")),
                        encerramento(1L, DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO, Instant.parse("2026-04-09T08:00:00Z"))));

        OficialJusticaProdutividadePainelResponse painel = service.painel(20L, 30);

        assertThat(painel.total()).isEqualTo(3);
        assertThat(painel.porOutcome()).containsEntry("CUMPRIMENTO_POSITIVO", 2).containsEntry("CUMPRIMENTO_FRUSTRADO", 1);
        assertThat(painel.taxaSucesso()).isEqualTo(2.0 / 3);
        assertThat(painel.intervaloMedioHoras()).isEqualTo(14.0);
    }

    @Test
    void retornaTaxaNulaSemNenhumEncerramento() {
        when(encerramentoRepository.findByOperatorUserIdAndCanalAndCreatedAtAfterOrderByCreatedAtDesc(eq(20L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any()))
                .thenReturn(List.of());

        OficialJusticaProdutividadePainelResponse painel = service.painel(20L, 30);

        assertThat(painel.total()).isZero();
        assertThat(painel.taxaSucesso()).isNull();
        assertThat(painel.intervaloMedioHoras()).isNull();
    }
}
