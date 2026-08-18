package com.tcc.pjb.backend.integration.govbr;

public final class GovBrAssuranceLevel {

    public static final String BRONZE = "https://www.rnp.br/oidc/loa/1";
    public static final String PRATA  = "https://www.rnp.br/oidc/loa/2";
    public static final String OURO   = "https://www.rnp.br/oidc/loa/3";

    private GovBrAssuranceLevel() {}

    public static boolean meetsMinimum(String acr, String minimum) {
        int acrLevel = toLevel(acr);
        int minLevel = toLevel(minimum);
        if (acrLevel < 0 || minLevel < 0) {
            return false;
        }
        return acrLevel >= minLevel;
    }

    private static int toLevel(String acr) {
        if (BRONZE.equals(acr)) return 1;
        if (PRATA.equals(acr))  return 2;
        if (OURO.equals(acr))   return 3;
        return -1;
    }
}
