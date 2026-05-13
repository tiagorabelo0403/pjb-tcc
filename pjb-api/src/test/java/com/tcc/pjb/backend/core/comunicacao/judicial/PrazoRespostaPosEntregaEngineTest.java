package com.tcc.pjb.backend.core.comunicacao.judicial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.judicial.PrazoRespostaPosEntregaEngine.MotorMultiplicador;
import com.tcc.pjb.backend.core.comunicacao.judicial.PrazoRespostaPosEntregaEngine.PrazoResposta;
import com.tcc.pjb.backend.core.comunicacao.judicial.PrazoRespostaPosEntregaEngine.StatusPrazoResposta;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.notificacao.NotificacaoInteligentePJB;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PrazoRespostaPosEntregaEngineTest {

    @Test
    void shouldTrimPrazoCacheWhenOverflowHappens() throws Exception {
        PrazoRespostaPosEntregaEngine engine = new PrazoRespostaPosEntregaEngine(
                mock(ExpedicaoJudicialRepository.class),
                mock(ProcessoRepository.class),
                mock(NationalPrazoEngine.class),
                mock(AuditLedgerService.class),
                mock(NotificacaoInteligentePJB.class),
                mock(com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore.class)
        );

        Method persistirPrazo = PrazoRespostaPosEntregaEngine.class.getDeclaredMethod("persistirPrazo", PrazoResposta.class);
        persistirPrazo.setAccessible(true);

        for (int i = 0; i < 20_500; i++) {
            PrazoResposta prazo = new PrazoResposta(
                    "prazo-" + i,
                    "exp-" + i,
                    (long) i,
                    "000" + i,
                    NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO,
                    LocalDate.now(),
                    LocalDate.now().plusDays(5),
                    5,
                    5,
                    true,
                    MotorMultiplicador.NORMAL,
                    StatusPrazoResposta.PRAZO_INICIADO,
                    "fundamento",
                    "TJCE",
                    GrauJurisdicao.PRIMEIRO_GRAU,
                    RamoDireito.CIVIL,
                    "hash-" + i
            );
            persistirPrazo.invoke(engine, prazo);
        }

        Field field = PrazoRespostaPosEntregaEngine.class.getDeclaredField("prazosPorExpedicao");
        field.setAccessible(true);
        Map<?, ?> store = (Map<?, ?>) field.get(engine);

        assertEquals(20_000, store.size());
    }
}
