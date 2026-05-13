package com.tcc.pjb.backend.service.security.operational;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.SecurityChallengeService;
import com.tcc.pjb.backend.model.dto.security.OperationalStepUpChallengeResponse;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialDirectorProvisionRequest;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialPasswordSetRequest;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialSnapshotResponse;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialUnlockRequest;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialUnlockResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.OperationalFunctionCredential;
import com.tcc.pjb.backend.model.entity.security.OperationalFunctionUnlockSession;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.OperationalFunctionCredentialRepository;
import com.tcc.pjb.backend.model.repository.security.OperationalFunctionUnlockSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperationalFunctionCredentialService {

    public static final String SECRETARIAT_PROCESS_WRITE = "SECRETARIAT_PROCESS_WRITE";
    public static final String OFFICIAL_PERSONAL_SERVICE_WRITE = "OFFICIAL_PERSONAL_SERVICE_WRITE";
    public static final String INSTITUTIONAL_SUPPORT_PROCESS_WRITE = "INSTITUTIONAL_SUPPORT_PROCESS_WRITE";
    public static final String HEADER_UNLOCK_TOKEN = "X-Operational-Function-Token";

    private static final int MAX_FAILURES = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int UNLOCK_TTL_MINUTES = 5;

    private final CurrentUserService currentUserService;
    private final UsuarioRepository usuarioRepository;
    private final OperationalFunctionCredentialRepository credentialRepository;
    private final OperationalFunctionUnlockSessionRepository unlockSessionRepository;
    private final OperationalFunctionCredentialAuthorityService authorityService;
    private final SecurityChallengeService challengeService;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final ClientIpResolver ipResolver;
    private final Argon2PasswordEncoder encoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public OperationalFunctionCredentialService(CurrentUserService currentUserService,
                                                UsuarioRepository usuarioRepository,
                                                OperationalFunctionCredentialRepository credentialRepository,
                                                OperationalFunctionUnlockSessionRepository unlockSessionRepository,
                                                OperationalFunctionCredentialAuthorityService authorityService,
                                                SecurityChallengeService challengeService,
                                                ObjectMapper objectMapper,
                                                ObjectProvider<HttpServletRequest> requestProvider,
                                                ClientIpResolver ipResolver) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.credentialRepository = Objects.requireNonNull(credentialRepository);
        this.unlockSessionRepository = Objects.requireNonNull(unlockSessionRepository);
        this.authorityService = Objects.requireNonNull(authorityService);
        this.challengeService = Objects.requireNonNull(challengeService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.requestProvider = Objects.requireNonNull(requestProvider);
        this.ipResolver = Objects.requireNonNull(ipResolver);
        this.encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Transactional(readOnly = true)
    public OperationalCredentialSnapshotResponse snapshotForCurrentUser(String laneCode) {
        Usuario actor = currentUserService.getRequired();
        String normalizedLane = normalize(laneCode);
        if ("INSTITUTIONAL_SUPPORT".equals(normalizedLane) && !authorityService.isInstitutionalSupportUser(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "usuário não pertence a uma secretaria institucional provisionável");
        }
        List<String> functions = functionsForLane(normalizedLane);
        List<OperationalFunctionCredential> existing = actor.getId() == null || functions.isEmpty()
                ? List.of()
                : credentialRepository.findAllByUsuarioIdAndFunctionCodeIn(actor.getId(), functions);
        Map<String, OperationalFunctionCredential> byFunction = new LinkedHashMap<>();
        for (OperationalFunctionCredential credential : existing) {
            byFunction.put(credential.getFunctionCode(), credential);
        }
        List<OperationalCredentialSnapshotResponse.Entry> entries = new ArrayList<>();
        for (String functionCode : functions) {
            OperationalFunctionCredential credential = byFunction.get(functionCode);
            entries.add(toEntry(actor, credential, functionCode, normalizedLane));
        }
        Map<String, Object> directorGovernance = new LinkedHashMap<>();
        directorGovernance.put("directorProvisionRequired", true);
        directorGovernance.put("currentActorIsDirector", authorityService.isDirector(actor));
        directorGovernance.put("governancePath", OperationalApiRoutes.institutionalCredentialGovernance());
        directorGovernance.put("headerUnlockToken", HEADER_UNLOCK_TOKEN);
        Map<String, Object> routes = routesForLane(normalizedLane, functions);
        return new OperationalCredentialSnapshotResponse(normalizedLane, List.copyOf(entries), Map.copyOf(directorGovernance), Map.copyOf(routes));
    }

    @Transactional(readOnly = true)
    public OperationalCredentialSnapshotResponse snapshotForTarget(Long targetUserId, String laneCode) {
        Usuario target = usuarioRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "usuário alvo não encontrado"));
        String normalizedLane = normalize(laneCode);
        List<String> functions = functionsForLane(normalizedLane);
        if (functions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lane operacional não suportada");
        }
        authorityService.requireDirectorForTarget(target, functions.get(0));
        List<OperationalFunctionCredential> existing = credentialRepository.findAllByUsuarioIdAndFunctionCodeIn(target.getId(), functions);
        Map<String, OperationalFunctionCredential> byFunction = new LinkedHashMap<>();
        for (OperationalFunctionCredential credential : existing) {
            byFunction.put(credential.getFunctionCode(), credential);
        }
        List<OperationalCredentialSnapshotResponse.Entry> entries = new ArrayList<>();
        for (String functionCode : functions) {
            entries.add(toEntry(target, byFunction.get(functionCode), functionCode, normalizedLane));
        }
        Map<String, Object> directorGovernance = Map.of(
                "targetUserId", targetUserId,
                "governancePath", OperationalApiRoutes.institutionalCredentialGovernance(),
                "resetByDirectorOnly", true,
                "headerUnlockToken", HEADER_UNLOCK_TOKEN
        );
        return new OperationalCredentialSnapshotResponse(normalizedLane, List.copyOf(entries), directorGovernance, Map.of());
    }

    @Transactional
    public OperationalCredentialSnapshotResponse directorProvision(OperationalCredentialDirectorProvisionRequest request) {
        if (request == null || request.targetUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetUserId é obrigatório");
        }
        String functionCode = requireSupportedFunction(request.functionCode());
        Usuario target = usuarioRepository.findById(request.targetUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "usuário alvo não encontrado"));
        Usuario director = authorityService.requireDirectorForTarget(target, functionCode);
        OperationalFunctionCredential credential = credentialRepository.findLockedByUsuarioIdAndFunctionCode(target.getId(), functionCode)
                .orElseGet(() -> newCredential(target, functionCode));
        boolean existingActive = credential.getSecretHash() != null
                && !credential.getSecretHash().isBlank()
                && "ACTIVE".equals(credential.getStatus());
        credential.setManagedByUser(director);
        credential.setProvisionedByUser(director);
        credential.setReason(trimToNull(request.reason()));
        credential.setJusticaAxis(firstNonBlank(trimToNull(request.justicaAxis()), deriveJusticaAxis(target, functionCode)));
        credential.setTribunalCodigo(firstNonBlank(trimToNull(request.tribunalCodigo()), authorityService.resolveTribunal(target)));
        credential.setForumCode(trimToNull(request.forumCode()));
        credential.setUnitCode(trimToNull(request.unitCode()));
        credential.setVaraLabel(trimToNull(request.varaLabel()));
        credential.setUf(firstNonBlank(trimToNull(target.getUf()), credential.getUf()));
        credential.setComarca(firstNonBlank(trimToNull(target.getComarca()), credential.getComarca()));
        if (!existingActive || request.forceReset()) {
            credential.setStatus("PENDING_SETUP");
            credential.setSecretHash(null);
            credential.setFailedAttempts(0);
            credential.setLockedUntil(null);
            credential.setLastRotationByUser(director);
            credential.setLastResetAt(LocalDateTime.now());
        }
        credential.setAuditTrailJson(writeAudit(appendAudit(
                credential.getAuditTrailJson(),
                (!existingActive || request.forceReset()) ? "DIRECTOR_PROVISION" : "DIRECTOR_SCOPE_UPDATE",
                director,
                linkedDetails(
                        "forceReset", request.forceReset(),
                        "reason", trimToNull(request.reason()),
                        "functionCode", functionCode,
                        "justicaAxis", credential.getJusticaAxis(),
                        "tribunalCodigo", credential.getTribunalCodigo(),
                        "forumCode", credential.getForumCode(),
                        "unitCode", credential.getUnitCode(),
                        "varaLabel", credential.getVaraLabel()
                ))));
        credentialRepository.save(credential);
        return snapshotForTarget(target.getId(), laneForFunction(functionCode));
    }

    @Transactional
    public OperationalStepUpChallengeResponse issueCurrentUserPasswordChallenge(String functionCode) {
        String normalizedFunction = requireSupportedFunction(functionCode);
        Usuario actor = currentUserService.getRequired();
        OperationalFunctionCredential credential = requireCredential(actor.getId(), normalizedFunction);
        if (!"PENDING_SETUP".equals(credential.getStatus()) && !"ACTIVE".equals(credential.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "credencial funcional não está pronta para desafio OTP");
        }
        var challenge = challengeService.createEmailOtp(actor, resolveIp(), normalizedFunction + "|PASSWORD_SETUP");
        return new OperationalStepUpChallengeResponse(
                challenge.getId(),
                normalize(challenge.getTipo()),
                "EMAIL_INSTITUCIONAL",
                normalizedFunction + "_PASSWORD_SETUP",
                challenge.getExpiresAt(),
                "OTP institucional emitido para criar ou redefinir a senha funcional.",
                "/api/v1/security/challenges/" + challenge.getId() + "/verify-otp",
                true
        );
    }

    @Transactional
    public OperationalCredentialSnapshotResponse setCurrentUserPassword(String functionCode, OperationalCredentialPasswordSetRequest request) {
        String normalizedFunction = requireSupportedFunction(functionCode);
        Usuario actor = currentUserService.getRequired();
        OperationalFunctionCredential credential = requireCredential(actor.getId(), normalizedFunction);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload obrigatório");
        }
        validatePassword(actor, request.newPassword(), request.confirmPassword(), normalizedFunction);
        if (request.challengeId() == null || request.otpCode() == null || request.otpCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "challengeId e otpCode são obrigatórios para definir a senha funcional");
        }
        challengeService.consumeOtp(request.challengeId(), actor, request.otpCode());
        credential.setSecretHash(encoder.encode(request.newPassword()));
        credential.setStatus("ACTIVE");
        credential.setActivatedAt(LocalDateTime.now());
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setLastRotationByUser(actor);
        credential.setAuditTrailJson(writeAudit(appendAudit(credential.getAuditTrailJson(), "PASSWORD_SET", actor, linkedDetails(
                "note", trimToNull(request.note()),
                "functionCode", normalizedFunction
        ))));
        credentialRepository.save(credential);
        return snapshotForCurrentUser(laneForFunction(normalizedFunction));
    }

    @Transactional
    public OperationalCredentialUnlockResponse unlockCurrentUserFunction(String functionCode, OperationalCredentialUnlockRequest request) {
        String normalizedFunction = requireSupportedFunction(functionCode);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload obrigatório");
        }
        Usuario actor = currentUserService.getRequired();
        OperationalFunctionCredential credential = requireCredential(actor.getId(), normalizedFunction);
        assertUnlockedCredentialState(credential);
        String actionCode = normalize(request.actionCode());
        String referenceId = firstNonBlank(trimToNull(request.referenceId()), "GLOBAL");
        if (!allowedActions(normalizedFunction).contains(actionCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "actionCode não suportado pela credencial funcional");
        }
        if (request.password() == null || request.password().isBlank() || !encoder.matches(request.password(), credential.getSecretHash())) {
            credential.setFailedAttempts(credential.getFailedAttempts() + 1);
            if (credential.getFailedAttempts() >= MAX_FAILURES) {
                credential.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                credential.setStatus("LOCKED");
            }
            credential.setAuditTrailJson(writeAudit(appendAudit(credential.getAuditTrailJson(), "UNLOCK_FAILURE", actor, Map.of(
                    "actionCode", actionCode,
                    "referenceId", referenceId,
                    "failedAttempts", credential.getFailedAttempts()
            ))));
            credentialRepository.save(credential);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "credencial funcional inválida");
        }
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setStatus("ACTIVE");
        credential.setLastVerifiedAt(LocalDateTime.now());
        String rawToken = generateOpaqueToken();
        OperationalFunctionUnlockSession session = new OperationalFunctionUnlockSession();
        session.setCredential(credential);
        session.setUsuario(actor);
        session.setFunctionCode(normalizedFunction);
        session.setScopeAction(actionCode);
        session.setScopeReference(referenceId);
        session.setTokenHash(sha256Hex(rawToken));
        session.setIp(resolveIp());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(UNLOCK_TTL_MINUTES));
        unlockSessionRepository.deleteExpiredOrConsumed(LocalDateTime.now().minusMinutes(1));
        unlockSessionRepository.save(session);
        credential.setAuditTrailJson(writeAudit(appendAudit(credential.getAuditTrailJson(), "UNLOCK_ISSUED", actor, Map.of(
                "actionCode", actionCode,
                "referenceId", referenceId,
                "unlockSessionId", session.getId()
        ))));
        credentialRepository.save(credential);
        return new OperationalCredentialUnlockResponse(normalizedFunction, actionCode, referenceId, rawToken, session.getExpiresAt(), Map.of(
                "header", HEADER_UNLOCK_TOKEN,
                "ttlMinutes", UNLOCK_TTL_MINUTES,
                "oneShot", true
        ));
    }

    @Transactional
    public void consumeUnlockTokenForCurrentUser(String functionCode, String actionCode, String referenceId, String unlockToken) {
        String normalizedFunction = requireSupportedFunction(functionCode);
        String normalizedAction = normalize(actionCode);
        if (!allowedActions(normalizedFunction).contains(normalizedAction)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "actionCode não suportado pela credencial funcional");
        }
        if (unlockToken == null || unlockToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "token efêmero da credencial funcional é obrigatório");
        }
        Usuario actor = currentUserService.getRequired();
        OperationalFunctionUnlockSession session = unlockSessionRepository.findLockedByTokenHash(sha256Hex(unlockToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token efêmero inválido"));
        if (!Objects.equals(session.getUsuario().getId(), actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "token efêmero não pertence ao usuário autenticado");
        }
        if (!normalizedFunction.equals(session.getFunctionCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "token efêmero fora da função operacional");
        }
        if (!normalizedAction.equals(session.getScopeAction())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "token efêmero fora do escopo da ação");
        }
        if (!firstNonBlank(trimToNull(referenceId), "GLOBAL").equals(session.getScopeReference())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "token efêmero fora do recurso operacional");
        }
        if (session.getConsumedAt() != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token efêmero já consumido");
        }
        if (session.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token efêmero expirado");
        }
        session.setConsumedAt(LocalDateTime.now());
        unlockSessionRepository.save(session);
        OperationalFunctionCredential credential = session.getCredential();
        credential.setLastVerifiedAt(LocalDateTime.now());
        credential.setAuditTrailJson(writeAudit(appendAudit(credential.getAuditTrailJson(), "UNLOCK_CONSUMED", actor, Map.of(
                "actionCode", normalizedAction,
                "referenceId", session.getScopeReference(),
                "unlockSessionId", session.getId()
        ))));
        credentialRepository.save(credential);
    }

    private OperationalFunctionCredential requireCredential(Long userId, String functionCode) {
        OperationalFunctionCredential credential = credentialRepository.findLockedByUsuarioIdAndFunctionCode(userId, functionCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "função operacional ainda não provisionada pela instituição"));
        if (!isEligible(credential.getUsuario(), functionCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "credencial provisionada para usuário fora da função elegível");
        }
        return credential;
    }

    private void assertUnlockedCredentialState(OperationalFunctionCredential credential) {
        if (credential.getSecretHash() == null || credential.getSecretHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "credencial funcional ainda não foi criada pelo servidor");
        }
        if (credential.isLocked()) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "credencial funcional temporariamente bloqueada");
        }
        if (!"ACTIVE".equals(credential.getStatus()) && !"LOCKED".equals(credential.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "credencial funcional não está ativa");
        }
    }

    private OperationalFunctionCredential newCredential(Usuario target, String functionCode) {
        OperationalFunctionCredential credential = new OperationalFunctionCredential();
        credential.setUsuario(target);
        credential.setFunctionCode(functionCode);
        credential.setStatus("PENDING_SETUP");
        credential.setUf(trimToNull(target.getUf()));
        credential.setComarca(trimToNull(target.getComarca()));
        credential.setJusticaAxis(deriveJusticaAxis(target, functionCode));
        credential.setTribunalCodigo(authorityService.resolveTribunal(target));
        return credential;
    }

    private OperationalCredentialSnapshotResponse.Entry toEntry(Usuario actor,
                                                                OperationalFunctionCredential credential,
                                                                String functionCode,
                                                                String laneCode) {
        boolean provisioned = credential != null;
        boolean active = credential != null && "ACTIVE".equals(credential.getStatus()) && credential.getSecretHash() != null && !credential.getSecretHash().isBlank();
        boolean resetRequired = credential != null && "PENDING_SETUP".equals(credential.getStatus());
        boolean locked = credential != null && credential.isLocked();
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("headerUnlockToken", HEADER_UNLOCK_TOKEN);
        policy.put("unlockTtlMinutes", UNLOCK_TTL_MINUTES);
        policy.put("maxFailures", MAX_FAILURES);
        policy.put("allowedActions", allowedActions(functionCode));
        policy.put("passwordPolicy", Map.of(
                "minLength", 15,
                "maxLength", 128,
                "argon2id", true,
                "periodicRotationDisabled", true,
                "directorProvisionRequired", true,
                "otpRequiredForSet", true
        ));
        Map<String, Object> routes = new LinkedHashMap<>();
        if ("SECRETARIAT".equals(normalize(laneCode))) {
            routes.put("challengePath", OperationalApiRoutes.secretariatCredentialChallenge(functionCode));
            routes.put("setPasswordPath", OperationalApiRoutes.secretariatCredentialPassword(functionCode));
            routes.put("unlockPath", OperationalApiRoutes.secretariatCredentialUnlock(functionCode));
        } else if ("OFICIAL_JUSTICA".equals(normalize(laneCode))) {
            routes.put("challengePath", OperationalApiRoutes.oficialCredentialChallenge(functionCode));
            routes.put("setPasswordPath", OperationalApiRoutes.oficialCredentialPassword(functionCode));
            routes.put("unlockPath", OperationalApiRoutes.oficialCredentialUnlock(functionCode));
        } else if ("INSTITUTIONAL_SUPPORT".equals(normalize(laneCode))) {
            routes.put("challengePath", OperationalApiRoutes.institutionalSupportCredentialChallenge("MINISTERIO_PUBLICO", functionCode));
            routes.put("setPasswordPath", OperationalApiRoutes.institutionalSupportCredentialPassword("MINISTERIO_PUBLICO", functionCode));
            routes.put("unlockPath", OperationalApiRoutes.institutionalSupportCredentialUnlock("MINISTERIO_PUBLICO", functionCode));
            routes.put("branchBound", true);
        }
        return new OperationalCredentialSnapshotResponse.Entry(
                functionCode,
                functionLabel(functionCode),
                provisioned ? credential.getStatus() : "NOT_PROVISIONED_BY_INSTITUTION",
                provisioned,
                active,
                resetRequired,
                locked,
                provisioned ? credential.getJusticaAxis() : deriveJusticaAxis(actor, functionCode),
                provisioned ? credential.getTribunalCodigo() : authorityService.resolveTribunal(actor),
                provisioned ? credential.getForumCode() : null,
                provisioned ? credential.getUnitCode() : null,
                provisioned ? credential.getVaraLabel() : null,
                provisioned ? credential.getUf() : trimToNull(actor.getUf()),
                provisioned ? credential.getComarca() : trimToNull(actor.getComarca()),
                provisioned ? credential.getActivatedAt() : null,
                provisioned ? credential.getLastVerifiedAt() : null,
                provisioned ? credential.getLastResetAt() : null,
                Map.copyOf(policy),
                Map.copyOf(routes)
        );
    }

    private void validatePassword(Usuario actor, String password, String confirm, String functionCode) {
        if (password == null || confirm == null || !password.equals(confirm)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "confirmação da senha funcional inválida");
        }
        if (password.length() < 15 || password.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senha funcional deve ter entre 15 e 128 caracteres");
        }
        if (password.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senha funcional contém caracteres inválidos");
        }
        String lowered = password.toLowerCase(Locale.ROOT);
        if (actor.getEmail() != null && lowered.contains(actor.getEmail().toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senha funcional não pode conter o e-mail institucional");
        }
        if (actor.getCpf() != null) {
            String digits = actor.getCpf().replaceAll("\\D+", "");
            if (!digits.isBlank() && lowered.contains(digits)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senha funcional não pode conter o CPF institucional");
            }
        }
        if (functionLabel(functionCode).toLowerCase(Locale.ROOT).contains(lowered) || lowered.contains("senha") || lowered.contains("password")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senha funcional fraca para a função operacional");
        }
    }

    private boolean isEligible(Usuario actor, String functionCode) {
        if (actor == null || actor.getTipoUsuario() == null) {
            return false;
        }
        return switch (requireSupportedFunction(functionCode)) {
            case SECRETARIAT_PROCESS_WRITE -> actor.getTipoUsuario().isServidorJudiciario();
            case OFFICIAL_PERSONAL_SERVICE_WRITE -> actor.getTipoUsuario().name().startsWith("OFICIAL_JUSTICA");
            case INSTITUTIONAL_SUPPORT_PROCESS_WRITE -> authorityService.isInstitutionalSupportUser(actor) || actor.getTipoUsuario().isMinisterioPublico() || actor.getTipoUsuario().isDefensoriaPublica() || actor.getTipoUsuario().isProcuradoria();
            default -> false;
        };
    }

    private String deriveJusticaAxis(Usuario actor, String functionCode) {
        if (actor == null || actor.getTipoUsuario() == null) {
            return "ESTADUAL";
        }
        String tribunal = authorityService.resolveTribunal(actor);
        if (tribunal != null && tribunal.startsWith("TRF")) {
            return "FEDERAL";
        }
        String normalizedFunction = requireSupportedFunction(functionCode);
        if (OFFICIAL_PERSONAL_SERVICE_WRITE.equals(normalizedFunction) && actor.getTipoUsuario().name().contains("OFICIAL")) {
            return tribunal != null && tribunal.startsWith("TRF") ? "FEDERAL" : "ESTADUAL";
        }
        if (INSTITUTIONAL_SUPPORT_PROCESS_WRITE.equals(normalizedFunction)) {
            if (actor.getTipoUsuario().isProcuradoria() && actor.getTipoUsuario() == com.tcc.pjb.backend.model.entity.enums.TipoUsuario.PROCURADORIA_MUNICIPAL) {
                return "MUNICIPAL";
            }
            return tribunal != null && tribunal.startsWith("TRF") ? "FEDERAL" : "ESTADUAL";
        }
        return tribunal != null && tribunal.startsWith("TRF") ? "FEDERAL" : "ESTADUAL";
    }

    private List<String> functionsForLane(String laneCode) {
        return switch (normalize(laneCode)) {
            case "SECRETARIAT" -> List.of(SECRETARIAT_PROCESS_WRITE);
            case "OFICIAL_JUSTICA" -> List.of(OFFICIAL_PERSONAL_SERVICE_WRITE);
            case "INSTITUTIONAL_SUPPORT", "MINISTERIO_PUBLICO_SECRETARIAT", "DEFENSORIA_SECRETARIAT", "PROCURADORIA_SECRETARIAT" -> List.of(INSTITUTIONAL_SUPPORT_PROCESS_WRITE);
            default -> List.of();
        };
    }

    private String laneForFunction(String functionCode) {
        return switch (requireSupportedFunction(functionCode)) {
            case SECRETARIAT_PROCESS_WRITE -> "SECRETARIAT";
            case OFFICIAL_PERSONAL_SERVICE_WRITE -> "OFICIAL_JUSTICA";
            case INSTITUTIONAL_SUPPORT_PROCESS_WRITE -> "INSTITUTIONAL_SUPPORT";
            default -> "UNKNOWN";
        };
    }

    private Map<String, Object> routesForLane(String laneCode, List<String> functions) {
        if (functions == null || functions.isEmpty()) {
            return Map.of();
        }
        String functionCode = functions.get(0);
        return switch (normalize(laneCode)) {
            case "SECRETARIAT" -> Map.of(
                    "challengePath", OperationalApiRoutes.secretariatCredentialChallenge(functionCode),
                    "setPasswordPath", OperationalApiRoutes.secretariatCredentialPassword(functionCode),
                    "unlockPath", OperationalApiRoutes.secretariatCredentialUnlock(functionCode)
            );
            case "OFICIAL_JUSTICA" -> Map.of(
                    "challengePath", OperationalApiRoutes.oficialCredentialChallenge(functionCode),
                    "setPasswordPath", OperationalApiRoutes.oficialCredentialPassword(functionCode),
                    "unlockPath", OperationalApiRoutes.oficialCredentialUnlock(functionCode)
            );
            case "INSTITUTIONAL_SUPPORT", "MINISTERIO_PUBLICO_SECRETARIAT", "DEFENSORIA_SECRETARIAT", "PROCURADORIA_SECRETARIAT" -> Map.of(
                    "challengePath", OperationalApiRoutes.institutionalSupportCredentialChallenge("MINISTERIO_PUBLICO", functionCode),
                    "setPasswordPath", OperationalApiRoutes.institutionalSupportCredentialPassword("MINISTERIO_PUBLICO", functionCode),
                    "unlockPath", OperationalApiRoutes.institutionalSupportCredentialUnlock("MINISTERIO_PUBLICO", functionCode),
                    "branchBound", true
            );
            default -> Map.of();
        };
    }

    private List<String> allowedActions(String functionCode) {
        return switch (requireSupportedFunction(functionCode)) {
            case SECRETARIAT_PROCESS_WRITE -> List.of(
                    "SECRETARIAT_CONFIRM_VENUE",
                    "SECRETARIAT_PARTICIPANT_NOTIFICATION",
                    "SECRETARIAT_COMPLETION_EVENT",
                    "SECRETARIAT_PROCESS_RETURN"
            );
            case OFFICIAL_PERSONAL_SERVICE_WRITE -> List.of(
                    "OFICIAL_CIENTE_INTIMACAO",
                    "OFICIAL_CERTIDAO_AUTOMATICA",
                    "OFICIAL_FORMALIZACAO_PROCESSUAL",
                    "OFICIAL_JUNTADA_AUTOMATICA",
                    "OFICIAL_ENCERRAMENTO_SOBERANO",
                    "OFICIAL_EMITIR_OFICIO",
                    "OFICIAL_RESPONDER_OFICIO"
            );
            case INSTITUTIONAL_SUPPORT_PROCESS_WRITE -> List.of(
                    "INSTITUTIONAL_SUPPORT_SCHEDULE_AUDIENCE",
                    "INSTITUTIONAL_SUPPORT_SCHEDULE_SESSION",
                    "INSTITUTIONAL_SUPPORT_PARTICIPANT_NOTIFICATION",
                    "INSTITUTIONAL_SUPPORT_PROCESS_ATTACHMENT",
                    "INSTITUTIONAL_SUPPORT_FORMAL_COMMUNICATION"
            );
            default -> List.of();
        };
    }

    private String functionLabel(String functionCode) {
        return switch (requireSupportedFunction(functionCode)) {
            case SECRETARIAT_PROCESS_WRITE -> "Credencial funcional da secretaria para atos que inserem conteúdo processual";
            case OFFICIAL_PERSONAL_SERVICE_WRITE -> "Credencial funcional do oficial de justiça para diligência pessoal e formalização processual";
            case INSTITUTIONAL_SUPPORT_PROCESS_WRITE -> "Credencial funcional da secretaria institucional para atos de agenda, intimação e formalização do órgão";
            default -> functionCode;
        };
    }

    private String requireSupportedFunction(String rawFunctionCode) {
        String normalized = normalize(rawFunctionCode);
        if (!List.of(SECRETARIAT_PROCESS_WRITE, OFFICIAL_PERSONAL_SERVICE_WRITE, INSTITUTIONAL_SUPPORT_PROCESS_WRITE).contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "functionCode operacional não suportado");
        }
        return normalized;
    }

    private String resolveIp() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        return request == null ? null : ipResolver.resolve(request);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    private List<Map<String, Object>> appendAudit(String rawAuditJson, String eventCode, Usuario actor, Map<String, Object> details) {
        List<Map<String, Object>> history = parseAudit(rawAuditJson);
        LinkedHashMap<String, Object> event = new LinkedHashMap<>();
        event.put("eventCode", eventCode);
        event.put("actorId", actor == null ? null : actor.getId());
        event.put("actorName", actor == null ? null : trimToNull(actor.getNome()));
        event.put("at", LocalDateTime.now().toString());
        event.put("details", details == null ? Map.of() : details);
        history.add(event);
        return history;
    }

    private List<Map<String, Object>> parseAudit(String rawAuditJson) {
        if (rawAuditJson == null || rawAuditJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(rawAuditJson, new TypeReference<List<Map<String, Object>>>() {}));
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private String writeAudit(List<Map<String, Object>> audit) {
        try {
            return objectMapper.writeValueAsString(audit == null ? List.of() : audit);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Map<String, Object> linkedDetails(Object... keyValues) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (keyValues == null) {
            return out;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            if (key == null) {
                continue;
            }
            out.put(String.valueOf(key), keyValues[i + 1]);
        }
        return out;
    }
}
