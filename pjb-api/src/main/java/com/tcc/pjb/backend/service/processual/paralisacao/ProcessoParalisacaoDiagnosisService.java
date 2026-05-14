package com.tcc.pjb.backend.service.processual.paralisacao;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessoParalisacaoDiagnosisService {

    public record ParalisacaoInput(
            UUID processoId,
            String numeroProcesso,
            Duration tempoParado,
            boolean temExpedienteSemCiencia,
            boolean temDocumentoSemAssinatura,
            boolean temTarefaSemResponsavel,
            boolean temPendenciaVencida,
            boolean sobrestado,
            String processoSobrestamentoRef
    ) {}

    public record DiagnosticoParalisacao(
            UUID processoId,
            String numeroProcesso,
            Duration tempoParado,
            List<ProcessoParalisacaoReason> causas,
            String resumo
    ) {}

    public DiagnosticoParalisacao diagnosticar(ParalisacaoInput input) {
        List<ProcessoParalisacaoReason> causas = new ArrayList<>();

        if (input.temExpedienteSemCiencia())
            causas.add(new ProcessoParalisacaoReason.ExpedienteSemCiencia("pendente"));
        if (input.temDocumentoSemAssinatura())
            causas.add(new ProcessoParalisacaoReason.DocumentoAguardandoAssinatura("documento"));
        if (input.temTarefaSemResponsavel())
            causas.add(new ProcessoParalisacaoReason.TarefaSemResponsavel("tarefa interna"));
        if (input.temPendenciaVencida())
            causas.add(new ProcessoParalisacaoReason.PendenciaVencida("pendência vencida"));
        if (input.sobrestado())
            causas.add(new ProcessoParalisacaoReason.SobrestamentoPorDependencia(
                    input.processoSobrestamentoRef() != null ? input.processoSobrestamentoRef() : "?"));
        if (causas.isEmpty())
            causas.add(new ProcessoParalisacaoReason.CausaNaoIdentificada());

        String resumo = "Processo " + input.numeroProcesso() + " parado há "
                + input.tempoParado().toDays() + " dias. Causa(s): "
                + causas.stream().map(ProcessoParalisacaoReason::descricao)
                        .reduce("", (a, b) -> a.isBlank() ? b : a + " | " + b);

        return new DiagnosticoParalisacao(input.processoId(), input.numeroProcesso(),
                input.tempoParado(), causas, resumo);
    }
}
