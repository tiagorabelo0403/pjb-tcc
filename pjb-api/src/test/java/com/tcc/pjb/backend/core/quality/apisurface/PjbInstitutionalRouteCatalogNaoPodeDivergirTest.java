package com.tcc.pjb.backend.core.quality.apisurface;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * O catálogo de rotas institucionais já existiu em dois lugares ao mesmo tempo: em
 * {@code core.comunicacao.institucional} e numa classe interna de <b>mesmo nome simples</b> aninhada
 * no holder legado da superfície. Dos 17 controllers institucionais, 5 importavam o primeiro e 12 o
 * segundo — e como o nome simples era idêntico, o {@code @RequestMapping} dos doze lia exatamente como
 * o dos cinco sem apontar para a mesma classe. Os valores nunca chegaram a divergir, mas nada impedia.
 *
 * <p>Hoje o catálogo mora em {@code platform.api.institucional}, fora de {@code core} e fora da
 * superfície, para que controller e domínio possam depender dele sem que o controller alcance o
 * domínio interno. O que este teste impede é o renascimento da duplicata: um segundo
 * {@code InstitutionalApiRoutes} em qualquer outro pacote volta a produzir o import que parece
 * canônico e não é.
 */
class PjbInstitutionalRouteCatalogNaoPodeDivergirTest {

    private static final Path RAIZ = Path.of("src/main/java/com/tcc/pjb/backend");
    private static final Path CANONICO =
            RAIZ.resolve("platform/api/institucional/InstitutionalApiRoutes.java");

    @Test
    void existeExatamenteUmCatalogoDeRotasInstitucionais() throws IOException {
        List<Path> declaracoes;
        try (Stream<Path> paths = Files.walk(RAIZ)) {
            declaracoes = paths
                    .filter(p -> p.getFileName().toString().equals("InstitutionalApiRoutes.java"))
                    .sorted()
                    .toList();
        }

        assertThat(CANONICO)
                .as("o catálogo canônico precisa existir onde os controllers o importam")
                .exists();
        assertThat(declaracoes)
                .as("um segundo arquivo InstitutionalApiRoutes recria a colisão de nome simples que fazia "
                        + "doze controllers parecerem canônicos importando o catálogo legado")
                .containsExactly(CANONICO);
    }

    @Test
    void nenhumaClasseInternaRessuscitaONomeDoCatalogo() throws IOException {
        List<String> aninhadas;
        try (Stream<Path> paths = Files.walk(RAIZ)) {
            aninhadas = paths
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.equals(CANONICO))
                    .filter(p -> ler(p).contains("class InstitutionalApiRoutes"))
                    .map(p -> RAIZ.relativize(p).toString())
                    .sorted()
                    .toList();
        }

        assertThat(aninhadas)
                .as("classe interna de mesmo nome simples é justamente a forma que a duplicata tinha: o "
                        + "import lê como canônico e resolve para outro catálogo")
                .isEmpty();
    }

    private static String ler(Path p) {
        try { return Files.readString(p); } catch (IOException e) { return ""; }
    }
}
