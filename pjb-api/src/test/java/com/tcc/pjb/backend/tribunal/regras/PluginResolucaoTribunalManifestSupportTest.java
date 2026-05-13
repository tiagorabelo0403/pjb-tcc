package com.tcc.pjb.backend.tribunal.regras;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;
import com.tcc.pjb.backend.tribunal.perfil.PerfilInstanciaTribunalService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.tribunal.regras.plugin.TipoPlugin;
import com.tcc.pjb.backend.tribunal.regras.spec.ContatoSpec;
import com.tcc.pjb.backend.tribunal.regras.spec.PerfilSpec;
import com.tcc.pjb.backend.tribunal.regras.spec.PluginManifest;
import com.tcc.pjb.backend.tribunal.regras.spec.PrazoConfig;
import com.tcc.pjb.backend.tribunal.regras.spec.UxSpec;
import com.tcc.pjb.backend.tribunal.regras.spec.VisualSpec;

class PluginResolucaoTribunalManifestSupportTest {

    private final PerfilInstanciaTribunalService perfilInstanciaTribunalService = mock(PerfilInstanciaTribunalService.class);
    private final PluginResolucaoTribunalManifestSupport support = new PluginResolucaoTribunalManifestSupport(new ObjectMapper(), perfilInstanciaTribunalService);

    @Test
    void deveMesclarManifestoLegadoEInferirTipoCompleto() {
        PluginManifest manifest = support.readManifest("""
                {
                  "tribunalCodigo": "tjce",
                  "resolucao": "Res 12/2026",
                  "versao": "2.1.0",
                  "regras": [
                    {
                      "chave": "prazo_sentenca",
                      "nivel": "TRIBUNAL",
                      "valor": 15,
                      "tipo": "INTEIRO"
                    }
                  ],
                  "feriados": [
                    {
                      "data": "2026-12-08",
                      "tipo": "FERIADO",
                      "descricao": "Nossa Senhora"
                    }
                  ]
                }
                """);

        assertThat(support.mergedTribunalRuleSpecs(manifest)).hasSize(1);
        assertThat(support.mergedCalendarioEntrySpecs(manifest)).hasSize(1);
        assertThat(support.resolveTipoPlugin(manifest)).isEqualTo(TipoPlugin.COMPLETO);
        assertThat(support.derivePluginId(manifest, "TJCE", "Res 12/2026")).startsWith("TJCE_RES_12_2026_COMPLETO_");
    }

    @Test
    void deveMesclarPerfilComFallbackInstitucionalEContexto() {
        PerfilInstanciaTribunalService.PerfilInstancia base = new PerfilInstanciaTribunalService.PerfilInstancia(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "TJCE",
                "CE",
                PerfilInstanciaTribunalService.RamoJustica.ESTADUAL,
                PerfilInstanciaTribunalService.GrauInstancia.PRIMEIRO_GRAU,
                new PerfilInstanciaTribunalService.IdentidadeVisual("#1", "#2", "#3", "#4", "#5", "brasao", "logo", "fav", "fonte", "rodape", true, true),
                Map.of(PerfilInstanciaTribunalService.TermoPadrao.JUIZ, "Magistrado"),
                Map.of(PerfilInstanciaTribunalService.TermoPadrao.JUIZ, "Magistrados"),
                new PerfilInstanciaTribunalService.ConfiguracaoUx(true, true, "0000", true, true, true, false, false, "America/Fortaleza", "dd/MM/yyyy", "BRL", 25, true, true, true, "AA"),
                new PerfilInstanciaTribunalService.ContatoInstitucional("site", "mail", "fone", "end", "cep", "cidade", "CE", "8-17", "ouv")
        );
        when(perfilInstanciaTribunalService.resolverPorCodigoOuPadrao("TJCE")).thenReturn(base);

        PerfilSpec spec = new PerfilSpec(
                "Tribunal de Justiça do Ceará - Digital",
                null,
                null,
                null,
                null,
                new VisualSpec(null, null, "#ABC", null, null, null, null, null, null, null, null, false),
                Map.of("magistrado", "Juiz"),
                Map.of("magistrado", "Juízes"),
                new UxSpec(null, null, null, null, null, null, true, null, null, null, null, null, null, null, null, null),
                new ContatoSpec(null, "suporte@tjce.jus.br", null, null, null, null, null, null, null),
                true
        );

        PerfilInstanciaTribunalService.PerfilInstancia perfil = support.converterPerfil(spec, "TJCE", RamoDireito.CIVIL, GrauJurisdicao.PRIMEIRO_GRAU);

        assertThat(perfil.tribunalNome()).isEqualTo("Tribunal de Justiça do Ceará - Digital");
        assertThat(perfil.ramo()).isEqualTo(PerfilInstanciaTribunalService.RamoJustica.ESTADUAL);
        assertThat(perfil.grau()).isEqualTo(PerfilInstanciaTribunalService.GrauInstancia.PRIMEIRO_GRAU);
        assertThat(perfil.visual().corAcento()).isEqualTo("#ABC");
        assertThat(perfil.ux().habilitaNotificacaoWhatsApp()).isTrue();
        assertThat(perfil.contato().email()).isEqualTo("suporte@tjce.jus.br");
        assertThat(perfil.terminologia().get(PerfilInstanciaTribunalService.TermoPadrao.JUIZ)).isEqualTo("Juiz");
    }

    @Test
    void deveCombinarFeriadosDePrazoCalendarioERecessoSemDuplicar() {
        PrazoConfig prazoConfig = new PrazoConfig(true, false, List.of("2026-12-08", "2026-12-09"));
        List<CalendarioForenseTribunalService.EntradaCalendario> entradas = List.of(
                new CalendarioForenseTribunalService.EntradaCalendario("TJCE", LocalDate.of(2026, 12, 8), CalendarioForenseTribunalService.TipoEntrada.FERIADO_NACIONAL, "Feriado", true, true, CalendarioForenseTribunalService.Recorrencia.UNICA, null, null, null, null, "plugin"),
                new CalendarioForenseTribunalService.EntradaCalendario("TJCE", LocalDate.of(2026, 12, 10), CalendarioForenseTribunalService.TipoEntrada.FERIADO_NACIONAL, "Outro", true, true, CalendarioForenseTribunalService.Recorrencia.UNICA, null, null, null, null, "plugin")
        );
        List<CalendarioForenseTribunalService.PeriodoRecesso> recessos = List.of(
                new CalendarioForenseTribunalService.PeriodoRecesso("TJCE", "Recesso", LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 22), true, null, null, null, "plugin")
        );

        Set<LocalDate> datas = support.mergeFeriadosPrazo(prazoConfig, entradas, recessos);

        assertThat(datas).containsExactlyInAnyOrder(
                LocalDate.of(2026, 12, 8),
                LocalDate.of(2026, 12, 9),
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 20),
                LocalDate.of(2026, 12, 21),
                LocalDate.of(2026, 12, 22)
        );
        assertThat(support.countPrazoFeriadosExplicitos(prazoConfig)).isEqualTo(2);
    }
}
