package com.tcc.pjb.backend.service.magistratura.acts;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.core.security.persona.UserPersonaService;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActPreviewResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActWorkspaceResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.juiz.guardrails.JuizProcessoGuardRailService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MagistraturaJudicialActWorkbenchService {

    private final CurrentUserService currentUserService;
    private final UserPersonaService personaService;
    private final PjbAuthorizationService authorizationService;
    private final ProcessoRepository processoRepository;
    private final JuizProcessoGuardRailService guardRailService;
    private final MagistraturaJudicialProvidenceAutomationService providenceAutomationService;
    private final MagistraturaJudicialActProjectionSupport projectionSupport;
    private final MagistraturaJudicialActExecutionSupport executionSupport;

    public MagistraturaJudicialActWorkbenchService(CurrentUserService currentUserService,
                                                   UserPersonaService personaService,
                                                   PjbAuthorizationService authorizationService,
                                                   ProcessoRepository processoRepository,
                                                   JuizProcessoGuardRailService guardRailService,
                                                   MagistraturaJudicialProvidenceAutomationService providenceAutomationService,
                                                   MagistraturaJudicialActProjectionSupport projectionSupport,
                                                   MagistraturaJudicialActExecutionSupport executionSupport) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.personaService = Objects.requireNonNull(personaService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.guardRailService = Objects.requireNonNull(guardRailService);
        this.providenceAutomationService = Objects.requireNonNull(providenceAutomationService);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
        this.executionSupport = Objects.requireNonNull(executionSupport);
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "magistratura.acts.workspace.read", maxMillis = 1800, critical = false)
    public MagistraturaJudicialActWorkspaceResponse workspace(Long processoId) {
        Usuario usuario = requireMagistrate();
        UserPersona persona = personaService.getRequiredPersona();
        Processo processo = loadProcessoOptional(processoId);
        if (processo != null) {
            authorizationService.requireReadProcesso(processo);
        }
        String lane = projectionSupport.resolveLane(usuario, persona);
        return new MagistraturaJudicialActWorkspaceResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getTipoUsuario(),
                persona.grau(),
                persona.esfera(),
                lane,
                processo != null ? processo.getId() : null,
                processo != null ? projectionSupport.processNumber(processo) : null,
                projectionSupport.anchors(usuario, persona, processo),
                projectionSupport.catalog(usuario, persona, processo),
                projectionSupport.processSignals(processo, usuario, persona)
        );
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "magistratura.acts.preview-envelope.read", maxMillis = 1500, critical = false)
    public MagistraturaJudicialActPreviewResponse preview(Long processoId, String action) {
        return preview(processoId, action == null ? null : new MagistraturaJudicialActCommandRequest(
                action,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "magistratura.acts.preview-command.read", maxMillis = 1600, critical = false)
    public MagistraturaJudicialActPreviewResponse preview(Long processoId, MagistraturaJudicialActCommandRequest request) {
        Usuario usuario = requireMagistrate();
        UserPersona persona = personaService.getRequiredPersona();
        Processo processo = requireProcesso(processoId);
        authorizationService.requireReadProcesso(processo);
        MagistraturaJudicialActCode code = request == null ? null : request.resolvedAction();
        if (code == null) {
            throw new IllegalArgumentException("action é obrigatória");
        }
        projectionSupport.ensureSupported(usuario, persona, code);
        JuizProcessoGuardRailService.GuardRailSnapshot guard = guardRailService.avaliar(processo, usuario, persona, code.name());
        boolean allowed = projectionSupport.supports(usuario, persona, code) && (!projectionSupport.requiresGuard(code) || guard.allowed());
        List<String> reasons = new ArrayList<>();
        reasons.addAll(projectionSupport.baseReasons(usuario, persona, processo, code));
        reasons.addAll(projectionSupport.guardReasons(guard));
        List<String> warnings = new ArrayList<>();
        if (!allowed) {
            warnings.add("Ato bloqueado pela malha jurisdicional/material do magistrado.");
        }
        Map<String, Object> metrics = new LinkedHashMap<>(projectionSupport.safeMetrics(guard));
        metrics.put("lane", projectionSupport.resolveLane(usuario, persona));
        metrics.put("nativeRoute", projectionSupport.nativeRoute(code, processoId));
        metrics.put("template", projectionSupport.templateFor(code));
        var providences = providenceAutomationService.preview(processo, usuario, code, request);
        return new MagistraturaJudicialActPreviewResponse(
                processo.getId(),
                projectionSupport.processNumber(processo),
                code,
                allowed,
                allowed ? "ALLOW" : "BLOCK",
                projectionSupport.resolveLane(usuario, persona),
                projectionSupport.suggestedTitle(usuario, processo, code),
                projectionSupport.nativeRoute(code, processoId),
                projectionSupport.templateFor(code),
                List.copyOf(reasons),
                List.copyOf(warnings),
                projectionSupport.fieldsFor(code, usuario),
                providences,
                Map.copyOf(metrics)
        );
    }

    @Transactional
    @PjbTransactionalBudget(operation = "magistratura.acts.execute.persist", maxMillis = 2600, critical = true)
    public MagistraturaJudicialActCommandResponse execute(Long processoId, MagistraturaJudicialActCommandRequest request) {
        Usuario usuario = requireMagistrate();
        UserPersona persona = personaService.getRequiredPersona();
        Processo processo = requireProcesso(processoId);
        authorizationService.requireReadProcesso(processo);
        MagistraturaJudicialActCode code = request == null ? null : request.resolvedAction();
        if (code == null) {
            throw new IllegalArgumentException("action é obrigatória");
        }
        projectionSupport.ensureSupported(usuario, persona, code);
        JuizProcessoGuardRailService.GuardRailSnapshot guard = guardRailService.avaliar(processo, usuario, persona, code.name());
        if (projectionSupport.requiresGuard(code) && !guard.allowed()) {
            throw new AccessDeniedPjbException("Ato fora da malha jurisdicional/material do magistrado.");
        }
        Map<String, Object> payload = executionSupport.execute(processo, usuario, processoId, code, request);
        var providences = providenceAutomationService.dispatch(processo, usuario, code, request, payload);
        LinkedHashMap<String, Object> responsePayload = new LinkedHashMap<>(projectionSupport.safeMap(payload));
        responsePayload.put("providenciasSecretaria", providences);
        return new MagistraturaJudicialActCommandResponse(
                code,
                projectionSupport.resolveLane(usuario, persona),
                String.valueOf(payload.getOrDefault("status", "EXECUTADO")),
                processoId,
                projectionSupport.baseReasons(usuario, persona, processo, code),
                providences,
                projectionSupport.safeMap(responsePayload)
        );
    }

    private Usuario requireMagistrate() {
        Usuario usuario = currentUserService.getRequired();
        if (usuario.getTipoUsuario() == null || !usuario.getTipoUsuario().isMagistratura()) {
            throw new AccessDeniedPjbException("Acesso restrito à magistratura.");
        }
        return usuario;
    }

    private Processo loadProcessoOptional(Long processoId) {
        if (processoId == null) {
            return null;
        }
        return requireProcesso(processoId);
    }

    private Processo requireProcesso(Long processoId) {
        if (processoId == null) {
            throw new IllegalArgumentException("processoId é obrigatório");
        }
        return processoRepository.findMagistraturaActsScopedById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }
}
