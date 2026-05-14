package com.tcc.pjb.backend.service.processual.acceleration.execucao;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ExecucaoFiscalPrescricaoIntercorrenteRadarService {

    private static final Duration LIMIAR_ALERTA = Duration.ofDays(365);
    private static final Duration LIMIAR_CRITICO = Duration.ofDays(1460);

    public record ExecucaoFiscalItem(
            UUID processoId,
            String numeroProcesso,
            Duration tempoParadoSemImpulsaoFazenda,
            boolean fazendaForaInativa
    ) {}

    public record AlertaPrescricao(
            UUID processoId,
            String numeroProcesso,
            String nivel,
            String mensagem,
            boolean exigeAcaoImediata
    ) {}

    public List<AlertaPrescricao> radar(List<ExecucaoFiscalItem> processos) {
        return processos.stream()
                .filter(p -> p.tempoParadoSemImpulsaoFazenda().compareTo(LIMIAR_ALERTA) >= 0)
                .map(this::avaliar)
                .sorted(Comparator.comparing(AlertaPrescricao::exigeAcaoImediata).reversed())
                .toList();
    }

    private AlertaPrescricao avaliar(ExecucaoFiscalItem item) {
        boolean critico = item.tempoParadoSemImpulsaoFazenda().compareTo(LIMIAR_CRITICO) >= 0;
        String nivel = critico ? "CRITICO" : "ATENCAO";
        String mensagem = critico
                ? "Risco iminente de prescrição intercorrente — intimação urgente à Fazenda."
                : "Execução parada sem impulso da Fazenda — verificar prescrição intercorrente (art. 40 LEF).";
        return new AlertaPrescricao(item.processoId(), item.numeroProcesso(), nivel, mensagem, critico);
    }
}
