package com.tcc.pjb.backend.core.comunicacao.institucional.workflow;

import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDelegationAssignment;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDraftManifestation;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusDelegacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusMinutaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoFluxoDelegacaoInstitucional;
import java.time.Instant;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InstitutionalWorkflowDomainTest {

    @Test
    void delegationShouldExpireOutsideValidityWindow() {
        Instant now = Instant.parse("2026-03-20T12:00:00Z");
        InstitutionalDelegationAssignment assignment = new InstitutionalDelegationAssignment(
                "a1", "exp-1", 10L, "UNI", "CX", 1L, 2L,
                TipoFluxoDelegacaoInstitucional.DELEGACAO,
                EnumSet.of(CapacidadeCaixaInstitucional.PREPARAR_MINUTA),
                StatusDelegacaoInstitucional.ATIVA,
                "teste",
                now,
                now.plusSeconds(60),
                now,
                now,
                "hash"
        );
        assertTrue(assignment.ativaEm(now.plusSeconds(30)));
        assertFalse(assignment.ativaEm(now.plusSeconds(61)));
    }

    @Test
    void draftShouldMoveAcrossApprovalStates() {
        Instant now = Instant.parse("2026-03-20T12:00:00Z");
        InstitutionalDraftManifestation draft = new InstitutionalDraftManifestation(
                "d1", "exp-1", 10L, "UNI", "CX", 1L, null,
                StatusMinutaInstitucional.RASCUNHO,
                "Titulo", "Conteudo", null,
                now, null, null, now, "hash"
        );
        InstitutionalDraftManifestation submitted = draft.withSubmissao(2L, "encaminhada", now.plusSeconds(10), "hash2");
        InstitutionalDraftManifestation approved = submitted.withAprovacao("ok", now.plusSeconds(20), "hash3");
        assertEquals(StatusMinutaInstitucional.EM_APROVACAO, submitted.status());
        assertEquals(StatusMinutaInstitucional.APROVADA, approved.status());
        assertEquals(2L, approved.aprovadorUsuarioId());
    }
}
