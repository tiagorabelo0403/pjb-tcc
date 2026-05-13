package com.tcc.pjb.backend.service.transito.surface;

import com.tcc.pjb.backend.core.transito.TransitoJulgadoArquivamentoEngine;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class TransitoJulgadoSurfaceFacadeService {

    private final TransitoJulgadoArquivamentoEngine engine;
    private final SurfaceProjectionSupport projectionSupport;

    public TransitoJulgadoSurfaceFacadeService(TransitoJulgadoArquivamentoEngine engine,
                                               SurfaceProjectionSupport projectionSupport) {
        this.engine = engine;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceActionResponse certificar(Long processoId, String fundamentacao) {
        return projectionSupport.action("transito.julgado", "certificar", processoId, engine.certificarTransitoEmJulgado(processoId, fundamentacao));
    }

    public SurfaceActionResponse iniciarCumprimento(Long processoId, String tipoCumprimento, double valorExequendo) {
        return projectionSupport.action("transito.julgado", "iniciarCumprimento", processoId, engine.iniciarCumprimentoSentenca(processoId, tipoCumprimento, valorExequendo));
    }

    public SurfaceActionResponse instaurarIncidente(Long processoId, String incidente, String fundamentacao, double valorGarantia) {
        return projectionSupport.action("transito.julgado", "instaurarIncidente", processoId, engine.instaurarIncidenteExecutivo(processoId, incidente, fundamentacao, valorGarantia));
    }

    public SurfaceActionResponse praticarAtoExecutivo(Long processoId, String ato, String detalhe, double valorOperacao) {
        return projectionSupport.action("transito.julgado", "praticarAtoExecutivo", processoId, engine.praticarAtoExecutivo(processoId, ato, detalhe, valorOperacao));
    }

    public SurfaceActionResponse praticarConstricao(Long processoId, String ato, String bem, String detalhe, String convenio, double valorOperacao) {
        return projectionSupport.action("transito.julgado", "praticarConstricaoPatrimonial", processoId,
                engine.praticarConstricaoPatrimonial(processoId, ato, bem, detalhe, convenio, valorOperacao));
    }

    public SurfaceActionResponse integrarConstricao(Long processoId, String ato, String bem, String convenio, String referenciaExterna, double valorOperacao) {
        return projectionSupport.action("transito.julgado", "integrarConstricao", processoId,
                engine.integrarConstricaoExterna(processoId, ato, bem, convenio, referenciaExterna, valorOperacao));
    }

    public SurfaceActionResponse governarExpropriacao(Long processoId, String ato, String bem, String modalidade, double valorReferencia) {
        return projectionSupport.action("transito.julgado", "governarExpropriacao", processoId,
                engine.governarExpropriacao(processoId, ato, bem, modalidade, valorReferencia));
    }

    public SurfaceActionResponse reconciliarConstricao(Long processoId, String bem, String convenio, String statusExterno, String referenciaExterna, double valorOperacao) {
        return projectionSupport.action("transito.julgado", "reconciliarConstricao", processoId,
                engine.reconciliarConstricaoExterna(processoId, bem, convenio, statusExterno, referenciaExterna, valorOperacao));
    }

    public SurfaceActionResponse planejarCicloLeilao(Long processoId, String ato, String bem, String modalidade, Integer tentativa, double valorReferencia) {
        return projectionSupport.action("transito.julgado", "planejarCicloLeilao", processoId,
                engine.planejarCicloLeilaoExpropriatorio(processoId, ato, bem, modalidade, tentativa, valorReferencia));
    }

    public SurfaceActionResponse deflagrarContingencia(Long processoId, String bem, String convenio, String statusExterno, String referenciaExterna, double valorOperacao) {
        return projectionSupport.action("transito.julgado", "deflagrarContingenciaConstricao", processoId,
                engine.deflagrarContingenciaConstricaoExterna(processoId, bem, convenio, statusExterno, referenciaExterna, valorOperacao));
    }

    public SurfaceActionResponse homologarExpropriacao(Long processoId, String ato, String bem, String modalidade, String adquirente, double valorArrematacao) {
        return projectionSupport.action("transito.julgado", "homologarExpropriacao", processoId,
                engine.homologarExpropriacaoFinal(processoId, ato, bem, modalidade, adquirente, valorArrematacao));
    }

    public SurfaceActionResponse liquidarProdutoExpropriacao(Long processoId, String bem, String modoProduto, String preferencia, String subrogacao, double valorProduto, double saldoExecutado, double saldoCredor) {
        return projectionSupport.action("transito.julgado", "liquidarProdutoExpropriacao", processoId,
                engine.liquidarProdutoExpropriacao(processoId, bem, modoProduto, preferencia, subrogacao, valorProduto, saldoExecutado, saldoCredor));
    }

    public SurfaceActionResponse consolidarFechamentoExecutivo(Long processoId, String modoFechamento, String preferencia, String subrogacao, double percentualSatisfeito, double saldoRemanescente, String motivo) {
        return projectionSupport.action("transito.julgado", "consolidarFechamentoExecutivo", processoId,
                engine.consolidarFechamentoExecutivo(processoId, modoFechamento, preferencia, subrogacao, percentualSatisfeito, saldoRemanescente, motivo));
    }

    public SurfaceActionResponse registrarSatisfacaoTerminal(Long processoId, String modo, double percentualSatisfeito, double saldoRemanescente, String fundamento) {
        return projectionSupport.action("transito.julgado", "registrarSatisfacaoTerminal", processoId,
                engine.registrarSatisfacaoTerminal(processoId, modo, percentualSatisfeito, saldoRemanescente, fundamento));
    }

    public SurfaceActionResponse vincularArquivamentoTerminal(Long processoId, String operacao, String disposicaoTerminal, String motivo, double percentualSatisfeito, double saldoRemanescente) {
        return projectionSupport.action("transito.julgado", "vincularArquivamentoTerminal", processoId,
                engine.vincularArquivamentoTerminal(processoId, operacao, disposicaoTerminal, motivo, percentualSatisfeito, saldoRemanescente));
    }

    public SurfaceActionResponse arquivar(Long processoId, String motivoArquivamento) {
        return projectionSupport.action("transito.julgado", "arquivar", processoId, engine.determinarBaixaArquivamento(processoId, motivoArquivamento));
    }

    public SurfaceActionResponse desarquivar(Long processoId, String justificativa) {
        return projectionSupport.action("transito.julgado", "desarquivar", processoId, engine.abrirDesarquivamento(processoId, justificativa));
    }

    public SurfaceSnapshotResponse efeitos(Long processoId) {
        return projectionSupport.snapshot("transito.julgado.efeitos", engine.calcularEfeitosTransito(processoId));
    }

    public SurfaceSnapshotResponse malhaExecutiva(Long processoId) {
        return projectionSupport.snapshot("transito.julgado.malhaExecutiva", engine.diagnosticarMalhaExecutiva(processoId));
    }

    public SurfaceSnapshotResponse snapshotExecutivo(Long processoId) {
        return projectionSupport.snapshot("transito.julgado.snapshotExecutivo", engine.consultarSnapshotExecutivo(processoId));
    }
}
