package com.tcc.pjb.backend.core.security.crypto.quantum;

import com.tcc.pjb.backend.core.security.crypto.BouncyCastleProviders;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

public final class PostQuantumSigner {

    private static final String KEY_FACTORY_FAMILY = "ML-DSA";

    private final PqcSignatureAlgorithm algorithm;
    private final KeyPair keyPair;

    public PostQuantumSigner(PqcSignatureAlgorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algoritmo PQC obrigatório");
        }
        this.algorithm = algorithm;
        BouncyCastleProviders.ensureRegistered();
        this.keyPair = generateKeyPair(algorithm);
    }

    public PqcEvidence sign(byte[] payload) {
        try {
            Signature signature = Signature.getInstance(algorithm.jcaName(), BouncyCastleProviders.NAME);
            signature.initSign(keyPair.getPrivate(), new SecureRandom());
            signature.update(payload);
            return new PqcEvidence(
                    algorithm.jcaName(),
                    Base64.getEncoder().encodeToString(signature.sign()),
                    Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
            );
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao assinar com PQC (" + algorithm.jcaName() + ")", e);
        }
    }

    public boolean verify(byte[] payload, PqcEvidence evidence) {
        if (evidence == null) {
            return false;
        }
        Optional<PqcSignatureAlgorithm> declared = PqcSignatureAlgorithm.fromJcaName(evidence.algorithm());
        if (declared.isEmpty()) {
            return false;
        }
        try {
            BouncyCastleProviders.ensureRegistered();
            PublicKey publicKey = KeyFactory.getInstance(KEY_FACTORY_FAMILY, BouncyCastleProviders.NAME)
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(evidence.publicKeyB64())));
            if (!declared.get().jcaName().equals(publicKey.getAlgorithm())) {
                return false;
            }
            Signature signature = Signature.getInstance(declared.get().jcaName(), BouncyCastleProviders.NAME);
            signature.initVerify(publicKey);
            signature.update(payload);
            return signature.verify(Base64.getDecoder().decode(evidence.signatureB64()));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    private static KeyPair generateKeyPair(PqcSignatureAlgorithm algorithm) {
        try {
            return KeyPairGenerator.getInstance(algorithm.jcaName(), BouncyCastleProviders.NAME).generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível gerar chave PQC com algoritmo: " + algorithm.jcaName(), e);
        }
    }
}
