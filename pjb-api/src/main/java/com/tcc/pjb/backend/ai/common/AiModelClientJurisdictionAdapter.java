package com.tcc.pjb.backend.ai.common;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.JurisdictionEngine;
import com.tcc.pjb.backend.model.entity.JurisdictionEngine.Context;
import com.tcc.pjb.backend.model.entity.JurisdictionEngine.Engine;
import com.tcc.pjb.backend.model.entity.JurisdictionEngine.Result;
import com.tcc.pjb.backend.model.entity.JurisdictionEngine.Rite;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiModelClientJurisdictionAdapter {

    private final Engine engine;

    public String generateWithJurisdiction(String materia, String orgao, String pais, String tratado, Rite rito) {
        Context ctx = new Context(materia, orgao, pais, tratado, rito);
        Result result = engine.identifyByContext(ctx);

        
        if (!result.isFound()) {
            return "[ERRO] Não foi possível identificar jurisdição. Fallback: COMUM.\n" + result.toJson();
        }

        JurisdictionEngine.JurisdictionSpec spec = result.getSpec();

        String prompt = "Gerar peça jurídica para jurisdição: " + spec.label
                + "\nCategoria: " + spec.category
                + "\nÓrgãos competentes: " + spec.authorities.stream()
                .map(a -> a.getName())
                .distinct()
                .toList()
                + "\nBase legal: " + spec.legalBases.stream()
                .map(l -> l.getCitation())
                .distinct()
                .toList()
                + "\nRito: " + spec.rite
                + "\nPaíses: " + spec.countries.stream()
                .map(c -> c.getName())
                .distinct()
                .toList()
                + "\nTratados: " + spec.treaties
                + "\nMatéria: " + materia;

        return "[PROMPT GERADO]\n" + prompt + "\n\n[DETALHES]\n" + result.toJson();
    }
}
