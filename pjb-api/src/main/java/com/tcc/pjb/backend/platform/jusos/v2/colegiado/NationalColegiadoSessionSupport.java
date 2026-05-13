package com.tcc.pjb.backend.platform.jusos.v2.colegiado;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.StatusJulgamentoColegiado;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
class NationalColegiadoSessionSupport {

    List<NationalColegiadoEngine.JanelaSustentacaoOral> gerarAgendaSustentacaoOral(NationalColegiadoEngine.SessaoPauta sessao) {
        Objects.requireNonNull(sessao, "sessao");
        if (sessao.itens().isEmpty() || sessao.dataHoraInicio() == null) {
            return List.of();
        }
        List<NationalColegiadoEngine.JanelaSustentacaoOral> agenda = new ArrayList<>();
        Instant cursor = sessao.dataHoraInicio();
        for (NationalColegiadoEngine.ItemPauta item : sessao.itens()) {
            int duracao = Math.max(4, item.temSustentacaoOral() ? item.minutosSustentacao() : 4);
            Instant inicio = cursor;
            Instant fim = cursor.plus(Duration.ofMinutes(duracao));
            LinkedHashSet<String> etiquetas = new LinkedHashSet<>(item.etiquetasPrioridade());
            if (item.urgente()) {
                etiquetas.add("PRIORITARIO");
            }
            if (item.temSustentacaoOral()) {
                etiquetas.add("SUSTENTACAO_ORAL");
            }
            agenda.add(new NationalColegiadoEngine.JanelaSustentacaoOral(
                    item.ordem(),
                    item.julgamentoId(),
                    item.numeroUnificado(),
                    inicio,
                    fim,
                    duracao,
                    item.urgente() || item.habeasCorpus(),
                    List.copyOf(etiquetas)
            ));
            cursor = fim.plus(Duration.ofMinutes(item.temSustentacaoOral() ? 3 : 2));
        }
        return List.copyOf(agenda);
    }

    List<NationalColegiadoEngine.InsightPrecedente> mapearInsightsPrecedentes(NationalColegiadoEngine.SessaoPauta sessao) {
        Objects.requireNonNull(sessao, "sessao");
        if (sessao.itens().isEmpty()) {
            return List.of();
        }
        Map<String, List<NationalColegiadoEngine.ItemPauta>> grupos = new LinkedHashMap<>();
        for (NationalColegiadoEngine.ItemPauta item : sessao.itens()) {
            String chave = montarChaveTema(item.classeTPU(), item.assunto(), item.ramo());
            grupos.computeIfAbsent(chave, k -> new ArrayList<>()).add(item);
        }
        List<NationalColegiadoEngine.InsightPrecedente> insights = new ArrayList<>();
        for (Map.Entry<String, List<NationalColegiadoEngine.ItemPauta>> entry : grupos.entrySet()) {
            List<NationalColegiadoEngine.ItemPauta> grupo = entry.getValue();
            if (grupo.size() < 2 && sessao.grau() != GrauJurisdicao.SUPERIOR && sessao.grau() != GrauJurisdicao.CONSTITUCIONAL) {
                continue;
            }
            NationalColegiadoEngine.ItemPauta base = grupo.get(0);
            List<String> alertas = new ArrayList<>();
            boolean urgencia = grupo.stream().anyMatch(NationalColegiadoEngine.ItemPauta::urgente);
            boolean candidato = grupo.size() >= 2 && (sessao.grau() == GrauJurisdicao.SUPERIOR || sessao.grau() == GrauJurisdicao.CONSTITUCIONAL);
            if (candidato) {
                alertas.add("Conjunto com potencial de uniformização jurisprudencial ou afetação temática");
            }
            if (urgencia) {
                alertas.add("Há item urgente dentro do agrupamento temático");
            }
            insights.add(new NationalColegiadoEngine.InsightPrecedente(
                    entry.getKey(),
                    base.classeTPU(),
                    base.assunto(),
                    base.ramo(),
                    grupo.size(),
                    grupo.stream().map(NationalColegiadoEngine.ItemPauta::numeroUnificado).filter(Objects::nonNull).distinct().toList(),
                    candidato,
                    urgencia,
                    alertas
            ));
        }
        return insights.stream()
                .sorted(Comparator.comparingInt(NationalColegiadoEngine.InsightPrecedente::ocorrencias).reversed().thenComparing(NationalColegiadoEngine.InsightPrecedente::chaveTema))
                .toList();
    }

