package com.tcc.pjb.backend.service.prazo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.prazo.PrazoCartorioPainelResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalCiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoCienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicaoCiencia;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.model.repository.CienciaProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazoCartorioPainelServiceTest {

    private final CienciaProcessualRepository cienciaProcessualRepository = mock(CienciaProcessualRepository.class);
    private final PrazoCartorioPainelService service = new PrazoCartorioPainelService(
            cienciaProcessualRepository, mock(ProcessoRepository.class), mock(ProcessoPrazoApplicationService.class));

    private CienciaProcessual ciencia(Long processoId, String numeroProcesso, Instant dataExpiracaoAlvo) {
        Processo processo = new Processo();
        processo.setId(processoId);
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Instant diesAQuo = dataExpiracaoAlvo.minus(TipoCienciaProcessual.INTIMACAO_DECISAO.prazoRespostaDias(), ChronoUnit.DAYS);
        return new CienciaProcessual(
                processo, usuario, TipoCienciaProcessual.INTIMACAO_DECISAO, CanalCiencia.DJE_TRIBUNAL,
                GrauJurisdicaoCiencia.PRIMEIRO_GRAU, "COMUM_ORDINARIO", "PJB", null, null, null,
                numeroProcesso, "PJB-EDICAO", "hash", diesAQuo, diesAQuo, diesAQuo);
    }

    @Test
    void classificaVencidosEVencendoEm7E15DiasCorretamente() {
        Instant agora = Instant.now();
        CienciaProcessual vencida = ciencia(1L, "PROC-VENCIDA", agora.minus(2, ChronoUnit.DAYS));
        CienciaProcessual em5Dias = ciencia(2L, "PROC-5D", agora.plus(5, ChronoUnit.DAYS));
        CienciaProcessual em10Dias = ciencia(3L, "PROC-10D", agora.plus(10, ChronoUnit.DAYS));
        CienciaProcessual em30Dias = ciencia(4L, "PROC-30D", agora.plus(30, ChronoUnit.DAYS));

        when(cienciaProcessualRepository.findPendentesPorVaraAteData(eq("1ª Vara Cível"), any(), any()))
                .thenReturn(List.of(vencida, em5Dias, em10Dias));

        PrazoCartorioPainelResponse resultado = service.painelPorVara("1ª Vara Cível", 15);

        assertThat(resultado.vara()).isEqualTo("1ª Vara Cível");
        assertThat(resultado.totalPendentes()).isEqualTo(3);
        assertThat(resultado.vencidos()).isEqualTo(1);
        assertThat(resultado.vencendoEm7Dias()).isEqualTo(2);
        assertThat(resultado.vencendoEm15Dias()).isEqualTo(3);
        assertThat(resultado.itens()).extracting("numeroProcesso")
                .containsExactly("PROC-VENCIDA", "PROC-5D", "PROC-10D");
    }

    @Test
    void retornaPainelVazioQuandoVaraNaoTemPrazosPendentes() {
        when(cienciaProcessualRepository.findPendentesPorVaraAteData(eq("Vara Vazia"), any(), any()))
                .thenReturn(List.of());

        PrazoCartorioPainelResponse resultado = service.painelPorVara("Vara Vazia", 15);

        assertThat(resultado.totalPendentes()).isZero();
        assertThat(resultado.vencidos()).isZero();
        assertThat(resultado.itens()).isEmpty();
    }
}
