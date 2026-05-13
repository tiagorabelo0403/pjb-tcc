package com.tcc.pjb.backend.core.security.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KeyMaterialService {

    private static final AtomicInteger ZEROIZE_GUARD = new AtomicInteger();

    private final String masterKeyBase64;

    private volatile SecretKey dcpHmacKey;
    private volatile SecretKey faceReauthHmacKey;
    private volatile SecretKey uiAccessibilitySuggestHmacKey;
    private volatile SecretKey uiPolicyHmacKey;
    private volatile SecretKey custodyMeshHmacKey;
    private volatile SecretKey operationalCertificateHmacKey;
    private volatile SecretKey operationalAnnexationHmacKey;
    private volatile SecretKey operationalMeshDispatchHmacKey;

    public KeyMaterialService(@Value("${pjb.security.master-key:}") String masterKeyBase64) {
        this.masterKeyBase64 = masterKeyBase64;
    }

    public SecretKey getDelegationSigningKey() {
        SecretKey cached = dcpHmacKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (dcpHmacKey == null) {
                byte[] master = decodeMasterKey();
                byte[] derived = hmacSha256(master, "PJB-DCP-SIGN-v1".getBytes(StandardCharsets.UTF_8));
                dcpHmacKey = new SecretKeySpec(derived, "HmacSHA256");
                zeroize(master);
            }
            return dcpHmacKey;
        }
    }

    public SecretKey getFaceReauthSigningKey() {
        SecretKey cached = faceReauthHmacKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (faceReauthHmacKey == null) {
                byte[] master = decodeMasterKey();
                byte[] derived = hmacSha256(master, "PJB-FACE-REAUTH-SIGN-v1".getBytes(StandardCharsets.UTF_8));
                faceReauthHmacKey = new SecretKeySpec(derived, "HmacSHA256");
                zeroize(master);
            }
            return faceReauthHmacKey;
        }
    }

    public SecretKey getUiAccessibilitySuggestSigningKey() {
        SecretKey cached = uiAccessibilitySuggestHmacKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (uiAccessibilitySuggestHmacKey == null) {
                byte[] master = decodeMasterKey();
                byte[] derived = hmacSha256(master, "PJB-UI-A11Y-SUGGEST-SIGN-v1".getBytes(StandardCharsets.UTF_8));
                uiAccessibilitySuggestHmacKey = new SecretKeySpec(derived, "HmacSHA256");
                zeroize(master);
            }
            return uiAccessibilitySuggestHmacKey;
        }
    }

    public SecretKey getUiPolicySigningKey() {
        SecretKey cached = uiPolicyHmacKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (uiPolicyHmacKey == null) {
                byte[] master = decodeMasterKey();
                byte[] derived = hmacSha256(master, "PJB-UI-POLICY-SIGN-v1".getBytes(StandardCharsets.UTF_8));
                uiPolicyHmacKey = new SecretKeySpec(derived, "HmacSHA256");
                zeroize(master);
            }
            return uiPolicyHmacKey;
        }
    }

    public SecretKey getCustodyMeshSigningKey() {
        SecretKey cached = custodyMeshHmacKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (custodyMeshHmacKey == null) {
                byte[] master = decodeMasterKey();
                byte[] derived = hmacSha256(master, "PJB-CUSTODY-MESH-SIGN-v1".getBytes(StandardCharsets.UTF_8));
                custodyMeshHmacKey = new SecretKeySpec(derived, "HmacSHA256");
                zeroize(master);
            }
            return custodyMeshHmacKey;
        }
    }

    public SecretKey getOperationalCertificateSigningKey() {
        SecretKey cached = operationalCertificateHmacKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (operationalCertificateHmacKey == null) {
                byte[] master = decodeMasterKey();
                byte[] derived = hmacSha256(master, "PJB-DILIGENCE-CERT-SIGN-v1".getBytes(StandardCharsets.UTF_8));
                operationalCertificateHmacKey = new SecretKeySpec(derived, "HmacSHA256");
                zeroize(master);
            }
            return operationalCertificateHmacKey;
        }
    }

    public SecretKey getOperationalAnnexationSigningKey() {
        SecretKey cached = operationalAnnexationHmacKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (operationalAnnexationHmacKey == null) {
                byte[] master = decodeMasterKey();
                byte[] derived = hmacSha256(master, "PJB-DILIGENCE-ANNEX-SIGN-v1".getBytes(StandardCharsets.UTF_8));
                operationalAnnexationHmacKey = new SecretKeySpec(derived, "HmacSHA256");
                zeroize(master);
            }
            return operationalAnnexationHmacKey;
        }
    }


    public SecretKey getOperationalMeshDispatchSigningKey() {
        SecretKey cached = operationalMeshDispatchHmacKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (operationalMeshDispatchHmacKey == null) {
                byte[] master = decodeMasterKey();
                byte[] derived = hmacSha256(master, "PJB-DILIGENCE-MESH-DISPATCH-SIGN-v1".getBytes(StandardCharsets.UTF_8));
                operationalMeshDispatchHmacKey = new SecretKeySpec(derived, "HmacSHA256");
                zeroize(master);
            }
            return operationalMeshDispatchHmacKey;
        }
    }

    private byte[] decodeMasterKey() {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            throw new IllegalStateException("pjb.security.master-key está vazio (Base64). Em produção, configure via Secret Manager.");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(masterKeyBase64.trim());
            if (decoded.length < 32) {
                throw new IllegalStateException("pjb.security.master-key precisa ter >= 32 bytes (Base64)");
            }
            if (isWeakMasterKey(decoded)) {
                throw new IllegalStateException("pjb.security.master-key insegura: configure uma chave aleatória forte e exclusiva");
            }
            return decoded;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("pjb.security.master-key inválido (não é Base64)", e);
        }
    }

    private static boolean isWeakMasterKey(byte[] decodedKey) {
        if (decodedKey == null || decodedKey.length == 0) {
            return true;
        }
        byte first = decodedKey[0];
        for (byte value : decodedKey) {
            if (value != first) {
                return false;
            }
        }
        return true;
    }

    private static byte[] hmacSha256(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(message);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 indisponível", e);
        }
    }

    private static void zeroize(byte[] buf) {
        if (buf == null) {
            return;
        }
        for (int i = 0; i < buf.length; i++) {
            buf[i] = 0;
        }
        ZEROIZE_GUARD.getAndAdd(buf.length);
    }
}
