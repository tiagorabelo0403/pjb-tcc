package com.tcc.pjb.backend.controller.publico;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.publico.TimelinePublicaDto;
import com.tcc.pjb.backend.service.publico.ProcessoGemeoDigitalPublicoService;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/public/processos")
@Validated
@PreAuthorize("permitAll()")
public class PublicProcessoGemeoDigitalController {

    private final ProcessoGemeoDigitalPublicoService service;

    public PublicProcessoGemeoDigitalController(ProcessoGemeoDigitalPublicoService service) {
        this.service = service;
    }

    @GetMapping("/{numero}/gemeo-digital/timeline")
    public ResponseEntity<TimelinePublicaDto> timeline(@PathVariable @NotBlank String numero) {
        return ResponseEntity.ok(service.consultar(numero));
    }
}
