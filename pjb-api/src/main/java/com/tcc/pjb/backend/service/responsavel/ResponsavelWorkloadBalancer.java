package com.tcc.pjb.backend.service.responsavel;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResponsavelWorkloadBalancer {

    public record ServidorCarga(
            String servidorId,
            String nome,
            int tarefasAtivas,
            List<String> especialidades
    ) {}

    public record SugestaoDesignacao(
            String servidorId,
            String nome,
            int cargaAtual,
            String justificativa
    ) {}

    public SugestaoDesignacao sugerir(List<ServidorCarga> servidores, String tipoAtividade) {
        return servidores.stream()
                .filter(s -> s.especialidades().contains(tipoAtividade)
                        || s.especialidades().isEmpty())
                .min(Comparator.comparingInt(ServidorCarga::tarefasAtivas))
                .map(s -> new SugestaoDesignacao(s.servidorId(), s.nome(), s.tarefasAtivas(),
                        "Servidor " + s.nome() + " tem " + s.tarefasAtivas()
                                + " tarefa(s) ativa(s) — menor carga disponível para " + tipoAtividade + "."))
                .orElse(new SugestaoDesignacao(null, null, 0,
                        "Nenhum servidor disponível para " + tipoAtividade + "."));
    }
}
