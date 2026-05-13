package com.tcc.pjb.backend.service.procuradoria.surface;

import com.tcc.pjb.backend.model.dto.procuradoria.surface.PrecatorioRpvCalculoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.ProcuradoriaContestacaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.ProcuradoriaParecerRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.procuradoria.PrecatorioRpvService;
import com.tcc.pjb.backend.service.procuradoria.ProcuradoriaOperacionalService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class ProcuradoriaOperationalSurfaceFacadeService {

    private final ProcuradoriaOperacionalService operacionalService;
    private final PrecatorioRpvService precatorioRpvService;
    private final SurfaceProjectionSupport projectionSupport;

    public ProcuradoriaOperationalSurfaceFacadeService(ProcuradoriaOperacionalService operacionalService,
                                                       PrecatorioRpvService precatorioRpvService,
                                                       SurfaceProjectionSupport projectionSupport) {
        this.operacionalService = operacionalService;
        this.precatorioRpvService = precatorioRpvService;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse snapshot() {
        return projectionSupport.snapshot("procuradoria.operacional", operacionalService.bootstrapPainel());
    }

    public SurfaceSnapshotResponse malhaProcesso(Long processoId) {
        return projectionSupport.snapshot("procuradoria.operacional.malha", operacionalService.malhaProcesso(processoId));
    }

    public SurfaceActionResponse apresentarContestacao(Long processoId, ProcuradoriaContestacaoRequest request) {
        return projectionSupport.action("procuradoria.operacional", "apresentarContestacao", processoId,
                operacionalService.apresentarContestatacao(processoId, request));
    }

    public SurfaceActionResponse ajuizarExecucaoFiscal(String devedorCpfCnpj, double valorDivida, String descricao, String comarca) {
        return projectionSupport.action("procuradoria.operacional", "ajuizarExecucaoFiscal", null,
                operacionalService.ajuizarExecucaoFiscal(devedorCpfCnpj, valorDivida, descricao, comarca));
    }

    public SurfaceActionResponse emitirParecer(Long processoId, ProcuradoriaParecerRequest request) {
        return projectionSupport.action("procuradoria.operacional", "emitirParecer", processoId,
                operacionalService.emitirParecer(processoId, request));
    }

    public SurfaceActionResponse interporRecurso(Long processoId,
                                                 String tipoRecurso,
                                                 String razoes,
                                                 String fundamentacao,
                                                 boolean pedidoEfeitoSuspensivo,
                                                 boolean preparoDispensado,
                                                 String observacoes) {
        return projectionSupport.action("procuradoria.operacional", "interporRecurso", processoId,
                operacionalService.interporRecurso(processoId, tipoRecurso, razoes, fundamentacao, pedidoEfeitoSuspensivo, preparoDispensado, observacoes));
    }

    public SurfaceSnapshotResponse calcularPrecatorioRpv(PrecatorioRpvCalculoRequest request) {
        PrecatorioRpvService.PrecatorioRpvRequest mapped = new PrecatorioRpvService.PrecatorioRpvRequest(
                request.processoId(),
                request.valorPrincipal(),
                request.indiceCorrecao(),
                request.indiceJuros(),
                request.indiceSelicTeto(),
                request.limiteRpv(),
                request.naturezaCredito(),
                request.enteDevedorTipo(),
                request.entidadeDevedoraCodigo(),
                request.dataBaseCalculo(),
                request.dataApresentacao(),
                request.dataNascimentoBeneficiario(),
                request.doencaGrave(),
                request.pessoaComDeficiencia(),
                request.regimeEspecial(),
                request.acordoDiretoHabilitado()
        );
        return projectionSupport.snapshot("procuradoria.financeiro", precatorioRpvService.calcular(mapped));
    }
}
