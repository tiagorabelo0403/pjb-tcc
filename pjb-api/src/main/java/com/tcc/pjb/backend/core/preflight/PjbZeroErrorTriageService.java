package com.tcc.pjb.backend.core.preflight;

import com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice.PjbDigitalCoreRitoKind;
import com.tcc.pjb.backend.core.procedural.rito.behavior.PjbRitoBehavior;
import com.tcc.pjb.backend.core.procedural.rito.behavior.PjbRitoBehaviorResolver;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@org.springframework.stereotype.Service
public final class PjbZeroErrorTriageService {

    private static final Set<String> URGENCY_TERMS = Set.of("UTI", "MEDICAMENTO", "CIRURGIA", "RISCO", "ENERGIA", "ALIMENTO", "VIOLENCIA", "CRIANCA", "IDOSO");
    private static final Set<String> PENAL_URGENCY_TERMS = Set.of("PRESO", "PRISAO", "FLAGRANTE", "HABEAS", "SOLTURA", "LIBERDADE");

    private final PjbRitoBehaviorResolver behaviorResolver;

    public PjbZeroErrorTriageService() {
        this(new PjbRitoBehaviorResolver());
    }

    public PjbZeroErrorTriageService(PjbRitoBehaviorResolver behaviorResolver) {
        this.behaviorResolver = behaviorResolver;
    }

    public PjbZeroErrorTriageDecision assess(PjbZeroErrorTriageRequest request) {
        if (request == null) {
            return new PjbZeroErrorTriageDecision(
                    RitoProcessual.COMUM_ORDINARIO,
                    "BLOCKED_BY_EMPTY_REQUEST",
                    0,
                    false,
                    true,
                    false,
                    List.of(new PjbZeroErrorTriageIssue("REQUERIMENTO_AUSENTE", "BLOCKER", "Dados de protocolo ausentes", "Reiniciar a triagem do protocolo")),
                    List.of());
        }

        RitoProcessual canonicalRito = RitoProcessual.tryParse(request.classeProcessual())
                .or(() -> RitoProcessual.tryParse(request.assuntoPrincipal()))
                .orElse(null);

        PjbDigitalCoreRitoKind kind;
        if (canonicalRito != null) {
            kind = PjbDigitalCoreRitoKind.fromRitoProcessual(canonicalRito);
        } else if (request.ritoKind() != null && request.ritoKind() != PjbDigitalCoreRitoKind.DESCONHECIDO) {
            kind = request.ritoKind();
            canonicalRito = kind.toCanonicalRito();
        } else {
            kind = PjbDigitalCoreRitoKind.resolve(request.classeProcessual(), request.assuntoPrincipal(), request.valorCausa());
            canonicalRito = kind.toCanonicalRito();
        }

        PjbRitoBehavior behavior = behaviorResolver.resolve(canonicalRito);
        List<PjbZeroErrorTriageIssue> issues = new ArrayList<>();
        boolean costsBlocked = false;

        if (kind == PjbDigitalCoreRitoKind.DESCONHECIDO) {
            issues.add(issue("RITO_NAO_IDENTIFICADO", "BLOCKER", "O rito não foi identificado pelos metadados informados", "Revisar classe, assunto e valor da causa"));
        }
        if (canonicalRito.isJuizado() && request.pedidoPericiaTecnicaComplexa()) {
            issues.add(issue("INCOMPATIBILIDADE_SUMARISSIMA_PERICIA_COMPLEXA", "WARNING", "O pedido sinaliza perícia técnica complexa incompatível com tramitação simplificada", "Reavaliar rito ordinário antes da distribuição"));
        }
        if (canonicalRito.isJuizado() && request.primeiroGrau()) {
            costsBlocked = true;
            issues.add(issue("CUSTAS_INICIAIS_BLOQUEADAS_NO_JUIZADO", "INFO", "Custas iniciais não devem ser geradas automaticamente para o primeiro grau do Juizado", "Manter guia bloqueada até evento legal posterior"));
        }
        if (canonicalRito.isEleitoral()) {
            issues.add(issue("RITO_ELEITORAL_EXIGE_REVISAO", "WARNING", "Processos eleitorais requerem validação especial de período e competência", "Validar período eleitoral e competência da zona eleitoral"));
        }
        if (canonicalRito.isMilitar()) {
            issues.add(issue("RITO_MILITAR_EXIGE_REVISAO", "WARNING", "Processos militares requerem validação de hierarquia e competência específica", "Verificar competência da Justiça Militar"));
        }
        if (canonicalRito.requiresSegredoByDefault()) {
            issues.add(issue("PROCESSO_REQUER_SIGILO_PADRAO", "INFO", "Este rito processual requer aplicação de sigilo por padrão", "Ativar controle de acesso restrito"));
        }
        if (territorialMismatch(request)) {
            issues.add(issue("COMPETENCIA_TERRITORIAL_POTENCIALMENTE_INCOMPATIVEL", "WARNING", "CEP das partes e comarca selecionada indicam possível divergência territorial", "Sugerir núcleo ou comarca compatível antes do protocolo"));
        }
        if (semanticMismatch(request)) {
            issues.add(issue("DIVERGENCIA_SEMANTICA_ENTRE_PETICAO_E_METADADOS", "WARNING", "Sinais extraídos dos documentos divergem da classe ou do assunto selecionado", "Exibir correção assistida ao usuário"));
        }

        int urgencyScore = urgencyScore(request, canonicalRito);
        boolean blocker = issues.stream().anyMatch(i -> "BLOCKER".equals(i.severity()));
        boolean humanReview = blocker || behavior.requiresHumanReview() || issues.stream().anyMatch(i -> "WARNING".equals(i.severity()));
        String status = blocker ? "BLOCKED" : humanReview ? "REVIEW_BEFORE_PROTOCOL" : "READY_TO_PROTOCOL";

        return new PjbZeroErrorTriageDecision(canonicalRito, status, urgencyScore, !blocker, humanReview, costsBlocked, List.copyOf(issues), behavior.firstMovements(urgencyScore));
    }

