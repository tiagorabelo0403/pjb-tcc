package com.tcc.pjb.backend.service.processual.acceleration.familia;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FamiliaUrgenciaHumanitariaService {

    public record UrgenciaInput(
            UUID processoId,
            boolean riscoBemEstarCrianca,
            boolean riscoVidaAdolescente,
            boolean alimentosAtrasados,
            boolean descumprimentoGuarda,
            boolean ameacaViolenciaDomestica
    ) {}

    public record AlertaUrgencia(
            UUID processoId,
            String tipo,
            String mensagem,
            int prioridade
    ) {}

    public List<AlertaUrgencia> avaliar(UrgenciaInput input) {
        List<AlertaUrgencia> alertas = new ArrayList<>();
        if (input.riscoBemEstarCrianca()) {
            alertas.add(new AlertaUrgencia(input.processoId(), "RISCO_CRIANCA",
                    "Risco ao bem-estar da criança — analisar pedido de tutela urgente.", 100));
        }
        if (input.riscoVidaAdolescente()) {
            alertas.add(new AlertaUrgencia(input.processoId(), "RISCO_ADOLESCENTE",
                    "Risco à vida ou integridade de adolescente — urgência constitucional.", 100));
        }
        if (input.alimentosAtrasados()) {
            alertas.add(new AlertaUrgencia(input.processoId(), "ALIMENTOS_ATRASADOS",
                    "Alimentos em atraso — verificar execução imediata.", 80));
        }
        if (input.descumprimentoGuarda()) {
            alertas.add(new AlertaUrgencia(input.processoId(), "DESCUMPRIMENTO_GUARDA",
                    "Descumprimento de guarda — verificar busca e apreensão.", 70));
        }
        if (input.ameacaViolenciaDomestica()) {
            alertas.add(new AlertaUrgencia(input.processoId(), "VIOLENCIA_DOMESTICA",
                    "Ameaça de violência doméstica — medida protetiva urgente.", 90));
        }
        alertas.sort((a, b) -> Integer.compare(b.prioridade(), a.prioridade()));
        return alertas;
    }
}
