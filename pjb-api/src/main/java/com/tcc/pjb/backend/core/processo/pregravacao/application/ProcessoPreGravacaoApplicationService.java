package com.tcc.pjb.backend.core.processo.pregravacao.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.application.InstitutionalProceduralCoherenceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralActEvaluation;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceFinding;
import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelPerfil;
import com.tcc.pjb.backend.core.processo.policy.application.ProcessoPolicyVigenciaApplicationService;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyAggregate;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyDecision;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoMarco;
import com.tcc.pjb.backend.core.processo.pregravacao.domain.ProcessoPreGravacaoAggregate;
import com.tcc.pjb.backend.core.processo.pregravacao.domain.ProcessoPreGravacaoTrigger;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPreGravacaoApplicationService {

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoPrazoApplicationService processoPrazoApplicationService;
    private final ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    private final ProcessoPolicyVigenciaApplicationService processoPolicyVigenciaApplicationService;
    private final ProcessoPapelApplicationService processoPapelApplicationService;
    private final InstitutionalProceduralCoherenceApplicationService institutionalProceduralCoherenceApplicationService;

    public ProcessoPreGravacaoApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                                 ProcessoPrazoApplicationService processoPrazoApplicationService,
                                                 ProcessoDocumentoApplicationService processoDocumentoApplicationService,
                                                 ProcessoPolicyVigenciaApplicationService processoPolicyVigenciaApplicationService,
                                                 ProcessoPapelApplicationService processoPapelApplicationService,
                                                 InstitutionalProceduralCoherenceApplicationService institutionalProceduralCoherenceApplicationService) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoPrazoApplicationService = Objects.requireNonNull(processoPrazoApplicationService);
        this.processoDocumentoApplicationService = Objects.requireNonNull(processoDocumentoApplicationService);
        this.processoPolicyVigenciaApplicationService = Objects.requireNonNull(processoPolicyVigenciaApplicationService);
        this.processoPapelApplicationService = Objects.requireNonNull(processoPapelApplicationService);
        this.institutionalProceduralCoherenceApplicationService = Objects.requireNonNull(institutionalProceduralCoherenceApplicationService);
    }

    public ProcessoPreGravacaoAggregate avaliar(Long processoId, String profileCode, String actionCode) {
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoPrazoAggregate prazo = processoPrazoApplicationService.detalhar(processoId);
        ProcessoDocumentoAggregate documento = processoDocumentoApplicationService.detalhar(processoId);
        ProcessoPolicyAggregate policy = processoPolicyVigenciaApplicationService.avaliar(processoId);
        ProcessoPapelPerfil perfil = processoPapelApplicationService.detalharPerfil(processoId, profileCode);
        Optional<ProcessoUnificadoAto> atoCatalogado = localizarAto(unificado, actionCode);
        InstitutionalProceduralActEvaluation coherence = avaliarCoerencia(profileCode, actionCode, unificado);

        ArrayList<ProcessoPreGravacaoTrigger> triggers = new ArrayList<>();
        LinkedHashSet<String> mandatoryGuards = new LinkedHashSet<>(coherence.mandatoryGuards());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(unificado.competencia().fundamentos());
        fundamentos.addAll(policy.invariants());
        fundamentos.addAll(perfil.fundamentos());
        fundamentos.addAll(coherence.fundamentos());

        if (atoCatalogado.isEmpty()) {
            triggers.add(new ProcessoPreGravacaoTrigger(
                    "ATO_FORA_DO_CATALOGO",
                    "CATALOGO_ATOS",
                    "ALTA",
                    true,
                    false,
                    "A gravação pretendida não está no catálogo processual ativo do processo.",
                    "Nenhum fluxo sensível deve persistir ato não mapeado para o rito, fase, status e ramo correntes.",
                    List.of(actionCode),
                    List.of("usar_ato_do_catalogo_ativo", "revalidar_rito_fase_status")
            ));
        }

        if (!coherence.allowed() || coherence.blocking()) {
            triggers.add(new ProcessoPreGravacaoTrigger(
                    "COERENCIA_PROCESSUAL_NEGADA",
                    "COERENCIA",
                    "ALTA",
                    true,
                    false,
                    "O motor de coerência não autorizou a persistência do ato para o perfil informado.",
                    coherence.decision(),
                    coherence.findings().stream().map(InstitutionalProceduralCoherenceFinding::message).toList(),
                    merge(coherence.mandatoryGuards(), List.of("resolver_findings_de_coerencia", "reativar_contexto_adequado"))
            ));
        }

        if (unificado.diagnostico().blockingFindings() > 0) {
            triggers.add(new ProcessoPreGravacaoTrigger(
                    "DIAGNOSTICO_BLOQUEANTE_DO_PROCESSO",
                    "PROCESSO_UNIFICADO",
                    "ALTA",
                    true,
                    false,
                    "O processo possui achados bloqueantes no diagnóstico estrutural.",
                    "Nenhum ato sensível deve ser salvo enquanto o processo tiver lacunas estruturais impeditivas.",
                    unificado.diagnostico().findings().stream().filter(item -> item.blocking()).map(item -> item.code() + ':' + item.detail()).toList(),
                    List.of("sanear_diagnostico_estrutural", "corrigir_competencia_e_cabimento")
            ));
        }

        if (policy.blockingPolicies() > 0) {
            triggers.add(new ProcessoPreGravacaoTrigger(
                    "POLITICA_VIGENTE_COM_RESTRICAO",
                    "VIGENCIA",
                    "ALTA",
                    true,
                    false,
                    "A política versionada aponta bloco com restrição ativa ou cobertura parcial incompatível com gravação direta.",
                    "Fluxo sensível exige regra vigente clara na data da operação.",
                    policy.decisions().stream().filter(this::isPolicyBlocking).map(item -> item.code() + ':' + item.summary()).toList(),
                    List.of("revalidar_regra_vigente", "registrar_excecao_formal_antes_da_persistencia")
            ));
        }

        if (prazo.marcosVencidos() > 0 && isRecursalOrEmbargos(actionCode, atoCatalogado.orElse(null), perfil)) {
            triggers.add(new ProcessoPreGravacaoTrigger(
                    "JANELA_RECURSAL_OU_EMBARGOS_VENCIDA",
                    "PRAZO",
                    "ALTA",
                    true,
                    false,
                    "Há marco vencido em trilha recursal ou de embargos para o ato pretendido.",
                    "Persistência recursal ou de embargos em janela vencida precisa de saneamento temporal explícito.",
                    prazo.marcos().stream().filter(ProcessoPrazoMarco::vencido).map(item -> item.codigo() + ':' + item.vencimento()).toList(),
                    List.of("reavaliar_tempestividade", "registrar_justificativa_ou_ato_substitutivo")
            ));
        }

        if (documento.totalDocumentos() == 0 && requiresDocumentTrail(actionCode, atoCatalogado.orElse(null), perfil)) {
            triggers.add(new ProcessoPreGravacaoTrigger(
                    "SEM_BASE_DOCUMENTAL_MINIMA",
                    "DOCUMENTAL",
                    "ALTA",
                    true,
                    false,
                    "O ato sensível depende de base documental mínima e o processo não possui lote documental consolidado.",
                    "Parecer, petição, recurso, embargo, laudo ou assinatura final não devem nascer sem trilha documental.",
                    List.of("documentos=0", "minutas=" + documento.minutas(), "assinados=" + documento.assinados()),
                    List.of("materializar_lote_documental", "gerar_minuta_ou_peca_base")
            ));
        }

        if (documento.minutas() == 0 && requiresDraft(actionCode, perfil)) {
            triggers.add(new ProcessoPreGravacaoTrigger(
                    "ATO_SENSIVEL_SEM_MINUTA_BASE",
                    "DOCUMENTAL",
                    "MEDIA",
                    false,
                    false,
                    "O ato sensível não possui minuta base mapeada.",
                    "Fluxos de aprovação, parecer, decisão, petição e assinatura devem respeitar trilha de minuta/versionamento.",
                    List.of("minutas=" + documento.minutas(), "trilhaAssinavel=" + documento.trilhaAssinavel().size()),
                    List.of("gerar_minuta", "abrir_versionamento")
            ));
        }

        if (atoCatalogado.filter(ato -> ato.sensivel() || ato.exigeSegurancaElevada()).isPresent() || !coherence.mandatoryGuards().isEmpty()) {
            triggers.add(new ProcessoPreGravacaoTrigger(
                    "STEP_UP_OBRIGATORIO",
                    "SEGURANCA",
                    "ELEVADA",
                    false,
                    true,
                    "A persistência exige step-up antes da gravação definitiva.",
                    "Ato sensível, assinatura forte ou guarda mandatória exigem MFA reforçado, certificado ou dupla conferência.",
                    merge(coherence.mandatoryGuards(), atoCatalogado.map(ProcessoUnificadoAto::alertas).orElse(List.of())),
                    List.of("executar_step_up", "confirmar_assinatura_forte", "validar_dupla_checagem")
            ));
        }

        if (!hasAuthorityForAction(perfil, actionCode, atoCatalogado.orElse(null))) {
            triggers.add(new ProcessoPreGravacaoTrigger(
                    "PERFIL_SEM_FAIXA_DE_AUTORIDADE",
                    "PAPEL_PROCESSUAL",
                    "ALTA",
                    true,
                    false,
                    "O perfil informado não demonstra faixa de autoridade suficiente para o ato pretendido.",
                    "A persistência deve respeitar a faixa de autoridade por papel, não apenas autenticação genérica.",
                    List.of(profileCode, actionCode),
                    List.of("escalar_para_papel_titular", "mudar_acao_para_fluxo_de_preparacao")
            ));
        }

        List<ProcessoPreGravacaoTrigger> ordered = triggers.stream()
                .sorted(Comparator.comparing(ProcessoPreGravacaoTrigger::blocking).reversed()
                        .thenComparing(ProcessoPreGravacaoTrigger::stepUpRequired).reversed()
                        .thenComparing(ProcessoPreGravacaoTrigger::code))
                .toList();
        List<String> correctivePlan = ordered.stream()
                .flatMap(item -> item.correctiveActions().stream())
                .distinct()
                .toList();
        boolean persistenciaPermitida = ordered.stream().noneMatch(ProcessoPreGravacaoTrigger::blocking);
        return new ProcessoPreGravacaoAggregate(
                unificado.identity(),
                profileCode,
                normalize(actionCode),
                persistenciaPermitida,
                ordered.size(),
                ordered.stream().filter(ProcessoPreGravacaoTrigger::blocking).count(),
                ordered.stream().filter(ProcessoPreGravacaoTrigger::stepUpRequired).count(),
                mandatoryGuards.size(),
                List.copyOf(mandatoryGuards),
                ordered,
                correctivePlan,
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private InstitutionalProceduralActEvaluation avaliarCoerencia(String profileCode,
                                                                  String actionCode,
                                                                  ProcessoUnificadoAggregate unificado) {
        return institutionalProceduralCoherenceApplicationService.avaliarAto(
                profileCode,
                actionCode,
                unificado.identity().processoId(),
                unificado.competencia().ritoProcessual(),
                unificado.competencia().faseProcessual(),
                unificado.competencia().statusProcessual(),
                unificado.competencia().ramoDireito()
        );
    }

    private Optional<ProcessoUnificadoAto> localizarAto(ProcessoUnificadoAggregate unificado, String actionCode) {
        return merge(unificado.atosPermitidos(), unificado.atosBloqueados()).stream()
                .filter(item -> normalize(item.codigo()).equals(normalize(actionCode)))
                .findFirst();
    }

    private boolean hasAuthorityForAction(ProcessoPapelPerfil perfil, String actionCode, ProcessoUnificadoAto ato) {
        String normalized = normalize(actionCode);
        if (matches(normalized, perfil.assinar(), ato)) return true;
        if (matches(normalized, perfil.preparar(), ato)) return true;
        if (matches(normalized, perfil.aprovar(), ato)) return true;
        if (matches(normalized, perfil.peticionar(), ato)) return true;
        if (matches(normalized, perfil.certificar(), ato)) return true;
        if (matches(normalized, perfil.redistribuir(), ato)) return true;
        if (matches(normalized, perfil.recorrer(), ato)) return true;
        return matches(normalized, perfil.embargar(), ato);
    }

    private boolean matches(String actionCode, List<String> authorityBucket, ProcessoUnificadoAto ato) {
        if (authorityBucket == null || authorityBucket.isEmpty()) {
            return false;
        }
        String title = ato == null ? "" : normalize(ato.titulo());
        String category = ato == null ? "" : normalize(ato.categoria());
        return authorityBucket.stream().map(this::normalize).anyMatch(item -> item.contains(actionCode)
                || (!title.isBlank() && item.contains(title))
                || (!category.isBlank() && item.contains(category)));
    }

    private boolean isRecursalOrEmbargos(String actionCode, ProcessoUnificadoAto ato, ProcessoPapelPerfil perfil) {
        String normalized = normalize(actionCode);
        if (normalized.contains("RECUR") || normalized.contains("CONTRARRAZ") || normalized.contains("EMBARG")) {
            return true;
        }
        if (ato != null && (ato.recursal() || normalize(ato.categoria()).contains("EMBARG"))) {
            return true;
        }
        return perfil.recorrer().stream().map(this::normalize).anyMatch(item -> item.contains(normalized))
                || perfil.embargar().stream().map(this::normalize).anyMatch(item -> item.contains(normalized));
    }

    private boolean requiresDocumentTrail(String actionCode, ProcessoUnificadoAto ato, ProcessoPapelPerfil perfil) {
        String normalized = normalize(actionCode);
        return normalized.contains("ASSIN")
                || normalized.contains("PETIC")
                || normalized.contains("PARECER")
                || normalized.contains("RECUR")
                || normalized.contains("EMBARG")
                || normalized.contains("LAUDO")
                || normalized.contains("MINUTA")
                || (ato != null && (ato.sensivel() || ato.exigeSegurancaElevada()))
                || !perfil.assinar().isEmpty();
    }

    private boolean requiresDraft(String actionCode, ProcessoPapelPerfil perfil) {
        String normalized = normalize(actionCode);
        return normalized.contains("ASSIN")
                || normalized.contains("PARECER")
                || normalized.contains("DECISAO")
                || normalized.contains("PETIC")
                || normalized.contains("RECUR")
                || perfil.aprovar().stream().map(this::normalize).anyMatch(item -> item.contains(normalized));
    }

    private boolean isPolicyBlocking(ProcessoPolicyDecision decision) {
        return decision.active() && (!decision.deferredRules().isEmpty() || normalize(decision.severity()).contains("CRITICA"));
    }

    private <T> List<T> merge(List<T> left, List<T> right) {
        LinkedHashSet<T> merged = new LinkedHashSet<>();
        if (left != null) merged.addAll(left);
        if (right != null) merged.addAll(right);
        return List.copyOf(merged);
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
