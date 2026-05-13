package com.tcc.pjb.backend.modules.laiane.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeCockpitRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeCockpitResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeDraftRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeDraftResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoValidateRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoValidateResponse;
import com.tcc.pjb.backend.modules.laiane.service.LaianeCockpitService;
import com.tcc.pjb.backend.modules.laiane.service.LaianeDraftService;
import com.tcc.pjb.backend.modules.laiane.service.LaianePeticaoAssistService;
import com.tcc.pjb.backend.modules.laiane.service.LaianePeticaoValidatorService;

@RestController
@RequestMapping("/api/v1/laiane/legal")
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')")
public class LaianeLegalController {

    private final LaianeCockpitService cockpitService;
    private final LaianeDraftService draftService;
    private final LaianePeticaoValidatorService validatorService;
    private final LaianePeticaoAssistService assistService;

    public LaianeLegalController(LaianeCockpitService cockpitService,
                                LaianeDraftService draftService,
                                LaianePeticaoValidatorService validatorService,
                                LaianePeticaoAssistService assistService) {
        this.cockpitService = cockpitService;
        this.draftService = draftService;
        this.validatorService = validatorService;
        this.assistService = assistService;
    }

    @PostMapping("/cockpit")
    public LaianeCockpitResponse cockpit(@RequestBody(required = false) LaianeCockpitRequest req) {
        return cockpitService.cockpit(req);
    }

    @PostMapping("/draft")
    public LaianeDraftResponse draft(@RequestBody(required = false) LaianeDraftRequest req) {
        return draftService.draft(req);
    }

    @PostMapping("/validate")
    public LaianePeticaoValidateResponse validate(@RequestBody(required = false) LaianePeticaoValidateRequest req) {
        String content = req != null ? req.getContent() : null;
        return validatorService.validate(content, req != null ? req.getRito() : null, req != null ? req.getClasseTpu() : null, req != null ? req.getRamoDireito() : null);
    }

    @PostMapping("/preflight")
    public LaianePeticaoAssistResponse preflight(@RequestBody(required = false) LaianePeticaoAssistRequest req) {
        return assistService.preflightLite(req);
    }

    @PostMapping("/draft-preflight")
    public LaianePeticaoAssistResponse draftPreflight(@RequestBody(required = false) LaianePeticaoAssistRequest req) {
        return assistService.draftAndPreflightLite(req);
    }
}
