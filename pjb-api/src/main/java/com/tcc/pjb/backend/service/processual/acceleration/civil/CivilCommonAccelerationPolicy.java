package com.tcc.pjb.backend.service.processual.acceleration.civil;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class CivilCommonAccelerationPolicy {

    private CivilCommonAccelerationPolicy() {}

    public record CivilAccelerationInput(
            RitoProcessual rito,
            FaseProcessual fase,
            Duration tempoParado,
            boolean temPreliminarPendente,
            boolean provasEncerradas,
            boolean acordoPossivel,
            boolean parteRevelContumaz
    ) {}

    public record AceleradorCivil(
            String acao,
            String mensagem,
            boolean eAtoJurisdicional
    ) {}

    public List<AceleradorCivil> sugerir(CivilAccelerationInput input) {
        List<AceleradorCivil> acoes = new ArrayList<>();
        if (!input.temPreliminarPendente() && input.provasEncerradas()) {
            acoes.add(new AceleradorCivil(
                    "JULGAMENTO_ANTECIPADO",
                    "Processo apto para julgamento antecipado do mérito.",
                    true));
        }
        if (input.acordoPossivel()) {
            acoes.add(new AceleradorCivil(
                    "PROPOR_CONCILIACAO",
                    "Há indícios de acordo possível — designar audiência de conciliação.",
                    true));
        }
        if (input.parteRevelContumaz()) {
            acoes.add(new AceleradorCivil(
                    "CERTIFICAR_REVELIA",
                    "Parte ré revel — certificar para análise dos efeitos.",
                    false));
        }
        if (input.tempoParado().toDays() > 30) {
            acoes.add(new AceleradorCivil(
                    "VERIFICAR_PARALISACAO",
                    "Processo parado há mais de 30 dias — verificar causa e desencalhar.",
                    false));
        }
        return acoes;
    }
}
