package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.repository.InfojudConsultaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.RenajudRestricaoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FinanceiroIntegracaoConstructorContractTest {

    @Test
    void infojudDeveFalharQuandoClienteForNulo() {
        assertThatThrownBy(() -> new InfojudConsultaService(
                Mockito.mock(ProcessoRepository.class),
                Mockito.mock(InfojudConsultaRepository.class),
                null,
                Mockito.mock(CurrentUserService.class),
                Mockito.mock(PjbAuthorizationService.class),
                Mockito.mock(AuditLedgerService.class),
                Mockito.mock(ReadAfterWriteConsistencyPolicy.class)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void renajudDeveFalharQuandoClienteForNulo() {
        assertThatThrownBy(() -> new RenajudRestricaoService(
                Mockito.mock(ProcessoRepository.class),
                Mockito.mock(RenajudRestricaoRepository.class),
                null,
                Mockito.mock(CurrentUserService.class),
                Mockito.mock(PjbAuthorizationService.class),
                Mockito.mock(AuditLedgerService.class),
                Mockito.mock(ReadAfterWriteConsistencyPolicy.class)))
                .isInstanceOf(NullPointerException.class);
    }
}
