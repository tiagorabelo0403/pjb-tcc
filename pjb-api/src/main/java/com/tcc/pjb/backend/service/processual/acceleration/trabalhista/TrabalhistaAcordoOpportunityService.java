package com.tcc.pjb.backend.service.processual.acceleration.trabalhista;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TrabalhistaAcordoOpportunityService {

    public record AcordoTrabalhistaInput(
            UUID processoId,
            BigDecimal valorPedido,
            boolean reclamadaPorteGrande,
            boolean reclamadaPorteMediaPequena,
            boolean reincidenteAcordos,
            boolean processoRepetitivo
    ) {}

    public record OportunidadeAcordoTrabalhista(
            UUID processoId,
            boolean altaProbabilidade,
            int probabilidadeEstimada,
            String motivoPrincipal,
            String sugestao
    ) {}

    public OportunidadeAcordoTrabalhista avaliar(AcordoTrabalhistaInput input) {
        int probabilidade = 30;
        String motivo = "Base";
        if (input.reclamadaPorteGrande()) { probabilidade += 20; motivo = "Grande empresa prefere acordar"; }
        if (input.reincidenteAcordos()) { probabilidade += 25; motivo = "Histórico de acordos anteriores"; }
        if (input.processoRepetitivo()) { probabilidade += 15; motivo = "Processo de massa — acordo preferível"; }

        String sugestao = probabilidade >= 50
                ? "Alta probabilidade de acordo — propor na audiência inicial."
                : "Baixa probabilidade no momento — prosseguir normalmente.";

        return new OportunidadeAcordoTrabalhista(
                input.processoId(), probabilidade >= 50, probabilidade, motivo, sugestao);
    }
}
