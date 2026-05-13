package com.tcc.pjb.backend.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.judge.JudgeDocketItemDto;
import com.tcc.pjb.backend.service.judge.JudgeDocketService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/judge")
@RequiredArgsConstructor
public class JudgeDocketController {

    private static final String MAGISTRACY_ROLES = "hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO')";

    private final JudgeDocketService judgeDocketService;

    @GetMapping("/docket")
    @PreAuthorize(MAGISTRACY_ROLES)
    public ResponseEntity<List<JudgeDocketItemDto>> docket(
            @RequestParam(name = "limit", required = false, defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(judgeDocketService.docket(limit));
    }
}
