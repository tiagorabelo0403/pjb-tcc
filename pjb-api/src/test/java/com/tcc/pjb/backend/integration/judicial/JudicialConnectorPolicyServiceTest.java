package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorPolicy;
import com.tcc.pjb.backend.model.repository.JudicialConnectorPolicyRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

class JudicialConnectorPolicyServiceTest {

    @Test
    void resolvesTribunalOverlayOverGlobalPolicy() {
        JudicialConnectorPolicyRepository repository = Mockito.mock(JudicialConnectorPolicyRepository.class);
        Environment environment = Mockito.mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
        when(repository.findAllByConnectorSystemAndActiveTrueOrderByCreatedAtDesc(JudicialSystem.PJE)).thenReturn(List.of(global(), tribunal()));
        JudicialConnectorPolicyService service = new JudicialConnectorPolicyService(repository, environment);

        JudicialConnectorPolicyOverlay overlay = service.resolve(JudicialSystem.PJE, "TJCE");

        assertThat(overlay.policyPresent()).isTrue();
        assertThat(overlay.productionReady()).isTrue();
        assertThat(overlay.tribunalHomologated()).isTrue();
        assertThat(overlay.quarantineEnabled()).isTrue();
        assertThat(overlay.submitPath()).isEqualTo("/tribunal/submit");
        assertThat(overlay.blockers()).contains("CONNECTOR_POLICY_QUARANTINED");
    }

    private JudicialConnectorPolicy global() {
        JudicialConnectorPolicy policy = new JudicialConnectorPolicy();
        policy.setConnectorSystem(JudicialSystem.PJE);
        policy.setEnvironmentName("PROD");
        policy.setProductionReady(Boolean.TRUE);
        policy.setSubmitPath("/global/submit");
        policy.setUpdatedAt(Instant.now().minusSeconds(60));
        return policy;
    }

    private JudicialConnectorPolicy tribunal() {
        JudicialConnectorPolicy policy = new JudicialConnectorPolicy();
        policy.setConnectorSystem(JudicialSystem.PJE);
        policy.setEnvironmentName("PROD");
        policy.setTribunalCodigo("TJCE");
        policy.setTribunalHomologated(Boolean.TRUE);
        policy.setQuarantineEnabled(Boolean.TRUE);
        policy.setSubmitPath("/tribunal/submit");
        policy.setUpdatedAt(Instant.now());
        return policy;
    }
}
