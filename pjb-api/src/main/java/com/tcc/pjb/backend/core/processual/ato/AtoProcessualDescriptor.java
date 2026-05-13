package com.tcc.pjb.backend.core.processual.ato;

import com.tcc.pjb.backend.model.entity.enums.WorkItemType;

public record AtoProcessualDescriptor(
        String codigo,
        String titulo,
        AtoProcessualCategoria categoria,
        WorkItemType workItemType,
        String filaPadrao,
        String inboxPadrao,
        String fundamentoPadrao,
        AtoProcessualSecurityProfile securityProfile
) {

    public AtoProcessualDescriptor(String codigo, String titulo, AtoProcessualCategoria categoria, WorkItemType workItemType, String filaPadrao, String inboxPadrao, String fundamentoPadrao) {
        this(codigo, titulo, categoria, workItemType, filaPadrao, inboxPadrao, fundamentoPadrao, null);
    }

    public boolean requiresElevatedSecurity() {
        return securityProfile != null && securityProfile.requiresElevatedSecurity();
    }
}
