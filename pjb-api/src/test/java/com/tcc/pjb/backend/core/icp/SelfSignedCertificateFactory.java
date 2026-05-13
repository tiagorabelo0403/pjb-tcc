package com.tcc.pjb.backend.core.icp;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

final class SelfSignedCertificateFactory {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private SelfSignedCertificateFactory() {
    }

    static X509Certificate generate(String subjectDn, String issuerDn, Instant notBefore, Instant notAfter) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();
        X500Name subject = new X500Name(subjectDn);
        X500Name issuer = new X500Name(issuerDn);
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer,
                new BigInteger(64, new SecureRandom()).abs(),
                Date.from(notBefore),
                Date.from(notAfter),
                subject,
                keyPair.getPublic()
        );
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer));
    }
}
