package com.tcc.pjb.backend.service.julgamento;

import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.julgamento.Acordao;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.VotoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.StatusJulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.TipoVotoColegiado;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.AcordaoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.VotoColegiadoRepository;
import com.tcc.pjb.backend.service.julgamento.live.JulgamentoVotosLiveHub;

@Service
public class JulgamentoColegiadoService {

  private static final Logger log = LoggerFactory.getLogger(JulgamentoColegiadoService.class);

  private final JulgamentoColegiadoRepository julgamentoRepo;
  private final VotoColegiadoRepository votoRepo;
  private final AcordaoRepository acordaoRepo;
  private final ProcessoRepository processoRepo;
  private final JulgamentoVotosLiveHub liveHub;
  private final AuditLedgerService audit;
  private final ObjectMapper mapper;

  public JulgamentoColegiadoService(JulgamentoColegiadoRepository julgamentoRepo,
                                   VotoColegiadoRepository votoRepo,
                                   AcordaoRepository acordaoRepo,
                                   ProcessoRepository processoRepo,
                                   JulgamentoVotosLiveHub liveHub,
                                   AuditLedgerService audit,
                                   ObjectMapper mapper) {
    this.julgamentoRepo = Objects.requireNonNull(julgamentoRepo);
    this.votoRepo = Objects.requireNonNull(votoRepo);
    this.acordaoRepo = Objects.requireNonNull(acordaoRepo);
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.liveHub = Objects.requireNonNull(liveHub);
    this.audit = Objects.requireNonNull(audit);
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Transactional(readOnly = true)
  public List<JulgamentoColegiado> listByProcesso(Long processoId) {
    return julgamentoRepo.findByProcessoId(processoId);
  }

  @Transactional(readOnly = true)
  public JulgamentoColegiado getRequired(Long julgamentoId) {
    return julgamentoRepo.findByIdWithProcesso(julgamentoId)
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND, "julgamento nao encontrado"));
  }

  @Transactional(readOnly = true)
  public List<VotoColegiado> listVotos(Long julgamentoId) {
    return votoRepo.findByJulgamentoIdOrdered(julgamentoId);
  }

  @Transactional(readOnly = true)
  public Optional<Acordao> findAcordao(Long julgamentoId) {
    return acordaoRepo.findByJulgamentoId(julgamentoId);
  }

  


  @Transactional
  public VotoColegiado registrarVoto(Long julgamentoId,
                                    int ordem,
                                    String magistradoNome,
                                    String magistradoCargo,
                                    String papel,
                                    TipoVotoColegiado votoTipo,
                                    String votoResumo,
                                    String documentoRef) {

    JulgamentoColegiado j = julgamentoRepo.findByIdForUpdate(julgamentoId).orElse(getRequired(julgamentoId));

    VotoColegiado v = votoRepo.findByJulgamentoIdAndOrdem(julgamentoId, ordem)
        .orElse(VotoColegiado.builder().julgamento(j).ordem(ordem).build());

    v.setMagistradoNome(Objects.requireNonNullElse(magistradoNome, ""));
    v.setMagistradoCargo(magistradoCargo);
    v.setPapel(parsePapel(papel));
    v.setVotoTipo(Objects.requireNonNullElse(votoTipo, TipoVotoColegiado.OUTRO));
    v.setVotoResumo(votoResumo);
    v.setProferidoEm(LocalDateTime.now());
    v.setDocumentoRef(documentoRef);

    VotoColegiado saved = votoRepo.save(v);

    
    recomputePlacar(j);
    j.setStatus(Objects.requireNonNullElse(j.getStatus(), StatusJulgamentoColegiado.EM_ANDAMENTO));
    j.setSessaoInicio(j.getSessaoInicio() != null ? j.getSessaoInicio() : LocalDateTime.now());
    julgamentoRepo.save(j);

    audit.appendSafely("JULGAMENTO_VOTO_REGISTRADO", "julgamento=" + julgamentoId + ",ordem=" + ordem + ",tipo=" + saved.getVotoTipo().name());

    publishVoteEvent(j, saved);

    return saved;
  }

  @Transactional
  public Acordao publicarAcordao(Long julgamentoId, String numero, String ementaResumo, String inteiroTeorRef) {
    JulgamentoColegiado j = julgamentoRepo.findByIdForUpdate(julgamentoId).orElse(getRequired(julgamentoId));

    Acordao a = acordaoRepo.findByJulgamentoId(julgamentoId).orElse(null);
    if (a == null) {
      a = Acordao.builder().julgamento(j).build();
    }
    a.setNumeroAcordao(numero);
    a.setEmentaResumo(ementaResumo);
    a.setInteiroTeorRef(inteiroTeorRef);
    a.setPublicadoEm(LocalDateTime.now());
    Acordao saved = acordaoRepo.save(a);

    j.setAcordaoPublicado(Boolean.TRUE);
    j.setAcordaoPublicadoEm(saved.getPublicadoEm());
    j.setStatus(StatusJulgamentoColegiado.ENCERRADO);
    j.setSessaoFim(LocalDateTime.now());
    julgamentoRepo.save(j);

    audit.appendSafely("JULGAMENTO_ACORDAO_PUBLICADO", "julgamento=" + julgamentoId + ",acordao=" + numero);

    publishAcordaoEvent(j, saved);

    return saved;
  }


  @Transactional
  public JulgamentoColegiado criarJulgamento(Long processoId,
                                           com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao grau,
                                           String tribunalSigla,
                                           String orgaoJulgador,
                                           String relatorNome,
                                           String revisorNome,
                                           StatusJulgamentoColegiado status,
                                           LocalDateTime pautaDataHora) {
    Processo p = processoRepo.findById(processoId).orElseThrow(() ->
        new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "processo nao encontrado"));

    JulgamentoColegiado j = JulgamentoColegiado.builder()
        .processo(p)
        .grau(grau != null ? grau : (p.getJurisdicao() != null ? p.getJurisdicao().getGrau() : com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao.SEGUNDO_GRAU))
        .tribunalSigla(tribunalSigla)
        .orgaoJulgador(orgaoJulgador)
        .relatorNome(relatorNome)
        .revisorNome(revisorNome)
        .status(status != null ? status : StatusJulgamentoColegiado.AGENDADO)
        .pautaDataHora(pautaDataHora)
        .build();

    JulgamentoColegiado saved = julgamentoRepo.save(j);
    audit.appendSafely("JULGAMENTO_CRIADO", "processo=" + processoId + ",julgamento=" + saved.getId());
    return saved;
  }

  @Transactional
  public JulgamentoColegiado atualizarStatus(Long julgamentoId, StatusJulgamentoColegiado status) {
    JulgamentoColegiado j = julgamentoRepo.findByIdForUpdate(julgamentoId).orElse(getRequired(julgamentoId));
    if (status == null) {
      return j;
    }
    j.setStatus(status);
    if (status == StatusJulgamentoColegiado.EM_ANDAMENTO && j.getSessaoInicio() == null) {
      j.setSessaoInicio(LocalDateTime.now());
    }
    if (status == StatusJulgamentoColegiado.ENCERRADO && j.getSessaoFim() == null) {
      j.setSessaoFim(LocalDateTime.now());
    }
    JulgamentoColegiado saved = julgamentoRepo.save(j);

    audit.appendSafely("JULGAMENTO_STATUS_ATUALIZADO", "julgamento=" + julgamentoId + ",status=" + status.name());

    try {
      Map<String, Object> ev = new LinkedHashMap<>();
      ev.put("type", "STATUS_ATUALIZADO");
      ev.put("julgamentoId", saved.getId());
      ev.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);
      ev.put("at", LocalDateTime.now().toString());
      ev.put("placar", placarMap(saved));
      liveHub.publish(saved.getId(), mapper.writeValueAsString(ev));
    } catch (Exception e) {
      log.debug("falha ao publicar evento de julgamento criado id={}", j.getId(), e);
    }

    return saved;
  }

  private void publishVoteEvent(JulgamentoColegiado j, VotoColegiado v) {
    try {
      Map<String, Object> ev = new LinkedHashMap<>();
      ev.put("type", "VOTO_ADICIONADO");
      ev.put("julgamentoId", j.getId());
      ev.put("grau", j.getGrau() != null ? j.getGrau().name() : null);
      ev.put("tribunal", j.getTribunalSigla());
      ev.put("orgao", j.getOrgaoJulgador());
      ev.put("at", LocalDateTime.now().toString());
      ev.put("placar", placarMap(j));

      Map<String, Object> voto = new LinkedHashMap<>();
      voto.put("ordem", v.getOrdem());
      voto.put("magistrado", v.getMagistradoNome());
      voto.put("cargo", v.getMagistradoCargo());
      voto.put("papel", v.getPapel() != null ? v.getPapel().name() : null);
      voto.put("tipo", v.getVotoTipo() != null ? v.getVotoTipo().name() : null);
      voto.put("resumo", v.getVotoResumo());
      voto.put("proferidoEm", v.getProferidoEm() != null ? v.getProferidoEm().toString() : null);
      voto.put("documentoRef", v.getDocumentoRef());

      ev.put("voto", voto);

      liveHub.publish(j.getId(), mapper.writeValueAsString(ev));
    } catch (Exception e) {
      log.debug("falha ao publicar evento de voto julgamentoId={} votoId={}", j.getId(), v.getId(), e);
    }
  }

  private void publishAcordaoEvent(JulgamentoColegiado j, Acordao a) {
    try {
      Map<String, Object> ev = new LinkedHashMap<>();
      ev.put("type", "ACORDAO_PUBLICADO");
      ev.put("julgamentoId", j.getId());
      ev.put("at", LocalDateTime.now().toString());
      ev.put("numeroAcordao", a.getNumeroAcordao());
      ev.put("ementaResumo", a.getEmentaResumo());
      ev.put("inteiroTeorRef", a.getInteiroTeorRef());
      ev.put("publicadoEm", a.getPublicadoEm() != null ? a.getPublicadoEm().toString() : null);
      ev.put("placar", placarMap(j));
      liveHub.publish(j.getId(), mapper.writeValueAsString(ev));
    } catch (Exception e) {
      log.debug("falha ao publicar evento de acordao julgamentoId={} acordaoId={}", j.getId(), a.getId(), e);
    }
  }


  private void recomputePlacar(JulgamentoColegiado j) {
    if (j == null || j.getId() == null) {
      return;
    }
    int favor = 0;
    int contra = 0;
    int parcial = 0;
    int outros = 0;
    for (VotoColegiado voto : votoRepo.findByJulgamentoIdOrdered(j.getId())) {
      TipoVotoColegiado tipo = voto != null ? voto.getVotoTipo() : null;
      if (tipo == null) {
        outros++;
        continue;
      }
      switch (JulgamentoPlacarClassifier.classify(tipo)) {
        case FAVOR -> favor++;
        case CONTRA -> contra++;
        case PARCIAL -> parcial++;
        case OUTROS -> outros++;
      }
    }
    j.setPlacarFavor(favor);
    j.setPlacarContra(contra);
    j.setPlacarParcial(parcial);
    j.setPlacarOutros(outros);
  }


  private Map<String, Object> placarMap(JulgamentoColegiado j) {
    Map<String, Object> p = new LinkedHashMap<>();
    p.put("favor", j.getPlacarFavor());
    p.put("contra", j.getPlacarContra());
    p.put("parcial", j.getPlacarParcial());
    p.put("outros", j.getPlacarOutros());
    return p;
  }

  private com.tcc.pjb.backend.model.entity.julgamento.enums.PapelMagistradoNoJulgamento parsePapel(String s) {
    if (s == null || s.isBlank()) return null;
    try {
      return com.tcc.pjb.backend.model.entity.julgamento.enums.PapelMagistradoNoJulgamento.valueOf(s.trim().toUpperCase());
    } catch (Exception ignored) {
      return null;
    }
  }
}
