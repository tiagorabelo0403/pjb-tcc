package com.tcc.pjb.backend.core.processo.vertical.eca.application;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoVerticalEcaApplicationService {

    private static final List<String> PROFILE_CODES = List.of(
            "MAGISTRADO_DIRETO",
            "MINISTERIO_PUBLICO__MP_INFANCIA_TITULAR",
            "NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR",
            "CONSELHO_TUTELAR__CONSELHO_TUTELAR_TITULAR",
            "ASSISTENCIA_SOCIAL__ASSISTENCIA_SOCIAL_OPERACAO",
            "ADVOGADO_DIRETO"
    );

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoPrazoApplicationService processoPrazoApplicationService;
    private final ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    private final ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoPapelApplicationService processoPapelApplicationService;

    public ProcessoVerticalEcaApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
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
                etapaApreensaoECustodia(unificado, prazo),
                etapaAudienciaApresentacao(unificado, trabalho, timeline),
                etapaEstudoSocialERelatorio(unificado, prazo, documental),
                etapaAudienciaInstrucaoEMedidas(unificado, documental, timeline),
                etapaSentencaEMedidasSocioeducativas(unificado, documental),
                etapaAcompanhamentoERevisao(unificado, prazo, trabalho)
        );
        LinkedHashSet<String> chips = new LinkedHashSet<>();
        chips.add("eca");
        chips.add("sigilo_absoluto");
        chips.add("infancia_juventude");
        chips.addAll(timeline.eixosAtivos());
        LinkedHashSet<String> alertas = new LinkedHashSet<>(prazo.alertasEstruturais());
        alertas.add("SIGILO_ABSOLUTO_ATIVO");
        alertas.addAll(trabalho.gates());
        alertas.addAll(documental.alertas());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(unificado.competencia().fundamentos());
        fundamentos.add("Todo processo ECA tramita sob sigilo absoluto; nenhuma dado do adolescente pode ser exposto publicamente.");
        fundamentos.add("MP é parte obrigatória em todos os atos; sua ausência gera nulidade absoluta.");
        fundamentos.add("Audiência de apresentação deve ocorrer em até 24 horas da apreensão do adolescente em flagrante.");
        fundamentos.add("Medidas socioeducativas devem ser revisadas periodicamente e não podem exceder os limites do ECA.");
        return new ProcessoVerticalAggregate(
                "ECA_INFANCIA_JUVENTUDE",
                "Fatia vertical infância e juventude — ECA",
                unificado.identity(),
                unificado.competencia().ritoProcessual(),
                unificado.competencia().faseProcessual(),
                unificado.competencia().statusProcessual(),
                etapas.size(),
                lanes.size(),
                prazo.marcosCriticos() + trabalho.bloqueantes(),
                etapas.stream().mapToLong(e -> e.handoffTo().size()).sum(),
                lanes,
                etapas,
                List.copyOf(chips),
                merge(trabalho.proximoMelhorFluxo(), unificado.proximoMelhorAto()),
                List.copyOf(alertas),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private ProcessoVerticalEtapa etapaApreensaoECustodia(ProcessoUnificadoAggregate unificado, ProcessoPrazoAggregate prazo) {
        return new ProcessoVerticalEtapa(
                "APREENSAO_E_CUSTODIA_ECA",
                "Apreensão em flagrante e comunicação obrigatória",
                "CONHECIMENTO",
                "MAGISTRADO_DIRETO",
                "apreensao_custodia",
                "red",
                prazo.marcosCriticos() > 0,
                atosPorCategoria(unificado, "BASE", "TRIAGEM", "EXECUCAO"),
                List.of("comunicar_conselho_tutelar_imediatamente", "comunicar_mp_imediatamente", "lavrar_auto_apreensao", "notificar_responsaveis"),
                List.of("auto_apreensao", "boletim_ocorrencia", "comunicacao_conselho_tutelar"),
                List.of("MINISTERIO_PUBLICO__MP_INFANCIA_TITULAR", "CONSELHO_TUTELAR__CONSELHO_TUTELAR_TITULAR"),
                List.of("Prazo de 24 horas para audiência de apresentação é constitucional e não comporta prorrogação.", "Toda comunicação ao Conselho Tutelar deve ser registrada com carimbo de tempo auditável.")
        );
    }

    private ProcessoVerticalEtapa etapaAudienciaApresentacao(ProcessoUnificadoAggregate unificado, ProcessoTrabalhoAggregate trabalho, ProcessoTimelineAggregate timeline) {
        return new ProcessoVerticalEtapa(
                "AUDIENCIA_APRESENTACAO_ECA",
                "Audiência de apresentação — prazo constitucional 24h",
                "CONHECIMENTO",
                "MAGISTRADO_DIRETO",
                "audiencia_apresentacao",
                "orange",
                trabalho.bloqueantes() > 0,
                atosPorCategoria(unificado, "BASE", "MERITO"),
                merge(timeline.proximoCiclo(), List.of("garantir_presenca_defensor_publico", "ouvir_adolescente_separadamente", "decidir_internacao_provisoria_ou_liberdade_assistida")),
                List.of("ata_audiencia_apresentacao", "decisao_medida_cautelar"),
                List.of("NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR", "MINISTERIO_PUBLICO__MP_INFANCIA_TITULAR"),
                List.of("Adolescente deve ser ouvido separado dos responsáveis; depoimento é sigiloso e não pode ser divulgado.", "Defensor público é obrigatório na audiência de apresentação mesmo se a família tiver advogado constituído.")
        );
    }

    private ProcessoVerticalEtapa etapaEstudoSocialERelatorio(ProcessoUnificadoAggregate unificado, ProcessoPrazoAggregate prazo, ProcessoDocumentoAggregate documental) {
        return new ProcessoVerticalEtapa(
                "ESTUDO_SOCIAL_ECA",
                "Estudo psicossocial, relatório técnico e instrução",
                "INSTRUTORIA",
                "ASSISTENCIA_SOCIAL__ASSISTENCIA_SOCIAL_OPERACAO",
                "estudo_social",
                "violet",
                prazo.marcosCriticos() > 0,
                atosPorCategoria(unificado, "MERITO", "TRILHA"),
                merge(prazo.proximaOndaOperacional(), List.of("aguardar_relatorio_tecnico_assistencia_social", "aguardar_relatorio_psicologico", "checar_historico_conselho_tutelar")),
                List.of("relatorio_assistencia_social", "relatorio_psicologico", "historico_conselho_tutelar"),
                List.of("MINISTERIO_PUBLICO__MP_INFANCIA_TITULAR", "MAGISTRADO_DIRETO"),
                List.of("Estudo psicossocial é prova técnica obrigatória para internação; sem ele a decisão é nula.", "Dados do relatório técnico são sigilosos e só podem ser acessados pelas partes e pelo juízo.")
        );
    }

    private ProcessoVerticalEtapa etapaAudienciaInstrucaoEMedidas(ProcessoUnificadoAggregate unificado, ProcessoDocumentoAggregate documental, ProcessoTimelineAggregate timeline) {
        return new ProcessoVerticalEtapa(
                "AUDIENCIA_INSTRUCAO_ECA",
                "Audiência em continuação, instrução e aplicação de medidas",
                "CONHECIMENTO",
                "MAGISTRADO_DIRETO",
                "audiencia_instrucao_eca",
                "amber",
                documental.minutas() == 0,
                atosPorCategoria(unificado, "MERITO", "BASE"),
                merge(timeline.proximoCiclo(), List.of("garantir_sigilo_audiencia", "colher_depoimento_adolescente", "ouvir_vitima_separadamente_se_houver")),
                List.of("ata_audiencia_instrucao", "termo_depoimento"),
                List.of("NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR", "ADVOGADO_DIRETO"),
                List.of("Audiência ECA é sempre sigilosa; proibida gravação por terceiros sem autorização judicial.")
        );
    }

    private ProcessoVerticalEtapa etapaSentencaEMedidasSocioeducativas(ProcessoUnificadoAggregate unificado, ProcessoDocumentoAggregate documental) {
        return new ProcessoVerticalEtapa(
                "SENTENCA_MEDIDAS_ECA",
                "Sentença e aplicação de medida socioeducativa",
                "CONHECIMENTO",
                "MAGISTRADO_DIRETO",
                "sentenca_eca",
                "red",
                documental.minutas() == 0,
                atosPorCategoria(unificado, "MERITO", "TRILHA"),
                merge(documental.trilhaAssinavel(), List.of("fundamentar_proporcionalidade_da_medida", "fixar_prazo_maximo_de_internacao", "comunicar_unidade_de_acolhimento")),
                List.of("sentenca_socioeducativa", "guia_de_internacao_se_houver"),
                List.of("MINISTERIO_PUBLICO__MP_INFANCIA_TITULAR", "NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR"),
                List.of("Internação só é cabível em ato infracional grave com violência ou grave ameaça, ou por reiteração.", "Prazo máximo de internação é de 3 anos; prazo mínimo de revisão é de 6 meses.")
        );
    }

    private ProcessoVerticalEtapa etapaAcompanhamentoERevisao(ProcessoUnificadoAggregate unificado, ProcessoPrazoAggregate prazo, ProcessoTrabalhoAggregate trabalho) {
        return new ProcessoVerticalEtapa(
                "ACOMPANHAMENTO_REVISAO_ECA",
                "Acompanhamento da medida, revisão obrigatória e extinção",
                "EXECUCAO",
                "ASSISTENCIA_SOCIAL__ASSISTENCIA_SOCIAL_OPERACAO",
                "acompanhamento_eca",
                "green",
                prazo.marcosVencidos() > 0,
                atosPorCategoria(unificado, "EXECUCAO", "TRILHA"),
                merge(trabalho.proximoMelhorFluxo(), List.of("agendar_revisao_em_6_meses", "receber_relatorio_tecnico_periodico", "decidir_progressao_ou_extincao")),
                List.of("relatorio_acompanhamento", "decisao_revisao_medida"),
                List.of("MAGISTRADO_DIRETO", "MINISTERIO_PUBLICO__MP_INFANCIA_TITULAR"),
                List.of("Revisão obrigatória a cada 6 meses independentemente de requerimento das partes.", "Extinção da medida deve ser comunicada ao sistema nacional SINASE.")
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
        return merged.stream().filter(Objects::nonNull).toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A').replace('À', 'A').replace('Ã', 'A').replace('Â', 'A')
                .replace('É', 'E').replace('Ê', 'E').replace('Í', 'I')
                .replace('Ó', 'O').replace('Õ', 'O').replace('Ô', 'O')
                .replace('Ú', 'U').replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_");
    }
}
