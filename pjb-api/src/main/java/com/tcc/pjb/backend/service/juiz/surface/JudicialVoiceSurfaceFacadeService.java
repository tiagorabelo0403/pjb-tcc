package com.tcc.pjb.backend.service.juiz.surface;

import com.tcc.pjb.backend.model.dto.juiz.JudicialVoiceDraftRequest;
import com.tcc.pjb.backend.model.dto.juiz.JudicialVoiceSessionChunkRequest;
import com.tcc.pjb.backend.model.dto.juiz.JudicialVoiceSessionOpenRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.juiz.JudicialVoiceService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class JudicialVoiceSurfaceFacadeService {

    private final JudicialVoiceService service;
    private final SurfaceProjectionSupport projectionSupport;

    public JudicialVoiceSurfaceFacadeService(JudicialVoiceService service,
                                             SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceCollectionResponse listar() {
        return projectionSupport.collection("judicial-voice", service.listarSessoesRecentes());
    }

    public SurfaceSnapshotResponse detalhar(Long sessaoId) {
        return projectionSupport.snapshot("judicial-voice", service.detalharSessao(sessaoId));
    }

    public SurfaceSnapshotResponse estruturar(JudicialVoiceDraftRequest request) {
        return projectionSupport.snapshot("judicial-voice", service.estruturar(new JudicialVoiceService.VoiceDraftRequest(
                request.processoId(),
                request.modoDocumento(),
                request.transcricaoBruta()
        )));
    }

    public SurfaceSnapshotResponse abrirSessao(JudicialVoiceSessionOpenRequest request) {
        return projectionSupport.snapshot("judicial-voice", service.abrirSessao(new JudicialVoiceService.VoiceSessionOpenRequest(
                request.processoId(),
                request.modoDocumento(),
                request.primeiraCaptura()
        )));
    }

    public SurfaceSnapshotResponse adicionarTrecho(Long sessaoId, JudicialVoiceSessionChunkRequest request) {
        return projectionSupport.snapshot("judicial-voice", service.adicionarTrecho(sessaoId, new JudicialVoiceService.VoiceSessionChunkRequest(
                request.trecho(),
                request.parcial()
        )));
    }

    public SurfaceSnapshotResponse finalizar(Long sessaoId) {
        return projectionSupport.snapshot("judicial-voice", service.finalizarSessao(sessaoId));
    }
}
