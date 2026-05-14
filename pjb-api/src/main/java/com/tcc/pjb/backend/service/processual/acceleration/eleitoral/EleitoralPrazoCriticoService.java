package com.tcc.pjb.backend.service.processual.acceleration.eleitoral;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EleitoralPrazoCriticoService {

    public record ProcessoEleitoralItem(
            UUID processoId,
            String numeroProcesso,
            LocalDate dataPleito,
            LocalDate prazoDecisao,
            boolean recursoPendente
    ) {}

    public record AlertaEleitoral(
            UUID processoId,
            String numeroProcesso,
            long diasRestantes,
            String nivel,
            String mensagem
    ) {}

    public List<AlertaEleitoral> identificarCriticos(List<ProcessoEleitoralItem> processos) {
        LocalDate hoje = LocalDate.now();
        return processos.stream()
                .filter(p -> p.prazoDecisao() != null && !p.prazoDecisao().isBefore(hoje))
                .map(p -> {
                    long dias = hoje.until(p.prazoDecisao()).getDays();
                    String nivel = dias <= 3 ? "CRITICO" : dias <= 10 ? "URGENTE" : "ATENCAO";
                    return new AlertaEleitoral(p.processoId(), p.numeroProcesso(), dias, nivel,
                            "Prazo eleitoral em " + dias + " dia(s) — julgamento prioritário.");
                })
                .sorted(Comparator.comparingLong(AlertaEleitoral::diasRestantes))
                .toList();
    }
}
