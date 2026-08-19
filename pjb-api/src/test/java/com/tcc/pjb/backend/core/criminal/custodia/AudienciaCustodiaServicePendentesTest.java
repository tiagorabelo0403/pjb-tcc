package com.tcc.pjb.backend.core.criminal.custodia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaPendenteView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.criminal.AudienciaCustodia;
import com.tcc.pjb.backend.model.repository.AudienciaCustodiaRepository;
import com.tcc.pjb.backend.model.repository.BnmpConsultaLogRepository;
import com.tcc.pjb.backend.model.repository.MedidaCautelarRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AudienciaCustodiaServicePendentesTest {

    private AudienciaCustodiaService service(AudienciaCustodiaRepository custodiaRepository) {
        return new AudienciaCustodiaService(
                mock(ProcessoRepository.class),
                custodiaRepository,
                mock(MedidaCautelarRepository.class),
                mock(BnmpConsultaLogRepository.class),
                mock(BnmpConsultaService.class),
                mock(CurrentUserService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                mock(AuditLedgerService.class));
    }

    @Test
    void marcaComoVencidaCustodiaComPrazoJaExpirado() {
        AudienciaCustodiaRepository custodiaRepository = mock(AudienciaCustodiaRepository.class);
        AudienciaCustodia vencida = AudienciaCustodia.builder()
                .id(1L).processoId(10L).presoNome("Fulano")
                .dataPrisao(Instant.parse("2020-01-01T08:00:00Z"))
                .prazoLimite24h(Instant.parse("2020-01-02T08:00:00Z"))
                .status("PENDENTE").createdAt(Instant.now()).build();
        AudienciaCustodia dentroDoPrazo = AudienciaCustodia.builder()
                .id(2L).processoId(11L).presoNome("Ciclano")
                .dataPrisao(Instant.now())
                .prazoLimite24h(Instant.now().plusSeconds(3600))
                .status("PENDENTE").createdAt(Instant.now()).build();
        when(custodiaRepository.findByStatusOrderByPrazoLimite24hAsc("PENDENTE")).thenReturn(List.of(vencida, dentroDoPrazo));

        List<CustodiaPendenteView> pendentes = service(custodiaRepository).pendentes();

        assertThat(pendentes).hasSize(2);
        assertThat(pendentes.get(0).vencida()).isTrue();
        assertThat(pendentes.get(1).vencida()).isFalse();
    }

    @Test
    void retornaListaVaziaQuandoNaoHaCustodiaPendente() {
        AudienciaCustodiaRepository custodiaRepository = mock(AudienciaCustodiaRepository.class);
        when(custodiaRepository.findByStatusOrderByPrazoLimite24hAsc("PENDENTE")).thenReturn(List.of());

        assertThat(service(custodiaRepository).pendentes()).isEmpty();
    }
}
