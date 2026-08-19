package com.tcc.pjb.backend.core.comunicacao.judicial;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.MotorInterceptacaoAtiva;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHsmProperties;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.SefazNfeCadastroResolver;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.platform.jusos.v2.notificacao.NotificacaoInteligentePJB;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import com.tcc.pjb.backend.service.institutional.movimentacao.MovimentacaoProcessualRegistrar;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class CitacaoIntimacaoEngineAcuseTest {

    private final ExpedicaoJudicialRepository expedicaoRepository = mock(ExpedicaoJudicialRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final MovimentacaoProcessualRegistrar movimentacaoRegistrar = mock(MovimentacaoProcessualRegistrar.class);

    @SuppressWarnings("unchecked")
    private CitacaoIntimacaoEngine engine() {
        return new CitacaoIntimacaoEngine(
                expedicaoRepository,
                processoRepository,
                mock(UsuarioRepository.class),
                mock(AuditLedgerService.class),
                currentUserService,
                mock(NotificacaoInteligentePJB.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(MatrizComunicacaoJudicialResolver.class),
                mock(PjbHsmProperties.class),
                mock(PjbExecutionOrchestrator.class),
                movimentacaoRegistrar);
    }

    private ExpedicaoJudicial expedicao(Long processoId) {
        return new ExpedicaoJudicial(processoId, "PROC-" + processoId, TipoComunicacaoJudicial.CITACAO_INICIAL,
                ModalidadeExpedicaoJudicial.DIGITAL_GOVBR_PUSH, ExpedicaoJudicial.TipoDestinatario.PESSOA_FISICA,
                "Fulano", "12345678900", "CIVIL", "PRIMEIRO_GRAU", null, "hash-anterior", "fundamento");
    }

    private Processo processo(Long id) {
        Processo processo = new Processo();
        processo.setId(id);
        return processo;
    }

    @Test
    void acuseDeRecebimentoRegistraMovimentacaoComOAtorAutenticado() {
        ExpedicaoJudicial exp = expedicao(80L);
        when(expedicaoRepository.findByExpedicaoUuid(exp.getExpedicaoUuid())).thenReturn(Optional.of(exp));
        when(processoRepository.findProcessoCompletoById(80L)).thenReturn(Optional.of(processo(80L)));
        Usuario cidadao = Usuario.builder().id(40L).nome("Cidadao").build();
        when(currentUserService.getOrNull()).thenReturn(cidadao);

        engine().processarAcuseRecebimento(new CitacaoIntimacaoEngine.AcuseRecebimentoRequest(
                exp.getExpedicaoUuid(), "token", "127.0.0.1", "device-1", "govbr-1"));

        verify(movimentacaoRegistrar).registrar(any(Processo.class), eq(cidadao), any(), any());
    }

    @Test
    void acuseDeRecebimentoNaoRegistraMovimentacaoSemUsuarioAutenticado() {
        ExpedicaoJudicial exp = expedicao(80L);
        when(expedicaoRepository.findByExpedicaoUuid(exp.getExpedicaoUuid())).thenReturn(Optional.of(exp));
        when(processoRepository.findProcessoCompletoById(80L)).thenReturn(Optional.of(processo(80L)));
        when(currentUserService.getOrNull()).thenReturn(null);

        engine().processarAcuseRecebimento(new CitacaoIntimacaoEngine.AcuseRecebimentoRequest(
                exp.getExpedicaoUuid(), "token", "127.0.0.1", "device-1", "govbr-1"));

        verify(movimentacaoRegistrar, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void confirmacaoDeLeituraRegistraMovimentacaoComOAtorAutenticado() {
        ExpedicaoJudicial exp = expedicao(80L);
        when(expedicaoRepository.findByExpedicaoUuid(exp.getExpedicaoUuid())).thenReturn(Optional.of(exp));
        when(processoRepository.findProcessoCompletoById(80L)).thenReturn(Optional.of(processo(80L)));
        Usuario cidadao = Usuario.builder().id(40L).nome("Cidadao").build();
        when(currentUserService.getOrNull()).thenReturn(cidadao);

        engine().processarConfirmacaoLeitura(exp.getExpedicaoUuid(), "acuse-hash");

        verify(movimentacaoRegistrar).registrar(any(Processo.class), eq(cidadao), any(), any());
    }
}
