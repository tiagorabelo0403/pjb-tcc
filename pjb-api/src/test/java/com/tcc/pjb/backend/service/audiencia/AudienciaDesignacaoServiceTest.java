package com.tcc.pjb.backend.service.audiencia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AudienciaDesignacaoServiceTest {

    private final AudienciaDesignacaoService service = new AudienciaDesignacaoService();

    @Test
    void analisar_designavel_quandoSemRestricoes() {
        var input = new AudienciaDesignacaoService.DesignacaoInput(
                UUID.randomUUID(), AudienciaTipo.CONCILIACAO,
                LocalDateTime.now().plusDays(10),
                false, false, true, false, true,
                List.of("participante-001"));
        var result = service.analisar(input);
        assertThat(result.designavel()).isTrue();
        assertThat(result.deveSerVirtual()).isTrue();
    }

    @Test
    void analisar_naoDesignavel_quandoMenorEPresencaFisicaNecessaria() {
        var input = new AudienciaDesignacaoService.DesignacaoInput(
                UUID.randomUUID(), AudienciaTipo.CUSTODIAL,
                LocalDateTime.now().plusDays(10),
                false, true, true, false, false,
                List.of("reu-001"));
        var result = service.analisar(input);
        assertThat(result.designavel()).isFalse();
        assertThat(result.restricoes()).anyMatch(r -> r.contains("presença física"));
    }

    @Test
    void analisar_virtualDesativada_quandoTipoNaoAdmite() {
        var input = new AudienciaDesignacaoService.DesignacaoInput(
                UUID.randomUUID(), AudienciaTipo.CUSTODIAL,
                LocalDateTime.now().plusDays(10),
                false, false, true, false, false,
                List.of("reu-001"));
        var result = service.analisar(input);
        assertThat(result.deveSerVirtual()).isFalse();
        assertThat(result.restricoes()).anyMatch(r -> r.contains("não admite modalidade virtual"));
    }

    @Test
    void analisar_restricao_semParticipantes() {
        var input = new AudienciaDesignacaoService.DesignacaoInput(
                UUID.randomUUID(), AudienciaTipo.CONCILIACAO,
                LocalDateTime.now().plusDays(5),
                false, false, false, false, false,
                List.of());
        var result = service.analisar(input);
        assertThat(result.restricoes()).anyMatch(r -> r.contains("participante"));
    }

    @Test
    void analisar_alertaPartesSemAdvogado() {
        var input = new AudienciaDesignacaoService.DesignacaoInput(
                UUID.randomUUID(), AudienciaTipo.INSTRUCAO_E_JULGAMENTO,
                LocalDateTime.now().plusDays(15),
                true, false, false, false, false,
                List.of("parte-001"));
        var result = service.analisar(input);
        assertThat(result.restricoes()).anyMatch(r -> r.contains("curador especial"));
    }
}
