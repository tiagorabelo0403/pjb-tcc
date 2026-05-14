package com.tcc.pjb.backend.service.processual.acceleration.trabalhista;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TrabalhistaDejtPublicationReadinessService {

    public record DejtInput(
            boolean decisaoAssinada,
            boolean partesCadastradasDeJt,
            boolean advogadosCadastradasDeJt,
            boolean processoSemSigiloIncompativel,
            boolean nomeParteCorretamenteGrafado
    ) {}

    public record DejtReadiness(
            boolean apto,
            List<String> pendencias,
            String mensagem
    ) {}

    public DejtReadiness avaliar(DejtInput input) {
        List<String> pendencias = new ArrayList<>();
        if (!input.decisaoAssinada()) pendencias.add("Decisão não assinada digitalmente.");
        if (!input.partesCadastradasDeJt()) pendencias.add("Partes sem cadastro no DeJT — publicação falhará.");
        if (!input.advogadosCadastradasDeJt()) pendencias.add("Advogados não cadastrados no DeJT.");
        if (!input.processoSemSigiloIncompativel()) pendencias.add("Processo com sigilo — verificar permissão de publicação no DeJT.");
        if (!input.nomeParteCorretamenteGrafado()) pendencias.add("Nome da parte pode estar grafado incorretamente — risco de nulidade.");

        boolean apto = pendencias.isEmpty();
        String mensagem = apto
                ? "Publicação DeJT apta — todos os requisitos atendidos."
                : pendencias.size() + " pendência(s) antes da publicação no DeJT.";

        return new DejtReadiness(apto, pendencias, mensagem);
    }
}
