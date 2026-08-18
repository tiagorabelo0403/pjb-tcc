package com.tcc.pjb.backend.service.criminal.surface;

import com.tcc.pjb.backend.model.dto.criminal.BoletimOcorrenciaCadastroRequest;
import com.tcc.pjb.backend.model.dto.criminal.BoletimOcorrenciaVinculoInqueritoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.criminal.BoletimOcorrenciaDigitalService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BoletimOcorrenciaDigitalSurfaceFacadeService {

    private final BoletimOcorrenciaDigitalService boletimService;
    private final SurfaceProjectionSupport surfaceProjectionSupport;

    public BoletimOcorrenciaDigitalSurfaceFacadeService(BoletimOcorrenciaDigitalService boletimService,
                                                        SurfaceProjectionSupport surfaceProjectionSupport) {
        this.boletimService = Objects.requireNonNull(boletimService);
        this.surfaceProjectionSupport = Objects.requireNonNull(surfaceProjectionSupport);
    }

    public SurfaceCollectionResponse listarMeus() {
        return surfaceProjectionSupport.collection("criminal.boletim-ocorrencia.meus", boletimService.listarMeus());
    }

    public SurfaceSnapshotResponse buscar(UUID uuid) {
        return surfaceProjectionSupport.snapshot("criminal.boletim-ocorrencia.buscar", boletimService.buscar(uuid));
    }

    public SurfaceSnapshotResponse registrar(BoletimOcorrenciaCadastroRequest request) {
        return surfaceProjectionSupport.snapshot(
                "criminal.boletim-ocorrencia.registrar",
                boletimService.registrar(new BoletimOcorrenciaDigitalService.BoletimOcorrenciaCadastroCommand(
                        request.unidadeRegistroId(),
                        request.naturezaFato(),
                        request.resumoFatos(),
                        request.localFato(),
                        request.ocorridoEm(),
                        request.comunicanteResumo(),
                        request.envolvidosResumo(),
                        request.providenciasIniciais()
                ))
        );
    }

    public SurfaceSnapshotResponse vincularInquerito(UUID uuid, BoletimOcorrenciaVinculoInqueritoRequest request) {
        return surfaceProjectionSupport.snapshot("criminal.boletim-ocorrencia.vincular-inquerito", boletimService.vincularInquerito(uuid, request.inqueritoId()));
    }
}
