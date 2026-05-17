package com.tcc.pjb.backend.core.processo.lifecycle;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.processo.lifecycle.civel.CivilLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.civel.FalenciaRecuperacaoLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.civel.FamiliaLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.civel.JuizadoLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.eleitoral.EleitoralLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.militar.MilitarLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.penal.ExecucaoPenalLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.penal.PenalLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.previdenciario.PrevidenciarioLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.trabalhista.TrabalhistaLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.tributario.ExecucaoFiscalLifecyclePack;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualCatalogService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.casefile.CaseContinuityOrchestratorService;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

@Service
public class ProcessoLifecycleMachine {

    private final AtoProcessualCatalogService atoCatalogService;
    private final RitoLifecyclePackRegistry ritoLifecyclePackRegistry;
    private final CaseContinuityOrchestratorService caseContinuityOrchestratorService;

    @Inject
    public ProcessoLifecycleMachine(AtoProcessualCatalogService atoCatalogService,
                                    RitoLifecyclePackRegistry ritoLifecyclePackRegistry,
                                    CaseContinuityOrchestratorService caseContinuityOrchestratorService) {
        this.atoCatalogService = Objects.requireNonNull(atoCatalogService);
        this.ritoLifecyclePackRegistry = Objects.requireNonNull(ritoLifecyclePackRegistry);
        this.caseContinuityOrchestratorService = caseContinuityOrchestratorService;
    }

    ProcessoLifecycleMachine(AtoProcessualCatalogService atoCatalogService) {
        this(atoCatalogService, defaultRegistry(), null);
    }

    private static RitoLifecyclePackRegistry defaultRegistry() {
        return new RitoLifecyclePackRegistry(List.of(
                new CivilLifecyclePack(),
                new PenalLifecyclePack(),
                new TrabalhistaLifecyclePack(),
                new EleitoralLifecyclePack(),
                new MilitarLifecyclePack(),
                new JuizadoLifecyclePack(),
                new ExecucaoFiscalLifecyclePack(),
                new ExecucaoPenalLifecyclePack(),
                new FamiliaLifecyclePack(),
                new PrevidenciarioLifecyclePack(),
                new FalenciaRecuperacaoLifecyclePack()
        ));
    }

    public ProcessoLifecycleDecision preview(Processo processo, ProcessoLifecycleAction action) {
        Objects.requireNonNull(processo);
        Objects.requireNonNull(action);
        FaseProcessual faseOrigem = processo.getFaseAtual() == null ? FaseProcessual.CONHECIMENTO : processo.getFaseAtual();
        StatusProcesso statusOrigem = processo.getStatusProcesso() == null ? StatusProcesso.EM_ANDAMENTO : processo.getStatusProcesso();
        List<String> warningList = warnings(processo, action, faseOrigem, statusOrigem);
        if (!isAllowed(processo, statusOrigem, action)) {
            return new ProcessoLifecycleDecision(
                    action,
                    faseOrigem,
                    statusOrigem,
                    faseOrigem,
                    statusOrigem,
                    false,
                    denialReason(processo, action, faseOrigem, statusOrigem),
                    responsibleFor(action, processo.getRito()),
                    atoCatalogService.descriptorFor(action),
                    warningList
            );
        }
        FaseProcessual faseDestino = nextPhase(processo, action, faseOrigem);
        StatusProcesso statusDestino = nextStatus(processo, action, statusOrigem);
        return new ProcessoLifecycleDecision(
                action,
                faseOrigem,
                statusOrigem,
                faseDestino,
                statusDestino,
                true,
                "OK",
                responsibleFor(action, processo.getRito()),
                atoCatalogService.descriptorFor(action),
                warningList
        );
    }

    public ProcessoLifecycleDecision apply(Processo processo, ProcessoLifecycleAction action) {
        ProcessoLifecycleDecision decision = preview(processo, action);
        if (decision.permitida()) {
            processo.setFaseAtual(decision.faseDestino());
            processo.setStatusProcesso(decision.statusDestino());
            processo.setDataUltimaMovimentacao(LocalDateTime.now());
            if (caseContinuityOrchestratorService != null) {
                caseContinuityOrchestratorService.syncFromLifecycle(processo, action);
            }
        }
        return decision;
    }

