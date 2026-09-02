package com.tcc.pjb.backend.service.processual.surface;

import com.tcc.pjb.backend.core.processo.busca.application.ProcessoBuscaAnalyticsApplicationService;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoAnalyticsAggregate;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoBuscaAggregate;
import com.tcc.pjb.backend.core.processo.dsl.application.ProcessoDslApplicationService;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.application.ProcessoEncaixeFinalApplicationService;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeCarteiraAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeFinalAggregate;
import com.tcc.pjb.backend.core.processo.policy.application.ProcessoPolicyVigenciaApplicationService;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyAggregate;
import com.tcc.pjb.backend.core.processo.posse.application.ProcessoPosseTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.posse.domain.ProcessoPosseAggregate;
import com.tcc.pjb.backend.core.processo.pregravacao.application.ProcessoPreGravacaoApplicationService;
import com.tcc.pjb.backend.core.processo.pregravacao.domain.ProcessoPreGravacaoAggregate;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de ProcessoSurfaceFacadeService: superfícies de leitura/avaliação
 * -- busca+analytics de acervo, encaixe final (score + carteira), DSL de regras,
 * policy de vigência, posse de trabalho e pré-gravação de ato (guarda step-up).
 */
@Service
public class ProcessoSurfaceInsightGovernancaOrchestrator {

    private final ProcessoBuscaAnalyticsApplicationService processoBuscaAnalyticsApplicationService;
    private final ProcessoEncaixeFinalApplicationService processoEncaixeFinalApplicationService;
    private final ProcessoDslApplicationService processoDslApplicationService;
    private final ProcessoPolicyVigenciaApplicationService processoPolicyVigenciaApplicationService;
    private final ProcessoPosseTrabalhoApplicationService processoPosseTrabalhoApplicationService;
    private final ProcessoPreGravacaoApplicationService processoPreGravacaoApplicationService;

    public ProcessoSurfaceInsightGovernancaOrchestrator(ProcessoBuscaAnalyticsApplicationService processoBuscaAnalyticsApplicationService,
                                                         ProcessoEncaixeFinalApplicationService processoEncaixeFinalApplicationService,
                                                         ProcessoDslApplicationService processoDslApplicationService,
                                                         ProcessoPolicyVigenciaApplicationService processoPolicyVigenciaApplicationService,
                                                         ProcessoPosseTrabalhoApplicationService processoPosseTrabalhoApplicationService,
                                                         ProcessoPreGravacaoApplicationService processoPreGravacaoApplicationService) {
        this.processoBuscaAnalyticsApplicationService = Objects.requireNonNull(processoBuscaAnalyticsApplicationService);
        this.processoEncaixeFinalApplicationService = Objects.requireNonNull(processoEncaixeFinalApplicationService);
        this.processoDslApplicationService = Objects.requireNonNull(processoDslApplicationService);
        this.processoPolicyVigenciaApplicationService = Objects.requireNonNull(processoPolicyVigenciaApplicationService);
        this.processoPosseTrabalhoApplicationService = Objects.requireNonNull(processoPosseTrabalhoApplicationService);
        this.processoPreGravacaoApplicationService = Objects.requireNonNull(processoPreGravacaoApplicationService);
    }

    public ProcessoBuscaAggregate buscar(String cpf, String nome, String numero, String uf, String comarca,
                                          String ramo, String status, String tribunal, int page, int size) {
        return processoBuscaAnalyticsApplicationService.buscar(cpf, nome, numero, uf, comarca, ramo, status, tribunal, page, size);
    }

    public ProcessoAnalyticsAggregate analytics(String ramo, String tribunal, String uf, String comarca) {
        return processoBuscaAnalyticsApplicationService.analytics(ramo, tribunal, uf, comarca);
    }

    public ProcessoEncaixeFinalAggregate encaixeFinal(Long processoId) {
        return processoEncaixeFinalApplicationService.detalhar(processoId);
    }

    public ProcessoEncaixeCarteiraAggregate encaixeCarteira(int limit) {
        return processoEncaixeFinalApplicationService.varrer(limit);
    }

    public ProcessoDslAggregate dsl(Long processoId) {
        return processoDslApplicationService.detalhar(processoId);
    }

    public ProcessoPolicyAggregate policy(Long processoId) {
        return processoPolicyVigenciaApplicationService.avaliar(processoId);
    }

    public ProcessoPolicyAggregate policy(Long processoId, LocalDate em) {
        return processoPolicyVigenciaApplicationService.avaliar(processoId, em);
    }

    public ProcessoPosseAggregate posse(Long processoId) {
        return processoPosseTrabalhoApplicationService.detalhar(processoId);
    }

    public ProcessoPreGravacaoAggregate preGravacao(Long processoId, String profileCode, String actionCode) {
        return processoPreGravacaoApplicationService.avaliar(processoId, profileCode, actionCode);
    }
}
