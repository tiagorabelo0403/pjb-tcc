package com.tcc.pjb.backend.core.comunicacao.judicial;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CitacaoEditalCuradoriaService {

    private final ObjectProvider<CuradorEspecialAutomaticoService> curadorProvider;

    public CitacaoEditalCuradoriaService(ObjectProvider<CuradorEspecialAutomaticoService> curadorProvider) {
        this.curadorProvider = curadorProvider;
    }

    public void registrarNecessidadeSeAusente(ExpedicaoJudicial expedicao) {
        CuradorEspecialAutomaticoService curador = curadorProvider.getIfAvailable();
        if (curador != null) {
            curador.registrarNecessidadeSeAusente(expedicao.getProcessoId(), expedicao.getExpedicaoUuid(), CuradorEspecialAutomaticoService.TipoCuradoria.REU_EM_LUGAR_INCERTO);
        }
    }
}
