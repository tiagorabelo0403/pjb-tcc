package com.tcc.pjb.backend.service.institutional.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalArchitectureResponse;
import org.junit.jupiter.api.Test;

class InstitutionalVisibilityGuardrailServiceTest {

    private final InstitutionalVisibilityGuardrailService service = new InstitutionalVisibilityGuardrailService();

    @Test
    void shouldPrioritizeLocalJurisdictionWhenUnitMatches() {
        AdminInstitutionalArchitectureResponse.VisibilitySimulation simulation = service.simulate(true, true, true, true, false, true);

        assertThat(simulation.allowed()).isTrue();
        assertThat(simulation.tierCode()).isEqualTo("LOCAL");
        assertThat(simulation.restrictions()).contains("sigilo_processual_reforcado");
    }

    @Test
    void shouldGrantCooperationWhenTemporaryLinkExists() {
        AdminInstitutionalArchitectureResponse.VisibilitySimulation simulation = service.simulate(false, false, true, false, false, false);

        assertThat(simulation.allowed()).isTrue();
        assertThat(simulation.tierCode()).isEqualTo("COOPERACAO");
        assertThat(simulation.timeBound()).isTrue();
    }

    @Test
    void shouldDenyWhenNoTopologicalOrFunctionalLinkExists() {
        AdminInstitutionalArchitectureResponse.VisibilitySimulation simulation = service.simulate(false, false, false, false, false, false);

        assertThat(simulation.allowed()).isFalse();
        assertThat(simulation.tierCode()).isEqualTo("NEGADO");
    }
}
