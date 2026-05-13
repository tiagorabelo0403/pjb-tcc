package com.tcc.pjb.backend.service.security.surface;

import com.fasterxml.jackson.databind.JsonNode;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.AdvogadoBaptismService;
import com.tcc.pjb.backend.core.security.device.PanicService;
import com.tcc.pjb.backend.core.security.device.SecurityChallengeService;
import com.tcc.pjb.backend.core.security.device.policy.StrongAuthState;
import com.tcc.pjb.backend.core.security.device.reqhash.BodyHashService;
import com.tcc.pjb.backend.core.security.device.reqhash.RequestHashService;
import com.tcc.pjb.backend.core.security.webauthn.WebAuthnService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.dto.security.BaptismCompleteRequest;
import com.tcc.pjb.backend.model.dto.security.BaptismStartResponse;
import com.tcc.pjb.backend.model.dto.security.BodyHashResponse;
import com.tcc.pjb.backend.model.dto.security.ChallengeVerifyRequest;
import com.tcc.pjb.backend.model.dto.security.PanicStatusResponse;
import com.tcc.pjb.backend.model.dto.security.PanicTriggerRequest;
import com.tcc.pjb.backend.model.dto.security.PanicTriggerResponse;
import com.tcc.pjb.backend.model.dto.security.RequestHashComputeRequest;
import com.tcc.pjb.backend.model.dto.security.RequestHashResponse;
import com.tcc.pjb.backend.model.dto.security.SecurityChallengeVerificationDetailsResponse;
import com.tcc.pjb.backend.model.dto.security.SecurityChallengeVerificationResponse;
import com.tcc.pjb.backend.model.dto.security.SecurityOperationResponse;
import com.tcc.pjb.backend.model.dto.security.WebAuthnEnrollFinishRequest;
import com.tcc.pjb.backend.model.dto.security.WebAuthnEnrollmentChallengeResponse;
import com.tcc.pjb.backend.model.dto.security.WebAuthnEnrollmentFinishResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityChallenge;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.modularity.PjbPublicApi;

@Service
@PjbPublicApi(module = PjbModuleId.IDENTIDADE_SEGURANCA)
public class SecuritySurfaceFacadeService {

    private final CurrentUserService currentUserService;
    private final PanicService panicService;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final ClientIpResolver ipResolver;
    private final SecurityChallengeService challengeService;
    private final WebAuthnService webAuthnService;
    private final BodyHashService bodyHashService;
    private final RequestHashService requestHashService;
    private final AdvogadoBaptismService baptismService;

    public SecuritySurfaceFacadeService(CurrentUserService currentUserService,
                                        PanicService panicService,
                                        TrustedDeviceRepository trustedDeviceRepository,
                                        ClientIpResolver ipResolver,
                                        SecurityChallengeService challengeService,
                                        WebAuthnService webAuthnService,
                                        BodyHashService bodyHashService,
                                        RequestHashService requestHashService,
                                        AdvogadoBaptismService baptismService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.panicService = Objects.requireNonNull(panicService);
        this.trustedDeviceRepository = Objects.requireNonNull(trustedDeviceRepository);
        this.ipResolver = Objects.requireNonNull(ipResolver);
        this.challengeService = Objects.requireNonNull(challengeService);
        this.webAuthnService = Objects.requireNonNull(webAuthnService);
        this.bodyHashService = Objects.requireNonNull(bodyHashService);
        this.requestHashService = Objects.requireNonNull(requestHashService);
        this.baptismService = Objects.requireNonNull(baptismService);
    }

    public PanicStatusResponse panicStatus() {
        Usuario usuario = currentUserService.getRequired();
        var status = panicService.status(usuario);
        return new PanicStatusResponse(status.frozen(), status.frozenAt(), status.frozenUntil(), status.reason());
    }

    public PanicTriggerResponse panicTrigger(PanicTriggerRequest request, HttpServletRequest servletRequest) {
        Usuario usuario = currentUserService.getRequired();
        enforceStrongPasskey(usuario, servletRequest);
        Long deviceId = resolveDeviceId(servletRequest);
        TrustedDevice device = requireVerifiedDevice(usuario, deviceId);
        String ip = ipResolver.resolve(servletRequest);
        var result = panicService.trigger(usuario, device.getId(), ip, request.getReason(), request.getFreezeMinutes(), request.isHard());
        return new PanicTriggerResponse(result.frozenAt(), result.frozenUntil(), result.reason(), result.deviceId());
    }

    public SecurityChallengeVerificationResponse verifyChallenge(Long challengeId, ChallengeVerifyRequest request) {
        Usuario usuario = currentUserService.getRequired();
        SecurityChallenge challenge = challengeService.getRequired(challengeId);
        String tipo = challenge.getTipo() != null ? challenge.getTipo().trim() : "";
        if (!"EMAIL_OTP".equalsIgnoreCase(tipo)) {
            List<Long> pendingDevices = challengeService.pendingDeviceIdsForChallenge(usuario.getId(), challengeId);
            Long deviceId = pendingDevices.size() == 1 ? pendingDevices.get(0) : null;
            String hint = "GOVBR_STEPUP".equalsIgnoreCase(tipo) ? "/api/v1/auth/govbr/stepup/start" : null;
            String message = "GOVBR_STEPUP".equalsIgnoreCase(tipo)
                    ? "Este desafio exige verificação via conta gov.br."
                    : "Tipo de desafio não suportado por este endpoint.";
            return new SecurityChallengeVerificationResponse(
                    false,
                    "CHALLENGE_STEP_UP_REQUIRED",
                    message,
                    new SecurityChallengeVerificationDetailsResponse(tipo, deviceId, List.copyOf(pendingDevices), hint)
            );
        }
        challengeService.consumeOtp(challengeId, usuario, request.getCode());
        return new SecurityChallengeVerificationResponse(true, null, null, null);
    }

