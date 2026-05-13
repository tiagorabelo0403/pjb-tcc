package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.financeiro.InfojudConsulta;
import com.tcc.pjb.backend.model.entity.financeiro.RenajudRestricao;
import com.tcc.pjb.backend.model.entity.financeiro.SisbajudOperacao;
import com.tcc.pjb.backend.model.repository.InfojudConsultaRepository;
import com.tcc.pjb.backend.model.repository.RenajudRestricaoRepository;
import com.tcc.pjb.backend.model.repository.SisbajudOperacaoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegracaoJudicialFinanceiraLifecycleServiceTest {

    @Test
    void deveReprocessarFalhasDasIntegracoes() {
        SisbajudOperacaoRepository sisbRepo = mock(SisbajudOperacaoRepository.class);
        RenajudRestricaoRepository renaRepo = mock(RenajudRestricaoRepository.class);
        InfojudConsultaRepository infoRepo = mock(InfojudConsultaRepository.class);
        when(sisbRepo.findRetryCandidates(any())).thenReturn(List.of(SisbajudOperacao.builder()
                .id(1L).processoId(10L).cpfDevedor("12345678900").valorSolicitado(BigDecimal.TEN).numeroOficio("OF1").status("FAILED").tentativas(1).createdAt(Instant.now()).build()));
        when(renaRepo.findRetryCandidates(any())).thenReturn(List.of(RenajudRestricao.builder()
                .id(2L).processoId(11L).placa("ABC1234").renavam("12345678901").tipo("RESTRICAO").status("FAILED").tentativas(1).createdAt(Instant.now()).build()));
        when(infoRepo.findRetryCandidates(any())).thenReturn(List.of(InfojudConsulta.builder()
                .id(3L).processoId(12L).cpfCnpjConsultado("12345678900").status("FAILED").tentativas(1).createdAt(Instant.now()).build()));
        when(sisbRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(renaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(infoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        IntegracaoJudicialFinanceiraLifecycleService service = new IntegracaoJudicialFinanceiraLifecycleService(
                sisbRepo,
                renaRepo,
                infoRepo,
                (cpf, valor, oficio) -> new com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudHttpResponse("PROTO-S", "ok"),
                (placa, renavam, tipo) -> new com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoResponse("PROTO-R", "ok"),
                alvo -> new com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaResponse("PROTO-I", "ok"),
                new IntegracaoJudicialFinanceiraProperties(true, true, 3, 300000, 10, 5),
                mock(ReadAfterWriteConsistencyPolicy.class),
                mock(AuditLedgerService.class)
        );
        var summary = service.reprocessarFalhas();
        assertThat(summary.sisbajudConfirmadas()).isEqualTo(1);
        assertThat(summary.renajudConfirmadas()).isEqualTo(1);
        assertThat(summary.infojudConfirmadas()).isEqualTo(1);
    }
}
