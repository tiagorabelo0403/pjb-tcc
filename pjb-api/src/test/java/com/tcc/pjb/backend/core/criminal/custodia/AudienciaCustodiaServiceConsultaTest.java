package com.tcc.pjb.backend.core.criminal.custodia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaPrazoConsultaCommand;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.criminal.AudienciaCustodia;
import com.tcc.pjb.backend.model.repository.AudienciaCustodiaRepository;
import com.tcc.pjb.backend.model.repository.BnmpConsultaLogRepository;
import com.tcc.pjb.backend.model.repository.MedidaCautelarRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AudienciaCustodiaServiceConsultaTest {

    @Test
    void deveConsultarCustodiaEPrazo() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        AudienciaCustodiaRepository custodiaRepository = mock(AudienciaCustodiaRepository.class);
        AudienciaCustodia entity = AudienciaCustodia.builder()
                .id(12L)
                .processoId(99L)
                .presoNome("Fulano")
                .dataPrisao(Instant.now().plusSeconds(3600))
                .prazoLimite24h(Instant.now().plusSeconds(90000))
                .status("PENDENTE")
                .createdAt(Instant.now())
                .build();
        when(custodiaRepository.findById(12L)).thenReturn(Optional.of(entity));
        AudienciaCustodiaService service = new AudienciaCustodiaService(
                processoRepository,
                custodiaRepository,
                mock(MedidaCautelarRepository.class),
                mock(BnmpConsultaLogRepository.class),
                mock(BnmpConsultaService.class),
                mock(CurrentUserService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                mock(AuditLedgerService.class));

        var consulta = service.consultar(new CustodiaConsultaCommand(12L));
        var prazo = service.consultarPrazo(new CustodiaPrazoConsultaCommand(12L));

        assertThat(consulta.id()).isEqualTo(12L);
        assertThat(prazo.custodiaId()).isEqualTo(12L);
        assertThat(prazo.vencido()).isFalse();
    }
}
