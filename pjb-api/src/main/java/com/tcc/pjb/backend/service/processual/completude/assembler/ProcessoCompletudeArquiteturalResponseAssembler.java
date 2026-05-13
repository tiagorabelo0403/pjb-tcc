package com.tcc.pjb.backend.service.processual.completude.assembler;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosProva;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosSistema;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsNacionalAggregate;
import com.tcc.pjb.backend.core.processo.completude.domain.ProcessoFechamentoTotalAggregate;
import com.tcc.pjb.backend.core.processo.orfandade.domain.ProcessoAntiOrfaoAggregate;
import com.tcc.pjb.backend.core.processo.plantao.domain.ProcessoPlantaoSubstituicaoAggregate;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoTransversalAggregate;
import com.tcc.pjb.backend.core.processo.sinalizacao.domain.ProcessoSinalizacaoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoInfraestruturaSoberanaAggregate;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceIssue;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalItem;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.presentation.PjbCodebaseSanityResponseMapper;
import com.tcc.pjb.backend.model.dto.processual.completude.apisurface.ProcessoApiSurfaceIssueResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.apisurface.ProcessoApiSurfaceSanityResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.certificacao.ProcessoCertificacaoOperacionalItemResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.certificacao.ProcessoCertificacaoOperacionalResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseSanityResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.ProcessoCompletudeModuloResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.ProcessoFechamentoTotalResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.infraestrutura.ProcessoInfraestruturaSoberanaResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.substituicao.ProcessoSubstituicaoLegadosProvaResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.substituicao.ProcessoSubstituicaoLegadosResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.substituicao.ProcessoSubstituicaoLegadosSistemaResponse;
import org.springframework.stereotype.Component;

@Component
public class ProcessoCompletudeArquiteturalResponseAssembler {

    public ProcessoCompletudeModuloResponse antiOrfao(ProcessoAntiOrfaoAggregate aggregate) {
        return modulo("anti-orfao", aggregate.totalGaps() == 0 ? "INTEGRADO" : "COM_GAPS", scoreAntiOrfao(aggregate), aggregate.proximasAcoes());
    }

    public ProcessoCompletudeModuloResponse sinalizacao(ProcessoSinalizacaoAggregate aggregate) {
        return modulo("sinalizacao", aggregate.priorityBand(), aggregate.alertas().isEmpty() ? 92 : 76, aggregate.alertas());
    }

    public ProcessoCompletudeModuloResponse plantaoSubstituicao(ProcessoPlantaoSubstituicaoAggregate aggregate) {
        int score = aggregate.alertas().isEmpty() ? 94 : 72;
        return modulo("plantao-substituicao", aggregate.regimeAtivo(), score, aggregate.alertas());
    }

    public ProcessoCompletudeModuloResponse analyticsNacional(ProcessoAnalyticsNacionalAggregate aggregate) {
        int score = aggregate.alertas().isEmpty() ? 90 : 75;
        return modulo("analytics-nacional", aggregate.riscoSlaGlobal() > 0.7d ? "ATENCAO" : "ESTAVEL", score, aggregate.alertas());
    }

    public ProcessoCompletudeModuloResponse operacaoTransversal(ProcessoOperacaoTransversalAggregate aggregate) {
        int score = (int) Math.round((aggregate.coberturaGlobal() + (100d - aggregate.saturacao())) / 2d);
        return modulo("operacao-transversal", aggregate.readiness(), score, aggregate.alertas());
    }

    public ProcessoInfraestruturaSoberanaResponse infraestrutura(ProcessoInfraestruturaSoberanaAggregate aggregate) {
        return new ProcessoInfraestruturaSoberanaResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                modulo("fonte-soberana", statusFonte(aggregate), aggregate.fonte().confiabilidadeMedia(), aggregate.fonte().registros().stream().map(registro -> registro.chave() + ":" + registro.status().name()).toList()),
                modulo("cumprimento-operacional", statusCumprimento(aggregate), scoreCumprimento(aggregate), aggregate.cumprimento().fundamentos()),
                modulo("cooperacao-institucional", statusCooperacao(aggregate), scoreCooperacao(aggregate), aggregate.cooperacao().fundamentos()),
                certificacao(aggregate.certificacao()),
                modulo("gemeo-digital", aggregate.gemeo().estadoAtual().name(), scoreGemeo(aggregate), aggregate.gemeo().riscos().stream().map(risco -> risco.codigo() + ":" + risco.nivel()).toList()),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    public ProcessoCertificacaoOperacionalResponse certificacao(PjbCertificacaoOperacionalAggregate aggregate) {
        return new ProcessoCertificacaoOperacionalResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.percentualCobertura(),
                aggregate.possuiFalhaCritica(),
                aggregate.modulosCriticos(),
                aggregate.itens().stream().map(this::certificacaoItem).toList(),
                aggregate.geradoEm()
        );
    }

