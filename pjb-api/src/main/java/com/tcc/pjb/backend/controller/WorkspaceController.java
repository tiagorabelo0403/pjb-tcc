package com.tcc.pjb.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.workspace.WorkspaceMeResponse;
import com.tcc.pjb.backend.service.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspace")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping("/me")
    public ResponseEntity<WorkspaceMeResponse> me() {
        return ResponseEntity.ok(workspaceService.me());
    }
}
