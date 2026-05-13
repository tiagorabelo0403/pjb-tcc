package com.tcc.pjb.backend.service.processual.substituicao.nacional.execucao;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoNacionalCommandApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoNacionalExecutionQueryApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoNacionalOperationalCockpitApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoSituacao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalExecucaoAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalExecucaoEvento;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit.PjbSubstituicaoNacionalCockpitResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoCommandRequest;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoCommandResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoControleRequest;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoEventoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoOperacionalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal.PjbSubstituicaoTribunalEvidenciaExportavelResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal.PjbSubstituicaoTribunalReconciliacaoResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.features", name = "substituicao-nacional", havingValue = "true", matchIfMissing = true)
public class PjbSubstituicaoNacionalExecutionFacadeService {

    private final PjbSubstituicaoNacionalCommandApplicationService commandApplicationService;
    private final PjbSubstituicaoNacionalExecutionQueryApplicationService queryApplicationService;
    private final PjbSubstituicaoNacionalOperationalCockpitApplicationService operationalCockpitApplicationService;

    public PjbSubstituicaoNacionalExecutionFacadeService(PjbSubstituicaoNacionalCommandApplicationService commandApplicationService,
                                                         PjbSubstituicaoNacionalExecutionQueryApplicationService queryApplicationService,
                                                         PjbSubstituicaoNacionalOperationalCockpitApplicationService operationalCockpitApplicationService) {
        this.commandApplicationService = Objects.requireNonNull(commandApplicationService);
        this.queryApplicationService = Objects.requireNonNull(queryApplicationService);
        this.operationalCockpitApplicationService = Objects.requireNonNull(operationalCockpitApplicationService);
    }

    public PjbSubstituicaoNacionalExecucaoCommandResponse submeter(PjbSubstituicaoNacionalExecucaoCommandRequest request,
                                                                   String requestedBy,
                                                                   String idempotencyKey) {
        return commandApplicationService.submeter(request, requestedBy, idempotencyKey);
    }

    public PjbSubstituicaoNacionalExecucaoResponse detalhar(Long execucaoId) {
        return map(queryApplicationService.detalhar(execucaoId));
    }

    public PjbSubstituicaoNacionalExecucaoOperacionalResponse detalharOperacional(Long execucaoId) {
        return operationalCockpitApplicationService.detalharOperacional(execucaoId);
    }

    public List<PjbSubstituicaoNacionalExecucaoResponse> listar(String tribunalCodigo,
                                                                PjbSubstituicaoExecucaoAcao acao,
                                                                PjbSubstituicaoExecucaoSituacao situacao) {
        return queryApplicationService.listar(tribunalCodigo, acao, situacao).stream().map(this::map).toList();
    }

    public PjbSubstituicaoNacionalCockpitResponse cockpit(String tribunalCodigo) {
        return operationalCockpitApplicationService.cockpit(tribunalCodigo);
    }

    public PjbSubstituicaoTribunalReconciliacaoResponse reconciliarTribunal(String tribunalCodigo) {
        return operationalCockpitApplicationService.reconciliarTribunal(tribunalCodigo);
    }

    public PjbSubstituicaoTribunalEvidenciaExportavelResponse evidenciasExportaveisTribunal(String tribunalCodigo) {
        return operationalCockpitApplicationService.evidenciaExportavelTribunal(tribunalCodigo);
    }

    public PjbSubstituicaoNacionalExecucaoResponse controlar(Long execucaoId,
                                                             PjbSubstituicaoNacionalExecucaoControleRequest request) {
        return map(commandApplicationService.controlar(execucaoId, request));
    }

    private PjbSubstituicaoNacionalExecucaoResponse map(PjbSubstituicaoNacionalExecucaoAggregate aggregate) {
        return new PjbSubstituicaoNacionalExecucaoResponse(
                aggregate.execucaoId(),
                aggregate.tribunalCodigo(),
                aggregate.tribunalNome(),
                aggregate.ramoJustica(),
                aggregate.acao(),
                aggregate.situacao(),
                aggregate.faseAtual(),
                aggregate.modoExecucao(),
                aggregate.dryRun(),
                aggregate.gateAprovado(),
                aggregate.rollbackReversivel(),
                aggregate.gateScore(),
                aggregate.jobId(),
                aggregate.correlationId(),
                aggregate.requestHash(),
                aggregate.requestedBy(),
                aggregate.justificativa(),
                aggregate.ondaAlvo(),
                aggregate.payload(),
                aggregate.resultado(),
                aggregate.eventos().stream().map(this::mapEvento).toList(),
                aggregate.criadoEm(),
                aggregate.iniciadoEm(),
                aggregate.concluidoEm(),
                aggregate.atualizadoEm()
        );
    }

    private PjbSubstituicaoNacionalExecucaoEventoResponse mapEvento(PjbSubstituicaoNacionalExecucaoEvento evento) {
        return new PjbSubstituicaoNacionalExecucaoEventoResponse(
                evento.eventoId(),
                evento.codigo(),
                evento.severidade(),
                evento.fase(),
                evento.descricao(),
                evento.detalhes(),
                evento.criadoEm()
        );
    }
}
