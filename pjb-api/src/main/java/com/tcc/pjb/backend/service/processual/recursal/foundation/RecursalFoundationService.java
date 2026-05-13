package com.tcc.pjb.backend.service.processual.recursal.foundation;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalApelacaoBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalEfeito;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalFormalSectionLabels;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalFundamentacaoPerfil;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalJuizoAdmissibilidadeCompetencia;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalMeritoErroTipo;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalMomentoInterposicao;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPrazoRegra;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPressupostoGenerico;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursoAdesivoRegra;
import com.tcc.pjb.backend.model.dto.processual.recursal.foundation.RecursalApelacaoBlueprintView;
import com.tcc.pjb.backend.model.dto.processual.recursal.foundation.RecursalFoundationResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.foundation.RecursalPrazoRuleView;
import com.tcc.pjb.backend.model.dto.processual.recursal.foundation.RecursoAdesivoRuleView;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecursalFoundationService {

    public RecursalFoundationResponse describe() {
        return new RecursalFoundationResponse(
                enumNames(Set.of("TOTAL", "PARCIAL")),
                enumNames(RecursalFundamentacaoPerfil.values()),
                enumNames(RecursalEfeito.values()),
                enumNames(RecursalMomentoInterposicao.values()),
                buildPrazoRules(),
                buildRecursoAdesivoRule(),
                buildApelacaoBlueprint(),
                enumNames(RecursalJuizoAdmissibilidadeCompetencia.values()),
                enumNames(RecursalMeritoErroTipo.values()),
                RecursalFormalSectionLabels.defaultOrder()
        );
    }

    private List<RecursalPrazoRuleView> buildPrazoRules() {
        return List.of(
                toView(new RecursalPrazoRegra("APELACAO", 15, true, true, true, true, true)),
                toView(new RecursalPrazoRegra("AGRAVO_DE_INSTRUMENTO", 15, true, true, true, true, true)),
                toView(new RecursalPrazoRegra("AGRAVO_INTERNO", 15, true, true, true, true, true)),
                toView(new RecursalPrazoRegra("RECURSO_ESPECIAL", 15, true, true, true, true, true)),
                toView(new RecursalPrazoRegra("RECURSO_EXTRAORDINARIO", 15, true, true, true, true, true)),
                toView(new RecursalPrazoRegra("EMBARGOS_DECLARACAO", 5, true, true, true, true, true))
        );
    }

    private RecursoAdesivoRuleView buildRecursoAdesivoRule() {
        RecursoAdesivoRegra regra = new RecursoAdesivoRegra(
                Set.of("APELACAO", "RECURSO_ESPECIAL", "RECURSO_EXTRAORDINARIO"),
                true,
                true,
                true,
                true
        );
        return new RecursoAdesivoRuleView(
                regra.recursosCabiveis(),
                regra.exigeSucumbenciaReciproca(),
                regra.prazoSegueContrarrazoes(),
                regra.subordinadoAoPrincipal(),
                regra.extingueSePrincipalDesistidoOuInadmitido()
        );
    }

    private RecursalApelacaoBlueprintView buildApelacaoBlueprint() {
        RecursalApelacaoBlueprint blueprint = new RecursalApelacaoBlueprint(
                15,
                true,
                true,
                true,
                List.of(RecursalFormalSectionLabels.PETICAO_INTERPOSICAO, RecursalFormalSectionLabels.RAZOES_RECURSAIS),
                Set.of(
                        RecursalPressupostoGenerico.CABIMENTO_ADEQUACAO,
                        RecursalPressupostoGenerico.LEGITIMIDADE,
                        RecursalPressupostoGenerico.INTERESSE_RECURSAL,
                        RecursalPressupostoGenerico.TEMPESTIVIDADE,
                        RecursalPressupostoGenerico.PREPARO,
                        RecursalPressupostoGenerico.REGULARIDADE_FORMAL
                )
        );
        return new RecursalApelacaoBlueprintView(
                blueprint.prazoDiasUteis(),
                blueprint.cabivelContraSentenca(),
                blueprint.juizAquonaoFazJuizoAdmissibilidade(),
                blueprint.admitePreliminarContraInterlocutoriaNaoAgravavel(),
                blueprint.pecasObrigatorias(),
                blueprint.pressupostosGenericos().stream().map(Enum::name).collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }

    private static RecursalPrazoRuleView toView(RecursalPrazoRegra regra) {
        return new RecursalPrazoRuleView(
                regra.recurso(),
                regra.diasUteis(),
                regra.contaDaPostagemViaCorreio(),
                regra.exigeComprovacaoFeriadoLocal(),
                regra.suspendeNoRecessoForense(),
                regra.admitePrazoEmDobroFazendaPublica(),
                regra.admitePrazoEmDobroLitisconsortes()
        );
    }

    private static Set<String> enumNames(Enum<?>[] values) {
        return java.util.Arrays.stream(values)
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> enumNames(Set<String> values) {
        return new LinkedHashSet<>(values);
    }
}
