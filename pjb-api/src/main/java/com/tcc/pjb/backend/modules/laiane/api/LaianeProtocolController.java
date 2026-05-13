package com.tcc.pjb.backend.modules.laiane.api;

import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolCreateRequest;
import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolPackageDto;
import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolVerificationResponse;
import com.tcc.pjb.backend.modules.laiane.service.LaianeNationalPreflightService;
import com.tcc.pjb.backend.modules.laiane.service.LaianeProtocolService;
import com.tcc.pjb.backend.modules.laiane.service.LaianeProtocolSubmissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/laiane/protocol")
@Validated
@PreAuthorize("hasAuthority('ROLE_ADVOGADO')")
public class LaianeProtocolController {

    private final LaianeProtocolService protocolService;
    private final LaianeProtocolSubmissionService submissionService;
    private final LaianeNationalPreflightService nationalPreflightService;

    public LaianeProtocolController(LaianeProtocolService protocolService,
                                    LaianeProtocolSubmissionService submissionService,
                                    LaianeNationalPreflightService nationalPreflightService) {
        this.protocolService = protocolService;
        this.submissionService = submissionService;
        this.nationalPreflightService = nationalPreflightService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LaianeProtocolPackageDto create(@Valid @RequestBody(required = false) LaianeProtocolCreateRequest req) {
        return protocolService.create(req);
    }

    @PostMapping("/preflight")
    public LaianeNationalPreflightService.PreflightResult preflight(@RequestBody(required = false) Map<String, Object> payload) {
        return nationalPreflightService.analyze(payload);
    }

    @GetMapping
    public List<LaianeProtocolPackageDto> listMine() {
        return protocolService.listMine();
    }

    @GetMapping("/{id}/verify")
    public LaianeProtocolVerificationResponse verify(@PathVariable @Positive Long id) {
        return new LaianeProtocolVerificationResponse(protocolService.verify(id));
    }

    @PostMapping("/{id}/submit")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public LaianeProtocolPackageDto submit(@PathVariable @Positive Long id) {
        return submissionService.submit(id);
    }
}
