package com.tcc.pjb.backend.service.processual.recursal.admissibilidade;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.service.processual.prazo.PrazoProcessualNacionalService;
import com.tcc.pjb.backend.service.processual.recursal.protocolo.RecursalTransmissionProfile;
import com.tcc.pjb.backend.service.processual.recursal.protocolo.RecursalTransmissionResolver;
import com.tcc.pjb.backend.service.recursal.mesh.NationalRecursalMeshService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.processo.recursal.domain.PreclusaoTipo;

@Service
public class RecursalAdmissibilityService {

    private final NationalRecursalMeshService meshService;
    private final PrazoProcessualNacionalService prazoService;
    private final RecursalAdmissibilityResolver profileResolver;
    private final RecursalTransmissionResolver transmissionResolver;

    public RecursalAdmissibilityService(NationalRecursalMeshService meshService,
                                        PrazoProcessualNacionalService prazoService,
                                        RecursalAdmissibilityResolver profileResolver,
                                        RecursalTransmissionResolver transmissionResolver) {
        this.meshService = Objects.requireNonNull(meshService);
        this.prazoService = Objects.requireNonNull(prazoService);
        this.profileResolver = Objects.requireNonNull(profileResolver);
        this.transmissionResolver = Objects.requireNonNull(transmissionResolver);
    }

