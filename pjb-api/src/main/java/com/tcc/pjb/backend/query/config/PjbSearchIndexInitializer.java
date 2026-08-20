package com.tcc.pjb.backend.query.config;

import com.tcc.pjb.backend.query.ProcessoQueryModel;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * Cria os indices de busca sob demanda, no lugar da autocriacao do Spring Data Elasticsearch
 * (desligada via {@code @Document(createIndex = false)} em {@link ProcessoQueryModel} e
 * {@link RecursalMeshQueryModel}). A autocriacao roda dentro do construtor do repositorio, sem
 * checar existencia previamente — quando dois nos da topologia HA sobem ao mesmo tempo contra um
 * Elasticsearch vazio, o segundo a chegar recebe {@code resource_already_exists_exception} e o
 * boot do Spring context inteiro falha. Aqui a checagem de existencia acontece antes da criacao, e
 * o resultado "outro no ja criou" e tratado como sucesso idempotente em vez de erro.
 */
@Component
@ConditionalOnProperty(prefix = "pjb.search", name = "enabled", havingValue = "true")
public class PjbSearchIndexInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PjbSearchIndexInitializer.class);

    private final ElasticsearchOperations operations;

    public PjbSearchIndexInitializer(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureIndexExists(ProcessoQueryModel.class);
        ensureIndexExists(RecursalMeshQueryModel.class);
    }

    private void ensureIndexExists(Class<?> entityClass) {
        IndexOperations indexOperations = operations.indexOps(entityClass);
        if (indexOperations.exists()) {
            return;
        }
        try {
            indexOperations.createWithMapping();
        } catch (RuntimeException ex) {
            if (!isResourceAlreadyExists(ex)) {
                throw ex;
            }
            LOG.info("Indice de {} ja foi criado por outro no da topologia entre a checagem e a criacao — seguindo normalmente.",
                    entityClass.getSimpleName());
        }
    }

    private boolean isResourceAlreadyExists(RuntimeException ex) {
        String message = ex.getMessage();
        if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("resource_already_exists_exception")) {
            return true;
        }
        Throwable cause = ex.getCause();
        return cause != null && cause.getMessage() != null
                && cause.getMessage().toLowerCase(java.util.Locale.ROOT).contains("resource_already_exists_exception");
    }
}
