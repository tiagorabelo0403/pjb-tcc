package com.tcc.pjb.backend.service.conciliation;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AcordoOpportunityService {

    public record OportunidadeInput(
            UUID processoId,
            RitoProcessual rito,
            boolean partesDispostas,
            boolean existePropostaFormal,
            boolean mediadorDesignado,
            boolean advogadosPresentes
    ) {}

    public record OportunidadeAcordo(
            UUID processoId,
            boolean acordoPossivel,
            String sugestao,
            String acaoImediata
    ) {}

    public OportunidadeAcordo avaliar(OportunidadeInput input) {
        boolean possivel = input.partesDispostas() || input.existePropostaFormal();

        String sugestao = possivel
                ? construirSugestao(input)
                : "Sem indicação de acordo no momento — prosseguir com instrução.";

        String acao = possivel && !input.mediadorDesignado()
                ? "Designar mediador ou conciliador para a sessão."
                : possivel ? "Agendar sessão de conciliação." : "Nenhuma ação imediata.";

        return new OportunidadeAcordo(input.processoId(), possivel, sugestao, acao);
    }

    private String construirSugestao(OportunidadeInput input) {
        if (input.existePropostaFormal()) return "Proposta formal em análise — facilitar negociação.";
        if (input.partesDispostas()) return "Partes indicam disposição para acordo — designar sessão de conciliação.";
        return "Há indícios de acordo — aprofundar verificação.";
    }
}
