package com.tcc.pjb.backend.service.cidadao;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.cidadao.AreaLinks;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPastaProcessosResponse;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoCardDto;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.core.util.EnumText;
import java.util.Locale;

@Service
public class CidadaoPastaProcessosService {

  private final ProcessoRepository processos;
  private final MovimentacaoProcessualRepository movRepo;
  private final DocumentoProcessualRepository docRepo;
  private final AudienciaRepository audienciaRepo;
  private final JulgamentoColegiadoRepository julgamentoRepo;
  private final CurrentUserService currentUser;
  private final CidadaoProcessoCardMapper cardMapper;

  public CidadaoPastaProcessosService(
      ProcessoRepository processos,
      MovimentacaoProcessualRepository movRepo,
      DocumentoProcessualRepository docRepo,
      AudienciaRepository audienciaRepo,
      JulgamentoColegiadoRepository julgamentoRepo,
      CurrentUserService currentUser,
      CidadaoProcessoCardMapper cardMapper
  ) {
    this.processos = Objects.requireNonNull(processos);
    this.movRepo = Objects.requireNonNull(movRepo);
    this.docRepo = Objects.requireNonNull(docRepo);
    this.audienciaRepo = Objects.requireNonNull(audienciaRepo);
    this.julgamentoRepo = Objects.requireNonNull(julgamentoRepo);
    this.currentUser = Objects.requireNonNull(currentUser);
    this.cardMapper = Objects.requireNonNull(cardMapper);
  }

  public CidadaoPastaProcessosResponse listar(Pageable pageable) {
    return listar(pageable, null, null, null);
  }

  


  public CidadaoPastaProcessosResponse listar(Pageable pageable, String numero, String uf, String statusRaw) {
    Usuario u = currentUser.getRequired();
    String cpf = u.getCpf();
    if (cpf == null || cpf.isBlank()) {
      return empty(pageable);
    }

    String ufNorm = normalizeUf(uf);
    StatusProcesso st = parseStatus(statusRaw);
    Page<Processo> page = processos.searchCidadao(cpf, numero, ufNorm, st, pageable);
    List<Processo> content = page.getContent();

    if (content.isEmpty()) {
      return new CidadaoPastaProcessosResponse(
          page.getNumber(),
          page.getSize(),
          page.getTotalElements(),
          page.getTotalPages(),
          List.of(),
          "/api/v1/ui/legend",
          defaultLinks()
      );
    }

    List<Long> ids = content.stream().map(Processo::getId).filter(Objects::nonNull).toList();

    Map<Long, MovimentacaoProcessual> lastMov = new HashMap<>();
    for (MovimentacaoProcessual m : movRepo.findLatestByProcessoIds(ids)) {
      if (m != null && m.getProcesso() != null && m.getProcesso().getId() != null) {
        lastMov.put(m.getProcesso().getId(), m);
      }
    }

    Map<Long, Long> docCount = new HashMap<>();
    for (DocumentoProcessualRepository.ProcessoDocCount row : docRepo.countDocsByProcessoIds(ids)) {
      if (row != null && row.getProcessoId() != null) {
        docCount.put(row.getProcessoId(), row.getCnt());
      }
    }

    long[] idArray = ids.stream().mapToLong(Long::longValue).toArray();
    Map<Long, Audiencia> nextAud = new HashMap<>();
    for (Audiencia a : audienciaRepo.findNextUpcomingByProcessoIds(idArray, LocalDateTime.now())) {
      if (a != null && a.getProcesso() != null && a.getProcesso().getId() != null) {
        nextAud.put(a.getProcesso().getId(), a);
      }
    }

    Map<Long, JulgamentoColegiado> nextJulg = new HashMap<>();
    for (JulgamentoColegiado j : julgamentoRepo.findNextPautaByProcessoIds(idArray, LocalDateTime.now())) {
      if (j != null && j.getProcesso() != null && j.getProcesso().getId() != null) {
        nextJulg.put(j.getProcesso().getId(), j);
      }
    }

    List<CidadaoProcessoCardDto> cards = content.stream()
        .map(p -> cardMapper.toCard(
                p,
                lastMov.get(p.getId()),
                docCount.getOrDefault(p.getId(), 0L),
                nextAud.get(p.getId()),
                nextJulg.get(p.getId())
        ))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    return new CidadaoPastaProcessosResponse(
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        cards,
        "/api/v1/ui/legend",
        defaultLinks()
    );
  }

  private CidadaoPastaProcessosResponse empty(Pageable pageable) {
    return new CidadaoPastaProcessosResponse(
        pageable.getPageNumber(),
        pageable.getPageSize(),
        0,
        0,
        List.of(),
        "/api/v1/ui/legend",
        defaultLinks()
    );
  }

  private static AreaLinks defaultLinks() {
    return new AreaLinks(
        "/api/v1/ui/legend",
        "/api/v1/ui/accessibility/preference",
        "/api/v1/ui/presentation/reading-preference",
        "/api/v1/ui/presentation/bundle",
        "/api/v1/atendimento/threads",
        "/api/v1/atendimento/processos/{processoId}/threads"
    );
  }

  private static String normalizeUf(String uf) {
    if (uf == null || uf.isBlank()) return null;
    String u = uf.trim().toUpperCase(Locale.ROOT);
    return (u.length() == 2) ? u : null;
  }

  private static StatusProcesso parseStatus(String raw) {
    if (raw == null || raw.isBlank()) return null;
    String token = EnumText.normalizeToken(raw);
    if (token.isBlank()) return null;
    
    return switch (token) {
      case "EM_TRAMITE", "TRAMITANDO" -> StatusProcesso.EM_ANDAMENTO;
      case "SENTENCIADO", "SENTENCA" -> StatusProcesso.SENTENCA_PROFERIDA;
      case "TRANSITO" -> StatusProcesso.TRANSITO_EM_JULGADO;
      case "BAIXA" -> StatusProcesso.BAIXADO;
      case "ARQUIVAMENTO" -> StatusProcesso.ARQUIVADO;
      case "SUSPENSO" -> StatusProcesso.SUSPENSO_POR_OBITO;
      default -> {
        try {
          yield StatusProcesso.valueOf(token);
        } catch (Exception e) {
          yield null;
        }
      }
    };
  }
}