    private boolean territorialMismatch(PjbZeroErrorTriageRequest request) {
        String comarca = normalize(request.comarcaSelecionada());
        String cepAutor = digits(request.cepParteAutora());
        String cepReu = digits(request.cepParteRe());
        return !comarca.isBlank() && cepAutor.length() >= 2 && cepReu.length() >= 2 && !cepAutor.substring(0, 2).equals(cepReu.substring(0, 2));
    }

    private boolean semanticMismatch(PjbZeroErrorTriageRequest request) {
        String classe = normalize(request.classeProcessual());
        String assunto = normalize(request.assuntoPrincipal());
        String signals = normalize(String.join(" ", request.safeDocumentSignals()) + " " + String.join(" ", request.safePetitionSignals()));
        if (signals.isBlank()) return false;
        boolean consumerSignal = signals.contains("TELEFONIA") || signals.contains("DANO MORAL") || signals.contains("CONSUMIDOR");
        boolean familySignal = signals.contains("ALIMENTOS") || signals.contains("GUARDA") || signals.contains("DIVORCIO");
        boolean evictionSignal = signals.contains("DESPEJO") || signals.contains("LOCACAO");
        return consumerSignal && (classe.contains("DESPEJO") || assunto.contains("DESPEJO"))
                || familySignal && (classe.contains("CONSUMIDOR") || assunto.contains("CONSUMIDOR"))
                || evictionSignal && (classe.contains("FAMILIA") || assunto.contains("FAMILIA"));
    }

    private int urgencyScore(PjbZeroErrorTriageRequest request, RitoProcessual rito) {
        String signals = normalize(String.join(" ", request.safePetitionSignals()) + " " + String.join(" ", request.safeDocumentSignals()));
        int score = request.pedidoTutelaUrgencia() ? 70 : 30;
        if (request.parteVulneravel()) score += 15;
        for (String term : URGENCY_TERMS) {
            if (signals.contains(term)) score += 14;
        }
        if (rito.isPenal()) {
            for (String term : PENAL_URGENCY_TERMS) {
                if (signals.contains(term)) score += 20;
            }
        }
        return Math.max(0, Math.min(99, score));
    }

    private PjbZeroErrorTriageIssue issue(String code, String severity, String message, String suggestedAction) {
        return new PjbZeroErrorTriageIssue(code, severity, message, suggestedAction);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.strip().toUpperCase(Locale.ROOT)
                .replace('Á', 'A').replace('À', 'A').replace('Â', 'A').replace('Ã', 'A')
                .replace('É', 'E').replace('Ê', 'E').replace('Í', 'I')
                .replace('Ó', 'O').replace('Ô', 'O').replace('Õ', 'O')
                .replace('Ú', 'U').replace('Ç', 'C');
    }

    private String digits(String value) {
        if (value == null) return "";
        return value.replaceAll("\\D", "");
    }
}
