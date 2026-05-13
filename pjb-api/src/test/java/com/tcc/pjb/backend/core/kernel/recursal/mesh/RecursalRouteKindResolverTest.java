package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucao;

class RecursalRouteKindResolverTest {

    @Test
    void shouldClassifySuperiorExceptionalRoute() {
        RecursalCaseContext context = new RecursalCaseContext(
                1L,
                "0001",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "Apelação cível",
                RecursalClassFamily.CIVIL_CONHECIMENTO,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                InstanceLevel.SECOND_INSTANCE,
                OrgaoJulgadorTipo.CAMARA,
                false,
                true,
                false,
                false,
                true,
                false,
                true
        );
        RecursalRoutePlan plan = new RecursalRoutePlan(
                "TJ_RULE_PROFILE",
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                RecursalAuthority.VICE_PRESIDENCIA,
                RecursalTribunal.STJ,
                RecursalTribunalDetalhado.STJ,
                InstanceLevel.SUPERIOR,
                RecursalAuthority.RELATOR,
                RecursalAuthority.TURMA,
                PreparoDisposition.obrigatorio(true),
                new AdmissibilityDisposition(true, RecursalAuthority.VICE_PRESIDENCIA, true, RecursalAuthority.RELATOR, true, true, true, false),
                PreventionDisposition.strictSameRelator(),
                RemessaDisposition.externaAutuacaoDistribuicao()
        );
        assertEquals(RecursalRouteKind.SUPERIOR_EXCEPTIONAL,
                RecursalRouteKindResolver.resolve(context, new RecursoEspecial(true, true, false, false), plan));
    }

    @Test
    void shouldClassifyJuizadoRoute() {
        RecursalCaseContext context = new RecursalCaseContext(
                2L,
                "0002",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.JUIZADO_ESPECIAL_CIVEL,
                FaseProcessual.RECURSAL,
                "Recurso inominado",
                RecursalClassFamily.JUIZADO_ESPECIAL,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                InstanceLevel.FIRST_INSTANCE,
                OrgaoJulgadorTipo.MONOCRATICO,
                true,
                false,
                false,
                false,
                false,
                false,
                true
        );
        RecursalRoutePlan plan = new RecursalRoutePlan(
                "JUIZADO_RULE_PROFILE",
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                RecursalAuthority.JUIZO_SINGULAR,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                InstanceLevel.SECOND_INSTANCE,
                null,
                RecursalAuthority.TURMA,
                PreparoDisposition.obrigatorio(true),
                new AdmissibilityDisposition(true, RecursalAuthority.JUIZO_SINGULAR, false, null, true, false, false, false),
                new PreventionDisposition(true, false, true, true),
                RemessaDisposition.externaAutuacaoDistribuicao()
        );
        assertEquals(RecursalRouteKind.JUIZADO_TURMA_RECURSAL,
                RecursalRouteKindResolver.resolve(context, new RecursoInominadoJuizado(true, true, true, true), plan));
    }


    @Test
    void shouldClassifyJuizadoUniformizacaoRoute() {
        RecursalCaseContext context = new RecursalCaseContext(
                4L,
                "0004",
                TipoJustica.FEDERAL,
                RamoDireito.PREVIDENCIARIO,
                RitoProcessual.JUIZADO_ESPECIAL_FEDERAL,
                FaseProcessual.RECURSAL,
                "Pedido de uniformização",
                RecursalClassFamily.JUIZADO_ESPECIAL,
                RecursalTribunal.TRF,
                RecursalTribunalDetalhado.TRF5,
                InstanceLevel.SECOND_INSTANCE,
                OrgaoJulgadorTipo.TURMA,
                false,
                true,
                true,
                true,
                true,
                false,
                true
        );
        RecursalRoutePlan plan = new RecursalRoutePlan(
                "TNU_RULE_PROFILE",
                RecursalTribunal.TRF,
                RecursalTribunalDetalhado.TRF5,
                RecursalAuthority.PRESIDENCIA,
                RecursalTribunal.TNU,
                RecursalTribunalDetalhado.TNU,
                InstanceLevel.SECOND_INSTANCE,
                null,
                RecursalAuthority.TURMA,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(true, RecursalAuthority.PRESIDENCIA, false, null, false, true, false, false),
                new PreventionDisposition(true, false, true, true),
                RemessaDisposition.externaAutuacaoDistribuicao()
        );
        assertEquals(RecursalRouteKind.JUIZADO_UNIFORMIZACAO,
                RecursalRouteKindResolver.resolve(context, new PedidoUniformizacaoFederal(true, true, true, true), plan));
    }

    @Test
    void shouldClassifyExecutionIncidentInternalRoute() {
        RecursalCaseContext context = new RecursalCaseContext(
                3L,
                "0003",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.CUMPRIMENTO_SENTENCA,
                "Execução",
                RecursalClassFamily.CIVIL_EXECUCAO,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                InstanceLevel.FIRST_INSTANCE,
                OrgaoJulgadorTipo.MONOCRATICO,
                true,
                false,
                false,
                false,
                false,
                false,
                true
        );
        RecursalRoutePlan plan = new RecursalRoutePlan(
                "EXEC_RULE_PROFILE",
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                null,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                InstanceLevel.FIRST_INSTANCE,
                null,
                RecursalAuthority.JUIZO_SINGULAR,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                PreventionDisposition.strictSameRelator(),
                RemessaDisposition.internaAutuacaoDependencia()
        );
        assertEquals(RecursalRouteKind.EXECUTION_INCIDENT_INTERNAL,
                RecursalRouteKindResolver.resolve(context, new EmbargosExecucao(true, true, true, true), plan));
    }
}
