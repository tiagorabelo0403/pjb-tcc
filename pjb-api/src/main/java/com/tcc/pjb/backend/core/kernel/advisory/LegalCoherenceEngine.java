package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.procedural.ProceduralRitoNames;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoValidateResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer.LaianeLawyerAttachmentValidationResponse;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;

@Service
public class LegalCoherenceEngine {

    public LegalCoherenceReport analyzeRequest(LaianePeticaoAssistRequest request,
                                               CanonicalContext canonical,
                                               String ritoName,
                                               LaianePeticaoValidateResponse validator,
                                               LaianeLawyerAttachmentValidationResponse attachments,
                                               DynamicCompetenceDistributionResponse competencia,
                                               TetoProcessualService.DiagnosticoTetoProcessual teto,
                                               RadarPadroesService.AnaliseRadarResultado radar,
                                               ProntuarioNacionalService.AnaliseConflitoProcessual conflito) {
        Objects.requireNonNull(request, "request");
        List<LegalCoherenceReport.Issue> issues = new ArrayList<>();
        LinkedHashSet<String> strengths = new LinkedHashSet<>();
        LinkedHashSet<String> recommendations = new LinkedHashSet<>();

        if (validator != null && !validator.isOk()) {
            issues.add(issue("PETITION_STRUCTURE_INVALID", "Estrutura da peça inconsistente", "A petição possui erros estruturais que precisam ser corrigidos antes do protocolo.", "CRITICAL", true, validator.getErrors()));
            recommendations.add("Corrigir os erros estruturais da petição e regenerar a versão final antes do protocolo.");
        } else {
            strengths.add("A petição passou pela validação estrutural básica.");
        }

        if (attachments != null && !attachments.isOk()) {
            issues.add(issue("MISSING_REQUIRED_DOCUMENTS", "Documentação obrigatória incompleta", "Há documentos exigidos pelo rito ou pela classe TPU que ainda não foram anexados.", "CRITICAL", true, attachments.getMissing()));
            recommendations.add("Anexar todos os documentos obrigatórios do rito antes de protocolar.");
        } else if (attachments != null) {
            strengths.add("Os anexos obrigatórios para o rito informado estão íntegros.");
        }

        if (teto != null && teto.bloqueante()) {
            issues.add(issue("VALUE_EXCEEDS_PROCEDURAL_LIMIT", "Valor da causa incompatível com o teto procedimental", firstNonBlank(teto.sugestaoOperacional(), "O valor informado ultrapassa o limite econômico aplicável ao fluxo pretendido."), "CRITICAL", true, List.of(firstNonBlank(teto.fundamentoLegal(), teto.codigoDiagnostico()))));
            recommendations.add("Revisar o valor da causa, eventual renúncia ao excedente ou a competência/rito adequados.");
        } else if (teto != null && teto.alerta()) {
            issues.add(issue("VALUE_NEAR_LIMIT", "Valor da causa muito próximo do limite", firstNonBlank(teto.sugestaoOperacional(), "O valor da causa está muito próximo do teto e pode gerar questionamento de competência."), "MEDIUM", false, List.of(firstNonBlank(teto.fundamentoLegal(), teto.codigoDiagnostico()))));
        } else if (teto != null) {
            strengths.add("O valor da causa é compatível com o teto procedimental analisado.");
        }

        if (competencia == null) {
            issues.add(issue("COMPETENCE_NOT_RESOLVED", "Competência ainda não resolvida", "O destino judicial não foi resolvido com segurança. O protocolo não deve seguir sem definição do tribunal/unidade.", "CRITICAL", true, List.of()));
            recommendations.add("Completar os elementos territoriais e materiais necessários para fechar a competência.");
        } else {
            if (!competencia.distribuicaoAutomatica()) {
                issues.add(issue("HUMAN_REVIEW_REQUIRED", "Distribuição exige revisão humana", firstNonBlank(competencia.motivacao(), "O caso demanda revisão humana antes da distribuição automática."), "HIGH", false, mergeLists(competencia.alertas(), competencia.fatoresRevisaoHumana())));
                recommendations.add("Submeter a competência para revisão humana antes do protocolo definitivo.");
            } else {
                strengths.add("A competência foi resolvida com distribuição automática habilitada.");
            }
        }

        if (radar != null && radar.temCritico()) {
            issues.add(issue("PATTERN_RISK_CRITICAL", "Radar identificou risco crítico", "Há sinais materiais relevantes que recomendam contenção, revisão ou reforço probatório antes do protocolo.", "HIGH", false, safe(radar.alertas().stream().map(a -> a.descricaoTecnica()).toList())));
            recommendations.add("Reforçar a narrativa fática e a documentação para neutralizar os riscos identificados pelo radar.");
        }

        if (conflito != null) {
            if (conflito.litispendenciaPotencial()) {
                    issues.add(issue("LITISPENDENCE_SIGNAL", "Sinal de litispendência", "Foi detectado indício de litispendência entre as mesmas partes e matéria correlata.", "HIGH", false, List.of()));
                    recommendations.add("Conferir prevenção, conexão e litispendência antes do ajuizamento.");
                }
            if (conflito.coisaJulgadaPotencial()) {
                    issues.add(issue("RES_JUDICATA_SIGNAL", "Sinal de coisa julgada", "Foi detectado indício de coisa julgada ou repetição material do litígio.", "CRITICAL", true, List.of()));
                    recommendations.add("Validar o histórico processual e a coisa julgada material antes de ajuizar.");
                }
        }

        if (Boolean.TRUE.equals(request.getRequerJuizadoEspecial()) && !ProceduralRitoNames.isOneOf(ritoName, "JUIZADO_ESPECIAL", "JUIZADO_ESPECIAL_CIVEL", "JUIZADO_ESPECIAL_FAZENDA_PUBLICA")) {
            issues.add(issue("JUIZADO_REQUEST_MISMATCH", "Preferência por juizado incompatível com o rito resolvido", "O usuário sinalizou juizado, mas o rito canônico não fechou em trilha de juizado especial.", "MEDIUM", false, List.of(ritoName)));
        }

        if (Boolean.TRUE.equals(request.getRequerLiminar()) && blank(request.getTextoFatosResumido())) {
            issues.add(issue("INJUNCTION_WITHOUT_FACTS", "Pedido urgente sem narrativa suficiente", "Há pedido de tutela/liminar sem narrativa fática resumida consistente para sustentar urgência e probabilidade.", "HIGH", false, List.of()));
            recommendations.add("Reforçar a exposição dos fatos, urgência e risco de dano para sustentar a tutela de urgência.");
        }

        if (canonical == null || blank(canonical.classeTpuCodigo())) {
            issues.add(issue("TPU_CLASS_MISSING", "Classe TPU não consolidada", "A classe TPU não foi consolidada pelo núcleo canônico, o que reduz a segurança do fluxo documental e competencial.", "HIGH", false, List.of()));
            recommendations.add("Consolidar a classe TPU antes de avançar no protocolo.");
        } else {
            strengths.add("A classe TPU foi consolidada pelo núcleo canônico.");
        }

        if (!blank(ritoName)) {
            strengths.add("O rito efetivo foi consolidado pela trilha canônica nominal.");
        }

        double score = score(issues);
        boolean blocking = issues.stream().anyMatch(LegalCoherenceReport.Issue::blocking);
        if (recommendations.isEmpty() && !blocking) {
            recommendations.add("A peça está coerente para seguimento, mantendo revisão final de estratégia e prova.");
        }
        return new LegalCoherenceReport(round(score), blocking, List.copyOf(issues), List.copyOf(strengths), List.copyOf(recommendations));
    }

