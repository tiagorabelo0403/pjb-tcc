package com.tcc.pjb.backend.service.gigs;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GigsVisibilityPolicyTest {

    private final GigsVisibilityPolicy policy = newPolicy();

    private static GigsVisibilityPolicy newPolicy() {
        try {
            var c = GigsVisibilityPolicy.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void avaliar_visivel_quandoSemSigilo() {
        var result = policy.avaliar(GigsActivityType.ANALISAR, "srv-001", "srv-002", "SERVIDOR", false);
        assertThat(result.visivel()).isTrue();
    }

    @Test
    void avaliar_invisivel_quandoSigilosoEExterno() {
        var result = policy.avaliar(GigsActivityType.ANALISAR, "srv-001", "adv-001", "ADVOGADO", true);
        assertThat(result.visivel()).isFalse();
        assertThat(result.editavel()).isFalse();
    }

    @Test
    void avaliar_editavel_quandoResponsavelEAtoNaoJurisdicional() {
        var result = policy.avaliar(GigsActivityType.EXPEDIR, "srv-001", "srv-001", "SERVIDOR", false);
        assertThat(result.visivel()).isTrue();
        assertThat(result.editavel()).isTrue();
    }

    @Test
    void avaliar_naoEditavel_quandoAtoJurisdicional() {
        var result = policy.avaliar(GigsActivityType.MINUTAR, "srv-001", "srv-001", "SERVIDOR", false);
        assertThat(result.visivel()).isTrue();
        assertThat(result.editavel()).isFalse();
    }
}
