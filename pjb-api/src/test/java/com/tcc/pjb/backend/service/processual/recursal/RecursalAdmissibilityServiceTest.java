package com.tcc.pjb.backend.service.processual.recursal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AdmissibilityDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ApelacaoCivel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.PreparoDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.PreventionDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassFamily;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRoutePlan;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RemessaDisposition;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshContextRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.service.processual.prazo.PrazoProcessualNacionalService;
import com.tcc.pjb.backend.service.processual.recursal.protocolo.RecursalTransmissionResolver;
import com.tcc.pjb.backend.service.processual.recursal.admissibilidade.RecursalAdmissibilityResolver;
import com.tcc.pjb.backend.service.processual.recursal.admissibilidade.RecursalAdmissibilityService;
import com.tcc.pjb.backend.core.processo.recursal.domain.PreclusaoTipo;
import com.tcc.pjb.backend.service.recursal.mesh.NationalRecursalMeshService;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RecursalAdmissibilityServiceTest {

    @Test
    void shouldMarkUntimelyAppealAsTemporalPreclusion() {
        NationalRecursalMeshService meshService = Mockito.mock(NationalRecursalMeshService.class);
        PrazoProcessualNacionalService prazoService = Mockito.mock(PrazoProcessualNacionalService.class);
        RecursalMeshContextRequest context = new RecursalMeshContextRequest(
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
        RecursalMeshSpeciesRequest species = new RecursalMeshSpeciesRequest(
                RecursalMeshSpeciesType.APCIV,
                Set.of(),
                null,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );
        RecursalMeshPlanRequest request = new RecursalMeshPlanRequest("R1", context, species);
        RecursalRoutePlan routePlan = new RecursalRoutePlan(
                "APELACAO_CIVEL",
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                RecursalAuthority.JUIZO_SINGULAR,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                InstanceLevel.SECOND_INSTANCE,
                RecursalAuthority.CAMARA,
                RecursalAuthority.CAMARA,
                PreparoDisposition.obrigatorio(true),
                new AdmissibilityDisposition(true, RecursalAuthority.JUIZO_SINGULAR, false, null, true, false, false, false),
                PreventionDisposition.strictSameRelator(),
                RemessaDisposition.externaAutuacaoDistribuicao()
        );
        when(meshService.plan(any())).thenReturn(new RecursalPlanningResult(
                new ApelacaoCivel(true, false, false, false),
                new com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalCaseContext(1L, "0001", TipoJustica.ESTADUAL, RamoDireito.CIVIL, RitoProcessual.COMUM_ORDINARIO, FaseProcessual.RECURSAL, "Apelação cível", RecursalClassFamily.CIVIL_CONHECIMENTO, RecursalTribunal.TJ, RecursalTribunalDetalhado.TJCE, InstanceLevel.FIRST_INSTANCE, OrgaoJulgadorTipo.MONOCRATICO, true, false, false, false, false, false, true),
                routePlan,
                RecursalStateSnapshot.newDraft("R1", new com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalCaseContext(1L, "0001", TipoJustica.ESTADUAL, RamoDireito.CIVIL, RitoProcessual.COMUM_ORDINARIO, FaseProcessual.RECURSAL, "Apelação cível", RecursalClassFamily.CIVIL_CONHECIMENTO, RecursalTribunal.TJ, RecursalTribunalDetalhado.TJCE, InstanceLevel.FIRST_INSTANCE, OrgaoJulgadorTipo.MONOCRATICO, true, false, false, false, false, false, true)),
                Set.of(RecursalTransitionEvent.PROTOCOLAR)
        ));
        when(prazoService.calcular(any())).thenReturn(new PrazoProcessualNacionalService.PrazoProcessualResult(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 15),
                14,
                10,
                10,
                NationalPrazoEngine.TipoPrazo.APELACAO,
                RamoDireito.CIVIL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "TJCE",
                "CE",
                "Quixadá",
                true,
                "Dia útil forense",
                java.util.List.of(),
                "CPC",
                "Calendário"
        ));
        RecursalAdmissibilityService service = new RecursalAdmissibilityService(meshService, prazoService, mock(RecursalAdmissibilityResolver.class), mock(RecursalTransmissionResolver.class));
        var result = service.avaliar(new RecursalAdmissibilityService.RecursalAdmissibilityCommand(
                request,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 20),
                "TJCE",
                "CE",
                "Quixadá",
                true,
                false,
                false,
                false
        ));
        assertFalse(result.admissivelEmTese());
        assertFalse(result.tempestivo());
        assertEquals(PreclusaoTipo.TEMPORAL, result.preclusao());
        assertTrue(result.preparoSatisfeito());
    }
}
