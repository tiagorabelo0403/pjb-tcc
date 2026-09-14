package com.tcc.pjb.backend.core.observability.unavailability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PjbDeadlineImpactAssessmentServiceTest {

    private static final ZoneId FORTALEZA = ZoneId.of("America/Fortaleza");
    private static final LocalDate SEXTA = LocalDate.of(2026, 9, 11);

    private final PjbDeadlineImpactAssessmentService service = new PjbDeadlineImpactAssessmentService();

    private PjbSystemUnavailabilityEvent indisponibilidade(Instant inicio, Duration duracao,
                                                           boolean planejada, PjbUnavailableService... servicos) {
        return new PjbSystemUnavailabilityEvent("TJCE", inicio, inicio.plus(duracao), Set.of(servicos), planejada);
    }

    private Instant horaDeFortaleza(int hora) {
        return SEXTA.atTime(hora, 0).atZone(FORTALEZA).toInstant();
    }

    @Test
    void indisponibilidadeLongaNaoPlanejadaEmServicoExternoProrrogaOPrazo() {
        var evento = indisponibilidade(horaDeFortaleza(10), Duration.ofMinutes(90), false,
                PjbUnavailableService.ELECTRONIC_FILING);

        var resultado = service.assess(evento, SEXTA, FORTALEZA);

        assertThat(resultado.extendsDeadline()).isTrue();
        assertThat(resultado.reasons()).anyMatch(r -> r.contains("policy threshold"));
    }

    @Test
    void indisponibilidadeAbaixoDoLimiarNaoProrroga() {
        var evento = indisponibilidade(horaDeFortaleza(10), Duration.ofMinutes(30), false,
                PjbUnavailableService.ELECTRONIC_FILING);

        assertThat(service.assess(evento, SEXTA, FORTALEZA).extendsDeadline()).isFalse();
    }

    @Test
    void indisponibilidadePlanejadaNaoProrrogaSozinha() {
        var evento = indisponibilidade(horaDeFortaleza(10), Duration.ofHours(4), true,
                PjbUnavailableService.ELECTRONIC_FILING);

        var resultado = service.assess(evento, SEXTA, FORTALEZA);

        assertThat(resultado.extendsDeadline())
                .as("parada planejada exige decisao operacional explicita, nao prorrogacao automatica")
                .isFalse();
        assertThat(resultado.reasons()).anyMatch(r -> r.contains("planned outage"));
    }

    @Test
    void indisponibilidadeNaHoraFinalDePeticionamentoProrrogaMesmoSendoCurta() {
        var evento = indisponibilidade(horaDeFortaleza(23), Duration.ofMinutes(10), false,
                PjbUnavailableService.ELECTRONIC_FILING);

        var resultado = service.assess(evento, SEXTA, FORTALEZA);

        assertThat(resultado.extendsDeadline())
                .as("a ultima hora do dia e o momento em que a indisponibilidade mais prejudica quem peticiona")
                .isTrue();
        assertThat(resultado.reasons()).anyMatch(r -> r.contains("final filing hour"));
    }

    @Test
    void servicoInternoAfetadoNaoProrrogaPrazoDeParteExterna() {
        var evento = indisponibilidade(horaDeFortaleza(10), Duration.ofHours(3), false,
                PjbUnavailableService.JUDICIAL_CONNECTOR);

        var resultado = service.assess(evento, SEXTA, FORTALEZA);

        assertThat(resultado.extendsDeadline()).isFalse();
        assertThat(resultado.reasons()).anyMatch(r -> r.contains("no external critical service"));
    }

    @Test
    void prazoProrrogadoCaiNoProximoDiaUtilPulandoFimDeSemana() {
        var evento = indisponibilidade(horaDeFortaleza(10), Duration.ofHours(2), false,
                PjbUnavailableService.ELECTRONIC_FILING);

        var resultado = service.assess(evento, SEXTA, FORTALEZA);

        assertThat(resultado.nextBusinessDayCandidate())
                .as("sexta prorroga para segunda, nao para sabado")
                .isEqualTo(LocalDate.of(2026, 9, 14));
    }

    @Test
    void eventoAusenteNaoProrrogaESinalizaOMotivo() {
        var resultado = service.assess(null, SEXTA, FORTALEZA);

        assertThat(resultado.extendsDeadline()).isFalse();
        assertThat(resultado.reasons()).anyMatch(r -> r.contains("missing unavailability event"));
    }
}
