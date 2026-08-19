package com.tcc.pjb.backend.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.NaturezaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.TipoJurisdicao;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JurisdicaoRepositoryComarcaFallbackIT extends PjbIntegrationTestBase {

    @Autowired
    private JurisdicaoRepository jurisdicaoRepository;

    @Test
    void findByUfEncontraJurisdicaoForaDoCatalogoDeComarcasViaFallbackString() {
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setCodigo("TJSP-SP-COMARCA-FALLBACK-IT");
        jurisdicao.setSigla("TJSPFBK");
        jurisdicao.setNome("1ª Vara Cível de São Paulo - Fallback IT");
        jurisdicao.setTipo(TipoJurisdicao.ESTADUAL);
        jurisdicao.setNatureza(NaturezaJurisdicao.CONTENCIOSA);
        jurisdicao.setGrau(GrauJurisdicao.PRIMEIRO_GRAU);
        jurisdicao.setEsfera(EsferaJurisdicao.JUSTICA_ESTADUAL);
        jurisdicao.setMateria(MateriaJurisdicao.CIVIL);
        jurisdicao.setComarca("São Paulo");
        jurisdicao.setEstado("SP");

        jurisdicaoRepository.saveAndFlush(jurisdicao);

        assertThat(jurisdicao.getComarcaEntidade()).isNull();

        List<Jurisdicao> encontradas = jurisdicaoRepository.findByUf("SP");

        assertThat(encontradas)
                .extracting(Jurisdicao::getCodigo)
                .contains("TJSP-SP-COMARCA-FALLBACK-IT");

        Jurisdicao persistida = encontradas.stream()
                .filter(j -> "TJSP-SP-COMARCA-FALLBACK-IT".equals(j.getCodigo()))
                .findFirst()
                .orElseThrow();

        assertThat(persistida.getComarcaEntidade()).isNull();
        assertThat(persistida.getUf()).isEqualTo("SP");
        assertThat(persistida.getCidade()).isEqualTo("São Paulo");
    }
}