    List<NationalColegiadoEngine.FilaPublicacaoAcordao> gerarFilaPublicacaoAcordao(String tribunal,
                                                                                   Collection<JulgamentoColegiado> julgamentos,
                                                                                   java.util.function.Function<String, List<NationalColegiadoEngine.RecursoRepetitivoTema>> temasPorProcesso,
                                                                                   java.util.function.Function<JulgamentoColegiado, Instant> marcoEncerramentoResolver,
                                                                                   java.util.function.Function<String, String> tribunalNormalizer) {
        List<NationalColegiadoEngine.FilaPublicacaoAcordao> fila = new ArrayList<>();
        for (JulgamentoColegiado julgamento : julgamentos) {
            if (julgamento.getStatus() != StatusJulgamentoColegiado.ENCERRADO || Boolean.TRUE.equals(julgamento.getAcordaoPublicado())) {
                continue;
            }
            if (tribunal != null && !tribunal.equals(tribunalNormalizer.apply(julgamento.getTribunalSigla()))) {
                continue;
            }
            String numeroUnificado = julgamento.getProcesso() != null ? julgamento.getProcesso().getNumeroUnificado() : null;
            Instant marco = marcoEncerramentoResolver.apply(julgamento);
            LocalDate limite = marco != null
                    ? marco.atZone(ZoneOffset.UTC).toLocalDate().plusDays(5)
                    : LocalDate.now().plusDays(5);
            boolean atrasado = LocalDate.now().isAfter(limite);
            List<String> alertas = new ArrayList<>();
            if (atrasado) {
                alertas.add("Publicação do acórdão acima do SLA interno sugerido");
            }
            if (numeroUnificado != null && !temasPorProcesso.apply(numeroUnificado).isEmpty()) {
                alertas.add("Processo vinculado a tema repetitivo: publicar com prioridade reforçada");
            }
            fila.add(new NationalColegiadoEngine.FilaPublicacaoAcordao(
                    julgamento.getId(),
                    numeroUnificado,
                    tribunalNormalizer.apply(julgamento.getTribunalSigla()),
                    julgamento.getOrgaoJulgador(),
                    marco,
                    limite,
                    atrasado,
                    alertas
            ));
        }
        return fila.stream()
                .sorted(Comparator.comparingInt((NationalColegiadoEngine.FilaPublicacaoAcordao item) -> item.atrasado() ? 1 : 0).reversed()
                        .thenComparing(NationalColegiadoEngine.FilaPublicacaoAcordao::limitePublicacaoSugerido, Comparator.nullsLast(LocalDate::compareTo)))
                .toList();
    }

    Map<String, Object> gerarChecklistOperacionalSessao(NationalColegiadoEngine.SessaoPauta sessao) {
        Objects.requireNonNull(sessao, "sessao");
        List<String> alertas = new ArrayList<>();
        if (sessao.totalItens() == 0) {
            alertas.add("Sessão sem itens pautados");
        }
        if (sessao.quorumMinimo() > 3 && sessao.totalItens() < sessao.quorumMinimo()) {
            alertas.add("Quantidade de itens inferior ao quórum mínimo parametrizado para a sessão");
        }
        if (sessao.itens().stream().anyMatch(NationalColegiadoEngine.ItemPauta::sigiloso) && !sessao.sessaoVirtual()) {
            alertas.add("Há itens sigilosos: validar sala reservada, controle de acesso e gravação restrita");
        }
        if (sessao.itens().stream().noneMatch(NationalColegiadoEngine.ItemPauta::temSustentacaoOral) && sessao.itensUrgentes() > 0) {
            alertas.add("Sessão urgente sem janelas formais de sustentação oral configuradas");
        }
        if (sessao.minutosEstimados() > 240) {
            alertas.add("Sessão com duração elevada: considerar fracionamento ou pauta suplementar");
        }
        Map<String, Object> checklist = new LinkedHashMap<>();
        checklist.put("sessaoId", sessao.sessaoId());
        checklist.put("quorumMinimo", sessao.quorumMinimo());
        checklist.put("itensComSustentacao", sessao.itens().stream().filter(NationalColegiadoEngine.ItemPauta::temSustentacaoOral).count());
        checklist.put("itensSigilosos", sessao.itens().stream().filter(NationalColegiadoEngine.ItemPauta::sigiloso).count());
        checklist.put("insightsPrecedente", mapearInsightsPrecedentes(sessao).size());
        checklist.put("agendaSustentacao", gerarAgendaSustentacaoOral(sessao).size());
        checklist.put("alertas", List.copyOf(new LinkedHashSet<>(alertas)));
        return Collections.unmodifiableMap(checklist);
    }

