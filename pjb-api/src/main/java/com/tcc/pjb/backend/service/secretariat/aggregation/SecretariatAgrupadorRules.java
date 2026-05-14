package com.tcc.pjb.backend.service.secretariat.aggregation;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

public final class SecretariatAgrupadorRules {

    private SecretariatAgrupadorRules() {}

    public record AgrupadorInput(
            boolean temJusticaGratuitaSemApreciacao,
            boolean temTutelaLiminarNaoApreciada,
            boolean temHabilitacaoNaoLida,
            boolean temDocumentoNaoLido,
            boolean temMandadoDevolvido,
            boolean temPeticaoAvulsaNaoLida,
            boolean temSegredoNaoApreciado,
            boolean temPrazoCritico,
            boolean temAutuacaoInconsistente,
            boolean temRitoIncompativelComClasse,
            Duration tempoParado,
            Duration slaDoRito,
            boolean temExpedienteSemCiencia
    ) {}

    public Set<SecretariatAgrupadorTipo> avaliar(AgrupadorInput input) {
        Set<SecretariatAgrupadorTipo> resultado = EnumSet.noneOf(SecretariatAgrupadorTipo.class);
        if (input.temJusticaGratuitaSemApreciacao()) resultado.add(SecretariatAgrupadorTipo.JUSTICA_GRATUITA_NAO_APRECIADA);
        if (input.temTutelaLiminarNaoApreciada()) resultado.add(SecretariatAgrupadorTipo.TUTELA_LIMINAR_NAO_APRECIADA);
        if (input.temHabilitacaoNaoLida()) resultado.add(SecretariatAgrupadorTipo.HABILITACAO_NAO_LIDA);
        if (input.temDocumentoNaoLido()) resultado.add(SecretariatAgrupadorTipo.DOCUMENTO_NAO_LIDO);
        if (input.temMandadoDevolvido()) resultado.add(SecretariatAgrupadorTipo.MANDADO_DEVOLVIDO);
        if (input.temPeticaoAvulsaNaoLida()) resultado.add(SecretariatAgrupadorTipo.PETICAO_AVULSA_NAO_LIDA);
        if (input.temSegredoNaoApreciado()) resultado.add(SecretariatAgrupadorTipo.SEGREDO_NAO_APRECIADO);
        if (input.temPrazoCritico()) resultado.add(SecretariatAgrupadorTipo.PRAZO_CRITICO);
        if (input.temAutuacaoInconsistente()) resultado.add(SecretariatAgrupadorTipo.AUTUACAO_INCONSISTENTE);
        if (input.temRitoIncompativelComClasse()) resultado.add(SecretariatAgrupadorTipo.RITO_INCOMPATIVEL_COM_CLASSE);
        if (input.slaDoRito() != null && input.tempoParado().compareTo(input.slaDoRito()) > 0)
            resultado.add(SecretariatAgrupadorTipo.PROCESSO_PARADO_ACIMA_SLA);
        if (input.temExpedienteSemCiencia()) resultado.add(SecretariatAgrupadorTipo.EXPEDIENTE_SEM_CIENCIA);
        return resultado;
    }
}
