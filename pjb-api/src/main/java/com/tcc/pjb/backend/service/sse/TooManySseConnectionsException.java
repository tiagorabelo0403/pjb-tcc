package com.tcc.pjb.backend.service.sse;

public class TooManySseConnectionsException extends RuntimeException {
  public TooManySseConnectionsException(String message) {
    super(message);
  }
}
