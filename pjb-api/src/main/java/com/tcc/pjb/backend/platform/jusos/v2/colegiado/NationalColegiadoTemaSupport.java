package com.tcc.pjb.backend.platform.jusos.v2.colegiado;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class NationalColegiadoTemaSupport {

    private static final int MAX_TEMAS_REPETITIVOS = 512;
    private static final int MAX_PROCESSOS_INDEXADOS = 20000;
    private static final int MAX_TEMAS_POR_PROCESSO = 8;

    private final Map<String, NationalColegiadoEngine.RecursoRepetitivoTema> temasRepetitivos = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> indiceTemaPorProcesso = new ConcurrentHashMap<>();

    NationalColegiadoEngine.ResultadoAfetacao afetarComoRepetitivo(String numeroTema,
                                                                   List<String> numerosProcessos,
                                                                   GrauJurisdicao grau,
                                                                   RamoDireito ramo) {
        String tema = normalizeNullable(numeroTema);
        Objects.requireNonNull(tema, "numeroTema");
        List<String> processos = immutableDistinct(numerosProcessos);
        List<String> alertas = new ArrayList<>();
        if (grau != GrauJurisdicao.SUPERIOR && grau != GrauJurisdicao.CONSTITUCIONAL) {
            alertas.add("Afetação repetitiva ou repercussão geral exige tribunal superior ou constitucional");
        }
        if (processos.size() < 2) {
            alertas.add("Afetação robusta requer ao menos 2 processos representativos válidos");
        }
        if (ramo == null) {
            alertas.add("Ramo não identificado: recomenda-se classificação temática antes do sobrestamento em massa");
        }
        alertas.add("Intimar partes e sinalizar sobrestamento automático dos casos correlatos");
        NationalColegiadoEngine.StatusRepetitivo status = processos.size() >= 2
                ? NationalColegiadoEngine.StatusRepetitivo.AFETADO_AGUARDANDO_JULGAMENTO
                : NationalColegiadoEngine.StatusRepetitivo.PENDENTE_AFETACAO;
        NationalColegiadoEngine.RecursoRepetitivoTema temaRegistrado = new NationalColegiadoEngine.RecursoRepetitivoTema(
                tema,
                inferirTribunalTema(grau),
                "Tema repetitivo " + tema,
                null,
                status,
                processos,
                processos,
                grau,
                ramo,
                Instant.now(),
                null,
                alertas
        );
        registrarTema(temaRegistrado);
        return new NationalColegiadoEngine.ResultadoAfetacao(
                tema,
                processos.size(),
                processos.size(),
                alertas,
                status,
                Instant.now()
        );
    }

    NationalColegiadoEngine.RecursoRepetitivoTema registrarTeseRepetitiva(String numeroTema,
                                                                          String teseFixada,
                                                                          List<String> processosAfetados) {
        String tema = normalizeNullable(numeroTema);
        Objects.requireNonNull(tema, "numeroTema");
        NationalColegiadoEngine.RecursoRepetitivoTema atual = temasRepetitivos.get(tema);
        boolean explicitProcessos = processosAfetados != null && !processosAfetados.isEmpty();
        List<String> processos = atual != null && !explicitProcessos
                ? atual.processosAfetados()
                : immutableDistinct(processosAfetados);
        NationalColegiadoEngine.RecursoRepetitivoTema atualizado = new NationalColegiadoEngine.RecursoRepetitivoTema(
                tema,
                atual != null ? atual.tribunalCodigo() : null,
                atual != null ? atual.descricaoTema() : "Tema repetitivo " + tema,
                teseFixada,
                NationalColegiadoEngine.StatusRepetitivo.JULGADO_TESE_FIRMADA,
                processos,
                explicitProcessos || atual == null ? processos : atual.processosRepresentativos(),
                atual != null ? atual.grau() : GrauJurisdicao.SUPERIOR,
                atual != null ? atual.ramo() : null,
                atual != null ? atual.afetadoEm() : Instant.now(),
                Instant.now(),
                atual != null ? atual.alertas() : List.of()
        );
        registrarTema(atualizado);
        return atualizado;
    }


    Map<String, NationalColegiadoEngine.RecursoRepetitivoTema> temasRepetitivosSnapshot() {
        return temasRepetitivos;
    }

    Map<String, Set<String>> indiceTemaPorProcessoSnapshot() {
        return indiceTemaPorProcesso;
    }

    NationalColegiadoEngine.RecursoRepetitivoTema consultarTema(String numeroTema) {
        return temasRepetitivos.get(normalizeNullable(numeroTema));
    }

    List<NationalColegiadoEngine.RecursoRepetitivoTema> listarTemas() {
        return temasRepetitivos.values().stream()
                .sorted(Comparator.comparing(NationalColegiadoEngine.RecursoRepetitivoTema::afetadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    List<NationalColegiadoEngine.RecursoRepetitivoTema> consultarTemasPorProcesso(String numeroUnificado) {
        String numero = normalizeNullable(numeroUnificado);
        if (numero == null) {
            return List.of();
        }
        Set<String> temas = indiceTemaPorProcesso.getOrDefault(numero, Set.of());
        if (temas.isEmpty()) {
            return List.of();
        }
        return temas.stream()
                .map(temasRepetitivos::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(NationalColegiadoEngine.RecursoRepetitivoTema::afetadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    int totalTemas() {
        return temasRepetitivos.size();
    }

    int totalProcessosIndexados() {
        return indiceTemaPorProcesso.size();
    }

    boolean processoEmTemaRepetitivo(String numeroUnificado) {
        return indiceTemaPorProcesso.containsKey(numeroUnificado);
    }

    private void registrarTema(NationalColegiadoEngine.RecursoRepetitivoTema temaRegistrado) {
        List<String> processosIndexados = processosIndexados(temaRegistrado);
        NationalColegiadoEngine.RecursoRepetitivoTema anterior = temasRepetitivos.put(temaRegistrado.numeroTema(), new NationalColegiadoEngine.RecursoRepetitivoTema(
                temaRegistrado.numeroTema(),
                temaRegistrado.tribunalCodigo(),
                temaRegistrado.descricaoTema(),
                temaRegistrado.teseFixada(),
                temaRegistrado.status(),
                processosIndexados,
                temaRegistrado.processosRepresentativos(),
                temaRegistrado.grau(),
                temaRegistrado.ramo(),
                temaRegistrado.afetadoEm(),
                temaRegistrado.julgadoEm(),
                temaRegistrado.alertas()
        ));
        if (anterior != null) {
            desindexarTemaProcessos(anterior.numeroTema(), anterior.processosAfetados());
        }
        indexarTemaProcessos(temaRegistrado.numeroTema(), processosIndexados);
        compactarTemasRepetitivos();
    }

    private void indexarTemaProcessos(String numeroTema, Collection<String> processos) {
        if (processos == null || processos.isEmpty()) {
            return;
        }
        for (String processo : processos) {
            if (processo == null || processo.isBlank()) {
                continue;
            }
            indiceTemaPorProcesso.compute(processo, (key, existing) -> {
                LinkedHashSet<String> temas = existing == null ? new LinkedHashSet<>() : new LinkedHashSet<>(existing);
                temas.add(numeroTema);
                if (temas.size() > MAX_TEMAS_POR_PROCESSO) {
                    List<String> ordered = new ArrayList<>(temas);
                    ordered.sort(Comparator.naturalOrder());
                    temas = new LinkedHashSet<>(ordered.subList(Math.max(0, ordered.size() - MAX_TEMAS_POR_PROCESSO), ordered.size()));
                }
                return Set.copyOf(temas);
            });
        }
        if (indiceTemaPorProcesso.size() > MAX_PROCESSOS_INDEXADOS) {
            List<String> orderedKeys = new ArrayList<>(indiceTemaPorProcesso.keySet());
            orderedKeys.sort(Comparator.naturalOrder());
            int excess = orderedKeys.size() - MAX_PROCESSOS_INDEXADOS;
            for (int i = 0; i < excess; i++) {
                indiceTemaPorProcesso.remove(orderedKeys.get(i));
            }
        }
    }

    private void desindexarTemaProcessos(String numeroTema, Collection<String> processos) {
        if (processos == null || processos.isEmpty()) {
            return;
        }
        for (String processo : processos) {
            if (processo == null || processo.isBlank()) {
                continue;
            }
            indiceTemaPorProcesso.computeIfPresent(processo, (key, existing) -> {
                LinkedHashSet<String> temas = new LinkedHashSet<>(existing);
                temas.remove(numeroTema);
                return temas.isEmpty() ? null : Set.copyOf(temas);
            });
        }
    }

    private void compactarTemasRepetitivos() {
        if (temasRepetitivos.size() <= MAX_TEMAS_REPETITIVOS) {
            return;
        }
        List<NationalColegiadoEngine.RecursoRepetitivoTema> ordered = temasRepetitivos.values().stream()
                .sorted(Comparator.comparing(this::marcoTema, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(NationalColegiadoEngine.RecursoRepetitivoTema::numeroTema, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        int excess = ordered.size() - MAX_TEMAS_REPETITIVOS;
        for (int i = 0; i < excess; i++) {
            NationalColegiadoEngine.RecursoRepetitivoTema tema = ordered.get(i);
            if (tema == null || tema.numeroTema() == null) {
                continue;
            }
            NationalColegiadoEngine.RecursoRepetitivoTema removido = temasRepetitivos.remove(tema.numeroTema());
            if (removido != null) {
                desindexarTemaProcessos(removido.numeroTema(), removido.processosAfetados());
            }
        }
    }

    private List<String> processosIndexados(NationalColegiadoEngine.RecursoRepetitivoTema tema) {
        return mergeDistinct(
                tema.processosAfetados(),
                tema.processosRepresentativos()
        ).stream().limit(MAX_PROCESSOS_INDEXADOS).toList();
    }

    @SafeVarargs
    private List<String> mergeDistinct(List<String>... groups) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (groups == null) {
            return List.of();
        }
        for (List<String> group : groups) {
            if (group == null) {
                continue;
            }
            for (String value : group) {
                String normalized = normalizeNullable(value);
                if (normalized != null) {
                    merged.add(normalized);
                }
            }
        }
        return List.copyOf(merged);
    }

    private Instant marcoTema(NationalColegiadoEngine.RecursoRepetitivoTema tema) {
        if (tema == null) {
            return null;
        }
        if (tema.julgadoEm() != null) {
            return tema.julgadoEm();
        }
        if (tema.afetadoEm() != null) {
            return tema.afetadoEm();
        }
        return null;
    }

    private String inferirTribunalTema(GrauJurisdicao grau) {
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            return "STF";
        }
        if (grau == GrauJurisdicao.SUPERIOR) {
            return "STJ";
        }
        return "TRIBUNAL";
    }

    private static String normalizeNullable(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static List<String> immutableDistinct(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        for (String value : values) {
            String clean = normalizeNullable(value);
            if (clean != null) {
                normalized.putIfAbsent(clean, clean);
            }
        }
        return List.copyOf(normalized.values());
    }
}
