package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import org.junit.jupiter.api.Test;

class MarketplaceRepresentacaoResolverTest {

    private final MarketplaceRepresentacaoResolver resolver =
            new MarketplaceRepresentacaoResolver(new RepresentacaoProcessualPolicyService());

    @Test
    void semPerfilAtorCaiNoDefaultDeMandatoAdJudicia() {
        InstrumentoRepresentacaoProcessual resolvido = resolver.resolve(
                RamoDireito.TRABALHISTA, RitoProcessual.TRABALHISTA_ORDINARIO, "TRT7", null);

        assertThat(resolvido).isEqualTo(InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA);
    }

    @Test
    void perfilCidadaoEmRitoTrabalhistaResolveJusPostulandi() {
        InstrumentoRepresentacaoProcessual resolvido = resolver.resolve(
                RamoDireito.TRABALHISTA, RitoProcessual.TRABALHISTA_ORDINARIO, "TRT7", "CIDADAO");

        assertThat(resolvido).isEqualTo(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA);
    }

    @Test
    void perfilCidadaoEmJuizadoEspecialCivelResolveJusPostulandiDoJuizado() {
        InstrumentoRepresentacaoProcessual resolvido = resolver.resolve(
                RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL, "TJCE", "CIDADAO");

        assertThat(resolvido).isEqualTo(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO);
    }

    @Test
    void perfilAtorInvalidoEhTratadoComoAusente() {
        InstrumentoRepresentacaoProcessual resolvido = resolver.resolve(
                RamoDireito.CIVIL, RitoProcessual.COMUM_ORDINARIO, "TJCE", "valor-desconhecido-invalido");

        assertThat(resolvido).isEqualTo(InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA);
    }
}
