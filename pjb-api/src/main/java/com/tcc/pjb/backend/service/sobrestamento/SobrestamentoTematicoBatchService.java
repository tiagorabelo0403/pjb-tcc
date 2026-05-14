package com.tcc.pjb.backend.service.sobrestamento;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SobrestamentoTematicoBatchService {

    public record ProcessoParaSobrestar(
            UUID processoId,
            String numero,
            String temaNumero,
            boolean jasobrestado
    ) {}

    public record SobrestamentoBatchResult(
            int totalProcessados,
            int sobrestados,
            int jaEstavamSobrestados,
            int erros,
            List<String> processosSobrestados,
            LocalDate dataSobrestamento
    ) {}

    public record ErroSobrestamento(
            UUID processoId,
            String motivo
    ) {}

    public SobrestamentoBatchResult aplicarSobrestamento(
            String temaNumero,
            List<ProcessoParaSobrestar> processos,
            LocalDate dataSobrestamento) {

        List<ProcessoParaSobrestar> novos = processos.stream()
                .filter(p -> !p.jasobrestado())
                .toList();

        List<String> sobrestados = novos.stream()
                .map(ProcessoParaSobrestar::numero)
                .toList();

        int jaEstavamSobrestados = processos.size() - novos.size();

        return new SobrestamentoBatchResult(
                processos.size(),
                novos.size(),
                jaEstavamSobrestados,
                0,
                sobrestados,
                dataSobrestamento);
    }

    public int contarElegiveis(List<ProcessoParaSobrestar> processos) {
        return (int) processos.stream().filter(p -> !p.jasobrestado()).count();
    }
}
