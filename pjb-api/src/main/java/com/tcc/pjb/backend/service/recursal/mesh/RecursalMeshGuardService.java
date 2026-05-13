package com.tcc.pjb.backend.service.recursal.mesh;

import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalCaseContext;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalConstraintViolationException;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRoutePlan;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;

@Service
public class RecursalMeshGuardService {

    public void validateContext(RecursalCaseContext context) {
        if (context == null) {
            throw new RecursalConstraintViolationException("Contexto recursal ausente");
        }
        if (context.classeProcessual() == null || context.classeProcessual().isBlank()) {
            throw new RecursalConstraintViolationException("Classe processual recursal é obrigatória");
        }
        if (context.tribunalOrigem() == null || context.tribunalDetalhadoOrigem() == null) {
            throw new RecursalConstraintViolationException("Tribunal de origem recursal é obrigatório");
        }
        if (context.tribunalDetalhadoOrigem().tribunal() != context.tribunalOrigem()) {
            throw new RecursalConstraintViolationException("Tribunal detalhado de origem incompatível com o tribunal de origem");
        }
        if (context.instanciaAtual() == null || context.autoridadeAtual() == null) {
            throw new RecursalConstraintViolationException("Instância e autoridade recursais são obrigatórias");
        }
    }

    public void validateSpecies(RecursalSpecies species) {
        if (species == null) {
            throw new RecursalConstraintViolationException("Espécie recursal ausente");
        }
        if (species instanceof EmbargosDeclaracao embargos && embargos.grounds().isEmpty()) {
            throw new RecursalConstraintViolationException("Embargos de declaração exigem ao menos um fundamento material");
        }
    }

    public void validateAggregate(RecursalAggregateState aggregate, RecursalStateSnapshot snapshot, RecursalRoutePlan routePlan) {
        if (aggregate.getRecursoId() == null || aggregate.getRecursoId().isBlank()) {
            throw new RecursalConstraintViolationException("Agregado recursal sem identificador");
        }
        if (!aggregate.getRecursoId().equals(snapshot.recursoId())) {
            throw new RecursalConstraintViolationException("Recurso persistido divergente do snapshot recursal");
        }
        if (aggregate.getCurrentState() != snapshot.state()) {
            throw new RecursalConstraintViolationException("Estado persistido divergente do snapshot recursal");
        }
        if (aggregate.getTribunalAtual() != snapshot.tribunalAtual()) {
            throw new RecursalConstraintViolationException("Tribunal persistido divergente do snapshot recursal");
        }
        if (aggregate.getTribunalDetalhadoAtual() != snapshot.tribunalDetalhadoAtual()) {
            throw new RecursalConstraintViolationException("Tribunal detalhado persistido divergente do snapshot recursal");
        }
        if (routePlan.tribunalDetalhadoOrigem().tribunal() != routePlan.tribunalOrigem()) {
            throw new RecursalConstraintViolationException("Plano recursal com tribunal detalhado de origem inconsistente");
        }
        if (routePlan.tribunalDetalhadoDestino().tribunal() != routePlan.tribunalDestino()) {
            throw new RecursalConstraintViolationException("Plano recursal com tribunal detalhado de destino inconsistente");
        }
    }
}
