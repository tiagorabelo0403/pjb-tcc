package com.tcc.pjb.backend.service.secretariat.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventType;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.model.repository.ProcessEventRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.unified.eventsourcing.JudiciarioEventSourcingEngine;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretariatMinutaJuntadaPdfService {

  public record MinutaResult(byte[] pdfBytes,
                             NivelSigilo effectiveSecrecy,
                             long seqUsed,
                             QualifiedSignatureMetadata assinaturaQualificada,
                             SovereignValidationResult validacaoSoberana) {}

  public record JuntadaItem(long seq, java.time.Instant createdAt, String eventType, String label, int docCount, UUID eventoId) {}

  private final ProcessoRepository processoRepo;
  private final ProcessEventRepository eventRepo;
  private final DocumentoProcessualRepository docRepo;
  private final PjbAuthorizationService authz;
  private final RecursalEffectiveSecrecyService secrecyService;
  private final ObjectMapper mapper;
  private final CurrentUserService currentUserService;
  private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;

  public SecretariatMinutaJuntadaPdfService(
      ProcessoRepository processoRepo,
      ProcessEventRepository eventRepo,
      DocumentoProcessualRepository docRepo,
      PjbAuthorizationService authz,
      RecursalEffectiveSecrecyService secrecyService,
      ObjectMapper mapper,
      CurrentUserService currentUserService,
      QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService
  ) {
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.eventRepo = Objects.requireNonNull(eventRepo);
    this.docRepo = Objects.requireNonNull(docRepo);
    this.authz = Objects.requireNonNull(authz);
    this.secrecyService = Objects.requireNonNull(secrecyService);
    this.mapper = Objects.requireNonNull(mapper);
    this.currentUserService = Objects.requireNonNull(currentUserService);
    this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
  }

  @Transactional(readOnly = true)
  public MinutaResult gerarMinuta(Long processoId, Long seq) {
    Objects.requireNonNull(processoId, "processoId");

    Processo p = processoRepo.findById(processoId)
        .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));

    authz.requireReadProcesso(p);
    NivelSigilo efetivo = secrecyService.effectiveSecrecyForProcesso(processoId);
    authz.requireReadProcessoAtSecrecy(p, efetivo);

    ProcessEventEnvelope env = resolveEnvelope(processoId, seq);
    long seqUsed = env.getSeq() != null ? env.getSeq() : (seq != null ? seq : 0L);
    Usuario usuario = currentUserService.getRequired();

    JuntadaPayload juntada = parseJuntadaPayload(env);
    Map<UUID, DocumentoProcessual> docsById = loadDocs(juntada.documentos);

    List<DocRef> liberados = new ArrayList<>();
    List<DocRef> bloqueados = new ArrayList<>();

    for (JudiciarioEventSourcingEngine.DocumentoMetadata dm : juntada.documentos) {
      if (dm == null || dm.docId() == null) continue;
      DocumentoProcessual dp = docsById.get(dm.docId());
      boolean allowed = dp == null || authz.canReadDocumentoAtSecrecy(p, dp, efetivo).allowed();
      DocRef ref = DocRef.from(dm, dp);
      if (allowed) {
        liberados.add(ref);
      } else {
        bloqueados.add(ref.withNote("NÃO LIBERADO (sigilo/credencial)"));
      }
    }

    try {
      SignedDocumentEnvelope assinatura = signMinuta(p, usuario, env, juntada, liberados, bloqueados);
      byte[] pdf = renderPdf(p, efetivo, env, juntada, liberados, bloqueados, assinatura);
      return new MinutaResult(pdf, efetivo, seqUsed, assinatura.assinaturaQualificada(), assinatura.validacaoSoberana());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }



  @Transactional(readOnly = true)
  public List<JuntadaItem> listarJuntadas(Long processoId, Integer limit) {
    Objects.requireNonNull(processoId, "processoId");

    Processo p = processoRepo.findById(processoId)
        .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));

    authz.requireReadProcesso(p);
    NivelSigilo efetivo = secrecyService.effectiveSecrecyForProcesso(processoId);
    authz.requireReadProcessoAtSecrecy(p, efetivo);

    int lim = (limit == null || limit <= 0) ? 50 : Math.min(200, limit);
    List<String> types = List.of(
        ProcessEventType.DOCUMENTS_BULK_ADDED.name(),
        ProcessEventType.DOCUMENT_ADDED.name()
    );

    List<ProcessEventEnvelope> events = eventRepo.findRecentByProcessoIdAndTypes(processoId, types, PageRequest.of(0, lim));

    return events.stream().map(env -> {
      JuntadaPayload jp = parseJuntadaPayload(env);
      int cnt = (jp.documentos == null) ? 0 : jp.documentos.size();
      return new JuntadaItem(
          env.getSeq() != null ? env.getSeq() : 0L,
          env.getCreatedAt(),
          env.getEventType(),
          jp.label,
          cnt,
          jp.eventoId
      );
    }).toList();
  }
  private ProcessEventEnvelope resolveEnvelope(Long processoId, Long seq) {
    if (seq != null) {
      return eventRepo.findFirstByProcessoIdAndSeq(processoId, seq)
          .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado: processoId=" + processoId + " seq=" + seq));
    }

    ProcessEventEnvelope env = eventRepo
        .findFirstByProcessoIdAndEventTypeOrderBySeqDesc(processoId, ProcessEventType.DOCUMENTS_BULK_ADDED.name())
        .orElse(null);
    if (env != null) return env;

    return eventRepo
        .findFirstByProcessoIdAndEventTypeOrderBySeqDesc(processoId, ProcessEventType.DOCUMENT_ADDED.name())
        .orElseThrow(() -> new IllegalArgumentException("Nenhum evento de juntada encontrado para o processoId=" + processoId));
  }

  private record JuntadaPayload(String label, UUID eventoId, List<JudiciarioEventSourcingEngine.DocumentoMetadata> documentos) {}

  private JuntadaPayload parseJuntadaPayload(ProcessEventEnvelope env) {
    String type = env.getEventType();
    String payload = env.getPayload();
    if (payload == null || payload.isBlank()) {
      return new JuntadaPayload("JUNTADA", null, List.of());
    }

    if (ProcessEventType.DOCUMENTS_BULK_ADDED.name().equals(type)) {
      try {
        JudiciarioEventSourcingEngine.DocumentosJuntados dj = mapper.readValue(payload, JudiciarioEventSourcingEngine.DocumentosJuntados.class);
        return new JuntadaPayload("JUNTADA_EM_LOTE", dj.eventoId(), dj.documentos() != null ? dj.documentos() : List.of());
      } catch (Exception ignore) {
      }
    }

    try {
      JudiciarioEventSourcingEngine.DocumentosJuntados dj = mapper.readValue(payload, JudiciarioEventSourcingEngine.DocumentosJuntados.class);
      if (dj.documentos() != null && !dj.documentos().isEmpty()) {
        return new JuntadaPayload("JUNTADA", dj.eventoId(), dj.documentos());
      }
    } catch (Exception ignore) {
    }

    try {
      Map<?, ?> m = mapper.readValue(payload, Map.class);
      Object docIdRaw = m.get("docId");
      UUID docId = docIdRaw != null ? UUID.fromString(String.valueOf(docIdRaw)) : null;
      String storageUri = m.get("storageUri") != null ? String.valueOf(m.get("storageUri")) : null;
      String hashSha384 = m.get("hashSha384") != null ? String.valueOf(m.get("hashSha384")) : null;
      long size = 0L;
      Object sz = m.get("tamanhoBytes");
      if (sz instanceof Number n) size = n.longValue();
      else if (sz != null) {
        try { size = Long.parseLong(String.valueOf(sz)); } catch (Exception ignore) {}
      }
      List<JudiciarioEventSourcingEngine.DocumentoMetadata> docs = docId != null
          ? List.of(new JudiciarioEventSourcingEngine.DocumentoMetadata(docId, storageUri, hashSha384, size))
          : List.of();
      return new JuntadaPayload("JUNTADA", null, docs);
    } catch (Exception ignore) {
      return new JuntadaPayload("JUNTADA", null, List.of());
    }
  }

  private Map<UUID, DocumentoProcessual> loadDocs(List<JudiciarioEventSourcingEngine.DocumentoMetadata> metas) {
    if (metas == null || metas.isEmpty()) return Map.of();
    List<UUID> ids = metas.stream().map(JudiciarioEventSourcingEngine.DocumentoMetadata::docId).filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) return Map.of();
    return docRepo.findAllById(ids).stream().collect(Collectors.toMap(DocumentoProcessual::getId, x -> x));
  }

  private static class DocRef {
    final UUID docId;
    final String nome;
    final String contentType;
    final Long tamanho;
    final String sha256;
    final String sha384;
    final String hashSha384FromEvent;
    final String note;

    private DocRef(UUID docId, String nome, String contentType, Long tamanho, String sha256, String sha384, String hashSha384FromEvent, String note) {
      this.docId = docId;
      this.nome = nome;
      this.contentType = contentType;
      this.tamanho = tamanho;
      this.sha256 = sha256;
      this.sha384 = sha384;
      this.hashSha384FromEvent = hashSha384FromEvent;
      this.note = note;
    }

    static DocRef from(JudiciarioEventSourcingEngine.DocumentoMetadata dm, DocumentoProcessual dp) {
      UUID id = dm != null ? dm.docId() : null;
      String nome = dp != null ? firstNonBlank(dp.getTitulo(), dp.getNomeOriginal()) : null;
      String ct = dp != null ? dp.getContentType() : null;
      Long size = dp != null ? dp.getTamanhoBytes() : (dm != null ? dm.tamanhoBytes() : null);
      String s256 = dp != null ? dp.getSha256() : null;
      String s384 = dp != null ? dp.getSha384() : null;
      String ev384 = dm != null ? dm.hashSha384() : null;
      return new DocRef(id, nome, ct, size, s256, s384, ev384, null);
    }

    DocRef withNote(String n) {
      return new DocRef(this.docId, this.nome, this.contentType, this.tamanho, this.sha256, this.sha384, this.hashSha384FromEvent, n);
    }

    static String firstNonBlank(String a, String b) {
      if (a != null && !a.isBlank()) return a;
      if (b != null && !b.isBlank()) return b;
      return "(sem nome)";
    }
  }

  private static final class Cursor {
    final PDDocument doc;
    final PDRectangle pageSize;
    final float margin;
    PDPage page;
    PDPageContentStream cs;
    float y;

    Cursor(PDDocument doc, PDRectangle pageSize, float margin) throws IOException {
      this.doc = doc;
      this.pageSize = pageSize;
      this.margin = margin;
      this.page = new PDPage(pageSize);
      doc.addPage(page);
      this.cs = new PDPageContentStream(doc, page);
      this.y = pageSize.getHeight() - margin;
    }

    void ensureSpace(float needed) throws IOException {
      if (y >= margin + needed) return;
      cs.close();
      page = new PDPage(pageSize);
      doc.addPage(page);
      cs = new PDPageContentStream(doc, page);
      y = pageSize.getHeight() - margin;
    }

    void close() throws IOException {
      if (cs != null) cs.close();
    }
  }

  private SignedDocumentEnvelope signMinuta(Processo processo,
                                                                             Usuario usuario,
                                                                             ProcessEventEnvelope env,
                                                                             JuntadaPayload juntada,
                                                                             List<DocRef> liberados,
                                                                             List<DocRef> bloqueados) {
    List<String> lines = new ArrayList<>();
    lines.add("MINUTA DE JUNTADA PROCESSUAL");
    lines.add("processo_id=" + safe(processo == null ? null : processo.getId()));
    lines.add("processo_numero=" + safe(processo == null ? null : firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero())));
    lines.add("evento_seq=" + safe(env == null ? null : env.getSeq()));
    lines.add("evento_tipo=" + safe(env == null ? null : env.getEventType()));
    lines.add("juntada_label=" + safe(juntada == null ? null : juntada.label()));
    lines.add("evento_id=" + safe(juntada == null ? null : juntada.eventoId()));
    lines.add("anexos_liberados=" + (liberados == null ? 0 : liberados.size()));
    lines.add("anexos_bloqueados=" + (bloqueados == null ? 0 : bloqueados.size()));
    lines.add("payload_hash=" + safe(env == null ? null : env.getPayloadHash()));
    lines.add("chain_hash=" + safe(env == null ? null : env.getChainHash()));
    lines.add("prev_chain_hash=" + safe(env == null ? null : env.getPrevChainHash()));
    if (liberados != null) {
      int index = 1;
      for (DocRef ref : liberados) {
        lines.add("liberado_" + index + "=" + safe(ref.docId) + "|" + safe(ref.nome) + "|" + safe(ref.sha256));
        index++;
      }
    }
    if (bloqueados != null) {
      int index = 1;
      for (DocRef ref : bloqueados) {
        lines.add("bloqueado_" + index + "=" + safe(ref.docId) + "|" + safe(ref.note));
        index++;
      }
    }
    return qualifiedDocumentSignatureEnvelopeService.signFreeContent(
        processo,
        usuario,
        "Minuta de juntada — " + safe(processo == null ? null : firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero())),
        String.join("\n", lines),
        "UNIDADE_JUDICIAL",
        "ATO_OFICIAL_QUALIFICADO_SOBERANO",
        true,
        List.of(
            "minuta_juntada_pdf",
            "secretaria_judicial",
            "envelope_qualificado_pdf"
        )
    );
  }

  private static byte[] renderPdf(Processo p,
                                  NivelSigilo efetivo,
                                  ProcessEventEnvelope env,
                                  JuntadaPayload juntada,
                                  List<DocRef> liberados,
                                  List<DocRef> bloqueados,
                                  SignedDocumentEnvelope assinatura) throws IOException {

    try (PDDocument doc = new PDDocument()) {
      setMetadata(doc, p, efetivo, env, juntada, assinatura);

      PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
      float fontSize = 11f;
      float leading = 14f;
      PDRectangle pageSize = PDRectangle.A4;
      float margin = 48f;
      float width = pageSize.getWidth() - 2 * margin;

      Cursor cur = new Cursor(doc, pageSize, margin);

      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

      Jurisdicao j = p.getJurisdicao();
      String uf = j != null ? j.getUf() : null;
      String orgaoNome = j != null ? j.getNome() : null;
      String orgaoSigla = j != null ? j.getSigla() : null;
      String comarca = j != null ? j.getCidade() : null;

      cur.ensureSpace(leading * 8);
      cur.y = writeLine(cur.cs, fontBold, 13f, margin, cur.y, "PODER JUDICIÁRIO", leading);
      String orgaoLine = "Órgão: " + safe(orgaoSigla) + (orgaoNome != null ? " - " + orgaoNome : "");
      cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, orgaoLine, leading);
      cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "UF: " + safe(uf) + (comarca != null ? " | Comarca: " + comarca : ""), leading);
      cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Processo: " + safe(p.getNumeroUnificado()) + " (id=" + p.getId() + ")", leading);
      if (p.getClasseProcessual() != null && !p.getClasseProcessual().isBlank()) {
        cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Classe: " + p.getClasseProcessual(), leading);
      }
      if (p.getAssunto() != null && !p.getAssunto().isBlank()) {
        cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Assunto: " + p.getAssunto(), leading);
      }
      cur.y -= leading / 2;

      cur.ensureSpace(leading * 8);
      cur.y = writeLine(cur.cs, fontBold, 14f, margin, cur.y, "MINUTA DE JUNTADA", leading);
      cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y,
          "Tipo: " + safe(juntada.label()) + " | Seq: " + safe(env.getSeq()) + " | Data: " + (env.getCreatedAt() != null ? fmt.format(env.getCreatedAt()) : ""),
          leading);
      cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "EventoType: " + safe(env.getEventType()) + " | payloadHash(SHA-256): " + safe(env.getPayloadHash()), leading);
      cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "prevChain(SHA-384): " + safe(env.getPrevChainHash()), leading);
      cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "chainHash(SHA-384): " + safe(env.getChainHash()), leading);
      if (env.getActorUserId() != null || env.getActorRole() != null) {
        cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Ator: userId=" + safe(env.getActorUserId()) + " role=" + safe(env.getActorRole()), leading);
      }

      cur.y -= leading;

      
      cur.ensureSpace(leading * 2);
      cur.y = writeLine(cur.cs, fontBold, 12f, margin, cur.y, "Anexos liberados (referência)", leading);

      int idx = 0;
      for (DocRef d : liberados) {
        idx++;
        List<String> lines = new ArrayList<>();
        lines.add(idx + ") docId=" + safe(d.docId) + (d.nome != null ? " | " + d.nome : ""));
        String meta = "contentType=" + safe(d.contentType) + " size=" + (d.tamanho != null ? d.tamanho : "")
            + " sha256=" + safe(d.sha256) + " sha384=" + safe(d.sha384);
        lines.addAll(splitText(meta, font, fontSize, width));
        if (d.hashSha384FromEvent != null && !d.hashSha384FromEvent.isBlank()) {
          lines.addAll(splitText("hashSha384(event)=" + d.hashSha384FromEvent, font, fontSize, width));
        }
        if (d.note != null && !d.note.isBlank()) {
          lines.addAll(splitText("nota=" + d.note, font, fontSize, width));
        }

        for (String ln : lines) {
          cur.ensureSpace(leading * 2);
          cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, ln, leading);
        }
        cur.y -= leading / 2;
      }

      if (!bloqueados.isEmpty()) {
        cur.ensureSpace(leading * 3);
        cur.y = writeLine(cur.cs, fontBold, 12f, margin, cur.y, "Anexos NÃO liberados (não referenciados na minuta)", leading);
        for (DocRef d : bloqueados) {
          cur.ensureSpace(leading * 2);
          String ln = "- docId=" + safe(d.docId) + (d.note != null ? " | " + d.note : "");
          cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, ln, leading);
        }
        cur.y -= leading;
      }

      cur.ensureSpace(leading * 6);
      cur.y = writeLine(cur.cs, fontBold, 12f, margin, cur.y, "Verificação", leading);
      cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Esta minuta referencia anexos liberados e registra o hash-chain do Event Store do processo.", leading);
      cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Para auditar: compare payloadHash e chainHash com tb_processo_event (processo_id, seq).", leading);
      if (efetivo != null && efetivo.exigeCredencial()) {
        cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Sigilo efetivo: " + efetivo.name() + " (cache: no-store recomendado).", leading);
      }

      if (assinatura != null) {
        cur.y -= leading;
        cur.ensureSpace(leading * 10);
        cur.y = writeLine(cur.cs, fontBold, 12f, margin, cur.y, "Assinatura qualificada", leading);
        cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Rubrica: " + safe(assinatura.assinaturaQualificada().rubricaEletronica()), leading);
        cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Data: " + safe(assinatura.assinaturaQualificada().data()) + " | Hora: " + safe(assinatura.assinaturaQualificada().hora()), leading);
        cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Local: " + safe(assinatura.assinaturaQualificada().local()), leading);
        cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Envelope: " + safe(assinatura.assinaturaQualificada().envelopeId()), leading);
        cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Hash assinado: " + safe(assinatura.validacaoSoberana().documentoAssinadoHash()), leading);
        cur.y = writeLine(cur.cs, font, fontSize, margin, cur.y, "Validação soberana: " + safe(assinatura.validacaoSoberana().status()) + " | Fonte: " + safe(assinatura.validacaoSoberana().fonte()), leading);
      }

      cur.close();

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      doc.save(bos);
      return bos.toByteArray();
    }
  }

  private static void setMetadata(PDDocument doc,
                                  Processo p,
                                  NivelSigilo efetivo,
                                  ProcessEventEnvelope env,
                                  JuntadaPayload juntada,
                                  SignedDocumentEnvelope assinatura) {
    PDDocumentInformation info = new PDDocumentInformation();
    String num = p != null ? p.getNumeroUnificado() : null;
    info.setTitle("Minuta de Juntada" + (num != null ? " - " + num : ""));
    info.setSubject("Minuta de juntada - PJB");
    info.setAuthor("PJB - Secretaria");
    info.setCreator("PJB (Java 21)");
    info.setKeywords("PJB, Minuta, Juntada, HashChain");

    if (p != null && p.getId() != null) {
      info.setCustomMetadataValue("PJB-Processo-Id", String.valueOf(p.getId()));
      info.setCustomMetadataValue("PJB-Processo-Numero", String.valueOf(p.getNumeroUnificado()));
    }
    if (env != null) {
      if (env.getSeq() != null) info.setCustomMetadataValue("PJB-Evento-Seq", String.valueOf(env.getSeq()));
      if (env.getEventType() != null) info.setCustomMetadataValue("PJB-Evento-Type", env.getEventType());
      if (env.getPayloadHash() != null) info.setCustomMetadataValue("PJB-Payload-Hash", env.getPayloadHash());
      if (env.getChainHash() != null) info.setCustomMetadataValue("PJB-Chain-Hash", env.getChainHash());
      if (env.getPrevChainHash() != null) info.setCustomMetadataValue("PJB-PrevChain-Hash", env.getPrevChainHash());
    }
    if (juntada != null && juntada.eventoId() != null) {
      info.setCustomMetadataValue("PJB-Evento-Id", juntada.eventoId().toString());
    }
    if (efetivo != null) {
      info.setCustomMetadataValue("PJB-Sigilo-Efetivo", efetivo.name());
    }
    if (assinatura != null) {
      info.setCustomMetadataValue("PJB-Envelope-Assinatura", safe(assinatura.assinaturaQualificada().envelopeId()));
      info.setCustomMetadataValue("PJB-Rubrica", safe(assinatura.assinaturaQualificada().rubricaEletronica()));
      info.setCustomMetadataValue("PJB-Documento-Assinado-Hash", safe(assinatura.validacaoSoberana().documentoAssinadoHash()));
      info.setCustomMetadataValue("PJB-Validacao-Soberana", safe(assinatura.validacaoSoberana().status()));
    }

    doc.setDocumentInformation(info);
  }

  private static float writeLine(PDPageContentStream cs, PDFont font, float fontSize, float x, float y, String text, float leading) throws IOException {
    cs.beginText();
    cs.setFont(font, fontSize);
    cs.newLineAtOffset(x, y);
    cs.showText(text != null ? text : "");
    cs.endText();
    return y - leading;
  }

  private static List<String> splitText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
    if (text == null) return List.of();
    String cleaned = text.replace("\r", "").replace("\t", " ");
    String[] words = cleaned.split("\\s+");
    List<String> lines = new ArrayList<>();
    StringBuilder line = new StringBuilder();

    for (String w : words) {
      if (w.isEmpty()) continue;
      String candidate = line.isEmpty() ? w : line + " " + w;
      float width = font.getStringWidth(candidate) / 1000f * fontSize;
      if (width <= maxWidth) {
        line.setLength(0);
        line.append(candidate);
      } else {
        if (!line.isEmpty()) {
          lines.add(line.toString());
          line.setLength(0);
          line.append(w);
        } else {
          lines.add(trimToWidth(w, font, fontSize, maxWidth));
        }
      }
    }

    if (!line.isEmpty()) lines.add(line.toString());
    return lines;
  }

  private static String trimToWidth(String word, PDFont font, float fontSize, float maxWidth) throws IOException {
    if (word == null) return "";
    String w = word;
    while (!w.isEmpty()) {
      float width = font.getStringWidth(w) / 1000f * fontSize;
      if (width <= maxWidth) return w;
      w = w.substring(0, w.length() - 1);
    }
    return "";
  }

  private static String safe(Object o) {
    return o != null ? String.valueOf(o) : "";
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }
}
