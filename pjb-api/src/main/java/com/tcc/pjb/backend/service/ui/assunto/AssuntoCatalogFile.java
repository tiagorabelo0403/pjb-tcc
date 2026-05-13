package com.tcc.pjb.backend.service.ui.assunto;

import java.util.List;

public record AssuntoCatalogFile(
    int version,
    List<AssuntoGroup> groups
) {
}
