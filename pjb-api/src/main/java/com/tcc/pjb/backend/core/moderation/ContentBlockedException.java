package com.tcc.pjb.backend.core.moderation;

public class ContentBlockedException extends RuntimeException {

  private final String reason;

  public ContentBlockedException(String reason) {
    super("Conteúdo bloqueado");
    this.reason = reason;
  }

  public String reason() {
    return reason;
  }
}
