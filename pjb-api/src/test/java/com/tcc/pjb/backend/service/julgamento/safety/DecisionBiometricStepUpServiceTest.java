package com.tcc.pjb.backend.service.julgamento.safety;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualDescriptor;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityPolicyService;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityProfile;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenPayload;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.julgamento.DecisionFocusSession;
import com.tcc.pjb.backend.model.repository.julgamento.DecisionStepUpConsumptionRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;

class DecisionBiometricStepUpServiceTest {

    private final DecisionStepUpConsumptionRepository repository = mock(DecisionStepUpConsumptionRepository.class);
    private final AtoProcessualSecurityPolicyService atoProcessualSecurityPolicyService = mock(AtoProcessualSecurityPolicyService.class);
    private final DecisionClientBindingGuardService decisionClientBindingGuardService = mock(DecisionClientBindingGuardService.class);
    private final DecisionBiometricStepUpService service = new DecisionBiometricStepUpService(
            repository,
            atoProcessualSecurityPolicyService,
            decisionClientBindingGuardService
    );

    private void stubActType(String actType) {
        AtoProcessualDescriptor descriptor = new AtoProcessualDescriptor(
                actType,
                actType,
                null,
                null,
                null,
                null,
                null,
                AtoProcessualSecurityProfile.reinforced()
        );
        when(atoProcessualSecurityPolicyService.descriptorForActType(actType)).thenReturn(descriptor);
        when(atoProcessualSecurityPolicyService.canonicalActType(actType)).thenReturn(actType);
    }

    @Test
    void consumes_valid_stepup_once() {
        stubActType("SENTENCA");
        Usuario usuario = user(7L);
        Processo processo = processo(11L);
        DecisionFocusSession session = focus(17L);
        String normalizedText = "sentenca condenatoria procede em parte";
        String requestHash = Hashes.sha256Hex(normalizedText);
        when(repository.existsByTokenJti("jti-1")).thenReturn(false);

        RequestContext.run("req-stepup-ok", () -> {
            RequestContext.setDecisionStepUpCredential(new DecisionStepUpTokenPayload(
                    "jti-1", 7L, now(), now() + 60, "FACE_DECISAO", "SENTENCA", 11L, 17L,
                    "win-1", "tab-1", "/app/processos/11/julgamento", requestHash));
            assertDoesNotThrow(() -> service.requireAndConsume(usuario, processo, session, "SENTENCA", normalizedText));
        });
        verify(repository).save(any());
    }

    @Test
    void rejects_mismatched_stepup_payload() {
        stubActType("SENTENCA");
        Usuario usuario = user(7L);
        Processo processo = processo(11L);
        DecisionFocusSession session = focus(17L);
        when(repository.existsByTokenJti("jti-2")).thenReturn(false);

        RequestContext.run("req-stepup-bad", () -> {
            RequestContext.setDecisionStepUpCredential(new DecisionStepUpTokenPayload(
                    "jti-2", 7L, now(), now() + 60, "FACE_DECISAO", "SENTENCA", 11L, 17L,
                    "win-1", "tab-1", "/app/processos/11/julgamento", Hashes.sha256Hex("texto diferente")));
            assertThrows(ErroDeValidacaoException.class, () -> service.requireAndConsume(usuario, processo, session, "SENTENCA", "sentenca correta"));
        });
    }

    private long now() {
        return java.time.Instant.now().getEpochSecond();
    }

    private Usuario user(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoUsuario(TipoUsuario.JUIZ);
        return usuario;
    }

    private Processo processo(Long id) {
        Processo processo = new Processo();
        processo.setId(id);
        return processo;
    }

    private DecisionFocusSession focus(Long id) {
        DecisionFocusSession session = new DecisionFocusSession();
        session.setId(id);
        session.setWindowBinding("win-1");
        session.setTabBinding("tab-1");
        return session;
    }
}
