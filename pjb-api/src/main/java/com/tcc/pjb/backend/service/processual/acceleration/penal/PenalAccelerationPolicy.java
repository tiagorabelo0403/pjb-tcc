package com.tcc.pjb.backend.service.processual.acceleration.penal;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class PenalAccelerationPolicy {

    private PenalAccelerationPolicy() {}

    public record PenalInput(
            RitoProcessual rito,
            boolean reuPreso,
            Duration tempoPreventiva,
            boolean audienciaInstrucaoMarcada,
            boolean laudoJuntado,
            boolean interrogatorioRealizado,
            boolean alegacoesFinaisPendentes
    ) {}

    public record AceleradorPenal(
            String acao,
            String mensagem,
            boolean urgente,
            boolean eAtoJurisdicional
    ) {}

    public List<AceleradorPenal> sugerir(PenalInput input) {
        List<AceleradorPenal> acoes = new ArrayList<>();
        if (input.reuPreso() && input.tempoPreventiva().toDays() > 90) {
            acoes.add(new AceleradorPenal(
                    "VERIFICAR_EXCESSO_PRISAO",
                    "Réu preso há mais de 90 dias — verificar excesso de prazo (Súmula 52 STJ).",
                    true, true));
        }
        if (input.reuPreso() && !input.audienciaInstrucaoMarcada()) {
            acoes.add(new AceleradorPenal(
                    "DESIGNAR_AUDIENCIA_INSTRUCAO",
                    "Réu preso sem audiência de instrução marcada — urgência constitucional.",
                    true, true));
        }
        if (!input.laudoJuntado()) {
            acoes.add(new AceleradorPenal(
                    "SOLICITAR_LAUDO",
                    "Laudo pericial pendente — solicitar ao instituto e designar prazo.",
                    false, false));
        }
        if (input.alegacoesFinaisPendentes()) {
            acoes.add(new AceleradorPenal(
                    "INTIMAR_ALEGACOES_FINAIS",
                    "Alegações finais pendentes — intimar partes para apresentação.",
                    false, false));
        }
        return acoes;
    }
}
