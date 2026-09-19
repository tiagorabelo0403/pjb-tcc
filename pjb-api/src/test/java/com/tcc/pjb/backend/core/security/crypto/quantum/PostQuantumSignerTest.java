package com.tcc.pjb.backend.core.security.crypto.quantum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PostQuantumSignerTest {

    private static final byte[] SENTENCA = "PJB|SENTENCA|V1\nuuid=9f1\n".getBytes(StandardCharsets.UTF_8);

    @ParameterizedTest
    @EnumSource(PqcSignatureAlgorithm.class)
    void assinaEVerificaEmTodosOsConjuntosDeParametros(PqcSignatureAlgorithm algoritmo) {
        PostQuantumSigner signer = new PostQuantumSigner(algoritmo);

        PqcEvidence evidence = signer.sign(SENTENCA);

        assertThat(evidence.algorithm()).isEqualTo(algoritmo.jcaName());
        assertThat(Base64.getDecoder().decode(evidence.signatureB64())).isNotEmpty();
        assertThat(signer.verify(SENTENCA, evidence)).isTrue();
    }

    @Test
    void payloadAlteradoNaoVerifica() {
        PostQuantumSigner signer = new PostQuantumSigner(PqcSignatureAlgorithm.ML_DSA_87);
        PqcEvidence evidence = signer.sign(SENTENCA);

        byte[] adulterado = "PJB|SENTENCA|V1\nuuid=9f2\n".getBytes(StandardCharsets.UTF_8);

        assertThat(signer.verify(adulterado, evidence)).isFalse();
    }

    @Test
    void assinaturaDeOutraChaveNaoVerifica() {
        PostQuantumSigner signer = new PostQuantumSigner(PqcSignatureAlgorithm.ML_DSA_87);
        PostQuantumSigner outroSigner = new PostQuantumSigner(PqcSignatureAlgorithm.ML_DSA_87);

        PqcEvidence deOutraChave = outroSigner.sign(SENTENCA);
        PqcEvidence propria = signer.sign(SENTENCA);

        assertThat(signer.verify(SENTENCA, new PqcEvidence(
                propria.algorithm(), deOutraChave.signatureB64(), propria.publicKeyB64()))).isFalse();
    }

    @Test
    void algoritmoDesconhecidoNaEvidenciaNaoVerifica() {
        PostQuantumSigner signer = new PostQuantumSigner(PqcSignatureAlgorithm.ML_DSA_87);
        PqcEvidence evidence = signer.sign(SENTENCA);

        assertThat(signer.verify(SENTENCA, new PqcEvidence(
                "DILITHIUM", evidence.signatureB64(), evidence.publicKeyB64()))).isFalse();
    }

    @Test
    void conjuntoDeParametrosDeclaradoPrecisaBaterComAChave() {
        PostQuantumSigner signer = new PostQuantumSigner(PqcSignatureAlgorithm.ML_DSA_87);
        PqcEvidence evidence = signer.sign(SENTENCA);

        assertThat(signer.verify(SENTENCA, new PqcEvidence(
                PqcSignatureAlgorithm.ML_DSA_44.jcaName(), evidence.signatureB64(), evidence.publicKeyB64()))).isFalse();
    }

    @Test
    void evidenciaAusenteOuCorrompidaNaoVerifica() {
        PostQuantumSigner signer = new PostQuantumSigner(PqcSignatureAlgorithm.ML_DSA_87);
        PqcEvidence evidence = signer.sign(SENTENCA);

        assertThat(signer.verify(SENTENCA, null)).isFalse();
        assertThat(signer.verify(SENTENCA, new PqcEvidence(
                evidence.algorithm(), "nao-e-base64-$$", evidence.publicKeyB64()))).isFalse();
    }

    @Test
    void algoritmoNuloNaoConstroiSigner() {
        assertThatThrownBy(() -> new PostQuantumSigner(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
