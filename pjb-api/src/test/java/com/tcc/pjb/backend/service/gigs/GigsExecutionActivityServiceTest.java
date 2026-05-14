package com.tcc.pjb.backend.service.gigs;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GigsExecutionActivityServiceTest {

    private final GigsExecutionActivityService service = new GigsExecutionActivityService();

    @Test
    void executar_sucesso_quandoAtoCartorario() {
        var input = new GigsExecutionActivityService.ExecucaoInput(
                UUID.randomUUID(), "servidor-001", GigsActivityType.EXPEDIR, "expedido ok");
        var resultado = service.executar(input);
        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.concluidaEm()).isNotNull();
    }

    @Test
    void executar_falha_quandoAtoJurisdicional() {
        var input = new GigsExecutionActivityService.ExecucaoInput(
                UUID.randomUUID(), "servidor-001", GigsActivityType.MINUTAR, "minuta redigida");
        var resultado = service.executar(input);
        assertThat(resultado.sucesso()).isFalse();
        assertThat(resultado.mensagem()).contains("magistrado");
    }

    @Test
    void executar_falha_quandoResultadoVazio() {
        var input = new GigsExecutionActivityService.ExecucaoInput(
                UUID.randomUUID(), "servidor-001", GigsActivityType.CERTIFICAR, "");
        var resultado = service.executar(input);
        assertThat(resultado.sucesso()).isFalse();
        assertThat(resultado.mensagem()).contains("Resultado não informado");
    }
}
