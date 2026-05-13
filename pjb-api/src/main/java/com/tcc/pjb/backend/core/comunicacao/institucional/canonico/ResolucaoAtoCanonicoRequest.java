package com.tcc.pjb.backend.core.comunicacao.institucional.canonico;

import java.text.Normalizer;
import java.util.Locale;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public record ResolucaoAtoCanonicoRequest(
        Long processoId,
        String processoNumero,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        FaseProcessual faseProcessual,
        String classeProcessual,
        String assunto,
        String objetoProcessual,
        String pedidoPrincipal,
        String uf,
        String comarca,
        String foro,
        boolean presencaIncapaz,
        boolean interesseCriancaAdolescente,
        boolean reuPresoOuCustodiado,
        boolean periciaNecessaria,
        boolean estudoPsicossocialNecessario,
        boolean derivacaoCejusc,
        boolean cooperacaoJudicial,
        boolean fazendaPublicaNoPolo,
        boolean demandaColetiva,
        boolean falenciaOuRecuperacao,
        boolean curadoriaEspecial,
        boolean conselhoTutelarNecessario,
        boolean orgaoTecnicoConveniadoNecessario,
        boolean cartorioExtrajudicialNecessario,
        boolean contadoriaJudicialNecessaria,
        boolean audienciaDesignada
) {
    public ResolucaoAtoCanonicoRequest {
        processoNumero = normalizeOptional(processoNumero);
        classeProcessual = normalizeOptional(classeProcessual);
        assunto = normalizeOptional(assunto);
        objetoProcessual = normalizeOptional(objetoProcessual);
        pedidoPrincipal = normalizeOptional(pedidoPrincipal);
        uf = normalizeOptionalUpper(uf);
        comarca = normalizeOptional(comarca);
        foro = normalizeOptional(foro);
    }

    public String corpus() {
        return String.join(" ",
                nullToEmpty(classeProcessual),
                nullToEmpty(assunto),
                nullToEmpty(objetoProcessual),
                nullToEmpty(pedidoPrincipal))
                .toLowerCase(Locale.ROOT);
    }

    public boolean isFamiliaOuInfancia() {
        return ramoDireito == RamoDireito.FAMILIA || ramoDireito == RamoDireito.INFANCIA_JUVENTUDE;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeOptionalUpper(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
