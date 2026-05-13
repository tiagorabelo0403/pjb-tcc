package com.tcc.pjb.backend.platform.jusos.v2.colegiado;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.julgamento.AcordaoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.VotoColegiadoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.platform.jusos.v2.colegiado.NationalColegiadoTemaSupport;
import com.tcc.pjb.backend.platform.jusos.v2.colegiado.NationalColegiadoSessionSupport;
import com.tcc.pjb.backend.service.julgamento.JulgamentoColegiadoService;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class NationalColegiadoEngineTest {

    @Test
    void shouldTrimRepetitiveThemeRegistryAndKeepProcessIndexBounded() throws Exception {
        NationalColegiadoEngine engine = new NationalColegiadoEngine(
                mock(JulgamentoColegiadoService.class),
                mock(JulgamentoColegiadoRepository.class),
                mock(VotoColegiadoRepository.class),
                mock(AcordaoRepository.class),
                mock(AuditLedgerService.class),
                mock(NationalRulePackEngine.class),
                mock(NationalPrazoEngine.class),
                new NationalColegiadoTemaSupport(),
                mock(NationalColegiadoSessionSupport.class)
        );

        for (int i = 0; i < 600; i++) {
            engine.afetarComoRepetitivo("TEMA_" + i, List.of("PROC_" + i, "PROC_EXTRA_" + i), GrauJurisdicao.SUPERIOR, RamoDireito.CIVIL);
        }

        Field temasField = NationalColegiadoEngine.class.getDeclaredField("temasRepetitivos");
        temasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> temas = (Map<String, ?>) temasField.get(engine);

        Field indiceField = NationalColegiadoEngine.class.getDeclaredField("indiceTemaPorProcesso");
        indiceField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> indice = (Map<String, ?>) indiceField.get(engine);

        assertEquals(512, temas.size());
        assertEquals(1024, indice.size());
    }

    @Test
    void shouldReplaceOldThemeIndexWhenThemeProcessesChange() throws Exception {
        NationalColegiadoEngine engine = new NationalColegiadoEngine(
                mock(JulgamentoColegiadoService.class),
                mock(JulgamentoColegiadoRepository.class),
                mock(VotoColegiadoRepository.class),
                mock(AcordaoRepository.class),
                mock(AuditLedgerService.class),
                mock(NationalRulePackEngine.class),
                mock(NationalPrazoEngine.class),
                new NationalColegiadoTemaSupport(),
                mock(NationalColegiadoSessionSupport.class)
        );

        engine.afetarComoRepetitivo("TEMA_X", List.of("PROC_A", "PROC_B"), GrauJurisdicao.SUPERIOR, RamoDireito.CIVIL);
        engine.registrarTeseRepetitiva("TEMA_X", "tese", List.of("PROC_C"));

        assertEquals(0, engine.consultarTemasPorProcesso("PROC_A").size());
        assertEquals(1, engine.consultarTemasPorProcesso("PROC_C").size());
    }
}
