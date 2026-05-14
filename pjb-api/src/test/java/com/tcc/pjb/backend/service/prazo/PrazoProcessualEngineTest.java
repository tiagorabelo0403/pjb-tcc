package com.tcc.pjb.backend.service.prazo;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrazoProcessualEngineTest {

    private final CalendarioUteisService calendarioService = new CalendarioUteisService();
    private final PrazoProcessualEngine engine = new PrazoProcessualEngine(calendarioService);

    private CalendarioUteisService.CalendarioInput calSemFeriados() {
        return new CalendarioUteisService.CalendarioInput(
                "comarca-test", "SP", List.of(), List.of(), false, null, null);
    }

    @Test
    void deveCalcularVencimentoParaPrazoSimples() {
        LocalDate inicio = LocalDate.of(2026, 5, 12);
        PrazoProcessualEngine.PrazoInput input = new PrazoProcessualEngine.PrazoInput(
                UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM,
                PrazoTipo.LEGAL, "Contestação", inicio, 15,
                false, false, false, calSemFeriados());
        PrazoProcessualEngine.PrazoSnapshot snap = engine.calcular(input);
        assertThat(snap.diasUteisTotal()).isEqualTo(15);
        assertThat(snap.dataVencimento()).isAfter(inicio);
        assertThat(snap.tipo()).isEqualTo(PrazoTipo.LEGAL);
    }

    @Test
    void deveDobrarPrazoParaFazendaPublica() {
        LocalDate inicio = LocalDate.of(2026, 5, 12);
        PrazoProcessualEngine.PrazoInput base = new PrazoProcessualEngine.PrazoInput(
                UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM,
                PrazoTipo.LEGAL, "Contestação Fazenda", inicio, 15,
                false, true, false, calSemFeriados());
        PrazoProcessualEngine.PrazoSnapshot snap = engine.calcular(base);
        assertThat(snap.diasUteisTotal()).isEqualTo(60);
    }

    @Test
    void deveSinalizarPrazoVencidoComAlertaCritico() {
        LocalDate inicioPassado = LocalDate.now().minusDays(30);
        PrazoProcessualEngine.PrazoInput input = new PrazoProcessualEngine.PrazoInput(
                UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM,
                PrazoTipo.PEREMPTÓRIO, "Prazo vencido", inicioPassado, 5,
                false, false, false, calSemFeriados());
        PrazoProcessualEngine.PrazoSnapshot snap = engine.calcular(input);
        assertThat(snap.vencido()).isTrue();
        assertThat(snap.alertaNivel()).isEqualTo("CRITICO");
    }

    @Test
    void deveCertificarDecursoQuandoPrazoVencido() {
        PrazoDecursoCertidaoService certService = new PrazoDecursoCertidaoService();
        LocalDate inicio = LocalDate.now().minusDays(20);
        PrazoProcessualEngine.PrazoInput input = new PrazoProcessualEngine.PrazoInput(
                UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM,
                PrazoTipo.LEGAL, "Contestação", inicio, 5,
                false, false, false, calSemFeriados());
        PrazoProcessualEngine.PrazoSnapshot snap = engine.calcular(input);
        PrazoDecursoCertidaoService.CertidaoDecursoInput certInput =
                new PrazoDecursoCertidaoService.CertidaoDecursoInput(
                        snap.processoId(), UUID.randomUUID(),
                        snap.descricao(), snap, "servidor-01");
        PrazoDecursoCertidaoService.CertidaoDecurso certidao = certService.avaliar(certInput);
        assertThat(certidao.aptaParaCertidao()).isTrue();
        assertThat(certidao.textoCertidao()).contains("CERTIDÃO DE DECURSO");
    }

    @Test
    void naoDeveCertificarDecursoComPrazoEmCurso() {
        PrazoDecursoCertidaoService certService = new PrazoDecursoCertidaoService();
        LocalDate inicio = LocalDate.now();
        PrazoProcessualEngine.PrazoInput input = new PrazoProcessualEngine.PrazoInput(
                UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM,
                PrazoTipo.LEGAL, "Prazo futuro", inicio, 15,
                false, false, false, calSemFeriados());
        PrazoProcessualEngine.PrazoSnapshot snap = engine.calcular(input);
        PrazoDecursoCertidaoService.CertidaoDecursoInput certInput =
                new PrazoDecursoCertidaoService.CertidaoDecursoInput(
                        snap.processoId(), UUID.randomUUID(),
                        snap.descricao(), snap, "servidor-01");
        PrazoDecursoCertidaoService.CertidaoDecurso certidao = certService.avaliar(certInput);
        assertThat(certidao.aptaParaCertidao()).isFalse();
    }
}
