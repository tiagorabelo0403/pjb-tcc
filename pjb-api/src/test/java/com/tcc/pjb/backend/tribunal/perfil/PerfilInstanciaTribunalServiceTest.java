package com.tcc.pjb.backend.tribunal.perfil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PerfilInstanciaTribunalServiceTest {

    @Test
    void deveRemoverBindingPluginAntigoDoMesmoTribunal() {
        PerfilInstanciaTribunalService service = new PerfilInstanciaTribunalService();
        PerfilInstanciaTribunalService.PerfilInstancia base = perfil("TJX", "Base Tribunal", "TJX");
        PerfilInstanciaTribunalService.PerfilInstancia pluginA = perfil("TJX", "Plugin A", "TJX");
        PerfilInstanciaTribunalService.PerfilInstancia pluginB = perfil("TJX", "Plugin B", "TJX");

        service.cadastrar(base);
        service.substituirPerfilPlugin("plugin-a", pluginA);
        service.substituirPerfilPlugin("plugin-b", pluginB);

        assertEquals("Plugin B", service.porCodigo("TJX").orElseThrow().tribunalNome());

        service.removerPerfilPlugin("plugin-a");

        assertEquals("Plugin B", service.porCodigo("TJX").orElseThrow().tribunalNome());

        service.removerPerfilPlugin("plugin-b");

        assertEquals("Base Tribunal", service.porCodigo("TJX").orElseThrow().tribunalNome());
    }

    private static PerfilInstanciaTribunalService.PerfilInstancia perfil(String codigo, String nome, String sigla) {
        return new PerfilInstanciaTribunalService.PerfilInstancia(
                codigo,
                nome,
                sigla,
                "CE",
                PerfilInstanciaTribunalService.RamoJustica.ESTADUAL,
                PerfilInstanciaTribunalService.GrauInstancia.PRIMEIRO_GRAU,
                PerfilInstanciaTribunalService.IdentidadeVisual.padrao(),
                java.util.Map.of(),
                java.util.Map.of(),
                PerfilInstanciaTribunalService.ConfiguracaoUx.padrao(),
                PerfilInstanciaTribunalService.ContatoInstitucional.vazio()
        );
    }
}
