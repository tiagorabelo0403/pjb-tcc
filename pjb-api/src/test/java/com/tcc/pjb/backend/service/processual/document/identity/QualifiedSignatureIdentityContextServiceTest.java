package com.tcc.pjb.backend.service.processual.document.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
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

    @Test
    void resolveSegmentoInstitucionalReconhecesEscrivaoPolicialComoPoliciaJudiciaria() throws Exception {
        String segmento = invokeResolveSegmentoInstitucional(TipoUsuario.ESCRIVAO_POLICIAL, "ESCRIVAO_POLICIAL", "ESCRIVAO_POLICIAL");

        assertThat(segmento).isEqualTo("POLICIA_JUDICIARIA");
    }

    @Test
    void resolveSegmentoInstitucionalReconheceOficialJusticaIndependenteDoPapelReivindicado() throws Exception {
        String segmentoOficial = invokeResolveSegmentoInstitucional(TipoUsuario.OFICIAL_JUSTICA, "ADVOGADO", "ADVOGADO");
        String segmentoAvaliador = invokeResolveSegmentoInstitucional(TipoUsuario.OFICIAL_JUSTICA_AVALIADOR, "ADVOGADO", "ADVOGADO");

        assertThat(segmentoOficial).isEqualTo("OFICIAL_JUSTICA");
        assertThat(segmentoAvaliador).isEqualTo("OFICIAL_JUSTICA");
    }

    @Test
    void resolveSegmentoInstitucionalNaoAcompanhaMaisPapelReivindicadoForaDoDominio() throws Exception {
        String segmentoUnidadeJudicial = invokeResolveSegmentoInstitucional(TipoUsuario.CIDADAO, "UNIDADE_JUDICIAL", "UNIDADE_JUDICIAL");
        String segmentoOficialJustica = invokeResolveSegmentoInstitucional(TipoUsuario.CIDADAO, "OFICIAL_JUSTICA", "OFICIAL_JUSTICA");

        assertThat(segmentoUnidadeJudicial).isEqualTo("ASSINANTE_INSTITUCIONAL");
        assertThat(segmentoOficialJustica).isEqualTo("ASSINANTE_INSTITUCIONAL");
    }

    private String invokeResolveSegmentoInstitucional(TipoUsuario tipoUsuario, String papelBase, String papelDetalhado) throws Exception {
        QualifiedSignatureIdentityContextService service = new QualifiedSignatureIdentityContextService();
        Method method = QualifiedSignatureIdentityContextService.class.getDeclaredMethod(
                "resolveSegmentoInstitucional",
                TipoUsuario.class,
                String.class,
                String.class
        );
        method.setAccessible(true);
        return (String) method.invoke(service, tipoUsuario, papelBase, papelDetalhado);
    }
}
