package com.tcc.pjb.backend.modules.laiane.api;

import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeAgendaResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeInitialDispatchRequest;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeInitialDispatchResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeOnePagerResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeQueuePanelResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeRadarJurisprudenciaResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeSanitationChecklistResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeUrgencyPanelResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeSentencaDraftRequest;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeSentencaDraftResponse;
import com.tcc.pjb.backend.modules.laiane.service.LaianeJudgeRadarJurisprudenciaService;
import com.tcc.pjb.backend.modules.laiane.service.LaianeJudgeService;
import com.tcc.pjb.backend.modules.laiane.service.LaianeSentencaService;

@RestController
@RequestMapping("/api/v1/laiane/judge")
@PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
public class LaianeJudgeController {

    private final LaianeJudgeService service;
    private final LaianeSentencaService sentencaService;
    private final LaianeJudgeRadarJurisprudenciaService radarJurisprudenciaService;

    public LaianeJudgeController(LaianeJudgeService service,
                                 LaianeSentencaService sentencaService,
                                 LaianeJudgeRadarJurisprudenciaService radarJurisprudenciaService) {
        this.service = service;
        this.sentencaService = sentencaService;
        this.radarJurisprudenciaService = radarJurisprudenciaService;
    }

    @GetMapping("/urgencies")
    public LaianeJudgeUrgencyPanelResponse urgencies() {
        return service.urgencies();
    }

    @GetMapping("/sanitation/{processoId}")
    public LaianeJudgeSanitationChecklistResponse sanitation(@PathVariable Long processoId) {
        return service.sanitationChecklist(processoId);
    }

    @PostMapping("/initial-dispatch")
    public LaianeJudgeInitialDispatchResponse initialDispatch(@Valid @RequestBody LaianeJudgeInitialDispatchRequest req) {
        return service.initialDispatch(req);
    }

    @GetMapping("/agenda")
    public LaianeJudgeAgendaResponse agenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim
    ) {
        return service.agenda(inicio, fim);
    }

    @GetMapping("/queues/panel")
    public LaianeJudgeQueuePanelResponse queuePanel(@RequestParam(defaultValue = "30") int limit) {
        return service.queuePanel(limit);
    }

    @GetMapping("/onepager/{processoId}")
    public LaianeJudgeOnePagerResponse onePager(@PathVariable Long processoId) {
        return service.onePager(processoId);
    }

    @GetMapping("/radar-jurisprudencia/{processoId}")
    public LaianeJudgeRadarJurisprudenciaResponse radarJurisprudencia(@PathVariable Long processoId) {
        return radarJurisprudenciaService.analisar(processoId);
    }

    @PostMapping("/sentenca/draft")
    public LaianeSentencaDraftResponse gerarSentencaDraft(@Valid @RequestBody LaianeSentencaDraftRequest req) {
        return sentencaService.gerarRascunho(req);
    }

    @GetMapping("/sentenca/draft/{processoId}")
    public LaianeSentencaDraftResponse gerarSentencaDraftPorProcesso(@PathVariable Long processoId,
                                                                     @RequestParam(defaultValue = "false") boolean includeAnalysis) {
        return sentencaService.gerarRascunho(LaianeSentencaDraftRequest.builder()
                .processoId(processoId)
                .incluirAnalise(includeAnalysis)
                .build());
    }

    @GetMapping("/sentenca/latest/{processoId}")
    public LaianeSentencaDraftResponse latestSentenca(@PathVariable Long processoId) {
        return sentencaService.latest(processoId);
    }

    @PostMapping("/sentenca/publish/{draftId}")
    public LaianeSentencaDraftResponse publicarSentenca(@PathVariable Long draftId) {
        return sentencaService.publicar(draftId);
    }
}
