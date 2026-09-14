package com.tcc.pjb.backend.service.advogado;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.model.dto.processual.AncoraTerritorial;
import com.tcc.pjb.backend.model.dto.processual.EnderecosProcessuaisRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.guard.DefensoriaInstitutionalCompetenceGuardService;
import com.tcc.pjb.backend.service.processual.legitimidade.OabValidationService;
import com.tcc.pjb.backend.service.processual.numero.NumeroProcessoCnjService;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.PeticaoInicialPdfExportService;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextDocumentSanitizer;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextPlainTextExtractor;
import com.tcc.pjb.backend.service.processual.protocolo.ProtocoloReciboService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class LaianePeticaoInicialDraftService {

    private static final java.util.Set<RitoProcessual> RITOS_PETICIONAMENTO_PESSOAL = java.util.Set.of(
            RitoProcessual.JUIZADO_ESPECIAL_CIVEL,
            RitoProcessual.TRABALHISTA_ORDINARIO,
            RitoProcessual.TRABALHISTA_SUMARISSIMO,
            RitoProcessual.TRABALHISTA_SUMARIO_ALCADA,
            RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO,
            RitoProcessual.TRABALHISTA_CUMPRIMENTO_SENTENCA,
            RitoProcessual.TRABALHISTA_EXECUCAO,
            RitoProcessual.TRABALHISTA_ACIDENTE_TRABALHO,
            RitoProcessual.JUIZADO_ESPECIAL_FEDERAL,
            RitoProcessual.PREVIDENCIARIO_JEF
    );

    private final LaianePeticaoInicialDraftSessionRepository repository;
    private final ProcessoRepository processoRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final LaianePeticaoInicialProtocolarService protocolarService;

    public LaianePeticaoInicialDraftService(LaianePeticaoInicialDraftSessionRepository repository,
                                            ProcessoRepository processoRepository,
                                            CurrentUserService currentUserService,
                                            ObjectMapper objectMapper,
                                            RepresentacaoProcessualPolicyService representacaoProcessualPolicyService,
                                            ObjectProvider<OfficeProcessWorkspaceScopeService> officeProcessWorkspaceScopeServiceProvider,
                                            LaianePeticaoInicialProtocolarService protocolarService) {
        this.repository = Objects.requireNonNull(repository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.representacaoProcessualPolicyService = Objects.requireNonNull(representacaoProcessualPolicyService);
        this.officeProcessWorkspaceScopeService = officeProcessWorkspaceScopeServiceProvider.getIfAvailable();
        this.protocolarService = Objects.requireNonNull(protocolarService);
    }

    @Transactional(readOnly = true)
    public DraftView estruturar(EstruturarRequest request) {
        rejeitarProcessoIdParaPeticionantePessoal(request.processoId());
        Processo processo = resolveProcesso(request.processoId());
        RamoDireito ramoRitoCheck = resolveRamo(request.ramoDireito(), processo);
        RitoProcessual ritoRitoCheck = resolveRito(request.ritoProcessual(), ramoRitoCheck, processo);
        Usuario usuario = requirePeticionante(ritoRitoCheck);
        return computeDraft(request, processo, usuario).view();
    }

    @Transactional
    public DraftView salvar(EstruturarRequest request) {
        rejeitarProcessoIdParaPeticionantePessoal(request.processoId());
        Processo processo = resolveProcesso(request.processoId());
        RamoDireito ramoRitoCheck = resolveRamo(request.ramoDireito(), processo);
        RitoProcessual ritoRitoCheck = resolveRito(request.ritoProcessual(), ramoRitoCheck, processo);
        Usuario usuario = requirePeticionante(ritoRitoCheck);
        DraftComputation draft = computeDraft(request, processo, usuario);

        LaianePeticaoInicialDraftSession entity = new LaianePeticaoInicialDraftSession();
        entity.setSolicitante(usuario);
        entity.setProcesso(processo);
        entity.setTituloCaso(draft.view().tituloCaso());
        entity.setRamoDireito(draft.view().ramoDireito());
        entity.setRitoSugerido(draft.view().ritoSugerido());
        entity.setClasseSugerida(draft.view().classeSugerida());
        entity.setUrgenciaScore(draft.view().urgenciaScore());
        entity.setReadinessScore(draft.view().readinessScore());
        entity.setFatosJson(writeList(draft.view().fatosEstruturados()));
        entity.setPedidosJson(writeList(draft.view().pedidosEstruturados()));
        entity.setFundamentosJson(writeList(draft.view().fundamentosEstruturados()));
        entity.setProvasJson(writeList(draft.view().provasIndicadas()));
        entity.setChecklistJson(writeList(draft.view().checklistDocumental()));
        AncoraTerritorial domicilioAutor = request.enderecos().domicilioAutor();
        AncoraTerritorial domicilioReu = request.enderecos().domicilioReu();
        entity.setUfAutor(domicilioAutor == null ? null : domicilioAutor.uf());
        entity.setComarcaAutor(domicilioAutor == null ? null : domicilioAutor.municipio());
        entity.setUfReu(domicilioReu == null ? null : domicilioReu.uf());
        entity.setComarcaReu(domicilioReu == null ? null : domicilioReu.municipio());
        entity.setEnderecoReuDesconhecido(request.enderecos().domicilioReuDesconhecido());
        entity.setMinutaInicial(draft.view().minutaInicial());
        entity.setHashIntegridade(draft.view().hashIntegridade());
        entity.setStatus("RASCUNHO");
        return toView(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<DraftView> listarMinhas() {
        Usuario usuario = requirePeticionante();
        return repository.findTop100BySolicitante_IdOrderByCreatedAtDesc(usuario.getId()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public DraftView detalhar(Long draftId) {
        Usuario usuario = requirePeticionante();
        LaianePeticaoInicialDraftSession entity = repository.findByIdAndSolicitante_Id(draftId, usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("LaianePeticaoInicialDraftSession", draftId));
        return toView(entity);
    }

    public ProtocolarResult protocolar(Long draftId, ProtocolarRequest request) {
        return protocolarService.protocolar(draftId, request);
    }



    private Usuario requirePeticionante() {
        return requirePeticionante(null);
    }

    private Usuario requirePeticionante(RitoProcessual rito) {
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        boolean profissional = tipo != null && (tipo.isAdvocacia() || tipo.isDefensoriaPublica() || tipo.isProcuradoria() || tipo.isMinisterioPublico());
        if (profissional) {
            return usuario;
        }
        boolean peticionantePessoal = tipo != null && tipo.isPeticionantePessoal()
                && rito != null && RITOS_PETICIONAMENTO_PESSOAL.contains(rito);
        if (peticionantePessoal) {
            return usuario;
        }
        throw new AccessDeniedPjbException("A funcionalidade e exclusiva para advocacia, procuradoria, defensoria e Ministerio Publico");
    }

    private boolean isPeticionantePessoal(Usuario usuario) {
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        return tipo != null && tipo.isPeticionantePessoal();
    }

    private void rejeitarProcessoIdParaPeticionantePessoal(Long processoId) {
        if (processoId == null) {
            return;
        }
        if (isPeticionantePessoal(currentUserService.getRequired())) {
            throw new AccessDeniedPjbException("Peticionamento pessoal nao permite referenciar processo existente");
        }
    }

    private Processo resolveProcesso(Long processoId) {
        if (processoId == null) {
            return null;
        }
        if (officeProcessWorkspaceScopeService != null && officeProcessWorkspaceScopeService.supportsCurrentUser()) {
            officeProcessWorkspaceScopeService.requireAccess(processoId, OfficeActionType.PETICIONAR, currentHttpRequest());
        }
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private HttpServletRequest currentHttpRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        return attrs.getRequest();
    }

    private DraftComputation computeDraft(EstruturarRequest request, Processo processo, Usuario usuario) {
        String petitionText = defaultText(request.textoPeticaoLivre(), null);
        String tituloCaso = defaultText(request.tituloCaso(), inferTitleFromPetition(petitionText, processo != null ? processo.getNumeroProcesso() : null));
        String parteAutora = isPeticionantePessoal(usuario)
                ? usuario.getNome()
                : defaultText(request.parteAutora(), processo != null ? processo.getParteAutoraNome() : null);
        String parteRe = defaultText(request.parteRe(), processo != null ? processo.getParteReuNome() : null);
        RamoDireito ramo = resolveRamo(request.ramoDireito(), processo);
        RitoProcessual rito = resolveRito(request.ritoProcessual(), ramo, processo);
        String ritoNome = rito.name();
        String classe = defaultText(request.classeProcessual(), inferClasseFromPetition(petitionText, processo != null ? processo.getClasseProcessual() : null));
        List<String> fatos = sanitizeList(request.fatos(), mergeFallback(
                extractPetitionSection(petitionText, SectionKind.FACTS),
                processo != null ? listOfNonBlank(
                        defaultText(processo.getObjetoProcessual(), null),
                        defaultText(processo.getResumoIA(), null)
                ) : List.of()
        ));
        List<String> pedidos = sanitizeList(request.pedidos(), mergeFallback(
                extractPetitionSection(petitionText, SectionKind.REQUESTS),
                processo != null ? listOfNonBlank(
                        defaultText(processo.getPedidoPrincipal(), null)
                ) : List.of()
        ));
        List<String> fundamentos = sanitizeList(request.fundamentosJuridicos(), mergeFallback(
                extractPetitionSection(petitionText, SectionKind.LEGAL),
                buildDefaultFundamentos(ramo, request.tutelaUrgencia())
        ));
        List<String> provas = sanitizeList(request.provasIndicadas(), mergeFallback(
                extractPetitionSection(petitionText, SectionKind.EVIDENCE),
                processo != null ? listOfNonBlank(
                        defaultText(processo.getMaterialProbatorioResumo(), null)
                ) : List.of()
        ));
        List<String> checklist = sanitizeList(null, buildChecklist(ramo, rito, processo, usuario, request.tutelaUrgencia(), request.valorCausa(), request.cidadeProtocolo(), request.ufProtocolo(), request.naturezaJuridica()));
        int urgencia = computeUrgencia(request, ramo);
        int readiness = computeReadiness(fatos, pedidos, fundamentos, provas, request.valorCausa());

        String minuta = buildPeticao(tituloCaso, parteAutora, parteRe, usuario, ramo, rito, classe, fatos, fundamentos, pedidos, provas, request.valorCausa(), request.tutelaUrgencia(), request.cidadeFato(), request.ufFato(), request.cidadeProtocolo(), request.ufProtocolo(), request.naturezaJuridica());
        String actorSeed = usuario.getId() == null ? defaultText(usuario.getCpf(), usuario.getEmail()) : usuario.getId().toString();
        String hash = Hashes.sha256Hex(UUID.nameUUIDFromBytes((minuta + "|" + actorSeed).getBytes(StandardCharsets.UTF_8)).toString() + "|" + minuta);

        DraftView view = new DraftView(
                null,
                processo != null ? processo.getId() : null,
                processo != null ? processo.getNumeroProcesso() : null,
                tituloCaso,
                ramo != null ? ramo.name() : null,
                ritoNome,
                classe,
                urgencia,
                readiness,
                fatos,
                pedidos,
                fundamentos,
                provas,
                checklist,
                minuta,
                hash,
                null,
                null
        );
        return new DraftComputation(view);
    }

    private DraftView toView(LaianePeticaoInicialDraftSession entity) {
        return new DraftView(
                entity.getId(),
                entity.getProcesso() != null ? entity.getProcesso().getId() : null,
                entity.getProcesso() != null ? entity.getProcesso().getNumeroProcesso() : null,
                entity.getTituloCaso(),
                entity.getRamoDireito(),
                entity.getRitoSugerido(),
                entity.getClasseSugerida(),
                entity.getUrgenciaScore(),
                entity.getReadinessScore(),
                readList(entity.getFatosJson()),
                readList(entity.getPedidosJson()),
                readList(entity.getFundamentosJson()),
                readList(entity.getProvasJson()),
                readList(entity.getChecklistJson()),
                entity.getMinutaInicial(),
                entity.getHashIntegridade(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private RamoDireito resolveRamo(String ramoRaw, Processo processo) {
        if (ramoRaw != null && !ramoRaw.isBlank()) {
            return RamoDireito.fromString(ramoRaw.trim().toUpperCase(Locale.ROOT));
        }
        if (processo != null && processo.getRamoDireito() != null) {
            return processo.getRamoDireito();
        }
        return RamoDireito.CIVIL;
    }

    private RitoProcessual resolveRito(String ritoRaw, RamoDireito ramo, Processo processo) {
        if (ritoRaw != null && !ritoRaw.isBlank()) {
            return RitoProcessual.tryParse(ritoRaw).orElse(RitoProcessual.COMUM_ORDINARIO);
        }
        if (processo != null && processo.getRito() != null) {
            return processo.getRito();
        }
        if (ramo == null) {
            return RitoProcessual.COMUM_ORDINARIO;
        }
        return switch (ramo) {
            case FAMILIA -> RitoProcessual.CIVIL_FAMILIA_ALIMENTOS;
            case CONSUMIDOR -> RitoProcessual.JUIZADO_ESPECIAL_CIVEL;
            case EMPRESARIAL -> RitoProcessual.RECUPERACAO_JUDICIAL;
            case TRABALHISTA -> RitoProcessual.TRABALHISTA_ORDINARIO;
            case PREVIDENCIARIO -> RitoProcessual.PREVIDENCIARIO_COMUM;
            case PENAL -> RitoProcessual.PROCEDIMENTO_PENAL_COMUM;
            case ELEITORAL -> RitoProcessual.ELEITORAL;
            case MILITAR -> RitoProcessual.MILITAR;
            case ADMINISTRATIVO -> RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO;
            case TRIBUTARIO -> RitoProcessual.TRIBUTARIO_DECLARATORIA;
            case CONSTITUCIONAL -> RitoProcessual.ESPECIAL_MANDADO_SEGURANCA;
            case AMBIENTAL -> RitoProcessual.AMBIENTAL_ACP;
            case INFANCIA_JUVENTUDE -> RitoProcessual.INFANCIA_JUVENTUDE_ECA;
            case AGRARIO -> RitoProcessual.AGRARIO_POSSE_TERRA;
            case INTERNACIONAL -> RitoProcessual.COOPERACAO_JURIDICA_INTERNACIONAL;
            default -> RitoProcessual.COMUM_ORDINARIO;
        };
    }

    private List<String> buildDefaultFundamentos(RamoDireito ramo, boolean tutelaUrgencia) {
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("Competência, legitimidade, interesse processual e adequação da via eleita ao caso concreto.");
        fundamentos.add("Conjunto documental inicial suficiente para conhecimento da pretensão e regular desenvolvimento do processo.");
        if (ramo == null) {
            if (tutelaUrgencia) {
                fundamentos.add("Probabilidade do direito e perigo de dano para apreciação urgente do pedido.");
            }
            return fundamentos;
        }
        switch (ramo) {
            case CIVIL -> fundamentos.add("Aplicação da boa-fé objetiva, da responsabilidade civil e da tutela específica quando cabível.");
            case CONSUMIDOR -> {
                fundamentos.add("Aplicação do CDC, vulnerabilidade do consumidor, boa-fé objetiva e reparação integral.");
                fundamentos.add("Distribuição dinâmica do ônus da prova e tutela efetiva do consumidor quando presentes os requisitos legais.");
            }
            case FAMILIA -> {
                fundamentos.add("Proteção da dignidade familiar, solidariedade, proporcionalidade e melhor interesse da criança ou pessoa vulnerável.");
                fundamentos.add("Necessidade de tutela adequada para guarda, convivência, alimentos, filiação ou sucessão, conforme a causa de pedir.");
            }
            case EMPRESARIAL -> {
                fundamentos.add("Preservação da empresa, segurança jurídica contratual e coerência do regime societário ou recuperacional pertinente.");
                fundamentos.add("Observância do juízo universal, da prevenção empresarial e dos documentos societários essenciais quando o caso exigir.");
            }
            case PENAL -> {
                fundamentos.add("Respeito ao devido processo legal, contraditório, ampla defesa, legalidade estrita e individualização da providência criminal postulada.");
                fundamentos.add("Necessidade de demonstração mínima de autoria, materialidade e adequação da medida penal requerida.");
            }
            case MILITAR -> {
                fundamentos.add("Adequação da via eleita ao regime militar, com observância da hierarquia, disciplina e competência especializada.");
                fundamentos.add("Compatibilização entre a peça inicial e o regime penal ou administrativo militar efetivamente incidente.");
            }
            case ELEITORAL -> {
                fundamentos.add("Observância da legitimidade ativa, do calendário eleitoral, da tempestividade e da competência da zona ou tribunal eleitoral.");
                fundamentos.add("Adequação do pedido ao rito eleitoral específico, com foco na preservação da normalidade e legitimidade do pleito.");
            }
            case ADMINISTRATIVO -> {
                fundamentos.add("Controle de legalidade do ato administrativo, motivação, proporcionalidade, devido processo administrativo e tutela jurisdicional efetiva.");
                fundamentos.add("Necessidade de confrontar o ato impugnado com o processo administrativo e a documentação pré-constituída disponível.");
            }
            case TRIBUTARIO -> {
                fundamentos.add("Legalidade tributária, devido processo fiscal, exigibilidade do crédito e adequação do rito tributário ou executivo correspondente.");
                fundamentos.add("Coerência entre a natureza do crédito, a inscrição, a CDA ou o lançamento e a tutela jurisdicional requerida.");
            }
            case CONSTITUCIONAL -> {
                fundamentos.add("Supremacia constitucional, tutela mandamental ou concentrada, prova pré-constituída e adequação da via constitucional eleita.");
                fundamentos.add("Necessidade de demonstrar violação concreta de direito ou preceito constitucional e competência do órgão jurisdicional apropriado.");
            }
            case AMBIENTAL -> {
                fundamentos.add("Prevenção, precaução, reparação integral do dano ambiental e tutela específica para cessação ou mitigação do ilícito.");
                fundamentos.add("Necessidade de compatibilizar prova técnica, nexo ambiental e definição do polo passivo responsável pelo dano ou risco.");
            }
            case TRABALHISTA -> {
                fundamentos.add("Proteção ao crédito trabalhista, primazia da realidade, indisponibilidade relativa de direitos laborais e efetividade do processo do trabalho.");
                fundamentos.add("Coerência entre contrato, jornada, verbas postuladas e meios de prova laborais indicados na inicial.");
            }
            case PREVIDENCIARIO -> {
                fundamentos.add("Proteção social, legislação previdenciária aplicável, interesse de agir e confronto entre prova administrativa e situação fática do segurado.");
                fundamentos.add("Adequação entre benefício, qualidade de segurado, carência, incapacidade ou requisito legal específico discutido na causa.");
            }
            case INFANCIA_JUVENTUDE -> {
                fundamentos.add("Proteção integral, prioridade absoluta, escuta qualificada e melhor interesse da criança e do adolescente.");
                fundamentos.add("Necessidade de atuação coordenada com rede protetiva, Ministério Público e equipe técnica quando o caso exigir.");
            }
            case AGRARIO -> {
                fundamentos.add("Função social da propriedade, posse, uso da terra, prevenção de conflito fundiário e adequação da tutela possessória ou agrária.");
                fundamentos.add("Necessidade de identificar corretamente imóvel, cadeia dominial, ocupação e prova técnica ou documental agrária essencial.");
            }
            case INTERNACIONAL -> {
                fundamentos.add("Elemento estrangeiro relevante, cooperação jurídica internacional, competência transnacional e observância das formalidades documentais cabíveis.");
                fundamentos.add("Necessidade de compatibilizar o pedido com carta rogatória, homologação, cooperação direta ou autoridade central, quando aplicável.");
            }
            default -> fundamentos.add("Adequar fundamentos jurídicos ao sub-ramo material e ao rito efetivamente selecionado, preservando coerência normativa e probatória.");
        }
        if (tutelaUrgencia) {
            fundamentos.add("Probabilidade do direito e perigo de dano para tutela de urgência ou providência liminar requerida.");
        }
        return fundamentos.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
    }

    private List<String> buildChecklist(RamoDireito ramo, RitoProcessual rito, Processo processo, Usuario usuario, boolean tutelaUrgencia, BigDecimal valorCausa, String cidadeProtocolo, String ufProtocolo, String naturezaJuridica) {
        ArrayList<String> checklist = new ArrayList<>();
        var politicaRepresentacao = representacaoProcessualPolicyService.resolve(
                processo != null && processo.getRamoDireito() != null ? processo.getRamoDireito().name() : ramo == null ? null : ramo.name(),
                processo != null && processo.getRito() != null ? processo.getRito().name() : rito == null ? null : rito.name(),
                processo != null ? processo.getTribunal() : null,
                usuario == null ? null : usuario.getTipoUsuario(),
                null,
                null,
                null,
                false,
                false,
                null,
                null
        );
        checklist.addAll(politicaRepresentacao.documentosBase());
        checklist.add("Documento principal da petição inicial ou peça-base consolidada no editor do PJB.");
        checklist.add("Documentos de qualificação da parte autora e instrumento de representação compatível com a atuação declarada.");
        checklist.add("Provas documentais essenciais relacionadas aos fatos narrados.");
        checklist.add("Comprovante de endereço, domicílio ou vínculo territorial para apoio à competência e citação.");
        if (valorCausa != null) {
            checklist.add("Memória mínima do valor da causa informado e aderência econômica ao rito escolhido.");
        }
        if (cidadeProtocolo != null && ufProtocolo != null) {
            checklist.add("Compatibilizar foro sugerido com a cidade de protocolo " + cidadeProtocolo + "/" + ufProtocolo + " e a unidade destinatária correta.");
        }
        if (naturezaJuridica != null && !naturezaJuridica.isBlank()) {
            checklist.add("Conferir aderência da natureza jurídica indicada à classe, ao rito e à competência sugeridos.");
        }
        if (ramo != null) {
            switch (ramo) {
                case CIVIL, CONSUMIDOR -> checklist.add("Conferir relação contratual, dano, inadimplemento, comunicação prévia e prova documental cível/consumerista pertinente.");
                case FAMILIA -> checklist.add("Documentos de vínculo familiar, alimentos, guarda, convivência, dependência ou sucessão, conforme a pretensão.");
                case EMPRESARIAL -> {
                    checklist.add("Contrato social, alterações, documentos societários, procurações e peças empresariais essenciais.");
                    checklist.add("Validar eventual prevenção empresarial ou juízo universal quando houver recuperação ou falência correlata.");
                }
                case PENAL -> checklist.add("Boletim, auto, laudo, peças informativas, prova de materialidade e elementos mínimos de autoria ou urgência criminal.");
                case MILITAR -> checklist.add("Sindicância, IPM, assentamentos, ato disciplinar ou documentação funcional militar relacionada ao caso.");
                case ELEITORAL -> checklist.add("Documentação do pleito, zona eleitoral, propaganda, candidatura, contas ou prova pré-constituída do ilícito eleitoral alegado.");
                case ADMINISTRATIVO -> checklist.add("Ato administrativo, processo administrativo, intimações, pareceres e prova pré-constituída sobre a ilegalidade apontada.");
                case TRIBUTARIO -> checklist.add("CDA, lançamento, processo fiscal, comprovantes de pagamento, parcelamento ou documentação tributária pertinente.");
                case CONSTITUCIONAL -> checklist.add("Prova pré-constituída e adequação estrita da via constitucional escolhida, especialmente em trilhas mandamentais.");
                case AMBIENTAL -> checklist.add("Autos, laudos, licenças, mapas, coordenadas, imagens e prova técnica do dano ou risco ambiental narrado.");
                case TRABALHISTA -> {
                    checklist.add("CTPS, contracheques, cartões de ponto, mensagens, TRCT e demais documentos laborais pertinentes.");
                    if (politicaRepresentacao.admiteJusPostulandi()) {
                        checklist.add("Registrar ciência das limitações recursais do jus postulandi e da necessidade de regularização quando a causa avançar para instância incompatível.");
                    }
                }
                case PREVIDENCIARIO -> checklist.add("CNIS, cartas de indeferimento, laudos, documentos médicos, provas contributivas e documentos do benefício discutido.");
                case INFANCIA_JUVENTUDE -> checklist.add("Documentos da criança ou adolescente, atos da rede protetiva, relatórios técnicos e elementos de risco ou proteção imediata.");
                case AGRARIO -> checklist.add("Matrícula, CAR, georreferenciamento, contratos, documentos possessórios e prova do conflito fundiário ou agrário.");
                case INTERNACIONAL -> checklist.add("Documentos estrangeiros, traduções, apostilas, indicação de autoridade central ou peça de cooperação internacional pertinente.");
                default -> checklist.add("Revisar aderência documental ao sub-ramo identificado, ao rito selecionado e à estratégia probatória do caso.");
            }
        }
        if (rito != null) {
            if (rito.isEspecialConstitucional()) {
                checklist.add("Checar prova pré-constituída, legitimidade e aderência estrita ao rito especial ou constitucional selecionado.");
            }
            if (rito.isPenal()) {
                checklist.add("Conferir unidade criminal especializada, plantão, custódia, júri ou execução penal, conforme o rito escolhido.");
            }
            if (rito.isTribFazenda()) {
                checklist.add("Conferir prevenção fazendária, unidade especializada, dívida, benefício ou ato estatal correlato ao rito fazendário/tributário.");
            }
            if (rito.isTrabalhista()) {
                checklist.add("Conferir aderência do pedido ao rito ordinário, sumaríssimo, execução ou fase trabalhista correspondente.");
            }
            if (rito.isEleitoral()) {
                checklist.add("Conferir tempestividade eleitoral e competência da zona ou tribunal eleitoral correspondente.");
            }
            if (rito.isMilitar()) {
                checklist.add("Conferir competência da Justiça Militar estadual ou federal e a unidade militar vinculada ao caso.");
            }
            if (rito.isAmbiental()) {
                checklist.add("Validar correspondência entre dano ambiental, órgão competente e providência inibitória/reparatória pretendida.");
            }
            if (rito.isInfancia()) {
                checklist.add("Verificar necessidade de sigilo, prioridade e atuação interinstitucional da rede de proteção.");
            }
            if (rito.isAgrario()) {
                checklist.add("Conferir imóvel, área, posse e eventual conflito coletivo com potencial necessidade de audiência ou mediação específica.");
            }
        }
        if (tutelaUrgencia) {
            checklist.add("Documentos que demonstrem risco de dano, urgência concreta e necessidade de providência imediata.");
        }
        return checklist.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
    }

    private int computeUrgencia(EstruturarRequest request, RamoDireito ramo) {
        int score = 25;
        if (request.tutelaUrgencia()) {
            score += 35;
        }
        if (ramo == null) {
            return Math.min(score, 100);
        }
        if (ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            score += 20;
        }
        if (ramo == RamoDireito.TRABALHISTA || ramo == RamoDireito.PREVIDENCIARIO) {
            score += 12;
        }
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            score += 18;
        }
        if (ramo == RamoDireito.ELEITORAL || ramo == RamoDireito.AMBIENTAL || ramo == RamoDireito.CONSTITUCIONAL) {
            score += 10;
        }
        if (ramo == RamoDireito.AGRARIO || ramo == RamoDireito.INTERNACIONAL) {
            score += 8;
        }
        return Math.min(score, 100);
    }

    private int computeReadiness(List<String> fatos, List<String> pedidos, List<String> fundamentos, List<String> provas, BigDecimal valorCausa) {
        int score = 10;
        score += Math.min(25, fatos.size() * 6);
        score += Math.min(25, pedidos.size() * 8);
        score += Math.min(20, fundamentos.size() * 5);
        score += Math.min(20, provas.size() * 5);
        if (valorCausa != null && valorCausa.compareTo(BigDecimal.ZERO) > 0) {
            score += 10;
        }
        return Math.min(score, 100);
    }


    private String buildPeticao(String tituloCaso,
                                String parteAutora,
                                String parteRe,
                                Usuario usuario,
                                RamoDireito ramo,
                                RitoProcessual rito,
                                String classe,
                                List<String> fatos,
                                List<String> fundamentos,
                                List<String> pedidos,
                                List<String> provas,
                                BigDecimal valorCausa,
                                boolean tutelaUrgencia,
                                String cidadeFato,
                                String ufFato,
                                String cidadeProtocolo,
                                String ufProtocolo,
                                String naturezaJuridica) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildEnderecoCabecalho(ramo, rito)).append("\n\n");
        if (classe != null && !classe.isBlank()) {
            sb.append("Classe sugerida: ").append(classe).append("\n");
        }
        if (ramo != null) {
            sb.append("Ramo: ").append(ramo.getDescricao()).append(" | Rito sugerido: ").append(rito.name()).append("\n");
        }
        if (naturezaJuridica != null && !naturezaJuridica.isBlank()) {
            sb.append("Natureza juridica sugerida: ").append(naturezaJuridica).append("\n");
        }
        if (cidadeProtocolo != null && ufProtocolo != null) {
            sb.append("Foro sugerido: ").append(cidadeProtocolo).append('/').append(ufProtocolo).append("\n");
        }
        if (cidadeFato != null && ufFato != null) {
            sb.append("Local do fato informado: ").append(cidadeFato).append('/').append(ufFato).append("\n");
        }
        sb.append("\n");
        sb.append(defaultText(parteAutora, "PARTE AUTORA"))
                .append(", por intermédio de ")
                .append(usuario.getNome())
                .append(", apresenta ")
                .append(resolveVerboInicial(ramo))
                .append("\n");
        sb.append(defaultText(tituloCaso, "PETICAO INICIAL")).append("\n");
        sb.append("em face de ").append(defaultText(parteRe, "PARTE RE")).append(".\n\n");
        sb.append("I - ").append(resolveSecaoFatos(ramo)).append("\n");
        appendEnumerated(sb, fatos);
        sb.append("\nII - ").append(resolveSecaoFundamentos(ramo)).append("\n");
        appendEnumerated(sb, fundamentos);
        if (tutelaUrgencia) {
            sb.append("\nIII - ").append(resolveSecaoUrgencia(ramo)).append("\n");
            sb.append(resolveTextoUrgencia(ramo)).append("\n");
        }
        sb.append("\n").append(tutelaUrgencia ? "IV" : "III").append(" - ").append(resolveSecaoPedidos(ramo)).append("\n");
        appendEnumerated(sb, pedidos);
        sb.append("\n").append(tutelaUrgencia ? "V" : "IV").append(" - DAS PROVAS E DOCUMENTOS\n");
        appendEnumerated(sb, provas);
        if (valorCausa != null && shouldShowValorCausa(ramo)) {
            sb.append("\nValor da causa: R$ ").append(valorCausa).append(".\n");
        }
        sb.append("\n").append(resolveFecho(ramo)).append("\n");
        return sb.toString();
    }

    private String buildEnderecoCabecalho(RamoDireito ramo, RitoProcessual rito) {
        if (rito != null && rito.isEleitoral()) {
            return "EXCELENTISSIMO(A) SENHOR(A) DOUTOR(A) JUIZ(A) ELEITORAL DO JUIZO COMPETENTE";
        }
        if (rito != null && rito.isTrabalhista()) {
            return "EXCELENTISSIMO(A) SENHOR(A) DOUTOR(A) JUIZ(A) DO TRABALHO DO JUIZO COMPETENTE";
        }
        if (rito != null && rito.isMilitar()) {
            return "EXCELENTISSIMO(A) SENHOR(A) DOUTOR(A) JUIZ(A) DA JUSTICA MILITAR COMPETENTE";
        }
        if (rito != null && rito.isPenal()) {
            return "EXCELENTISSIMO(A) SENHOR(A) DOUTOR(A) JUIZ(A) DE DIREITO DO JUIZO CRIMINAL COMPETENTE";
        }
        if (ramo == RamoDireito.PREVIDENCIARIO || ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.CONSTITUCIONAL || ramo == RamoDireito.INTERNACIONAL) {
            return "EXCELENTISSIMO(A) SENHOR(A) DOUTOR(A) JUIZ(A) DO JUIZO COMPETENTE DA JUSTICA PUBLICA/FEDERAL";
        }
        return "EXCELENTISSIMO(A) SENHOR(A) DOUTOR(A) JUIZ(A) DE DIREITO DO JUIZO COMPETENTE";
    }

    private String resolveVerboInicial(RamoDireito ramo) {
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR || ramo == RamoDireito.ELEITORAL) {
            return "a presente peça inicial";
        }
        return "a presente petição inicial";
    }

    private String resolveSecaoFatos(RamoDireito ramo) {
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            return "DA NARRATIVA FÁTICA E DA CONTEXTUALIZAÇÃO DO CASO";
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            return "DO CONTRATO E DA REALIDADE FÁTICA";
        }
        return "DOS FATOS";
    }

    private String resolveSecaoFundamentos(RamoDireito ramo) {
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            return "DO ENQUADRAMENTO JURÍDICO E DA MEDIDA POSTULADA";
        }
        if (ramo == RamoDireito.ELEITORAL) {
            return "DOS FUNDAMENTOS ELEITORAIS";
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            return "DOS FUNDAMENTOS TRABALHISTAS";
        }
        if (ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.PREVIDENCIARIO || ramo == RamoDireito.CONSTITUCIONAL) {
            return "DOS FUNDAMENTOS JURÍDICOS E DA ILEGALIDADE APONTADA";
        }
        return "DOS FUNDAMENTOS JURÍDICOS";
    }

    private String resolveSecaoUrgencia(RamoDireito ramo) {
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            return "DA PROVIDÊNCIA URGENTE REQUERIDA";
        }
        if (ramo == RamoDireito.AMBIENTAL) {
            return "DA TUTELA INIBITÓRIA E DA URGÊNCIA AMBIENTAL";
        }
        return "DA TUTELA DE URGÊNCIA";
    }

    private String resolveTextoUrgencia(RamoDireito ramo) {
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            return "Diante da urgência concreta e do risco processual identificado, requer-se apreciação imediata da medida postulada.";
        }
        if (ramo == RamoDireito.AMBIENTAL) {
            return "Diante do risco de continuidade do dano e da necessidade de cessação imediata do ilícito, requer-se tutela ambiental urgente.";
        }
        return "Diante da probabilidade do direito e do risco de dano, requer-se apreciação urgente da tutela postulada.";
    }

    private String resolveSecaoPedidos(RamoDireito ramo) {
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            return "DAS PROVIDÊNCIAS REQUERIDAS";
        }
        if (ramo == RamoDireito.ELEITORAL) {
            return "DOS PEDIDOS E DAS PROVIDÊNCIAS ELEITORAIS";
        }
        return "DOS PEDIDOS";
    }

    private boolean shouldShowValorCausa(RamoDireito ramo) {
        return ramo != RamoDireito.PENAL && ramo != RamoDireito.MILITAR && ramo != RamoDireito.ELEITORAL;
    }

    private String resolveFecho(RamoDireito ramo) {
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            return "Nesses termos, requer o regular processamento da medida, com apreciação imediata do pedido, quando cabível.";
        }
        if (ramo == RamoDireito.ELEITORAL) {
            return "Nesses termos, requer o regular processamento do feito eleitoral, com prioridade compatível com o calendário e a urgência do caso.";
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            return "Termos em que, requer o regular processamento da reclamação, com designação de audiência e apreciação dos pedidos formulados.";
        }
        return "Termos em que, pede deferimento.";
    }

    private String inferTitleFromPetition(String petitionText, String fallback) {
        String normalized = trimToNull(petitionText);
        if (normalized == null) {
            return defaultText(null, fallback);
        }
        for (String line : normalized.split("\\R")) {
            String trimmed = trimToNull(line);
            if (trimmed != null && trimmed.length() > 8) {
                return trimmed.length() > 160 ? trimmed.substring(0, 160) : trimmed;
            }
        }
        return defaultText(null, fallback);
    }

    private String inferClasseFromPetition(String petitionText, String fallback) {
        String normalized = trimToNull(petitionText);
        if (normalized == null) {
            return defaultText(null, fallback);
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.contains("MANDADO DE SEGURANCA")) {
            return "MANDADO DE SEGURANCA";
        }
        if (upper.contains("HABEAS CORPUS")) {
            return "HABEAS CORPUS";
        }
        if (upper.contains("EXECUCAO FISCAL")) {
            return "EXECUCAO FISCAL";
        }
        if (upper.contains("RECLAMACAO TRABALHISTA")) {
            return "RECLAMACAO TRABALHISTA";
        }
        if (upper.contains("ACAO CIVIL PUBLICA")) {
            return "ACAO CIVIL PUBLICA";
        }
        return defaultText(null, fallback);
    }

    private List<String> mergeFallback(List<String> primary, List<String> fallback) {
        return primary != null && !primary.isEmpty() ? primary : fallback;
    }

    private List<String> listOfNonBlank(String... values) {
        ArrayList<String> out = new ArrayList<>();
        if (values == null || values.length == 0) {
            return List.of();
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private List<String> extractPetitionSection(String petitionText, SectionKind target) {
        String normalized = trimToNull(petitionText);
        if (normalized == null) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        SectionKind current = null;
        for (String rawLine : normalized.split("\\R")) {
            String line = trimToNull(rawLine);
            if (line == null) {
                continue;
            }
            SectionKind detected = detectSection(line);
            if (detected != null) {
                current = detected;
                continue;
            }
            if (current == target) {
                String cleaned = line.replaceFirst("^[\\-•*\\d\\.)\\s]+", "").trim();
                if (!cleaned.isBlank()) {
                    out.add(cleaned);
                }
            }
        }
        return out.stream().distinct().toList();
    }

    private SectionKind detectSection(String line) {
        String normalized = line.toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.contains("DOS FATOS") || normalized.contains("DO FATO") || normalized.contains("EXPOSICAO DOS FATOS")) {
            return SectionKind.FACTS;
        }
        if (normalized.contains("DO DIREITO") || normalized.contains("FUNDAMENTOS JURIDICOS") || normalized.contains("DOS FUNDAMENTOS")) {
            return SectionKind.LEGAL;
        }
        if (normalized.contains("DOS PEDIDOS") || normalized.contains("REQUERIMENTOS") || normalized.equals("PEDIDOS")) {
            return SectionKind.REQUESTS;
        }
        if (normalized.contains("DAS PROVAS") || normalized.contains("MEIOS DE PROVA") || normalized.equals("PROVAS")) {
            return SectionKind.EVIDENCE;
        }
        return null;
    }

    private void appendEnumerated(StringBuilder sb, List<String> items) {
        if (items.isEmpty()) {
            sb.append("1. Complementar campo obrigatorio.\n");
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            sb.append(i + 1).append(". ").append(items.get(i)).append("\n");
        }
    }

    private List<String> sanitizeList(List<String> direct, List<String> fallback) {
        List<String> source = direct != null && !direct.isEmpty() ? direct : fallback;
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .distinct()
                .toList();
    }

    private String defaultText(String preferred, String fallback) {
        String first = preferred == null ? null : preferred.trim();
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (fallback == null) {
            return null;
        }
        String second = fallback.trim();
        return second.isBlank() ? null : second;
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar elementos do draft inicial", e);
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readerForListOf(String.class).readValue(json);
        } catch (Exception e) {
            return List.of();
        }
    }

    private enum SectionKind {
        FACTS,
        LEGAL,
        REQUESTS,
        EVIDENCE
    }

    private record DraftComputation(DraftView view) {
    }

    private static List<String> sanitizeRecordList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(LaianePeticaoInicialDraftService::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record EstruturarRequest(
            Long processoId,
            @NotBlank String tituloCaso,
            String parteAutora,
            String parteRe,
            String ramoDireito,
            String ritoProcessual,
            String classeProcessual,
            List<String> fatos,
            List<String> fundamentosJuridicos,
            List<String> pedidos,
            List<String> provasIndicadas,
            BigDecimal valorCausa,
            boolean tutelaUrgencia,
            String textoPeticaoLivre,
            String cidadeFato,
            String ufFato,
            String cidadeProtocolo,
            String ufProtocolo,
            String naturezaJuridica,
            EnderecosProcessuaisRequest enderecos
    ) {
        public EstruturarRequest {
            tituloCaso = trimToNull(tituloCaso);
            parteAutora = trimToNull(parteAutora);
            parteRe = trimToNull(parteRe);
            ramoDireito = trimToNull(ramoDireito);
            ritoProcessual = trimToNull(ritoProcessual);
            classeProcessual = trimToNull(classeProcessual);
            fatos = sanitizeRecordList(fatos);
            fundamentosJuridicos = sanitizeRecordList(fundamentosJuridicos);
            pedidos = sanitizeRecordList(pedidos);
            provasIndicadas = sanitizeRecordList(provasIndicadas);
            textoPeticaoLivre = trimToNull(textoPeticaoLivre);
            cidadeFato = trimToNull(cidadeFato);
            ufFato = trimToNull(ufFato);
            cidadeProtocolo = trimToNull(cidadeProtocolo);
            ufProtocolo = trimToNull(ufProtocolo);
            naturezaJuridica = trimToNull(naturezaJuridica);
            enderecos = enderecos == null ? EnderecosProcessuaisRequest.vazio() : enderecos;
        }
    }

    public record ProtocolarRequest(
            String tipoJustica,
            com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoParteProcessual tipoPartePrincipal,
            java.util.Set<com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoCondicaoRequisito> condicoesAplicaveis,
            java.util.List<com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento> documentosAnexados
    ) {
        public ProtocolarRequest(String tipoJustica) {
            this(tipoJustica, null, java.util.Set.of(), java.util.List.of());
        }
    }

    public record ProtocolarResult(
            Long draftId,
            Long processoId,
            String numeroProcesso,
            String status,
            Instant protocoladoEm,
            String hashIntegridade,
            String connectorReference
    ) {
    }

    public record DraftView(
            Long id,
            Long processoId,
            String processoNumero,
            String tituloCaso,
            String ramoDireito,
            String ritoSugerido,
            String classeSugerida,
            Integer urgenciaScore,
            Integer readinessScore,
            List<String> fatosEstruturados,
            List<String> pedidosEstruturados,
            List<String> fundamentosEstruturados,
            List<String> provasIndicadas,
            List<String> checklistDocumental,
            String minutaInicial,
            String hashIntegridade,
            Instant createdAt,
            Instant updatedAt
    ) {
        public DraftView {
            processoNumero = trimToNull(processoNumero);
            tituloCaso = trimToNull(tituloCaso);
            ramoDireito = trimToNull(ramoDireito);
            ritoSugerido = trimToNull(ritoSugerido);
            classeSugerida = trimToNull(classeSugerida);
            fatosEstruturados = sanitizeRecordList(fatosEstruturados);
            pedidosEstruturados = sanitizeRecordList(pedidosEstruturados);
            fundamentosEstruturados = sanitizeRecordList(fundamentosEstruturados);
            provasIndicadas = sanitizeRecordList(provasIndicadas);
            checklistDocumental = sanitizeRecordList(checklistDocumental);
            minutaInicial = trimToNull(minutaInicial);
            hashIntegridade = trimToNull(hashIntegridade);
        }
    }
}
