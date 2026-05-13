package com.tcc.pjb.backend.model.dto.processual.surface.unificado;

import java.util.List;

public record ProcessoSurfacePerfilResponse(
        String codigo,
        String nomeExibicao,
        String painel,
        String processProfile,
        String trustFloor,
        String accentColor,
        List<String> visualizar,
        List<String> receber,
        List<String> preparar,
        List<String> aprovar,
        List<String> assinar,
        List<String> peticionar,
        List<String> certificar,
        List<String> redistribuir,
        List<String> recorrer,
        List<String> embargar,
        List<String> sugerir,
        List<String> separadores,
        List<String> guardas,
        List<String> fundamentos
) {
}
