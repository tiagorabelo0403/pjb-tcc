package com.tcc.pjb.backend.service.processual.acceleration.familia;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public final class FamiliaAccelerationPolicy {

    private FamiliaAccelerationPolicy() {}

    public record FamiliaInput(
            RitoProcessual rito,
            boolean envolveAlimentos,
            boolean envolveGuarda,
            boolean criancaOuAdolescente,
            boolean violenciaDomestica,
            boolean acordoFamiliarPossivel,
            boolean mediacaoTentada
    ) {}

    public record AceleradorFamilia(
            String acao,
            String mensagem,
            boolean urgente,
            boolean eAtoJurisdicional
    ) {}

    public List<AceleradorFamilia> sugerir(FamiliaInput input) {
        List<AceleradorFamilia> acoes = new ArrayList<>();
        if (input.envolveAlimentos() && !input.mediacaoTentada()) {
            acoes.add(new AceleradorFamilia(
                    "DESIGNAR_MEDIACAO_ALIMENTAR",
                    "Processo de alimentos — designar mediação familiar antes da instrução.",
                    true, true));
        }
        if (input.criancaOuAdolescente()) {
            acoes.add(new AceleradorFamilia(
                    "PRIORIDADE_ECA",
                    "Processo envolve criança ou adolescente — tramitação prioritária (ECA art. 152).",
                    true, false));
        }
        if (input.violenciaDomestica()) {
            acoes.add(new AceleradorFamilia(
                    "VERIFICAR_MEDIDA_PROTETIVA",
                    "Processo relacionado a violência doméstica — verificar medidas protetivas vigentes.",
                    true, true));
        }
        if (input.acordoFamiliarPossivel() && !input.mediacaoTentada()) {
            acoes.add(new AceleradorFamilia(
                    "TENTAR_ACORDO_FAMILIAR",
                    "Há indicação de acordo familiar possível — tentar conciliação.",
                    false, true));
        }
        return acoes;
    }
}
