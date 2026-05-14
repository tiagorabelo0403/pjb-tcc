package com.tcc.pjb.backend.service.processual.acceleration.execucao;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class ExecucaoFiscalAccelerationPolicy {

    private ExecucaoFiscalAccelerationPolicy() {}

    public record ExecucaoFiscalInput(
            RitoProcessual rito,
            boolean cdasJuntadas,
            boolean devedorCitado,
            boolean garantiaOferecida,
            Duration tempoParado,
            boolean prescricaoIntercorrenteRisco
    ) {}

    public record AceleradorExecucaoFiscal(
            String acao,
            String mensagem,
            boolean urgente,
            boolean eAtoJurisdicional
    ) {}

    public List<AceleradorExecucaoFiscal> sugerir(ExecucaoFiscalInput input) {
        List<AceleradorExecucaoFiscal> acoes = new ArrayList<>();
        if (!input.cdasJuntadas()) {
            acoes.add(new AceleradorExecucaoFiscal(
                    "REQUERER_CDAS",
                    "CDAs não juntadas — requerer à Fazenda para regularizar a execução.",
                    false, false));
        }
        if (!input.devedorCitado()) {
            acoes.add(new AceleradorExecucaoFiscal(
                    "EXPEDIR_CITACAO",
                    "Devedor não citado — expedir mandado citatório.",
                    false, false));
        }
        if (input.prescricaoIntercorrenteRisco()) {
            acoes.add(new AceleradorExecucaoFiscal(
                    "VERIFICAR_PRESCRICAO_INTERCORRENTE",
                    "Risco de prescrição intercorrente (Súmula 314 STJ) — intimar Fazenda para impulsionar.",
                    true, true));
        }
        if (!input.garantiaOferecida() && input.devedorCitado()) {
            acoes.add(new AceleradorExecucaoFiscal(
                    "PENHORA_ONLINE",
                    "Sem garantia — requerer penhora online (BACENJUD/SISBAJUD).",
                    false, false));
        }
        return acoes;
    }
}
