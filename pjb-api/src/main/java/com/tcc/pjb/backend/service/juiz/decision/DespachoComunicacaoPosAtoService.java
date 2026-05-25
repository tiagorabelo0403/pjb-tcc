package com.tcc.pjb.backend.service.juiz.decision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalCiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoCienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicaoCiencia;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.judicial.DjePublicacao;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.CienciaProcessualRepository;
import com.tcc.pjb.backend.model.repository.DjePublicacaoRepository;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DespachoComunicacaoPosAtoService {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");
    private static final String TIPO_ATO_DESPACHO = "DESPACHO";
    private static final TipoCienciaProcessual TIPO_CIENCIA_DESPACHO = TipoCienciaProcessual.INTIMACAO_DECISAO;

    private final DjePublicacaoRepository djePublicacaoRepository;
    private final CienciaProcessualRepository cienciaProcessualRepository;
    private final PoloProcessualRepository poloProcessualRepository;
    private final UsuarioRepository usuarioRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public DespachoComunicacaoPosAtoService(DjePublicacaoRepository djePublicacaoRepository,
                                            CienciaProcessualRepository cienciaProcessualRepository,
                                            PoloProcessualRepository poloProcessualRepository,
                                            UsuarioRepository usuarioRepository,
                                            OutboxEventRepository outboxEventRepository,
                                            ObjectMapper objectMapper) {
        this.djePublicacaoRepository = Objects.requireNonNull(djePublicacaoRepository);
        this.cienciaProcessualRepository = Objects.requireNonNull(cienciaProcessualRepository);
        this.poloProcessualRepository = Objects.requireNonNull(poloProcessualRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper).findAndRegisterModules();
    }

    @Transactional
    public DespachoComunicacaoPosAtoResult registrar(Processo processo,
                                                     Usuario magistrado,
                                                     MovimentacaoProcessual movimentacao,
                                                     OfficialDocumentTemplateRenderResponse documentoAssinado,
                                                     String conteudo) {
        Objects.requireNonNull(processo);
        Objects.requireNonNull(magistrado);
        Objects.requireNonNull(movimentacao);
        Objects.requireNonNull(documentoAssinado);

        String conteudoHash = firstNonBlank(documentoAssinado.hashSha256(), Hashes.sha256Hex(conteudo == null ? "" : conteudo));
        LocalDate hoje = LocalDate.now(ZONE_ID);
        LocalDate disponibilizacao = hoje.plusDays(1);
        LocalDate publicacao = disponibilizacao.plusDays(1);
        LocalDate prazoComecaEm = publicacao.plusDays(1);
        LocalDate prazoFinalEstimado = prazoComecaEm.plusDays(TIPO_CIENCIA_DESPACHO.prazoRespostaDias());
        String tribunalCodigo = firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), "PJB");
        String edicaoDje = "PJB-" + disponibilizacao.format(DateTimeFormatter.BASIC_ISO_DATE) + "-OUTBOX";

        DjePublicacao publicacaoDje = publicarDje(processo, conteudoHash, tribunalCodigo, disponibilizacao, publicacao, prazoComecaEm, edicaoDje);
        List<UUID> outboxEventIds = new ArrayList<>();
        UUID djeOutboxId = salvarOutboxSeguro(
                "processo.dje",
                "DJE_PUBLICACAO_SOLICITADA",
                dadosDje(processo, movimentacao, documentoAssinado, publicacaoDje, prazoComecaEm),
                "dje:despacho:" + processo.getId() + ':' + movimentacao.getId(),
                "Processo",
                String.valueOf(processo.getId()));
        if (djeOutboxId != null) {
            outboxEventIds.add(djeOutboxId);
        } else {
            publicacaoDje.setStatus("FALHA");
            publicacaoDje.setFailureReason("Falha controlada no enfileiramento da publicacao DJe.");
            djePublicacaoRepository.save(publicacaoDje);
        }

        List<Long> cienciaIds = new ArrayList<>();
        int intimacoesCriadas = registrarCiencias(processo, movimentacao, conteudoHash, publicacaoDje, disponibilizacao, publicacao, prazoComecaEm, cienciaIds, outboxEventIds);
        UUID prazoOutboxId = salvarOutboxSeguro(
                "processo.prazo",
                "PRAZO_PROCESSUAL_CRIADO",
                dadosPrazo(processo, movimentacao, publicacaoDje, prazoComecaEm, prazoFinalEstimado),
                "prazo:despacho:" + processo.getId() + ':' + movimentacao.getId(),
                "Processo",
                String.valueOf(processo.getId()));
        if (prazoOutboxId != null) {
            outboxEventIds.add(prazoOutboxId);
        }

        boolean falhaControlada = djeOutboxId == null || prazoOutboxId == null;
        return new DespachoComunicacaoPosAtoResult(
                publicacaoDje.getId(),
                publicacaoDje.getStatus(),
                djeOutboxId,
                intimacoesCriadas,
                prazoOutboxId == null ? 0 : 1,
                List.copyOf(cienciaIds),
                List.copyOf(outboxEventIds),
                prazoComecaEm,
                prazoFinalEstimado,
                falhaControlada
        );
    }

    private DjePublicacao publicarDje(Processo processo,
                                      String conteudoHash,
                                      String tribunalCodigo,
                                      LocalDate disponibilizacao,
                                      LocalDate publicacao,
                                      LocalDate prazoComecaEm,
                                      String edicaoDje) {
        return djePublicacaoRepository.findTop100ByProcessoIdOrderByCreatedAtDesc(processo.getId()).stream()
                .filter(item -> TIPO_ATO_DESPACHO.equals(item.getTipoAto()))
                .filter(item -> conteudoHash.equals(item.getConteudoHash()))
                .findFirst()
                .orElseGet(() -> djePublicacaoRepository.save(DjePublicacao.builder()
                        .processoId(processo.getId())
                        .tipoAto(TIPO_ATO_DESPACHO)
                        .conteudoHash(conteudoHash)
                        .edicaoDje(edicaoDje)
                        .dataDisponibilizacao(disponibilizacao)
                        .dataPublicacao(publicacao)
                        .prazoComecaEm(prazoComecaEm)
                        .tribunalCodigo(tribunalCodigo)
                        .status("PENDENTE_ENVIO")
                        .partesNotificadas(false)
                        .createdAt(Instant.now())
                        .build()));
    }

    private int registrarCiencias(Processo processo,
                                  MovimentacaoProcessual movimentacao,
                                  String conteudoHash,
                                  DjePublicacao publicacaoDje,
                                  LocalDate disponibilizacao,
                                  LocalDate publicacao,
                                  LocalDate prazoComecaEm,
                                  List<Long> cienciaIds,
                                  List<UUID> outboxEventIds) {
        List<PoloProcessual> destinatarios = poloProcessualRepository.findDestinatariosCiencia(processo.getId()).stream()
                .filter(PoloProcessual::isAtivo)
                .filter(polo -> polo.getTipoPolo() != null && polo.getTipoPolo().recebeCiencia())
                .filter(polo -> polo.getUsuarioId() != null)
                .toList();
        int criadas = 0;
        for (PoloProcessual polo : destinatarios) {
            Usuario destinatario = usuarioRepository.findById(polo.getUsuarioId()).orElse(null);
            if (destinatario == null) {
                continue;
            }
            CienciaProcessual ciencia = cienciaProcessualRepository.findByAtoProcessualIdAndUsuarioId(movimentacao.getId(), destinatario.getId())
                    .orElseGet(() -> cienciaProcessualRepository.save(new CienciaProcessual(
                            processo,
                            destinatario,
                            TIPO_CIENCIA_DESPACHO,
                            CanalCiencia.DJE_TRIBUNAL,
                            resolverGrau(processo),
                            ritoProcessual(processo),
                            instanciaTribunal(processo),
                            limite(polo.getTipoPolo().name(), 15),
                            null,
                            movimentacao.getId(),
                            processo.getNumeroProcesso(),
                            limite(publicacaoDje.getEdicaoDje(), 20),
                            conteudoHash,
                            toInstant(disponibilizacao),
                            toInstant(publicacao),
                            toInstant(prazoComecaEm)
                    )));
            if (!cienciaIds.contains(ciencia.getId())) {
                cienciaIds.add(ciencia.getId());
            }
            UUID outboxId = salvarOutboxSeguro(
                    "processo.intimacao",
                    "INTIMACAO_PROCESSUAL_CRIADA",
                    dadosCiencia(processo, movimentacao, publicacaoDje, ciencia),
                    "intimacao:despacho:" + processo.getId() + ':' + movimentacao.getId() + ':' + destinatario.getId(),
                    "Processo",
                    String.valueOf(processo.getId()));
            if (outboxId != null) {
                outboxEventIds.add(outboxId);
            }
            criadas++;
        }
        return criadas;
    }

    private LinkedHashMap<String, Object> dadosDje(Processo processo,
                                                   MovimentacaoProcessual movimentacao,
                                                   OfficialDocumentTemplateRenderResponse documentoAssinado,
                                                   DjePublicacao publicacaoDje,
                                                   LocalDate prazoComecaEm) {
        LinkedHashMap<String, Object> dados = new LinkedHashMap<>();
        dados.put("processoId", processo.getId());
        dados.put("numeroProcesso", processo.getNumeroProcesso());
        dados.put("movimentacaoId", movimentacao.getId());
        dados.put("documentoId", documentoAssinado.documentoId());
        dados.put("documentoHash", documentoAssinado.hashSha256());
        dados.put("djePublicacaoId", publicacaoDje.getId());
        dados.put("tipoAto", publicacaoDje.getTipoAto());
        dados.put("tribunalCodigo", publicacaoDje.getTribunalCodigo());
        dados.put("dataDisponibilizacao", publicacaoDje.getDataDisponibilizacao());
        dados.put("dataPublicacao", publicacaoDje.getDataPublicacao());
        dados.put("prazoComecaEm", prazoComecaEm);
        return dados;
    }

    private LinkedHashMap<String, Object> dadosPrazo(Processo processo,
                                                     MovimentacaoProcessual movimentacao,
                                                     DjePublicacao publicacaoDje,
                                                     LocalDate prazoComecaEm,
                                                     LocalDate prazoFinalEstimado) {
        LinkedHashMap<String, Object> dados = new LinkedHashMap<>();
        dados.put("processoId", processo.getId());
        dados.put("numeroProcesso", processo.getNumeroProcesso());
        dados.put("movimentacaoId", movimentacao.getId());
        dados.put("djePublicacaoId", publicacaoDje.getId());
        dados.put("tipoPrazo", TIPO_CIENCIA_DESPACHO.name());
        dados.put("dias", TIPO_CIENCIA_DESPACHO.prazoRespostaDias());
        dados.put("emDiasUteis", TIPO_CIENCIA_DESPACHO.emDiasUteis());
        dados.put("inicio", prazoComecaEm);
        dados.put("fimEstimado", prazoFinalEstimado);
        return dados;
    }

    private LinkedHashMap<String, Object> dadosCiencia(Processo processo,
                                                       MovimentacaoProcessual movimentacao,
                                                       DjePublicacao publicacaoDje,
                                                       CienciaProcessual ciencia) {
        LinkedHashMap<String, Object> dados = new LinkedHashMap<>();
        dados.put("processoId", processo.getId());
        dados.put("numeroProcesso", processo.getNumeroProcesso());
        dados.put("movimentacaoId", movimentacao.getId());
        dados.put("djePublicacaoId", publicacaoDje.getId());
        dados.put("cienciaId", ciencia.getId());
        dados.put("usuarioId", ciencia.getUsuario().getId());
        dados.put("poloProcessual", ciencia.getPoloProcessual());
        dados.put("status", ciencia.getStatus().name());
        dados.put("dataExpiracao", ciencia.getDataExpiracao());
        return dados;
    }

    private UUID salvarOutboxSeguro(String routingKey,
                                    String eventType,
                                    LinkedHashMap<String, Object> payload,
                                    String dedupKey,
                                    String aggregateType,
                                    String aggregateId) {
        try {
            OutboxEvent event = new OutboxEvent(
                    UUID.randomUUID(),
                    routingKey,
                    eventType,
                    objectMapper.writeValueAsString(payload),
                    Instant.now(),
                    dedupKey,
                    aggregateType,
                    aggregateId,
                    objectMapper.writeValueAsString(new LinkedHashMap<>(java.util.Map.of("origem", "PJB")))
            );
            return outboxEventRepository.save(event).getId();
        } catch (Exception ex) {
            return null;
        }
    }

    private GrauJurisdicaoCiencia resolverGrau(Processo processo) {
        String tribunal = normalize(firstNonBlank(processo.getTribunal(), processo.getTribunalCodigoRoteado(), ""));
        String vara = normalize(firstNonBlank(processo.getVara(), ""));
        if (tribunal.startsWith("STF")) {
            return GrauJurisdicaoCiencia.STF;
        }
        if (tribunal.startsWith("STJ") || tribunal.startsWith("TST") || tribunal.startsWith("TSE") || tribunal.startsWith("STM")) {
            return GrauJurisdicaoCiencia.TERCEIRO_GRAU;
        }
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL || vara.contains("CAMARA") || vara.contains("TURMA") || vara.contains("COLEGIADO")) {
            return GrauJurisdicaoCiencia.SEGUNDO_GRAU;
        }
        return GrauJurisdicaoCiencia.PRIMEIRO_GRAU;
    }

    private String ritoProcessual(Processo processo) {
        return processo.getRito() == null ? null : limite(processo.getRito().name(), 50);
    }

    private String instanciaTribunal(Processo processo) {
        return limite(normalize(firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), "PJB")), 20);
    }

    private Instant toInstant(LocalDate data) {
        return data.atStartOfDay(ZONE_ID).toInstant();
    }

    private String firstNonBlank(String... values) {
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

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
    }

    private String limite(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }
}
