package com.tcc.pjb.backend.service.magistratura.acts;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActAvailabilityResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActFieldResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.juiz.guardrails.JuizProcessoGuardRailService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MagistraturaJudicialActProjectionSupport {

    public List<MagistraturaJudicialActAvailabilityResponse> catalog(Usuario usuario, UserPersona persona, Processo processo) {
        List<MagistraturaJudicialActAvailabilityResponse> out = new ArrayList<>();
        for (MagistraturaJudicialActCode code : supportedActions(usuario, persona)) {
            out.add(new MagistraturaJudicialActAvailabilityResponse(
                    code,
                    label(code),
                    resolveLane(usuario, persona),
                    true,
                    "ALLOW",
                    nativeRoute(code, processo == null ? null : processo.getId()),
                    templateFor(code),
                    List.of(),
                    List.of(),
                    fieldsFor(code, usuario)
            ));
        }
        return List.copyOf(out);
    }

    public List<MagistraturaJudicialActCode> supportedActions(Usuario usuario, UserPersona persona) {
        if (usuario.getTipoUsuario() == TipoUsuario.MINISTRO || persona.grau() == GrauJurisdicao.SUPERIOR) {
            return List.of(
                    MagistraturaJudicialActCode.DECISAO_MONOCRATICA,
                    MagistraturaJudicialActCode.INCLUSAO_PAUTA,
                    MagistraturaJudicialActCode.DECISAO_PLENARIA,
                    MagistraturaJudicialActCode.NOMEACAO_PERITO
            );
        }
        if (usuario.getTipoUsuario() == TipoUsuario.DESEMBARGADOR || usuario.getTipoUsuario() == TipoUsuario.DESEMBARGADOR_FEDERAL || persona.grau() == GrauJurisdicao.SEGUNDO_GRAU) {
            return List.of(
                    MagistraturaJudicialActCode.DESPACHO_RELATOR,
                    MagistraturaJudicialActCode.DECISAO_MONOCRATICA,
                    MagistraturaJudicialActCode.VOTO_COLEGIADO,
                    MagistraturaJudicialActCode.ACORDAO,
                    MagistraturaJudicialActCode.PEDIDO_VISTA,
                    MagistraturaJudicialActCode.DESTAQUE,
                    MagistraturaJudicialActCode.NOMEACAO_PERITO
            );
        }
        return List.of(
                MagistraturaJudicialActCode.DESPACHO,
                MagistraturaJudicialActCode.DECISAO_INTERLOCUTORIA,
                MagistraturaJudicialActCode.SENTENCA,
                MagistraturaJudicialActCode.DESIGNAR_AUDIENCIA,
                MagistraturaJudicialActCode.ORDEM_CUMPRIMENTO_OFICIAL,
                MagistraturaJudicialActCode.CERTIDAO_TRANSITO_JULGADO,
                MagistraturaJudicialActCode.NOMEACAO_PERITO
        );
    }

    public boolean supports(Usuario usuario, UserPersona persona, MagistraturaJudicialActCode code) {
        return supportedActions(usuario, persona).contains(code);
    }

    public void ensureSupported(Usuario usuario, UserPersona persona, MagistraturaJudicialActCode code) {
        if (!supports(usuario, persona, code)) {
            throw new AccessDeniedPjbException("Ato não disponível para a trilha da magistratura autenticada.");
        }
    }

    public boolean requiresGuard(MagistraturaJudicialActCode code) {
        return switch (code) {
            case NOMEACAO_PERITO, CERTIDAO_TRANSITO_JULGADO -> false;
            default -> true;
        };
    }

    public String resolveLane(Usuario usuario, UserPersona persona) {
        if (usuario.getTipoUsuario() == TipoUsuario.MINISTRO || persona.grau() == GrauJurisdicao.SUPERIOR) {
            return "SUPERIOR";
        }
        if (usuario.getTipoUsuario() == TipoUsuario.DESEMBARGADOR || usuario.getTipoUsuario() == TipoUsuario.DESEMBARGADOR_FEDERAL || persona.grau() == GrauJurisdicao.SEGUNDO_GRAU) {
            return "SEGUNDO_GRAU";
        }
        return "PRIMEIRO_GRAU";
    }

    public List<String> anchors(Usuario usuario, UserPersona persona, Processo processo) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(resolveLane(usuario, persona));
        if (persona.esfera() != null) {
            out.add(persona.esfera().name());
        }
        if (usuario.getTipoUsuario() != null) {
            out.add(usuario.getTipoUsuario().name());
        }
        if (usuario.getUf() != null && !usuario.getUf().isBlank()) {
            out.add("UF_" + usuario.getUf().toUpperCase(Locale.ROOT));
        }
        if (processo != null && processo.getTipoJustica() != null) {
            out.add("JUSTICA_" + processo.getTipoJustica().name());
        }
        return List.copyOf(out);
    }

    public Map<String, Object> processSignals(Processo processo, Usuario usuario, UserPersona persona) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("lane", resolveLane(usuario, persona));
        out.put("uf", usuario.getUf());
        out.put("comarca", usuario.getComarca());
        out.put("grau", persona.grau() == null ? null : persona.grau().name());
        out.put("esfera", persona.esfera() == null ? null : persona.esfera().name());
        if (processo != null) {
            out.put("processoId", processo.getId());
            out.put("numeroProcesso", processNumber(processo));
            out.put("classeProcessual", processo.getClasseProcessual());
            out.put("assunto", processo.getAssunto());
            out.put("faseAtual", processo.getFaseAtual());
            out.put("ramoDireito", processo.getRamoDireito() == null ? null : processo.getRamoDireito().name());
        }
        return safeMap(out);
    }

    public List<String> baseReasons(Usuario usuario, UserPersona persona, Processo processo, MagistraturaJudicialActCode code) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Trilha jurisdicional ativa: " + resolveLane(usuario, persona) + '.');
        if (persona.esfera() != null) {
            reasons.add("Esfera jurisdicional: " + persona.esfera().name() + '.');
        }
        if (processo != null) {
            reasons.add("Processo " + processNumber(processo) + " projetado para ato " + code.name() + '.');
        }
        if (code == MagistraturaJudicialActCode.NOMEACAO_PERITO) {
            reasons.add("Nomeação de perito reaproveita a trilha institucional já existente no PJB.");
        }
        return List.copyOf(reasons);
    }

    public List<String> guardReasons(JuizProcessoGuardRailService.GuardRailSnapshot guard) {
        List<String> out = new ArrayList<>();
        if (guard == null) {
            return out;
        }
        out.add("Verdict jurisdicional: " + guard.verdictBand() + '.');
        if (guard.fundamentos() != null) {
            out.addAll(guard.fundamentos());
        }
        return List.copyOf(out);
    }

    public String label(MagistraturaJudicialActCode code) {
        return switch (code) {
            case DESPACHO -> "Despacho";
            case DECISAO_INTERLOCUTORIA -> "Decisão interlocutória";
            case SENTENCA -> "Sentença";
            case DESIGNAR_AUDIENCIA -> "Designar audiência";
            case ORDEM_CUMPRIMENTO_OFICIAL -> "Ordem de cumprimento ao oficial";
            case CERTIDAO_TRANSITO_JULGADO -> "Certidão de trânsito em julgado";
            case NOMEACAO_PERITO -> "Nomeação de perito";
            case DESPACHO_RELATOR -> "Despacho da relatoria";
            case DECISAO_MONOCRATICA -> "Decisão monocrática";
            case VOTO_COLEGIADO -> "Voto colegiado";
            case ACORDAO -> "Acórdão";
            case PEDIDO_VISTA -> "Pedido de vista";
            case DESTAQUE -> "Destaque";
            case INCLUSAO_PAUTA -> "Inclusão em pauta";
            case DECISAO_PLENARIA -> "Decisão plenária";
        };
    }

    public List<MagistraturaJudicialActFieldResponse> fieldsFor(MagistraturaJudicialActCode code, Usuario usuario) {
        return switch (code) {
            case DESPACHO, DESPACHO_RELATOR -> List.of(
                    field("conteudo", "Conteúdo", "TEXTAREA", true, "Determine-se a intimação e o regular prosseguimento."),
                    field("fundamentacao", "Fundamentação", "TEXTAREA", false, "Fundamento legal ou motivação objetiva.")
            );
            case DECISAO_INTERLOCUTORIA -> List.of(
                    field("dispositivo", "Dispositivo", "TEXTAREA", true, "Defiro a tutela e determino o cumprimento."),
                    field("fundamentacao", "Fundamentação", "TEXTAREA", false, "Motivação da decisão interlocutória."),
                    field("tipo", "Tipo da decisão", "TEXT", false, "TUTELA_URGENCIA")
            );
            case SENTENCA -> List.of(
                    field("dispositivo", "Dispositivo", "TEXTAREA", true, "Julgo procedente o pedido."),
                    field("fundamentacao", "Fundamentação", "TEXTAREA", false, "Fundamentação judicial."),
                    field("tipo", "Espécie", "TEXT", false, "MERITO")
            );
            case DESIGNAR_AUDIENCIA -> List.of(
                    field("dataHora", "Data e hora", "INSTANT", false, "2026-04-20T13:00:00Z"),
                    field("tipo", "Tipo", "TEXT", false, "AUDIENCIA_INSTRUCAO"),
                    field("local", "Local", "TEXT", false, "Sala 01")
            );
            case ORDEM_CUMPRIMENTO_OFICIAL -> List.of(
                    field("fundamentacao", "Fundamento", "TEXTAREA", true, "Cumpra-se o mandado."),
                    field("conteudo", "Conteúdo operacional", "TEXTAREA", false, "Diligência com ciência obrigatória."),
                    field("tipo", "Tipo de cumprimento", "TEXT", false, "CUMPRIMENTO_JUDICIAL"),
                    field("oficialId", "Oficial", "LONG", false, "12"),
                    field("prioridade", "Prioridade", "INT", false, "1")
            );
            case CERTIDAO_TRANSITO_JULGADO -> List.of();
            case NOMEACAO_PERITO -> List.of(
                    field("peritoId", "Perito", "LONG", true, "34"),
                    field("observacao", "Observação", "TEXTAREA", false, "Nomeação para prova técnica especializada.")
            );
            case DECISAO_MONOCRATICA -> List.of(
                    field("relatorio", "Relatório", "TEXTAREA", true, usuario.getTipoUsuario() == TipoUsuario.MINISTRO ? "Síntese do caso constitucional." : "Relatório recursal do caso."),
                    field("fundamentacao", "Fundamentação", "TEXTAREA", false, "Motivação do relator."),
                    field("dispositivo", "Dispositivo", "TEXTAREA", true, "Nego provimento ao recurso.")
            );
            case VOTO_COLEGIADO -> List.of(
                    field("voto", "Voto", "TEXTAREA", true, "Acompanho o relator."),
                    field("fundamentacao", "Fundamentação", "TEXTAREA", false, "Razões do voto."),
                    field("decisao", "Resultado", "TEXT", true, "NEGAR_PROVIMENTO")
            );
            case ACORDAO -> List.of(
                    field("ementa", "Ementa", "TEXTAREA", true, "Processual civil. Recurso. ..."),
                    field("fundamentacao", "Fundamentação", "TEXTAREA", false, "Fundamentação colegiada."),
                    field("dispositivo", "Dispositivo", "TEXTAREA", true, "Por unanimidade, negar provimento.")
            );
            case PEDIDO_VISTA -> List.of(field("diasVista", "Dias de vista", "INT", false, "5"));
            case DESTAQUE -> List.of(field("observacao", "Motivo", "TEXTAREA", true, "Necessidade de destaque para julgamento presencial."));
            case INCLUSAO_PAUTA -> List.of(
                    field("dataHora", "Data da sessão", "INSTANT", false, "2026-05-04T18:00:00Z"),
                    field("orgao", "Órgão julgador", "TEXT", false, "PLENARIO")
            );
            case DECISAO_PLENARIA -> List.of(
                    field("votacao", "Votação", "TEXT", true, "MAIORIA"),
                    field("ementa", "Ementa", "TEXTAREA", true, "Tema constitucional fixado."),
                    field("dispositivo", "Dispositivo", "TEXTAREA", true, "Fixada a tese e determinado o retorno à origem.")
            );
        };
    }

    public String nativeRoute(MagistraturaJudicialActCode code, Long processoId) {
        return switch (code) {
            case DESPACHO -> OperationalApiRoutes.judgeGabineteDespacho(processoId);
            case DECISAO_INTERLOCUTORIA -> OperationalApiRoutes.judgeGabineteDecisaoInterlocutoria(processoId);
            case SENTENCA -> OperationalApiRoutes.judgeGabineteSentenca(processoId);
            case DESIGNAR_AUDIENCIA -> OperationalApiRoutes.judgeGabineteAudiencia(processoId);
            case ORDEM_CUMPRIMENTO_OFICIAL -> OperationalApiRoutes.judgeGabineteOrdemCumprimentoOficial(processoId);
            case CERTIDAO_TRANSITO_JULGADO -> OperationalApiRoutes.judgeGabineteCertidaoTransitoJulgado(processoId);
            case NOMEACAO_PERITO -> "/api/v1/processos/" + (processoId == null ? "{processoId}" : processoId) + "/peritos/nomear";
            case DESPACHO_RELATOR, DECISAO_MONOCRATICA -> "/api/v1/magistratura/processos/" + (processoId == null ? "{processoId}" : processoId) + "/atos";
            case VOTO_COLEGIADO -> OperationalApiRoutes.desembargadorColegiadoVoto(processoId);
            case ACORDAO -> OperationalApiRoutes.desembargadorColegiadoAcordao(processoId);
            case PEDIDO_VISTA -> OperationalApiRoutes.desembargadorColegiadoVista(processoId);
            case DESTAQUE -> OperationalApiRoutes.desembargadorColegiadoDestaque(processoId);
            case INCLUSAO_PAUTA -> OperationalApiRoutes.ministroPlenarioPauta(processoId);
            case DECISAO_PLENARIA -> OperationalApiRoutes.ministroPlenarioDecisaoPlenaria(processoId);
        };
    }

    public String templateFor(MagistraturaJudicialActCode code) {
        return switch (code) {
            case DESPACHO, DESPACHO_RELATOR -> "DESPACHO";
            case DECISAO_INTERLOCUTORIA, DECISAO_MONOCRATICA, VOTO_COLEGIADO -> "DECISAO";
            case SENTENCA -> "SENTENCA";
            case ACORDAO, DECISAO_PLENARIA -> "ACORDAO";
            case DESIGNAR_AUDIENCIA -> "TERMO_AUDIENCIA";
            case ORDEM_CUMPRIMENTO_OFICIAL -> "MANDADO";
            case CERTIDAO_TRANSITO_JULGADO -> "CERTIDAO_TRANSITO_JULGADO";
            case NOMEACAO_PERITO -> "NOMEACAO_OPERACIONAL";
            case PEDIDO_VISTA, DESTAQUE, INCLUSAO_PAUTA -> "GOVERNANCA_PROCESSUAL";
        };
    }

    public String actorCourtLabel(Usuario usuario) {
        return switch (usuario.getTipoUsuario()) {
            case MINISTRO -> "TRIBUNAL_SUPERIOR";
            case DESEMBARGADOR_FEDERAL -> "TRIBUNAL_REGIONAL_FEDERAL";
            case DESEMBARGADOR -> "TRIBUNAL_ESTADUAL";
            default -> "JUIZO_SINGULAR";
        };
    }

    public String actorInstanceLabel(Usuario usuario) {
        return switch (usuario.getTipoUsuario()) {
            case MINISTRO -> "ULTIMA_INSTANCIA";
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL -> "SEGUNDO_GRAU";
            default -> "PRIMEIRO_GRAU";
        };
    }

    public String actorHistoryLane(Usuario usuario) {
        return switch (usuario.getTipoUsuario()) {
            case MINISTRO -> "MINISTRO";
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL -> "DESEMBARGADOR";
            default -> "JUIZ";
        };
    }

    public String suggestedTitle(Usuario usuario, Processo processo, MagistraturaJudicialActCode code) {
        return label(code) + " — " + actorCourtLabel(usuario) + " — " + processNumber(processo);
    }

    public Map<String, Object> safeMetrics(JuizProcessoGuardRailService.GuardRailSnapshot guard) {
        return guard == null ? Map.of() : safeMap(guard.metrics());
    }

    public Map<String, Object> safeMap(Map<String, ?> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((key, value) -> {
                if (key != null && value != null) {
                    out.put(key, value);
                }
            });
        }
        return Collections.unmodifiableMap(out);
    }

    public String processNumber(Processo processo) {
        return firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero(), "PROCESSO_SEM_NUMERO");
    }

    public String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private MagistraturaJudicialActFieldResponse field(String name, String label, String kind, boolean required, String sample) {
        return new MagistraturaJudicialActFieldResponse(name, label, kind, required, sample);
    }
}
