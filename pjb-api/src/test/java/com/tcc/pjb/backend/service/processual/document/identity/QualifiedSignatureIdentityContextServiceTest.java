package com.tcc.pjb.backend.service.processual.document.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.Test;

class QualifiedSignatureIdentityContextServiceTest {

    @Test
    void resolveCertificateFingerprintFallsBackToHeaderFingerprintWhenEncodingFails() throws Exception {
        QualifiedSignatureIdentityContextService service = new QualifiedSignatureIdentityContextService();
        X509Certificate certificate = mock(X509Certificate.class);
        doThrow(new CertificateEncodingException("encoding failure")).when(certificate).getEncoded();

        Method method = QualifiedSignatureIdentityContextService.class.getDeclaredMethod(
                "resolveCertificateFingerprint",
                X509Certificate.class,
                String.class
        );
        method.setAccessible(true);

        String resolved = (String) method.invoke(service, certificate, "AA:BB:CC");

        assertThat(resolved).isEqualTo("AABBCC");
    }
}
