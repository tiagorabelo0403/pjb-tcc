package com.tcc.pjb.backend.service.upload;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;

@Component
public class UploadStreamLimiter {

    private final Semaphore semaphore;

    public UploadStreamLimiter(ObjectStorageProperties props) {
        int max = Math.max(1, props.getUpload().getMaxConcurrentStreams());
        this.semaphore = new Semaphore(max);
    }

    public Permit acquire(Duration maxWait) {
        boolean ok;
        try {
            ok = semaphore.tryAcquire(maxWait.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ok = false;
        }
        if (!ok) {
            throw new TooManyUploadsException("limite de uploads simultâneos atingido");
        }
        return new Permit(semaphore);
    }

    public static final class Permit implements AutoCloseable {
        private final Semaphore semaphore;
        private boolean released;

        private Permit(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void close() {
            if (!released) {
                released = true;
                semaphore.release();
            }
        }
    }
}
