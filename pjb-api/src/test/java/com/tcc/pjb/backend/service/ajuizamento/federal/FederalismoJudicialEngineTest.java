package com.tcc.pjb.backend.service.ajuizamento.federal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.federalismo.NoFederacaoJudicial;
import com.tcc.pjb.backend.model.entity.federalismo.StatusNoFederacao;
import com.tcc.pjb.backend.model.repository.FederacaoEventoOutboxRepository;
import com.tcc.pjb.backend.model.repository.FederacaoLedgerEntryRepository;
import com.tcc.pjb.backend.model.repository.NoFederacaoJudicialRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.util.List;
import org.junit.jupiter.api.Test;

class FederalismoJudicialEngineTest {

    @Test
    void deveReutilizarSnapshotCurtoDosNosEntreChamadasConsecutivas() {
        NoFederacaoJudicialRepository noRepository = mock(NoFederacaoJudicialRepository.class);
        when(noRepository.findAll()).thenReturn(List.of(no("TJCE", StatusNoFederacao.ONLINE)));
        FederalismoJudicialEngine engine = new FederalismoJudicialEngine(
                noRepository,
                mock(FederacaoLedgerEntryRepository.class),
                mock(FederacaoEventoOutboxRepository.class),
                mock(OutboxPublisher.class),
                mock(AuditLedgerService.class),
                new ObjectMapper()
        );

        List<?> first = engine.listarNos();
        List<?> second = engine.listarNos();

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        verify(noRepository, times(1)).findAll();
    }

    @Test
    void deveReutilizarSnapshotCurtoDaSaudeFederativa() {
        NoFederacaoJudicialRepository noRepository = mock(NoFederacaoJudicialRepository.class);
        when(noRepository.findAll()).thenReturn(List.of(no("TJCE", StatusNoFederacao.ONLINE)));
        FederalismoJudicialEngine engine = new FederalismoJudicialEngine(
                noRepository,
                mock(FederacaoLedgerEntryRepository.class),
                mock(FederacaoEventoOutboxRepository.class),
                mock(OutboxPublisher.class),
                mock(AuditLedgerService.class),
                new ObjectMapper()
        );

        FederalismoJudicialEngine.FederacaoHealth first = engine.healthFederacao();
        FederalismoJudicialEngine.FederacaoHealth second = engine.healthFederacao();

        assertThat(first.totalNos()).isEqualTo(1);
        assertThat(second.totalNos()).isEqualTo(1);
        verify(noRepository, times(1)).findAll();
    }

    private static NoFederacaoJudicial no(String codigoTribunal, StatusNoFederacao status) {
        NoFederacaoJudicial node = new NoFederacaoJudicial(codigoTribunal, codigoTribunal, "CE", TipoJustica.ESTADUAL, "https://" + codigoTribunal.toLowerCase() + ".pjb.local");
        node.setStatusAtual(status);
        node.setAceitaRecepcaoEventos(true);
        node.setOperacaoAutonomaAtiva(true);
        node.setCapacidadeBacklog(100L);
        node.setBacklogPendente(10L);
        node.setVersaoSchemaAtual(1L);
        return node;
    }
}
