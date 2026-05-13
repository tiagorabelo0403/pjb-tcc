package com.tcc.pjb.backend.platform.jusos.v2.colegiado;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NationalColegiadoTemaSupportTest {

    private NationalColegiadoTemaSupport support;

    @BeforeEach
    void setUp() {
        support = new NationalColegiadoTemaSupport();
    }

    @Test
    void deveAfetarTemaEIndexarProcessosRepresentativos() {
        NationalColegiadoEngine.ResultadoAfetacao resultado = support.afetarComoRepetitivo(
                "Tema 501",
                List.of("0001111-22.2026.8.06.0001", "0002222-33.2026.8.06.0001", "0001111-22.2026.8.06.0001"),
                GrauJurisdicao.SUPERIOR,
                RamoDireito.CIVEL
        );

        assertThat(resultado.status()).isEqualTo(NationalColegiadoEngine.StatusRepetitivo.AFETADO_AGUARDANDO_JULGAMENTO);
        assertThat(support.consultarTema("Tema 501")).isNotNull();
        assertThat(support.consultarTemasPorProcesso("0001111-22.2026.8.06.0001"))
                .extracting(NationalColegiadoEngine.RecursoRepetitivoTema::numeroTema)
                .containsExactly("Tema 501");
        assertThat(support.totalProcessosIndexados()).isEqualTo(2);
    }

    @Test
    void registrarTeseDeveAtualizarTemaSemPerderIndiceAnterior() {
        support.afetarComoRepetitivo(
                "Tema 777",
                List.of("0003333-44.2026.8.06.0001", "0004444-55.2026.8.06.0001"),
                GrauJurisdicao.CONSTITUCIONAL,
                RamoDireito.CONSTITUCIONAL
        );

        NationalColegiadoEngine.RecursoRepetitivoTema tema = support.registrarTeseRepetitiva("Tema 777", "Tese firmada", null);

        assertThat(tema.status()).isEqualTo(NationalColegiadoEngine.StatusRepetitivo.JULGADO_TESE_FIRMADA);
        assertThat(tema.teseFixada()).isEqualTo("Tese firmada");
        assertThat(support.consultarTemasPorProcesso("0003333-44.2026.8.06.0001"))
                .extracting(NationalColegiadoEngine.RecursoRepetitivoTema::teseFixada)
                .containsExactly("Tese firmada");
    }
}
