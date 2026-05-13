package com.tcc.pjb.backend.core.jobs.runtime;

import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public final class JobPgNotifyEmitter {

    private final JdbcTemplate jdbc;

    public JobPgNotifyEmitter(DataSource ds) {
        this.jdbc = new JdbcTemplate(Objects.requireNonNull(ds));
    }

    public void notifyJobCreated(String jobId) {
        String payload = jobId == null ? "" : jobId;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emit(payload);
                }
            });
            return;
        }
        emit(payload);
    }

    private void emit(String payload) {
        String p = payload.length() > 256 ? payload.substring(0, 256) : payload;
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) con -> {
            try (var ps = con.prepareStatement("select pg_notify('pjb_job', ?)") ) {
                ps.setString(1, p);
                ps.execute();
                return null;
            }
        });
    }
}
