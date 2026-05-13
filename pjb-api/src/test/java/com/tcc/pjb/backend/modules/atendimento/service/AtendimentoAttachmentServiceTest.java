package com.tcc.pjb.backend.modules.atendimento.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AtendimentoAttachmentServiceTest {

  @Test
  void normalizesPdfContentType() {
    Assertions.assertEquals("application/pdf", AtendimentoAttachmentService.normalizeContentType("application/pdf"));
    Assertions.assertEquals("application/pdf", AtendimentoAttachmentService.normalizeContentType("application/pdf; charset=utf-8"));
  }

  @Test
  void safeNameDefaults() {
    Assertions.assertEquals("arquivo.pdf", AtendimentoAttachmentService.safeName(null));
    Assertions.assertEquals("arquivo.pdf", AtendimentoAttachmentService.safeName("   "));
  }
}
