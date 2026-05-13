package com.tcc.pjb.backend.governance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DecisionSecurityWiringGuardTest {

    @Test
    void security_config_keeps_decision_filters_wired_in_sequence() throws Exception {
        Path source = Path.of("src/main/java/com/tcc/pjb/backend/configs/SecurityConfig.java");
        String java = Files.readString(source);
        assertTrue(java.contains("http.addFilterAfter(decisionStepUpFilter, MinisterStepUpFilter.class);"),
                "DecisionStepUpFilter deve permanecer encadeado após o step-up ministerial.");
        assertTrue(java.contains("http.addFilterAfter(decisionClientBindingFilter, DecisionStepUpFilter.class);"),
                "DecisionClientBindingFilter deve permanecer após o filtro de credencial decisória.");
    }

    @Test
    void decision_safety_controller_keeps_binding_routes() throws Exception {
        Path source = Path.of("src/main/java/com/tcc/pjb/backend/controller/julgamento/DecisionSafetyController.java");
        String java = Files.readString(source);
        assertTrue(java.contains("@PostMapping(\"/foco/{sessionId}/heartbeat\")"),
                "Endpoint de heartbeat do foco decisional não pode ser removido.");
        assertTrue(java.contains("decisionSafetyService.openFocus(processoId, binding, request == null ? null : request.tabBinding(), request == null ? null : request.routeBinding())"),
                "Open focus deve continuar levando janela, aba e rota para o serviço.");
    }

    @Test
    void juiz_service_keeps_blindagem_for_sensitive_despacho() throws Exception {
        Path source = Path.of("src/main/java/com/tcc/pjb/backend/service/juiz/decision/JuizGabineteDecisionalService.java");
        String java = Files.readString(source);
        assertTrue(java.contains("requireSafeDecisionContext(processo, usuario, \"DESPACHO\""),
                "Despacho sensível deve continuar protegido pela blindagem decisória.");
        assertTrue(java.contains("registrarConferenciaCruzadaSeNecessario(processo, usuario, \"DESPACHO\""),
                "Despacho sensível deve continuar gerando conferência cruzada quando o perfil exigir.");
    }
}
