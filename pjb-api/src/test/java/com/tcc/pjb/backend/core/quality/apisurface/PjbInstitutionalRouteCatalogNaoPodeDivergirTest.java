package com.tcc.pjb.backend.core.quality.apisurface;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * O catálogo de rotas institucionais existe hoje em dois lugares: `core.comunicacao.institucional` e o
 * holder legado da superfície, que ainda tem uma classe interna de mesmo nome simples
 * ({@code InstitutionalApiRoutes}). Dos 17 controllers institucionais, 5 importam o primeiro e 12 o
 * segundo — e como o nome simples é igual, o {@code @RequestMapping} dos doze lê exatamente como o dos
 * cinco sem apontar para a mesma classe.
 *
 * <p>Enquanto os dois catálogos coexistirem, editar um e esquecer o outro muda a rota de uma parte dos
 * controllers em silêncio. A comparação aqui é por <b>valor resolvido</b>, não por texto da expressão:
 * constante derivada como {@code PATH_X + "/sufixo"} só prova igualdade depois de resolvida.
 */
class PjbInstitutionalRouteCatalogNaoPodeDivergirTest {

    private static Map<String, String> constantesDe(Class<?> tipo) throws IllegalAccessException {
        Map<String, String> valores = new LinkedHashMap<>();
        for (Field campo : tipo.getDeclaredFields()) {
            if (Modifier.isStatic(campo.getModifiers()) && campo.getType() == String.class) {
                campo.setAccessible(true);
                valores.put(campo.getName(), (String) campo.get(null));
            }
        }
        return valores;
    }

    private static Map<String, String> divergencias(Map<String, String> canonico, Map<String, String> outro) {
        Map<String, String> divergentes = new TreeMap<>();
        outro.forEach((nome, valor) -> {
            String esperado = canonico.get(nome);
            if (esperado == null) {
                divergentes.put(nome, "ausente no catálogo canônico; valor legado " + valor);
            } else if (!esperado.equals(valor)) {
                divergentes.put(nome, "canônico=" + esperado + " legado=" + valor);
            }
        });
        return divergentes;
    }

    @Test
    void catalogoLegadoResolveExatamenteOsMesmosCaminhosDoCanonico() throws IllegalAccessException {
        Map<String, String> canonico =
                constantesDe(com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.class);
        Map<String, String> holderLegado = constantesDe(NationalCommunicationInstitutionalHttpRoutes.class);
        Map<String, String> internaLegada =
                constantesDe(NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes.class);

        assertThat(canonico)
                .as("catálogo canônico vazio tornaria as comparações seguintes aprovadas por vacuidade")
                .hasSizeGreaterThan(50);
        assertThat(holderLegado)
                .as("catálogo legado vazio tornaria a comparação seguinte aprovada por vacuidade")
                .isNotEmpty();
        assertThat(internaLegada).isNotEmpty();

        assertThat(divergencias(canonico, holderLegado))
                .as("o holder legado da superfície deixou de resolver os mesmos caminhos do catálogo "
                        + "canônico. Enquanto os dois existirem, divergir aqui muda a rota dos 12 controllers "
                        + "que importam o legado sem tocar nos 5 que importam o canônico.")
                .isEmpty();
        assertThat(divergencias(canonico, internaLegada))
                .as("a classe interna de mesmo nome simples divergiu do catálogo canônico — é justamente a "
                        + "que os @RequestMapping dos doze controllers resolvem")
                .isEmpty();
    }
}
