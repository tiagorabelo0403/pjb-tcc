package com.tcc.pjb.backend.core.processo.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.integracao.application.ProcessoIntegracaoApplicationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessService;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoIntegracaoApplicationServiceTest {

    @Mock
    private ProcessoRepository processoRepository;
    @Mock
    private TribunalProtocolRoutingService tribunalProtocolRoutingService;
    @Mock
    private JudicialConnectorReadinessService judicialConnectorReadinessService;

    @Test
    void deveMontarPainelDeIntegracaoComProntidaoReady() {
        Processo processo = Processo.builder()
                .id(701L)
                .numeroProcesso("0000701-88.2026.8.06.0001")
                .numeroUnificado("0000701-88.2026.8.06.0001")
                .tribunalCodigoRoteado("TJCE")
                .unidadeJudiciariaCodigo("1VFAZ")
                .ramoDireito(RamoDireito.ADMINISTRATIVO)
                .rito(RitoProcessual.EXECUCAO_FISCAL)
                .faseAtual(FaseProcessual.EXECUCAO)
                .statusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA)
                .connectorSystem("PJE")
                .connectorProtocolReference("PROTOCOLO-123")
                .connectorSubmissionStatus("SUBMITTED")
                .connectorSyncStatus("SYNCED")
                .connectorSnapshotSyncedAt(LocalDateTime.now())
                .materialProbatorioHash("abc123")
                .valorCausa(BigDecimal.TEN)
                .build();
        when(processoRepository.findById(701L)).thenReturn(Optional.of(processo));
        when(tribunalProtocolRoutingService.resolve(ArgumentMatchers.anyMap(), ArgumentMatchers.eq(processo.getRito()), ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.eq(false)))
                .thenReturn(new TribunalProtocolRoutingService.RoutingDecision(
                        "TJCE",
                        "Tribunal de Justiça do Ceará",
                        JudicialSystem.PJE,
                        new JudicialSubmissionCapability(JudicialSystem.PJE, true, true, true, true, true, false, false, true, List.of("PDF"), List.of("FAZENDA_PUBLICA"), List.of("PROTOCOLO"), "https://pje.example"),
                        "FAZENDA_PUBLICA",
                        false,
                        false,
                        List.of(),
                        Map.of(),
                        Instant.now()
                ));
        when(judicialConnectorReadinessService.analyze(ArgumentMatchers.eq(JudicialSystem.PJE), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new JudicialConnectorReadinessReport(Instant.now(), JudicialSystem.PJE, true, true, true, true, true, true, true, true, List.of(), List.of("SYNC_OK"), Map.of("authMode", "BEARER")));

        ProcessoIntegracaoApplicationService service = new ProcessoIntegracaoApplicationService(
                processoRepository,
                tribunalProtocolRoutingService,
                judicialConnectorReadinessService
        );

        var aggregate = service.detalhar(701L);

        assertThat(aggregate.prontidaoEnvio()).isEqualTo("READY");
        assertThat(aggregate.canais()).hasSize(2);
        assertThat(aggregate.eventos()).anyMatch(item -> item.codigo().equals("SUBMISSAO_EXTERNA"));
    }
}
