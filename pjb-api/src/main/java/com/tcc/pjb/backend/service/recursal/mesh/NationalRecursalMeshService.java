package com.tcc.pjb.backend.service.recursal.mesh;

import java.time.Instant;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.NationalRecursalMeshEngine;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionCommand;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionResult;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshTransitionRequest;
import jakarta.inject.Inject;

@Service
public class NationalRecursalMeshService {

    private final NationalRecursalMeshEngine engine;
    private final RecursalMeshRequestMapper requestMapper;

    @Inject
    public NationalRecursalMeshService() {
        this(new NationalRecursalMeshEngine(), new RecursalMeshRequestMapper());
    }

    public NationalRecursalMeshService(NationalRecursalMeshEngine engine, RecursalMeshRequestMapper requestMapper) {
        this.engine = engine;
        this.requestMapper = requestMapper;
    }

    public RecursalPlanningResult plan(RecursalMeshPlanRequest request) {
        var context = requestMapper.toContext(request.context());
        var species = requestMapper.toSpecies(request.species());
        return engine.plan(context, species, request.recursoId());
    }

    public RecursalTransitionResult transition(RecursalMeshTransitionRequest request) {
        var context = requestMapper.toContext(request.context());
        var species = requestMapper.toSpecies(request.species());
        RecursalStateSnapshot snapshot = request.snapshot() == null
                ? engine.plan(context, species, request.recursoId()).initialSnapshot()
                : request.snapshot();
        RecursalTransitionCommand command = new RecursalTransitionCommand(
                snapshot,
                context,
                species,
                request.event(),
                request.actor(),
                request.occurredAt() == null ? Instant.now() : request.occurredAt(),
                request.details()
        );
        return engine.transition(command);
    }
}
