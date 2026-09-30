package com.tcc.pjb.backend.core.comunicacao.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.model.entity.Processo;
import org.junit.jupiter.api.Test;

class CitacaoDespachoFisicoServiceTest {

    private final ExpedicaoJudicialRepository expedicaoRepository = mock(ExpedicaoJudicialRepository.class);
    private final CitacaoOficialJusticaQrMandadoService qrMandado = mock(CitacaoOficialJusticaQrMandadoService.class);
    private final CitacaoEditalCuradoriaService editalCuradoria = mock(CitacaoEditalCuradoriaService.class);
    private final CitacaoJudiciaryNotificationService judiciary = mock(CitacaoJudiciaryNotificationService.class);
    private final CitacaoExpedicaoNotificacaoService notificacao = mock(CitacaoExpedicaoNotificacaoService.class);
    private final CitacaoDespachoFisicoService service = new CitacaoDespachoFisicoService(
            expedicaoRepository, qrMandado, editalCuradoria, judiciary, notificacao);

    private ExpedicaoJudicial expedicao() {
        return new ExpedicaoJudicial(80L, "PROC-80", TipoComunicacaoJudicial.CITACAO_INICIAL,
                ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA, ExpedicaoJudicial.TipoDestinatario.PESSOA_FISICA,
                "Fulano", "12345678900", "CIVIL", "PRIMEIRO_GRAU", null, "hash-anterior", "fundamento");
    }

    private Processo processo() {
        Processo processo = new Processo();
        processo.setId(80L);
        return processo;
    }

    @Test
    void oficialJusticaMarcaPendenteGeraMandadoENotificaServidorQuandoHaExpedidor() {
        ExpedicaoJudicial exp = expedicao();
        exp.setServidorExpedidorId(7L);
        Processo proc = processo();

        service.despacharOficialJustica(exp, proc);

        assertThat(exp.getStatus()).isEqualTo(ExpedicaoJudicial.StatusExpedicao.PENDENTE_OFICIAL);
        verify(expedicaoRepository).save(exp);
        verify(qrMandado).gerarSeCabivel(exp);
        verify(judiciary).notificarServidor(7L, exp, proc, "Mandado físico gerado. Atribuir a Oficial de Justiça.");
    }

    @Test
    void oficialJusticaNaoNotificaServidorQuandoNaoHaExpedidor() {
        ExpedicaoJudicial exp = expedicao();

        service.despacharOficialJustica(exp, processo());

        assertThat(exp.getStatus()).isEqualTo(ExpedicaoJudicial.StatusExpedicao.PENDENTE_OFICIAL);
        verify(qrMandado).gerarSeCabivel(exp);
        verify(judiciary, never()).notificarServidor(anyLong(), any(), any(), anyString());
    }

    @Test
    void correioArDigitalDefineRastreioEStatusEGeraMandado() {
        ExpedicaoJudicial exp = expedicao();

        service.despacharCorreioArDigital(exp);

        assertThat(exp.getStatus()).isEqualTo(ExpedicaoJudicial.StatusExpedicao.REMETIDA_CORREIO);
        assertThat(exp.getCodigoRastreioCorreio()).startsWith("PJB");
        verify(expedicaoRepository).save(exp);
        verify(qrMandado).gerarSeCabivel(exp);
    }

    @Test
    void editalDefineNumeroEStatusERegistraCuradoriaENotifica() {
        ExpedicaoJudicial exp = expedicao();
        Processo proc = processo();

        service.despacharEdital(exp, proc);

        assertThat(exp.getStatus()).isEqualTo(ExpedicaoJudicial.StatusExpedicao.PUBLICADA_EDITAL);
        assertThat(exp.getNumeroEdital()).startsWith("EDT-");
        verify(expedicaoRepository).save(exp);
        verify(editalCuradoria).registrarNecessidadeSeAusente(exp);
        verify(notificacao).notificarEditalPublicado(exp, proc);
        verify(judiciary).notificarJuizEdital(eq(exp), eq(proc), startsWith("EDT-"));
    }
}
