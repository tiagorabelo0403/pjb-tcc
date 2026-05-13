package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.governance.PeticionamentoGuardrailResponse;
import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistResponse;
import com.tcc.pjb.backend.service.SigiloService;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoPreventiveGuardrailService {

    public PeticionamentoGuardrailResponse analyze(RepresentacaoProcessualPolicyResponse representacao,
                                                   SigiloService.SigiloDecision sigiloDecision,
                                                   LaianePeticaoInicialDraftService.DraftView manualDraft,
                                                   LaianePeticaoAssistResponse assistiveAnalysis,
                                                   boolean protocolPackageReady) {
        if (representacao == null && sigiloDecision == null && manualDraft == null && assistiveAnalysis == null) {
            return PeticionamentoGuardrailResponse.vazio();
        }

        LinkedHashSet<String> bloqueios = new LinkedHashSet<>();
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        LinkedHashSet<String> checklist = new LinkedHashSet<>();
        LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();

        if (representacao != null) {
            envelope.put("representacaoRegular", representacao.regularidadeSuficiente());
            envelope.put("instrumento", representacao.resolvedInstrument());
            if (!representacao.regularidadeSuficiente()) {
                bloqueios.add("Regularizar o instrumento de representação antes do protocolo sensível.");
            }
            if (representacao.exigeProcuracaoFormal()) {
                checklist.add("Anexar procuração válida ou comprovar dispensa institucional do mandato.");
            }
            if (representacao.exigePoderesEspeciaisTransigir()) {
                checklist.add("Conferir poderes especiais quando houver transação, acordo ou composição processual.");
            }
            if (representacao.exigeTermoOuAtaAudiencia()) {
                checklist.add("Vincular termo ou ata de audiência exigidos pela moldura representativa escolhida.");
            }
            alertas.addAll(safe(representacao.alertas()));
            checklist.addAll(safe(representacao.validacoesObrigatorias()));
        }

        if (sigiloDecision != null && sigiloDecision.nivel() != null) {
            envelope.put("sigiloNivel", sigiloDecision.nivel().name());
            envelope.put("sigiloScore", sigiloDecision.score());
            if (sigiloDecision.nivel().exigeCredencial()) {
                alertas.add("Caso sensível com trilha reforçada de sigilo, need-to-know e governança de leitura.");
                checklist.addAll(safe(sigiloDecision.recomendacoes()));
            }
        }

        if (manualDraft != null) {
            envelope.put("manualReadiness", manualDraft.readinessScore());
            if (manualDraft.readinessScore() != null && manualDraft.readinessScore() < 70) {
                alertas.add("A minuta manual ainda exige reforço de fatos, fundamentos, pedidos ou prova antes do protocolo.");
            }
            checklist.addAll(safe(manualDraft.checklistDocumental()));
        }

        if (assistiveAnalysis != null) {
            envelope.put("assistReadiness", assistiveAnalysis.getReadinessScore());
            envelope.put("assistReady", assistiveAnalysis.isProntaParaProtocolo());
            if (assistiveAnalysis.getValidator() != null && !assistiveAnalysis.getValidator().isOk()) {
                bloqueios.addAll(safe(assistiveAnalysis.getValidator().getErrors()));
                alertas.addAll(safe(assistiveAnalysis.getValidator().getWarnings()));
            }
            if (assistiveAnalysis.getAttachmentValidation() != null && !assistiveAnalysis.getAttachmentValidation().isOk()) {
                bloqueios.addAll(safe(assistiveAnalysis.getAttachmentValidation().getMissing()));
            }
            if (assistiveAnalysis.getTetoProcessual() != null) {
                envelope.put("tetoCodigo", assistiveAnalysis.getTetoProcessual().codigoDiagnostico());
                envelope.put("tetoBloqueante", assistiveAnalysis.getTetoProcessual().bloqueante());
                if (assistiveAnalysis.getTetoProcessual().bloqueante()) {
                    bloqueios.add(firstNonBlank(assistiveAnalysis.getTetoProcessual().fundamentoLegal(), "Erro de teto bloqueante detectado para o valor da causa informado."));
                } else if (assistiveAnalysis.getTetoProcessual().alerta()) {
                    alertas.add(firstNonBlank(assistiveAnalysis.getTetoProcessual().fundamentoLegal(), "Erro de teto em faixa de alerta exige conferência antes do protocolo."));
                }
                checklist.add(firstNonBlank(assistiveAnalysis.getTetoProcessual().sugestaoOperacional(), "Conferir teto legal, competência e rito sugeridos antes de protocolar."));
            }
            if (assistiveAnalysis.getTerritorialProcessual() != null) {
                envelope.put("territorialCodigo", assistiveAnalysis.getTerritorialProcessual().codigoDiagnostico());
                envelope.put("territorialMode", assistiveAnalysis.getTerritorialProcessual().territorialMode());
                envelope.put("territorialBloqueante", assistiveAnalysis.getTerritorialProcessual().bloqueante());
                if (assistiveAnalysis.getTerritorialProcessual().bloqueante()) {
                    bloqueios.add(firstNonBlank(assistiveAnalysis.getTerritorialProcessual().fundamentoLegal(), "Inconsistência territorial bloqueante detectada pela malha nacional."));
                } else if (assistiveAnalysis.getTerritorialProcessual().alerta()) {
                    alertas.add(firstNonBlank(assistiveAnalysis.getTerritorialProcessual().fundamentoLegal(), "Risco territorial recomenda revisão humana antes do protocolo."));
                }
                alertas.addAll(safe(assistiveAnalysis.getTerritorialProcessual().alertas()));
                checklist.addAll(safe(assistiveAnalysis.getTerritorialProcessual().reviewChecklist()));
                checklist.add(firstNonBlank(assistiveAnalysis.getTerritorialProcessual().sugestaoOperacional(), null));
            }
            if (assistiveAnalysis.getCompetencia() != null) {
                envelope.put("tribunalCodigo", assistiveAnalysis.getCompetencia().tribunalCodigo());
                envelope.put("unidadeCodigo", assistiveAnalysis.getCompetencia().unidadeCodigo());
                envelope.put("distribuicaoAutomatica", assistiveAnalysis.getCompetencia().distribuicaoAutomatica());
                if (!assistiveAnalysis.getCompetencia().distribuicaoAutomatica()) {
                    alertas.add("A distribuição exige revisão humana antes do impulso útil seguinte.");
                }
                alertas.addAll(safe(assistiveAnalysis.getCompetencia().alertas()));
                checklist.addAll(safe(assistiveAnalysis.getCompetencia().fatoresRevisaoHumana()));
            }
            if (assistiveAnalysis.getProceduralRouting() != null) {
                envelope.put("routingRiskLevel", assistiveAnalysis.getProceduralRouting().riskLevel());
                envelope.put("routingConfidence", assistiveAnalysis.getProceduralRouting().confidence());
                alertas.addAll(safe(assistiveAnalysis.getProceduralRouting().blockingIssues()));
                checklist.addAll(safe(assistiveAnalysis.getProceduralRouting().reviewChecklist()));
            }
            if (assistiveAnalysis.getSubmissionBlueprint() != null && !assistiveAnalysis.getSubmissionBlueprint().readyForRealConnectorSubmission()) {
                alertas.add("O blueprint procedimental ainda não está apto para protocolo automático integral.");
                checklist.addAll(safe(assistiveAnalysis.getSubmissionBlueprint().reviewChecklist()));
            }
            if (assistiveAnalysis.getConnectorExecution() != null && !assistiveAnalysis.getConnectorExecution().allowedToAutoSubmit()) {
                alertas.add("O conector judicial ainda requer saneamento antes do envio automático definitivo.");
                checklist.addAll(safe(assistiveAnalysis.getConnectorExecution().executionChecklist()));
            }
        }

        boolean bloqueante = !bloqueios.isEmpty();
        boolean alerta = !bloqueante && !alertas.isEmpty();
        boolean prontaParaProtocolar = !bloqueante
                && assistiveAnalysis != null
                && assistiveAnalysis.isProntaParaProtocolo()
                && (assistiveAnalysis.getTerritorialProcessual() == null || !assistiveAnalysis.getTerritorialProcessual().bloqueante())
                && (assistiveAnalysis.getTetoProcessual() == null || !assistiveAnalysis.getTetoProcessual().bloqueante());
        String status = bloqueante ? "BLOQUEADO" : alerta ? "ALERTA" : prontaParaProtocolar ? "APTO" : "EM_AJUSTE";
        envelope.put("protocolPackageReady", protocolPackageReady);
        envelope.put("blockingCount", bloqueios.size());
        envelope.put("alertCount", alertas.size());
        envelope.put("checklistCount", checklist.size());

        return new PeticionamentoGuardrailResponse(
                status,
                bloqueante,
                alerta || !alertas.isEmpty(),
                prontaParaProtocolar,
                resolveNextAction(bloqueios, alertas, checklist, representacao, assistiveAnalysis, protocolPackageReady),
                List.copyOf(bloqueios),
                List.copyOf(alertas),
                List.copyOf(checklist),
                Map.copyOf(envelope)
        );
    }

    private static String resolveNextAction(LinkedHashSet<String> bloqueios,
                                            LinkedHashSet<String> alertas,
                                            LinkedHashSet<String> checklist,
                                            RepresentacaoProcessualPolicyResponse representacao,
                                            LaianePeticaoAssistResponse assistiveAnalysis,
                                            boolean protocolPackageReady) {
        if (representacao != null && !representacao.regularidadeSuficiente()) {
            return "REGULARIZAR_REPRESENTACAO";
        }
        if (assistiveAnalysis != null && assistiveAnalysis.getTetoProcessual() != null && assistiveAnalysis.getTetoProcessual().bloqueante()) {
            return "CORRIGIR_ERRO_DE_TETO";
        }
        if (assistiveAnalysis != null && assistiveAnalysis.getTerritorialProcessual() != null && assistiveAnalysis.getTerritorialProcessual().bloqueante()) {
            return "CORRIGIR_ERRO_TERRITORIAL";
        }
        if (!bloqueios.isEmpty()) {
            return "SANEAR_BLOQUEIOS";
        }
        if (!alertas.isEmpty()) {
            return "REVISAR_ALERTAS_CRITICOS";
        }
        if (!checklist.isEmpty() && !protocolPackageReady) {
            return "MONTAR_PACOTE_PROTOCOLO";
        }
        if (assistiveAnalysis != null && assistiveAnalysis.isProntaParaProtocolo() && !protocolPackageReady) {
            return "GERAR_PACOTE_PROTOCOLO";
        }
        return protocolPackageReady ? "SEGUIR_PARA_ASSINATURA" : "REVISAR_PETICIONAMENTO";
    }

    private static List<String> safe(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (String value : values) {
            String normalized = firstNonBlank(value, null);
            if (normalized != null && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static String firstNonBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }
}
