package com.tcc.pjb.backend.tribunal.regras.plugin;

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


    public record PluginSnapshot(
            String pluginKey,
            String pluginId,
            String tribunalCodigo,
            String resolucao,
            String versao,
            TipoPlugin tipoPlugin,
            StatusPlugin status,
            String origem,
            String operadorId,
            String hashSha256,
            Instant carregadoEm,
            boolean federado,
            boolean possuiConfiguracaoPrazo,
            RamoDireito ramoContexto,
            GrauJurisdicao grauContexto,
            Set<BucketRegraPack> bucketsRulePack,
            int totalRegrasTribunal,
            int totalRegrasRulePack,
            int totalFeriados,
            int totalCalendarioEntradas,
            int totalRecessos,
            int ignorados,
            List<String> erros,
            List<String> avisos
    ) {
        public boolean ativo() {
            return status == StatusPlugin.CARREGADO;
        }

        public String resumo() {
            return tribunalCodigo + " " + pluginId + " v" + versao + " [" + status + "] regras=" + totalRegrasTribunal + ", rulePack=" + totalRegrasRulePack + ", calendario=" + totalCalendarioEntradas + ", recessos=" + totalRecessos;
        }
    }
