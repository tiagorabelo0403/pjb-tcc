package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AgreementChatGovernanceService {

    public AgreementChannelPolicy analyze(Processo processo, PropostaAcordo proposta) {
        StatusAcordo status = proposta != null ? proposta.getStatus() : null;
        FaseProcessual fase = processo != null ? processo.getFaseAtual() : null;
        LinkedHashSet<String> allowedBands = new LinkedHashSet<>();
        allowedBands.add("MAGISTRATURA");
        allowedBands.add("ASSESSORIA");
        allowedBands.add("CONCILIACAO_MEDIACAO");
        allowedBands.add("ADVOCACIA");
        allowedBands.add("PROCURADORIA");
        allowedBands.add("DEFENSORIA");
        allowedBands.add("MINISTERIO_PUBLICO");
        allowedBands.add("CIDADAO_PARTE");
        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        guardrails.add("Negociação sensível deve manter linguagem objetiva, cláusulas executáveis e trilha de homologação íntegra.");
        if (fase != null && fase.isExecutionLike()) {
            guardrails.add("Na fase executória, privilegiar garantias reais, cronograma de cumprimento e prova de adimplemento.");
        }
        if (fase != null && fase.isRecursal()) {
            guardrails.add("Na fase recursal, qualquer ajuste deve indicar impacto no recurso, desistência ou perda superveniente do objeto.");
        }
        boolean judgeDecisionOpen = status == StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ;
        boolean freezeTermChanges = judgeDecisionOpen;
        if (judgeDecisionOpen) {
            guardrails.add("Com a proposta na mesa do magistrado, alterações materiais de valor, cronograma ou cláusulas ficam congeladas até decisão judicial.");
        }
        return new AgreementChannelPolicy(
                "ACORDO_PROCESSUAL",
                resolveStage(fase, status),
                List.copyOf(allowedBands),
                List.copyOf(guardrails),
                judgeDecisionOpen,
                freezeTermChanges
        );
    }

    public AgreementChannelPolicy enforcePost(Processo processo,
                                              PropostaAcordo proposta,
                                              Usuario usuario,
                                              String channel,
                                              String content) {
        AgreementChannelPolicy policy = analyze(processo, proposta);
        if (!isAgreementTraffic(channel, content)) {
            return policy;
        }
        if (usuario == null || usuario.getTipoUsuario() == null) {
            throw new RegraNegocioException("Canal negocial indisponível para remetente não identificado.");
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (!isSpeakerAllowed(tipo)) {
            throw new RegraNegocioException("Perfil sem alçada para manifestação no canal negocial do acordo.");
        }
        if (policy.freezeTermChanges() && isMaterialTermChange(content) && !canOverrideFrozenTerms(tipo)) {
            throw new RegraNegocioException("A proposta já está submetida ao magistrado. Alterações materiais devem aguardar decisão judicial ou retorno para revisão.");
        }
        if (policy.judgeDecisionOpen() && isExternalSpeaker(tipo) && isPressureLanguage(content)) {
            throw new RegraNegocioException("Canal negocial bloqueado para pressão indevida enquanto a homologação judicial estiver pendente.");
        }
        return policy;
    }

    private boolean isSpeakerAllowed(TipoUsuario tipo) {
        return tipo.isMagistratura()
                || tipo.isAssessor()
                || tipo.isConciliacaoMediacao()
                || tipo.isAdvocacia()
                || tipo.isProcuradoria()
                || tipo.isDefensoriaPublica()
                || tipo.isMinisterioPublico()
                || tipo.isServidorJudiciario()
                || tipo == TipoUsuario.CIDADAO;
    }

    private boolean canOverrideFrozenTerms(TipoUsuario tipo) {
        return tipo.isMagistratura() || tipo.isAssessor() || tipo.isConciliacaoMediacao();
    }

    private boolean isExternalSpeaker(TipoUsuario tipo) {
        return tipo.isAdvocacia() || tipo.isProcuradoria() || tipo.isDefensoriaPublica() || tipo == TipoUsuario.CIDADAO || tipo.isMinisterioPublico();
    }

    private boolean isPressureLanguage(String content) {
        String normalized = normalize(content);
        return containsAny(normalized, "publique imediatamente", "homologue agora", "pressionar", "urgente sem revisão", "sem revisar");
    }

    private boolean isMaterialTermChange(String content) {
        String normalized = normalize(content);
        return containsAny(normalized,
                "nova proposta",
                "contraproposta",
                "alterar valor",
                "novo valor",
                "parcelamento",
                "mudar cláusula",
                "mudar clausula",
                "cronograma",
                "minuta revisada",
                "revisar multa",
                "novos termos");
    }

    private boolean isAgreementTraffic(String channel, String content) {
        String normalizedChannel = normalize(channel);
        if (containsAny(normalizedChannel, "acordo", "negocial", "conciliacao", "mediacao")) {
            return true;
        }
        String normalized = normalize(content);
        return containsAny(normalized, "acordo", "homolog", "clausula", "minuta", "contraproposta", "parcela", "negocia");
    }

    private String resolveStage(FaseProcessual fase, StatusAcordo status) {
        ArrayList<String> parts = new ArrayList<>();
        if (fase != null) {
            parts.add(fase.name());
        }
        if (status != null) {
            parts.add(status.name());
        }
        return parts.isEmpty() ? "NEGOCIACAO_GERAL" : String.join("_", parts);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && value.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public record AgreementChannelPolicy(
            String channel,
            String stage,
            List<String> allowedSpeakerBands,
            List<String> guardrails,
            boolean judgeDecisionOpen,
            boolean freezeTermChanges
    ) {
        public AgreementChannelPolicy {
            Objects.requireNonNull(channel);
            Objects.requireNonNull(stage);
            allowedSpeakerBands = allowedSpeakerBands == null ? List.of() : List.copyOf(allowedSpeakerBands);
            guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        }
    }
}
