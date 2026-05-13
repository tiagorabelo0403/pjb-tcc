package com.tcc.pjb.backend.integration.govbr.mock;

import com.tcc.pjb.backend.core.util.DeterministicUuid;
import com.tcc.pjb.backend.model.dto.GovBrFlowStartResponse;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "pjb.integrations.govbr", name = "mock-enabled", havingValue = "true", matchIfMissing = false)
public class GovBrMockSignatureService {

  public GovBrFlowStartResponse iniciarAssinatura(Long id, String html, String url) {
    Objects.requireNonNull(id, "Identificador do documento é obrigatório");
    String documentUrl = buildSignedDocumentUrl(id, html, url);
    return new GovBrFlowStartResponse(documentUrl);
  }

  private String buildSignedDocumentUrl(Long id, String html, String url) {
    String fingerprint = DeterministicUuid.v5("govbr-mock-signature", String.valueOf(id), normalize(html), normalize(url)).toString();
    return "/govbr/mock/assinatura/" + fingerprint;
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
