package com.tcc.pjb.backend.core.security.device;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityChallenge;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;

@Service
public class TrustedDeviceService {

    private final TrustedDeviceRepository deviceRepo;
    private final UserSecurityProfileRepository profileRepo;
    private final DeviceRiskEngine riskEngine;
    private final SecurityAlertService alertService;
    private final SecurityChallengeService challengeService;
    private final DeviceSecurityProperties props;
    private final GovBrOidcProperties govbr;

    public TrustedDeviceService(TrustedDeviceRepository deviceRepo,
                               UserSecurityProfileRepository profileRepo,
                               DeviceRiskEngine riskEngine,
                               SecurityAlertService alertService,
                               SecurityChallengeService challengeService,
                               DeviceSecurityProperties props,
                               GovBrOidcProperties govbr) {
        this.deviceRepo = Objects.requireNonNull(deviceRepo);
        this.profileRepo = Objects.requireNonNull(profileRepo);
        this.riskEngine = Objects.requireNonNull(riskEngine);
        this.alertService = Objects.requireNonNull(alertService);
        this.challengeService = Objects.requireNonNull(challengeService);
        this.props = Objects.requireNonNull(props);
        this.govbr = Objects.requireNonNull(govbr);
    }

    @Transactional(readOnly = true)
    public List<TrustedDevice> listActiveDevices(Long userId) {
        return deviceRepo.findActiveByUser(userId);
    }

    @Transactional
    public TrustedDevice register(Usuario usuario,
                                 String credentialId,
                                 String publicKey,
                                 String alias,
                                 String aaguid,
                                 String attestationFmt,
                                 boolean attestationTrusted,
                                 String ip) {
        return registerInternal(usuario, credentialId, publicKey, alias, aaguid, attestationFmt, attestationTrusted, 0L, ip);
    }

    @Transactional
    public TrustedDevice registerWebAuthn(Usuario usuario,
                                         String credentialId,
                                         String publicKeyCoseBase64Url,
                                         String alias,
                                         String aaguid,
                                         String attestationFmt,
                                         boolean attestationTrusted,
                                         long signatureCount,
                                         String ip) {
        return registerInternal(usuario, credentialId, publicKeyCoseBase64Url, alias, aaguid, attestationFmt, attestationTrusted, signatureCount, ip);
    }

