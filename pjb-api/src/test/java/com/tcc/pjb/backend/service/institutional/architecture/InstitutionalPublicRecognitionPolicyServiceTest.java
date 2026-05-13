package com.tcc.pjb.backend.service.institutional.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import org.junit.jupiter.api.Test;

class InstitutionalPublicRecognitionPolicyServiceTest {

    private final InstitutionalPublicRecognitionPolicyService service = new InstitutionalPublicRecognitionPolicyService();

    @Test
    void shouldRecognizeAutomaticallyWhenOfficialJudicialAnchorsArePresent() {
        AdminInstitutionalPublicRecognitionResponse response = service.assess(
                "JUDICIARIO_CNJ",
                true,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                false,
                false
        );

        assertThat(response.statusCode()).isEqualTo("RECONHECIDA_AUTOMATICAMENTE");
        assertThat(response.recognized()).isTrue();
        assertThat(response.autoActivatable()).isTrue();
    }

    @Test
    void shouldRequireAssistedHomologationForSubordinateUnitWithoutOwnCnpj() {
        AdminInstitutionalPublicRecognitionResponse response = service.assess(
                "SUBUNIDADE_VINCULADA",
                false,
                false,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true
        );

        assertThat(response.statusCode()).isEqualTo("RECONHECIDA_COM_HOMOLOGACAO");
        assertThat(response.recognized()).isTrue();
        assertThat(response.humanReviewRequired()).isTrue();
    }

    @Test
    void shouldDenyWhenThereIsNoPublicAnchor() {
        AdminInstitutionalPublicRecognitionResponse response = service.assess(
                "ESTADUAL_MUNICIPAL",
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

        assertThat(response.statusCode()).isEqualTo("NEGADA");
        assertThat(response.blockers()).contains("sem_ancora_institucional_oficial_suficiente");
    }
}
