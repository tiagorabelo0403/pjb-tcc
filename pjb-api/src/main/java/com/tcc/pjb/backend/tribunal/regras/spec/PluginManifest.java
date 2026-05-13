package com.tcc.pjb.backend.tribunal.regras.spec;

import java.util.List;


    public record PluginManifest(
            String pluginId,
            String tribunalCodigo,
            String resolucao,
            String versao,
            String descricao,
            String tipoPlugin,
            List<String> dependencias,
            String origem,
            String operadorId,
            String ramo,
            String grau,
            Boolean federar,
            PrazoConfig prazoConfig,
            List<TribunalRuleSpec> tribunalRules,
            List<RulePackSpec> rulePackRules,
            List<CalendarioEntrySpec> calendarioEntries,
            List<CalendarioRecessoSpec> recessoPeriods,
            PerfilSpec perfil,
            List<RegraJSON> regras,
            List<FeriadoJSON> feriados,
            List<RecessoJSON> recessos
    ) {}
