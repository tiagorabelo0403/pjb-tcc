package com.tcc.pjb.backend.core.security.crypto.quantum;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.modules.laiane.entity.LaianeSentencaDraft;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QuantumDecisionSignerServiceTest {

    private static LaianeSentencaDraft minuta() {
        return LaianeSentencaDraft.builder()
                .id(9L)
                .inputHash("a".repeat(64))
                .draftMarkdown("Julgo procedente o pedido.")
                .contextJson("{}")
                .build();
    }

    private static QuantumDecisionSignerService servicoCom(boolean habilitado) {
        return new QuantumDecisionSignerService(
                new PjbQuantumProperties(habilitado, PqcSignatureAlgorithm.ML_DSA_87));
    }

    @Test
    void desligadoNaoAssinaENaoImpedeAPublicacao() {
        assertThat(servicoCom(false).signAndAttachEvidenceOrThrowIfEnabled(minuta())).isEmpty();
    }

    @Test
    void minutaAusenteNaoAssina() {
        assertThat(servicoCom(true).signAndAttachEvidenceOrThrowIfEnabled(null)).isEmpty();
    }

    @Test
    void ligadoProduzEvidenciaVerificavelSobreOConteudoDaMinuta() {
        LaianeSentencaDraft minuta = minuta();

        Optional<PqcEvidence> evidencia = servicoCom(true).signAndAttachEvidenceOrThrowIfEnabled(minuta);

        assertThat(evidencia).isPresent();
        assertThat(evidencia.get().algorithm()).isEqualTo(PqcSignatureAlgorithm.ML_DSA_87.jcaName());
        assertThat(Base64.getDecoder().decode(evidencia.get().signatureB64())).isNotEmpty();
        assertThat(new String(Base64.getDecoder().decode(evidencia.get().publicKeyB64()), StandardCharsets.ISO_8859_1))
                .isNotEmpty();
    }

    @Test
    void cadaPublicacaoUsaUmaChaveEfemeraPropria() {
        QuantumDecisionSignerService service = servicoCom(true);

        PqcEvidence primeira = service.signAndAttachEvidenceOrThrowIfEnabled(minuta()).orElseThrow();
        PqcEvidence segunda = service.signAndAttachEvidenceOrThrowIfEnabled(minuta()).orElseThrow();

        assertThat(primeira.publicKeyB64()).isNotEqualTo(segunda.publicKeyB64());
    }
}
