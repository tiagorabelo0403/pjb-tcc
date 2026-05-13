package com.tcc.pjb.backend.core.security.device;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityChallenge;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;

@Service
public class AdvogadoBaptismService {

    private final SecurityChallengeService challengeService;
    private final UserSecurityProfileRepository profileRepo;

    public AdvogadoBaptismService(SecurityChallengeService challengeService,
                                  UserSecurityProfileRepository profileRepo) {
        this.challengeService = Objects.requireNonNull(challengeService);
        this.profileRepo = Objects.requireNonNull(profileRepo);
    }

    @Transactional
    public BaptismStart start(Usuario usuario, String ip) {
        Objects.requireNonNull(usuario, "usuario");

        String details = "adv_baptism oab=" + safe(usuario.getOabNormalizada());
        SecurityChallenge c = challengeService.createBaptismNonce(usuario, ip, details);

        String term = "TERMO DE ACESSO PJB\n" +
                "Declaro, sob as penas da lei, que sou o titular da OAB informada e autorizo o vínculo seguro de dispositivo e acesso ao PJB.\n" +
                "Nonce: " + c.getNonce();

        return new BaptismStart(c.getId(), c.getNonce(), term);
    }

    @Transactional
    public void complete(Usuario usuario,
                         Long challengeId,
                         String signatureBase64,
                         String certificateDerBase64,
                         String signatureAlgorithm) {

        Objects.requireNonNull(usuario, "usuario");
        if (challengeId == null) throw new IllegalArgumentException("challengeId obrigatório");

        SecurityChallenge c = challengeService.getRequired(challengeId);
        if (c.isConsumed()) throw new IllegalStateException("Challenge já consumido");
        if (c.isExpired()) throw new IllegalStateException("Challenge expirado");
        if (c.getUsuario() == null || !Objects.equals(c.getUsuario().getId(), usuario.getId())) {
            throw new IllegalArgumentException("Challenge não pertence ao usuário");
        }
        if (!"A3_NONCE".equalsIgnoreCase(c.getTipo())) {
            throw new IllegalArgumentException("Tipo de challenge inválido");
        }

        String nonceB64 = c.getNonce();
        if (nonceB64 == null || nonceB64.isBlank()) throw new IllegalStateException("Nonce ausente");

        byte[] nonce = java.util.Base64.getUrlDecoder().decode(nonceB64);
        byte[] sig = java.util.Base64.getDecoder().decode(req(signatureBase64));
        byte[] certDer = java.util.Base64.getDecoder().decode(req(certificateDerBase64));

        X509Certificate cert = parseCert(certDer);
        String alg = chooseAlgorithm(cert, signatureAlgorithm);

        if (!verify(cert, alg, nonce, sig)) {
            throw new IllegalArgumentException("Assinatura inválida");
        }

        String fp = sha256Hex(certDer);

        UserSecurityProfile p = profileRepo.findByUserId(usuario.getId()).orElse(null);
        if (p == null) {
            p = new UserSecurityProfile();
            p.setUsuario(usuario);
        }
        p.setAdvBaptizedAt(LocalDateTime.now());
        p.setAdvBaptismMethod("A3_SIGNATURE");
        p.setAdvBaptismCertFingerprint(fp);
        p.setLastStrongAuthAt(LocalDateTime.now());
        profileRepo.save(p);

        challengeService.markConsumed(c);
    }

    private static X509Certificate parseCert(byte[] der) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
            cert.checkValidity();
            boolean[] ku = cert.getKeyUsage();
            if (ku != null && ku.length > 0 && !ku[0]) {
                throw new IllegalArgumentException("Certificado sem KeyUsage de assinatura");
            }
            return cert;
        } catch (Exception e) {
            throw new IllegalArgumentException("Certificado inválido", e);
        }
    }

    private static String chooseAlgorithm(X509Certificate cert, String requested) {
        String alg = requested == null ? null : requested.trim();
        if (alg != null && alg.isBlank()) alg = null;

        String effective = alg != null ? alg : defaultAlgFor(cert);
        if (!isAllowedAlgorithm(effective)) {
            throw new IllegalArgumentException("Algoritmo de assinatura não permitido");
        }
        return effective;
    }

    private static String defaultAlgFor(X509Certificate cert) {
        if (cert == null || cert.getPublicKey() == null) return "SHA256withRSA";
        if (cert.getPublicKey() instanceof RSAPublicKey) return "SHA256withRSA";
        if (cert.getPublicKey() instanceof ECPublicKey) return "SHA256withECDSA";
        return "SHA256withRSA";
    }

    private static boolean isAllowedAlgorithm(String alg) {
        if (alg == null) return false;
        String a = alg.trim();
        return a.equalsIgnoreCase("SHA256withRSA")
                || a.equalsIgnoreCase("SHA384withRSA")
                || a.equalsIgnoreCase("SHA256withECDSA")
                || a.equalsIgnoreCase("SHA384withECDSA");
    }

    private static boolean verify(X509Certificate cert, String alg, byte[] data, byte[] sig) {
        try {
            Signature s = Signature.getInstance(alg);
            s.initVerify(cert.getPublicKey());
            s.update(data);
            return s.verify(sig);
        } catch (Exception e) {
            return false;
        }
    }

    private static String sha256Hex(byte[] b) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(b));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static String req(String v) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException("campo obrigatório ausente");
        return v.trim();
    }

    private static String safe(String v) {
        if (v == null) return "";
        String s = v.trim();
        if (s.length() > 120) s = s.substring(0, 120);
        return s;
    }

    public record BaptismStart(Long challengeId, String nonceBase64Url, String termText) {}
}
