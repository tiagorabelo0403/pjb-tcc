package com.tcc.pjb.backend.service.advogado.surface;

import com.tcc.pjb.backend.model.dto.advogado.LaianePeticaoInicialEstruturarRequest;
import com.tcc.pjb.backend.model.dto.advogado.LaianePeticaoInicialProtocolarRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftRequestMapper;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class LaianePeticaoInicialDraftSurfaceFacadeService {

    private final LaianePeticaoInicialDraftService service;
    private final SurfaceProjectionSupport projectionSupport;

    public LaianePeticaoInicialDraftSurfaceFacadeService(LaianePeticaoInicialDraftService service,
                                                         SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse estruturar(LaianePeticaoInicialEstruturarRequest request) {
        return projectionSupport.snapshot("laiane-peticao-inicial", service.estruturar(LaianePeticaoInicialDraftRequestMapper.toServiceRequest(request)));
    }

    public SurfaceSnapshotResponse salvar(LaianePeticaoInicialEstruturarRequest request) {
        return projectionSupport.snapshot("laiane-peticao-inicial", service.salvar(LaianePeticaoInicialDraftRequestMapper.toServiceRequest(request)));
    }

    public SurfaceCollectionResponse listarMinhas() {
        return projectionSupport.collection("laiane-peticao-inicial", service.listarMinhas());
    }

    public SurfaceSnapshotResponse detalhar(Long draftId) {
        return projectionSupport.snapshot("laiane-peticao-inicial", service.detalhar(draftId));
    }

    public SurfaceSnapshotResponse protocolar(Long draftId, LaianePeticaoInicialProtocolarRequest request) {
        return projectionSupport.snapshot("laiane-peticao-inicial", service.protocolar(draftId,
                request == null ? null : new LaianePeticaoInicialDraftService.ProtocolarRequest(request.tipoJustica())));
    }

}
