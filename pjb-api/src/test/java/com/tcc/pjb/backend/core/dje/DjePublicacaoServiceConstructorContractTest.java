package com.tcc.pjb.backend.core.dje;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.prazos.auditoria.PrazoAuditTrailService;
import com.tcc.pjb.backend.core.prazos.calculo.PrazosEngine;
import com.tcc.pjb.backend.model.repository.DjePublicacaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DjePublicacaoServiceConstructorContractTest {

    @Test
    void deveFalharQuandoDependenciaCriticaForNula() {
        assertThatThrownBy(() -> new DjePublicacaoService(
                Mockito.mock(ProcessoRepository.class),
                Mockito.mock(DjePublicacaoRepository.class),
                Mockito.mock(DjeHttpClient.class),
                Mockito.mock(PrazosEngine.class),
                Mockito.mock(AuditLedgerService.class),
                Mockito.mock(ReadAfterWriteConsistencyPolicy.class),
                Mockito.mock(PrazoAuditTrailService.class),
                null))
                .isInstanceOf(NullPointerException.class);
    }
}
