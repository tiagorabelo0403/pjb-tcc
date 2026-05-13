package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.util.List;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CanalEntregaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoCaixaInstitucional;

public record AlvoInstitucionalJudicial(
        DestinatarioInstitucionalKind destinatarioKind,
        PapelProcessualInstitucional papelProcessual,
        String entidadeSigla,
        String unidadeCodigo,
        String unidadeSigla,
        String uf,
        String comarca,
        String foro,
        String especializacao,
        String competenciaMaterial,
        String competenciaTerritorial,
        CanalComunicacaoInstitucional canalPrincipal,
        List<CanalComunicacaoInstitucional> canaisSecundarios,
        boolean exigeCienciaPessoal,
        boolean permiteTriagem,
        boolean permiteCaixaPessoalFuncional,
        String fundamentoLegal
) {
    public static AlvoInstitucionalJudicial fromModel(com.tcc.pjb.backend.core.comunicacao.institucional.model.AlvoInstitucional model) {
        UnidadeInstitucional unidade = model.unidade();
        CaixaInstitucional caixa = model.caixa();
        CanalEntregaInstitucional principal = model.canalPrincipal();
        return new AlvoInstitucionalJudicial(
                model.destinatarioKind(),
                model.papelProcessual(),
                unidade != null ? unidade.sigla() : null,
                unidade != null ? unidade.codigo() : caixa != null ? caixa.unidadeCodigo() : null,
                unidade != null ? unidade.unidade() : caixa != null ? caixa.nomeExibicao() : null,
                unidade != null ? unidade.uf() : null,
                unidade != null ? unidade.comarca() : null,
                unidade != null ? unidade.foro() : null,
                unidade != null ? unidade.nucleo() : null,
                unidade != null && unidade.ramoDireito() != null ? unidade.ramoDireito().name() : null,
                unidade != null ? resolveCompetenciaTerritorial(unidade) : null,
                principal != null ? principal.canal() : null,
                model.canaisElegiveis().stream()
                        .map(CanalEntregaInstitucional::canal)
                        .filter(canal -> principal == null || canal != principal.canal())
                        .toList(),
                principal != null && principal.exigeCienciaPessoal(),
                caixa != null && caixa.permiteTriagem(),
                caixa != null && caixa.tipo() == TipoCaixaInstitucional.CAIXA_PESSOAL_FUNCIONAL,
                model.fundamentoLegal()
        );
    }

    private static String resolveCompetenciaTerritorial(UnidadeInstitucional unidade) {
        if (unidade.foro() != null && !unidade.foro().isBlank()) {
            return unidade.foro();
        }
        if (unidade.comarca() != null && !unidade.comarca().isBlank()) {
            return unidade.comarca();
        }
        return unidade.uf();
    }
}
