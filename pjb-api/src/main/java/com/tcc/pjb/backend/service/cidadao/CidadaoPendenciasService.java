package com.tcc.pjb.backend.service.cidadao;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tcc.pjb.backend.core.prazos.policy.PrazoInteligenteService;
import com.tcc.pjb.backend.core.comunicacao.judicial.ExpedicaoJudicial;
import com.tcc.pjb.backend.core.comunicacao.judicial.ExpedicaoJudicialRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.cidadao.AreaLinks;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPendenciaDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPendenciasResponse;
import com.tcc.pjb.backend.model.dto.cidadao.Links;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.service.ui.UiHintService;
import java.util.Locale;

@Service
public class CidadaoPendenciasService {

    private final CurrentUserService currentUser;
    private final ProcessoRepository processoRepo;
    private final MovimentacaoProcessualRepository movRepo;
    private final AudienciaRepository audienciaRepo;
    private final JulgamentoColegiadoRepository julgamentoRepo;
    private final PrazoInteligenteService prazoService;
    private final UiHintService ui;
    private final ExpedicaoJudicialRepository expedicaoRepository;

    public CidadaoPendenciasService(CurrentUserService currentUser,
                                   ProcessoRepository processoRepo,
                                   MovimentacaoProcessualRepository movRepo,
                                   AudienciaRepository audienciaRepo,
                                   JulgamentoColegiadoRepository julgamentoRepo,
                                   PrazoInteligenteService prazoService,
                                   UiHintService ui,
                                   ExpedicaoJudicialRepository expedicaoRepository) {
        this.currentUser = Objects.requireNonNull(currentUser);
        this.processoRepo = Objects.requireNonNull(processoRepo);
        this.movRepo = Objects.requireNonNull(movRepo);
        this.audienciaRepo = Objects.requireNonNull(audienciaRepo);
        this.julgamentoRepo = Objects.requireNonNull(julgamentoRepo);
        this.prazoService = Objects.requireNonNull(prazoService);
        this.ui = Objects.requireNonNull(ui);
        this.expedicaoRepository = Objects.requireNonNull(expedicaoRepository);
    }

    public CidadaoPendenciasResponse pendencias() {
        Usuario u = currentUser.getRequired();
        String cpf = u.getCpf();
        if (cpf == null || cpf.isBlank()) {
            return new CidadaoPendenciasResponse(LocalDateTime.now(), 0, List.of(), "/api/v1/ui/legend", defaultLinks());
        }

        List<CidadaoPendenciaDto> trimmed = pendenciasForCpf(cpf);
        return new CidadaoPendenciasResponse(LocalDateTime.now(), trimmed.size(), List.copyOf(trimmed), "/api/v1/ui/legend", defaultLinks());
    }

