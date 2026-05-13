package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessage;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageStatus;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoChecklistItemRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoChecklistAuditEventRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoReportService {

  private final CurrentUserService currentUser;
  private final AtendimentoChatService chat;
  private final AtendimentoTosService tos;
  private final AtendimentoThreadRepository threadRepo;
  private final AtendimentoMessageRepository messageRepo;
  private final AtendimentoMessageAttachmentRepository msgAttRepo;
  private final AtendimentoAttachmentRepository attachmentRepo;
  private final ProcessoRepository processoRepo;
  private final UsuarioRepository usuarioRepo;
  private final AtendimentoChecklistItemRepository checklistItemRepo;
  private final AtendimentoChecklistAuditEventRepository checklistAuditRepo;

  public AtendimentoReportService(CurrentUserService currentUser,
                                 AtendimentoChatService chat,
                                 AtendimentoTosService tos,
                                 AtendimentoThreadRepository threadRepo,
                                 AtendimentoMessageRepository messageRepo,
                                 AtendimentoMessageAttachmentRepository msgAttRepo,
                                 AtendimentoAttachmentRepository attachmentRepo,
                                 ProcessoRepository processoRepo,
                                 UsuarioRepository usuarioRepo,
                                 AtendimentoChecklistItemRepository checklistItemRepo,
                                 AtendimentoChecklistAuditEventRepository checklistAuditRepo) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.chat = Objects.requireNonNull(chat);
    this.tos = Objects.requireNonNull(tos);
    this.threadRepo = Objects.requireNonNull(threadRepo);
    this.messageRepo = Objects.requireNonNull(messageRepo);
    this.msgAttRepo = Objects.requireNonNull(msgAttRepo);
    this.attachmentRepo = Objects.requireNonNull(attachmentRepo);
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.usuarioRepo = Objects.requireNonNull(usuarioRepo);
    this.checklistItemRepo = Objects.requireNonNull(checklistItemRepo);
    this.checklistAuditRepo = Objects.requireNonNull(checklistAuditRepo);
  }

  @Transactional(readOnly = true)
  public byte[] threadPdf(Long threadId, LocalDate from, LocalDate to) {
    Usuario requester = currentUser.getRequired();
    AtendimentoThread t = threadRepo.findById(threadId).orElseThrow();
    if (!isInstitutional(requester)) {
      tos.requireAccepted();
      chat.requireThreadAccess(threadId);
    }

    Processo pr = t.getProcessoId() != null ? processoRepo.findById(t.getProcessoId()).orElse(null) : null;
    Usuario adv = usuarioRepo.findById(t.getAdvogadoId()).orElse(null);
    Usuario cid = usuarioRepo.findById(t.getCidadaoUsuarioId()).orElse(null);

    Instant f = from != null ? from.atStartOfDay(ZoneId.of("UTC")).toInstant() : null;
    Instant tt = to != null ? to.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant() : null;

    List<AtendimentoMessage> messages = loadMessages(threadId, f, tt);
    Map<Long, List<AtendimentoAttachment>> atts = loadAttachments(messages);

    try {
      return renderThreadPdf(t, pr, adv, cid, messages, atts);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional(readOnly = true)
  

  public byte[] threadAtaPdf(Long threadId, java.time.LocalDate from, java.time.LocalDate to) {
    
    AtendimentoThread t = threadRepo.findById(threadId).orElseThrow();
    List<AtendimentoMessage> messages = loadMessagesForReport(threadId, from, to);
    Map<Long, java.util.List<AtendimentoAttachment>> attsByMsg = loadAttachmentsByMessage(messages);

    try (PDDocument doc = new PDDocument()) {
      
      var info = doc.getDocumentInformation();
      info.setTitle("Ata do Atendimento - Thread " + threadId);
      info.setAuthor("PJB");
      info.setCreator("PJB/AtendimentoReportService");
      info.setSubject("Ata do chat com hash chain e referencias de anexos");

      PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      float fontSize = 10f;
      float leading = 1.35f * fontSize;
      PDRectangle pageSize = PDRectangle.A4;
      float margin = 48;
      float width = pageSize.getWidth() - 2 * margin;

      java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.systemDefault());
      String generatedAt = fmt.format(java.time.Instant.now());

      
      PDPage page = new PDPage(pageSize);
      doc.addPage(page);
      PDPageContentStream cs = new PDPageContentStream(doc, page);
      float y = pageSize.getHeight() - margin;

      y = writeLine(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14f, margin, y, "ATA DO ATENDIMENTO (CHAT)", 18f);
      y = writeLine(cs, font, fontSize, margin, y, "ThreadId: " + threadId + "  ProcessoId: " + safe(String.valueOf(t.getProcessoId())), leading);
      y = writeLine(cs, font, fontSize, margin, y, "Gerada em: " + generatedAt, leading);
      y = writeLine(cs, font, fontSize, margin, y, "Participantes: cidadaoUsuarioId=" + safe(String.valueOf(t.getCidadaoUsuarioId())) + " advogadoId=" + safe(String.valueOf(t.getAdvogadoId())), leading);
      y -= leading;

      
      String brokenAt = null;
      String prev = null;
      java.util.List<String> chainLines = new java.util.ArrayList<>();
      for (AtendimentoMessage m : messages) {
        String expected = Hashes.sha256Hex((prev != null ? prev : "") + "|" + safe(String.valueOf(m.getThreadId())) + "|" + safe(String.valueOf(m.getSenderUsuarioId())) + "|" + safe(m.getSenderTipo()) + "|" + (m.getCreatedAt() != null ? String.valueOf(m.getCreatedAt().toEpochMilli()) : "") + "|" + safe(m.getBody()) + "|" + safe(String.valueOf(m.getReplyToMessageId())));
        
        if (brokenAt == null && prev != null && m.getPrevHash() != null && !prev.equals(m.getPrevHash())) {
          brokenAt = String.valueOf(m.getId());
        }
        chainLines.add(safe(m.getMsgHash()));
        prev = m.getMsgHash();
      }
      String payloadHash = Hashes.sha256Hex(String.join("\n", chainLines));
      info.setCustomMetadataValue("PJB-Payload-Hash", payloadHash);
      info.setCustomMetadataValue("PJB-ThreadId", String.valueOf(threadId));
      info.setCustomMetadataValue("PJB-GeneratedAt", generatedAt);

      y = writeLine(cs, font, fontSize, margin, y, "payloadHash=" + payloadHash, leading);
      y = writeLine(cs, font, fontSize, margin, y, "Integridade: " + (brokenAt == null ? "hash chain OK" : "hash chain com quebra em messageId=" + brokenAt), leading);
      y -= leading;

      
      for (AtendimentoMessage m : messages) {
        java.util.List<String> header = java.util.List.of(
            "[" + (m.getCreatedAt() != null ? fmt.format(m.getCreatedAt()) : "") + "]",
            "sender=" + m.getSenderTipo() + "#" + m.getSenderUsuarioId(),
            "status=" + (m.getStatus() != null ? m.getStatus().name() : ""),
            "id=" + m.getId()
        );
        String headerLine = String.join(" ", header);

        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(headerLine);

        String body = includeBody(m.getStatus()) ? m.getBody() : null;
        if (body == null || body.isBlank()) {
          lines.add("(conteudo indisponivel)");
        } else {
          lines.addAll(splitText(body, font, fontSize, width));
        }

        lines.add("hash=" + safe(m.getMsgHash()) + " prev=" + safe(m.getPrevHash()));

        java.util.List<AtendimentoAttachment> atts = attsByMsg.getOrDefault(m.getId(), java.util.List.of());
        if (!atts.isEmpty()) {
          lines.add("anexos (referencia):");
          for (AtendimentoAttachment a : atts) {
            String s = "- " + a.getFileName() + " " + a.getContentType() + " " + a.getStatus() + " sha256=" + (a.getSha256() != null ? a.getSha256() : "");
            lines.addAll(splitText(s, font, fontSize, width));
          }
        }

        for (String ln : lines) {
          if (y < margin + leading) {
            cs.close();
            page = new PDPage(pageSize);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = pageSize.getHeight() - margin;
          }
          y = writeLine(cs, font, fontSize, margin, y, ln, leading);
        }
        y -= leading;
      }



      
      java.util.List<com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoChecklistItem> chk = checklistItemRepo.findByThreadIdOrderByIdAsc(threadId);
      if (chk != null && !chk.isEmpty()) {
        if (y < margin + 6 * leading) {
          cs.close();
          page = new PDPage(pageSize);
          doc.addPage(page);
          cs = new PDPageContentStream(doc, page);
          y = pageSize.getHeight() - margin;
        }

        y = writeLine(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12f, margin, y, "CHECKLIST DO THREAD", leading);
        String chkChain = checklistAuditRepo.findTopByThreadIdOrderByIdDesc(threadId)
            .map(com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoChecklistAuditEvent::getChainHash)
            .orElse(null);
        if (chkChain != null) {
          y = writeLine(cs, font, fontSize, margin, y, "checklistChainHash=" + chkChain, leading);
          info.setCustomMetadataValue("PJB-Checklist-Chain-Hash", chkChain);
        }
        y -= leading;

        for (var it : chk) {
          StringBuilder lineSb = new StringBuilder();
          lineSb.append("[").append(it.getStatus() != null ? it.getStatus().name() : "").append("] ")
              .append(it.getKind() != null ? it.getKind().name() : "OUTRO")
              .append(" • ").append(safe(it.getTitle()));
          if (it.getDueAt() != null) {
            lineSb.append(" • vencimento=").append(fmt.format(it.getDueAt()));
          }
          if (it.getDocumentoId() != null) {
            lineSb.append(" • documentoId=").append(it.getDocumentoId());
          }
          String line = lineSb.toString();

          for (String ln : splitText(line, font, fontSize, width)) {
            if (y < margin + leading) {
              cs.close();
              page = new PDPage(pageSize);
              doc.addPage(page);
              cs = new PDPageContentStream(doc, page);
              y = pageSize.getHeight() - margin;
            }
            y = writeLine(cs, font, fontSize, margin, y, ln, leading);
          }

          if (it.getNote() != null && !it.getNote().isBlank()) {
            for (String ln : splitText("nota: " + it.getNote(), font, fontSize, width)) {
              if (y < margin + leading) {
                cs.close();
                page = new PDPage(pageSize);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                y = pageSize.getHeight() - margin;
              }
              y = writeLine(cs, font, fontSize, margin, y, ln, leading);
            }
          }

          if (it.getCompletedAt() != null) {
            y = writeLine(cs, font, fontSize, margin, y, "concluidoEm=" + fmt.format(it.getCompletedAt()), leading);
          }
          if (it.getCancelledAt() != null) {
            y = writeLine(cs, font, fontSize, margin, y, "canceladoEm=" + fmt.format(it.getCancelledAt()), leading);
          }

          y -= leading;
        }
      }
      cs.close();
      java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
      doc.save(bos);
      return bos.toByteArray();
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  private java.util.List<AtendimentoMessage> loadMessagesForReport(Long threadId, java.time.LocalDate from, java.time.LocalDate to) {
    if (from != null && to != null) {
      java.time.Instant a = from.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
      java.time.Instant b = to.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
      return messageRepo.findByThreadIdAndCreatedAtBetweenOrderByIdAsc(threadId, a, b);
    }
    return messageRepo.findByThreadIdOrderByIdAsc(threadId);
  }

  private java.util.Map<Long, java.util.List<AtendimentoAttachment>> loadAttachmentsByMessage(java.util.List<AtendimentoMessage> messages) {
    if (messages == null || messages.isEmpty()) return java.util.Map.of();
    java.util.List<Long> msgIds = messages.stream().map(AtendimentoMessage::getId).filter(java.util.Objects::nonNull).toList();
    if (msgIds.isEmpty()) return java.util.Map.of();
    java.util.Map<Long, java.util.List<Long>> attIdsByMsg = new java.util.HashMap<>();
    for (AtendimentoMessageAttachment ma : msgAttRepo.findByMessageIds(msgIds)) {
      attIdsByMsg.computeIfAbsent(ma.getId().getMessageId(), k -> new java.util.ArrayList<>()).add(ma.getId().getAttachmentId());
    }
    java.util.Set<Long> allAttIds = attIdsByMsg.values().stream().flatMap(java.util.List::stream).collect(java.util.stream.Collectors.toSet());
    java.util.Map<Long, AtendimentoAttachment> attMap = allAttIds.isEmpty() ? java.util.Map.of() : attachmentRepo.findAllById(allAttIds)
        .stream().collect(java.util.stream.Collectors.toMap(AtendimentoAttachment::getId, x -> x));

    java.util.Map<Long, java.util.List<AtendimentoAttachment>> out = new java.util.HashMap<>();
    for (var e : attIdsByMsg.entrySet()) {
      java.util.List<AtendimentoAttachment> atts = e.getValue().stream().map(attMap::get).filter(java.util.Objects::nonNull).toList();
      out.put(e.getKey(), atts);
    }
    return out;
  }
public byte[] processoZip(Long processoId, LocalDate from, LocalDate to) {
    Usuario requester = currentUser.getRequired();

    if (!isInstitutional(requester)) {
      tos.requireAccepted();
    }

    List<AtendimentoThread> threads;
    if (isInstitutional(requester)) {
      threads = threadRepo.findByProcessoIdOrderByUpdatedAtDesc(processoId);
    } else if (requester.getTipoUsuario() == TipoUsuario.ADVOGADO) {
      threads = threadRepo.findByProcessoIdAndAdvogadoIdOrderByUpdatedAtDesc(processoId, requester.getId());
    } else if (requester.getTipoUsuario() == TipoUsuario.CIDADAO) {
      threads = threadRepo.findByProcessoIdAndCidadaoUsuarioIdOrderByUpdatedAtDesc(processoId, requester.getId());
    } else {
      throw new AccessDeniedException("Acesso negado");
    }

    if (threads.isEmpty()) throw new AccessDeniedException("Acesso negado");

    Processo pr = processoRepo.findById(processoId).orElse(null);

    Instant f = from != null ? from.atStartOfDay(ZoneId.of("UTC")).toInstant() : null;
    Instant tt = to != null ? to.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant() : null;

    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(bos)) {
      String base = pr != null && pr.getNumeroUnificado() != null ? pr.getNumeroUnificado().replaceAll("[^0-9A-Za-z.-]", "_") : "processo_" + processoId;

      ZipEntry manifest = new ZipEntry(base + "/manifest.txt");
      zos.putNextEntry(manifest);
      zos.write(buildManifest(pr, threads, from, to).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      zos.closeEntry();

      for (AtendimentoThread t : threads) {
        Long tid = t.getId();
        Usuario adv = usuarioRepo.findById(t.getAdvogadoId()).orElse(null);
        Usuario cid = usuarioRepo.findById(t.getCidadaoUsuarioId()).orElse(null);

        List<AtendimentoMessage> messages = loadMessages(tid, f, tt);
        Map<Long, List<AtendimentoAttachment>> atts = loadAttachments(messages);
        byte[] pdf = renderThreadPdf(t, pr, adv, cid, messages, atts);

        String name = base + "/thread_" + tid + ".pdf";
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(pdf);
        zos.closeEntry();
      }

      zos.finish();
      return bos.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private List<AtendimentoMessage> loadMessages(Long threadId, Instant from, Instant to) {
    if (from != null && to != null) {
      return messageRepo.findByThreadIdAndCreatedAtBetweenOrderByIdAsc(threadId, from, to);
    }
    return messageRepo.findByThreadIdOrderByIdAsc(threadId);
  }

  private Map<Long, List<AtendimentoAttachment>> loadAttachments(List<AtendimentoMessage> messages) {
    if (messages == null || messages.isEmpty()) return Map.of();
    List<Long> msgIds = messages.stream().map(AtendimentoMessage::getId).filter(Objects::nonNull).toList();
    Map<Long, List<Long>> attIdsByMsg = new HashMap<>();
    for (AtendimentoMessageAttachment ma : msgAttRepo.findByMessageIds(msgIds)) {
      attIdsByMsg.computeIfAbsent(ma.getId().getMessageId(), k -> new ArrayList<>()).add(ma.getId().getAttachmentId());
    }

    Set<Long> allAttIds = attIdsByMsg.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    Map<Long, AtendimentoAttachment> attMap = allAttIds.isEmpty() ? Map.of() : attachmentRepo.findAllById(allAttIds).stream().collect(Collectors.toMap(AtendimentoAttachment::getId, x -> x));

    Map<Long, List<AtendimentoAttachment>> out = new HashMap<>();
    for (Map.Entry<Long, List<Long>> e : attIdsByMsg.entrySet()) {
      List<AtendimentoAttachment> atts = e.getValue().stream().map(attMap::get).filter(Objects::nonNull).toList();
      out.put(e.getKey(), atts);
    }
    return out;
  }

  private static String buildManifest(Processo pr, List<AtendimentoThread> threads, LocalDate from, LocalDate to) {
    StringBuilder sb = new StringBuilder();
    sb.append("PJB Atendimento Export\n");
    if (pr != null) {
      sb.append("Processo: ").append(pr.getNumeroUnificado()).append("\n");
    }
    if (from != null) sb.append("De: ").append(from).append("\n");
    if (to != null) sb.append("Ate: ").append(to).append("\n");
    sb.append("Threads: ").append(threads.size()).append("\n");
    for (AtendimentoThread t : threads) {
      sb.append("- thread_").append(t.getId()).append(".pdf");
      sb.append(" cidadao=").append(t.getCidadaoUsuarioId());
      sb.append(" advogado=").append(t.getAdvogadoId());
      sb.append("\n");
    }
    return sb.toString();
  }

  private static byte[] renderThreadPdf(AtendimentoThread t,
                                       Processo pr,
                                       Usuario adv,
                                       Usuario cid,
                                       List<AtendimentoMessage> messages,
                                       Map<Long, List<AtendimentoAttachment>> attsByMsg) throws IOException {
    try (PDDocument doc = new PDDocument()) {
      PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      float fontSize = 11f;
      float leading = 14f;
      PDRectangle pageSize = PDRectangle.A4;
      float margin = 48f;
      float width = pageSize.getWidth() - 2 * margin;

      PDPage page = new PDPage(pageSize);
      doc.addPage(page);
      PDPageContentStream cs = new PDPageContentStream(doc, page);
      float y = pageSize.getHeight() - margin;

      y = writeLine(cs, font, 14f, margin, y, "Relatorio de Atendimento", leading);
      y = writeLine(cs, font, fontSize, margin, y, "Thread: " + t.getId(), leading);
      if (pr != null) {
        y = writeLine(cs, font, fontSize, margin, y, "Processo: " + pr.getNumeroUnificado(), leading);
      } else {
        y = writeLine(cs, font, fontSize, margin, y, "ProcessoId: " + t.getProcessoId(), leading);
      }
      y = writeLine(cs, font, fontSize, margin, y, "Cidadao: " + (cid != null ? cid.getNome() : t.getCidadaoUsuarioId()), leading);
      y = writeLine(cs, font, fontSize, margin, y, "Advogado: " + (adv != null ? adv.getNome() : t.getAdvogadoId()), leading);
      y = writeLine(cs, font, fontSize, margin, y, "Exportado em: " + Instant.now().toString(), leading);
      y -= leading;

      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("UTC"));

      String prev = null;
      Long brokenAt = null;
      for (AtendimentoMessage m : messages) {
        if (prev != null && brokenAt == null && !Objects.equals(m.getPrevHash(), prev)) {
          brokenAt = m.getId();
        }
        prev = m.getMsgHash();

        List<String> header = List.of(
            "[" + (m.getCreatedAt() != null ? fmt.format(m.getCreatedAt()) : "") + "]",
            "sender=" + m.getSenderTipo() + "#" + m.getSenderUsuarioId(),
            "status=" + (m.getStatus() != null ? m.getStatus().name() : ""),
            "id=" + m.getId()
        );
        String headerLine = String.join(" ", header);

        List<String> lines = new ArrayList<>();
        lines.add(headerLine);

        String body = includeBody(m.getStatus()) ? m.getBody() : null;
        if (body == null || body.isBlank()) {
          lines.add("(conteudo indisponivel)");
        } else {
          lines.addAll(splitText(body, font, fontSize, width));
        }

        lines.add("hash=" + safe(m.getMsgHash()) + " prev=" + safe(m.getPrevHash()));

        List<AtendimentoAttachment> atts = attsByMsg.getOrDefault(m.getId(), List.of());
        if (!atts.isEmpty()) {
          lines.add("anexos:");
          for (AtendimentoAttachment a : atts) {
            String s = "- " + a.getFileName() + " " + a.getContentType() + " " + a.getStatus() + " sha256=" + (a.getSha256() != null ? a.getSha256() : "");
            lines.addAll(splitText(s, font, fontSize, width));
          }
        }

        for (String ln : lines) {
          if (y < margin + leading) {
            cs.close();
            page = new PDPage(pageSize);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = pageSize.getHeight() - margin;
          }
          y = writeLine(cs, font, fontSize, margin, y, ln, leading);
        }
        y -= leading;
      }

      if (y < margin + leading * 4) {
        cs.close();
        page = new PDPage(pageSize);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        y = pageSize.getHeight() - margin;
      }
      y = writeLine(cs, font, fontSize, margin, y, "Integridade:", leading);
      if (brokenAt == null) {
        writeLine(cs, font, fontSize, margin, y, "hash chain OK", leading);
      } else {
        writeLine(cs, font, fontSize, margin, y, "hash chain com quebra em messageId=" + brokenAt, leading);
      }

      cs.close();

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      doc.save(bos);
      return bos.toByteArray();
    }
  }

  private static float writeLine(PDPageContentStream cs, PDFont font, float fontSize, float x, float y, String text, float leading) throws IOException {
    cs.beginText();
    cs.setFont(font, fontSize);
    cs.newLineAtOffset(x, y);
    cs.showText(text != null ? text : "");
    cs.endText();
    return y - leading;
  }

  private static boolean includeBody(AtendimentoMessageStatus status) {
    return status == AtendimentoMessageStatus.DELIVERED;
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

  private static String safe(String s) {
    return s != null ? s : "";
  }

  private static boolean isInstitutional(Usuario u) {
    TipoUsuario t = u != null ? u.getTipoUsuario() : null;
    if (t == null) return false;
    return t.isAdmin() || t.isMagistratura() || t.isServidorJudiciario();
  }
}