    public RecursalAdmissibilityDecision avaliar(RecursalAdmissibilityCommand command) {
        Objects.requireNonNull(command);
        if (command.planRequest() == null) {
            throw new IllegalArgumentException("Planejamento recursal obrigatório.");
        }

        RecursalPlanningResult planning = meshService.plan(command.planRequest());
        NationalPrazoEngine.TipoPrazo tipoPrazo = mapPrazo(command.planRequest().species().type());
        PrazoProcessualNacionalService.PrazoProcessualResult prazo = command.dataIntimacao() == null
                ? null
                : prazoService.calcular(new PrazoProcessualNacionalService.CalculoPrazoCommand(
                        command.dataIntimacao(),
                        tipoPrazo,
                        command.planRequest().context().ramo(),
                        mapGrau(command.planRequest().context().instanciaAtual()),
                        command.tribunalCodigo(),
                        command.uf(),
                        command.comarca(),
                        null
                ));

        boolean tempestivo = resolveTempestividade(command, prazo);
        PreclusaoTipo preclusao = resolvePreclusao(command, tempestivo);
        boolean preparoExigido = planning.routePlan().preparo().exigido();
        boolean preparoDispensado = planning.routePlan().preparo().dispensadoPorLeiOuRegimento()
                || command.planRequest().context().justicaGratuitaOuIsencaoLegal()
                || command.preparoDispensado();
        boolean preparoSatisfeito = !preparoExigido || preparoDispensado || command.preparoRecolhido();
        boolean admissivel = preclusao == PreclusaoTipo.NENHUMA && tempestivo && preparoSatisfeito;

        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (!tempestivo) {
            alertas.add("Tempestividade negativa para a data de protocolo informada.");
        }
        if (preparoExigido && !preparoSatisfeito) {
            alertas.add("Preparo obrigatório não satisfeito e sem causa de dispensa reconhecida.");
        }
        if (planning.routePlan().admissibilidade().exigePrequestionamento()) {
            alertas.add("Espécie recursal exige prequestionamento explícito no caso concreto.");
        }
        if (planning.routePlan().admissibilidade().exigeDemonstracaoRepercussaoGeral()) {
            alertas.add("Espécie recursal exige demonstração específica de repercussão geral.");
        }
        if (command.pedidoEfeitoSuspensivo()) {
            alertas.add("Recurso com pedido de efeito suspensivo exige trilha prioritária de exame.");
        }
        if (command.tutelaUrgenciaRecursal()) {
            alertas.add("Recurso com tutela de urgência recursal exige coordenação imediata com gabinete ou plantão.");
        }
        if (command.segredoJustica()) {
            alertas.add("Fluxo recursal deve observar trilha restrita de credencial e minimização de payload.");
        }
        if (command.aceitouDecisaoOuPraticouAtoIncompativel()) {
            alertas.add("Há indício de preclusão lógica por ato incompatível com a insurgência recursal.");
        }
        if (command.recursoAnteriorMesmaEspecieInterposto()) {
            alertas.add("Há indício de preclusão consumativa por recurso anterior da mesma espécie.");
        }
        if (prazo != null) {
            alertas.addAll(prazo.advertencias());
        }

        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Perfil recursal: " + planning.routePlan().profileName());
        fundamentos.add("Autoridade de mérito: " + planning.routePlan().autoridadeJulgamentoMerito().name());
        fundamentos.add("Juízo de admissibilidade na origem: " + (planning.routePlan().admissibilidade().juizoOrigem() ? "SIM" : "NAO"));
        fundamentos.add("Juízo de admissibilidade no destino: " + (planning.routePlan().admissibilidade().juizoDestino() ? "SIM" : "NAO"));
        fundamentos.add("Preparo exigido: " + (preparoExigido ? "SIM" : "NAO"));
        if (prazo != null) {
            fundamentos.add("Vencimento forense calculado: " + prazo.vencimentoForense());
        }

        RecursalAdmissibilityProfile profile = profileResolver.resolve(command, planning, prazo, tempestivo, preparoSatisfeito, preclusao);
        if (profile == null) {
            profile = RecursalAdmissibilityProfile.fallback(
                    planning.routePlan().profileName(),
                    planning.routePlan().prevencao().name(),
                    command.pedidoEfeitoSuspensivo(),
                    preparoExigido,
                    planning.routePlan().admissibilidade().admiteRetratacao(),
                    planning.routePlan().admissibilidade().admiteSobrestamento(),
                    false,
                    false,
                    command.tribunalCodigo()
            );
        }
        RecursalTransmissionProfile transmissionProfile = transmissionResolver.resolve(command, planning, profile);
        if (transmissionProfile == null) {
            transmissionProfile = RecursalTransmissionProfile.fallback(
                    command.segredoJustica() ? "MINIMO" : "COMPLETO",
                    command.tribunalCodigo(),
                    false,
                    false
            );
        }
        profile = new RecursalAdmissibilityProfile(
                profile.secretariaOrigem(),
                profile.secretariaDestino(),
                profile.admissibilityDesk(),
                profile.gabineteDestino(),
                profile.supportDesk(),
                profile.distributionDesk(),
                profile.sessionMode(),
                profile.routingBucket(),
                profile.riskLevel(),
                profile.routeKind(),
                profile.counterReasonsMode(),
                profile.counterReasonsDesk(),
                profile.effectMode(),
                profile.automaticSuspensiveEffect(),
                profile.retratacaoMode(),
                profile.sobrestamentoMode(),
                profile.preparoMode(),
                profile.preventionMode(),
                transmissionProfile.protocolDesk(),
                transmissionProfile.remessaDesk(),
                transmissionProfile.autuacaoDesk(),
                transmissionProfile.integrationChannel(),
                transmissionProfile.credentialMode(),
                transmissionProfile.payloadPolicy(),
                transmissionProfile.transmissionMode(),
                transmissionProfile.queueSuffix(),
                transmissionProfile.reviewDesk(),
                transmissionProfile.ackDesk(),
                transmissionProfile.receiptChannel(),
                transmissionProfile.retryMode(),
                transmissionProfile.evidencePolicy(),
                transmissionProfile.complianceDesk(),
                transmissionProfile.protocolWindow(),
                transmissionProfile.connectorSystem(),
                transmissionProfile.connectorBaseUrl(),
                transmissionProfile.connectorWorkflowMode(),
                transmissionProfile.fallbackMode(),
                transmissionProfile.contingencyDesk(),
                transmissionProfile.replayQueue(),
                transmissionProfile.evidenceRetentionPolicy(),
                transmissionProfile.manualSubmissionDesk(),
                transmissionProfile.telemetryMode(),
                transmissionProfile.telemetryChannel(),
                transmissionProfile.deadLetterQueue(),
                transmissionProfile.reconciliationDesk(),
                transmissionProfile.submissionAuditMode(),
                transmissionProfile.protocolSlaBucket(),
                transmissionProfile.escalationDesk(),
                transmissionProfile.receiptAuditDesk(),
                transmissionProfile.proofBundleMode(),
                transmissionProfile.reconciliationWindow(),
                transmissionProfile.competenceHint(),
                transmissionProfile.stepUpRequired(),
                transmissionProfile.certificateRequired(),
                transmissionProfile.connectorWarnings(),
                mergeLabels(profile.labels(), transmissionProfile.labels()),
                mergeMetadata(profile.toMap(), transmissionProfile.toMap())
        );
        fundamentos.add("Mesa de admissibilidade: " + profile.admissibilityDesk());
        fundamentos.add("Mesa de julgamento: " + profile.gabineteDestino());
        if (profile.distributionDesk() != null) {
            fundamentos.add("Canal de distribuição: " + profile.distributionDesk());
        }
        fundamentos.add("Canal de protocolo: " + profile.protocolDesk());
        fundamentos.add("Kind de rota: " + profile.routeKind());
        fundamentos.add("Modo de contrarrazões: " + profile.counterReasonsMode());
        if (profile.counterReasonsDesk() != null) {
            fundamentos.add("Mesa de contrarrazões: " + profile.counterReasonsDesk());
        }
        fundamentos.add("Modo de efeito recursal: " + profile.effectMode());
        fundamentos.add("Modo de retratação: " + profile.retratacaoMode());
        fundamentos.add("Modo de sobrestamento: " + profile.sobrestamentoMode());
        fundamentos.add("Regime de preparo: " + profile.preparoMode());
        fundamentos.add("Regime de prevenção: " + profile.preventionMode());
        fundamentos.add("Modo de remessa: " + profile.transmissionMode());
        fundamentos.add("Canal de integração: " + profile.integrationChannel());
        fundamentos.add("Canal de recibo: " + profile.receiptChannel());
        fundamentos.add("Política de evidência: " + profile.evidencePolicy());
        fundamentos.add("Conector judicial: " + profile.connectorSystem());
        fundamentos.add("Fluxo do conector: " + profile.connectorWorkflowMode());
        fundamentos.add("Fallback operacional: " + profile.fallbackMode());
        fundamentos.add("Desk de contingência: " + profile.contingencyDesk());
        fundamentos.add("Canal de telemetria: " + profile.telemetryChannel());
        fundamentos.add("Fila morta/replay: " + profile.deadLetterQueue());
        fundamentos.add("Modo de auditoria da submissão: " + profile.submissionAuditMode());
        fundamentos.add("Bucket SLA de protocolo: " + profile.protocolSlaBucket());

        return new RecursalAdmissibilityDecision(
                admissivel,
                planning.routePlan().profileName(),
                planning.routePlan().tribunalDestino().name(),
                planning.routePlan().instanciaDestino().name(),
                planning.routePlan().autoridadeJulgamentoMerito().name(),
                planning.routePlan().admissibilidade().juizoOrigem(),
                planning.routePlan().admissibilidade().autoridadeOrigem() == null ? null : planning.routePlan().admissibilidade().autoridadeOrigem().name(),
                planning.routePlan().admissibilidade().juizoDestino(),
                planning.routePlan().admissibilidade().autoridadeDestino() == null ? null : planning.routePlan().admissibilidade().autoridadeDestino().name(),
                tempo(prazo),
                command.dataProtocolo(),
                prazo == null ? null : prazo.vencimentoForense(),
                tempestivo,
                preparoExigido,
                preparoDispensado,
                preparoSatisfeito,
                preclusao,
                profile.secretariaOrigem(),
                profile.secretariaDestino(),
                profile.admissibilityDesk(),
                profile.gabineteDestino(),
                profile.supportDesk(),
                profile.distributionDesk(),
                profile.sessionMode(),
                profile.routingBucket(),
                profile.riskLevel(),
                profile.routeKind(),
                profile.counterReasonsMode(),
                profile.counterReasonsDesk(),
                profile.effectMode(),
                profile.automaticSuspensiveEffect(),
                profile.retratacaoMode(),
                profile.sobrestamentoMode(),
                profile.preparoMode(),
                profile.preventionMode(),
                profile.protocolDesk(),
                profile.remessaDesk(),
                profile.autuacaoDesk(),
                profile.integrationChannel(),
                profile.credentialMode(),
                profile.payloadPolicy(),
                profile.transmissionMode(),
                profile.queueSuffix(),
                profile.reviewDesk(),
                profile.ackDesk(),
                profile.receiptChannel(),
                profile.retryMode(),
                profile.evidencePolicy(),
                profile.complianceDesk(),
                profile.protocolWindow(),
                profile.connectorSystem(),
                profile.connectorBaseUrl(),
                profile.connectorWorkflowMode(),
                profile.fallbackMode(),
                profile.contingencyDesk(),
                profile.replayQueue(),
                profile.evidenceRetentionPolicy(),
                profile.manualSubmissionDesk(),
                profile.telemetryMode(),
                profile.telemetryChannel(),
                profile.deadLetterQueue(),
                profile.reconciliationDesk(),
                profile.submissionAuditMode(),
                profile.protocolSlaBucket(),
                profile.escalationDesk(),
                profile.receiptAuditDesk(),
                profile.proofBundleMode(),
                profile.reconciliationWindow(),
                profile.competenceHint(),
                profile.stepUpRequired(),
                profile.certificateRequired(),
                profile.connectorWarnings(),
                List.copyOf(alertas),
                List.copyOf(fundamentos),
                profile.labels(),
                profile.toMap()
        );
    }


