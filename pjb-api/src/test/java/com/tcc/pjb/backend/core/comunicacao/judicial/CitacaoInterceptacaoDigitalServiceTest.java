package com.tcc.pjb.backend.core.comunicacao.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.MotorInterceptacaoAtiva;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHsmProperties;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.ReciboCitacaoHsm;
import com.tcc.pjb.backend.model.entity.Processo;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class CitacaoInterceptacaoDigitalServiceTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<MotorInterceptacaoAtiva> motorProvider = mock(ObjectProvider.class);
    private final PjbHsmProperties hsmProperties = mock(PjbHsmProperties.class);
    private final CitacaoSefazCadastroEnrichmentService sefaz = mock(CitacaoSefazCadastroEnrichmentService.class);
    private final CitacaoInterceptacaoDigitalService service = new CitacaoInterceptacaoDigitalService(
            motorProvider, hsmProperties, sefaz);

    private ExpedicaoJudicial expedicao() {
        return new ExpedicaoJudicial(80L, "PROC-80", TipoComunicacaoJudicial.CITACAO_INICIAL,
                ModalidadeExpedicaoJudicial.DIGITAL_GOVBR_PUSH, ExpedicaoJudicial.TipoDestinatario.PESSOA_FISICA,
                "Fulano", "12345678900", "CIVIL", "PRIMEIRO_GRAU", null, "hash-anterior", "fundamento");
    }

    private CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica pessoaFisica() {
        return new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                "12345678900", "Fulano", null, "fulano@exemplo.com", "11999999999", false, false);
    }

    private Processo processo() {
        Processo processo = new Processo();
        processo.setId(80L);
        return processo;
    }

    private ReciboCitacaoHsm recibo(String canalVencedor) {
        return new ReciboCitacaoHsm(UUID.randomUUID(), null, null, "trilha", canalVencedor,
                "12345678900", "PROC-80", 80L, "hash", true, List.of(), List.of(), null);
    }

    @Test
    void semMotorRetornaSemAcaoENaoEnriquece() {
        when(motorProvider.getIfAvailable()).thenReturn(null);

        CitacaoInterceptacaoDigitalService.Resultado r = service.interceptar(
                expedicao(), pessoaFisica(), "ato".getBytes(StandardCharsets.UTF_8), processo());

        assertThat(r.tipo()).isEqualTo(CitacaoInterceptacaoDigitalService.Tipo.SEM_ACAO);
        verify(sefaz, never()).enriquecerExpedicaoComCadastroSefaz(any(), any());
    }

    @Test
    void interceptacaoEntregueRetornaEntregueComRecibo() {
        MotorInterceptacaoAtiva motor = mock(MotorInterceptacaoAtiva.class);
        when(motorProvider.getIfAvailable()).thenReturn(motor);
        ReciboCitacaoHsm entregue = recibo("GOV_BR_PUSH");
        when(motor.interceptarComViasSugeridas(any(), any(), any())).thenReturn(entregue);

        CitacaoInterceptacaoDigitalService.Resultado r = service.interceptar(
                expedicao(), pessoaFisica(), "ato".getBytes(StandardCharsets.UTF_8), processo());

        assertThat(r.tipo()).isEqualTo(CitacaoInterceptacaoDigitalService.Tipo.ENTREGUE);
        assertThat(r.recibo()).isSameAs(entregue);
        verify(motor, never()).acionarFallbackFisicoSeNecessario(any(), anyString());
    }

    @Test
    void interceptacaoNaoEntregueAcionaFallbackERetornaSemAcao() {
        MotorInterceptacaoAtiva motor = mock(MotorInterceptacaoAtiva.class);
        when(motorProvider.getIfAvailable()).thenReturn(motor);
        ReciboCitacaoHsm naoEntregue = recibo(null);
        when(motor.interceptarComViasSugeridas(any(), any(), any())).thenReturn(naoEntregue);

        CitacaoInterceptacaoDigitalService.Resultado r = service.interceptar(
                expedicao(), pessoaFisica(), "ato".getBytes(StandardCharsets.UTF_8), processo());

        assertThat(r.tipo()).isEqualTo(CitacaoInterceptacaoDigitalService.Tipo.SEM_ACAO);
        verify(motor).acionarFallbackFisicoSeNecessario(eq(naoEntregue), anyString());
    }
}
