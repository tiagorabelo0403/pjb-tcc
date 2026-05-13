package com.tcc.pjb.backend.service.magistratura.acts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.desembargador.DesembargadorColegialdoPainelService;
import com.tcc.pjb.backend.service.ministro.MinistroPlenarioService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MagistraturaJudicialActPanelExecutionSupportTest {

    @Mock private DesembargadorColegialdoPainelService desembargadorColegialdoPainelService;
    @Mock private MinistroPlenarioService ministroPlenarioService;
    @Mock private MagistraturaJudicialActRelatoriaFormalizationSupport relatoriaFormalizationSupport;

    private MagistraturaJudicialActPanelExecutionSupport support;

    @BeforeEach
    void setUp() {
        support = new MagistraturaJudicialActPanelExecutionSupport(
                desembargadorColegialdoPainelService,
                ministroPlenarioService,
                relatoriaFormalizationSupport
        );
    }

    @Test
    void executarDecisaoMonocraticaDeMinistroDeveDelegarParaPlenario() {
        Processo processo = Processo.builder().id(501L).build();
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.MINISTRO);
        MagistraturaJudicialActCommandRequest request = new MagistraturaJudicialActCommandRequest(
                "DECISAO_MONOCRATICA",
                null,
                "Tema constitucional",
                "Nego seguimento.",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Relatório sintetizado.",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(ministroPlenarioService.proferirDecisaoMonocratica(501L, "Relatório sintetizado.", "Tema constitucional", "Nego seguimento."))
                .thenReturn(Map.of("status", "DECISAO_MONOCRATICA_REGISTRADA", "processoId", 501L));

        Map<String, Object> response = support.executarDecisaoMonocratica(processo, usuario, 501L, request, "Relatório sintetizado.", "Tema constitucional", "Nego seguimento.");

        verify(ministroPlenarioService).proferirDecisaoMonocratica(501L, "Relatório sintetizado.", "Tema constitucional", "Nego seguimento.");
        assertThat(response).containsEntry("status", "DECISAO_MONOCRATICA_REGISTRADA");
    }

    @Test
    void incluirPautaSemOrgaoDeveUsarPlenarioComoFallback() {
        when(ministroPlenarioService.incluirPautaPlenario(777L, Instant.parse("2026-05-04T18:00:00Z"), "PLENARIO"))
                .thenReturn(Map.of("status", "PAUTA_INCLUIDA", "processoId", 777L));

        Map<String, Object> response = support.incluirPauta(777L, Instant.parse("2026-05-04T18:00:00Z"), null);

        verify(ministroPlenarioService).incluirPautaPlenario(777L, Instant.parse("2026-05-04T18:00:00Z"), "PLENARIO");
        assertThat(response).containsEntry("status", "PAUTA_INCLUIDA");
    }
}
