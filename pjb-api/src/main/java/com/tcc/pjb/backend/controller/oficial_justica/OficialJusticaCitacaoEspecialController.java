package com.tcc.pjb.backend.controller.oficial_justica;

import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoHoraCertaEngine;
import com.tcc.pjb.backend.core.comunicacao.judicial.RecusaRecebimentoService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.oficial_justica.HoraCertaExecucaoRequest;
import com.tcc.pjb.backend.model.dto.oficial_justica.HoraCertaTentativaRequest;
import com.tcc.pjb.backend.model.dto.oficial_justica.RecusaRecebimentoRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/oficial-justica/citacao-especial")
public class OficialJusticaCitacaoEspecialController {

    private static final String ROLES = "hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')";

    private final CitacaoHoraCertaEngine horaCertaEngine;
    private final RecusaRecebimentoService recusaRecebimentoService;
    private final CurrentUserService currentUserService;
    private final CapabilityRateLimiter rateLimiter;

    public OficialJusticaCitacaoEspecialController(CitacaoHoraCertaEngine horaCertaEngine,
                                                   RecusaRecebimentoService recusaRecebimentoService,
                                                   CurrentUserService currentUserService,
                                                   CapabilityRateLimiter rateLimiter) {
        this.horaCertaEngine = Objects.requireNonNull(horaCertaEngine);
        this.recusaRecebimentoService = Objects.requireNonNull(recusaRecebimentoService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @PostMapping("/hora-certa/{mandadoId}/tentativas")
    @PreAuthorize(ROLES)
    public ResponseEntity<Optional<CitacaoHoraCertaEngine.AgendamentoHoraCerta>> registrarTentativaHoraCerta(
            @PathVariable String mandadoId,
            @Valid @RequestBody HoraCertaTentativaRequest request,
            Authentication authentication) {
        enforce(authentication, "oficial_hora_certa_tentativa");
        Usuario oficial = currentUserService.getRequired();
        Optional<CitacaoHoraCertaEngine.AgendamentoHoraCerta> agendamento = horaCertaEngine.registrarTentativa(
                new CitacaoHoraCertaEngine.TentativaHoraCerta(
                        mandadoId,
                        request.processoId(),
                        oficial.getId(),
                        request.numeroTentativa(),
                        Instant.now(),
                        request.latitude(),
                        request.longitude(),
                        request.enderecoConfirmado(),
                        request.evidencias(),
                        request.observacoes(),
                        request.destinatarioAusente()));
        return ResponseEntity.status(agendamento.isPresent() ? HttpStatus.CREATED : HttpStatus.OK).body(agendamento);
    }

    @PostMapping("/hora-certa/{mandadoId}/execucao")
    @PreAuthorize(ROLES)
    public ResponseEntity<CitacaoHoraCertaEngine.ResultadoHoraCerta> executarHoraCerta(
            @PathVariable String mandadoId,
            @Valid @RequestBody HoraCertaExecucaoRequest request,
            Authentication authentication) {
        enforce(authentication, "oficial_hora_certa_execucao");
        Usuario oficial = currentUserService.getRequired();
        return ResponseEntity.status(HttpStatus.CREATED).body(horaCertaEngine.executarHoraCerta(
                mandadoId, oficial.getId(), request.latitude(), request.longitude(),
                request.destinatarioPresente(), request.observacoes()));
    }

    @GetMapping("/hora-certa/{mandadoId}/agendamento")
    @PreAuthorize(ROLES)
    public ResponseEntity<Optional<CitacaoHoraCertaEngine.AgendamentoHoraCerta>> consultarAgendamentoHoraCerta(
            @PathVariable String mandadoId, Authentication authentication) {
        enforce(authentication, "oficial_hora_certa_agendamento");
        return ResponseEntity.ok(horaCertaEngine.consultarAgendamento(mandadoId));
    }

    @GetMapping("/hora-certa/{mandadoId}/tentativas")
    @PreAuthorize(ROLES)
    public ResponseEntity<List<CitacaoHoraCertaEngine.TentativaHoraCerta>> consultarTentativasHoraCerta(
            @PathVariable String mandadoId, Authentication authentication) {
        enforce(authentication, "oficial_hora_certa_tentativas");
        return ResponseEntity.ok(horaCertaEngine.consultarTentativas(mandadoId));
    }

    @PostMapping("/recusa-recebimento/{mandadoId}")
    @PreAuthorize(ROLES)
    public ResponseEntity<RecusaRecebimentoService.RegistroRecusa> registrarRecusaRecebimento(
            @PathVariable String mandadoId,
            @Valid @RequestBody RecusaRecebimentoRequest request,
            Authentication authentication) {
        enforce(authentication, "oficial_recusa_recebimento");
        Usuario oficial = currentUserService.getRequired();
        return ResponseEntity.status(HttpStatus.CREATED).body(recusaRecebimentoService.registrar(
                new RecusaRecebimentoService.SolicitacaoRecusa(
                        mandadoId,
                        request.processoId(),
                        oficial.getId(),
                        request.destinatarioNome(),
                        request.destinatarioDocumento(),
                        request.latitude(),
                        request.longitude(),
                        request.enderecoConfirmado(),
                        request.evidencias(),
                        request.fotoHashBase64(),
                        request.biometriaHashOficial(),
                        request.observacoes())));
    }

    @GetMapping("/recusa-recebimento/{mandadoId}")
    @PreAuthorize(ROLES)
    public ResponseEntity<Optional<RecusaRecebimentoService.RegistroRecusa>> consultarRecusaRecebimento(
            @PathVariable String mandadoId, Authentication authentication) {
        enforce(authentication, "oficial_recusa_recebimento_consulta");
        return ResponseEntity.ok(recusaRecebimentoService.consultar(mandadoId));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, capability, ApiVersion.V1);
    }
}