    Map<String, Object> gerarRelatorioSessao(NationalColegiadoEngine.SessaoPauta sessao) {
        Objects.requireNonNull(sessao, "sessao");
        int julgados = (int) sessao.itens().stream().filter(i -> i.status() == NationalColegiadoEngine.ItemPauta.StatusItemPauta.JULGADO).count();
        int adiados = (int) sessao.itens().stream().filter(i -> i.status() == NationalColegiadoEngine.ItemPauta.StatusItemPauta.ADIADO).count();
        int vistas = (int) sessao.itens().stream().filter(i -> i.status() == NationalColegiadoEngine.ItemPauta.StatusItemPauta.VISTA).count();
        int sigilosos = (int) sessao.itens().stream().filter(NationalColegiadoEngine.ItemPauta::sigiloso).count();
        int minutosSustentacao = sessao.itens().stream().mapToInt(NationalColegiadoEngine.ItemPauta::minutosSustentacao).sum();
        double taxaResolucao = sessao.totalItens() > 0 ? (julgados * 100.0d) / sessao.totalItens() : 0.0d;
        List<NationalColegiadoEngine.JanelaSustentacaoOral> agendaSustentacao = gerarAgendaSustentacaoOral(sessao);
        List<NationalColegiadoEngine.InsightPrecedente> insights = mapearInsightsPrecedentes(sessao);
        Map<String, Object> checklistOperacional = gerarChecklistOperacionalSessao(sessao);

        Map<String, Object> relatorio = new LinkedHashMap<>();
        relatorio.put("sessaoId", sessao.sessaoId());
        relatorio.put("tribunal", sessao.tribunalCodigo());
        relatorio.put("orgaoJulgador", Objects.requireNonNullElse(sessao.orgaoJulgador(), ""));
        relatorio.put("grau", sessao.grau() != null ? sessao.grau().name() : null);
        relatorio.put("tipoSessao", sessao.tipoSessao() != null ? sessao.tipoSessao().name() : null);
        relatorio.put("totalPauta", sessao.totalItens());
        relatorio.put("itensUrgentes", sessao.itensUrgentes());
        relatorio.put("itensSigilosos", sigilosos);
        relatorio.put("julgados", julgados);
        relatorio.put("adiados", adiados);
        relatorio.put("vistas", vistas);
        relatorio.put("quorumMinimo", sessao.quorumMinimo());
        relatorio.put("minutosEstimados", sessao.minutosEstimados());
        relatorio.put("minutosSustentacao", minutosSustentacao);
        relatorio.put("taxaResolucao", String.format(Locale.ROOT, "%.2f%%", taxaResolucao));
        relatorio.put("etiquetas", sessao.etiquetas());
        relatorio.put("agendaSustentacaoTotal", agendaSustentacao.size());
        relatorio.put("agendaSustentacaoPrimeiraJanela", agendaSustentacao.isEmpty() ? null : agendaSustentacao.get(0).inicioPrevisto().toString());
        relatorio.put("insightsPrecedente", insights.size());
        relatorio.put("temCandidatoAfetacao", insights.stream().anyMatch(NationalColegiadoEngine.InsightPrecedente::candidatoAfetacao));
        relatorio.put("checklistOperacional", checklistOperacional);
        return Collections.unmodifiableMap(relatorio);
    }

