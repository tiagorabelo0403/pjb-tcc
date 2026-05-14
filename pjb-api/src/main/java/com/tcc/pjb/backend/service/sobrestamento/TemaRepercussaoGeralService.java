package com.tcc.pjb.backend.service.sobrestamento;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TemaRepercussaoGeralService {

    public record TemaInput(
            String temaNumero,
            String descricao,
            boolean repercussaoGeralReconhecida,
            boolean afetadoParaJulgamento,
            boolean julgamentoConcluido,
            LocalDate dataAfetacao,
            LocalDate dataJulgamento,
            String tribunalOrigem
    ) {}

    public record TemaStatus(
            String temaNumero,
            boolean sobrestamentoObrigatorio,
            boolean liberadoParaCumprimento,
            String situacao,
            String orientacao
    ) {}

    public TemaStatus avaliarTema(TemaInput input) {
        if (!input.repercussaoGeralReconhecida()) {
            return new TemaStatus(input.temaNumero(), false, true,
                    "SEM_REPERCUSSAO_GERAL",
                    "Tema sem repercussão geral reconhecida — processo prossegue normalmente.");
        }
        if (input.julgamentoConcluido()) {
            return new TemaStatus(input.temaNumero(), false, true,
                    "JULGADO",
                    "Tema " + input.temaNumero() + " julgado em " + input.dataJulgamento()
                            + " — aplicar o precedente vinculante (art. 927, CPC).");
        }
        if (input.afetadoParaJulgamento()) {
            return new TemaStatus(input.temaNumero(), true, false,
                    "AFETADO_PENDENTE",
                    "Tema " + input.temaNumero() + " afetado desde " + input.dataAfetacao()
                            + " — sobrestamento obrigatório (art. 1.037, CPC).");
        }
        return new TemaStatus(input.temaNumero(), false, false,
                "REPERCUSSAO_RECONHECIDA_NAO_AFETADO",
                "Repercussão geral reconhecida, aguardando afetação formal para julgamento.");
    }

    public List<TemaStatus> avaliarTemas(List<TemaInput> temas) {
        return temas.stream().map(this::avaliarTema).toList();
    }
}
