package com.tcc.pjb.backend.service.leilao.surface;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.leilao.LeiloeiroJudicialPainelService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LeiloeiroPainelSurfaceFacadeService {

    private final LeiloeiroJudicialPainelService service;
    private final SurfaceProjectionSupport projectionSupport;

    public LeiloeiroPainelSurfaceFacadeService(LeiloeiroJudicialPainelService service,
                                               SurfaceProjectionSupport projectionSupport) {
        this.service = Objects.requireNonNull(service);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public SurfaceCollectionResponse pendentes() {
        return projectionSupport.collection("leiloeiro.pendentes", service.listarLeiloesPendentes());
    }

    public SurfaceCollectionResponse editaisPendentes() {
        return projectionSupport.collection("leiloeiro.editais-pendentes", service.listarEditaisPendentes());
    }

    public SurfaceCollectionResponse prestacoesContas() {
        return projectionSupport.collection("leiloeiro.prestacoes-contas", service.listarPrestacoesContas());
    }

    public SurfaceSnapshotResponse resumoOperacional() {
        return projectionSupport.snapshot("leiloeiro.resumo-operacional", service.resumoOperacional());
    }
}
