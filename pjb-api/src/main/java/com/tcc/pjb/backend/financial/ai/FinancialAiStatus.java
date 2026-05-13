package com.tcc.pjb.backend.financial.ai;

import com.tcc.pjb.backend.ai.contract.IAResponse;

public enum FinancialAiStatus {

    SUCCESS,
    ALERT,
    ERROR,
    INDETERMINATE;

    public static FinancialAiStatus from(IAResponse.StatusIA status) {
        if (status == null) {
            return INDETERMINATE;
        }
        return switch (status) {
            case SUCESSO -> SUCCESS;
            case ALERTA -> ALERT;
            case ERRO -> ERROR;
            case INDETERMINADO -> INDETERMINATE;
        };
    }
}
