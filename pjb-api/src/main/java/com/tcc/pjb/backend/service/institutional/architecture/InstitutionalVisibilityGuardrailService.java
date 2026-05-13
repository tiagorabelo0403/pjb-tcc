package com.tcc.pjb.backend.service.institutional.architecture;

import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalArchitectureResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalVisibilityGuardrailService {

    public AdminInstitutionalArchitectureResponse.VisibilitySimulation simulate(boolean sameJurisdictionUnit,
                                                                                boolean funcionalCompetence,
                                                                                boolean cooperativeGrantActive,
                                                                                boolean systemicSupervision,
                                                                                boolean breakGlassActive,
                                                                                boolean sigiloProcessual) {
        ArrayList<String> reasons = new ArrayList<>();
        ArrayList<String> restrictions = new ArrayList<>();
        if (sameJurisdictionUnit) {
            reasons.add("jurisdicao_local_compativel");
            restrictions.add("need_to_know");
            if (sigiloProcessual) {
                restrictions.add("sigilo_processual_reforcado");
                restrictions.add("mascaramento_contextual");
            }
            return new AdminInstitutionalArchitectureResponse.VisibilitySimulation(
                    "LOCAL",
                    "Visibilidade local",
                    true,
                    sigiloProcessual,
                    false,
                    List.copyOf(reasons),
                    List.copyOf(restrictions)
            );
        }
        if (funcionalCompetence) {
            reasons.add("competencia_funcional_reconhecida_pelo_processo");
            restrictions.add("somente_processo_autorizado");
            restrictions.add("escopo_contextual_automatico");
            if (sigiloProcessual) {
                restrictions.add("sigilo_processual_reforcado");
            }
            return new AdminInstitutionalArchitectureResponse.VisibilitySimulation(
                    "FUNCIONAL",
                    "Visibilidade por competência funcional",
                    true,
                    true,
                    false,
                    List.copyOf(reasons),
                    List.copyOf(restrictions)
            );
        }
        if (cooperativeGrantActive) {
            reasons.add("cooperacao_institucional_temporaria_ativa");
            restrictions.add("janela_temporal_limitada");
            restrictions.add("somente_ato_precatorio_ou_cooperado");
            if (sigiloProcessual) {
                restrictions.add("sigilo_processual_reforcado");
            }
            return new AdminInstitutionalArchitectureResponse.VisibilitySimulation(
                    "COOPERACAO",
                    "Visibilidade por cooperação institucional",
                    true,
                    true,
                    true,
                    List.copyOf(reasons),
                    List.copyOf(restrictions)
            );
        }
        if (systemicSupervision) {
            reasons.add("supervisao_sistemica_finalistica");
            restrictions.add("auditoria_obrigatoria");
            restrictions.add("finalidade_correcional_ou_cnj");
            if (sigiloProcessual) {
                restrictions.add("sigilo_processual_reforcado");
            }
            return new AdminInstitutionalArchitectureResponse.VisibilitySimulation(
                    "SISTEMICA",
                    "Visibilidade sistêmica",
                    true,
                    true,
                    false,
                    List.copyOf(reasons),
                    List.copyOf(restrictions)
            );
        }
        if (breakGlassActive) {
            reasons.add("break_glass_institucional_ativo");
            restrictions.add("fundamentacao_obrigatoria");
            restrictions.add("expiracao_automatica");
            restrictions.add("auditoria_reforcada");
            return new AdminInstitutionalArchitectureResponse.VisibilitySimulation(
                    "BREAK_GLASS",
                    "Break-glass institucional",
                    true,
                    true,
                    true,
                    List.copyOf(reasons),
                    List.copyOf(restrictions)
            );
        }
        reasons.add("ausencia_de_vinculo_topologico_ou_funcional");
        restrictions.add("acesso_negado");
        return new AdminInstitutionalArchitectureResponse.VisibilitySimulation(
                "NEGADO",
                "Acesso negado",
                false,
                false,
                false,
                List.copyOf(reasons),
                List.copyOf(restrictions)
        );
    }

    public List<AdminInstitutionalArchitectureResponse.VisibilityTier> tiers() {
        return List.of(
                new AdminInstitutionalArchitectureResponse.VisibilityTier(1, "LOCAL", "Visibilidade local", "jurisdicao_default_da_unidade", false),
                new AdminInstitutionalArchitectureResponse.VisibilityTier(2, "FUNCIONAL", "Visibilidade por competência funcional", "processo_autoriza_pelo_inbox_topologico", true),
                new AdminInstitutionalArchitectureResponse.VisibilityTier(3, "COOPERACAO", "Visibilidade por cooperação institucional", "vinculo_temporario_auditado", true),
                new AdminInstitutionalArchitectureResponse.VisibilityTier(4, "SISTEMICA", "Visibilidade sistêmica", "escopo_correcional_ou_cnj_com_finalidade", true)
        );
    }
}
