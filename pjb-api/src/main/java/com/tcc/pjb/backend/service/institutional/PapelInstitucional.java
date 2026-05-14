package com.tcc.pjb.backend.service.institutional;

import java.util.Set;

public record PapelInstitucional(
        String codigo,
        String descricao,
        Set<String> permissoes,
        int nivelHierarquico,
        boolean podeAssinar,
        boolean podeDecidir,
        boolean podeMovimentar,
        boolean podeAcessarSigiloso,
        boolean podeUsarBreakGlass
) {

    public PapelInstitucional {
        permissoes = Set.copyOf(permissoes);
    }

    public boolean temPermissao(String permissao) {
        return permissoes.contains(permissao);
    }

    public static PapelInstitucional magistrado() {
        return new PapelInstitucional("MAGISTRADO", "Magistrado",
                Set.of("DECIDIR", "ASSINAR", "MOVIMENTAR", "ACESSAR_SIGILOSO", "BREAK_GLASS"),
                10, true, true, true, true, true);
    }

    public static PapelInstitucional servidorJudiciario() {
        return new PapelInstitucional("SERVIDOR_JUDICIARIO", "Servidor Judiciário",
                Set.of("MOVIMENTAR", "ASSINAR_TERMO"),
                5, true, false, true, false, false);
    }

    public static PapelInstitucional assessor() {
        return new PapelInstitucional("ASSESSOR", "Assessor de Gabinete",
                Set.of("MINUTA", "MOVIMENTAR"),
                6, false, false, true, false, false);
    }

    public static PapelInstitucional substituto() {
        return new PapelInstitucional("MAGISTRADO_SUBSTITUTO", "Magistrado Substituto",
                Set.of("DECIDIR", "ASSINAR", "MOVIMENTAR", "ACESSAR_SIGILOSO"),
                9, true, true, true, true, false);
    }
}
