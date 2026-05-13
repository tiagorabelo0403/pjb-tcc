package com.tcc.pjb.backend.service.processual.participacao.workspace;

import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.processual.participacao.ActionProfile;
import com.tcc.pjb.backend.service.processual.participacao.Persona;
import com.tcc.pjb.backend.service.processual.participacao.ProcessualParticipacaoAtivaSupportUtils;
import com.tcc.pjb.backend.service.processual.participacao.SignaturePolicy;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionRequest;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionView;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ProcessualParticipacaoAtivaWorkspaceSupport {

    private final DocumentoProcessualRepository documentoRepository;
    private final WorkItemRepository workItemRepository;
    private final RepresentacaoProcessualPolicyService representacaoPolicyService;

    public ProcessualParticipacaoAtivaWorkspaceSupport(DocumentoProcessualRepository documentoRepository,
                                                WorkItemRepository workItemRepository,
                                                RepresentacaoProcessualPolicyService representacaoPolicyService) {
        this.documentoRepository = Objects.requireNonNull(documentoRepository, "documentoRepository");
        this.workItemRepository = Objects.requireNonNull(workItemRepository, "workItemRepository");
        this.representacaoPolicyService = Objects.requireNonNull(representacaoPolicyService, "representacaoPolicyService");
    }

    public CapabilityMatrix buildCapabilityMatrix(Processo processo, Usuario usuario, Persona persona) {
        SignaturePolicy signaturePolicy = buildSignaturePolicy(persona, processo);
        LinkedHashSet<String> capacities = new LinkedHashSet<>();
        capacities.add("WORKSPACE_UNIFICADO_POR_PERFIL");
        capacities.add("TRIAGEM_TOPOLOGICA_AUTOMATICA");
        capacities.add("JUNTADA_COM_HASH_E_IDEMPOTENCIA");
        capacities.add("SEGREGACAO_POR_SIGILO_E_FASE");
        if (signaturePolicy.certificateRequired()) {
            capacities.add("CERTIFICADO_OBRIGATORIO");
        }
        if (signaturePolicy.mfaRecommended()) {
            capacities.add("PASSKEY_OU_STEP_UP_RECOMENDADO");
        }
        List<ActionProfile> actions = resolveActionCatalog(processo.getFaseAtual(), persona, signaturePolicy);
        return new CapabilityMatrix(List.copyOf(capacities), List.copyOf(actions));
    }

    public List<SubmissionView> buildRecentSubmissions(Processo processo, Usuario usuario, CapabilityMatrix matrix, int limit) {
        List<DocumentoProcessual> docs = documentoRepository.findRecentContextoByProcessoId(processo.getId(), Math.max(limit * 3, 20));
        if (docs.isEmpty() || usuario.getId() == null) {
            return List.of();
        }
        return docs.stream()
                .filter(doc -> Objects.equals(doc.getCriadoPor(), usuario.getId()))
                .filter(doc -> doc.getOrigemSistema() != null && doc.getOrigemSistema().startsWith(ProcessualParticipacaoAtivaSupportUtils.ORIGEM_SISTEMA))
                .sorted(Comparator.comparing(DocumentoProcessual::getCriadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(doc -> new SubmissionView(
                        doc.getId().toString(),
                        ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(doc.getTitulo(), doc.getNomeOriginal()),
                        matrix.closestActionFor(doc).map(ActionProfile::label).orElse("Submissão ativa"),
                        doc.getCriadoEm() == null ? null : doc.getCriadoEm().toInstant(ZoneOffset.UTC),
                        doc.getContentType(),
                        doc.getTamanhoBytes(),
                        ProcessualParticipacaoAtivaSupportUtils.safeName(doc.getNivelSigilo()),
                        doc.getStorageUri(),
                        List.of(ProcessualParticipacaoAtivaSupportUtils.safeName(doc.getCategoria()), ProcessualParticipacaoAtivaSupportUtils.ORIGEM_SISTEMA)
                ))
                .toList();
    }

    public List<PendingView> buildPendingViews(Processo processo,
                                        Usuario usuario,
                                        Persona persona,
                                        CapabilityMatrix capabilityMatrix) {
        LinkedHashMap<String, PendingView> map = new LinkedHashMap<>();
        PendingView inferred = inferByPhase(processo, persona, capabilityMatrix);
        if (inferred != null) {
            map.put(inferred.codigo(), inferred);
        }
        List<TipoUsuario> roles = ProcessualParticipacaoAtivaSupportUtils.resolveActorRoles(usuario.getTipoUsuario());
        if (!roles.isEmpty() && usuario.getId() != null) {
            List<WorkItem> linked = workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(
                    processo.getId(),
                    usuario.getId(),
                    roles,
                    WorkItemStatus.CANCELADO,
                    PageRequest.of(0, 12));
            for (WorkItem workItem : linked) {
                String code = workItem.getTemplateCode() == null ? "WORKITEM:" + workItem.getId() : workItem.getTemplateCode();
                map.put(code, new PendingView(
                        code,
                        workItem.getTitulo(),
                        capabilityMatrix.closestActionFor(workItem.getType()).map(ActionProfile::code).orElse(null),
                        capabilityMatrix.closestActionFor(workItem.getType()).map(ActionProfile::label).orElse(null),
                        ProcessualParticipacaoAtivaSupportUtils.safeName(workItem.getFaseOrigem()),
                        workItem.getPrioridade() == null ? 3 : workItem.getPrioridade(),
                        workItem.isBlocking(),
                        workItem.getDueAt(),
                        List.of(
                                ProcessualParticipacaoAtivaSupportUtils.safeName(workItem.getType()),
                                ProcessualParticipacaoAtivaSupportUtils.safeName(workItem.getAssignedRole()),
                                ProcessualParticipacaoAtivaSupportUtils.safeName(workItem.getStatus())
                        )
                ));
            }
        }
        return List.copyOf(map.values());
    }

    public RoutingView buildRouting(Processo processo, Persona persona, Usuario usuario) {
        String scope = ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(
                ProcessualParticipacaoAtivaSupportUtils.trimToNull(processo.getUnidadeJudiciariaCodigo()),
                ProcessualParticipacaoAtivaSupportUtils.trimToNull(processo.getVara()),
                ProcessualParticipacaoAtivaSupportUtils.trimToNull(processo.getComarca()),
                ProcessualParticipacaoAtivaSupportUtils.trimToNull(processo.getTribunal()),
                "UNIDADE_DESCONHECIDA");
        String scopeToken = ProcessualParticipacaoAtivaSupportUtils.normalizeToken(scope);
        String tribunal = ProcessualParticipacaoAtivaSupportUtils.normalizeToken(processo.getTribunal());
        String uf = ProcessualParticipacaoAtivaSupportUtils.normalizeUf(processo.getUf());
        String comarca = ProcessualParticipacaoAtivaSupportUtils.normalizeToken(processo.getComarca());
        String inboxKey = String.join(":",
                "SECRETARIA",
                "PARTICIPACAO_ATIVA",
                ProcessualParticipacaoAtivaSupportUtils.blankToDefault(tribunal, "TRIBUNAL"),
                ProcessualParticipacaoAtivaSupportUtils.blankToDefault(uf, "UF"),
                ProcessualParticipacaoAtivaSupportUtils.blankToDefault(comarca, "COMARCA"),
                ProcessualParticipacaoAtivaSupportUtils.blankToDefault(scopeToken, "UNIDADE"));
        String queueCode = String.join(":",
                "QUEUE",
                "SECRETARIA",
                "PARTICIPACAO_ATIVA",
                persona.name(),
                ProcessualParticipacaoAtivaSupportUtils.blankToDefault(scopeToken, "UNIDADE"));
        return new RoutingView(inboxKey, queueCode, scope, persona.name(), usuario.getId(), List.of(
                "ROTEAMENTO_TOPOLOGICO",
                "RECEPCAO_INTELIGENTE",
                "TRILHA_UNIFICADA"
        ));
    }

    public SignaturePolicy buildSignaturePolicy(Persona persona, Processo processo) {
        boolean certificateRequired = switch (persona) {
            case DEFENSORIA, PROCURADORIA, MINISTERIO_PUBLICO, PERICIA -> true;
            case ADVOCACIA_PRIVADA, NAO_SUPORTADA -> false;
        };
        boolean mfaRecommended = persona != Persona.NAO_SUPORTADA;
        boolean strongChannel = ProcessualParticipacaoAtivaSupportUtils.resolveSigilo(processo).getNivel() >= NivelSigilo.SIGILO_N2.getNivel()
                || persona != Persona.ADVOCACIA_PRIVADA;
        List<String> admissible = persona == Persona.ADVOCACIA_PRIVADA
                ? List.of("ICP_BRASIL", "PASSKEY_GOVBR", "ASSINATURA_PJB_AVANCADA")
                : List.of("ICP_BRASIL", "CERTIFICADO_INSTITUCIONAL", "ASSINATURA_PJB_INSTITUCIONAL");
        List<String> guards = new ArrayList<>();
        guards.add("HASH_SHA256_E_SHA384");
        guards.add("IDEMPOTENCIA_POR_CONTEUDO");
        if (strongChannel) {
            guards.add("CANAL_FORTE_RECOMENDADO");
        }
        if (certificateRequired) {
            guards.add("SERIAL_DE_CERTIFICADO_OBRIGATORIO");
        }
        if (mfaRecommended) {
            guards.add("STEP_UP_RECOMENDADO");
        }
        return new SignaturePolicy(
                persona.name(),
                certificateRequired,
                mfaRecommended,
                strongChannel,
                admissible,
                List.copyOf(guards),
                persona == Persona.ADVOCACIA_PRIVADA ? "ADVOGACIA_HIBRIDA_V2026" : "INSTITUCIONAL_FORTE_V2026"
        );
    }

    public RepresentationGuardView buildRepresentationGuard(Processo processo,
                                                     Usuario usuario,
                                                     Persona persona,
                                                     SubmissionRequest request,
                                                     boolean strict) {
        RepresentacaoProcessualPolicyResponse policy = representacaoPolicyService.resolve(
                processo,
                usuario,
                request == null ? null : request.instrumentoRepresentacao(),
                null,
                request == null ? null : request.tipoAudiencia(),
                request != null && Boolean.TRUE.equals(request.contextoConsensual()),
                request != null && Boolean.TRUE.equals(request.poderesEspeciaisTransigir()),
                request == null ? null : request.termoAudienciaReferencia(),
                request == null ? null : request.ataAudienciaReferencia()
        );
        boolean possuiDocumentoRepresentacao = request != null && Boolean.TRUE.equals(request.possuiDocumentoRepresentacao());
        boolean possuiIdentificacaoProfissional = persona != Persona.ADVOCACIA_PRIVADA
                || (request != null && (Boolean.TRUE.equals(request.possuiIdentificacaoProfissional())
                || ProcessualParticipacaoAtivaSupportUtils.trimToNull(request.identificacaoProfissional()) != null));
        boolean regularidade = strict
                ? representacaoPolicyService.representacaoSuficiente(policy, possuiDocumentoRepresentacao, possuiIdentificacaoProfissional)
                : (!policy.exigeProcuracaoFormal() || policy.dispensaMandatoPorFuncaoInstitucional());
        String status;
        if (strict) {
            status = regularidade ? "REGULAR" : "PENDENTE_OU_INSUFICIENTE";
        } else if (policy.exigeProcuracaoFormal() && persona == Persona.ADVOCACIA_PRIVADA) {
            status = "CONFERENCIA_REPRESENTACAO_REQUERIDA";
        } else {
            status = "REGRA_REPRESENTACAO_RESOLVIDA";
        }
        LinkedHashSet<String> documentos = new LinkedHashSet<>();
        documentos.addAll(policy.documentosBase());
        documentos.addAll(policy.documentosAudiencia());
        LinkedHashSet<String> validacoes = new LinkedHashSet<>(policy.validacoesObrigatorias());
        if (policy.exigeProcuracaoFormal() && persona == Persona.ADVOCACIA_PRIVADA) {
            validacoes.add("COMPROVAR_VINCULO_REPRESENTATIVO_ATIVO");
        }
        return new RepresentationGuardView(
                status,
                regularidade,
                policy.exigeProcuracaoFormal(),
                policy.dispensaMandatoPorFuncaoInstitucional(),
                policy.resolvedInstrument(),
                policy.regimePostulacao(),
                List.copyOf(documentos),
                List.copyOf(validacoes),
                policy.alertas()
        );
    }

    public SecurityGuardView buildSecurityGuard(Processo processo,
                                         SignaturePolicy signaturePolicy,
                                         ActionProfile action,
                                         SubmissionRequest request) {
        NivelSigilo targetSigilo = request == null || action == null
                ? ProcessualParticipacaoAtivaSupportUtils.resolveSigilo(processo)
                : ProcessualParticipacaoAtivaSupportUtils.resolveRequestedSigilo(request, processo, action);
        boolean canalForteObrigatorio = signaturePolicy.strongChannel() || targetSigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel();
        boolean certificadoObrigatorio = signaturePolicy.certificateRequired() || targetSigilo.getNivel() >= NivelSigilo.SIGILO_N3.getNivel();
        boolean stepUpObrigatorio = targetSigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel() || (action != null && action.highSensitivity());
        boolean restritoAAtuacaoInstitucional = targetSigilo.getNivel() >= NivelSigilo.SIGILO_N4.getNivel() || targetSigilo == NivelSigilo.SEGREDO_ESTADO;
        List<String> invariantes = new ArrayList<>();
        invariantes.add("HASH_FORTE_DOCUMENTAL");
        invariantes.add("RECEPCAO_TOPOLOGICA_IMUTAVEL");
        if (canalForteObrigatorio) {
            invariantes.add("CANAL_FORTE_OBRIGATORIO");
        }
        if (certificadoObrigatorio) {
            invariantes.add("CERTIFICADO_OU_SERIAL_OBRIGATORIO");
        }
        if (stepUpObrigatorio) {
            invariantes.add("STEP_UP_CONFIRMADO");
        }
        if (restritoAAtuacaoInstitucional) {
            invariantes.add("ATUACAO_INSTITUCIONAL_OU_PERICIAL_QUALIFICADA");
        }
        List<String> alertas = new ArrayList<>();
        if (targetSigilo.getNivel() >= NivelSigilo.SIGILO_N3.getNivel()) {
            alertas.add("Sigilo elevado exige assinatura forte e certificado identificável.");
        }
        if (restritoAAtuacaoInstitucional) {
            alertas.add("Nível de sigilo máximo não deve circular por credenciais leves ou representação privada sem reforço institucional.");
        }
        String classificacao = switch (targetSigilo) {
            case PUBLICO -> "ABERTO_GOVERNADO";
            case SEGREDO_JUSTICA, SIGILO_N2 -> "RESTRITO_REFORCADO";
            case SIGILO_N3, SIGILO_N4 -> "ALTA_RESTRICAO";
            case SEGREDO_ESTADO -> "MAXIMA_RESTRICAO";
        };
        return new SecurityGuardView(
                classificacao,
                targetSigilo.name(),
                canalForteObrigatorio,
                certificadoObrigatorio,
                stepUpObrigatorio,
                restritoAAtuacaoInstitucional,
                List.of("ICP_BRASIL", "CERTIFICADO_INSTITUCIONAL", "ASSINATURA_PJB_INSTITUCIONAL"),
                List.copyOf(invariantes),
                List.copyOf(alertas)
        );
    }

    public DeadlineGuardView buildDeadlineGuard(Processo processo,
                                         List<ActionProfile> actions,
                                         List<PendingView> pendencias,
                                         boolean urgent,
                                         String referenciaPrazo) {
        Instant nextPending = pendencias == null ? null : pendencias.stream()
                .map(PendingView::dueAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        ActionProfile leadAction = actions == null || actions.isEmpty() ? null : actions.getFirst();
        Instant dueAt = nextPending != null ? nextPending : ProcessualParticipacaoAtivaSupportUtils.resolveDueAt(urgent, processo.getFaseAtual());
        String lane = urgent ? "FAST_TRACK" : switch (processo.getFaseAtual() == null ? FaseProcessual.CONHECIMENTO : processo.getFaseAtual()) {
            case RECURSAL -> "RECURSAL_EXPRESS";
            case PERICIA_TECNICA, AUDIENCIA_CUSTODIA -> "SENSIVEL_DE_CURTO_PRAZO";
            case EXECUCAO, CUMPRIMENTO_SENTENCA -> "EXECUCAO_CONTROLADA";
            default -> "ORDINARIO_GOVERNADO";
        };
        List<String> signals = new ArrayList<>();
        signals.add("PRAZO_VIVO_NO_WORKSPACE");
        if (leadAction != null) {
            signals.add("ACAO_LIDER=" + leadAction.code());
        }
        if (nextPending != null) {
            signals.add("PENDENCIA_EXISTENTE_PRIORIZADA");
        }
        if (ProcessualParticipacaoAtivaSupportUtils.trimToNull(referenciaPrazo) != null) {
            signals.add("REFERENCIA_USUARIO_PRESENTE");
        }
        return new DeadlineGuardView(lane, dueAt, nextPending, urgent, ProcessualParticipacaoAtivaSupportUtils.trimToNull(referenciaPrazo), List.copyOf(signals));
    }

    public ExperienceDifferentialView buildExperienceDifferential(Processo processo,
                                                           Persona persona,
                                                           CapabilityMatrix matrix,
                                                           SignaturePolicy signaturePolicy,
                                                           RoutingView routing,
                                                           List<PendingView> pendencias) {
        List<String> differences = new ArrayList<>();
        differences.add("Workspace por perfil e fase, sem separar o usuário entre portais diferentes.");
        differences.add("Recepção automática em secretaria ou unidade correta, com inbox topológica.");
        differences.add("Juntada em lote com hash, idempotência por conteúdo e trilha de auditoria interna.");
        differences.add("Ação sugerida já aberta conforme a fase processual e o papel do ator.");
        if (signaturePolicy.certificateRequired()) {
            differences.add("Assinatura institucional forte exigida para a categoria atual de atuação.");
        }
        if (!pendencias.isEmpty()) {
            differences.add("Pendências e prazos do próprio ator aparecem dentro do processo e não em filas paralelas.");
        }
        differences.add("Representação, sigilo e prazo vivem no mesmo workspace, evitando protocolo cego e retrabalho cartorário.");
        return new ExperienceDifferentialView(
                persona.label(),
                ProcessualParticipacaoAtivaSupportUtils.safeName(processo.getFaseAtual()),
                matrix.actions().stream().map(ActionProfile::label).limit(4).toList(),
                differences,
                routing.inboxKey(),
                Instant.now()
        );
    }

    private List<ActionProfile> resolveActionCatalog(FaseProcessual fase, Persona persona, SignaturePolicy signaturePolicy) {
        FaseProcessual normalized = fase == null ? FaseProcessual.CONHECIMENTO : fase;
        List<ActionProfile> out = new ArrayList<>();
        switch (persona) {
            case ADVOCACIA_PRIVADA -> {
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("APRESENTAR_MANIFESTACAO", "Apresentar manifestação", WorkItemType.MANIFESTACAO, 3, false, false,
                        List.of(FaseProcessual.CONHECIMENTO, FaseProcessual.INSTRUTORIA, FaseProcessual.EXECUCAO, FaseProcessual.CUMPRIMENTO_SENTENCA, FaseProcessual.RECURSAL),
                        List.of("PECA_PRINCIPAL", "ANEXOS_PROBATORIOS", "HASH_IDEMPOTENTE"),
                        List.of("Manifestação geral, resposta a juntada, petição intermediária e reforço probatório."),
                        List.of("Conteúdo estruturado", "Documentos correlatos", "Assinatura forte recomendada")));
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("APRESENTAR_REPLICA", "Apresentar réplica", WorkItemType.PETICAO, 2, true, false,
                        List.of(FaseProcessual.CONHECIMENTO, FaseProcessual.INSTRUTORIA),
                        List.of("RESPOSTA_A_CONTESTACAO", "DOCUMENTOS_DE_IMPUGNACAO"),
                        List.of("Réplica topológica sem navegação em menus fragmentados."),
                        List.of("Pedir provas", "Impugnar preliminares", "Ratificar tutela")));
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("APRESENTAR_CONTRARRAZOES", "Apresentar contrarrazões", WorkItemType.RECURSO, 2, true, false,
                        List.of(FaseProcessual.RECURSAL),
                        List.of("PECA_RECURSAL", "ENVELOPE_RECURSAL"),
                        List.of("Contrarrazões tratadas como trilha autônoma e auditável."),
                        List.of("Preliminares", "Mérito recursal", "Pedido final")));
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("JUNTAR_MEMORIAIS", "Juntar memoriais", WorkItemType.MANIFESTACAO, 3, false, false,
                        List.of(FaseProcessual.CONHECIMENTO, FaseProcessual.RECURSAL),
                        List.of("MEMORIAIS", "NOTAS_DE_SUSTENTACAO"),
                        List.of("Memoriais com hash e recepção diretamente na unidade correta."),
                        List.of("Síntese fática", "Tese jurídica", "Pedidos")));
            }
            case DEFENSORIA -> {
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("APRESENTAR_DEFESA", "Apresentar defesa", WorkItemType.PETICAO, 1, true, true,
                        List.of(FaseProcessual.CONHECIMENTO, FaseProcessual.RECURSAL, FaseProcessual.AUDIENCIA_CUSTODIA),
                        List.of("DEFESA_TECNICA", "URGENTE_QUANDO_APLICAVEL"),
                        List.of("Fluxo defensivo com priorização por liberdade, saúde e urgência do assistido."),
                        List.of("Narrativa defensiva", "Teses", "Pedidos", "Documentos essenciais")));
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("APRESENTAR_REPLICA_ASSISTIDO", "Apresentar réplica do assistido", WorkItemType.PETICAO, 2, true, true,
                        List.of(FaseProcessual.CONHECIMENTO, FaseProcessual.INSTRUTORIA),
                        List.of("RESPOSTA_ESTRUTURADA", "IMPUGNACAO_A_DOCUMENTOS"),
                        List.of("Réplica ou resposta em favor do assistido com fila própria da defensoria."),
                        List.of("Réplica", "Impugnação", "Provas")));
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("RECURSO_DEFENSIVO", "Interpor ou responder recurso", WorkItemType.RECURSO, 1, true, true,
                        List.of(FaseProcessual.RECURSAL),
                        List.of("RECURSO", "CONTRARRAZOES", "EMBARGOS"),
                        List.of("Trilha recursal defensiva segregada e priorizada."),
                        List.of("Cabimento", "Tempestividade", "Fundamentos")));
            }
            case PROCURADORIA -> {
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("APRESENTAR_INFORMACOES", "Apresentar informações", WorkItemType.MANIFESTACAO, 2, true, false,
                        List.of(FaseProcessual.CONHECIMENTO, FaseProcessual.EXECUCAO, FaseProcessual.CUMPRIMENTO_SENTENCA),
                        List.of("INFORMACOES_INSTITUCIONAIS", "PECA_FAZENDARIA"),
                        List.of("Informações fazendárias, contestação e resposta institucional na mesma malha."),
                        List.of("Síntese da defesa", "Normas internas", "Documentos públicos")));
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("NEGOCIAR_ACORDO_PUBLICO", "Registrar proposta de acordo público", WorkItemType.ACORDO, 2, true, true,
                        List.of(FaseProcessual.CONHECIMENTO, FaseProcessual.EXECUCAO, FaseProcessual.CUMPRIMENTO_SENTENCA),
                        List.of("ACORDO_PUBLICO", "AUTORIZACAO_INTERNA"),
                        List.of("Acordo público com governança e recepção automática pela unidade."),
                        List.of("Minuta", "Autorização", "Parâmetros financeiros")));
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("ATUAR_EMBARGOS_EXECUCAO", "Atuar em embargos ou impugnações", WorkItemType.RECURSO, 1, true, false,
                        List.of(FaseProcessual.EXECUCAO, FaseProcessual.RECURSAL),
                        List.of("IMPUGNACAO", "EMBARGOS", "CONTRARRAZOES"),
                        List.of("Embargos e impugnações entram em fila própria de cobrança pública."),
                        List.of("Cabimento", "Garantia", "Prescrição", "Mérito")));
            }
            case MINISTERIO_PUBLICO -> {
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("EMITIR_PARECER_MP", "Emitir parecer ministerial", WorkItemType.MANIFESTACAO, 1, true, true,
                        List.of(FaseProcessual.CONHECIMENTO, FaseProcessual.RECURSAL),
                        List.of("PARECER", "MANIFESTACAO_MINISTERIAL"),
                        List.of("Parecer ministerial com fila própria e recepção auditável."),
                        List.of("Relatório", "Fundamentação", "Conclusão")));
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("PROMOVER_DILIGENCIA", "Promover diligência", WorkItemType.DILIGENCIA, 2, false, true,
                        List.of(FaseProcessual.CONHECIMENTO, FaseProcessual.PERICIA_TECNICA),
                        List.of("REQUISICAO", "COMPLEMENTACAO_PROBATORIA"),
                        List.of("Promove diligência ou requisição sem abrir outro sistema."),
                        List.of("Objeto", "Órgão destinatário", "Prazo")));
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("RECURSO_MP", "Interpor ou responder recurso", WorkItemType.RECURSO, 1, true, true,
                        List.of(FaseProcessual.RECURSAL),
                        List.of("RECURSO_MP", "CONTRARRAZOES_MP"),
                        List.of("Recursal ministerial com rastreio próprio."),
                        List.of("Cabimento", "Razões", "Pedido")));
            }
            case PERICIA -> {
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("SUBMETER_LAUDO", "Submeter laudo, estudo ou cálculo", WorkItemType.LAUDO, 1, true, true,
                        List.of(FaseProcessual.PERICIA_TECNICA, FaseProcessual.LIQUIDACAO, FaseProcessual.EXECUCAO),
                        List.of("LAUDO_PRINCIPAL", "ANEXOS_TECNICOS"),
                        List.of("Laudo com hash do lote, segregação técnica e recepção topológica."),
                        List.of("Conclusões", "Metodologia", "Quesitos respondidos")));
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("ESCLARECIMENTOS_PERICIAIS", "Prestar esclarecimentos", WorkItemType.MANIFESTACAO, 2, true, true,
                        List.of(FaseProcessual.PERICIA_TECNICA, FaseProcessual.LIQUIDACAO),
                        List.of("ESCLARECIMENTOS", "COMPLEMENTACAO"),
                        List.of("Esclarecimentos técnicos preservam vínculo com o laudo principal."),
                        List.of("Quesitos", "Resposta complementar", "Retificação")));
                ProcessualParticipacaoAtivaSupportUtils.add(out, persona, ProcessualParticipacaoAtivaSupportUtils.action("SOLICITAR_DOCUMENTOS_TECNICOS", "Solicitar complementação documental", WorkItemType.VISTA, 3, false, true,
                        List.of(FaseProcessual.PERICIA_TECNICA),
                        List.of("REQUISICAO_TECNICA"),
                        List.of("Pede documentos ou prontuários sem quebrar a trilha técnica."),
                        List.of("Documento faltante", "Justificativa", "Prazo sugerido")));
            }
            case NAO_SUPORTADA -> {
            }
        }
        return out.stream()
                .filter(action -> action.phases().isEmpty() || action.phases().contains(normalized))
                .map(action -> action.withSignature(signaturePolicy.modeLabel()))
                .sorted(Comparator.comparing(ActionProfile::defaultPriority).thenComparing(ActionProfile::label))
                .toList();
    }

    private PendingView inferByPhase(Processo processo, Persona persona, CapabilityMatrix matrix) {
        ActionProfile top = matrix.actions().isEmpty() ? null : matrix.actions().getFirst();
        if (top == null) {
            return null;
        }
        String headline = switch (persona) {
            case ADVOCACIA_PRIVADA -> "Workspace pronto para manifestação, réplica e trilha recursal sem troca de sistema.";
            case DEFENSORIA -> "Workspace defensivo ativo com prioridade para urgência do assistido e defesa técnica.";
            case PROCURADORIA -> "Workspace fazendário e institucional ativo para informações, acordos e impugnações.";
            case MINISTERIO_PUBLICO -> "Workspace ministerial pronto para parecer, diligência e recurso.";
            case PERICIA -> "Workspace técnico pronto para laudo, esclarecimentos e anexos especializados.";
            case NAO_SUPORTADA -> null;
        };
        if (headline == null) {
            return null;
        }
        return new PendingView(
                "PHASE_SMART_INFERENCE",
                headline,
                top.code(),
                top.label(),
                ProcessualParticipacaoAtivaSupportUtils.safeName(processo.getFaseAtual()),
                top.defaultPriority(),
                false,
                null,
                List.of("INFERENCIA_POR_FASE", "CAPACIDADE_RESOLVIDA", "SEM_MENU_FRAGMENTADO")
        );
    }
}
