package com.tcc.pjb.backend.core.comunicacao.judicial;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CitacaoPrazoReveliaGatilhoService {

    private final ObjectProvider<PrazoRespostaPosEntregaEngine> prazoProvider;
    private final ObjectProvider<ReveliaAutomaticaEngine> reveliaProvider;

    public CitacaoPrazoReveliaGatilhoService(ObjectProvider<PrazoRespostaPosEntregaEngine> prazoProvider,
                                             ObjectProvider<ReveliaAutomaticaEngine> reveliaProvider) {
        this.prazoProvider = prazoProvider;
        this.reveliaProvider = reveliaProvider;
    }

    public void iniciarPrazoEReveliaSeCabivel(ExpedicaoJudicial expedicao) {
        if (expedicao == null || !expedicao.isEntregueOuLida()) {
            return;
        }
        PrazoRespostaPosEntregaEngine prazoEngine = prazoProvider.getIfAvailable();
        if (prazoEngine == null) {
            return;
        }
        PrazoRespostaPosEntregaEngine.PrazoResposta prazo = prazoEngine.iniciarPrazoAposEntrega(
                expedicao.getExpedicaoUuid(),
                CitacaoIntimacaoExpedicaoSupport.resolverTipoUsuarioPrazo(expedicao),
                CitacaoIntimacaoExpedicaoSupport.resolverTipoPrazoPadrao(expedicao)
        );
        ReveliaAutomaticaEngine reveliaEngine = reveliaProvider.getIfAvailable();
        if (reveliaEngine != null && prazo != null && prazo.vencimentoEm() != null) {
            reveliaEngine.iniciarMonitoramento(expedicao.getExpedicaoUuid(), expedicao.getProcessoId(), prazo.vencimentoEm());
        }
    }
}
