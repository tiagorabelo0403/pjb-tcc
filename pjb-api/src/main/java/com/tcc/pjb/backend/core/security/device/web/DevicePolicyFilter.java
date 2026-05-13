package com.tcc.pjb.backend.core.security.device.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;
import com.tcc.pjb.backend.core.security.device.SecurityAlertService;
import com.tcc.pjb.backend.core.security.device.SecurityDualApprovalService;
import com.tcc.pjb.backend.core.security.device.StrongAuthUsageService;
import com.tcc.pjb.backend.core.security.device.TrustedDeviceService;
import com.tcc.pjb.backend.core.security.device.reqhash.RequestHashService;
import com.tcc.pjb.backend.core.security.device.policy.SecurityActionCatalog;
import com.tcc.pjb.backend.core.security.device.policy.SecurityActionDecision;
import com.tcc.pjb.backend.core.security.device.policy.SecurityActionPolicy;
import com.tcc.pjb.backend.core.security.device.policy.StrongAuthState;
import com.tcc.pjb.backend.core.security.device.policy.store.DeviceSecurityPolicyManager;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalRequest;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DevicePolicyFilter extends OncePerRequestFilter {

    private final DeviceSecurityProperties props;
    private final CurrentUserService currentUserService;
    private final TrustedDeviceRepository deviceRepo;
    private final TrustedDeviceService deviceService;
    private final SecurityAlertService alertService;
    private final ClientIpResolver ipResolver;
    private final UserSecurityProfileRepository profileRepo;
    private final ObjectMapper objectMapper;
    private final AuditLedgerService auditLedgerService;
    private final DeviceSecurityPolicyManager policyManager;
    private final SecurityDualApprovalService dualApprovalService;
    private final StrongAuthUsageService strongAuthUsageService;
    private final RequestHashService requestHashService;
    private final SecureRandom random = new SecureRandom();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SecurityActionCatalog baseCatalog;

    public DevicePolicyFilter(DeviceSecurityProperties props,
                              CurrentUserService currentUserService,
                              TrustedDeviceRepository deviceRepo,
                              TrustedDeviceService deviceService,
                              SecurityAlertService alertService,
                              ClientIpResolver ipResolver,
                              UserSecurityProfileRepository profileRepo,
                              ObjectMapper objectMapper,
                              AuditLedgerService auditLedgerService,
                              DeviceSecurityPolicyManager policyManager,
                              SecurityDualApprovalService dualApprovalService,
                              StrongAuthUsageService strongAuthUsageService,
                              RequestHashService requestHashService) {
        this.props = Objects.requireNonNull(props);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.deviceRepo = Objects.requireNonNull(deviceRepo);
        this.deviceService = Objects.requireNonNull(deviceService);
        this.alertService = Objects.requireNonNull(alertService);
        this.ipResolver = Objects.requireNonNull(ipResolver);
        this.profileRepo = Objects.requireNonNull(profileRepo);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.policyManager = Objects.requireNonNull(policyManager);
        this.dualApprovalService = Objects.requireNonNull(dualApprovalService);
        this.strongAuthUsageService = Objects.requireNonNull(strongAuthUsageService);
        this.requestHashService = Objects.requireNonNull(requestHashService);
        this.baseCatalog = new SecurityActionCatalog(this.props, this.pathMatcher);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isEnabled()) return true;

        String method = request.getMethod();
        String path = request.getRequestURI();
        if (method == null || path == null) return false;

        if (props.getExemptPaths() != null) {
            for (String pattern : props.getExemptPaths()) {
                if (pattern != null && !pattern.isBlank() && pathMatcher.match(pattern.trim(), path)) {
                    return true;
                }
            }
        }

        if (props.getSensitivePaths() != null) {
            for (String pattern : props.getSensitivePaths()) {
                if (pattern != null && !pattern.isBlank() && pathMatcher.match(pattern.trim(), path)) {
                    return false;
                }
            }
        }

        boolean isReadMethod = baseCatalog.isReadMethod(method);
        boolean enforceMethod = baseCatalog.isWriteMethod(method);
        if (!enforceMethod) {
            if (!(props.isEnforceOnSensitiveReads() && isReadMethod && matches(props.getSensitiveReadPaths(), path))) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Usuario u = currentUserService.getOptional().orElse(null);
        if (u == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!isSensitiveUser(u)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI() != null ? request.getRequestURI() : "";
        String method = request.getMethod() != null ? request.getMethod() : "";
        String ip = ipResolver.resolve(request);

        SecurityActionDecision decision = policyManager.resolve(method, path, baseCatalog);
        SecurityActionPolicy policy = policyManager.effectivePolicy(decision.action(), baseCatalog);

        String actionHash = sha256Hex(decision.action().name() + "|" + method + "|" + path + "|" + safe(decision.ruleId()));

        if (policy.justificativaRequired()) {
            String justificativa = request.getHeader("X-PJB-Justificativa");
            if (justificativa == null || justificativa.trim().isEmpty()) {
                deny(response, u, ip, 428, "JUSTIFICATIVA_REQUIRED",
                        "Este ato exige justificativa.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
            RequestContext.setJustificativa(justificativa.trim());
        }

        UserSecurityProfile profile = profileRepo.findByUserId(u.getId()).orElse(null);

        if (policy.advogadoBaptismRequired() && u.isAdvogado()) {
            if (profile == null || profile.getAdvBaptizedAt() == null) {
                deny(response, u, ip, 428, "ADV_BAPTISM_REQUIRED",
                        "Para executar atos sensíveis, conclua o batismo.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
        }

        if (policy.govBrRequired()) {
            if (profile == null || profile.getGovVerifiedAt() == null) {
                deny(response, u, ip, 428, "GOVBR_REQUIRED",
                        "Este ato exige verificação recente via conta gov.br.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId()), "hint", "/api/v1/auth/govbr/stepup/start"),
                        policy, actionHash);
                return;
            }
            long age = Math.abs(Duration.between(profile.getGovVerifiedAt(), LocalDateTime.now()).getSeconds());
            if (policy.govBrMaxAgeSeconds() > 0 && age > policy.govBrMaxAgeSeconds()) {
                deny(response, u, ip, 428, "GOVBR_EXPIRED",
                        "Verificação gov.br expirada para este ato.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId()), "maxAgeSeconds", policy.govBrMaxAgeSeconds(), "hint", "/api/v1/auth/govbr/stepup/start"),
                        policy, actionHash);
                return;
            }
        }

        StrongAuthState strong = StrongAuthState.from(request);

        if (policy.stepUpRequired()) {
            if (!strong.isPresent() || strong.issuedAt() == null) {
                deny(response, u, ip, 428, "STRONG_AUTH_REQUIRED",
                        "Este ato exige autenticação forte.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
        }

        if (policy.strongAuthMaxAgeSeconds() > 0) {
            if (!strong.isPresent() || strong.issuedAt() == null) {
                deny(response, u, ip, 428, "STRONG_AUTH_REQUIRED",
                        "Este ato exige autenticação forte.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
            long age = Math.abs(Duration.between(strong.issuedAt(), LocalDateTime.now()).getSeconds());
            if (age > policy.strongAuthMaxAgeSeconds()) {
                deny(response, u, ip, 428, "STRONG_AUTH_EXPIRED",
                        "Autenticação forte expirada para este ato.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId()), "maxAgeSeconds", policy.strongAuthMaxAgeSeconds()),
                        policy, actionHash);
                return;
            }
        }

        if (policy.passkeyRequiredForAdvogado() && u.isAdvogado()) {
            if (!"PASSKEY".equalsIgnoreCase(strong.method())) {
                deny(response, u, ip, 428, "PASSKEY_REQUIRED",
                        "Este ato exige autenticação forte por passkey.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId()), "hint", "/api/v1/auth/passkey"),
                        policy, actionHash);
                return;
            }
        }

        if (policy.passkeyRequiredForAdmin() && isAdminUser(u)) {
            if (!"PASSKEY".equalsIgnoreCase(strong.method())) {
                deny(response, u, ip, 428, "PASSKEY_REQUIRED",
                        "Este ato exige autenticação forte.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
        }

        if (!policy.deviceRequired()) {
            allow(filterChain, request, response, policy, actionHash);
            return;
        }

        Long deviceId = parseLong(request.getHeader("X-Device-ID"));
        if (deviceId == null) {
            Object fromSession = request.getAttribute("PJB_DEVICE_ID");
            if (fromSession instanceof Long l) deviceId = l;
            if (fromSession instanceof String s) deviceId = parseLong(s);
        }

        if (deviceId == null) {
            deny(response, u, ip, 403, "DEVICE_REQUIRED",
                    "Para executar esta ação, vincule um dispositivo confiável.",
                    Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                    policy, actionHash);
            return;
        }

        if (policy.bindStrongAuthToDevice()) {
            Long strongDeviceId = strong.deviceId();
            if (strongDeviceId == null || !Objects.equals(strongDeviceId, deviceId)) {
                deny(response, u, ip, 409, "STRONG_AUTH_DEVICE_MISMATCH",
                        "Autenticação forte não corresponde ao dispositivo ativo.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId()), "deviceId", deviceId, "strongDeviceId", String.valueOf(strongDeviceId)),
                        policy, actionHash);
                return;
            }
        }

        TrustedDevice d = deviceRepo.findByIdAndUser(deviceId, u.getId()).orElse(null);
        if (d == null || d.isRevogado()) {
            deny(response, u, ip, 403, "DEVICE_INVALID",
                    "Dispositivo inválido ou revogado.",
                    Map.of("deviceId", deviceId, "path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                    policy, actionHash);
            return;
        }

        request.setAttribute("PJB_DEVICE_ID", deviceId);

        if (policy.attestationTrustedRequired() && !d.isAttestationTrusted()) {
            deny(response, u, ip, 403, "DEVICE_ATTESTATION_REQUIRED",
                    "Este ato exige dispositivo com attestation confiável.",
                    Map.of("deviceId", deviceId, "attestationFmt", safe(d.getAttestationFmt()), "path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                    policy, actionHash);
            return;
        }

        if (policy.verifiedDeviceRequired() && (d.getPendingChallengeId() != null || d.getVerifiedAt() == null)) {
            Map<String, Object> det = new LinkedHashMap<>();
            det.put("deviceId", deviceId);
            if (d.getPendingChallengeId() != null) det.put("challengeId", d.getPendingChallengeId());
            det.put("path", safe(path));
            det.put("action", decision.action().name());
            det.put("rule", safe(decision.ruleId()));
            deny(response, u, ip, 428, "DEVICE_CHALLENGE_REQUIRED",
                    "Novo dispositivo exige confirmação adicional.",
                    det,
                    policy, actionHash);
            return;
        }

        LocalDateTime q = d.getQuarentenaAte();
        if (q != null && q.isAfter(LocalDateTime.now())) {
            boolean isReadMethod = baseCatalog.isReadMethod(method);
            if (props.isQuarantineReadOnlyEnabled()) {
                if (isReadMethod) {
                    if (!policy.allowReadDuringQuarantine()) {
                        deny(response, u, ip, 423, "DEVICE_QUARANTINE_ACTION_BLOCKED",
                                "Novo dispositivo em quarentena: ação não permitida.",
                                Map.of("deviceId", deviceId, "quarentenaAte", q.toString(), "path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                                policy, actionHash);
                        return;
                    }
                } else {
                    if (!policy.allowWriteDuringQuarantine()) {
                        deny(response, u, ip, 423, "DEVICE_QUARANTINE_ACTION_BLOCKED",
                                "Novo dispositivo em quarentena: apenas leitura.",
                                Map.of("deviceId", deviceId, "quarentenaAte", q.toString(), "path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                                policy, actionHash);
                        return;
                    }
                }
            } else {
                deny(response, u, ip, 423, "DEVICE_QUARANTINE",
                        "Novo dispositivo em quarentena.",
                        Map.of("deviceId", deviceId, "quarentenaAte", q.toString(), "path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
        }

        Long equipeId = activeEquipeId();

        String requestHash;
        String resolvedBodyHash = resolveBodyHash(request);

        String ct = request.getContentType();
        boolean isJsonBody = ct != null && ct.toLowerCase(java.util.Locale.ROOT).startsWith("application/json");

        if (isJsonBody && props.isRequireBodyHashForOneTimeStepUpWrites() && policy.oneTimeStepUp() && baseCatalog.isWriteMethod(method)) {
            String header = request.getHeader("X-PJB-Body-Hash");
            if (header == null || header.isBlank()) {
                deny(response, u, ip, 428, "BODY_HASH_REQUIRED",
                        "Este ato exige X-PJB-Body-Hash.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
            if (resolvedBodyHash == null) {
                deny(response, u, ip, 400, "BODY_HASH_INVALID",
                        "Body hash inválido.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
        }

        try {
            requestHash = requestHashService.compute(method, path, request.getQueryString(), equipeId, deviceId, resolvedBodyHash);
        } catch (IllegalArgumentException e) {
            requestHash = null;
        }

        if (policy.dualApprovalRequired()) {
            Long approvalId = parseLong(request.getHeader("X-PJB-Approval"));
            if (approvalId == null || !dualApprovalService.isApprovedFor(u, approvalId, actionHash, equipeId)) {
                int ttl = policy.dualApprovalTtlSeconds() > 0 ? policy.dualApprovalTtlSeconds() : props.getDualApprovalDefaultTtlSeconds();
                SecurityDualApprovalRequest ar = dualApprovalService.request(u, deviceId, equipeId, decision.action().name(), method, path, decision.ruleId(), actionHash, ttl);
                deny(response, u, ip, 409, "DUAL_APPROVAL_REQUIRED",
                        "Este ato exige dupla aprovação.",
                        Map.of("approvalId", ar.getId(), "approveEndpoint", "/api/v1/security/approvals/" + ar.getId() + "/approve", "expiresAt", ar.getExpiresAt().toString(), "path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
        }

        if (policy.oneTimeStepUp()) {
            if (!"PASSKEY".equalsIgnoreCase(strong.method())) {
                deny(response, u, ip, 428, "PASSKEY_REQUIRED",
                        "Este ato exige step-up por passkey.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
            Long sessionId = strong.sessionId();
            if (sessionId == null) {
                deny(response, u, ip, 428, "STRONG_AUTH_REQUIRED",
                        "Este ato exige step-up por passkey.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
            if (!strong.scopeOneShot() || strong.scopeAction() == null || strong.scopeRequestHash() == null) {
                deny(response, u, ip, 428, "STEP_UP_SCOPE_REQUIRED",
                        "Este ato exige step-up escopado.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
            if (!decision.action().name().equalsIgnoreCase(strong.scopeAction())) {
                deny(response, u, ip, 409, "STEP_UP_SCOPE_MISMATCH",
                        "Step-up não corresponde à ação.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId()), "scopeAction", strong.scopeAction()),
                        policy, actionHash);
                return;
            }
            if (!equalsIgnoreCase(strong.scopeRequestHash(), String.valueOf(requestHash))) {
                deny(response, u, ip, 409, "STEP_UP_REQHASH_MISMATCH",
                        "Step-up não corresponde à requisição.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
            try {
                strongAuthUsageService.consumeOnce(sessionId, u, actionHash, requestHash);
            } catch (Exception e) {
                deny(response, u, ip, 409, "STEP_UP_CONSUMED",
                        "Step-up já utilizado.",
                        Map.of("path", safe(path), "action", decision.action().name(), "rule", safe(decision.ruleId())),
                        policy, actionHash);
                return;
            }
        }

        boolean sensitiveRead = baseCatalog.isReadMethod(method) && matches(props.getSensitiveReadPaths(), path);
        if (sensitiveRead && props.isWatermarkEnabled()) {
            request.setAttribute("PJB_WATERMARK_REQUIRED", Boolean.TRUE);
            String wmSeed = "wm|u=" + u.getId() + "|d=" + deviceId + "|rh=" + (requestHash != null ? requestHash : actionHash) + "|t=" + System.nanoTime() + "|r=" + random.nextLong();
            request.setAttribute("PJB_WATERMARK_ID", sha256Hex(wmSeed));
        }

        deviceService.markUsed(d);
        allow(filterChain, request, response, policy, actionHash);
    }

    private void allow(FilterChain chain,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       SecurityActionPolicy policy,
                       String actionHash) throws IOException, ServletException {
        if (policy.auditRequired()) {
            auditLedgerService.appendSafely("DEVICE_POLICY_ALLOW", "HTTP", safe(request.getRequestURI()), actionHash);
        }
        chain.doFilter(request, response);
    }

    private void deny(HttpServletResponse response,
                      Usuario u,
                      String ip,
                      int status,
                      String code,
                      String message,
                      Map<String, Object> details,
                      SecurityActionPolicy policy,
                      String actionHash) throws IOException {

        alertService.create(u, code, "Ação bloqueada", "path=" + safe(String.valueOf(details != null ? details.get("path") : "")) + " action=" + (details != null ? details.get("action") : null), ip, 50);

        if (policy.auditRequired()) {
            auditLedgerService.appendSafely("DEVICE_POLICY_DENY", "HTTP", safe(details != null ? String.valueOf(details.get("path")) : null), actionHash, safe(code));
        }

        writeJson(response, status, code, message, details);
    }

    private static boolean isSensitiveUser(Usuario u) {
        if (u == null || u.getTipoUsuario() == null) return false;
        String tipo = u.getTipoUsuario().name();
        return tipo.contains("ADVOGADO")
                || tipo.contains("JUIZ")
                || tipo.contains("MINISTRO")
                || tipo.contains("DESEMBARGADOR")
                || tipo.contains("PROMOTOR")
                || tipo.contains("DEFENSOR")
                || tipo.contains("PROCURADOR")
                || tipo.contains("SERVIDOR")
                || tipo.contains("ADMIN");
    }

    private static boolean isAdminUser(Usuario u) {
        if (u == null || u.getTipoUsuario() == null) return false;
        return u.getTipoUsuario().name().contains("ADMIN");
    }

    private Long parseLong(String v) {
        try {
            if (v == null) return null;
            String s = v.trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private void writeJson(HttpServletResponse response,
                           int status,
                           String code,
                           String message,
                           Map<String, Object> details) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        if (details != null && !details.isEmpty()) body.put("details", details);

        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();

        log.warn("[DEVICE_POLICY] status={} code={} path={} action={} rule={}",
                status,
                code,
                details != null ? details.get("path") : null,
                details != null ? details.get("action") : null,
                details != null ? details.get("rule") : null);
    }

    private String resolveBodyHash(HttpServletRequest request) {
        if (request == null) return null;
        String header = request.getHeader("X-PJB-Body-Hash");
        if (header != null && !header.isBlank()) {
            return normalizeHex64(header);
        }
        Object attr = request.getAttribute("PJB_BODY_HASH");
        if (attr instanceof String s && !s.isBlank()) {
            return normalizeHex64(s);
        }
        return null;
    }

    private static String normalizeHex64(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.length() != 64) return null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!ok) return null;
        }
        return s.toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }

    private String safe(String v) {
        if (v == null) return "";
        String s = v.trim();
        if (s.isEmpty()) return "";
        if (s.length() > 160) s = s.substring(0, 160);
        return s.replaceAll("[^a-zA-Z0-9_./-]", "");
    }

    private boolean matches(Iterable<String> patterns, String path) {
        if (patterns == null || path == null) return false;
        for (String pattern : patterns) {
            if (pattern != null && !pattern.isBlank() && pathMatcher.match(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }

    private Long activeEquipeId() {
        MembroEquipe m = RequestContext.getMembroEquipeAtivo().orElse(null);
        if (m == null || m.getEquipe() == null) return null;
        return m.getEquipe().getId();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
