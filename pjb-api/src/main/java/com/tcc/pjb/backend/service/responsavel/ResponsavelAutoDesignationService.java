package com.tcc.pjb.backend.service.responsavel;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ResponsavelAutoDesignationService {

    public record DesignacaoRequest(
            UUID atividadeId,
            String tipoAtividade,
            List<ResponsavelWorkloadBalancer.ServidorCarga> servidoresDisponiveis
    ) {}

    public record DesignacaoResponse(
            UUID atividadeId,
            boolean designacaoSugerida,
            ResponsavelWorkloadBalancer.SugestaoDesignacao sugestao,
            String mensagem
    ) {}

    private final ResponsavelWorkloadBalancer workloadBalancer;

    public ResponsavelAutoDesignationService(ResponsavelWorkloadBalancer workloadBalancer) {
        this.workloadBalancer = workloadBalancer;
    }

    public DesignacaoResponse sugerir(DesignacaoRequest request) {
        ResponsavelWorkloadBalancer.SugestaoDesignacao sugestao =
                workloadBalancer.sugerir(request.servidoresDisponiveis(), request.tipoAtividade());

        boolean designado = sugestao.servidorId() != null;
        String mensagem = designado
                ? "Sugestão: " + sugestao.justificativa() + " (não aplica automaticamente — exige confirmação)."
                : "Nenhum servidor disponível — designar manualmente.";

        return new DesignacaoResponse(request.atividadeId(), designado, sugestao, mensagem);
    }
}
