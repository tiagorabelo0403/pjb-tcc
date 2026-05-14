package com.tcc.pjb.backend.core.api;

public record PjbPaginationMetadata(
        int pagina,
        int tamanhoPagina,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima
) {
    public static PjbPaginationMetadata de(int pagina, int tamanho, long total) {
        int totalPags = tamanho > 0 ? (int) Math.ceil((double) total / tamanho) : 0;
        return new PjbPaginationMetadata(
                pagina, tamanho, total, totalPags, pagina == 0, pagina >= totalPags - 1);
    }
}
