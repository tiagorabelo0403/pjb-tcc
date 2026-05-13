package com.tcc.pjb.backend.service.intelligence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoFundamento;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoGovernanceMetricas;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;

@Service
public class PessoaLocalizacaoService {

    public enum CanalConsulta {
        OFICIAL_JUSTICA,
        DELEGADO,
        MAGISTRATURA
    }

    public interface EnderecoPessoaRegistryClient {
        SnapshotEndereco consultarPorCpf(String cpf, Usuario executor, PessoaLocalizacaoRequest request, Processo processoContexto);
    }

    public interface RestricaoPessoaRegistryClient {
        SnapshotRestricao consultarPorCpf(String cpf, Usuario executor, PessoaLocalizacaoRequest request, Processo processoContexto);
    }

    public interface MandadoPessoaRegistryClient {
        SnapshotMandado consultarPorCpf(String cpf, Usuario executor, PessoaLocalizacaoRequest request, Processo processoContexto);
    }

    public record EnderecoInfo(
            String fonte,
            String tipo,
            String descricao,
            String bairro,
            String cidade,
            String uf,
            String cep,
            boolean principal,
            boolean parcial,
            double confianca,
            Instant atualizadoEm
    ) {
    }

    public record RestricaoInfo(
            String sistema,
            String tipo,
            String situacao,
            String referencia,
            String descricao,
            Instant atualizadoEm
    ) {
    }

    public record MandadoInfo(
            String sistema,
            String tipo,
            String situacao,
            String referencia,
            String descricao,
            Instant atualizadoEm
    ) {
    }

    public record SnapshotEndereco(boolean enabled, boolean realtime, List<EnderecoInfo> enderecos, List<String> highlights) {
        public SnapshotEndereco {
            enderecos = enderecos == null ? List.of() : List.copyOf(enderecos);
            highlights = highlights == null ? List.of() : List.copyOf(highlights);
        }
    }

    public record SnapshotRestricao(boolean enabled, boolean realtime, List<RestricaoInfo> restricoes, List<String> highlights) {
        public SnapshotRestricao {
            restricoes = restricoes == null ? List.of() : List.copyOf(restricoes);
            highlights = highlights == null ? List.of() : List.copyOf(highlights);
        }
    }

    public record SnapshotMandado(boolean enabled, boolean realtime, List<MandadoInfo> mandados, List<String> highlights) {
        public SnapshotMandado {
            mandados = mandados == null ? List.of() : List.copyOf(mandados);
            highlights = highlights == null ? List.of() : List.copyOf(highlights);
        }
    }

    private final CurrentUserService currentUserService;
    private final DocumentoNacionalValidator documentoValidator;
    private final UsuarioRepository usuarioRepository;
    private final ProcessoRepository processoRepository;
    private final ObjectProvider<ProntuarioNacionalService> prontuarioNacionalService;
    private final AuditLedgerService auditLedgerService;
    private final PjbAuthorizationService authorizationService;
    private final PessoaLocalizacaoProviderRegistry providerRegistry;
    private final PessoaLocalizacaoGovernanceService governanceService;
    private final PessoaLocalizacaoIntelligenceSummaryService summaryService;