    private TrustedDevice registerInternal(Usuario usuario,
                                          String credentialId,
                                          String publicKey,
                                          String alias,
                                          String aaguid,
                                          String attestationFmt,
                                          boolean attestationTrusted,
                                          long signatureCount,
                                          String ip) {

        Objects.requireNonNull(usuario, "usuario");
        String cred = normalize(credentialId, 512, true);
        String pk = normalize(publicKey, 4096, true);
        String al = normalize(alias, 80, true);

        Optional<TrustedDevice> existing = deviceRepo.findByCredentialId(cred);
        if (existing.isPresent()) {
            TrustedDevice d = existing.get();
            if (d.getUsuario() != null && Objects.equals(d.getUsuario().getId(), usuario.getId())) {
                return d;
            }
            throw new IllegalStateException("credentialId já registrado para outro usuário.");
        }

        UserSecurityProfile profile = profileRepo.findByUserId(usuario.getId()).orElse(null);
        RiskEvaluation risk = riskEngine.evaluateFirstLink(usuario, ip, profile, props.isDenyPrivateIps());

        if (risk.decision() == RiskDecision.DENY) {
            alertService.create(usuario, "DEVICE_LINK_DENY", "Vínculo de dispositivo bloqueado",
                    "Motivo: " + risk.reason() + " | network=" + risk.networkLabel(), ip, risk.riskScore());
            throw new IllegalStateException("Vínculo de dispositivo bloqueado por risco. " + risk.reason());
        }

        TrustedDevice d = new TrustedDevice();
        d.setUsuario(usuario);
        d.setCredentialId(cred);
        d.setPublicKey(pk);
        d.setAlias(al);
        d.setAaguid(normalize(aaguid, 64, false));
        d.setAttestationFmt(normalize(attestationFmt, 40, false));
        d.setAttestationTrusted(attestationTrusted);
        d.setSignCount(Math.max(0, signatureCount));
        d.setEnrollIp(normalize(ip, 64, false));
        d.setEnrollNetwork(normalize(risk.networkLabel(), 120, false));
        d.setEnrollSuspectNetwork(risk.suspectNetwork());
        d.setRiskScoreEnroll(risk.riskScore());
        d.setQuarentenaAte(LocalDateTime.now().plusHours(Math.max(1, props.getQuarantineHours())));

        if (risk.decision() == RiskDecision.CHALLENGE) {
            boolean govBrEnabled = govbr.enabled();
            boolean highRiskNetwork = isHighRiskNetwork(risk);
            if (govBrEnabled && highRiskNetwork) {
                SecurityChallenge c = challengeService.createGovBrStepUpChallenge(usuario, ip,
                        "Desafio de Vida (gov.br) exigido para novo dispositivo. network=" + risk.networkLabel());
                d.setPendingChallengeId(c.getId());
                d.setVerifiedAt(null);
                alertService.create(usuario, "DEVICE_LINK_GOVBR_CHALLENGE", "Novo dispositivo exige gov.br",
                        "challengeId=" + c.getId() + " | Motivo: " + risk.reason() + " | network=" + risk.networkLabel() + " | quarantineHours=" + props.getQuarantineHours(), ip, risk.riskScore());
            } else {
                SecurityChallenge c = challengeService.createEmailOtp(usuario, ip,
                        "Desafio adicional exigido para novo dispositivo. network=" + risk.networkLabel());
                d.setPendingChallengeId(c.getId());
                d.setVerifiedAt(null);
                alertService.create(usuario, "DEVICE_LINK_CHALLENGE", "Novo dispositivo em modo desafio",
                        "challengeId=" + c.getId() + " | Motivo: " + risk.reason() + " | quarantineHours=" + props.getQuarantineHours(), ip, risk.riskScore());
            }
        } else {
            d.setVerifiedAt(LocalDateTime.now());
            alertService.create(usuario, "DEVICE_LINK_OK", "Novo dispositivo vinculado",
                    "Novo dispositivo cadastrado. Em quarentena por " + props.getQuarantineHours() + "h.", ip, risk.riskScore());
        }

        TrustedDevice saved = deviceRepo.save(d);

        UserSecurityProfile p = profile != null ? profile : new UserSecurityProfile();
        if (p.getUsuario() == null) p.setUsuario(usuario);
        p.setLastLoginIp(normalize(ip, 64, false));
        String net = DeviceRiskEngine.networkFingerprint(ip);
        p.setLastLoginNetwork(normalize(net != null ? net : risk.networkLabel(), 120, false));
        p.setLastLoginAt(LocalDateTime.now());
        profileRepo.save(p);

        return saved;
    }

    @Transactional
    public void revoke(Usuario usuario, Long deviceId, String reason) {
        TrustedDevice d = deviceRepo.findByIdAndUser(deviceId, usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo não encontrado."));
        if (d.isRevogado()) return;
        d.setRevogadoEm(LocalDateTime.now());
        deviceRepo.save(d);
        alertService.create(usuario, "DEVICE_REVOKED", "Dispositivo revogado",
                "deviceId=" + deviceId + " reason=" + safe(reason), null, 0);
    }

    @Transactional
    public void markUsed(TrustedDevice device) {
        if (device == null || device.getId() == null) return;
        device.setUltimoUsoEm(LocalDateTime.now());
        deviceRepo.save(device);
    }

    private static String normalize(String v, int max, boolean required) {
        if (v == null) {
            if (required) throw new IllegalArgumentException("Campo obrigatório ausente.");
            return null;
        }
        String s = v.trim();
        if (required && s.isBlank()) throw new IllegalArgumentException("Campo obrigatório vazio.");
        if (s.isBlank()) return null;
        if (s.length() > max) s = s.substring(0, max);
        return s;
    }

    private static String safe(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.length() > 240) s = s.substring(0, 240);
        return s;
    }

    private static boolean isHighRiskNetwork(RiskEvaluation risk) {
        if (risk == null) return false;
        if (risk.suspectNetwork()) return true;
        String n = risk.networkLabel();
        if (n == null) return false;
        String s = n.toUpperCase(java.util.Locale.ROOT);
        return s.contains("GEO_VELOCITY") || s.contains("PRIVATE") || s.contains("NO_IP") || s.contains("RESERVED");
    }
}