    public HttpStatus challengeVerifyHttpStatus(SecurityChallengeVerificationResponse response) {
        return response != null && !response.ok() && "CHALLENGE_STEP_UP_REQUIRED".equals(response.code())
                ? HttpStatus.PRECONDITION_REQUIRED
                : HttpStatus.OK;
    }

    public WebAuthnEnrollmentChallengeResponse startEnrollment() {
        Usuario usuario = currentUserService.getRequired();
        var start = webAuthnService.startEnrollment(usuario);
        return new WebAuthnEnrollmentChallengeResponse(start.sessionId(), start.optionsJson());
    }

    public WebAuthnEnrollmentFinishResponse finishEnrollment(WebAuthnEnrollFinishRequest request, HttpServletRequest servletRequest) {
        Usuario usuario = currentUserService.getRequired();
        String ip = ipResolver.resolve(servletRequest);
        var result = webAuthnService.finishEnrollment(usuario, request.getSessionId(), request.getCredentialJson(), request.getAlias(), ip);
        return new WebAuthnEnrollmentFinishResponse(result.deviceId(), result.pendingChallengeId());
    }

    public BodyHashResponse computeBodyHash(JsonNode body) {
        currentUserService.getRequired();
        return new BodyHashResponse(bodyHashService.canonicalJsonHash(body));
    }

    public RequestHashResponse computeRequestHash(RequestHashComputeRequest request) {
        currentUserService.getRequired();
        return new RequestHashResponse(requestHashService.compute(
                request.getMethod(),
                request.getPath(),
                request.getQuery(),
                request.getEquipeId(),
                request.getDeviceId(),
                request.getBodyHash()
        ));
    }

    public BaptismStartResponse startBaptism(HttpServletRequest request) {
        Usuario usuario = currentUserService.getRequired();
        String ip = ipResolver.resolve(request);
        var start = baptismService.start(usuario, ip);
        return new BaptismStartResponse(start.challengeId(), start.nonceBase64Url(), start.termText());
    }

    public SecurityOperationResponse completeBaptism(BaptismCompleteRequest request) {
        Usuario usuario = currentUserService.getRequired();
        baptismService.complete(usuario, request.getChallengeId(), request.getSignatureBase64(), request.getCertificateDerBase64(), request.getSignatureAlgorithm());
        return new SecurityOperationResponse(true);
    }

    private void enforceStrongPasskey(Usuario usuario, HttpServletRequest request) {
        StrongAuthState strong = StrongAuthState.from(request);
        if (!strong.isPresent()) {
            throw new AccessDeniedPjbException("autenticação forte obrigatória");
        }
        if (!"PASSKEY".equalsIgnoreCase(strong.method())) {
            throw new AccessDeniedPjbException("passkey obrigatória");
        }
        if (strong.issuedAt() == null) {
            throw new AccessDeniedPjbException("autenticação forte inválida");
        }
        long age = Math.abs(Duration.between(strong.issuedAt(), LocalDateTime.now()).getSeconds());
        if (age > 300) {
            throw new AccessDeniedPjbException("autenticação forte expirada");
        }
        if (usuario == null || usuario.getId() == null) {
            throw new AccessDeniedPjbException("usuário inválido");
        }
    }

    private Long resolveDeviceId(HttpServletRequest request) {
        String header = request != null ? request.getHeader("X-Device-ID") : null;
        Long id = parseLong(header);
        if (id != null) {
            return id;
        }
        Object fromSession = request != null ? request.getAttribute("PJB_DEVICE_ID") : null;
        if (fromSession instanceof Long value) {
            return value;
        }
        if (fromSession != null) {
            return parseLong(String.valueOf(fromSession));
        }
        throw new AccessDeniedPjbException("device obrigatório");
    }

    private TrustedDevice requireVerifiedDevice(Usuario usuario, Long deviceId) {
        TrustedDevice device = trustedDeviceRepository.findByIdAndUser(deviceId, usuario.getId()).orElse(null);
        if (device == null || device.isRevogado()) {
            throw new AccessDeniedPjbException("device inválido");
        }
        if (device.getPendingChallengeId() != null || device.getVerifiedAt() == null) {
            throw new AccessDeniedPjbException("device não verificado");
        }
        LocalDateTime quarantine = device.getQuarentenaAte();
        if (quarantine != null && quarantine.isAfter(LocalDateTime.now())) {
            throw new AccessDeniedPjbException("device em quarentena");
        }
        return device;
    }

    private Long parseLong(String value) {
        try {
            if (value == null) {
                return null;
            }
            String normalized = value.trim();
            if (normalized.isEmpty()) {
                return null;
            }
            return Long.parseLong(normalized);
        } catch (Exception ex) {
            return null;
        }
    }
}
