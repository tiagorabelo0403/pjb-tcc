package com.tcc.pjb.backend.core.forum.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;

@Component
public class ForumDeskResolver {

  private final ForumOrganResolver organResolver;
  private final ForumDeskPortfolioResolver portfolioResolver;

  public ForumDeskResolver(ForumOrganResolver organResolver, ForumDeskPortfolioResolver portfolioResolver) {
    this.organResolver = Objects.requireNonNull(organResolver);
    this.portfolioResolver = Objects.requireNonNull(portfolioResolver);
  }

  public ForumDeskKey resolveForProcess(Processo p) {
    Objects.requireNonNull(p, "processo");
    Jurisdicao j = p.getJurisdicao();

    ForumInstance instance = resolveInstance(p, j);
    JudicialOrganRef organ = organResolver.resolve(p, j, hint(p));

    ForumLane lane = ForumLane.fromProcess(p);

    String uf = (j != null && j.getUf() != null) ? j.getUf() : "XX";
    String comarca = j != null ? nz(firstNonBlank(j.getForo(), j.getSecaoOuSubsecao(), j.getMunicipioOuComarca(), j.getCidade())) : "";
    String jurisCode = (j != null && j.getCodigo() != null) ? j.getCodigo() : "";

    String inboxKey = InboxKeyFactory.secretariatInboxKey(organ, instance, lane, uf, comarca, jurisCode);

    String unitHint = buildUnitHint(p, j, instance, lane);
    return enrichWithPortfolio(new ForumDeskKey(inboxKey, organ, instance, lane, up(uf), comarca, unitHint + ":" + lane.dashboardBucket()));
  }

  
  public List<ForumDeskKey> resolveDefaultForStaffUser(Usuario u) {
    Objects.requireNonNull(u, "usuario");
    String uf = u.getUf() != null ? u.getUf() : "XX";
    String comarca = u.getComarca() != null ? u.getComarca() : "";

    List<ForumDeskKey> out = new ArrayList<>();

    boolean includeEstadual = u.getEnteFederativo() == null || u.atuaNoMunicipio() || u.atuaNoEstado();
    boolean includeFederal = u.atuaNaUniao() || u.getEnteFederativo() == null;

    if (includeEstadual) {
      JudicialOrganRef tj = new JudicialOrganRef("TJ" + up(uf), JudicialOrganKind.TJ, "Tribunal de Justiça " + up(uf));
      appendDeskGroup(out, tj, ForumInstance.FIRST, uf, comarca, "FORUM_COMARCA",
          ForumLane.COMUM,
          ForumLane.JEC,
          ForumLane.FAZENDA,
          ForumLane.FAMILIA,
          ForumLane.SUCESSOES,
          ForumLane.CRIMINAL,
          ForumLane.JURI,
          ForumLane.VIOLENCIA_DOMESTICA,
          ForumLane.INFANCIA,
          ForumLane.EMPRESARIAL);

      appendDeskGroup(out, tj, ForumInstance.SECOND, uf, comarca, "TJ_2G",
          ForumLane.COMUM,
          ForumLane.FAZENDA,
          ForumLane.FAMILIA,
          ForumLane.CRIMINAL,
          ForumLane.EMPRESARIAL);
    }

    if (includeFederal) {
      int r = ForumOrganResolver.trfRegionByUf(up(uf));
      JudicialOrganRef trf = new JudicialOrganRef("TRF" + r, JudicialOrganKind.TRF, "Tribunal Regional Federal da " + r + "ª Região");
      appendDeskGroup(out, trf, ForumInstance.FIRST, uf, comarca, "JF_SECAO",
          ForumLane.COMUM,
          ForumLane.JEF,
          ForumLane.PREVIDENCIARIO,
          ForumLane.FAZENDA);
      appendDeskGroup(out, trf, ForumInstance.SECOND, uf, comarca, "TRF_2G",
          ForumLane.COMUM,
          ForumLane.PREVIDENCIARIO,
          ForumLane.FAZENDA);
    }

    JudicialOrganRef trt = new JudicialOrganRef("TRT" + ForumOrganResolver.trtNumberByUf(up(uf)), JudicialOrganKind.TRT, "Tribunal Regional do Trabalho");
    appendDeskGroup(out, trt, ForumInstance.FIRST, uf, comarca, "JT_1G", ForumLane.TRABALHO);
    appendDeskGroup(out, trt, ForumInstance.SECOND, uf, comarca, "JT_2G", ForumLane.TRABALHO);

    JudicialOrganRef tre = new JudicialOrganRef("TRE" + up(uf), JudicialOrganKind.TRE, "Tribunal Regional Eleitoral " + up(uf));
    appendDeskGroup(out, tre, ForumInstance.FIRST, uf, comarca, "JE_ZONA", ForumLane.ELEITORAL);
    appendDeskGroup(out, tre, ForumInstance.SECOND, uf, comarca, "JE_TRE", ForumLane.ELEITORAL);

    if ("MG".equals(up(uf)) || "RS".equals(up(uf)) || "SP".equals(up(uf))) {
      JudicialOrganRef tjm = new JudicialOrganRef("TJM" + up(uf), JudicialOrganKind.TJM, "Tribunal de Justiça Militar " + up(uf));
      appendDeskGroup(out, tjm, ForumInstance.FIRST, uf, comarca, "JM_AUDITORIA", ForumLane.MILITAR);
      appendDeskGroup(out, tjm, ForumInstance.SECOND, uf, comarca, "JM_2G", ForumLane.MILITAR);
    }

    return out;
  }

