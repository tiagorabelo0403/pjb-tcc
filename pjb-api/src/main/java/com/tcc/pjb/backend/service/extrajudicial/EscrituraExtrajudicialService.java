package com.tcc.pjb.backend.service.extrajudicial;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.extrajudicial.EscrituraExtrajudicialRegistro;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.EscrituraExtrajudicialRegistroRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class EscrituraExtrajudicialService {

    private final EscrituraExtrajudicialRegistroRepository repository;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final CurrentUserService currentUserService;

    public EscrituraExtrajudicialService(EscrituraExtrajudicialRegistroRepository repository,
                                         ProcessoRepository processoRepository,
                                         WorkItemRepository workItemRepository,
                                         CurrentUserService currentUserService) {
        this.repository = Objects.requireNonNull(repository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @Transactional(readOnly = true)
    public List<EscrituraView> minhasEscrituras() {
        Usuario usuario = requireCartorio();
        return repository.findTop50ByCartorioResponsavel_IdOrderByLavradaEmDesc(usuario.getId()).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<EscrituraView> listarPorProcesso(Long processoId) {
        requireCartorio();
        return repository.findTop50ByProcessoVinculado_IdOrderByLavradaEmDesc(processoId).stream().map(this::toView).toList();
    }

    @Transactional
    public EscrituraView lavrar(LavraturaRequest request) {
        Usuario usuario = requireCartorio();
        EscrituraExtrajudicialRegistro registro = new EscrituraExtrajudicialRegistro();
        registro.setProtocolo(gerarProtocolo(request.tipo(), usuario));
        registro.setTipo(normalizeUpper(request.tipo()));
        registro.setStatus("LAVRADA");
        registro.setAtoResumo(request.atoResumo());
        registro.setPartesResumo(request.partesResumo());
        registro.setBensResumo(request.bensResumo());
        registro.setValorDeclarado(request.valorDeclarado());
        registro.setComarca(usuario.getComarca());
        registro.setUf(usuario.getUf());
        registro.setCartorioResponsavel(usuario);
        registro.setAssinaturaHash(Hashes.sha256Hex(request.tipo() + "|" + request.atoResumo() + "|" + request.partesResumo() + "|" + Instant.now()));
        registro.setLavradaEm(Instant.now());
        EscrituraExtrajudicialRegistro salvo = repository.save(registro);
        return toView(salvo);
    }

    @Transactional
    public EscrituraView vincularProcesso(Long escrituraId, Long processoId, VinculacaoProcessoRequest request) {
        Usuario usuario = requireCartorio();
        EscrituraExtrajudicialRegistro registro = repository.findById(escrituraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("EscrituraExtrajudicial", escrituraId));
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        registro.setProcessoVinculado(processo);
        registro.setStatus("VINCULADA_JUDICIALMENTE");
        registro.setVinculadaEm(Instant.now());
        repository.save(registro);

        String templateCode = "ESCRITURA_VINCULADA:" + registro.getProtocolo() + ":" + processo.getId();
        if (workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processo.getId(), templateCode, WorkItemStatus.CANCELADO).isEmpty()) {
            WorkItem item = WorkItem.builder()
                    .processo(processo)
                    .faseOrigem(processo.getFaseAtual())
                    .templateCode(templateCode)
                    .type(WorkItemType.CERTIDAO)
                    .titulo("Escritura extrajudicial vinculada — " + registro.getTipo() + " — " + processo.getNumeroProcesso())
                    .descricao(request.observacaoVinculacao())
                    .queueCode("EXTRAJUDICIAL_VINCULACAO_JUDICIAL")
                    .inboxKey("SECRETARIA_VALIDAR_VINCULO_EXTRAJUDICIAL")
                    .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                    .status(WorkItemStatus.PENDENTE)
                    .prioridade(1)
                    .uf(usuario.getUf())
                    .comarca(usuario.getComarca())
                    .dueAt(Instant.now().plus(24, ChronoUnit.HOURS))
                    .baseLegal("Vinculo extrajudicial-jurisdicional controlado pelo PJB")
                    .build();
            workItemRepository.save(item);
        }
        return toView(registro);
    }

    private Usuario requireCartorio() {
        Usuario usuario = currentUserService.getRequired();
        if (usuario.getTipoUsuario() == null || !usuario.getTipoUsuario().isCartorioExtrajudicial()) {
            throw new IllegalStateException("Operacao exclusiva de cartorio extrajudicial.");
        }
        return usuario;
    }

    private EscrituraView toView(EscrituraExtrajudicialRegistro registro) {
        return new EscrituraView(
                registro.getId(),
                registro.getProtocolo(),
                registro.getTipo(),
                registro.getStatus(),
                registro.getAtoResumo(),
                registro.getPartesResumo(),
                registro.getBensResumo(),
                registro.getValorDeclarado(),
                registro.getUf(),
                registro.getComarca(),
                registro.getCartorioResponsavel() != null ? registro.getCartorioResponsavel().getId() : null,
                registro.getCartorioResponsavel() != null ? registro.getCartorioResponsavel().getNome() : null,
                registro.getProcessoVinculado() != null ? registro.getProcessoVinculado().getId() : null,
                registro.getProcessoVinculado() != null ? registro.getProcessoVinculado().getNumeroProcesso() : null,
                registro.getAssinaturaHash(),
                registro.getLavradaEm(),
                registro.getVinculadaEm()
        );
    }

    private String gerarProtocolo(String tipo, Usuario usuario) {
        String base = normalizeUpper(tipo) + ":" + usuario.getId() + ":" + Instant.now().toEpochMilli();
        String hash = Hashes.sha256Hex(base).substring(0, 12).toUpperCase(Locale.ROOT);
        return normalizeUpper(tipo).replace(' ', '_') + "-" + hash;
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public record LavraturaRequest(
            String tipo,
            String atoResumo,
            String partesResumo,
            String bensResumo,
            BigDecimal valorDeclarado
    ) {
    }

    public record VinculacaoProcessoRequest(String observacaoVinculacao) {
    }

    public record EscrituraView(
            Long id,
            String protocolo,
            String tipo,
            String status,
            String atoResumo,
            String partesResumo,
            String bensResumo,
            BigDecimal valorDeclarado,
            String uf,
            String comarca,
            Long cartorioResponsavelId,
            String cartorioResponsavelNome,
            Long processoVinculadoId,
            String processoVinculadoNumero,
            String assinaturaHash,
            Instant lavradaEm,
            Instant vinculadaEm
    ) {
    }
}
