package com.tcc.pjb.backend.service.processual.acceleration.civil;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CivilMinutaSentencaPreparationService {

    public record MinutaInput(
            RitoProcessual rito,
            boolean procedente,
            boolean improcedente,
            boolean extincaoSemMerito,
            String resumoPedidos,
            List<String> fundamentosRelevantes
    ) {}

    public record MinutaPreparada(
            String tipoDecisao,
            String estruturaSugerida,
            List<String> pontosCriticos,
            boolean exigeRevisaoJudicial
    ) {}

    public MinutaPreparada preparar(MinutaInput input) {
        String tipo = resolverTipo(input);
        List<String> pontos = construirPontos(input);
        String estrutura = estruturaPara(tipo, input.rito());
        return new MinutaPreparada(tipo, estrutura, pontos, true);
    }

    private String resolverTipo(MinutaInput input) {
        if (input.extincaoSemMerito()) return "SENTENCA_EXTINCAO_SEM_RESOLUCAO_MERITO";
        if (input.procedente()) return "SENTENCA_PROCEDENCIA";
        if (input.improcedente()) return "SENTENCA_IMPROCEDENCIA";
        return "SENTENCA_PARCIAL_PROCEDENCIA";
    }

    private List<String> construirPontos(MinutaInput input) {
        var pontos = new java.util.ArrayList<String>();
        if (input.resumoPedidos() != null && !input.resumoPedidos().isBlank())
            pontos.add("Pedidos: " + input.resumoPedidos());
        pontos.addAll(input.fundamentosRelevantes());
        return pontos;
    }

    private String estruturaPara(String tipo, RitoProcessual rito) {
        return "Relatório → Fundamentação → Dispositivo [" + tipo + "] — Rito: " + rito.name();
    }
}
