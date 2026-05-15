package com.tcc.pjb.backend.core.processo.busca.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.busca.query.ProcessoSearchSpecification.ProcessoSearchCriteria;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class ProcessoSearchSpecificationTest {

    private final ProcessoSearchSpecification specFactory = new ProcessoSearchSpecification();

    @Test
    void nullCriteriaProducesConjunction() {
        Specification<Processo> spec = specFactory.build(null);
        assertThat(spec).isNotNull();
    }

    @Test
    void emptyCriteriaProducesConjunction() {
        ProcessoSearchCriteria criteria = new ProcessoSearchCriteria(
                null, null, null, null, null, null, null, null, null, null, null, null);
        Specification<Processo> spec = specFactory.build(criteria);
        assertThat(spec).isNotNull();
    }

    @Test
    void specificationCompositionWithRepository() {
        ProcessoRepository repository = mock(ProcessoRepository.class);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        ProcessoSearchCriteria criteria = ProcessoSearchCriteria.of(
                null, "CE", "Fortaleza", "TJCE", null, null,
                RamoDireito.CIVEL, StatusProcesso.EM_ANDAMENTO, FaseProcessual.CONHECIMENTO,
                LocalDateTime.now().minusYears(1), LocalDateTime.now(), null);

        Specification<Processo> spec = specFactory.build(criteria);
        List<Processo> result = repository.findAll(spec);

        assertThat(result).isNotNull();
    }

    @Test
    void criteriaOfFactoryMatchesConstructor() {
        LocalDateTime after = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime before = LocalDateTime.of(2025, 1, 1, 0, 0);

        ProcessoSearchCriteria via = ProcessoSearchCriteria.of(
                "0000001-00.2024.8.06.0001", "CE", "Fortaleza", "TJCE",
                "Saúde", "João Silva", RamoDireito.CIVEL, StatusProcesso.EM_ANDAMENTO,
                FaseProcessual.CONHECIMENTO, after, before, null);

        assertThat(via.numero()).isEqualTo("0000001-00.2024.8.06.0001");
        assertThat(via.uf()).isEqualTo("CE");
        assertThat(via.comarca()).isEqualTo("Fortaleza");
        assertThat(via.tribunal()).isEqualTo("TJCE");
        assertThat(via.assunto()).isEqualTo("Saúde");
        assertThat(via.parteNome()).isEqualTo("João Silva");
        assertThat(via.ramoDireito()).isEqualTo(RamoDireito.CIVEL);
        assertThat(via.status()).isEqualTo(StatusProcesso.EM_ANDAMENTO);
        assertThat(via.fase()).isEqualTo(FaseProcessual.CONHECIMENTO);
        assertThat(via.distribuicaoApos()).isEqualTo(after);
        assertThat(via.distribuicaoAntes()).isEqualTo(before);
        assertThat(via.movimentacaoApos()).isNull();
    }
}
