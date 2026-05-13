package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class RecursalSlaPolicyCatalog {

    private final Map<RecursalLifecycleState, RecursalSlaPolicy> defaults = new EnumMap<>(RecursalLifecycleState.class);

    public RecursalSlaPolicyCatalog() {
        register(RecursalLifecycleState.INTERPOSTO, 1, true, "Governança recursal do protocolo e controle de tempestividade");
        register(RecursalLifecycleState.EM_SANEAMENTO_FORMAL, 2, true, "Governança cartorária do saneamento formal recursal");
        register(RecursalLifecycleState.PREPARO_EM_COMPLEMENTACAO, 5, true, "Janela de regularização do preparo e governança da secretaria");
        register(RecursalLifecycleState.PREPARO_CERTIFICADO, 2, true, "Governança cartorária pós-preparo e remessa para contrarrazões ou admissibilidade");
        register(RecursalLifecycleState.AGUARDANDO_CONTRARRAZOES, 15, true, "CPC arts. 1.003, §5º, e 1.010, §1º, quando compatíveis com a espécie");
        register(RecursalLifecycleState.ADMISSIBILIDADE_ORIGEM, 5, true, "CPC e regimento interno para admissibilidade na origem");
        register(RecursalLifecycleState.ADMISSIBILIDADE_DESTINO, 5, true, "CPC, legislação especial e regimento interno para admissibilidade no destino");
        register(RecursalLifecycleState.REMESSA_EM_CURSO, 2, false, "Governança da secretaria para remessa externa rastreável");
        register(RecursalLifecycleState.AUTOS_EM_TRANSITO, 2, false, "Governança da remessa entre órgãos judiciais e confirmação de recebimento");
        register(RecursalLifecycleState.REMESSA_DEVOLVIDA, 2, false, "Saneamento de inconsistência de remessa e reencaminhamento imediato");
        register(RecursalLifecycleState.AUTUADO_NO_DESTINO, 2, false, "Autuação e preparação distributiva no tribunal destinatário");
        register(RecursalLifecycleState.DISTRIBUIDO_NO_DESTINO, 2, false, "Distribuição e afetação do órgão julgador");
        register(RecursalLifecycleState.JULGAMENTO_MONOCRATICO, 30, false, "Regimento interno e gestão de gabinete/relatoria");
        register(RecursalLifecycleState.JULGAMENTO_COLEGIADO, 30, false, "Regimento interno e gestão de pauta colegiada");
        register(RecursalLifecycleState.PAUTA_SUSTENTACAO_DESIGNADA, 10, false, "Regimento interno e janela de sessão para sustentação oral");
        register(RecursalLifecycleState.SUSTENTACAO_REALIZADA, 5, false, "Conclusão imediata pós-sustentação para julgamento");
        register(RecursalLifecycleState.SOBRESTADO, 30, false, "Revisão periódica do sobrestamento e retorno controlado ao fluxo");
        register(RecursalLifecycleState.SOBRESTADO_POR_PRECEDENTE, 30, false, "Monitoramento de tema vinculante e retomada recursal");
        register(RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE, 5, false, "Aplicação ou distinguishing fundamentado do precedente vinculante");
        register(RecursalLifecycleState.PRECEDENTE_APLICADO, 2, false, "Retomada controlada após aplicação de precedente");
        register(RecursalLifecycleState.CASO_DISTINGUIDO, 2, false, "Retomada controlada após distinguishing fundamentado");
        register(RecursalLifecycleState.RETRATACAO, 5, false, "Janela de retratação na origem");
        register(RecursalLifecycleState.RETRATACAO_POR_PRECEDENTE, 5, false, "Retratação orientada por precedente vinculante");
        register(RecursalLifecycleState.DILIGENCIA_DETERMINADA, 10, false, "Cumprimento de diligência determinada no curso recursal");
        register(RecursalLifecycleState.SUSCITADO, 5, false, "Recebimento do conflito suscitado");
        register(RecursalLifecycleState.AGUARDANDO_RESOLUCAO_CONFLITO, 10, false, "Resolução do conflito de competência");
        register(RecursalLifecycleState.COMPETENCIA_DEFINIDA, 2, false, "Execução imediata da decisão definidora da competência");
        register(RecursalLifecycleState.RETORNO_AO_JUIZO_COMPETENTE, 2, false, "Retorno rápido dos autos ao juízo competente");
        register(RecursalLifecycleState.AGUARDANDO_REQUISICAO_PAGAMENTO_PUBLICO, 10, true, "Preparação de RPV ou precatório após provimento contra Fazenda Pública");
        register(RecursalLifecycleState.AGUARDANDO_PRECATORIO, 15, true, "Preparação específica da expedição de precatório");
        register(RecursalLifecycleState.RPV_EXPEDIDA, 30, true, "Esteira operacional da RPV até liberação do pagamento");
        register(RecursalLifecycleState.PRECATORIO_EXPEDIDO, 60, true, "Esteira operacional do precatório até liberação do pagamento");
        register(RecursalLifecycleState.PAGAMENTO_PUBLICO_LIBERADO, 5, true, "Baixa e certificação após liberação do pagamento público");
        register(RecursalLifecycleState.BAIXADO, 2, false, "Baixa técnica e certificação do estado final recursal");
    }

    public Optional<RecursalSlaPolicy> resolve(RecursalLifecycleState estado, RecursalTribunal tribunal) {
        if (estado == null || tribunal == null) {
            return Optional.empty();
        }
        RecursalSlaPolicy base = defaults.get(estado);
        if (base == null || base.estado().finalState() || estado == RecursalLifecycleState.RASCUNHO) {
            return Optional.empty();
        }
        return Optional.of(new RecursalSlaPolicy(base.estado(), tribunal, adjustDays(base.diasUteis(), tribunal, estado), base.fatalParaPartes(), base.fundamentoLegal()));
    }

    private void register(RecursalLifecycleState estado, int diasUteis, boolean fatalParaPartes, String fundamentoLegal) {
        defaults.put(estado, new RecursalSlaPolicy(estado, RecursalTribunal.TJ, diasUteis, fatalParaPartes, fundamentoLegal));
    }

    private int adjustDays(int diasUteis, RecursalTribunal tribunal, RecursalLifecycleState estado) {
        if (tribunal == RecursalTribunal.STF || tribunal == RecursalTribunal.STJ || tribunal == RecursalTribunal.TST || tribunal == RecursalTribunal.TSE || tribunal == RecursalTribunal.STM) {
            return switch (estado) {
                case REMESSA_EM_CURSO, AUTOS_EM_TRANSITO, AUTUADO_NO_DESTINO, DISTRIBUIDO_NO_DESTINO, COMPETENCIA_DEFINIDA, RETORNO_AO_JUIZO_COMPETENTE -> Math.max(diasUteis, 3);
                case JULGAMENTO_MONOCRATICO, JULGAMENTO_COLEGIADO, PAUTA_SUSTENTACAO_DESIGNADA, SUSTENTACAO_REALIZADA, SOBRESTADO_POR_PRECEDENTE, AGUARDANDO_APLICACAO_PRECEDENTE -> diasUteis + 5;
                default -> diasUteis;
            };
        }
        if (tribunal == RecursalTribunal.TNU) {
            return switch (estado) {
                case ADMISSIBILIDADE_DESTINO, JULGAMENTO_COLEGIADO, SOBRESTADO_POR_PRECEDENTE, AGUARDANDO_APLICACAO_PRECEDENTE -> diasUteis + 3;
                default -> diasUteis;
            };
        }
        return diasUteis;
    }
}
