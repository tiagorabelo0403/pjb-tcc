package com.tcc.pjb.backend.service.criminal;

import com.tcc.pjb.backend.core.id.PjbUuidV7Generator;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.criminal.BoletimOcorrenciaDigital;
import com.tcc.pjb.backend.model.entity.criminal.BoletimOcorrenciaInqueritoVinculo;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import com.tcc.pjb.backend.model.entity.enums.StatusBoletimOcorrenciaDigital;
import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.BoletimOcorrenciaDigitalRepository;
import com.tcc.pjb.backend.model.repository.BoletimOcorrenciaInqueritoVinculoRepository;
import com.tcc.pjb.backend.model.repository.InqueritoPolicialDigitalRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoletimOcorrenciaDigitalService {

    private static final String AGGREGATE_TYPE = "BOLETIM_OCORRENCIA_DIGITAL";
    private static final String EVENTO_REGISTRADO = "BOLETIM_OCORRENCIA_REGISTRADO";
    private static final String EVENTO_VINCULADO_INQUERITO = "BOLETIM_OCORRENCIA_VINCULADO_INQUERITO";

    private final BoletimOcorrenciaDigitalRepository repository;
    private final BoletimOcorrenciaInqueritoVinculoRepository vinculoRepository;
    private final UnidadeInstituicaoRepository unidadeInstituicaoRepository;
    private final LotacaoInstituicaoRepository lotacaoInstituicaoRepository;
    private final InqueritoPolicialDigitalRepository inqueritoRepository;
    private final CurrentUserService currentUserService;
    private final OutboxPublisher outboxPublisher;

    public BoletimOcorrenciaDigitalService(BoletimOcorrenciaDigitalRepository repository,
                                           BoletimOcorrenciaInqueritoVinculoRepository vinculoRepository,
                                           UnidadeInstituicaoRepository unidadeInstituicaoRepository,
                                           LotacaoInstituicaoRepository lotacaoInstituicaoRepository,
                                           InqueritoPolicialDigitalRepository inqueritoRepository,
                                           CurrentUserService currentUserService,
                                           OutboxPublisher outboxPublisher) {
        this.repository = Objects.requireNonNull(repository);
        this.vinculoRepository = Objects.requireNonNull(vinculoRepository);
        this.unidadeInstituicaoRepository = Objects.requireNonNull(unidadeInstituicaoRepository);
        this.lotacaoInstituicaoRepository = Objects.requireNonNull(lotacaoInstituicaoRepository);
        this.inqueritoRepository = Objects.requireNonNull(inqueritoRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
    }

    @Transactional(readOnly = true)
    public List<BoletimOcorrenciaView> listarMeus() {
        Usuario usuario = requireSegurancaPublica();
        List<Long> unidadeIds = delegaciasAtivasDoUsuario(usuario).stream()
                .map(UnidadeInstituicao::getId)
                .filter(Objects::nonNull)
                .toList();
        if (unidadeIds.isEmpty()) {
            return List.of();
        }
        return repository.findTop100ByUnidadeRegistro_IdInOrderByUpdatedAtDesc(unidadeIds).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public BoletimOcorrenciaView buscar(UUID uuid) {
        Usuario usuario = requireSegurancaPublica();
        BoletimOcorrenciaDigital boletim = repository.findByUuid(uuid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("BoletimOcorrenciaDigital", uuid));
        requireLotacaoAtivaNaUnidade(usuario, boletim.getUnidadeRegistro());
        return toView(boletim);
    }

    @Transactional
    public BoletimOcorrenciaView registrar(BoletimOcorrenciaCadastroCommand request) {
        Objects.requireNonNull(request);
        Usuario usuario = requireSegurancaPublica();
        UnidadeInstituicao unidade = requireDelegaciaLotada(usuario, request.unidadeRegistroId());
        UUID uuid = PjbUuidV7Generator.generate();
        Instant now = Instant.now();
        BoletimOcorrenciaDigital boletim = new BoletimOcorrenciaDigital();
        boletim.setUuid(uuid);
        boletim.setNumeroBoletim(numeroBoletim(uuid));
        boletim.setStatus(StatusBoletimOcorrenciaDigital.REGISTRADO);
        boletim.setNaturezaFato(textoObrigatorio(request.naturezaFato(), "naturezaFato"));
        boletim.setResumoFatos(textoObrigatorio(request.resumoFatos(), "resumoFatos"));
        boletim.setLocalFato(textoObrigatorio(request.localFato(), "localFato"));
        boletim.setOcorridoEm(Objects.requireNonNull(request.ocorridoEm(), "ocorridoEm"));
        boletim.setComunicanteResumo(textoObrigatorio(request.comunicanteResumo(), "comunicanteResumo"));
        boletim.setEnvolvidosResumo(textoLivre(request.envolvidosResumo()));
        boletim.setProvidenciasIniciais(textoObrigatorio(request.providenciasIniciais(), "providenciasIniciais"));
        boletim.setUnidadeRegistro(unidade);
        boletim.setRegistradoPor(usuario);
        boletim.setRegistradoEm(now);
        boletim.setUpdatedAt(now);
        boletim.setCadeiaCustodiaHash(custodyHash(boletim));
        BoletimOcorrenciaDigital salvo = repository.save(boletim);
        BoletimOcorrenciaView view = toView(salvo, List.of());
        publicarEvento(salvo, EVENTO_REGISTRADO, view);
        return view;
    }

    @Transactional
    public BoletimOcorrenciaView vincularInquerito(UUID boletimUuid, Long inqueritoId) {
        Usuario usuario = requireSegurancaPublica();
        BoletimOcorrenciaDigital boletim = repository.findByUuid(boletimUuid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("BoletimOcorrenciaDigital", boletimUuid));
        requireLotacaoAtivaNaUnidade(usuario, boletim.getUnidadeRegistro());
        InqueritoPolicialDigital inquerito = inqueritoRepository.findById(inqueritoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("InqueritoPolicialDigital", inqueritoId));
        requireMesmoRegistroInstitucional(boletim, inquerito);
        var existente = vinculoRepository.findByBoletim_Id(boletim.getId());
        if (existente.isPresent()) {
            BoletimOcorrenciaInqueritoVinculo vinculoExistente = existente.get();
            if (Objects.equals(vinculoExistente.getInquerito().getId(), inquerito.getId())) {
                return toView(boletim, List.of(vinculoExistente));
            }
            throw new IllegalStateException("Boletim ja possui vinculo com outro inquerito.");
        }
        boletim.setStatus(StatusBoletimOcorrenciaDigital.VINCULADO_INQUERITO);
        boletim.setUpdatedAt(Instant.now());
        boletim.setCadeiaCustodiaHash(custodyHash(boletim));
        BoletimOcorrenciaInqueritoVinculo vinculo = new BoletimOcorrenciaInqueritoVinculo();
        vinculo.setBoletim(boletim);
        vinculo.setInquerito(inquerito);
        vinculo.setVinculadoPor(usuario);
        vinculo.setVinculadoEm(Instant.now());
        vinculo.setCadeiaCustodiaHash(vinculoHash(boletim, inquerito, usuario));
        BoletimOcorrenciaInqueritoVinculo vinculoSalvo = vinculoRepository.save(vinculo);
        BoletimOcorrenciaDigital salvo = repository.save(boletim);
        BoletimOcorrenciaView view = toView(salvo, List.of(vinculoSalvo));
        publicarEvento(salvo, EVENTO_VINCULADO_INQUERITO, view);
        return view;
    }

    @Transactional(readOnly = true)
    public List<String> resumosPainel(Usuario usuario, int limit) {
        if (usuario == null || usuario.getTipoUsuario() == null || !usuario.getTipoUsuario().isSegurancaPublica()) {
            return List.of();
        }
        List<Long> unidadeIds = delegaciasAtivasDoUsuario(usuario).stream()
                .map(UnidadeInstituicao::getId)
                .filter(Objects::nonNull)
                .toList();
        if (unidadeIds.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return repository.findTop100ByUnidadeRegistro_IdInOrderByUpdatedAtDesc(unidadeIds).stream()
                .limit(safeLimit)
                .map(this::resumoPainel)
                .toList();
    }

    private Usuario requireSegurancaPublica() {
        Usuario usuario = currentUserService.getRequired();
        if (usuario.getTipoUsuario() == null || !usuario.getTipoUsuario().isSegurancaPublica()) {
            throw new IllegalStateException("Operacao exclusiva de autoridade policial ou equipe investigativa.");
        }
        return usuario;
    }

    private UnidadeInstituicao requireDelegaciaLotada(Usuario usuario, Long unidadeId) {
        if (unidadeId == null) {
            throw new IllegalArgumentException("unidadeRegistroId e obrigatorio para boletim de ocorrencia digital.");
        }
        UnidadeInstituicao unidade = unidadeInstituicaoRepository.findById(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UnidadeInstituicao", unidadeId));
        requireDelegaciaAtiva(unidade);
        requireLotacaoAtivaNaUnidade(usuario, unidade);
        return unidade;
    }

    private void requireDelegaciaAtiva(UnidadeInstituicao unidade) {
        if (unidade == null
                || unidade.getTipo() != TipoUnidadeInstitucional.DELEGACIA
                || unidade.getStatusUnidade() != StatusUnidadeInstitucional.ATIVA
                || !hasText(unidade.getUf())
                || !hasText(unidade.getComarca())) {
            throw new IllegalStateException("A unidade de registro deve ser uma delegacia institucional ativa com UF e comarca.");
        }
    }

    private void requireLotacaoAtivaNaUnidade(Usuario usuario, UnidadeInstituicao unidade) {
        requireDelegaciaAtiva(unidade);
        boolean lotado = delegaciasAtivasDoUsuario(usuario).stream()
                .anyMatch(ativa -> Objects.equals(ativa.getId(), unidade.getId()));
        if (!lotado) {
            throw new IllegalStateException("Usuario sem lotacao ativa na delegacia informada.");
        }
    }

    private List<UnidadeInstituicao> delegaciasAtivasDoUsuario(Usuario usuario) {
        return lotacaoInstituicaoRepository.findAtivasByUsuario(usuario).stream()
                .map(LotacaoInstituicao::getUnidade)
                .filter(Objects::nonNull)
                .filter(this::isDelegaciaAtiva)
                .toList();
    }

    private boolean isDelegaciaAtiva(UnidadeInstituicao unidade) {
        return unidade.getTipo() == TipoUnidadeInstitucional.DELEGACIA
                && unidade.getStatusUnidade() == StatusUnidadeInstitucional.ATIVA
                && hasText(unidade.getUf())
                && hasText(unidade.getComarca());
    }

    private void requireMesmoRegistroInstitucional(BoletimOcorrenciaDigital boletim, InqueritoPolicialDigital inquerito) {
        UnidadeInstituicao unidadeBoletim = boletim.getUnidadeRegistro();
        UnidadeInstituicao unidadeInquerito = inquerito.getUnidadeApuracao();
        if (unidadeBoletim == null || unidadeInquerito == null || !Objects.equals(unidadeBoletim.getId(), unidadeInquerito.getId())) {
            throw new IllegalStateException("Boletim e inquerito devem pertencer a mesma delegacia institucional.");
        }
    }

    private void publicarEvento(BoletimOcorrenciaDigital boletim, String eventCode, BoletimOcorrenciaView view) {
        outboxPublisher.enqueueTracked(
                "POLICIA_DELEGACIA:" + boletim.getUnidadeRegistro().getId(),
                eventCode,
                view,
                Map.of(
                        "canal", "API",
                        "unidadeRegistroId", boletim.getUnidadeRegistro().getId(),
                        "status", boletim.getStatus().name()
                ),
                "bo:" + eventCode + ":" + boletim.getUuid() + ":" + boletim.getCadeiaCustodiaHash(),
                AGGREGATE_TYPE,
                boletim.getUuid().toString()
        );
    }

    private String custodyHash(BoletimOcorrenciaDigital boletim) {
        String base = String.join("|",
                textoHash(boletim.getUuid()),
                textoHash(boletim.getNumeroBoletim()),
                textoHash(boletim.getStatus()),
                textoHash(boletim.getNaturezaFato()),
                textoHash(boletim.getResumoFatos()),
                textoHash(boletim.getLocalFato()),
                textoHash(boletim.getOcorridoEm()),
                textoHash(boletim.getComunicanteResumo()),
                textoHash(boletim.getEnvolvidosResumo()),
                textoHash(boletim.getProvidenciasIniciais()),
                idHash(boletim.getUnidadeRegistro()),
                boletim.getRegistradoPor() == null || boletim.getRegistradoPor().getId() == null ? "" : String.valueOf(boletim.getRegistradoPor().getId()));
        return Hashes.sha256HexBytes(base.getBytes(StandardCharsets.UTF_8));
    }

    private String vinculoHash(BoletimOcorrenciaDigital boletim, InqueritoPolicialDigital inquerito, Usuario usuario) {
        String base = String.join("|",
                textoHash(boletim.getUuid()),
                boletim.getId() == null ? "" : String.valueOf(boletim.getId()),
                inquerito.getId() == null ? "" : String.valueOf(inquerito.getId()),
                textoHash(inquerito.getNumeroProcedimento()),
                usuario.getId() == null ? "" : String.valueOf(usuario.getId()));
        return Hashes.sha256HexBytes(base.getBytes(StandardCharsets.UTF_8));
    }

    private String resumoPainel(BoletimOcorrenciaDigital boletim) {
        return "BO " + boletim.getNumeroBoletim()
                + " | " + boletim.getStatus().name()
                + " | " + boletim.getNaturezaFato()
                + " | " + boletim.getUnidadeRegistro().getNome();
    }

    private String numeroBoletim(UUID uuid) {
        String prefix = "BO-" + LocalDate.now().getYear() + "-";
        return prefix + uuid.toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private BoletimOcorrenciaView toView(BoletimOcorrenciaDigital boletim) {
        List<BoletimOcorrenciaInqueritoVinculo> vinculos = boletim.getId() == null
                ? List.of()
                : vinculoRepository.findByBoletim_Id(boletim.getId()).stream().toList();
        return toView(boletim, vinculos);
    }

    private BoletimOcorrenciaView toView(BoletimOcorrenciaDigital boletim, List<BoletimOcorrenciaInqueritoVinculo> vinculosOperacionais) {
        UnidadeInstituicao unidade = boletim.getUnidadeRegistro();
        Instituicao instituicao = unidade.getInstituicao();
        List<VinculoInqueritoView> vinculos = vinculosOperacionais.stream()
                .map(this::toVinculoView)
                .toList();
        return new BoletimOcorrenciaView(
                boletim.getUuid(),
                boletim.getNumeroBoletim(),
                boletim.getStatus(),
                boletim.getNaturezaFato(),
                boletim.getResumoFatos(),
                boletim.getLocalFato(),
                boletim.getOcorridoEm(),
                boletim.getComunicanteResumo(),
                textoLivre(boletim.getEnvolvidosResumo()),
                boletim.getProvidenciasIniciais(),
                unidade.getId(),
                unidade.getNome(),
                instituicao.getId(),
                instituicao.getNome(),
                boletim.getRegistradoPor().getId(),
                boletim.getRegistradoPor().getNome(),
                !vinculos.isEmpty(),
                vinculos,
                boletim.getCadeiaCustodiaHash(),
                boletim.getRegistradoEm(),
                boletim.getUpdatedAt()
        );
    }

    private VinculoInqueritoView toVinculoView(BoletimOcorrenciaInqueritoVinculo vinculo) {
        InqueritoPolicialDigital inquerito = vinculo.getInquerito();
        return new VinculoInqueritoView(
                inquerito.getId(),
                textoLivre(inquerito.getNumeroProcedimento()),
                textoLivre(inquerito.getStatus())
        );
    }

    private String textoObrigatorio(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " e obrigatorio.");
        }
        return value.trim();
    }

    private String textoLivre(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private String textoHash(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String idHash(UnidadeInstituicao unidade) {
        return unidade == null || unidade.getId() == null ? "" : String.valueOf(unidade.getId());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record BoletimOcorrenciaCadastroCommand(
            Long unidadeRegistroId,
            String naturezaFato,
            String resumoFatos,
            String localFato,
            Instant ocorridoEm,
            String comunicanteResumo,
            String envolvidosResumo,
            String providenciasIniciais
    ) {
    }

    public record BoletimOcorrenciaView(
            UUID uuid,
            String numeroBoletim,
            StatusBoletimOcorrenciaDigital status,
            String naturezaFato,
            String resumoFatos,
            String localFato,
            Instant ocorridoEm,
            String comunicanteResumo,
            String envolvidosResumo,
            String providenciasIniciais,
            Long unidadeRegistroId,
            String unidadeRegistroNome,
            Long instituicaoRegistroId,
            String instituicaoRegistroNome,
            Long registradoPorId,
            String registradoPorNome,
            boolean inqueritoVinculado,
            List<VinculoInqueritoView> vinculosInquerito,
            String cadeiaCustodiaHash,
            Instant registradoEm,
            Instant updatedAt
    ) {
    }

    public record VinculoInqueritoView(
            Long inqueritoId,
            String numeroProcedimento,
            String status
    ) {
    }
}
