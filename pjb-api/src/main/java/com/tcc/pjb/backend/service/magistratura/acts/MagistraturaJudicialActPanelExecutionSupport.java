package com.tcc.pjb.backend.service.magistratura.acts;

import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.desembargador.DesembargadorColegialdoPainelService;
import com.tcc.pjb.backend.service.ministro.MinistroPlenarioService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MagistraturaJudicialActPanelExecutionSupport {

    private final DesembargadorColegialdoPainelService desembargadorColegialdoPainelService;
    private final MinistroPlenarioService ministroPlenarioService;
    private final MagistraturaJudicialActRelatoriaFormalizationSupport relatoriaFormalizationSupport;

    public MagistraturaJudicialActPanelExecutionSupport(DesembargadorColegialdoPainelService desembargadorColegialdoPainelService,
                                                        MinistroPlenarioService ministroPlenarioService,
                                                        MagistraturaJudicialActRelatoriaFormalizationSupport relatoriaFormalizationSupport) {
        this.desembargadorColegialdoPainelService = Objects.requireNonNull(desembargadorColegialdoPainelService);
        this.ministroPlenarioService = Objects.requireNonNull(ministroPlenarioService);
        this.relatoriaFormalizationSupport = Objects.requireNonNull(relatoriaFormalizationSupport);
    }

    public Map<String, Object> executarDecisaoMonocratica(Processo processo,
                                                          Usuario usuario,
                                                          Long processoId,
                                                          MagistraturaJudicialActCommandRequest request,
                                                          String relatorio,
                                                          String fundamentacao,
                                                          String dispositivo) {
        if (usuario.getTipoUsuario() == TipoUsuario.MINISTRO) {
            return ministroPlenarioService.proferirDecisaoMonocratica(processoId, relatorio, fundamentacao, dispositivo);
        }
        return relatoriaFormalizationSupport.registrarDecisaoMonocraticaRelatoria(processo, usuario, relatorio, fundamentacao, dispositivo);
    }

    public Map<String, Object> proferirVoto(Long processoId, String voto, String fundamentacao, String decisao) {
        return desembargadorColegialdoPainelService.proferirVoto(processoId, voto, fundamentacao, decisao);
    }

    public Map<String, Object> lavrarAcordao(Long processoId, String ementa, String dispositivo, String fundamentacao) {
        return desembargadorColegialdoPainelService.lavrarAcordao(processoId, ementa, dispositivo, fundamentacao);
    }

    public Map<String, Object> pedirVista(Long processoId, Integer diasVista) {
        return desembargadorColegialdoPainelService.pedirVista(processoId, diasVista == null ? 5 : Math.max(1, diasVista));
    }

    public Map<String, Object> registrarDestaque(Long processoId, String observacao) {
        return desembargadorColegialdoPainelService.registrarDestaque(processoId, observacao);
    }

    public Map<String, Object> incluirPauta(Long processoId, Instant dataHora, String orgao) {
        Instant sessao = dataHora == null ? Instant.now().plus(7, ChronoUnit.DAYS) : dataHora;
        return ministroPlenarioService.incluirPautaPlenario(processoId, sessao, firstNonBlank(orgao, "PLENARIO"));
    }

    public Map<String, Object> registrarDecisaoPlenaria(Long processoId, String votacao, String ementa, String dispositivo) {
        return ministroPlenarioService.registrarDecisaoPlenaria(processoId, votacao, ementa, dispositivo);
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
