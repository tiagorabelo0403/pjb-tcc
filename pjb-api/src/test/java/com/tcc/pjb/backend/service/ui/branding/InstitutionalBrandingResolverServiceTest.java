package com.tcc.pjb.backend.service.ui.branding;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.ui.InstitutionalBrandingProperties;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InstitutionalBrandingResolverServiceTest {

    private final InstitutionalBrandingProperties properties = new InstitutionalBrandingProperties();
    private final InstitutionalBrandingPolicyService policyService = new InstitutionalBrandingPolicyService(properties);
    private final InstitutionalBrandingResolverService service = new InstitutionalBrandingResolverService(new ObjectMapper(), properties, policyService);

    @Test
    void shouldResolveMinisterioPublicoProfileFromActorLane() {
        Map<String, Object> profile = service.resolveProfile(new InstitutionalBrandingResolverService.ResolveRequest(
                "MINISTERIO_PUBLICO",
                "PARECER_MINISTERIAL",
                TipoUsuario.MEMBRO_MINISTERIO_PUBLICO,
                Map.of("unitDisplayName", "2ª Promotoria Criminal")
        ));
        assertThat(profile.get("profileCode")).isEqualTo("MINISTERIO_PUBLICO");
        assertThat(profile.get("displayName")).isEqualTo("Ministério Público");
        assertThat(profile.get("unitDisplayName")).isEqualTo("2ª Promotoria Criminal");
        assertThat(profile).containsEntry("databaseBlobForbidden", true);
    }

    @Test
    void shouldRespectGovernedBrandingProfileCodeWhenKnown() {
        Map<String, Object> profile = service.resolveProfile(new InstitutionalBrandingResolverService.ResolveRequest(
                "INSTITUCIONAL",
                "LAUDO_PERICIAL",
                TipoUsuario.PERITO_DIGITAL,
                Map.of("brandingProfileCode", "PERICIA")
        ));
        assertThat(profile.get("profileCode")).isEqualTo("PERICIA");
    }
}