    private boolean isAllowed(Processo processo, StatusProcesso status, ProcessoLifecycleAction action) {
        RitoProcessual rito = processo.getRito();
        return switch (action) {
            case DISTRIBUIR -> true;
            case EXPEDIR_INTIMACAO,
                    REALIZAR_JUNTADA,
                    CONCLUIR_PARA_DESPACHO,
                    ASSINAR_DESPACHO,
                    DESIGNAR_AUDIENCIA,
                    APRESENTAR_LAUDO,
                    RESPONDER_QUESITOS,
                    REGISTRAR_ACORDO -> status != StatusProcesso.ARQUIVADO && status != StatusProcesso.TRANSITO_EM_JULGADO;
            case PROFERIR_SENTENCA -> status != StatusProcesso.ARQUIVADO
                    && status != StatusProcesso.TRANSITO_EM_JULGADO
                    && status != StatusProcesso.RECURSO_INTERPOSTO;
            case INTERPOR_RECURSO -> status != StatusProcesso.ARQUIVADO
                    && status != StatusProcesso.TRANSITO_EM_JULGADO
                    && status != StatusProcesso.BAIXADO;
            case PROFERIR_VOTO, LAVRAR_ACORDAO, PEDIR_VISTA -> status == StatusProcesso.RECURSO_INTERPOSTO
                    || status == StatusProcesso.EM_ANDAMENTO
                    || status == StatusProcesso.JULGADO;
            case CERTIFICAR_TRANSITO -> status != StatusProcesso.ARQUIVADO;
            case INICIAR_CUMPRIMENTO -> status == StatusProcesso.TRANSITO_EM_JULGADO
                    || status == StatusProcesso.JULGADO
                    || status == StatusProcesso.SENTENCA_PROFERIDA;
            case ARQUIVAR -> status == StatusProcesso.TRANSITO_EM_JULGADO
                    || status == StatusProcesso.CUMPRIMENTO_SENTENCA
                    || status == StatusProcesso.JULGADO
                    || status == StatusProcesso.SENTENCA_PROFERIDA
                    || (rito != null && rito.isAutocompositivo() && status == StatusProcesso.EM_ANDAMENTO);
            case DESARQUIVAR -> status == StatusProcesso.ARQUIVADO;
        };
    }

    private FaseProcessual nextPhase(Processo processo, ProcessoLifecycleAction action, FaseProcessual faseOrigem) {
        RitoProcessual rito = processo.getRito();
        RitoLifecyclePack pack = ritoLifecyclePackRegistry.resolve(rito);
        return switch (action) {
            case DISTRIBUIR -> pack.initialPhase(rito, phaseFallback(faseOrigem, FaseProcessual.CONHECIMENTO));
            case EXPEDIR_INTIMACAO, REALIZAR_JUNTADA, CONCLUIR_PARA_DESPACHO, ASSINAR_DESPACHO -> phaseFallback(faseOrigem, pack.initialPhase(rito, FaseProcessual.CONHECIMENTO));
            case DESIGNAR_AUDIENCIA -> pack.audiencePhase(rito);
            case APRESENTAR_LAUDO, RESPONDER_QUESITOS -> FaseProcessual.PERICIA_TECNICA;
            case PROFERIR_SENTENCA, REGISTRAR_ACORDO -> pack.decisionPhase(rito);
            case INTERPOR_RECURSO, PROFERIR_VOTO, LAVRAR_ACORDAO, PEDIR_VISTA, CERTIFICAR_TRANSITO -> FaseProcessual.RECURSAL;
            case INICIAR_CUMPRIMENTO -> pack.executionPhase(rito);
            case ARQUIVAR -> phaseFallback(faseOrigem, pack.initialPhase(rito, FaseProcessual.CONHECIMENTO));
            case DESARQUIVAR -> pack.reopenPhase(rito);
        };
    }

    private StatusProcesso nextStatus(Processo processo, ProcessoLifecycleAction action, StatusProcesso statusOrigem) {
        RitoProcessual rito = processo.getRito();
        return switch (action) {
            case DISTRIBUIR -> StatusProcesso.DISTRIBUIDO;
            case EXPEDIR_INTIMACAO -> statusOrigem == StatusProcesso.DISTRIBUIDO ? StatusProcesso.CITACAO_REALIZADA : StatusProcesso.EM_ANDAMENTO;
            case REALIZAR_JUNTADA, CONCLUIR_PARA_DESPACHO, ASSINAR_DESPACHO, APRESENTAR_LAUDO, RESPONDER_QUESITOS -> StatusProcesso.EM_ANDAMENTO;
            case DESIGNAR_AUDIENCIA -> StatusProcesso.AUDIENCIA_DESIGNADA;
            case PROFERIR_SENTENCA -> StatusProcesso.SENTENCA_PROFERIDA;
            case INTERPOR_RECURSO -> StatusProcesso.RECURSO_INTERPOSTO;
            case PROFERIR_VOTO -> StatusProcesso.RECURSO_INTERPOSTO;
            case LAVRAR_ACORDAO, REGISTRAR_ACORDO -> StatusProcesso.JULGADO;
            case PEDIR_VISTA -> StatusProcesso.RECURSO_INTERPOSTO;
            case CERTIFICAR_TRANSITO -> StatusProcesso.TRANSITO_EM_JULGADO;
            case INICIAR_CUMPRIMENTO -> StatusProcesso.CUMPRIMENTO_SENTENCA;
            case ARQUIVAR -> StatusProcesso.ARQUIVADO;
            case DESARQUIVAR -> StatusProcesso.EM_ANDAMENTO;
        };
    }

    private FaseProcessual phaseFallback(FaseProcessual current, FaseProcessual fallback) {
        return current == null ? fallback : current;
    }

