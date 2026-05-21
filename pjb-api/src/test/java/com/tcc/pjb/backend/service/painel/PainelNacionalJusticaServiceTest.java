package com.tcc.pjb.backend.service.painel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.NoFederacaoJudicialRepository;
import com.tcc.pjb.backend.model.repository.PainelAlertaPrazoRepository;
import com.tcc.pjb.backend.model.repository.PainelMateriaMetricaRepository;
import com.tcc.pjb.backend.model.repository.PainelSerieTemporalDiariaRepository;
import com.tcc.pjb.backend.model.repository.PainelTribunalMetricaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService.AlertaPrazo;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService.MetricaTribunal;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService.NivelAlerta;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PainelNacionalJusticaServiceTest {

    private final PainelTribunalMetricaRepository tribunalRepository = mock(PainelTribunalMetricaRepository.class);
    private final PainelMateriaMetricaRepository materiaRepository = mock(PainelMateriaMetricaRepository.class);
    private final PainelAlertaPrazoRepository alertaRepository = mock(PainelAlertaPrazoRepository.class);
    private final PainelSerieTemporalDiariaRepository serieRepository = mock(PainelSerieTemporalDiariaRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final NoFederacaoJudicialRepository noFederacaoRepository = mock(NoFederacaoJudicialRepository.class);
    private final FederalismoJudicialEngine federalismoJudicialEngine = mock(FederalismoJudicialEngine.class);
    private final AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private PainelNacionalJusticaService service;

    @BeforeEach
    void setUp() {
        service = new PainelNacionalJusticaService(
                tribunalRepository, materiaRepository, alertaRepository,
                serieRepository, processoRepository, noFederacaoRepository,
                federalismoJudicialEngine, auditLedgerService, outboxPublisher, objectMapper
        );
    }

    @ParameterizedTest(name = "congestionamento={0} tempo={1} prazo={2} disp={3} → {4}")
    @CsvSource({
            "0.40, 150.0, 30,  0.95, EXCELENTE",
            "0.60, 300.0, 100, 0.75, BOM",
            "0.80, 600.0, 300, 0.50, REGULAR",
            "0.90, 800.0, 500, 0.30, CRITICO"
    })
    void deveClassificarDesempenhoDoTribunal(double congestionamento, double tempo,
                                              long prazoExcedido, double disponibilidade,
                                              String classificacaoEsperada) {
        String resultado = MetricaTribunal.classificar(congestionamento, tempo, prazoExcedido, disponibilidade);

        assertThat(resultado).isEqualTo(classificacaoEsperada);
    }

    @ParameterizedTest(name = "diasEmAndamento={0} prazoRazoavel={1} → nivel={2}")
    @CsvSource({
            "100,  365, BAIXO",
            "500,  100, MEDIO",
            "900,  100, ALTO",
            "2000, 100, CRITICO"
    })
    void deveCriarAlertaPrazoComNivelCorreto(long diasEmAndamento, long prazoRazoavel, String nivelEsperado) {
        AlertaPrazo alerta = AlertaPrazo.criar(
                "0001234-56.2020.8.06.0001", "TJCE", "CIVIL",
                diasEmAndamento, prazoRazoavel, "CONHECIMENTO");

        assertThat(alerta.nivel().name()).isEqualTo(nivelEsperado);
        assertThat(alerta.diasExcedidos()).isEqualTo(Math.max(0L, diasEmAndamento - prazoRazoavel));
    }

    @Test
    void deveInvocarLedgerEPublicarEventoAoReceberProcessoAjuizado() {
        Processo processo = new Processo();
        processo.setId(1L);
        processo.setNumeroUnificado("0001234-56.2026.8.06.0001");
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setTribunal("TJCE");

        when(tribunalRepository.findByCodigoTribunal(anyString())).thenReturn(Optional.empty());
        when(materiaRepository.findByChaveMetrica(anyString())).thenReturn(Optional.empty());
        when(serieRepository.findByChaveSerie(anyString())).thenReturn(Optional.empty());

        service.onProcessoAjuizado(processo);

        verify(auditLedgerService).appendSafely(anyString(), anyString(), anyString(), any(), anyString());
    }
}
