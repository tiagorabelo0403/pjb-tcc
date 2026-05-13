package com.tcc.pjb.backend.service.processual.linkage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.linkage.ProcessoLinkageAnalysisRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.VinculoProcessualTipo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProcessoLinkageGovernanceServiceTest {

    @Test
    void shouldDetectContinenciaCandidate() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        AuditLedgerService auditLedgerService = Mockito.mock(AuditLedgerService.class);
        Processo base = new Processo();
        base.setId(10L);
        base.setNumeroProcesso("00010");
        base.setParteAutoraCpf("11111111111");
        base.setParteReuCpf("22222222222");
        base.setClasseProcessual("Procedimento Comum");
        base.setAssunto("Indenização por danos");
        Processo correlato = new Processo();
        correlato.setId(1L);
        correlato.setNumeroProcesso("00001");
        correlato.setParteAutoraCpf("11111111111");
        correlato.setParteReuCpf("22222222222");
        correlato.setClasseProcessual("Procedimento Comum");
        correlato.setAssunto("Indenização por danos");
        when(processoRepository.findById(10L)).thenReturn(Optional.of(base));
        when(processoRepository.findAllByPartesCpf("11111111111")).thenReturn(List.of(correlato));
        when(processoRepository.findAllByPartesCpf("22222222222")).thenReturn(List.of(correlato));
        when(processoRepository.findByComarcaAndUf(null, null, org.springframework.data.domain.PageRequest.of(0, 80)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(correlato)));
        ProcessoLinkageGovernanceService service = new ProcessoLinkageGovernanceService(
                processoRepository,
                currentUserService,
                authorizationService,
                auditLedgerService,
                Mockito.mock(com.tcc.pjb.backend.service.casefile.CaseContinuityOrchestratorService.class)
        );
        var response = service.analisar(new ProcessoLinkageAnalysisRequest(10L, null, true, true, false));
        assertFalse(response.candidatos().isEmpty());
        assertEquals(VinculoProcessualTipo.CONTINENCIA, response.candidatos().getFirst().vinculoTipo());
    }
}
