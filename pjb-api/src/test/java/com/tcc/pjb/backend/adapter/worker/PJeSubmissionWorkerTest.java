package com.tcc.pjb.backend.adapter.worker;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tcc.pjb.backend.adapter.factory.PJeAdapterFactory;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PJeSubmissionWorkerTest {

    @Test
    void deveFalharQuandoCorrelationIdAusente() throws Exception {
        PJeSubmissionWorker worker = new PJeSubmissionWorker((PJeAdapterFactory) null);
        Method method = PJeSubmissionWorker.class.getDeclaredMethod("getCorrelationId", Map.class);
        method.setAccessible(true);
        assertThrows(IllegalArgumentException.class, () -> invoke(method, worker, Map.of()));
    }

    @Test
    void deveFalharQuandoAdapterBeanNameAusente() throws Exception {
        PJeSubmissionWorker worker = new PJeSubmissionWorker((PJeAdapterFactory) null);
        Method method = PJeSubmissionWorker.class.getDeclaredMethod("getAdapterKey", Map.class, String.class);
        method.setAccessible(true);
        assertThrows(IllegalArgumentException.class, () -> invoke(method, worker, Map.of(), "corr-96"));
    }

    private Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException ex) {
            if (ex.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(ex);
        }
    }
}
