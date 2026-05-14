package com.tcc.pjb.backend.service.processual.distribution;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.institutional.InstituicaoJudicial;
import com.tcc.pjb.backend.service.institutional.InstituicaoJudicialTipo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CompetenciaJudicialMatrix {

    public record ResolucaoCompetencia(
            Optional<InstituicaoJudicial> orgaoSugerido,
            EsferaJurisdicao esfera,
            String justificativa,
            boolean requerDistribuicaoManual
    ) {}

    private final TribunalConfigurationRegistry registry;

    public CompetenciaJudicialMatrix(TribunalConfigurationRegistry registry) {
        this.registry = registry;
    }

    public ResolucaoCompetencia resolver(
            RitoProcessual rito,
            NaturezaProcessualResolver natureza,
            MateriaJurisdicao materia,
            BigDecimal valorCausa,
            String uf) {

        EsferaJurisdicao esfera = natureza.esfera();
        List<InstituicaoJudicial> candidatos = new ArrayList<>(registry.listarPorUf(uf));

        List<InstituicaoJudicial> compativeis = candidatos.stream()
                .filter(i -> i.aceitaRito(rito))
                .filter(i -> !i.ePlantao())
                .toList();

        if (compativeis.isEmpty()) {
            return new ResolucaoCompetencia(Optional.empty(), esfera,
                    "Nenhum órgão competente localizado para o rito " + rito.name() + " na UF " + uf,
                    true);
        }

        if (natureza.admiteJuizadoEspecial() && valorCausa != null
                && valorCausa.compareTo(new BigDecimal("40000")) <= 0) {
            Optional<InstituicaoJudicial> jec = compativeis.stream()
                    .filter(i -> i.tipo() instanceof InstituicaoJudicialTipo.JuizadoEspecial
                            || i.tipo() instanceof InstituicaoJudicialTipo.JuizadoEspecialFederal)
                    .findFirst();
            if (jec.isPresent()) {
                return new ResolucaoCompetencia(jec, esfera,
                        "Valor da causa elegível para JEC (até 40 salários mínimos)", false);
            }
        }

        Optional<InstituicaoJudicial> varaUnica = compativeis.stream()
                .filter(InstituicaoJudicial::eVaraUnica)
                .findFirst();

        if (varaUnica.isPresent()) {
            return new ResolucaoCompetencia(varaUnica, esfera,
                    "Vara única da comarca — competência universal de primeiro grau", false);
        }

        InstituicaoJudicial escolhido = compativeis.get(0);
        return new ResolucaoCompetencia(Optional.of(escolhido), esfera,
                "Órgão selecionado por compatibilidade de rito e matéria", false);
    }

    public boolean eCompetente(InstituicaoJudicial orgao, RitoProcessual rito, BigDecimal valorCausa) {
        if (!orgao.aceitaRito(rito)) return false;
        if (orgao.tipo() instanceof InstituicaoJudicialTipo.JuizadoEspecial
                || orgao.tipo() instanceof InstituicaoJudicialTipo.JuizadoEspecialFederal) {
            return valorCausa == null || valorCausa.compareTo(new BigDecimal("40000")) <= 0;
        }
        return true;
    }
}
