package com.tcc.pjb.backend.core.processo.vertical.estadual.civel.application;

import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelPerfil;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.vertical.domain.ProcessoVerticalAggregate;
import com.tcc.pjb.backend.core.processo.vertical.domain.ProcessoVerticalEtapa;
import com.tcc.pjb.backend.core.processo.vertical.domain.ProcessoVerticalLane;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoVerticalCivelPrimeiroGrauApplicationService {

    private static final List<String> PROFILE_CODES = List.of(
            "MAGISTRADO_DIRETO",
            "ADVOGADO_DIRETO",
            "PROMOTORIA__PROMOTORIA_TITULAR",
            "NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR",
            "PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR",
            "CEJUSC__CEJUSC_AGENDAMENTO",
            "CONTADORIA__CONTADORIA_OPERACAO"
    );

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoPrazoApplicationService processoPrazoApplicationService;
    private final ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    private final ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoPapelApplicationService processoPapelApplicationService;

    public ProcessoVerticalCivelPrimeiroGrauApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                                               ProcessoPrazoApplicationService processoPrazoApplicationService,
                                                               ProcessoTrabalhoApplicationService processoTrabalhoApplicationService,
                                                               ProcessoDocumentoApplicationService processoDocumentoApplicationService,
                                                               ProcessoTimelineApplicationService processoTimelineApplicationService,
                                                               ProcessoPapelApplicationService processoPapelApplicationService) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoPrazoApplicationService = Objects.requireNonNull(processoPrazoApplicationService);
        this.processoTrabalhoApplicationService = Objects.requireNonNull(processoTrabalhoApplicationService);
        this.processoDocumentoApplicationService = Objects.requireNonNull(processoDocumentoApplicationService);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoPapelApplicationService = Objects.requireNonNull(processoPapelApplicationService);
    }

    public ProcessoVerticalAggregate detalhar(Long processoId) {
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoPrazoAggregate prazo = processoPrazoApplicationService.detalhar(processoId);
        ProcessoTrabalhoAggregate trabalho = processoTrabalhoApplicationService.detalhar(processoId);
        ProcessoDocumentoAggregate documental = processoDocumentoApplicationService.detalhar(processoId);
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        List<ProcessoVerticalLane> lanes = PROFILE_CODES.stream()
                .map(code -> processoPapelApplicationService.detalharPerfil(processoId, code))
                .map(this::toLane)
                .toList();
        List<ProcessoVerticalEtapa> etapas = List.of(
                etapaDistribuicao(unificado, prazo),
                etapaTriagemESaneamento(unificado, trabalho),
                etapaRespostaEProva(unificado, prazo, documental),
                etapaAutocomposicaoOuAudiencia(unificado, timeline),
                etapaSentenca(unificado, documental),
                etapaPosSentenca(unificado, prazo, trabalho)
        );
        LinkedHashSet<String> chips = new LinkedHashSet<>();
        chips.add("civel");
        chips.add("primeiro_grau");
        chips.add(colorChip(unificado));
        chips.addAll(timeline.eixosAtivos());
        LinkedHashSet<String> alertas = new LinkedHashSet<>(prazo.alertasEstruturais());
        alertas.addAll(trabalho.gates());
        alertas.addAll(documental.alertas());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(unificado.competencia().fundamentos());
        fundamentos.add("A fatia cível de primeiro grau precisa conectar distribuição, saneamento, prova, audiência, sentença e pós-sentença sem controller paralelo.");
        fundamentos.add("Cada lane operacional recebe separadores, guardas e faixa de autoridade compatíveis com o rito cível comum.");
        return new ProcessoVerticalAggregate(
                "CIVEL_COMUM_PRIMEIRO_GRAU",
                "Fatia vertical cível comum de primeiro grau",
                unificado.identity(),
                unificado.competencia().ritoProcessual(),
                unificado.competencia().faseProcessual(),
                unificado.competencia().statusProcessual(),
                etapas.size(),
                lanes.size(),
                prazo.marcosCriticos() + trabalho.bloqueantes(),
                etapas.stream().mapToLong(item -> item.handoffTo().size()).sum(),
                lanes,
                etapas,
                List.copyOf(chips),
                merge(trabalho.proximoMelhorFluxo(), unificado.proximoMelhorAto()),
                List.copyOf(alertas),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private ProcessoVerticalEtapa etapaDistribuicao(ProcessoUnificadoAggregate unificado, ProcessoPrazoAggregate prazo) {
        return new ProcessoVerticalEtapa(
                "DISTRIBUICAO_E_COMPETENCIA",
                "Distribuição, prevenção e competência",
                "CONHECIMENTO",
                "MAGISTRADO_DIRETO",
                "distribuicao_competencia",
                "blue",
                true,
                atosPorCategoria(unificado, "DISTRIBUICAO", "TRIAGEM", "BASE"),
                merge(unificado.competencia().reviewChecklist(), List.of("validar_classe_assunto_competencia", "confirmar_orgao_julgador")),
                List.of("peticao_inicial", "documentos_iniciais", "prova_minima"),
                List.of("PROMOTORIA__PROMOTORIA_TITULAR", "NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR"),
                List.of("A entrada cível nasce da competência correta e da classificação processual íntegra.", "Prazo de triagem e prevenção não podem ficar fora da trilha inicial.")
        );
    }

    private ProcessoVerticalEtapa etapaTriagemESaneamento(ProcessoUnificadoAggregate unificado, ProcessoTrabalhoAggregate trabalho) {
        return new ProcessoVerticalEtapa(
                "TRIAGEM_E_SANEAMENTO",
                "Triagem, saneamento e delimitação do objeto",
                "CONHECIMENTO",
                "CEJUSC__CEJUSC_AGENDAMENTO",
                "triagem_saneamento",
                "amber",
                trabalho.bloqueantes() > 0,
                atosPorCategoria(unificado, "BASE", "TRIAGEM", "MERITO"),
                merge(trabalho.gates(), List.of("conferir_polos_e_enderecamento", "delimitar_pontos_controvertidos")),
                List.of("contestacao", "documentos_complementares", "minuta_de_saneamento"),
                List.of("ADVOGADO_DIRETO", "MAGISTRADO_DIRETO"),
                List.of("O saneamento puxa a maior parte dos erros clássicos de rito e prova; por isso entra como etapa própria.")
        );
    }

    private ProcessoVerticalEtapa etapaRespostaEProva(ProcessoUnificadoAggregate unificado,
                                                      ProcessoPrazoAggregate prazo,
                                                      ProcessoDocumentoAggregate documental) {
        return new ProcessoVerticalEtapa(
                "RESPOSTA_E_PROVA",
                "Resposta do réu, réplica e produção de prova",
                "INSTRUTORIA",
                "ADVOGADO_DIRETO",
                "prova_instrucao",
                "violet",
                prazo.marcosCriticos() > 0,
                atosPorCategoria(unificado, "MERITO", "TRILHA", "BASE"),
                merge(prazo.proximaOndaOperacional(), List.of("checar_prazo_de_resposta", "habilitar_prova_testemunhal_pericial_documental")),
                List.of("contestacao", "replica", "rol_testemunhas", "quesitos_periciais"),
                List.of("CONTADORIA__CONTADORIA_OPERACAO", "MAGISTRADO_DIRETO"),
                List.of("A etapa instrutória precisa separar prova material, prova técnica e impulso judicial sem misturar filas.", "A cor do processo muda quando a prova domina a carteira operacional.")
        );
    }

    private ProcessoVerticalEtapa etapaAutocomposicaoOuAudiencia(ProcessoUnificadoAggregate unificado, ProcessoTimelineAggregate timeline) {
        return new ProcessoVerticalEtapa(
                "AUDIENCIA_OU_AUTOCOMPOSICAO",
                "Audiência, conciliação e tentativa de composição",
                "CONHECIMENTO",
                "CEJUSC__CEJUSC_AGENDAMENTO",
                "audiencias_conciliacao",
                "green",
                false,
                atosPorCategoria(unificado, "MERITO", "BASE"),
                merge(timeline.proximoCiclo(), List.of("confirmar_presencas", "registrar_termos", "encaminhar_para_sentenca_se_frustrada")),
                List.of("termo_de_audiencia", "ata_de_conciliacao"),
                List.of("MAGISTRADO_DIRETO", "ADVOGADO_DIRETO"),
                List.of("A autocomposição ou audiência não é aba estética; é eixo processual com painel, prazo e fila próprios.")
        );
    }

    private ProcessoVerticalEtapa etapaSentenca(ProcessoUnificadoAggregate unificado, ProcessoDocumentoAggregate documental) {
        return new ProcessoVerticalEtapa(
                "SENTENCA_E_PUBLICACAO",
                "Sentença, publicação e ciência qualificada",
                "CONHECIMENTO",
                "MAGISTRADO_DIRETO",
                "sentenca_publicacao",
                "red",
                documental.minutas() == 0,
                atosPorCategoria(unificado, "MERITO", "TRILHA"),
                merge(documental.trilhaAssinavel(), List.of("validar_minuta_assinavel", "publicar_e_abrir_ciencia")),
                List.of("minuta_de_sentenca", "versao_assinada", "registro_de_publicacao"),
                List.of("ADVOGADO_DIRETO", "PROMOTORIA__PROMOTORIA_TITULAR"),
                List.of("A sentença precisa nascer do versionamento e da trilha assinável, não de texto solto fora do agregado documental.")
        );
    }

    private ProcessoVerticalEtapa etapaPosSentenca(ProcessoUnificadoAggregate unificado,
                                                   ProcessoPrazoAggregate prazo,
                                                   ProcessoTrabalhoAggregate trabalho) {
        return new ProcessoVerticalEtapa(
                "POS_SENTENCA_RECURSO_CUMPRIMENTO",
                "Pós-sentença, recurso e cumprimento",
                "RECURSAL",
                "ADVOGADO_DIRETO",
                "recursal_execucao",
                "orange",
                prazo.marcosVencidos() > 0 || trabalho.vencidos() > 0,
                atosPorCategoria(unificado, "RECURSAL", "EXECUCAO", "MERITO"),
                merge(prazo.proximaOndaOperacional(), trabalho.proximoMelhorFluxo()),
                List.of("apelacao_ou_contrarrazoes", "memoriais_de_calculo", "peticao_de_cumprimento"),
                List.of("PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR", "CONTADORIA__CONTADORIA_OPERACAO"),
                List.of("A fase pós-sentença cível precisa manter recurso e cumprimento separados por cor, separador e workstream.")
        );
    }

    private ProcessoVerticalLane toLane(ProcessoPapelPerfil perfil) {
        ArrayList<String> authorityBands = new ArrayList<>();
        if (!perfil.preparar().isEmpty()) authorityBands.add("PREPARAR");
        if (!perfil.aprovar().isEmpty()) authorityBands.add("APROVAR");
        if (!perfil.assinar().isEmpty()) authorityBands.add("ASSINAR");
        if (!perfil.peticionar().isEmpty()) authorityBands.add("PETICIONAR");
        if (!perfil.recorrer().isEmpty()) authorityBands.add("RECORRER");
        return new ProcessoVerticalLane(
                perfil.codigo(),
                perfil.nomeExibicao(),
                perfil.painel(),
                perfil.accentColor(),
                perfil.trustFloor(),
                List.copyOf(authorityBands),
                topHints(perfil),
                perfil.separadores(),
                perfil.guardas()
        );
    }

    private List<String> atosPorCategoria(ProcessoUnificadoAggregate unificado, String... eixos) {
        return merge(unificado.atosPermitidos(), unificado.atosBloqueados()).stream()
                .filter(ato -> matchesAny(ato, eixos))
                .map(ProcessoUnificadoAto::codigo)
                .distinct()
                .limit(10)
                .toList();
    }

    private boolean matchesAny(ProcessoUnificadoAto ato, String... eixos) {
        String composed = normalize(ato.categoria()) + '|' + normalize(ato.eixoOperacional()) + '|' + normalize(ato.workItemType());
        for (String eixo : eixos) {
            if (composed.contains(normalize(eixo))) {
                return true;
            }
        }
        return false;
    }

    private List<String> topHints(ProcessoPapelPerfil perfil) {
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        perfil.preparar().stream().limit(2).forEach(hints::add);
        perfil.aprovar().stream().limit(2).forEach(hints::add);
        perfil.assinar().stream().limit(2).forEach(hints::add);
        perfil.peticionar().stream().limit(2).forEach(hints::add);
        perfil.recorrer().stream().limit(2).forEach(hints::add);
        return List.copyOf(hints);
    }

    private <T> List<T> merge(List<T> left, List<T> right) {
        LinkedHashSet<T> merged = new LinkedHashSet<>();
        if (left != null) merged.addAll(left);
        if (right != null) merged.addAll(right);
        return merged.stream().filter(java.util.Objects::nonNull).toList();
    }

    private String colorChip(ProcessoUnificadoAggregate unificado) {
        long recursal = merge(unificado.atosPermitidos(), unificado.atosBloqueados()).stream().filter(ProcessoUnificadoAto::recursal).count();
        if (recursal > 0) return "cor=orange";
        if (normalize(unificado.competencia().faseProcessual()).contains("EXECU")) return "cor=red";
        return "cor=blue";
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_");
    }
}
