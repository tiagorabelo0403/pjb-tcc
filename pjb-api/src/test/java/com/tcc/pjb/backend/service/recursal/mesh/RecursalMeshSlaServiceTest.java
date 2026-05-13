package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;

@ExtendWith(MockitoExtension.class)
class RecursalMeshSlaServiceTest {

    @Mock
    private CalendarioForenseTribunalService calendarioService;

    @Test
    void deveProjetarSlaComCalendarioForense() {
        when(calendarioService.calcularPrazo(LocalDate.of(2026, 4, 9), 5, "TJCE", "CE", "Fortaleza"))
                .thenReturn(new CalendarioForenseTribunalService.PrazoCalculado(
                        LocalDate.of(2026, 4, 9),
                        LocalDate.of(2026, 4, 10),
                        5,
                        LocalDate.of(2026, 4, 16),
                        "TJCE",
                        "CE",
                        "Fortaleza",
                        java.util.List.of(),
                        "Prazo calculado",
                        "Fundamento"
                ));
        RecursalMeshSlaService service = new RecursalMeshSlaService(calendarioService);

        RecursalSlaSnapshot snapshot = service.snapshot(
                RecursalLifecycleState.ADMISSIBILIDADE_ORIGEM,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                Instant.parse("2026-04-09T12:00:00Z"),
                "CE",
                "Fortaleza"
        ).orElseThrow();

        assertThat(snapshot.diasUteis()).isEqualTo(5);
        assertThat(snapshot.dataPrevistaSaida()).isEqualTo(LocalDate.of(2026, 4, 16));
        assertThat(snapshot.severidade()).startsWith("MONITORAR");
    }

    @Test
    void deveMarcarSlaVencidoQuandoDataDeReferenciaEstaMuitoAtras() {
        when(calendarioService.calcularPrazo(LocalDate.of(2026, 1, 5), 3, "STJ"))
                .thenReturn(new CalendarioForenseTribunalService.PrazoCalculado(
                        LocalDate.of(2026, 1, 5),
                        LocalDate.of(2026, 1, 6),
                        3,
                        LocalDate.of(2026, 1, 8),
                        "STJ",
                        null,
                        null,
                        java.util.List.of(),
                        "Prazo calculado",
                        "Fundamento"
                ));
        when(calendarioService.contarDiasUteis(eq(LocalDate.of(2026, 1, 9)), any(LocalDate.class), eq("STJ"))).thenReturn(20);
        RecursalMeshSlaService service = new RecursalMeshSlaService(calendarioService);

        RecursalSlaSnapshot snapshot = service.snapshot(
                RecursalLifecycleState.REMESSA_EM_CURSO,
                RecursalTribunal.STJ,
                RecursalTribunalDetalhado.STJ,
                Instant.parse("2026-01-05T12:00:00Z"),
                null,
                null
        ).orElseThrow();

        assertThat(snapshot.vencido()).isTrue();
        assertThat(snapshot.diasUteisExcedidos()).isEqualTo(20);
        assertThat(snapshot.severidade()).isIn("ALERTA_INTERNO", "CRITICO_INTERNO", "ALERTA_PARTES", "CRITICO_PARTES");
    }
}
