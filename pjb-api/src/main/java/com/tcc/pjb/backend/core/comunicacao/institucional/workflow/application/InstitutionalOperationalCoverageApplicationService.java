package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.audit.application.InstitutionalCommunicationAuditApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalOperationalCoverageRule;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalOperationalCoverageRuleStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusCoberturaOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoCoberturaOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoFluxoDelegacaoInstitucional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class InstitutionalOperationalCoverageApplicationService {

    private final InstitutionalOperationalCoverageRuleStateRepository repository;
    private final InstitutionalWorkflowApplicationService workflowService;
    private final InstitutionalInboxApplicationService inboxApplicationService;
    private final InstitutionalInboxStateRepository inboxStateRepository;
    private final InstitutionalCommunicationAuditApplicationService auditService;
    private final CurrentUserService currentUserService;
    private final OutboxPublisher outboxPublisher;

    public InstitutionalOperationalCoverageApplicationService(InstitutionalOperationalCoverageRuleStateRepository repository,
                                                             InstitutionalWorkflowApplicationService workflowService,
                                                             InstitutionalInboxApplicationService inboxApplicationService,
                                                             InstitutionalInboxStateRepository inboxStateRepository,
                                                             InstitutionalCommunicationAuditApplicationService auditService,
                                                             CurrentUserService currentUserService,
                                                             OutboxPublisher outboxPublisher) {
        this.repository = Objects.requireNonNull(repository);
        this.workflowService = Objects.requireNonNull(workflowService);
        this.inboxApplicationService = Objects.requireNonNull(inboxApplicationService);
        this.inboxStateRepository = Objects.requireNonNull(inboxStateRepository);
        this.auditService = Objects.requireNonNull(auditService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
    }

    @Transactional
    public InstitutionalOperationalCoverageRule criar(String unidadeCodigo,
                                                      String caixaCodigo,
                                                      Long titularUsuarioId,
                                                      Long coberturaUsuarioId,
                                                      TipoCoberturaOperacionalInstitucional tipoCobertura,
                                                      Set<CapacidadeCaixaInstitucional> capacidades,
                                                      Instant inicioVigencia,
                                                      Instant fimVigencia,
                                                      String motivo,
                                                      String observacoes) {
        Usuario actor = currentUserService.getRequired();
        Instant now = Instant.now();
        String ruleId = UUID.nameUUIDFromBytes((unidadeCodigo + "|" + caixaCodigo + "|" + coberturaUsuarioId + "|" + now).getBytes(StandardCharsets.UTF_8)).toString();
        Set<CapacidadeCaixaInstitucional> granted = capacidades == null || capacidades.isEmpty()
                ? defaultCaps(tipoCobertura)
                : EnumSet.copyOf(capacidades);
        InstitutionalOperationalCoverageRule rule = new InstitutionalOperationalCoverageRule(
                ruleId,
                unidadeCodigo,
                caixaCodigo,
                titularUsuarioId,
                coberturaUsuarioId,
                tipoCobertura,
                granted,
                StatusCoberturaOperacionalInstitucional.ATIVA,
                inicioVigencia == null ? now : inicioVigencia,
                fimVigencia,
                motivo,
                observacoes,
                now,
                now,
                hash("coverage_create", unidadeCodigo, caixaCodigo, titularUsuarioId, coberturaUsuarioId, now)
        );
        repository.save(rule);
        emit("INSTITUTIONAL_OPERATIONAL_COVERAGE_CREATED", rule.ruleId(), Map.of("unidadeCodigo", unidadeCodigo, "caixaCodigo", caixaCodigo, "tipoCobertura", tipoCobertura.name(), "coberturaUsuarioId", coberturaUsuarioId));
        return rule;
    }

    @PjbTransactionalBudget(operation = "institucional.operational-coverage.listar", maxMillis = 3000)
    @Transactional(readOnly = true)
    public List<InstitutionalOperationalCoverageRule> listar(String unidadeCodigo) {
        List<InstitutionalOperationalCoverageRule> rules = unidadeCodigo == null || unidadeCodigo.isBlank() ? repository.findAll() : repository.findByUnidadeCodigo(unidadeCodigo);
        Instant now = Instant.now();
        return rules.stream()
                .filter(rule -> rule.status() != StatusCoberturaOperacionalInstitucional.ENCERRADA || rule.ativaEm(now))
                .toList();
    }

    @Transactional
    public List<com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDelegationAssignment> aplicarAtivas(String expedicaoUuid, String motivoComplementar) {
        InstitutionalInboxItem item = inboxStateRepository.findByExpedicaoUuid(expedicaoUuid)
                .orElseThrow(() -> new com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException("InstitutionalInboxItem", expedicaoUuid));
        Instant now = Instant.now();
        List<InstitutionalOperationalCoverageRule> applicable = repository.findByUnidadeCodigo(item.unidadeCodigo()).stream()
                .filter(rule -> rule.caixaCodigo().equalsIgnoreCase(item.caixaCodigoAtual()))
                .filter(rule -> rule.ativaEm(now))
                .toList();
        return applicable.stream().map(rule -> applyRule(item, rule, motivoComplementar)).toList();
    }

    private com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDelegationAssignment applyRule(InstitutionalInboxItem item,
                                                                                                                             InstitutionalOperationalCoverageRule rule,
                                                                                                                             String motivoComplementar) {
        String motivo = buildMotivo(rule, motivoComplementar);
        Integer horas = rule.fimVigencia() == null ? 72 : Math.max(1, (int) ChronoUnit.HOURS.between(Instant.now(), rule.fimVigencia()));
        if (rule.tipoCobertura() == TipoCoberturaOperacionalInstitucional.SUBSTITUICAO_PROGRAMADA || rule.tipoCobertura() == TipoCoberturaOperacionalInstitucional.PLANTAO) {
            return workflowService.substituir(item.expedicaoUuid(), rule.coberturaUsuarioId(), horas, motivo);
        }
        return workflowService.delegar(item.expedicaoUuid(), rule.coberturaUsuarioId(), rule.capacidades(), horas, motivo);
    }

    private Set<CapacidadeCaixaInstitucional> defaultCaps(TipoCoberturaOperacionalInstitucional tipoCobertura) {
        return switch (tipoCobertura) {
            case DELEGACAO_PROGRAMADA -> EnumSet.of(
                    CapacidadeCaixaInstitucional.VISUALIZAR,
                    CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                    CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                    CapacidadeCaixaInstitucional.DAR_CIENCIA,
                    CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA);
            case SUBSTITUICAO_PROGRAMADA, PLANTAO -> EnumSet.of(
                    CapacidadeCaixaInstitucional.VISUALIZAR,
                    CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                    CapacidadeCaixaInstitucional.DAR_CIENCIA,
                    CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                    CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                    CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO,
                    CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO,
                    CapacidadeCaixaInstitucional.DEVOLVER_PARA_FILA,
                    CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR);
        };
    }

    private String buildMotivo(InstitutionalOperationalCoverageRule rule, String detalhe) {
        String base = "cobertura_operacional=" + rule.tipoCobertura().name() + ";ruleId=" + rule.ruleId();
        return detalhe == null || detalhe.isBlank() ? base : base + ";" + detalhe.trim();
    }

    private String hash(Object... values) {
        return Hashes.sha256Hex(java.util.Arrays.stream(values).map(String::valueOf).collect(java.util.stream.Collectors.joining("|")));
    }

    private void emit(String type, String aggregateId, Map<String, Object> payload) {
        outboxPublisher.enqueueTracked(
                "processual.comunicacao.institucional",
                type,
                payload,
                Map.of("aggregateType", "INSTITUTIONAL_OPERATIONAL_COVERAGE", "aggregateId", aggregateId),
                "institutional:" + type + ":" + aggregateId,
                "INSTITUTIONAL_OPERATIONAL_COVERAGE",
                aggregateId
        );
    }
}
