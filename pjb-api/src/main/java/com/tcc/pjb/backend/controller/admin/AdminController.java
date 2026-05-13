package com.tcc.pjb.backend.controller.admin;

import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> ping(Principal principal) {
        String name = principal != null ? principal.getName() : "unknown";
        return ResponseEntity.ok("admin:ok (" + name + ")");
    }
}
