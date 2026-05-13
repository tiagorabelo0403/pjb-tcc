package com.tcc.pjb.backend.service.audiencia.surface;

import com.tcc.pjb.backend.model.dto.audiencia.AudienciaWebRtcBiometriaRequest;
import com.tcc.pjb.backend.model.dto.audiencia.AudienciaWebRtcEncerrarRequest;
import com.tcc.pjb.backend.model.dto.audiencia.AudienciaWebRtcOfferRequest;
import com.tcc.pjb.backend.model.dto.audiencia.AudienciaWebRtcSessaoRequest;
import com.tcc.pjb.backend.model.dto.audiencia.AudienciaWebRtcTranscricaoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.audiencia.AudienciaWebRtcService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class AudienciaWebRtcSurfaceFacadeService {

    private final AudienciaWebRtcService service;
    private final SurfaceProjectionSupport projectionSupport;

    public AudienciaWebRtcSurfaceFacadeService(AudienciaWebRtcService service,
                                               SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceCollectionResponse listar() {
        return projectionSupport.collection("audiencia-webrtc", service.listarMinhasSessoes());
    }

    public SurfaceSnapshotResponse detalhar(String sessaoToken) {
        return projectionSupport.snapshot("audiencia-webrtc", service.detalharSessao(sessaoToken));
    }

    public SurfaceSnapshotResponse abrir(AudienciaWebRtcSessaoRequest request) {
        return projectionSupport.snapshot("audiencia-webrtc", service.abrirSessao(new AudienciaWebRtcService.SessaoWebRtcRequest(
                request.audienciaId(),
                request.processoId(),
                request.identificadorParticipante(),
                request.exigirBiometria()
        )));
    }

    public SurfaceSnapshotResponse offer(AudienciaWebRtcOfferRequest request) {
        return projectionSupport.snapshot("audiencia-webrtc", service.registrarOffer(new AudienciaWebRtcService.SinalizacaoWebRtcRequest(
                request.audienciaId(),
                request.sessaoToken(),
                request.sdpOffer()
        )));
    }

    public SurfaceSnapshotResponse transcrever(AudienciaWebRtcTranscricaoRequest request) {
        return projectionSupport.snapshot("audiencia-webrtc", service.registrarTranscricao(new AudienciaWebRtcService.TranscricaoWebRtcRequest(
                request.sessaoToken(),
                request.trecho(),
                request.sequencia(),
                request.parcial()
        )));
    }

    public SurfaceSnapshotResponse biometria(AudienciaWebRtcBiometriaRequest request) {
        return projectionSupport.snapshot("audiencia-webrtc", service.registrarBiometria(new AudienciaWebRtcService.BiometriaWebRtcRequest(
                request.sessaoToken(),
                request.referenciaHash(),
                request.similaridade(),
                request.dispositivoId()
        )));
    }

    public SurfaceSnapshotResponse encerrar(AudienciaWebRtcEncerrarRequest request) {
        return projectionSupport.snapshot("audiencia-webrtc", service.encerrarSessao(new AudienciaWebRtcService.EncerrarSessaoRequest(
                request.sessaoToken(),
                request.gravacaoHash(),
                request.metricasResumo(),
                request.gravarTranscricaoFinal()
        )));
    }
}
