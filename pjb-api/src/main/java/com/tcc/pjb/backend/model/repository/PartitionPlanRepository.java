package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.infra.PartitionPlan;

@Repository
public interface PartitionPlanRepository extends JpaRepository<PartitionPlan, Long> {

    Optional<PartitionPlan> findByTableNameIgnoreCase(String tableName);
}
