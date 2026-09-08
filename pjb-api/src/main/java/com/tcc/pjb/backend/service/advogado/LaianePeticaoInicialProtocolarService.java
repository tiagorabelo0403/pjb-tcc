package com.tcc.pjb.backend.service.advogado;

import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.core.protocolo.completude.ContextoValidacaoCompletude;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeMetrics;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeValidator;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloPendenciaApplicationService;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloPendenteException;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ResultadoValidacao;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.OrigemValidacao;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoRepresentanteProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloPendencia;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.guard.DefensoriaInstitutionalCompetenceGuardService;
import com.tcc.pjb.backend.service.processual.legitimidade.OabValidationService;
import com.tcc.pjb.backend.service.processual.numero.NumeroProcessoCnjService;
import com.tcc.pjb.backend.service.processual.protocolo.ProtocoloReciboService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Converte um rascunho de petição inicial da Laiane num ajuizamento real: valida completude,
 * materializa a peça como PDF, registra polo institucional e emite recibo. Extraído de
 * {@link LaianePeticaoInicialDraftService} porque esses 15 colaboradores são usados
 * exclusivamente pelo fluxo de protocolo -- nenhum é tocado por estruturar/salvar/listarMinhas
 * (o fluxo de composição do rascunho, que fica na raiz).
 */
@Service
public class LaianePeticaoInicialProtocolarService {

    private static final Pattern AUTOR_MINUTA_PATTERN = Pattern.compile("(?m)^(.{1,255}?), por intermédio de .+?, apresenta ");
    private static final Pattern REU_MINUTA_PATTERN = Pattern.compile("(?m)^em face de (.{1,255}?)\\.$");

    private final LaianePeticaoInicialDraftSessionRepository repository;
    private final ProcessoRepository processoRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final AjuizamentoService ajuizamentoService;
    private final DefensoriaInstitutionalCompetenceGuardService defensoriaInstitutionalCompetenceGuardService;
    private final OabValidationService oabValidationService;
    private final NumeroProcessoCnjService numeroProcessoCnjService;
    private final PoloProcessualApplicationService poloProcessualApplicationService;
    private final ProtocoloReciboService protocoloReciboService;
    private final MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine;
    private final ProtocoloCompletudeValidator completudeValidator;
    private final ProtocoloPendenciaApplicationService completudeService;
    private final ProtocoloCompletudeMetrics completudeMetrics;
    private final LaianePeticaoInicialPecaMaterializationService pecaMaterializationService;

