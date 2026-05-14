package com.tcc.pjb.backend.controller.processual;

import com.tcc.pjb.backend.core.api.PjbApiResponseEnvelope;
import com.tcc.pjb.backend.service.processual.celerity.CeleridadeBottleneckDiagnosisService;
import com.tcc.pjb.backend.service.processual.celerity.CeleridadeNextBestActionService;
import com.tcc.pjb.backend.service.processual.celerity.CeleridadeNivel;
import com.tcc.pjb.backend.service.processual.celerity.ProcessoDestravavelRadarService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/celeridade")
public class CeleridadeController {

    private final CeleridadeBottleneckDiagnosisService bottleneckService;
    private final CeleridadeNextBestActionService nextBestActionService;
    private final ProcessoDestravavelRadarService destravavelRadar;

    public CeleridadeController(CeleridadeBottleneckDiagnosisService bottleneckService,
            CeleridadeNextBestActionService nextBestActionService,
            ProcessoDestravavelRadarService destravavelRadar) {
        this.bottleneckService = bottleneckService;
        this.nextBestActionService = nextBestActionService;
        this.destravavelRadar = destravavelRadar;
    }

    @PostMapping("/diagnosticar-gargalo")
    @PreAuthorize("hasAnyRole('SERVIDOR_JUDICIARIO','SUPERVISOR','MAGISTRADO')")
    public ResponseEntity<PjbApiResponseEnvelope<List<CeleridadeBottleneckDiagnosisService.DiagnosticoGargalo>>>
            diagnosticarGargalo(
                    @RequestBody CeleridadeBottleneckDiagnosisService.DiagnosticoInput input) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(
                bottleneckService.diagnosticar(input)));
    }

    @PostMapping("/proximas-acoes")
    @PreAuthorize("hasAnyRole('SERVIDOR_JUDICIARIO','SUPERVISOR')")
    public ResponseEntity<PjbApiResponseEnvelope<List<CeleridadeNextBestActionService.ProximaAcao>>>
            proximasAcoes(
                    @RequestBody List<CeleridadeBottleneckDiagnosisService.DiagnosticoGargalo> gargalos,
                    @RequestParam(defaultValue = "NORMAL") CeleridadeNivel nivel) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(
                nextBestActionService.sugerirProximasAcoes(gargalos, nivel)));
    }

    @PostMapping("/radar-destravavel")
    @PreAuthorize("hasAnyRole('SERVIDOR_JUDICIARIO','SUPERVISOR','DIRETOR_SECRETARIA')")
    public ResponseEntity<PjbApiResponseEnvelope<List<ProcessoDestravavelRadarService.ProcessoDestravavel>>>
            radarDestravavel(
                    @RequestBody List<ProcessoDestravavelRadarService.RadarInput> processos) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(
                destravavelRadar.filtrarDestraveis(processos)));
    }
}
