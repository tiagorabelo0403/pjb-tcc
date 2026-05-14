package com.tcc.pjb.backend.service.consulta;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConsultaPublicaProcessoService {

    public record ConsultaFiltro(
            String numero,
            String cpfCnpjParte,
            String nomeParteFragmento,
            String varaId,
            String tribunal,
            LocalDate dataInicioDistribuicao,
            LocalDate dataFimDistribuicao
    ) {}

    public record ProcessoResumoPublico(
            UUID processoId,
            String numero,
            String classe,
            String assunto,
            String vara,
            String tribunal,
            String situacao,
            LocalDate dataDistribuicao,
            boolean sigiloso
    ) {}

    public record ConsultaResultado(
            List<ProcessoResumoPublico> processos,
            int total,
            int pagina,
            int tamanhoPagina,
            boolean houveResultados
    ) {}

    public ConsultaResultado consultar(ConsultaFiltro filtro,
            List<ProcessoResumoPublico> candidatos, int pagina, int tamanhoPagina) {
        List<ProcessoResumoPublico> visiveis = candidatos.stream()
                .filter(p -> !p.sigiloso())
                .filter(p -> filtro.numero() == null || p.numero().contains(filtro.numero()))
                .filter(p -> filtro.tribunal() == null || filtro.tribunal().equals(p.tribunal()))
                .filter(p -> filtro.varaId() == null || filtro.varaId().equals(p.vara()))
                .toList();

        int total = visiveis.size();
        int inicio = Math.min(pagina * tamanhoPagina, total);
        int fim = Math.min(inicio + tamanhoPagina, total);
        List<ProcessoResumoPublico> paginated = visiveis.subList(inicio, fim);

        return new ConsultaResultado(paginated, total, pagina, tamanhoPagina, total > 0);
    }

    public ProcessoResumoPublico mascararSeNecessario(ProcessoResumoPublico processo) {
        if (!processo.sigiloso()) return processo;
        return new ProcessoResumoPublico(
                processo.processoId(),
                "SIGILOSO",
                "SIGILOSO",
                "SIGILOSO",
                processo.vara(),
                processo.tribunal(),
                processo.situacao(),
                null,
                true);
    }
}
