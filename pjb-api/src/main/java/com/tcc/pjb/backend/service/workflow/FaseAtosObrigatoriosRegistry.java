package com.tcc.pjb.backend.service.workflow;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FaseAtosObrigatoriosRegistry {

    public record AtosRequeridos(
            FaseProcessual fase,
            List<String> atosObrigatorios,
            List<String> atosFacultativos
    ) {}

    public List<String> confirmarAtos(RitoProcessual rito, FaseProcessual fase,
            List<String> atosConcluidos) {
        AtosRequeridos requeridos = consultar(rito, fase);
        return atosConcluidos.stream()
                .filter(a -> requeridos.atosObrigatorios().contains(a)
                        || requeridos.atosFacultativos().contains(a))
                .toList();
    }

    public AtosRequeridos consultar(RitoProcessual rito, FaseProcessual fase) {
        return switch (fase) {
            case CITACAO -> new AtosRequeridos(fase,
                    List.of("MANDADO_CUMPRIDO", "CITACAO_POSTAL_CONFIRMADA",
                            "CITACAO_EDITAL_PUBLICADA"),
                    List.of("CURADOR_ESPECIAL_NOMEADO"));
            case SANEAMENTO -> new AtosRequeridos(fase,
                    List.of("SANEAMENTO_PUBLICADO"),
                    List.of("PERICIA_DESIGNADA", "AUDIENCIA_CONCILIACAO_REALIZADA"));
            case JULGAMENTO -> new AtosRequeridos(fase,
                    List.of("INSTRUCAO_ENCERRADA"),
                    List.of("SUSTENTACAO_ORAL", "MEMORIAIS_APRESENTADOS",
                            "AUDIENCIA_INSTRUCAO_REALIZADA"));
            case ARQUIVAMENTO -> new AtosRequeridos(fase,
                    List.of("CUSTAS_QUITADAS", "TRANSITO_EM_JULGADO_CERTIFICADO"),
                    List.of("MANDADO_CUMPRIDO_CUMPRIMENTO", "TERMO_ARQUIVAMENTO_ASSINADO"));
            default -> new AtosRequeridos(fase, List.of(), List.of());
        };
    }

    public boolean todasObrigatoriasConcluidasParaFase(RitoProcessual rito,
            FaseProcessual fase, Set<String> concluidos) {
        AtosRequeridos req = consultar(rito, fase);
        return concluidos.containsAll(req.atosObrigatorios());
    }
}
