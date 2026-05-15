package com.tcc.pjb.backend.service.execucaopenal;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PenaPrivativaCalculatorService {

    public enum ConcursoTipo {
        MATERIAL,
        FORMAL_PROPRIO,
        FORMAL_IMPROPRIO,
        CONTINUADO,
        CONTINUADO_ESPECIAL
    }

    public record PenaInput(
            List<Integer> penasMeses,
            ConcursoTipo concurso,
            boolean reincidente
    ) {}

    public record PenaUnificada(
            int totalMeses,
            String fundamentacao,
            List<String> alertas
    ) {}

    public PenaUnificada unificar(PenaInput input) {
        if (input.penasMeses() == null || input.penasMeses().isEmpty()) {
            return new PenaUnificada(0, "Sem penas a unificar.", List.of());
        }
        int penaMaiorMeses = input.penasMeses().stream().mapToInt(Integer::intValue).max().orElse(0);
        int somaTotal = input.penasMeses().stream().mapToInt(Integer::intValue).sum();

        return switch (input.concurso()) {
            case MATERIAL -> new PenaUnificada(
                    somaTotal,
                    "Concurso material (CP art. 69): soma das penas.",
                    limiteLegal(somaTotal));

            case FORMAL_PROPRIO -> {
                int adicional = (int) Math.ceil(penaMaiorMeses * 1.0 / 6);
                int total = penaMaiorMeses + adicional;
                yield new PenaUnificada(
                        total,
                        "Concurso formal próprio (CP art. 70): pena mais grave acrescida de 1/6.",
                        limiteLegal(total));
            }

            case FORMAL_IMPROPRIO -> new PenaUnificada(
                    somaTotal,
                    "Concurso formal impróprio (CP art. 70 pú): soma das penas.",
                    limiteLegal(somaTotal));

            case CONTINUADO -> {
                int fracaoMeses = (int) Math.ceil(penaMaiorMeses * 2.0 / 3);
                int total = penaMaiorMeses + fracaoMeses;
                yield new PenaUnificada(
                        total,
                        "Crime continuado (CP art. 71): pena mais grave acrescida de 2/3.",
                        limiteLegal(total));
            }

            case CONTINUADO_ESPECIAL -> {
                int total = penaMaiorMeses * 3;
                yield new PenaUnificada(
                        total,
                        "Crime continuado especial (CP art. 71 pú): pena triplicada.",
                        limiteLegal(total));
            }
        };
    }

    private List<String> limiteLegal(int totalMeses) {
        if (totalMeses > 40 * 12) {
            return List.of("Pena excede 40 anos — aplicar limite legal (CP art. 75). Execução limitada a 40 anos.");
        }
        return List.of();
    }
}
