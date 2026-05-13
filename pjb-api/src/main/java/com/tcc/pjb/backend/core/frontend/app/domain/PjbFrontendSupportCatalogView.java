package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendSupportCatalogView(
        List<String> tipoUsuarios,
        List<String> ramosDireito,
        List<String> statusProcesso,
        List<String> ritoProcessual,
        List<String> authModes,
        List<String> menuDomains
) {
}
