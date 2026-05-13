package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class RecursalTribunalPerfilRealCatalog {

    private final Map<RecursalTribunalDetalhado, RecursalTribunalPerfilReal> perfis;

    public RecursalTribunalPerfilRealCatalog(Map<RecursalTribunalDetalhado, RecursalTribunalPerfilReal> perfis) {
        this.perfis = Map.copyOf(perfis);
    }

    public static RecursalTribunalPerfilRealCatalog defaultCatalog() {
        return new RecursalTribunalPerfilRealCatalog(buildDefaultProfiles());
    }

    private static Map<RecursalTribunalDetalhado, RecursalTribunalPerfilReal> buildDefaultProfiles() {
        EnumMap<RecursalTribunalDetalhado, RecursalTribunalPerfilReal> perfis =
                new EnumMap<>(RecursalTribunalDetalhado.class);
        register(perfis, RecursalTribunalDetalhado.TJAC, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJAC_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJAL, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJAL_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJAP, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJAP_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJAM, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJAM_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJBA, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJBA_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJCE, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJCE_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJDFT, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJDFT_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJES, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJES_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJGO, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJGO_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJMA, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJMA_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJMT, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJMT_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJMS, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJMS_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJMG, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJMG_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJPA, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJPA_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJPB, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJPB_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJPR, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJPR_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJPE, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJPE_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJPI, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJPI_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJRJ, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJRJ_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJRN, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJRN_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJRS, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJRS_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJRO, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJRO_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJRR, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJRR_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJSC, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJSC_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJSP, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJSP_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJSE, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJSE_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TJTO, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.ORGAO_ESPECIAL, "TJTO_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRF1, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.CORTE_ESPECIAL, RecursalAuthority.CORTE_ESPECIAL, "TRF1_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRF2, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.CORTE_ESPECIAL, RecursalAuthority.CORTE_ESPECIAL, "TRF2_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRF3, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.CORTE_ESPECIAL, RecursalAuthority.CORTE_ESPECIAL, "TRF3_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRF4, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.CORTE_ESPECIAL, RecursalAuthority.CORTE_ESPECIAL, "TRF4_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRF5, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.CORTE_ESPECIAL, RecursalAuthority.CORTE_ESPECIAL, "TRF5_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRF6, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.CORTE_ESPECIAL, RecursalAuthority.CORTE_ESPECIAL, "TRF6_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT1, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT1_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT2, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT2_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT3, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT3_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT4, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT4_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT5, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT5_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT6, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT6_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT7, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT7_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT8, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT8_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT9, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT9_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT10, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT10_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT11, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT11_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT12, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT12_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT13, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT13_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT14, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT14_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT15, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT15_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT16, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT16_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT17, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT17_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT18, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT18_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT19, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT19_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT20, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT20_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT21, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT21_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT22, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT22_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT23, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT23_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRT24, RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TRT24_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREAC, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREAC_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREAL, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREAL_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREAP, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREAP_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREAM, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREAM_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREBA, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREBA_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRECE, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TRECE_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREDF, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREDF_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREES, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREES_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREGO, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREGO_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREMA, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREMA_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREMT, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREMT_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREMS, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREMS_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREMG, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREMG_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREPA, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREPA_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREPB, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREPB_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREPR, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREPR_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREPE, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREPE_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TREPI, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TREPI_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRERJ, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TRERJ_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRERN, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TRERN_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRERS, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TRERS_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRERO, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TRERO_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRERR, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TRERR_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRESC, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TRESC_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRESP, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TRESP_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRESE, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TRESE_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TRETO, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "TRETO_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.STJ, RecursalAuthority.PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.CORTE_ESPECIAL, "STJ_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TST, RecursalAuthority.PRESIDENCIA, RecursalAuthority.TRIBUNAL_PLENO, RecursalAuthority.TRIBUNAL_PLENO, "TST_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.STF, RecursalAuthority.PRESIDENCIA, RecursalAuthority.PLENARIO, RecursalAuthority.PLENARIO, "STF_RULE_PROFILE");
        register(perfis, RecursalTribunalDetalhado.TNU, RecursalAuthority.PRESIDENCIA, RecursalAuthority.TURMA, RecursalAuthority.TURMA, "TNU_RULE_PROFILE");
        return perfis;
    }

    private static void register(Map<RecursalTribunalDetalhado, RecursalTribunalPerfilReal> perfis,
                                 RecursalTribunalDetalhado tribunal,
                                 RecursalAuthority filingAuthority,
                                 RecursalAuthority internalReviewAuthority,
                                 RecursalAuthority specialReviewAuthority,
                                 String ruleProfile) {
        perfis.put(
                Objects.requireNonNull(tribunal, "tribunal"),
                new RecursalTribunalPerfilReal(
                        tribunal,
                        Objects.requireNonNull(filingAuthority, "filingAuthority"),
                        Objects.requireNonNull(internalReviewAuthority, "internalReviewAuthority"),
                        Objects.requireNonNull(specialReviewAuthority, "specialReviewAuthority"),
                        Objects.requireNonNull(ruleProfile, "ruleProfile")
                )
        );
    }

    public RecursalTribunalPerfilReal profileOf(RecursalCaseContext context) {
        Objects.requireNonNull(context, "context");
        return perfis.getOrDefault(
                context.tribunalDetalhadoOrigem(),
                perfis.get(RecursalTribunalDetalhado.fromFamily(context.tribunalOrigem()))
        );
    }
}
