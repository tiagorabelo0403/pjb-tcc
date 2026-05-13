package com.tcc.pjb.backend.service.governance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.lgpd.LgpdProcessualSensibilityEngine;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SensitiveDataAccessControlServiceTest {

    @Test
    void shouldRequireJustificationForMaximumSensitivityExceptionalAccess() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        LgpdProcessualSensibilityEngine sensibilityEngine = Mockito.mock(LgpdProcessualSensibilityEngine.class);
        AuditLedgerService auditLedgerService = Mockito.mock(AuditLedgerService.class);
        Processo processo = new Processo();
        processo.setId(5L);
        processo.setNumeroProcesso("0005");
        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setTipoUsuario(TipoUsuario.DEFENSOR_PUBLICO);
        when(processoRepository.findById(5L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(authorizationService.canReadProcesso(processo)).thenReturn(new AuthzDecision(true, "OK", "v1"));
        when(sensibilityEngine.classificar(5L)).thenReturn(new LgpdProcessualSensibilityEngine.SensibilidadeReport(5L, "0005", LgpdProcessualSensibilityEngine.NivelSensibilidade.MAXIMO, java.util.List.of(), java.util.List.of("Step-up"), 30L, Instant.now(), true));
        SensitiveDataAccessControlService service = new SensitiveDataAccessControlService(
                processoRepository,
                currentUserService,
                authorizationService,
                sensibilityEngine,
                auditLedgerService
        );
        var denied = service.avaliar(5L, true, true, false, false, "atuação defensiva");
        assertFalse(denied.permitido());
        assertTrue(denied.restricoes().stream().anyMatch(v -> v.contains("Justificativa")));
    }
}