    public ProcessoSubstituicaoLegadosResponse substituicao(PjbSubstituicaoLegadosAggregate aggregate) {
        return new ProcessoSubstituicaoLegadosResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.scoreGeral(),
                aggregate.prontoSubstituicaoImediata(),
                aggregate.conclusaoTecnica(),
                aggregate.provas().stream().map(this::prova).toList(),
                aggregate.sistemas().stream().map(this::sistema).toList(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    public ProcessoCodebaseSanityResponse codebase(PjbCodebaseSanityAggregate aggregate) {
        return PjbCodebaseSanityResponseMapper.toProcessual(aggregate);
    }

    public ProcessoApiSurfaceSanityResponse apiSurface(PjbApiSurfaceSanityAggregate aggregate) {
        return new ProcessoApiSurfaceSanityResponse(
                aggregate.raizEncontrada(),
                aggregate.limpo(),
                aggregate.score(),
                aggregate.controllersInspecionados(),
                aggregate.dtoInspecionados(),
                aggregate.rotasDuplicadas(),
                aggregate.dtoForaDoPadrao(),
                aggregate.entidadesExpostasDiretamente(),
                aggregate.issues().stream().map(this::apiSurfaceIssue).toList(),
                aggregate.auditadoEm()
        );
    }

    public ProcessoFechamentoTotalResponse fechamentoTotal(ProcessoFechamentoTotalAggregate aggregate,
                                                           PjbApiSurfaceSanityAggregate apiSurface) {
        return new ProcessoFechamentoTotalResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.readiness(),
                aggregate.scoreGeral(),
                antiOrfao(aggregate.antiOrfao()),
                sinalizacao(aggregate.sinalizacao()),
                plantaoSubstituicao(aggregate.plantaoSubstituicao()),
                analyticsNacional(aggregate.analyticsNacional()),
                operacaoTransversal(aggregate.operacaoTransversal()),
                infraestrutura(aggregate.infraestruturaSoberana()),
                certificacao(aggregate.certificacaoOperacional()),
                substituicao(aggregate.substituicaoLegados()),
                codebase(aggregate.codebaseSanity()),
                apiSurface(apiSurface),
                aggregate.alertas(),
                aggregate.plano(),
                aggregate.geradoEm()
        );
    }

    private ProcessoCertificacaoOperacionalItemResponse certificacaoItem(PjbCertificacaoOperacionalItem item) {
        return new ProcessoCertificacaoOperacionalItemResponse(item.codigo(), item.categoria(), item.severidade(), item.conforme(), item.diagnostico(), item.acaoCorretiva());
    }

    private ProcessoSubstituicaoLegadosProvaResponse prova(PjbSubstituicaoLegadosProva prova) {
        return new ProcessoSubstituicaoLegadosProvaResponse(prova.codigo(), prova.titulo(), prova.status().name(), prova.score(), prova.concluida(), prova.fundamentos(), prova.bloqueios());
    }

    private ProcessoSubstituicaoLegadosSistemaResponse sistema(PjbSubstituicaoLegadosSistema sistema) {
        return new ProcessoSubstituicaoLegadosSistemaResponse(sistema.sistema(), sistema.status().name(), sistema.scoreAderencia(), sistema.conclusao(), sistema.pendencias());
    }

    private ProcessoApiSurfaceIssueResponse apiSurfaceIssue(PjbApiSurfaceIssue issue) {
        return new ProcessoApiSurfaceIssueResponse(issue.codigo(), issue.severidade(), issue.alvo(), issue.verbo(), issue.rota(), issue.detalhes());
    }

    private ProcessoCompletudeModuloResponse modulo(String codigo, String status, int score, java.util.List<String> alertas) {
        return new ProcessoCompletudeModuloResponse(codigo, status, score, alertas);
    }

    private int scoreAntiOrfao(ProcessoAntiOrfaoAggregate aggregate) {
        return (int) Math.max(0L, Math.min(100L, aggregate.coberturaPercentual()));
    }

    private String statusFonte(ProcessoInfraestruturaSoberanaAggregate aggregate) {
        if (aggregate.fonte().possuiConflito()) {
            return "CONFLITO";
        }
        if (aggregate.fonte().exigeRefresh()) {
            return "REFRESH_PENDENTE";
        }
        return "ESTAVEL";
    }

    private String statusCumprimento(ProcessoInfraestruturaSoberanaAggregate aggregate) {
        if (aggregate.cumprimento().possuiBloqueio()) {
            return "BLOQUEADO";
        }
        return aggregate.cumprimento().totalMaterializado() > 0 ? "MATERIALIZADO" : "PLANEJADO";
    }

    private int scoreCumprimento(ProcessoInfraestruturaSoberanaAggregate aggregate) {
        int base = aggregate.cumprimento().totalMaterializado() > 0 ? 90 : 72;
        return aggregate.cumprimento().possuiBloqueio() ? Math.max(0, base - 30) : base;
    }

    private String statusCooperacao(ProcessoInfraestruturaSoberanaAggregate aggregate) {
        if (aggregate.cooperacao().exigeCooperacaoSigilosa()) {
            return "SIGILOSA";
        }
        if (aggregate.cooperacao().exigeRetornoExterno()) {
            return "RETORNO_PENDENTE";
        }
        return "ESTAVEL";
    }

    private int scoreCooperacao(ProcessoInfraestruturaSoberanaAggregate aggregate) {
        int base = aggregate.cooperacao().itens().isEmpty() ? 88 : 78;
        return aggregate.cooperacao().exigeRetornoExterno() ? Math.max(0, base - 18) : base;
    }

    private int scoreGemeo(ProcessoInfraestruturaSoberanaAggregate aggregate) {
        int base = 90 - Math.min(35, aggregate.gemeo().riscos().size() * 10);
        return Math.max(0, base - Math.min(25, aggregate.gemeo().custoOperacionalEstimado()));
    }
}
