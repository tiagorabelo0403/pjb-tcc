package com.tcc.pjb.backend.service.secretariat.ingest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventType;
import com.tcc.pjb.backend.core.kernel.process.events.ProcessEventAppendedEvent;

@Component
@ConditionalOnProperty(name = "pjb.secretariat.ingest.enabled", havingValue = "true", matchIfMissing = true)
public class SecretariatDocumentBulkListener {

  private final SecretariatDocumentBulkProcessor processor;

  public SecretariatDocumentBulkListener(SecretariatDocumentBulkProcessor processor) {
    this.processor = processor;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onAppended(ProcessEventAppendedEvent evt) {
    if (evt == null) {
      return;
    }
    if (!ProcessEventType.DOCUMENTS_BULK_ADDED.name().equals(evt.eventType())) {
      return;
    }
    processor.enqueue(evt);
  }
}
