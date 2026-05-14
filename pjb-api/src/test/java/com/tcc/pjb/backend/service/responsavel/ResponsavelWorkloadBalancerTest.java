package com.tcc.pjb.backend.service.responsavel;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResponsavelWorkloadBalancerTest {

    private final ResponsavelWorkloadBalancer balancer = new ResponsavelWorkloadBalancer();

    @Test
    void sugerir_servidorMenorCarga_quandoDisponivel() {
        var servidores = List.of(
                new ResponsavelWorkloadBalancer.ServidorCarga("srv-001", "Ana", 10, List.of()),
                new ResponsavelWorkloadBalancer.ServidorCarga("srv-002", "Bia", 3, List.of()));
        var sugestao = balancer.sugerir(servidores, "EXPEDIR");
        assertThat(sugestao.servidorId()).isEqualTo("srv-002");
        assertThat(sugestao.cargaAtual()).isEqualTo(3);
    }

    @Test
    void sugerir_semSugestao_quandoListaVazia() {
        var sugestao = balancer.sugerir(List.of(), "EXPEDIR");
        assertThat(sugestao.servidorId()).isNull();
        assertThat(sugestao.justificativa()).contains("Nenhum servidor");
    }

    @Test
    void sugerir_somenteEspecialidade_quandoOutroTemEspecialidadeDiferente() {
        var servidores = List.of(
                new ResponsavelWorkloadBalancer.ServidorCarga("srv-001", "Ana", 2, List.of("CIVEL")),
                new ResponsavelWorkloadBalancer.ServidorCarga("srv-002", "Bia", 5, List.of("TRABALHISTA")));
        var sugestao = balancer.sugerir(servidores, "TRABALHISTA");
        assertThat(sugestao.servidorId()).isEqualTo("srv-002");
    }

    @Test
    void sugerir_justificativaContemCarga() {
        var servidores = List.of(
                new ResponsavelWorkloadBalancer.ServidorCarga("srv-001", "Carlos", 7, List.of()));
        var sugestao = balancer.sugerir(servidores, "CERTIFICAR");
        assertThat(sugestao.justificativa()).contains("7");
    }
}
