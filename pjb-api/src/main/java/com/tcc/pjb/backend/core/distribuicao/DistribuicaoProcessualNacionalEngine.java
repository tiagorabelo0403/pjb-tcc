package com.tcc.pjb.backend.core.distribuicao;

import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService;
import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService.RoutingCommand;
import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService.RoutingDecision;
import com.tcc.pjb.backend.core.util.DeterministicUuid;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class DistribuicaoProcessualNacionalEngine {

    private final NationalProcessRoutingService nationalProcessRoutingService;
    private final DistributionConstraintSnapshotService distributionConstraintSnapshotService;
    private final DistributionGovernanceResolver distributionGovernanceResolver;
    private final DistribuicaoProcessualTrackSupport distribuicaoProcessualTrackSupport;
    private final DistribuicaoProcessualProcessoSupport distribuicaoProcessualProcessoSupport;
    private final ProcessoRepository processoRepository;

    public DistribuicaoProcessualNacionalEngine(NationalProcessRoutingService nationalProcessRoutingService,
                                                DistributionConstraintSnapshotService distributionConstraintSnapshotService,
                                                DistributionGovernanceResolver distributionGovernanceResolver,
                                                DistribuicaoProcessualTrackSupport distribuicaoProcessualTrackSupport,
                                                DistribuicaoProcessualProcessoSupport distribuicaoProcessualProcessoSupport,
                                                ObjectProvider<ProcessoRepository> processoRepositoryProvider) {
        this.nationalProcessRoutingService = Objects.requireNonNull(nationalProcessRoutingService);
        this.distributionConstraintSnapshotService = Objects.requireNonNull(distributionConstraintSnapshotService);
        this.distributionGovernanceResolver = Objects.requireNonNull(distributionGovernanceResolver);
        this.distribuicaoProcessualTrackSupport = Objects.requireNonNull(distribuicaoProcessualTrackSupport);
        this.distribuicaoProcessualProcessoSupport = Objects.requireNonNull(distribuicaoProcessualProcessoSupport);
        this.processoRepository = processoRepositoryProvider.getIfAvailable();
    }

    public DistribuicaoResult distribuir(DistribuicaoRequest request) {
        DistributionAssessment assessment = assess(request);
        return new DistribuicaoResult(
                assessment.workItemId(),
                assessment.status(),
                assessment.filaDistribuicao(),
                assessment.inboxKey(),
                assessment.varaDestino(),
                assessment.comarcaDestino(),
                assessment.trilhoCompetencia()
        );
    }

    public DistributionSnapshot snapshotInicial(Processo processo) {
        if (processo == null) {
            return null;
        }
        return toSnapshot(assess(distribuicaoProcessualProcessoSupport.buildFromProcesso(processo)));
    }

    public Map<String, Object> redistribuirPorImpedimento(Long processoId, String motivoImpedimento) {
        String reason = firstNonBlank(motivoImpedimento, "IMPEDIMENTO_NAO_INFORMADO");
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("processoId", processoId);
        payload.put("motivo", reason);
        payload.put("status", "REDISTRIBUIDO_IMPEDIMENTO");
        payload.put("fila", "FILA_REDISTRIBUICAO_IMPEDIMENTO");
        payload.put("inbox", "INBOX_REDISTRIBUICAO_IMPEDIMENTO");
        payload.put("descriptor", "REDISTRIBUICAO:IMPEDIMENTO:REVISAO_REFORCADA");
        payload.put("priority", 0);
        payload.put("reviewChecklist", List.of(
                "Bloquear prevento anterior antes do novo sorteio.",
                "Preservar rastreabilidade do motivo de impedimento ou suspeição.",
                "Homologar a nova trilha sem relator ou unidade contaminados."
        ));
        payload.put("workItemId", Math.abs(DeterministicUuid.v5("pjb-redistribuicao", String.valueOf(processoId) + '|' + reason).getMostSignificantBits()));
        return Collections.unmodifiableMap(payload);
    }

    public Map<String, Object> consultarDistribuicao(String numeroProcesso) {
        String numeroNormalizado = normalizeText(numeroProcesso);
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("numeroProcesso", numeroNormalizado);
        if (numeroNormalizado == null) {
            payload.put("encontrado", false);
            payload.put("status", "NUMERO_PROCESSUAL_AUSENTE");
            payload.put("trilhoCompetencia", "TRILHO_DISTRIBUICAO_NACIONAL");
            return Collections.unmodifiableMap(payload);
        }

        Processo processo = processoRepository == null ? null : processoRepository.findByNumero(numeroNormalizado).orElse(null);
        if (processo == null) {
            payload.put("encontrado", false);
            payload.put("status", "CONSULTA_SEM_PROCESSO_REGISTRADO");
            payload.put("trilhoCompetencia", "TRILHO_DISTRIBUICAO_NACIONAL");
            payload.put("foro", "FORO_NAO_ENCONTRADO");
            payload.put("fila", "FILA_PRE_DISTRIBUICAO");
            payload.put("reviewChecklist", List.of(
                    "Rodar diagnóstico pré-distribuição para competência territorial e material.",
                    "Conferir prevenção, dependência, conexão ou continência antes do sorteio.",
                    "Confirmar rito, classe, assunto e sigilo antes da autuação final."
            ));
            return Collections.unmodifiableMap(payload);
        }

        DistributionAssessment assessment = assess(distribuicaoProcessualProcessoSupport.buildFromProcesso(processo));
        payload.put("processoId", processo.getId());
        payload.put("encontrado", true);
        payload.put("status", firstNonBlank(processo.getPreProtocoloStatus(), assessment.status()));
        payload.put("trilhoCompetencia", assessment.trilhoCompetencia());
        payload.put("foro", firstNonBlank(assessment.routing().foroSugerido(), processo.getTribunalCodigoRoteado(), processo.getTribunal(), assessment.routing().tribunalCodigo()));
        payload.put("vara", firstNonBlank(processo.getUnidadeJudiciariaCodigo(), assessment.varaDestino(), processo.getVara()));
        payload.put("comarca", firstNonBlank(processo.getComarca(), assessment.comarcaDestino(), assessment.routing().comarcaSugerida()));
        payload.put("uf", firstNonBlank(processo.getUf(), assessment.request().uf(), assessment.routing().metadata() != null ? stringMetadata(assessment.routing().metadata(), "territorial.uf") : null));
        payload.put("tribunalCodigoRoteado", firstNonBlank(processo.getTribunalCodigoRoteado(), assessment.routing().tribunalCodigo()));
        payload.put("unidadeJudiciariaCodigo", firstNonBlank(processo.getUnidadeJudiciariaCodigo(), assessment.routing().unidadeJudiciariaCodigo(), assessment.varaDestino()));
        payload.put("ultimaFilaDistribuicao", assessment.filaDistribuicao());
        payload.put("ultimaInboxKey", assessment.inboxKey());
        payload.put("ultimaPrioridade", assessment.priority());
        payload.put("routingRiskLevel", firstNonBlank(processo.getRoutingRiskLevel(), assessment.routing().routingRiskLevel()));
        payload.put("specializedTrack", assessment.specializedTrack());
        payload.put("connectorSystem", firstNonBlank(processo.getConnectorSystem(), assessment.routing().sistemaPrimario()));
        payload.put("competenciaTerritorialModo", firstNonBlank(processo.getCompetenciaTerritorialModo(), assessment.routing().territorialMode()));
        payload.put("preventionMode", firstNonBlank(processo.getPreventionMode(), assessment.routing().preventionMode(), assessment.constraintSnapshot().relationMode()));
        payload.put("faseAtual", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null);
        payload.put("ultimoStatusOperacional", firstNonBlank(processo.getSubmissionBlueprintStatus(), processo.getConnectorSubmissionStatus(), assessment.status()));
        payload.put("ultimoPrazoOperacional", assessment.routing().prazoTriagemHoras());
        payload.put("ultimoWorkItemId", assessment.workItemId());
        payload.put("distributionDescriptor", assessment.governance().descriptor());
        payload.put("constraintDescriptor", assessment.constraintSnapshot().descriptor());
        payload.put("reviewChecklist", assessment.reviewChecklist());
        payload.put("alertas", assessment.alertas());
        payload.put("fundamentos", assessment.fundamentos());
        return Collections.unmodifiableMap(payload);
    }

    public Map<String, Object> diagnosticarCompetencia(DiagnosticoRequest request) {
        DistributionAssessment assessment = assess(toDistribuicaoRequest(request));
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("numeroProcesso", assessment.request().numeroProcesso());
        payload.put("rito", assessment.request().rito() != null ? assessment.request().rito().name() : null);
        payload.put("grau", assessment.request().grauJurisdicao() != null ? assessment.request().grauJurisdicao().name() : null);
        payload.put("comarca", assessment.comarcaDestino());
        payload.put("uf", firstNonBlank(assessment.request().uf(), stringMetadata(assessment.routing().metadata(), "territorial.uf")));
        payload.put("preventoNumero", assessment.request().preventoNumero());
        payload.put("conexoNumero", assessment.request().conexoNumero());
        payload.put("dependencia", assessment.request().temDependencia());
        payload.put("conexao", assessment.request().temConexao());
        payload.put("continencia", assessment.request().temContinencia());
        payload.put("sigiloReforcado", assessment.request().sigiloReforcado());
        payload.put("status", assessment.status());
        payload.put("filaDistribuicao", assessment.filaDistribuicao());
        payload.put("inboxKey", assessment.inboxKey());
        payload.put("varaDestino", assessment.varaDestino());
        payload.put("priority", assessment.priority());
        payload.put("routingRiskLevel", assessment.routing().routingRiskLevel());
        payload.put("specializedTrack", assessment.specializedTrack());
        payload.put("territorialMode", assessment.routing().territorialMode());
        payload.put("preventionMode", assessment.routing().preventionMode());
        payload.put("allocationStrategy", assessment.routing().allocationStrategy());
        payload.put("linkageMode", assessment.routing().linkageMode());
        payload.put("constraintSnapshot", assessment.constraintSnapshot().toMap());
        payload.put("governance", assessment.governance().toMap());
        payload.put("alertas", assessment.alertas());
        payload.put("fundamentos", assessment.fundamentos());
        payload.put("reviewChecklist", assessment.reviewChecklist());
        payload.put("routing", sanitizeRoutingMetadata(assessment.routing()));
        return Collections.unmodifiableMap(payload);
    }

    private DistributionAssessment assess(DistribuicaoRequest request) {
        DistribuicaoRequest normalized = request == null ? new DistribuicaoRequest(null, null, null, null, 0d, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false, false, false, false, false, false, false, false, false, false, false) : request;
        RoutingDecision routing = nationalProcessRoutingService.route(toRoutingCommand(normalized));
        DistributionConstraintSnapshot constraintSnapshot = distributionConstraintSnapshotService.resolve(
                routing.metadata(),
                normalized.numeroProcesso(),
                normalized.preventoNumero(),
                normalized.conexoNumero()
        );
        DistributionGovernanceProfile governance = distributionGovernanceResolver.resolve(normalized, routing, constraintSnapshot);
        String comarcaDestino = firstNonBlank(routing.comarcaSugerida(), normalized.comarca(), normalized.cidade(), "COMARCA_PADRAO");
        String specializedTrack = distribuicaoProcessualTrackSupport.resolveSpecializedTrack(normalized, routing);
        String varaDestino = resolveVaraDestino(normalized, routing, constraintSnapshot, governance);
        String filaDistribuicao = resolveFilaDistribuicao(normalized, routing, constraintSnapshot, governance, specializedTrack);
        String inboxKey = resolveInboxKey(normalized, routing, governance, specializedTrack);
        String status = resolveStatus(normalized, routing, constraintSnapshot, specializedTrack);
        int priority = resolvePriority(normalized, routing, constraintSnapshot, governance, specializedTrack);
        long workItemId = Math.abs(DeterministicUuid.v5(
                "pjb-distribuicao-work-item",
                String.join("|",
                        firstNonBlank(normalized.numeroProcesso(), "SEM_NUMERO"),
                        status,
                        filaDistribuicao,
                        inboxKey,
                        firstNonBlank(varaDestino, "SEM_UNIDADE"),
                        governance.descriptor(),
                        constraintSnapshot.descriptor())
        ).getMostSignificantBits());
        LinkedHashSet<String> alertas = new LinkedHashSet<>(safeList(routing.alertas()));
        alertas.addAll(governance.warnings());
        alertas.addAll(distribuicaoProcessualTrackSupport.buildSpecializedAlertas(normalized, routing, specializedTrack));
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(safeList(routing.fundamentos()));
        fundamentos.addAll(governance.fundamentos());
        fundamentos.addAll(distribuicaoProcessualTrackSupport.buildSpecializedFundamentos(normalized, routing, specializedTrack));
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>(safeList(routing.reviewChecklist()));
        reviewChecklist.addAll(governance.reviewChecklist());
        reviewChecklist.addAll(distribuicaoProcessualTrackSupport.buildSpecializedReviewChecklist(normalized, routing, specializedTrack));
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("routing", sanitizeRoutingMetadata(routing));
        metadata.put("governance", governance.toMap());
        metadata.put("constraint", constraintSnapshot.toMap());
        metadata.put("priority", priority);
        metadata.put("status", status);
        metadata.put("specializedTrack", specializedTrack);
        metadata.put("filaDistribuicao", filaDistribuicao);
        metadata.put("inboxKey", inboxKey);
        metadata.put("varaDestino", varaDestino);
        metadata.put("comarcaDestino", comarcaDestino);
        return new DistributionAssessment(
                normalized,
                routing,
                constraintSnapshot,
                governance,
                specializedTrack,
                status,
                filaDistribuicao,
                inboxKey,
                varaDestino,
                comarcaDestino,
                resolveTrilhoCompetencia(routing, governance, constraintSnapshot),
                priority,
                workItemId,
                List.copyOf(alertas),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private RoutingCommand toRoutingCommand(DistribuicaoRequest request) {
        return new RoutingCommand(
                request.rito(),
                inferRamoDireito(request),
                request.grauJurisdicao(),
                request.uf(),
                request.comarca(),
                BigDecimal.valueOf(Math.max(0d, request.valorCausa())).setScale(2, RoundingMode.HALF_UP),
                firstNonBlank(request.classeProcessual(), request.classeProcessualLocal(), request.classeMacro()),
                firstNonBlank(request.assunto(), request.assuntoLocal(), request.assuntoMacro()),
                Instant.now(),
                request.numeroProcesso(),
                request.cidade(),
                request.foro(),
                request.juizoOrigem(),
                request.orgaoJulgador(),
                request.areaEspecializada(),
                null,
                null,
                null,
                null,
                request.preventoNumero(),
                request.conexoNumero(),
                normalizeToken(firstNonBlank(request.juizoOrigem(), request.foro(), request.orgaoJulgador())),
                request.temDependencia(),
                request.temConexao(),
                request.temContinencia(),
                request.pedidoLiminar(),
                request.plantaoJudicial(),
                request.segredoSolicitado() || request.sigiloReforcado(),
                request.redistribuicaoImpedimento()
        );
    }

    private DistributionSnapshot toSnapshot(DistributionAssessment assessment) {
        if (assessment == null) {
            return null;
        }
        return new DistributionSnapshot(
                assessment.request().numeroProcesso(),
                assessment.specializedTrack(),
                assessment.status(),
                assessment.filaDistribuicao(),
                assessment.inboxKey(),
                assessment.varaDestino(),
                assessment.comarcaDestino(),
                assessment.trilhoCompetencia(),
                assessment.priority(),
                assessment.routing().tribunalCodigo(),
                assessment.routing().sistemaPrimario(),
                assessment.routing().territorialMode(),
                assessment.routing().preventionMode(),
                assessment.routing().linkageMode(),
                assessment.routing().routingRiskLevel(),
                assessment.alertas(),
                assessment.fundamentos(),
                assessment.reviewChecklist(),
                assessment.metadata()
        );
    }

    private DistribuicaoRequest toDistribuicaoRequest(DiagnosticoRequest request) {
        if (request == null) {
            return new DistribuicaoRequest(null, null, null, null, 0d, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false, false, false, false, false, false, false, false, false, false, false);
        }
        return new DistribuicaoRequest(
                firstNonBlank(request.preventoNumero(), request.conexoNumero(), request.classeProcessual(), request.assunto()),
                request.uf(),
                request.comarca(),
                request.rito(),
                request.valorCausa(),
                request.cidadeAutor(),
                request.cidadeReu(),
                request.grauJurisdicao(),
                request.cidade(),
                request.foro(),
                request.juizoOrigem(),
                request.orgaoJulgador(),
                request.assunto(),
                request.classeProcessual(),
                request.classeProcessual(),
                request.assunto(),
                request.areaEspecializada(),
                request.preventoNumero(),
                request.conexoNumero(),
                request.classeProcessual(),
                request.assunto(),
                request.temDependencia(),
                request.temConexao(),
                request.temContinencia(),
                request.urgente(),
                request.prioridadeLegal(),
                request.sigiloReforcado(),
                request.escaladoGovernanca()
        );
    }


    private String resolveVaraDestino(DistribuicaoRequest request,
                                      RoutingDecision routing,
                                      DistributionConstraintSnapshot constraintSnapshot,
                                      DistributionGovernanceProfile governance) {
        String linkedTarget = firstNonBlank(
                routing.unidadeJudiciariaCodigo(),
                routing.orgaoJulgadorSugerido(),
                request.orgaoJulgador(),
                request.juizoOrigem());
        if (request.redistribuicaoImpedimento()) {
            return firstNonBlank(linkedTarget, normalizeToken(governance.incidentDesk()), "UNIDADE_REDISTRIBUICAO_CONTROLADA");
        }
        if (constraintSnapshot.reviewRequired() || !"AUTONOMA".equals(firstNonBlank(routing.linkageMode(), constraintSnapshot.relationMode(), "AUTONOMA"))) {
            return firstNonBlank(linkedTarget, normalizeToken(request.preventoNumero()), normalizeToken(request.conexoNumero()), "UNIDADE_PREVENTO_ASSISTIDO");
        }
        return firstNonBlank(linkedTarget, normalizeToken(request.foro()), "UNIDADE_DISTRIBUICAO_NACIONAL");
    }

    private String resolveFilaDistribuicao(DistribuicaoRequest request,
                                           RoutingDecision routing,
                                           DistributionConstraintSnapshot constraintSnapshot,
                                           DistributionGovernanceProfile governance,
                                           String specializedTrack) {
        String anchor = normalizeToken(firstNonBlank(governance.queueLane(), routing.foroSugerido(), routing.comarcaSugerida(), request.comarca(), "BASE"));
        if (request.redistribuicaoImpedimento()) {
            return "FILA_REDISTRIBUICAO_IMPEDIMENTO_" + anchor;
        }
        if (request.plantaoJudicial()) {
            return "FILA_PLANTAO_" + anchor;
        }
        String queueSegment = distribuicaoProcessualTrackSupport.resolveQueueSegment(specializedTrack, anchor);
        if (isSpecializedTrack(specializedTrack)) {
            return "FILA_" + queueSegment + "_" + anchor;
        }
        if (request.segredoSolicitado() || request.sigiloReforcado() || routing.sigiloPadrao()) {
            return "FILA_SIGILO_" + anchor;
        }
        if (constraintSnapshot.reviewRequired() || !"CONTROLADO".equals(firstNonBlank(routing.routingRiskLevel(), "CONTROLADO"))) {
            return "FILA_REVISAO_" + queueSegment;
        }
        if (!"AUTONOMA".equals(firstNonBlank(routing.linkageMode(), "AUTONOMA"))) {
            return "FILA_PREVENCAO_" + queueSegment;
        }
        return "FILA_" + queueSegment;
    }

    private String resolveInboxKey(DistribuicaoRequest request,
                                   RoutingDecision routing,
                                   DistributionGovernanceProfile governance,
                                   String specializedTrack) {
        String anchor = normalizeToken(firstNonBlank(
                routing.suggestedDeskProfile(),
                governance.incidentDesk(),
                governance.auditDesk(),
                routing.mesaTriagem(),
                request.foro(),
                routing.comarcaSugerida(),
                "BASE"));
        if (request.redistribuicaoImpedimento()) {
            return "INBOX_REDISTRIBUICAO_" + anchor;
        }
        if (request.segredoSolicitado() || request.sigiloReforcado() || routing.sigiloPadrao()) {
            return "INBOX_SIGILO_" + anchor;
        }
        if (request.plantaoJudicial() || request.pedidoLiminar()) {
            return "INBOX_PRIORIDADE_" + anchor;
        }
        if (!"AUTONOMA".equals(firstNonBlank(routing.linkageMode(), "AUTONOMA"))) {
            return "INBOX_DISTRIBUICAO_" + anchor;
        }
        String inboxSegment = distribuicaoProcessualTrackSupport.resolveInboxSegment(specializedTrack, anchor);
        if (isSpecializedTrack(specializedTrack)) {
            return "INBOX_" + inboxSegment + "_" + anchor;
        }
        return "INBOX_" + inboxSegment;
    }

    private String resolveStatus(DistribuicaoRequest request,
                                 RoutingDecision routing,
                                 DistributionConstraintSnapshot constraintSnapshot,
                                 String specializedTrack) {
        if (request.redistribuicaoImpedimento()) {
            return "REDISTRIBUICAO_CONTROLADA_POR_IMPEDIMENTO";
        }
        if (!"AUTONOMA".equals(firstNonBlank(routing.linkageMode(), constraintSnapshot.relationMode(), "AUTONOMA")) && constraintSnapshot.reviewRequired()) {
            return "REDIRECIONAMENTO_PREVENTIVO_EM_REVISAO";
        }
        if (!"AUTONOMA".equals(firstNonBlank(routing.linkageMode(), constraintSnapshot.relationMode(), "AUTONOMA"))) {
            return "REDIRECIONADO_POR_RELACAO_PROCESSUAL";
        }
        if (constraintSnapshot.reviewRequired() || "CRITICO".equals(firstNonBlank(routing.routingRiskLevel(), null))) {
            return "PENDENTE_REVISAO_DISTRIBUICAO";
        }
        if (request.plantaoJudicial() || request.pedidoLiminar() || request.urgente()) {
            return "DISTRIBUICAO_PRIORITARIA";
        }
        if ("CUSTODIA".equals(specializedTrack)) {
            return "DISTRIBUICAO_PRIORIDADE_CUSTODIA";
        }
        if (!"CONHECIMENTO_GERAL".equals(specializedTrack)) {
            return "DISTRIBUICAO_ESPECIALIZADA_" + specializedTrack;
        }
        return "DISTRIBUIDO";
    }

    private boolean isSpecializedTrack(String specializedTrack) {
        String track = firstNonBlank(specializedTrack, "CONHECIMENTO_GERAL");
        return !"CONHECIMENTO_GERAL".equals(track) && !"CONHECIMENTO".equals(track);
    }

    private int resolvePriority(DistribuicaoRequest request,
                                RoutingDecision routing,
                                DistributionConstraintSnapshot constraintSnapshot,
                                DistributionGovernanceProfile governance,
                                String specializedTrack) {
        int base = governance.priorityFloor();
        if (request.redistribuicaoImpedimento()) {
            return 0;
        }
        if (request.plantaoJudicial()) {
            return 0;
        }
        if (request.pedidoLiminar() || request.urgente()) {
            return Math.min(base, 1);
        }
        if (constraintSnapshot.reviewRequired() || "CRITICO".equals(firstNonBlank(routing.routingRiskLevel(), null))) {
            return Math.min(base, 1);
        }
        if (!"AUTONOMA".equals(firstNonBlank(routing.linkageMode(), "AUTONOMA"))) {
            return Math.min(base, 2);
        }
        if ("CUSTODIA".equals(specializedTrack)) {
            return 0;
        }
        if ("TRIBUNAL_JURI".equals(specializedTrack)
                || specializedTrack.startsWith("JUIZADO")
                || "TRABALHISTA".equals(specializedTrack)
                || "EXECUCAO_FISCAL".equals(specializedTrack)
                || "EXECUCAO_PENAL".equals(specializedTrack)
                || "ELEITORAL".equals(specializedTrack)
                || "INFANCIA_JUVENTUDE".equals(specializedTrack)
                || "FAMILIA_SUCESSOES".equals(specializedTrack)
                || "AMBIENTAL".equals(specializedTrack)
                || "AGRARIO".equals(specializedTrack)
                || "EMPRESARIAL".equals(specializedTrack)
                || "ADMINISTRATIVO_IMPROBIDADE".equals(specializedTrack)
                || "INTERNACIONAL".equals(specializedTrack)
                || "CONSTITUCIONAL".equals(specializedTrack)) {
            return Math.min(base, 2);
        }
        return Math.max(base, 3);
    }

    private String resolveTrilhoCompetencia(RoutingDecision routing,
                                            DistributionGovernanceProfile governance,
                                            DistributionConstraintSnapshot constraintSnapshot) {
        return firstNonBlank(routing.competenceEnvelope(), governance.descriptor(), constraintSnapshot.descriptor(), "TRILHO_DISTRIBUICAO_NACIONAL");
    }

    private RamoDireito inferRamoDireito(DistribuicaoRequest request) {
        if (request == null || request.rito() == null) {
            return null;
        }
        return request.rito().suggestedRamo();
    }


    private Map<String, Object> sanitizeRoutingMetadata(RoutingDecision routing) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (routing == null) {
            return Map.of();
        }
        out.put("tribunalCodigo", routing.tribunalCodigo());
        out.put("tribunalNome", routing.tribunalNome());
        out.put("ramoJusticaNacional", routing.ramoJusticaNacional());
        out.put("sistemaPrimario", routing.sistemaPrimario());
        out.put("instancia", routing.instancia());
        out.put("orgaoJulgadorSugerido", routing.orgaoJulgadorSugerido());
        out.put("unidadeJudiciariaCodigo", routing.unidadeJudiciariaCodigo());
        out.put("filaDistribuicao", routing.filaDistribuicao());
        out.put("territorialMode", routing.territorialMode());
        out.put("preventionMode", routing.preventionMode());
        out.put("distributionMode", routing.distributionMode());
        out.put("allocationStrategy", routing.allocationStrategy());
        out.put("linkageMode", routing.linkageMode());
        out.put("competenceEnvelope", routing.competenceEnvelope());
        out.put("routingRiskLevel", routing.routingRiskLevel());
        out.put("suggestedDeskProfile", routing.suggestedDeskProfile());
        out.put("mesaTriagem", routing.mesaTriagem());
        out.put("cidadeSugerida", routing.cidadeSugerida());
        out.put("comarcaSugerida", routing.comarcaSugerida());
        out.put("foroSugerido", routing.foroSugerido());
        out.put("secaoJudiciariaSugerida", routing.secaoJudiciariaSugerida());
        out.put("subsecaoJudiciariaSugerida", routing.subsecaoJudiciariaSugerida());
        out.put("circunscricaoJudiciariaSugerida", routing.circunscricaoJudiciariaSugerida());
        out.put("prazoTriagemHoras", routing.prazoTriagemHoras());
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }


    private String stringMetadata(Map<String, Object> metadata, String dottedPath) {
        if (metadata == null || dottedPath == null || dottedPath.isBlank()) {
            return null;
        }
        Object current = metadata;
        for (String token : dottedPath.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(token);
            if (current == null) {
                return null;
            }
        }
        if (current instanceof String value) {
            return normalizeText(value);
        }
        return current == null ? null : normalizeText(String.valueOf(current));
    }

    private static boolean containsAny(String value, String... needles) {
        String normalized = normalizeToken(value);
        if (normalized == null || needles == null) {
            return false;
        }
        for (String needle : needles) {
            String token = normalizeToken(needle);
            if (token != null && normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeUf(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? null : normalized;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public record DistribuicaoRequest(String numeroProcesso,
                                      String uf,
                                      String comarca,
                                      RitoProcessual rito,
                                      double valorCausa,
                                      String autor,
                                      String reu,
                                      GrauJurisdicao grauJurisdicao,
                                      String cidade,
                                      String foro,
                                      String juizoOrigem,
                                      String orgaoJulgador,
                                      String assuntoMacro,
                                      String classeMacro,
                                      String classeProcessualLocal,
                                      String assuntoLocal,
                                      String areaEspecializada,
                                      String preventoNumero,
                                      String conexoNumero,
                                      String classeProcessual,
                                      String assunto,
                                      boolean temDependencia,
                                      boolean temConexao,
                                      boolean temContinencia,
                                      boolean urgente,
                                      boolean prioridadeLegal,
                                      boolean sigiloReforcado,
                                      boolean escaladoGovernanca,
                                      boolean redistribuicaoImpedimento,
                                      boolean plantaoJudicial,
                                      boolean pedidoLiminar,
                                      boolean segredoSolicitado) {

        public DistribuicaoRequest {
            numeroProcesso = normalizeText(numeroProcesso);
            uf = normalizeUf(uf);
            comarca = normalizeText(comarca);
            rito = rito == null ? RitoProcessual.COMUM_ORDINARIO : rito;
            autor = normalizeText(autor);
            reu = normalizeText(reu);
            grauJurisdicao = grauJurisdicao == null ? GrauJurisdicao.PRIMEIRO_GRAU : grauJurisdicao;
            cidade = normalizeText(cidade);
            foro = normalizeText(foro);
            juizoOrigem = normalizeText(juizoOrigem);
            orgaoJulgador = normalizeText(orgaoJulgador);
            assuntoMacro = normalizeText(assuntoMacro);
            classeMacro = normalizeText(classeMacro);
            classeProcessualLocal = normalizeText(classeProcessualLocal);
            assuntoLocal = normalizeText(assuntoLocal);
            areaEspecializada = normalizeText(areaEspecializada);
            preventoNumero = normalizeText(preventoNumero);
            conexoNumero = normalizeText(conexoNumero);
            classeProcessual = normalizeText(classeProcessual);
            assunto = normalizeText(assunto);
            urgente = urgente || plantaoJudicial || pedidoLiminar;
            sigiloReforcado = sigiloReforcado || segredoSolicitado;
            escaladoGovernanca = escaladoGovernanca || redistribuicaoImpedimento;
        }

        public DistribuicaoRequest(String numeroProcesso,
                                   String uf,
                                   String comarca,
                                   RitoProcessual rito,
                                   double valorCausa,
                                   String autor,
                                   String reu,
                                   GrauJurisdicao grauJurisdicao,
                                   String cidade,
                                   String foro,
                                   String juizoOrigem,
                                   String orgaoJulgador,
                                   String assuntoMacro,
                                   String classeMacro,
                                   String classeProcessualLocal,
                                   String assuntoLocal,
                                   String areaEspecializada,
                                   String preventoNumero,
                                   String conexoNumero,
                                   String classeProcessual,
                                   String assunto,
                                   boolean temDependencia,
                                   boolean temConexao,
                                   boolean temContinencia,
                                   boolean urgente,
                                   boolean prioridadeLegal,
                                   boolean sigiloReforcado,
                                   boolean escaladoGovernanca) {
            this(numeroProcesso,
                    uf,
                    comarca,
                    rito,
                    valorCausa,
                    autor,
                    reu,
                    grauJurisdicao,
                    cidade,
                    foro,
                    juizoOrigem,
                    orgaoJulgador,
                    assuntoMacro,
                    classeMacro,
                    classeProcessualLocal,
                    assuntoLocal,
                    areaEspecializada,
                    preventoNumero,
                    conexoNumero,
                    classeProcessual,
                    assunto,
                    temDependencia,
                    temConexao,
                    temContinencia,
                    urgente,
                    prioridadeLegal,
                    sigiloReforcado,
                    escaladoGovernanca,
                    false,
                    false,
                    false,
                    false);
        }
    }

    public record DiagnosticoRequest(RitoProcessual rito,
                                     GrauJurisdicao grauJurisdicao,
                                     String comarca,
                                     String uf,
                                     double valorCausa,
                                     String cidade,
                                     String foro,
                                     String juizoOrigem,
                                     String orgaoJulgador,
                                     String areaEspecializada,
                                     String cidadeAutor,
                                     String cidadeReu,
                                     String cidadeFato,
                                     String municipioFato,
                                     String preventoNumero,
                                     String conexoNumero,
                                     String classeProcessual,
                                     String assunto,
                                     boolean temDependencia,
                                     boolean temConexao,
                                     boolean temContinencia,
                                     boolean urgente,
                                     boolean prioridadeLegal,
                                     boolean sigiloReforcado,
                                     boolean escaladoGovernanca) {

        public DiagnosticoRequest {
            rito = rito == null ? RitoProcessual.COMUM_ORDINARIO : rito;
            grauJurisdicao = grauJurisdicao == null ? GrauJurisdicao.PRIMEIRO_GRAU : grauJurisdicao;
            comarca = normalizeText(comarca);
            uf = normalizeUf(uf);
            cidade = normalizeText(cidade);
            foro = normalizeText(foro);
            juizoOrigem = normalizeText(juizoOrigem);
            orgaoJulgador = normalizeText(orgaoJulgador);
            areaEspecializada = normalizeText(areaEspecializada);
            cidadeAutor = normalizeText(cidadeAutor);
            cidadeReu = normalizeText(cidadeReu);
            cidadeFato = normalizeText(cidadeFato);
            municipioFato = normalizeText(municipioFato);
            preventoNumero = normalizeText(preventoNumero);
            conexoNumero = normalizeText(conexoNumero);
            classeProcessual = normalizeText(classeProcessual);
            assunto = normalizeText(assunto);
        }

        public DiagnosticoRequest(RitoProcessual rito,
                                  GrauJurisdicao grauJurisdicao,
                                  String comarca,
                                  String uf,
                                  double valorCausa,
                                  String cidade,
                                  String foro,
                                  String secaoJudiciaria,
                                  String subsecaoJudiciaria,
                                  String circunscricao,
                                  String cidadeAutor,
                                  String cidadeReu,
                                  String cidadeFato,
                                  String municipioFato,
                                  String preventionReference,
                                  String processoReferencia,
                                  String classeProcessual,
                                  String assunto,
                                  boolean dependenciaDeclarada,
                                  boolean conexaoDeclarada,
                                  boolean continenciaDeclarada,
                                  boolean pedidoLiminar,
                                  boolean prioridadeLegal,
                                  boolean segredoSolicitado,
                                  boolean escaladoGovernanca,
                                  boolean redistribuicaoImpedimento,
                                  boolean plantaoJudicial,
                                  boolean urgenteRelacional,
                                  boolean sigiloReforcado) {
            this(rito,
                    grauJurisdicao,
                    comarca,
                    uf,
                    valorCausa,
                    cidade,
                    foro,
                    secaoJudiciaria,
                    subsecaoJudiciaria,
                    circunscricao,
                    cidadeAutor,
                    cidadeReu,
                    cidadeFato,
                    municipioFato,
                    preventionReference,
                    processoReferencia,
                    classeProcessual,
                    assunto,
                    dependenciaDeclarada,
                    conexaoDeclarada,
                    continenciaDeclarada,
                    pedidoLiminar || urgenteRelacional || plantaoJudicial,
                    prioridadeLegal,
                    segredoSolicitado || sigiloReforcado,
                    escaladoGovernanca || redistribuicaoImpedimento);
        }
    }

    private static List<String> safeList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        return input.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    public record DistributionSnapshot(String numeroProcesso,
                                     String specializedTrack,
                                     String status,
                                     String filaDistribuicao,
                                     String inboxKey,
                                     String varaDestino,
                                     String comarcaDestino,
                                     String trilhoCompetencia,
                                     int priority,
                                     String tribunalCodigoRoteado,
                                     String connectorSystem,
                                     String competenciaTerritorialModo,
                                     String preventionMode,
                                     String linkageMode,
                                     String routingRiskLevel,
                                     List<String> alertas,
                                     List<String> fundamentos,
                                     List<String> reviewChecklist,
                                     Map<String, Object> metadata) {
        public DistributionSnapshot {
            alertas = alertas == null ? List.of() : List.copyOf(alertas);
            fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
            reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
        }
    }

    public record DistribuicaoResult(Long workItemId,
                                     String status,
                                     String filaDistribuicao,
                                     String inboxKey,
                                     String varaDestino,
                                     String comarcaDestino,
                                     String trilhoCompetencia) {
    }
}
