package com.tcc.pjb.backend.service.criminal;

import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import java.util.Optional;

/**
 * Minuta de despacho para o recebimento de inquérito policial pelo juízo — só sugestão,
 * pendente de assinatura do magistrado via JuizGabineteDecisionalService.assinarDespacho
 * (que já separa conteúdo de fundamentação). Nunca produz efeito sozinha.
 *
 * Só gera minuta para o tipo de procedimento correto (INQUERITO_POLICIAL) e com número
 * de procedimento presente — para qualquer outro caso, o chamador deve cair no texto
 * genérico em vez de produzir um despacho com terminologia errada.
 */
final class InqueritoJudicialDespachoDraft {

    private static final String TIPO_INQUERITO_POLICIAL = "INQUERITO_POLICIAL";
    private static final int ORIGEM_MAX_LENGTH = 120;

    private static final String FUNDAMENTACAO =
            "CPP art. 28, na redação da Lei nº 13.964/2019 — vista dos autos ao Ministério "
                    + "Público após recebimento de inquérito policial pelo juízo.";

    private InqueritoJudicialDespachoDraft() {
    }

    record Minuta(String conteudo, String fundamentacao) {
    }

    static Optional<Minuta> gerar(InqueritoPolicialDigital inquerito) {
        if (inquerito == null || !isInqueritoPolicial(inquerito) || isBlank(inquerito.getNumeroProcedimento())) {
            return Optional.empty();
        }
        String numero = inquerito.getNumeroProcedimento().trim();
        String origem = origem(inquerito);
        String conteudo = """
                Vistos.

                1. Recebo o inquérito policial nº %s, oriundo de %s.
                2. Vista dos autos ao Ministério Público para as providências cabíveis no prazo legal.
                3. Cumpra-se.
                """.formatted(numero, origem);
        return Optional.of(new Minuta(conteudo, FUNDAMENTACAO));
    }

    private static boolean isInqueritoPolicial(InqueritoPolicialDigital inquerito) {
        return TIPO_INQUERITO_POLICIAL.equalsIgnoreCase(trim(inquerito.getTipo()));
    }

    private static String origem(InqueritoPolicialDigital inquerito) {
        String orgao = trim(inquerito.getOrgaoApuracao());
        if (orgao != null) {
            return cap(orgao);
        }
        String unidade = inquerito.getUnidadeApuracao() != null ? trim(inquerito.getUnidadeApuracao().getNome()) : null;
        if (unidade != null) {
            return cap(unidade);
        }
        return "autoridade policial";
    }

    private static String cap(String value) {
        return value.length() <= ORIGEM_MAX_LENGTH ? value : value.substring(0, ORIGEM_MAX_LENGTH);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
