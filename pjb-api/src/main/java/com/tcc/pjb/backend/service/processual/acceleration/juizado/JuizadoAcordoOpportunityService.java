package com.tcc.pjb.backend.service.processual.acceleration.juizado;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JuizadoAcordoOpportunityService {

    public record AcordoInput(
            UUID processoId,
            RitoProcessual rito,
            BigDecimal valorCausa,
            boolean historicoPagamentoParcial,
            boolean parteApresentouProposta,
            boolean advogadoAmbosLados
    ) {}

    public record OportunidadeAcordo(
            UUID processoId,
            boolean acordoPossivel,
            int probabilidadeEstimada,
            String sugestao
    ) {}

    public OportunidadeAcordo avaliar(AcordoInput input) {
        int probabilidade = 0;
        if (input.historicoPagamentoParcial()) probabilidade += 30;
        if (input.parteApresentouProposta()) probabilidade += 40;
        if (!input.advogadoAmbosLados()) probabilidade += 10;

        boolean possivel = probabilidade >= 40;
        String sugestao = possivel
                ? "Há indícios de acordo possível no JEC — designar sessão de conciliação antes da instrução."
                : "Sem indícios suficientes para acordo no momento.";

        return new OportunidadeAcordo(input.processoId(), possivel, probabilidade, sugestao);
    }
}
