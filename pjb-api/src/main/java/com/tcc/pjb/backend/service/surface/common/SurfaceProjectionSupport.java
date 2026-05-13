package com.tcc.pjb.backend.service.surface.common;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceFieldResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceListItemResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SurfaceProjectionSupport {

    public SurfaceSnapshotResponse snapshot(String scope, Object source) {
        return new SurfaceSnapshotResponse(scope, toFields(source));
    }

    public SurfaceActionResponse action(String scope, String action, Long processoId, Object source) {
        Map<String, Object> normalized = asMap(source);
        String status = Objects.toString(normalized.getOrDefault("status", "OK"), "OK");
        return new SurfaceActionResponse(scope, action, processoId, status, toFields(normalized));
    }

    public SurfaceCollectionResponse collection(String scope, Iterable<?> source) {
        ArrayList<SurfaceListItemResponse> items = new ArrayList<>();
        if (source != null) {
            int index = 0;
            for (Object item : source) {
                items.add(new SurfaceListItemResponse(String.valueOf(index++), toFields(item)));
            }
        }
        return new SurfaceCollectionResponse(scope, List.copyOf(items));
    }

    public SurfaceCollectionResponse collection(String scope, Map<String, ?> source) {
        ArrayList<SurfaceListItemResponse> items = new ArrayList<>();
        if (source != null) {
            source.forEach((key, value) -> items.add(new SurfaceListItemResponse(String.valueOf(key), toFields(value))));
        }
        return new SurfaceCollectionResponse(scope, List.copyOf(items));
    }

    public List<SurfaceFieldResponse> toFields(Object source) {
        Map<String, Object> normalized = asMap(source);
        ArrayList<SurfaceFieldResponse> fields = new ArrayList<>();
        normalized.forEach((key, value) -> fields.add(new SurfaceFieldResponse(key, normalizeValue(value))));
        return List.copyOf(fields);
    }

    private Map<String, Object> asMap(Object source) {
        Object normalized = normalizeValue(source);
        if (normalized instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, value) -> out.put(String.valueOf(key), value));
            return out;
        }
        LinkedHashMap<String, Object> single = new LinkedHashMap<>();
        single.put("value", normalized);
        return single;
    }

    private Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumeration) {
            return enumeration.name();
        }
        if (value instanceof UUID uuid) {
            return uuid.toString();
        }
        if (value instanceof TemporalAccessor) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, entryValue) -> normalized.put(String.valueOf(key), normalizeValue(entryValue)));
            return normalized;
        }
        if (value instanceof Iterable<?> iterable) {
            ArrayList<Object> normalized = new ArrayList<>();
            for (Object item : iterable) {
                normalized.add(normalizeValue(item));
            }
            return List.copyOf(normalized);
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            ArrayList<Object> normalized = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                normalized.add(normalizeValue(Array.get(value, index)));
            }
            return List.copyOf(normalized);
        }
        if (type.isRecord()) {
            LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
            for (RecordComponent component : type.getRecordComponents()) {
                try {
                    normalized.put(component.getName(), normalizeValue(component.getAccessor().invoke(value)));
                } catch (ReflectiveOperationException ex) {
                    normalized.put(component.getName(), "<erro-reflexao>");
                }
            }
            return normalized;
        }
        Package objectPackage = type.getPackage();
        if (objectPackage != null && objectPackage.getName().startsWith("java.")) {
            return value;
        }
        return Objects.toString(value);
    }
}
