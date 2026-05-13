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
import com.tcc.pjb.backend.model.entity.enums.OperacaoProcessualCritica;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SegregacaoFuncionalProcessualServiceTest {

    @Test
    void shouldBlockSignatureWithoutStepUpForJudge() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        LgpdProcessualSensibilityEngine sensibilityEngine = Mockito.mock(LgpdProcessualSensibilityEngine.class);
        AuditLedgerService auditLedgerService = Mockito.mock(AuditLedgerService.class);
        Processo processo = new Processo();
        processo.setId(1L);
        processo.setNumeroProcesso("0001");
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setTipoUsuario(TipoUsuario.JUIZ);
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(authorizationService.canReadProcesso(processo)).thenReturn(new AuthzDecision(true, "OK", "v1"));
        when(sensibilityEngine.classificar(1L)).thenReturn(new LgpdProcessualSensibilityEngine.SensibilidadeReport(1L, "0001", LgpdProcessualSensibilityEngine.NivelSensibilidade.NORMAL, java.util.List.of(), java.util.List.of(), 30L, java.time.Instant.now(), false));
        SegregacaoFuncionalProcessualService service = new SegregacaoFuncionalProcessualService(
                processoRepository,
                currentUserService,
                authorizationService,
                sensibilityEngine,
                auditLedgerService
        );
        var response = service.avaliar(1L, OperacaoProcessualCritica.ASSINAR, false, true, true, true, "assinatura de sentença");
        assertFalse(response.permitido());
        assertTrue(response.exigencias().stream().anyMatch(v -> v.contains("Step-up")));
    }
}
