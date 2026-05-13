package com.tcc.pjb.backend.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.ai.common.AiModelClientJurisdictionAdapter;
import com.tcc.pjb.backend.model.entity.JurisdictionEngine.Rite;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/jurisdicao-debug")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "pjb.debug.enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "pjb.debug.jurisdiction", name = "enabled", havingValue = "true")
public class JurisdictionDebugController {

    private final AiModelClientJurisdictionAdapter adapter;

    @GetMapping
    public ResponseEntity<String> debug(
            @RequestParam String materia,
            @RequestParam String orgao,
            @RequestParam(defaultValue = "BRASIL") String pais,
            @RequestParam(defaultValue = "") String tratado,
            @RequestParam(defaultValue = "CIVIL") Rite rito
    ) {
        return ResponseEntity.ok(adapter.generateWithJurisdiction(materia, orgao, pais, tratado, rito));
    }
}
