package com.tcc.pjb.backend.ai.skills;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;

public interface IASkill {

    boolean suporta(IARequest request);

    IAResponse executar(IARequest request, Map<String, Object> contexto);

    String getNome();

    default String getAcaoSuportada() {
        return getNome();
    }

    default boolean suportaAcao(IARequest request, String acao) {
        if (request == null || acao == null || acao.isBlank()) {
            return false;
        }
        return acao.equalsIgnoreCase(Objects.toString(request.getAcao(), ""));
    }

    default boolean suportaAcaoContendo(IARequest request, String fragmento) {
        if (request == null || fragmento == null || fragmento.isBlank()) {
            return false;
        }
        String acao = Objects.toString(request.getAcao(), "").toUpperCase(Locale.ROOT);
        return acao.contains(fragmento.toUpperCase(Locale.ROOT));
    }
}
