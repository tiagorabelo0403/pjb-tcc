package com.tcc.pjb.backend.service.rito;

import com.tcc.pjb.backend.model.entity.enums.processual.NivelUrgenciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.secretariat.acceleration.SecretariatQueuePriorityPolicy;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Eleva (nunca reduz) a prioridade de um work item quando o próprio rito já é urgente por
 * definição legal — sem depender de classificação automática de conteúdo, só do dado
 * estruturado que o rito já carrega. A decisão continua sendo sempre do magistrado; esta
 * política só afeta em que ordem o caso aparece no painel.
 *
 * MAXIMA: risco à liberdade ou à vida (habeas corpus, Maria da Penha) — CF art. 5º, LXVIII;
 * Lei 11.340/06 art. 18/22.
 * ALTA: tutela de urgência/cautelar (CPC art. 300) e infracional de adolescente com prazo
 * legal de conclusão (ECA, Lei 8.069/90, art. 108).
 */
@Component
public class RitoUrgenciaPriorityPolicy {

    public static final int PRIORIDADE_MAXIMA = 1;
    public static final int PRIORIDADE_ALTA = 2;

    private static final String BASE_LEGAL_MARIA_DA_PENHA =
            "Lei 11.340/06 art. 18/22 — medida protetiva de urgência, apreciação em até 48h";
    private static final String BASE_LEGAL_HABEAS_CORPUS =
            "CF art. 5º, LXVIII — habeas corpus, remédio constitucional de urgência máxima contra ameaça à liberdade";
    private static final String BASE_LEGAL_TUTELA_URGENCIA =
            "CPC art. 300 — tutela de urgência (fumus boni iuris e periculum in mora)";
    private static final String BASE_LEGAL_ECA_INFRACIONAL =
            "ECA (Lei 8.069/90) art. 108 — internação provisória de adolescente limitada a 45 dias";

    public int prioridade(RitoProcessual rito, int prioridadeBase) {
        return switch (nivel(rito)) {
            case MAXIMA -> Math.min(prioridadeBase, PRIORIDADE_MAXIMA);
            case ALTA -> Math.min(prioridadeBase, PRIORIDADE_ALTA);
            case PADRAO -> prioridadeBase;
        };
    }

    public String baseLegalAdicional(RitoProcessual rito) {
        if (rito == null) {
            return null;
        }
        return switch (rito) {
            case PENAL_MARIA_DA_PENHA -> BASE_LEGAL_MARIA_DA_PENHA;
            case ESPECIAL_HABEAS_CORPUS, PENAL_HABEAS_CORPUS_PREVENTIVO, MILITAR_HABEAS_CORPUS_MILITAR ->
                    BASE_LEGAL_HABEAS_CORPUS;
            case CIVIL_TUTELA_URGENTE, CIVIL_TUTELA_CAUTELAR_ANTECEDENTE, CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE,
                    AMBIENTAL_TUTELA_URGENTE, TRABALHISTA_TUTELA_CAUTELAR -> BASE_LEGAL_TUTELA_URGENCIA;
            case INFANCIA_JUVENTUDE_INFRACIONAL, PENAL_ECA_INFRACIONAL -> BASE_LEGAL_ECA_INFRACIONAL;
            default -> null;
        };
    }

    /**
     * Traduz o rito nas mesmas tags que {@link SecretariatQueuePriorityPolicy} já sabe pontuar
     * na fila da secretaria/fórum — para o sinal chegar lá também, não só no painel do juiz.
     */
    public List<String> tagsSecretariat(RitoProcessual rito) {
        if (rito == null) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        NivelUrgenciaProcessual nivel = nivel(rito);
        if (nivel == NivelUrgenciaProcessual.MAXIMA || nivel == NivelUrgenciaProcessual.ALTA) {
            tags.add(SecretariatQueuePriorityPolicy.TAG_PRIORIDADE_LEGAL);
        }
        if (isRitoTutela(rito)) {
            tags.add(SecretariatQueuePriorityPolicy.TAG_TUTELA);
        }
        if (rito.requiresSegredoByDefault()) {
            tags.add(SecretariatQueuePriorityPolicy.TAG_SIGILO);
        }
        return List.copyOf(tags);
    }

    private boolean isRitoTutela(RitoProcessual rito) {
        return switch (rito) {
            case CIVIL_TUTELA_URGENTE, CIVIL_TUTELA_CAUTELAR_ANTECEDENTE, CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE,
                    AMBIENTAL_TUTELA_URGENTE, TRABALHISTA_TUTELA_CAUTELAR -> true;
            default -> false;
        };
    }

    private NivelUrgenciaProcessual nivel(RitoProcessual rito) {
        return rito == null ? NivelUrgenciaProcessual.PADRAO : rito.nivelUrgenciaPadrao();
    }
}
