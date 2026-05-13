package com.tcc.pjb.backend.service.processual.postarchive;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.transito.PostArchiveAccessRequest;
import com.tcc.pjb.backend.model.dto.transito.PostArchiveAccessResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.postarchive.visibility.ArchivedProcessVisibilityPolicyEngine;
import com.tcc.pjb.backend.service.processual.postarchive.visibility.ArchivedProcessVisibilityPolicyReport;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostArchiveAccessRequestService {

    private final ProcessoRepository processoRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;

    public PostArchiveAccessRequestService(ProcessoRepository processoRepository,
                                           CurrentUserService currentUserService,
                                           PjbAuthorizationService authorizationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    @Transactional(readOnly = true)
    public PostArchiveAccessResponse solicitar(PostArchiveAccessRequest request) {
        Objects.requireNonNull(request);
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        Usuario usuario = currentUserService.getRequired();
        ArchivedProcessVisibilityPolicyReport policy = ArchivedProcessVisibilityPolicyEngine.analyze(
                processo,
                new com.tcc.pjb.backend.model.dto.transito.PostArchiveLifecycleRequest(request.processoId(), false, request.motivo(), false, false, 30),
                Map.of()
        );
        AccessQualification qualification = qualify(usuario, processo);
        boolean authorized = qualification.authorized() || authorizationService.canReadProcesso(processo).allowed();
        boolean reviewRequired = policy.controlledAccessRequired() || request.solicitarCopiaIntegral() || request.solicitarReativacaoControlada();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (qualification.authorized()) {
            fundamentos.add(qualification.basis());
        }
        if (!policy.alerts().isEmpty()) {
            alertas.addAll(policy.alerts());
        }
        if (!authorized) {
            alertas.add("Solicitação negada: o solicitante não é parte direta nem possui credencial institucional suficiente.");
        } else if (reviewRequired) {
            alertas.add("Solicitação aceita em trilha controlada, sujeita a verificação institucional e auditoria.");
        } else {
            alertas.add("Solicitação aceita para reexposição controlada do processo arquivado.");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestedAt", Instant.now().toString());
        metadata.put("requesterId", usuario.getId());
        metadata.put("requesterType", usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : null);
        metadata.put("requesterCpf", usuario.getCpf());
        metadata.put("scope", resolveScope(request));
        metadata.put("reviewRequired", reviewRequired);
        metadata.put("concealmentMode", policy.mode() != null ? policy.mode().name() : null);
        metadata.put("controlledAccessRequired", policy.controlledAccessRequired());
        metadata.put("partyAuthorizationPreferred", policy.partyAuthorizationPreferred());
        metadata.put("solicitarCopiaIntegral", request.solicitarCopiaIntegral());
        metadata.put("solicitarReativacaoControlada", request.solicitarReativacaoControlada());
        metadata.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return new PostArchiveAccessResponse(
                authorized ? UUID.nameUUIDFromBytes((processo.getId() + ":" + usuario.getId() + ":" + request.motivo()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString() : null,
                processo.getId(),
                firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso()),
                authorized,
                qualification.profile(),
                policy.mode() != null ? policy.mode().name() : null,
                reviewRequired,
                resolveScope(request),
                List.copyOf(fundamentos),
                List.copyOf(alertas),
                Collections.unmodifiableMap(metadata)
        );
    }

    private AccessQualification qualify(Usuario usuario, Processo processo) {
        if (usuario == null) {
            return new AccessQualification(false, "ANONIMO", null);
        }
        if (usuario.isMagistrado()) {
            return new AccessQualification(true, "MAGISTRADO", "Ato institucional de reexposição controlada em ambiente judicial.");
        }
        if (usuario.isServidorJudiciario()) {
            return new AccessQualification(true, "SERVIDOR_JUDICIARIO", "Atuação institucional em secretaria ou gabinete com trilha auditável.");
        }
        String cpf = usuario.getCpf();
        if (cpf != null && !cpf.isBlank()) {
            if (cpf.equals(processo.getParteAutoraCpf())) {
                return new AccessQualification(true, "CIDADAO_PARTE_AUTORA", "Parte autora autenticada no feito arquivado.");
            }
            if (cpf.equals(processo.getParteReuCpf())) {
                return new AccessQualification(true, "CIDADAO_PARTE_RE", "Parte ré autenticada no feito arquivado.");
            }
        }
        if (usuario.getTipoUsuario() == TipoUsuario.ADVOGADO || usuario.getTipoUsuario() == TipoUsuario.PROCURADOR) {
            boolean readAllowed = authorizationService.canReadProcesso(processo).allowed();
            if (readAllowed) {
                return new AccessQualification(true, "REPRESENTANTE_HABILITADO", "Representante profissional com acesso previamente reconhecido ao processo.");
            }
        }
        return new AccessQualification(false, usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "USUARIO", null);
    }

    private String resolveScope(PostArchiveAccessRequest request) {
        if (request.solicitarCopiaIntegral() && request.solicitarReativacaoControlada()) {
            return "CONSULTA_CONTROLADA_E_REATIVACAO";
        }
        if (request.solicitarCopiaIntegral()) {
            return "CONSULTA_CONTROLADA_INTEGRAL";
        }
        if (request.solicitarReativacaoControlada()) {
            return "REATIVACAO_CONTROLADA";
        }
        return "CONSULTA_CONTROLADA";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record AccessQualification(boolean authorized, String profile, String basis) {
    }
}
