package com.tcc.pjb.backend.service.desembargador.surface;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.desembargador.DesembargadorColegialdoPainelService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class DesembargadorColegialSurfaceFacadeService {

    private final DesembargadorColegialdoPainelService service;
    private final SurfaceProjectionSupport projectionSupport;

    public DesembargadorColegialSurfaceFacadeService(DesembargadorColegialdoPainelService service,
                                                     SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse snapshot() {
        return projectionSupport.snapshot("desembargador.colegiado", service.bootstrapPainel());
    }

    public SurfaceSnapshotResponse governanca() {
        return projectionSupport.snapshot("desembargador.colegiado.governanca", service.governancaCamara());
    }

    public SurfaceSnapshotResponse malhaProcesso(Long processoId) {
        return projectionSupport.snapshot("desembargador.colegiado.malha", service.malhaProcesso(processoId));
    }

    public SurfaceActionResponse proferirVoto(Long processoId, String voto, String fundamentacao, String decisao) {
        return projectionSupport.action("desembargador.colegiado", "proferirVoto", processoId, service.proferirVoto(processoId, voto, fundamentacao, decisao));
    }

    public SurfaceActionResponse lavrarAcordao(Long processoId, String ementa, String dispositivo, String fundamentacao) {
        return projectionSupport.action("desembargador.colegiado", "lavrarAcordao", processoId, service.lavrarAcordao(processoId, ementa, dispositivo, fundamentacao));
    }

    public SurfaceActionResponse pedirVista(Long processoId, int diasVista) {
        return projectionSupport.action("desembargador.colegiado", "pedirVista", processoId, service.pedirVista(processoId, diasVista));
    }

    public SurfaceActionResponse gerenciarDestaque(Long processoId, String motivo) {
        return projectionSupport.action("desembargador.colegiado", "gerenciarDestaque", processoId, service.registrarDestaque(processoId, motivo));
    }

    public SurfaceActionResponse deferirSustentacao(Long processoId, boolean deferido, String observacao) {
        return projectionSupport.action("desembargador.colegiado", "deferirSustentacao", processoId, service.gerenciarSustentacaoOral(processoId, deferido, observacao));
    }

    public SurfaceActionResponse abrirSessao(String sessaoId, String pauta) {
        return projectionSupport.action("desembargador.colegiado", "abrirSessao", null, service.abrirSessao(sessaoId, pauta));
    }

    public SurfaceActionResponse fecharSessao(String sessaoId, String pauta) {
        return projectionSupport.action("desembargador.colegiado", "fecharSessao", null, service.fecharSessao(sessaoId, pauta));
    }

    public SurfaceActionResponse registrarImpedimento(Long processoId, String tipo, String fundamento) {
        return projectionSupport.action("desembargador.colegiado", "registrarImpedimento", processoId, service.registrarImpedimentoOuSuspeicao(processoId, tipo, fundamento));
    }
}
