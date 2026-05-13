package com.tcc.pjb.backend.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.catalog.IdeaDto;
import com.tcc.pjb.backend.service.catalog.IdeaCatalogService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class IdeaCatalogController {

    private final IdeaCatalogService ideaCatalogService;

    @GetMapping("/roles")
    public ResponseEntity<List<String>> roles() {
        return ResponseEntity.ok(ideaCatalogService.listRoles());
    }

    
    @GetMapping("/{role}")
    public ResponseEntity<List<IdeaDto>> byRole(@PathVariable String role) {
        return ResponseEntity.ok(ideaCatalogService.getByRole(role));
    }
}
