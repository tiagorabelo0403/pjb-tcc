package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class RecursalLocalRegimentalPolicyCatalog {

    private final Map<RecursalTribunalDetalhado, RecursalLocalRegimentalPolicy> policies;

    public RecursalLocalRegimentalPolicyCatalog(Map<RecursalTribunalDetalhado, RecursalLocalRegimentalPolicy> policies) {
        this.policies = Map.copyOf(policies);
    }

    public static RecursalLocalRegimentalPolicyCatalog defaultCatalog() {
        EnumMap<RecursalTribunalDetalhado, RecursalLocalRegimentalPolicy> map = new EnumMap<>(RecursalTribunalDetalhado.class);
        RecursalLocalRegimentalPolicy defaultTj = new RecursalLocalRegimentalPolicy(
                RecursalAuthority.VICE_PRESIDENCIA,
                RecursalAuthority.ORGAO_ESPECIAL,
                RecursalAuthority.CAMARA,
                RecursalAuthority.TURMA,
                true,
                true
        );
        RecursalLocalRegimentalPolicy defaultTrf = new RecursalLocalRegimentalPolicy(
                RecursalAuthority.VICE_PRESIDENCIA,
                RecursalAuthority.SECAO,
                RecursalAuthority.TURMA,
                RecursalAuthority.TURMA,
                true,
                true
        );
        RecursalLocalRegimentalPolicy defaultTrt = new RecursalLocalRegimentalPolicy(
                RecursalAuthority.VICE_PRESIDENCIA,
                RecursalAuthority.TRIBUNAL_PLENO,
                RecursalAuthority.TURMA,
                RecursalAuthority.TURMA,
                true,
                true
        );
        RecursalLocalRegimentalPolicy defaultTre = new RecursalLocalRegimentalPolicy(
                RecursalAuthority.PRESIDENCIA,
                RecursalAuthority.PLENARIO,
                RecursalAuthority.PLENARIO,
                RecursalAuthority.PLENARIO,
                true,
                false
        );
        RecursalLocalRegimentalPolicy defaultStj = new RecursalLocalRegimentalPolicy(
                RecursalAuthority.PRESIDENCIA,
                RecursalAuthority.CORTE_ESPECIAL,
                RecursalAuthority.CORTE_ESPECIAL,
                RecursalAuthority.TURMA,
                true,
                false
        );
        RecursalLocalRegimentalPolicy defaultTst = new RecursalLocalRegimentalPolicy(
                RecursalAuthority.PRESIDENCIA,
                RecursalAuthority.TRIBUNAL_PLENO,
                RecursalAuthority.TRIBUNAL_PLENO,
                RecursalAuthority.TURMA,
                true,
                false
        );
        RecursalLocalRegimentalPolicy defaultStf = new RecursalLocalRegimentalPolicy(
                RecursalAuthority.PRESIDENCIA,
                RecursalAuthority.PLENARIO,
                RecursalAuthority.PLENARIO,
                RecursalAuthority.TURMA,
                true,
                false
        );
        RecursalLocalRegimentalPolicy defaultTse = new RecursalLocalRegimentalPolicy(
                RecursalAuthority.PRESIDENCIA,
                RecursalAuthority.PLENARIO,
                RecursalAuthority.PLENARIO,
                RecursalAuthority.PLENARIO,
                true,
                false
        );
        RecursalLocalRegimentalPolicy defaultStm = new RecursalLocalRegimentalPolicy(
                RecursalAuthority.PRESIDENCIA,
                RecursalAuthority.PLENARIO,
                RecursalAuthority.PLENARIO,
                RecursalAuthority.PLENARIO,
                true,
                false
        );
        RecursalLocalRegimentalPolicy defaultTnu = new RecursalLocalRegimentalPolicy(
                RecursalAuthority.PRESIDENCIA,
                RecursalAuthority.TURMA,
                RecursalAuthority.TURMA,
                RecursalAuthority.TURMA,
                true,
                false
        );
        for (RecursalTribunalDetalhado value : RecursalTribunalDetalhado.values()) {
            map.put(value, switch (value.familia()) {
                case TJ -> defaultTj;
                case TRF -> defaultTrf;
                case TRT -> defaultTrt;
                case TRE -> defaultTre;
                case STJ -> defaultStj;
                case TST -> defaultTst;
                case TSE -> defaultTse;
                case STM -> defaultStm;
                case TNU -> defaultTnu;
                case STF -> defaultStf;
            });
        }
        map.put(RecursalTribunalDetalhado.TJSP, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.CAMARA, RecursalAuthority.SECAO, true, true));
        map.put(RecursalTribunalDetalhado.TJRJ, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.CAMARA, RecursalAuthority.CAMARA, true, true));
        map.put(RecursalTribunalDetalhado.TJMG, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.CAMARA, RecursalAuthority.CAMARA, true, true));
        map.put(RecursalTribunalDetalhado.TJRS, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.CAMARA, RecursalAuthority.CAMARA, true, true));
        map.put(RecursalTribunalDetalhado.TJPR, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.CAMARA, RecursalAuthority.CAMARA, true, true));
        map.put(RecursalTribunalDetalhado.TJDFT, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.ORGAO_ESPECIAL, RecursalAuthority.CAMARA, RecursalAuthority.TURMA, true, true));
        map.put(RecursalTribunalDetalhado.TRF1, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.SECAO, RecursalAuthority.TURMA, RecursalAuthority.TURMA, true, true));
        map.put(RecursalTribunalDetalhado.TRF2, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.SECAO, RecursalAuthority.TURMA, RecursalAuthority.TURMA, true, true));
        map.put(RecursalTribunalDetalhado.TRF3, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.SECAO, RecursalAuthority.TURMA, RecursalAuthority.TURMA, true, true));
        map.put(RecursalTribunalDetalhado.TRF4, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.SECAO, RecursalAuthority.TURMA, RecursalAuthority.TURMA, true, true));
        map.put(RecursalTribunalDetalhado.TRF5, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.SECAO, RecursalAuthority.TURMA, RecursalAuthority.TURMA, true, true));
        map.put(RecursalTribunalDetalhado.TRF6, new RecursalLocalRegimentalPolicy(RecursalAuthority.VICE_PRESIDENCIA, RecursalAuthority.SECAO, RecursalAuthority.TURMA, RecursalAuthority.TURMA, true, true));
        map.put(RecursalTribunalDetalhado.STJ, defaultStj);
        map.put(RecursalTribunalDetalhado.STF, defaultStf);
        map.put(RecursalTribunalDetalhado.TST, defaultTst);
        return new RecursalLocalRegimentalPolicyCatalog(map);
    }

    public RecursalLocalRegimentalPolicy policyOf(RecursalTribunalDetalhado tribunalDetalhado) {
        Objects.requireNonNull(tribunalDetalhado, "tribunalDetalhado");
        return policies.getOrDefault(tribunalDetalhado, policies.get(RecursalTribunalDetalhado.fromFamily(tribunalDetalhado.familia())));
    }
}
