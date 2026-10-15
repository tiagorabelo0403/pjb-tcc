package com.tcc.pjb.backend.core.quality.apisurface;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

class PjbControllerNaoDevolveEntidadeJpaSondasTest {

    @Test
    void entidadeDentroDePaginaEhAcusada() {
        assertThat(PjbControllerNaoDevolveEntidadeJpaTest.entidadeNaResposta(retornoDe("pagina"))).contains("Precedente");
    }

    @Test
    void entidadeEmListaDentroDeResponseEntityEhAcusada() {
        assertThat(PjbControllerNaoDevolveEntidadeJpaTest.entidadeNaResposta(retornoDe("listaEmResponseEntity"))).contains("Precedente");
    }

    @Test
    void entidadeEmOptionalOuArrayEhAcusada() {
        assertThat(PjbControllerNaoDevolveEntidadeJpaTest.entidadeNaResposta(retornoDe("opcional"))).contains("Precedente");
        assertThat(PjbControllerNaoDevolveEntidadeJpaTest.entidadeNaResposta(retornoDe("array"))).contains("Precedente");
    }

    @Test
    void entidadeNumCampoDeDtoEhAcusadaComOCaminho() {
        assertThat(PjbControllerNaoDevolveEntidadeJpaTest.entidadeNaResposta(retornoDe("envelope")))
                .contains("EnvelopeComEntidade.precedente:Precedente");
    }

    @Test
    void entidadeEmDtoAninhadoDentroDeColecaoEhAcusada() {
        assertThat(PjbControllerNaoDevolveEntidadeJpaTest.entidadeNaResposta(retornoDe("aninhado")))
                .contains("EnvelopeAninhado.envelopes:EnvelopeComEntidade.precedente:Precedente");
    }

    @Test
    void dtoSemEntidadeNaoEhAcusado() {
        assertThat(PjbControllerNaoDevolveEntidadeJpaTest.entidadeNaResposta(retornoDe("semEntidade"))).isEmpty();
    }

    @Test
    void dtoQueReferenciaASiMesmoTerminaSemAcusar() {
        assertThat(PjbControllerNaoDevolveEntidadeJpaTest.entidadeNaResposta(retornoDe("arvore"))).isEmpty();
    }

    private static Type retornoDe(String metodo) {
        try {
            return Respostas.class.getDeclaredMethod(metodo).getGenericReturnType();
        } catch (NoSuchMethodException ausente) {
            throw new IllegalStateException(ausente);
        }
    }

    record EnvelopeComEntidade(String id, Precedente precedente) {
    }

    record EnvelopeAninhado(List<EnvelopeComEntidade> envelopes) {
    }

    record SemEntidade(String titulo, List<String> tags) {
    }

    record No(String rotulo, List<No> filhos) {
    }

    interface Respostas {
        Page<Precedente> pagina();

        ResponseEntity<List<Precedente>> listaEmResponseEntity();

        Optional<Precedente> opcional();

        Precedente[] array();

        ResponseEntity<EnvelopeComEntidade> envelope();

        EnvelopeAninhado aninhado();

        ResponseEntity<List<SemEntidade>> semEntidade();

        No arvore();
    }
}
