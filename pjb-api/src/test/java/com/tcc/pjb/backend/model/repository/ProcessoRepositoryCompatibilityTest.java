package com.tcc.pjb.backend.model.repository;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

class ProcessoRepositoryCompatibilityTest {

    @Test
    void shouldDelegateLegacyNumeroAliasToCanonicalQuery() {
        ProcessoRepository repository = Mockito.mock(ProcessoRepository.class, Mockito.withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
        LinkedHashSet<String> numeros = new LinkedHashSet<>(List.of("0001", "0002"));
        List<com.tcc.pjb.backend.model.entity.Processo> expected = List.of();

        doReturn(expected).when(repository).findByNumeroUnificadoInOrNumeroProcessoIn(numeros, numeros);

        assertSame(expected, repository.findAllByNumeroUnificadoInOrNumeroProcessoIn(numeros, numeros));
        verify(repository).findByNumeroUnificadoInOrNumeroProcessoIn(numeros, numeros);
    }
}
