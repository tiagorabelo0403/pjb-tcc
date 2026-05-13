package com.tcc.pjb.backend.workflow.zeebe;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ZeebeCompat {

    private static final ObjectMapper OM = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private static final long DEFAULT_AWAIT_TIMEOUT_MS = Math.max(1000L, Long.getLong("pjb.zeebe.await.timeout-ms", 30000L));

    private ZeebeCompat() {
    }

    public static Object withVariables(Object fluentCommand, Map<String, Object> vars) {
        if (fluentCommand == null) {
            return null;
        }
        Map<String, Object> safe = (vars == null) ? Collections.emptyMap() : vars;
        Object out = tryInvoke(fluentCommand, "variables", new Class[]{Map.class}, new Object[]{safe});
        if (out != null) {
            return out;
        }
        try {
            String json = OM.writeValueAsString(safe);
            out = tryInvoke(fluentCommand, "variables", new Class[]{String.class}, new Object[]{json});
            if (out != null) {
                return out;
            }
        } catch (Exception ignored) {
        }
        return fluentCommand;
    }

    public static Object send(Object fluentCommand) {
        if (fluentCommand == null) {
            return null;
        }
        Object out = tryInvoke(fluentCommand, "send", new Class[]{}, new Object[]{});
        if (out != null) {
            return out;
        }
        throw new IllegalStateException("Unable to send command: send() not found on " + fluentCommand.getClass());
    }

    public static Object latestVersionIfPossible(Object fluentCommand) {
        if (fluentCommand == null) {
            return null;
        }
        Object out = tryInvoke(fluentCommand, "latestVersion", new Class[]{}, new Object[]{});
        if (out != null) {
            return out;
        }
        out = tryInvoke(fluentCommand, "version", new Class[]{int.class}, new Object[]{-1});
        if (out != null) {
            return out;
        }
        return fluentCommand;
    }

    public static Object await(Object future) {
        if (future == null) {
            return null;
        }
        try {
            if (future instanceof CompletionStage<?> cs) {
                return cs.toCompletableFuture().get(DEFAULT_AWAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
            if (future instanceof Future<?> f) {
                return f.get(DEFAULT_AWAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to await future: " + e.getMessage(), e);
        }
        Object timed = tryInvoke(future, "get", new Class[]{long.class, TimeUnit.class}, new Object[]{DEFAULT_AWAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS});
        if (timed != null) {
            return timed;
        }
        Object joined = tryInvoke(future, "join", new Class[]{}, new Object[]{});
        if (joined != null) {
            return joined;
        }
        Object get = tryInvoke(future, "get", new Class[]{}, new Object[]{});
        if (get != null) {
            return get;
        }
        throw new IllegalStateException("Unsupported future type: " + future.getClass());
    }

    public static Map<String, Object> decisionOutputAsMap(Object evaluateDecisionResponse) {
        if (evaluateDecisionResponse == null) {
            return Collections.emptyMap();
        }
        Object map = tryInvoke(evaluateDecisionResponse, "getDecisionOutputAsMap", new Class[]{}, new Object[]{});
        if (map instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        Object out = tryInvoke(evaluateDecisionResponse, "getDecisionOutput", new Class[]{}, new Object[]{});
        if (out instanceof Map<?, ?> m2) {
            return (Map<String, Object>) m2;
        }
        if (out instanceof String s) {
            return parseJsonMap(s);
        }
        out = tryInvoke(evaluateDecisionResponse, "getDecisionOutputJson", new Class[]{}, new Object[]{});
        if (out instanceof String s2) {
            return parseJsonMap(s2);
        }
        out = tryInvoke(evaluateDecisionResponse, "getOutput", new Class[]{}, new Object[]{});
        if (out instanceof String s3) {
            return parseJsonMap(s3);
        }
        return Collections.emptyMap();
    }

    private static Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OM.readValue(json, MAP);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private static Object tryInvoke(Object target, String name, Class<?>[] paramTypes, Object[] args) {
        try {
            Method m = target.getClass().getMethod(name, paramTypes);
            return m.invoke(target, args);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Invocation failed: " + target.getClass().getSimpleName() + "." + name, e);
        }
    }
}
