package com.tcc.pjb.backend.service.processual.precedente;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PrecedenteAplicavelRadarServiceTest {

    private final PrecedenteDecisionSupportAssembler assembler =
            new PrecedenteDecisionSupportAssembler();
    private final PrecedenteAplicavelRadarService service =
            new PrecedenteAplicavelRadarService(assembler);

    @Test
    void radar_retornaAlerta_comAssuntoCNJ() {
        var input = new PrecedenteAplicavelRadarService.RadarPrecedenteInput(
                UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM,
                "cobrança indevida de tarifa", "TARIFAS_BANCARIAS");
        var alertas = service.radar(input);
        assertThat(alertas).hasSize(1);
        assertThat(alertas.getFirst().mensagem()).contains("TARIFAS_BANCARIAS");
        assertThat(alertas.getFirst().tipoAlerta()).isEqualTo("PRECEDENTE_DISPONIVEL");
    }

    @Test
    void radar_alertaContemProcessoId() {
        var processoId = UUID.randomUUID();
        var input = new PrecedenteAplicavelRadarService.RadarPrecedenteInput(
                processoId, RitoProcessual.TRABALHISTA_ORDINARIO,
                "horas extras", "HORAS_EXTRAS");
        var alertas = service.radar(input);
        assertThat(alertas.getFirst().processoId()).isEqualTo(processoId);
    }
}