    public LaianePeticaoInicialProtocolarService(LaianePeticaoInicialDraftSessionRepository repository,
                                                  ProcessoRepository processoRepository,
                                                  CurrentUserService currentUserService,
                                                  ObjectMapper objectMapper,
                                                  AjuizamentoService ajuizamentoService,
                                                  DefensoriaInstitutionalCompetenceGuardService defensoriaInstitutionalCompetenceGuardService,
                                                  OabValidationService oabValidationService,
                                                  NumeroProcessoCnjService numeroProcessoCnjService,
                                                  PoloProcessualApplicationService poloProcessualApplicationService,
                                                  ProtocoloReciboService protocoloReciboService,
                                                  MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine,
                                                  ProtocoloCompletudeValidator completudeValidator,
                                                  ProtocoloPendenciaApplicationService completudeService,
                                                  ProtocoloCompletudeMetrics completudeMetrics,
                                                  LaianePeticaoInicialPecaMaterializationService pecaMaterializationService) {
        this.repository = Objects.requireNonNull(repository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.ajuizamentoService = Objects.requireNonNull(ajuizamentoService);
        this.defensoriaInstitutionalCompetenceGuardService = Objects.requireNonNull(defensoriaInstitutionalCompetenceGuardService);
        this.oabValidationService = Objects.requireNonNull(oabValidationService);
        this.numeroProcessoCnjService = Objects.requireNonNull(numeroProcessoCnjService);
        this.poloProcessualApplicationService = Objects.requireNonNull(poloProcessualApplicationService);
        this.protocoloReciboService = Objects.requireNonNull(protocoloReciboService);
        this.mapaCompetenciaDinamicoEngine = Objects.requireNonNull(mapaCompetenciaDinamicoEngine);
        this.completudeValidator = Objects.requireNonNull(completudeValidator);
        this.completudeService = Objects.requireNonNull(completudeService);
        this.completudeMetrics = Objects.requireNonNull(completudeMetrics);
        this.pecaMaterializationService = Objects.requireNonNull(pecaMaterializationService);
    }

    @Transactional
    public LaianePeticaoInicialDraftService.ProtocolarResult protocolar(Long draftId, LaianePeticaoInicialDraftService.ProtocolarRequest request) {
        Usuario solicitante = currentUserService.getRequired();
        LaianePeticaoInicialDraftSession entity = repository.findByIdAndSolicitante_Id(draftId, solicitante.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("LaianePeticaoInicialDraftSession", draftId));
        RitoProcessual ritoDraft = RitoProcessual.tryParse(entity.getRitoSugerido()).orElse(null);
        Usuario usuario = requirePeticionante(ritoDraft);

        if (entity.getProcesso() != null && "PROTOCOLO_REALIZADO".equalsIgnoreCase(entity.getStatus())) {
            Processo existing = entity.getProcesso();
            return new LaianePeticaoInicialDraftService.ProtocolarResult(
                    entity.getId(),
                    existing.getId(),
                    existing.getNumeroProcesso(),
                    entity.getStatus(),
                    entity.getUpdatedAt(),
                    entity.getHashIntegridade(),
                    existing.getConnectorProtocolReference()
            );
        }

        defensoriaInstitutionalCompetenceGuardService.requireAllowedForDraftProtocol(entity, request != null ? request.tipoJustica() : null);
        oabValidationService.requireAdvogadoAptoParaProtocolo(usuario);

        PartesProtocoladas partes = resolvePartesProtocoladas(entity, usuario);
        TipoJustica tipoJusticaProtocolo = resolveTipoJustica(request != null ? request.tipoJustica() : null);
        RamoDireito ramoDireitoProtocolo = RamoDireito.fromString(entity.getRamoDireito());
        if (ramoDireitoProtocolo == null) {
            ramoDireitoProtocolo = RamoDireito.CIVIL;
        }
        Processo processo = new Processo();
        processo.setTipoJustica(tipoJusticaProtocolo);
        processo.setRamoDireito(ramoDireitoProtocolo);
        processo.setMateria(com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao.fromRamo(ramoDireitoProtocolo));
        processo.setClasseProcessual(defaultText(entity.getClasseSugerida(), "PETICAO_INICIAL"));
        processo.setAssunto(defaultText(entity.getTituloCaso(), "PETICAO INICIAL"));
        processo.setObjetoProcessual(firstOrNull(readList(entity.getFatosJson())));
        processo.setPedidoPrincipal(firstOrNull(readList(entity.getPedidosJson())));
        processo.setPedidosConsolidados(joinLines(readList(entity.getPedidosJson())));
        processo.setMaterialProbatorioResumo(joinLines(readList(entity.getProvasJson())));
        processo.setResumoIA(joinLines(readList(entity.getFundamentosJson())));
        processo.setValorCausa(null);
        processo.setUsuario(usuario);
        processo.setUf(defaultText(usuario.getUf(), null));
        processo.setComarca(defaultText(usuario.getComarca(), null));
        processo.setNumeroUnificado(numeroProcessoCnjService.gerarParaAjuizamento(processo));
        processo.setNumeroProcesso(processo.getNumeroUnificado());
        processo.setParteAutoraNome(partes.autoraNome());
        processo.setParteReuNome(partes.reuNome());
        if (isPeticionantePessoal(usuario)) {
            processo.setParteAutoraCpf(usuario.getCpf());
        }
        processo.setUfAutor(entity.getUfAutor());
        processo.setComarcaAutor(entity.getComarcaAutor());
        if (!entity.isEnderecoReuDesconhecido()) {
            processo.setUfReu(entity.getUfReu());
            processo.setComarcaReu(entity.getComarcaReu());
        }
        processo.setConnectorSystem("LAIANE_PETICAO_INICIAL");
        processo.setConnectorProtocolReference("LAIANE-DRAFT:" + entity.getId());
        processo.setConnectorSubmissionStatus("PROTOCOLO_REALIZADO");
        processo.setConnectorSubmissionMessage("Draft da petição inicial convertido em ajuizamento real.");
        processo.setConnectorSubmissionProcessedAt(java.time.LocalDateTime.now());
        processo.setRito(RitoProcessual.tryParse(entity.getRitoSugerido()).orElse(RitoProcessual.COMUM_ORDINARIO));

        List<String> docNomes = request != null && request.documentosAnexados() != null
                ? request.documentosAnexados().stream().map(Enum::name).toList()
                : List.of();
        ContextoValidacaoCompletude contextoGate =
                new ContextoValidacaoCompletude(
                        processo.getRito(),
                        resolveRepresentanteCompletude(usuario),
                        request != null ? request.tipoPartePrincipal() : null,
                        request != null && request.condicoesAplicaveis() != null
                                ? request.condicoesAplicaveis() : java.util.Set.of(),
                        request != null && request.documentosAnexados() != null
                                ? request.documentosAnexados() : List.of(),
                        java.time.LocalDate.now());
        ResultadoValidacao resultadoGate =
                completudeValidator.validar(contextoGate);
        completudeMetrics.registrarValidacao(OrigemValidacao.PROTOCOLO);
        if (resultadoGate.temBloqueante()) {
            completudeMetrics.registrarBloqueado(processo.getRito());
            resultadoGate.bloqueantes().forEach(v -> {
                if (v instanceof ViolacaoCompletude.DocumentoObrigatorioAusente ausente) {
                    completudeMetrics.registrarViolacaoTipoDoc(ausente.tipoDocumento());
                }
            });
            ProtocoloPendencia pendencia =
                    completudeService.registrarPendencia(entity.getId(), resultadoGate, docNomes,
                            OrigemValidacao.PROTOCOLO,
                            usuario.getId());
            throw new ProtocoloPendenteException(
                    entity.getId(), resultadoGate, pendencia.getPrazoRegularizacao());
        }
        completudeMetrics.registrarLiberado(processo.getRito());
        completudeService.registrarCompleto(entity.getId(), resultadoGate, docNomes,
                OrigemValidacao.PROTOCOLO,
                usuario.getId());

        Processo salvo = ajuizamentoService.ajuizar(processo);
        salvo.setTipoJustica(tipoJusticaProtocolo);
        salvo.setRamoDireito(ramoDireitoProtocolo);
        salvo.setMateria(com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao.fromRamo(ramoDireitoProtocolo));
        processoRepository.saveAndFlush(salvo);
        mapaCompetenciaDinamicoEngine.registrarDistribuicaoInicial(salvo);
        registrarInstitucional(salvo, usuario, usuario.getTipoUsuario());
        pecaMaterializationService.materializar(salvo, usuario, entity);
        protocoloReciboService.emitirReciboPeticaoInicial(salvo, usuario, entity.getHashIntegridade());

        entity.setProcesso(salvo);
        entity.setStatus("PROTOCOLO_REALIZADO");
        LaianePeticaoInicialDraftSession saved = repository.save(entity);

        return new LaianePeticaoInicialDraftService.ProtocolarResult(
                saved.getId(),
                salvo.getId(),
                salvo.getNumeroProcesso(),
                saved.getStatus(),
                saved.getUpdatedAt(),
                saved.getHashIntegridade(),
                salvo.getConnectorProtocolReference()
        );
    }

    private void registrarInstitucional(Processo processo, Usuario peticionante, TipoUsuario tipoUsuario) {
        TipoPolo tipoPolo = tipoPoloInstitucional(tipoUsuario);
        if (tipoPolo == null || peticionante == null) {
            return;
        }
        poloProcessualApplicationService.incluir(
                processo.getId(),
                tipoPolo,
                tipoPolo == TipoPolo.MINISTERIO_PUBLICO ? TipoParte.MINISTERIO_PUBLICO : TipoParte.TERCEIRO_INTERESSADO,
                firstNonBlank(peticionante.getNome(), tipoPolo.label()),
                documentoInstitucional(peticionante),
                documentoTipo(documentoInstitucional(peticionante)),
                null,
                null,
                peticionante.getId(),
                null,
                null
        );
    }

    private TipoPolo tipoPoloInstitucional(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return null;
        }
        if (tipoUsuario.isDefensoriaPublica()) {
            return TipoPolo.DEFENSORIA;
        }
        if (tipoUsuario.isMinisterioPublico()) {
            return TipoPolo.MINISTERIO_PUBLICO;
        }
        if (tipoUsuario.isProcuradoria()) {
            return TipoPolo.PROCURADORIA;
        }
        return null;
    }

