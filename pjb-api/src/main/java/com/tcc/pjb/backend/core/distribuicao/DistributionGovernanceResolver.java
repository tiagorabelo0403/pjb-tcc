package com.tcc.pjb.backend.core.distribuicao;

import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class DistributionGovernanceResolver {

    public DistributionGovernanceProfile resolve(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest req,
                                                 NationalProcessRoutingService.RoutingDecision routing,
                                                 DistributionConstraintSnapshot constraintSnapshot) {
        String tribunal = normalize(firstNonBlank(routing.tribunalCodigo(), "TRIBUNAL"), "TRIBUNAL");
        String eixoMaterial = normalize(firstNonBlank(metadataString(routing, "coverage.materialityAxis"), routing.specializationAxis(), "GERAL"), "GERAL");
        String queueLane = firstNonBlank(
                metadataString(routing, "coverage.territorialAnchor"),
                routing.foroSugerido(),
                routing.comarcaSugerida(),
                routing.cidadeSugerida(),
                req.comarca(),
                req.cidade(),
                "BASE");
        String workloadClass = resolveWorkloadClass(req, routing);
        String governanceMode = resolveGovernanceMode(req, routing, constraintSnapshot);
        String randomizationMode = resolveRandomizationMode(req, routing, constraintSnapshot);
        String equalizationRule = resolveEqualizationRule(routing);
        String preventionLockMode = resolvePreventionLockMode(req, routing, constraintSnapshot);
        String urgencyLane = resolveUrgencyLane(req, routing);
        String sigiloLane = resolveSigiloLane(req, routing);
        String recusalLane = req.redistribuicaoImpedimento() ? "REDISTRIBUICAO_POR_IMPEDIMENTO" : "SEM_AFASTAMENTO_DECLARADO";
        String auditDesk = "AUDITORIA_DISTRIBUICAO_" + tribunal;
        String incidentDesk = firstNonBlank(
                metadataString(routing, "relational.targetDeskProfile"),
                routing.suggestedDeskProfile(),
                routing.mesaTriagem(),
                "CONTROLE_DISTRIBUICAO_" + tribunal);
        int priorityFloor = resolvePriorityFloor(req, routing, constraintSnapshot);

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        fundamentos.add("Governança de distribuição classificada em " + governanceMode + '.');
        fundamentos.add("Randomização controlada por " + randomizationMode + '.');
        fundamentos.add("Equalização vinculada à regra " + equalizationRule + '.');
        fundamentos.add("Trilha operacional posicionada em " + workloadClass + '.');
        fundamentos.add("Auditoria primária direcionada para " + auditDesk + '.');

        if (constraintSnapshot.reviewRequired()) {
            warnings.add("Há restrição relacional ou preventiva que desloca a distribuição para revisão reforçada.");
            reviewChecklist.add("Conferir dependência, conexão, continência ou prevenção antes do fechamento da distribuição.");
        }
        if (req.plantaoJudicial() || req.pedidoLiminar()) {
            reviewChecklist.add("Confirmar aderência da urgência ao plantão, à tutela provisória e à mesa prioritária.");
        }
        if (req.segredoSolicitado() || routing.sigiloPadrao()) {
            reviewChecklist.add("Garantir segregação de fila, protocolo e visualização por sigilo.");
        }
        if (req.redistribuicaoImpedimento()) {
            reviewChecklist.add("Verificar recusa, impedimento, suspeição e nova trilha de sorteio sem relator contaminado.");
        }
        if (routing.grau() != GrauJurisdicao.PRIMEIRO_GRAU) {
            reviewChecklist.add("Homologar distribuição colegiada com prevenção do relator e composição do órgão fracionário.");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunal", tribunal);
        metadata.put("materialityAxis", eixoMaterial);
        metadata.put("queueLaneNormalized", normalize(queueLane, "BASE"));
        metadata.put("descriptor", String.join(":",
                normalize(governanceMode, "GOVERNANCA"),
                normalize(workloadClass, "TRILHA"),
                normalize(randomizationMode, "RANDOMIZACAO"),
                normalize(preventionLockMode, "PREVENCAO"),
                normalize(sigiloLane, "PUBLICIDADE")));
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new DistributionGovernanceProfile(
                governanceMode,
                queueLane,
                workloadClass,
                randomizationMode,
                equalizationRule,
                preventionLockMode,
                urgencyLane,
                sigiloLane,
                recusalLane,
                auditDesk,
                incidentDesk,
                priorityFloor,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private String resolveGovernanceMode(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest req,
                                         NationalProcessRoutingService.RoutingDecision routing,
                                         DistributionConstraintSnapshot constraintSnapshot) {
        if (req.redistribuicaoImpedimento()) {
            return "REDISTRIBUICAO_CONTROLADA_POR_IMPEDIMENTO";
        }
        if (req.plantaoJudicial()) {
            return "PLANTAO_COM_CONTROLE_ESTATISTICO";
        }
        if (req.pedidoLiminar()) {
            return "PRIORIDADE_JUDICIAL_IMEDIATA";
        }
        if (!"AUTONOMA".equals(firstNonBlank(routing.linkageMode(), "AUTONOMA")) || constraintSnapshot.reviewRequired()) {
            return "SORTEIO_RESTRITO_COM_PREVENCAO";
        }
        if (routing.grau() == GrauJurisdicao.PRIMEIRO_GRAU) {
            return "SORTEIO_ESTATISTICO_DE_ORIGEM";
        }
        if (routing.grau() == GrauJurisdicao.SEGUNDO_GRAU) {
            return "DISTRIBUICAO_COLEGIADA_CONTROLADA";
        }
        if (routing.grau() == GrauJurisdicao.SUPERIOR) {
            return "DISTRIBUICAO_CORTE_SUPERIOR";
        }
        return "DISTRIBUICAO_CORTE_CONSTITUCIONAL";
    }

    private String resolveRandomizationMode(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest req,
                                            NationalProcessRoutingService.RoutingDecision routing,
                                            DistributionConstraintSnapshot constraintSnapshot) {
        if (req.redistribuicaoImpedimento()) {
            return "NOVO_SORTEIO_SEM_RELATOR_IMPEDIDO";
        }
        if (!"AUTONOMA".equals(firstNonBlank(routing.linkageMode(), "AUTONOMA"))) {
            return "REDIRECIONAMENTO_POR_VINCULO_PROCESSUAL";
        }
        if (constraintSnapshot.reviewRequired()) {
            return "SORTEIO_COM_TRAVA_PREVENTIVA";
        }
        return routing.grau() == GrauJurisdicao.PRIMEIRO_GRAU
                ? "SORTEIO_ESTATISTICO_EQUALIZADO"
                : "DISTRIBUICAO_RELATORIA_EQUALIZADA";
    }

    private String resolveEqualizationRule(NationalProcessRoutingService.RoutingDecision routing) {
        if (routing.grau() == GrauJurisdicao.PRIMEIRO_GRAU) {
            return "BALANCEAMENTO_POR_ACERVO_DA_UNIDADE";
        }
        if (routing.grau() == GrauJurisdicao.SEGUNDO_GRAU) {
            return "BALANCEAMENTO_POR_RELATORIA_E_ORGAO";
        }
        return "BALANCEAMENTO_POR_GABINETE_E_CLASSE";
    }

    private String resolvePreventionLockMode(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest req,
                                             NationalProcessRoutingService.RoutingDecision routing,
                                             DistributionConstraintSnapshot constraintSnapshot) {
        if (req.redistribuicaoImpedimento()) {
            return "PREVENCAO_AFASTADA_POR_IMPEDIMENTO";
        }
        if (constraintSnapshot.reviewRequired()) {
            return "PREVENCAO_ESTRITA_COM_REVISOR";
        }
        if (!"AUTONOMA".equals(firstNonBlank(routing.linkageMode(), "AUTONOMA"))) {
            return "PREVENCAO_VINCULADA";
        }
        return firstNonBlank(routing.preventionMode(), "SEM_PREVENCAO_ATIVA");
    }

    private String resolveUrgencyLane(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest req,
                                      NationalProcessRoutingService.RoutingDecision routing) {
        if (req.plantaoJudicial()) {
            return "PLANTAO_24H";
        }
        if (req.pedidoLiminar()) {
            return "TUTELA_URGENTE";
        }
        if ("CRITICO".equals(routing.routingRiskLevel())) {
            return "RISCO_CRITICO";
        }
        return "ROTINA_CONTROLADA";
    }

    private String resolveSigiloLane(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest req,
                                     NationalProcessRoutingService.RoutingDecision routing) {
        return req.segredoSolicitado() || routing.sigiloPadrao()
                ? "SIGILO_REFORCADO"
                : "PUBLICIDADE_CONTROLADA";
    }

    private String resolveWorkloadClass(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest req,
                                        NationalProcessRoutingService.RoutingDecision routing) {
        if (req.plantaoJudicial()) {
            return "PLANTAO_CRITICO";
        }
        if (req.pedidoLiminar()) {
            return "TUTELA_IMEDIATA";
        }
        RitoProcessual rito = req.rito();
        if (rito != null && rito.isPenal()) {
            return "CRIMINAL_CONTROLE_INTENSIVO";
        }
        if (rito != null && rito.isPrevidenciario()) {
            return "PREVIDENCIARIO_VOLUME_REPETITIVO";
        }
        if (rito != null && rito.isTrabalhista()) {
            return "TRABALHISTA_RITO_CELERE";
        }
        if (rito != null && rito.isEleitoral()) {
            return "ELEITORAL_JANELA_SAZONAL";
        }
        if (routing.grau() != GrauJurisdicao.PRIMEIRO_GRAU) {
            return "COLEGIADO_ACERVO_RELATORIA";
        }
        return "CONHECIMENTO_GERAL";
    }

    private int resolvePriorityFloor(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest req,
                                     NationalProcessRoutingService.RoutingDecision routing,
                                     DistributionConstraintSnapshot constraintSnapshot) {
        if (req.plantaoJudicial() || req.pedidoLiminar() || req.redistribuicaoImpedimento()) {
            return 0;
        }
        if (constraintSnapshot.reviewRequired() || "CRITICO".equals(routing.routingRiskLevel())) {
            return 1;
        }
        if (!"AUTONOMA".equals(firstNonBlank(routing.linkageMode(), "AUTONOMA"))) {
            return 1;
        }
        return 3;
    }

    private String metadataString(NationalProcessRoutingService.RoutingDecision routing, String dottedPath) {
        if (routing == null || routing.metadata() == null || dottedPath == null || dottedPath.isBlank()) {
            return null;
        }
        Object current = routing.metadata();
        for (String token : dottedPath.split("\\.")) {
            if (!(current instanceof java.util.Map<?, ?> map)) {
                return null;
            }
            current = map.get(token);
            if (current == null) {
                return null;
            }
        }
        if (current instanceof String value) {
            return value.isBlank() ? null : value.trim();
        }
        return String.valueOf(current);
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

    private static String normalize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? fallback : normalized;
    }
}