    Map<String, Object> gerarPainelColegiado(String tribunal,
                                             Collection<JulgamentoColegiado> julgamentos,
                                             long acordaosPublicados,
                                             List<NationalColegiadoEngine.FilaPublicacaoAcordao> filaPublicacao,
                                             long temasRepetitivos,
                                             long processosComTema,
                                             java.util.function.Predicate<JulgamentoColegiado> urgentPendingPredicate,
                                             java.util.function.Function<JulgamentoColegiado, Instant> marcoEncerramentoResolver,
                                             Collection<StatusJulgamentoColegiado> statusPendentes) {
        long abertos = julgamentos.stream().filter(j -> statusPendentes.contains(j.getStatus())).count();
        long encerrados = julgamentos.stream().filter(j -> j.getStatus() == StatusJulgamentoColegiado.ENCERRADO).count();
        long vista = julgamentos.stream().filter(j -> j.getStatus() == StatusJulgamentoColegiado.VISTA).count();
        long urgentesPendentes = julgamentos.stream()
                .filter(j -> statusPendentes.contains(j.getStatus()))
                .filter(urgentPendingPredicate)
                .count();
        double mediaDiasPendentes = julgamentos.stream()
                .filter(j -> statusPendentes.contains(j.getStatus()))
                .map(marcoEncerramentoResolver)
                .filter(Objects::nonNull)
                .mapToLong(m -> Duration.between(m, Instant.now()).toDays())
                .average()
                .orElse(0d);
        Map<String, Object> painel = new LinkedHashMap<>();
        painel.put("tribunal", tribunal);
        painel.put("julgamentosTotais", julgamentos.size());
        painel.put("abertos", abertos);
        painel.put("encerrados", encerrados);
        painel.put("comAcordaoPublicado", acordaosPublicados);
        painel.put("comVista", vista);
        painel.put("temasRepetitivos", temasRepetitivos);
        painel.put("processosIndexadosEmTema", processosComTema);
        painel.put("pendentesPublicacaoAcordao", filaPublicacao.size());
        painel.put("pendentesPublicacaoAtrasados", filaPublicacao.stream().filter(NationalColegiadoEngine.FilaPublicacaoAcordao::atrasado).count());
        painel.put("urgentesPendentes", urgentesPendentes);
        painel.put("mediaDiasPendentes", String.format(Locale.ROOT, "%.1f", mediaDiasPendentes));
        return Collections.unmodifiableMap(painel);
    }

    List<String> montarEtiquetasSessao(List<NationalColegiadoEngine.ItemPauta> itens,
                                       NationalColegiadoEngine.TipoSessao tipo,
                                       boolean virtual,
                                       GrauJurisdicao grau) {
        LinkedHashSet<String> etiquetas = new LinkedHashSet<>();
        if (virtual || tipo == NationalColegiadoEngine.TipoSessao.SESSAO_VIRTUAL) {
            etiquetas.add("VIRTUAL");
        }
        if (tipo == NationalColegiadoEngine.TipoSessao.SESSAO_PLENARIA) {
            etiquetas.add("PLENÁRIO");
        }
        if (grau == GrauJurisdicao.SUPERIOR || grau == GrauJurisdicao.CONSTITUCIONAL) {
            etiquetas.add("PRECEDENTES");
        }
        if (itens.stream().anyMatch(NationalColegiadoEngine.ItemPauta::habeasCorpus)) {
            etiquetas.add("LIBERDADE");
        }
        if (itens.stream().anyMatch(NationalColegiadoEngine.ItemPauta::sigiloso)) {
            etiquetas.add("SIGILO_CONTROLADO");
        }
        if (itens.stream().anyMatch(NationalColegiadoEngine.ItemPauta::temSustentacaoOral)) {
            etiquetas.add("SUSTENTAÇÃO_ORAL");
        }
        return List.copyOf(etiquetas);
    }

    private String montarChaveTema(String classeTPU, String assunto, com.tcc.pjb.backend.model.entity.enums.RamoDireito ramo) {
        return (normalizeNullable(classeTPU) + "|" + normalizeNullable(assunto) + "|" + (ramo != null ? ramo.name() : "SEM_RAMO"))
                .toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
