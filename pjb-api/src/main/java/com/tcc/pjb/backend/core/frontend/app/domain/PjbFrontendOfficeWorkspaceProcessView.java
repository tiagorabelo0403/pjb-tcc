package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendOfficeWorkspaceProcessView(
        Long processoId,
        String numeroProcesso,
        Long equipeId,
        String equipeNome,
        Long ownerUserId,
        String ownerNome,
        String ramoDireito,
        String nivelSigilo,
        String statusProcesso,
        boolean officeOwned,
        boolean visibleInWorkspace,
        boolean sensitive,
        boolean ownPersonalCase,
        boolean patronCertificateRequired,
        List<String> blockers,
        List<String> warnings
) {

    public Long getProcessoId() {
        return processoId();
    }

    public String getNumeroProcesso() {
        return numeroProcesso();
    }

    public Long getEquipeId() {
        return equipeId();
    }

    public String getEquipeNome() {
        return equipeNome();
    }

    public Long getOwnerUserId() {
        return ownerUserId();
    }

    public String getOwnerNome() {
        return ownerNome();
    }

    public String getRamoDireito() {
        return ramoDireito();
    }

    public String getNivelSigilo() {
        return nivelSigilo();
    }

    public String getStatusProcesso() {
        return statusProcesso();
    }

    public boolean isOfficeOwned() {
        return officeOwned();
    }

    public boolean getOfficeOwned() {
        return officeOwned();
    }

    public boolean isVisibleInWorkspace() {
        return visibleInWorkspace();
    }

    public boolean getVisibleInWorkspace() {
        return visibleInWorkspace();
    }

    public boolean isSensitive() {
        return sensitive();
    }

    public boolean getSensitive() {
        return sensitive();
    }

    public boolean isOwnPersonalCase() {
        return ownPersonalCase();
    }

    public boolean getOwnPersonalCase() {
        return ownPersonalCase();
    }

    public boolean isPatronCertificateRequired() {
        return patronCertificateRequired();
    }

    public boolean getPatronCertificateRequired() {
        return patronCertificateRequired();
    }

    public List<String> getBlockers() {
        return blockers();
    }

    public List<String> getWarnings() {
        return warnings();
    }

    public boolean isPatronSigningContext() {
        return patronCertificateRequired();
    }

    public boolean patronSigningContext() {
        return patronCertificateRequired();
    }
}
