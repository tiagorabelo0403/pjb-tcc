package com.tcc.pjb.backend.service.consulta;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConsultaPublicaProcessoServiceTest {

    private final ConsultaPublicaProcessoService service = new ConsultaPublicaProcessoService();

    @Test
    void consultar_retornaSomentePublicos() {
        var candidatos = List.of(
                processo("0001234-56.2024.8.26.0001", "TJSP", false),
                processo("0001234-57.2024.8.26.0001", "TJSP", true));
        var filtro = new ConsultaPublicaProcessoService.ConsultaFiltro(
                null, null, null, null, null, null, null);
        var result = service.consultar(filtro, candidatos, 0, 10);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.processos()).noneMatch(ConsultaPublicaProcessoService.ProcessoResumoPublico::sigiloso);
    }

    @Test
    void consultar_filtraPorNumero() {
        var candidatos = List.of(
                processo("0001234-56.2024.8.26.0001", "TJSP", false),
                processo("0009999-99.2024.8.26.0001", "TJSP", false));
        var filtro = new ConsultaPublicaProcessoService.ConsultaFiltro(
                "0001234", null, null, null, null, null, null);
        var result = service.consultar(filtro, candidatos, 0, 10);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    void consultar_filtraPorTribunal() {
        var candidatos = List.of(
                processo("0001", "TJSP", false),
                processo("0002", "TJRJ", false));
        var filtro = new ConsultaPublicaProcessoService.ConsultaFiltro(
                null, null, null, null, "TJSP", null, null);
        var result = service.consultar(filtro, candidatos, 0, 10);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.processos().getFirst().tribunal()).isEqualTo("TJSP");
    }

    @Test
    void consultar_paginacaoCorreta() {
        List<ConsultaPublicaProcessoService.ProcessoResumoPublico> candidatos = List.of(
                processo("0001", "TJSP", false),
                processo("0002", "TJSP", false),
                processo("0003", "TJSP", false));
        var filtro = new ConsultaPublicaProcessoService.ConsultaFiltro(
                null, null, null, null, null, null, null);
        var result = service.consultar(filtro, candidatos, 0, 2);
        assertThat(result.processos()).hasSize(2);
        assertThat(result.total()).isEqualTo(3);
    }

    @Test
    void mascararSeNecessario_processoPublico_naoMascara() {
        var p = processo("0001", "TJSP", false);
        assertThat(service.mascararSeNecessario(p).numero()).isEqualTo("0001");
    }

    @Test
    void mascararSeNecessario_processoSigiloso_mascara() {
        var p = processo("0001", "TJSP", true);
        var mascarado = service.mascararSeNecessario(p);
        assertThat(mascarado.numero()).isEqualTo("SIGILOSO");
        assertThat(mascarado.classe()).isEqualTo("SIGILOSO");
    }

    private ConsultaPublicaProcessoService.ProcessoResumoPublico processo(
            String numero, String tribunal, boolean sigiloso) {
        return new ConsultaPublicaProcessoService.ProcessoResumoPublico(
                UUID.randomUUID(), numero, "Ação Civil", "Responsabilidade Civil",
                "1ª Vara Cível", tribunal, "ATIVO", LocalDate.now().minusYears(1), sigiloso);
    }
}
