package com.tcc.pjb.backend.controller.publico;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tcc.pjb.backend.model.dto.publico.SessaoPublicaDetalheDto;
import com.tcc.pjb.backend.model.dto.publico.SessaoPublicaDto;
import com.tcc.pjb.backend.service.julgamento.live.JulgamentoVotosLiveHub;
import com.tcc.pjb.backend.service.publico.PublicPlenarioService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/public/plenario")
@RequiredArgsConstructor
@Validated
@PreAuthorize("permitAll()")
public class PublicPlenarioController {

    private final PublicPlenarioService publicPlenarioService;
    private final JulgamentoVotosLiveHub julgamentoVotosLiveHub;

    @GetMapping("/sessoes")
    public ResponseEntity<List<SessaoPublicaDto>> listarSessoes(@RequestParam(required = false) String colegiado,
                                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                @RequestParam(defaultValue = "20") @Min(1) int size) {
        return ResponseEntity.ok(publicPlenarioService.listarSessoes(colegiado, page, size));
    }

    @GetMapping("/sessoes/{sessaoId}")
    public ResponseEntity<SessaoPublicaDetalheDto> detalhar(@PathVariable Long sessaoId) {
        return ResponseEntity.ok(publicPlenarioService.detalhar(sessaoId));
    }

    @GetMapping(value = "/sessoes/{sessaoId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long sessaoId,
                             @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {
        publicPlenarioService.validarSessaoPublica(sessaoId);
        return julgamentoVotosLiveHub.register(sessaoId, lastEventId);
    }
}
