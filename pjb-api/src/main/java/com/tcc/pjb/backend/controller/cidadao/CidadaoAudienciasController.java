package com.tcc.pjb.backend.controller.cidadao;

import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoAudienciasResponse;
import com.tcc.pjb.backend.service.cidadao.CidadaoAudienciasService;

@RestController
@RequestMapping("/api/v1/cidadao")
public class CidadaoAudienciasController {

    private final CidadaoAudienciasService service;

    public CidadaoAudienciasController(CidadaoAudienciasService service) {
        this.service = service;
    }

    @GetMapping("/audiencias")
    @PreAuthorize("hasRole('CIDADAO')")
    public CidadaoAudienciasResponse audiencias(
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to
    ) {
        LocalDate f = from != null ? from : LocalDate.now();
        LocalDate t = to != null ? to : LocalDate.now().plusDays(90);
        if (t.isBefore(f)) {
            LocalDate tmp = f;
            f = t;
            t = tmp;
        }
        return service.listar(f, t);
    }
}
