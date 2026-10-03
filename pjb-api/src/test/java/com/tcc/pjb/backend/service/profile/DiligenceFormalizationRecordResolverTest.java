package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DiligenceFormalizationRecordResolverTest {

    private static final TelemetriaOperacionalCanal CANAL = TelemetriaOperacionalCanal.OFICIAL_JUSTICA;

    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository = mock(DiligenciaOperadorEncerramentoRepository.class);
    private final DiligenciaOperadorCertidaoRepository certidaoRepository = mock(DiligenciaOperadorCertidaoRepository.class);
    private final DiligenceFormalizationRecordResolver resolver = new DiligenceFormalizationRecordResolver(
            encerramentoRepository, certidaoRepository);

    private Usuario actor() {
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(88L);
        return u;
    }

    @Test
    void resolveEncerramentoPeloMaisRecenteQuandoSemId() {
        Usuario actor = actor();
        DiligenciaOperadorEncerramento encerramento = mock(DiligenciaOperadorEncerramento.class);
        when(encerramentoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, CANAL, "77"))
                .thenReturn(Optional.of(encerramento));

        assertThat(resolver.resolveEncerramento(actor, CANAL, "77", null)).isSameAs(encerramento);
    }

    @Test
    void resolveEncerramentoLancaQuandoAusente() {
        Usuario actor = actor();
        when(encerramentoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveEncerramento(actor, CANAL, "77", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveCertidaoPeloIdDoEncerramentoQuandoSemIdNoRequest() {
        Usuario actor = actor();
        DiligenciaOperadorEncerramento encerramento = mock(DiligenciaOperadorEncerramento.class);
        when(encerramento.getCertidaoId()).thenReturn(900L);
        DiligenciaOperadorCertidao certidao = mock(DiligenciaOperadorCertidao.class);
        when(certidaoRepository.findById(900L)).thenReturn(Optional.of(certidao));

        assertThat(resolver.resolveCertidao(actor, CANAL, "77", null, encerramento)).isSameAs(certidao);
    }
}