    private String responsibleFor(ProcessoLifecycleAction action, RitoProcessual rito) {
        RitoLifecyclePack pack = ritoLifecyclePackRegistry.resolve(rito);
        return switch (action) {
            case DISTRIBUIR, EXPEDIR_INTIMACAO, REALIZAR_JUNTADA, CONCLUIR_PARA_DESPACHO, CERTIFICAR_TRANSITO, ARQUIVAR, DESARQUIVAR -> "SECRETARIA";
            case DESIGNAR_AUDIENCIA, ASSINAR_DESPACHO, PROFERIR_SENTENCA, INICIAR_CUMPRIMENTO -> pack.magistratureDesk(rito);
            case INTERPOR_RECURSO -> "ADVOCACIA";
            case PROFERIR_VOTO, LAVRAR_ACORDAO, PEDIR_VISTA -> pack.collegiateDesk(rito);
            case REGISTRAR_ACORDO -> "CEJUSC";
            case APRESENTAR_LAUDO, RESPONDER_QUESITOS -> "PERICIA";
        };
    }

    private String denialReason(Processo processo,
                                ProcessoLifecycleAction action,
                                FaseProcessual faseOrigem,
                                StatusProcesso statusOrigem) {
        RitoProcessual rito = processo.getRito();
        if (statusOrigem == StatusProcesso.ARQUIVADO && action != ProcessoLifecycleAction.DESARQUIVAR) {
            return "Transição incompatível com processo arquivado.";
        }
        if (statusOrigem == StatusProcesso.TRANSITO_EM_JULGADO && action != ProcessoLifecycleAction.INICIAR_CUMPRIMENTO && action != ProcessoLifecycleAction.ARQUIVAR) {
            return "Transição incompatível com processo já transitado em julgado.";
        }
        if (action == ProcessoLifecycleAction.PROFERIR_SENTENCA && faseOrigem == FaseProcessual.RECURSAL) {
            return "Sentença de primeiro grau não deve ser emitida em fase recursal consolidada.";
        }
        if (action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO && rito != null && rito.isPenal()) {
            return "Cumprimento civil não é compatível com rito penal materializado neste estado.";
        }
        return "Transição incompatível com o estado processual atual.";
    }

    private List<String> warnings(Processo processo,
                                  ProcessoLifecycleAction action,
                                  FaseProcessual faseOrigem,
                                  StatusProcesso statusOrigem) {
        List<String> warnings = new ArrayList<>();
        RitoProcessual rito = processo.getRito();
        NivelSigilo nivelSigilo = processo.getNivelSigilo() != null ? processo.getNivelSigilo() : NivelSigilo.PUBLICO;

        if (rito == null) {
            warnings.add("Processo sem rito consolidado para transição refinada.");
        }
        if (action == ProcessoLifecycleAction.INTERPOR_RECURSO
                && statusOrigem != StatusProcesso.SENTENCA_PROFERIDA
                && statusOrigem != StatusProcesso.JULGADO) {
            warnings.add("Recurso interposto sem marco decisório explícito no status atual.");
        }
        if (action == ProcessoLifecycleAction.CERTIFICAR_TRANSITO
                && statusOrigem != StatusProcesso.RECURSO_INTERPOSTO
                && statusOrigem != StatusProcesso.JULGADO
                && statusOrigem != StatusProcesso.SENTENCA_PROFERIDA) {
            warnings.add("Certificação de trânsito ocorreu sem cadeia recursal ou decisória clássica.");
        }
        if (action == ProcessoLifecycleAction.ARQUIVAR
                && faseOrigem == FaseProcessual.RECURSAL
                && statusOrigem != StatusProcesso.TRANSITO_EM_JULGADO) {
            warnings.add("Arquivamento em fase recursal sem trânsito em julgado explícito.");
        }
        if (rito != null && rito.requiresSegredoByDefault() && !nivelSigilo.exigeCredencial()) {
            warnings.add("Rito sensível sem nível de sigilo reforçado para a natureza material do caso.");
        }
        if (rito == RitoProcessual.TRIBUNAL_JURI
                && action == ProcessoLifecycleAction.PROFERIR_SENTENCA
                && faseOrigem != FaseProcessual.PRONUNCIA
                && faseOrigem != FaseProcessual.PLENARIO_JURI) {
            warnings.add("Júri sem marcos típicos de pronúncia/plenário antes do julgamento final.");
        }
        if (rito != null && rito.name().contains("JUIZADO")
                && action == ProcessoLifecycleAction.INTERPOR_RECURSO) {
            warnings.add("Revisar encaminhamento para turma recursal e pressupostos específicos do microssistema dos juizados.");
        }
        if (rito != null && rito.isTrabalhista()
                && action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO
                && faseOrigem == FaseProcessual.CONHECIMENTO) {
            warnings.add("Fluxo trabalhista pode exigir liquidação ou cálculos antes da executória, conforme o caso.");
        }
        if (rito != null && rito.isEleitoral() && action.isRecursal()) {
            warnings.add("Validar prazos peremptórios e órgão colegiado competente da Justiça Eleitoral.");
        }
        if (rito != null && rito.isMilitar() && action == ProcessoLifecycleAction.DESIGNAR_AUDIENCIA) {
            warnings.add("Verificar composição de conselho e competência militar antes da audiência.");
        }
        return List.copyOf(warnings);
    }
}
