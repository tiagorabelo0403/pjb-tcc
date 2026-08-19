package com.tcc.pjb.backend.model.entity.competencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.repository.ComarcaRepository;
import com.tcc.pjb.backend.model.repository.TribunalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TribunalComarcaRepositoryIT extends PjbIntegrationTestBase {

    @Autowired
    private TribunalRepository tribunalRepository;

    @Autowired
    private ComarcaRepository comarcaRepository;

    @Test
    void seedPopulaAoMenosUmTribunalEUmaComarcaAPartirDoDadoRealJaCarregado() {
        assertThat(tribunalRepository.count()).isGreaterThan(0);
        assertThat(comarcaRepository.count()).isGreaterThan(0);
    }

    @Test
    void findBySiglaResolveTribunalRealDoSeed() {
        assertThat(tribunalRepository.findAll()).isNotEmpty();
        var qualquerTribunal = tribunalRepository.findAll().get(0);
        assertThat(tribunalRepository.findBySigla(qualquerTribunal.getSigla())).isPresent();
    }
}