    private String documentoInstitucional(Usuario peticionante) {
        return digits(trimToNull(peticionante == null ? null : peticionante.getCpf()));
    }

    private String documentoTipo(String documento) {
        if (documento == null) {
            return null;
        }
        return documento.length() == 14 ? "CNPJ" : documento.length() == 11 ? "CPF" : null;
    }

    private String digits(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String onlyDigits = normalized.replaceAll("\\D+", "");
        return onlyDigits.isBlank() ? null : onlyDigits;
    }

    private String firstNonBlank(String first, String second) {
        String a = trimToNull(first);
        return a == null ? trimToNull(second) : a;
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
        throw new com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException("A funcionalidade e exclusiva para advocacia, procuradoria, defensoria e Ministerio Publico");
    }

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

    private boolean isPeticionantePessoal(Usuario usuario) {
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        return tipo != null && tipo.isPeticionantePessoal();
    }

    private TipoJustica resolveTipoJustica(String raw) {
        TipoJustica parsed = TipoJustica.fromString(raw);
        return parsed == null ? TipoJustica.ESTADUAL : parsed;
    }

    private PartesProtocoladas resolvePartesProtocoladas(LaianePeticaoInicialDraftSession entity, Usuario usuario) {
        String minuta = defaultText(entity.getMinutaInicial(), null);
        String reu = defaultText(extractFirstGroup(REU_MINUTA_PATTERN, minuta), null);
        if (isPeticionantePessoal(usuario)) {
            return new PartesProtocoladas(usuario.getNome(), reu);
        }
        String autora = defaultText(extractFirstGroup(AUTOR_MINUTA_PATTERN, minuta), usuario.getNome());
        return new PartesProtocoladas(autora, reu);
    }

    private String extractFirstGroup(Pattern pattern, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var matcher = pattern.matcher(value);
        return matcher.find() ? trimToNull(matcher.group(1)) : null;
    }

    private String joinLines(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join("\n", values);
    }

    private String firstOrNull(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
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

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private TipoRepresentanteProcessual resolveRepresentanteCompletude(Usuario usuario) {
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null) return TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO;
        if (tipo.isDefensoriaPublica()) return TipoRepresentanteProcessual.DEFENSOR_PUBLICO;
        if (tipo.isMinisterioPublico() || tipo.isProcuradoria()) return TipoRepresentanteProcessual.MINISTERIO_PUBLICO;
        if (tipo.isAdvocacia()) return TipoRepresentanteProcessual.ADVOGADO_PRIVADO;
        return TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO;
    }

    private record PartesProtocoladas(String autoraNome, String reuNome) {
    }
}
