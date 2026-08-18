package com.tcc.pjb.backend.modules.prazos.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.modules.prazos.api.PrazoDiaForenseCommand;
import com.tcc.pjb.backend.modules.prazos.api.PrazoDiaForenseResult;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualCalculoCommand;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualCalculoResult;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualPort;
import com.tcc.pjb.backend.modules.prazos.domain.PrazoProcessualDomainException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazoProcessualApplicationServiceTest {

    @Test
    void normalizaEntradaAntesDeChamarPortaLegada() {
        RecordingPort port = new RecordingPort();
        PrazoProcessualApplicationService service = new PrazoProcessualApplicationService(port);

        var result = service.calcularPrazo(new PrazoProcessualCalculoCommand(
                LocalDate.of(2026, 3, 17),
                "apelacao",
                "civil",
                "primeiro-grau",
                "tjce",
                "ce",
                " Quixada ",
                null
        ));

        assertEquals("APELACAO", port.lastCalculo.tipoPrazo());
        assertEquals("CIVIL", port.lastCalculo.ramo());
        assertEquals("PRIMEIRO_GRAU", port.lastCalculo.grau());
        assertEquals("TJCE", port.lastCalculo.tribunalCodigo());
        assertEquals("CE", port.lastCalculo.uf());
        assertFalse(result.conferenciaManualRecomendada());
    }

    @Test
    void marcaConferenciaManualQuandoLegadoRetornaAdvertencia() {
        RecordingPort port = new RecordingPort();
        port.advertencias = List.of("Marco inicial em dia nao util forense");
        PrazoProcessualApplicationService service = new PrazoProcessualApplicationService(port);

        var result = service.calcularPrazo(new PrazoProcessualCalculoCommand(
                LocalDate.of(2026, 3, 17),
                "APELACAO",
                "CIVIL",
                "PRIMEIRO_GRAU",
                "TJCE",
                "CE",
                "Quixada",
                null
        ));

        assertTrue(result.conferenciaManualRecomendada());
    }

    @Test
    void rejeitaComandoInvalidoAntesDeChamarPorta() {
        RecordingPort port = new RecordingPort();
        PrazoProcessualApplicationService service = new PrazoProcessualApplicationService(port);

        assertThrows(PrazoProcessualDomainException.class, () -> service.calcularPrazo(new PrazoProcessualCalculoCommand(
                LocalDate.of(2026, 3, 17),
                "",
                "CIVIL",
                "PRIMEIRO_GRAU",
                "TJCE",
                "CE",
                "Quixada",
                null
        )));
        assertFalse(port.called);
    }

    @Test
    void analiseDeDiaForenseUsaContratoSemDtoHttp() {
        RecordingPort port = new RecordingPort();
        PrazoProcessualApplicationService service = new PrazoProcessualApplicationService(port);

        var result = service.analisarDiaForense(new PrazoDiaForenseCommand(
                LocalDate.of(2026, 3, 18),
                "tjce",
                "ce",
                "Quixada",
                "civil",
                "primeiro-grau"
        ));

        assertEquals("TJCE", port.lastDia.tribunalCodigo());
        assertEquals("CIVIL", port.lastDia.ramo());
        assertFalse(result.conferenciaManualRecomendada());
    }

    private static final class RecordingPort implements PrazoProcessualPort {
        private PrazoProcessualCalculoCommand lastCalculo;
        private PrazoDiaForenseCommand lastDia;
        private boolean called;
        private List<String> advertencias = List.of();

        @Override
        public PrazoProcessualCalculoResult calcularPrazo(PrazoProcessualCalculoCommand command) {
            called = true;
            lastCalculo = command;
            return new PrazoProcessualCalculoResult(
                    command.dataInicio(),
                    LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 4, 1),
                    15,
                    11,
                    11,
                    command.tipoPrazo(),
                    command.ramo(),
                    command.grau(),
                    command.tribunalCodigo(),
                    command.uf(),
                    command.comarca(),
                    true,
                    "Dia util forense",
                    advertencias,
                    "CPC",
                    "Calendario forense",
                    false
            );
        }

        @Override
        public PrazoDiaForenseResult analisarDiaForense(PrazoDiaForenseCommand command) {
            called = true;
            lastDia = command;
            return new PrazoDiaForenseResult(command.data(), true, "Dia util forense", "DIA_UTIL", false);
        }
    }
}
