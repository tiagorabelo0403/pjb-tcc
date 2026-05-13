package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.core.security.GovBrAssuranceApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/govbr/assurance")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminGovBrAssuranceController {

    private final GovBrAssuranceApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminGovBrAssuranceController(GovBrAssuranceApplicationService applicationService,
                                         ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @GetMapping("/readiness")
    public ResponseEntity<ApiQueryResponse<?>> readiness() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.readiness(), List.of()));
    }

    @GetMapping("/identity")
    public ResponseEntity<ApiQueryResponse<?>> identity() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.identityAssurance(), List.of()));
    }

    @GetMapping("/evaluate")
    public ResponseEntity<ApiQueryResponse<?>> evaluate(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                        @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.evaluate(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/level")
    public ResponseEntity<ApiQueryResponse<?>> level(@RequestParam(value = "nivelAtual", required = false) String nivelAtual) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.level(nivelAtual), List.of()));
    }

    @GetMapping("/level/result")
    public ResponseEntity<ApiQueryResponse<?>> levelResult(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                           @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.levelResult(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/decision")
    public ResponseEntity<ApiQueryResponse<?>> decision(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                        @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.decision(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiQueryResponse<?>> health(@RequestParam(value = "nivelAtual", required = false) String nivelAtual) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(nivelAtual), List.of()));
    }

    @GetMapping("/window")
    public ResponseEntity<ApiQueryResponse<?>> window(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                      @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.window(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/policy")
    public ResponseEntity<ApiQueryResponse<?>> policy() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.policy(), List.of()));
    }

    @GetMapping("/sensitive-act")
    public ResponseEntity<ApiQueryResponse<?>> sensitiveAct(@RequestParam(value = "tipoAto", required = false) String tipoAto,
                                                            @RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                            @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.sensitiveAct(tipoAto, nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/sensitive-act/assessment")
    public ResponseEntity<ApiQueryResponse<?>> sensitiveActAssessment(@RequestParam(value = "tipoAto", required = false) String tipoAto,
                                                                      @RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                                      @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.sensitiveActAssessment(tipoAto, nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/step-up/decision")
    public ResponseEntity<ApiQueryResponse<?>> stepUpDecision(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                              @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.stepUpDecision(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                        @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiQueryResponse<?>> status(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                      @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.status(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/timeline/health")
    public ResponseEntity<ApiQueryResponse<?>> timelineHealth(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                              @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timelineHealth(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/sensitive-act/health")
    public ResponseEntity<ApiQueryResponse<?>> sensitiveActHealth(@RequestParam(value = "tipoAto", required = false) String tipoAto,
                                                                  @RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                                  @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.sensitiveActHealth(tipoAto, nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/budget")
    public ResponseEntity<ApiQueryResponse<?>> budget() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.budget(), List.of()));
    }

    @GetMapping("/policy/budget")
    public ResponseEntity<ApiQueryResponse<?>> policyBudget() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.policyBudget(), List.of()));
    }

    @GetMapping("/step-up/window")
    public ResponseEntity<ApiQueryResponse<?>> stepUpWindow(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                            @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.stepUpWindow(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/decision/window")
    public ResponseEntity<ApiQueryResponse<?>> decisionWindow(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                              @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.decisionWindow(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/consistency")
    public ResponseEntity<ApiQueryResponse<?>> consistency(@RequestParam(value = "nivelAtual", required = false) String nivelAtual) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consistency(nivelAtual), List.of()));
    }

    @GetMapping("/envelope")
    public ResponseEntity<ApiQueryResponse<?>> envelope(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                        @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.envelope(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/signal")
    public ResponseEntity<ApiQueryResponse<?>> signal(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                      @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.signal(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/owner")
    public ResponseEntity<ApiQueryResponse<?>> owner() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.owner(), List.of()));
    }

    @GetMapping("/sensitive-act/window")
    public ResponseEntity<ApiQueryResponse<?>> sensitiveActWindow(@RequestParam(value = "tipoAto", required = false) String tipoAto,
                                                                  @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.sensitiveActWindow(tipoAto, atoSensivel), List.of()));
    }

    @GetMapping("/step-up/envelope")
    public ResponseEntity<ApiQueryResponse<?>> stepUpEnvelope(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                              @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.stepUpEnvelope(nivelAtual, atoSensivel), List.of()));
    }

    @GetMapping("/step-up/health")
    public ResponseEntity<ApiQueryResponse<?>> stepUpHealth(@RequestParam(value = "nivelAtual", required = false) String nivelAtual,
                                                            @RequestParam(value = "atoSensivel", defaultValue = "false") boolean atoSensivel) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.stepUpHealth(nivelAtual, atoSensivel), List.of()));
    }
}
