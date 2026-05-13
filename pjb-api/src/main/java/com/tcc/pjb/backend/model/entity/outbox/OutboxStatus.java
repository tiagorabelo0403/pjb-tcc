package com.tcc.pjb.backend.model.entity.outbox;

public enum OutboxStatus {
  PENDING,
  INFLIGHT,
  DONE,
  FAILED
}