    private List<String> mergeLabels(List<String> left, List<String> right) {
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            merged.addAll(right);
        }
        return List.copyOf(merged);
    }

    private java.util.LinkedHashMap<String, Object> mergeMetadata(Map<String, Object> left, Map<String, Object> right) {
        java.util.LinkedHashMap<String, Object> merged = new java.util.LinkedHashMap<>();
        if (left != null) {
            merged.putAll(left);
        }
        if (right != null) {
            merged.putAll(right);
        }
        merged.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return merged;
    }

    private boolean resolveTempestividade(RecursalAdmissibilityCommand command,
                                          PrazoProcessualNacionalService.PrazoProcessualResult prazo) {
        if (command.dataProtocolo() == null || prazo == null) {
            return command.planRequest().context().tempestivo();
        }
        return !command.dataProtocolo().isAfter(prazo.vencimentoForense());
    }

    private PreclusaoTipo resolvePreclusao(RecursalAdmissibilityCommand command, boolean tempestivo) {
        if (!tempestivo) {
            return PreclusaoTipo.TEMPORAL;
        }
        if (command.aceitouDecisaoOuPraticouAtoIncompativel()) {
            return PreclusaoTipo.LOGICA;
        }
        if (command.recursoAnteriorMesmaEspecieInterposto()) {
            return PreclusaoTipo.CONSUMATIVA;
        }
        return PreclusaoTipo.NENHUMA;
    }

    private String tempo(PrazoProcessualNacionalService.PrazoProcessualResult prazo) {
        return prazo == null ? null : prazo.tipoPrazo().name();
    }

    private NationalPrazoEngine.TipoPrazo mapPrazo(RecursalMeshSpeciesType type) {
        return switch (type) {
            case EDCL -> NationalPrazoEngine.TipoPrazo.EMBARGOS_DECLARACAO;
            case AGINT, AGREG -> NationalPrazoEngine.TipoPrazo.AGRAVO_INTERNO;
            case APCIV, APCRIM, RINOM -> NationalPrazoEngine.TipoPrazo.APELACAO;
            case PUILF -> NationalPrazoEngine.TipoPrazo.RECURSO_ESPECIAL;
            case AGINST, AGITRAB -> NationalPrazoEngine.TipoPrazo.AGRAVO_INSTRUMENTO;
            case ROC -> NationalPrazoEngine.TipoPrazo.RECURSO_ORDINARIO_CONSTITUCIONAL;
            case ROT, RR, AIRR, AGPET -> NationalPrazoEngine.TipoPrazo.RECURSO_TRABALHISTA;
            case EEXEC, EEFISC, ETERC -> NationalPrazoEngine.TipoPrazo.EMBARGOS_EXECUCAO;
            case RESP -> NationalPrazoEngine.TipoPrazo.RECURSO_ESPECIAL;
            case RE, CPARCIAL -> NationalPrazoEngine.TipoPrazo.RECURSO_EXTRAORDINARIO;
            case ARESP, ARE -> NationalPrazoEngine.TipoPrazo.AGRAVO_RECURSO_SUPERIOR;
            case EDIV -> NationalPrazoEngine.TipoPrazo.EMBARGOS_DIVERGENCIA;
            case RCL -> NationalPrazoEngine.TipoPrazo.RECLAMACAO_CONSTITUCIONAL;
            case CC -> NationalPrazoEngine.TipoPrazo.CONFLITO_COMPETENCIA;
        };
    }

    private GrauJurisdicao mapGrau(InstanceLevel level) {
        return switch (level) {
            case FIRST_INSTANCE -> GrauJurisdicao.PRIMEIRO_GRAU;
            case SECOND_INSTANCE -> GrauJurisdicao.SEGUNDO_GRAU;
            case SUPERIOR -> GrauJurisdicao.SUPERIOR;
            case EXTRAORDINARY -> GrauJurisdicao.CONSTITUCIONAL;
        };
    }

    public record RecursalAdmissibilityCommand(
            RecursalMeshPlanRequest planRequest,
            LocalDate dataIntimacao,
            LocalDate dataProtocolo,
            String tribunalCodigo,
            String uf,
            String comarca,
            boolean preparoRecolhido,
            boolean preparoDispensado,
            boolean aceitouDecisaoOuPraticouAtoIncompativel,
            boolean recursoAnteriorMesmaEspecieInterposto,
            boolean pedidoEfeitoSuspensivo,
            boolean tutelaUrgenciaRecursal,
            boolean segredoJustica,
            boolean priorizaIdosoOuSaude) {
        public RecursalAdmissibilityCommand(RecursalMeshPlanRequest planRequest,
                                            LocalDate dataIntimacao,
                                            LocalDate dataProtocolo,
                                            String tribunalCodigo,
                                            String uf,
                                            String comarca,
                                            boolean preparoRecolhido,
                                            boolean preparoDispensado,
                                            boolean aceitouDecisaoOuPraticouAtoIncompativel,
                                            boolean recursoAnteriorMesmaEspecieInterposto) {
            this(planRequest, dataIntimacao, dataProtocolo, tribunalCodigo, uf, comarca, preparoRecolhido, preparoDispensado,
                    aceitouDecisaoOuPraticouAtoIncompativel, recursoAnteriorMesmaEspecieInterposto, false, false, false, false);
        }
    }

    public record RecursalAdmissibilityDecision(
            boolean admissivelEmTese,
            String perfilRecursal,
            String tribunalDestino,
            String instanciaDestino,
            String autoridadeJulgamento,
            boolean juizoAdmissibilidadeOrigem,
            String autoridadeOrigem,
            boolean juizoAdmissibilidadeDestino,
            String autoridadeDestino,
            String tipoPrazo,
            LocalDate dataProtocolo,
            LocalDate dataLimite,
            boolean tempestivo,
            boolean preparoExigido,
            boolean preparoDispensado,
            boolean preparoSatisfeito,
            PreclusaoTipo preclusao,
            String secretariaOrigem,
            String secretariaDestino,
            String admissibilityDesk,
            String gabineteDestino,
            String supportDesk,
            String distributionDesk,
            String sessionMode,
            String routingBucket,
            String riskLevel,
            String routeKind,
            String counterReasonsMode,
            String counterReasonsDesk,
            String effectMode,
            boolean automaticSuspensiveEffect,
            String retratacaoMode,
            String sobrestamentoMode,
            String preparoMode,
            String preventionMode,
            String protocolDesk,
            String remessaDesk,
            String autuacaoDesk,
            String integrationChannel,
            String credentialMode,
            String payloadPolicy,
            String transmissionMode,
            String queueSuffix,
            String reviewDesk,
            String ackDesk,
            String receiptChannel,
            String retryMode,
            String evidencePolicy,
            String complianceDesk,
            String protocolWindow,
            String connectorSystem,
            String connectorBaseUrl,
            String connectorWorkflowMode,
            String fallbackMode,
            String contingencyDesk,
            String replayQueue,
            String evidenceRetentionPolicy,
            String manualSubmissionDesk,
            String telemetryMode,
            String telemetryChannel,
            String deadLetterQueue,
            String reconciliationDesk,
            String submissionAuditMode,
            String protocolSlaBucket,
            String escalationDesk,
            String receiptAuditDesk,
            String proofBundleMode,
            String reconciliationWindow,
            String competenceHint,
            boolean stepUpRequired,
            boolean certificateRequired,
            List<String> connectorWarnings,
            List<String> alertas,
            List<String> fundamentos,
            List<String> labels,
            Map<String, Object> metadata) {
    }
}
