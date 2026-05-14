package com.tcc.pjb.backend.service.processual.acceleration.trabalhista;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TrabalhistaExecucaoFastTrackService {

    public record ExecucaoInput(
            UUID processoId,
            boolean tituloExecutivoLiquido,
            boolean devedorLocalizavel,
            boolean bemIndicadoParaPenhora,
            boolean calculosHomologados,
            boolean garantiaDeposito
    ) {}

    public record FastTrackElegibilidade(
            UUID processoId,
            boolean elegivel,
            List<String> etapasDesbloqueadas,
            List<String> pendencias
    ) {}

    public FastTrackElegibilidade avaliar(ExecucaoInput input) {
        List<String> etapas = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        if (input.tituloExecutivoLiquido()) etapas.add("Título executivo líquido — fase de execução apta");
        else pendencias.add("Título não está líquido — necessária liquidação prévia.");

        if (input.calculosHomologados()) etapas.add("Cálculos homologados — prosseguir à penhora");
        else pendencias.add("Cálculos pendentes de homologação.");

        if (input.devedorLocalizavel() && input.bemIndicadoParaPenhora())
            etapas.add("Devedor localizado e bem indicado — expedir mandado de penhora");
        else if (!input.devedorLocalizavel())
            pendencias.add("Devedor não localizado — requerer pesquisa BACENJUD/RENAJUD.");

        if (input.garantiaDeposito()) etapas.add("Garantia por depósito — avaliar embargos");

        boolean elegivel = pendencias.isEmpty() && !etapas.isEmpty();
        return new FastTrackElegibilidade(input.processoId(), elegivel, etapas, pendencias);
    }
}
