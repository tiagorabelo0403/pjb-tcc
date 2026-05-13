package com.tcc.pjb.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.core.kernel.twin.ProcessDigitalTwinService;
import com.tcc.pjb.backend.model.dto.twin.ProcessTwinDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/processos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProcessTwinController {

    private final ProcessDigitalTwinService digitalTwinService;

    @GetMapping("/{processoId}/twin")
    public ResponseEntity<ProcessTwinDto> twin(@PathVariable Long processoId) {
        return ResponseEntity.ok(digitalTwinService.twin(processoId));
    }
}
