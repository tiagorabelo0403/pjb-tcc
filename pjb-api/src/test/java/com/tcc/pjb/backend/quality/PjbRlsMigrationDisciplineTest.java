package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Invariante de disciplina de RLS: nenhuma tabela pode ficar com Row Level Security habilitado sem
 * uma policy real E sem FORCE.
 *
 * <p>Em PostgreSQL, {@code ENABLE ROW LEVEL SECURITY} sem {@code FORCE} é ignorado pelo dono da
 * tabela — e o papel de escrita da aplicação é o dono. Sem policy, não há filtro nenhum. As duas
 * coisas juntas produzem "RLS órfão": o schema anuncia proteção que em runtime não filtra linha
 * alguma. Este teste varre as migrations, calcula o estado líquido de RLS por tabela (o último
 * ENABLE/DISABLE por ordem de versão vence) e exige, para cada tabela com RLS ligado, ao menos uma
 * {@code CREATE POLICY ... ON tabela} e um {@code FORCE ROW LEVEL SECURITY} — o padrão correto e já
 * verificado da V316. Assim, um ENABLE órfão nunca volta silenciosamente.</p>
 */
class PjbRlsMigrationDisciplineTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    private static final Pattern VERSION = Pattern.compile("^V(\\d+)__");
    private static final Pattern RLS_TOGGLE = Pattern.compile(
            "ALTER\\s+TABLE\\s+(?:ONLY\\s+)?([A-Za-z0-9_.\"]+)\\s+(ENABLE|DISABLE|FORCE|NO\\s+FORCE)\\s+ROW\\s+LEVEL\\s+SECURITY",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_POLICY = Pattern.compile(
            "CREATE\\s+POLICY\\s+\\S+\\s+ON\\s+([A-Za-z0-9_.\"]+)",
            Pattern.CASE_INSENSITIVE);

    @Test
    void nenhuma_tabela_com_rls_ligado_pode_ficar_sem_policy_e_sem_force() throws IOException {
        // estado por tabela, resolvido na ordem das versões
        Map<String, Integer> enableVersion = new LinkedHashMap<>();
        Map<String, Integer> disableVersion = new LinkedHashMap<>();
        Map<String, Integer> forceVersion = new LinkedHashMap<>();
        Map<String, Integer> noForceVersion = new LinkedHashMap<>();
        Set<String> tabelasComPolicy = new LinkedHashSet<>();

        List<Path> arquivos;
        try (Stream<Path> paths = Files.walk(MIGRATIONS)) {
            arquivos = paths.filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql"))
                    .sorted((a, b) -> Integer.compare(versao(a), versao(b)))
                    .toList();
        }

        for (Path arquivo : arquivos) {
            int versao = versao(arquivo);
            String sql = semComentarios(Files.readString(arquivo));

            Matcher toggle = RLS_TOGGLE.matcher(sql);
            while (toggle.find()) {
                String tabela = normalizar(toggle.group(1));
                String acao = toggle.group(2).toUpperCase().replaceAll("\\s+", " ");
                switch (acao) {
                    case "ENABLE" -> enableVersion.merge(tabela, versao, Math::max);
                    case "DISABLE" -> disableVersion.merge(tabela, versao, Math::max);
                    case "FORCE" -> forceVersion.merge(tabela, versao, Math::max);
                    case "NO FORCE" -> noForceVersion.merge(tabela, versao, Math::max);
                    default -> { /* ignore */ }
                }
            }

            Matcher policy = CREATE_POLICY.matcher(sql);
            while (policy.find()) {
                tabelasComPolicy.add(normalizar(policy.group(1)));
            }
        }

        Set<String> orfaos = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> e : enableVersion.entrySet()) {
            String tabela = e.getKey();
            int ligadoEm = e.getValue();
            int desligadoEm = disableVersion.getOrDefault(tabela, -1);
            boolean rlsLigado = ligadoEm > desligadoEm;
            if (!rlsLigado) {
                continue;
            }
            int forcadoEm = forceVersion.getOrDefault(tabela, -1);
            int naoForcadoEm = noForceVersion.getOrDefault(tabela, -1);
            boolean forcado = forcadoEm > naoForcadoEm;
            boolean temPolicy = tabelasComPolicy.contains(tabela);
            if (!forcado || !temPolicy) {
                orfaos.add(tabela + " (force=" + forcado + ", policy=" + temPolicy + ")");
            }
        }

        assertThat(orfaos)
                .as("RLS órfão detectado: tabela com ENABLE ROW LEVEL SECURITY mas sem FORCE e/ou sem "
                        + "CREATE POLICY não filtra nada em runtime (o dono da tabela ignora RLS sem FORCE). "
                        + "Ou complete o RLS (ENABLE + FORCE + policy + wiring de GUC, padrão da V316), "
                        + "ou remova o ENABLE via nova migration DISABLE ROW LEVEL SECURITY.")
                .isEmpty();
    }

    /** Remove comentários de linha (--) e de bloco de forma que palavras-chave em comentários
     *  (ex.: um comentário explicando o próprio DISABLE) não sejam interpretadas como DDL. */
    private static String semComentarios(String sql) {
        String semBloco = sql.replaceAll("(?s)/\\*.*?\\*/", " ");
        StringBuilder sb = new StringBuilder();
        for (String linha : semBloco.split("\n")) {
            int idx = linha.indexOf("--");
            sb.append(idx >= 0 ? linha.substring(0, idx) : linha).append('\n');
        }
        return sb.toString();
    }

    private static int versao(Path arquivo) {
        Matcher m = VERSION.matcher(arquivo.getFileName().toString());
        return m.find() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
    }

    private static String normalizar(String tabela) {
        String t = tabela.replace("\"", "").trim().toLowerCase();
        int ponto = t.lastIndexOf('.');
        return ponto >= 0 ? t.substring(ponto + 1) : t;
    }
}
