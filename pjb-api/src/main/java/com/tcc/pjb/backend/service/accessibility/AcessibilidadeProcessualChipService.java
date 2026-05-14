package com.tcc.pjb.backend.service.accessibility;

import com.tcc.pjb.backend.service.processual.chip.ProcessoChipTipo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AcessibilidadeProcessualChipService {

    public record NecessidadeAcessibilidade(
            ProcessoChipTipo chip,
            String recursoNecessario,
            String providenciaSugerida,
            boolean obrigatoria
    ) {}

    public List<NecessidadeAcessibilidade> identificar(Collection<ProcessoChipTipo> chips) {
        List<NecessidadeAcessibilidade> necessidades = new ArrayList<>();
        if (chips.contains(ProcessoChipTipo.LIBRAS)) {
            necessidades.add(new NecessidadeAcessibilidade(
                    ProcessoChipTipo.LIBRAS,
                    "Intérprete de Libras",
                    "Solicitar intérprete de Libras para a audiência — Lei 10.436/2002.",
                    true));
        }
        if (chips.contains(ProcessoChipTipo.INTERPRETE_GUIA)) {
            necessidades.add(new NecessidadeAcessibilidade(
                    ProcessoChipTipo.INTERPRETE_GUIA,
                    "Intérprete-guia",
                    "Providenciar intérprete-guia para pessoa surdo-cega.",
                    true));
        }
        if (chips.contains(ProcessoChipTipo.PESSOA_COM_DEFICIENCIA)) {
            necessidades.add(new NecessidadeAcessibilidade(
                    ProcessoChipTipo.PESSOA_COM_DEFICIENCIA,
                    "Acessibilidade estrutural",
                    "Verificar acessibilidade do local de audiência — Lei 13.146/2015.",
                    true));
        }
        return necessidades;
    }
}