    public LegalCoherenceReport analyzeProcess(Processo processo,
                                               String ritoName,
                                               RitoPlanDto ritoPlan,
                                               List<String> riskSignals,
                                               boolean hasEvidence) {
        Objects.requireNonNull(processo, "processo");
        List<LegalCoherenceReport.Issue> issues = new ArrayList<>();
        LinkedHashSet<String> strengths = new LinkedHashSet<>();
        LinkedHashSet<String> recommendations = new LinkedHashSet<>();

        if (blank(ritoName)) {
            issues.add(issue("RITO_MISSING", "Rito indefinido", "O processo ainda não consolidou rito efetivo, o que compromete a previsibilidade do fluxo.", "CRITICAL", true, List.of()));
        } else {
            strengths.add("O processo possui assinatura procedimental consolidada.");
        }
        if (processo.getFaseAtual() == null) {
            issues.add(issue("FASE_MISSING", "Fase atual ausente", "A fase atual não está definida no processo e isso reduz a segurança do workflow.", "HIGH", false, List.of()));
        } else {
            strengths.add("A fase atual do processo está materializada no twin.");
        }
        if (ritoPlan != null && ritoPlan.getBlockingOpen() != null && !ritoPlan.getBlockingOpen().isEmpty()) {
            issues.add(issue("BLOCKING_WORK_ITEMS", "Há work items bloqueantes em aberto", "O fluxo possui pendências bloqueantes abertas que impedem evolução segura.", "HIGH", false, ritoPlan.getBlockingOpen().stream().map(item -> item.getTitulo() != null ? item.getTitulo() : java.util.Objects.toString(item.getTemplateCode(), "workitem")).toList()));
            recommendations.add("Atacar as pendências bloqueantes antes da próxima transição do rito.");
        }
        if (riskSignals != null) {
            for (String signal : riskSignals) {
                if (!blank(signal)) {
                    issues.add(issue("PROCESS_RISK_SIGNAL", "Radar processual ativo", signal, "MEDIUM", false, List.of(signal)));
                }
            }
        }
        if (hasEvidence) {
            strengths.add("Foram encontrados precedentes potencialmente aderentes ao caso.");
        }
        if (processo.getValorCausa() != null && processo.getValorCausa().compareTo(BigDecimal.ZERO) > 0) {
            strengths.add("O processo possui valor da causa materializado para análises procedimentais e econômicas.");
        }
        double score = score(issues);
        boolean blocking = issues.stream().anyMatch(LegalCoherenceReport.Issue::blocking);
        if (recommendations.isEmpty() && !blocking) {
            recommendations.add("O processo está coerente para seguimento, com monitoramento contínuo de riscos e pendências.");
        }
        return new LegalCoherenceReport(round(score), blocking, List.copyOf(issues), List.copyOf(strengths), List.copyOf(recommendations));
    }

    private LegalCoherenceReport.Issue issue(String code, String title, String description, String severity, boolean blocking, List<String> evidence) {
        return new LegalCoherenceReport.Issue(code, title, description, severity, blocking, safe(evidence));
    }

    private List<String> safe(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
    }

    private List<String> mergeLists(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(safe(first));
        merged.addAll(safe(second));
        return List.copyOf(merged);
    }

    private double score(List<LegalCoherenceReport.Issue> issues) {
        double score = 1.0d;
        for (LegalCoherenceReport.Issue issue : issues) {
            score -= switch (normalize(issue.severity())) {
                case "CRITICAL" -> 0.22d;
                case "HIGH" -> 0.14d;
                case "MEDIUM" -> 0.08d;
                default -> 0.04d;
            };
        }
        return Math.max(0.0d, Math.min(1.0d, score));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
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
}