    public PessoaLocalizacaoService(CurrentUserService currentUserService,
                                    DocumentoNacionalValidator documentoValidator,
                                    UsuarioRepository usuarioRepository,
                                    ProcessoRepository processoRepository,
                                    ObjectProvider<ProntuarioNacionalService> prontuarioNacionalService,
                                    AuditLedgerService auditLedgerService,
                                    PjbAuthorizationService authorizationService,
                                    PessoaLocalizacaoProviderRegistry providerRegistry,
                                    PessoaLocalizacaoGovernanceService governanceService,
                                    PessoaLocalizacaoIntelligenceSummaryService summaryService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.documentoValidator = Objects.requireNonNull(documentoValidator);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.prontuarioNacionalService = Objects.requireNonNull(prontuarioNacionalService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.providerRegistry = Objects.requireNonNull(providerRegistry);
        this.governanceService = Objects.requireNonNull(governanceService);
        this.summaryService = Objects.requireNonNull(summaryService);
    }

    @Transactional(readOnly = true)
    public PessoaLocalizacaoResponse localizar(PessoaLocalizacaoRequest request, CanalConsulta canal) {
        Usuario executor = currentUserService.getRequired();
        String cpf = normalizeCpf(request.cpf());
        Processo processoContexto = resolveProcessoContexto(request.processoId());
        boolean possuiContextoFormal = processoContexto != null || request.mandadoId() != null;
        PessoaLocalizacaoFundamento fundamento = resolveFundamento(request, canal);
        enforceAccess(executor, canal, possuiContextoFormal, request, fundamento);

        Usuario cadastroInterno = usuarioRepository.findByCpf(cpf).orElse(null);
        List<Processo> processosRelacionados = processoRepository.findAllByPartesCpf(cpf).stream()
                .filter(processo -> authorizationService.canReadProcesso(processo).allowed())
                .toList();
        boolean enderecoEstritoElegivel = authorizationService.canAccessEnderecoEstrito(executor, possuiContextoFormal) && fundamento.permiteEnderecoEstrito();
        boolean enderecoEstritoLiberado = enderecoEstritoElegivel && (request.exigirEnderecoEstrito() || possuiContextoFormal);

        SnapshotEndereco snapshotEndereco = providerRegistry.consultarEnderecos(cpf, executor, request, processoContexto);
        SnapshotRestricao snapshotRestricao = request.incluirRestricoes() && authorizationService.canConsultarInteligenciaPatrimonial(executor)
                ? providerRegistry.consultarRestricoes(cpf, executor, request, processoContexto)
                : new SnapshotRestricao(false, false, List.of(), List.of("Consulta de restrições não liberada para o perfil executor."));
        SnapshotMandado snapshotMandado = request.incluirMandados() && authorizationService.canConsultarMandadosPessoa(executor)
                ? providerRegistry.consultarMandados(cpf, executor, request, processoContexto)
                : new SnapshotMandado(false, false, List.of(), List.of("Consulta de mandados por pessoa não liberada para o perfil executor."));

        ProntuarioNacionalService.ProntuarioNacionalView prontuario = request.incluirProntuario() && authorizationService.canLocatePessoaByCpf(executor)
                ? consultarProntuarioSilencioso(cpf)
                : null;

        String scope = buildScope(executor, canal);
        String finalidade = normalizeFinalidade(request.finalidade(), canal, fundamento);
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (!possuiContextoFormal) {
            alertas.add("Consulta sem processo ou mandado vinculado exige justificativa institucional consistente.");
        }
        if (!enderecoEstritoLiberado) {
            alertas.add("Endereço estrito não liberado; retorno reduzido a sinais de localização e referências institucionais.");
        }
        if (snapshotEndereco.enderecos().isEmpty()) {
            alertas.add("Nenhum endereço materializado por fonte externa nesta execução.");
        }
        if (snapshotRestricao.enabled() && !snapshotRestricao.restricoes().isEmpty()) {
            alertas.add("Há restrições complementares que podem impactar mandado, penhora, apreensão ou decisão executiva.");
        }
        if (snapshotMandado.enabled() && !snapshotMandado.mandados().isEmpty()) {
            alertas.add("Existe sinal externo de mandado associado ao CPF consultado.");
        }
        if (request.exigirEnderecoEstrito() && !fundamento.permiteEnderecoEstrito()) {
            alertas.add("O fundamento informado não autoriza exposição de endereço estrito; retorno mantido em nível minimizado.");
        }

        List<PessoaLocalizacaoResponse.VinculoProcessualResumo> vinculos = collectVinculos(processosRelacionados, prontuario);
        List<PessoaLocalizacaoResponse.RestricaoResumo> restricoes = collectRestricoes(snapshotRestricao, snapshotMandado);
        List<PessoaLocalizacaoResponse.FonteConsultaResumo> fontes = List.of(
                toFonte("CADASTRO_INTERNO", cadastroInterno != null, false, cadastroInterno != null ? 1 : 0, cadastroInterno != null ? List.of("CPF localizado em cadastro interno do PJB.") : List.of("CPF não vinculado diretamente a usuário interno.")),
                toFonte("PRONTUARIO_NACIONAL", prontuario != null, false, prontuario != null ? prontuario.entradas().size() : 0, prontuarioHighlights(prontuario)),
                toFonte("LOCALIZADOR_ENDERECO", snapshotEndereco.enabled(), snapshotEndereco.realtime(), snapshotEndereco.enderecos().size(), snapshotEndereco.highlights()),
                toFonte("INTEL_RESTRICOES", snapshotRestricao.enabled(), snapshotRestricao.realtime(), snapshotRestricao.restricoes().size(), snapshotRestricao.highlights()),
                toFonte("MANDADOS_PESSOA", snapshotMandado.enabled(), snapshotMandado.realtime(), snapshotMandado.mandados().size(), snapshotMandado.highlights())
        );

        String correlationId = UUID.randomUUID().toString();
        PessoaLocalizacaoGovernanceService.PosturaConsulta postura = governanceService.avaliar(
                executor,
                canal,
                request,
                possuiContextoFormal,
                enderecoEstritoLiberado,
                enderecoEstritoElegivel
        );

        boolean enderecoEstritoEfetivo = enderecoEstritoLiberado && (!postura.stepUpRequired() || postura.stepUpSatisfied());
        if (postura.stepUpRequired() && !postura.stepUpSatisfied()) {
            alertas.add("Step-up de autenticação pendente; endereço estrito suprimido até reforço de sessão.");
        }
        List<PessoaLocalizacaoResponse.EnderecoCandidato> enderecos = collectEnderecos(cadastroInterno, snapshotEndereco, enderecoEstritoEfetivo);

        PessoaLocalizacaoResponse.IdentificacaoPessoa identificacao = new PessoaLocalizacaoResponse.IdentificacaoPessoa(
                resolveNome(cadastroInterno, prontuario, processosRelacionados),
                documentoValidator.mascararDocumento(cpf),
                cadastroInterno != null,
                cadastroInterno != null && cadastroInterno.getTipoUsuario() != null ? cadastroInterno.getTipoUsuario().name() : null,
                cadastroInterno != null ? cadastroInterno.getComarca() : null,
                cadastroInterno != null ? cadastroInterno.getUf() : null,
                vinculos.size(),
                resolveNivelConfianca(cadastroInterno, prontuario, snapshotEndereco)
        );

        PessoaLocalizacaoResponse.GovernancaConsultaResumo governanca = buildGovernanca(
                executor,
                request,
                fundamento,
                possuiContextoFormal,
                authorizationService.canConsultarLocalizacaoSemProcesso(executor),
                enderecoEstritoElegivel,
                true
        );

        PessoaLocalizacaoResponse.SecurityPostureResumo securityPosture = new PessoaLocalizacaoResponse.SecurityPostureResumo(
                postura.level(),
                postura.score(),
                postura.requiresReview(),
                postura.offHours(),
                postura.releaseMode(),
                postura.stepUpRequired(),
                postura.stepUpSatisfied(),
                postura.challengeHint(),
                postura.sinais()
        );

        String referenciaProcedimental = normalizeReferenciaProcedimental(request.referenciaProcedimental(), request.processoId(), request.mandadoId());
        String resourceKey = canal.name() + ':' + cpf + ':' + fundamento.name() + ':' + referenciaProcedimental;
        auditLedgerService.appendSafely("PESSOA_LOCALIZACAO_CPF", "PESSOA", resourceKey, correlationId, finalidade);

        PessoaLocalizacaoResponse response = new PessoaLocalizacaoResponse(
                correlationId,
                Instant.now(),
                executor.getTipoUsuario() != null ? executor.getTipoUsuario().name() : null,
                scope,
                finalidade,
                documentoValidator.mascararDocumento(cpf),
                enderecoEstritoEfetivo,
                enderecoEstritoEfetivo ? "ESTRITO" : "MINIMIZADO",
                governanca,
                securityPosture,
                identificacao,
                enderecos,
                vinculos,
                restricoes,
                fontes,
                List.copyOf(alertas),
                recomendarAcao(canal, fundamento, enderecos, restricoes, vinculos, possuiContextoFormal)
        );
        governanceService.registrar(correlationId, executor, cpf, canal, request, response, postura);
        return response;
    }


    @Transactional(readOnly = true)
    public List<com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoConsultaResumo> listarRecentes(CanalConsulta canal, int limit) {
        return governanceService.listarRecentes(currentUserService.getRequired(), canal, limit);
    }

    @Transactional(readOnly = true)
    public PessoaLocalizacaoGovernanceMetricas metricas(CanalConsulta canal, int recentLimit) {
        return summaryService.resumir(currentUserService.getRequired(), canal, recentLimit);
    }

    private void enforceAccess(Usuario executor,
                               CanalConsulta canal,
                               boolean possuiContextoFormal,
                               PessoaLocalizacaoRequest request,
                               PessoaLocalizacaoFundamento fundamento) {
        if (!authorizationService.canLocatePessoaByCpf(executor)) {
            throw new AccessDeniedPjbException("Perfil sem permissão para localizar pessoa por CPF.");
        }
        TipoUsuario tipo = executor != null ? executor.getTipoUsuario() : null;
        boolean canalValido = switch (canal) {
            case OFICIAL_JUSTICA -> tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR;
            case DELEGADO -> tipo == TipoUsuario.DELEGADO_POLICIA || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL;
            case MAGISTRATURA -> tipo != null && (tipo.isMagistratura() || tipo.isAssessor());
        };
        if (!canalValido) {
            throw new AccessDeniedPjbException("Canal de inteligência incompatível com o perfil autenticado.");
        }
        if (!possuiContextoFormal && !authorizationService.canConsultarLocalizacaoSemProcesso(executor)) {
            throw new AccessDeniedPjbException("Consulta sem processo ou mandado vinculado não liberada para o perfil autenticado.");
        }
        if (request.justificativaOperacional() == null || request.justificativaOperacional().isBlank()) {
            throw new IllegalArgumentException("Justificativa operacional obrigatória para consulta de localização.");
        }
        if (!possuiContextoFormal && fundamento.recomendaContextoFormal() && !fundamento.permiteConsultaAberta()) {
            throw new AccessDeniedPjbException("Fundamento informado exige processo, mandado ou referência procedimental válida.");
        }
        if ((tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) && !possuiContextoFormal && request.exigirEnderecoEstrito()) {
            throw new AccessDeniedPjbException("Oficial de justiça só pode solicitar endereço estrito com processo ou mandado vinculado.");
        }
        if ((tipo == TipoUsuario.DELEGADO_POLICIA || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL) && request.exigirEnderecoEstrito() && !possuiContextoFormal) {
            throw new AccessDeniedPjbException("Autoridade policial precisa de contexto formal para materialização de endereço estrito.");
        }
        if (!tipo.isMagistratura() && request.referenciaProcedimental() == null && !possuiContextoFormal) {
            throw new IllegalArgumentException("Referência procedimental obrigatória para consulta sem processo ou mandado vinculado.");
        }
    }

    private Processo resolveProcessoContexto(Long processoId) {
        if (processoId == null) {
            return null;
        }
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return processo;
    }

    private String normalizeCpf(String raw) {
        String documento = documentoValidator.normalizarDocumento(raw);
        if (documento.length() != 11 || !documentoValidator.cpfValido(documento)) {
            throw new IllegalArgumentException("CPF inválido para localização judicial.");
        }
        return documento;
    }

    private PessoaLocalizacaoFundamento resolveFundamento(PessoaLocalizacaoRequest request, CanalConsulta canal) {
        if (request.fundamento() != null) {
            return request.fundamento();
        }
        return switch (canal) {
            case OFICIAL_JUSTICA -> PessoaLocalizacaoFundamento.CUMPRIMENTO_MANDADO;
            case DELEGADO -> PessoaLocalizacaoFundamento.INVESTIGACAO_POLICIAL_FORMAL;
            case MAGISTRATURA -> PessoaLocalizacaoFundamento.DECISAO_JUDICIAL_EXECUTIVA;
        };
    }

    private ProntuarioNacionalService.ProntuarioNacionalView consultarProntuarioSilencioso(String cpf) {
        ProntuarioNacionalService service = prontuarioNacionalService.getIfAvailable();
        if (service == null) {
            return null;
        }
        try {
            return service.consultarPorDocumento(cpf);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String buildScope(Usuario executor, CanalConsulta canal) {
        String uf = executor != null && executor.getUf() != null ? executor.getUf() : "NA";
        String comarca = executor != null && executor.getComarca() != null ? executor.getComarca() : "NACIONAL";
        return canal.name() + ':' + uf + ':' + comarca;
    }

    private static String normalizeFinalidade(String finalidade, CanalConsulta canal, PessoaLocalizacaoFundamento fundamento) {
        if (finalidade != null && !finalidade.isBlank()) {
            return finalidade.trim();
        }
        return switch (canal) {
            case OFICIAL_JUSTICA -> "Cumprimento de mandado, diligência ou penhora judicial com fundamento " + fundamento.name();
            case DELEGADO -> "Apoio investigativo e checagem operacional por autoridade policial com fundamento " + fundamento.name();
            case MAGISTRATURA -> "Instrução decisória, saneamento ou execução judicial com fundamento " + fundamento.name();
        };
    }

    private static String normalizeReferenciaProcedimental(String referencia, Long processoId, Long mandadoId) {
        if (referencia != null && !referencia.isBlank()) {
            return referencia.trim();
        }
        if (processoId != null) {
            return "PROC-" + processoId;
        }
        if (mandadoId != null) {
            return "MAND-" + mandadoId;
        }
        return "SEM_REFERENCIA_FORMAL";
    }

    private static PessoaLocalizacaoResponse.GovernancaConsultaResumo buildGovernanca(Usuario executor,
                                                                                       PessoaLocalizacaoRequest request,
                                                                                       PessoaLocalizacaoFundamento fundamento,
                                                                                       boolean possuiContextoFormal,
                                                                                       boolean consultaSemProcessoAutorizada,
                                                                                       boolean enderecoEstritoElegivel,
                                                                                       boolean consultaPersistida) {
        LinkedHashSet<String> controles = new LinkedHashSet<>();
        controles.add("CPF_VALIDADO");
        controles.add("AUDITORIA_LEDGER");
        controles.add("AUTORIZACAO_POR_PERFIL");
        controles.add(possuiContextoFormal ? "CONTEXTO_FORMAL_PRESENTE" : "CONTEXTO_FORMAL_AUSENTE");
        controles.add(enderecoEstritoElegivel ? "ENDERECO_ESTRITO_ELEGIVEL" : "ENDERECO_MINIMIZADO");
        if (executor != null && executor.getTipoUsuario() != null && executor.getTipoUsuario().isAssessor()) {
            controles.add("OPERACAO_DELEGADA_SUPORTADA");
        }
        return new PessoaLocalizacaoResponse.GovernancaConsultaResumo(
                fundamento.name(),
                possuiContextoFormal,
                consultaSemProcessoAutorizada,
                enderecoEstritoElegivel,
                buildGovernanceTrail(executor, fundamento, possuiContextoFormal),
                List.copyOf(controles),
                normalizeReferenciaProcedimental(request.referenciaProcedimental(), request.processoId(), request.mandadoId()),
                consultaPersistida
        );
    }

    private static String buildGovernanceTrail(Usuario executor, PessoaLocalizacaoFundamento fundamento, boolean possuiContextoFormal) {
        String perfil = executor != null && executor.getTipoUsuario() != null ? executor.getTipoUsuario().name() : "SEM_PERFIL";
        String territorial = joinNonBlank("/", executor != null ? executor.getUf() : null, executor != null ? executor.getComarca() : null);
        return joinNonBlank(" | ", perfil, fundamento.name(), possuiContextoFormal ? "FORMAL" : "ABERTO_CONTROLADO", territorial);
    }

    private List<PessoaLocalizacaoResponse.EnderecoCandidato> collectEnderecos(Usuario cadastroInterno,
                                                                                SnapshotEndereco snapshotEndereco,
                                                                                boolean enderecoEstritoLiberado) {
        List<PessoaLocalizacaoResponse.EnderecoCandidato> enderecos = new ArrayList<>();
        if (cadastroInterno != null && (cadastroInterno.getComarca() != null || cadastroInterno.getUf() != null)) {
            enderecos.add(new PessoaLocalizacaoResponse.EnderecoCandidato(
                    "CADASTRO_INTERNO",
                    "REFERENCIA_TERRITORIAL",
                    enderecoEstritoLiberado
                            ? joinNonBlank("/", cadastroInterno.getComarca(), cadastroInterno.getUf())
                            : maskTerritory(cadastroInterno.getComarca(), cadastroInterno.getUf()),
                    null,
                    cadastroInterno.getComarca(),
                    cadastroInterno.getUf(),
                    null,
                    true,
                    true,
                    0.64,
                    Instant.now()
            ));
        }
        for (EnderecoInfo info : snapshotEndereco.enderecos()) {
            enderecos.add(new PessoaLocalizacaoResponse.EnderecoCandidato(
                    info.fonte(),
                    info.tipo(),
                    enderecoEstritoLiberado ? info.descricao() : minimizeDescricao(info),
                    enderecoEstritoLiberado ? info.bairro() : null,
                    info.cidade(),
                    info.uf(),
                    enderecoEstritoLiberado ? info.cep() : null,
                    info.principal(),
                    !enderecoEstritoLiberado || info.parcial(),
                    info.confianca(),
                    info.atualizadoEm()
            ));
        }
        return enderecos.stream()
                .sorted(Comparator.comparing(PessoaLocalizacaoResponse.EnderecoCandidato::principal).reversed()
                        .thenComparing(PessoaLocalizacaoResponse.EnderecoCandidato::confianca).reversed())
                .limit(8)
                .toList();
    }

    private static String minimizeDescricao(EnderecoInfo info) {
        String cidade = info.cidade();
        String uf = info.uf();
        String bairro = info.bairro();
        if (cidade == null && uf == null && bairro == null) {
            return "Referência territorial disponível em fonte protegida.";
        }
        return joinNonBlank(" / ", bairro, cidade, uf);
    }

    private static String maskTerritory(String comarca, String uf) {
        if (comarca == null && uf == null) {
            return "Referência territorial protegida";
        }
        String prefixo = comarca == null || comarca.isBlank() ? "LOCALIDADE" : comarca.substring(0, Math.min(3, comarca.length())).toUpperCase(Locale.ROOT) + "***";
        return joinNonBlank(" / ", prefixo, uf);
    }

    private List<PessoaLocalizacaoResponse.VinculoProcessualResumo> collectVinculos(List<Processo> processos,
                                                                                     ProntuarioNacionalService.ProntuarioNacionalView prontuario) {
        Map<String, PessoaLocalizacaoResponse.VinculoProcessualResumo> out = new LinkedHashMap<>();
        for (Processo processo : processos) {
            String numero = safeNumero(processo);
            out.putIfAbsent("PJB:" + processo.getId(), new PessoaLocalizacaoResponse.VinculoProcessualResumo(
                    processo.getId(),
                    numero,
                    resolvePolo(processo),
                    processo.getClasseProcessual(),
                    processo.getAssunto(),
                    processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null,
                    processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null,
                    processo.getDataUltimaMovimentacao() != null ? processo.getDataUltimaMovimentacao().toInstant(java.time.ZoneOffset.UTC) : null
            ));
        }
        if (prontuario != null) {
            for (ProntuarioNacionalService.EntradaProntuarioView entrada : prontuario.entradas()) {
                String key = "PN:" + entrada.entradaId();
                out.putIfAbsent(key, new PessoaLocalizacaoResponse.VinculoProcessualResumo(
                        entrada.processoLocalId(),
                        entrada.nupn(),
                        entrada.polo(),
                        entrada.classeProcessual(),
                        entrada.assunto(),
                        entrada.tribunalCodigo(),
                        entrada.statusProcesso(),
                        entrada.atualizadoEm()
                ));
            }
        }
        return out.values().stream().limit(20).toList();
    }

    private List<PessoaLocalizacaoResponse.RestricaoResumo> collectRestricoes(SnapshotRestricao snapshotRestricao,
                                                                               SnapshotMandado snapshotMandado) {
        List<PessoaLocalizacaoResponse.RestricaoResumo> out = new ArrayList<>();
        for (RestricaoInfo info : snapshotRestricao.restricoes()) {
            out.add(new PessoaLocalizacaoResponse.RestricaoResumo(
                    info.sistema(),
                    info.tipo(),
                    info.situacao(),
                    info.referencia(),
                    info.descricao(),
                    info.atualizadoEm()
            ));
        }
        for (MandadoInfo info : snapshotMandado.mandados()) {
            out.add(new PessoaLocalizacaoResponse.RestricaoResumo(
                    info.sistema(),
                    info.tipo(),
                    info.situacao(),
                    info.referencia(),
                    info.descricao(),
                    info.atualizadoEm()
            ));
        }
        return out.stream().limit(15).toList();
    }

    private static PessoaLocalizacaoResponse.FonteConsultaResumo toFonte(String fonte,
                                                                         boolean habilitada,
                                                                         boolean realtime,
                                                                         int itens,
                                                                         List<String> highlights) {
        return new PessoaLocalizacaoResponse.FonteConsultaResumo(fonte, habilitada, realtime, itens, highlights == null ? List.of() : List.copyOf(highlights));
    }

    private static List<String> prontuarioHighlights(ProntuarioNacionalService.ProntuarioNacionalView prontuario) {
        if (prontuario == null) {
            return List.of("Prontuário nacional não consultado ou indisponível.");
        }
        return List.of(
                "Total de processos relacionados: " + prontuario.totalProcessos(),
                "Processos ativos: " + prontuario.processosAtivos(),
                "Tribunais distintos: " + prontuario.tribunaisDistintos()
        );
    }

    private static String resolveNome(Usuario cadastroInterno,
                                      ProntuarioNacionalService.ProntuarioNacionalView prontuario,
                                      List<Processo> processos) {
        if (cadastroInterno != null && cadastroInterno.getNome() != null && !cadastroInterno.getNome().isBlank()) {
            return cadastroInterno.getNome();
        }
        if (prontuario != null && prontuario.nomeCanonico() != null && !prontuario.nomeCanonico().isBlank()) {
            return prontuario.nomeCanonico();
        }
        return processos.stream()
                .flatMap(processo -> java.util.stream.Stream.of(processo.getParteAutoraNome(), processo.getParteReuNome()))
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse("NAO INFORMADO");
    }

    private static String resolveNivelConfianca(Usuario cadastroInterno,
                                                ProntuarioNacionalService.ProntuarioNacionalView prontuario,
                                                SnapshotEndereco snapshotEndereco) {
        int pontos = 0;
        if (cadastroInterno != null) {
            pontos += 2;
        }
        if (prontuario != null && prontuario.totalProcessos() > 0) {
            pontos += 2;
        }
        if (snapshotEndereco.enabled() && !snapshotEndereco.enderecos().isEmpty()) {
            pontos += 3;
        }
        if (pontos >= 6) {
            return "ALTO";
        }
        if (pontos >= 3) {
            return "MEDIO";
        }
        return "BASICO";
    }

    private static String recomendarAcao(CanalConsulta canal,
                                         PessoaLocalizacaoFundamento fundamento,
                                         List<PessoaLocalizacaoResponse.EnderecoCandidato> enderecos,
                                         List<PessoaLocalizacaoResponse.RestricaoResumo> restricoes,
                                         List<PessoaLocalizacaoResponse.VinculoProcessualResumo> vinculos,
                                         boolean possuiContextoFormal) {
        if (!enderecos.isEmpty() && possuiContextoFormal) {
            return switch (canal) {
                case OFICIAL_JUSTICA -> "Gerar diligência priorizada com rota, tentativa principal e certificação estruturada de resultado sob fundamento " + fundamento.name() + ".";
                case DELEGADO -> "Cruzar endereço candidato com inteligência operacional e programar diligência validada pela circunscrição sob fundamento " + fundamento.name() + ".";
                case MAGISTRATURA -> "Avaliar expedição, reforço, renovação ou cooperação do mandado com base no melhor endereço candidato e fundamento " + fundamento.name() + ".";
            };
        }
        if (!restricoes.isEmpty()) {
            return "Existem restrições ou sinais externos úteis; avaliar integração de ordem judicial ou requisição institucional antes da próxima etapa.";
        }
        if (!vinculos.isEmpty()) {
            return "Usar os vínculos processuais encontrados para aprofundar cooperação interinstitucional, intimações e nova coleta de endereços.";
        }
        return "Sem material suficiente para localização fina; manter trilha de auditoria e acionar provedor externo homologado ou diligência complementar.";
    }

    private static String safeNumero(Processo processo) {
        if (processo == null) {
            return null;
        }
        String numero = processo.getNumeroUnificado();
        if (numero != null && !numero.isBlank()) {
            return numero;
        }
        return processo.getNumeroProcesso();
    }

    private static String resolvePolo(Processo processo) {
        if (processo == null) {
            return null;
        }
        return "RELACIONADO";
    }

    private static String joinNonBlank(String separator, String... values) {
        return java.util.stream.Stream.of(values).filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.joining(separator));
    }
}
