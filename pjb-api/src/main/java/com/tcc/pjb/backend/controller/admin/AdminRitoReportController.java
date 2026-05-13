package com.tcc.pjb.backend.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.admin.RitoDraftBulkProposeResponse;
import com.tcc.pjb.backend.model.dto.admin.RitoReportResponse;
import com.tcc.pjb.backend.model.dto.admin.RitoRuleDraftResponse;
import com.tcc.pjb.backend.model.dto.admin.RitoRuleProposalCreateRequest;
import com.tcc.pjb.backend.service.rito.RitoReportService;
import com.tcc.pjb.backend.service.rito.RitoRuleProposalService;







@RestController
@RequestMapping("/api/admin/ritos/relatorios")
public class AdminRitoReportController {

    private final RitoReportService reportService;
    private final RitoRuleProposalService proposalService;
    private final ObjectMapper objectMapper;

    public AdminRitoReportController(RitoReportService reportService,
                                     RitoRuleProposalService proposalService,
                                     ObjectMapper objectMapper) {
        this.reportService = reportService;
        this.proposalService = proposalService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/resumo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RitoReportResponse> resumo(
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "threshold", defaultValue = "0.70") double threshold,
            @RequestParam(name = "top", defaultValue = "25") int top
    ) {
        return ResponseEntity.ok(reportService.buildSummary(days, threshold, top));
    }

    
    @GetMapping(value = "/resumo.csv", produces = "text/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resumoCsv(
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "threshold", defaultValue = "0.70") double threshold,
            @RequestParam(name = "top", defaultValue = "25") int top
    ) {
        String csv = reportService.buildSummaryCsv(days, threshold, top);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ritos_resumo.csv")
                .body(csv);
    }

    
    @GetMapping(value = "/sugestoes/regras-rascunho", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RitoRuleDraftResponse> rascunhoRegras(
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "threshold", defaultValue = "0.70") double threshold,
            @RequestParam(name = "top", defaultValue = "25") int top
    ) {
        return ResponseEntity.ok(reportService.buildRuleDraft(days, threshold, top));
    }

    



    @PostMapping(value = "/sugestoes/regras-rascunho/propor", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RitoDraftBulkProposeResponse> proporRegrasDoRascunho(
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "threshold", defaultValue = "0.70") double threshold,
            @RequestParam(name = "top", defaultValue = "25") int top,
            @RequestParam(name = "minOcc", defaultValue = "3") int minOcc
    ) {
        int min = Math.max(1, minOcc);
        var draft = reportService.buildRuleDraft(days, threshold, top);
        var ids = new java.util.ArrayList<java.util.UUID>();

        for (var item : draft.items()) {
            if (item == null) continue;
            if (item.occurrences() < min) continue;
            String reasonsJson = safeJson(item.sampleReasons());
            var created = proposalService.create(new RitoRuleProposalCreateRequest(
                    item.ritoResolved(),
                    item.ritoChosen(),
                    (int) Math.min(Integer.MAX_VALUE, item.occurrences()),
                    reasonsJson,
                    "Proposta gerada a partir do rascunho (days=" + days + ", threshold=" + threshold + ")"
            ));
            ids.add(created.id());
        }

        return ResponseEntity.ok(new RitoDraftBulkProposeResponse(
                draft.items().size(),
                ids.size(),
                ids
        ));
    }

    private String safeJson(java.util.List<String> reasons) {
        try {
            return objectMapper.writeValueAsString(reasons == null ? java.util.List.of() : reasons);
        } catch (Exception e) {
            return "[]";
        }
    }
}
