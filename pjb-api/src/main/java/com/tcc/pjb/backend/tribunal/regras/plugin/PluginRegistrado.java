package com.tcc.pjb.backend.tribunal.regras.plugin;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.tribunal.regras.spec.PluginManifest;


    public record PluginRegistrado(
            String pluginKey,
            String pluginId,
            String tribunalCodigo,
            String resolucao,
            String versao,
            TipoPlugin tipoPlugin,
            StatusPlugin status,
            PluginManifest manifesto,
            PluginSnapshot snapshot,
            List<String> dependencias,
            String hashConteudo,
            Instant carregadoEm,
            String carregadoPor,
            List<String> erros,
            List<String> avisos
    ) {
        public boolean ativo() {
            return snapshot != null && snapshot.ativo();
        }
    }