  private void appendDeskGroup(List<ForumDeskKey> out,
                               JudicialOrganRef organ,
                               ForumInstance instance,
                               String uf,
                               String comarca,
                               String unitPrefix,
                               ForumLane... lanes) {
    if (lanes == null) {
      return;
    }
    for (ForumLane lane : lanes) {
      if (lane != null) {
        appendDesk(out, organ, instance, lane, uf, comarca, unitPrefix + ':' + lane.name());
      }
    }
  }

  private void appendDesk(List<ForumDeskKey> out,
                          JudicialOrganRef organ,
                          ForumInstance instance,
                          ForumLane lane,
                          String uf,
                          String comarca,
                          String unitHint) {
    String inbox = InboxKeyFactory.secretariatInboxKey(organ, instance, lane, uf, comarca, "");
    if (!containsInbox(out, inbox)) {
      out.add(enrichWithPortfolio(new ForumDeskKey(inbox, organ, instance, lane, up(uf), comarca, unitHint)));
    }
  }

  private boolean containsInbox(List<ForumDeskKey> out, String inbox) {
    for (ForumDeskKey key : out) {
      if (key != null && key.inboxKey().equals(inbox)) {
        return true;
      }
    }
    return false;
  }

  private ForumDeskKey enrichWithPortfolio(ForumDeskKey baseKey) {
    ForumDeskPortfolioProfile profile = portfolioResolver.resolve(baseKey);
    String mergedHint = baseKey.unitHint() + ':' + profile.operationalDescriptor();
    return new ForumDeskKey(baseKey.inboxKey(), baseKey.organ(), baseKey.instance(), baseKey.lane(), baseKey.uf(), baseKey.comarca(), mergedHint);
  }

  private static ForumInstance resolveInstance(Processo p, Jurisdicao j) {
    GrauJurisdicao grau = j != null ? j.getGrau() : null;
    if (grau == GrauJurisdicao.SUPERIOR) {
      return ForumInstance.SUPERIOR;
    }
    if (grau == GrauJurisdicao.SEGUNDO_GRAU) {
      return ForumInstance.SECOND;
    }
    if (p.getFaseAtual() == FaseProcessual.RECURSAL) {
      return ForumInstance.SECOND;
    }
    return ForumInstance.FIRST;
  }

  private static String buildUnitHint(Processo p, Jurisdicao j, ForumInstance instance, ForumLane lane) {
    String base = instance == ForumInstance.FIRST ? "1G" : (instance == ForumInstance.SECOND ? "2G" : "SUP");
    String l = lane == ForumLane.COMUM ? "" : (":" + lane.token());
    String cod = j != null ? nz(j.getCodigo()) : "";
    String territory = j != null ? nz(firstNonBlank(j.getForo(), j.getSecaoOuSubsecao(), j.getMunicipioOuComarca())) : "";
    return base + l + (cod.isEmpty() ? "" : (":" + cod)) + (territory.isEmpty() ? "" : (":" + up(territory)));
  }

  private static String nz(String s) {
    return s == null ? "" : s;
  }

  private static String up(String s) {
    if (s == null) return "XX";
    String v = s.trim().toUpperCase(Locale.ROOT);
    return v.isEmpty() ? "XX" : (v.length() <= 4 ? v : v.substring(0, 4));
  }

  private static String firstNonBlank(String... values) {
    if (values == null) return null;
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private static String hint(Processo p) {
    if (p == null) return "";
    StringBuilder sb = new StringBuilder(220);
    if (p.getNumeroUnificado() != null) sb.append(p.getNumeroUnificado()).append(' ');
    if (p.getNumeroProcesso() != null) sb.append(p.getNumeroProcesso()).append(' ');
    if (p.getRamoDireito() != null) sb.append(p.getRamoDireito().name()).append(' ');
    if (p.getMateria() != null) sb.append(p.getMateria().name()).append(' ');
    if (p.getRito() != null) sb.append(p.getRito().name()).append(' ');
    return sb.toString();
  }
}
