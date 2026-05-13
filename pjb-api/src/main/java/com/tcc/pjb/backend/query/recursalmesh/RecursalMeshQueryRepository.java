package com.tcc.pjb.backend.query.recursalmesh;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "pjb.search", name = "enabled", havingValue = "true")
public interface RecursalMeshQueryRepository extends ElasticsearchRepository<RecursalMeshQueryModel, String> {
}
