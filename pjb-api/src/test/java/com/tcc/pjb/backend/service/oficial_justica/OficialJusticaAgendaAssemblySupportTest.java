package com.tcc.pjb.backend.service.oficial_justica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaAgendaOperacionalResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaDiligenciaQueueResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OficialJusticaAgendaAssemblySupportTest {

    private final OficialJusticaCommunicationFormalModelService communicationFormalModelService = mock(OficialJusticaCommunicationFormalModelService.class);
    private final OficialJusticaAgendaAssemblySupport support = new OficialJusticaAgendaAssemblySupport(communicationFormalModelService);

    @Test
    void deveMontarStopRowComAlertaFederalEFrustracaoEstruturada() {
        when(communicationFormalModelService.buildProfile(any(), any(), any())).thenReturn(Map.of(
                "justicaAxis", "FEDERAL",
                "manualActions", List.of("CERTIFICAR"),
                "automaticActions", List.of("INTIMAR")
        ));
        Usuario usuario = new Usuario();
        usuario.setEnteFederativo(EnteFederativo.UNIAO);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        OficialJusticaDiligenciaQueueResponse.Row row = new OficialJusticaDiligenciaQueueResponse.Row(
                91L,
                22L,
                "0001234-56.2026.4.05.8100",
                "TRF5",
                "Vara Federal Cível",
                "Subseção Fortaleza",
                "Comum",
                "Federal",
                "Cumprimento",
                "EM_ANDAMENTO",
                "AMARELO",
                "AGUARDANDO_RETORNO",
                "Mandado",
                "CRITICA",
                null,
                null,
                null,
                Instant.parse("2026-04-17T18:00:00Z"),
                Instant.parse("2026-04-17T12:00:00Z"),
                1,
                Instant.parse("2026-04-17T16:00:00Z"),
                false,
                "BLOQUEIO_SIGILO",
                "Rua das Flores, 100",
                "Fortaleza",
                "Resumo processual",
                "Fundamento",
                "CALCULO",
                Map.of(),
                Map.of(),
                List.of("RETORNO_RECOMENDADO"),
                Map.of()
        );
        DiligenceRouteOptimizationResponse.OptimizedStop stop = new DiligenceRouteOptimizationResponse.OptimizedStop(
                2,
                "91",
                "Mandado",
                "Rua das Flores, 100",
                0d,
                0d,
                1,
                12.4d,
                35L,
                Instant.parse("2026-04-17T15:30:00Z"),
                "LOTE_A"
        );
        OficialJusticaAgendaOperacionalResponse.StopRow out = support.toAgendaRow(
                usuario,
                row,
                stop,
                new OficialJusticaAgendaTerritorialHint("Rua das Flores, 100", "Centro", "Fortaleza / CE", "Centro:FORTALEZA", "cadastro", 0.91),
                new OficialJusticaAgendaLiveEventDigest(3, Instant.parse("2026-04-17T14:00:00Z"), "REINCIDENCIA_TENTATIVA_FRUSTRADA", "Frustração recorrente", "RETORNO_COM_ESCALONAMENTO_DA_PRIORIDADE", true)
        );

        assertThat(out.esfera()).isEqualTo("JUSTICA_FEDERAL");
        assertThat(out.statusOperacional()).isEqualTo("AGUARDANDO_RETORNO");
        assertThat(out.replanejamentoRecomendado()).isTrue();
        assertThat(out.motivoFrustracaoEstruturado()).isEqualTo("REINCIDENCIA_TENTATIVA_FRUSTRADA");
        assertThat(out.quickActions()).containsKeys("formalModel", "manualActions", "automaticActions", "officialLane");
        assertThat(out.alertas())
                .contains("Agenda com paridade funcional para oficial federal habilitada na mesma espinha operacional.")
                .contains("AGRUPAMENTO_BAIRRO_ATIVO");
    }
}
