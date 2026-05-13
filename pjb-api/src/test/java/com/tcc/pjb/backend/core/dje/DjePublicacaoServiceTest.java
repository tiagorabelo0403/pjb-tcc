package com.tcc.pjb.backend.core.dje;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import com.tcc.pjb.backend.core.prazos.calculo.PrazosEngine;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.DjePublicacao;
import com.tcc.pjb.backend.model.repository.DjePublicacaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class DjePublicacaoServiceTest {

    @Test
    void shouldCalculateDatesAndPersistDje() {
        Processo processo = Processo.builder().id(10L).uf("CE").comarca("Fortaleza").build();
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        DjePublicacaoRepository djeRepository = mock(DjePublicacaoRepository.class);
        when(djeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DjeHttpClient djeHttpClient = (tribunalCodigo, conteudo, tipoAto) -> new com.tcc.pjb.backend.core.dje.domain.DjeEnvioResult("1234");
        CalendarioForenseRepository calendario = mock(CalendarioForenseRepository.class);
        when(calendario.findApplicableBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        PrazosEngine prazosEngine = new PrazosEngine(calendario, "America/Sao_Paulo");
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        DjePublicacaoService service = new DjePublicacaoService(processoRepository, djeRepository, djeHttpClient, prazosEngine, auditLedgerService, new ReadAfterWriteConsistencyPolicy(), new com.tcc.pjb.backend.core.prazos.auditoria.PrazoAuditTrailService(calendario), new DjePartesNotificacaoService(publicacao -> com.tcc.pjb.backend.core.dje.domain.DjePartesNotificacaoResult.success(publicacao.getId(), "ok"), auditLedgerService));
        var result = service.publicar(10L, "SENTENCA", "conteudo", "TJCE");
        assertThat(result.djeId()).isNull();
        assertThat(result.dataPublicacao()).isAfter(result.dataDisponibilizacao().minusDays(1));
        assertThat(result.prazoComecaEm()).isAfterOrEqualTo(result.dataPublicacao());
    }

    @Test
    void shouldConsolidateAndNotifyPublishedActs() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        DjePublicacaoRepository djeRepository = mock(DjePublicacaoRepository.class);
        CalendarioForenseRepository calendario = mock(CalendarioForenseRepository.class);
        when(calendario.findApplicableBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        PrazosEngine prazosEngine = new PrazosEngine(calendario, "America/Sao_Paulo");
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        DjePublicacaoService service = new DjePublicacaoService(processoRepository, djeRepository, (t, c, a) -> new com.tcc.pjb.backend.core.dje.domain.DjeEnvioResult("123"), prazosEngine, auditLedgerService, new ReadAfterWriteConsistencyPolicy(), new com.tcc.pjb.backend.core.prazos.auditoria.PrazoAuditTrailService(calendario), new DjePartesNotificacaoService(publicacao -> com.tcc.pjb.backend.core.dje.domain.DjePartesNotificacaoResult.success(publicacao.getId(), "ok"), auditLedgerService));
        DjePublicacao enviado = DjePublicacao.builder().id(1L).processoId(99L).status("ENVIADO").dataPublicacao(LocalDate.now()).createdAt(Instant.now()).build();
        DjePublicacao publicado = DjePublicacao.builder().id(2L).processoId(99L).status("PUBLICADO").prazoComecaEm(LocalDate.now()).createdAt(Instant.now()).partesNotificadas(false).build();
        when(djeRepository.findByStatusAndDataPublicacaoLessThanEqualOrderByDataPublicacaoAscIdAsc(eq("ENVIADO"), any(), any(Pageable.class))).thenReturn(List.of(enviado));
        when(djeRepository.findByStatusAndPrazoComecaEmLessThanEqualAndPartesNotificadasFalseOrderByPrazoComecaEmAscIdAsc(eq("PUBLICADO"), any(), any(Pageable.class))).thenReturn(List.of(publicado));
        when(djeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.consolidarPublicadas(LocalDate.now(), 10)).isEqualTo(1);
        assertThat(service.notificarPartesPublicadas(LocalDate.now(), 10)).isEqualTo(1);
    }
}
