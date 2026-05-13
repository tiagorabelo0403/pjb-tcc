package com.tcc.pjb.backend.service.magistratura.acts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.service.juiz.CertidaoTransitoJulgadoService;
import com.tcc.pjb.backend.service.juiz.decision.JuizGabineteDecisionalService;
import com.tcc.pjb.backend.service.juiz.decision.JuizOficialCumprimentoOrderService;
import com.tcc.pjb.backend.service.pericia.PeritoNomeacaoService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MagistraturaJudicialActExecutionSupportTest {

    @Mock private JuizGabineteDecisionalService juizGabineteDecisionalService;
    @Mock private JuizOficialCumprimentoOrderService juizOficialCumprimentoOrderService;
    @Mock private CertidaoTransitoJulgadoService certidaoTransitoJulgadoService;
    @Mock private PeritoNomeacaoService peritoNomeacaoService;
    @Mock private MagistraturaJudicialActRelatoriaFormalizationSupport relatoriaFormalizationSupport;
    @Mock private MagistraturaJudicialActPanelExecutionSupport panelExecutionSupport;

    private MagistraturaJudicialActExecutionSupport executionSupport;

    @BeforeEach
    void setUp() {
        executionSupport = new MagistraturaJudicialActExecutionSupport(
                juizGabineteDecisionalService,
                juizOficialCumprimentoOrderService,
                certidaoTransitoJulgadoService,
                peritoNomeacaoService,
                relatoriaFormalizationSupport,
                panelExecutionSupport,
                new MagistraturaJudicialActProjectionSupport()
        );
    }

    @Test
    void executeDespachoDeveDelegarParaFluxoSingularExistente() {
        Processo processo = Processo.builder()
                .id(77L)
                .numeroProcesso("0001234-56.2026.8.06.0001")
                .build();
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setTipoUsuario(TipoUsuario.JUIZ_ESTADUAL);

        MagistraturaJudicialActCommandRequest request = new MagistraturaJudicialActCommandRequest(
                "DESPACHO",
                "Intime-se.",
                "CPC",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(juizGabineteDecisionalService.assinarDespacho(77L, "Intime-se.", "CPC"))
                .thenReturn(Map.of("status", "ASSINADO", "processoId", 77L));

        Map<String, Object> response = executionSupport.execute(processo, usuario, 77L, MagistraturaJudicialActCode.DESPACHO, request);

        verify(juizGabineteDecisionalService).assinarDespacho(77L, "Intime-se.", "CPC");
        assertThat(response).containsEntry("status", "ASSINADO");
    }

    @Test
    void executeVotoColegialDeveDelegarParaSupportDeColegiado() {
        Processo processo = Processo.builder().id(91L).build();
        Usuario usuario = new Usuario();
        usuario.setId(22L);
        usuario.setTipoUsuario(TipoUsuario.DESEMBARGADOR);

        MagistraturaJudicialActCommandRequest request = new MagistraturaJudicialActCommandRequest(
                "VOTO_COLEGIADO",
                null,
                "fundamentacao colegiada",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Acompanho o relator.",
                "NEGAR_PROVIMENTO",
                null,
                null,
                null,
                null,
                null
        );

        when(panelExecutionSupport.proferirVoto(91L, "Acompanho o relator.", "fundamentacao colegiada", "NEGAR_PROVIMENTO"))
                .thenReturn(Map.of("status", "VOTO_REGISTRADO", "processoId", 91L));

        Map<String, Object> response = executionSupport.execute(processo, usuario, 91L, MagistraturaJudicialActCode.VOTO_COLEGIADO, request);

        verify(panelExecutionSupport).proferirVoto(91L, "Acompanho o relator.", "fundamentacao colegiada", "NEGAR_PROVIMENTO");
        assertThat(response).containsEntry("status", "VOTO_REGISTRADO");
    }

    @Test
    void executeDespachoRelatorDeveDelegarParaFormalizacaoRelatorial() {
        Processo processo = Processo.builder()
                .id(55L)
                .numeroProcesso("0004321-10.2026.8.06.0001")
                .faseAtual(FaseProcessual.RECURSAL)
                .build();
        Usuario usuario = new Usuario();
        usuario.setId(30L);
        usuario.setTipoUsuario(TipoUsuario.DESEMBARGADOR);

        MagistraturaJudicialActCommandRequest request = new MagistraturaJudicialActCommandRequest(
                "DESPACHO_RELATOR",
                "Determine-se a vista.",
                "CPC",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(relatoriaFormalizationSupport.registrarDespachoRelatoria(processo, usuario, "Determine-se a vista.", "CPC"))
                .thenReturn(Map.of("status", "DESPACHO_RELATORIA_REGISTRADO", "processoId", 55L));

        Map<String, Object> response = executionSupport.execute(processo, usuario, 55L, MagistraturaJudicialActCode.DESPACHO_RELATOR, request);

        verify(relatoriaFormalizationSupport).registrarDespachoRelatoria(processo, usuario, "Determine-se a vista.", "CPC");
        assertThat(response).containsEntry("status", "DESPACHO_RELATORIA_REGISTRADO");
    }
}
