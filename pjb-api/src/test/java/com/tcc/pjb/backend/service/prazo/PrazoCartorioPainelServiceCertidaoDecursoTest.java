package com.tcc.pjb.backend.service.prazo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.prazo.PrazoCertidaoDecursoLoteResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalCiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusCiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoCienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicaoCiencia;
import com.tcc.pjb.backend.model.repository.CienciaProcessualRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazoCartorioPainelServiceCertidaoDecursoTest {

    private final CienciaProcessualRepository cienciaProcessualRepository = mock(CienciaProcessualRepository.class);
    private final PrazoCartorioPainelService service = new PrazoCartorioPainelService(cienciaProcessualRepository);

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
    void certificaDecursoDeTodasAsCienciasVencidasDaVaraEEmiteTextoDeCertidao() {
        CienciaProcessual vencida1 = ciencia(1L, "PROC-1", Instant.now().minus(3, ChronoUnit.DAYS));
        CienciaProcessual vencida2 = ciencia(2L, "PROC-2", Instant.now().minus(1, ChronoUnit.DAYS));
        when(cienciaProcessualRepository.findPendentesPorVaraAteData(eq("1ª Vara Cível"), any(), any()))
                .thenReturn(List.of(vencida1, vencida2));

        PrazoCertidaoDecursoLoteResponse resultado = service.certificarDecursoEmLote("1ª Vara Cível");

        assertThat(resultado.vara()).isEqualTo("1ª Vara Cível");
        assertThat(resultado.totalCertificadas()).isEqualTo(2);
        assertThat(vencida1.getStatus()).isEqualTo(StatusCiencia.FICTA_CONFIRMADA);
        assertThat(vencida2.getStatus()).isEqualTo(StatusCiencia.FICTA_CONFIRMADA);
        assertThat(resultado.certidoes()).extracting("numeroProcesso").containsExactly("PROC-1", "PROC-2");
        assertThat(resultado.certidoes().get(0).textoCertidao())
                .contains("CERTIDÃO DE DECURSO DE PRAZO")
                .contains("PROC-1")
                .contains("art. 231 do CPC");
        verify(cienciaProcessualRepository).saveAll(List.of(vencida1, vencida2));
    }

    @Test
    void naoGeraCertidaoQuandoVaraNaoTemCienciaVencidaPendente() {
        when(cienciaProcessualRepository.findPendentesPorVaraAteData(eq("Vara Vazia"), any(), any()))
                .thenReturn(List.of());

        PrazoCertidaoDecursoLoteResponse resultado = service.certificarDecursoEmLote("Vara Vazia");

        assertThat(resultado.totalCertificadas()).isZero();
        assertThat(resultado.certidoes()).isEmpty();
    }
}
