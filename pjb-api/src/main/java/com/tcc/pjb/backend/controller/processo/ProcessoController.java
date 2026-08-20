package com.tcc.pjb.backend.controller.processo;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.ProcessoResponse;
import com.tcc.pjb.backend.service.ProcessoService;

@RestController
@RequestMapping({"/api/processos", "/api/v1/processos"})
@PreAuthorize("isAuthenticated()")
public class ProcessoController {

    private final ProcessoService service;

    public ProcessoController(ProcessoService service) {
        this.service = service;
    }

    @GetMapping("/{numero}")
    public ProcessoResponse consultar(
            @PathVariable String numero,
            @RequestParam(required = false) String usuario
    ) {
        return service.consultar(numero, usuario);
    }
}
