package com.tcc.pjb.backend.modules.prazos.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazoProcessualBoundaryPolicyTest {

    private final PrazoProcessualBoundaryPolicy policy = new PrazoProcessualBoundaryPolicy();

    @Test
    void normalizaEntradaDeCalculoSemCarregarTiposLegados() {
        var parametros = policy.validarCalculo(
                LocalDate.of(2026, 3, 17),
                "apelacao",
                "civil",
                "primeiro-grau",
                "tjce",
                "ce",
                " Quixada ",
                15
        );

        assertEquals("APELACAO", parametros.tipoPrazo());
        assertEquals("CIVIL", parametros.ramo());
        assertEquals("PRIMEIRO_GRAU", parametros.grau());
        assertEquals("TJCE", parametros.tribunalCodigo());
        assertEquals("CE", parametros.uf());
        assertEquals("Quixada", parametros.comarca());
        assertTrue(policy.exigeConferenciaManual(parametros, true, List.of()));
    }

    @Test
    void rejeitaUfInvalida() {
        assertThrows(PrazoProcessualDomainException.class, () -> policy.validarCalculo(
                LocalDate.of(2026, 3, 17),
                "APELACAO",
                "CIVIL",
                "PRIMEIRO_GRAU",
                "TJCE",
                "CEA",
                "Quixada",
                null
        ));
    }

    @Test
    void rejeitaOverrideForaDoLimiteOperacional() {
        assertThrows(PrazoProcessualDomainException.class, () -> policy.validarCalculo(
                LocalDate.of(2026, 3, 17),
                "APELACAO",
                "CIVIL",
                "PRIMEIRO_GRAU",
                "TJCE",
                "CE",
                "Quixada",
                3651
        ));
    }

    @Test
    void marcaConferenciaManualQuandoMarcoInicialNaoEhUtil() {
        var parametros = policy.validarCalculo(
                LocalDate.of(2026, 3, 17),
                "APELACAO",
                "CIVIL",
                "PRIMEIRO_GRAU",
                "TJCE",
                "CE",
                "Quixada",
                null
        );

        assertTrue(policy.exigeConferenciaManual(parametros, false, List.of()));
    }
}
