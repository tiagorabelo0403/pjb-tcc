package com.tcc.pjb.backend.service.admin.surface;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.rito.RitoResolutionService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AdminRitoDiagnosticoSurfaceService {

    private final ProcessoRepository processoRepository;
    private final RitoResolutionService ritoResolutionService;
    private final SurfaceProjectionSupport projectionSupport;

    public AdminRitoDiagnosticoSurfaceService(ProcessoRepository processoRepository,
                                              RitoResolutionService ritoResolutionService,
                                              SurfaceProjectionSupport projectionSupport) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.ritoResolutionService = Objects.requireNonNull(ritoResolutionService);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public Optional<SurfaceSnapshotResponse> ritoDiagnostico(Long processoId) {
        Processo processo = processoRepository.findById(processoId).orElse(null);
        if (processo == null) {
            return Optional.empty();
        }
        var detail = ritoResolutionService.resolveDetailed(processo, null);
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("processoId", processo.getId());
        body.put("numero", processo.getNumeroUnificado());
        body.put("materia", processo.getMateria() != null ? processo.getMateria().name() : null);
        body.put("classeProcessual", processo.getClasseProcessual());
        body.put("assunto", processo.getAssunto());
        body.put("ritoDb", processo.getRito() != null ? processo.getRito().name() : null);
        body.put("ritoResolved", detail.resolution().rito() != null ? detail.resolution().rito().name() : null);
        body.put("ritoTitle", detail.resolution().ritoTitle());
        body.put("ramoSugerido", detail.resolution().ramoSugerido());
        body.put("confidence", detail.resolution().confidence());
        body.put("reasons", detail.resolution().reasons());
        body.put("status", detail.status());
        body.put("blocking", detail.blocking());
        body.put("canonicalContext", detail.canonicalContext() != null ? detail.canonicalContext().toMap() : Map.of());
        body.put("metadata", detail.metadata());
        return Optional.of(projectionSupport.snapshot("admin.ritos.diagnostico", body));
    }
}
