package com.tcc.pjb.backend.service.identity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.model.entity.identity.ProntuarioNacionalEntrada;
import com.tcc.pjb.backend.model.repository.ProntuarioNacionalEntradaRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class ProntuarioNacionalService {

    public static final String EVT_PRONTUARIO_ENTRADA_REGISTRADA = "pjb.prontuario.entrada.registrada";
    public static final String EVT_PRONTUARIO_STATUS_ATUALIZADO = "pjb.prontuario.status.atualizado";
    public static final String TOPIC_PRONTUARIO_NACIONAL = "pjb.nacional.prontuario.v1";

    private static final Logger log = LoggerFactory.getLogger(ProntuarioNacionalService.class);

    private final ProntuarioNacionalEntradaRepository prontuarioRepository;
    private final IdentidadeJuridicaNacionalService identidadeService;
    private final DocumentoNacionalValidator documentoValidator;
    private final OutboxPublisher outbox;

    public ProntuarioNacionalService(ProntuarioNacionalEntradaRepository prontuarioRepository,
                                     IdentidadeJuridicaNacionalService identidadeService,
                                     DocumentoNacionalValidator documentoValidator,
                                     OutboxPublisher outbox) {
        this.prontuarioRepository = Objects.requireNonNull(prontuarioRepository);
        this.identidadeService = Objects.requireNonNull(identidadeService);
        this.documentoValidator = Objects.requireNonNull(documentoValidator);
        this.outbox = Objects.requireNonNull(outbox);
    }

    @Transactional
    public void registrarProcessoAjuizado(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        String nupn = resolverNupn(processo);
        if (nupn == null) {
            log.warn("Prontuario nacional ignorado para processo sem numero unificado. processoId={}", processo.getId());
            return;
        }

        Instant ocorridoEm = Instant.now();
        String tribunalCodigo = resolverTribunalCodigo(processo);
        String tribunalOrigemUri = construirTribunalOrigemUri(processo, tribunalCodigo, nupn);
        StatusProcesso status = processo.getStatusProcesso() != null ? processo.getStatusProcesso() : StatusProcesso.EM_ANDAMENTO;

        registrarSeValido(new RegistroEntradaCommand(
                processo.getParteAutoraCpf(),
                processo.getParteAutoraNome(),
                nupn,
                processo.getId(),
                tribunalCodigo,
                tribunalOrigemUri,
                ProntuarioNacionalEntrada.PoloProcessual.ATIVO,
                ProntuarioNacionalEntrada.QualificacaoProcessual.AUTOR,
                processo.getRamoDireito(),
                status,
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getNivelSigilo(),
                ProntuarioNacionalEntrada.OrigemRegistro.AJUIZAMENTO_LOCAL,
                "processo:" + processo.getId(),
                ocorridoEm
        ));

        registrarSeValido(new RegistroEntradaCommand(
                processo.getParteReuCpf(),
                processo.getParteReuNome(),
                nupn,
                processo.getId(),
                tribunalCodigo,
                tribunalOrigemUri,
                ProntuarioNacionalEntrada.PoloProcessual.PASSIVO,
                ProntuarioNacionalEntrada.QualificacaoProcessual.REU,
                processo.getRamoDireito(),
                status,
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getNivelSigilo(),
                ProntuarioNacionalEntrada.OrigemRegistro.AJUIZAMENTO_LOCAL,
                "processo:" + processo.getId(),
                ocorridoEm
        ));

        registrarRepresentacaoUsuario(processo, nupn, tribunalCodigo, tribunalOrigemUri, status, ocorridoEm);
    }

    @Transactional
    public ProntuarioNacionalEntrada registrarEntrada(RegistroEntradaCommand command) {
        Objects.requireNonNull(command, "command");
        String documento = documentoValidator.normalizarDocumento(command.documento());
        documentoValidator.validarDocumento(documento);
        if (command.nupn() == null || command.nupn().isBlank()) {
            throw new IllegalArgumentException("NUPN obrigatorio");
        }
        if (command.tribunalCodigo() == null || command.tribunalCodigo().isBlank()) {
            throw new IllegalArgumentException("Codigo do tribunal obrigatorio");
        }

        String nomeBase = command.nomeSujeito() == null || command.nomeSujeito().isBlank() ? "NAO INFORMADO" : command.nomeSujeito();
        String nomeCanonico = documentoValidator.normalizarNomeCanonico(nomeBase);
        String nomeChave = documentoValidator.gerarChavePesquisa(nomeBase);
        String documentoHash = Hashes.sha256Hex(documento);
        IdentidadeJuridicaNacional identidade = identidadeService.resolverOuCriarPorDocumento(
                documento,
                nomeCanonico,
                IdentidadeJuridicaNacional.OrigemCadastro.TRIBUNAL,
                mapearPapeis(command.polo(), command.qualificacao())
        );

        Optional<ProntuarioNacionalEntrada> existente = prontuarioRepository
                .findByDocumentoHashAndNupnAndPoloAndQualificacaoAndTribunalCodigo(
                        documentoHash,
                        command.nupn().trim(),
                        command.polo(),
                        command.qualificacao(),
                        normalizarTribunalCodigo(command.tribunalCodigo())
                );

        ProntuarioNacionalEntrada entrada = existente.orElseGet(() -> new ProntuarioNacionalEntrada(
                UUID.randomUUID(),
                identidade,
                documento,
                documentoHash,
                nomeCanonico,
                nomeChave,
                command.nupn().trim(),
                command.processoLocalId(),
                normalizarTribunalCodigo(command.tribunalCodigo()),
                normalizarTexto(command.tribunalOrigemUri()),
                command.polo(),
                command.qualificacao(),
                command.ramoDireito(),
                command.statusProcesso() != null ? command.statusProcesso() : StatusProcesso.EM_ANDAMENTO,
                normalizarTexto(command.classeProcessual()),
                normalizarTexto(command.assunto()),
                command.nivelSigilo(),
                command.origemRegistro() != null ? command.origemRegistro() : ProntuarioNacionalEntrada.OrigemRegistro.SINCRONIZACAO_NACIONAL,
                normalizarTexto(command.fonteEventoId()),
                command.ocorridoEm() != null ? command.ocorridoEm() : Instant.now()
        ));

        entrada.setIdentidade(identidade);
        entrada.atualizarProcesso(
                nomeCanonico,
                nomeChave,
                command.ramoDireito(),
                command.statusProcesso() != null ? command.statusProcesso() : StatusProcesso.EM_ANDAMENTO,
                normalizarTexto(command.classeProcessual()),
                normalizarTexto(command.assunto()),
                command.nivelSigilo(),
                normalizarTexto(command.tribunalOrigemUri()),
                normalizarTexto(command.fonteEventoId()),
                command.ocorridoEm() != null ? command.ocorridoEm() : Instant.now()
        );

        ProntuarioNacionalEntrada salvo = prontuarioRepository.save(entrada);
        publicarEventoRegistro(salvo);
        return salvo;
    }

    @PjbTransactionalBudget(operation = "identity.prontuario-nacional.atualizar-status-nupn", maxMillis = 3000)
    @Transactional
    public int atualizarStatusPorNupn(String nupn, StatusProcesso novoStatus, String fonteEventoId) {
        Objects.requireNonNull(novoStatus, "novoStatus");
        if (nupn == null || nupn.isBlank()) {
            return 0;
        }
        List<ProntuarioNacionalEntrada> entradas = prontuarioRepository.findAllByNupn(nupn.trim());
        for (ProntuarioNacionalEntrada entrada : entradas) {
            entrada.atualizarStatus(novoStatus);
        }
        if (!entradas.isEmpty()) {
            prontuarioRepository.saveAll(entradas);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("nupn", nupn.trim());
            payload.put("statusProcesso", novoStatus.name());
            payload.put("ocorridoEm", Instant.now().toString());
            payload.put("topic", TOPIC_PRONTUARIO_NACIONAL);
            if (fonteEventoId != null && !fonteEventoId.isBlank()) {
                payload.put("fonteEventoId", fonteEventoId);
            }
            outbox.enqueue(
                    "prontuario:nupn:" + nupn.trim(),
                    EVT_PRONTUARIO_STATUS_ATUALIZADO,
                    payload,
                    Map.of("source", "prontuario_nacional"),
                    "prontuarioStatus:" + nupn.trim() + ":" + novoStatus.name(),
                    "ProntuarioNacional",
                    nupn.trim()
            );
        }
        return entradas.size();
    }

    @Transactional(readOnly = true)
    public ProntuarioNacionalView consultarPorDocumento(String documentoRaw) {
        String documento = documentoValidator.normalizarDocumento(documentoRaw);
        documentoValidator.validarDocumento(documento);
        String documentoHash = Hashes.sha256Hex(documento);
        List<ProntuarioNacionalEntrada> entradas = prontuarioRepository.findAllByDocumentoHashOrderByOcorridoEmDescAtualizadoEmDesc(documentoHash);
        Optional<IdentidadeJuridicaNacional> identidadeOpt = identidadeService.buscarPorDocumento(documento);

        if (entradas.isEmpty() && identidadeOpt.isEmpty()) {
            return ProntuarioNacionalView.vazio(documentoValidator.mascararDocumento(documento));
        }

        Map<String, ProntuarioNacionalEntrada> ultimaPorNupn = latestByNupn(entradas);
        int totalProcessos = ultimaPorNupn.size();
        int processosAtivos = (int) ultimaPorNupn.values().stream().filter(e -> e.getStatusProcesso().isAtivo()).count();
        int processosArquivados = (int) ultimaPorNupn.values().stream().filter(e -> e.getStatusProcesso().isEncerrado()).count();
        int tribunaisDistintos = (int) entradas.stream().map(ProntuarioNacionalEntrada::getTribunalCodigo).distinct().count();

        String nomeCanonico = identidadeOpt.map(IdentidadeJuridicaNacional::getNomeCanonico)
                .orElseGet(() -> entradas.stream().map(ProntuarioNacionalEntrada::getNomeSujeito).filter(Objects::nonNull).findFirst().orElse("NAO INFORMADO"));
        String prontuarioUri = identidadeOpt.map(IdentidadeJuridicaNacional::getProntuarioNacionalUri)
                .orElse("pjb://prontuario/documento/" + documento);
        boolean govBrVinculado = identidadeOpt.map(i -> i.getGovBrNivel() != IdentidadeJuridicaNacional.GovBrNivel.NAO_VINCULADO).orElse(false);

        List<EntradaProntuarioView> itens = entradas.stream()
                .map(e -> EntradaProntuarioView.of(e, documentoValidator.mascararDocumento(documento)))
                .toList();

        return new ProntuarioNacionalView(
                identidadeOpt.map(IdentidadeJuridicaNacional::getId).orElse(null),
                documentoValidator.mascararDocumento(documento),
                nomeCanonico,
                totalProcessos,
                processosAtivos,
                processosArquivados,
                tribunaisDistintos,
                itens,
                Instant.now(),
                false,
                govBrVinculado,
                prontuarioUri
        );
    }

    @Transactional(readOnly = true)
    public AnaliseConflitoProcessual detectarLitispendenciaOuCoisaJulgada(String documentoAutorRaw,
                                                                          String documentoReuRaw,
                                                                          RamoDireito ramoDireito) {
        Objects.requireNonNull(ramoDireito, "ramoDireito");
        String documentoAutor = documentoValidator.normalizarDocumento(documentoAutorRaw);
        String documentoReu = documentoValidator.normalizarDocumento(documentoReuRaw);
        documentoValidator.validarDocumento(documentoAutor);
        documentoValidator.validarDocumento(documentoReu);

        List<ProntuarioNacionalEntrada> entradasAutor = prontuarioRepository.findAllByDocumentoHashAndPoloAndRamoDireitoOrderByOcorridoEmDesc(
                Hashes.sha256Hex(documentoAutor),
                ProntuarioNacionalEntrada.PoloProcessual.ATIVO,
                ramoDireito
        );
        List<ProntuarioNacionalEntrada> entradasReu = prontuarioRepository.findAllByDocumentoHashAndPoloAndRamoDireitoOrderByOcorridoEmDesc(
                Hashes.sha256Hex(documentoReu),
                ProntuarioNacionalEntrada.PoloProcessual.PASSIVO,
                ramoDireito
        );

        Set<String> nupnsAutor = new LinkedHashSet<>(latestByNupn(entradasAutor).keySet());
        Map<String, StatusProcesso> statusReu = latestByNupn(entradasReu).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getStatusProcesso(), (a, b) -> a, LinkedHashMap::new));

        List<String> ativos = new ArrayList<>();
        List<String> encerrados = new ArrayList<>();
        for (String nupn : nupnsAutor) {
            StatusProcesso status = statusReu.get(nupn);
            if (status == null) {
                continue;
            }
            if (status.isEncerrado()) {
                encerrados.add(nupn);
            } else {
                ativos.add(nupn);
            }
        }

        return new AnaliseConflitoProcessual(
                !ativos.isEmpty(),
                !encerrados.isEmpty(),
                List.copyOf(ativos),
                List.copyOf(encerrados),
                ativos.size() + encerrados.size(),
                Instant.now()
        );
    }

    @Transactional
    public void consumirEvento(EventoProcessoRegistrado evento) {
        Objects.requireNonNull(evento, "evento");
        registrarEntrada(new RegistroEntradaCommand(
                evento.documento(),
                evento.nomeSujeito(),
                evento.nupn(),
                evento.processoLocalId(),
                evento.tribunalCodigo(),
                evento.tribunalOrigemUri(),
                evento.polo(),
                evento.qualificacao(),
                evento.ramoDireito(),
                evento.statusProcesso(),
                evento.classeProcessual(),
                evento.assunto(),
                evento.nivelSigilo(),
                ProntuarioNacionalEntrada.OrigemRegistro.SINCRONIZACAO_NACIONAL,
                evento.eventoId(),
                evento.ocorridoEm()
        ));
    }

    private void registrarSeValido(RegistroEntradaCommand command) {
        try {
            if (command.documento() == null || command.documento().isBlank()) {
                return;
            }
            registrarEntrada(command);
        } catch (Exception ex) {
            log.warn("Falha ao registrar entrada no prontuario nacional. nupn={} documento={} motivo={}",
                    command.nupn(), command.documento(), ex.getMessage());
        }
    }

    private void registrarRepresentacaoUsuario(Processo processo,
                                               String nupn,
                                               String tribunalCodigo,
                                               String tribunalOrigemUri,
                                               StatusProcesso status,
                                               Instant ocorridoEm) {
        Usuario usuario = processo.getUsuario();
        if (usuario == null || usuario.getCpf() == null || usuario.getCpf().isBlank()) {
            return;
        }
        TipoUsuario tipoUsuario = usuario.getTipoUsuario();
        if (tipoUsuario == null) {
            return;
        }
        ProntuarioNacionalEntrada.QualificacaoProcessual qualificacao = mapearQualificacaoRepresentacao(tipoUsuario);
        if (qualificacao == null) {
            return;
        }
        registrarSeValido(new RegistroEntradaCommand(
                usuario.getCpf(),
                usuario.getNome(),
                nupn,
                processo.getId(),
                tribunalCodigo,
                tribunalOrigemUri,
                ProntuarioNacionalEntrada.PoloProcessual.REPRESENTANTE,
                qualificacao,
                processo.getRamoDireito(),
                status,
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getNivelSigilo(),
                ProntuarioNacionalEntrada.OrigemRegistro.AJUIZAMENTO_LOCAL,
                "processo-representante:" + processo.getId() + ":" + usuario.getId(),
                ocorridoEm
        ));
    }

    private void publicarEventoRegistro(ProntuarioNacionalEntrada entrada) {
        EventoProcessoRegistrado payload = EventoProcessoRegistrado.of(entrada);
        outbox.enqueue(
                "prontuario:documento:" + entrada.getDocumentoHash(),
                EVT_PRONTUARIO_ENTRADA_REGISTRADA,
                payload,
                Map.of("source", "prontuario_nacional", "topic", TOPIC_PRONTUARIO_NACIONAL),
                "prontuarioEntrada:" + entrada.getDocumentoHash() + ":" + entrada.getNupn() + ":" + entrada.getPolo().name() + ":" + entrada.getQualificacao().name(),
                "ProntuarioNacional",
                entrada.getId().toString()
        );
    }

    private static Map<String, ProntuarioNacionalEntrada> latestByNupn(Collection<ProntuarioNacionalEntrada> entradas) {
        Map<String, ProntuarioNacionalEntrada> mapa = new LinkedHashMap<>();
        List<ProntuarioNacionalEntrada> ordenadas = entradas.stream()
                .sorted(Comparator.comparing(ProntuarioNacionalEntrada::getOcorridoEm, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ProntuarioNacionalEntrada::getAtualizadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        for (ProntuarioNacionalEntrada entrada : ordenadas) {
            mapa.putIfAbsent(entrada.getNupn(), entrada);
        }
        return mapa;
    }

    private Set<IdentidadeJuridicaNacional.PapelNacional> mapearPapeis(ProntuarioNacionalEntrada.PoloProcessual polo,
                                                                       ProntuarioNacionalEntrada.QualificacaoProcessual qualificacao) {
        Set<IdentidadeJuridicaNacional.PapelNacional> papeis = EnumSet.of(IdentidadeJuridicaNacional.PapelNacional.SUJEITO_PROCESSUAL);
        switch (polo) {
            case ATIVO, PASSIVO, INTERESSADO, TERCEIRO -> papeis.add(IdentidadeJuridicaNacional.PapelNacional.PARTE);
            case REPRESENTANTE -> papeis.add(IdentidadeJuridicaNacional.PapelNacional.REPRESENTANTE_LEGAL);
            case AUXILIAR_JUSTICA -> papeis.add(IdentidadeJuridicaNacional.PapelNacional.PERITO);
        }
        switch (qualificacao) {
            case ADVOGADO -> {
                papeis.add(IdentidadeJuridicaNacional.PapelNacional.ADVOGADO);
                papeis.add(IdentidadeJuridicaNacional.PapelNacional.REPRESENTANTE_LEGAL);
            }
            case MEMBRO_MP -> papeis.add(IdentidadeJuridicaNacional.PapelNacional.MEMBRO_MP);
            case DEFENSOR_PUBLICO -> papeis.add(IdentidadeJuridicaNacional.PapelNacional.DEFENSOR_PUBLICO);
            case PROCURADOR_PUBLICO -> {
                papeis.add(IdentidadeJuridicaNacional.PapelNacional.PROCURADOR_PUBLICO);
                papeis.add(IdentidadeJuridicaNacional.PapelNacional.REPRESENTANTE_LEGAL);
            }
            case PERITO -> papeis.add(IdentidadeJuridicaNacional.PapelNacional.PERITO);
            case REPRESENTANTE_LEGAL -> papeis.add(IdentidadeJuridicaNacional.PapelNacional.REPRESENTANTE_LEGAL);
            default -> {
            }
        }
        return papeis;
    }

    private static ProntuarioNacionalEntrada.QualificacaoProcessual mapearQualificacaoRepresentacao(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return null;
        }
        if (tipoUsuario.isAdvocacia() || tipoUsuario == TipoUsuario.OAB_PRESIDENTE_SECCIONAL) {
            return ProntuarioNacionalEntrada.QualificacaoProcessual.ADVOGADO;
        }
        if (tipoUsuario.isDefensoriaPublica()) {
            return ProntuarioNacionalEntrada.QualificacaoProcessual.DEFENSOR_PUBLICO;
        }
        if (tipoUsuario.isProcuradoria()) {
            return ProntuarioNacionalEntrada.QualificacaoProcessual.PROCURADOR_PUBLICO;
        }
        if (tipoUsuario.isMinisterioPublico()) {
            return ProntuarioNacionalEntrada.QualificacaoProcessual.MEMBRO_MP;
        }
        if (tipoUsuario.isPerito() || tipoUsuario.isAuxiliarJustica()) {
            return ProntuarioNacionalEntrada.QualificacaoProcessual.PERITO;
        }
        return null;
    }

    private String resolverTribunalCodigo(Processo processo) {
        if (processo.getJurisdicao() != null && processo.getJurisdicao().getSigla() != null && !processo.getJurisdicao().getSigla().isBlank()) {
            return normalizarTribunalCodigo(processo.getJurisdicao().getSigla());
        }
        TipoJustica tipoJustica = processo.getTipoJustica();
        String uf = processo.getJurisdicao() != null ? processo.getJurisdicao().getEstado() : null;
        if (tipoJustica == null) {
            return "PJB";
        }
        return switch (tipoJustica) {
            case ESTADUAL -> uf != null && !uf.isBlank() ? "TJ" + uf.trim().toUpperCase(Locale.ROOT) : "TJBR";
            case FEDERAL -> "JFBR";
            case ELEITORAL -> "TRE" + (uf != null && !uf.isBlank() ? uf.trim().toUpperCase(Locale.ROOT) : "BR");
            case MILITAR_ESTADUAL -> uf != null && !uf.isBlank() ? "TJM" + uf.trim().toUpperCase(Locale.ROOT) : "TJMBR";
            case MILITAR_FEDERAL -> "STM";
            case TRABALHO -> "TRT" + (uf != null && !uf.isBlank() ? uf.trim().toUpperCase(Locale.ROOT) : "BR");
            case SUPERIOR -> "STJ";
        };
    }

    private static String construirTribunalOrigemUri(Processo processo, String tribunalCodigo, String nupn) {
        if (processo.getId() == null) {
            return "pjb://tribunal/" + tribunalCodigo + "/processo/" + nupn;
        }
        return "pjb://tribunal/" + tribunalCodigo + "/processo-local/" + processo.getId() + "/" + nupn;
    }

    private static String resolverNupn(Processo processo) {
        String numero = processo.getNumeroUnificado();
        if (numero != null && !numero.isBlank()) {
            return numero.trim();
        }
        numero = processo.getNumeroProcesso();
        return numero == null || numero.isBlank() ? null : numero.trim();
    }

    private static String normalizarTribunalCodigo(String valor) {
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }

    public record RegistroEntradaCommand(
            String documento,
            String nomeSujeito,
            String nupn,
            Long processoLocalId,
            String tribunalCodigo,
            String tribunalOrigemUri,
            ProntuarioNacionalEntrada.PoloProcessual polo,
            ProntuarioNacionalEntrada.QualificacaoProcessual qualificacao,
            RamoDireito ramoDireito,
            StatusProcesso statusProcesso,
            String classeProcessual,
            String assunto,
            com.tcc.pjb.backend.model.entity.enums.NivelSigilo nivelSigilo,
            ProntuarioNacionalEntrada.OrigemRegistro origemRegistro,
            String fonteEventoId,
            Instant ocorridoEm
    ) {
    }

    public record EntradaProntuarioView(
            UUID entradaId,
            String documentoMascarado,
            String nomeSujeito,
            String nupn,
            Long processoLocalId,
            String tribunalCodigo,
            String polo,
            String qualificacao,
            String ramoDireito,
            String statusProcesso,
            String classeProcessual,
            String assunto,
            String nivelSigilo,
            Instant registradoEm,
            Instant atualizadoEm,
            String tribunalOrigemUri
    ) {
        public static EntradaProntuarioView of(ProntuarioNacionalEntrada entrada, String documentoMascarado) {
            return new EntradaProntuarioView(
                    entrada.getId(),
                    documentoMascarado,
                    entrada.getNomeSujeito(),
                    entrada.getNupn(),
                    entrada.getProcessoLocalId(),
                    entrada.getTribunalCodigo(),
                    entrada.getPolo().name(),
                    entrada.getQualificacao().name(),
                    entrada.getRamoDireito() != null ? entrada.getRamoDireito().name() : null,
                    entrada.getStatusProcesso().name(),
                    entrada.getClasseProcessual(),
                    entrada.getAssunto(),
                    entrada.getNivelSigilo() != null ? entrada.getNivelSigilo().name() : null,
                    entrada.getRegistradoEm(),
                    entrada.getAtualizadoEm(),
                    entrada.getTribunalOrigemUri()
            );
        }
    }

    public record ProntuarioNacionalView(
            UUID identidadeId,
            String documentoMascarado,
            String nomeCanonico,
            int totalProcessos,
            int processosAtivos,
            int processosArquivados,
            int tribunaisDistintos,
            List<EntradaProntuarioView> entradas,
            Instant consultadoEm,
            boolean dadosParciais,
            boolean govBrVinculado,
            String prontuarioNacionalUri
    ) {
        public static ProntuarioNacionalView vazio(String documentoMascarado) {
            return new ProntuarioNacionalView(null, documentoMascarado, "NAO INFORMADO", 0, 0, 0, 0, List.of(), Instant.now(), false, false, null);
        }

        public List<EntradaProntuarioView> comoAtivo() {
            return entradas.stream().filter(e -> "ATIVO".equalsIgnoreCase(e.polo())).toList();
        }

        public List<EntradaProntuarioView> comoPassivo() {
            return entradas.stream().filter(e -> "PASSIVO".equalsIgnoreCase(e.polo())).toList();
        }

        public Map<String, List<EntradaProntuarioView>> porTribunal() {
            return entradas.stream().collect(Collectors.groupingBy(EntradaProntuarioView::tribunalCodigo, LinkedHashMap::new, Collectors.toList()));
        }

        public boolean possuiCoisaJulgada(RamoDireito ramoDireito) {
            if (ramoDireito == null) {
                return false;
            }
            return entradas.stream().anyMatch(e -> "ARQUIVADO".equalsIgnoreCase(e.statusProcesso()) && ramoDireito.name().equalsIgnoreCase(e.ramoDireito()));
        }
    }

    public record AnaliseConflitoProcessual(
            boolean litispendenciaPotencial,
            boolean coisaJulgadaPotencial,
            List<String> nupnsEmAndamento,
            List<String> nupnsArquivados,
            int totalCoincidencias,
            Instant analisadoEm
    ) {
    }

    public record EventoProcessoRegistrado(
            String eventoId,
            String documento,
            String nomeSujeito,
            String nupn,
            Long processoLocalId,
            String tribunalCodigo,
            ProntuarioNacionalEntrada.PoloProcessual polo,
            ProntuarioNacionalEntrada.QualificacaoProcessual qualificacao,
            RamoDireito ramoDireito,
            StatusProcesso statusProcesso,
            String classeProcessual,
            String assunto,
            com.tcc.pjb.backend.model.entity.enums.NivelSigilo nivelSigilo,
            String tribunalOrigemUri,
            Instant ocorridoEm,
            String topic
    ) {
        public static EventoProcessoRegistrado of(ProntuarioNacionalEntrada entrada) {
            return new EventoProcessoRegistrado(
                    UUID.randomUUID().toString(),
                    entrada.getDocumento(),
                    entrada.getNomeSujeito(),
                    entrada.getNupn(),
                    entrada.getProcessoLocalId(),
                    entrada.getTribunalCodigo(),
                    entrada.getPolo(),
                    entrada.getQualificacao(),
                    entrada.getRamoDireito(),
                    entrada.getStatusProcesso(),
                    entrada.getClasseProcessual(),
                    entrada.getAssunto(),
                    entrada.getNivelSigilo(),
                    entrada.getTribunalOrigemUri(),
                    Instant.now(),
                    TOPIC_PRONTUARIO_NACIONAL
            );
        }
    }
}
