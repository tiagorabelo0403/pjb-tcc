package com.tcc.pjb.backend.core.processo.sigilo.application;

import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.policy.application.ProcessoPolicyVigenciaApplicationService;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloFinding;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloGuarda;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.security.sigilo.repository.SigiloAccessRequestRepository;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoSigiloApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    private final ProcessoPolicyVigenciaApplicationService processoPolicyVigenciaApplicationService;
    private final SigiloAccessRequestRepository sigiloAccessRequestRepository;

    public ProcessoSigiloApplicationService(ProcessoRepository processoRepository,
                                            ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                            ProcessoDocumentoApplicationService processoDocumentoApplicationService,
                                            ProcessoPolicyVigenciaApplicationService processoPolicyVigenciaApplicationService,
                                            SigiloAccessRequestRepository sigiloAccessRequestRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoDocumentoApplicationService = Objects.requireNonNull(processoDocumentoApplicationService);
        this.processoPolicyVigenciaApplicationService = Objects.requireNonNull(processoPolicyVigenciaApplicationService);
        this.sigiloAccessRequestRepository = Objects.requireNonNull(sigiloAccessRequestRepository);
    }

    public ProcessoSigiloAggregate detalhar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoDocumentoAggregate documental = processoDocumentoApplicationService.detalhar(processoId);
        ProcessoPolicyAggregate policy = processoPolicyVigenciaApplicationService.avaliar(processoId);
        NivelSigilo nivelSigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        long pendingApprovals = sigiloAccessRequestRepository.findByProcessoIdAndStatus(processoId, com.tcc.pjb.backend.core.security.sigilo.SigiloAccessStatus.PENDENTE).size();
        long approvedCredentials = sigiloAccessRequestRepository.findByProcessoIdAndStatus(processoId, com.tcc.pjb.backend.core.security.sigilo.SigiloAccessStatus.APROVADA).size();

        ArrayList<ProcessoSigiloGuarda> guardas = new ArrayList<>(baseGuards(nivelSigilo));
        ArrayList<ProcessoSigiloFinding> findings = new ArrayList<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(unificado.competencia().fundamentos());
        fundamentos.addAll(policy.invariants());
        fundamentos.add("Segredo de justiça, acesso restrito, sigilo máximo e segredo de Estado precisam nascer como domínio processual e não como flag cosmética.");
        fundamentos.add("O PJB deve distinguir publicidade controlada, segredo judicial e segredo de Estado com guardas, credenciais e contexto institucional diferentes.");

        String visibilityMode = disclosureMode(nivelSigilo);
        boolean exigeStepUp = nivelSigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel();
        boolean exigeDuplaAutorizacao = nivelSigilo == NivelSigilo.SEGREDO_ESTADO || pendingApprovals > 0;
        LinkedHashSet<String> chips = new LinkedHashSet<>(List.of(visibilityMode.toLowerCase(Locale.ROOT), nivelSigilo.name()));
        chips.addAll(unificado.identity().etiquetas());
        if (nivelSigilo == NivelSigilo.PUBLICO && suggestsRestrictedScope(processo)) {
            findings.add(new ProcessoSigiloFinding(
                    "CLASSIFICACAO_PUBLICA_POTENCIALMENTE_INSUFICIENTE",
                    "Classificação pública pode estar abaixo do necessário",
                    "ALTA",
                    false,
                    "O assunto, a classe ou o contexto do processo indicam necessidade de segredo de justiça ou acesso restrito mais forte.",
                    List.of("RECLASSIFICAR_NIVEL_SIGILO", "REAVALIAR_VISIBILIDADE_POR_RAMO_E_ASSUNTO")
            ));
        }
        if (nivelSigilo.exigeCredencial() && approvedCredentials == 0 && allowsExternalSolicitation(nivelSigilo)) {
            findings.add(new ProcessoSigiloFinding(
                    "SEM_CREDENCIAL_ATIVA_DE_SIGILO",
                    "Não há credencial ativa de sigilo aprovada",
                    "MEDIA",
                    false,
                    "O processo restrito não possui credencial ativa aprovada, o que tende a bloquear acesso externo justificado.",
                    List.of("PROVISIONAR_CREDENCIAL_DE_SIGILO", "AUDITAR_REGRAS_DE_ACESSO_EXTERNO")
            ));
        }
        if (nivelSigilo == NivelSigilo.SEGREDO_ESTADO && !hasInstitutionalEnvelope(unificado)) {
            findings.add(new ProcessoSigiloFinding(
                    "SEGREDO_ESTADO_SEM_ENVELOPE_INSTITUCIONAL",
                    "Segredo de Estado sem envelope institucional suficiente",
                    "CRITICAL",
                    true,
                    "Segredo de Estado exige tribunal, unidade e contexto institucional materializados com segurança máxima.",
                    List.of("MATERIALIZAR_TRIBUNAL_UNIDADE", "ATIVAR_CONTEXTO_INSTITUCIONAL_MAXIMO")
            ));
        }
        if (nivelSigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel() && documental.trilhaAssinavel().isEmpty()) {
            findings.add(new ProcessoSigiloFinding(
                    "PROCESSO_RESTRITO_SEM_TRILHA_ASSINAVEL",
                    "Processo restrito sem trilha documental assinável",
                    "ALTA",
                    false,
                    "Fluxos sigilosos exigem trilha documental assinável, inclusive para parecer, despacho, decisão e peças sensíveis.",
                    List.of("ABRIR_VERSIONAMENTO_ASSINAVEL", "FORCAR_ASSINATURA_FORTE_NOS_ATOS_SENSIVEIS")
            ));
        }
        if (policy.blockingPolicies() > 0 && nivelSigilo.getNivel() >= NivelSigilo.SEGREDO_JUSTICA.getNivel()) {
            findings.add(new ProcessoSigiloFinding(
                    "POLITICA_VIGENTE_CRITICA_EM_AMBIENTE_SIGILOSO",
                    "Política vigente crítica em processo sigiloso",
                    "CRITICAL",
                    true,
                    "Há política crítica ativa e o processo já opera em regime sigiloso, o que recomenda bloqueio até saneamento.",
                    List.of("SANEAR_POLICY_ENGINE", "REVALIDAR_VIGENCIA_ANTES_DE_LIBERAR_ATO")
            ));
        }
        if (pendingApprovals > 0 && nivelSigilo == NivelSigilo.SEGREDO_ESTADO) {
            findings.add(new ProcessoSigiloFinding(
                    "SOLICITACAO_PENDENTE_EM_SEGREDO_DE_ESTADO",
                    "Solicitação pendente em segredo de Estado",
                    "ALTA",
                    false,
                    "Segredo de Estado exige aprovação rastreável e fechamento rápido da janela pendente.",
                    List.of("DECIDIR_SOLICITACAO_DE_SIGILO", "REGISTRAR_MOTIVO_DA_DECISAO")
            ));
        }

        findings.sort(Comparator.comparing(ProcessoSigiloFinding::blocking).reversed().thenComparing(ProcessoSigiloFinding::code));
        return new ProcessoSigiloAggregate(
                unificado.identity(),
                nivelSigilo,
                visibilityMode,
                nivelSigilo.exigeCredencial(),
                exigeStepUp,
                exigeDuplaAutorizacao,
                pendingApprovals,
                approvedCredentials,
                guardas.size(),
                findings.size(),
                List.copyOf(chips),
                allowedDirectProfiles(nivelSigilo),
                guardas,
                findings,
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private List<ProcessoSigiloGuarda> baseGuards(NivelSigilo nivelSigilo) {
        ArrayList<ProcessoSigiloGuarda> guardas = new ArrayList<>();
        guardas.add(new ProcessoSigiloGuarda(
                "PUBLICIDADE_CONTROLADA",
                "Publicidade controlada do processo",
                "VISIBILIDADE",
                "CONTROLADA",
                true,
                false,
                false,
                List.of("PUBLICO", "PESSOAL", "INSTITUCIONAL"),
                List.of("LEITURA_DO_PROCESSO")
        ));
        if (nivelSigilo.getNivel() >= NivelSigilo.SEGREDO_JUSTICA.getNivel()) {
            guardas.add(new ProcessoSigiloGuarda(
                    "STEP_UP_PARA_ACESSO_RESTRITO",
                    "Step-up obrigatório para ambiente restrito",
                    "ACESSO",
                    "ALTA",
                    true,
                    false,
                    true,
                    List.of("INSTITUCIONAL", "SIGILO_CREDENCIADO"),
                    List.of("STEP_UP", "CONTEXTO_INSTITUCIONAL_ATIVO")
            ));
        }
        if (nivelSigilo.getNivel() >= NivelSigilo.SIGILO_N3.getNivel()) {
            guardas.add(new ProcessoSigiloGuarda(
                    "MASKING_E_AUDITORIA_FORTE",
                    "Mascaramento de dados e auditoria forte",
                    "DADOS",
                    "ELEVADA",
                    true,
                    false,
                    true,
                    List.of("INSTITUCIONAL", "SIGILO_CREDENCIADO"),
                    List.of("TRILHA_FORENSE", "MASCARAMENTO_DADOS")
            ));
        }
        if (nivelSigilo == NivelSigilo.SEGREDO_ESTADO) {
            guardas.add(new ProcessoSigiloGuarda(
                    "DUPLA_AUTORIZACAO_SEGREDO_ESTADO",
                    "Dupla autorização para segredo de Estado",
                    "DECISAO",
                    "CRITICAL",
                    true,
                    true,
                    true,
                    List.of("INSTITUCIONAL_MAXIMO"),
                    List.of("DUPLA_ADMINISTRACAO", "STEP_UP_MAXIMO", "REDE_INSTITUCIONAL")
            ));
        }
        return List.copyOf(guardas);
    }

    private boolean suggestsRestrictedScope(Processo processo) {
        String descriptor = normalize(processo.getClasseProcessual()) + ' ' + normalize(processo.getAssunto()) + ' ' + normalize(processo.getObjetoProcessual());
        return descriptor.contains("INFANC")
                || descriptor.contains("ADOCAO")
                || descriptor.contains("INTERCEPT")
                || descriptor.contains("ABUSO")
                || descriptor.contains("VIOLENCIA_DOMESTICA")
                || descriptor.contains("MEDIDA_PROTETIVA")
                || descriptor.contains("INTELIGENCIA")
                || descriptor.contains("ESTADO");
    }

    private boolean hasInstitutionalEnvelope(ProcessoUnificadoAggregate unificado) {
        return notBlank(unificado.competencia().tribunalCodigo())
                && notBlank(unificado.competencia().orgaoJulgadorSugerido())
                && notBlank(unificado.competencia().unidadeJudiciariaSugerida());
    }

    private boolean allowsExternalSolicitation(NivelSigilo nivelSigilo) {
        return nivelSigilo != NivelSigilo.SEGREDO_ESTADO;
    }

    private String disclosureMode(NivelSigilo nivelSigilo) {
        if (nivelSigilo == NivelSigilo.PUBLICO) {
            return "PUBLICO_CONTROLADO";
        }
        if (nivelSigilo == NivelSigilo.SEGREDO_ESTADO) {
            return "SEGREDO_DE_ESTADO";
        }
        if (nivelSigilo == NivelSigilo.SEGREDO_JUSTICA) {
            return "SEGREDO_JUDICIAL";
        }
        return "RESTRICAO_FORTE";
    }

    private List<String> allowedDirectProfiles(NivelSigilo nivelSigilo) {
        if (nivelSigilo == NivelSigilo.PUBLICO) {
            return List.of("MAGISTRADO_DIRETO", "ADVOGADO_DIRETO");
        }
        if (nivelSigilo == NivelSigilo.SEGREDO_ESTADO) {
            return List.of("MAGISTRADO_DIRETO");
        }
        return List.of("MAGISTRADO_DIRETO", "ADVOGADO_DIRETO", "PROMOTORIA__PROMOTORIA_TITULAR", "NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replace('Ã', 'A')
                .replace('Á', 'A')
                .replace('É', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ú', 'U');
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
