package com.tcc.pjb.backend.service.processual.acceleration.eleitoral;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public final class EleitoralAccelerationPolicy {

    private EleitoralAccelerationPolicy() {}

    public record EleitoralInput(
            RitoProcessual rito,
            boolean periodoEleitoral,
            boolean prazoCriticoAntesPleito,
            boolean recursoEleitoral,
            boolean representacaoEleitoral
    ) {}

    public record AceleradorEleitoral(
            String acao,
            String mensagem,
            boolean urgente,
            boolean eAtoJurisdicional
    ) {}

    public List<AceleradorEleitoral> sugerir(EleitoralInput input) {
        List<AceleradorEleitoral> acoes = new ArrayList<>();
        if (input.prazoCriticoAntesPleito()) {
            acoes.add(new AceleradorEleitoral(
                    "PRIORIDADE_ELEITORAL",
                    "Prazo crítico antes do pleito — tramitação com urgência eleitoral.",
                    true, true));
        }
        if (input.recursoEleitoral() && input.periodoEleitoral()) {
            acoes.add(new AceleradorEleitoral(
                    "JULGAMENTO_PRIORITARIO_RECURSO",
                    "Recurso eleitoral em período eleitoral — pautar com prioridade absoluta.",
                    true, true));
        }
        if (input.representacaoEleitoral()) {
            acoes.add(new AceleradorEleitoral(
                    "VERIFICAR_PRAZO_REPRESENTACAO",
                    "Representação eleitoral — verificar prazo decadencial de 15 dias.",
                    true, false));
        }
        return acoes;
    }
}