    public List<CidadaoPendenciaDto> pendenciasForCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) return List.of();

        List<Processo> processos = processoRepo.findAllByPartesCpf(cpf);
        if (processos.isEmpty()) return List.of();

        List<Long> ids = processos.stream().map(Processo::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) return List.of();
        long[] idArray = ids.stream().mapToLong(Long::longValue).toArray();

        Map<Long, MovimentacaoProcessual> lastMoves = movRepo.findLatestByProcessoIds(ids).stream()
                .filter(m -> m.getProcesso() != null && m.getProcesso().getId() != null)
                .collect(Collectors.toMap(m -> m.getProcesso().getId(), m -> m, (a, b) -> a));

        Map<Long, Audiencia> nextAud = audienciaRepo.findNextUpcomingByProcessoIds(idArray, LocalDateTime.now()).stream()
                .filter(a -> a.getProcesso() != null && a.getProcesso().getId() != null)
                .collect(Collectors.toMap(a -> a.getProcesso().getId(), a -> a, (a, b) -> a));

        Map<Long, JulgamentoColegiado> nextJulg = julgamentoRepo.findNextPautaByProcessoIds(idArray, LocalDateTime.now()).stream()
                .filter(j -> j.getProcesso() != null && j.getProcesso().getId() != null)
                .collect(Collectors.toMap(j -> j.getProcesso().getId(), j -> j, (a, b) -> a));

        List<CidadaoPendenciaDto> out = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Processo> processoById = processos.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(Processo::getId, p -> p, (a, b) -> a));
        Set<Long> processosComComunicacao = adicionarPendenciasComunicacao(out, cpf, processoById, now);

        for (Processo p : processos) {
            if (p == null || p.getId() == null) continue;

            MovimentacaoProcessual mov = lastMoves.get(p.getId());
            String movDesc = mov != null ? mov.getDescricao() : null;
            LocalDateTime movData = mov != null ? toLocalDateTime(mov.getDataMovimentacao()) : null;

            List<String> tokens = ui.tokenSetForProcess(p).stream().map(Enum::name).toList();

            if (movDesc != null) {
                String d = movDesc.toLowerCase(Locale.ROOT);
                boolean isCitaIntima = d.contains("cita") || d.contains("intima") || d.contains("mandado") || d.contains("notifica");
                boolean recente = movData != null && ChronoUnit.DAYS.between(movData, now) <= 30;
                if (isCitaIntima && recente && !processosComComunicacao.contains(p.getId())) {
                    out.add(new CidadaoPendenciaDto(
                            "CITACAO_INTIMACAO",
                            0,
                            true,
                            movData,
                            p.getId(),
                            p.getNumeroUnificado(),
                            "Citação/Intimação recente",
                            safeShort(movDesc, 220),
                            tokens,
                            linksFor(p.getId())
                    ));
                }
            }

            LocalDateTime referencia = movData != null ? movData : p.getDataUltimaMovimentacao();
            var prazo = prazoService.calcularPrazo(p, referencia, movDesc);
            if (prazo != null && prazo.fim() != null) {
                long diasRestantes = prazo.diasRestantes();
                boolean urgente = prazo.urgente() || diasRestantes <= 3;
                int prio = urgente ? 1 : 3;
                out.add(new CidadaoPendenciaDto(
                        "PRAZO",
                        prio,
                        urgente,
                        prazo.fim(),
                        p.getId(),
                        p.getNumeroUnificado(),
                        "Prazo em andamento",
                        "Faltam " + diasRestantes + " dia(s) para o término do prazo.",
                        tokens,
                        linksFor(p.getId())
                ));
            }

            Audiencia a = nextAud.get(p.getId());
            if (a != null && a.getDataHora() != null) {
                long dias = ChronoUnit.DAYS.between(now, a.getDataHora());
                if (dias <= 14) {
                    boolean urgente = dias <= 3;
                    out.add(new CidadaoPendenciaDto(
                            "AUDIENCIA",
                            2,
                            urgente,
                            a.getDataHora(),
                            p.getId(),
                            p.getNumeroUnificado(),
                            "Audiência marcada",
                            safeShort((a.getTipo() != null ? a.getTipo().name() : "") +
                                    (a.getModalidade() != null ? (" • " + a.getModalidade().name()) : "") +
                                    (a.getLocal() != null ? (" • " + a.getLocal()) : ""), 220),
                            tokens,
                            linksFor(p.getId())
                    ));
                }
            }

            JulgamentoColegiado j = nextJulg.get(p.getId());
            if (j != null && j.getPautaDataHora() != null) {
                long dias = ChronoUnit.DAYS.between(now, j.getPautaDataHora());
                if (dias <= 14) {
                    boolean urgente = dias <= 3;
                    String resumo = (j.getGrau() != null ? j.getGrau().getLabel() : null) + " - " +
                            (j.getTribunalSigla() != null ? j.getTribunalSigla() : "") +
                            (j.getOrgaoJulgador() != null ? " " + j.getOrgaoJulgador() : "") +
                            (j.getStatus() != null ? " - " + j.getStatus().name() : "");
                    out.add(new CidadaoPendenciaDto(
                            "JULGAMENTO",
                            4,
                            urgente,
                            j.getPautaDataHora(),
                            p.getId(),
                            p.getNumeroUnificado(),
                            "Julgamento na pauta",
                            safeShort(resumo, 220),
                            tokens,
                            linksFor(p.getId())
                    ));
                }
            }
        }

        out.sort(Comparator
                .comparingInt(CidadaoPendenciaDto::prioridade)
                .thenComparing(p -> p.quando() == null ? LocalDateTime.MAX : p.quando())
                .thenComparing(p -> p.processoId() == null ? Long.MAX_VALUE : p.processoId())
        );

        return out.size() > 50 ? List.copyOf(out.subList(0, 50)) : List.copyOf(out);
    }

    private Set<Long> adicionarPendenciasComunicacao(List<CidadaoPendenciaDto> out,
                                                    String cpf,
                                                    Map<Long, Processo> processoById,
                                                    LocalDateTime now) {
        String documento = normalizarDocumento(cpf);
        if (documento == null || documento.isBlank()) {
            return Set.of();
        }
        Set<Long> processosComComunicacao = new LinkedHashSet<>();
        for (ExpedicaoJudicial expedicao : expedicaoRepository.findTop100ByDestinatarioDocumentoOrderByExpedidaEmDesc(documento)) {
            if (expedicao == null || expedicao.getProcessoId() == null || expedicao.getStatus() == null) {
                continue;
            }
            if (!statusVisivelAoPortal(expedicao.getStatus())) {
                continue;
            }
            Processo processo = processoById.get(expedicao.getProcessoId());
            if (processo == null) {
                continue;
            }
            if (!processosComComunicacao.add(processo.getId())) {
                continue;
            }
            List<String> tokens = ui.tokenSetForProcess(processo).stream().map(Enum::name).toList();
            LocalDateTime referencia = toLocalDateTime(priorizarMarco(expedicao));
            out.add(new CidadaoPendenciaDto(
                    tipoPendencia(expedicao),
                    prioridadePendencia(expedicao),
                    urgentePendencia(expedicao, now),
                    referencia,
                    processo.getId(),
                    processo.getNumeroUnificado(),
                    tituloPendencia(expedicao),
                    resumoPendencia(expedicao),
                    tokens,
                    linksFor(processo.getId())
            ));
        }
        return processosComComunicacao;
    }

    private boolean statusVisivelAoPortal(ExpedicaoJudicial.StatusExpedicao status) {
        return switch (status) {
            case EXPEDIDA, ENTREGUE_CONFIRMADA, LIDA_CONFIRMADA, PRESUMIDA_ENTREGUE, PENDENTE_OFICIAL, REMETIDA_CORREIO, PUBLICADA_EDITAL -> true;
            case FRUSTRADA_DEFINITIVA, DEVOLVIDA, CANCELADA -> false;
        };
    }

    private String tipoPendencia(ExpedicaoJudicial expedicao) {
        if (expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao()) {
            return "CITACAO_JUDICIAL";
        }
        if (expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isIntimacao()) {
            return "INTIMACAO_JUDICIAL";
        }
        return "COMUNICACAO_JUDICIAL";
    }

    private int prioridadePendencia(ExpedicaoJudicial expedicao) {
        return switch (expedicao.getStatus()) {
            case EXPEDIDA, ENTREGUE_CONFIRMADA, PUBLICADA_EDITAL -> 0;
            case PRESUMIDA_ENTREGUE -> 1;
            case PENDENTE_OFICIAL, REMETIDA_CORREIO -> 2;
            case LIDA_CONFIRMADA -> 3;
            case FRUSTRADA_DEFINITIVA, DEVOLVIDA, CANCELADA -> 4;
        };
    }

    private boolean urgentePendencia(ExpedicaoJudicial expedicao, LocalDateTime now) {
        if (expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao()) {
            return true;
        }
        if (expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.PUBLICADA_EDITAL || expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.PRESUMIDA_ENTREGUE) {
            return true;
        }
        LocalDateTime presuncao = toLocalDateTime(expedicao.getPresuncaoEntregaEm());
        return presuncao != null && ChronoUnit.DAYS.between(now, presuncao) <= 2;
    }

    private String tituloPendencia(ExpedicaoJudicial expedicao) {
        if (expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao()) {
            return "Citação em curso";
        }
        if (expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isIntimacao()) {
            return "Intimação em curso";
        }
        return "Comunicação judicial em curso";
    }

    private String resumoPendencia(ExpedicaoJudicial expedicao) {
        String modalidade = expedicao.getModalidade() != null ? expedicao.getModalidade().name().toLowerCase(Locale.ROOT).replace('_', ' ') : "modalidade não identificada";
        String status = switch (expedicao.getStatus()) {
            case EXPEDIDA -> "Aguardando ciência no portal.";
            case ENTREGUE_CONFIRMADA -> "Entrega confirmada. Verifique o teor do ato.";
            case LIDA_CONFIRMADA -> "Ciência confirmada e trilha auditável consolidada.";
            case PRESUMIDA_ENTREGUE -> "Presunção de entrega aplicada conforme a modalidade adotada.";
            case PENDENTE_OFICIAL -> "Diligência física em curso com Oficial de Justiça.";
            case REMETIDA_CORREIO -> "Envio físico em rota com rastreamento.";
            case PUBLICADA_EDITAL -> "Edital publicado e disponível para consulta.";
            case FRUSTRADA_DEFINITIVA -> "Comunicação frustrada.";
            case DEVOLVIDA -> "Comunicação devolvida.";
            case CANCELADA -> "Comunicação cancelada.";
        };
        return status + " Modalidade: " + modalidade + ".";
    }

    private Instant priorizarMarco(ExpedicaoJudicial expedicao) {
        if (expedicao.getPrazoRespostaInicioEm() != null) {
            return expedicao.getPrazoRespostaInicioEm();
        }
        if (expedicao.getPresuncaoEntregaEm() != null) {
            return expedicao.getPresuncaoEntregaEm();
        }
        return expedicao.getExpedidaEm();
    }

    private static String normalizarDocumento(String value) {
        if (value == null || value.isBlank()) return null;
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private static Links linksFor(Long processoId) {
        return CidadaoProcessoCardMapper.linksFor(processoId);
    }

    private static AreaLinks defaultLinks() {
        return new AreaLinks(
                "/api/v1/ui/legend",
                "/api/v1/ui/accessibility/preference",
                "/api/v1/ui/presentation/reading-preference",
                "/api/v1/ui/presentation/bundle",
                "/api/v1/chat",
                "/api/v1/chat/processo/{processoId}"
        );
    }

    private static LocalDateTime toLocalDateTime(Instant i) {
        if (i == null) return null;
        return LocalDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    private static String safeShort(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() <= max) return t;
        return t.substring(0, max - 1) + "…";
    }
}
