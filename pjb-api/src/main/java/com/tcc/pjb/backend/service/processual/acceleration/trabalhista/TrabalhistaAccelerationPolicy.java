package com.tcc.pjb.backend.service.processual.acceleration.trabalhista;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public final class TrabalhistaAccelerationPolicy {

    private TrabalhistaAccelerationPolicy() {}

    public record TrabalhistaInput(
            RitoProcessual rito,
            boolean homologacaoPendente,
            boolean acordoTentado,
            boolean calculosApresentados,
            boolean peritoNomeado,
            boolean execucaoIniciada,
            boolean depositoRecursoFeito
    ) {}

    public record AceleradorTrabalhista(
            String acao,
            String mensagem,
            boolean eAtoJurisdicional
    ) {}

    public List<AceleradorTrabalhista> sugerir(TrabalhistaInput input) {
        List<AceleradorTrabalhista> acoes = new ArrayList<>();
        if (!input.acordoTentado()) {
            acoes.add(new AceleradorTrabalhista(
                    "TENTAR_ACORDO",
                    "Acordo trabalhista não tentado — designar audiência de conciliação.",
                    true));
        }
        if (input.homologacaoPendente()) {
            acoes.add(new AceleradorTrabalhista(
                    "HOMOLOGAR_ACORDO",
                    "Acordo aguardando homologação — verificar assinaturas e homologar.",
                    true));
        }
        if (input.calculosApresentados() && !input.execucaoIniciada()) {
            acoes.add(new AceleradorTrabalhista(
                    "INICIAR_EXECUCAO",
                    "Cálculos apresentados — processo apto para início da execução.",
                    true));
        }
        if (!input.depositoRecursoFeito() && input.execucaoIniciada()) {
            acoes.add(new AceleradorTrabalhista(
                    "VERIFICAR_DEPOSITO_RECURSAL",
                    "Execução iniciada sem depósito recursal — verificar regularidade.",
                    false));
        }
        return acoes;
    }
}
