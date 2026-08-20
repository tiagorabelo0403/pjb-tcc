package com.tcc.pjb.backend.query.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.query.ProcessoQueryModel;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryModel;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

class PjbSearchIndexInitializerTest {

    @Test
    void naoRecriaIndiceQuandoJaExiste() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations processoIndex = mock(IndexOperations.class);
        IndexOperations recursalIndex = mock(IndexOperations.class);
        when(operations.indexOps(ProcessoQueryModel.class)).thenReturn(processoIndex);
        when(operations.indexOps(RecursalMeshQueryModel.class)).thenReturn(recursalIndex);
        when(processoIndex.exists()).thenReturn(true);
        when(recursalIndex.exists()).thenReturn(true);

        new PjbSearchIndexInitializer(operations).run(null);

        verify(processoIndex, never()).createWithMapping();
        verify(recursalIndex, never()).createWithMapping();
    }

    @Test
    void criaIndiceQuandoNaoExiste() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations processoIndex = mock(IndexOperations.class);
        IndexOperations recursalIndex = mock(IndexOperations.class);
        when(operations.indexOps(ProcessoQueryModel.class)).thenReturn(processoIndex);
        when(operations.indexOps(RecursalMeshQueryModel.class)).thenReturn(recursalIndex);
        when(processoIndex.exists()).thenReturn(false);
        when(recursalIndex.exists()).thenReturn(false);

        new PjbSearchIndexInitializer(operations).run(null);

        verify(processoIndex).createWithMapping();
        verify(recursalIndex).createWithMapping();
    }

    @Test
    void trataResourceAlreadyExistsComoSucessoIdempotenteQuandoOutroNoVenceARace() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations processoIndex = mock(IndexOperations.class);
        IndexOperations recursalIndex = mock(IndexOperations.class);
        when(operations.indexOps(ProcessoQueryModel.class)).thenReturn(processoIndex);
        when(operations.indexOps(RecursalMeshQueryModel.class)).thenReturn(recursalIndex);
        when(processoIndex.exists()).thenReturn(false);
        when(recursalIndex.exists()).thenReturn(true);
        when(processoIndex.createWithMapping()).thenThrow(new RuntimeException(
                "[es/indices.create] failed: [resource_already_exists_exception] index [pjb-processos] already exists"));

        new PjbSearchIndexInitializer(operations).run(null);

        verify(processoIndex).createWithMapping();
    }

    @Test
    void propagaExcecaoQuandoNaoEUmaColisaoDeIndiceJaExistente() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations processoIndex = mock(IndexOperations.class);
        when(operations.indexOps(ProcessoQueryModel.class)).thenReturn(processoIndex);
        when(processoIndex.exists()).thenReturn(false);
        when(processoIndex.createWithMapping()).thenThrow(new RuntimeException("cluster indisponivel"));

        assertThatThrownBy(() -> new PjbSearchIndexInitializer(operations).run(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("cluster indisponivel");
    }
}
