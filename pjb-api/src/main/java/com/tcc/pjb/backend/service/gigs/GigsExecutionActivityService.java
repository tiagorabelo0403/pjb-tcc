package com.tcc.pjb.backend.service.gigs;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GigsExecutionActivityService {

    public record ExecucaoInput(
            UUID atividadeId,
            String operadorId,
            GigsActivityType tipo,
            String resultado
    ) {}

    public record ExecucaoResultado(
            UUID atividadeId,
            boolean sucesso,
            Instant concluidaEm,
            String mensagem
    ) {}

    public ExecucaoResultado executar(ExecucaoInput input) {
        if (input.tipo().eAtoJurisdicional()) {
            return new ExecucaoResultado(input.atividadeId(), false, null,
                    "Ato jurisdicional — não pode ser concluído automaticamente. Exige confirmação do magistrado.");
        }
        if (input.resultado() == null || input.resultado().isBlank()) {
            return new ExecucaoResultado(input.atividadeId(), false, null,
                    "Resultado não informado — preencher antes de concluir.");
        }
        return new ExecucaoResultado(input.atividadeId(), true, Instant.now(),
                "Atividade '" + input.tipo().name() + "' concluída por " + input.operadorId() + ".");
    }
}
