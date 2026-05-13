package com.tcc.pjb.backend.tribunal.regras;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;
import com.tcc.pjb.backend.tribunal.perfil.PerfilInstanciaTribunalService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.tcc.pjb.backend.tribunal.regras.plugin.BucketRegraPack;
import com.tcc.pjb.backend.tribunal.regras.plugin.TipoPlugin;
import com.tcc.pjb.backend.tribunal.regras.spec.PluginManifest;


record LoadPlan(
            String pluginKey,
            String pluginId,
            String tribunalCodigo,
            String resolucao,
            TipoPlugin tipoPlugin,
            String versao,
            String origem,
            String operadorId,
            String hash,
            boolean federar,
            RamoDireito ramoContexto,
            GrauJurisdicao grauContexto,
            PluginManifest manifesto,
            List<String> dependencias,
            List<TribunalRuleEngine.EntradaRegra> regrasTribunal,
            Map<BucketRegraPack, List<NationalRulePackEngine.Regra>> regrasRulePack,
            List<CalendarioForenseTribunalService.EntradaCalendario> calendarioEntradas,
            List<CalendarioForenseTribunalService.PeriodoRecesso> recessoPeriods,
            Set<LocalDate> feriadosPrazoCombinados,
            boolean contarSabado,
            boolean integralmenteCorrido,
            PerfilInstanciaTribunalService.PerfilInstancia perfilInstancia,
            boolean ativarPerfil,
            List<String> avisos
    ) {}
