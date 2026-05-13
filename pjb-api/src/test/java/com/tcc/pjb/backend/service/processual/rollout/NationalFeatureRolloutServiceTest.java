package com.tcc.pjb.backend.service.processual.rollout;

import com.tcc.pjb.backend.configs.PjbFeatureFlagsProperties;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorPolicyOverlay;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorPolicyService;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.dto.processual.rollout.NationalFeatureRolloutRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

class NationalFeatureRolloutServiceTest {

    @Test
    void enablesPilotForCriticalProfile() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        JudicialConnectorPolicyService policyService = Mockito.mock(JudicialConnectorPolicyService.class);
        PjbFeatureFlagsProperties properties = new PjbFeatureFlagsProperties();
        properties.getWorkflow().setEnabled(true);
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        NationalFeatureRolloutService service = new NationalFeatureRolloutService(processoRepository, authorizationService, policyService, properties, currentUserService);
        Processo processo = new Processo();
        processo.setId(99L);
        processo.setNumeroProcesso("00099-10");
        processo.setConnectorSystem("PJE");
        when(processoRepository.findById(99L)).thenReturn(Optional.of(processo));
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setTipoUsuario(TipoUsuario.JUIZ);
        when(currentUserService.getOrNull()).thenReturn(usuario);
        when(policyService.resolve(JudicialSystem.PJE, null)).thenReturn(new JudicialConnectorPolicyOverlay(null, JudicialSystem.PJE, "default", null, true, true, true, false, false, false, null, null, null, null, null, null, "PILOT", null, null, null, null, java.util.List.of(), java.util.List.of(), java.util.Map.of()));

        var response = service.resolve(new NationalFeatureRolloutRequest("workflow", 99L, null, null, null, null, null, null));

        assertEquals("WORKFLOW", response.featureCode());
        assertTrue(response.enabled());
        assertEquals("PILOT", response.rolloutMode());
    }

    @Test
    void disablesWhenMaintenanceModeIsActive() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        JudicialConnectorPolicyService policyService = Mockito.mock(JudicialConnectorPolicyService.class);
        PjbFeatureFlagsProperties properties = new PjbFeatureFlagsProperties();
        properties.getWorkflow().setEnabled(true);
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        NationalFeatureRolloutService service = new NationalFeatureRolloutService(processoRepository, authorizationService, policyService, properties, currentUserService);
        when(currentUserService.getOrNull()).thenReturn(null);
        when(policyService.resolve(JudicialSystem.PJE, "TJCE")).thenReturn(new JudicialConnectorPolicyOverlay(null, JudicialSystem.PJE, "default", "TJCE", true, true, true, false, false, true, null, null, null, null, null, null, "FULL", null, null, null, null, java.util.List.of(), java.util.List.of(), java.util.Map.of()));

        var response = service.resolve(new NationalFeatureRolloutRequest("workflow", null, "TJCE", JudicialSystem.PJE, "SERVIDOR", null, null, null));

        assertFalse(response.enabled());
    }
}
