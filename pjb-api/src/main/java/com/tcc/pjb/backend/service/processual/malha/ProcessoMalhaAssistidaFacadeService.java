package com.tcc.pjb.backend.service.processual.malha;

import com.tcc.pjb.backend.service.processual.malha.internal.ProcessoMalhaAuthorizationService;
import com.tcc.pjb.backend.service.processual.malha.internal.ProcessoMalhaSigiloAuthorizationService;
import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelMalhaPapelApplicationService;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelContextualWidget;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelMalhaPapelAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelRotaTaticaItem;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaContractGuard;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaEntradaCanonicalizer;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaActorContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaSigiloContexto;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaViewLevel;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoMalhaExecucaoAssistidaApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoMalhaNacionalFechamentoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoMalhaOperacaoInstitucionalApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaExecucaoAssistidaAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalFechamentoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaOperacaoInstitucionalAggregate;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaAtorResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaExecucaoAssistidaResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaFechamentoResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaOperacaoInstitucionalResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaPainelPapelResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaRotaTaticaItemResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaRuntimeResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaSigiloResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaWidgetResponse;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoMalhaAssistidaFacadeService {

    private final ProcessoMalhaExecucaoAssistidaApplicationService processoMalhaExecucaoAssistidaApplicationService;
    private final ProcessoMalhaNacionalFechamentoApplicationService processoMalhaNacionalFechamentoApplicationService;
    private final ProcessoPainelMalhaPapelApplicationService processoPainelMalhaPapelApplicationService;
    private final ProcessoMalhaOperacaoInstitucionalApplicationService processoMalhaOperacaoInstitucionalApplicationService;
    private final ProcessoMalhaAuthorizationService processoMalhaAuthorizationService;
    private final ProcessoMalhaSigiloAuthorizationService processoMalhaSigiloAuthorizationService;
    private final ProcessoMalhaEntradaCanonicalizer canonicalizer;
    private final ProcessoMalhaContractGuard guard;

    public ProcessoMalhaAssistidaFacadeService(ProcessoMalhaExecucaoAssistidaApplicationService processoMalhaExecucaoAssistidaApplicationService,
                                               ProcessoMalhaNacionalFechamentoApplicationService processoMalhaNacionalFechamentoApplicationService,
                                               ProcessoPainelMalhaPapelApplicationService processoPainelMalhaPapelApplicationService,
                                               ProcessoMalhaOperacaoInstitucionalApplicationService processoMalhaOperacaoInstitucionalApplicationService,
                                               ProcessoMalhaAuthorizationService processoMalhaAuthorizationService,
                                               ProcessoMalhaSigiloAuthorizationService processoMalhaSigiloAuthorizationService) {
        this.processoMalhaExecucaoAssistidaApplicationService = Objects.requireNonNull(processoMalhaExecucaoAssistidaApplicationService);
        this.processoMalhaNacionalFechamentoApplicationService = Objects.requireNonNull(processoMalhaNacionalFechamentoApplicationService);
        this.processoPainelMalhaPapelApplicationService = Objects.requireNonNull(processoPainelMalhaPapelApplicationService);
        this.processoMalhaOperacaoInstitucionalApplicationService = Objects.requireNonNull(processoMalhaOperacaoInstitucionalApplicationService);
        this.processoMalhaAuthorizationService = Objects.requireNonNull(processoMalhaAuthorizationService);
        this.processoMalhaSigiloAuthorizationService = Objects.requireNonNull(processoMalhaSigiloAuthorizationService);
        this.canonicalizer = new ProcessoMalhaEntradaCanonicalizer();
        this.guard = new ProcessoMalhaContractGuard();
    }

    @Transactional
    public ProcessoMalhaExecucaoAssistidaResponse execucaoAssistida(Long processoId, String papel, String ramo) {
        return execucaoAssistida(processoId, papel, ramo, null, null, null, null);
    }

    @Transactional
    public ProcessoMalhaExecucaoAssistidaResponse execucaoAssistida(Long processoId,
                                                                    String papel,
                                                                    String ramo,
                                                                    String stepUpToken,
                                                                    String sigiloRequestId,
                                                                    String sigiloPassword,
                                                                    String forwardedFor) {
        ContextoEntrada contexto = resolverEntrada(processoId, papel, ramo, stepUpToken, sigiloRequestId, sigiloPassword, forwardedFor);
        ProcessoMalhaExecucaoAssistidaAggregate execucao = processoMalhaExecucaoAssistidaApplicationService.executar(contexto.processoId());
        ProcessoPainelMalhaPapelAggregate painelSolicitado = processoPainelMalhaPapelApplicationService.detalhar(contexto.processoId(), contexto.actor().papelEfetivo(), contexto.actor().ramoEfetivo());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(execucao.fundamentos());
        fundamentos.addAll(contexto.sigilo().fundamentos());
        fundamentos.add("entrada.papel=" + contexto.actor().papelEfetivo().name());
        fundamentos.add("entrada.ramo=" + ramoEntrada(contexto.actor().ramoEfetivo()));
        return new ProcessoMalhaExecucaoAssistidaResponse(
                execucao.processoId(),
                execucao.numeroProcesso(),
                execucao.statusExecucao(),
                execucao.acaoRecomendada(),
                ator(contexto.actor()),
                sigilo(contexto.sigilo()),
                runtime(execucao.runtime()),
                fechamento(execucao.fechamento(), contexto.actor(), contexto.sigilo()),
                painel(painelSolicitado, contexto.actor(), contexto.sigilo()),
                execucao.rotaTatica().itens().stream().map(item -> rotaItem(item, contexto.sigilo())).toList(),
                List.copyOf(fundamentos.stream().limit(160).toList()),
                execucao.geradoEm()
        );
    }

    @Transactional
    public ProcessoMalhaFechamentoResponse fechamento(Long processoId, String papel, String ramo) {
        return fechamento(processoId, papel, ramo, null, null, null, null);
    }

    @Transactional
    public ProcessoMalhaFechamentoResponse fechamento(Long processoId,
                                                      String papel,
                                                      String ramo,
                                                      String stepUpToken,
                                                      String sigiloRequestId,
                                                      String sigiloPassword,
                                                      String forwardedFor) {
        ContextoEntrada contexto = resolverEntrada(processoId, papel, ramo, stepUpToken, sigiloRequestId, sigiloPassword, forwardedFor);
        ProcessoMalhaNacionalFechamentoAggregate fechamento = processoMalhaNacionalFechamentoApplicationService.executar(contexto.processoId());
        ProcessoPainelMalhaPapelAggregate painelSolicitado = processoPainelMalhaPapelApplicationService.detalhar(contexto.processoId(), contexto.actor().papelEfetivo(), contexto.actor().ramoEfetivo());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(fechamento.fundamentos());
        fundamentos.addAll(contexto.sigilo().fundamentos());
        fundamentos.add("entrada.papel=" + contexto.actor().papelEfetivo().name());
        fundamentos.add("entrada.ramo=" + ramoEntrada(contexto.actor().ramoEfetivo()));
        return new ProcessoMalhaFechamentoResponse(
                fechamento.processoId(),
                fechamento.numeroProcesso(),
                fechamento.distribuicao().acaoExecutada(),
                fechamento.distribuicao().bloqueada(),
                fechamento.distribuicao().remessaManual(),
                fechamento.distribuicao().redistribuicaoManual(),
                fechamento.antifraude().scoreGlobal(),
                fechamento.observabilidade().saudeProcessual(),
                ator(contexto.actor()),
                sigilo(contexto.sigilo()),
                painel(painelSolicitado, contexto.actor(), contexto.sigilo()),
                runtime(fechamento.runtime()),
                List.copyOf(fundamentos.stream().limit(120).toList()),
                fechamento.geradoEm()
        );
    }

    @Transactional
    public ProcessoMalhaPainelPapelResponse painel(Long processoId, String papel, String ramo) {
        return painel(processoId, papel, ramo, null, null, null, null);
    }

    @Transactional
    public ProcessoMalhaPainelPapelResponse painel(Long processoId,
                                                   String papel,
                                                   String ramo,
                                                   String stepUpToken,
                                                   String sigiloRequestId,
                                                   String sigiloPassword,
                                                   String forwardedFor) {
        ContextoEntrada contexto = resolverEntrada(processoId, papel, ramo, stepUpToken, sigiloRequestId, sigiloPassword, forwardedFor);
        return painel(processoPainelMalhaPapelApplicationService.detalhar(contexto.processoId(), contexto.actor().papelEfetivo(), contexto.actor().ramoEfetivo()), contexto.actor(), contexto.sigilo());
    }

    @Transactional
    public ProcessoMalhaOperacaoInstitucionalResponse materializarOperacao(Long processoId,
                                                                           String papel,
                                                                           String ramo,
                                                                           String stepUpToken,
                                                                           String sigiloRequestId,
                                                                           String sigiloPassword,
                                                                           String forwardedFor) {
        ContextoEntrada contexto = resolverEntrada(processoId, papel, ramo, stepUpToken, sigiloRequestId, sigiloPassword, forwardedFor);
        ProcessoMalhaOperacaoInstitucionalAggregate aggregate = processoMalhaOperacaoInstitucionalApplicationService.materializar(
                contexto.processoId(),
                contexto.actor(),
                contexto.sigilo()
        );
        return new ProcessoMalhaOperacaoInstitucionalResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.workItemId(),
                aggregate.inboxSnapshotId(),
                aggregate.queueCode(),
                aggregate.inboxKey(),
                aggregate.status(),
                aggregate.snapshotHash(),
                ator(contexto.actor()),
                sigilo(contexto.sigilo()),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    private ContextoEntrada resolverEntrada(Long processoId,
                                            String papel,
                                            String ramo,
                                            String stepUpToken,
                                            String sigiloRequestId,
                                            String sigiloPassword,
                                            String forwardedFor) {
        Long processoIdValido = guard.requireProcessoId(processoId);
        RamoDireito ramoDireito = RamoDireito.fromNullable(ramo);
        Map<String, Object> canonicalizado = canonicalizer.canonicalizar(processoIdValido, ramoDireito, "", "", papel);
        ProcessoMalhaActorContext actor = processoMalhaAuthorizationService.resolver(
                (Long) canonicalizado.get("processoId"),
                papel,
                ramo
        );
        ProcessoMalhaSigiloContexto sigilo = processoMalhaSigiloAuthorizationService.avaliar(
                actor.actorId() == null ? processoIdValido : (Long) canonicalizado.get("processoId"),
                actor,
                stepUpToken,
                sigiloRequestId,
                sigiloPassword,
                forwardedFor
        );
        return new ContextoEntrada((Long) canonicalizado.get("processoId"), actor, sigilo);
    }

    private String ramoEntrada(RamoDireito ramoDireito) {
        return ramoDireito == null ? "PROCESSO" : ramoDireito.name();
    }

    private ProcessoMalhaRuntimeResponse runtime(ProcessoRuntimePreparationAggregate aggregate) {
        ProcessoRuntimeContext context = aggregate.context();
        return new ProcessoMalhaRuntimeResponse(
                context.numeroProcesso(),
                context.numeroUnificado(),
                context.ramoDireito() == null ? "NAO_INFORMADO" : context.ramoDireito().name(),
                context.ritoProcessual() == null ? "NAO_INFORMADO" : context.ritoProcessual().name(),
                context.papelPrincipal() == null ? TipoUsuario.CIDADAO.name() : context.papelPrincipal().name(),
                context.tribunal(),
                context.vara(),
                context.comarca(),
                context.uf(),
                context.sigiloReforcado(),
                aggregate.integrationStatus().percentualProntidao(),
                aggregate.integrationStatus().prontoMinimo(),
                aggregate.integrationStatus().componentesAusentes(),
                aggregate.alertas(),
                aggregate.fingerprint(),
                aggregate.preparadoEm()
        );
    }

    private ProcessoMalhaFechamentoResponse fechamento(ProcessoMalhaNacionalFechamentoAggregate aggregate,
                                                       ProcessoMalhaActorContext actor,
                                                       ProcessoMalhaSigiloContexto sigilo) {
        return new ProcessoMalhaFechamentoResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.distribuicao().acaoExecutada(),
                aggregate.distribuicao().bloqueada(),
                aggregate.distribuicao().remessaManual(),
                aggregate.distribuicao().redistribuicaoManual(),
                aggregate.antifraude().scoreGlobal(),
                aggregate.observabilidade().saudeProcessual(),
                ator(actor),
                sigilo(sigilo),
                painel(aggregate.painelPapel(), actor, sigilo),
                runtime(aggregate.runtime()),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    private ProcessoMalhaPainelPapelResponse painel(ProcessoPainelMalhaPapelAggregate aggregate,
                                                    ProcessoMalhaActorContext actor,
                                                    ProcessoMalhaSigiloContexto sigilo) {
        return new ProcessoMalhaPainelPapelResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.papel(),
                aggregate.ramo(),
                aggregate.statusGeral(),
                ator(actor),
                sigilo(sigilo),
                aggregate.widgets().stream().map(widget -> widget(widget, sigilo)).toList(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    private ProcessoMalhaAtorResponse ator(ProcessoMalhaActorContext actor) {
        return new ProcessoMalhaAtorResponse(
                actor.actorId(),
                actor.nome(),
                mascararCpf(actor.cpf(), actor.visualizacaoElevada()),
                actor.tipoUsuario().name(),
                actor.papelEfetivo().name(),
                actor.ramoEfetivo() == null ? "NAO_INFORMADO" : actor.ramoEfetivo().name(),
                actor.roles(),
                actor.visualizacaoElevada(),
                actor.visualizacaoContextual(),
                actor.parteRelacionada()
        );
    }

    private ProcessoMalhaSigiloResponse sigilo(ProcessoMalhaSigiloContexto sigilo) {
        return new ProcessoMalhaSigiloResponse(
                sigilo.nivelSigilo().name(),
                sigilo.viewLevel().name(),
                sigilo.acessoSensivel(),
                sigilo.stepUpExigido(),
                sigilo.stepUpAtivo(),
                sigilo.mascarado(),
                sigilo.requestId(),
                sigilo.fundamentos()
        );
    }

    private ProcessoMalhaWidgetResponse widget(ProcessoPainelContextualWidget widget,
                                               ProcessoMalhaSigiloContexto sigilo) {
        boolean mascara = sigilo.viewLevel() == ProcessoMalhaViewLevel.RESTRITO;
        return new ProcessoMalhaWidgetResponse(
                widget.code(),
                widget.title(),
                widget.kind(),
                widget.status(),
                widget.accentColor(),
                mascara ? "Conteúdo restrito pela malha e sigilo contextual" : widget.headline(),
                mascara ? "Realize step-up para expandir o detalhe" : widget.subtitle(),
                mascara ? List.of("Visualização resumida liberada", "Expansão condicionada a credencial reforçada") : widget.insights(),
                mascara ? "" : widget.navigationPath()
        );
    }

    private ProcessoMalhaRotaTaticaItemResponse rotaItem(ProcessoPainelRotaTaticaItem item,
                                                         ProcessoMalhaSigiloContexto sigilo) {
        boolean mascara = sigilo.viewLevel() == ProcessoMalhaViewLevel.RESTRITO;
        return new ProcessoMalhaRotaTaticaItemResponse(
                item.code(),
                item.severity(),
                mascara ? "Fundamento restrito por sigilo contextual" : item.fundamento(),
                mascara ? "REALIZAR_STEP_UP" : item.acao(),
                mascara ? "" : item.navigationPath()
        );
    }

    private String mascararCpf(String cpf, boolean visualizacaoElevada) {
        if (visualizacaoElevada) {
            return cpf == null ? "" : cpf;
        }
        if (cpf == null || cpf.isBlank()) {
            return "";
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "***";
        }
        return "***" + digits.substring(Math.max(0, digits.length() - 4));
    }

    private record ContextoEntrada(Long processoId,
                                   ProcessoMalhaActorContext actor,
                                   ProcessoMalhaSigiloContexto sigilo) {
    }
}
