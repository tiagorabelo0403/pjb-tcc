package com.tcc.pjb.backend.query.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.query.ProcessoQueryRepository;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

class QueryInfrastructureConfigTest {

    @Test
    void declaraScanElasticsearchCondicionadoAoSearchEnabled() {
        ConditionalOnProperty condition = QueryInfrastructureConfig.class.getAnnotation(ConditionalOnProperty.class);
        EnableElasticsearchRepositories repositories =
                QueryInfrastructureConfig.class.getAnnotation(EnableElasticsearchRepositories.class);

        assertThat(condition).isNotNull();
        assertThat(condition.prefix()).isEqualTo("pjb.search");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");

        assertThat(repositories).isNotNull();
        assertThat(repositories.basePackageClasses()).containsExactly(ProcessoQueryRepository.class);
        assertThat(repositories.basePackages()).isEmpty();
        assertThat(repositories.value()).isEmpty();
    }

    @Test
    void raizDeProcessoQueryRepositoryCobreRecursalMeshSemDuplicarRaiz() {
        String raiz = ProcessoQueryRepository.class.getPackageName();

        assertThat(RecursalMeshQueryRepository.class.getPackageName()).startsWith(raiz);
        assertThat(QueryInfrastructureConfig.class
                        .getAnnotation(EnableElasticsearchRepositories.class)
                        .basePackageClasses())
                .containsExactly(ProcessoQueryRepository.class);
    }
}
