package com.tcc.pjb.backend.model.repository.julgamento;

import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.julgamento.DecisionStepUpConsumption;

public interface DecisionStepUpConsumptionRepository extends JpaRepository<DecisionStepUpConsumption, Long> {

    boolean existsByTokenJti(String tokenJti);
}
